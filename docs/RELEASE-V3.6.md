# 律法通 V3.6 版本更新说明

> 发布日期：2026-05-28
> 版本代号：团队协作
> 状态：已发布

---

## 📋 版本概述

V3.6 是一个重要的功能增强版本，聚焦于**团队协作能力**的建设。本版本为律师团队提供了完整的协作工具链，包括团队管理、任务分配、消息通知和文件共享，大幅提升团队办案效率。

---

## ✨ 新增功能

### 1. 团队管理

| 功能 | 说明 |
|------|------|
| 创建团队 | 支持创建办案团队，设置名称、描述 |
| 邀请成员 | 通过手机号邀请成员加入团队 |
| 邀请码 | 生成邀请码，支持链接邀请 |
| 成员管理 | 查看成员列表、修改角色、移除成员 |
| 角色权限 | 所有者/管理员/成员三级权限控制 |
| 解散团队 | 团队创建者可解散团队 |

### 2. 任务看板

| 功能 | 说明 |
|------|------|
| 创建任务 | 创建任务并分配给团队成员 |
| 任务看板 | Kanban视图展示任务状态 |
| 状态流转 | 待办→进行中→审核→完成 |
| 优先级 | 低/中/高/紧急四级优先级 |
| 截止日期 | 设置任务截止时间 |
| 任务评论 | 支持任务评论和回复 |

### 3. 消息通知

| 功能 | 说明 |
|------|------|
| 团队消息 | 团队频道消息发送和接收 |
| 私信 | 用户之间私信 |
| 系统通知 | 任务分配、状态变更等系统通知 |
| 未读提醒 | 未读消息数量显示 |
| 消息已读 | 支持标记已读 |

### 4. 协作工具

| 功能 | 说明 |
|------|------|
| 案件评论 | 案件/任务评论功能 |
| 评论回复 | 支持评论回复 |
| 文件共享 | 团队文件上传、下载、删除 |
| 文件统计 | 文件数量和大小统计 |

---

## 📊 新增文件统计

### 后端文件（约40个）

```
src/main/java/com/lvatong/lft/
├── team/                          # 团队管理
│   ├── TeamService.java
│   ├── TeamMemberService.java
│   └── TeamInvitationService.java
├── collaboration/                 # 协作功能
│   ├── TaskService.java
│   ├── CaseCollaborationService.java
│   ├── CommentService.java
│   ├── TeamFileService.java
│   └── ActivityLogService.java
├── messaging/                     # 消息通知
│   ├── MessageService.java
│   └── NotificationService.java
├── model/entity/                  # 实体类
│   ├── Team.java
│   ├── TeamMember.java
│   ├── TeamInvitation.java
│   ├── Task.java
│   ├── TaskComment.java
│   ├── CaseCollaboration.java
│   ├── CaseActivityLog.java
│   ├── Comment.java
│   ├── Message.java
│   ├── MessageReadStatus.java
│   └── TeamFile.java
├── repository/                    # 数据访问层
│   ├── TeamRepository.java
│   ├── TeamMemberRepository.java
│   ├── TeamInvitationRepository.java
│   ├── TaskRepository.java
│   ├── TaskCommentRepository.java
│   ├── CaseCollaborationRepository.java
│   ├── CaseActivityLogRepository.java
│   ├── CommentRepository.java
│   ├── MessageRepository.java
│   ├── MessageReadStatusRepository.java
│   └── TeamFileRepository.java
└── controller/                    # API控制器
    ├── TeamController.java
    ├── TaskController.java
    ├── CollaborationController.java
    ├── MessageController.java
    ├── NotificationController.java
    ├── CommentController.java
    └── TeamFileController.java
```

### 前端文件（约15个）

```
frontend/src/
├── views/
│   ├── team/
│   │   ├── TeamListView.vue       # 团队列表
│   │   └── TeamDetailView.vue     # 团队详情
│   ├── collaboration/
│   │   └── TaskBoardView.vue      # 任务看板
│   └── MessageCenterView.vue      # 消息中心
├── components/
│   └── collaboration/
│       ├── CommentSection.vue     # 评论组件
│       └── FileManager.vue        # 文件管理组件
├── stores/
│   └── team.ts                    # 团队状态管理
└── router/
    └── index.ts                   # 路由更新
```

### 数据库迁移脚本（5个）

```
src/main/resources/db/migration/
├── V8__add_recommendation_tables.sql    # 推荐系统表
├── V9__add_performance_indexes.sql      # 性能优化索引
├── V10__add_dashboard_stats.sql         # Dashboard统计表
├── V11__add_team_tables.sql             # 团队相关表
├── V12__add_collaboration_tables.sql    # 协作相关表
├── V13__add_messaging_tables.sql        # 消息相关表
└── V14__add_comment_file_tables.sql     # 评论文件表
```

