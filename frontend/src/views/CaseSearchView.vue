<template>
  <div class="case-search-view">
    <div class="page-header">
      <h1 class="page-title">司法案例检索</h1>
    </div>

    <div class="search-section">
      <div class="search-bar-row">
        <el-input v-model="keyword" placeholder="输入关键词搜索案例..." clearable size="large" @keyup.enter="handleSearch" class="search-input">
          <template #prepend><el-icon :size="18"><Search /></el-icon></template>
          <template #append><el-button @click="handleSearch" :loading="loading" class="search-btn">搜索</el-button></template>
        </el-input>
        <el-button plain @click="showSemantic = true" class="semantic-btn">AI 语义检索</el-button>
      </div>
      <div class="filter-row">
        <el-select v-model="caseType" placeholder="案件类型" clearable @change="handleSearch" class="filter-select">
          <el-option v-for="t in caseTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="domain" placeholder="法律领域" clearable @change="handleSearch" class="filter-select">
          <el-option v-for="d in domains" :key="d" :label="d" :value="d" />
        </el-select>
        <el-select v-model="year" placeholder="年份" clearable @change="handleSearch" class="filter-select filter-select--year">
          <el-option v-for="y in years" :key="y" :label="y" :value="y" />
        </el-select>
      </div>
    </div>

    <div v-if="!searched && recentCases.length > 0" class="recent-section">
      <div class="recent-header">
        <h3 class="recent-title">最近查看</h3>
        <button class="recent-clear" @click="clearRecent">清空</button>
      </div>
      <div class="recent-list">
        <div v-for="r in recentCases" :key="r.id" class="recent-item" @click="goDetail(r.id)">
          <span class="recent-name">{{ r.title }}</span>
          <span class="recent-time">{{ r.time }}</span>
        </div>
      </div>
    </div>

    <div v-if="loading" class="skeleton-list">
      <div v-for="i in 3" :key="i" class="skeleton-card">
        <div class="skeleton-line skeleton-line--title"></div>
        <div class="skeleton-line skeleton-line--meta"></div>
        <div class="skeleton-line skeleton-line--text"></div>
        <div class="skeleton-line skeleton-line--text skeleton-line--text--short"></div>
      </div>
    </div>

    <div v-else-if="cases.length > 0" class="results">
      <div v-for="c in cases" :key="c.id" class="case-card" @click="goDetail(c.id)">
        <div class="case-card__header">
          <h3 class="case-card__title">{{ c.title }}</h3>
          <span class="case-card__type-tag" v-if="c.caseType">{{ c.caseType }}</span>
        </div>
        <div class="case-card__meta">
          <span v-if="c.caseNo">{{ c.caseNo }}</span>
          <span v-if="c.caseNo && c.court" class="meta-sep">|</span>
          <span v-if="c.court">{{ c.court }}</span>
          <span v-if="c.court && c.year" class="meta-sep">|</span>
          <span v-if="c.year">{{ c.year }}年</span>
          <span v-if="c.year && c.domain" class="meta-sep">|</span>
          <span v-if="c.domain">{{ c.domain }}</span>
        </div>
        <p class="case-card__summary">{{ c.summary }}</p>
        <div v-if="c.keywords" class="case-card__keywords">
          <span v-for="kw in c.keywords.split(',').slice(0, 5)" :key="kw" class="case-kw-tag">{{ kw.trim() }}</span>
        </div>
      </div>
    </div>

    <div v-else-if="searched && cases.length === 0" class="empty-state">
      <div class="empty-state__icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="9" y1="12" x2="15" y2="12"/><line x1="9" y1="16" x2="15" y2="16"/>
        </svg>
      </div>
      <p class="empty-state__title">未找到相关案例</p>
      <p class="empty-state__desc">尝试调整关键词或清空筛选</p>
    </div>

    <div class="pagination" v-if="total > pageSize">
      <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="handlePageChange" />
    </div>

    <el-dialog v-model="showSemantic" title="AI 语义检索案例" width="520px">
      <p class="semantic-desc">用自然语言描述您想查找的案例类型，AI将基于语义相似度匹配最相关的案例。</p>
      <el-input v-model="semanticQuery" type="textarea" :rows="3" placeholder="例如：劳动者因加班费与用人单位产生争议的案例" />
      <div v-if="semanticResults.length > 0" class="semantic-results">
        <h4 class="semantic-results-title">相关案例</h4>
        <div v-for="r in semanticResults" :key="r.id" class="semantic-card" @click="goDetail(Number(r.id))">
          <div class="semantic-card__title">{{ r.title || `案例 #${r.id}` }}</div>
          <div class="semantic-card__content">{{ truncate(r.content, 120) }}</div>
          <div class="semantic-card__score">相似度: {{ (r.score * 100).toFixed(1) }}%</div>
        </div>
      </div>
      <template #footer><el-button @click="showSemantic = false">关闭</el-button><el-button type="primary" @click="handleSemanticSearch" :loading="semanticLoading">检索</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import api from '@/api'

