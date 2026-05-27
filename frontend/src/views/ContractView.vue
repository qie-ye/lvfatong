<template>
  <div class="contract-view">
    <el-tabs v-model="mainTab" class="main-tabs">
      <el-tab-pane label="合同分析" name="analysis">
        <el-container class="analysis-container">
          <el-aside width="280px">
            <div class="sidebar-header">
              <h4>合同列表</h4>
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                accept=".pdf,.doc,.docx"
                :on-change="handleFileChange"
                :disabled="contractStore.uploading"
                class="sidebar-upload"
              >
                <el-button size="small" type="primary" :loading="contractStore.uploading">
                  <el-icon :size="14"><Plus /></el-icon> 上传合同
                </el-button>
              </el-upload>
            </div>
            <div v-if="contractStore.uploadProgress > 0 && contractStore.uploadProgress < 100" class="sidebar-progress">
              <el-progress :percentage="contractStore.uploadProgress" :stroke-width="4" :show-text="false" />
            </div>
            <div class="contract-list">
              <div
                v-for="c in contractStore.contracts"
                :key="c.id"
                :class="['contract-item', { active: contractStore.currentContract?.id === c.id }]"
                @click="handleSelectContract(c)"
              >
                <div class="contract-name">
                  {{ c.filename }}
                </div>
                <div class="contract-meta">
                  <el-tag :type="statusType(c.status)" size="small">{{ statusText(c.status) }}</el-tag>
                  <span class="contract-size">{{ formatSize(c.fileSize) }}</span>
                </div>
              </div>
              <p v-if="contractStore.contracts.length === 0" class="placeholder-text">暂无合同，点击上方按钮上传合同文件</p>
            </div>
          </el-aside>

          <el-main>
            <div v-if="!contractStore.currentContract && !contractStore.analysisResult" class="upload-section">
              <div class="upload-zone">
                <el-upload
                  drag
                  :auto-upload="false"
                  :show-file-list="false"
                  accept=".pdf,.doc,.docx"
                  :on-change="handleFileChange"
                  :disabled="contractStore.uploading"
                >
                  <div class="upload-content">
                    <div class="upload-icon-wrapper">
                      <el-icon class="upload-icon" :size="40"><upload-filled /></el-icon>
                    </div>
                    <div class="upload-text">
                      拖拽合同文件到此处，或 <em>点击上传</em>
                    </div>
                  </div>
                  <template #tip>
                    <div class="upload-tip">支持 PDF、Word（.doc/.docx），单文件不超过 20MB</div>
                  </template>
                </el-upload>
              </div>
            </div>

            <div v-if="contractStore.currentContract && !contractStore.analysisResult" class="analyze-section">
              <el-card shadow="never">
                <div class="uploaded-info">
                  <div class="file-icon-wrapper">
                    <el-icon :size="20"><document /></el-icon>
                  </div>
                  <div>
                    <p class="filename">{{ contractStore.currentContract.filename }}</p>
                    <p class="file-meta">{{ formatSize(contractStore.currentContract.fileSize) }} · {{ statusText(contractStore.currentContract.status) }}</p>
                  </div>
                </div>
                <el-button
                  type="primary" size="large" style="width: 100%; margin-top: 16px"
                  @click="handleAnalyze" :loading="contractStore.analyzing" :disabled="contractStore.analyzing"
                >
                  {{ contractStore.analyzing ? '分析进行中...' : '开始合同分析' }}
                </el-button>
                <p v-if="contractStore.analyzing" class="analyzing-hint">合同分析通常需要30-60秒，结果将自动显示</p>
              </el-card>
            </div>

            <div v-if="contractStore.analysisResult" class="result-section">
              <el-card class="summary-card" :class="'risk-' + riskClass(contractStore.analysisResult.overallRisk)" shadow="never">
                <div class="summary-header">
                  <h3>合同分析报告</h3>
                  <el-tag :type="riskTagType(contractStore.analysisResult.overallRisk)" size="large" effect="dark">
                    整体风险：{{ contractStore.analysisResult.overallRisk }}
                  </el-tag>
                </div>
                <p class="summary-text" v-html="renderMarkdown(contractStore.analysisResult.summary)"></p>
              </el-card>

              <el-tabs v-model="activeTab" class="result-tabs">
                <el-tab-pane label="条款分析" name="clauses">
                  <div class="clauses-section">
                    <h4>条款分析（{{ contractStore.analysisResult.clauses.length }}项）</h4>
                    <el-card v-for="clause in contractStore.analysisResult.clauses" :key="clause.index" class="clause-card" :class="'clause-' + riskClass(clause.riskLevel)" shadow="never">
                      <div class="clause-header">
                        <span class="clause-title">{{ clause.index }}. {{ clause.title }}</span>
                        <el-tag :type="riskTagType(clause.riskLevel)" size="small">{{ clause.riskLevel }}风险</el-tag>
                      </div>
                      <div class="clause-content" v-if="clause.content">
                        <p class="content-text">{{ clause.content }}</p>
                      </div>
                      <div v-if="clause.description" class="clause-risk">
                        <strong>风险说明：</strong><span v-html="renderMarkdown(clause.description)"></span>
                        <span v-if="clause.riskCategory" class="risk-category">（{{ clause.riskCategory }}）</span>
                      </div>
                      <div v-if="clause.legalBasis" class="clause-legal">
                        <strong>法律依据：</strong><span v-html="renderMarkdown(clause.legalBasis)"></span>
                      </div>
                      <div v-if="clause.suggestion" class="clause-suggestion">
                        <strong>修改建议：</strong><span v-html="renderMarkdown(clause.suggestion)"></span>
                      </div>
                    </el-card>
                  </div>
                </el-tab-pane>
                <el-tab-pane label="修改建议" name="suggestions">
                  <div v-if="suggestions.length === 0" class="suggestions-empty">
                    <el-button type="primary" @click="loadSuggestions" :loading="suggestionsLoading">生成修改建议</el-button>
                  </div>
                  <div v-else>
                    <el-card v-for="s in suggestions" :key="s.clauseIndex" class="suggestion-card" :class="'clause-' + riskClass(s.riskLevel)" shadow="never">
                      <div class="clause-header">
                        <span class="clause-title">条款{{ s.clauseIndex }}: {{ s.clauseTitle }}</span>
                        <el-tag :type="riskTagType(s.riskLevel)" size="small">{{ s.riskLevel }}风险</el-tag>
                      </div>
                      <div class="suggestion-original" v-if="s.originalContent">
                        <strong>原文：</strong>
                        <p class="content-text">{{ s.originalContent }}</p>
                      </div>
                      <div class="suggestion-text">
                        <strong>建议：</strong><span v-html="renderMarkdown(s.suggestion)"></span>
                      </div>
                      <div v-if="s.legalBasis" class="clause-legal">
                        <strong>法律依据：</strong><span v-html="renderMarkdown(s.legalBasis)"></span>
                      </div>
                      <div v-if="s.aiModificationDetail" class="ai-detail">
                        <el-divider />
                        <strong>AI修改方案：</strong>
                        <div class="ai-detail-content" v-html="renderMarkdown(s.aiModificationDetail)"></div>
                      </div>
                    </el-card>
                  </div>
                </el-tab-pane>
                <el-tab-pane label="合同对比" name="compare">
                  <div class="compare-section">
                    <p class="compare-desc">选择两份已分析的合同进行AI对比分析</p>
                    <div class="compare-controls">
                      <el-select v-model="compareId1" placeholder="选择合同一" style="width: 200px; margin-right: 12px">
                        <el-option v-for="c in contractStore.contracts.filter(c => c.status === 'COMPLETED')" :key="c.id" :label="c.filename" :value="c.id" />
                      </el-select>
                      <el-select v-model="compareId2" placeholder="选择合同二" style="width: 200px">
                        <el-option v-for="c in contractStore.contracts.filter(c => c.status === 'COMPLETED')" :key="c.id" :label="c.filename" :value="c.id" />
                      </el-select>
                      <el-button type="primary" @click="handleCompare" :loading="compareLoading" :disabled="!compareId1 || !compareId2" style="margin-left: 12px">开始对比</el-button>
                    </div>
                    <div v-if="compareResult" class="compare-result">
                      <el-divider />
                      <div class="ai-content" v-html="renderMarkdown(compareResult)"></div>
                    </div>
                  </div>
                </el-tab-pane>
              </el-tabs>
            </div>
          </el-main>
        </el-container>
      </el-tab-pane>

      <el-tab-pane label="合同模板库" name="templates">
        <div class="template-library">
          <div class="template-toolbar">
            <el-select v-model="templateCategory" placeholder="全部分类" clearable style="width: 180px">
              <el-option v-for="cat in templateCategories" :key="cat" :label="cat" :value="cat" />
            </el-select>
            <el-input v-model="templateKeyword" placeholder="搜索模板名称..." clearable style="width: 220px" @input="filterTemplates" />
            <span class="template-count" v-if="filteredTemplates.length > 0">共 {{ filteredTemplates.length }} 个模板</span>
          </div>

          <el-skeleton :loading="templatesLoading" animated :count="3">
            <template #default>
              <div class="template-grid">
                <el-card v-for="t in filteredTemplates" :key="t.id" shadow="never" class="template-card" @click="showTemplateDetail(t)">
                  <div class="template-card-header">
                    <el-tag size="small" type="primary">{{ t.category }}</el-tag>
                    <el-tag v-if="t.applicableLaw" size="small" type="info" style="margin-left: 6px">{{ t.applicableLaw }}</el-tag>
                  </div>
                  <div class="template-title">{{ t.title }}</div>
                  <div class="template-desc">{{ t.description || '暂无描述' }}</div>
                  <div class="template-footer">
                    <span class="template-action">查看模板 <span class="arrow">→</span></span>
                  </div>
                </el-card>
              </div>
              <el-empty v-if="!templatesLoading && filteredTemplates.length === 0" description="暂无模板" class="template-empty" />
            </template>
          </el-skeleton>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="templateDetailVisible" :title="selectedTemplate?.title" width="720px" top="5vh" destroy-on-close>
      <div v-if="selectedTemplate">
        <div class="template-detail-meta">
          <el-tag type="primary">{{ selectedTemplate.category }}</el-tag>
          <el-tag v-if="selectedTemplate.applicableLaw" type="info" style="margin-left: 8px">{{ selectedTemplate.applicableLaw }}</el-tag>
          <span v-if="selectedTemplate.description" class="template-detail-desc">{{ selectedTemplate.description }}</span>
        </div>
        <el-divider />
        <div class="template-detail-content" v-html="renderMarkdown(selectedTemplate.content || '模板内容为空')"></div>
      </div>
      <template #footer>
        <el-button @click="templateDetailVisible = false">关闭</el-button>
        <el-button type="primary" @click="copyTemplateContent">复制全文</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { UploadFilled, Document, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useContractStore } from '@/stores/contract'