---

## 🗄️ 数据库变更

### 新增表（12个）

| 表名 | 说明 |
|------|------|
| `teams` | 团队表 |
| `team_members` | 团队成员表 |
| `team_invitations` | 团队邀请表 |
| `tasks` | 任务表 |
| `task_comments` | 任务评论表 |
| `case_collaborations` | 案件协作表 |
| `case_activity_logs` | 案件活动日志表 |
| `messages` | 消息表 |
| `message_read_status` | 消息已读状态表 |
| `comments` | 评论表 |
| `team_files` | 团队文件表 |
| `dashboard_stats` | Dashboard统计表 |

---

## 🔌 API接口列表

### 团队管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/teams` | 创建团队 |
| GET | `/api/v1/teams` | 获取我的团队 |
| GET | `/api/v1/teams/{id}` | 团队详情 |
| PUT | `/api/v1/teams/{id}` | 更新团队 |
| DELETE | `/api/v1/teams/{id}` | 解散团队 |
| POST | `/api/v1/teams/{id}/invite` | 邀请成员 |
| GET | `/api/v1/teams/{id}/members` | 成员列表 |
| PUT | `/api/v1/teams/{id}/members/{userId}/role` | 修改角色 |
| DELETE | `/api/v1/teams/{id}/members/{userId}` | 移除成员 |
| POST | `/api/v1/teams/{id}/leave` | 退出团队 |
| POST | `/api/v1/teams/join/{inviteCode}` | 邀请码加入 |

### 任务管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/tasks` | 创建任务 |
| GET | `/api/v1/tasks/{id}` | 任务详情 |
| GET | `/api/v1/tasks/team/{teamId}` | 团队任务列表 |
| GET | `/api/v1/tasks/team/{teamId}/kanban` | 看板数据 |
| PUT | `/api/v1/tasks/{id}` | 更新任务 |
| PUT | `/api/v1/tasks/{id}/status` | 更新状态 |
| DELETE | `/api/v1/tasks/{id}` | 删除任务 |

### 消息通知

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/messages/team/{teamId}` | 发送团队消息 |
| POST | `/api/v1/messages/private/{receiverId}` | 发送私信 |
| GET | `/api/v1/messages/team/{teamId}` | 团队消息列表 |
| GET | `/api/v1/messages/private` | 私信列表 |
| GET | `/api/v1/notifications` | 通知列表 |
| GET | `/api/v1/notifications/unread-count` | 未读数量 |
| POST | `/api/v1/notifications/{id}/read` | 标记已读 |
| POST | `/api/v1/notifications/read-all` | 全部已读 |

### 协作工具

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/comments` | 添加评论 |
| GET | `/api/v1/comments` | 评论列表 |
| PUT | `/api/v1/comments/{id}` | 编辑评论 |
| DELETE | `/api/v1/comments/{id}` | 删除评论 |
| POST | `/api/v1/teams/{teamId}/files` | 上传文件 |
| GET | `/api/v1/teams/{teamId}/files` | 文件列表 |
| DELETE | `/api/v1/teams/{teamId}/files/{fileId}` | 删除文件 |

### 案件协作

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/cases/{id}/share` | 共享案件 |
| DELETE | `/api/v1/cases/{id}/share/{teamId}` | 取消共享 |
| GET | `/api/v1/cases/{id}/collaborators` | 协作者列表 |
| GET | `/api/v1/cases/{id}/logs` | 案件日志 |

---

## 🔧 技术实现

### 后端技术

- **Spring Data JPA**：数据持久化
- **Spring Security**：权限控制
- **WebSocket**：实时消息（预留）
- **@Lazy**：解决循环依赖

### 前端技术

- **Vue 3 + TypeScript**：前端框架
- **Pinia**：状态管理
- **Element Plus**：UI组件
- **ECharts**：图表可视化

---

## ⚠️ 升级说明

### 数据库升级

1. 执行Flyway迁移脚本（V8-V14）
2. 新增12个数据库表
3. 无破坏性变更

### 配置变更

无需额外配置，V3.6功能开箱即用。

---

## 📝 后续规划

| 版本 | 功能 | 状态 |
|------|------|------|
| V3.7 | 本地模型部署 | 待开发 |
| V3.8 | 移动端适配 | 待开发 |
| V4.0 | 高级协作（实时协同编辑） | 规划中 |

---

## 🙏 致谢

感谢所有参与V3.6开发和测试的团队成员。

---

**律法通团队**
2026年5月28日