interface CaseItem { id: number; title: string; caseNo: string; caseType: string; court: string; year: string; domain: string; keywords: string; summary: string }
interface SemanticResult { id: string; title: string; content: string; score: number }
interface RecentCase { id: number; title: string; time: string }

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

const STORAGE_KEY = 'lft_recent_cases'
const recentCases = reactive<RecentCase[]>(JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]').slice(0, 8))

onMounted(() => handleSearch())

function saveRecent(id: number, title: string) {
  const now = new Date()
  const time = `${now.getMonth() + 1}-${now.getDate()} ${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}`
  const idx = recentCases.findIndex(r => r.id === id)
  if (idx >= 0) { recentCases.splice(idx, 1) }
  recentCases.unshift({ id, title, time })
  if (recentCases.length > 8) recentCases.pop()
  localStorage.setItem(STORAGE_KEY, JSON.stringify(recentCases))
}

function clearRecent() { recentCases.length = 0; localStorage.removeItem(STORAGE_KEY) }

async function handleSearch() {
  loading.value = true; searched.value = true
  try {
    const res = await api.get('/cases', {
      params: { keyword: keyword.value || undefined, caseType: caseType.value || undefined, domain: domain.value || undefined, year: year.value || undefined, page: currentPage.value - 1, size: pageSize }
    })
    const data = res.data as { content: CaseItem[]; totalElements: number }
    cases.value = data.content || []
    total.value = data.totalElements || 0
  } catch { ElMessage.error('搜索失败') } finally { loading.value = false }
}

function handlePageChange(page: number) { currentPage.value = page; handleSearch() }

function goDetail(id: number) {
  const c = cases.value.find(item => item.id === id)
  if (c) saveRecent(id, c.title)
  router.push(`/cases/${id}`)
}

async function handleSemanticSearch() {
  if (!semanticQuery.value.trim()) { ElMessage.warning('请描述您要查找的案例'); return }
  semanticLoading.value = true
  try {
    const res = await api.post('/cases/semantic', { query: semanticQuery.value })
    semanticResults.value = (res.data as SemanticResult[]) || []
  } catch { ElMessage.error('语义检索失败') } finally { semanticLoading.value = false }
}

function truncate(text: string, len: number) { return (!text) ? '' : text.length > len ? text.substring(0, len) + '...' : text }
</script>

