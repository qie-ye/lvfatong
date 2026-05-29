# 律法通 后端项目结构文档

> 生成时间：2025-01-27
> 技术栈：Java 17 / Spring Boot 3.2.5 / MySQL 8 / Redis 7 / Milvus 2.4 / RabbitMQ 3.x
> 状态：V3 完成版本

---

## 1. 项目结构树

```
src/main/java/com/lvatong/lft/
│
├── LvatongApplication.java                    # 应用启动类
│
├── agent/                                     # [V3新增] Agent 推理模块
│   ├── ReActAgent.java                        # ReAct 推理框架
│   ├── PlanAndExecuteService.java             # Plan-and-Execute 模式
│   └── SubTask.java                           # 子任务数据模型
│
├── ai/                                        # AI 核心模块
│   ├── AgentOrchestrator.java                 # Multi-Agent 编排器（Supervisor 模式）
│   ├── AgentPrompt.java                       # Agent 提示词定义
│   ├── ChatService.java                       # 聊天服务（核心）
│   ├── EmbeddingService.java                  # 向量化服务
│   ├── IntentClassifier.java                  # 意图分类器
│   ├── ModelRouterService.java                # 模型路由服务
│   ├── PromptTemplateService.java             # 提示词模板服务
│   ├── ToolExecutor.java                      # 工具执行器
│   ├── ToolRegistry.java                      # 工具注册中心
│   ├── VerificationService.java               # 验证服务
│   ├── ZhipuApiClient.java                    # 智谱 API 客户端
│   └── tools/                                 # [V3新增] AI 工具
│       ├── CompensationCalculator.java        # 赔偿计算工具
│       └── StatuteChecker.java                # 时效检查工具
│
├── async/                                     # 异步任务模块
│   ├── AsyncTaskConsumer.java                 # 异步任务消费者
│   ├── AsyncTaskMessage.java                  # 异步任务消息体
│   ├── AsyncTaskProducer.java                 # 异步任务生产者
│   ├── AsyncTaskRouter.java                   # 异步任务路由器
│   └── AsyncTaskService.java                  # [V3新增] 统一异步任务服务
│
├── cache/                                     # [V3新增] 缓存模块
│   ├── L1CacheConfig.java                     # Caffeine 本地缓存配置
│   └── CacheService.java                      # 统一缓存服务
│
├── gpu/                                       # [V3新增] GPU 服务模块
│   ├── GpuServiceConfig.java                  # GPU 服务配置
│   └── GpuServiceClient.java                  # GPU 服务客户端
│
├── mq/                                        # [V3新增] 消息队列模块
│   ├── RabbitMQConfig.java                    # RabbitMQ 配置
│   ├── ContractAnalysisProducer.java          # 合同分析生产者
│   ├── ContractAnalysisConsumer.java          # 合同分析消费者
│   └── dto/
│       └── AnalysisTaskMessage.java           # 分析任务消息体
│
├── recommendation/                            # [V3新增] 推荐模块
│   ├── UserBehaviorAnalyzer.java              # 用户行为分析
│   └── RecommendationEngine.java              # 推荐引擎
│
├── common/                                    # 通用模块
│   ├── audit/                                 # 审计模块
│   │   ├── AuditAspect.java                   # 审计切面
│   │   └── Auditable.java                     # 审计注解
│   ├── exception/                             # 异常处理
│   │   └── BusinessException.java             # 业务异常
│   ├── filter/                                # 过滤器
│   │   └── MdcFilter.java                     # MDC 过滤器
│   ├── ratelimit/                             # 限流模块
│   │   └── RateLimitAspect.java               # 限流切面
│   └── result/                                # 统一结果
│       ├── ApiResult.java                     # API 响应封装
│       └── GlobalExceptionHandler.java        # 全局异常处理
│
├── config/                                    # 配置模块
│   ├── AsyncConfig.java                       # 异步配置
│   ├── CacheConfig.java                       # 缓存配置
│   ├── DataInitializer.java                   # 数据初始化
│   ├── MilvusConfig.java                      # Milvus 配置
│   ├── OpenApiConfig.java                     # OpenAPI/Swagger 配置
│   ├── RedisConfig.java                       # Redis 配置
│   ├── SecurityConfig.java                    # 安全配置
│   ├── WebConfig.java                         # Web 配置
│   ├── WebSocketConfig.java                   # WebSocket 配置
│   └── XfyunConfig.java                       # 讯飞配置
│
├── contract/                                  # 合同分析模块
│   ├── ClauseExtractor.java                   # 条款提取器
│   ├── ContractService.java                   # 合同服务
│   ├── DocumentParser.java                    # 文档解析器
│   └── RiskAssessor.java                      # 风险评估器
│
├── controller/                                # 控制器层（16个）
│   ├── AdminController.java                   # 管理后台
│   ├── AuthController.java                    # 认证授权
│   ├── CaseController.java                    # 案例检索
│   ├── ContractController.java                # 合同分析
│   ├── FeedbackController.java                # 反馈管理
│   ├── KnowledgeController.java               # 知识库管理
│   ├── LawyerController.java                  # 律师管理
│   ├── LegalController.java                   # 法律咨询（核心）
│   ├── LegalDocumentController.java           # 法律文书
│   ├── LegalOpinionController.java            # 法律意见
│   ├── MemoryController.java                  # 记忆管理
│   ├── NotificationController.java            # 通知管理
│   ├── RecommendationController.java          # [V3新增] 推荐系统
│   ├── SpeechController.java                  # 语音服务
│   ├── SystemController.java                  # 系统管理
│   └── UserController.java                    # 用户管理
│
├── knowledge/                                 # 知识库模块
│   ├── CaseDataImportService.java             # 案例数据导入
│   ├── FaqService.java                        # FAQ 服务
│   ├── KnowledgeService.java                  # 知识库服务
│   └── NpcLawFetchService.java                # 法条抓取服务
│
├── model/                                     # 数据模型
│   ├── dto/                                   # 数据传输对象（28个）
│   │   ├── AdminOverviewResponse.java
│   │   ├── AppointmentResponse.java
│   │   ├── AuthResponse.java
│   │   ├── ChangePasswordRequest.java
│   │   ├── ChatRequest.java
│   │   ├── ContractAnalysisResult.java
│   │   ├── ContractModificationSuggestion.java
│   │   ├── ContractUploadResponse.java
│   │   ├── CreateAppointmentRequest.java
│   │   ├── CreateLawyerProfileRequest.java
│   │   ├── CreateReviewRequest.java
│   │   ├── DailyStatDto.java
│   │   ├── FeedbackRequest.java
│   │   ├── FeedbackStatsResponse.java
│   │   ├── GenerateDocumentRequest.java
│   │   ├── GenerateOpinionRequest.java
│   │   ├── LawyerProfileResponse.java
│   │   ├── LawyerReviewResponse.java
│   │   ├── LegalCaseResponse.java
│   │   ├── LegalDocumentResponse.java
│   │   ├── LegalOpinionResponse.java
│   │   ├── LoginRequest.java
│   │   ├── NameValueDto.java
│   │   ├── RefreshRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── SearchRequest.java
│   │   ├── UpdateProfileRequest.java
│   │   └── UserProfileResponse.java
│   └── entity/                                # 数据实体（20个）
│       ├── AnswerFeedback.java
│       ├── Appointment.java
│       ├── AuditLog.java
│       ├── BaseEntity.java
│       ├── ChatMessage.java
│       ├── ChatSession.java
│       ├── ContractDocument.java
│       ├── ContractTemplate.java
│       ├── FaqEntry.java
│       ├── KnowledgeChunk.java
│       ├── KnowledgeDocument.java
│       ├── LawyerProfile.java
│       ├── LawyerReview.java
│       ├── LegalCase.java
│       ├── LegalDocument.java
│       ├── LegalOpinion.java
│       ├── Notification.java
│       ├── SessionSummary.java
│       ├── User.java
│       └── UserMemory.java
│
├── rag/                                       # RAG 检索模块
│   ├── DocumentChunker.java                   # 文档分块器
│   ├── SemanticChunker.java                   # [V3新增] 语义分块器
│   ├── HybridSearchService.java               # 混合检索服务
│   ├── RAGService.java                        # RAG 服务（核心）
│   ├── RerankerService.java                   # [V3新增] Reranker 服务
│   └── VectorStoreService.java                # 向量存储服务
│
├── recommendation/                            # [V3新增] 推荐模块
│   ├── UserBehaviorAnalyzer.java              # 用户行为分析
│   └── RecommendationEngine.java              # 推荐引擎
│
├── repository/                                # 数据访问层
│   ├── recommendation/                        # [V3新增] 推荐相关
│   │   ├── UserBehaviorRepository.java
│   │   ├── UserPreferenceRepository.java
│   │   ├── RecommendationLogRepository.java
│   │   └── PopularQueryRepository.java
│   ├── AnswerFeedbackRepository.java
│   ├── AnswerFeedbackRepository.java
│   ├── AppointmentRepository.java
│   ├── AuditLogRepository.java
│   ├── ChatMessageRepository.java
│   ├── ChatSessionRepository.java
│   ├── ContractDocumentRepository.java
│   ├── ContractTemplateRepository.java
│   ├── FaqEntryRepository.java
│   ├── KnowledgeChunkRepository.java
│   ├── KnowledgeDocumentRepository.java
│   ├── LawyerProfileRepository.java
│   ├── LawyerReviewRepository.java
│   ├── LegalCaseRepository.java
│   ├── LegalDocumentRepository.java
│   ├── LegalOpinionRepository.java
│   ├── NotificationRepository.java
│   ├── SessionSummaryRepository.java
│   ├── UserMemoryRepository.java
│   └── UserRepository.java
│
├── security/                                  # 安全模块
│   ├── JwtAccessDeniedHandler.java            # JWT 访问拒绝处理
│   ├── JwtAuthEntryPoint.java                 # JWT 认证入口
│   ├── JwtAuthenticationFilter.java           # JWT 认证过滤器
│   └── JwtTokenProvider.java                  # JWT Token 提供者
│
├── service/                                   # 业务服务层（16个）
│   ├── AuthService.java                       # 认证服务
│   ├── CaseService.java                       # 案例服务
│   ├── ChatMemoryService.java                 # 聊天记忆服务
│   ├── FeedbackAnalysisService.java           # 反馈分析服务
│   ├── FeedbackService.java                   # 反馈服务
│   ├── LawyerReviewService.java               # 律师评价服务
│   ├── LawyerService.java                     # 律师服务
│   ├── LegalDocumentService.java              # 法律文书服务
│   ├── LegalOpinionService.java               # 法律意见服务
│   ├── LegalService.java                      # 法律服务（核心）
│   ├── MemoryCleanupScheduler.java            # 记忆清理调度器
│   ├── NotificationService.java               # 通知服务
│   ├── SessionSummaryService.java             # 会话摘要服务
│   ├── UserMemoryService.java                 # 用户记忆服务
│   ├── UserService.java                       # 用户服务
│   └── XfyunAuthService.java                  # 讯飞认证服务
│
└── websocket/                                 # WebSocket 模块
    └── AuthChannelInterceptor.java            # WebSocket 认证拦截器
```

