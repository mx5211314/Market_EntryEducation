import { createRouter, createWebHistory } from 'vue-router'
import ChatView from '../views/ChatView.vue'
import RiskTestView from '../views/RiskTestView.vue'
import SimTradeView from '../views/SimTradeView.vue'
import ReportView from '../views/ReportView.vue'
import LoginView from '../views/LoginView.vue'
import ProfileView from '../views/ProfileView.vue'
import AdminUserView from '../views/AdminUserView.vue'

const routes = [
  { path: '/login', component: LoginView },
  {
    path: '/',
    component: () => import('@/components/BackendLayout.vue'),
    children: [
      {
        path: 'chat',
        component: ChatView,
        meta: {
          title: '智能问答',
          icon: 'ChatDotRound',
          roles: ['USER', 'ADMIN'],
        },
      },
      {
        path: 'risk',
        component: RiskTestView,
        meta: { title: '风险测评', icon: 'PieChart', roles: ['USER', 'ADMIN'] },
      },
      {
        path: 'sim',
        component: SimTradeView,
        meta: { title: '模拟引导', icon: 'Monitor', roles: ['USER', 'ADMIN'] },
      },
      {
        path: 'report',
        component: ReportView,
        meta: { title: '风险报告', icon: 'Document', roles: ['USER', 'ADMIN'] },
      },
      {
        path: 'profile',
        component: ProfileView,
        meta: { title: '个人中心', icon: 'User', roles: ['USER', 'ADMIN'] },
      },
      {
        path: 'admin/users',
        component: AdminUserView,
        meta: { title: '用户管理', icon: 'Setting', roles: ['ADMIN'] },
      },
      { path: '', redirect: '/chat' },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  if (to.path === '/login' && token) {
    return next('/chat')
  }
  // 如果路由需要认证但未登录
  if (to.meta.requiresAuth && !token) {
    return next('/login')
  }
  // 如果路由需要管理员权限
  if (to.meta.requiresAdmin && role !== 'ADMIN') {
    return next('/chat')
  }
  // 检查角色权限（roles 数组）
  if (to.meta.roles && !to.meta.roles.includes(role)) {
    return next('/chat')
  }
  next()
})

export default router
