import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

interface AuthState {
  accessToken: string
  refreshToken: string
  username: string
  role: string
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isLawyer = computed(() => role.value === 'LAWYER' || role.value === 'ADMIN')

  async function login(user: string, password: string) {
    const res = await api.post('/auth/login', { username: user, password })
    const data = res.data as AuthState
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    username.value = data.username
    role.value = data.role
    localStorage.setItem('token', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
  }

  async function register(user: string, password: string, nickname?: string) {
    const res = await api.post('/auth/register', { username: user, password, nickname })
    const data = res.data as AuthState
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    username.value = data.username
    role.value = data.role
    localStorage.setItem('token', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
  }

  async function refresh() {
    const res = await api.post('/auth/refresh', { refreshToken: refreshToken.value })
    const data = res.data as AuthState
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    localStorage.setItem('token', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
  }

  function logout() {
    token.value = ''
    refreshToken.value = ''
    username.value = ''
    role.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
  }

  return { token, refreshToken, username, role, isLoggedIn, isAdmin, isLawyer, login, register, refresh, logout }
})
