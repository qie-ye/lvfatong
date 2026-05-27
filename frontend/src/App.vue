<template>
  <el-container class="app-container">
    <el-header class="app-header" height="52px">
      <div class="header-left">
        <router-link to="/" class="logo">
          <span>律法通</span>
        </router-link>
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
          <el-dropdown trigger="click" @command="handleUserCommand" class="user-dropdown">
            <span class="user-trigger">
              <el-avatar :size="26" :style="{ background: 'var(--color-primary-600)', fontSize: '11px' }">
                {{ (authStore.username || 'U')[0].toUpperCase() }}
              </el-avatar>
              <span class="user-name">{{ authStore.username }}</span>
              <el-icon :size="12"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile"><el-icon><User /></el-icon>个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button size="small" class="btn-login" @click="$router.push('/login')">登录</el-button>
          <el-button type="primary" size="small" @click="$router.push('/login')">注册</el-button>
        </template>
        <button class="theme-toggle" @click="toggleDark" :title="isDark ? '切换亮色模式' : '切换暗色模式'">
          <el-icon :size="16"><component :is="isDark ? Sunny : Moon" /></el-icon>
        </button>
        <div class="hamburger" @click="mobileMenuOpen = !mobileMenuOpen">
          <el-icon :size="20"><component :is="mobileMenuOpen ? Close : Operation" /></el-icon>
        </div>
      </div>
    </el-header>

    <transition name="slide">
      <div v-if="mobileMenuOpen" class="mobile-menu">
        <router-link v-for="item in navItems" :key="item.path" :to="item.path" class="mobile-link" :class="{ active: isActive(item.path) }" @click="mobileMenuOpen = false">
          <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>{{ item.label }}
        </router-link>
      </div>
    </transition>

    <el-main>
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in"><component :is="Component" /></transition>
      </router-view>
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
  ArrowDown, SwitchButton, Close, Operation, DataBoard,
  Sunny, Moon
} from '@element-plus/icons-vue'

const User = UserIcon
const authStore = useAuthStore()
const notifStore = useNotificationStore()
const route = useRoute()
const router = useRouter()
const mobileMenuOpen = ref(false)

const isDark = ref(false)
const toggleDark = () => {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('lft-theme', isDark.value ? 'dark' : 'light')
}

