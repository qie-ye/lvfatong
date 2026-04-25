<template>
  <div class="chat-view">
    <el-container>
      <el-aside width="260px">
        <div class="session-header">
          <el-button type="primary" size="small" @click="handleNewSession" style="width: 100%">
            + 新建对话
          </el-button>
        </div>
        <div class="session-list">
          <div
            v-for="session in chatStore.sessions"
            :key="session.id"
            :class="['session-item', { active: session.id === chatStore.currentSessionId }]"
            @click="handleSelectSession(session.id)"
            @contextmenu.prevent="handleContextMenu($event, session)"
          >
            <span class="session-title">{{ session.title || '新对话' }}</span>
            <div class="session-actions" @click.stop>
              <el-dropdown trigger="click" size="small">
                <span class="action-btn">⋯</span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleRenameSession(session)">重命名</el-dropdown-item>
                    <el-dropdown-item @click="handleDeleteSession(session.id)" divided>
                      <span style="color: #f56c6c">删除</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
          <p v-if="chatStore.sessions.length === 0" class="placeholder-text">暂无对话</p>
        </div>
      </el-aside>
      <el-main>
        <div class="chat-area">
          <DisclaimerBanner />
          <div class="messages" ref="messagesRef">
            <div v-if="chatStore.messages.length === 0 && !chatStore.isStreaming" class="welcome-message">
              <h3>欢迎使用律法通法律咨询</h3>
              <p>请输入您的法律问题，我将为您提供专业解答</p>
            </div>
            <div
              v-for="(msg, idx) in chatStore.messages"
              :key="msg.id"
              :class="['message-row', msg.role === 'USER' ? 'user' : 'assistant']"
            >
              <div :class="['message-bubble', msg.role === 'USER' ? 'user-bubble' : 'assistant-bubble']">
                <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
                <div v-if="msg.role === 'ASSISTANT'" class="feedback-bar">
                  <button
                    :class="['feedback-btn', feedbackStore.getRating(msg.sessionId, idx) === 'GOOD' ? 'active-good' : '']"
                    :disabled="feedbackStore.hasRating(msg.sessionId, idx)"
                    @click="handleFeedback(msg.sessionId, idx, 'GOOD')"
                    title="有帮助"
                  >👍</button>
                  <button
                    :class="['feedback-btn', feedbackStore.getRating(msg.sessionId, idx) === 'BAD' ? 'active-bad' : '']"
                    :disabled="feedbackStore.hasRating(msg.sessionId, idx)"
                    @click="handleFeedback(msg.sessionId, idx, 'BAD')"
                    title="没帮助"
                  >👎</button>
                </div>
              </div>
            </div>
            <div v-if="chatStore.isStreaming" class="message-row assistant">
              <div class="message-bubble assistant-bubble">
                <div class="message-content" v-html="renderMarkdown(chatStore.streamingContent)"></div>
                <span class="cursor-blink">▌</span>
              </div>
            </div>
          </div>
          <div class="input-area">
            <el-button
              class="new-session-btn"
              size="small"
              :disabled="chatStore.isStreaming"
              @click="handleStartNewSession"
            >✨ 新会话</el-button>
            <div
              v-if="chatStore.contextUsage"
              class="context-indicator"
              @click="handleCompressContext"
            >
              <el-tooltip
                :content="contextTooltipText"
                placement="top"
              >
                <svg class="context-ring" width="32" height="32" viewBox="0 0 36 36">
                  <circle class="ring-bg" cx="18" cy="18" r="15.5" fill="none" stroke="#e4e7ed" stroke-width="3" />
                  <circle
                    class="ring-fill"
                    cx="18" cy="18" r="15.5"
                    fill="none"
                    :stroke="contextRingColor"
                    stroke-width="3"
                    :stroke-dasharray="contextRingDash"
                    stroke-dashoffset="0"
                    stroke-linecap="round"
                    transform="rotate(-90 18 18)"
                  />
                </svg>
                <span class="context-pct">{{ contextPctText }}</span>
              </el-tooltip>
            </div>
            <div class="input-box-wrapper">
              <el-input
                v-model="inputText"
                type="textarea"
                :autosize="{ minRows: 1, maxRows: 6 }"
                placeholder="请输入您的法律问题... (Enter发送，Ctrl+Enter换行)"
                :disabled="chatStore.isStreaming"
                resize="none"
                @keydown="handleInputKeydown"
              />
              <div class="input-actions">
                <SpeechButton :disabled="chatStore.isStreaming" @result="handleSpeechResult" />
                <el-button class="send-btn" type="primary" @click="handleSend" :loading="chatStore.isStreaming" :disabled="!inputText.trim() || chatStore.isStreaming">发送</el-button>
              </div>
            </div>
          </div>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useChatStore } from '@/stores/chat'
