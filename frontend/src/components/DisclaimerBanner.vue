<template>
  <div class="disclaimer-banner">
    <el-alert type="warning" :closable="false" show-icon>
      <template #title>
        ⚠️ 免责声明：本平台提供的信息由AI生成，仅供参考，不构成法律意见。如有正式法律需求，请咨询持有执业证的专业律师。
      </template>
    </el-alert>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.disclaimer-banner {
  margin-bottom: 12px;
}
</style>
