# 律法通 前端项目结构文档

> 生成时间：2025-01-27
> 技术栈：Vue 3.4 / TypeScript / Pinia / Element Plus / Vite 5
> 状态：V2 当前版本（V3 开发前基线）

---

## 1. 项目结构树

```
frontend/src/
│
├── main.ts                                    # 应用入口
├── App.vue                                    # 根组件
├── env.d.ts                                   # 环境类型声明
├── auto-imports.d.ts                          # 自动导入类型
├── components.d.ts                            # 组件类型声明
│
├── api/                                       # API 封装层
│   └── index.ts                               # Axios 实例 + API 封装
│
├── assets/                                    # 静态资源
│   └── styles/                                # 样式文件
│       └── main.css                           # 主样式
│
├── components/                                # 通用组件（3个）
│   ├── DisclaimerBanner.vue                   # 免责声明横幅
│   ├── NotificationDropdown.vue               # 通知下拉
│   └── SpeechButton.vue                       # 语音按钮
│
├── composables/                               # 组合式函数
│   └── ...                                    # (待扩展)
│
├── router/                                    # 路由配置
│   └── index.ts                               # 路由定义 + 权限守卫
│
├── stores/                                    # 状态管理（8个）
│   ├── auth.ts                                # 认证状态
│   ├── chat.ts                                # 聊天状态
│   ├── contract.ts                            # 合同状态
│   ├── document.ts                            # 文书状态
│   ├── feedback.ts                            # 反馈状态
│   ├── lawyer.ts                              # 律师状态
│   ├── notification.ts                        # 通知状态
│   └── opinion.ts                             # 意见状态
│
├── styles/                                    # 全局样式
│   └── ...                                    # (待扩展)
│
├── utils/                                     # 工具函数
│   └── ...                                    # (待扩展)
│
└── views/                                     # 页面组件（14个）
    ├── AdminDashboardView.vue                 # 管理后台
    ├── CaseDetailView.vue                     # 案例详情
    ├── CaseSearchView.vue                     # 案例搜索
    ├── ChatView.vue                           # 智能问答（核心）
    ├── ContractView.vue                       # 合同分析
    ├── DocumentView.vue                       # 法律文书
    ├── FaqView.vue                            # 常见问题
    ├── HomeView.vue                           # 首页
    ├── LawSearchView.vue                      # 法条查询
    ├── LawyerDetailView.vue                   # 律师详情
    ├── LawyerListView.vue                     # 律师列表
    ├── LoginView.vue                          # 登录页
    ├── OpinionView.vue                        # 法律意见
    └── ProfileView.vue                        # 个人中心
```

---

## 2. 页面列表

| 页面 | 路由 | 组件 | 说明 | 权限 |
|------|------|------|------|------|
| 首页 | `/` | `HomeView.vue` | 产品介绍 | 公开 |
| 登录 | `/login` | `LoginView.vue` | 登录/注册 | 公开 |
| 智能问答 | `/chat` | `ChatView.vue` | AI 法律咨询 | 已登录 |
| 合同分析 | `/contract` | `ContractView.vue` | 合同上传分析 | 已登录 |
| 法条查询 | `/laws` | `LawSearchView.vue` | 法条检索 | 公开 |
| 常见问题 | `/faq` | `FaqView.vue` | FAQ 列表 | 公开 |
| 案例搜索 | `/cases` | `CaseSearchView.vue` | 案例检索 | 公开 |
| 案例详情 | `/cases/:id` | `CaseDetailView.vue` | 案例详情 | 公开 |
| 律师列表 | `/lawyers` | `LawyerListView.vue` | 律师列表 | 公开 |
| 律师详情 | `/lawyers/:id` | `LawyerDetailView.vue` | 律师详情 | 公开 |
| 法律意见 | `/opinion` | `OpinionView.vue` | 生成意见 | 已登录 |
| 法律文书 | `/document` | `DocumentView.vue` | 生成文书 | 已登录 |
| 个人中心 | `/profile` | `ProfileView.vue` | 用户信息 | 已登录 |
| 管理后台 | `/admin` | `AdminDashboardView.vue` | 后台管理 | 管理员 |

---

## 3. 路由配置

