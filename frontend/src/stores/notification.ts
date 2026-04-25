import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { Client } from '@stomp/stompjs'
import api from '@/api'

export interface AppNotification {
  id: number
  type: string
  title: string
  content: string
  read: boolean
  createdAt: string
}

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<AppNotification[]>([])
  const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

  let stompClient: Client | null = null

  async function loadAll() {
    try {
      const res = await api.get('/notifications')
      notifications.value = res.data as AppNotification[]
    } catch {
      // silent fail
    }
  }

  function connect(token: string) {
    if (stompClient?.active) return

    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const host = window.location.host
    const brokerURL = `${protocol}://${host}/ws`

    stompClient = new Client({
      brokerURL,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        stompClient!.subscribe('/user/queue/notifications', (message) => {
          try {
            const n = JSON.parse(message.body) as AppNotification
            notifications.value.unshift(n)
          } catch {
            // ignore parse error
          }
        })
      }
    })
    stompClient.activate()
  }

  function disconnect() {
    stompClient?.deactivate()
    stompClient = null
    notifications.value = []
  }

  async function markRead(id: number) {
    await api.put(`/notifications/${id}/read`)
    const n = notifications.value.find(n => n.id === id)
    if (n) n.read = true
  }

  async function markAllRead() {
    await api.put('/notifications/read-all')
    notifications.value.forEach(n => (n.read = true))
  }

  return {
    notifications, unreadCount,
    loadAll, connect, disconnect, markRead, markAllRead
  }
})