---

## 2. 模块说明

### 2.1 AI 核心模块 (`ai/`)

| 类名 | 职责 | 关键方法 |
|------|------|----------|
| `ChatService` | 聊天核心服务 | `legalQa()`, `legalQaStream()`, `legalQaWithTools()`, `contractAnalysis()` |
| `AgentOrchestrator` | Multi-Agent 编排 | `orchestrateLegalOpinion()`, `orchestateComplexContract()` |
| `AgentPrompt` | Agent 提示词 | `buildResearcherPrompt()`, `buildAnalystPrompt()`, `buildCriticPrompt()` |
| `IntentClassifier` | 意图分类 | `classify()`, `classifyWithConfidence()` |
| `ModelRouterService` | 模型路由 | `getModelForTask()`, `getParamsForTask()` |
| `PromptTemplateService` | 提示词模板 | `buildLegalQaSystemPrompt()`, `buildContractAnalysisSystemPrompt()` |
| `ToolRegistry` | 工具注册 | `getAllTools()` |
| `ToolExecutor` | 工具执行 | `execute()` |
| `VerificationService` | 验证服务 | `verifyLegalAnswer()`, `detectContractConflicts()` |
| `EmbeddingService` | 向量化 | `embed()` |
| `ZhipuApiClient` | 智谱 API | `chat()`, `chatStream()`, `chatWithTools()` |