<style scoped>
.case-search-view { max-width: 960px; margin: 0 auto; padding: 40px 24px 60px; }
.page-header { margin-bottom: 28px; }
.page-title { font-size: 24px; font-weight: 600; color: #111827; margin: 0; }

.search-section { margin-bottom: 28px; }
.search-bar-row { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.search-input { flex: 1; min-width: 260px; }
.search-input :deep(.el-input__wrapper) { background: #f5f7fa; border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: none; transition: border-color 0.2s, box-shadow 0.2s; height: 44px; }
.search-input :deep(.el-input__wrapper:hover) { border-color: #2563eb; }
.search-input :deep(.el-input__wrapper.is-focus) { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37,99,235,0.08); }
.search-input :deep(.el-input-group__prepend) { background: transparent; border: none; color: #9ca3af; padding: 0 0 0 14px; }
.search-input :deep(.el-input-group__append) { background: transparent; border: none; padding: 0; }
.search-btn { height: 44px; border-radius: 0 8px 8px 0; padding: 0 24px; font-weight: 500; }
.semantic-btn { flex-shrink: 0; border: 1px solid #2563eb; color: #2563eb; background: #fff; border-radius: 8px; padding: 0 20px; height: 44px; font-weight: 500; transition: all 0.2s; }
.semantic-btn:hover { background: #eff6ff; border-color: #1d4ed8; color: #1d4ed8; }

.filter-row { display: flex; gap: 10px; margin-top: 14px; flex-wrap: wrap; }
.filter-select { width: 140px; }
.filter-select--year { width: 100px; }
.filter-select :deep(.el-input__wrapper) { background: #f5f7fa; border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: none; height: 38px; }
.filter-select :deep(.el-input__wrapper:hover) { border-color: #2563eb; }
.filter-select :deep(.el-input__wrapper.is-focus) { border-color: #2563eb; box-shadow: 0 0 0 2px rgba(37,99,235,0.08); }

/* Recent section */
.recent-section { margin-bottom: 28px; background: #fff; border: 1px solid #f3f4f6; border-radius: 12px; padding: 16px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.03); }
.recent-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.recent-title { font-size: 13px; font-weight: 600; color: #111827; margin: 0; }
.recent-clear { background: none; border: none; font-size: 12px; color: #9ca3af; cursor: pointer; padding: 2px 8px; border-radius: 4px; }
.recent-clear:hover { color: #ef4444; background: #fef2f2; }
.recent-list { display: flex; gap: 8px; flex-wrap: wrap; }
.recent-item {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 5px 12px; font-size: 12px; color: #4b5563;
  background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 6px;
  cursor: pointer; transition: all 0.2s;
}
.recent-item:hover { border-color: #2563eb; color: #2563eb; }
.recent-time { color: #9ca3af; font-size: 11px; }

.skeleton-list { display: flex; flex-direction: column; gap: 14px; }
.skeleton-card { background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 22px 24px; }
.skeleton-line { height: 14px; background: #f3f4f6; border-radius: 4px; animation: skeleton-pulse 1.5s ease-in-out infinite; }
.skeleton-line--title { width: 55%; height: 18px; margin-bottom: 14px; }
.skeleton-line--meta { width: 40%; height: 12px; margin-bottom: 14px; }
.skeleton-line--text { width: 100%; height: 13px; margin-bottom: 8px; }
.skeleton-line--text--short { width: 70%; }
@keyframes skeleton-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }

.results { display: flex; flex-direction: column; gap: 12px; }
.case-card { background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 20px 24px; box-shadow: 0 1px 3px rgba(37,99,235,0.04); cursor: pointer; transition: all 0.25s; }
.case-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(37,99,235,0.08); }
.case-card__header { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 8px; }
.case-card__title { font-size: 15px; font-weight: 600; color: #111827; margin: 0; line-height: 1.4; }
.case-card__type-tag { display: inline-flex; padding: 2px 10px; font-size: 11px; font-weight: 500; color: #2563eb; background: rgba(37,99,235,0.08); border-radius: 4px; flex-shrink: 0; }
.case-card__meta { font-size: 12px; color: #9ca3af; margin-bottom: 12px; }
.meta-sep { margin: 0 6px; color: #e5e7eb; }
.case-card__summary { font-size: 13px; color: #4b5563; line-height: 1.6; margin: 0 0 12px; }
.case-card__keywords { display: flex; flex-wrap: wrap; gap: 6px; }
.case-kw-tag { display: inline-flex; padding: 2px 8px; font-size: 11px; color: #6b7280; background: #f3f4f6; border: 1px solid #e5e7eb; border-radius: 4px; }

.empty-state { text-align: center; padding: 80px 24px; }
.empty-state__icon { color: #9ca3af; margin-bottom: 20px; }
.empty-state__title { font-size: 16px; font-weight: 500; color: #4b5563; margin: 0 0 8px; }
.empty-state__desc { font-size: 13px; color: #9ca3af; margin: 0; }

.pagination { display: flex; justify-content: center; margin-top: 32px; }

.semantic-desc { color: #4b5563; margin: 0 0 16px; font-size: 13px; line-height: 1.6; }
.semantic-results { margin-top: 18px; }
.semantic-results-title { font-size: 14px; font-weight: 600; color: #111827; margin: 0 0 10px; }
.semantic-card { background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 8px; padding: 14px 16px; cursor: pointer; transition: all 0.2s; margin-bottom: 8px; }
.semantic-card:hover { border-color: #2563eb; background: rgba(37,99,235,0.03); }
.semantic-card__title { font-weight: 600; font-size: 14px; color: #111827; margin-bottom: 6px; }
.semantic-card__content { font-size: 13px; color: #4b5563; line-height: 1.5; margin-bottom: 6px; }
.semantic-card__score { font-size: 12px; color: #2563eb; }

html.dark .page-title { color: var(--text-primary); }

html.dark .search-input :deep(.el-input__wrapper) { background: #1e293b; border-color: rgba(255,255,255,0.1); }
html.dark .search-input :deep(.el-input__wrapper:hover) { border-color: #3b82f6; }
html.dark .search-input :deep(.el-input__wrapper.is-focus) { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.15); }
html.dark .search-input :deep(.el-input__inner) { color: #e2e8f0; }
html.dark .search-input :deep(.el-input-group__prepend) { color: #94a3b8; }

html.dark .filter-select :deep(.el-input__wrapper) { background: #1e293b; border-color: rgba(255,255,255,0.1); }
html.dark .filter-select :deep(.el-input__wrapper:hover) { border-color: #3b82f6; }
html.dark .filter-select :deep(.el-input__wrapper.is-focus) { border-color: #3b82f6; box-shadow: 0 0 0 2px rgba(59,130,246,0.15); }

html.dark .semantic-btn { background: #1a2744; border-color: #3b82f6; color: #3b82f6; }
html.dark .semantic-btn:hover { background: rgba(59,130,246,0.15); border-color: #3b82f6; color: #3b82f6; }

html.dark .recent-section { background: #1a2744; border-color: rgba(255,255,255,0.08); }
html.dark .recent-title { color: var(--text-primary); }
html.dark .recent-clear { color: var(--text-tertiary); }
html.dark .recent-clear:hover { color: #f87171; background: rgba(248,113,113,0.1); }
html.dark .recent-item { color: var(--text-secondary); background: transparent; border-color: rgba(255,255,255,0.08); }
html.dark .recent-item:hover { color: #3b82f6; border-color: #3b82f6; }
html.dark .recent-time { color: var(--text-tertiary); }

html.dark .skeleton-card { background: #1a2744; border-color: rgba(255,255,255,0.08); }
html.dark .skeleton-line { background: rgba(255,255,255,0.05); }

html.dark .case-card { background: #1a2744; border-color: rgba(255,255,255,0.08); }
html.dark .case-card__title { color: var(--text-primary); }
html.dark .case-card__type-tag { color: #3b82f6; background: rgba(59,130,246,0.15); }
html.dark .case-card__meta { color: var(--text-tertiary); }
html.dark .meta-sep { color: rgba(255,255,255,0.08); }
html.dark .case-card__summary { color: var(--text-secondary); }
html.dark .case-kw-tag { color: var(--text-secondary); background: transparent; border-color: rgba(255,255,255,0.08); }

html.dark .empty-state__icon { color: var(--text-tertiary); }
html.dark .empty-state__title { color: var(--text-secondary); }
html.dark .empty-state__desc { color: var(--text-tertiary); }

html.dark .semantic-desc { color: var(--text-secondary); }
html.dark .semantic-results-title { color: var(--text-primary); }
html.dark .semantic-card { background: #1a2744; border-color: rgba(255,255,255,0.08); }
html.dark .semantic-card:hover { border-color: #3b82f6; background: rgba(59,130,246,0.15); }
html.dark .semantic-card__title { color: var(--text-primary); }
html.dark .semantic-card__content { color: var(--text-secondary); }
html.dark .semantic-card__score { color: #3b82f6; }
</style>
