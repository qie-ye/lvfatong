<template>
  <div class="contract-view">
    <!-- Top-level tabs: analysis flow vs template library -->
    <el-tabs v-model="mainTab" class="main-tabs">

      <!-- ===== Tab 1: Contract Analysis ===== -->
      <el-tab-pane label="合同分析" name="analysis">
        <el-container class="analysis-container">
          <el-aside width="280px">
            <div class="sidebar-header">
              <h4>我的合同</h4>
            </div>
            <div class="contract-list">
              <div
                v-for="c in contractStore.contracts"
                :key="c.id"
                :class="['contract-item', { active: contractStore.currentContract?.id === c.id }]"
                @click="handleSelectContract(c)"
              >
                <div class="contract-name">{{ c.filename }}</div>
                <div class="contract-meta">
                  <el-tag :type="statusType(c.status)" size="small">{{ statusText(c.status) }}</el-tag>
                  <span class="contract-size">{{ formatSize(c.fileSize) }}</span>
                </div>
              </div>
              <p v-if="contractStore.contracts.length === 0" class="placeholder-text">暂无合同</p>
            </div>
          </el-aside>

          <el-main>
            <!-- Upload Area -->
            <div v-if="!contractStore.analysisResult" class="upload-section">
              <el-upload
                drag
                :auto-upload="false"
                :show-file-list="false"
                accept=".pdf,.doc,.docx"
                :on-change="handleFileChange"
                :disabled="contractStore.uploading"
              >
                <el-icon class="el-icon--upload" :size="48"><upload-filled /></el-icon>
                <div class="el-upload__text">
                  拖拽合同文件到此处，或 <em>点击上传</em>
                </div>
                <template #tip>
                  <div class="el-upload__tip">支持 PDF、Word 文档，文件大小不超过 20MB</div>
                </template>
              </el-upload>

              <el-progress
                v-if="contractStore.uploading || contractStore.uploadProgress > 0"
                :percentage="contractStore.uploadProgress"
                :status="contractStore.uploadProgress === 100 ? 'success' : ''"
                style="margin-top: 16px"
              />

              <div v-if="contractStore.currentContract && !contractStore.analysisResult" class="analyze-section">
                <el-card shadow="hover">
                  <div class="uploaded-info">
                    <el-icon :size="24"><document /></el-icon>
                    <div>
                      <p class="filename">{{ contractStore.currentContract.filename }}</p>
                      <p class="file-meta">{{ formatSize(contractStore.currentContract.fileSize) }} · {{ statusText(contractStore.currentContract.status) }}</p>
                    </div>
                  </div>
                  <el-button
                    type="primary"
                    size="large"
                    style="width: 100%; margin-top: 16px"
                    @click="handleAnalyze"
                    :loading="contractStore.analyzing"
                    :disabled="contractStore.analyzing"
                  >
                    {{ contractStore.analyzing ? '分析进行中，请稍候...' : '开始合同分析' }}
                  </el-button>
                  <p v-if="contractStore.analyzing" class="analyzing-hint">
                    合同分析通常需要30-60秒，结果将自动显示
                  </p>
                </el-card>
              </div>
            </div>

            <!-- Analysis Result -->
            <div v-if="contractStore.analysisResult" class="result-section">
              <el-button text @click="contractStore.reset()" style="margin-bottom: 12px">&lt; 返回上传</el-button>

              <el-card class="summary-card" :class="'risk-' + riskClass(contractStore.analysisResult.overallRisk)">
                <div class="summary-header">
                  <h3>合同分析报告</h3>
                  <el-tag :type="riskTagType(contractStore.analysisResult.overallRisk)" size="large" effect="dark">
                    整体风险：{{ contractStore.analysisResult.overallRisk }}
                  </el-tag>
                </div>
                <p class="summary-text" v-html="renderMarkdown(contractStore.analysisResult.summary)"></p>
              </el-card>

              <el-tabs v-model="activeTab" style="margin-top: 16px">
                <el-tab-pane label="条款分析" name="clauses">
                  <div class="clauses-section">
                    <h4>条款分析（{{ contractStore.analysisResult.clauses.length }}项）</h4>
                    <el-card
                      v-for="clause in contractStore.analysisResult.clauses"
                      :key="clause.index"
                      class="clause-card"
                      :class="'clause-' + riskClass(clause.riskLevel)"
                    >
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
                  <div v-if="suggestions.length === 0" style="text-align: center; padding: 40px; color: #999">
                    <el-button type="primary" @click="loadSuggestions" :loading="suggestionsLoading">生成修改建议</el-button>
                  </div>
                  <div v-else>
                    <el-card v-for="s in suggestions" :key="s.clauseIndex" class="suggestion-card" :class="'clause-' + riskClass(s.riskLevel)">
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
                  <div style="padding: 20px; text-align: center">
                    <p style="color: #666; margin-bottom: 16px">选择两份已分析的合同进行AI对比分析</p>
                    <el-select v-model="compareId1" placeholder="选择合同一" style="width: 200px; margin-right: 12px">
                      <el-option v-for="c in contractStore.contracts.filter(c => c.status === 'COMPLETED')" :key="c.id" :label="c.filename" :value="c.id" />
                    </el-select>
                    <el-select v-model="compareId2" placeholder="选择合同二" style="width: 200px">
                      <el-option v-for="c in contractStore.contracts.filter(c => c.status === 'COMPLETED')" :key="c.id" :label="c.filename" :value="c.id" />
                    </el-select>
                    <el-button type="primary" @click="handleCompare" :loading="compareLoading" :disabled="!compareId1 || !compareId2" style="margin-left: 12px">开始对比</el-button>
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

      <!-- ===== Tab 2: Template Library (always accessible) ===== -->
      <el-tab-pane label="合同模板库" name="templates">
        <div class="template-library">
          <div class="template-toolbar">
            <el-select
              v-model="templateCategory"
              placeholder="全部分类"
              clearable
              @change="filterTemplates"
              style="width: 200px"
            >
              <el-option v-for="cat in templateCategories" :key="cat" :label="cat" :value="cat" />
            </el-select>
            <el-input
              v-model="templateKeyword"
              placeholder="搜索模板名称..."
              clearable
              style="width: 240px; margin-left: 12px"
              @input="filterTemplates"
            />
            <span class="template-count" v-if="filteredTemplates.length > 0">共 {{ filteredTemplates.length }} 个模板</span>
          </div>

          <el-skeleton :loading="templatesLoading" animated :count="3">
            <template #default>
              <div class="template-grid">
                <el-card
                  v-for="t in filteredTemplates"
                  :key="t.id"
                  shadow="hover"
                  class="template-card"
                  @click="showTemplateDetail(t)"
                >
                  <div class="template-card-header">
                    <el-tag size="small" type="primary">{{ t.category }}</el-tag>
                    <el-tag v-if="t.applicableLaw" size="small" type="info" style="margin-left: 6px">{{ t.applicableLaw }}</el-tag>
                  </div>
                  <div class="template-title">{{ t.title }}</div>
                  <div class="template-desc">{{ t.description || '暂无描述' }}</div>
                  <div class="template-footer">
                    <el-button size="small" type="primary" text>查看模板 →</el-button>
                  </div>
                </el-card>
              </div>
              <el-empty v-if="!templatesLoading && filteredTemplates.length === 0" description="暂无模板" style="margin-top: 60px" />
            </template>
          </el-skeleton>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Template Detail Dialog -->
    <el-dialog
      v-model="templateDetailVisible"
      :title="selectedTemplate?.title"
      width="720px"
      top="5vh"
      destroy-on-close
    >
      <div v-if="selectedTemplate">
        <div class="template-detail-meta">
          <el-tag type="primary">{{ selectedTemplate.category }}</el-tag>
          <el-tag v-if="selectedTemplate.applicableLaw" type="info" style="margin-left: 8px">{{ selectedTemplate.applicableLaw }}</el-tag>
          <span v-if="selectedTemplate.description" style="margin-left: 12px; color: #666; font-size: 13px">{{ selectedTemplate.description }}</span>
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
import { UploadFilled, Document } from '@element-plus/icons-vue'
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

