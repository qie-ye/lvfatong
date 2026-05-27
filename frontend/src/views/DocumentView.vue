<template>
  <div class="document-page">
    <div class="page-header">
      <h1 class="page-title">法律文书生成</h1>
      <el-tooltip content="选择文书类型、填写案情，AI将自动生成符合格式的法律文书" placement="bottom">
        <el-button type="primary" class="generate-btn" @click="showGenerateDialog = true">生成文书</el-button>
      </el-tooltip>
    </div>

    <div class="content-layout">
      <div class="doc-list">
        <el-select v-model="filterType" placeholder="按类型筛选" clearable @change="handleFilter" class="filter-select">
          <el-option v-for="t in docTypes" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <div v-loading="docStore.loading" class="list-scroll">
          <div
            v-for="d in docStore.documents"
            :key="d.id"
            class="doc-card"
            :class="{ active: currentId === d.id }"
            @click="selectDoc(d.id)"
          >
            <div class="doc-title">{{ d.title }}</div>
            <div class="doc-meta">
              <span class="doc-type-tag">{{ docTypeLabel(d.docType) }}</span>
              <span class="status-tag" :class="'status-' + d.status.toLowerCase()">{{ statusLabel(d.status) }}</span>
              <span class="doc-date">{{ formatDate(d.createdAt) }}</span>
            </div>
          </div>
        </div>
        <div v-if="!docStore.loading && docStore.documents.length === 0" class="empty-state">
          <div class="empty-icon-circle">
            <el-icon :size="28"><EditPen /></el-icon>
          </div>
          <p>暂无法律文书</p>
          <span>点击右上角「生成文书」，选择文书类型、填写案情，AI将自动生成符合格式的法律文书</span>
        </div>
      </div>

      <div class="doc-detail" v-loading="docStore.loading">
        <template v-if="docStore.currentDoc">
          <div v-if="docStore.currentDoc.status === 'GENERATING'" class="status-screen">
            <el-icon class="is-loading" :size="36"><Loading /></el-icon>
            <p>正在使用 GLM-4-Plus 生成法律文书，请稍候...</p>
            <el-button type="primary" link @click="refreshDoc">刷新查看</el-button>
          </div>
          <div v-else-if="docStore.currentDoc.status === 'FAILED'" class="status-screen failed-screen">
            <el-icon :size="36" color="#ef4444"><CircleCloseFilled /></el-icon>
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
            <div class="section">
              <div class="content-toolbar">
                <h3>文书内容</h3>
                <el-button size="small" @click="copyContent">复制全文</el-button>
              </div>
              <div class="document-text" v-html="renderMarkdown(docStore.currentDoc.content)"></div>
            </div>
            <el-alert type="warning" :closable="false" style="margin-top: 20px" class="disclaimer-alert">
              本法律文书由AI生成，仅供参考，不构成正式法律意见。正式法律文书应由持证律师审核后使用。
            </el-alert>
          </template>
        </template>
        <div v-else class="empty-state">
          <div class="empty-icon-circle">
            <el-icon :size="28"><EditPen /></el-icon>
          </div>
          <p>请从左侧选择或创建新的法律文书</p>
          <span>点击右上角「生成文书」，选择文书类型和案件事实，AI将自动生成符合格式的法律文书</span>
        </div>
      </div>
    </div>

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
import { Loading, CircleCloseFilled, EditPen } from '@element-plus/icons-vue'
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
  padding: 24px 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin: 0;
  letter-spacing: -0.02em;
}

.generate-btn {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.generate-btn:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.25);
}

.content-layout {
  display: flex;
  gap: 16px;
  min-height: 500px;
}

.doc-list {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.filter-select {
  width: 100%;
  margin-bottom: 12px;
}

.list-scroll {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  overflow-y: auto;
}

.doc-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 2px solid transparent;
}

.doc-card:hover {
  border-color: #2563eb;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);
}

.doc-card.active {
  border-left-color: #2563eb;
  background: #f8fafc;
  border-color: #e5e7eb;
  border-left-color: #2563eb;
}

.doc-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.doc-type-tag {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
  line-height: 18px;
  background: rgba(37, 99, 235, 0.06);
  color: #2563eb;
  border: 1px solid rgba(37, 99, 235, 0.12);
}

.doc-date {
  font-size: 12px;
  color: #9ca3af;
}

.status-tag {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
  line-height: 18px;
}

