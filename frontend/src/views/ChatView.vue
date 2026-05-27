<template>
  <div class="chat-view">
    <el-container>
      <el-aside width="260px">
        <div class="sidebar">
          <div class="sidebar-header">
            <el-button class="new-chat-btn" @click="handleNewSession">
              <el-icon :size="14"><Plus /></el-icon>
              新建对话
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
              <span class="session-dot"></span>
              <span class="session-title">{{ session.title || '新对话' }}</span>
              <div class="session-actions" @click.stop>
                <el-dropdown trigger="click" size="small">
                  <span class="action-trigger">⋯</span>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="handleRenameSession(session)">重命名</el-dropdown-item>
                      <el-dropdown-item @click="handleDeleteSession(session.id)" divided>
                        <span style="color: var(--color-danger)">删除</span>
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
            <div v-if="chatStore.sessions.length === 0 && !chatStore.isLoading" class="empty-sessions">
              <p>暂无对话记录</p>
            </div>
          </div>
        </div>
      </el-aside>
      <el-main>
        <div class="chat-area">
          <DisclaimerBanner />
          <div class="messages" ref="messagesRef">
            <div v-if="chatStore.messages.length === 0 && !chatStore.isStreaming" class="welcome">
              <div class="welcome-icon">
                <el-icon :size="24"><ChatDotRound /></el-icon>
              </div>
              <h2 class="welcome-title">法律咨询</h2>
              <p class="welcome-subtitle">输入您的法律问题，获取专业解答</p>
              <div class="quick-chips">
                <span v-for="q in quickQuestions" :key="q" class="quick-chip" @click="handleQuickQuestion(q)">{{ q }}</span>
              </div>
            </div>
            <transition-group name="msg" tag="div">
              <div
                v-for="(msg, idx) in chatStore.messages"
                :key="msg.id"
                :class="['message-row', msg.role === 'USER' ? 'user' : 'assistant']"
              >
                <div :class="['message-bubble', msg.role === 'USER' ? 'user-bubble' : 'assistant-bubble']">
                  <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
                  <div v-if="msg.role === 'ASSISTANT'" class="feedback-row">
                    <button
                      :class="['feedback-btn', feedbackStore.getRating(msg.sessionId, idx) === 'GOOD' ? 'active good' : '']"
                      :disabled="feedbackStore.hasRating(msg.sessionId, idx)"
                      @click="handleFeedback(msg.sessionId, idx, 'GOOD')"
                    >有帮助</button>
                    <button
                      :class="['feedback-btn', feedbackStore.getRating(msg.sessionId, idx) === 'BAD' ? 'active bad' : '']"
                      :disabled="feedbackStore.hasRating(msg.sessionId, idx)"
                      @click="handleFeedback(msg.sessionId, idx, 'BAD')"
                    >没帮助</button>
                  </div>
                </div>
              </div>
            </transition-group>
            <div v-if="chatStore.isStreaming" class="message-row assistant">
              <div class="message-bubble assistant-bubble streaming">
                <div class="message-content" v-html="renderMarkdown(chatStore.streamingContent)"></div>
                <span class="cursor-blink">▌</span>
              </div>
            </div>
          </div>
          <div class="input-area">
            <div class="input-wrapper">
              <el-input
                v-model="inputText"
                type="textarea"
                :autosize="{ minRows: 1, maxRows: 6 }"
                placeholder="描述您的法律问题，例如：劳动仲裁流程、合同违约处理..."
                :disabled="chatStore.isStreaming"
                resize="none"
                @keydown="handleInputKeydown"
              />
              <div class="input-footer">
                <div v-if="chatStore.contextUsage" class="context-indicator" @click="handleCompressContext">
                  <el-tooltip :content="contextTooltipText" placement="top">
                    <svg class="context-ring" width="28" height="28" viewBox="0 0 36 36">
                      <circle cx="18" cy="18" r="14" fill="none" stroke="var(--border)" stroke-width="3"/>
                      <circle cx="18" cy="18" r="14" fill="none" :stroke="contextRingColor" stroke-width="3" :stroke-dasharray="contextRingDash" stroke-dashoffset="0" stroke-linecap="round" transform="rotate(-90 18 18)"/>
                    </svg>
                    <span class="context-pct">{{ contextPctText }}</span>
                  </el-tooltip>
                </div>
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
import { Plus, ChatDotRound } from '@element-plus/icons-vue'

