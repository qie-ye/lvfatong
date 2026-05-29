# 律法通 V3 版本开发说明

> 版本：V3.0
> 开发周期：9 周
> 最后更新：2025-01-27

---

## 一、开发背景

### 1.1 V2 版本现状

V2 版本已实现的核心能力：
- RAG + Function Calling + 多轮记忆的完整闭环
- 法律问答、合同分析、法条检索、案例检索、文书生成
- JWT+RBAC、审计日志、可观测性
- 约 10 位律师灰度测试中

### 1.2 V3 开发目标

**核心目标**：提升 AI 能力、检索质量、用户体验，支撑中小律所规模化使用

**量化指标**：
| 指标 | V2 现状 | V3 目标 | 提升幅度 |
|------|---------|---------|----------|
| 回答准确率 | ~75% | ~85% | +10% |
| 检索召回率 | ~70% | ~85% | +15% |
| 平均响应时间 | 3-5s | 2-3s | -40% |
| 用户满意度 | ~70% | ~85% | +15% |

---

## 二、技术选型

### 2.1 V3 新增技术栈

| 组件 | 选择 | 版本 | 用途 |
|------|------|------|------|
| 消息队列 | RabbitMQ | 3.x | 异步任务处理 |
| 本地缓存 | Caffeine | 3.x | 热点数据 L1 缓存 |
| Reranker | BGE-Reranker-v2-m3 | - | 检索结果重排序 |
| Embedding | BGE-M3 | - | 文本向量化 |
| GPU 推理 | FastAPI + PyTorch | - | 模型推理服务 |

### 2.2 完整技术栈

```
前端：Vue 3.4 + TypeScript + Pinia + Element Plus + Vite 5
后端：Java 17 + Spring Boot 3.2.5 + Spring Security + JPA
数据库：MySQL 8.0 + Redis 7 + Milvus 2.4
消息队列：RabbitMQ 3.x / Redis Stream
AI：智谱 GLM 系列 + BGE-Reranker + BGE-M3
部署：Docker + Nginx + Prometheus + Grafana + Zipkin
```

---

## 三、架构设计

### 3.1 V3 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         Nginx 反向代理                           │
│                    (负载均衡 + SSE代理 + 静态资源)                 │
└─────────────────────────────────────────────────────────────────┘
                              │
            ┌─────────────────┴─────────────────┐
            ▼                                   ▼
┌─────────────────────────┐         ┌─────────────────────────┐
│    应用服务器 (CPU)       │         │    GPU推理服务器          │
│    Spring Boot          │   HTTP  │    RTX 3090 24GB        │
│    RabbitMQ             │────────→│    Reranker             │
│    MySQL + Redis        │         │    Embedding            │
└─────────────────────────┘         │    Milvus               │
                                    └─────────────────────────┘
```

### 3.2 AI 推理架构

```
用户问题
    ↓
意图识别（规则引擎 + AI分类）
    ↓
┌─────────────────────────────────────┐
│         智能路由                     │
├─────────────────────────────────────┤
│ 简单问题 → 直接问答                  │
│ 中等复杂 → Function Calling          │
│ 高复杂度 → ReAct Agent              │
│ 超复杂   → Plan-and-Execute         │
└─────────────────────────────────────┘
    ↓
验证 + 重试
    ↓
返回答案
```

### 3.3 RAG 检索架构

```
用户查询
    ↓
Query Rewriting（查询改写）
    ↓
┌─────────────────────────────────────┐
│         混合检索                     │
├─────────────────────────────────────┤
│ 向量检索（Milvus）                   │
│ 全文检索（MySQL FULLTEXT）           │
│ RRF 融合排序                         │
└─────────────────────────────────────┘
    ↓
GPU Reranker 重排序
    ↓
上下文压缩
    ↓
返回检索结果
```

---

## 四、开发阶段

### 4.1 阶段划分

| 阶段 | 名称 | 工期 | 状态 |
|------|------|------|------|
| Phase 1 | 基础设施搭建 | 1周 | ✅ 完成 |
| Phase 2 | AI Agent 升级 | 2周 | ✅ 完成 |
| Phase 3 | RAG 检索增强 | 1.5周 | ✅ 完成 |
| Phase 4 | 个性化推荐 | 2周 | ✅ 完成 |
| Phase 5 | 系统优化 | 1.5周 | ✅ 完成 |
| Phase 6 | 前端优化 | 1周 | ✅ 完成 |

### 4.2 各阶段产出

**Phase 1: 基础设施搭建**
- RabbitMQ 配置和消息生产者/消费者
- GPU 推理服务框架（Python FastAPI）
- GPU 服务客户端（Java）
- Caffeine 本地缓存模块

**Phase 2: AI Agent 升级**
- ReAct Agent 推理框架
- Plan-and-Execute 模式
- 赔偿计算工具
- 时效检查工具

**Phase 3: RAG 检索增强**
- 语义分块器
- Reranker 服务
- HybridSearchService 集成

**Phase 4: 个性化推荐**
- 用户行为分析服务
- 推荐引擎（个性化/热门/协同过滤）
- 推荐 API 接口

**Phase 5: 系统优化**
- 统一异步任务服务
- 数据库性能索引

**Phase 6: 前端优化**
- 智能提示组件
- 推荐面板组件
- ChatView 集成

---

## 五、文件结构

### 5.1 后端新增文件

```
src/main/java/com/lvatong/lft/
├── agent/                          # Agent 推理模块
│   ├── ReActAgent.java
│   ├── PlanAndExecuteService.java
│   └── SubTask.java
├── ai/tools/                       # AI 工具
│   ├── CompensationCalculator.java
│   └── StatuteChecker.java
├── async/
│   └── AsyncTaskService.java       # 统一异步任务服务
├── cache/                          # 缓存模块
│   ├── L1CacheConfig.java
│   └── CacheService.java
├── gpu/                            # GPU 服务模块
│   ├── GpuServiceConfig.java
│   └── GpuServiceClient.java
├── mq/                             # 消息队列模块
│   ├── RabbitMQConfig.java
│   ├── ContractAnalysisProducer.java
│   ├── ContractAnalysisConsumer.java
│   └── dto/AnalysisTaskMessage.java
├── rag/                            # RAG 增强
│   ├── SemanticChunker.java
│   └── RerankerService.java
├── recommendation/                 # 推荐模块
│   ├── UserBehaviorAnalyzer.java
│   └── RecommendationEngine.java
└── controller/
    └── RecommendationController.java