import type { ChatSession } from '@/stores/chat'
import { renderMarkdown } from '@/utils/renderMarkdown'
import { useFeedbackStore } from '@/stores/feedback'
import type { FeedbackRating } from '@/stores/feedback'
import DisclaimerBanner from '@/components/DisclaimerBanner.vue'
import SpeechButton from '@/components/SpeechButton.vue'

const chatStore = useChatStore()
const feedbackStore = useFeedbackStore()
const inputText = ref('')
const messagesRef = ref<HTMLElement>()

const circumference = 2 * Math.PI * 15.5  // ≈ 97.39

const contextRingColor = computed(() => {
  const ratio = chatStore.contextUsage?.usageRatio ?? 0
  if (ratio >= 0.9) return '#f56c6c'
  if (ratio >= 0.7) return '#e6a23c'
  if (ratio >= 0.5) return '#409eff'
  return '#67c23a'
})

const contextRingDash = computed(() => {
  const ratio = chatStore.contextUsage?.usageRatio ?? 0
  const filled = circumference * Math.min(ratio, 1)
  return `${filled} ${circumference}`
})

const contextPctText = computed(() => {
  const ratio = chatStore.contextUsage?.usageRatio ?? 0
  return Math.round(ratio * 100) + '%'
})

const contextTooltipText = computed(() => {
  const u = chatStore.contextUsage
  if (!u) return ''
  const usedK = Math.round(u.usedTokens / 1000)
  const totalK = Math.round(u.totalTokens / 1000)
  const remainK = totalK - usedK
  let tip = `上下文: ${usedK}K / ${totalK}K tokens，剩余约 ${remainK}K`
  if (u.needCompress) tip += '\n⚠️ 接近上限，点击压缩上下文'
  else tip += '\n点击可手动压缩'
  return tip
})

async function handleCompressContext() {
  if (!chatStore.contextUsage?.needCompress && chatStore.contextUsage?.usageRatio < 0.3) {
    ElMessage.info('上下文充裕，无需压缩')
    return
  }
  try {
    const ok = await chatStore.compressContext()
    if (ok) ElMessage.success('上下文已压缩，释放了部分空间')
    else ElMessage.info('当前无需压缩')
  } catch {
    ElMessage.error('压缩失败')
  }
}

onMounted(() => {
  chatStore.loadSessions()
})

watch(() => chatStore.messages.length, () => {
  nextTick(scrollToBottom)
})
watch(() => chatStore.streamingContent, () => {
  nextTick(scrollToBottom)
})

async function handleNewSession() {
  await chatStore.createSession()
}

async function handleStartNewSession() {
  try {
    await chatStore.startNewSession(chatStore.currentSessionId ?? undefined)
    ElMessage.success('已开启新会话，AI已记住您的偏好')
  } catch {
    ElMessage.error('创建新会话失败')
  }
}

async function handleSelectSession(sessionId: number) {
  await chatStore.loadMessages(sessionId)
  chatStore.loadContextUsage()
}

function handleSpeechResult(text: string) {
  inputText.value = text
}

function handleInputKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.ctrlKey && !e.shiftKey && !e.altKey) {
    e.preventDefault()
    handleSend()
  }
  // Ctrl+Enter / Shift+Enter: default textarea newline behavior (no preventDefault)
}

async function handleSend() {
  const question = inputText.value.trim()
  if (!question || chatStore.isStreaming) return
  inputText.value = ''

  if (!chatStore.currentSessionId) {
    await chatStore.createSession(question.length > 20 ? question.substring(0, 20) + '...' : question)
  }

  await chatStore.sendMessage(question)
}

async function handleDeleteSession(sessionId: number) {
  try {
    await ElMessageBox.confirm('确定要删除该对话吗？删除后不可恢复。', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await chatStore.deleteSession(sessionId)
    ElMessage.success('对话已删除')
  } catch {
    // cancelled
  }
}

async function handleRenameSession(session: ChatSession) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的对话标题', '重命名', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: session.title || '',
      inputPlaceholder: '输入对话标题'
    })
    if (value && value.trim()) {
      await chatStore.renameSession(session.id, value.trim())
      ElMessage.success('已重命名')
    }
  } catch {
    // cancelled
  }
}

function handleContextMenu(_event: MouseEvent, _session: ChatSession) {
  // Context menu handled by dropdown, this prevents default browser menu
}

function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

