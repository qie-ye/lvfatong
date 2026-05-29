import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export const useDashboardStore = defineStore('dashboard', () => {
  // 状态
  const loading = ref(false)
  const timeRange = ref(30)
  
  // 用户概览数据
  const userOverview = ref({
    totalUsers: 0,
    dailyActiveUsers: 0,
    weeklyActiveUsers: 0,
    monthlyActiveUsers: 0
  })

  // 查询统计数据
  const queryStats = ref({
    totalQueries: 0,
    dailyTrend: [] as Array<{date: string, count: number}>,
    peakHours: [] as Array<{hour: string, count: number}>
  })

  // 热门问题数据
  const hotQueries = ref({
    hotKeywords: [] as Array<{keyword: string, count: number}>,
    hotDomains: [] as Array<{domain: string, count: number}>
  })

  // AI效果数据
  const aiPerformance = ref({
    satisfactionRate: 0,
    totalFeedback: 0,
    goodCount: 0,
    badCount: 0,
    estimatedAccuracy: 0
  })

  // 推荐效果数据
  const recommendationStats = ref({
    totalRecommendations: 0,
    clickedRecommendations: 0,
    clickRate: 0,
    conversionRate: 0
  })

  // 合同分析数据
  const contractStats = ref({
    totalContracts: 0,
    riskDistribution: [] as Array<{status: string, count: number}>,
    dailyTrend: [] as Array<{date: string, count: number}>
  })

  // 计算属性
  const userKpis = computed(() => [
    { label: '总用户数', value: userOverview.value.totalUsers, color: '#6366f1' },
    { label: '今日活跃', value: userOverview.value.dailyActiveUsers, color: '#3b82f6' },
    { label: '本周活跃', value: userOverview.value.weeklyActiveUsers, color: '#22c55e' },
    { label: '本月活跃', value: userOverview.value.monthlyActiveUsers, color: '#f59e0b' }
  ])

  // 加载所有数据
  async function loadAllData() {
    loading.value = true
    try {
      const [overviewRes, queryRes, hotRes, aiRes, recRes, contractRes] = await Promise.all([
        api.get('/dashboard/overview'),
        api.get('/dashboard/query-stats', { params: { days: timeRange.value } }),
        api.get('/dashboard/hot-queries', { params: { limit: 10 } }),
        api.get('/dashboard/ai-performance'),
        api.get('/dashboard/recommendation-stats'),
        api.get('/dashboard/contract-stats')
      ])

      userOverview.value = overviewRes.data
      queryStats.value = queryRes.data
      hotQueries.value = hotRes.data
      aiPerformance.value = aiRes.data
      recommendationStats.value = recRes.data
      contractStats.value = contractRes.data
    } catch (error) {
      console.error('加载Dashboard数据失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 加载用户概览数据
  async function loadUserOverview() {
    try {
      const res = await api.get('/dashboard/overview')
      userOverview.value = res.data
    } catch (error) {
      console.error('加载用户概览数据失败:', error)
      throw error
    }
  }

  // 加载查询统计数据
  async function loadQueryStats(days?: number) {
    try {
      const res = await api.get('/dashboard/query-stats', { params: { days: days || timeRange.value } })
      queryStats.value = res.data
    } catch (error) {
      console.error('加载查询统计数据失败:', error)
      throw error
    }
  }

  // 加载热门问题数据
  async function loadHotQueries(limit?: number) {
    try {
      const res = await api.get('/dashboard/hot-queries', { params: { limit: limit || 10 } })
      hotQueries.value = res.data
    } catch (error) {
      console.error('加载热门问题数据失败:', error)
      throw error
    }
  }

  // 加载AI效果数据
  async function loadAIPerformance() {
    try {
      const res = await api.get('/dashboard/ai-performance')
      aiPerformance.value = res.data
    } catch (error) {
      console.error('加载AI效果数据失败:', error)
      throw error
    }
  }

  // 加载推荐效果数据
  async function loadRecommendationStats() {
    try {
      const res = await api.get('/dashboard/recommendation-stats')
      recommendationStats.value = res.data
    } catch (error) {
      console.error('加载推荐效果数据失败:', error)
      throw error
    }
  }

  // 加载合同分析数据
  async function loadContractStats() {
    try {
      const res = await api.get('/dashboard/contract-stats')
      contractStats.value = res.data
    } catch (error) {
      console.error('加载合同分析数据失败:', error)
      throw error
    }
  }

  // 设置时间范围
  function setTimeRange(range: number) {
    timeRange.value = range
  }

  return {
    // 状态
    loading,
    timeRange,
    userOverview,
    queryStats,
    hotQueries,
    aiPerformance,
    recommendationStats,
    contractStats,
    
    // 计算属性
    userKpis,
    
    // 方法
    loadAllData,
    loadUserOverview,
    loadQueryStats,
    loadHotQueries,
    loadAIPerformance,
    loadRecommendationStats,
    loadContractStats,
    setTimeRange
  }
})