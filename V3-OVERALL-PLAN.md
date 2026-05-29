# 律法通 V3 总体开发计划书

> 生成时间：2025-01-27
> 版本：V3.0
> 状态：已确认，待执行

---

## 一、项目概述

### 1.1 项目背景

律法通（LvFaTong）是一个面向律师日常办案场景的AI智能平台，目前已完成V2版本，具备：
- RAG + Function Calling + 多轮记忆的完整闭环
- 法律问答、合同分析、法条检索、案例检索、文书生成等核心功能
- JWT+RBAC、审计日志、可观测性等企业级能力
- 约10位律师灰度测试中

### 1.2 V3目标

**核心目标**：提升AI能力、检索质量、用户体验，支撑中小律所规模化使用

**量化指标**：
| 指标 | V2现状 | V3目标 | 提升幅度 |
|------|--------|--------|----------|
| 回答准确率 | ~75% | ~85% | +10% |
| 检索召回率 | ~70% | ~85% | +15% |
| 平均响应时间 | 3-5s | 2-3s | -40% |
| 用户满意度 | ~70% | ~85% | +15% |

---

## 二、技术选型

### 2.1 基础设施

| 组件 | 选择 | 说明 |
|------|------|------|
| GPU | RTX 3090/4090 24GB | 本地部署，推理服务 |
| 消息队列 | RabbitMQ | 轻量级异步任务 |
| 本地缓存 | Caffeine | 热点数据L1缓存 |
| 分布式缓存 | Redis 7 | 会话/记忆L2缓存 |
| Reranker | BGE-Reranker-v2-m3 | 本地部署，568M参数 |
| Embedding | BGE-M3 | 复用现有 |

### 2.2 架构设计

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
│    4核8G / 8核16G        │         │    RTX 3090 24GB        │
│                         │         │                         │
│  ┌───────────────────┐  │         │  ┌───────────────────┐  │
│  │  Spring Boot 应用  │  │  HTTP   │  │  Reranker Service │  │
│  │  - 业务逻辑        │──┼─────────┼─→│  Embedding Service│  │
│  │  - Agent编排       │  │         │  │  模型推理          │  │
│  │  - Function Calling│  │         │  └───────────────────┘  │
│  └───────────────────┘  │         │                         │
│                         │         │  ┌───────────────────┐  │
│  ┌───────────────────┐  │         │  │  Milvus Standalone │  │
│  │  RabbitMQ          │  │         │  │  (向量数据库)       │  │
│  │  (消息队列)         │  │         │  └───────────────────┘  │
│  └───────────────────┘  │         │                         │
│                         │         │  ┌───────────────────┐  │
│  ┌───────────────────┐  │         │  │  模型文件存储       │  │
│  │  MySQL 8           │  │         │  │  /models/          │  │
│  │  (业务数据库)       │  │         │  └───────────────────┘  │
│  └───────────────────┘  │         └─────────────────────────┘
│                         │
│  ┌───────────────────┐  │
│  │  Redis 7           │  │
│  │  (缓存+会话)       │  │
│  └───────────────────┘  │
└─────────────────────────┘
```

---

## 三、开发阶段

### 3.1 阶段划分

| 阶段 | 名称 | 工期 | 优先级 | 依赖 |
|------|------|------|--------|------|
| Phase 1 | 基础设施搭建 | 1周 | ⭐⭐⭐ | 无 |
| Phase 2 | AI Agent升级 | 2周 | ⭐⭐⭐ | Phase 1 |
| Phase 3 | RAG检索增强 | 1.5周 | ⭐⭐⭐ | Phase 1 |
| Phase 4 | 个性化推荐 | 2周 | ⭐⭐ | Phase 2,3 |
| Phase 5 | 系统优化 | 1.5周 | ⭐⭐ | Phase 1 |
| Phase 6 | 前端优化 | 1周 | ⭐ | Phase 2,3,4 |

**总工期：9周**

### 3.2 各阶段概要

#### Phase 1: 基础设施搭建（1周）

**目标**：搭建GPU推理服务、消息队列、缓存等基础设施

**任务清单**：
- [ ] GPU服务器环境搭建（CUDA/Docker）
- [ ] RabbitMQ集成配置
- [ ] GPU推理服务框架（Reranker/Embedding）
- [ ] 服务间通信封装
- [ ] 集成测试

**产出文件**：
```
新增：
├── src/main/java/com/lvatong/lft/mq/
│   ├── RabbitMQConfig.java
│   ├── ContractAnalysisProducer.java
│   └── ContractAnalysisConsumer.java
├── src/main/java/com/lvatong/lft/gpu/
│   ├── GpuServiceClient.java
│   └── GpuServiceConfig.java
├── src/main/java/com/lvatong/lft/cache/
│   ├── L1CacheConfig.java
│   └── CacheService.java
└── docker/gpu-inference/
    ├── Dockerfile
    └── requirements.txt

