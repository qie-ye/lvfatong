<template>
  <div class="case-page">
    <div class="search-section">
      <h2>案例检索</h2>
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="输入关键词搜索案例..." clearable size="large" @keyup.enter="handleSearch" style="width: 400px">
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
        <el-button type="primary" plain @click="showSemantic = true" style="margin-left: 12px">AI语义检索</el-button>
      </div>
      <div class="filters">
        <el-select v-model="caseType" placeholder="案件类型" clearable @change="handleSearch" style="width: 140px">
          <el-option v-for="t in caseTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="domain" placeholder="法律领域" clearable @change="handleSearch" style="width: 140px; margin-left: 8px">
          <el-option v-for="d in domains" :key="d" :label="d" :value="d" />
        </el-select>
        <el-select v-model="year" placeholder="年份" clearable @change="handleSearch" style="width: 100px; margin-left: 8px">
          <el-option v-for="y in years" :key="y" :label="y" :value="y" />
        </el-select>
      </div>
    </div>

    <div v-loading="loading" class="results">
      <el-card v-for="c in cases" :key="c.id" shadow="hover" class="case-card" @click="goDetail(c.id)">
        <div class="case-header">
          <h3>{{ c.title }}</h3>
          <el-tag size="small" type="info">{{ c.caseType || '未分类' }}</el-tag>
        </div>
        <div class="case-meta">
          <span v-if="c.caseNo">案号: {{ c.caseNo }}</span>
          <span v-if="c.court"> | {{ c.court }}</span>
          <span v-if="c.year"> | {{ c.year }}年</span>
          <span v-if="c.domain"> | {{ c.domain }}</span>
        </div>
        <p class="case-summary">{{ c.summary }}</p>
        <div v-if="c.keywords" class="case-keywords">
          <el-tag v-for="kw in c.keywords.split(',').slice(0, 5)" :key="kw" size="small" style="margin-right: 4px">{{ kw.trim() }}</el-tag>
        </div>
      </el-card>
    </div>

    <el-empty v-if="!loading && cases.length === 0 && searched" description="未找到相关案例">
      <template #description>
        <p style="color: #909399">未找到相关案例</p>
        <p style="color: #c0c4cc; font-size: 12px; margin-top: 4px">请尝试其他关键词，或确认案例数据已导入</p>
      </template>
    </el-empty>

    <div class="pagination" v-if="total > pageSize">
      <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="handlePageChange" />
    </div>

    <!-- AI语义检索对话框 -->
    <el-dialog v-model="showSemantic" title="AI语义检索案例" width="500px">
      <p style="color: #666; margin-bottom: 12px">用自然语言描述您想查找的案例类型，AI将基于语义相似度匹配最相关的案例。</p>
      <el-input v-model="semanticQuery" type="textarea" :rows="3" placeholder="例如：劳动者因加班费与用人单位产生争议的案例" />
      <div v-if="semanticResults.length > 0" style="margin-top: 16px">
        <h4>相关案例</h4>
        <el-card v-for="r in semanticResults" :key="r.id" shadow="never" style="margin-bottom: 8px; cursor: pointer" @click="goDetail(Number(r.id))">
          <div style="font-weight: 600">{{ r.title || `案例 #${r.id}` }}</div>
          <div style="font-size: 13px; color: #666; margin-top: 4px">{{ truncate(r.content, 120) }}</div>
          <div style="font-size: 12px; color: #4fc3f7; margin-top: 4px">相似度: {{ (r.score * 100).toFixed(1) }}%</div>
        </el-card>
      </div>
      <template #footer>
        <el-button @click="showSemantic = false">关闭</el-button>
        <el-button type="primary" @click="handleSemanticSearch" :loading="semanticLoading">检索</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api'

interface CaseItem {
  id: number
  title: string
  caseNo: string
  caseType: string
  court: string
  year: string
  domain: string
  keywords: string
  summary: string
}

interface SemanticResult {
  id: string
  title: string
  content: string
  score: number
}

const router = useRouter()
const keyword = ref('')
const caseType = ref('')
const domain = ref('')
const year = ref('')
const cases = ref<CaseItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 10
const loading = ref(false)
const searched = ref(false)

const showSemantic = ref(false)
const semanticQuery = ref('')
const semanticResults = ref<SemanticResult[]>([])
const semanticLoading = ref(false)

const caseTypes = ['民事', '刑事', '行政', '执行', '赔偿']
const domains = ['劳动', '合同', '婚姻', '房产', '知识产权', '公司', '交通事故', '医疗']
const years = ['2024', '2023', '2022', '2021', '2020', '2019']

onMounted(() => handleSearch())

async function handleSearch() {
  loading.value = true
  searched.value = true
  try {
    const res = await api.get('/cases', {
      params: {
        keyword: keyword.value || undefined,
        caseType: caseType.value || undefined,
        domain: domain.value || undefined,
        year: year.value || undefined,
        page: currentPage.value - 1,
        size: pageSize
      }
    })
    const data = res.data as { content: CaseItem[]; totalElements: number }
    cases.value = data.content || []
    total.value = data.totalElements || 0
  } catch (e: unknown) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  handleSearch()
}

function goDetail(id: number) {
  router.push(`/cases/${id}`)
}

async function handleSemanticSearch() {
  if (!semanticQuery.value.trim()) {
    ElMessage.warning('请输入检索描述')
    return
  }
  semanticLoading.value = true
  try {
    const res = await api.post('/cases/semantic-search', null, {
      params: { query: semanticQuery.value, topK: 5 }
    })
    semanticResults.value = (res.data as SemanticResult[]) || []
  } catch {
    ElMessage.error('语义检索失败')
  } finally {
    semanticLoading.value = false
  }
}

function truncate(text: string, len: number) {
  if (!text) return ''
  return text.length > len ? text.substring(0, len) + '...' : text
}
</script>

<style scoped>
.case-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.search-section {
  margin-bottom: 20px;
}

.search-section h2 {
  margin: 0 0 12px;
  color: #1a1a2e;
}

.search-bar {
  display: flex;
  align-items: center;
}

.filters {
  display: flex;
  margin-top: 12px;
}

.case-card {
  margin-bottom: 12px;
  cursor: pointer;
  transition: transform 0.2s;
}

.case-card:hover {
  transform: translateY(-1px);
}

.case-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.case-header h3 {
  margin: 0;
  font-size: 15px;
  color: #1a1a2e;
  flex: 1;
}

.case-meta {
  font-size: 12px;
  color: #999;
  margin: 4px 0 8px;
}

.case-summary {
  font-size: 13px;
  color: #555;
  line-height: 1.5;
  margin: 0;
}

.case-keywords {
  margin-top: 8px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
