import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

export interface LegalOpinion {
  id: number
  title: string
  domain: string
  question: string
  facts: string
  analysis: string
  conclusion: string
  legalBasis: string
  suggestions: string
  status: string
  model: string
  createdAt: string
}

export const useOpinionStore = defineStore('opinion', () => {
  const opinions = ref<LegalOpinion[]>([])
  const currentOpinion = ref<LegalOpinion | null>(null)
  const loading = ref(false)

  async function generateOpinion(title: string, domain: string, question: string, facts: string) {
    const res = await api.post('/opinions', { title, domain, question, facts })
    return res.data as LegalOpinion
  }

  async function getOpinion(id: number) {
    loading.value = true
    try {
      const res = await api.get(`/opinions/${id}`)
      currentOpinion.value = res.data as LegalOpinion
      return currentOpinion.value
    } finally {
      loading.value = false
    }
  }

  async function loadOpinions() {
    loading.value = true
    try {
      const res = await api.get('/opinions')
      opinions.value = (res.data as LegalOpinion[]) || []
    } finally {
      loading.value = false
    }
  }

  return { opinions, currentOpinion, loading, generateOpinion, getOpinion, loadOpinions }
})
