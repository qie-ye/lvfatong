import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

export interface ChatSession {
  id: number
  userId: number
  title: string
  type: string
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: number
  sessionId: number
  userId: number
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  createdAt: string
}

export interface ContextUsage {
  usedTokens: number
  totalTokens: number
  usageRatio: number
  needCompress: boolean
  messageCount: number
}

export const useChatStore = defineStore('chat', () => {
  const sessions = ref<ChatSession[]>([])
  const currentSessionId = ref<number | null>(null)
  const messages = ref<ChatMessage[]>([])
  const streamingContent = ref('')
  const isStreaming = ref(false)
  const contextUsage = ref<ContextUsage | null>(null)

  async function loadSessions() {
    try {
      const res = await api.get('/legal/sessions')
      sessions.value = (res.data as ChatSession[]) || []
    } catch {
      sessions.value = []
    }
  }

  async function createSession(title?: string) {
    const res = await api.post('/legal/sessions', null, { params: { title } })
    const session = res.data as ChatSession
    sessions.value.unshift(session)
    currentSessionId.value = session.id
    messages.value = []
    return session
  }

  async function loadMessages(sessionId: number) {
    currentSessionId.value = sessionId
    try {
      const res = await api.get(`/legal/sessions/${sessionId}/messages`)
      messages.value = (res.data as ChatMessage[]) || []
    } catch {
      messages.value = []
    }
  }

  async function sendMessage(question: string, docType?: string, lawDomain?: string) {
    if (!question.trim()) return

    // Add optimistic user message
    const userMsg: ChatMessage = {
      id: Date.now(),
      sessionId: currentSessionId.value!,
      userId: 0,
      role: 'USER',
      content: question,
      createdAt: new Date().toISOString()
    }
    messages.value.push(userMsg)
    streamingContent.value = ''
    isStreaming.value = true

    try {
      const token = localStorage.getItem('token')
      const controller = new AbortController()
      const timeout = setTimeout(() => controller.abort(), 90000)

      const response = await fetch('/api/v1/legal/chat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        signal: controller.signal,
        body: JSON.stringify({
          sessionId: currentSessionId.value,
          question,
          docType,
          lawDomain
        })
      })
      clearTimeout(timeout)

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const reader = response.body?.getReader()
      const decoder = new TextDecoder()

      if (!reader) throw new Error('No readable stream')

      let buffer = ''
      let streamDone = false
      while (true) {
        const { done, value } = await reader.read()
        if (done || streamDone) break
        buffer += decoder.decode(value, { stream: true })

        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          const trimmed = line.trim()
          if (trimmed.startsWith('event:')) continue  // skip event type lines
          if (trimmed.startsWith('data:')) {
            const data = trimmed.slice(5).trim()
            if (data === '[DONE]') {
              streamDone = true
              try {
                await reader.cancel()
              } catch {
                // ignore
              }
              break
            }
            streamingContent.value += data
          }
        }

        if (streamDone) break
      }

      // Add assistant message
      const assistantMsg: ChatMessage = {
        id: Date.now() + 1,
        sessionId: currentSessionId.value!,
        userId: 0,
        role: 'ASSISTANT',
        content: streamingContent.value,
        createdAt: new Date().toISOString()
      }
      messages.value.push(assistantMsg)

      // Update session title if first message
      if (sessions.value.length > 0 && currentSessionId.value) {
        const session = sessions.value.find(s => s.id === currentSessionId.value)
        if (session && (!session.title || session.title === '新对话')) {
          session.title = question.length > 20 ? question.substring(0, 20) + '...' : question
        }
      }
    } catch (e: any) {
      // Show error as assistant message instead of hanging forever
      const errMsg: ChatMessage = {
        id: Date.now() + 1,
        sessionId: currentSessionId.value!,
        userId: 0,
        role: 'ASSISTANT',
        content: `⚠️ 请求失败：${e.message || '网络错误'}，请重试`,
        createdAt: new Date().toISOString()
      }
      messages.value.push(errMsg)
    } finally {
      isStreaming.value = false
      streamingContent.value = ''
      loadContextUsage()
    }
  }

  async function deleteSession(sessionId: number) {
    await api.delete(`/legal/sessions/${sessionId}`)
    sessions.value = sessions.value.filter(s => s.id !== sessionId)
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
      messages.value = []
    }
  }

  async function renameSession(sessionId: number, title: string) {
    const res = await api.put(`/legal/sessions/${sessionId}`, null, { params: { title } })
    const updated = res.data as ChatSession
    const idx = sessions.value.findIndex(s => s.id === sessionId)
    if (idx >= 0) sessions.value[idx] = updated
  }

  async function endSession(sessionId: number) {
    await api.post(`/legal/sessions/${sessionId}/end`)
  }

  async function startNewSession(oldSessionId?: number, question?: string) {
    const res = await api.post('/legal/sessions/new', null, {
      params: { currentSessionId: oldSessionId || '', question: question || '' }
    })
    const session = res.data as ChatSession
    sessions.value.unshift(session)
    currentSessionId.value = session.id
    messages.value = []
    return session
  }

  async function loadContextUsage() {
    if (!currentSessionId.value) return
    try {
      const res = await api.get(`/legal/sessions/${currentSessionId.value}/context-usage`)
      contextUsage.value = res.data as ContextUsage
    } catch {
      contextUsage.value = null
    }
  }

  async function compressContext() {
    if (!currentSessionId.value) return false
    try {
      await api.post(`/legal/sessions/${currentSessionId.value}/compress`)
      await loadContextUsage()
      return true
    } catch {
      return false
    }
  }

  return {
    sessions, currentSessionId, messages, streamingContent, isStreaming, contextUsage,
    loadSessions, createSession, loadMessages, sendMessage, deleteSession, renameSession,
    endSession, startNewSession, loadContextUsage, compressContext
  }
})