import type { ContractDocument } from '@/stores/contract'
import { renderMarkdown } from '@/utils/renderMarkdown'
import api from '@/api'

const contractStore = useContractStore()

const mainTab = ref('analysis')
const activeTab = ref('clauses')
const suggestions = ref<any[]>([])
const suggestionsLoading = ref(false)
const compareId1 = ref<number | null>(null)
const compareId2 = ref<number | null>(null)
const compareLoading = ref(false)
const compareResult = ref('')

const templates = ref<any[]>([])
const templatesLoading = ref(false)
const templateLoaded = ref(false)
const templateCategory = ref('')
const templateKeyword = ref('')
const templateDetailVisible = ref(false)
const selectedTemplate = ref<any>(null)

const templateCategories = computed(() => {
  const cats = templates.value.map((t: any) => t.category).filter(Boolean)
  return [...new Set(cats)] as string[]
})

const filteredTemplates = computed(() => {
  let list = templates.value
  if (templateCategory.value) {
    list = list.filter((t: any) => t.category === templateCategory.value)
  }
  if (templateKeyword.value.trim()) {
    const kw = templateKeyword.value.trim().toLowerCase()
    list = list.filter((t: any) => t.title?.toLowerCase().includes(kw) || t.description?.toLowerCase().includes(kw))
  }
  return list
})

