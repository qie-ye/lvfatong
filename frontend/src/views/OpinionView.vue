<template>
  <div class="opinion-page">
    <div class="page-header">
      <h1 class="page-title">法律意见书</h1>
      <el-tooltip content="选择场景、填写案情，AI将生成结构化法律意见书" placement="bottom">
        <el-button type="primary" class="generate-btn" @click="showGenerateDialog = true" :disabled="!aiAvailable">生成意见书</el-button>
      </el-tooltip>
    </div>

    <el-alert v-if="!aiAvailable" type="error" :closable="false" show-icon style="margin-bottom: 16px">
      <template #title>AI服务未配置</template>
      {{ aiMessage || 'ZHIPU_API_KEY未设置，法律意见书生成功能不可用。请联系管理员配置。' }}
    </el-alert>

    <div class="content-layout">
      <div class="opinion-list">
        <div v-loading="opinionStore.loading" class="list-scroll">
          <div
            v-for="op in opinionStore.opinions"
            :key="op.id"
            class="opinion-card"
            :class="{ active: currentId === op.id }"
            @click="selectOpinion(op.id)"
          >
            <div class="op-title">{{ op.title }}</div>
            <div class="op-meta">
              <span class="status-tag" :class="'status-' + op.status.toLowerCase()">{{ statusLabel(op.status) }}</span>
              <span class="op-date">{{ formatDate(op.createdAt) }}</span>
            </div>
          </div>
        </div>
        <div v-if="!opinionStore.loading && opinionStore.opinions.length === 0" class="empty-state">
          <div class="empty-icon-circle">
            <el-icon :size="28"><ScaleToOriginal /></el-icon>
          </div>
          <p>暂无法律意见书</p>
          <span>点击右上角「生成意见书」，选择场景、填写案情，AI将生成结构化法律意见书</span>
        </div>
      </div>

      <div class="opinion-detail" v-loading="opinionStore.loading">
        <template v-if="opinionStore.currentOpinion">
          <div v-if="opinionStore.currentOpinion.status === 'GENERATING'" class="status-screen">
            <el-icon class="is-loading" :size="36"><Loading /></el-icon>
            <p>正在使用 GLM-4-Plus 深度推理生成法律意见书，请稍候...</p>
            <el-button type="primary" link @click="refreshOpinion">刷新查看</el-button>
          </div>
          <div v-else-if="opinionStore.currentOpinion.status === 'FAILED'" class="status-screen failed-screen">
            <el-icon :size="36" color="#ef4444"><CircleCloseFilled /></el-icon>
            <p>意见书生成失败，请重新尝试</p>
          </div>
          <template v-else>
            <h1 class="detail-title">{{ opinionStore.currentOpinion.title }}</h1>
            <div class="detail-meta">
              <el-tag size="small">{{ opinionStore.currentOpinion.domain || '综合' }}</el-tag>
              <span>模型: {{ opinionStore.currentOpinion.model }}</span>
              <span>{{ formatDate(opinionStore.currentOpinion.createdAt) }}</span>
            </div>
            <el-divider />
            <div v-if="opinionStore.currentOpinion.question" class="section">
              <h3>咨询问题</h3>
              <div class="opinion-content" v-html="renderMarkdown(opinionStore.currentOpinion.question)"></div>
            </div>
            <div v-if="opinionStore.currentOpinion.facts" class="section">
              <h3>案件事实</h3>
              <div class="opinion-content" v-html="renderMarkdown(opinionStore.currentOpinion.facts)"></div>
            </div>
            <div v-if="opinionStore.currentOpinion.analysis" class="section">
              <h3>法律分析</h3>
              <div class="opinion-content" v-html="renderMarkdown(opinionStore.currentOpinion.analysis)"></div>
            </div>
            <div v-if="opinionStore.currentOpinion.conclusion" class="section">
              <h3>结论</h3>
              <div class="opinion-content" v-html="renderMarkdown(opinionStore.currentOpinion.conclusion)"></div>
            </div>
            <div v-if="opinionStore.currentOpinion.legalBasis" class="section">
              <h3>法律依据</h3>
              <div class="opinion-content" v-html="renderMarkdown(opinionStore.currentOpinion.legalBasis)"></div>
            </div>
            <div v-if="opinionStore.currentOpinion.suggestions" class="section">
              <h3>建议</h3>
              <div class="opinion-content" v-html="renderMarkdown(opinionStore.currentOpinion.suggestions)"></div>
            </div>
            <el-alert type="warning" :closable="false" style="margin-top: 20px" class="disclaimer-alert">
              本法律意见书由AI生成，仅供参考，不构成正式法律意见。如需正式法律服务，请咨询持证律师。
            </el-alert>
          </template>
        </template>
        <div v-else class="empty-state">
          <div class="empty-icon-circle">
            <el-icon :size="28"><ScaleToOriginal /></el-icon>
          </div>
          <p>请从左侧选择意见书，或点击右上角创建新的</p>
          <span>选择已生成的意见书查看详情，或创建新的法律意见书</span>
        </div>
      </div>
    </div>

    <el-dialog v-model="showGenerateDialog" title="生成法律意见书" width="600px" @close="resetForm">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="如：劳动争议法律意见" maxlength="300" />
        </el-form-item>
        <el-form-item label="法律领域">
          <el-select v-model="form.domain" placeholder="选择领域" clearable style="width: 100%">
            <el-option v-for="d in domains" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述" required>
          <el-input v-model="form.question" type="textarea" :rows="4" placeholder="详细描述您遇到的法律问题" maxlength="5000" show-word-limit />
        </el-form-item>
        <el-form-item label="案件事实">
          <el-input v-model="form.facts" type="textarea" :rows="4" placeholder="描述相关事实经过、时间线等" maxlength="10000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGenerateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleGenerate" :loading="generating">生成意见书</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, CircleCloseFilled, ScaleToOriginal } from '@element-plus/icons-vue'
