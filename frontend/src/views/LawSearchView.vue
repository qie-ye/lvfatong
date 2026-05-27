<template>
  <div class="law-search-view">
    <div class="page-header">
      <h1 class="page-title">法律条文查询</h1>
      <button class="catalog-toggle" @click="showCatalog = !showCatalog">
        <el-icon :size="16"><component :is="showCatalog ? Fold : Expand" /></el-icon>
        {{ showCatalog ? '收起目录' : '法典目录' }}
      </button>
    </div>

    <div class="search-body">
      <transition name="slide-sidebar">
        <aside v-if="showCatalog" class="catalog-sidebar">
          <div class="catalog-title">按法典浏览</div>
          <div class="catalog-tree">
            <div
              v-for="book in lawCatalog"
              :key="book.name"
              class="catalog-book"
            >
              <div
                class="catalog-book-header"
                :class="{ open: expandedBooks.has(book.name) }"
                @click="toggleBook(book.name)"
              >
                <span>{{ book.name }}</span>
                <span class="catalog-count">{{ book.chapters.length }}章</span>
              </div>
              <div v-if="expandedBooks.has(book.name)" class="catalog-chapters">
                <div
                  v-for="ch in book.chapters"
                  :key="ch"
                  class="catalog-chapter"
                  :class="{ active: query === book.name + ' ' + ch }"
                  @click="handleCatalogSelect(book.name, ch)"
                >{{ ch }}</div>
              </div>
            </div>
          </div>
        </aside>
      </transition>

      <div class="search-main" :class="{ 'search-main--wide': !showCatalog }">
        <div class="search-section">
          <div class="search-bar-row">
            <el-input
              v-model="query"
              placeholder="输入法律名称、条文号或关键词"
              size="large"
              clearable
              @keyup.enter="handleSearch"
              class="search-input"
            >
              <template #prepend>
                <el-icon :size="18"><Search /></el-icon>
              </template>
              <template #append>
                <el-button @click="handleSearch" :loading="loading" class="search-btn">检索</el-button>
              </template>
            </el-input>
          </div>

          <div class="quick-tags-row">
            <span
              v-for="t in quickTags"
              :key="t"
              :class="['quick-tag', { 'quick-tag--active': query === t }]"
              @click="handleQuickTag(t)"
            >{{ t }}</span>
          </div>

          <div class="filter-row">
            <el-select v-model="docType" placeholder="文档类型" clearable size="default" class="filter-select">
              <el-option label="法律法规" value="LAW" />
              <el-option label="司法解释" value="JUDICIAL_INTERPRETATION" />
              <el-option label="部门规章" value="DEPARTMENTAL_RULES" />
            </el-select>
            <el-select v-model="lawDomain" placeholder="法律领域" clearable size="default" class="filter-select">
              <el-option label="民法" value="民法" />
              <el-option label="合同法" value="合同法" />
              <el-option label="劳动法" value="劳动法" />
              <el-option label="刑法" value="刑法" />
              <el-option label="婚姻家庭" value="婚姻家庭" />
            </el-select>
            <el-select v-model="topK" size="default" class="filter-select filter-select--topk">
              <el-option :label="'显示 ' + k + ' 条'" :value="k" v-for="k in [5, 10, 15, 20]" :key="k" />
            </el-select>
          </div>
        </div>

        <div class="results" v-if="results.length > 0">
          <p class="result-count">共检索到 {{ results.length }} 条结果</p>
          <div class="result-list">
            <div v-for="(item, index) in results" :key="index" class="result-card">
              <div class="result-card__header">
                <div class="result-card__tags">
                  <span class="result-tag result-tag--doctype" v-if="item.docType">{{ item.docType }}</span>
                  <span class="result-tag result-tag--domain" v-if="item.lawDomain">{{ item.lawDomain }}</span>
                </div>
                <span class="result-card__score">相关度 {{ (item.score * 100).toFixed(1) }}%</span>
              </div>
              <div class="result-card__content">{{ item.content }}</div>
            </div>
          </div>
        </div>

        <div v-if="searched && results.length === 0 && !loading" class="empty-state">
          <div class="empty-state__icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/><path d="M8 11h6"/>
            </svg>
          </div>
          <p class="empty-state__title">未找到相关条文，请尝试调整关键词</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import api from '@/api'
import { ElMessage } from 'element-plus'
import { Search, Fold, Expand } from '@element-plus/icons-vue'

interface SearchResult { content: string; docType: string; lawDomain: string; score: number }

const query = ref('')
const docType = ref('')
const lawDomain = ref('')
const topK = ref(5)
const results = ref<SearchResult[]>([])
const loading = ref(false)
const searched = ref(false)
const showCatalog = ref(true)

const quickTags = ['民法典', '劳动合同法', '刑法', '公司法', '婚姻法']

const expandedBooks = reactive(new Set<string>(['民法典']))