### 2.2 RAG 检索模块 (`rag/`)

| 类名 | 职责 | 关键方法 |
|------|------|----------|
| `RAGService` | RAG 核心服务 | `ingestDocument()`, `retrieveAndBuildContext()`, `retrieveAndBuildContextEnhanced()` |
| `HybridSearchService` | 混合检索 | `search()` - 向量+全文 RRF 融合 |
| `DocumentChunker` | 文档分块 | `chunk()` |
| `VectorStoreService` | 向量存储 | `batchInsert()`, `search()` |

### 2.3 合同分析模块 (`contract/`)

| 类名 | 职责 |
|------|------|
| `ContractService` | 合同分析主服务 |
| `DocumentParser` | PDF/Word 文档解析 |
| `ClauseExtractor` | 条款提取 |
| `RiskAssessor` | 风险评估 |

### 2.4 安全模块 (`security/`)

| 类名 | 职责 |
|------|------|
| `JwtTokenProvider` | JWT Token 生成/验证 |
| `JwtAuthenticationFilter` | JWT 认证过滤器 |
| `JwtAuthEntryPoint` | 认证失败入口 |
| `JwtAccessDeniedHandler` | 访问拒绝处理 |

---

## 3. API 接口文档

### 3.1 AuthController (`/api/auth`)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/login` | 用户登录 | 公开 |
| POST | `/api/auth/register` | 用户注册 | 公开 |
| POST | `/api/auth/refresh` | 刷新令牌 | 公开 |
| POST | `/api/auth/logout` | 退出登录 | 已登录 |

