<template>
  <div class="admin-dashboard">
    <h2 class="page-title">管理后台</h2>

    <!-- KPI Cards -->
    <div class="kpi-row">
      <div class="kpi-card">
        <div class="kpi-label">总用户数</div>
        <div class="kpi-value">{{ overview.totalUsers }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">总对话数</div>
        <div class="kpi-value">{{ overview.totalSessions }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">今日活跃</div>
        <div class="kpi-value">{{ overview.todayActive }}</div>
      </div>
      <div class="kpi-card accent">
        <div class="kpi-label">满意率</div>
        <div class="kpi-value">{{ overview.satisfactionRate }}%</div>
      </div>
    </div>

    <!-- Charts -->
    <div class="charts-grid">
      <div class="chart-card">
        <h3>近30天注册趋势</h3>
        <v-chart :option="userTrendOption" autoresize style="height:240px" />
      </div>
      <div class="chart-card">
        <h3>意图类型分布</h3>
        <v-chart :option="intentOption" autoresize style="height:240px" />
      </div>
      <div class="chart-card">
        <h3>近7天好评率（%）</h3>
        <v-chart :option="feedbackOption" autoresize style="height:240px" />
      </div>
      <div class="chart-card">
        <h3>律师咨询量 TOP10</h3>
        <v-chart :option="lawyerOption" autoresize style="height:240px" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent,
  TitleComponent, DataZoomComponent
} from 'echarts/components'
import api from '@/api'

use([CanvasRenderer, LineChart, PieChart, BarChart,
     GridComponent, TooltipComponent, LegendComponent,
     TitleComponent, DataZoomComponent])

interface Overview { totalUsers: number; totalSessions: number; todayActive: number; satisfactionRate: number }
interface DailyStat { date: string; count: number }
interface NameValue  { name: string; value: number }

const overview = ref<Overview>({ totalUsers: 0, totalSessions: 0, todayActive: 0, satisfactionRate: 0 })

const userTrendOption  = ref<object>({})
const intentOption     = ref<object>({})
const feedbackOption   = ref<object>({})
const lawyerOption     = ref<object>({})

const BASE_TOOLTIP = { trigger: 'axis', backgroundColor: '#1a1a2e', borderColor: '#4fc3f7', textStyle: { color: '#fff' } }

function buildLineOption(xData: string[], yData: number[], color: string) {
  return {
    tooltip: BASE_TOOLTIP,
    grid: { left: 40, right: 16, top: 16, bottom: 36 },
    xAxis: { type: 'category', data: xData, axisLabel: { fontSize: 11, color: '#666' } },
    yAxis: { type: 'value', axisLabel: { fontSize: 11, color: '#666' } },
    series: [{ type: 'line', data: yData, smooth: true,
               itemStyle: { color }, areaStyle: { color, opacity: 0.12 } }]
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
  userTrendOption.value = buildLineOption(
    userRows.map(r => r.date), userRows.map(r => r.count), '#4fc3f7')

  const intentRows = intents.data as NameValue[]
  intentOption.value = {
    tooltip: { trigger: 'item', backgroundColor: '#1a1a2e', borderColor: '#4fc3f7', textStyle: { color: '#fff' } },
    legend: { bottom: 0, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie', radius: ['35%', '65%'], center: ['50%', '45%'],
      data: intentRows.map(r => ({ name: r.name, value: r.value })),
      label: { fontSize: 11 }
    }]
  }

  const fbRows = feedback.data as DailyStat[]
  feedbackOption.value = buildLineOption(
    fbRows.map(r => r.date), fbRows.map(r => r.count), '#67c23a')

  const lawyerRows = lawyers.data as NameValue[]
  lawyerOption.value = {
    tooltip: { trigger: 'axis', backgroundColor: '#1a1a2e', borderColor: '#4fc3f7', textStyle: { color: '#fff' } },
    grid: { left: 80, right: 16, top: 8, bottom: 16 },
    xAxis: { type: 'value', axisLabel: { fontSize: 11, color: '#666' } },
    yAxis: { type: 'category', data: lawyerRows.map(r => r.name).reverse(),
             axisLabel: { fontSize: 11, color: '#444' } },
    series: [{ type: 'bar', data: lawyerRows.map(r => r.value).reverse(),
               itemStyle: { color: '#a855f7' }, barMaxWidth: 20 }]
  }
}

onMounted(loadAll)
</script>

<style scoped>
.admin-dashboard {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 20px;
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.kpi-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kpi-card.accent {
  background: linear-gradient(135deg, #1a1a2e, #16213e);
}

.kpi-card.accent .kpi-label,
.kpi-card.accent .kpi-value {
  color: #4fc3f7;
}

.kpi-label {
  font-size: 13px;
  color: #888;
}

.kpi-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
}

.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.chart-card h3 {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

@media (max-width: 768px) {
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
  .charts-grid { grid-template-columns: 1fr; }
}
</style>
