import { ref } from 'vue'
import api from '@/api'

export function useMultimodal() {
  const loading = ref(false)
  const error = ref<string | null>(null)

  // OCR识别
  async function recognizeText(file: File) {
    loading.value = true
    error.value = null
    try {
      const formData = new FormData()
      formData.append('file', file)
      const res = await api.post('/ocr/recognize', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      return res.data
    } catch (err: any) {
      error.value = err.message || 'OCR识别失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 合同识别
  async function recognizeContract(file: File) {
    loading.value = true
    error.value = null
    try {
      const formData = new FormData()
      formData.append('file', file)
      const res = await api.post('/ocr/contract', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      return res.data
    } catch (err: any) {
      error.value = err.message || '合同识别失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 语音识别
  async function recognizeSpeech(file: File) {
    loading.value = true
    error.value = null
    try {
      const formData = new FormData()
      formData.append('audio', file)
      const res = await api.post('/multimodal/asr', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      return res.data
    } catch (err: any) {
      error.value = err.message || '语音识别失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 文本转语音
  async function textToSpeech(text: string, voice?: string) {
    loading.value = true
    error.value = null
    try {
      const formData = new FormData()
      formData.append('text', text)
      if (voice) formData.append('voice', voice)
      const res = await api.post('/multimodal/tts', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      return res.data
    } catch (err: any) {
      error.value = err.message || '语音合成失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 多模态聊天
  async function multimodalChat(text?: string, image?: File, audio?: File) {
    loading.value = true
    error.value = null
    try {
      const formData = new FormData()
      if (text) formData.append('text', text)
      if (image) formData.append('image', image)
      if (audio) formData.append('audio', audio)
      const res = await api.post('/multimodal/chat', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      return res.data
    } catch (err: any) {
      error.value = err.message || '多模态聊天失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 图片问答
  async function imageQuestion(image: File, question: string) {
    loading.value = true
    error.value = null
    try {
      const formData = new FormData()
      formData.append('image', image)
      formData.append('question', question)
      const res = await api.post('/multimodal/image-question', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      return res.data
    } catch (err: any) {
      error.value = err.message || '图片问答失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 语音问答
  async function voiceQuestion(audio: File) {
    loading.value = true
    error.value = null
    try {
      const formData = new FormData()
      formData.append('audio', audio)
      const res = await api.post('/multimodal/voice-question', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      return res.data
    } catch (err: any) {
      error.value = err.message || '语音问答失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 获取可用语音列表
  async function getAvailableVoices() {
    try {
      const res = await api.get('/multimodal/voices')
      return res.data
    } catch (err: any) {
      error.value = err.message || '获取语音列表失败'
      throw err
    }
  }

  return {
    loading,
    error,
    recognizeText,
    recognizeContract,
    recognizeSpeech,
    textToSpeech,
    multimodalChat,
    imageQuestion,
    voiceQuestion,
    getAvailableVoices
  }
}