onMounted(() => { contractStore.loadContracts() })

watch(mainTab, (tab) => {
  if (tab === 'templates' && !templateLoaded.value) { loadAllTemplates() }
})

async function handleFileChange(uploadFile: any) {
  const file = uploadFile.raw as File
  if (!file) return
  const allowedExts = ['pdf', 'doc', 'docx']
  const ext = file.name.split('.').pop()?.toLowerCase() || ''
  if (!allowedExts.includes(ext)) { ElMessage.error('仅支持 PDF 和 Word 文档'); return }
  if (file.size > 20 * 1024 * 1024) { ElMessage.error('文件大小不能超过 20MB'); return }
  try {
    await contractStore.uploadFile(file)
    ElMessage.success('文件上传成功')
  } catch (e: any) { ElMessage.error(e.message || '上传失败') }
}

async function handleAnalyze() {
  if (!contractStore.currentContract) return
  try {
    await contractStore.analyzeContract(contractStore.currentContract.id)
    ElMessage.success('合同分析完成')
  } catch (e: any) { ElMessage.error(e.message || '分析失败') }
}

async function handleSelectContract(c: ContractDocument) {
  contractStore.currentContract = c
  contractStore.analysisResult = null
  if (c.status === 'COMPLETED') {
    try { await contractStore.getAnalysis(c.id) } catch (e: any) { ElMessage.warning('加载分析结果失败') }
  }
}

function statusType(status: string) {
  const map: Record<string, string> = { UPLOADED: 'info', PARSING: 'warning', PARSED: '', ANALYZING: 'warning', COMPLETED: 'success', FAILED: 'danger' }
  return map[status] || 'info'
}

