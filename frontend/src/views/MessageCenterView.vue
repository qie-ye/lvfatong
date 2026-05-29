<template>
  <div class="message-center-container">
    <div class="page-header">
      <h2 class="page-title">消息中心</h2>
      <div class="header-actions">
        <el-badge :value="unreadCount" :hidden="unreadCount === 0">
          <el-button @click="activeTab = 'notifications'">
            <el-icon><Bell /></el-icon>
            通知
          </el-button>
        </el-badge>
        <el-button type="primary" @click="handleMarkAllRead" :loading="markingAllRead">
          全部已读
        </el-button>
      </div>
    </div>

    <!-- 标签页 -->
    <el-tabs v-model="activeTab" class="message-tabs">
      <!-- 通知标签页 -->
      <el-tab-pane label="通知" name="notifications">
        <div class="notifications-list" v-loading="loadingNotifications">
          <div v-for="notification in notifications" :key="notification.id" 
               class="notification-item" :class="{ unread: !notification.isRead }"
               @click="handleNotificationClick(notification)">
            <div class="notification-icon" :class="getNotificationTypeClass(notification.type)">
              <el-icon><component :is="getNotificationIcon(notification.type)" /></el-icon>
            </div>
            <div class="notification-content">
              <div class="notification-title">{{ notification.title }}</div>
              <div class="notification-text">{{ notification.content }}</div>
              <div class="notification-time">{{ formatTime(notification.createdAt) }}</div>
            </div>
            <div class="notification-status">
              <el-tag v-if="!notification.isRead" type="danger" size="small">未读</el-tag>
            </div>
          </div>
          <el-empty v-if="!loadingNotifications && notifications.length === 0" description="暂无通知" />
        </div>
      </el-tab-pane>

      <!-- 团队消息标签页 -->
      <el-tab-pane label="团队消息" name="team">
        <div class="team-messages">
          <div class="team-selector">
            <el-select v-model="selectedTeam" placeholder="选择团队" @change="loadTeamMessages">
              <el-option v-for="team in teams" :key="team.id" :label="team.name" :value="team.id" />
            </el-select>
          </div>
          <div class="messages-list" v-loading="loadingMessages">
            <div v-for="message in teamMessages" :key="message.id" class="message-item">
              <div class="message-avatar">
                <el-avatar :size="36">{{ message.senderId }}</el-avatar>
              </div>
              <div class="message-content">
                <div class="message-header">
                  <span class="message-sender">用户 {{ message.senderId }}</span>
                  <span class="message-time">{{ formatTime(message.createdAt) }}</span>
                </div>
                <div class="message-text">{{ message.content }}</div>
              </div>
            </div>
            <el-empty v-if="!loadingMessages && teamMessages.length === 0" description="暂无消息" />
          </div>
          <div class="message-input" v-if="selectedTeam">
            <el-input v-model="newMessage" placeholder="输入消息..." @keyup.enter="handleSendMessage">
              <template #append>
                <el-button @click="handleSendMessage" :loading="sendingMessage">发送</el-button>
              </template>
            </el-input>
          </div>
        </div>
      </el-tab-pane>

      <!-- 私信标签页 -->
      <el-tab-pane label="私信" name="private">
        <div class="private-messages" v-loading="loadingPrivate">
          <div v-for="message in privateMessages" :key="message.id" class="message-item">
            <div class="message-avatar">
              <el-avatar :size="36">{{ message.senderId === currentUserId ? message.receiverId : message.senderId }}</el-avatar>
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="message-sender">
                  {{ message.senderId === currentUserId ? '我' : '用户 ' + message.senderId }}
                  →
                  {{ message.receiverId === currentUserId ? '我' : '用户 ' + message.receiverId }}
                </span>
                <span class="message-time">{{ formatTime(message.createdAt) }}</span>
              </div>
              <div class="message-text">{{ message.content }}</div>
            </div>
          </div>
          <el-empty v-if="!loadingPrivate && privateMessages.length === 0" description="暂无私信" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Bell, Message, User, Setting } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/api'

const currentUserId = ref(1) // 临时硬编码
const activeTab = ref('notifications')
const unreadCount = ref(0)
const markingAllRead = ref(false)

// 通知相关
const loadingNotifications = ref(false)
const notifications = ref<any[]>([])

// 团队消息相关
const loadingMessages = ref(false)
const teams = ref<any[]>([])
const selectedTeam = ref<number | null>(null)
const teamMessages = ref<any[]>([])
const newMessage = ref('')
const sendingMessage = ref(false)

// 私信相关
const loadingPrivate = ref(false)
const privateMessages = ref<any[]>([])

let refreshTimer: number | null = null

// 加载通知
async function loadNotifications() {
  loadingNotifications.value = true
  try {
    const res = await api.get('/notifications')
    notifications.value = res.data.content || []
  } catch (error) {
    console.error('加载通知失败:', error)
  } finally {
    loadingNotifications.value = false
  }
}