修改：
├── pom.xml（新增依赖）
├── application.yml（新增配置）
└── docker-compose.yml（新增服务）
```

#### Phase 2: AI Agent升级（2周）

**目标**：实现ReAct推理框架、Plan-and-Execute模式、工具链扩展

**任务清单**：
- [ ] ReAct推理框架实现
- [ ] Plan-and-Execute模式实现
- [ ] 新增工具：赔偿计算
- [ ] 新增工具：时效检查
- [ ] 工具链集成测试
- [ ] ChatService集成

**产出文件**：
```
新增：
├── src/main/java/com/lvatong/lft/agent/
│   ├── ReActAgent.java
│   ├── PlanAndExecuteService.java
│   └── SubTask.java
└── src/main/java/com/lvatong/lft/ai/tools/
    ├── CompensationCalculator.java
    └── StatuteChecker.java

修改：
├── src/main/java/com/lvatong/lft/ai/ChatService.java
├── src/main/java/com/lvatong/lft/ai/ToolRegistry.java
└── src/main/java/com/lvatong/lft/ai/ToolExecutor.java
```

#### Phase 3: RAG检索增强（1.5周）

**目标**：实现语义分块、Reranker集成、多路召回优化

**任务清单**：
- [ ] 语义分块实现
- [ ] Reranker服务对接
- [ ] 多路召回策略优化
- [ ] 检索质量评估
- [ ] 性能调优

**产出文件**：
```
新增：
├── src/main/java/com/lvatong/lft/rag/
│   ├── SemanticChunker.java
│   └── RerankerService.java
└── src/test/java/com/lvatong/lft/rag/
    └── RetrievalQualityTest.java

修改：
├── src/main/java/com/lvatong/lft/rag/RAGService.java
├── src/main/java/com/lvatong/lft/rag/HybridSearchService.java
└── src/main/java/com/lvatong/lft/rag/DocumentChunker.java
```

#### Phase 4: 个性化推荐（2周）

**目标**：实现用户行为分析、推荐引擎、智能推荐

**任务清单**：
- [ ] 用户行为数据表设计
- [ ] 行为采集服务
- [ ] 推荐算法实现
- [ ] 推荐API接口
- [ ] 前端推荐组件
- [ ] 推荐效果验证

**产出文件**：
```
新增：
├── src/main/java/com/lvatong/lft/recommendation/
│   ├── UserBehaviorAnalyzer.java
│   ├── RecommendationEngine.java
│   └── UserPreference.java
├── src/main/java/com/lvatong/lft/controller/RecommendationController.java
├── src/main/java/com/lvatong/lft/service/RecommendationService.java
├── frontend/src/components/RecommendationPanel.vue
└── src/main/resources/db/migration/V8__add_recommendation.sql

修改：
├── src/main/java/com/lvatong/lft/service/LegalService.java
└── frontend/src/views/ChatView.vue
```

#### Phase 5: 系统优化（1.5周）

**目标**：异步任务改造、缓存策略优化、数据库优化

**任务清单**：
- [ ] 异步任务改造（RabbitMQ）
- [ ] Caffeine本地缓存集成
- [ ] 数据库索引优化
- [ ] 压力测试
- [ ] 监控告警完善

**产出文件**：
```
修改：
├── src/main/java/com/lvatong/lft/service/ContractService.java
├── src/main/java/com/lvatong/lft/service/LegalOpinionService.java
├── src/main/java/com/lvatong/lft/rag/RAGService.java
├── src/main/resources/db/migration/V9__add_indexes.sql
└── docker/grafana-dashboards/v3-dashboard.json
```

#### Phase 6: 前端优化（1周）

**目标**：Markdown渲染优化、智能提示、UI细节打磨

**任务清单**：
- [ ] Markdown渲染优化
- [ ] 智能提示组件
- [ ] 推荐面板集成
- [ ] UI细节打磨

**产出文件**：
```
新增：
├── frontend/src/components/SmartSuggestion.vue
└── frontend/src/components/MarkdownRenderer.vue

