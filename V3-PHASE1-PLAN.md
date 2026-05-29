# V3 Phase 1: 基础设施搭建 阶段计划书

> 开始时间：待定
> 预计工期：1周
> 状态：待执行

---

## 一、阶段目标

搭建 V3 所需的基础设施，包括：
1. GPU 推理服务框架
2. RabbitMQ 消息队列集成
3. Caffeine 本地缓存
4. 服务间通信封装

---

## 二、任务清单

### 2.1 GPU 服务器环境搭建（1天）

**任务**：
- [ ] 编写 GPU 推理服务 Dockerfile
- [ ] 编写 Python 推理服务代码（Reranker + Embedding）
- [ ] 配置 CUDA 环境
- [ ] 测试模型加载

**产出文件**：
```
新增：
└── docker/gpu-inference/
    ├── Dockerfile                    # GPU 推理服务镜像
    ├── requirements.txt              # Python 依赖
    ├── app/
    │   ├── main.py                   # FastAPI 服务入口
    │   ├── reranker.py               # Reranker 服务
    │   ├── embedding.py              # Embedding 服务
    │   └── models.py                 # 数据模型
    └── config.yaml                   # 配置文件
```

### 2.2 RabbitMQ 集成配置（1天）

**任务**：
- [ ] 添加 RabbitMQ 依赖到 pom.xml
- [ ] 编写 RabbitMQ 配置类
- [ ] 编写消息生产者
- [ ] 编写消息消费者
- [ ] 配置 docker-compose 添加 RabbitMQ 服务

**产出文件**：
```
新增：
├── src/main/java/com/lvatong/lft/mq/
│   ├── RabbitMQConfig.java           # RabbitMQ 配置
│   ├── ContractAnalysisProducer.java # 合同分析生产者
│   └── ContractAnalysisConsumer.java # 合同分析消费者
└── src/main/java/com/lvatong/lft/mq/dto/
    └── AnalysisTaskMessage.java      # 分析任务消息体

修改：
├── pom.xml                           # 添加 spring-boot-starter-amqp
├── application.yml                   # 添加 RabbitMQ 配置
└── docker-compose.yml                # 添加 RabbitMQ 服务
```

### 2.3 GPU 推理服务客户端（1天）

**任务**：
- [ ] 编写 GPU 服务配置类
- [ ] 编写 GPU 服务客户端（HTTP 调用）
- [ ] 封装 Reranker 调用接口
- [ ] 封装 Embedding 调用接口
- [ ] 编写降级策略（GPU 不可用时）

**产出文件**：
```
新增：
├── src/main/java/com/lvatong/lft/gpu/
│   ├── GpuServiceConfig.java         # GPU 服务配置
│   ├── GpuServiceClient.java         # GPU 服务客户端
│   ├── RerankRequest.java            # Rerank 请求
│   ├── RerankResponse.java           # Rerank 响应
│   ├── EmbedRequest.java             # Embed 请求
│   └── EmbedResponse.java            # Embed 响应

修改：
└── application.yml                   # 添加 GPU 服务配置
```

### 2.4 Caffeine 本地缓存集成（1天）

**任务**：
- [ ] 添加 Caffeine 依赖到 pom.xml
- [ ] 编写 Caffeine 配置类
- [ ] 编写统一缓存服务
- [ ] 配置缓存策略

**产出文件**：
```
新增：
├── src/main/java/com/lvatong/lft/cache/
│   ├── L1CacheConfig.java            # Caffeine 配置
│   ├── CacheService.java             # 统一缓存服务
│   └── CacheKey.java                 # 缓存 Key 定义

修改：
├── pom.xml                           # 添加 caffeine 依赖
└── application.yml                   # 添加缓存配置
```

### 2.5 服务间通信封装（0.5天）

**任务**：
- [ ] 编写服务发现配置
- [ ] 编写负载均衡配置
- [ ] 编写超时重试配置

**产出文件**：
```
修改：
├── src/main/java/com/lvatong/lft/config/
│   └── RestTemplateConfig.java       # RestTemplate 配置
└── application.yml                   # 添加服务配置
```

### 2.6 集成测试（1.5天）

**任务**：
- [ ] 测试 RabbitMQ 消息发送接收
- [ ] 测试 GPU 服务调用
- [ ] 测试缓存命中率
- [ ] 测试降级策略
- [ ] 编写测试文档

**产出文件**：
```
新增：
├── src/test/java/com/lvatong/lft/mq/
│   └── RabbitMQTest.java
├── src/test/java/com/lvatong/lft/gpu/
│   └── GpuServiceTest.java
└── src/test/java/com/lvatong/lft/cache/
    └── CacheServiceTest.java
```

---

## 三、技术细节

### 3.1 RabbitMQ 配置

```yaml
# application.yml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: /
    listener:
      simple:
        concurrency: 1
        max-concurrency: 5
```

### 3.2 GPU 服务配置

```yaml
# application.yml
lvatong:
  gpu:
    enabled: ${GPU_ENABLED:true}
    base-url: ${GPU_SERVICE_URL:http://localhost:8081}
    timeout: 5000
    retry:
      max-attempts: 3
      backoff: 1000
```

### 3.3 Caffeine 缓存配置

```yaml
# application.yml
lvatong:
  cache:
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m
    rag-results:
      ttl: 5m
      max-size: 500
    intent-results:
      ttl: 10m
      max-size: 1000
```

---

## 四、Docker Compose 配置

### 4.1 新增服务

```yaml
# docker-compose.yml 新增
services:
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:guest}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:guest}
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq

  gpu-inference:
    build: ./docker/gpu-inference
    ports:
      - "8081:8081"
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
    volumes:
      - ./models:/models

volumes:
  rabbitmq_data:
```

---

## 五、验收标准

### 5.1 功能验收

- [ ] RabbitMQ 可正常发送接收消息
- [ ] GPU 服务可正常调用（Reranker + Embedding）
- [ ] Caffeine 缓存命中率 > 30%
- [ ] 降级策略正常工作

### 5.2 性能验收

- [ ] RabbitMQ 消息延迟 < 10ms
- [ ] GPU Rerank 延迟 < 200ms
- [ ] GPU Embedding 延迟 < 100ms
- [ ] 缓存读取延迟 < 1ms

### 5.3 稳定性验收

- [ ] GPU 服务不可用时，系统可降级运行
- [ ] RabbitMQ 连接断开时，可自动重连
- [ ] 缓存雪崩/穿透防护正常

---

## 六、风险与应对

| 风险 | 概率 | 应对措施 |
|------|------|----------|
| GPU 环境搭建困难 | 中 | 提前准备 Docker 镜像 |
| RabbitMQ 配置问题 | 低 | 参考官方文档 |
| 模型加载失败 | 低 | 预下载模型文件 |

---

## 七、依赖项

### 7.1 Maven 依赖

```xml
<!-- RabbitMQ -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- Caffeine -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### 7.2 Python 依赖

```txt
# requirements.txt
fastapi==0.104.1
uvicorn==0.24.0
transformers==4.36.0
torch==2.1.1
sentence-transformers==2.2.2
```

---

**文档状态**：✅ 已确认，待执行
**下一步**：执行 Phase 1，完成后进入 Phase 2