```typescript
// router/index.ts 核心路由
const routes = [
  { path: '/', component: HomeView },
  { path: '/login', component: LoginView },
  { path: '/chat', component: ChatView, meta: { requiresAuth: true } },
  { path: '/contract', component: ContractView, meta: { requiresAuth: true } },
  { path: '/laws', component: LawSearchView },
  { path: '/faq', component: FaqView },
  { path: '/cases', component: CaseSearchView },
  { path: '/cases/:id', component: CaseDetailView },
  { path: '/lawyers', component: LawyerListView },
  { path: '/lawyers/:id', component: LawyerDetailView },
  { path: '/opinion', component: OpinionView, meta: { requiresAuth: true } },
  { path: '/document', component: DocumentView, meta: { requiresAuth: true } },
  { path: '/profile', component: ProfileView, meta: { requiresAuth: true } },
  { path: '/admin', component: AdminDashboardView, meta: { requiresAuth: true, requiresAdmin: true } }
]
```

---

## 4. 状态管理（Pinia Stores）

### 4.1 auth.ts - 认证状态

| 状态 | 类型 | 说明 |
|------|------|------|
| `user` | `User \| null` | 当前用户信息 |
| `token` | `string \| null` | JWT Token |
| `isAuthenticated` | `boolean` | 是否已认证 |

| Actions | 说明 |
|---------|------|
| `login(credentials)` | 用户登录 |
| `register(data)` | 用户注册 |
| `logout()` | 退出登录 |
| `refreshToken()` | 刷新令牌 |
| `fetchProfile()` | 获取用户信息 |

### 4.2 chat.ts - 聊天状态

| 状态 | 类型 | 说明 |
|------|------|------|
| `sessions` | `Session[]` | 会话列表 |
| `currentSessionId` | `number \| null` | 当前会话ID |
| `messages` | `Message[]` | 消息列表 |
| `isStreaming` | `boolean` | 是否流式输出中 |
| `streamingContent` | `string` | 流式内容 |
| `contextUsage` | `object` | 上下文用量 |

| Actions | 说明 |
|---------|------|
| `createSession()` | 创建会话 |
| `selectSession(id)` | 选择会话 |
| `sendMessage(content)` | 发送消息 |
| `deleteSession(id)` | 删除会话 |
| `renameSession(id, title)` | 重命名会话 |

### 4.3 contract.ts - 合同状态

| 状态 | 类型 | 说明 |
|------|------|------|
| `contracts` | `Contract[]` | 合同列表 |
| `currentContract` | `Contract \| null` | 当前合同 |
| `analysisResult` | `AnalysisResult \| null` | 分析结果 |
| `isAnalyzing` | `boolean` | 是否分析中 |

| Actions | 说明 |
|---------|------|
| `uploadContract(file)` | 上传合同 |
| `analyzeContract(id)` | 分析合同 |
| `fetchContracts()` | 获取合同列表 |
| `fetchAnalysis(id)` | 获取分析结果 |

### 4.4 feedback.ts - 反馈状态

| 状态 | 类型 | 说明 |
|------|------|------|
| `ratings` | `Map<string, string>` | 评分记录 |

| Actions | 说明 |
|---------|------|
| `submitFeedback(sessionId, messageIndex, rating)` | 提交反馈 |
| `hasRating(sessionId, messageIndex)` | 是否已评分 |
| `getRating(sessionId, messageIndex)` | 获取评分 |

### 4.5 lawyer.ts - 律师状态

| Actions | 说明 |
|---------|------|
| `fetchLawyers(params)` | 获取律师列表 |
| `fetchLawyerDetail(id)` | 获取律师详情 |
| `searchLawyers(keyword)` | 搜索律师 |

### 4.6 notification.ts - 通知状态

| 状态 | 类型 | 说明 |
|------|------|------|
| `notifications` | `Notification[]` | 通知列表 |
| `unreadCount` | `number` | 未读数量 |

### 4.7 opinion.ts - 意见状态

| Actions | 说明 |
|---------|------|
| `generateOpinion(data)` | 生成法律意见 |
| `fetchOpinions()` | 获取意见列表 |
| `fetchOpinionDetail(id)` | 获取意见详情 |

### 4.8 document.ts - 文书状态

| Actions | 说明 |
|---------|------|
| `generateDocument(data)` | 生成法律文书 |
| `fetchDocuments()` | 获取文书列表 |
| `fetchDocumentDetail(id)` | 获取文书详情 |

---

## 5. API 调用列表

### 5.1 认证相关

```typescript
// api/index.ts
auth.login(credentials)          // POST /api/auth/login
auth.register(data)              // POST /api/auth/register
auth.refresh(refreshToken)       // POST /api/auth/refresh
auth.logout()                    // POST /api/auth/logout
```

### 5.2 聊天相关

