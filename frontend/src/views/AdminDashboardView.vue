<template>
  <div class="admin-dashboard">
    <h2 class="page-title">管理后台</h2>

    <div class="kpi-row">
      <div class="kpi-card" v-for="kpi in kpiList" :key="kpi.label" :class="{ 'kpi-accent': kpi.accent }">
        <div class="kpi-icon" :style="{ background: kpi.color }">
          <el-icon :size="20" color="#ffffff"><component :is="kpi.icon" /></el-icon>
        </div>
        <div class="kpi-body">
          <div class="kpi-label">{{ kpi.label }}</div>
          <div class="kpi-value">{{ kpi.display }}</div>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <div class="chart-card">
        <h3>近30天注册趋势</h3>
        <v-chart :option="userTrendOption" autoresize style="height:260px" />
      </div>
      <div class="chart-card">
        <h3>意图类型分布</h3>
        <v-chart :option="intentOption" autoresize style="height:260px" />
      </div>
      <div class="chart-card">
        <h3>近7天好评率（%）</h3>
        <v-chart :option="feedbackOption" autoresize style="height:260px" />
      </div>
      <div class="chart-card">
        <h3>律师咨询量 TOP10</h3>
        <v-chart :option="lawyerOption" autoresize style="height:260px" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { User, ChatDotRound, TrendCharts, Star } from '@element-plus/icons-vue'
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

interface Overview { totalUsers: number; totalSessions: number; todayActive: number; satisfactionRate: number }
interface DailyStat { date: string; count: number }
interface NameValue  { name: string; value: number }

const overview = ref<Overview>({ totalUsers: 0, totalSessions: 0, todayActive: 0, satisfactionRate: 0 })

const userTrendOption  = ref<object>({})
const intentOption     = ref<object>({})
const feedbackOption   = ref<object>({})
const lawyerOption     = ref<object>({})

const PIE_COLORS = ['#6366f1', '#3b82f6', '#06b6d4', '#22c55e', '#f59e0b', '#f97316', '#ec4899', '#8b5cf6']

const kpiList = computed(() => [
  { label: '注册用户', display: overview.value.totalUsers.toLocaleString(), icon: User, color: '#6366f1', accent: false },
  { label: '累计咨询', display: overview.value.totalSessions.toLocaleString(), icon: ChatDotRound, color: '#3b82f6', accent: false },
  { label: '今日活跃', display: overview.value.todayActive.toLocaleString(), icon: TrendCharts, color: '#22c55e', accent: false },
  { label: '好评率', display: overview.value.satisfactionRate + '%', icon: Star, color: '#6366f1', accent: true }
])

const BASE_TOOLTIP = {
  trigger: 'axis',
  backgroundColor: '#111827',
  borderColor: '#3b82f6',
  textStyle: { color: '#f9fafb', fontSize: 12 }
}

function buildLineOption(xData: string[], yData: number[], color: string) {
  return {
    tooltip: { ...BASE_TOOLTIP, trigger: 'axis' },
    grid: { left: 44, right: 16, top: 20, bottom: 36 },
    xAxis: { type: 'category', data: xData, axisLabel: { fontSize: 11, color: '#9ca3af' }, axisLine: { lineStyle: { color: '#e5e7eb' } } },
    yAxis: { type: 'value', axisLabel: { fontSize: 11, color: '#9ca3af' }, splitLine: { lineStyle: { color: '#f3f4f6' } } },
    series: [{
      type: 'line', data: yData, smooth: true,
      itemStyle: { color }, lineStyle: { width: 2.5 },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: color + '30' }, { offset: 1, color: color + '05' }] } }
    }]
  }
}

async function loadAll() {
  const [ov, users, intents, feedback, lawyers] = await Promise.all([
    api.get('/admin/stats/overview'),
    api.get('/admin/stats/users'),
    api.get('/admin/stats/intents'),
    api.get('/admin/stats/feedback'),
    api.get('/admin/stats/lawyers')
  ])

  overview.value = ov.data as Overview

  const userRows = users.data as DailyStat[]
  userTrendOption.value = buildLineOption(userRows.map(r => r.date), userRows.map(r => r.count), '#6366f1')

  const intentRows = intents.data as NameValue[]
  intentOption.value = {
    tooltip: { trigger: 'item', backgroundColor: '#111827', borderColor: '#3b82f6', textStyle: { color: '#f9fafb', fontSize: 12 } },
    legend: { bottom: 0, textStyle: { fontSize: 11, color: '#9ca3af' } },
    color: PIE_COLORS,
    series: [{
      type: 'pie', radius: ['42%', '72%'], center: ['50%', '44%'],
      data: intentRows.map(r => ({ name: r.name, value: r.value })),
      label: { fontSize: 11, color: '#9ca3af' },
      emphasis: { scaleSize: 8 },
      itemStyle: { borderColor: '#ffffff', borderWidth: 2 }
    }]
  }

  const fbRows = feedback.data as DailyStat[]
  feedbackOption.value = buildLineOption(fbRows.map(r => r.date), fbRows.map(r => r.count), '#22c55e')

  const lawyerRows = lawyers.data as NameValue[]
  lawyerOption.value = {
    tooltip: { trigger: 'axis', backgroundColor: '#111827', borderColor: '#3b82f6', textStyle: { color: '#f9fafb', fontSize: 12 } },
    grid: { left: 80, right: 16, top: 8, bottom: 16 },
    xAxis: { type: 'value', axisLabel: { fontSize: 11, color: '#9ca3af' }, splitLine: { lineStyle: { color: '#f3f4f6' } } },
    yAxis: { type: 'category', data: lawyerRows.map(r => r.name).reverse(), axisLabel: { fontSize: 11, color: '#4b5563' } },
    series: [{
      type: 'bar', data: lawyerRows.map(r => r.value).reverse(),
      itemStyle: { color: '#6366f1', borderRadius: [0, 4, 4, 0] },
      barMaxWidth: 22, emphasis: { itemStyle: { color: '#818cf8' } }
    }]
  }
}

onMounted(loadAll)
</script>

<style scoped>
.admin-dashboard {
  padding: 28px 24px;
  max-width: 1240px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 24px;
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}

.kpi-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px 22px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  gap: 14px;
  border: 1px solid #e5e7eb;
  transition: all 0.25s ease;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
}

.kpi-card.kpi-accent {
  background: linear-gradient(135deg, #1e1b4b, #312e81);
  border-color: transparent;
}

.kpi-card.kpi-accent .kpi-label,
.kpi-card.kpi-accent .kpi-value {
  color: #a5b4fc;
}

.kpi-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.kpi-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.kpi-label {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 500;
}

.kpi-value {
  font-size: 26px;
  font-weight: 700;
  color: #111827;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
}

.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 22px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #e5e7eb;
  transition: all 0.25s ease;
}

.chart-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.06);
}

.chart-card h3 {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
  .charts-grid { grid-template-columns: 1fr; }
}

html.dark .page-title { color: var(--text-primary); }
html.dark .kpi-card { background: var(--bg-card); border-color: var(--border); }
html.dark .kpi-card.kpi-accent { background: linear-gradient(135deg, #1e1b4b, #312e81); border-color: transparent; }
html.dark .kpi-label { color: var(--text-tertiary); }
html.dark .kpi-value { color: var(--text-primary); }
html.dark .chart-card { background: var(--bg-card); border-color: var(--border); }
html.dark .chart-card h3 { color: var(--text-primary); }
</style>
