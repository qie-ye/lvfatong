import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

export interface ContractDocument {
  id: number
  userId: number
  filename: string
  fileType: string
  fileSize: number
  status: string
  createdAt: string
}

export interface ClauseAnalysis {
  index: number
  title: string
  content: string
  riskLevel: string
  riskCategory: string
  description: string
  legalBasis: string
  suggestion: string
}

export interface ContractAnalysisResult {
  summary: string
  overallRisk: string
  clauses: ClauseAnalysis[]
}

export const useContractStore = defineStore('contract', () => {
  const contracts = ref<ContractDocument[]>([])
  const currentContract = ref<ContractDocument | null>(null)
  const analysisResult = ref<ContractAnalysisResult | null>(null)
  const uploading = ref(false)
  const analyzing = ref(false)
  const uploadProgress = ref(0)

  async function loadContracts() {
    const res = await api.get('/contract/list')
    contracts.value = res.data as ContractDocument[]
  }

  async function uploadFile(file: File): Promise<ContractDocument> {
    uploading.value = true
    uploadProgress.value = 0
    try {
      const formData = new FormData()
      formData.append('file', file)
      const token = localStorage.getItem('token')

      return new Promise<ContractDocument>((resolve, reject) => {
        const xhr = new XMLHttpRequest()
        xhr.open('POST', '/api/v1/contract/upload')
        if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`)

        xhr.upload.onprogress = (e) => {
          if (e.lengthComputable) {
            uploadProgress.value = Math.round((e.loaded / e.total) * 100)
          }
        }

        xhr.onload = () => {
          if (xhr.status >= 200 && xhr.status < 300) {
            try {
              const data = JSON.parse(xhr.responseText)
              if (data.code !== 200) {
                reject(new Error(data.message || '上传失败'))
                return
              }
              const contract = data.data as ContractDocument
              contracts.value.unshift(contract)
              currentContract.value = contract
              resolve(contract)
            } catch {
              reject(new Error('响应解析失败'))
            }
          } else {
            reject(new Error(`上传失败 (${xhr.status})`))
          }
        }

        xhr.onerror = () => reject(new Error('网络错误'))
        xhr.send(formData)
      })
    } finally {
      uploading.value = false
    }
  }

  async function analyzeContract(id: number) {
    analyzing.value = true
    try {
      // Trigger async analysis (returns 202)
      await api.post(`/contract/${id}/analyze`)
      // Update status in list
      const idx = contracts.value.findIndex(c => c.id === id)
      if (idx >= 0) contracts.value[idx].status = 'ANALYZING'
      // Poll for completion
      await pollAnalysisStatus(id)
    } finally {
      analyzing.value = false
    }
  }

  async function pollAnalysisStatus(id: number, maxAttempts = 60, interval = 3000) {
    for (let i = 0; i < maxAttempts; i++) {
      await new Promise(r => setTimeout(r, interval))
      try {
        const statusRes = await api.get(`/contract/${id}/status`)
        const status = (statusRes.data as { status: string }).status
        if (status === 'COMPLETED') {
          await getAnalysis(id)
          const idx = contracts.value.findIndex(c => c.id === id)
          if (idx >= 0) contracts.value[idx].status = 'COMPLETED'
          return
        }
        if (status === 'FAILED') {
          const idx = contracts.value.findIndex(c => c.id === id)
          if (idx >= 0) contracts.value[idx].status = 'FAILED'
          throw new Error('合同分析失败')
        }
        // Still ANALYZING, continue polling
      } catch (e: any) {
        if (e.message === '合同分析失败') throw e
        // Network error, continue polling
      }
    }
    throw new Error('分析超时，请稍后在合同列表中查看结果')
  }

  async function getAnalysis(id: number) {
    const res = await api.get(`/contract/${id}/analysis`)
    analysisResult.value = res.data as ContractAnalysisResult
    return analysisResult.value
  }

  function reset() {
    currentContract.value = null
    analysisResult.value = null
    uploadProgress.value = 0
  }

  return {
    contracts, currentContract, analysisResult, uploading, analyzing, uploadProgress,
    loadContracts, uploadFile, analyzeContract, getAnalysis, reset
  }
})
