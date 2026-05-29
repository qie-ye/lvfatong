<template>
  <div class="dashboard-container">
    <h2 class="page-title">数据分析看板</h2>
    
    <!-- 时间范围筛选 -->
    <div class="filter-bar">
      <el-select v-model="timeRange" placeholder="时间范围" @change="loadData">
        <el-option label="最近7天" :value="7" />
        <el-option label="最近30天" :value="30" />
        <el-option label="最近90天" :value="90" />
      </el-select>
      <el-button type="primary" @click="loadData" :loading="loading">
        <el-icon><Refresh /></el-icon>
        刷新数据
      </el-button>
    </div>

    <!-- 用户概览 -->
    <div class="section">
      <h3 class="section-title">用户概览</h3>
      <div class="kpi-row">
        <div class="kpi-card" v-for="kpi in userKpis" :key="kpi.label">
          <div class="kpi-icon" :style="{ background: kpi.color }">
            <el-icon :size="20" color="#ffffff"><component :is="kpi.icon" /></el-icon>
          </div>
          <div class="kpi-body">
            <div class="kpi-label">{{ kpi.label }}</div>
            <div class="kpi-value">{{ kpi.display }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 查询统计 -->
    <div class="section">
      <h3 class="section-title">查询统计</h3>
      <div class="charts-grid">
        <div class="chart-card">
          <h4>查询趋势</h4>
          <v-chart :option="queryTrendOption" autoresize style="height:300px" />
        </div>
        <div class="chart-card">
          <h4>高峰时段分布</h4>
          <v-chart :option="peakHoursOption" autoresize style="height:300px" />
        </div>
      </div>
    </div>

    <!-- 热门问题 -->
    <div class="section">
      <h3 class="section-title">热门问题</h3>
      <div class="charts-grid">
        <div class="chart-card">
          <h4>高频查询词 TOP10</h4>
          <v-chart :option="hotKeywordsOption" autoresize style="height:300px" />
        </div>
        <div class="chart-card">
          <h4>热门法律领域</h4>
          <v-chart :option="hotDomainsOption" autoresize style="height:300px" />
        </div>
      </div>
    </div>

    <!-- AI效果 -->
    <div class="section">
      <h3 class="section-title">AI效果</h3>
      <div class="charts-grid">
        <div class="chart-card">
          <h4>用户满意度</h4>
          <v-chart :option="satisfactionOption" autoresize style="height:300px" />
        </div>
        <div class="chart-card">
          <h4>反馈统计</h4>
          <v-chart :option="feedbackOption" autoresize style="height:300px" />
        </div>
      </div>
    </div>

    <!-- 推荐效果 -->
    <div class="section">
      <h3 class="section-title">推荐效果</h3>
      <div class="charts-grid">
        <div class="chart-card">
          <h4>推荐点击率</h4>
          <v-chart :option="clickRateOption" autoresize style="height:300px" />
        </div>
        <div class="chart-card">
          <h4>推荐转化率</h4>
          <v-chart :option="conversionRateOption" autoresize style="height:300px" />
        </div>
      </div>
    </div>

    <!-- 合同分析 -->
    <div class="section">
      <h3 class="section-title">合同分析</h3>
      <div class="charts-grid">
        <div class="chart-card">
          <h4>合同分析趋势</h4>
          <v-chart :option="contractTrendOption" autoresize style="height:300px" />
        </div>
        <div class="chart-card">
          <h4>风险分布</h4>
          <v-chart :option="riskDistributionOption" autoresize style="height:300px" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Refresh, User, ChatDotRound, TrendCharts, Star } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent,
  TitleComponent
} from 'echarts/components'
import api from '@/api'

use([CanvasRenderer, LineChart, PieChart, BarChart,
     GridComponent, TooltipComponent, LegendComponent,
     TitleComponent])

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

// KPI卡片数据
const userKpis = computed(() => [
  { label: '总用户数', display: userOverview.value.totalUsers.toLocaleString(), icon: User, color: '#6366f1' },
  { label: '今日活跃', display: userOverview.value.dailyActiveUsers.toLocaleString(), icon: ChatDotRound, color: '#3b82f6' },
  { label: '本周活跃', display: userOverview.value.weeklyActiveUsers.toLocaleString(), icon: TrendCharts, color: '#22c55e' },
  { label: '本月活跃', display: userOverview.value.monthlyActiveUsers.toLocaleString(), icon: Star, color: '#f59e0b' }
])

