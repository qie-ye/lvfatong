# -*- coding: utf-8 -*-
"""
从ModelScope下载中文法律数据集
ModelScope是国内可访问的模型社区
"""

import subprocess
import sys
import json
from pathlib import Path


def install_modelscope():
    """安装ModelScope"""
    print("Installing ModelScope...")
    subprocess.check_call([
        sys.executable, "-m", "pip", "install", "modelscope", 
        "-i", "https://pypi.tuna.tsinghua.edu.cn/simple"
    ])
    print("ModelScope installed successfully")


def download_dataset(dataset_name="xusenlin/LawGPT-data", cache_dir="./data"):
    """下载数据集"""
    try:
        from modelscope import snapshot_download
        
        print(f"Downloading dataset: {dataset_name}")
        print(f"Cache directory: {cache_dir}")
        
        # 下载数据集
        snapshot_download(dataset_name, cache_dir=cache_dir)
        
        print(f"Dataset downloaded successfully to {cache_dir}")
        return True
        
    except ImportError:
        print("ModelScope not installed. Installing...")
        install_modelscope()
        return download_dataset(dataset_name, cache_dir)
    except Exception as e:
        print(f"Download failed: {e}")
        return False


def find_dataset_files(cache_dir="./data"):
    """查找下载的数据集文件"""
    cache_path = Path(cache_dir)
    
    # 查找JSON文件
    json_files = list(cache_path.rglob("*.json"))
    
    print(f"\nFound {len(json_files)} JSON files:")
    for f in json_files:
        print(f"  - {f}")
    
    return json_files


def convert_to_alpaca(input_file, output_file):
    """转换数据为Alpaca格式"""
    try:
        with open(input_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # 检查数据格式
        if isinstance(data, list) and len(data) > 0:
            sample = data[0]
            
            # 如果已经是Alpaca格式
            if 'instruction' in sample and 'output' in sample:
                print(f"Data is already in Alpaca format")
                return data
            
            # 如果是对话格式
            if 'conversations' in sample:
                alpaca_data = []
                for item in data:
                    conversations = item.get('conversations', [])
                    if len(conversations) >= 2:
                        instruction = "你是'律法通'法律咨询助手，请回答用户问题。"
                        input_text = conversations[0].get('value', '')
                        output_text = conversations[1].get('value', '')
                        
                        alpaca_data.append({
                            "instruction": instruction,
                            "input": input_text,
                            "output": output_text
                        })
                
                print(f"Converted {len(alpaca_data)} samples to Alpaca format")
                return alpaca_data
        
        print("Unable to determine data format")
        return None
        
    except Exception as e:
        print(f"Conversion failed: {e}")
        return None


def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='Download dataset from ModelScope')
    parser.add_argument('--dataset', type=str, default='xusenlin/LawGPT-data',
                       help='Dataset name on ModelScope')
    parser.add_argument('--cache-dir', type=str, default='./data',
                       help='Cache directory')
    parser.add_argument('--convert', action='store_true',
                       help='Convert downloaded data to Alpaca format')
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("ModelScope Dataset Downloader")
    print("=" * 60)
    
    # 下载数据集
    success = download_dataset(args.dataset, args.cache_dir)
    
    if success:
        # 查找下载的文件
        json_files = find_dataset_files(args.cache_dir)
        
        if args.convert and json_files:
            print("\nConverting data to Alpaca format...")
            for input_file in json_files:
                output_file = input_file.parent / f"{input_file.stem}_alpaca.json"
                alpaca_data = convert_to_alpaca(input_file, output_file)
                
                if alpaca_data:
                    with open(output_file, 'w', encoding='utf-8') as f:
                        json.dump(alpaca_data, f, ensure_ascii=False, indent=2)
                    print(f"Saved to {output_file}")
        
        print("\n" + "=" * 60)
        print("Download Complete!")
        print("=" * 60)
    else:
        print("\nDownload failed. Please check your network connection.")


if __name__ == "__main__":
    main()