import { useOpinionStore } from '@/stores/opinion'
import { renderMarkdown } from '@/utils/renderMarkdown'
import api from '@/api'

const opinionStore = useOpinionStore()
const currentId = ref<number | null>(null)
const showGenerateDialog = ref(false)
const generating = ref(false)
const aiAvailable = ref(true)
const aiMessage = ref('')

const domains = ['劳动法', '合同法', '婚姻法', '房产法', '知识产权', '公司法', '刑事', '交通事故', '医疗纠纷', '行政法']

const form = reactive({
  title: '',
  domain: '',
  question: '',
  facts: ''
})

onMounted(async () => {
  opinionStore.loadOpinions()
  try {
    const res = await api.get('/system/ai-status')
    aiAvailable.value = res.data.available
    aiMessage.value = res.data.message || ''
    if (!aiAvailable.value) {
      ElMessage.warning({ message: aiMessage.value, duration: 5000 })
    }
  } catch {
    // ignore
  }
})

async function selectOpinion(id: number) {
  currentId.value = id
  await opinionStore.getOpinion(id)
}

async function refreshOpinion() {
  if (currentId.value) {
    await opinionStore.getOpinion(currentId.value)
  }
}

async function handleGenerate() {
  if (!aiAvailable.value) {
    ElMessage.error(aiMessage.value || 'AI服务未配置，无法生成法律意见书')
    return
  }
  if (!form.title.trim() || !form.question.trim()) {
    ElMessage.warning('请填写标题和问题描述')
    return
  }
  generating.value = true
  try {
    const result = await opinionStore.generateOpinion(form.title, form.domain, form.question, form.facts)
    showGenerateDialog.value = false
    ElMessage.success('意见书生成中，请稍后查看')
    await opinionStore.loadOpinions()
    currentId.value = result.id
    pollOpinionStatus(result.id)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '生成失败'
    ElMessage.error(msg.includes('401') ? '请先登录后再试' : msg || '生成失败，请检查后端服务是否正常')
  } finally {
    generating.value = false
  }
}