### 3.2 LegalController (`/api/legal`)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/legal/chat` | 法律问答（SSE流式） | 已登录 |
| POST | `/api/legal/chat-sync` | 法律问答（同步） | 已登录 |
| POST | `/api/legal/session` | 创建会话 | 已登录 |
| GET | `/api/legal/sessions` | 获取会话列表 | 已登录 |
| GET | `/api/legal/sessions/{id}/messages` | 获取会话消息 | 已登录 |
| PUT | `/api/legal/sessions/{id}` | 重命名会话 | 已登录 |
| DELETE | `/api/legal/sessions/{id}` | 删除会话 | 已登录 |
| POST | `/api/legal/sessions/{id}/end` | 结束会话 | 已登录 |

### 3.3 ContractController (`/api/contracts`)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/contracts/upload` | 上传合同 | 已登录 |
| GET | `/api/contracts/{id}` | 获取合同详情 | 已登录 |
| GET | `/api/contracts/{id}/analysis` | 获取分析结果 | 已登录 |
| POST | `/api/contracts/{id}/analyze` | 触发分析 | 已登录 |

### 3.4 KnowledgeController (`/api/knowledge`)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/knowledge/laws` | 法条检索 | 公开 |
| GET | `/api/knowledge/laws/{id}` | 法条详情 | 公开 |
| GET | `/api/knowledge/faq` | FAQ 列表 | 公开 |
| GET | `/api/knowledge/faq/search` | FAQ 搜索 | 公开 |

