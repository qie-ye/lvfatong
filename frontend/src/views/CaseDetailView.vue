<template>
  <div class="case-detail" v-loading="loading">
    <el-page-header @back="$router.push('/cases')" title="返回案例列表" />

    <div v-if="caseData" class="detail-content">
      <el-card class="main-card">
        <h1>{{ caseData.title }}</h1>
        <div class="meta-bar">
          <el-tag size="small">{{ caseData.caseType || '未分类' }}</el-tag>
          <span v-if="caseData.caseNo" class="meta-item">案号: {{ caseData.caseNo }}</span>
          <span v-if="caseData.court" class="meta-item">法院: {{ caseData.court }}</span>
          <span v-if="caseData.year" class="meta-item">{{ caseData.year }}年</span>
          <span v-if="caseData.domain" class="meta-item">{{ caseData.domain }}</span>
          <span v-if="caseData.province" class="meta-item">{{ caseData.province }}</span>
        </div>

        <div v-if="caseData.keywords" class="keywords">
          <el-tag v-for="kw in caseData.keywords.split(',')" :key="kw" size="small" type="info" style="margin-right: 4px">{{ kw.trim() }}</el-tag>
        </div>

        <el-divider />

        <div v-if="caseData.summary" class="section">
          <h3>案件摘要</h3>
          <div class="ai-content" v-html="renderMarkdown(caseData.summary)"></div>
        </div>

        <div v-if="caseData.facts" class="section">
          <h3>案件事实</h3>
          <div class="ai-content" v-html="renderMarkdown(caseData.facts)"></div>
        </div>

        <div v-if="caseData.ruling" class="section">
          <h3>裁判结果</h3>
          <div class="ai-content" v-html="renderMarkdown(caseData.ruling)"></div>
        </div>

        <div v-if="caseData.analysis" class="section">
          <h3>法律分析</h3>
          <div class="ai-content" v-html="renderMarkdown(caseData.analysis)"></div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { renderMarkdown } from '@/utils/renderMarkdown'
import api from '@/api'

interface CaseDetail {
  id: number
  title: string
  caseNo: string
  caseType: string
  court: string
  year: string
  domain: string
  keywords: string
  province: string
  summary: string
  facts: string
  ruling: string
  analysis: string
}

const route = useRoute()
const loading = ref(false)
const caseData = ref<CaseDetail | null>(null)

onMounted(async () => {
  loading.value = true
  try {
    const id = route.params.id
    const res = await api.get(`/cases/${id}`)
    caseData.value = res.data as CaseDetail
  } catch {
    ElMessage.error('案例加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.case-detail {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.main-card {
  margin-top: 16px;
}

.main-card h1 {
  font-size: 20px;
  color: #1a1a2e;
  margin: 0 0 12px;
}

.meta-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.meta-item {
  font-size: 13px;
  color: #666;
}

.keywords {
  margin-top: 8px;
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
</style>