const chatStore = useChatStore()
const feedbackStore = useFeedbackStore()
const inputText = ref('')
const messagesRef = ref<HTMLElement>()

const quickQuestions = [
  '劳动合同纠纷怎么处理？',
  '婚姻财产如何分割？',
  '借贷合同违约如何处理？'
]

const circumference = 2 * Math.PI * 14

const contextRingColor = computed(() => {
  const ratio = chatStore.contextUsage?.usageRatio ?? 0
  if (ratio >= 0.9) return '#ef4444'
  if (ratio >= 0.7) return '#f59e0b'
  if (ratio >= 0.5) return '#3b82f6'
  return '#22c55e'
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
  if (!chatStore.contextUsage?.needCompress && (chatStore.contextUsage?.usageRatio ?? 1) < 0.3) {
    ElMessage.info('上下文充裕，无需压缩')
    return
  }
  try {
    const ok = await chatStore.compressContext()
    if (ok) ElMessage.success('上下文已压缩')
    else ElMessage.info('当前无需压缩')
  } catch {
    ElMessage.error('压缩失败')
  }
}

onMounted(() => { chatStore.loadSessions() })
watch(() => chatStore.messages.length, () => { nextTick(scrollToBottom) })
watch(() => chatStore.streamingContent, () => { nextTick(scrollToBottom) })

async function handleNewSession() { await chatStore.createSession() }

async function handleStartNewSession() {
  try {
    await chatStore.startNewSession(chatStore.currentSessionId ?? undefined)
    ElMessage.success('已开启新会话')
  } catch { ElMessage.error('创建新会话失败') }
}

async function handleSelectSession(sessionId: number) {
  await chatStore.loadMessages(sessionId)
  chatStore.loadContextUsage()
}

function handleSpeechResult(text: string) { inputText.value = text }

function handleQuickQuestion(q: string) {
  inputText.value = q
  nextTick(() => handleSend())
}

function handleInputKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.ctrlKey && !e.shiftKey && !e.altKey) {
    e.preventDefault()
    handleSend()
  }
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
    await ElMessageBox.confirm('确定要删除该对话吗？', '删除确认', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    await chatStore.deleteSession(sessionId)
    ElMessage.success('对话已删除')
  } catch {}
}

async function handleRenameSession(session: ChatSession) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的对话标题', '重命名', {
      confirmButtonText: '确定', cancelButtonText: '取消', inputValue: session.title || '', inputPlaceholder: '输入对话标题'
    })
    if (value && value.trim()) {
      await chatStore.renameSession(session.id, value.trim())
      ElMessage.success('已重命名')
    }
  } catch {}
}

function handleContextMenu(_event: MouseEvent, _session: ChatSession) {}

function scrollToBottom() {
  if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
}

async function handleFeedback(sessionId: number, messageIndex: number, rating: FeedbackRating) {
  try {
    await feedbackStore.submitFeedback(sessionId, messageIndex, rating)
    ElMessage.success(rating === 'GOOD' ? '感谢好评' : '感谢反馈')
  } catch { ElMessage.error('提交失败') }
}
</script>

<style scoped>
/* ======================================
   律法通 ChatView — 法大大风格
   Clean · Professional · Blue-tinted
   ====================================== */

/* ---------- LAYOUT ---------- */
.chat-view { height: calc(100vh - var(--header-height)); }
.el-container { height: 100%; }

