<template>
  <div class="voice-player">
    <el-button
      :type="isPlaying ? 'warning' : 'success'"
      :icon="isPlaying ? 'VideoPause' : 'VideoPlay'"
      @click="togglePlay"
      :disabled="!audioSrc"
    >
      {{ isPlaying ? '暂停' : '播放' }}
    </el-button>
    
    <div v-if="audioSrc" class="player-controls">
      <div class="progress-bar" @click="seek">
        <div class="progress" :style="{ width: progress + '%' }"></div>
      </div>
      <span class="time">{{ formatTime(currentTime) }} / {{ formatTime(duration) }}</span>
    </div>
    
    <el-button v-if="audioSrc" size="small" @click="stop">
      <el-icon><Close /></el-icon>
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'

const props = defineProps<{
  audioSrc?: string
}>()

const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const progress = ref(0)

let audio: HTMLAudioElement | null = null

watch(() => props.audioSrc, (newSrc) => {
  if (audio) {
    audio.pause()
    audio = null
  }
  if (newSrc) {
    audio = new Audio(newSrc)
    audio.addEventListener('loadedmetadata', () => {
      duration.value = audio?.duration || 0
    })
    audio.addEventListener('timeupdate', () => {
      if (audio) {
        currentTime.value = audio.currentTime
        progress.value = (audio.currentTime / audio.duration) * 100
      }
    })
    audio.addEventListener('ended', () => {
      isPlaying.value = false
      currentTime.value = 0
      progress.value = 0
    })
  }
})

function togglePlay() {
  if (!audio) return
  
  if (isPlaying.value) {
    audio.pause()
    isPlaying.value = false
  } else {
    audio.play()
    isPlaying.value = true
  }
}

function stop() {
  if (audio) {
    audio.pause()
    audio.currentTime = 0
    isPlaying.value = false
    currentTime.value = 0
    progress.value = 0
  }
}

function seek(event: MouseEvent) {
  if (!audio) return
  const rect = (event.target as HTMLElement).getBoundingClientRect()
  const percent = (event.clientX - rect.left) / rect.width
  audio.currentTime = percent * audio.duration
}

function formatTime(seconds: number): string {
  const min = Math.floor(seconds / 60)
  const sec = Math.floor(seconds % 60)
  return `${min}:${sec.toString().padStart(2, '0')}`
}

onUnmounted(() => {
  if (audio) {
    audio.pause()
    audio = null
  }
})
</script>

<style scoped>
.voice-player {
  display: flex;
  align-items: center;
  gap: 12px;
}

.player-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background-color: #e5e7eb;
  border-radius: 3px;
  cursor: pointer;
  min-width: 100px;
}

.progress {
  height: 100%;
  background-color: #22c55e;
  border-radius: 3px;
  transition: width 0.1s;
}

.time {
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap;
}
</style>