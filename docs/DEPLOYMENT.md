# 律法通 V3 部署指南

## 一、环境要求

### 1.1 必需环境

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 推荐 OpenJDK 17 |
| Maven | 3.9+ | 构建工具 |
| Node.js | 20+ | 前端构建 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 7+ | 缓存和会话 |

### 1.2 可选环境

| 组件 | 版本 | 说明 |
|------|------|------|
| Docker | 24+ | 容器化部署 |
| NVIDIA GPU | RTX 3090+ | Reranker 推理 |
| RabbitMQ | 3.x | 企业级消息队列 |

---

## 二、快速开始（本地开发）

### 2.1 克隆项目

```bash
git clone https://github.com/your-org/lvatong.git
cd lvatong
```

### 2.2 配置环境变量

```bash
# 复制环境变量模板
cp env.example .env

# 编辑 .env 文件，填写实际配置
# 必填项：
#   - MYSQL_PASSWORD
#   - ZHIPU_API_KEY
#   - JWT_SECRET
```

### 2.3 启动基础服务

```bash
# 使用 Docker 启动依赖服务
docker compose up -d mysql redis

# 或手动安装 MySQL 和 Redis
```

### 2.4 启动后端

```bash
# 方式一：直接运行
mvn spring-boot:run

# 方式二：打包后运行
mvn clean package -DskipTests
java -jar target/*.jar
```

### 2.5 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 2.6 访问应用

- 前端：http://localhost:5173
- 后端 API：http://localhost:8080
- Swagger UI：http://localhost:8080/swagger-ui.html

---

## 三、生产部署

### 3.1 使用 Docker Compose

```bash
# 1. 准备环境变量
cp env.example .env
# 编辑 .env 填写生产配置

# 2. 构建前端
cd frontend
npm ci
npm run build
cd ..

# 3. 启动所有服务
docker compose -f docker-compose.prod.yml up -d --build
```

### 3.2 服务列表

| 服务 | 端口 | 说明 |
|------|------|------|
| Nginx | 80 | 反向代理 |
| Backend | 8080 | Spring Boot 应用 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| RabbitMQ | 5672, 15672 | 消息队列 |
| GPU Inference | 8081 | AI 推理服务 |
| Prometheus | 9090 | 监控指标 |
| Grafana | 3001 | 监控面板 |
| Zipkin | 9411 | 链路追踪 |

---

## 四、GPU 推理服务部署

### 4.1 准备工作

```bash
# 1. 确保 NVIDIA 驱动和 CUDA 已安装
nvidia-smi

# 2. 安装 Docker NVIDIA 运行时
# 参考：https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html
```

### 4.2 下载模型

```bash
# 运行模型下载脚本
bash scripts/download-models.sh

# 或手动下载
huggingface-cli download BAAI/bge-reranker-v2-m3 --local-dir ./models/bge-reranker-v2-m3
huggingface-cli download BAAI/bge-m3 --local-dir ./models/bge-m3
```

### 4.3 启动 GPU 服务

```bash
# 方式一：Docker
cd docker/gpu-inference
docker build -t lvatong-gpu-inference .
docker run -d --gpus all \
    -v ../../models:/models \
    -p 8081:8081 \
    --restart unless-stopped \
    lvatong-gpu-inference

# 方式二：直接运行（需要 Python 环境）
cd docker/gpu-inference
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 8081
```

### 4.4 验证 GPU 服务

```bash
# 健康检查
curl http://localhost:8081/health

# 测试 Rerank
curl -X POST http://localhost:8081/rerank \
    -H "Content-Type: application/json" \
    -d '{"query": "劳动法", "documents": ["劳动合同", "婚姻法"], "top_k": 2}'
```

---

## 五、配置说明

### 5.1 核心配置

```yaml
# application.yml
lvatong:
  mq:
    type: redis  # 消息队列类型：redis 或 rabbitmq
  gpu:
    enabled: true
    base-url: http://localhost:8081
  cache:
    rag-results:
      ttl-minutes: 5
      max-size: 500
```

### 5.2 环境变量说明

| 变量 | 必填 | 说明 |
|------|------|------|
| `MYSQL_HOST` | 是 | MySQL 地址 |
| `MYSQL_PASSWORD` | 是 | MySQL 密码 |
| `ZHIPU_API_KEY` | 是 | 智谱 AI API Key |
| `JWT_SECRET` | 是 | JWT 密钥（32位+） |
| `MQ_TYPE` | 否 | 消息队列类型，默认 redis |
| `GPU_ENABLED` | 否 | 是否启用 GPU 服务，默认 false |

---

## 六、运维监控

### 6.1 健康检查

```bash
# 应用健康
curl http://localhost:8080/actuator/health

# GPU 服务健康
curl http://localhost:8081/health
```

### 6.2 监控面板

- Prometheus：http://localhost:9090
- Grafana：http://localhost:3001
- Zipkin：http://localhost:9411

### 6.3 日志查看

```bash
# Docker 日志
docker logs -f lvatong-backend
docker logs -f lvatong-gpu-inference

# 应用日志
tail -f logs/application.log
```

---

## 七、常见问题

### 7.1 GPU 服务不可用

**问题**：系统提示 GPU 服务连接失败

**解决**：
1. 检查 GPU 服务是否启动：`curl http://localhost:8081/health`
2. 检查 CUDA 是否正确安装：`nvidia-smi`
3. 临时禁用 GPU 服务：设置 `GPU_ENABLED=false`

### 7.2 RabbitMQ 连接失败

**问题**：无法连接到 RabbitMQ

**解决**：
1. 检查 RabbitMQ 是否启动：`docker ps | grep rabbitmq`
2. 切换到 Redis 模式：设置 `MQ_TYPE=redis`

### 7.3 模型下载失败

**问题**：HuggingFace 下载超时

**解决**：
1. 使用镜像源：`export HF_ENDPOINT=https://hf-mirror.com`
2. 手动下载后放到 `models/` 目录

---

## 八、回滚方案

如果 V3 部署出现问题，可以快速回滚到 V2：

```bash
# 1. 切换到 V2 分支
git checkout v2

# 2. 重新部署
docker compose -f docker-compose.prod.yml up -d --build
```

---

**文档版本**：V3.0
**最后更新**：2025-01-27
