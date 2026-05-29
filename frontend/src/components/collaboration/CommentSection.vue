<template>
  <div class="comment-section">
    <h4 class="section-title">评论 ({{ comments.length }})</h4>
    
    <!-- 评论输入 -->
    <div class="comment-input">
      <el-input
        v-model="newComment"
        type="textarea"
        :rows="3"
        placeholder="添加评论..."
        maxlength="1000"
        show-word-limit
      />
      <div class="input-actions">
        <el-button type="primary" @click="handleSubmitComment" :loading="submitting" :disabled="!newComment.trim()">
          发送
        </el-button>
      </div>
    </div>

    <!-- 评论列表 -->
    <div class="comments-list" v-loading="loading">
      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="comment-avatar">
          <el-avatar :size="36">{{ getUserName(comment.userId)?.charAt(0) || 'U' }}</el-avatar>
        </div>
        <div class="comment-body">
          <div class="comment-header">
            <span class="comment-author">{{ getUserName(comment.userId) }}</span>
            <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
          </div>
          <div class="comment-content">{{ comment.content }}</div>
          <div class="comment-actions">
            <el-button text size="small" @click="showReplyInput(comment.id)">回复</el-button>
            <el-button v-if="comment.userId === currentUserId" text size="small" type="danger" @click="handleDeleteComment(comment.id)">删除</el-button>
          </div>

          <!-- 回复输入框 -->
          <div v-if="replyingTo === comment.id" class="reply-input">
            <el-input
              v-model="replyContent"
              type="textarea"
              :rows="2"
              placeholder="回复..."
              maxlength="500"
            />
            <div class="reply-actions">
              <el-button size="small" @click="cancelReply">取消</el-button>
              <el-button size="small" type="primary" @click="handleSubmitReply(comment.id)" :loading="submittingReply">
                回复
              </el-button>
            </div>
          </div>

          <!-- 回复列表 -->
          <div v-if="comment.replies && comment.replies.length > 0" class="replies-list">
            <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
              <div class="reply-avatar">
                <el-avatar :size="24">{{ getUserName(reply.userId)?.charAt(0) || 'U' }}</el-avatar>
              </div>
              <div class="reply-body">
                <div class="reply-header">
                  <span class="reply-author">{{ getUserName(reply.userId) }}</span>
                  <span class="reply-time">{{ formatTime(reply.createdAt) }}</span>
                </div>
                <div class="reply-content">{{ reply.content }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && comments.length === 0" description="暂无评论" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const props = defineProps<{
  targetType: 'CASE' | 'TASK' | 'DOCUMENT'
  targetId: number
}>()

const currentUserId = ref(1) // 临时硬编码
const loading = ref(false)
const submitting = ref(false)
const submittingReply = ref(false)
const comments = ref<any[]>([])
const newComment = ref('')
const replyContent = ref('')
const replyingTo = ref<number | null>(null)

// 用户名映射（简化实现）
const userNames: Record<number, string> = {}

function getUserName(userId: number) {
  return userNames[userId] || `用户${userId}`
}

// 加载评论
async function loadComments() {
  loading.value = true
  try {
    const res = await api.get('/comments', {
      params: {
        targetType: props.targetType,
        targetId: props.targetId,
        all: true
      }
    })
    comments.value = res.data || []
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    loading.value = false
  }
}

// 提交评论
async function handleSubmitComment() {
  if (!newComment.value.trim()) return

  submitting.value = true
  try {
    await api.post('/comments', {
      targetType: props.targetType,
      targetId: props.targetId,
      content: newComment.value
    })
    newComment.value = ''
    ElMessage.success('评论已添加')
    loadComments()
  } catch (error: any) {
    ElMessage.error(error.message || '评论失败')
  } finally {
    submitting.value = false
  }
}

// 显示回复输入框
function showReplyInput(commentId: number) {
  replyingTo.value = commentId
  replyContent.value = ''
}

// 取消回复
function cancelReply() {
  replyingTo.value = null
  replyContent.value = ''
}

// 提交回复
async function handleSubmitReply(parentId: number) {
  if (!replyContent.value.trim()) return

  submittingReply.value = true
  try {
    await api.post('/comments', {
      targetType: props.targetType,
      targetId: props.targetId,
      content: replyContent.value,
      parentId
    })
    replyContent.value = ''
    replyingTo.value = null
    ElMessage.success('回复已添加')
    loadComments()
  } catch (error: any) {
    ElMessage.error(error.message || '回复失败')
  } finally {
    submittingReply.value = false
  }
}

// 删除评论
async function handleDeleteComment(commentId: number) {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '确认删除', { type: 'warning' })
    await api.delete(`/comments/${commentId}`)
    ElMessage.success('评论已删除')
    loadComments()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
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

onMounted(loadComments)
</script>

<style scoped>
.comment-section {
  margin-top: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 16px;
}

.comment-input {
  margin-bottom: 24px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-author {
  font-weight: 500;
  color: #374151;
}

.comment-time {
  font-size: 12px;
  color: #9ca3af;
}

.comment-content {
  color: #4b5563;
  margin-bottom: 8px;
  line-height: 1.5;
}

.comment-actions {
  display: flex;
  gap: 8px;
}

.reply-input {
  margin-top: 12px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.replies-list {
  margin-top: 12px;
  padding-left: 20px;
  border-left: 2px solid #e5e7eb;
}

.reply-item {
  display: flex;
  gap: 8px;
  padding: 8px 0;
}

.reply-avatar {
  flex-shrink: 0;
}

.reply-body {
  flex: 1;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.reply-author {
  font-weight: 500;
  font-size: 13px;
  color: #374151;
}

.reply-time {
  font-size: 11px;
  color: #9ca3af;
}

.reply-content {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.4;
}

html.dark .section-title { color: var(--text-primary); }
html.dark .comment-author { color: var(--text-primary); }
html.dark .comment-content { color: var(--text-secondary); }
html.dark .reply-input { background: var(--bg-secondary); }
html.dark .reply-author { color: var(--text-primary); }
html.dark .reply-content { color: var(--text-secondary); }
</style>