function statusText(status: string) {
  const map: Record<string, string> = { UPLOADED: '待解析', PARSING: '解析中', PARSED: '已解析', ANALYZING: '分析中', COMPLETED: '已完成', FAILED: '失败' }
  return map[status] || status
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
}

function riskClass(level: string) { if (level === '高') return 'high'; if (level === '中') return 'medium'; return 'low' }
function riskTagType(level: string) { if (level === '高') return 'danger'; if (level === '中') return 'warning'; return 'success' }

async function loadSuggestions() {
  if (!contractStore.currentContract) return
  suggestionsLoading.value = true
  try {
    const res = await api.get(`/contract/${contractStore.currentContract.id}/suggestions`)
    suggestions.value = (res.data as any[]) || []
  } catch { ElMessage.error('加载修改建议失败') } finally { suggestionsLoading.value = false }
}

async function handleCompare() {
  if (!compareId1.value || !compareId2.value) return
  compareLoading.value = true
  try {
    const res = await api.post('/contract/compare', null, { params: { contractId1: compareId1.value, contractId2: compareId2.value } })
    compareResult.value = res.data as string
  } catch { ElMessage.error('合同对比失败') } finally { compareLoading.value = false }
}

async function loadAllTemplates() {
  templatesLoading.value = true
  try {
    const res = await api.get('/contract/templates')
    templates.value = (res.data as any[]) || []
    templateLoaded.value = true
  } catch { ElMessage.error('加载模板列表失败') } finally { templatesLoading.value = false }
}

function filterTemplates() {}

async function showTemplateDetail(t: any) {
  try {
    const res = await api.get(`/contract/templates/${t.id}`)
    selectedTemplate.value = res.data as any
    templateDetailVisible.value = true
  } catch { ElMessage.error('加载模板详情失败') }
}

async function copyTemplateContent() {
  const text = selectedTemplate.value?.content
  if (!text) return
  try { await navigator.clipboard.writeText(text); ElMessage.success('已复制到剪贴板') }
  catch { ElMessage.warning('复制失败，请手动选择文本复制') }
}
</script>

<style scoped>
/* ===== Design System Variables ===== */
.contract-view {
  --app-bg: #f8fafc;
  --bg-card: #ffffff;
  --border: #e5e7eb;
  --border-light: #f3f4f6;
  --color-primary: #2563eb;
  --color-primary-hover: #1d4ed7;
  --color-primary-light: #eff6ff;
  --color-primary-50: #eff6ff;
  --text-primary: #111827;
  --text-secondary: #4b5563;
  --text-tertiary: #9ca3af;
  --shadow-xs: 0 1px 2px rgba(37, 99, 235, 0.04);
  --shadow-sm: 0 1px 3px rgba(37, 99, 235, 0.06), 0 1px 2px rgba(37, 99, 235, 0.04);
  --shadow-md: 0 4px 12px rgba(37, 99, 235, 0.08);
  --shadow-lg: 0 10px 25px rgba(37, 99, 235, 0.1);
  --radius-xs: 4px;
  --radius-sm: 6px;
  --radius: 8px;
  --radius-lg: 12px;
  --transition: all 0.2s ease;
  --transition-slow: all 0.3s ease;

  --el-color-primary: var(--color-primary);
  --el-color-primary-light-3: rgba(37, 99, 235, 0.3);
  --el-color-primary-light-5: rgba(37, 99, 235, 0.5);
  --el-color-primary-light-7: rgba(37, 99, 235, 0.7);
  --el-color-primary-light-9: rgba(37, 99, 235, 0.9);

  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  background: var(--app-bg);
}

/* ===== Layout ===== */
.main-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.main-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow: hidden;
}

.main-tabs :deep(.el-tab-pane) {
  height: 100%;
}

.analysis-container {
  height: calc(100vh - 105px);
}

/* ===== Tab Header ===== */
.main-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
  padding: 0 24px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
}

.main-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.main-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-tertiary);
  padding: 0 20px;
  height: 44px;
  line-height: 44px;
  transition: var(--transition);
}

.main-tabs :deep(.el-tabs__item:hover) {
  color: var(--text-primary);
}

.main-tabs :deep(.el-tabs__item.is-active) {
  color: var(--color-primary);
  font-weight: 600;
}

.main-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--color-primary);
  height: 2px;
}

/* ===== Sidebar ===== */
.el-aside {
  background: var(--bg-card);
  border-right: 1px solid var(--border);
  padding: 16px;
  overflow-y: auto;
}

.sidebar-header {
  padding: 0 0 12px 0;
  border-bottom: 1px solid var(--border-light);
  margin-bottom: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.sidebar-header h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}