const lawCatalog = [
  { name: '民法典', chapters: ['总则', '物权', '合同', '人格权', '婚姻家庭', '继承', '侵权责任'] },
  { name: '刑法', chapters: ['总则', '危害公共安全罪', '破坏市场经济秩序罪', '侵犯公民人身权利罪', '侵犯财产罪', '妨害社会管理秩序罪'] },
  { name: '劳动法', chapters: ['总则', '劳动合同', '工作时间与休息', '工资', '劳动安全卫生', '社会保险与福利', '劳动争议'] },
  { name: '公司法', chapters: ['总则', '有限责任公司的设立', '股份有限公司的设立', '组织机构', '股份发行与转让', '公司合并与分立'] },
  { name: '行政法', chapters: ['总则', '行政行为', '行政许可', '行政处罚', '行政强制', '行政复议', '行政诉讼'] }
]

function toggleBook(name: string) {
  if (expandedBooks.has(name)) expandedBooks.delete(name)
  else expandedBooks.add(name)
}

function handleCatalogSelect(book: string, chapter: string) {
  query.value = book + ' ' + chapter
  handleSearch()
}

function handleQuickTag(t: string) { query.value = t; handleSearch() }

async function handleSearch() {
  if (!query.value.trim()) { ElMessage.warning('请输入检索关键词'); return }
  loading.value = true; searched.value = true
  try {
    const res = await api.get('/knowledge/laws', {
      params: { query: query.value, docType: docType.value || undefined, lawDomain: lawDomain.value || undefined, topK: topK.value }
    })
    results.value = (res.data as SearchResult[]) || []
    if (results.value.length === 0) ElMessage.info('未找到相关法律条文')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || e.message || '检索失败')
  } finally { loading.value = false }
}
</script>

<style scoped>
.law-search-view { max-width: 1200px; margin: 0 auto; padding: 32px 24px 60px; }

.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 28px; }
.page-title { font-size: 24px; font-weight: 600; color: #111827; margin: 0; }

.catalog-toggle {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 7px 16px; font-size: 13px; color: var(--color-primary-600);
  background: var(--color-primary-50); border: 1px solid var(--color-primary-200);
  border-radius: 8px; cursor: pointer; transition: all 0.2s;
}
.catalog-toggle:hover { background: var(--color-primary-100); }

.search-body { display: flex; gap: 24px; align-items: flex-start; }

.catalog-sidebar {
  width: 220px; flex-shrink: 0; background: #fff;
  border: 1px solid var(--border); border-radius: 12px;
  padding: 16px; position: sticky; top: 72px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04); max-height: calc(100vh - 120px); overflow-y: auto;
}

.catalog-title { font-size: 14px; font-weight: 600; color: #111827; margin-bottom: 14px; padding-bottom: 10px; border-bottom: 1px solid var(--border); }

.catalog-book { margin-bottom: 4px; }
.catalog-book-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 7px 10px; font-size: 13px; color: #374151; font-weight: 500;
  border-radius: 6px; cursor: pointer; transition: background 0.15s;
}
.catalog-book-header:hover { background: var(--gray-100); }
.catalog-book-header.open { color: var(--color-primary-600); }

