<template>
  <div class="voice-recorder">
    <el-button
      :type="isRecording ? 'danger' : 'primary'"
      :icon="isRecording ? 'Microphone' : 'Microphone'"
      @click="toggleRecording"
      :loading="processing"
    >
      {{ isRecording ? '停止录音' : '开始录音' }}
    </el-button>
    
    <div v-if="isRecording" class="recording-indicator">
      <div class="recording-dot"></div>
      <span>录音中... {{ recordingTime }}s</span>
    </div>
    
    <div v-if="audioUrl" class="audio-preview">
      <audio :src="audioUrl" controls></audio>
      <el-button size="small" @click="clearRecording">清除</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'

const emit = defineEmits<{
  (e: 'recorded', audioBlob: Blob): void
  (e: 'error', error: string): void
}>()

const isRecording = ref(false)
const processing = ref(false)
const recordingTime = ref(0)
const audioUrl = ref('')

let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []
let timer: number | null = null

async function toggleRecording() {
  if (isRecording.value) {
    stopRecording()
  } else {
    await startRecording()
  }
}

async function startRecording() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder = new MediaRecorder(stream)
    audioChunks = []

    mediaRecorder.ondataavailable = (event) => {
      audioChunks.push(event.data)
    }

    mediaRecorder.onstop = () => {
      const audioBlob = new Blob(audioChunks, { type: 'audio/wav' })
      audioUrl.value = URL.createObjectURL(audioBlob)
      emit('recorded', audioBlob)
      
      // 停止所有音轨
      stream.getTracks().forEach(track => track.stop())
    }

    mediaRecorder.start()
    isRecording.value = true
    recordingTime.value = 0
    
    // 开始计时
    timer = window.setInterval(() => {
      recordingTime.value++
    }, 1000)
  } catch (error) {
    console.error('录音失败:', error)
    emit('error', '无法访问麦克风')
  }
}

function stopRecording() {
  if (mediaRecorder && isRecording.value) {
    mediaRecorder.stop()
    isRecording.value = false
    
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }
}

function clearRecording() {
  audioUrl.value = ''
  audioChunks = []
  recordingTime.value = 0
}

onUnmounted(() => {
  stopRecording()
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.voice-recorder {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-start;
}

.recording-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #ef4444;
  font-size: 14px;
}

.recording-dot {
  width: 12px;
  height: 12px;
  background-color: #ef4444;
  border-radius: 50%;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.audio-preview {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

audio {
  width: 300px;
  height: 40px;
}
</style>