/* ---------- SIDEBAR ---------- */
.el-aside {
  background: var(--bg-card);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px 12px;
}

.sidebar-header {
  flex-shrink: 0;
  margin-bottom: 12px;
}

.new-chat-btn {
  width: 100%;
  height: 38px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 500;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all var(--transition-fast);
}

.new-chat-btn:hover {
  background: var(--color-primary-50);
  border-color: var(--color-primary-400);
  color: var(--color-primary-600);
  box-shadow: var(--shadow-sm);
}

/* ---------- SESSION LIST ---------- */
.session-list {
  flex: 1;
  overflow-y: auto;
  margin-right: -4px;
  padding-right: 4px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 2px;
  transition: all var(--transition-fast);
  border-left: 2px solid transparent;
  position: relative;
}

.session-item:hover {
  background: var(--gray-100);
}

.session-item.active {
  border-left-color: var(--color-primary-600);
  background: var(--color-primary-50);
}

.session-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-tertiary);
  flex-shrink: 0;
  transition: background var(--transition-fast);
}

.session-item.active .session-dot {
  background: var(--color-primary-600);
}

.session-title {
  flex: 1;
  font-size: 13px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item.active .session-title {
  color: var(--text-primary);
  font-weight: 500;
}

.session-actions {
  opacity: 0;
  transition: opacity 0.15s;
  margin-left: auto;
  flex-shrink: 0;
}

.session-item:hover .session-actions {
  opacity: 1;
}

.action-trigger {
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 16px;
  color: var(--text-tertiary);
  transition: all var(--transition-fast);
}

.action-trigger:hover {
  background: var(--gray-200);
  color: var(--text-primary);
}

.empty-sessions {
  text-align: center;
  padding: 48px 12px;
  color: var(--text-tertiary);
  font-size: 13px;
}

/* ---------- CHAT AREA ---------- */
.chat-area {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg);
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

/* ---------- WELCOME ---------- */
.welcome {
  text-align: center;
  padding: 72px 20px 48px;
}

.welcome-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--color-primary-50);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: var(--color-primary-600);
}

.welcome-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 6px;
}

.welcome-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 24px;
}

.quick-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}

.quick-chip {
  padding: 8px 18px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-base);
  background: var(--bg-card);
}

.quick-chip:hover {
  border-color: var(--color-primary-500);
  color: var(--color-primary-600);
  background: var(--color-primary-50);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

/* ---------- MESSAGES ---------- */
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
  max-width: 72%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.75;
  word-break: break-word;
}

.user-bubble {
  background: var(--color-primary-50);
  color: var(--text-primary);
  border: 1px solid rgba(37, 99, 235, 0.12);
}

.assistant-bubble {
  background: var(--bg-card);
  color: var(--text-primary);
  border: 1px solid var(--border);
}

.assistant-bubble.streaming {
  border-color: rgba(37, 99, 235, 0.2);
}

.message-content {
  white-space: pre-wrap;
  line-height: 1.75;
}

.message-content :deep(.md-h1),
.message-content :deep(.md-h2),
.message-content :deep(.md-h3) {
  font-weight: 600;
  margin: 12px 0 6px;
  color: var(--text-primary);
}

.message-content :deep(.md-h1) { font-size: 16px; }
.message-content :deep(.md-h2) { font-size: 15px; }
.message-content :deep(.md-h3) { font-size: 14px; }

.message-content :deep(p) { margin: 0 0 6px; }
.message-content :deep(p:last-child) { margin-bottom: 0; }

.message-content :deep(ul), .message-content :deep(ol) {
  padding-left: 20px;
  margin: 6px 0;
}

.message-content :deep(li) { margin-bottom: 2px; }

.message-content :deep(strong) { font-weight: 600; }

.message-content :deep(code) {
  background: var(--gray-100);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  color: var(--color-primary-700);
}