// Template state
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
    list = list.filter((t: any) =>
      t.title?.toLowerCase().includes(kw) || t.description?.toLowerCase().includes(kw)
    )
  }
  return list
})

onMounted(() => {
  contractStore.loadContracts()
})

watch(mainTab, (tab) => {
  if (tab === 'templates' && !templateLoaded.value) {
    loadAllTemplates()
  }
})

async function handleFileChange(uploadFile: any) {
  const file = uploadFile.raw as File
  if (!file) return

  const allowedExts = ['pdf', 'doc', 'docx']
  const ext = file.name.split('.').pop()?.toLowerCase() || ''
  if (!allowedExts.includes(ext)) {
    ElMessage.error('仅支持 PDF 和 Word 文档')
    return
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 20MB')
    return
  }

  try {
    await contractStore.uploadFile(file)
    ElMessage.success('文件上传成功')
  } catch (e: any) {
    ElMessage.error(e.message || '上传失败')
  }
}

async function handleAnalyze() {
  if (!contractStore.currentContract) return
  try {
    await contractStore.analyzeContract(contractStore.currentContract.id)
    ElMessage.success('合同分析完成')
  } catch (e: any) {
    ElMessage.error(e.message || '分析失败')
  }
}

async function handleSelectContract(c: ContractDocument) {
  contractStore.currentContract = c
  contractStore.analysisResult = null
  if (c.status === 'COMPLETED') {
    try {
      await contractStore.getAnalysis(c.id)
    } catch (e: any) {
      ElMessage.warning('加载分析结果失败')
    }
  }
}

