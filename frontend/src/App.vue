<template>
  <el-container class="app-container">
    <el-header class="app-header" height="56px">
      <div class="header-left">
        <router-link to="/" class="logo">
          <span class="logo-icon">⚖</span>
          <span class="logo-text">律法通</span>
        </router-link>
        <!-- Desktop nav -->
        <nav class="nav-desktop">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: isActive(item.path) }"
          >
            <el-icon v-if="item.icon" :size="14"><component :is="item.icon" /></el-icon>
            {{ item.label }}
          </router-link>
        </nav>
      </div>

      <div class="header-right">
        <template v-if="authStore.isLoggedIn">
          <NotificationDropdown />
          <el-dropdown trigger="click" @command="handleUserCommand">
            <span class="user-trigger">
              <el-avatar :size="28" :style="{ background: '#4fc3f7', fontSize: '12px', cursor: 'pointer' }">
                {{ (authStore.username || 'U')[0].toUpperCase() }}
              </el-avatar>
              <span class="user-name">{{ authStore.username }}</span>
              <el-icon :size="12"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" size="small" round @click="$router.push('/login')">登录</el-button>
        </template>

        <!-- Mobile hamburger -->
        <div class="hamburger" @click="mobileMenuOpen = !mobileMenuOpen">
          <el-icon :size="22"><component :is="mobileMenuOpen ? Close : Operation" /></el-icon>
        </div>
      </div>
    </el-header>

    <!-- Mobile menu overlay -->
    <transition name="slide">
      <div v-if="mobileMenuOpen" class="mobile-menu">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="mobile-link"
          :class="{ active: isActive(item.path) }"
          @click="mobileMenuOpen = false"
        >
          <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
          {{ item.label }}
        </router-link>
      </div>
    </transition>

    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import NotificationDropdown from '@/components/NotificationDropdown.vue'
import {
  ChatDotRound, Document, Search, ScaleToOriginal,
  EditPen, Reading, User as UserIcon, QuestionFilled,
  ArrowDown, SwitchButton, Close, Operation, DataBoard
} from '@element-plus/icons-vue'

// Alias to avoid conflict with HTML <User>
const User = UserIcon

const authStore = useAuthStore()
const notifStore = useNotificationStore()
const route = useRoute()
const router = useRouter()
const mobileMenuOpen = ref(false)

onMounted(() => {
  if (authStore.isLoggedIn && authStore.token) {
    notifStore.loadAll()
    notifStore.connect(authStore.token)
  }
})

const navItems = computed(() => [
  { path: '/chat', label: '法律咨询', icon: ChatDotRound },
  { path: '/contract', label: '合同分析', icon: Document },
  { path: '/laws', label: '法条查询', icon: Search },
  { path: '/opinions', label: '法律意见', icon: ScaleToOriginal },
  { path: '/documents', label: '文书生成', icon: EditPen },
  { path: '/cases', label: '案例检索', icon: Reading },
  { path: '/lawyers', label: '律师服务', icon: UserIcon },
  { path: '/faq', label: '常见问题', icon: QuestionFilled },
  ...(authStore.isAdmin ? [{ path: '/admin', label: '管理后台', icon: DataBoard }] : [])
])

function isActive(path: string) {
  return route.path === path
}

function handleUserCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    notifStore.disconnect()
    authStore.logout()
    router.push('/')
  }
}
</script>

<style>
:root {
  --primary: #4fc3f7;
  --primary-dark: #1a1a2e;
  --bg: #f5f7fa;
  --radius: 8px;
  --shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  --header-height: 56px;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background-color: var(--bg);
  color: #333;
}

.app-container {
  min-height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--primary-dark);
  color: #fff;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 6px;
  text-decoration: none;
  margin-right: 12px;
  flex-shrink: 0;
}

.logo-icon {
  font-size: 22px;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--primary);
  letter-spacing: 1px;
}

/* Desktop nav */
.nav-desktop {
  display: flex;
  gap: 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: rgba(255, 255, 255, 0.65);
  text-decoration: none;
  font-size: 13px;
  padding: 6px 10px;
  border-radius: 6px;
  transition: all 0.2s;
  white-space: nowrap;
}

.nav-link:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.nav-link.active {
  color: var(--primary);
  background: rgba(79, 195, 247, 0.12);
  font-weight: 500;
}

/* Header right */
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
}

.user-name {
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hamburger {
  display: none;
  cursor: pointer;
  color: #fff;
  margin-left: 8px;
}

/* Mobile menu */
.mobile-menu {
  position: fixed;
  top: var(--header-height);
  left: 0;
  right: 0;
  background: var(--primary-dark);
  padding: 12px 20px;
  z-index: 99;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.mobile-link {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  font-size: 14px;
  padding: 10px 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  transition: color 0.2s;
}

.mobile-link:last-child {
  border-bottom: none;
}

.mobile-link:hover,
.mobile-link.active {
  color: var(--primary);
}

/* Slide transition */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.25s ease;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* Responsive */
@media (max-width: 900px) {
  .nav-desktop {
    display: none;
  }
  .hamburger {
    display: flex;
  }
}

@media (min-width: 901px) {
  .mobile-menu {
    display: none !important;
  }
}
</style>