修改：
├── frontend/src/views/ChatView.vue
├── frontend/src/views/HomeView.vue
└── frontend/src/assets/styles/main.css
```

---

## 四、容量规划

### 4.1 硬件配置

**应用服务器**：
- CPU: 4核8G 或 8核16G
- 存储: 256G SSD
- 运行: Spring Boot + RabbitMQ + MySQL + Redis

**GPU推理服务器**：
- GPU: RTX 3090 24GB
- CPU: 4核8G
- 存储: 512G SSD（模型文件）
- 运行: Reranker + Embedding + Milvus

### 4.2 容量指标

| 指标 | 数值 |
|------|------|
| 同时在线用户 | 200-400人 |
| 注册用户 | 1000-3000人 |
| 日活用户 | 400-800人 |
| 峰值QPS | 10-20 |
| 日均查询 | 3000-8000次 |

### 4.3 扩展路径

```
阶段一（当前）：单GPU 24GB
├── 支撑：1-3 个中小律所
└── 成本：¥5000-7000（一次性）

阶段二（用户增长）：双GPU 48GB
├── 支撑：5-10 个中小律所
├── 方案：2 × RTX 3090 或 1 × A6000 48GB
└── 成本：¥10000-15000

阶段三（规模化）：GPU集群
├── 支撑：20+ 个律所
├── 方案：多节点 + 负载均衡
└── 成本：¥30000-50000
```

---

## 五、风险评估

### 5.1 技术风险

| 风险 | 概率 | 影响 | 应对措施 |
|------|------|------|----------|
| GPU服务不稳定 | 低 | 高 | 降级到CPU推理 |
| Reranker效果不佳 | 低 | 中 | 回退到LLM Reranking |
| RabbitMQ消息堆积 | 低 | 中 | 监控告警 + 扩容 |
| 数据库性能瓶颈 | 中 | 中 | 索引优化 + 读写分离 |

### 5.2 进度风险

| 风险 | 概率 | 影响 | 应对措施 |
|------|------|------|----------|
| 技术难点超预期 | 中 | 中 | 预留buffer时间 |
| 依赖服务延迟 | 低 | 低 | 提前准备备选方案 |
| 需求变更 | 中 | 中 | 控制范围，分期迭代 |

---

## 六、里程碑

| 里程碑 | 时间点 | 验收标准 |
|--------|--------|----------|
| M1: 基础设施就绪 | 第1周 | GPU服务可调用、RabbitMQ连通 |
| M2: Agent升级完成 | 第3周 | ReAct推理可用、工具链扩展完成 |
| M3: RAG增强上线 | 第4.5周 | Reranker生效、检索质量提升 |
| M4: 推荐系统上线 | 第6.5周 | 个性化推荐可用 |
| M5: 系统优化完成 | 第8周 | 压测通过、性能达标 |
| M6: 前端优化完成 | 第9周 | UI优化完成、用户验收 |

---

## 七、验收标准

### 7.1 功能验收

- [ ] ReAct Agent推理链可正常工作
- [ ] Plan-and-Execute复杂问题分解可用
- [ ] Reranker检索质量提升明显
- [ ] 个性化推荐准确率>70%
- [ ] 异步任务处理正常
- [ ] 前端体验流畅

### 7.2 性能验收

- [ ] 平均响应时间<3s
- [ ] 峰值QPS>10
- [ ] GPU利用率<80%
- [ ] 系统可用性>99%

### 7.3 质量验收

- [ ] 单元测试覆盖率>70%
- [ ] 集成测试通过
- [ ] 压力测试通过
- [ ] 安全扫描通过

---

## 八、附录

### 8.1 依赖服务

| 服务 | 版本 | 用途 |
|------|------|------|
| MySQL | 8.0 | 业务主库 |
| Redis | 7.x | 缓存+会话 |
| RabbitMQ | 3.x | 消息队列 |
| Milvus | 2.4.x | 向量数据库 |
| Nginx | 1.24+ | 反向代理 |

### 8.2 开发工具

| 工具 | 用途 |
|------|------|
| IntelliJ IDEA | Java开发 |
| VS Code | 前端开发 |
| Docker Desktop | 本地容器化 |
| Postman | API测试 |
| Git | 版本控制 |

### 8.3 参考文档

- [V2 README](./README.md)
- [项目结构文档](./PROJECT-BACKEND.md)
- [前端结构文档](./PROJECT-FRONTEND.md)

---

**文档状态**：✅ 已确认
**下一步**：生成阶段计划书（Phase 1）
