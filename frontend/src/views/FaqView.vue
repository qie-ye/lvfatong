<template>
  <div class="faq-view">
    <div class="faq-header">
      <h2>常见法律问题</h2>
      <p class="description">预置高频法律问题与标准答案，快速获取专业解答</p>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索常见问题..."
        size="large"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button @click="handleSearch" :loading="searching">搜索</el-button>
        </template>
      </el-input>
    </div>

    <div class="category-tabs">
      <el-tag
        :type="activeCategory === '' ? '' : 'info'"
        class="category-tag"
        @click="selectCategory('')"
        effect="plain"
      >全部</el-tag>
      <el-tag
        v-for="cat in categories"
        :key="cat"
        :type="activeCategory === cat ? '' : 'info'"
        class="category-tag"
        @click="selectCategory(cat)"
        effect="plain"
      >{{ cat }}</el-tag>
    </div>

    <div class="faq-list">
      <el-collapse v-model="activeNames" accordion>
        <el-collapse-item
          v-for="faq in displayFaqs"
          :key="faq.id"
          :name="faq.id"
        >
          <template #title>
            <div class="faq-question">
              <el-icon><question-filled /></el-icon>
              <span>{{ faq.question }}</span>
              <el-tag v-if="faq.category" size="small" type="info" class="faq-category-tag">{{ faq.category }}</el-tag>
            </div>
          </template>
          <div class="faq-answer">{{ faq.answer }}</div>
        </el-collapse-item>
      </el-collapse>

      <div v-if="displayFaqs.length === 0 && !loading" class="no-result">
        <p>暂无匹配的常见问题</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { QuestionFilled } from '@element-plus/icons-vue'
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
  max-width: 900px;
  margin: 0 auto;
  padding: 30px 20px;
}
.faq-header {
  text-align: center;
  margin-bottom: 24px;
}
.faq-header h2 {
  color: #1a1a2e;
  margin-bottom: 8px;
}
.description {
  color: #999;
  font-size: 14px;
}
.search-bar {
  margin-bottom: 16px;
}
.category-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.category-tag {
  cursor: pointer;
  transition: all 0.2s;
}
.category-tag:hover {
  opacity: 0.8;
}
.faq-list {
  background: #fff;
  border-radius: 8px;
}
.faq-question {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
}
.faq-category-tag {
  margin-left: auto;
}
.faq-answer {
  font-size: 14px;
  line-height: 1.8;
  color: #555;
  white-space: pre-wrap;
  padding: 8px 0;
}
.no-result {
  text-align: center;
  color: #999;
  padding: 60px 0;
}
</style>
