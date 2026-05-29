<template>
  <div class="image-uploader">
    <el-upload
      class="upload-area"
      drag
      :auto-upload="false"
      :show-file-list="false"
      :on-change="handleFileChange"
      accept="image/*"
    >
      <div v-if="!imageUrl" class="upload-placeholder">
        <el-icon class="upload-icon"><Plus /></el-icon>
        <div class="upload-text">点击或拖拽图片到此处</div>
        <div class="upload-hint">支持 JPG、PNG、BMP 等格式</div>
      </div>
      <div v-else class="image-preview">
        <img :src="imageUrl" alt="预览图片" />
        <div class="image-overlay">
          <el-button type="danger" size="small" @click.stop="clearImage">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </el-upload>
    
    <div v-if="file" class="file-info">
      <span>{{ file.name }}</span>
      <span>{{ formatFileSize(file.size) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'
import type { UploadFile } from 'element-plus'

const emit = defineEmits<{
  (e: 'selected', file: File): void
  (e: 'cleared'): void
}>()

const file = ref<File | null>(null)
const imageUrl = ref('')

function handleFileChange(uploadFile: UploadFile) {
  if (uploadFile.raw) {
    file.value = uploadFile.raw
    imageUrl.value = URL.createObjectURL(uploadFile.raw)
    emit('selected', uploadFile.raw)
  }
}

function clearImage() {
  file.value = null
  imageUrl.value = ''
  emit('cleared')
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
</script>

<style scoped>
.image-uploader {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upload-area {
  width: 100%;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.3s;
}

.upload-placeholder:hover {
  border-color: #409eff;
}

.upload-icon {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 12px;
}

.upload-text {
  font-size: 16px;
  color: #606266;
  margin-bottom: 8px;
}

.upload-hint {
  font-size: 12px;
  color: #909399;
}

.image-preview {
  position: relative;
  display: inline-block;
}

.image-preview img {
  max-width: 100%;
  max-height: 300px;
  border-radius: 8px;
}

.image-overlay {
  position: absolute;
  top: 8px;
  right: 8px;
  opacity: 0;
  transition: opacity 0.3s;
}

.image-preview:hover .image-overlay {
  opacity: 1;
}

.file-info {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #606266;
}
</style>