.status-generating {
  background: #fef3c7;
  color: #b45309;
}

.status-completed {
  background: #dcfce7;
  color: #15803d;
}

.status-failed {
  background: #fee2e2;
  color: #b91c1c;
}

.doc-detail {
  flex: 1;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 28px;
}

.detail-header h1 {
  font-size: 20px;
  color: #111827;
  margin: 0 0 10px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #4b5563;
}

.section {
  margin-top: 24px;
}

.section h3 {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 10px;
  padding-left: 12px;
  border-left: 3px solid #2563eb;
}

.content-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-toolbar h3 {
  margin: 0;
  padding-left: 12px;
  border-left: 3px solid #2563eb;
}

.document-text {
  font-size: 14px;
  line-height: 2;
  color: #111827;
  background: #f8fafc;
  padding: 24px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  max-height: 600px;
  overflow-y: auto;
  margin-top: 10px;
  font-family: Georgia, 'Times New Roman', '宋体', serif;
  letter-spacing: 0.02em;
}

.ai-content {
  color: #4b5563;
  line-height: 1.8;
  font-size: 14px;
}

.ai-content :deep(.md-h1),
.ai-content :deep(.md-h2),
.ai-content :deep(.md-h3) {
  font-weight: 700;
  margin: 14px 0 8px;
  color: #111827;
}

.ai-content :deep(.md-h1) { font-size: 17px; }
.ai-content :deep(.md-h2) { font-size: 16px; }
.ai-content :deep(.md-h3) { font-size: 15px; }
.ai-content :deep(.md-li) { margin: 6px 0; padding-left: 2px; }
.ai-content :deep(.md-idx) { font-weight: 600; }
.ai-content :deep(.md-bold-line) { margin: 10px 0 4px; }
.ai-content :deep(.md-intro) { margin-bottom: 8px; }

.ai-content :deep(.md-section) {
  margin: 14px 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: #f8fafc;
}

.ai-content :deep(.md-section-title) {
  padding: 10px 14px;
  font-weight: 700;
  color: #111827;
  background: rgba(37, 99, 235, 0.04);
  border-bottom: 1px solid #e5e7eb;
}

.ai-content :deep(.md-section-body) { padding: 14px; }

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  text-align: center;
  padding: 40px 20px;
}

.empty-icon-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(37, 99, 235, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  color: #2563eb;
}

.empty-state p {
  color: #4b5563;
  font-size: 14px;
  font-weight: 500;
  margin: 0 0 8px;
}

.empty-state span {
  color: #9ca3af;
  font-size: 13px;
  line-height: 1.5;
  max-width: 320px;
}

.status-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: #4b5563;
}

.status-screen p {
  margin-top: 12px;
}

.failed-screen p {
  margin-top: 12px;
}

.disclaimer-alert {
  border-radius: 8px;
}

html.dark .page-title { color: var(--text-primary); }
html.dark .doc-card { background: var(--bg-card); border-color: var(--border); }
html.dark .doc-card.active { background: rgba(59,130,246,0.1); border-color: #3b82f6; }
html.dark .doc-title { color: var(--text-primary); }
html.dark .doc-date { color: var(--text-tertiary); }
html.dark .doc-detail { background: var(--bg-card); border-color: var(--border); }
html.dark .detail-header h1 { color: var(--text-primary); }
html.dark .detail-meta { color: var(--text-secondary); }
html.dark .section h3 { color: var(--text-primary); border-left-color: #3b82f6; }
html.dark .document-text { color: var(--text-primary); background: rgba(255,255,255,0.03); border-color: var(--border); }
html.dark .ai-content { color: var(--text-secondary); }
html.dark .ai-content :deep(.md-h1),
html.dark .ai-content :deep(.md-h2),
html.dark .ai-content :deep(.md-h3) { color: var(--text-primary); }
html.dark .ai-content :deep(.md-section) { background: rgba(255,255,255,0.03); border-color: var(--border); }
html.dark .ai-content :deep(.md-section-title) { color: var(--text-primary); border-color: var(--border); background: rgba(59,130,246,0.08); }
html.dark .empty-icon-circle { background: rgba(59,130,246,0.1); }
html.dark .empty-state p { color: var(--text-secondary); }
html.dark .empty-state span { color: var(--text-tertiary); }
html.dark .status-screen { color: var(--text-secondary); }
</style>