.sidebar-upload {
  flex-shrink: 0;
}

.sidebar-progress {
  margin: 0 0 8px 0;
}

.contract-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.contract-item {
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition);
  border: 1px solid transparent;
  border-left: 2px solid transparent;
  position: relative;
}

.contract-item:hover {
  background: #f9fafb;
  border-color: var(--border-light);
}

.contract-item.active {
  border-left-color: var(--color-primary);
  background: var(--color-primary-50);
  border-top-color: var(--border-light);
  border-right-color: var(--border-light);
  border-bottom-color: var(--border-light);
}

.contract-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.contract-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.contract-size {
  font-size: 11px;
  color: var(--text-tertiary);
  font-weight: 400;
}

.placeholder-text {
  color: var(--text-tertiary);
  font-size: 13px;
  text-align: center;
  margin-top: 32px;
  padding: 20px;
}

/* ===== Status Tags Override ===== */
.contract-meta :deep(.el-tag) {
  border-radius: 3px;
  font-size: 11px;
  font-weight: 500;
  height: 20px;
  line-height: 18px;
  padding: 0 6px;
  border: none;
}

.contract-meta :deep(.el-tag--info) {
  background: #f3f4f6;
  color: #6b7280;
}

.contract-meta :deep(.el-tag--warning) {
  background: #fffbeb;
  color: #d97706;
}

.contract-meta :deep(.el-tag--success) {
  background: #ecfdf5;
  color: #059669;
}

.contract-meta :deep(.el-tag--danger) {
  background: #fef2f2;
  color: #dc2626;
}

/* ===== Upload Section ===== */
.upload-section {
  max-width: 560px;
  margin: 60px auto;
  padding: 0 24px;
}

.upload-zone {
  margin-bottom: 0;
}

.upload-zone :deep(.el-upload) {
  width: 100%;
}

.upload-zone :deep(.el-upload-dragger) {
  width: 100%;
  min-height: 180px;
  border: 2px dashed #d1d5db;
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  transition: var(--transition-slow);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  box-shadow: var(--shadow-xs);
}

.upload-zone :deep(.el-upload-dragger:hover) {
  border-color: var(--color-primary);
  background: rgba(37, 99, 235, 0.02);
  box-shadow: var(--shadow-md);
}

.upload-zone :deep(.el-upload-dragger .el-upload__inner) {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
}

.upload-zone :deep(.el-icon) {
  color: var(--text-tertiary);
  transition: var(--transition);
}

.upload-zone :deep(.el-upload-dragger:hover .el-icon) {
  color: var(--color-primary);
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.upload-icon-wrapper {
  margin-bottom: 4px;
}

.upload-icon {
  color: var(--text-tertiary);
}

.upload-text {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.upload-text em {
  color: var(--color-primary);
  font-style: normal;
  font-weight: 500;
  cursor: pointer;
}

.upload-tip {
  color: var(--text-tertiary);
  font-size: 12px;
  text-align: center;
  margin-top: 10px;
  line-height: 1.5;
}

/* ===== Progress Bar ===== */
.progress-wrapper {
  margin-top: 20px;
}

.progress-wrapper :deep(.el-progress-bar__outer) {
  background: var(--border-light);
  border-radius: 10px;
  height: 8px;
}

.progress-wrapper :deep(.el-progress-bar__inner) {
  border-radius: 10px;
  background: var(--color-primary);
}

.progress-wrapper :deep(.el-progress__text) {
  font-size: 12px;
  color: var(--text-secondary);
}

/* ===== Analyze Section ===== */
.analyze-section {
  margin-top: 24px;
}

.analyze-section :deep(.el-card) {
  border-radius: var(--radius);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
}

.uploaded-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-icon-wrapper {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary-50);
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.file-icon-wrapper :deep(.el-icon) {
  color: var(--color-primary);
}

.filename {
  font-weight: 600;
  margin: 0;
  font-size: 14px;
  color: var(--text-primary);
}

.file-meta {
  font-size: 12px;
  color: var(--text-tertiary);
  margin: 2px 0 0 0;
}

.analyzing-hint {
  text-align: center;
  color: var(--text-tertiary);
  font-size: 12px;
  margin-top: 10px;
}

/* ===== Buttons ===== */
:deep(.el-button--primary) {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-hover-bg-color: var(--color-primary-hover);
  --el-button-hover-border-color: var(--color-primary-hover);
  --el-button-active-bg-color: #1e40af;
  --el-button-active-border-color: #1e40af;
  border-radius: var(--radius-sm);
  font-weight: 500;
  transition: var(--transition);
}

:deep(.el-button--large) {
  padding: 12px 20px;
  font-size: 14px;
  border-radius: var(--radius-sm);
}

/* ===== Result Section ===== */
.result-section {
  padding: 0 24px 24px;
}

.back-button {
  margin-bottom: 16px;
  color: var(--text-secondary);
  font-size: 13px;
  padding: 4px 8px;
}

.back-button:hover {
  color: var(--color-primary);
}

/* ===== Summary Card ===== */
.summary-card {
  margin-bottom: 20px;
  border-left-width: 3px;
  border-radius: var(--radius) !important;
  box-shadow: var(--shadow-sm) !important;
  border: 1px solid var(--border) !important;
}

.summary-card :deep(.el-card__body) {
  padding: 20px 24px;
}

.summary-card.risk-high {
  border-left-color: #ef4444 !important;
}

.summary-card.risk-medium {
  border-left-color: #f59e0b !important;
}

.summary-card.risk-low {
  border-left-color: #10b981 !important;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.summary-header h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.summary-header :deep(.el-tag--large) {
  font-size: 12px;
  padding: 4px 12px;
  height: auto;
  border-radius: 4px;
  font-weight: 600;
}

.summary-text {
  color: var(--text-secondary);
  line-height: 1.8;
  font-size: 14px;
}

.summary-text :deep(p) {
  margin: 0 0 8px 0;
}

.summary-text :deep(p:last-child) {
  margin-bottom: 0;
}

/* ===== Result Tabs ===== */
.result-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
  background: transparent;
  border-bottom: 1px solid var(--border);
}

.result-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.result-tabs :deep(.el-tabs__item) {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-tertiary);
  height: 40px;
  line-height: 40px;
}

