<template>
  <div class="file-manager">
    <div class="file-header">
      <h4 class="section-title">团队文件</h4>
      <el-upload
        :action="uploadUrl"
        :headers="uploadHeaders"
        :data="uploadData"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :before-upload="beforeUpload"
        :show-file-list="false"
      >
        <el-button type="primary" size="small">
          <el-icon><Upload /></el-icon>
          上传文件
        </el-button>
      </el-upload>
    </div>

    <!-- 文件统计 -->
    <div class="file-stats" v-if="stats">
      <span>文件数量: {{ stats.fileCount }}</span>
      <span>总大小: {{ formatFileSize(stats.totalSize) }}</span>
    </div>

    <!-- 文件列表 -->
    <div class="file-list" v-loading="loading">
      <div v-for="file in files" :key="file.id" class="file-item">
        <div class="file-icon">
          <el-icon :size="24"><component :is="getFileIcon(file.fileType)" /></el-icon>
        </div>
        <div class="file-info">
          <div class="file-name">{{ file.name }}</div>
          <div class="file-meta">
            <span>{{ formatFileSize(file.fileSize) }}</span>
            <span>{{ formatTime(file.createdAt) }}</span>
          </div>
        </div>
        <div class="file-actions">
          <el-button text size="small" @click="handleDownload(file)">
            <el-icon><Download /></el-icon>
          </el-button>
          <el-button v-if="file.uploaderId === currentUserId" text size="small" type="danger" @click="handleDelete(file)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>

      <el-empty v-if="!loading && files.length === 0" description="暂无文件" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Upload, Download, Delete, Document, Picture, VideoPlay, Folder } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const props = defineProps<{
  teamId: number
  caseId?: number
}>()

const currentUserId = ref(1) // 临时硬编码
const loading = ref(false)
const files = ref<any[]>([])
const stats = ref<any>(null)

const uploadUrl = computed(() => `/api/v1/teams/${props.teamId}/files`)
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))
const uploadData = computed(() => ({
  caseId: props.caseId
}))

// 加载文件列表
async function loadFiles() {
  loading.value = true
  try {
    const params: any = {}
    if (props.caseId) params.caseId = props.caseId
    
    const res = await api.get(`/teams/${props.teamId}/files`, { params })
    files.value = res.data || []
  } catch (error) {
    console.error('加载文件列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载文件统计
async function loadStats() {
  try {
    const res = await api.get(`/teams/${props.teamId}/files/stats`)
    stats.value = res.data
  } catch (error) {
    console.error('加载文件统计失败:', error)
  }
}

// 上传前检查
function beforeUpload(file: File) {
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!')
    return false
  }
  return true
}

// 上传成功
function handleUploadSuccess(response: any) {
  ElMessage.success('文件上传成功')
  loadFiles()
  loadStats()
}

// 上传失败
function handleUploadError(error: any) {
  ElMessage.error('文件上传失败')
}

// 下载文件
function handleDownload(file: any) {
  window.open(file.fileUrl, '_blank')
}

// 删除文件
async function handleDelete(file: any) {
  try {
    await ElMessageBox.confirm(`确定要删除文件 "${file.name}" 吗？`, '确认删除', { type: 'warning' })
    await api.delete(`/teams/${props.teamId}/files/${file.id}`)
    ElMessage.success('文件已删除')
    loadFiles()
    loadStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 获取文件图标
function getFileIcon(fileType: string) {
  if (!fileType) return Document
  if (fileType.startsWith('image/')) return Picture
  if (fileType.startsWith('video/')) return VideoPlay
  if (fileType.includes('folder')) return Folder
  return Document
}

// 格式化文件大小
function formatFileSize(bytes: number) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 格式化时间
function formatTime(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadFiles()
  loadStats()
})
</script>

<style scoped>
.file-manager {
  margin-top: 24px;
}

.file-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.file-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #6b7280;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  transition: background 0.2s;
}

.file-item:hover {
  background: #f3f4f6;
}

.file-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e5e7eb;
  border-radius: 8px;
  color: #6b7280;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-weight: 500;
  color: #111827;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #9ca3af;
}

.file-actions {
  display: flex;
  gap: 4px;
}

html.dark .section-title { color: var(--text-primary); }
html.dark .file-item { background: var(--bg-secondary); }
html.dark .file-item:hover { background: var(--bg-tertiary); }
html.dark .file-icon { background: var(--bg-tertiary); }
html.dark .file-name { color: var(--text-primary); }
</style>