// 查询趋势图表选项
const queryTrendOption = computed(() => {
  const data = queryStats.value.dailyTrend
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: data.map(d => d.date) },
    yAxis: { type: 'value' },
    series: [{
      type: 'line',
      data: data.map(d => d.count),
      smooth: true,
      areaStyle: { opacity: 0.3 },
      itemStyle: { color: '#6366f1' }
    }]
  }
})

// 高峰时段图表选项
const peakHoursOption = computed(() => {
  const data = queryStats.value.peakHours
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: data.map(d => d.hour + ':00') },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: data.map(d => d.count),
      itemStyle: { color: '#3b82f6' }
    }]
  }
})

// 高频查询词图表选项
const hotKeywordsOption = computed(() => {
  const data = hotQueries.value.hotKeywords.slice(0, 10)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 100, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: data.map(d => d.keyword).reverse() },
    series: [{
      type: 'bar',
      data: data.map(d => d.count).reverse(),
      itemStyle: { color: '#22c55e' }
    }]
  }
})

// 热门法律领域图表选项
const hotDomainsOption = computed(() => {
  const data = hotQueries.value.hotDomains
  return {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: data.map(d => ({ name: d.domain, value: d.count })),
      label: { show: true, formatter: '{b}: {c}' }
    }]
  }
})

// 用户满意度图表选项
const satisfactionOption = computed(() => {
  const rate = aiPerformance.value.satisfactionRate
  return {
    series: [{
      type: 'gauge',
      progress: { show: true },
      detail: { valueAnimation: true, formatter: '{value}%', color: '#333' },
      data: [{ value: rate, name: '满意度' }],
      axisLine: { lineStyle: { width: 20, color: [[0.3, '#ff4d4f'], [0.7, '#faad14'], [1, '#52c41a']] } }
    }]
  }
})

// 反馈统计图表选项
const feedbackOption = computed(() => {
  const good = aiPerformance.value.goodCount
  const bad = aiPerformance.value.badCount
  return {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { name: '好评', value: good, itemStyle: { color: '#52c41a' } },
        { name: '差评', value: bad, itemStyle: { color: '#ff4d4f' } }
      ],
      label: { show: true, formatter: '{b}: {c}' }
    }]
  }
})

// 推荐点击率图表选项
const clickRateOption = computed(() => {
  const rate = recommendationStats.value.clickRate
  return {
    series: [{
      type: 'gauge',
      progress: { show: true },
      detail: { valueAnimation: true, formatter: '{value}%', color: '#333' },
      data: [{ value: rate, name: '点击率' }],
      axisLine: { lineStyle: { width: 20, color: [[0.3, '#ff4d4f'], [0.7, '#faad14'], [1, '#52c41a']] } }
    }]
  }
})

// 推荐转化率图表选项
const conversionRateOption = computed(() => {
  const rate = recommendationStats.value.conversionRate
  return {
    series: [{
      type: 'gauge',
      progress: { show: true },
      detail: { valueAnimation: true, formatter: '{value}%', color: '#333' },
      data: [{ value: rate, name: '转化率' }],
      axisLine: { lineStyle: { width: 20, color: [[0.3, '#ff4d4f'], [0.7, '#faad14'], [1, '#52c41a']] } }
    }]
  }
})

// 合同分析趋势图表选项
const contractTrendOption = computed(() => {
  const data = contractStats.value.dailyTrend
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: data.map(d => d.date) },
    yAxis: { type: 'value' },
    series: [{
      type: 'line',
      data: data.map(d => d.count),
      smooth: true,
      areaStyle: { opacity: 0.3 },
      itemStyle: { color: '#f59e0b' }
    }]
  }
})

// 风险分布图表选项
const riskDistributionOption = computed(() => {
  const data = contractStats.value.riskDistribution
  return {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: data.map(d => ({ name: d.status, value: d.count })),
      label: { show: true, formatter: '{b}: {c}' }
    }]
  }
})

// 加载所有数据
async function loadData() {
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
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.dashboard-container {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 24px;
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  align-items: center;
}

.section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e5e7eb;
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.kpi-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #e5e7eb;
}

.kpi-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.kpi-body {
  display: flex;
  flex-direction: column;
}

.kpi-label {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 4px;
}

.kpi-value {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}

.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e7eb;
}

.chart-card h4 {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
  .charts-grid { grid-template-columns: 1fr; }
}

html.dark .page-title,
html.dark .section-title,
html.dark .chart-card h4 { color: var(--text-primary); }
html.dark .kpi-card,
html.dark .chart-card { background: var(--bg-card); border-color: var(--border); }
html.dark .kpi-label { color: var(--text-tertiary); }
html.dark .kpi-value { color: var(--text-primary); }
</style>