.result-tabs :deep(.el-tabs__item.is-active) {
  color: var(--color-primary);
  font-weight: 600;
}

.result-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--color-primary);
  height: 2px;
}

/* ===== Clauses Section ===== */
.clauses-section h4 {
  margin: 0 0 16px 0;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
}

.clause-card {
  margin-bottom: 12px;
  border-left-width: 3px;
  border-radius: var(--radius) !important;
  box-shadow: var(--shadow-xs) !important;
  border: 1px solid var(--border) !important;
  transition: var(--transition);
}

.clause-card:hover {
  box-shadow: var(--shadow-sm) !important;
}

.clause-card.clause-high {
  border-left-color: #ef4444 !important;
}

.clause-card.clause-medium {
  border-left-color: #f59e0b !important;
}

.clause-card.clause-low {
  border-left-color: #10b981 !important;
}

.clause-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.clause-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.clause-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.5;
}

.clause-header :deep(.el-tag) {
  border-radius: 3px;
  font-size: 11px;
  font-weight: 600;
  height: 22px;
  line-height: 20px;
  flex-shrink: 0;
  margin-left: 12px;
}

.clause-header :deep(.el-tag--danger) {
  background: #fef2f2;
  color: #dc2626;
  border-color: #fecaca;
}

.clause-header :deep(.el-tag--warning) {
  background: #fffbeb;
  color: #d97706;
  border-color: #fde68a;
}

.clause-header :deep(.el-tag--success) {
  background: #ecfdf5;
  color: #059669;
  border-color: #a7f3d0;
}

.content-text {
  background: var(--app-bg);
  padding: 10px 14px;
  border-radius: var(--radius-xs);
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 8px 0;
  max-height: 120px;
  overflow-y: auto;
  border: 1px solid var(--border);
}

.clause-risk,
.clause-legal,
.clause-suggestion {
  font-size: 13px;
  line-height: 1.7;
  margin-top: 8px;
}

.clause-risk {
  color: #dc2626;
}

.clause-risk strong {
  color: var(--text-primary);
}

.clause-legal {
  color: var(--color-primary);
}

.clause-legal strong {
  color: var(--text-primary);
}

.clause-suggestion {
  color: #059669;
}

.clause-suggestion strong {
  color: var(--text-primary);
}

.risk-category {
  font-size: 12px;
  color: var(--text-tertiary);
}

/* ===== Suggestions ===== */
.suggestions-empty {
  text-align: center;
  padding: 64px 20px;
}

.suggestions-empty :deep(.el-button--primary) {
  padding: 12px 32px;
  font-size: 14px;
}

.suggestion-card {
  margin-bottom: 12px;
  border-left-width: 3px;
  border-radius: var(--radius) !important;
  box-shadow: var(--shadow-xs) !important;
  border: 1px solid var(--border) !important;
  transition: var(--transition);
}

.suggestion-card:hover {
  box-shadow: var(--shadow-sm) !important;
}