```typescript
legal.createSession()            // POST /api/legal/session
legal.getSessions()              // GET /api/legal/sessions
legal.getMessages(sessionId)     // GET /api/legal/sessions/{id}/messages
legal.chat(sessionId, content)   // POST /api/legal/chat (SSE)
legal.renameSession(id, title)   // PUT /api/legal/sessions/{id}
legal.deleteSession(id)          // DELETE /api/legal/sessions/{id}
```

### 5.3 合同相关

```typescript
contracts.upload(file)           // POST /api/contracts/upload
contracts.getDetail(id)          // GET /api/contracts/{id}
contracts.getAnalysis(id)        // GET /api/contracts/{id}/analysis
contracts.analyze(id)            // POST /api/contracts/{id}/analyze
```

### 5.4 知识库相关

```typescript
knowledge.searchLaws(query)      // GET /api/knowledge/laws
knowledge.getLawDetail(id)       // GET /api/knowledge/laws/{id}
knowledge.getFaqList()           // GET /api/knowledge/faq
knowledge.searchFaq(query)       // GET /api/knowledge/faq/search
```

### 5.5 案例相关

```typescript
cases.search(query)              // GET /api/cases/search
cases.getDetail(id)              // GET /api/cases/{id}
```

### 5.6 律师相关

```typescript
lawyers.getList(params)           // GET /api/lawyers
lawyers.getDetail(id)            // GET /api/lawyers/{id}
lawyers.search(keyword)          // GET /api/lawyers/search
lawyers.addReview(id, data)      // POST /api/lawyers/{id}/reviews
```

### 5.7 意见/文书相关

```typescript
opinions.generate(data)          // POST /api/opinions/generate
opinions.getList()               // GET /api/opinions
opinions.getDetail(id)           // GET /api/opinions/{id}

documents.generate(data)         // POST /api/documents/generate
documents.getList()              // GET /api/documents
documents.getDetail(id)          // GET /api/documents/{id}
```

### 5.8 用户相关

```typescript
users.getProfile()               // GET /api/users/profile
users.updateProfile(data)        // PUT /api/users/profile
users.changePassword(data)       // PUT /api/users/password
```

### 5.9 反馈相关

```typescript
feedback.submit(data)            // POST /api/feedback
```

---

## 6. 组件列表

### 6.1 通用组件

| 组件 | 文件 | 说明 |
|------|------|------|
| `DisclaimerBanner` | `DisclaimerBanner.vue` | 免责声明横幅，显示在聊天页面顶部 |
| `NotificationDropdown` | `NotificationDropdown.vue` | 通知下拉菜单，显示在导航栏 |
| `SpeechButton` | `SpeechButton.vue` | 语音输入按钮，集成讯飞语音识别 |

### 6.2 页面组件核心功能

| 页面 | 核心功能 |
|------|----------|
| `ChatView` | SSE 流式聊天、会话管理、Markdown 渲染、反馈按钮、上下文指示器 |
| `ContractView` | 文件上传、分析进度、结果展示、高风险高亮 |
| `LawSearchView` | 关键词搜索、分类筛选、法条详情 |
| `CaseSearchView` | 案例搜索、列表展示、详情跳转 |
| `OpinionView` | 事实输入、领域选择、意见生成、Markdown 渲染 |
| `DocumentView` | 模板选择、文书生成、内容编辑 |
| `AdminDashboardView` | 数据概览、用户管理、审计日志、统计图表 |

---

## 7. 样式架构

```
styles/
├── variables.css                 # CSS 变量（颜色、间距、字体）
├── base.css                      # 基础样式重置
├── components.css                # 组件样式
└── pages/                        # 页面样式
    ├── chat.css
    ├── contract.css
    └── ...
```

### 7.1 设计规范

- 主色调：`#2c6cdf`（律法蓝）
- 背景色：`#f5f7fa`
- 文字色：`#333` / `#666` / `#999`
- 圆角：`8px`
- 阴影：`0 2px 12px rgba(0, 0, 0, 0.1)`

---

## 8. 技术栈总结

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue | 3.4 |
| 构建 | Vite | 5.x |
| 语言 | TypeScript | 5.x |
| 状态管理 | Pinia | 2.x |
| UI 组件 | Element Plus | 2.x |
| 路由 | Vue Router | 4.x |
| HTTP | Axios | 1.x |
| 图表 | ECharts | 5.x |
| Markdown | marked | - |

---

**文档状态**：✅ V2 基线版本
**下一步**：V3 开发，新增组件将按阶段添加