### 3.5 CaseController (`/api/cases`)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/cases` | 案例列表 | 公开 |
| GET | `/api/cases/search` | 案例搜索 | 公开 |
| GET | `/api/cases/{id}` | 案例详情 | 公开 |

### 3.6 LawyerController (`/api/lawyers`)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/lawyers` | 律师列表 | 公开 |
| GET | `/api/lawyers/{id}` | 律师详情 | 公开 |
| GET | `/api/lawyers/search` | 律师搜索 | 公开 |
| POST | `/api/lawyers/{id}/reviews` | 添加评价 | 已登录 |

### 3.7 LegalOpinionController (`/api/opinions`)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/opinions/generate` | 生成法律意见 | 已登录 |
| GET | `/api/opinions` | 意见列表 | 已登录 |
| GET | `/api/opinions/{id}` | 意见详情 | 已登录 |

### 3.8 LegalDocumentController (`/api/documents`)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/documents/generate` | 生成文书 | 已登录 |
| GET | `/api/documents` | 文书列表 | 已登录 |
| GET | `/api/documents/{id}` | 文书详情 | 已登录 |

### 3.9 UserController (`/api/users`)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/users/profile` | 获取个人信息 | 已登录 |
| PUT | `/api/users/profile` | 更新个人信息 | 已登录 |
| PUT | `/api/users/password` | 修改密码 | 已登录 |

### 3.10 AdminController (`/api/admin`)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/admin/overview` | 管理概览 | 管理员 |
| GET | `/api/admin/users` | 用户列表 | 管理员 |
| PUT | `/api/admin/users/{id}/role` | 修改用户角色 | 管理员 |
| GET | `/api/admin/audit-logs` | 审计日志 | 管理员 |
| GET | `/api/admin/stats` | 统计数据 | 管理员 |

---

## 4. 数据模型

### 4.1 Entity 实体（20个）

| 实体 | 表名 | 说明 |
|------|------|------|
| `User` | `users` | 用户表 |
| `ChatSession` | `chat_sessions` | 聊天会话 |
| `ChatMessage` | `chat_messages` | 聊天消息 |
| `ContractDocument` | `contract_documents` | 合同文档 |
| `ContractTemplate` | `contract_templates` | 合同模板 |
| `LegalCase` | `legal_cases` | 法律案例 |
| `LegalDocument` | `legal_documents` | 法律文书 |
| `LegalOpinion` | `legal_opinions` | 法律意见 |
| `LawyerProfile` | `lawyer_profiles` | 律师档案 |
| `LawyerReview` | `lawyer_reviews` | 律师评价 |
| `FaqEntry` | `faq_entries` | FAQ 条目 |
| `KnowledgeDocument` | `knowledge_documents` | 知识文档 |
| `KnowledgeChunk` | `knowledge_chunks` | 知识分块 |
| `AnswerFeedback` | `answer_feedback` | 回答反馈 |
| `Appointment` | `appointments` | 预约 |
| `AuditLog` | `audit_logs` | 审计日志 |
| `Notification` | `notifications` | 通知 |
| `SessionSummary` | `session_summaries` | 会话摘要 |
| `UserMemory` | `user_memory` | 用户记忆 |
| `BaseEntity` | - | 基础实体（id, createdAt, updatedAt） |

### 4.2 DTO 数据传输对象（28个）