.message-content :deep(pre) {
  background: var(--gray-50);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px 16px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.6;
}

.message-content :deep(pre code) {
  background: transparent;
  padding: 0;
  border-radius: 0;
  color: var(--text-primary);
}

.cursor-blink {
  animation: blink 1s step-end infinite;
  color: var(--color-primary-600);
}

@keyframes blink { 50% { opacity: 0; } }

/* ---------- FEEDBACK ---------- */
.feedback-row {
  display: flex;
  gap: 6px;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid var(--border);
}

.feedback-btn {
  background: none;
  border: 1px solid var(--border);
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  padding: 3px 12px;
  color: var(--text-tertiary);
  transition: all var(--transition-fast);
}

.feedback-btn:hover:not(:disabled) {
  background: var(--gray-50);
  color: var(--text-secondary);
  border-color: var(--gray-300);
}

.feedback-btn:disabled {
  cursor: default;
  opacity: 0.45;
}

.feedback-btn.active.good {
  color: var(--color-success);
  border-color: rgba(34, 197, 94, 0.25);
  background: rgba(34, 197, 94, 0.06);
}

.feedback-btn.active.bad {
  color: var(--color-danger);
  border-color: rgba(239, 68, 68, 0.25);
  background: rgba(239, 68, 68, 0.06);
}

/* ---------- INPUT AREA ---------- */
.input-area {
  padding: 14px 20px 16px;
  border-top: 1px solid var(--border);
  background: var(--bg-card);
}

.input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.input-wrapper :deep(.el-textarea__inner) {
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.6;
  border-radius: 8px;
  background: var(--bg);
  color: var(--text-primary);
  border: 1px solid var(--border);
  resize: none;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.input-wrapper :deep(.el-textarea__inner):focus {
  border-color: var(--color-primary-500);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08);
}

.input-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-end;
}

.send-btn {
  flex-shrink: 0;
  height: 32px;
  font-weight: 500;
  font-size: 13px;
  padding: 0 18px;
}

.send-btn:not(:disabled):hover {
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
}

.context-indicator {
  position: relative;
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
}

.context-ring {
  position: absolute;
  top: 0;
  left: 0;
}

.context-pct {
  font-size: 8px;
  font-weight: 700;
  color: var(--text-tertiary);
  line-height: 1;
  pointer-events: none;
}

/* ---------- TRANSITIONS ---------- */
.msg-enter-active { transition: all 0.3s ease-out; }
.msg-leave-active { transition: all 0.15s ease-in; }
.msg-enter-from { opacity: 0; transform: translateY(8px); }
.msg-leave-to { opacity: 0; }

/* ---------- RESPONSIVE ---------- */
@media (max-width: 768px) {
  .el-aside { display: none; }
  .messages { padding: 16px; }
  .message-bubble { max-width: 88%; }
  .welcome { padding: 48px 16px 32px; }
  .welcome-title { font-size: 20px; }
}

html.dark .session-item:hover { background: rgba(59,130,246,0.06); }
html.dark .session-item.active { background: rgba(59,130,246,0.1); border-left-color: #3b82f6; }
html.dark .session-item.active .session-dot { background: #3b82f6; }
html.dark .user-bubble { background: rgba(37,99,235,0.15); }
html.dark .assistant-bubble { background: var(--bg-card); border-color: var(--border); }
html.dark .input-area { background: var(--bg-card); }
html.dark .input-wrapper :deep(.el-textarea__inner) { background: var(--bg); border-color: var(--border); }
html.dark .quick-chip:hover { background: rgba(59,130,246,0.08); border-color: #3b82f6; color: var(--text-primary); }
html.dark .welcome-icon { background: rgba(59,130,246,0.1); }
html.dark .message-content :deep(code) { background: var(--border-light); }
html.dark .message-content :deep(pre) { background: var(--bg); }
html.dark .feedback-btn:hover:not(:disabled) { background: var(--bg); }
</style>
