<template>
  <div class="knowledge-graph-container">
    <h2 class="page-title">知识图谱</h2>
    
    <!-- 搜索和筛选 -->
    <div class="filter-bar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索实体..."
        clearable
        @keyup.enter="searchEntities"
      >
        <template #append>
          <el-button @click="searchEntities">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      
      <el-select v-model="entityType" placeholder="实体类型" clearable>
        <el-option label="法律法规" value="LAW" />
        <el-option label="法条" value="ARTICLE" />
        <el-option label="案例" value="CASE" />
        <el-option label="法律概念" value="CONCEPT" />
      </el-select>
      
      <el-button type="primary" @click="loadEntities" :loading="loading">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 图谱可视化 -->
    <div class="graph-section">
      <div class="graph-container" ref="graphContainer">
        <v-chart :option="graphOption" autoresize style="height: 500px" />
      </div>
    </div>

    <!-- 实体列表 -->
    <div class="entities-section">
      <h3 class="section-title">实体列表</h3>
      <el-table :data="entities" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="type" label="类型">
          <template #default="{ row }">
            <el-tag :type="getEntityTypeTag(row.type)">{{ getEntityTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="viewEntity(row)">查看</el-button>
            <el-button size="small" type="primary" @click="viewRelations(row)">关系</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 实体详情对话框 -->
    <el-dialog v-model="entityDialogVisible" title="实体详情" width="600px">
      <div v-if="selectedEntity">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ selectedEntity.id }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ selectedEntity.name }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag :type="getEntityTypeTag(selectedEntity.type)">{{ getEntityTypeName(selectedEntity.type) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="来源">{{ selectedEntity.source }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ selectedEntity.description || '暂无描述' }}</el-descriptions-item>
        </el-descriptions>
        
        <h4 style="margin-top: 20px; margin-bottom: 10px;">相关关系</h4>
        <el-table :data="entityRelations" style="width: 100%">
          <el-table-column prop="target.name" label="关联实体" />
          <el-table-column prop="type" label="关系类型">
            <template #default="{ row }">
              <el-tag>{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { GraphChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import api from '@/api'

use([CanvasRenderer, GraphChart, TooltipComponent, LegendComponent])

interface LegalEntity {
  id: string
  name: string
  type: string
  description: string
  source: string
}

interface LegalRelation {
  id: string
  target: LegalEntity
  type: string
  description: string
  weight: number
}

const loading = ref(false)
const searchQuery = ref('')
const entityType = ref('')
const entities = ref<LegalEntity[]>([])
const selectedEntity = ref<LegalEntity | null>(null)
const entityRelations = ref<LegalRelation[]>([])
const entityDialogVisible = ref(false)
const graphContainer = ref<HTMLElement>()

const graphOption = ref({})

// 加载实体列表
async function loadEntities() {
  loading.value = true
  try {
    const params: any = { limit: 50 }
    if (searchQuery.value) params.name = searchQuery.value
    if (entityType.value) params.type = entityType.value
    
    const res = await api.get('/knowledge-graph/entities', { params })
    entities.value = res.data
    
    // 更新图谱可视化
    updateGraphVisualization()
  } catch (error) {
    console.error('加载实体失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索实体
function searchEntities() {
  loadEntities()
}

// 查看实体详情
async function viewEntity(entity: LegalEntity) {
  selectedEntity.value = entity
  entityDialogVisible.value = true
  
  // 加载实体关系
  try {
    const res = await api.get(`/knowledge-graph/entities/${entity.id}/relations`)
    entityRelations.value = res.data
  } catch (error) {
    console.error('加载实体关系失败:', error)
  }
}

// 查看实体关系
async function viewRelations(entity: LegalEntity) {
  selectedEntity.value = entity
  entityDialogVisible.value = true
  
  try {
    const res = await api.get(`/knowledge-graph/entities/${entity.id}/relations`)
    entityRelations.value = res.data
  } catch (error) {
    console.error('加载实体关系失败:', error)
  }
}

// 更新图谱可视化
function updateGraphVisualization() {
  const nodes = entities.value.map(entity => ({
    id: entity.id,
    name: entity.name,
    symbolSize: 50,
    category: getEntityCategory(entity.type),
    itemStyle: {
      color: getEntityColor(entity.type)
    }
  }))

  const links: any[] = []
  // 这里可以添加关系连线，需要从后端获取关系数据

  graphOption.value = {
    tooltip: {},
    legend: {
      data: ['法律法规', '法条', '案例', '法律概念']
    },
    series: [{
      type: 'graph',
      layout: 'force',
      data: nodes,
      links: links,
      categories: [
        { name: '法律法规' },
        { name: '法条' },
        { name: '案例' },
        { name: '法律概念' }
      ],
      roam: true,
      label: {
        show: true,
        position: 'right',
        formatter: '{b}'
      },
      lineStyle: {
        color: 'source',
        curveness: 0.3
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: {
          width: 10
        }
      },
      force: {
        repulsion: 100,
        gravity: 0.1,
        edgeLength: 150,
        layoutAnimation: true
      }
    }]
  }
}

// 获取实体类型标签
function getEntityTypeName(type: string) {
  const typeMap: Record<string, string> = {
    'LAW': '法律法规',
    'ARTICLE': '法条',
    'CASE': '案例',
    'CONCEPT': '法律概念',
    'LAWYER': '律师',
    'USER': '用户'
  }
  return typeMap[type] || type
}

// 获取实体类型标签样式
function getEntityTypeTag(type: string) {
  const tagMap: Record<string, string> = {
    'LAW': '',
    'ARTICLE': 'success',
    'CASE': 'warning',
    'CONCEPT': 'info'
  }
  return tagMap[type] || ''
}

// 获取实体分类
function getEntityCategory(type: string) {
  const categoryMap: Record<string, number> = {
    'LAW': 0,
    'ARTICLE': 1,
    'CASE': 2,
    'CONCEPT': 3
  }
  return categoryMap[type] || 0
}

// 获取实体颜色
function getEntityColor(type: string) {
  const colorMap: Record<string, string> = {
    'LAW': '#6366f1',
    'ARTICLE': '#22c55e',
    'CASE': '#f59e0b',
    'CONCEPT': '#3b82f6'
  }
  return colorMap[type] || '#9ca3af'
}

onMounted(loadEntities)
</script>

<style scoped>
.knowledge-graph-container {
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

.graph-section {
  margin-bottom: 32px;
}

.graph-container {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e7eb;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e5e7eb;
}

.entities-section {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e7eb;
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
}

html.dark .page-title,
html.dark .section-title { color: var(--text-primary); }
html.dark .graph-container,
html.dark .entities-section { background: var(--bg-card); border-color: var(--border); }
</style>