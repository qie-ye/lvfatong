#!/usr/bin/env python3
"""
下载LawGPT-data数据集用于大模型微调
数据来源：HuggingFace - xusenlin/LawGPT-data
格式：Alpaca格式（instruction, input, output）
"""

import os
import json
from pathlib import Path

def download_dataset():
    """下载LawGPT-data数据集"""
    try:
        from datasets import load_dataset
        
        print("正在下载LawGPT-data数据集...")
        print("数据集地址：https://huggingface.co/datasets/xusenlin/LawGPT-data")
        
        # 下载数据集
        dataset = load_dataset("xusenlin/LawGPT-data", split="train")
        
        print(f"下载完成！共 {len(dataset)} 条数据")
        
        # 保存到本地
        output_dir = Path("data")
        output_dir.mkdir(exist_ok=True)
        
        output_file = output_dir / "LawGPT-data-full.json"
        
        # 转换为列表并保存
        data_list = [item for item in dataset]
        
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(data_list, f, ensure_ascii=False, indent=2)
        
        print(f"数据已保存到：{output_file}")
        print(f"数据格式示例：")
        if data_list:
            print(json.dumps(data_list[0], ensure_ascii=False, indent=2))
        
        return output_file, data_list
        
    except ImportError:
        print("错误：需要安装 datasets 库")
        print("请运行：pip install datasets")
        return None, None
    except Exception as e:
        print(f"下载失败：{e}")
        return None, None

def extract_subset(data_list, num_samples=1000):
    """提取子集用于Demo训练"""
    if not data_list:
        return None
    
    # 随机采样（可设置随机种子保证可复现）
    import random
    random.seed(42)
    
    if len(data_list) <= num_samples:
        print(f"数据集只有 {len(data_list)} 条，全部使用")
        subset = data_list
    else:
        subset = random.sample(data_list, num_samples)
        print(f"已从 {len(data_list)} 条中随机抽取 {num_samples} 条")
    
    # 保存子集
    output_dir = Path("data")
    output_file = output_dir / f"LawGPT-data-{num_samples}.json"
    
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(subset, f, ensure_ascii=False, indent=2)
    
    print(f"子集已保存到：{output_file}")
    return output_file

if __name__ == "__main__":
    print("=" * 60)
    print("LawGPT-data 数据集下载工具")
    print("=" * 60)
    
    # 下载数据集
    full_file, data_list = download_dataset()
    
    if data_list:
        # 提取1000条子集
        subset_file = extract_subset(data_list, num_samples=1000)
        
        if subset_file:
            print("\n" + "=" * 60)
            print("下载完成！")
            print(f"完整数据集：{full_file}")
            print(f"1000条子集：{subset_file}")
            print("=" * 60)
    else:
        print("\n下载失败，请检查网络连接或数据集地址")
