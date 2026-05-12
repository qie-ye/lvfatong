#!/usr/bin/env python3
"""
分批次训练调度器 - 支持断点续训、失败重试、自动恢复
用于律法通法律问答模型微调
"""

import json
import os
import sys
import time
import logging
import subprocess
from pathlib import Path
from datetime import datetime
from dataclasses import dataclass, asdict
from typing import List, Optional
from enum import Enum

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('logs/training.log', encoding='utf-8'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


class BatchStatus(Enum):
    """批次状态枚举"""
    PENDING = "pending"      # 待训练
    RUNNING = "running"      # 训练中
    COMPLETED = "completed"  # 已完成
    FAILED = "failed"        # 失败（可重试）
    SKIPPED = "skipped"      # 已跳过（最终失败）


@dataclass
class BatchInfo:
    """批次信息"""
    batch_id: int
    status: str
    data_range: List[int]
    data_count: int
    epochs_completed: int
    total_steps: int
    loss: Optional[float]
    started_at: Optional[str]
    completed_at: Optional[str]
    retry_count: int
    checkpoint_path: Optional[str]
    error_message: Optional[str]


@dataclass
class TrainingState:
    """训练状态"""
    version: str
    total_batches: int
    batch_size: int
    current_batch_index: int
    created_at: str
    updated_at: str
    batches: List[BatchInfo]
    retry_queue: List[int]
    failed_queue: List[int]
    logs: List[dict]


class TrainingError(Exception):
    """训练错误"""
    pass


class BatchTrainingScheduler:
    """分批次训练调度器"""
    
    def __init__(self, state_file: str = "training_state.json"):
        self.state_file = Path(state_file)
        self.max_retries = 3
        self.state = self.load_state()
        
        # 确保必要的目录存在
        Path("logs").mkdir(exist_ok=True)
        Path("data/batches").mkdir(parents=True, exist_ok=True)
        Path("outputs").mkdir(exist_ok=True)
    
    def load_state(self) -> TrainingState:
        """加载训练状态（支持断点续训）"""
        if self.state_file.exists():
            logger.info(f"加载训练状态：{self.state_file}")
            with open(self.state_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
                # 正确反序列化嵌套的BatchInfo对象
                batches = [BatchInfo(**b) for b in data.get('batches', [])]
                return TrainingState(
                    version=data.get('version', '1.0'),
                    total_batches=data.get('total_batches', 0),
                    batch_size=data.get('batch_size', 5000),
                    current_batch_index=data.get('current_batch_index', 0),
                    created_at=data.get('created_at', ''),
                    updated_at=data.get('updated_at', ''),
                    batches=batches,
                    retry_queue=data.get('retry_queue', []),
                    failed_queue=data.get('failed_queue', []),
                    logs=data.get('logs', [])
                )
        else:
            logger.info("创建新的训练状态")
            return self.create_initial_state()
    
    def create_initial_state(self) -> TrainingState:
        """创建初始训练状态"""
        # 检查批次文件是否存在
        batches_dir = Path("data/batches")
        batch_files = sorted(batches_dir.glob("batch_*.json"))
        
        if not batch_files:
            raise FileNotFoundError("未找到批次文件，请先运行 prepare_batches.py")
        
        total_batches = len(batch_files)
        batch_size = self.estimate_batch_size(batch_files[0])
        
        batches = []
        for i, batch_file in enumerate(batch_files, 1):
            batch_info = BatchInfo(
                batch_id=i,
                status=BatchStatus.PENDING.value,
                data_range=[0, 0],  # 将在初始化时填充
                data_count=batch_size,
                epochs_completed=0,
                total_steps=0,
                loss=None,
                started_at=None,
                completed_at=None,
                retry_count=0,
                checkpoint_path=None,
                error_message=None
            )
            batches.append(batch_info)
        
        state = TrainingState(
            version="1.0",
            total_batches=total_batches,
            batch_size=batch_size,
            current_batch_index=0,
            created_at=datetime.now().isoformat(),
            updated_at=datetime.now().isoformat(),
            batches=batches,
            retry_queue=[],
            failed_queue=[],
            logs=[]
        )
        
        self.save_state(state)
        return state
    
    def estimate_batch_size(self, batch_file: Path) -> int:
        """估算批次数据量"""
        try:
            with open(batch_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
                return len(data)
        except Exception:
            return 5000  # 默认值
    
    def save_state(self, state: Optional[TrainingState] = None):
        """保存训练状态"""
        if state is None:
            state = self.state
        
        state.updated_at = datetime.now().isoformat()
        
        with open(self.state_file, 'w', encoding='utf-8') as f:
            json.dump(asdict(state), f, ensure_ascii=False, indent=2)
        
        logger.debug("训练状态已保存")
    
    def get_next_batch(self) -> Optional[BatchInfo]:
        """获取下一个待训练的批次"""
        # 优先从重试队列获取
        if self.state.retry_queue:
            batch_id = self.state.retry_queue.pop(0)
            batch = self.get_batch(batch_id)
            if batch:
                logger.info(f"从重试队列获取批次 {batch_id}")
                return batch
        
        # 然后获取下一个pending批次
        for batch in self.state.batches:
            if batch.status == BatchStatus.PENDING.value:
                logger.info(f"获取待训练批次 {batch.batch_id}")
                return batch
        
        return None
    
    def get_batch(self, batch_id: int) -> Optional[BatchInfo]:
        """根据ID获取批次信息"""
        for batch in self.state.batches:
            if batch.batch_id == batch_id:
                return batch
        return None
    
    def has_pending_batches(self) -> bool:
        """检查是否还有待训练的批次"""
        # 检查pending状态的批次
        for batch in self.state.batches:
            if batch.status == BatchStatus.PENDING.value:
                return True
        
        # 检查重试队列
        if self.state.retry_queue:
            return True
        
        return False
    
    def train_batch_with_retry(self, batch: BatchInfo, is_retry_phase: bool = False) -> bool:
        """带重试的批次训练"""
        for attempt in range(self.max_retries):
            try:
                logger.info(f"训练批次 {batch.batch_id}，第 {attempt+1}/{self.max_retries} 次尝试")
                
                # 更新状态为running
                batch.status = BatchStatus.RUNNING.value
                batch.retry_count = attempt
                batch.started_at = datetime.now().isoformat()
                self.save_state()
                
                # 执行训练
                result = self.execute_training(batch)
                
                # 训练成功
                batch.status = BatchStatus.COMPLETED.value
                batch.epochs_completed = result['epochs']
                batch.total_steps = result['steps']
                batch.loss = result['final_loss']
                batch.completed_at = datetime.now().isoformat()
                batch.checkpoint_path = result.get('checkpoint_path')
                self.save_state()
                
                # 记录日志
                self.log_event(batch.batch_id, "training_completed", {
                    "attempt": attempt + 1,
                    "epochs": result['epochs'],
                    "steps": result['steps'],
                    "loss": result['final_loss']
                })
                
                logger.info(f"批次 {batch.batch_id} 训练完成，loss: {result['final_loss']:.4f}")
                return True
                
            except TrainingError as e:
                logger.error(f"批次 {batch.batch_id} 训练失败：{e}")
                batch.error_message = str(e)
                
                # 记录失败日志
                self.log_event(batch.batch_id, "training_failed", {
                    "attempt": attempt + 1,
                    "error": str(e)
                })
                
                if attempt < self.max_retries - 1:
                    # 还有重试机会，等待后重试
                    wait_time = (attempt + 1) * 60  # 递增等待：1分钟、2分钟、3分钟
                    logger.info(f"等待 {wait_time} 秒后重试...")
                    time.sleep(wait_time)
                else:
                    # 重试次数用完
                    logger.warning(f"批次 {batch.batch_id} 重试 {self.max_retries} 次后失败")
                    batch.status = BatchStatus.FAILED.value
                    self.save_state()
                    return False
        
        return False
    
    def execute_training(self, batch: BatchInfo) -> dict:
        """执行实际训练（调用LLaMA-Factory）"""
        batch_id = batch.batch_id
        data_file = f"data/batches/batch_{batch_id:02d}.json"
        output_dir = f"outputs/batch_{batch_id:02d}"
        config_file = f"train_config_batch_{batch_id:02d}.yaml"
        
        # 检查数据文件是否存在
        if not Path(data_file).exists():
            raise TrainingError(f"数据文件不存在：{data_file}")
        
        # 生成批次训练配置
        self.generate_batch_config(batch_id, data_file, output_dir, config_file)
        
        # 生成数据集信息文件
        self.generate_dataset_info(batch_id, data_file)
        
        # 调用LLaMA-Factory训练
        logger.info(f"开始训练批次 {batch_id}...")
        
        try:
            # 使用llamafactory-cli执行训练
            # 获取项目根目录的绝对路径
            project_root = Path(__file__).parent.parent
            llamafactory_cli = project_root / "llmfactory_env" / "Scripts" / "llamafactory-cli.exe"
            
            if not llamafactory_cli.exists():
                raise TrainingError(f"LLaMA-Factory CLI not found: {llamafactory_cli}")
            
            cmd = [
                str(llamafactory_cli),
                "train",
                str(project_root / "finetune" / config_file)
            ]
            
            # 执行训练命令
            process = subprocess.Popen(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                encoding='utf-8'
            )
            
            # 实时输出日志
            log_file = f"logs/batch_{batch_id:02d}.log"
            with open(log_file, 'w', encoding='utf-8') as f:
                for line in process.stdout:
                    f.write(line)
                    f.flush()
                    # 检查是否是关键信息
                    if "loss" in line.lower():
                        logger.info(f"Batch {batch_id}: {line.strip()}")
            
            # 等待进程完成
            process.wait()
            
            if process.returncode != 0:
                raise TrainingError(f"训练命令失败，返回码：{process.returncode}")
            
            # 解析训练结果
            result = self.parse_training_result(output_dir)
            result['checkpoint_path'] = output_dir
            
            return result
            
        except Exception as e:
            raise TrainingError(f"训练执行失败：{e}")
    
    def generate_batch_config(self, batch_id: int, data_file: str, output_dir: str, config_file: str):
        """生成批次训练配置"""
        template_config = Path("train_config.yaml")
        
        if not template_config.exists():
            # 创建默认配置
                config_content = f"""### model
model_name_or_path: {os.environ.get('MODEL_PATH', '/path/to/Qwen2-7B-Instruct')}

### method
stage: sft
do_train: true
finetuning_type: lora

### QLoRA (4-bit, 8GB显存适配)
quantization_method: bnb
quantization_bit: 4
quantization_type: nf4
double_quantization: true

### lora
lora_target: all
lora_rank: 8
lora_alpha: 16
lora_dropout: 0.1

### dataset
dataset: lawgpt_batch_{batch_id}
dataset_dir: {self.state_file.parent.resolve()}
template: alpaca
cutoff_len: 512
max_samples: 6000
overwrite_cache: true

### training args
output_dir: {output_dir}
logging_steps: 10
save_steps: 500
save_total_limit: 3
plot_loss: true

learning_rate: 1.0e-4
num_train_epochs: 2.0
per_device_train_batch_size: 1
gradient_accumulation_steps: 16
lr_scheduler_type: cosine
warmup_ratio: 0.1
optim: paged_adamw_8bit

bf16: false
fp16: true

dataloader_num_workers: 0
val_size: 0.05
per_device_eval_batch_size: 1
eval_strategy: steps
eval_steps: 500
"""
        else:
            # 读取模板配置并修改
            with open(template_config, 'r', encoding='utf-8') as f:
                config_content = f.read()
            
            # 替换关键参数
            config_content = config_content.replace(
                "dataset: lawgpt_legal_qa",
                f"dataset: lawgpt_batch_{batch_id}"
            )
            config_content = config_content.replace(
                "output_dir: outputs/lawgpt-lora",
                f"output_dir: {output_dir}"
            )
        
        # 写入配置文件
        with open(config_file, 'w', encoding='utf-8') as f:
            f.write(config_content)
        
        logger.info(f"生成训练配置：{config_file}")
    
    def generate_dataset_info(self, batch_id: int, data_file: str):
        """生成数据集信息文件"""
        dataset_info = {
            f"lawgpt_batch_{batch_id}": {
                "file_name": data_file,
                "formatting": "alpaca",
                "columns": {
                    "prompt": "instruction",
                    "query": "input",
                    "response": "output"
                },
                "tags": {
                    "role_tag": "system",
                    "content_tag": "instruction",
                    "user_tag": "human",
                    "assistant_tag": "gpt"
                }
            }
        }
        
        # 写入数据集信息文件
        dataset_info_file = Path("dataset_info.json")
        if dataset_info_file.exists():
            with open(dataset_info_file, 'r', encoding='utf-8') as f:
                existing_info = json.load(f)
            existing_info.update(dataset_info)
            dataset_info = existing_info
        
        with open(dataset_info_file, 'w', encoding='utf-8') as f:
            json.dump(dataset_info, f, ensure_ascii=False, indent=2)
        
        logger.info(f"生成数据集信息：{dataset_info_file}")
    
    def parse_training_result(self, output_dir: str) -> dict:
        """解析训练结果"""
        # 尝试从训练日志中解析结果
        result = {
            'epochs': 2,
            'steps': 0,
            'final_loss': 0.0
        }
        
        # 查找训练日志文件
        log_file = Path(output_dir) / "trainer_log.json"
        if log_file.exists():
            try:
                with open(log_file, 'r', encoding='utf-8') as f:
                    logs = json.load(f)
                    if logs:
                        last_log = logs[-1]
                        result['steps'] = last_log.get('global_step', 0)
                        result['final_loss'] = last_log.get('loss', 0.0)
            except Exception as e:
                logger.warning(f"解析训练日志失败：{e}")
        
        return result
    
    def log_event(self, batch_id: int, event: str, details: dict):
        """记录事件日志"""
        log_entry = {
            "timestamp": datetime.now().isoformat(),
            "batch_id": batch_id,
            "event": event,
            "details": details
        }
        self.state.logs.append(log_entry)
        self.save_state()
    
    def run(self):
        """主训练循环"""
        logger.info("=" * 60)
        logger.info("开始分批次训练")
        logger.info(f"总批次：{self.state.total_batches}")
        logger.info("=" * 60)
        
        # 阶段1：执行所有批次（带重试）
        while self.has_pending_batches():
            batch = self.get_next_batch()
            
            if batch is None:
                break
            
            logger.info(f"开始训练批次 {batch.batch_id}/{self.state.total_batches}")
            
            success = self.train_batch_with_retry(batch)
            
            if not success:
                # 加入失败队列
                if batch.batch_id not in self.state.failed_queue:
                    self.state.failed_queue.append(batch.batch_id)
                logger.warning(f"批次 {batch.batch_id} 最终失败，已加入失败队列")
            
            # 更新当前批次索引
            self.state.current_batch_index = batch.batch_id
            self.save_state()
            
            # 显示进度
            self.show_progress()
        
        # 阶段2：重试失败的批次
        if self.state.failed_queue:
            logger.info("=" * 60)
            logger.info(f"开始重试 {len(self.state.failed_queue)} 个失败批次")
            logger.info("=" * 60)
            
            failed_batches = self.state.failed_queue.copy()
            for batch_id in failed_batches:
                batch = self.get_batch(batch_id)
                if batch:
                    # 重置批次状态
                    batch.status = BatchStatus.PENDING.value
                    batch.retry_count = 0
                    batch.error_message = None
                    
                    logger.info(f"重试批次 {batch_id}")
                    success = self.train_batch_with_retry(batch, is_retry_phase=True)
                    
                    if success:
                        self.state.failed_queue.remove(batch_id)
                    else:
                        # 最终失败，标记为skipped
                        batch.status = BatchStatus.SKIPPED.value
                        logger.error(f"批次 {batch_id} 最终失败，已跳过")
            
            self.save_state()
        
        # 阶段3：显示最终结果
        self.show_final_result()
    
    def show_progress(self):
        """显示训练进度"""
        completed = sum(1 for b in self.state.batches if b.status == BatchStatus.COMPLETED.value)
        total = self.state.total_batches
        progress = completed / total * 100 if total > 0 else 0
        
        # 进度条
        bar_length = 20
        filled = int(bar_length * completed / total) if total > 0 else 0
        bar = '█' * filled + '░' * (bar_length - filled)
        
        logger.info(f"\n训练进度：[{bar}] {progress:.1f}% ({completed}/{total} 批次完成)")
        
        # 当前状态
        for batch in self.state.batches:
            if batch.status == BatchStatus.RUNNING.value:
                logger.info(f"当前批次：批次{batch.batch_id} - 训练中")
            elif batch.status == BatchStatus.FAILED.value:
                logger.info(f"失败批次：批次{batch.batch_id} - 已重试{batch.retry_count}次")
    
    def show_final_result(self):
        """显示最终训练结果"""
        logger.info("\n" + "=" * 60)
        logger.info("训练完成！最终结果：")
        logger.info("=" * 60)
        
        completed = [b for b in self.state.batches if b.status == BatchStatus.COMPLETED.value]
        failed = [b for b in self.state.batches if b.status in [BatchStatus.FAILED.value, BatchStatus.SKIPPED.value]]
        
        logger.info(f"成功批次：{len(completed)}/{self.state.total_batches}")
        logger.info(f"失败批次：{len(failed)}")
        
        if completed:
            avg_loss = sum(b.loss for b in completed if b.loss) / len(completed)
            logger.info(f"平均loss：{avg_loss:.4f}")
        
        if failed:
            logger.warning(f"失败的批次ID：{[b.batch_id for b in failed]}")
        
        logger.info("=" * 60)
    
    def status(self):
        """显示当前训练状态"""
        print("\n" + "=" * 60)
        print("训练状态")
        print("=" * 60)
        print(f"总批次：{self.state.total_batches}")
        print(f"批次大小：{self.state.batch_size}")
        print(f"创建时间：{self.state.created_at}")
        print(f"更新时间：{self.state.updated_at}")
        print("\n批次详情：")
        
        for batch in self.state.batches:
            status_icon = {
                BatchStatus.PENDING.value: "[ ]",
                BatchStatus.RUNNING.value: "[>]",
                BatchStatus.COMPLETED.value: "[v]",
                BatchStatus.FAILED.value: "[x]",
                BatchStatus.SKIPPED.value: "[-]"
            }.get(batch.status, "[?]")
            
            print(f"  {status_icon} Batch{batch.batch_id:2d}: {batch.status:10s} | "
                  f"Data: {batch.data_count:5d} | "
                  f"Retries: {batch.retry_count}/{self.max_retries}")
        
        print("\n" + "=" * 60)


def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='分批次训练调度器')
    parser.add_argument('command', choices=['start', 'resume', 'status', 'retry-failed'],
                       help='命令：start=开始训练, resume=恢复训练, status=查看状态, retry-failed=重试失败批次')
    
    args = parser.parse_args()
    
    scheduler = BatchTrainingScheduler()
    
    if args.command == 'start':
        scheduler.run()
    elif args.command == 'resume':
        logger.info("从断点恢复训练...")
        scheduler.run()
    elif args.command == 'status':
        scheduler.status()
    elif args.command == 'retry-failed':
        if not scheduler.state.failed_queue:
            print("没有需要重试的失败批次")
        else:
            logger.info(f"重试 {len(scheduler.state.failed_queue)} 个失败批次...")
            scheduler.run()


if __name__ == "__main__":
    main()
