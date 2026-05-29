import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

export interface RecommendationItem {
  id?: number
  type: 'law' | 'case' | 'faq' | 'lawyer' | 'topic'
  title: string
  description?: string
  score?: number
  url?: string
}

export interface RecommendationResult {
  items: RecommendationItem[]
  strategy: string
}

export const useRecommendationStore = defineStore('recommendation', () => {
  const personalizedRecommendations = ref<RecommendationItem[]>([])
  const queryBasedRecommendations = ref<RecommendationItem[]>([])
  const popularRecommendations = ref<RecommendationItem[]>([])
  const isLoading = ref(false)

  // 获取个性化推荐
  async function fetchPersonalized(limit = 10) {
    isLoading.value = true
    try {
      const res = await api.get('/recommendations/personalized', { params: { limit } })
      const data = res as any
      personalizedRecommendations.value = data.data?.items || []
    } catch (error) {
      console.error('Failed to fetch personalized recommendations:', error)
      personalizedRecommendations.value = []
    } finally {
      isLoading.value = false
    }
  }

  // 获取基于查询的推荐
  async function fetchQueryBased(query: string, domain?: string, limit = 10) {
    try {
      const res = await api.get('/recommendations/query-based', {
        params: { query, domain, limit }
      })
      const data = res as any
      queryBasedRecommendations.value = data.data?.items || []
    } catch (error) {
      console.error('Failed to fetch query-based recommendations:', error)
      queryBasedRecommendations.value = []
    }
  }

  // 获取热门推荐
  async function fetchPopular(limit = 10) {
    try {
      const res = await api.get('/recommendations/popular', { params: { limit } })
      const data = res as any
      popularRecommendations.value = data.data?.items || []
    } catch (error) {
      console.error('Failed to fetch popular recommendations:', error)
      popularRecommendations.value = []
    }
  }

  // 记录用户行为
  async function recordBehavior(
    actionType: string,
    targetType: string,
    targetId?: number,
    query?: string,
    domain?: string
  ) {
    try {
      await api.post('/recommendations/behaviors', null, {
        params: { actionType, targetType, targetId, query, domain }
      })
    } catch (error) {
      console.error('Failed to record behavior:', error)
    }
  }

  // 获取查询历史
  async function fetchQueryHistory(limit = 10): Promise<string[]> {
    try {
      const res = await api.get('/recommendations/history', { params: { limit } })
      const data = res as any
      return data.data || []
    } catch (error) {
      console.error('Failed to fetch query history:', error)
      return []
    }
  }

  return {
    personalizedRecommendations,
    queryBasedRecommendations,
    popularRecommendations,
    isLoading,
    fetchPersonalized,
    fetchQueryBased,
    fetchPopular,
    recordBehavior,
    fetchQueryHistory
  }
})
