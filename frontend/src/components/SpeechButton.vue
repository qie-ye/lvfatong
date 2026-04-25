<template>
  <div class="speech-btn-wrapper">
    <el-popover
      :visible="showDialectMenu"
      placement="top"
      :width="120"
      trigger="manual"
    >
      <template #reference>
        <button
          :class="['speech-btn', { listening: speech.isListening.value, disabled }]"
          :disabled="disabled"
          @click="handleClick"
          type="button"
          :title="speech.isListening.value ? '停止录音' : '语音输入'"
        >
          <svg v-if="!speech.isListening.value" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
            <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z"/>
            <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
          </svg>
          <span v-else class="pulse-icon">🎤</span>
          <span v-if="!speech.isListening.value" class="btn-label">{{ speech.getDialectLabel(speech.currentDialect.value) }}</span>
        </button>
      </template>
      <div class="dialect-menu">
        <div
          v-for="d in dialects"
          :key="d"
          :class="['dialect-item', { active: speech.currentDialect.value === d }]"
          @click="selectDialect(d)"
        >{{ speech.getDialectLabel(d) }}</div>
      </div>
    </el-popover>
    <div v-if="speech.isListening.value && speech.interimResult.value" class="interim-text">
      {{ speech.interimResult.value }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useSpeechRecognition, type Dialect } from '@/composables/useSpeechRecognition'

const props = defineProps<{
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'result', text: string): void
}>()

const speech = useSpeechRecognition()
const showDialectMenu = ref(false)
const dialects: Dialect[] = ['mandarin', 'cantonese', 'henanese']

let longPressTimer: ReturnType<typeof setTimeout> | null = null

function handleClick() {
  if (speech.isListening.value) {
    speech.stopListening()
    return
  }
  speech.startListening()
}

function selectDialect(d: Dialect) {
  showDialectMenu.value = false
  if (speech.isListening.value) {
    speech.stopListening()
    setTimeout(() => {
      speech.startListening(d)
    }, 300)
  } else {
    speech.currentDialect.value = d
    ElMessage.success(`已切换为${speech.getDialectLabel(d)}`)
  }
}

// Emit result when final text accumulates and listening stops
watch(() => speech.isListening.value, (listening, wasListening) => {
  if (wasListening && !listening) {
    const text = speech.finalResult.value.trim()
    if (text) {
      emit('result', text)
    }
    if (speech.error.value) {
      ElMessage.error(speech.error.value)
    }
    speech.reset()
  }
})
</script>

<style scoped>
.speech-btn-wrapper {
  display: inline-flex;
  align-items: center;
  position: relative;
}

.speech-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  color: #606266;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
  white-space: nowrap;
  flex-shrink: 0;
}

.speech-btn:hover:not(.disabled) {
  color: #409eff;
  border-color: #c6e2ff;
  background: #ecf5ff;
}

.speech-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.speech-btn.listening {
  color: #f56c6c;
  border-color: #fab6b6;
  background: #fef0f0;
  animation: pulse-ring 1.5s ease-out infinite;
}

.btn-label {
  font-size: 12px;
}

.pulse-icon {
  font-size: 16px;
  animation: pulse-scale 1s ease-in-out infinite;
}

@keyframes pulse-ring {
  0% { box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.4); }
  70% { box-shadow: 0 0 0 6px rgba(245, 108, 108, 0); }
  100% { box-shadow: 0 0 0 0 rgba(245, 108, 108, 0); }
}

@keyframes pulse-scale {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}

.dialect-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dialect-item {
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.2s;
}

.dialect-item:hover {
  background: #f0f2f5;
}

.dialect-item.active {
  color: #409eff;
  font-weight: 600;
}

.interim-text {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 6px;
}
</style>
