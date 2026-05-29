# 律法通 V3 新增功能说明

> 版本：V3.0
> 最后更新：2025-01-27

---

## 目录

1. [AI Agent 推理能力](#1-ai-agent-推理能力)
2. [赔偿计算工具](#2-赔偿计算工具)
3. [时效检查工具](#3-时效检查工具)
4. [RAG 检索增强](#4-rag-检索增强)
5. [个性化推荐系统](#5-个性化推荐系统)
6. [智能提示功能](#6-智能提示功能)
7. [消息队列支持](#7-消息队列支持)
8. [GPU 推理服务](#8-gpu-推理服务)

---

## 1. AI Agent 推理能力

### 1.1 功能概述

V3 引入了两种高级 AI 推理模式，提升复杂法律问题的处理能力：

| 推理模式 | 适用场景 | 特点 |
|----------|----------|------|
| **ReAct Agent** | 需要多步推理的问题 | 显式推理链，可解释性强 |
| **Plan-and-Execute** | 涉及多领域的复杂问题 | 问题分解，逐步执行 |

### 1.2 ReAct Agent

**工作流程**：
```
Thought: 用户问的是劳动仲裁问题，需要先查询相关法条...
Action: search_law(query="劳动仲裁时效")
Observation: 《劳动争议调解仲裁法》第27条...
Thought: 找到法条，现在需要查找类似案例...
Action: search_case(query="劳动仲裁时效争议")
Observation: ...
Final Answer: 基于以上研究...
```

**触发条件**：
- 用户问题意图不明确（置信度 < 0.7）
- 需要多步推理才能回答
- 涉及多个法律概念

### 1.3 Plan-and-Execute

**工作流程**：
```
1. Planner：分析问题，制定执行计划
2. Executor：逐个执行子任务
3. Synthesizer：综合所有结果，生成最终答案
```

**触发条件**：
- 问题涉及多个法律领域
- 需要综合分析多个方面
- 问题复杂度高

### 1.4 智能路由

系统会根据问题复杂度自动选择最优推理模式：

```java
// ChatService.java
public String smartLegalQa(String question, ...) {
    if (复杂法律问题) {
        return legalQaWithPlanExecute(question, ...);
    } else if (不确定的法律问题) {
        return legalQaWithReAct(question, ...);
    } else if (案例/法条查询) {
        return legalQaWithTools(question, ...);
    } else {
        return legalQa(question, ...);
    }
}
```

---

## 2. 赔偿计算工具

### 2.1 功能概述

支持多种法律场景的赔偿金额计算，AI 可自动调用此工具。

### 2.2 支持的赔偿类型

| 类型 | 说明 | 参数 |
|------|------|------|
| `labor_compensation` | 劳动经济补偿金 | 月工资、工作年限 |
| `labor_penalty` | 违法解除赔偿金 | 月工资、工作年限 |
| `work_injury` | 工伤赔偿 | 伤残等级、月工资、医疗费 |
| `traffic_accident` | 交通事故赔偿 | 各项费用 |
| `overtime` | 加班费 | 时薪、加班小时数 |

### 2.3 使用示例

**用户提问**：
> "我在公司工作了5年，月工资10000元，被违法辞退能拿多少赔偿？"

**AI 自动调用**：
```json
{
  "type": "labor_penalty",
  "monthlySalary": 10000,
  "workYears": 5
}
```

**计算结果**：
```json
{
  "type": "违法解除赔偿金",
  "monthlySalary": 10000,
  "workYears": 5,
  "compensation": 50000,
  "penalty": 100000,
  "legalBasis": "《劳动合同法》第87条",
  "explanation": "工作5年，月工资10000元，违法解除赔偿金为100000元（经济补偿50000元的2倍）"
}
```

### 2.4 法律依据

- 劳动经济补偿：《劳动合同法》第 47 条
- 违法解除赔偿：《劳动合同法》第 87 条
- 工伤赔偿：《工伤保险条例》第 35-37 条
- 加班费：《劳动法》第 44 条

---

## 3. 时效检查工具

### 3.1 功能概述

检查各类法律纠纷的诉讼时效，告知用户是否还在时效期内。

### 3.2 支持的案件类型

| 类型 | 时效期限 | 法律依据 |
|------|----------|----------|
| `civil` | 3 年 | 《民法典》第 188 条 |
| `labor` | 1 年 | 《劳动争议调解仲裁法》第 27 条 |
| `contract` | 3 年 | 《民法典》第 188 条 |
| `tort` | 3 年（最长 20 年） | 《民法典》第 188 条 |
| `product_liability` | 3 年（最长 10 年） | 《产品质量法》第 45 条 |
| `environmental` | 3 年 | 《环境保护法》第 66 条 |

### 3.3 使用示例

**用户提问**：
> "我去年3月15日被公司拖欠工资，现在申请劳动仲裁还来得及吗？"

**AI 自动调用**：
```json
{
  "caseType": "labor",
  "incidentDate": "2024-03-15"
}
```

**检查结果**：
```json
{
  "caseType": "劳动争议仲裁",
  "statutePeriod": "1年",
  "incidentDate": "2024-03-15",
  "deadline": "2025-03-15",
  "daysRemaining": 47,
  "expired": false,
  "legalBasis": "《劳动争议调解仲裁法》第27条",
  "explanation": "劳动仲裁时效至2025-03-15届满，还剩47天"
}
```

---

## 4. RAG 检索增强

### 4.1 语义分块

**改进点**：从固定长度分块升级为基于语义的智能分块

**特点**：
- 按句子边界分割，不切断句子
- 支持重叠窗口，保持上下文连贯
- 识别法律文档结构（条款、章节）
- 支持中英文混合文本

**配置**：
```java
// SemanticChunker.java
private static final int DEFAULT_CHUNK_SIZE = 512;  // tokens
private static final int DEFAULT_OVERLAP = 64;      // tokens
```

### 4.2 GPU Reranker

**改进点**：从 LLM Reranking 升级为 GPU Reranker 模型

**优势**：
- 速度更快：<100ms（vs LLM 的 1-2s）
- 成本更低：无 API 调用费用
- 效果更好：专业 reranking 模型

**模型**：BAAI/bge-reranker-v2-m3（568M 参数）

**配置**：
```yaml
lvatong:
  gpu:
    enabled: true
    base-url: http://localhost:8081
```

### 4.3 检索流程

```
用户查询
    ↓
Query Rewriting（查询改写）
    ↓
混合检索（向量 + 全文）
    ↓
RRF 融合排序
    ↓
GPU Reranker 重排序
    ↓
上下文压缩
    ↓
返回结果
```

---

## 5. 个性化推荐系统

### 5.1 功能概述

基于用户行为和偏好，提供个性化的内容推荐。

### 5.2 推荐策略

| 策略 | 说明 | 触发场景 |
|------|------|----------|
| **个性化推荐** | 基于用户偏好领域 | 首页、登录后 |
| **热门推荐** | 基于全局热门查询 | 新用户、首页 |
| **协同过滤** | 基于相似用户行为 | 个人中心 |
| **查询推荐** | 基于当前查询内容 | 聊天页面 |

### 5.3 用户行为采集

系统会自动记录以下用户行为：

| 行为类型 | 说明 |
|----------|------|
| `SEARCH` | 用户搜索查询 |
| `VIEW` | 用户查看内容 |
| `CLICK` | 用户点击推荐 |
| `FEEDBACK` | 用户提交反馈 |
| `SHARE` | 用户分享内容 |

### 5.4 API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/recommendations/personalized` | 个性化推荐 |
| GET | `/api/recommendations/query-based` | 基于查询推荐 |
| GET | `/api/recommendations/popular` | 热门推荐 |
| GET | `/api/recommendations/collaborative` | 协同过滤推荐 |
| POST | `/api/recommendations/behaviors` | 记录用户行为 |
| GET | `/api/recommendations/history` | 查询历史 |

### 5.5 前端展示

**推荐面板组件**：
```vue
<RecommendationPanel
  :visible="true"
  title="热门问题"
  :recommendations="recommendations"
  @select="handleSelect"
  @refresh="handleRefresh"
/>
```

---

## 6. 智能提示功能

### 6.1 功能概述

在用户输入时，根据输入内容智能推荐相关问题和内容。

### 6.2 触发条件

- 用户输入超过 3 个字符
- 输入停顿 300ms 后触发
- 包含特定法律关键词

### 6.3 提示类型

| 类型 | 图标 | 说明 |
|------|------|------|
| `law` | 📄 | 相关法条 |
| `case` | 🔍 | 相似案例 |
| `faq` | ❓ | 常见问题 |
| `lawyer` | 👤 | 推荐律师 |

### 6.4 使用示例

**用户输入**：`劳动仲裁`

**智能提示**：
- 📄 劳动合同法相关条款
- ❓ 劳动纠纷常见问题
- 👤 劳动法专业律师

### 6.5 前端组件

```vue
<SmartSuggestion
  :visible="showSuggestions"
  :suggestions="suggestions"
  @select="handleSuggestionSelect"
  @close="showSuggestions = false"
/>
```

---

## 7. 消息队列支持

### 7.1 功能概述

V3 支持两种消息队列，可通过配置切换：

| 消息队列 | 特点 | 适用场景 |
|----------|------|----------|
| **Redis Stream** | 轻量级、无需额外部署 | 开发环境、小规模 |
| **RabbitMQ** | 企业级、功能丰富 | 生产环境、大规模 |

### 7.2 配置方式

```bash
# .env 文件
MQ_TYPE=redis      # 使用 Redis Stream
MQ_TYPE=rabbitmq   # 使用 RabbitMQ
```

### 7.3 应用场景

- 合同分析异步处理
- 知识库更新任务
- 用户行为事件处理
- 通知推送

### 7.4 代码示例

```java
// AsyncTaskService.java
public void publishContractAnalysis(Long documentId, Long userId, ...) {
    if ("rabbitmq".equalsIgnoreCase(mqType)) {
        // 发送到 RabbitMQ
        rabbitProducer.sendAnalysisTask(message);
    } else {
        // 发送到 Redis Stream
        redisTaskProducer.publish(message);
    }
}
```

---

## 8. GPU 推理服务

### 8.1 功能概述

独立的 GPU 推理服务，提供 Reranker 和 Embedding 能力。

### 8.2 服务架构

```
┌─────────────────────────────────────┐
│       GPU 推理服务 (FastAPI)         │
├─────────────────────────────────────┤
│  /rerank    → BGE-Reranker 模型     │
│  /embed     → BGE-M3 Embedding 模型 │
│  /health    → 健康检查              │
└─────────────────────────────────────┘
```

### 8.3 部署方式

**Docker 部署**：
```bash
cd docker/gpu-inference
docker build -t lvatong-gpu-inference .
docker run -d --gpus all -v ../../models:/models -p 8081:8081 lvatong-gpu-inference
```

**直接运行**：
```bash
cd docker/gpu-inference
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 8081
```

### 8.4 API 接口

**Rerank 接口**：
```bash
curl -X POST http://localhost:8081/rerank \
    -H "Content-Type: application/json" \
    -d '{
        "query": "劳动法",
        "documents": ["劳动合同", "婚姻法", "工资支付"],
        "top_k": 2
    }'
```

**Embed 接口**：
```bash
curl -X POST http://localhost:8081/embed \
    -H "Content-Type: application/json" \
    -d '{"texts": ["劳动法", "合同法"]}'
```

### 8.5 降级策略

当 GPU 服务不可用时：
- Reranker：使用原始检索顺序
- Embedding：跳过向量检索，仅使用全文检索

---

## 附录：常见问题

### Q1: 如何启用 GPU 推理服务？

**A**：
1. 准备 GPU 服务器（RTX 3090+）
2. 下载模型：`bash scripts/download-models.sh`
3. 启动服务：`docker compose up -d gpu-inference`
4. 配置启用：`.env` 设置 `GPU_ENABLED=true`

### Q2: 如何切换消息队列？

**A**：修改 `.env` 文件中的 `MQ_TYPE` 配置：
- `MQ_TYPE=redis`：使用 Redis Stream（默认）
- `MQ_TYPE=rabbitmq`：使用 RabbitMQ

### Q3: 推荐系统如何工作？

**A**：
1. 系统自动记录用户行为（搜索、查看、反馈）
2. 分析用户偏好（领域、频率、专业水平）
3. 基于多种策略生成推荐
4. 前端展示推荐结果

### Q4: 智能提示如何触发？

**A**：
- 用户输入超过 3 个字符
- 输入停顿 300ms 后自动触发
- 包含特定法律关键词（劳动、合同、婚姻等）

---

**文档版本**：V3.0
**最后更新**：2025-01-27