onMounted(() => {
  const saved = localStorage.getItem('lft-theme')
  if (saved === 'dark' || (!saved && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    isDark.value = true
    document.documentElement.classList.add('dark')
  }
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

function isActive(path: string) { return route.path === path }
function handleUserCommand(command: string) {
  if (command === 'profile') { router.push('/profile') }
  else if (command === 'logout') { notifStore.disconnect(); authStore.logout(); router.push('/') }
}
</script>

<style>
/* ======================================
   律法通 Design Token System
   参考法大大: 明亮干净 · 蓝调阴影 · 克制专业
   ====================================== */
:root {
  --color-primary-50: #eff6ff;
  --color-primary-100: #dbeafe;
  --color-primary-200: #bfdbfe;
  --color-primary-300: #93c5fd;
  --color-primary-400: #60a5fa;
  --color-primary-500: #3b82f6;
  --color-primary-600: #2563eb;
  --color-primary-700: #1d4ed8;
  --color-primary-800: #1e40af;
  --color-primary-900: #1e3a8a;

  --color-success: #22c55e;
  --color-warning: #f59e0b;
  --color-danger: #ef4444;

  --gray-50: #f9fafb;
  --gray-100: #f3f4f6;
  --gray-200: #e5e7eb;
  --gray-300: #d1d5db;
  --gray-400: #9ca3af;
  --gray-500: #6b7280;
  --gray-600: #4b5563;
  --gray-700: #374151;
  --gray-800: #1f2937;
  --gray-900: #111827;

  --bg: #f8fafc;
  --bg-card: #ffffff;
  --bg-elevated: #f1f5f9;

  --border: #e5e7eb;
  --border-light: #f3f4f6;

  --text-primary: #111827;
  --text-secondary: #4b5563;
  --text-tertiary: #9ca3af;

  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;

  --shadow-xs: 0 1px 2px rgba(37, 99, 235, 0.04);
  --shadow-sm: 0 1px 4px rgba(37, 99, 235, 0.06);
  --shadow-md: 0 4px 16px rgba(37, 99, 235, 0.08);
  --shadow-lg: 0 8px 32px rgba(37, 99, 235, 0.1);

  --transition-fast: 0.15s ease;
  --transition-base: 0.25s ease;
  --header-height: 52px;
}

/* ======================================
   暗色模式
   ====================================== */
html.dark {
  --bg: #0f172a;
  --bg-card: #1a2744;
  --bg-elevated: #1e3056;

  --border: rgba(255, 255, 255, 0.08);
  --border-light: rgba(255, 255, 255, 0.05);

  --text-primary: #ffffff;
  --text-secondary: #e2e8f0;
  --text-tertiary: #94a3b8;

  --shadow-xs: 0 1px 2px rgba(0, 0, 0, 0.3);
  --shadow-sm: 0 1px 4px rgba(0, 0, 0, 0.4);
  --shadow-md: 0 4px 16px rgba(0, 0, 0, 0.45);
  --shadow-lg: 0 8px 32px rgba(0, 0, 0, 0.5);

  --gray-50: #0f172a;
  --gray-100: #1a2744;
  --gray-200: #334155;
  --gray-300: #475569;
  --gray-400: #64748b;
  --gray-500: #94a3b8;
  --gray-600: #cbd5e1;
  --gray-700: #e2e8f0;
  --gray-800: #f1f5f9;
  --gray-900: #f8fafc;

  color-scheme: dark;
}

* { margin: 0; padding: 0; box-sizing: border-box; }

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: var(--bg);
  color: var(--text-primary);
  -webkit-font-smoothing: antialiased;
  font-size: 14px;
  line-height: 1.6;
}

::-webkit-scrollbar { width: 5px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: var(--gray-300); border-radius: 3px; }

.app-container { min-height: 100vh; }

.app-header {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border-bottom: 1px solid #e5e7eb;
  padding: 0 24px; height: 52px; position: sticky; top: 0; z-index: 100;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.header-left { display: flex; align-items: center; gap: 32px; }

.logo {
  text-decoration: none; font-size: 18px; font-weight: 700;
  color: var(--color-primary-600); letter-spacing: 2px;
}

.nav-desktop { display: flex; gap: 4px; }

.nav-link {
  display: flex; align-items: center; gap: 4px;
  color: var(--gray-500); text-decoration: none;
  font-size: 13px; padding: 6px 12px; border-radius: 6px;
  transition: all var(--transition-fast); white-space: nowrap;
}

.nav-link:hover { color: var(--color-primary-600); background: var(--color-primary-50); }

.nav-link.active {
  color: var(--color-primary-600); font-weight: 500;
  background: var(--color-primary-50);
}

.header-right { display: flex; align-items: center; gap: 10px; }
.user-dropdown { display: flex; }

.user-trigger {
  display: flex; align-items: center; gap: 6px; cursor: pointer;
  color: var(--text-secondary); font-size: 13px; padding: 4px 8px;
  border-radius: 6px; transition: all var(--transition-fast);
}

.user-trigger:hover { background: var(--gray-100); }
.user-name { max-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.btn-login { border-color: #e5e7eb; color: var(--gray-600); }

.theme-toggle {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; border: 1px solid var(--border);
  border-radius: 6px; background: transparent; cursor: pointer;
  color: var(--gray-500); transition: all var(--transition-fast);
}

.theme-toggle:hover { color: var(--color-primary-600); border-color: var(--color-primary-300); }

.hamburger { display: none; cursor: pointer; color: var(--gray-500); }

.mobile-menu {
  position: fixed; top: 52px; left: 0; right: 0;
  background: #fff; padding: 8px 16px; z-index: 99;
  border-bottom: 1px solid var(--border); box-shadow: var(--shadow-sm);
}

.mobile-link {
  display: flex; align-items: center; gap: 8px;
  color: var(--text-secondary); text-decoration: none;
  font-size: 14px; padding: 10px 8px;
  border-bottom: 1px solid var(--border-light); transition: color var(--transition-fast);
}
.mobile-link:hover, .mobile-link.active { color: var(--color-primary-600); }
.mobile-link:last-child { border-bottom: none; }

.slide-enter-active, .slide-leave-active { transition: all 0.2s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(-6px); }
.page-fade-enter-active, .page-fade-leave-active { transition: opacity 0.15s ease; }
.page-fade-enter-from, .page-fade-leave-to { opacity: 0; }
.el-main { padding: 0; }

@media (max-width: 900px) { .nav-desktop { display: none; } .hamburger { display: flex; } }
@media (min-width: 901px) { .mobile-menu { display: none !important; } }

/* 暗色模式全局覆盖 */
html.dark .app-header {
  background: var(--gray-950); border-bottom-color: var(--border);
  box-shadow: 0 1px 3px rgba(0,0,0,0.3);
}

html.dark .logo { color: #e2e8f0; }

html.dark .nav-link { color: var(--text-secondary); }
html.dark .nav-link:hover { color: var(--color-primary-300); background: rgba(59, 130, 246, 0.08); }
html.dark .nav-link.active { color: #fff; background: rgba(59, 130, 246, 0.12); }

html.dark .user-trigger:hover { background: rgba(255,255,255,0.06); }

html.dark .mobile-menu { background: var(--gray-850); border-bottom-color: var(--border); box-shadow: 0 4px 12px rgba(0,0,0,0.3); }
html.dark .mobile-link:hover, html.dark .mobile-link.active { color: var(--color-primary-300); }
</style>
