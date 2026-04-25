import { ref } from 'vue'
import api from '@/api'

export type Dialect = 'mandarin' | 'cantonese' | 'henanese'

const DIALECT_LABELS: Record<Dialect, string> = {
  mandarin: '普通话',
  cantonese: '粤语',
  henanese: '河南话'
}

export function useSpeechRecognition() {
  const isListening = ref(false)
  const interimResult = ref('')
  const finalResult = ref('')
  const error = ref<string | null>(null)
  const currentDialect = ref<Dialect>('mandarin')

  let ws: WebSocket | null = null
  let audioContext: AudioContext | null = null
  let mediaStream: MediaStream | null = null
  let processor: ScriptProcessorNode | null = null

  function getDialectLabel(d: Dialect): string {
    return DIALECT_LABELS[d]
  }

  async function requestAuthUrl(dialect: Dialect): Promise<string> {
    const res = await api.get('/speech/auth', { params: { dialect } })
    return (res.data as any).url
  }

  async function startListening(dialect?: Dialect) {
    if (isListening.value) return
    if (dialect) currentDialect.value = dialect

    error.value = null
    interimResult.value = ''
    finalResult.value = ''

    try {
      // 1. Get auth URL from backend
      const authUrl = await requestAuthUrl(currentDialect.value)

      // 2. Get microphone access
      mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          sampleRate: 16000,
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true
        }
      })

      // 3. Setup audio processing
      audioContext = new AudioContext({ sampleRate: 16000 })
      const source = audioContext.createMediaStreamSource(mediaStream)
      processor = audioContext.createScriptProcessor(4096, 1, 1)

      // 4. Connect to Xfyun IAT WebSocket
      ws = new WebSocket(authUrl)

      ws.onopen = () => {
        isListening.value = true
        // Send first frame with business params
        const firstFrame = {
          common: {
            app_id: ''  // App ID is embedded in auth URL, not needed here
          },
          business: {
            language: currentDialect.value === 'cantonese' ? 'cn' : 'cn',
            domain: 'iat',
            accent: mapAccent(currentDialect.value),
            dwa: 'wpgs',
            pgs: 'apd',
            vad: {
              eos: 2000,
              bos: 5000
            }
          },
          data: {
            status: 0,
            format: 'audio/L16;rate=16000',
            encoding: 'raw',
            audio: ''
          }
        }
        ws!.send(JSON.stringify(firstFrame))
      }

      ws.onmessage = (event) => {
        try {
          const resp = JSON.parse(event.data)
          if (resp.code !== 0) {
            error.value = `识别错误: ${resp.message || resp.code}`
            stopListening()
            return
          }

          const result = resp.data?.result
          if (!result) return

          // Extract text from result
          const wsList = result.ws || []
          let text = ''
          for (const w of wsList) {
            for (const cw of (w.cw || [])) {
              text += cw.w
            }
          }

          if (result.ls === 1) {
            // Final result for this segment
            finalResult.value += text
            interimResult.value = ''
          } else {
            // Interim result
            interimResult.value = text
          }
        } catch (e) {
          // Ignore parse errors for partial data
        }
      }

      ws.onerror = () => {
        error.value = '语音识别连接失败'
        stopListening()
      }

      ws.onclose = () => {
        isListening.value = false
      }

      // 5. Process audio frames and send to WebSocket
      processor.onaudioprocess = (e) => {
        if (!ws || ws.readyState !== WebSocket.OPEN) return

        const inputData = e.inputBuffer.getChannelData(0)
        // Convert float32 to int16 PCM
        const pcm16 = float32ToInt16(inputData)
        const base64Audio = arrayBufferToBase64(pcm16.buffer as ArrayBuffer)

        const frame = {
          data: {
            status: 1,
            format: 'audio/L16;rate=16000',
            encoding: 'raw',
            audio: base64Audio
          }
        }
        ws.send(JSON.stringify(frame))
      }

      source.connect(processor)
      processor.connect(audioContext.destination)

    } catch (e: any) {
      if (e.name === 'NotAllowedError') {
        error.value = '请允许麦克风访问权限'
      } else if (e.name === 'NotFoundError') {
        error.value = '未检测到麦克风设备'
      } else {
        error.value = e.message || '语音识别启动失败'
      }
      stopListening()
    }
  }

  function stopListening() {
    // Send end frame
    if (ws && ws.readyState === WebSocket.OPEN) {
      const endFrame = {
        data: {
          status: 2,
          format: 'audio/L16;rate=16000',
          encoding: 'raw',
          audio: ''
        }
      }
      try {
        ws.send(JSON.stringify(endFrame))
      } catch {
        // ignore
      }
    }

    // Cleanup audio
    if (processor) {
      processor.disconnect()
      processor = null
    }
    if (audioContext) {
      audioContext.close()
      audioContext = null
    }
    if (mediaStream) {
      mediaStream.getTracks().forEach(t => t.stop())
      mediaStream = null
    }

    // Close WebSocket after a short delay to receive final result
    if (ws) {
      const w = ws
      ws = null
      setTimeout(() => {
        if (w.readyState === WebSocket.OPEN || w.readyState === WebSocket.CONNECTING) {
          w.close()
        }
      }, 1000)
    }

    isListening.value = false
  }

  function float32ToInt16(float32Array: Float32Array): Int16Array {
    const int16Array = new Int16Array(float32Array.length)
    for (let i = 0; i < float32Array.length; i++) {
      const s = Math.max(-1, Math.min(1, float32Array[i]))
      int16Array[i] = s < 0 ? s * 0x8000 : s * 0x7FFF
    }
    return int16Array
  }

  function arrayBufferToBase64(buffer: ArrayBuffer): string {
    const bytes = new Uint8Array(buffer)
    let binary = ''
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i])
    }
    return btoa(binary)
  }

  function mapAccent(dialect: Dialect): string {
    switch (dialect) {
      case 'cantonese': return 'cantonese'
      case 'henanese': return 'henanese'
      default: return 'mandarin'
    }
  }

  function reset() {
    interimResult.value = ''
    finalResult.value = ''
    error.value = null
  }

  return {
    isListening,
    interimResult,
    finalResult,
    error,
    currentDialect,
    startListening,
    stopListening,
    reset,
    getDialectLabel
  }
}