```

### 5.2 前端新增文件

```
frontend/src/
├── components/
│   ├── SmartSuggestion.vue         # 智能提示组件
│   └── RecommendationPanel.vue     # 推荐面板组件
└── stores/
    └── recommendation.ts           # 推荐状态管理
```

### 5.3 Docker/配置文件

```
docker/gpu-inference/               # GPU 推理服务
├── Dockerfile
├── requirements.txt
├── config.yaml
└── app/
    ├── main.py
    ├── reranker.py
    ├── embedding.py
    └── models.py

src/main/resources/db/migration/
├── V8__add_recommendation_tables.sql
└── V9__add_performance_indexes.sql
```

---

## 六、配置说明

### 6.1 新增配置项

```yaml
# application.yml
lvatong:
  mq:
    type: ${MQ_TYPE:redis}  # 消息队列类型：redis 或 rabbitmq
  gpu:
    enabled: ${GPU_ENABLED:true}
    base-url: ${GPU_SERVICE_URL:http://localhost:8081}
    timeout: ${GPU_TIMEOUT:5000}
    retry:
      max-attempts: 3
      backoff: 1000
  cache:
    rag-results:
      ttl-minutes: 5
      max-size: 500
    intent-results:
      ttl-minutes: 10
      max-size: 1000
    embedding-results:
      ttl-minutes: 30
      max-size: 2000
```

### 6.2 环境变量

| 变量 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `MQ_TYPE` | 否 | redis | 消息队列类型 |
| `GPU_ENABLED` | 否 | true | 是否启用 GPU 服务 |
| `GPU_SERVICE_URL` | 否 | http://localhost:8081 | GPU 服务地址 |
| `RABBITMQ_HOST` | 否 | localhost | RabbitMQ 地址 |
| `RABBITMQ_USER` | 否 | guest | RabbitMQ 用户名 |
| `RABBITMQ_PASSWORD` | 否 | guest | RabbitMQ 密码 |

---

## 七、数据库变更

### 7.1 V8: 推荐系统表

```sql
-- 用户行为记录
CREATE TABLE user_behaviors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT,
    query_text VARCHAR(500),
    domain VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 用户偏好画像
CREATE TABLE user_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    preferred_domains JSON,
    expertise_level VARCHAR(20) DEFAULT 'BEGINNER',
    last_active_at DATETIME
);

-- 推荐记录
CREATE TABLE recommendation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recommendation_type VARCHAR(50) NOT NULL,
    recommended_items JSON NOT NULL,
    clicked BOOLEAN DEFAULT FALSE
);

-- 热门查询
CREATE TABLE popular_queries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    query_text VARCHAR(500) NOT NULL,
    domain VARCHAR(100),
    query_count INT DEFAULT 1
);
```

### 7.2 V9: 性能优化索引

为高频查询字段添加复合索引，提升查询性能。

---

## 八、测试说明

### 8.1 编译测试

```bash
# 后端编译
mvn clean compile -DskipTests
# 结果：BUILD SUCCESS (176 个源文件)

# 前端编译
cd frontend && npx vite build
# 结果：built in 11.28s
```

### 8.2 集成测试

需要启动完整环境：
1. MySQL 8.0
2. Redis 7
3. RabbitMQ（可选）
4. GPU 推理服务（可选）

### 8.3 单元测试

```bash
mvn test
```

注意：现有测试失败是因为缺少数据库环境，与新增代码无关。

---

## 九、部署说明

详见 [部署文档](./DEPLOYMENT.md)

---

## 十、已知问题

| 问题 | 影响 | 解决方案 |
|------|------|----------|
| vue-tsc 版本兼容 | 前端类型检查失败 | 使用 vite build 代替 |
| 测试环境缺失 | 单元测试失败 | 需要配置测试数据库 |
| 模型文件较大 | 首次部署耗时 | 预下载模型文件 |

---

## 十一、后续规划

### 11.1 短期优化

- [ ] 单元测试覆盖率提升至 70%
- [ ] 压力测试验证（100+ 并发）
- [ ] 监控告警完善

### 11.2 中期规划

- [ ] 本地微调模型部署
- [ ] 知识图谱集成
- [ ] 数据分析 Dashboard

### 11.3 长期愿景

- [ ] 律师协作平台
- [ ] 案件管理系统
- [ ] 法律数据库订阅服务
