#!/bin/bash
# 律法通 V3 环境检查脚本
# 检查所有依赖服务是否就绪

set -e

echo "=========================================="
echo "律法通 V3 环境检查"
echo "=========================================="

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

check_pass() {
    echo -e "  ${GREEN}✓${NC} $1"
}

check_fail() {
    echo -e "  ${RED}✗${NC} $1"
}

check_warn() {
    echo -e "  ${YELLOW}!${NC} $1"
}

# 检查 Java
echo ""
echo "[1/7] 检查 Java 环境..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    check_pass "Java 已安装: $JAVA_VERSION"
else
    check_fail "Java 未安装 (需要 JDK 17+)"
fi

# 检查 Maven
echo ""
echo "[2/7] 检查 Maven..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version 2>&1 | head -n 1)
    check_pass "Maven 已安装: $MVN_VERSION"
else
    check_fail "Maven 未安装"
fi

# 检查 Node.js
echo ""
echo "[3/7] 检查 Node.js..."
if command -v node &> /dev/null; then
    NODE_VERSION=$(node -v)
    check_pass "Node.js 已安装: $NODE_VERSION"
else
    check_fail "Node.js 未安装 (需要 20+)"
fi

# 检查 Docker
echo ""
echo "[4/7] 检查 Docker..."
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker -v)
    check_pass "Docker 已安装: $DOCKER_VERSION"
else
    check_warn "Docker 未安装 (可选，用于容器化部署)"
fi

# 检查 MySQL
echo ""
echo "[5/7] 检查 MySQL 连接..."
if command -v mysql &> /dev/null; then
    if mysql -h"${MYSQL_HOST:-127.0.0.1}" -P"${MYSQL_PORT:-3306}" -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD}" -e "SELECT 1" &> /dev/null; then
        check_pass "MySQL 连接成功"
    else
        check_fail "MySQL 连接失败 (请检查配置)"
    fi
else
    check_warn "MySQL 客户端未安装"
fi

# 检查 Redis
echo ""
echo "[6/7] 检查 Redis 连接..."
if command -v redis-cli &> /dev/null; then
    if redis-cli -h "${REDIS_HOST:-127.0.0.1}" -p "${REDIS_PORT:-6379}" ping &> /dev/null; then
        check_pass "Redis 连接成功"
    else
        check_fail "Redis 连接失败 (请检查配置)"
    fi
else
    check_warn "Redis 客户端未安装"
fi

# 检查 GPU (可选)
echo ""
echo "[7/7] 检查 GPU (可选)..."
if command -v nvidia-smi &> /dev/null; then
    GPU_INFO=$(nvidia-smi --query-gpu=name,memory.total --format=csv,noheader 2>/dev/null | head -n 1)
    if [ -n "$GPU_INFO" ]; then
        check_pass "GPU 已检测到: $GPU_INFO"
    else
        check_warn "nvidia-smi 可用但未检测到 GPU"
    fi
else
    check_warn "nvidia-smi 未安装 (GPU 推理服务将不可用)"
fi

# 检查环境变量文件
echo ""
echo "[额外] 检查配置文件..."
if [ -f ".env" ]; then
    check_pass ".env 文件存在"
else
    check_warn ".env 文件不存在 (请从 env.example 复制)"
fi

# 检查模型文件
echo ""
echo "[额外] 检查模型文件..."
if [ -d "models/bge-reranker-v2-m3" ]; then
    check_pass "Reranker 模型已下载"
else
    check_warn "Reranker 模型未下载 (运行 scripts/download-models.sh)"
fi

if [ -d "models/bge-m3" ]; then
    check_pass "Embedding 模型已下载"
else
    check_warn "Embedding 模型未下载 (运行 scripts/download-models.sh)"
fi

echo ""
echo "=========================================="
echo "检查完成"
echo "=========================================="
echo ""
echo "启动命令："
echo "  1. 启动基础服务: docker compose up -d mysql redis"
echo "  2. 启动后端:     mvn spring-boot:run"
echo "  3. 启动前端:     cd frontend && npm run dev"
echo ""