.suggestion-card.clause-high {
  border-left-color: #ef4444 !important;
}

.suggestion-card.clause-medium {
  border-left-color: #f59e0b !important;
}

.suggestion-card.clause-low {
  border-left-color: #10b981 !important;
}

.suggestion-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.suggestion-original {
  margin-top: 8px;
}

.suggestion-original strong {
  color: var(--text-primary);
  font-size: 13px;
}

.suggestion-text {
  color: #059669;
  font-size: 13px;
  line-height: 1.7;
  margin-top: 8px;
}

.suggestion-text strong {
  color: var(--text-primary);
}

.ai-detail {
  margin-top: 12px;
}

.ai-detail strong {
  color: var(--text-primary);
  font-size: 13px;
}

.ai-detail-content {
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-secondary);
  max-height: 400px;
  overflow-y: auto;
  padding: 4px 0;
}

.ai-detail-content :deep(p) {
  margin: 0 0 6px 0;
}

/* ===== Divider ===== */
.result-section :deep(.el-divider--horizontal) {
  margin: 16px 0;
  border-top-color: var(--border);
}

/* ===== Compare Section ===== */
.compare-section {
  padding: 4px 0;
}

.compare-desc {
  color: var(--text-secondary);
  margin-bottom: 20px;
  font-size: 14px;
}

.compare-controls {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.compare-result {
  margin-top: 24px;
}

.compare-result .ai-content {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 20px 24px;
  box-shadow: var(--shadow-xs);
}

.ai-content {
  color: var(--text-primary);
  line-height: 1.8;
  font-size: 14px;
}

.ai-content :deep(.md-h1),
.ai-content :deep(.md-h2),
.ai-content :deep(.md-h3) {
  font-weight: 600;
  margin: 16px 0 8px;
  color: var(--text-primary);
}

.ai-content :deep(.md-h1) {
  font-size: 18px;
}

.ai-content :deep(.md-h2) {
  font-size: 16px;
}

.ai-content :deep(.md-h3) {
  font-size: 15px;
}

.ai-content :deep(ul),
.ai-content :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}

.ai-content :deep(li) {
  margin-bottom: 4px;
}

/* ===== Template Library ===== */
.template-library {
  padding: 24px;
  height: calc(100vh - 105px);
  overflow-y: auto;
  background: var(--app-bg);
}

.template-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 10px;
}

.template-count {
  color: var(--text-tertiary);
  font-size: 13px;
  margin-left: 8px;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.template-card {
  cursor: pointer;
  border-radius: var(--radius) !important;
  border: 1px solid var(--border) !important;
  box-shadow: var(--shadow-xs) !important;
  transition: var(--transition-slow);
  background: var(--bg-card) !important;
}

.template-card :deep(.el-card__body) {
  padding: 20px;
}

.template-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(37, 99, 235, 0.1) !important;
  border-color: #93c5fd !important;
}

.template-card-header {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.template-card-header :deep(.el-tag) {
  border-radius: 3px;
  font-size: 11px;
  height: 22px;
  line-height: 20px;
  font-weight: 500;
}

.template-card-header :deep(.el-tag--primary) {
  background: var(--color-primary-50);
  color: var(--color-primary);
  border-color: #bfdbfe;
}

.template-card-header :deep(.el-tag--info) {
  background: #f3f4f6;
  color: #6b7280;
  border-color: #e5e7eb;
}

.template-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
  margin-bottom: 8px;
  line-height: 1.4;
}

.template-desc {
  font-size: 13px;
  color: var(--text-tertiary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.template-footer {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

.template-action {
  font-size: 13px;
  color: var(--color-primary);
  font-weight: 500;
  transition: var(--transition);
}

.template-action .arrow {
  display: inline-block;
  transition: transform 0.3s ease;
  margin-left: 2px;
}

.template-card:hover .template-action .arrow {
  transform: translateX(4px);
}

.template-empty {
  margin-top: 80px;
}

.template-empty :deep(.el-empty__description) {
  color: var(--text-tertiary);
}

/* ===== Select & Input Overrides ===== */
:deep(.el-select .el-input__wrapper) {
  border-radius: var(--radius-sm);
  box-shadow: none;
  border: 1px solid var(--border);
  background: var(--bg-card);
  transition: var(--transition);
}

:deep(.el-select .el-input__wrapper:hover) {
  border-color: var(--text-tertiary);
}

:deep(.el-select .el-input__wrapper.is-focus) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 1px var(--color-primary);
}

:deep(.el-input .el-input__wrapper) {
  border-radius: var(--radius-sm);
  box-shadow: none;
  border: 1px solid var(--border);
  background: var(--bg-card);
  transition: var(--transition);
}

:deep(.el-input .el-input__wrapper:hover) {
  border-color: var(--text-tertiary);
}

:deep(.el-input .el-input__wrapper.is-focus) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 1px var(--color-primary);
}

/* ===== Dialog ===== */
:deep(.el-dialog) {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}

:deep(.el-dialog__header) {
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--border-light);
}

:deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

:deep(.el-dialog__body) {
  padding: 20px 24px;
}

:deep(.el-dialog__footer) {
  padding: 12px 24px 20px;
  border-top: 1px solid var(--border-light);
}

/* ===== Template Detail ===== */
.template-detail-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.template-detail-meta :deep(.el-tag--primary) {
  background: var(--color-primary-50);
  color: var(--color-primary);
  border-color: #bfdbfe;
}

.template-detail-meta :deep(.el-tag--info) {
  background: #f3f4f6;
  color: #6b7280;
  border-color: #e5e7eb;
}

.template-detail-desc {
  color: var(--text-secondary);
  font-size: 13px;
  margin-left: 4px;
}

.template-detail-content {
  font-size: 14px;
  line-height: 2;
  color: var(--text-primary);
  max-height: 60vh;
  overflow-y: auto;
  background: var(--app-bg);
  padding: 20px 24px;
  border-radius: var(--radius);
  border: 1px solid var(--border);
}

.template-detail-content :deep(p) {
  margin: 0 0 8px 0;
}

.template-detail-content :deep(h1),
.template-detail-content :deep(h2),
.template-detail-content :deep(h3) {
  margin: 16px 0 8px;
  color: var(--text-primary);
}

/* ===== Skeleton ===== */
:deep(.el-skeleton) {
  --el-skeleton-color: var(--border-light);
  --el-skeleton-to-color: #e5e7eb;
}

/* ===== Scrollbar ===== */
.el-aside::-webkit-scrollbar {
  width: 4px;
}

.el-aside::-webkit-scrollbar-track {
  background: transparent;
}

.el-aside::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 4px;
}

.el-aside::-webkit-scrollbar-thumb:hover {
  background: #d1d5db;
}

.template-library::-webkit-scrollbar {
  width: 6px;
}

.template-library::-webkit-scrollbar-track {
  background: transparent;
}

.template-library::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 4px;
}

.template-library::-webkit-scrollbar-thumb:hover {
  background: #d1d5db;
}

/* ===== Disabled upload state ===== */
.upload-zone :deep(.el-upload-dragger.is-dragover) {
  border-color: var(--color-primary) !important;
  background: rgba(37, 99, 235, 0.04) !important;
}

/* ===== Empty state ===== */
:deep(.el-empty__image) {
  opacity: 0.5;
}

html.dark .contract-view {
  --app-bg: var(--bg);
  --bg-card: var(--bg-card);
  --border: var(--border);
  --border-light: var(--border-light);
  --text-primary: var(--text-primary);
  --text-secondary: var(--text-secondary);
  --text-tertiary: var(--text-tertiary);
  --color-primary-50: rgba(37,99,235,0.08);
  --el-skeleton-color: var(--border-light);
  --el-skeleton-to-color: var(--border);
}
html.dark .contract-item:hover { background: var(--bg); }
html.dark .contract-item.active { background: rgba(59,130,246,0.1); border-left-color: #3b82f6; }
html.dark .upload-zone :deep(.el-upload-dragger) { background: var(--bg-card); border-color: rgba(255,255,255,0.12); }
html.dark .template-card { background: var(--bg-card) !important; }
html.dark .template-card:hover { border-color: var(--border) !important; box-shadow: 0 8px 25px rgba(0,0,0,0.3) !important; }
html.dark .template-library { background: var(--bg); }
html.dark .template-detail-content { background: var(--bg); }
html.dark .content-text { background: var(--bg); border-color: var(--border); }
html.dark .main-tabs :deep(.el-tabs__header) { background: var(--bg-card); }
html.dark .el-aside { background: var(--bg-card); }
html.dark .clause-card { background: var(--bg-card) !important; border-color: var(--border) !important; box-shadow: none !important; }
html.dark .suggestion-card { background: var(--bg-card) !important; border-color: var(--border) !important; box-shadow: none !important; }
html.dark .summary-card { background: var(--bg-card) !important; border-color: var(--border) !important; }
html.dark .compare-result .ai-content { background: var(--bg-card); border-color: var(--border); }
html.dark .contract-meta :deep(.el-tag--info),
html.dark .template-card-header :deep(.el-tag--info),
html.dark .template-detail-meta :deep(.el-tag--info) {
  background: var(--border-light); color: var(--text-secondary); border-color: var(--border);
}
</style>
