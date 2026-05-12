# LawGPT-data 数据集下载指南

## 数据集信息

- **名称**: LawGPT-data
- **地址**: https://huggingface.co/datasets/xusenlin/LawGPT-data
- **格式**: Alpaca格式 (instruction, input, output)
- **大小**: 约52K条中文法律问答数据

## 下载方式

### 方式1: 使用huggingface-cli (推荐)

```bash
# 安装huggingface-hub
pip install huggingface-hub -i https://pypi.tuna.tsinghua.edu.cn/simple

# 下载数据集到本地
huggingface-cli download --repo-type dataset xusenlin/LawGPT-data --local-dir ./data
```

### 方式2: 使用Python脚本

```python
from datasets import load_dataset
import json

# 下载数据集
dataset = load_dataset("xusenlin/LawGPT-data", split="train")

# 转换为列表并保存
data_list = [item for item in dataset]

with open("data/LawGPT-data-full.json", "w", encoding="utf-8") as f:
    json.dump(data_list, f, ensure_ascii=False, indent=2)

print(f"下载完成！共 {len(data_list)} 条数据")
```

### 方式3: 手动下载

1. 访问 https://huggingface.co/datasets/xusenlin/LawGPT-data
2. 点击 "Files and versions" 标签
3. 下载 `data` 文件夹中的JSON文件
4. 将文件保存到 `finetune/data/` 目录

## 下载后操作

下载完成后，将数据集文件放到以下位置：

```
lvatong/finetune/data/LawGPT-data-full.json
```

然后运行以下命令分割数据集：

```bash
cd finetune
python prepare_batches.py --batch-size 5000
```

## 配置代理 (如果网络有问题)

```bash
# 设置代理 (Windows PowerShell)
$env:HTTP_PROXY="http://127.0.0.1:7890"
$env:HTTPS_PROXY="http://127.0.0.1:7890"

# 或者使用国内镜像
pip install datasets -i https://pypi.tuna.tsinghua.edu.cn/simple
```

## 数据格式示例

```json
{
  "instruction": "你是\"律法通\"法律咨询助手，专注于中国法律领域。",
  "input": "租房合同没到期，房东突然要我搬走，我该怎么办？",
  "output": "【事实分析】\n- 租赁合同尚在有效期内\n..."
}
```

## 联系

如果下载遇到问题，请检查：
1. 网络连接是否正常
2. 是否需要配置代理
3. HuggingFace是否可以访问
