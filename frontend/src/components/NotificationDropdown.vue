<template>
  <div class="notif-wrapper" v-click-outside="() => open = false">
    <button class="bell-btn" @click="open = !open" :title="unreadCount > 0 ? `${unreadCount}条未读` : '通知'">
      <span class="bell-icon">🔔</span>
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
  padding: 4px 6px;
  border-radius: 6px;
  line-height: 1;
  transition: background 0.15s;
}

.bell-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.bell-icon {
  font-size: 18px;
}

.badge {
  position: absolute;
  top: -2px;
  right: -4px;
  background: #f56c6c;
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
  width: 320px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  z-index: 200;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
}

.read-all-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: #4fc3f7;
  padding: 0;
}

.read-all-btn:hover {
  color: #0ea5e9;
}

.notif-list {
  max-height: 360px;
  overflow-y: auto;
}

.empty-tip {
  text-align: center;
  color: #aaa;
  font-size: 13px;
  padding: 32px 0;
}

.notif-item {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f8f8f8;
  transition: background 0.15s;
}

.notif-item:hover {
  background: #f9fafb;
}

.notif-item.unread {
  background: #f0f9ff;
}

.notif-dot {
  width: 8px;
  height: 8px;
  background: #4fc3f7;
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
  color: #222;
  margin-bottom: 2px;
}

.notif-content {
  font-size: 12px;
  color: #666;
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notif-time {
  font-size: 11px;
  color: #aaa;
  margin-top: 4px;
}

.dropdown-enter-active, .dropdown-leave-active {
  transition: all 0.15s ease;
}
.dropdown-enter-from, .dropdown-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
