<template>
  <div class="disclaimer-banner">
    <div class="disclaimer-content">
      <el-icon :size="16" class="disclaimer-icon"><WarningFilled /></el-icon>
      <span>本平台提供的信息由AI生成，仅供参考，不构成法律意见。如有正式法律需求，请咨询持有执业证的专业律师。</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { WarningFilled } from '@element-plus/icons-vue'
</script>

<style scoped>
.disclaimer-banner {
  padding: 10px 16px;
  background: #fffbeb;
  border-left: 3px solid #f59e0b;
}

.disclaimer-content {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #92400e;
  line-height: 1.5;
}

.disclaimer-icon {
  color: #d97706;
  flex-shrink: 0;
}
</style>