function statusType(status: string) {
  const map: Record<string, string> = {
    UPLOADED: 'info', PARSING: 'warning', PARSED: '', ANALYZING: 'warning', COMPLETED: 'success', FAILED: 'danger'
  }
  return map[status] || 'info'
}

function statusText(status: string) {
  const map: Record<string, string> = {
    UPLOADED: '已上传', PARSING: '解析中', PARSED: '已解析', ANALYZING: '分析中', COMPLETED: '已完成', FAILED: '失败'
  }
  return map[status] || status
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
}

function riskClass(level: string) {
  if (level === '高') return 'high'
  if (level === '中') return 'medium'
  return 'low'
}

function riskTagType(level: string) {
  if (level === '高') return 'danger'
  if (level === '中') return 'warning'
  return 'success'
}

async function loadSuggestions() {
  if (!contractStore.currentContract) return
  suggestionsLoading.value = true
  try {
    const res = await api.get(`/contract/${contractStore.currentContract.id}/suggestions`)
    suggestions.value = (res.data as any[]) || []
  } catch {
    ElMessage.error('加载修改建议失败')
  } finally {
    suggestionsLoading.value = false
  }
}

async function handleCompare() {
  if (!compareId1.value || !compareId2.value) return
  compareLoading.value = true
  try {
    const res = await api.post('/contract/compare', null, {
      params: { contractId1: compareId1.value, contractId2: compareId2.value }
    })
    compareResult.value = res.data as string
  } catch {
    ElMessage.error('合同对比失败')
  } finally {
    compareLoading.value = false
  }
}

async function loadAllTemplates() {
  templatesLoading.value = true
  try {
    const res = await api.get('/contract/templates')
    templates.value = (res.data as any[]) || []
    templateLoaded.value = true
  } catch {
    ElMessage.error('加载模板列表失败')
    templates.value = []
  } finally {
    templatesLoading.value = false
  }
}

function filterTemplates() {
  // filteredTemplates is a computed property, nothing to do here
}

