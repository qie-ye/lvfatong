<template>
  <div class="document-page">
    <div class="page-header">
      <h2>法律文书生成</h2>
      <el-button type="primary" @click="showGenerateDialog = true">生成文书</el-button>
    </div>

    <div class="content-layout">
      <!-- 左侧列表 -->
      <div class="doc-list">
        <el-select v-model="filterType" placeholder="按类型筛选" clearable @change="handleFilter" style="width: 100%; margin-bottom: 12px">
          <el-option v-for="t in docTypes" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <div v-loading="docStore.loading">
          <el-card
            v-for="d in docStore.documents"
            :key="d.id"
            shadow="hover"
            class="doc-card"
            :class="{ active: currentId === d.id }"
            @click="selectDoc(d.id)"
          >
            <div class="doc-title">{{ d.title }}</div>
            <div class="doc-meta">
              <el-tag size="small">{{ docTypeLabel(d.docType) }}</el-tag>
              <el-tag size="small" :type="statusType(d.status)">{{ statusLabel(d.status) }}</el-tag>
              <span class="doc-date">{{ formatDate(d.createdAt) }}</span>
            </div>
          </el-card>
        </div>
        <el-empty v-if="!docStore.loading && docStore.documents.length === 0" description="暂无法律文书" />
      </div>

      <!-- 右侧详情 -->
      <div class="doc-detail" v-loading="docStore.loading">
        <template v-if="docStore.currentDoc">
          <div v-if="docStore.currentDoc.status === 'GENERATING'" class="generating">
            <el-icon class="is-loading" :size="32"><Loading /></el-icon>
            <p>正在使用 GLM-4-Plus 生成法律文书，请稍候...</p>
            <el-button type="primary" link @click="refreshDoc">刷新查看</el-button>
          </div>
          <div v-else-if="docStore.currentDoc.status === 'FAILED'" class="failed">
            <el-icon :size="32" color="#f56c6c"><CircleCloseFilled /></el-icon>
            <p>文书生成失败，请重新尝试</p>
          </div>
          <template v-else>
            <div class="detail-header">
              <h1>{{ docStore.currentDoc.title }}</h1>
              <div class="detail-meta">
                <el-tag>{{ docTypeLabel(docStore.currentDoc.docType) }}</el-tag>
                <el-tag type="info">{{ docStore.currentDoc.domain || '综合' }}</el-tag>
                <span>模型: {{ docStore.currentDoc.model }}</span>
                <span>{{ formatDate(docStore.currentDoc.createdAt) }}</span>
              </div>
            </div>
            <el-divider />
            <div v-if="docStore.currentDoc.facts" class="section">
              <h3>案件事实</h3>
              <div class="ai-content" v-html="renderMarkdown(docStore.currentDoc.facts)"></div>
            </div>
            <div v-if="docStore.currentDoc.claims" class="section">
              <h3>请求/主张</h3>
              <div class="ai-content" v-html="renderMarkdown(docStore.currentDoc.claims)"></div>
            </div>
            <div class="section document-content">
              <div class="content-toolbar">
                <h3>文书内容</h3>
                <el-button size="small" @click="copyContent">复制全文</el-button>
              </div>
              <div class="ai-content document-text" v-html="renderMarkdown(docStore.currentDoc.content)"></div>
            </div>
            <el-alert type="warning" :closable="false" style="margin-top: 20px">
              本法律文书由AI生成，仅供参考，不构成正式法律意见。正式法律文书应由持证律师审核后使用。
            </el-alert>
          </template>
        </template>
        <el-empty v-else description="请从左侧选择或创建新的法律文书" />
      </div>
    </div>

    <!-- 生成对话框 -->
    <el-dialog v-model="showGenerateDialog" title="生成法律文书" width="650px" @close="resetForm">
      <el-form :model="form" label-width="90px">
        <el-form-item label="文书标题" required>
          <el-input v-model="form.title" placeholder="如：张某诉某公司劳动争议起诉状" maxlength="300" />
        </el-form-item>
        <el-form-item label="文书类型" required>
          <el-select v-model="form.docType" placeholder="选择文书类型" style="width: 100%">
            <el-option v-for="t in docTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="法律领域">
          <el-select v-model="form.domain" placeholder="选择领域" clearable style="width: 100%">
            <el-option v-for="d in domains" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="案件事实" required>
          <el-input v-model="form.facts" type="textarea" :rows="5" placeholder="详细描述案件事实经过" maxlength="10000" show-word-limit />
        </el-form-item>
        <el-form-item label="请求/主张">
          <el-input v-model="form.claims" type="textarea" :rows="3" placeholder="诉讼请求、仲裁请求等" maxlength="5000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGenerateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleGenerate" :loading="generating">生成文书</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, CircleCloseFilled } from '@element-plus/icons-vue'
import { useDocumentStore } from '@/stores/document'
import { renderMarkdown } from '@/utils/renderMarkdown'

const docStore = useDocumentStore()
const currentId = ref<number | null>(null)
const showGenerateDialog = ref(false)
const generating = ref(false)
const filterType = ref('')

