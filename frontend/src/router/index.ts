import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('@/views/ChatView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/contract',
      name: 'contract',
      component: () => import('@/views/ContractView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/laws',
      name: 'laws',
      component: () => import('@/views/LawSearchView.vue')
    },
    {
      path: '/faq',
      name: 'faq',
      component: () => import('@/views/FaqView.vue')
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/lawyers',
      name: 'lawyers',
      component: () => import('@/views/LawyerListView.vue')
    },
    {
      path: '/lawyers/:id',
      name: 'lawyerDetail',
      component: () => import('@/views/LawyerDetailView.vue')
    },
    {
      path: '/cases',
      name: 'cases',
      component: () => import('@/views/CaseSearchView.vue')
    },
    {
      path: '/cases/:id',
      name: 'caseDetail',
      component: () => import('@/views/CaseDetailView.vue')
    },
    {
      path: '/opinions',
      name: 'opinions',
      component: () => import('@/views/OpinionView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/documents',
      name: 'documents',
      component: () => import('@/views/DocumentView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('@/views/admin/DashboardView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true }
    },
    {
      path: '/admin/legacy',
      name: 'adminLegacy',
      component: () => import('@/views/AdminDashboardView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true }
    },
    {
      path: '/knowledge-graph',
      name: 'knowledgeGraph',
      component: () => import('@/views/KnowledgeGraphView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/teams',
      name: 'teams',
      component: () => import('@/views/team/TeamListView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/teams/:id',
      name: 'teamDetail',
      component: () => import('@/views/team/TeamDetailView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/tasks/board',
      name: 'taskBoard',
      component: () => import('@/views/collaboration/TaskBoardView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/messages',
      name: 'messages',
      component: () => import('@/views/MessageCenterView.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const role  = localStorage.getItem('role')
  if (to.meta.requiresAuth && !token) {
    next({ name: 'login', query: { redirect: to.fullPath } })
  } else if (to.meta.requiresAdmin && role !== 'ADMIN') {
    next({ name: 'home' })
  } else {
    next()
  }
})

export default router