async function showTemplateDetail(t: any) {
  try {
    const res = await api.get(`/contract/templates/${t.id}`)
    selectedTemplate.value = res.data as any
    templateDetailVisible.value = true
  } catch {
    ElMessage.error('加载模板详情失败')
  }
}

async function copyTemplateContent() {
  const text = selectedTemplate.value?.content
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败，请手动选择文本复制')
  }
}
</script>

<style scoped>
.contract-view {
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
}
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
.el-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  padding: 12px;
  overflow-y: auto;
}
.sidebar-header h4 {
  margin: 0 0 12px 0;
  color: #333;
}
.contract-item {
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 6px;
  transition: background 0.2s;
}
.contract-item:hover { background: #f0f2f5; }
.contract-item.active { background: #e3f2fd; }
.contract-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.contract-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}
.contract-size {
  font-size: 12px;
  color: #999;
}
.placeholder-text {
  color: #999;
  font-size: 13px;
  text-align: center;
  margin-top: 20px;
}
.upload-section {
  max-width: 600px;
  margin: 40px auto;
}
.analyze-section {
  margin-top: 24px;
}
.uploaded-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.analyzing-hint {
  text-align: center;
  color: #909399;
  font-size: 13px;
  margin-top: 8px;
}
.filename {
  font-weight: 500;
  margin: 0;
}
.file-meta {
  font-size: 12px;
  color: #999;
  margin: 2px 0 0 0;
}
.result-section {
  padding: 0 20px 20px;
}
.summary-card {
  margin-bottom: 20px;
}
.summary-card.risk-high { border-left: 4px solid #f56c6c; }
.summary-card.risk-medium { border-left: 4px solid #e6a23c; }
.summary-card.risk-low { border-left: 4px solid #67c23a; }
.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.summary-header h3 { margin: 0; }
.summary-text {
  color: #666;
  line-height: 1.6;
}
.clauses-section h4 {
  margin: 20px 0 12px;
}
.clause-card {
  margin-bottom: 12px;
}
.clause-card.clause-high { border-left: 4px solid #f56c6c; }
.clause-card.clause-medium { border-left: 4px solid #e6a23c; }
.clause-card.clause-low { border-left: 4px solid #67c23a; }
.clause-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.clause-title {
  font-weight: 500;
  font-size: 14px;
}
.content-text {
  background: #f9fafb;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  color: #555;
  line-height: 1.5;
  margin: 8px 0;
  max-height: 120px;
  overflow-y: auto;
}
.clause-risk, .clause-legal, .clause-suggestion {
  font-size: 13px;
  line-height: 1.6;
  margin-top: 6px;
}
.clause-risk { color: #f56c6c; }
.clause-legal { color: #409eff; }
.clause-suggestion { color: #67c23a; }
.risk-category {
  font-size: 12px;
  color: #999;
}
.suggestion-card {
  margin-bottom: 12px;
}
.suggestion-original {
  margin-top: 6px;
}
.suggestion-text {
  color: #67c23a;
  font-size: 13px;
  line-height: 1.6;
  margin-top: 6px;
}
.ai-detail {
  margin-top: 8px;
}
.ai-detail-content {
  font-size: 13px;
  line-height: 1.8;
  color: #555;
  max-height: 400px;
  overflow-y: auto;
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
.template-library {
  padding: 20px;
  height: calc(100vh - 105px);
  overflow-y: auto;
}
.template-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 8px;
}
.template-count {
  color: #909399;
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
  transition: transform 0.15s, box-shadow 0.15s;
}
.template-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}
.template-card-header {
  margin-bottom: 10px;
}
.template-title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
  margin-bottom: 6px;
  line-height: 1.4;
}
.template-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.template-footer {
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
  padding-top: 10px;
}
.template-detail-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}
.template-detail-content {
  font-size: 14px;
  line-height: 1.9;
  color: #333;
  max-height: 60vh;
  overflow-y: auto;
  background: #fafafa;
  padding: 16px;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}
</style>
