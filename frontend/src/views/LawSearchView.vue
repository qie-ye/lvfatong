<template>
  <div class="law-search-view">
    <div class="search-header">
      <h2>法律条文查询</h2>
      <p class="description">按法律名称、条文号或关键词检索相关法律条文</p>
    </div>

    <div class="search-bar">
      <el-input
        v-model="query"
        placeholder="输入法律名称、条文号或关键词，如：民法典 合同违约"
        size="large"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button @click="handleSearch" :loading="loading">检索</el-button>
        </template>
      </el-input>

      <div class="filter-row">
        <el-select v-model="docType" placeholder="文档类型" clearable size="default" style="width: 160px">
          <el-option label="法律法规" value="LAW" />
          <el-option label="案例" value="CASE" />
          <el-option label="合同模板" value="CONTRACT_TEMPLATE" />
          <el-option label="法律知识" value="LEGAL_KNOWLEDGE" />
        </el-select>
        <el-select v-model="lawDomain" placeholder="法律领域" clearable size="default" style="width: 160px">
          <el-option label="民法" value="民法" />
          <el-option label="合同法" value="合同法" />
          <el-option label="劳动法" value="劳动法" />
          <el-option label="刑法" value="刑法" />
          <el-option label="婚姻家庭" value="婚姻家庭" />
          <el-option label="消费者权益" value="消费者权益" />
        </el-select>
        <el-input-number v-model="topK" :min="1" :max="20" size="default" style="width: 120px" />
        <span class="topk-label">条结果</span>
      </div>
    </div>

    <div class="results" v-if="results.length > 0">
      <p class="result-count">共检索到 {{ results.length }} 条结果</p>
      <el-card v-for="(item, index) in results" :key="index" class="result-card" shadow="hover">
        <div class="result-header">
          <el-tag v-if="item.docType" size="small" type="info">{{ item.docType }}</el-tag>
          <el-tag v-if="item.lawDomain" size="small">{{ item.lawDomain }}</el-tag>
          <span class="score">相关度 {{ (item.score * 100).toFixed(1) }}%</span>
        </div>
        <div class="result-content">{{ item.content }}</div>
      </el-card>
    </div>

    <div v-if="searched && results.length === 0 && !loading" class="no-result">
      <p>未找到相关法律条文，请尝试其他关键词</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import api from '@/api'
import { ElMessage } from 'element-plus'

interface SearchResult {
  content: string
  docType: string
  lawDomain: string
  score: number
}

const query = ref('')
const docType = ref('')
const lawDomain = ref('')
const topK = ref(5)
const results = ref<SearchResult[]>([])
const loading = ref(false)
const searched = ref(false)

async function handleSearch() {
  if (!query.value.trim()) {
    ElMessage.warning('请输入检索关键词')
    return
  }
  loading.value = true
  searched.value = true
  try {
    const res = await api.get('/knowledge/laws', {
      params: {
        query: query.value,
        docType: docType.value || undefined,
        lawDomain: lawDomain.value || undefined,
        topK: topK.value
      }
    })
    results.value = (res.data as SearchResult[]) || []
    if (results.value.length === 0) {
      ElMessage.info('未找到相关法律条文，请尝试其他关键词或先导入法律数据')
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || e.message || '检索失败，请检查后端服务是否正常')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.law-search-view {
  max-width: 900px;
  margin: 0 auto;
  padding: 30px 20px;
}
.search-header {
  text-align: center;
  margin-bottom: 30px;
}
.search-header h2 {
  color: #1a1a2e;
  margin-bottom: 8px;
}
.description {
  color: #999;
  font-size: 14px;
}
.search-bar {
  margin-bottom: 24px;
}
.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 12px;
}
.topk-label {
  font-size: 13px;
  color: #666;
}
.result-count {
  color: #666;
  font-size: 13px;
  margin-bottom: 12px;
}
.result-card {
  margin-bottom: 12px;
}
.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.score {
  margin-left: auto;
  font-size: 12px;
  color: #999;
}
.result-content {
  font-size: 14px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
}
.no-result {
  text-align: center;
  color: #999;
  padding: 60px 0;
}
</style>
