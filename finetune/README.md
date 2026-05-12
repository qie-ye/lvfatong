# 律法通 - 法律大模型微调

基于 [LLaMA-Factory](https://github.com/hiyouga/LLaMA-Factory) 对 Qwen2-7B-Instruct 进行 LoRA 微调，训练法律领域专用问答模型。

## 微调流程

```
数据获取 → 数据转换 → 质量验证 → 分批训练 → 断点续训
```

### 1. 数据获取

| 数据集 | 来源 | 用途 |
|--------|------|------|
| CAIL2018 | 中国法律AI挑战赛 | 刑法案例问答 |
| LawGPT-data | HuggingFace | 通用法律问答 |

```bash
python download_dataset.py          # 从 HuggingFace 下载
python download_modelscope.py       # 从 ModelScope 下载（国内镜像）
python extract_existing_data.py     # 从项目 FAQ/法条提取
```

### 2. 数据转换

将原始数据转换为 Alpaca 训练格式：

```bash
python convert_cail.py --input data/CAIL2018/... --output data/LawGPT-data-full.json
```

输出格式：
```json
{
  "instruction": "你是律法通法律咨询助手...",
  "input": "租房合同没到期房东要我搬走怎么办？",
  "output": "【事实分析】...\n【法律适用】...\n【结论】..."
}
```

### 3. 质量验证

```bash
python verify_data.py        # 验证数据格式
python check_encoding.py     # 检查中文编码
python check_diversity.py    # 分析数据多样性
python check_dataset.py      # 检查数据集配置
```

### 4. 分批训练

大数据集分批训练，支持断点续训和失败重试：

```bash
python prepare_batches.py --batch-size 1000     # 分割批次
python batch_scheduler.py start                 # 开始训练
python batch_scheduler.py status                # 查看进度
python batch_scheduler.py resume                # 断点恢复
python batch_scheduler.py retry-failed          # 重试失败批次
```

## 训练配置

| 参数 | 值 | 说明 |
|------|-----|------|
| 基座模型 | Qwen2-7B-Instruct | 中文能力优秀的 7B 模型 |
| 微调方法 | LoRA (rank=8, alpha=16) | 参数高效微调，可训练参数 0.26% |
| 量化 | QLoRA 4-bit (NF4) | 降低显存需求，适配 RTX 4090 |
| 学习率 | 1e-4 → 0 (cosine) | cosine 调度，warmup 10% |
| 批大小 | 1 × 16(梯度累积) | 等效 batch_size=16 |
| 优化器 | Paged AdamW 8-bit | 节省显存 |
| 训练设备 | NVIDIA RTX 4090 | 单卡训练 |
| 训练耗时 | 约 36 分钟 | 2 epoch，1000 条数据 |

## 训练结果

| 指标 | 起始 | 最终 | 说明 |
|------|------|------|------|
| Train Loss | 1.6964 | **1.0199** | 训练集 loss 从 1.70 收敛至 1.02 |
| Eval Loss | 1.0506 | **1.0409** | 验证集 loss 保持稳定 |

## 训练 Loss 曲线

| 训练 Loss | 验证 Loss |
|:---:|:---:|
| ![Training Loss](../docs/screenshots/training_loss.png) | ![Eval Loss](../docs/screenshots/training_eval_loss.png) |

> LLaMA-Factory 自动生成的 loss 曲线，左图为训练集 loss 收敛趋势，右图为验证集 loss 变化，展示了模型在 LoRA 微调过程中的学习效果。

## 文件说明

| 文件 | 说明 |
|------|------|
| `convert_cail.py` | CAIL2018 刑法数据集转换 |
| `prepare_batches.py` | 数据分批工具 |
| `batch_scheduler.py` | 分批训练调度器（断点续训/失败重试） |
| `train_config.yaml` | 基础训练配置 |
| `train_config_batch_*.yaml` | 分批训练配置 |
| `training_state.json` | 训练状态记录 |
| `download_dataset.py` | HuggingFace 数据下载 |
| `download_modelscope.py` | ModelScope 数据下载 |
| `extract_existing_data.py` | 从项目数据提取训练样本 |
| `check_*.py` / `verify_*.py` | 数据质量验证工具 |

## 遇到的问题与解决

1. **显存不足**：从全量微调改为 QLoRA 4-bit 量化，显存需求从 16GB 降至 8GB
2. **数据格式不兼容**：CAIL2018 原始格式需转换为 Alpaca 格式，编写 `convert_cail.py` 处理
3. **训练中断**：实现 `batch_scheduler.py` 支持断点续训和自动重试
4. **数据质量**：编写验证脚本检查编码、多样性和格式一致性
