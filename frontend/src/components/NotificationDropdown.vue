<template>
  <div class="notif-wrapper" v-click-outside="() => open = false">
    <button class="bell-btn" @click="open = !open" :title="unreadCount > 0 ? `${unreadCount}条未读` : '通知'">
      <el-icon :size="18"><Bell /></el-icon>
      <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
    </button>

    <transition name="dropdown">
      <div v-if="open" class="dropdown-panel">
        <div class="panel-header">
          <span>通知中心</span>
          <button v-if="unreadCount > 0" class="read-all-btn" @click="handleMarkAllRead">全部已读</button>
        </div>

        <div class="notif-list">
          <div v-if="notifications.length === 0" class="empty-tip">暂无通知</div>
          <div
            v-for="n in notifications"
            :key="n.id"
            :class="['notif-item', { unread: !n.read }]"
            @click="handleRead(n)"
          >
            <div class="notif-dot" v-if="!n.read"></div>
            <div class="notif-body">
              <div class="notif-title">{{ n.title }}</div>
              <div class="notif-content">{{ n.content }}</div>
              <div class="notif-time">{{ formatTime(n.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'

const store = useNotificationStore()
const { notifications, unreadCount } = store

const open = ref(false)

const vClickOutside = {
  mounted(el: HTMLElement, binding: { value: () => void }) {
    (el as any)._clickOutsideHandler = (e: MouseEvent) => {
      if (!el.contains(e.target as Node)) binding.value()
    }
    document.addEventListener('click', (el as any)._clickOutsideHandler)
  },
  unmounted(el: HTMLElement) {
    document.removeEventListener('click', (el as any)._clickOutsideHandler)
  }
}

async function handleRead(n: { id: number; read: boolean }) {
  if (!n.read) await store.markRead(n.id)
}

async function handleMarkAllRead() {
  await store.markAllRead()
}

function formatTime(iso: string): string {
  if (!iso) return ''
  const d = new Date(iso)
  const now = new Date()
  const diffMs = now.getTime() - d.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1)   return '刚刚'
  if (diffMin < 60)  return `${diffMin}分钟前`
  const diffH = Math.floor(diffMin / 60)
  if (diffH < 24)    return `${diffH}小时前`
  return d.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.notif-wrapper {
  position: relative;
}

.bell-btn {
  background: none;
  border: none;
  cursor: pointer;
  position: relative;
  padding: 6px 8px;
  border-radius: 8px;
  color: #4b5563;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
}

.bell-btn:hover {
  background: #f3f4f6;
  color: #111827;
}

.badge {
  position: absolute;
  top: 0;
  right: 0;
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 3px;
  line-height: 1;
}

.dropdown-panel {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 340px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 10px 40px rgba(37, 99, 235, 0.08);
  z-index: 200;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  border-bottom: 1px solid #e5e7eb;
}

.read-all-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: #2563eb;
  padding: 2px 8px;
  border-radius: 4px;
  transition: all 0.15s;
}

.read-all-btn:hover {
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
}

.notif-list {
  max-height: 360px;
  overflow-y: auto;
}

.empty-tip {
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  padding: 40px 0;
}

.notif-item {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f3f4f6;
  transition: background 0.15s;
}

.notif-item:hover {
  background: #f8fafc;
}

.notif-item.unread {
  background: rgba(37, 99, 235, 0.04);
}

.notif-dot {
  width: 8px;
  height: 8px;
  background: #2563eb;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 5px;
}

.notif-body {
  flex: 1;
  min-width: 0;
}

.notif-title {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 2px;
}

.notif-content {
  font-size: 12px;
  color: #4b5563;
  line-height: 1.5;
}

.notif-time {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
}

.dropdown-enter-active, .dropdown-leave-active {
  transition: all 0.18s cubic-bezier(0.4, 0, 0.2, 1);
}

.dropdown-enter-from, .dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.97);
}
</style>