async function handleFeedback(sessionId: number, messageIndex: number, rating: FeedbackRating) {
  try {
    await feedbackStore.submitFeedback(sessionId, messageIndex, rating)
    ElMessage.success(rating === 'GOOD' ? '感谢您的好评！' : '感谢反馈，我们会继续改进')
  } catch {
    ElMessage.error('提交失败，请稍后重试')
  }
}

// renderMarkdown 已提取到 @/utils/renderMarkdown.ts
</script>

<style scoped>
.chat-view {
  height: calc(100vh - 60px);
}

.el-container {
  height: 100%;
}

.el-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  padding: 12px;
  overflow-y: auto;
}

.session-header {
  margin-bottom: 12px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
}

.session-item:hover {
  background: #f0f2f5;
}

.session-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.session-item:hover .session-actions {
  opacity: 1;
}

.action-btn {
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 16px;
  color: #999;
}

.action-btn:hover {
  background: #e4e7ed;
  color: #333;
}

.session-item.active {
  background: #e3f2fd;
}

.session-title {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}

.placeholder-text {
  color: #999;
  font-size: 13px;
  text-align: center;
  margin-top: 20px;
}

.chat-area {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f9fafb;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.welcome-message {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.welcome-message h3 {
  font-size: 20px;
  color: #333;
  margin-bottom: 8px;
}

.message-row {
  display: flex;
  margin-bottom: 16px;
}

.message-row.user {
  justify-content: flex-end;
}

.message-row.assistant {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 75%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.user-bubble {
  background: #1a1a2e;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.assistant-bubble {
  background: #fff;
  color: #333;
  border: 1px solid #e4e7ed;
  border-bottom-left-radius: 4px;
}

.message-content {
  white-space: pre-wrap;
  line-height: 1.8;
}

.message-content :deep(.md-h1),
.message-content :deep(.md-h2),
.message-content :deep(.md-h3) {
  font-weight: 700;
  margin: 14px 0 8px;
  color: #1f2d3d;
}

.message-content :deep(.md-h1) { font-size: 17px; }
.message-content :deep(.md-h2) { font-size: 16px; }
.message-content :deep(.md-h3) { font-size: 15px; }

.message-content :deep(.md-li) {
  margin: 6px 0;
  padding-left: 2px;
}

.message-content :deep(.md-idx) {
  font-weight: 600;
}

.message-content :deep(.md-bold-line) {
  margin: 10px 0 4px;
}

.message-content :deep(.md-intro) {
  margin-bottom: 8px;
}

.message-content :deep(.md-section) {
  margin: 14px 0;
  border: 1px solid #e7ecf3;
  border-radius: 10px;
  overflow: hidden;
  background: #fafcff;
}

.message-content :deep(.md-section-title) {
  padding: 10px 14px;
  font-weight: 700;
  color: #1f2d3d;
  background: #eef4ff;
  border-bottom: 1px solid #e7ecf3;
}

.message-content :deep(.md-section-body) {
  padding: 14px;
}

.cursor-blink {
  animation: blink 1s step-end infinite;
  color: #4fc3f7;
}

@keyframes blink {
  50% { opacity: 0; }
}

.input-area {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
  flex-wrap: wrap;
}

.input-box-wrapper {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-box-wrapper :deep(.el-textarea__inner) {
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.6;
  border-radius: 8px;
  box-shadow: 0 0 0 1px var(--el-border-color) inset;
  transition: box-shadow 0.2s;
}

.input-box-wrapper :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--el-color-primary) inset;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.send-btn {
  flex-shrink: 0;
}

.context-indicator {
  position: relative;
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  transition: transform 0.2s;
}

.context-indicator:hover {
  transform: scale(1.15);
}

.context-ring {
  position: absolute;
  top: 2px;
  left: 2px;
}

.ring-fill {
  transition: stroke-dasharray 0.6s ease, stroke 0.4s ease;
}

.context-pct {
  position: relative;
  z-index: 1;
  font-size: 9px;
  font-weight: 700;
  color: #606266;
  line-height: 1;
  pointer-events: none;
}

.new-session-btn {
  flex-shrink: 0;
  white-space: nowrap;
}

.feedback-bar {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  padding-top: 6px;
  border-top: 1px solid #f0f0f0;
}

.feedback-btn {
  background: none;
  border: 1px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  padding: 2px 8px;
  transition: all 0.15s;
  opacity: 0.55;
}

.feedback-btn:hover:not(:disabled) {
  opacity: 1;
  background: #f5f7fa;
  border-color: #e4e7ed;
}

.feedback-btn:disabled {
  cursor: default;
}

.feedback-btn.active-good {
  opacity: 1;
  background: #f0fdf4;
  border-color: #86efac;
}

.feedback-btn.active-bad {
  opacity: 1;
  background: #fff1f2;
  border-color: #fca5a5;
}
</style>
