# 真实中文法律数据集下载指南

## 推荐数据集（优先级排序）

### 1. CAIL2018（推荐）
- **内容**：中国法律AI挑战赛-刑事案子罪名预测、法条推荐、刑期预测
- **数据量**：约15万条刑事案件
- **格式**：JSON
- **下载地址**：https://github.com/thunlp/CAIL
- **备选地址**：https://pan.baidu.com/s/1nwfV3VsHJYVfCkRJcCgK1Q (提取码: cail)
- **特点**：真实裁判文书，质量高

### 2. JEC-QA（推荐）
- **内容**：司法考试问答数据集
- **数据量**：约3万道选择题
- **格式**：JSON
- **下载地址**：https://github.com/SooMe/JEC-QA
- **特点**：专业法律问题，适合问答训练

### 3. LawGPT训练数据
- **内容**：中文法律问答
- **数据量**：约52K条
- **格式**：Alpaca格式
- **下载地址**：https://huggingface.co/datasets/xusenlin/LawGPT-data (需要VPN)
- **备选**：https://modelscope.cn/datasets/xusenlin/LawGPT-data

### 4. 裁判文书网数据
- **内容**：中国裁判文书网公开文书
- **数据量**：数百万条
- **格式**：需要自行爬取
- **网址**：https://wenshu.court.gov.cn/
- **特点**：最真实的法律数据

## 手动下载步骤

### 方法1：百度网盘下载CAIL2018

```bash
# 1. 下载百度网盘文件
# 链接：https://pan.baidu.com/s/1nwfV3VsHJYVfCkRJcCgK1Q
# 提取码：cail

# 2. 解压后将文件放到
lvatong/finetune/data/

# 3. 文件结构应该是
lvatong/finetune/data/
├── criminal_train.json  # 刑事案件训练集
├── criminal_test.json   # 刑事案件测试集
└── ...
```

### 方法2：从GitHub下载JEC-QA

```bash
# 1. 访问 https://github.com/SooMe/JEC-QA
# 2. 点击 Code -> Download ZIP
# 3. 解压后将数据文件放到
lvatong/finetune/data/
```

### 方法3：使用ModelScope（国内可访问）

```bash
# 1. 安装ModelScope
pip install modelscope -i https://pypi.tuna.tsinghua.edu.cn/simple

# 2. 下载数据集
python -c "
from modelscope import snapshot_download
snapshot_download('xusenlin/LawGPT-data', cache_dir='./data')
"
```

## 数据格式转换

下载后需要转换为Alpaca训练格式：

```json
{
  "instruction": "你是'律法通'法律咨询助手，请回答用户问题。",
  "input": "用户的问题",
  "output": "AI的回答"
}
```

## 联系支持

如果下载遇到问题，可以：
1. 检查网络连接和代理设置
2. 尝试使用VPN访问HuggingFace
3. 使用国内镜像源（ModelScope、百度网盘）