const docTypes = [
  { value: 'COMPLAINT', label: '起诉状' },
  { value: 'DEFENSE', label: '答辩状' },
  { value: 'ARBITRATION', label: '仲裁申请书' },
  { value: 'PETITION', label: '申请书' },
  { value: 'INDICTMENT', label: '刑事自诉状' },
  { value: 'OTHER', label: '其他文书' }
]

const domains = ['劳动法', '合同法', '婚姻法', '房产法', '知识产权', '公司法', '刑事', '交通事故', '医疗纠纷', '行政法']

const form = reactive({
  title: '',
  docType: '',
  domain: '',
  facts: '',
  claims: ''
})

onMounted(() => {
  docStore.loadDocuments()
})

async function selectDoc(id: number) {
  currentId.value = id
  await docStore.getDocument(id)
}

async function refreshDoc() {
  if (currentId.value) {
    await docStore.getDocument(currentId.value)
  }
}

async function handleGenerate() {
  if (!form.title.trim() || !form.docType || !form.facts.trim()) {
    ElMessage.warning('请填写标题、文书类型和案件事实')
    return
  }
  generating.value = true
  try {
    const result = await docStore.generateDocument(form.title, form.docType, form.domain, form.facts, form.claims)
    showGenerateDialog.value = false
    ElMessage.success('文书生成中，请稍后查看')
    await docStore.loadDocuments()
    currentId.value = result.id
    pollDocStatus(result.id)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '生成失败')
  } finally {
    generating.value = false
  }
}

function pollDocStatus(id: number) {
  let count = 0
  const timer = setInterval(async () => {
    count++
    if (count > 60) {
      clearInterval(timer)
      return
    }
    await docStore.getDocument(id)
    if (docStore.currentDoc?.status !== 'GENERATING') {
      clearInterval(timer)
      await docStore.loadDocuments()
    }
  }, 5000)
}

function handleFilter() {
  docStore.loadDocuments(filterType.value || undefined)
}

function resetForm() {
  form.title = ''
  form.docType = ''
  form.domain = ''
  form.facts = ''
  form.claims = ''
}

function docTypeLabel(type: string) {
  const found = docTypes.find(t => t.value === type)
  return found ? found.label : type
}

function statusType(status: string) {
  const map: Record<string, string> = { GENERATING: 'warning', COMPLETED: 'success', FAILED: 'danger' }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = { GENERATING: '生成中', COMPLETED: '已完成', FAILED: '失败' }
  return map[status] || status
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function copyContent() {
  if (docStore.currentDoc?.content) {
    navigator.clipboard.writeText(docStore.currentDoc.content)
    ElMessage.success('已复制到剪贴板')
  }
}
</script>

<style scoped>
.document-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  color: #1a1a2e;
}

.content-layout {
  display: flex;
  gap: 16px;
  min-height: 500px;
}

.doc-list {
  width: 280px;
  flex-shrink: 0;
}

.doc-card {
  margin-bottom: 8px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.doc-card.active {
  border-color: #4fc3f7;
}

.doc-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #999;
  flex-wrap: wrap;
}

.doc-date {
  color: #999;
}

.doc-detail {
  flex: 1;
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.detail-header h1 {
  font-size: 20px;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #999;
}

.section {
  margin-top: 20px;
}

.section h3 {
  font-size: 16px;
  color: #1a1a2e;
  margin-bottom: 8px;
  padding-left: 10px;
  border-left: 3px solid #4fc3f7;
}

.section p {
  color: #333;
  line-height: 1.8;
  font-size: 14px;
  white-space: pre-wrap;
}

.content-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-toolbar h3 {
  margin: 0;
  padding-left: 10px;
  border-left: 3px solid #4fc3f7;
}

.document-text {
  font-size: 14px;
  line-height: 2;
  color: #333;
  background: #fafbfc;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  max-height: 600px;
  overflow-y: auto;
  margin-top: 8px;
}

.ai-content {
  color: #333;
  line-height: 1.8;
  font-size: 14px;
}

.ai-content :deep(.md-h1),
.ai-content :deep(.md-h2),
.ai-content :deep(.md-h3) {
  font-weight: 700;
  margin: 14px 0 8px;
  color: #1f2d3d;
}

.ai-content :deep(.md-h1) { font-size: 17px; }
.ai-content :deep(.md-h2) { font-size: 16px; }
.ai-content :deep(.md-h3) { font-size: 15px; }

.ai-content :deep(.md-li) {
  margin: 6px 0;
  padding-left: 2px;
}

.ai-content :deep(.md-idx) {
  font-weight: 600;
}

.ai-content :deep(.md-bold-line) {
  margin: 10px 0 4px;
}

.ai-content :deep(.md-intro) {
  margin-bottom: 8px;
}

.ai-content :deep(.md-section) {
  margin: 14px 0;
  border: 1px solid #e7ecf3;
  border-radius: 10px;
  overflow: hidden;
  background: #fafcff;
}

.ai-content :deep(.md-section-title) {
  padding: 10px 14px;
  font-weight: 700;
  color: #1f2d3d;
  background: #eef4ff;
  border-bottom: 1px solid #e7ecf3;
}

.ai-content :deep(.md-section-body) {
  padding: 14px;
}

.generating, .failed {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: #999;
}

.generating p, .failed p {
  margin-top: 12px;
}
</style>
