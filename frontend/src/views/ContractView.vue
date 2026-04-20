<template>
  <div class="contract-view">
    <el-container>
      <el-main>
        <div class="upload-area">
          <el-upload
            drag
            action="/api/contract/upload"
            :auto-upload="false"
            accept=".pdf,.doc,.docx"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              拖拽合同文件到此处，或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">支持 PDF、Word 文档，文件大小不超过 20MB</div>
            </template>
          </el-upload>
        </div>
        <div class="analysis-placeholder">
          <p>合同分析结果将在此显示（v0.3实现）</p>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { UploadFilled } from '@element-plus/icons-vue'
</script>

<style scoped>
.contract-view {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.upload-area {
  margin: 40px 0;
}

.analysis-placeholder {
  text-align: center;
  color: #999;
  padding: 40px;
}
</style>
