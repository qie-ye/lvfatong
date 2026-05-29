<template>
  <div class="team-list-container">
    <div class="page-header">
      <h2 class="page-title">我的团队</h2>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        创建团队
      </el-button>
    </div>

    <!-- 团队列表 -->
    <div class="team-grid" v-loading="loading">
      <div v-for="team in teams" :key="team.id" class="team-card" @click="goToTeam(team.id)">
        <div class="team-avatar">
          <el-avatar :size="60" :src="team.logoUrl">
            {{ team.name?.charAt(0) }}
          </el-avatar>
        </div>
        <div class="team-info">
          <h3 class="team-name">{{ team.name }}</h3>
          <p class="team-desc">{{ team.description || '暂无描述' }}</p>
          <div class="team-meta">
            <span class="member-count">
              <el-icon><User /></el-icon>
              {{ team.memberCount || 0 }} 成员
            </span>
            <span class="team-role" v-if="team.ownerId === currentUserId">
              <el-tag size="small" type="warning">创建者</el-tag>
            </span>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && teams.length === 0" class="empty-state">
        <el-empty description="暂无团队">
          <el-button type="primary" @click="showCreateDialog = true">创建团队</el-button>
        </el-empty>
      </div>
    </div>

    <!-- 创建团队对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建团队" width="500px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="团队名称" required>
          <el-input v-model="createForm.name" placeholder="请输入团队名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="团队描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="请输入团队描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateTeam" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <!-- 加入团队对话框 -->
    <el-dialog v-model="showJoinDialog" title="加入团队" width="500px">
      <el-form :model="joinForm" label-width="80px">
        <el-form-item label="邀请码" required>
          <el-input v-model="joinForm.inviteCode" placeholder="请输入邀请码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showJoinDialog = false">取消</el-button>
        <el-button type="primary" @click="handleJoinTeam" :loading="joining">加入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/api'

const router = useRouter()
const loading = ref(false)
const creating = ref(false)
const joining = ref(false)
const showCreateDialog = ref(false)
const showJoinDialog = ref(false)
const teams = ref<any[]>([])
const currentUserId = ref(1) // 临时硬编码

const createForm = ref({
  name: '',
  description: ''
})

const joinForm = ref({
  inviteCode: ''
})

// 加载团队列表
async function loadTeams() {
  loading.value = true
  try {
    const res = await api.get('/teams')
    teams.value = res.data
  } catch (error) {
    console.error('加载团队列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 创建团队
async function handleCreateTeam() {
  if (!createForm.value.name) {
    ElMessage.warning('请输入团队名称')
    return
  }

  creating.value = true
  try {
    await api.post('/teams', createForm.value)
    ElMessage.success('团队创建成功')
    showCreateDialog.value = false
    createForm.value = { name: '', description: '' }
    loadTeams()
  } catch (error: any) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    creating.value = false
  }
}

// 加入团队
async function handleJoinTeam() {
  if (!joinForm.value.inviteCode) {
    ElMessage.warning('请输入邀请码')
    return
  }

  joining.value = true
  try {
    await api.post(`/teams/join/${joinForm.value.inviteCode}`)
    ElMessage.success('加入团队成功')
    showJoinDialog.value = false
    joinForm.value = { inviteCode: '' }
    loadTeams()
  } catch (error: any) {
    ElMessage.error(error.message || '加入失败')
  } finally {
    joining.value = false
  }
}

// 跳转到团队详情
function goToTeam(teamId: number) {
  router.push(`/teams/${teamId}`)
}

onMounted(loadTeams)
</script>

<style scoped>
.team-list-container {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.team-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e7eb;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  gap: 16px;
}

.team-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.team-avatar {
  flex-shrink: 0;
}

.team-info {
  flex: 1;
  min-width: 0;
}

.team-name {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
}

.team-desc {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #9ca3af;
}

.member-count {
  display: flex;
  align-items: center;
  gap: 4px;
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

@media (max-width: 768px) {
  .team-grid {
    grid-template-columns: 1fr;
  }
}

html.dark .page-title { color: var(--text-primary); }
html.dark .team-card { background: var(--bg-card); border-color: var(--border); }
html.dark .team-name { color: var(--text-primary); }
</style>