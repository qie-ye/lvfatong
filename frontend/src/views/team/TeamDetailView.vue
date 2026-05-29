<template>
  <div class="team-detail-container" v-loading="loading">
    <!-- 团队头部信息 -->
    <div class="team-header">
      <div class="team-info">
        <el-avatar :size="80" :src="team.logoUrl">
          {{ team.name?.charAt(0) }}
        </el-avatar>
        <div class="team-text">
          <h2 class="team-name">{{ team.name }}</h2>
          <p class="team-desc">{{ team.description || '暂无描述' }}</p>
          <div class="team-meta">
            <span>邀请码: <strong>{{ team.inviteCode }}</strong></span>
            <el-button size="small" text type="primary" @click="copyInviteCode">复制</el-button>
          </div>
        </div>
      </div>
      <div class="team-actions">
        <el-button @click="showInviteDialog = true" v-if="isOwnerOrAdmin">
          <el-icon><Plus /></el-icon>
          邀请成员
        </el-button>
        <el-button @click="refreshInviteCode" v-if="isOwner">
          <el-icon><Refresh /></el-icon>
          刷新邀请码
        </el-button>
        <el-dropdown v-if="isOwner" @command="handleCommand">
          <el-button>
            <el-icon><More /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="edit">编辑团队</el-dropdown-item>
              <el-dropdown-item command="disband" divided>解散团队</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button v-if="!isOwner" type="danger" @click="handleLeaveTeam">退出团队</el-button>
      </div>
    </div>

    <!-- 成员列表 -->
    <div class="members-section">
      <h3 class="section-title">团队成员 ({{ members.length }})</h3>
      <div class="members-list">
        <div v-for="member in members" :key="member.memberId" class="member-item">
          <div class="member-info">
            <el-avatar :size="40">{{ member.nickname?.charAt(0) || member.username?.charAt(0) }}</el-avatar>
            <div class="member-text">
              <div class="member-name">{{ member.nickname || member.username }}</div>
              <div class="member-email">{{ member.email || member.phone }}</div>
            </div>
          </div>
          <div class="member-actions">
            <el-tag :type="getRoleTagType(member.role)" size="small">{{ getRoleName(member.role) }}</el-tag>
            <el-dropdown v-if="isOwner && member.userId !== currentUserId" @command="(cmd: string) => handleMemberCommand(cmd, member)">
              <el-button size="small" text>
                <el-icon><More /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="admin" v-if="member.role === 'MEMBER'">设为管理员</el-dropdown-item>
                  <el-dropdown-item command="member" v-if="member.role === 'ADMIN'">设为成员</el-dropdown-item>
                  <el-dropdown-item command="remove" divided>移除成员</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
    </div>

    <!-- 邀请成员对话框 -->
    <el-dialog v-model="showInviteDialog" title="邀请成员" width="500px">
      <el-form :model="inviteForm" label-width="80px">
        <el-form-item label="手机号" required>
          <el-input v-model="inviteForm.phone" placeholder="请输入被邀请人的手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showInviteDialog = false">取消</el-button>
        <el-button type="primary" @click="handleInvite" :loading="inviting">邀请</el-button>
      </template>
    </el-dialog>

    <!-- 编辑团队对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑团队" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="团队名称" required>
          <el-input v-model="editForm.name" placeholder="请输入团队名称" />
        </el-form-item>
        <el-form-item label="团队描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入团队描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleEditTeam" :loading="editing">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus, Refresh, More } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const route = useRoute()
const router = useRouter()
const teamId = Number(route.params.id)
const currentUserId = ref(1) // 临时硬编码

const loading = ref(false)
const inviting = ref(false)
const editing = ref(false)
const showInviteDialog = ref(false)
const showEditDialog = ref(false)
const team = ref<any>({})
const members = ref<any[]>([])

const inviteForm = ref({ phone: '' })
const editForm = ref({ name: '', description: '' })

const isOwner = computed(() => team.value.ownerId === currentUserId.value)
const isOwnerOrAdmin = computed(() => {
  if (isOwner.value) return true
  const currentMember = members.value.find(m => m.userId === currentUserId.value)
  return currentMember?.role === 'ADMIN'
})