function pollOpinionStatus(id: number) {
  let count = 0
  const timer = setInterval(async () => {
    count++
    if (count > 60) {
      clearInterval(timer)
      return
    }
    await opinionStore.getOpinion(id)
    if (opinionStore.currentOpinion?.status !== 'GENERATING') {
      clearInterval(timer)
      await opinionStore.loadOpinions()
    }
  }, 5000)
}

function resetForm() {
  form.title = ''
  form.domain = ''
  form.question = ''
  form.facts = ''
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
</script>

<style scoped>
.opinion-page {
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

.opinion-list {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.list-scroll {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  overflow-y: auto;
}

.opinion-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 2px solid transparent;
}

.opinion-card:hover {
  border-color: #2563eb;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);
}

.opinion-card.active {
  border-left-color: #2563eb;
  background: #f8fafc;
  border-color: #e5e7eb;
  border-left-color: #2563eb;
}

.op-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.op-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.op-date {
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

.opinion-detail {
  flex: 1;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 28px;
}

.detail-title {
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

.opinion-content {
  color: #4b5563;
  line-height: 1.8;
  font-size: 14px;
}

.opinion-content :deep(.md-h1),
.opinion-content :deep(.md-h2),
.opinion-content :deep(.md-h3) {
  font-weight: 700;
  margin: 14px 0 8px;
  color: #111827;
}

.opinion-content :deep(.md-h1) { font-size: 17px; }
.opinion-content :deep(.md-h2) { font-size: 16px; }
.opinion-content :deep(.md-h3) { font-size: 15px; }
.opinion-content :deep(.md-li) { margin: 6px 0; padding-left: 2px; }
.opinion-content :deep(.md-idx) { font-weight: 600; }
.opinion-content :deep(.md-bold-line) { margin: 10px 0 4px; }
.opinion-content :deep(.md-intro) { margin-bottom: 8px; }

.opinion-content :deep(.md-section) {
  margin: 14px 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: #f8fafc;
}

.opinion-content :deep(.md-section-title) {
  padding: 10px 14px;
  font-weight: 700;
  color: #111827;
  background: rgba(37, 99, 235, 0.04);
  border-bottom: 1px solid #e5e7eb;
}

.opinion-content :deep(.md-section-body) { padding: 14px; }

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
html.dark .opinion-card { background: var(--bg-card); border-color: var(--border); }
html.dark .opinion-card.active { background: rgba(59,130,246,0.1); border-color: #3b82f6; }
html.dark .op-title { color: var(--text-primary); }
html.dark .op-date { color: var(--text-tertiary); }
html.dark .opinion-detail { background: var(--bg-card); border-color: var(--border); }
html.dark .detail-title { color: var(--text-primary); }
html.dark .detail-meta { color: var(--text-secondary); }
html.dark .section h3 { color: var(--text-primary); border-left-color: #3b82f6; }
html.dark .opinion-content { color: var(--text-secondary); }
html.dark .opinion-content :deep(.md-h1),
html.dark .opinion-content :deep(.md-h2),
html.dark .opinion-content :deep(.md-h3) { color: var(--text-primary); }
html.dark .opinion-content :deep(.md-section) { background: rgba(255,255,255,0.03); border-color: var(--border); }
html.dark .opinion-content :deep(.md-section-title) { color: var(--text-primary); border-color: var(--border); background: rgba(59,130,246,0.08); }
html.dark .empty-icon-circle { background: rgba(59,130,246,0.1); }
html.dark .empty-state p { color: var(--text-secondary); }
html.dark .empty-state span { color: var(--text-tertiary); }
html.dark .status-screen { color: var(--text-secondary); }
</style>
