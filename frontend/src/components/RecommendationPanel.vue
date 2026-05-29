<template>
  <div class="recommendation-panel" v-if="visible && recommendations.length > 0">
    <div class="panel-header">
      <div class="header-left">
        <el-icon :size="14"><TrendCharts /></el-icon>
        <span>{{ title }}</span>
      </div>
      <el-button class="refresh-btn" text size="small" @click="handleRefresh" :loading="loading">
        <el-icon><Refresh /></el-icon>
      </el-button>
    </div>
    <div class="panel-content">
      <div
        v-for="(item, index) in recommendations"
        :key="index"
        class="recommendation-item"
        @click="handleSelect(item)"
      >
        <div class="item-badge" :class="item.type">
          {{ getTypeLabel(item.type) }}
        </div>
        <div class="item-info">
          <div class="item-title">{{ item.title }}</div>
          <div class="item-desc" v-if="item.description">{{ item.description }}</div>
        </div>
        <div class="item-score" v-if="item.score">
          <span class="score-value">{{ Math.round(item.score * 100) }}%</span>
          <span class="score-label">相关</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { TrendCharts, Refresh } from '@element-plus/icons-vue'

interface RecommendationItem {
  type: 'law' | 'case' | 'faq' | 'lawyer' | 'topic'
  title: string
  description?: string
  score?: number
  url?: string
}

const props = defineProps<{
  visible: boolean
  title?: string
  recommendations: RecommendationItem[]
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'select', item: RecommendationItem): void
  (e: 'refresh'): void
}>()

const getTypeLabel = (type: string): string => {
  const labelMap: Record<string, string> = {
    law: '法条',
    case: '案例',
    faq: 'FAQ',
    lawyer: '律师',
    topic: '话题'
  }
  return labelMap[type] || '推荐'
}

const handleSelect = (item: RecommendationItem) => {
  emit('select', item)
}

const handleRefresh = () => {
  emit('refresh')
}
</script>

<style scoped>
.recommendation-panel {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 16px;
  box-shadow: var(--shadow-sm);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 0 2px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.header-left .el-icon {
  color: var(--color-primary-500);
}

.refresh-btn {
  color: var(--text-tertiary);
}

.refresh-btn:hover {
  color: var(--color-primary-500);
}

.panel-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.recommendation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all var(--transition-base);
  border: 1px solid transparent;
}

.recommendation-item:hover {
  background: var(--color-primary-50);
  border-color: var(--color-primary-100);
  transform: translateX(2px);
}

.item-badge {
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
  flex-shrink: 0;
}

.item-badge.law {
  background: #dbeafe;
  color: #1d4ed8;
}

.item-badge.case {
  background: #fce7f3;
  color: #be185d;
}

.item-badge.faq {
  background: #d1fae5;
  color: #047857;
}

.item-badge.lawyer {
  background: #e0e7ff;
  color: #4338ca;
}

.item-badge.topic {
  background: #fef3c7;
  color: #b45309;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 13px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-desc {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}

.score-value {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary-600);
}

.score-label {
  font-size: 10px;
  color: var(--text-tertiary);
}
</style>
