import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'

const BackendLayout = () => import('../components/BackendLayout.vue')
const ChatView = () => import('../views/ChatView.vue')
const RiskTestView = () => import('../views/RiskTestView.vue')
const SimTradeView = () => import('../views/SimTradeView.vue')
const ReportView = () => import('../views/ReportView.vue')
const ProfileView = () => import('../views/ProfileView.vue')
const AdminUserView = () => import('../views/AdminUserView.vue')

const routes = [
  {
    path: '/login',
    component: LoginView,
    meta: { guest: true }, // 允许未登录访问，已登录也不强制跳转
  },
  {
    path: '/',
    component: BackendLayout,
    redirect: '/chat',
    children: [
      {
        path: 'chat',
        component: ChatView,
        meta: { title: '智能问答', roles: ['USER', 'ADMIN'] },
      },
      {
        path: 'risk',
        component: RiskTestView,
        meta: { title: '风险测评', roles: ['USER', 'ADMIN'] },
      },
      {
        path: 'sim',
        component: SimTradeView,
        meta: { title: '模拟引导', roles: ['USER', 'ADMIN'] },
      },
      {
        path: 'report',
        component: ReportView,
        meta: { title: '风险报告', roles: ['USER', 'ADMIN'] },
      },
      {
        path: 'profile',
        component: ProfileView,
        meta: { title: '个人中心', roles: ['USER', 'ADMIN'] },
      },
      {
        path: 'admin/users',
        component: AdminUserView,
        meta: { title: '用户管理', roles: ['ADMIN'] },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/chat' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role') || 'USER'

  // 未登录，且访问的不是 guest 路由
  if (!token && !to.meta.guest) {
    return next('/login')
  }

  // 已登录，访问 /login 时不自动重定向，让 LoginView 处理参数
  // （如果希望正常登录后手动跳转，这里可以放开，但为了回调，不自动跳）
  if (token && to.path === '/login') {
    // 这里不跳转，让 LoginView 的 onMounted 处理
    return next()
  }

  // 角色权限检查
  if (to.meta.roles && !to.meta.roles.includes(role)) {
    return next('/chat')
  }

  next()
})

export default router