.catalog-count { font-size: 11px; color: #9ca3af; }

.catalog-chapters { padding: 2px 0 2px 8px; border-left: 2px solid var(--color-primary-100); margin-left: 10px; }
.catalog-chapter {
  padding: 5px 10px; font-size: 12px; color: #6b7280; cursor: pointer;
  border-radius: 4px; transition: all 0.15s;
}
.catalog-chapter:hover { color: var(--color-primary-600); background: var(--color-primary-50); }
.catalog-chapter.active { color: var(--color-primary-600); font-weight: 500; background: var(--color-primary-50); }

.search-main { flex: 1; min-width: 0; }
.search-main--wide { max-width: 960px; }

.search-section { margin-bottom: 28px; }
.search-bar-row { width: 100%; }
.search-input :deep(.el-input__wrapper) {
  background: #f5f7fa; border: 1px solid #e2e8f0; border-radius: 8px;
  box-shadow: none; transition: border-color 0.2s, box-shadow 0.2s; height: 44px;
}
.search-input :deep(.el-input__wrapper:hover) { border-color: #2563eb; }
.search-input :deep(.el-input__wrapper.is-focus) { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37,99,235,0.08); }
.search-input :deep(.el-input-group__prepend) { background: transparent; border: none; color: #9ca3af; padding: 0 0 0 14px; }
.search-input :deep(.el-input-group__append) { background: transparent; border: none; padding: 0; }
.search-btn { height: 44px; border-radius: 0 8px 8px 0; padding: 0 24px; font-weight: 500; }

.quick-tags-row { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 14px; }
.quick-tag {
  padding: 5px 14px; font-size: 13px; color: #4b5563; background: #f5f7fa;
  border: 1px solid #e2e8f0; border-radius: 6px; cursor: pointer; transition: all 0.2s;
}
.quick-tag:hover { color: #2563eb; border-color: #2563eb; background: rgba(37,99,235,0.04); }
.quick-tag--active, .quick-tag--active:hover { color: #fff; background: #2563eb; border-color: #2563eb; }

.filter-row { display: flex; gap: 10px; margin-top: 14px; flex-wrap: wrap; }
.filter-select { width: 160px; }
.filter-select--topk { width: 130px; }
.filter-select :deep(.el-input__wrapper) { background: #f5f7fa; border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: none; height: 38px; }
.filter-select :deep(.el-input__wrapper:hover) { border-color: #2563eb; }
.filter-select :deep(.el-input__wrapper.is-focus) { border-color: #2563eb; box-shadow: 0 0 0 2px rgba(37,99,235,0.08); }

.results { margin-top: 8px; }
.result-count { font-size: 13px; color: #4b5563; margin-bottom: 14px; font-weight: 500; }
.result-list { display: flex; flex-direction: column; gap: 12px; }
.result-card { background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 20px 24px; box-shadow: 0 1px 3px rgba(37,99,235,0.04); transition: all 0.25s; }
.result-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(37,99,235,0.08); }
.result-card__header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.result-card__tags { display: flex; gap: 8px; }
.result-tag { display: inline-flex; padding: 2px 10px; font-size: 11px; font-weight: 500; border-radius: 4px; }
.result-tag--doctype { color: #2563eb; background: rgba(37,99,235,0.08); }
.result-tag--domain { color: #4b5563; background: #f3f4f6; }
.result-card__score { font-size: 12px; color: #9ca3af; white-space: nowrap; }
.result-card__content { font-size: 14px; line-height: 1.75; color: #111827; white-space: pre-wrap; }

.empty-state { text-align: center; padding: 80px 24px; }
.empty-state__icon { color: #9ca3af; margin-bottom: 20px; }
.empty-state__title { font-size: 16px; font-weight: 500; color: #4b5563; }

.slide-sidebar-enter-active, .slide-sidebar-leave-active { transition: all 0.3s ease; }
.slide-sidebar-enter-from, .slide-sidebar-leave-to { opacity: 0; width: 0; overflow: hidden; }

@media (max-width: 900px) {
  .search-body { flex-direction: column; }
  .catalog-sidebar { width: 100%; position: static; max-height: 240px; }
}

html.dark .page-title { color: var(--text-primary); }

html.dark .catalog-toggle { color: #3b82f6; background: rgba(59,130,246,0.15); border-color: rgba(59,130,246,0.25); }
html.dark .catalog-toggle:hover { background: rgba(59,130,246,0.25); }

html.dark .catalog-sidebar { background: #1a2744; }
html.dark .catalog-title { color: var(--text-primary); border-color: rgba(255,255,255,0.08); }
html.dark .catalog-book-header { color: var(--text-secondary); }
html.dark .catalog-book-header:hover { background: rgba(59,130,246,0.15); }
html.dark .catalog-book-header.open { color: #3b82f6; }
html.dark .catalog-count { color: var(--text-tertiary); }
html.dark .catalog-chapter { color: var(--text-secondary); }
html.dark .catalog-chapter:hover { color: #3b82f6; background: rgba(59,130,246,0.15); }
html.dark .catalog-chapter.active { color: #3b82f6; background: rgba(59,130,246,0.15); }

html.dark .search-input :deep(.el-input__wrapper) { background: #1e293b; border-color: rgba(255,255,255,0.1); }
html.dark .search-input :deep(.el-input__wrapper:hover) { border-color: #3b82f6; }
html.dark .search-input :deep(.el-input__wrapper.is-focus) { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.15); }
html.dark .search-input :deep(.el-input__inner) { color: #e2e8f0; }
html.dark .search-input :deep(.el-input-group__prepend) { color: #94a3b8; }

html.dark .filter-select :deep(.el-input__wrapper) { background: #1e293b; border-color: rgba(255,255,255,0.1); }
html.dark .filter-select :deep(.el-input__wrapper:hover) { border-color: #3b82f6; }
html.dark .filter-select :deep(.el-input__wrapper.is-focus) { border-color: #3b82f6; box-shadow: 0 0 0 2px rgba(59,130,246,0.15); }

html.dark .quick-tag { color: var(--text-secondary); background: transparent; border-color: rgba(255,255,255,0.06); }
html.dark .quick-tag:hover { color: #e2e8f0; border-color: rgba(255,255,255,0.1); background: rgba(255,255,255,0.08); }
html.dark .quick-tag--active { color: #fff; background: #3b82f6; border-color: #3b82f6; }
html.dark .quick-tag--active:hover { color: #fff; background: #3b82f6; border-color: #3b82f6; }

html.dark .result-count { color: var(--text-secondary); }
html.dark .result-card { background: #1a2744; border-color: rgba(255,255,255,0.08); }
html.dark .result-card__content { color: var(--text-primary); }
html.dark .result-card__score { color: var(--text-tertiary); }
html.dark .result-tag--doctype { color: #3b82f6; background: rgba(59,130,246,0.15); }
html.dark .result-tag--domain { color: var(--text-secondary); background: rgba(255,255,255,0.05); }

html.dark .empty-state__icon { color: var(--text-tertiary); }
html.dark .empty-state__title { color: var(--text-secondary); }
</style>
