<template>
  <div class="faq-view">
    <h1 class="page-title">常见法律问题</h1>

    <div class="search-bar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索常见法律问题..."
        size="large"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon :size="18"><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <div class="category-tabs">
      <span
        :class="['category-tag', { active: activeCategory === '' }]"
        @click="selectCategory('')"
      >全部</span>
      <span
        v-for="cat in categories"
        :key="cat"
        :class="['category-tag', { active: activeCategory === cat }]"
        @click="selectCategory(cat)"
      >{{ cat }}</span>
    </div>

    <div v-loading="loading" class="faq-list">
      <template v-if="displayFaqs.length > 0">
        <el-collapse v-model="activeNames" accordion>
          <el-collapse-item v-for="faq in displayFaqs" :key="faq.id" :name="faq.id">
            <template #title>
              <div class="faq-question">
                <span class="faq-text">{{ faq.question }}</span>
                <span v-if="faq.category" class="faq-category-tag">{{ faq.category }}</span>
                <span class="faq-arrow">&#8250;</span>
              </div>
            </template>
            <div class="faq-answer">{{ faq.answer }}</div>
          </el-collapse-item>
        </el-collapse>
      </template>
      <div v-else class="empty-state">
        <div class="empty-icon-circle">
          <el-icon :size="28"><QuestionFilled /></el-icon>
        </div>
        <p>未找到相关问题</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { QuestionFilled, Search, Document, Coin } from '@element-plus/icons-vue'
import api from '@/api'
import { ElMessage } from 'element-plus'

interface FaqItem {
  id: number
  question: string
  answer: string
  category: string
  tags: string
}

const allFaqs = ref<FaqItem[]>([])
const searchResults = ref<FaqItem[]>([])
const categories = ref<string[]>([])
const activeCategory = ref('')
const searchQuery = ref('')
const activeNames = ref<number[]>([])
const loading = ref(false)
const searching = ref(false)
const isSearchMode = ref(false)

const displayFaqs = computed(() => {
  if (isSearchMode.value) return searchResults.value
  if (!activeCategory.value) return allFaqs.value
  return allFaqs.value.filter(f => f.category === activeCategory.value)
})

function categoryIcon(cat: string) {
  const map: Record<string, any> = { '劳动法': Document, '合同法': Document, '婚姻法': Coin }
  return map[cat] || null
}

onMounted(async () => {
  await loadData()
})

async function loadData() {
  loading.value = true
  try {
    const [faqRes, catRes] = await Promise.all([
      api.get('/knowledge/faq'),
      api.get('/knowledge/faq/categories')
    ])
    allFaqs.value = faqRes.data as FaqItem[]
    categories.value = catRes.data as string[]
  } catch (e: any) {
    ElMessage.error('加载常见问题失败')
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  if (!searchQuery.value.trim()) {
    isSearchMode.value = false
    return
  }
  searching.value = true
  isSearchMode.value = true
  try {
    const res = await api.get('/knowledge/faq/search', {
      params: { query: searchQuery.value, limit: 10 }
    })
    searchResults.value = res.data as FaqItem[]
  } catch (e: any) {
    ElMessage.error('搜索失败')
  } finally {
    searching.value = false
  }
}

function selectCategory(cat: string) {
  activeCategory.value = cat
  isSearchMode.value = false
  searchQuery.value = ''
}
</script>

<style scoped>
.faq-view {
  max-width: 860px;
  margin: 0 auto;
  padding: 40px 24px 60px;
  background: var(--bg);
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 24px;
  letter-spacing: -0.02em;
}

.search-bar {
  margin-bottom: 20px;
}

.search-bar :deep(.el-input__wrapper) {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(37, 99, 235, 0.04);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.search-bar :deep(.el-input__wrapper:hover) {
  border-color: #2563eb;
}

.search-bar :deep(.el-input__wrapper.is-focus) {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08);
}

.search-bar :deep(.el-input__inner) {
  color: var(--text-primary);
}

.category-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.category-tag {
  cursor: pointer;
  font-size: 13px;
  padding: 5px 14px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg-card);
  color: var(--text-secondary);
  transition: all 0.2s ease;
  user-select: none;
}

.category-tag:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: var(--color-primary-50);
}

html.dark .category-tag:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.2);
}

.category-tag.active {
  background: #2563eb;
  border-color: #2563eb;
  color: #ffffff;
}

.faq-list {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(37, 99, 235, 0.04);
  overflow: hidden;
}

:deep(.el-collapse) {
  border: none;
}

:deep(.el-collapse-item__header) {
  height: auto;
  padding: 16px 20px;
  font-size: 14px;
  color: var(--text-primary);
  background: transparent;
  border-bottom: 1px solid var(--border-light);
  line-height: 1.5;
  transition: background 0.2s ease;
}

:deep(.el-collapse-item__header:hover) {
  background: var(--gray-100);
}

html.dark :deep(.el-collapse-item__header:hover) {
  background: rgba(59, 130, 246, 0.1);
}

:deep(.el-collapse-item__wrap) {
  background: transparent;
  border-bottom: 1px solid var(--border-light);
}

:deep(.el-collapse-item__content) {
  padding: 12px 20px 20px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-secondary);
}

.faq-question {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.faq-text {
  flex: 1;
  font-weight: 500;
}

.faq-category-tag {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 500;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.06);
  border: 1px solid rgba(37, 99, 235, 0.12);
  border-radius: 4px;
  line-height: 1.5;
}

.faq-arrow {
  flex-shrink: 0;
  color: var(--text-tertiary);
  font-size: 16px;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-collapse-item.is-active) .faq-arrow {
  transform: rotate(90deg);
}

.faq-answer {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-secondary);
  white-space: pre-wrap;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 20px;
  text-align: center;
}

.empty-icon-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--color-primary-50);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  color: #2563eb;
}

html.dark .empty-icon-circle {
  background: rgba(59, 130, 246, 0.1);
}

.empty-state p {
  color: var(--text-tertiary);
  font-size: 14px;
  margin: 0;
}

html.dark .faq-list {
  border-color: rgba(255, 255, 255, 0.08);
}

html.dark :deep(.el-collapse-item__wrap) {
  border-color: rgba(255, 255, 255, 0.08);
}

html.dark .faq-category-tag {
  color: #60a5fa;
  background: rgba(59, 130, 246, 0.1);
  border-color: rgba(59, 130, 246, 0.15);
}

html.dark .search-bar :deep(.el-input__wrapper) {
  background: #1e293b;
  border-color: rgba(255, 255, 255, 0.1);
}
</style>