// 加载团队详情
async function loadTeam() {
  loading.value = true
  try {
    const res = await api.get(`/teams/${teamId}`)
    team.value = res.data
    editForm.value = { name: team.value.name, description: team.value.description }
  } catch (error) {
    console.error('加载团队详情失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载成员列表
async function loadMembers() {
  try {
    const res = await api.get(`/teams/${teamId}/members`)
    members.value = res.data
  } catch (error) {
    console.error('加载成员列表失败:', error)
  }
}

// 复制邀请码
function copyInviteCode() {
  navigator.clipboard.writeText(team.value.inviteCode)
  ElMessage.success('邀请码已复制')
}

// 刷新邀请码
async function refreshInviteCode() {
  try {
    const res = await api.post(`/teams/${teamId}/refresh-invite-code`)
    team.value.inviteCode = res.data.inviteCode
    ElMessage.success('邀请码已刷新')
  } catch (error: any) {
    ElMessage.error(error.message || '刷新失败')
  }
}

// 邀请成员
async function handleInvite() {
  if (!inviteForm.value.phone) {
    ElMessage.warning('请输入手机号')
    return
  }

  inviting.value = true
  try {
    await api.post(`/teams/${teamId}/invite`, inviteForm.value)
    ElMessage.success('邀请已发送')
    showInviteDialog.value = false
    inviteForm.value = { phone: '' }
  } catch (error: any) {
    ElMessage.error(error.message || '邀请失败')
  } finally {
    inviting.value = false
  }
}

// 编辑团队
async function handleEditTeam() {
  if (!editForm.value.name) {
    ElMessage.warning('请输入团队名称')
    return
  }

  editing.value = true
  try {
    await api.put(`/teams/${teamId}`, editForm.value)
    ElMessage.success('团队信息已更新')
    showEditDialog.value = false
    loadTeam()
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    editing.value = false
  }
}

// 解散团队
async function handleDisband() {
  try {
    await ElMessageBox.confirm('确定要解散团队吗？此操作不可恢复。', '确认解散', {
      type: 'warning'
    })
    await api.delete(`/teams/${teamId}`)
    ElMessage.success('团队已解散')
    router.push('/teams')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '解散失败')
    }
  }
}

// 退出团队
async function handleLeaveTeam() {
  try {
    await ElMessageBox.confirm('确定要退出团队吗？', '确认退出', {
      type: 'warning'
    })
    await api.post(`/teams/${teamId}/leave`)
    ElMessage.success('已退出团队')
    router.push('/teams')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '退出失败')
    }
  }
}

// 修改成员角色
async function handleMemberCommand(command: string, member: any) {
  if (command === 'remove') {
    try {
      await ElMessageBox.confirm(`确定要移除成员 ${member.nickname || member.username} 吗？`, '确认移除', {
        type: 'warning'
      })
      await api.delete(`/teams/${teamId}/members/${member.userId}`)
      ElMessage.success('成员已移除')
      loadMembers()
    } catch (error: any) {
      if (error !== 'cancel') {
        ElMessage.error(error.message || '移除失败')
      }
    }
  } else if (command === 'admin' || command === 'member') {
    const newRole = command === 'admin' ? 'ADMIN' : 'MEMBER'
    try {
      await api.put(`/teams/${teamId}/members/${member.userId}/role`, { role: newRole })
      ElMessage.success('角色已更新')
      loadMembers()
    } catch (error: any) {
      ElMessage.error(error.message || '更新失败')
    }
  }
}

// 下拉菜单命令
function handleCommand(command: string) {
  if (command === 'edit') {
    showEditDialog.value = true
  } else if (command === 'disband') {
    handleDisband()
  }
}

// 获取角色标签类型
function getRoleTagType(role: string) {
  const map: Record<string, string> = {
    'OWNER': 'danger',
    'ADMIN': 'warning',
    'MEMBER': ''
  }
  return map[role] || ''
}

// 获取角色名称
function getRoleName(role: string) {
  const map: Record<string, string> = {
    'OWNER': '所有者',
    'ADMIN': '管理员',
    'MEMBER': '成员'
  }
  return map[role] || role
}

onMounted(() => {
  loadTeam()
  loadMembers()
})
</script>

<style scoped>
.team-detail-container {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.team-header {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.team-info {
  display: flex;
  gap: 20px;
}

.team-text {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.team-name {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
}

.team-desc {
  font-size: 14px;
  color: #6b7280;
}

.team-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #9ca3af;
}

.team-actions {
  display: flex;
  gap: 12px;
}

.members-section {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 20px;
}

.members-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.member-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  background: #f9fafb;
}

.member-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.member-text {
  display: flex;
  flex-direction: column;
}

.member-name {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
}

.member-email {
  font-size: 12px;
  color: #9ca3af;
}

.member-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

@media (max-width: 768px) {
  .team-header {
    flex-direction: column;
    gap: 16px;
  }

  .team-actions {
    width: 100%;
    justify-content: flex-end;
  }
}

html.dark .team-header,
html.dark .members-section { background: var(--bg-card); }
html.dark .team-name,
html.dark .section-title,
html.dark .member-name { color: var(--text-primary); }
html.dark .member-item { background: var(--bg-secondary); }
</style>