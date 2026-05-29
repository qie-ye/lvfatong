<template>
  <div class="smart-suggestion" v-if="visible">
    <div class="suggestion-header">
      <el-icon :size="14"><MagicStick /></el-icon>
      <span>智能提示</span>
      <el-button class="close-btn" text size="small" @click="handleClose">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>
    <div class="suggestion-list">
      <div
        v-for="(item, index) in suggestions"
        :key="index"
        class="suggestion-item"
        @click="handleSelect(item)"
      >
        <el-icon :size="14" class="item-icon">
          <component :is="getIcon(item.type)" />
        </el-icon>
        <div class="item-content">
          <div class="item-title">{{ item.title }}</div>
          <div class="item-desc" v-if="item.description">{{ item.description }}</div>
        </div>
        <el-icon :size="12" class="item-arrow"><ArrowRight /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { MagicStick, Close, ArrowRight, Document, Search, QuestionFilled, User } from '@element-plus/icons-vue'

interface SuggestionItem {
  type: 'law' | 'case' | 'faq' | 'lawyer' | 'topic'
  title: string
  description?: string
  query?: string
}

const props = defineProps<{
  visible: boolean
  suggestions: SuggestionItem[]
}>()

const emit = defineEmits<{
  (e: 'select', item: SuggestionItem): void
  (e: 'close'): void
}>()

const getIcon = (type: string) => {
  const iconMap: Record<string, any> = {
    law: Document,
    case: Search,
    faq: QuestionFilled,
    lawyer: User,
    topic: Search
  }
  return iconMap[type] || Search
}

const handleSelect = (item: SuggestionItem) => {
  emit('select', item)
}

const handleClose = () => {
  emit('close')
}
</script>

<style scoped>
.smart-suggestion {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 12px;
  box-shadow: var(--shadow-sm);
  animation: slideUp 0.2s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.suggestion-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-tertiary);
  margin-bottom: 10px;
  padding: 0 4px;
}

.close-btn {
  margin-left: auto;
  color: var(--text-tertiary);
}

.close-btn:hover {
  color: var(--text-primary);
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all var(--transition-base);
}

.suggestion-item:hover {
  background: var(--color-primary-50);
  transform: translateX(2px);
}

.item-icon {
  color: var(--color-primary-500);
  flex-shrink: 0;
}

.item-content {
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
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-arrow {
  color: var(--text-tertiary);
  flex-shrink: 0;
  opacity: 0;
  transition: opacity var(--transition-base);
}

.suggestion-item:hover .item-arrow {
  opacity: 1;
}
</style>