// 加载未读数量
async function loadUnreadCount() {
  try {
    const res = await api.get('/notifications/unread-count')
    unreadCount.value = res.data.count || 0
  } catch (error) {
    console.error('加载未读数量失败:', error)
  }
}

// 加载团队列表
async function loadTeams() {
  try {
    const res = await api.get('/teams')
    teams.value = res.data
    if (teams.value.length > 0) {
      selectedTeam.value = teams.value[0].id
      loadTeamMessages()
    }
  } catch (error) {
    console.error('加载团队列表失败:', error)
  }
}

// 加载团队消息
async function loadTeamMessages() {
  if (!selectedTeam.value) return
  
  loadingMessages.value = true
  try {
    const res = await api.get(`/messages/team/${selectedTeam.value}`)
    teamMessages.value = res.data.content || []
  } catch (error) {
    console.error('加载团队消息失败:', error)
  } finally {
    loadingMessages.value = false
  }
}

// 加载私信
async function loadPrivateMessages() {
  loadingPrivate.value = true
  try {
    const res = await api.get('/messages/private')
    privateMessages.value = res.data.content || []
  } catch (error) {
    console.error('加载私信失败:', error)
  } finally {
    loadingPrivate.value = false
  }
}

// 发送团队消息
async function handleSendMessage() {
  if (!newMessage.value.trim() || !selectedTeam.value) return
  
  sendingMessage.value = true
  try {
    await api.post(`/messages/team/${selectedTeam.value}`, { content: newMessage.value })
    newMessage.value = ''
    loadTeamMessages()
  } catch (error: any) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    sendingMessage.value = false
  }
}

// 点击通知
async function handleNotificationClick(notification: any) {
  if (!notification.isRead) {
    try {
      await api.post(`/notifications/${notification.id}/read`)
      notification.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
}

// 全部已读
async function handleMarkAllRead() {
  markingAllRead.value = true
  try {
    await api.post('/notifications/read-all')
    notifications.value.forEach(n => n.isRead = true)
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    markingAllRead.value = false
  }
}

// 获取通知类型样式
function getNotificationTypeClass(type: string) {
  const map: Record<string, string> = {
    'TASK_ASSIGNED': 'task',
    'TASK_STATUS_CHANGED': 'task',
    'TASK_COMMENT': 'comment',
    'TEAM_INVITE': 'team'
  }
  return map[type] || 'default'
}

// 获取通知图标
function getNotificationIcon(type: string) {
  const map: Record<string, any> = {
    'TASK_ASSIGNED': 'Setting',
    'TASK_STATUS_CHANGED': 'Setting',
    'TASK_COMMENT': 'Message',
    'TEAM_INVITE': 'User'
  }
  return map[type] || 'Bell'
}

// 格式化时间
function formatTime(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadNotifications()
  loadUnreadCount()
  loadTeams()
  loadPrivateMessages()
  
  // 定时刷新未读数量
  refreshTimer = window.setInterval(() => {
    loadUnreadCount()
  }, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.message-center-container {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.message-tabs {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  border-radius: 8px;
  background: #f9fafb;
  cursor: pointer;
  transition: all 0.2s;
}

.notification-item:hover {
  background: #f3f4f6;
}

.notification-item.unread {
  background: #eff6ff;
  border-left: 4px solid #3b82f6;
}

.notification-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notification-icon.task { background: #dbeafe; color: #3b82f6; }
.notification-icon.comment { background: #fef3c7; color: #f59e0b; }
.notification-icon.team { background: #d1fae5; color: #10b981; }
.notification-icon.default { background: #e5e7eb; color: #6b7280; }

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-weight: 600;
  color: #111827;
  margin-bottom: 4px;
}

.notification-text {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}

.notification-time {
  font-size: 12px;
  color: #9ca3af;
}

.notification-status {
  flex-shrink: 0;
}

.team-messages {
  display: flex;
  flex-direction: column;
  height: 500px;
}

.team-selector {
  margin-bottom: 16px;
}

.messages-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.message-item {
  display: flex;
  gap: 12px;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.message-sender {
  font-weight: 500;
  color: #374151;
}

.message-time {
  font-size: 12px;
  color: #9ca3af;
}

.message-text {
  color: #4b5563;
  font-size: 14px;
}

.message-input {
  margin-top: 16px;
}

.private-messages {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
}

html.dark .page-title { color: var(--text-primary); }
html.dark .message-tabs { background: var(--bg-card); }
html.dark .notification-item { background: var(--bg-secondary); }
html.dark .notification-item.unread { background: #1e3a5f; }
html.dark .notification-title { color: var(--text-primary); }
html.dark .message-content { background: var(--bg-card); }
html.dark .messages-list { background: var(--bg-secondary); }
</style>