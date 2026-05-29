import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export const useTeamStore = defineStore('team', () => {
  // 状态
  const loading = ref(false)
  const teams = ref<any[]>([])
  const currentTeam = ref<any>(null)
  const members = ref<any[]>([])
  const invitations = ref<any[]>([])

  // 计算属性
  const teamCount = computed(() => teams.value.length)
  const memberCount = computed(() => members.value.length)

  // 加载团队列表
  async function loadTeams() {
    loading.value = true
    try {
      const res = await api.get('/teams')
      teams.value = res.data
    } catch (error) {
      console.error('加载团队列表失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 加载团队详情
  async function loadTeam(teamId: number) {
    loading.value = true
    try {
      const res = await api.get(`/teams/${teamId}`)
      currentTeam.value = res.data
      return res.data
    } catch (error) {
      console.error('加载团队详情失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 创建团队
  async function createTeam(name: string, description?: string) {
    loading.value = true
    try {
      const res = await api.post('/teams', { name, description })
      teams.value.push(res.data)
      return res.data
    } catch (error) {
      console.error('创建团队失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 更新团队
  async function updateTeam(teamId: number, name: string, description?: string) {
    try {
      const res = await api.put(`/teams/${teamId}`, { name, description })
      const index = teams.value.findIndex(t => t.id === teamId)
      if (index !== -1) {
        teams.value[index] = res.data
      }
      if (currentTeam.value?.id === teamId) {
        currentTeam.value = res.data
      }
      return res.data
    } catch (error) {
      console.error('更新团队失败:', error)
      throw error
    }
  }

  // 解散团队
  async function disbandTeam(teamId: number) {
    try {
      await api.delete(`/teams/${teamId}`)
      teams.value = teams.value.filter(t => t.id !== teamId)
      if (currentTeam.value?.id === teamId) {
        currentTeam.value = null
      }
    } catch (error) {
      console.error('解散团队失败:', error)
      throw error
    }
  }

  // 加载成员列表
  async function loadMembers(teamId: number) {
    try {
      const res = await api.get(`/teams/${teamId}/members`)
      members.value = res.data
      return res.data
    } catch (error) {
      console.error('加载成员列表失败:', error)
      throw error
    }
  }

  // 邀请成员
  async function inviteMember(teamId: number, phone: string) {
    try {
      const res = await api.post(`/teams/${teamId}/invite`, { phone })
      return res.data
    } catch (error) {
      console.error('邀请成员失败:', error)
      throw error
    }
  }

  // 移除成员
  async function removeMember(teamId: number, userId: number) {
    try {
      await api.delete(`/teams/${teamId}/members/${userId}`)
      members.value = members.value.filter(m => m.userId !== userId)
    } catch (error) {
      console.error('移除成员失败:', error)
      throw error
    }
  }

  // 修改成员角色
  async function updateMemberRole(teamId: number, userId: number, role: string) {
    try {
      await api.put(`/teams/${teamId}/members/${userId}/role`, { role })
      const member = members.value.find(m => m.userId === userId)
      if (member) {
        member.role = role
      }
    } catch (error) {
      console.error('修改角色失败:', error)
      throw error
    }
  }

  // 通过邀请码加入团队
  async function joinByInviteCode(inviteCode: string) {
    try {
      const res = await api.post(`/teams/join/${inviteCode}`)
      teams.value.push(res.data)
      return res.data
    } catch (error) {
      console.error('加入团队失败:', error)
      throw error
    }
  }

  // 退出团队
  async function leaveTeam(teamId: number) {
    try {
      await api.post(`/teams/${teamId}/leave`)
      teams.value = teams.value.filter(t => t.id !== teamId)
      if (currentTeam.value?.id === teamId) {
        currentTeam.value = null
      }
    } catch (error) {
      console.error('退出团队失败:', error)
      throw error
    }
  }

  // 加载邀请列表
  async function loadInvitations(teamId: number) {
    try {
      const res = await api.get(`/teams/${teamId}/invitations`)
      invitations.value = res.data
      return res.data
    } catch (error) {
      console.error('加载邀请列表失败:', error)
      throw error
    }
  }

  // 加载我的邀请
  async function loadMyInvitations() {
    try {
      const res = await api.get('/teams/invitations/my')
      return res.data
    } catch (error) {
      console.error('加载我的邀请失败:', error)
      throw error
    }
  }

  // 接受邀请
  async function acceptInvitation(invitationId: number) {
    try {
      const res = await api.post(`/teams/invitations/${invitationId}/accept`)
      teams.value.push(res.data)
      return res.data
    } catch (error) {
      console.error('接受邀请失败:', error)
      throw error
    }
  }

  // 拒绝邀请
  async function rejectInvitation(invitationId: number) {
    try {
      await api.post(`/teams/invitations/${invitationId}/reject`)
    } catch (error) {
      console.error('拒绝邀请失败:', error)
      throw error
    }
  }

  // 刷新邀请码
  async function refreshInviteCode(teamId: number) {
    try {
      const res = await api.post(`/teams/${teamId}/refresh-invite-code`)
      if (currentTeam.value?.id === teamId) {
        currentTeam.value.inviteCode = res.data.inviteCode
      }
      return res.data.inviteCode
    } catch (error) {
      console.error('刷新邀请码失败:', error)
      throw error
    }
  }

  return {
    // 状态
    loading,
    teams,
    currentTeam,
    members,
    invitations,
    
    // 计算属性
    teamCount,
    memberCount,
    
    // 方法
    loadTeams,
    loadTeam,
    createTeam,
    updateTeam,
    disbandTeam,
    loadMembers,
    inviteMember,
    removeMember,
    updateMemberRole,
    joinByInviteCode,
    leaveTeam,
    loadInvitations,
    loadMyInvitations,
    acceptInvitation,
    rejectInvitation,
    refreshInviteCode
  }
})