| DTO | 说明 |
|-----|------|
| `LoginRequest` | 登录请求 |
| `RegisterRequest` | 注册请求 |
| `AuthResponse` | 认证响应 |
| `ChatRequest` | 聊天请求 |
| `ContractAnalysisResult` | 合同分析结果 |
| `LegalOpinionResponse` | 法律意见响应 |
| `LegalDocumentResponse` | 法律文书响应 |
| `LegalCaseResponse` | 案例响应 |
| `LawyerProfileResponse` | 律师档案响应 |
| `UserProfileResponse` | 用户档案响应 |
| `AdminOverviewResponse` | 管理概览响应 |
| ... | 其他 DTO |

---

## 5. 配置说明

### 5.1 核心配置文件

- `application.yml` - 主配置
- `application-prod.yml` - 生产环境配置

### 5.2 关键配置项

```yaml
# AI 配置
zhipu:
  api-key: ${ZHIPU_API_KEY}
  model:
    fast: glm-4-flash
    deep: glm-4-plus
    long: glm-4-long

# RAG 配置
rag:
  max-context-tokens: 4096
  chunk-size: 500
  chunk-overlap: 50

# Milvus 配置
milvus:
  enabled: ${MILVUS_ENABLED:true}
  host: ${MILVUS_HOST:localhost}
  port: ${MILVUS_PORT:19530}

# Redis 配置
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
```

---

## 6. Flyway 迁移脚本

| 版本 | 文件 | 说明 |
|------|------|------|
| V1 | `V1__init_schema.sql` | 初始化表结构 |
| V2 | `V2__add_source_url.sql` | 添加来源URL |
| V3 | `V3__add_memory_tables.sql` | 记忆相关表 |
| V4 | `V4__seed_legal_cases.sql` | 案例数据 |
| V6 | `V6__seed_faq_and_templates.sql` | FAQ和模板数据 |
| V7 | `V7__extend_feedback_table.sql` | 扩展反馈表 |

---

## 7. 技术栈总结

| 层次 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.2.5 |
| 安全 | Spring Security + JWT | - |
| 持久化 | Spring Data JPA + Hibernate | - |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.x |
| 向量库 | Milvus | 2.4.x |
| AI | 智谱 GLM 系列 | - |
| 文档 | Springdoc OpenAPI | 2.3.0 |
| 熔断 | Resilience4j | 2.2.0 |
| 迁移 | Flyway | - |
| 消息队列 | RabbitMQ | 3.x |
| 本地缓存 | Caffeine | 3.x |

---

## V3 新增模块说明

### 1. Agent 推理模块 (`agent/`)
- **ReActAgent**: 显式推理链框架，Thought→Action→Observation循环
- **PlanAndExecuteService**: 复杂问题分解执行，Planner→Executor→Synthesizer
- **SubTask**: 子任务数据模型

### 2. AI 工具 (`ai/tools/`)
- **CompensationCalculator**: 赔偿计算（劳动/工伤/交通事故/加班费）
- **StatuteChecker**: 时效检查（民事/劳动/合同/侵权）

### 3. 缓存模块 (`cache/`)
- **L1CacheConfig**: Caffeine 本地缓存配置
- **CacheService**: 统一缓存服务（RAG/Intent/Embedding）

### 4. GPU 服务模块 (`gpu/`)
- **GpuServiceConfig**: GPU 推理服务配置
- **GpuServiceClient**: GPU 服务客户端（Reranker/Embedding）

### 5. 消息队列模块 (`mq/`)
- **RabbitMQConfig**: RabbitMQ 配置
- **ContractAnalysisProducer**: 合同分析消息生产者
- **ContractAnalysisConsumer**: 合同分析消息消费者

### 6. 推荐模块 (`recommendation/`)
- **UserBehaviorAnalyzer**: 用户行为分析，偏好画像更新
- **RecommendationEngine**: 推荐引擎（个性化/热门/协同过滤）

### 7. RAG 增强 (`rag/`)
- **SemanticChunker**: 语义分块器，支持法律文档结构识别
- **RerankerService**: Reranker 服务，调用 GPU BGE-Reranker 模型

---

**文档状态**：✅ V3 完成版本
**最后更新**：2025-01-27
