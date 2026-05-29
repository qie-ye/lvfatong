<template>
  <div class="task-board-container">
    <div class="board-header">
      <h2 class="board-title">任务看板</h2>
      <div class="board-actions">
        <el-select v-model="selectedTeam" placeholder="选择团队" @change="loadKanbanData">
          <el-option v-for="team in teams" :key="team.id" :label="team.name" :value="team.id" />
        </el-select>
        <el-button type="primary" @click="showCreateDialog = true" :disabled="!selectedTeam">
          <el-icon><Plus /></el-icon>
          新建任务
        </el-button>
      </div>
    </div>

    <!-- 看板 -->
    <div class="kanban-board" v-loading="loading">
      <!-- 待办 -->
      <div class="kanban-column">
        <div class="column-header todo">
          <span class="column-title">待办</span>
          <el-badge :value="kanbanData.TODO?.length || 0" class="column-badge" />
        </div>
        <div class="column-body">
          <div v-for="task in kanbanData.TODO" :key="task.id" class="task-card" @click="openTaskDetail(task)">
            <div class="task-priority" :class="task.priority?.toLowerCase()"></div>
            <div class="task-title">{{ task.title }}</div>
            <div class="task-meta">
              <span class="task-due" v-if="task.dueDate">
                <el-icon><Calendar /></el-icon>
                {{ task.dueDate }}
              </span>
              <el-avatar :size="24" v-if="task.assigneeId">{{ task.assigneeId }}</el-avatar>
            </div>
          </div>
          <div class="add-task-btn" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon>
            添加任务
          </div>
        </div>
      </div>

      <!-- 进行中 -->
      <div class="kanban-column">
        <div class="column-header in-progress">
          <span class="column-title">进行中</span>
          <el-badge :value="kanbanData.IN_PROGRESS?.length || 0" class="column-badge" />
        </div>
        <div class="column-body">
          <div v-for="task in kanbanData.IN_PROGRESS" :key="task.id" class="task-card" @click="openTaskDetail(task)">
            <div class="task-priority" :class="task.priority?.toLowerCase()"></div>
            <div class="task-title">{{ task.title }}</div>
            <div class="task-meta">
              <span class="task-due" v-if="task.dueDate">
                <el-icon><Calendar /></el-icon>
                {{ task.dueDate }}
              </span>
              <el-avatar :size="24" v-if="task.assigneeId">{{ task.assigneeId }}</el-avatar>
            </div>
          </div>
        </div>
      </div>

      <!-- 审核中 -->
      <div class="kanban-column">
        <div class="column-header review">
          <span class="column-title">审核中</span>
          <el-badge :value="kanbanData.REVIEW?.length || 0" class="column-badge" />
        </div>
        <div class="column-body">
          <div v-for="task in kanbanData.REVIEW" :key="task.id" class="task-card" @click="openTaskDetail(task)">
            <div class="task-priority" :class="task.priority?.toLowerCase()"></div>
            <div class="task-title">{{ task.title }}</div>
            <div class="task-meta">
              <span class="task-due" v-if="task.dueDate">
                <el-icon><Calendar /></el-icon>
                {{ task.dueDate }}
              </span>
              <el-avatar :size="24" v-if="task.assigneeId">{{ task.assigneeId }}</el-avatar>
            </div>
          </div>
        </div>
      </div>

      <!-- 已完成 -->
      <div class="kanban-column">
        <div class="column-header done">
          <span class="column-title">已完成</span>
          <el-badge :value="kanbanData.DONE?.length || 0" class="column-badge" />
        </div>
        <div class="column-body">
          <div v-for="task in kanbanData.DONE" :key="task.id" class="task-card completed" @click="openTaskDetail(task)">
            <div class="task-priority" :class="task.priority?.toLowerCase()"></div>
            <div class="task-title">{{ task.title }}</div>
            <div class="task-meta">
              <span class="task-due" v-if="task.completedAt">
                <el-icon><Check /></el-icon>
                {{ formatDate(task.completedAt) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建任务对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建任务" width="600px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="任务标题" required>
          <el-input v-model="createForm.title" placeholder="请输入任务标题" />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="请输入任务描述" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="createForm.assigneeId" placeholder="选择负责人" clearable>
            <el-option v-for="member in members" :key="member.userId" 
                       :label="member.nickname || member.username" :value="member.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="createForm.priority" placeholder="选择优先级">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
            <el-option label="紧急" value="URGENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="createForm.dueDate" type="date" placeholder="选择截止日期" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateTask" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <!-- 任务详情对话框 -->
    <el-dialog v-model="showDetailDialog" title="任务详情" width="700px">
      <div v-if="selectedTask" class="task-detail">
        <div class="task-header">
          <h3>{{ selectedTask.title }}</h3>
          <div class="task-actions">
            <el-dropdown @command="handleStatusChange">
              <el-button size="small">
                {{ getStatusName(selectedTask.status) }}
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="TODO">待办</el-dropdown-item>
                  <el-dropdown-item command="IN_PROGRESS">进行中</el-dropdown-item>
                  <el-dropdown-item command="REVIEW">审核中</el-dropdown-item>
                  <el-dropdown-item command="DONE">已完成</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button size="small" type="danger" @click="handleDeleteTask">删除</el-button>
          </div>
        </div>
        <div class="task-info">
          <p><strong>描述：</strong>{{ selectedTask.description || '暂无描述' }}</p>
          <p><strong>优先级：</strong><el-tag :type="getPriorityType(selectedTask.priority)" size="small">{{ getPriorityName(selectedTask.priority) }}</el-tag></p>
          <p><strong>截止日期：</strong>{{ selectedTask.dueDate || '未设置' }}</p>
        </div>

        <!-- 评论区 -->
        <div class="comments-section">
          <h4>评论</h4>
          <div class="comment-input">
            <el-input v-model="newComment" type="textarea" :rows="2" placeholder="添加评论..." />
            <el-button type="primary" size="small" @click="handleAddComment" :loading="addingComment">发送</el-button>
          </div>
          <div class="comments-list">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <div class="comment-header">
                <span class="comment-user">用户 {{ comment.userId }}</span>
                <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
              </div>
              <div class="comment-content">{{ comment.content }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Calendar, Check, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const loading = ref(false)
const creating = ref(false)
const addingComment = ref(false)
const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const selectedTeam = ref<number | null>(null)
const teams = ref<any[]>([])
const members = ref<any[]>([])
const kanbanData = ref<any>({})
const selectedTask = ref<any>(null)
const comments = ref<any[]>([])
const newComment = ref('')

const createForm = ref({
  title: '',
  description: '',
  assigneeId: null,
  priority: 'MEDIUM',
  dueDate: null
})

// 加载团队列表
async function loadTeams() {
  try {
    const res = await api.get('/teams')
    teams.value = res.data
    if (teams.value.length > 0) {
      selectedTeam.value = teams.value[0].id
      loadKanbanData()
    }
  } catch (error) {
    console.error('加载团队列表失败:', error)
  }
}

// 加载看板数据
async function loadKanbanData() {
  if (!selectedTeam.value) return
  
  loading.value = true
  try {
    const [kanbanRes, membersRes] = await Promise.all([
      api.get(`/tasks/team/${selectedTeam.value}/kanban`),
      api.get(`/teams/${selectedTeam.value}/members`)
    ])
    kanbanData.value = kanbanRes.data
    members.value = membersRes.data
  } catch (error) {
    console.error('加载看板数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 创建任务
async function handleCreateTask() {
  if (!createForm.value.title) {
    ElMessage.warning('请输入任务标题')
    return
  }

  creating.value = true
  try {
    await api.post('/tasks', {
      teamId: selectedTeam.value,
      ...createForm.value
    })
    ElMessage.success('任务创建成功')
    showCreateDialog.value = false
    createForm.value = { title: '', description: '', assigneeId: null, priority: 'MEDIUM', dueDate: null }
    loadKanbanData()
  } catch (error: any) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    creating.value = false
  }
}

// 打开任务详情
async function openTaskDetail(task: any) {
  selectedTask.value = task
  showDetailDialog.value = true
  
  // 加载评论
  try {
    const res = await api.get(`/tasks/${task.id}/comments`)
    comments.value = res.data
  } catch (error) {
    console.error('加载评论失败:', error)
  }
}

// 修改任务状态
async function handleStatusChange(status: string) {
  try {
    await api.put(`/tasks/${selectedTask.value.id}/status`, { status })
    ElMessage.success('状态已更新')
    selectedTask.value.status = status
    loadKanbanData()
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败')
  }
}

// 删除任务
async function handleDeleteTask() {
  try {
    await ElMessageBox.confirm('确定要删除此任务吗？', '确认删除', { type: 'warning' })
    await api.delete(`/tasks/${selectedTask.value.id}`)
    ElMessage.success('任务已删除')
    showDetailDialog.value = false
    loadKanbanData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 添加评论
async function handleAddComment() {
  if (!newComment.value.trim()) return
  
  addingComment.value = true
  try {
    const res = await api.post(`/tasks/${selectedTask.value.id}/comments`, {
      content: newComment.value
    })
    comments.value.unshift(res.data)
    newComment.value = ''
    ElMessage.success('评论已添加')
  } catch (error: any) {
    ElMessage.error(error.message || '评论失败')
  } finally {
    addingComment.value = false
  }
}

// 格式化日期
function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

// 获取状态名称
function getStatusName(status: string) {
  const map: Record<string, string> = {
    'TODO': '待办',
    'IN_PROGRESS': '进行中',
    'REVIEW': '审核中',
    'DONE': '已完成'
  }
  return map[status] || status
}

// 获取优先级名称
function getPriorityName(priority: string) {
  const map: Record<string, string> = {
    'LOW': '低',
    'MEDIUM': '中',
    'HIGH': '高',
    'URGENT': '紧急'
  }
  return map[priority] || priority
}

// 获取优先级类型
function getPriorityType(priority: string) {
  const map: Record<string, string> = {
    'LOW': 'info',
    'MEDIUM': '',
    'HIGH': 'warning',
    'URGENT': 'danger'
  }
  return map[priority] || ''
}

onMounted(loadTeams)
</script>

<style scoped>
.task-board-container {
  padding: 24px;
  height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
}

.board-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.board-title {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
}

.board-actions {
  display: flex;
  gap: 12px;
}

.kanban-board {
  display: flex;
  gap: 16px;
  flex: 1;
  overflow-x: auto;
}

.kanban-column {
  min-width: 280px;
  flex: 1;
  background: #f3f4f6;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
}

.column-header {
  padding: 16px;
  border-radius: 12px 12px 0 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.column-header.todo { background: #dbeafe; }
.column-header.in-progress { background: #fef3c7; }
.column-header.review { background: #ede9fe; }
.column-header.done { background: #d1fae5; }

.column-title {
  font-weight: 600;
  color: #374151;
}

.column-body {
  padding: 12px;
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 4px solid transparent;
}

.task-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.task-card.completed {
  opacity: 0.7;
}

.task-priority {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-bottom: 8px;
}

.task-priority.low { background: #3b82f6; }
.task-priority.medium { background: #f59e0b; }
.task-priority.high { background: #f97316; }
.task-priority.urgent { background: #ef4444; }

.task-title {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 12px;
}

.task-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #9ca3af;
}

.task-due {
  display: flex;
  align-items: center;
  gap: 4px;
}

.add-task-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.2s;
}

.add-task-btn:hover {
  border-color: #6366f1;
  color: #6366f1;
}

.task-detail .task-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.task-detail .task-header h3 {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
}

.task-detail .task-actions {
  display: flex;
  gap: 8px;
}

.task-detail .task-info {
  margin-bottom: 24px;
}

.task-detail .task-info p {
  margin-bottom: 8px;
  color: #4b5563;
}

.comments-section {
  border-top: 1px solid #e5e7eb;
  padding-top: 20px;
}

.comments-section h4 {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 16px;
}

.comment-input {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.comment-input .el-input {
  flex: 1;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  background: #f9fafb;
  border-radius: 8px;
  padding: 12px;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.comment-user {
  font-weight: 500;
  color: #374151;
}

.comment-time {
  font-size: 12px;
  color: #9ca3af;
}

.comment-content {
  color: #4b5563;
  font-size: 14px;
}

@media (max-width: 768px) {
  .kanban-board {
    flex-direction: column;
  }
  
  .kanban-column {
    min-width: 100%;
  }
}

html.dark .board-title { color: var(--text-primary); }
html.dark .kanban-column { background: var(--bg-secondary); }
html.dark .task-card { background: var(--bg-card); }
html.dark .task-title { color: var(--text-primary); }
html.dark .comment-item { background: var(--bg-secondary); }
</style>