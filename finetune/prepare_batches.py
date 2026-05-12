#!/usr/bin/env python3
"""
分割数据集为批次用于分批次训练
支持自动分割、验证数据格式
"""

import json
import os
import random
from pathlib import Path
from typing import List, Dict


def load_full_dataset(data_file: str = "data/LawGPT-data-full.json") -> List[Dict]:
    """加载完整数据集"""
    if not Path(data_file).exists():
        print(f"错误：数据文件不存在 {data_file}")
        print("请先运行 download_dataset.py 下载数据集")
        return []
    
    with open(data_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    print(f"加载数据集：{len(data)} 条")
    return data


def validate_data_format(data: List[Dict]) -> bool:
    """验证数据格式是否符合Alpaca格式"""
    required_keys = ['instruction', 'output']
    
    for i, item in enumerate(data[:10]):  # 只检查前10条
        if not all(key in item for key in required_keys):
            print(f"数据格式错误：第 {i+1} 条缺少必要字段")
            print(f"必需字段：{required_keys}")
            print(f"当前字段：{list(item.keys())}")
            return False
    
    print("数据格式验证通过")
    return True


def split_into_batches(data: List[Dict], batch_size: int = 5000) -> List[List[Dict]]:
    """将数据分割为批次"""
    batches = []
    
    for i in range(0, len(data), batch_size):
        batch = data[i:i + batch_size]
        batches.append(batch)
    
    print(f"已分割为 {len(batches)} 个批次")
    for i, batch in enumerate(batches, 1):
        print(f"  批次 {i:2d}: {len(batch)} 条")
    
    return batches


def save_batches(batches: List[List[Dict]], output_dir: str = "data/batches"):
    """保存批次文件"""
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)
    
    for i, batch in enumerate(batches, 1):
        output_file = output_path / f"batch_{i:02d}.json"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(batch, f, ensure_ascii=False, indent=2)
        
        print(f"保存批次 {i}: {output_file}")
    
    print(f"所有批次已保存到：{output_dir}")


def create_dataset_info(batches: List[List[Dict]], output_file: str = "dataset_info.json"):
    """创建数据集信息文件（用于LLaMA-Factory）"""
    dataset_info = {}
    
    for i, batch in enumerate(batches, 1):
        dataset_name = f"lawgpt_batch_{i}"
        data_file = f"data/batches/batch_{i:02d}.json"
        
        dataset_info[dataset_name] = {
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
    
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(dataset_info, f, ensure_ascii=False, indent=2)
    
    print(f"数据集信息已保存到：{output_file}")


def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='分割数据集为批次')
    parser.add_argument('--batch-size', type=int, default=5000,
                       help='每个批次的数据量（默认：5000）')
    parser.add_argument('--data-file', type=str, default='data/LawGPT-data-full.json',
                       help='输入数据文件路径')
    parser.add_argument('--output-dir', type=str, default='data/batches',
                       help='输出目录')
    parser.add_argument('--shuffle', action='store_true',
                       help='打乱数据顺序')
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("数据集批次分割工具")
    print("=" * 60)
    
    # 加载数据
    data = load_full_dataset(args.data_file)
    if not data:
        return
    
    # 验证格式
    if not validate_data_format(data):
        return
    
    # 打乱数据（可选）
    if args.shuffle:
        random.seed(42)
        random.shuffle(data)
        print("已打乱数据顺序")
    
    # 分割批次
    batches = split_into_batches(data, args.batch_size)
    
    # 保存批次
    save_batches(batches, args.output_dir)
    
    # 创建数据集信息
    create_dataset_info(batches)
    
    print("\n" + "=" * 60)
    print("批次分割完成！")
    print(f"总数据量：{len(data)} 条")
    print(f"批次大小：{args.batch_size} 条")
    print(f"批次数量：{len(batches)} 个")
    print("=" * 60)


if __name__ == "__main__":
    main()
