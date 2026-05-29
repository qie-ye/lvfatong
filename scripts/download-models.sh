#!/bin/bash
# 律法通 V3 模型下载脚本
# 用于下载 Reranker 和 Embedding 模型

set -e

MODEL_DIR="./models"

echo "=========================================="
echo "律法通 V3 模型下载"
echo "=========================================="

# 创建模型目录
mkdir -p $MODEL_DIR

# 检查 huggingface-cli 是否安装
if ! command -v huggingface-cli &> /dev/null; then
    echo "huggingface-cli 未安装，正在安装..."
    pip install -U huggingface_hub
fi

# 下载 Reranker 模型
echo ""
echo "[1/2] 下载 BGE-Reranker 模型..."
if [ -d "$MODEL_DIR/bge-reranker-v2-m3" ]; then
    echo "  -> 模型已存在，跳过下载"
else
    huggingface-cli download BAAI/bge-reranker-v2-m3 \
        --local-dir $MODEL_DIR/bge-reranker-v2-m3 \
        --local-dir-use-symlinks False
    echo "  -> 下载完成"
fi

# 下载 Embedding 模型
echo ""
echo "[2/2] 下载 BGE-M3 Embedding 模型..."
if [ -d "$MODEL_DIR/bge-m3" ]; then
    echo "  -> 模型已存在，跳过下载"
else
    huggingface-cli download BAAI/bge-m3 \
        --local-dir $MODEL_DIR/bge-m3 \
        --local-dir-use-symlinks False
    echo "  -> 下载完成"
fi

echo ""
echo "=========================================="
echo "模型下载完成！"
echo "=========================================="
echo ""
echo "模型位置："
echo "  Reranker:   $MODEL_DIR/bge-reranker-v2-m3"
echo "  Embedding:  $MODEL_DIR/bge-m3"
echo ""
echo "GPU 推理服务启动命令："
echo "  cd docker/gpu-inference"
echo "  docker build -t lvatong-gpu-inference ."
echo "  docker run -d --gpus all -v ../../models:/models -p 8081:8081 lvatong-gpu-inference"
echo ""
