<template>
  <div class="opinion-page">
    <div class="page-header">
      <h2>法律意见书</h2>
      <el-button type="primary" @click="showGenerateDialog = true" :disabled="!aiAvailable">生成意见书</el-button>
    </div>

    <el-alert v-if="!aiAvailable" type="error" :closable="false" show-icon style="margin-bottom: 16px">
      <template #title>AI服务未配置</template>
      {{ aiMessage || 'ZHIPU_API_KEY未设置，法律意见书生成功能不可用。请联系管理员配置。' }}
    </el-alert>

    <div class="content-layout">
      <!-- 左侧列表 -->
      <div class="opinion-list">
        <div v-loading="opinionStore.loading">
          <el-card
            v-for="op in opinionStore.opinions"
            :key="op.id"
            shadow="hover"
            class="opinion-card"
            :class="{ active: currentId === op.id }"
            @click="selectOpinion(op.id)"
          >
            <div class="op-title">{{ op.title }}</div>
            <div class="op-meta">
              <el-tag size="small" :type="statusType(op.status)">{{ statusLabel(op.status) }}</el-tag>
              <span v-if="op.domain" class="op-domain">{{ op.domain }}</span>
              <span class="op-date">{{ formatDate(op.createdAt) }}</span>
            </div>
          </el-card>
        </div>
        <el-empty v-if="!opinionStore.loading && opinionStore.opinions.length === 0" description="暂无法律意见书" />
      </div>

      <!-- 右侧详情 -->
      <div class="opinion-detail" v-loading="opinionStore.loading">
        <template v-if="opinionStore.currentOpinion">
          <div v-if="opinionStore.currentOpinion.status === 'GENERATING'" class="generating">
            <el-icon class="is-loading" :size="32"><Loading /></el-icon>
            <p>正在使用 GLM-4-Plus 深度推理生成法律意见书，请稍候...</p>
            <el-button type="primary" link @click="refreshOpinion">刷新查看</el-button>
          </div>
          <div v-else-if="opinionStore.currentOpinion.status === 'FAILED'" class="failed">
            <el-icon :size="32" color="#f56c6c"><CircleCloseFilled /></el-icon>
            <p>意见书生成失败，请重新尝试</p>
          </div>
          <template v-else>
            <h1>{{ opinionStore.currentOpinion.title }}</h1>
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

            <div v-if="opinionStore.currentOpinion.analysis" class="section highlight">
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

            <div v-if="opinionStore.currentOpinion.suggestions" class="section highlight">
              <h3>建议</h3>
              <div class="opinion-content" v-html="renderMarkdown(opinionStore.currentOpinion.suggestions)"></div>
            </div>

            <el-alert type="warning" :closable="false" style="margin-top: 20px">
              本法律意见书由AI生成，仅供参考，不构成正式法律意见。如需正式法律服务，请咨询持证律师。
            </el-alert>
          </template>
        </template>
        <el-empty v-else description="请从左侧选择或创建新的法律意见书" />
      </div>
    </div>

    <!-- 生成对话框 -->
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
import { Loading, CircleCloseFilled } from '@element-plus/icons-vue'
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
    // endpoint may not exist in older versions, ignore
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
    // 自动轮询检查状态
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
    if (count > 60) { // 最多5分钟
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

.opinion-list {
  width: 280px;
  flex-shrink: 0;
}

.opinion-card {
  margin-bottom: 8px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.opinion-card.active {
  border-color: #4fc3f7;
}

.op-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.op-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #999;
}

.op-domain {
  color: #666;
}

.opinion-detail {
  flex: 1;
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.opinion-detail h1 {
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

.section.highlight h3 {
  border-left-color: #e6a23c;
}

.section p {
  color: #333;
  line-height: 1.8;
  font-size: 14px;
  white-space: pre-wrap;
}

.opinion-content {
  color: #333;
  line-height: 1.8;
  font-size: 14px;
}

.opinion-content :deep(.md-h1),
.opinion-content :deep(.md-h2),
.opinion-content :deep(.md-h3) {
  font-weight: 700;
  margin: 14px 0 8px;
  color: #1f2d3d;
}

.opinion-content :deep(.md-h1) { font-size: 17px; }
.opinion-content :deep(.md-h2) { font-size: 16px; }
.opinion-content :deep(.md-h3) { font-size: 15px; }

.opinion-content :deep(.md-li) {
  margin: 6px 0;
  padding-left: 2px;
}

.opinion-content :deep(.md-idx) {
  font-weight: 600;
}

.opinion-content :deep(.md-bold-line) {
  margin: 10px 0 4px;
}

.opinion-content :deep(.md-intro) {
  margin-bottom: 8px;
}

.opinion-content :deep(.md-section) {
  margin: 14px 0;
  border: 1px solid #e7ecf3;
  border-radius: 10px;
  overflow: hidden;
  background: #fafcff;
}

.opinion-content :deep(.md-section-title) {
  padding: 10px 14px;
  font-weight: 700;
  color: #1f2d3d;
  background: #eef4ff;
  border-bottom: 1px solid #e7ecf3;
}

.opinion-content :deep(.md-section-body) {
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
