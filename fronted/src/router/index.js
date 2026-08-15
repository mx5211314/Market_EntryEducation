import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import AuthLayout from '../layouts/AuthLayout.vue'
import FrontendLayout from '../layouts/FrontendLayout.vue'
import BackendLayout from '../layouts/BackendLayout.vue'

// 用户端页面
const HomeView = () => import('../views/HomeView.vue')
const ChatView = () => import('../views/ChatView.vue')
const KnowledgeView = () => import('../views/KnowledgeView.vue')
const ArticleDetailView = () => import('../views/ArticleDetailView.vue')
const RiskAssessmentView = () => import('../views/RiskAssessmentView.vue')
const SimTradeView = () => import('../views/SimTradeView.vue')
const DiaryView = () => import('../views/DiaryView.vue')
const FavoritesView = () => import('../views/FavoritesView.vue')
const ProfileView = () => import('../views/ProfileView.vue')

// 管理端页面
const DashboardView = () => import('../views/DashboardView.vue')
const AdminUserView = () => import('../views/AdminUserView.vue')
const AdminArticleView = () => import('../views/AdminArticleView.vue')
const FileManageView = () => import('../views/FileManageView.vue')

const routes = [
  {
    path: '/auth',
    component: AuthLayout,
    children: [
      { path: 'login', component: LoginView, meta: { title: '登录' } },
      { path: 'register', component: RegisterView, meta: { title: '注册' } },
    ]
  },
  {
    path: '/',
    component: FrontendLayout,
    children: [
      { path: '', component: HomeView, meta: { title: '首页', public: true } },
      { path: 'chat', component: ChatView, meta: { title: '智能问答' } },
      { path: 'knowledge', component: KnowledgeView, meta: { title: '知识库', public: true }, children: [
          { path: ':id', component: ArticleDetailView, meta: { title: '文章详情', public: true }, props: true },
      ] },
      { path: 'assessment', component: RiskAssessmentView, meta: { title: '风险测评' } },
      { path: 'simulation', component: SimTradeView, meta: { title: '模拟引导' } },
      { path: 'diary', component: DiaryView, meta: { title: '投资日记' } },
      { path: 'favorites', component: FavoritesView, meta: { title: '我的收藏' } },
      { path: 'profile', component: ProfileView, meta: { title: '个人中心' } },
    ]
  },
  {
    path: '/admin',
    component: BackendLayout,
    redirect: '/admin/dashboard',
    meta: { role: 'ADMIN' },
    children: [
      { path: 'dashboard', component: DashboardView, meta: { title: '数据看板' } },
      { path: 'users', component: AdminUserView, meta: { title: '用户管理' } },
      { path: 'articles', component: AdminArticleView, meta: { title: '文章管理' } },
      { path: 'files', component: FileManageView, meta: { title: '文件管理' } },
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = sessionStorage.getItem('token')
  const role = sessionStorage.getItem('role') || 'USER'

  // 登录相关页面
  if (to.path.startsWith('/auth')) {
    if (token) {
      // 已登录用户访问登录页，跳转到对应角色的首页
      if (role === 'ADMIN') {
        next('/admin/dashboard')
      } else {
        next('/')
      }
    } else {
      next()
    }
    return
  }

  // 管理后台页面
  if (to.path.startsWith('/admin')) {
    if (!token) {
      // 保存目标路径，登录后跳转
      sessionStorage.setItem('redirect', to.path)
      next('/auth/login')
      return
    }

    if (role !== 'ADMIN') {
      // 非管理员访问管理后台，跳转到首页
      next('/')
      return
    }

    next()
    return
  }

  // 前台页面
  if (!token && !to.meta.public) {
    // 保存目标路径，登录后跳转
    sessionStorage.setItem('redirect', to.path)
    next('/auth/login')
    return
  }

  next()
})

export default router
