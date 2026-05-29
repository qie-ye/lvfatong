import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export const useKnowledgeGraphStore = defineStore('knowledgeGraph', () => {
  // 状态
  const loading = ref(false)
  const entities = ref<any[]>([])
  const selectedEntity = ref<any>(null)
  const entityRelations = ref<any[]>([])
  const searchQuery = ref('')
  const entityType = ref('')

  // 计算属性
  const entityCount = computed(() => entities.value.length)
  const relationCount = computed(() => entityRelations.value.length)

  // 加载实体列表
  async function loadEntities(params?: { name?: string; type?: string; limit?: number }) {
    loading.value = true
    try {
      const res = await api.get('/knowledge-graph/entities', { params: params || { limit: 50 } })
      entities.value = res.data
    } catch (error) {
      console.error('加载实体失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 搜索实体
  async function searchEntities(query: string, type?: string) {
    searchQuery.value = query
    entityType.value = type || ''
    await loadEntities({ name: query, type: type, limit: 50 })
  }

  // 获取实体详情
  async function getEntity(id: string) {
    try {
      const res = await api.get(`/knowledge-graph/entities/${id}`)
      selectedEntity.value = res.data
      return res.data
    } catch (error) {
      console.error('获取实体详情失败:', error)
      throw error
    }
  }

  // 获取实体关系
  async function getEntityRelations(id: string) {
    try {
      const res = await api.get(`/knowledge-graph/entities/${id}/relations`)
      entityRelations.value = res.data
      return res.data
    } catch (error) {
      console.error('获取实体关系失败:', error)
      throw error
    }
  }

  // 图谱搜索
  async function searchGraph(query: string, limit?: number) {
    loading.value = true
    try {
      const res = await api.get('/knowledge-graph/search', { params: { query, limit: limit || 20 } })
      return res.data
    } catch (error) {
      console.error('图谱搜索失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 图谱问答
  async function queryGraph(question: string) {
    loading.value = true
    try {
      const res = await api.post('/knowledge-graph/query', question)
      return res.data
    } catch (error) {
      console.error('图谱问答失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 获取可视化数据
  async function getVisualizationData(id: string) {
    try {
      const res = await api.get(`/knowledge-graph/visualize/${id}`)
      return res.data
    } catch (error) {
      console.error('获取可视化数据失败:', error)
      throw error
    }
  }

  // 同步知识文档
  async function syncDocuments() {
    loading.value = true
    try {
      await api.post('/knowledge-graph/sync')
    } catch (error) {
      console.error('同步知识文档失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 重建知识图谱
  async function rebuildGraph() {
    loading.value = true
    try {
      await api.post('/knowledge-graph/rebuild')
    } catch (error) {
      console.error('重建知识图谱失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 设置选中的实体
  function setSelectedEntity(entity: any) {
    selectedEntity.value = entity
  }

  // 清空选中的实体
  function clearSelectedEntity() {
    selectedEntity.value = null
    entityRelations.value = []
  }

  return {
    // 状态
    loading,
    entities,
    selectedEntity,
    entityRelations,
    searchQuery,
    entityType,
    
    // 计算属性
    entityCount,
    relationCount,
    
    // 方法
    loadEntities,
    searchEntities,
    getEntity,
    getEntityRelations,
    searchGraph,
    queryGraph,
    getVisualizationData,
    syncDocuments,
    rebuildGraph,
    setSelectedEntity,
    clearSelectedEntity
  }
})