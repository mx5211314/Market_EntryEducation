import { createRouter, createWebHistory } from 'vue-router'
const ChatView = () => import('../views/ChatView.vue')
const RiskTestView = () => import('../views/RiskTestView.vue')
const SimTradeView = () => import('../views/SimTradeView.vue')
const ReportView = () => import('../views/ReportView.vue')

const routes = [
  { path: '/', redirect: '/chat' },
  { path: '/chat', component: ChatView },
  { path: '/risk', component: RiskTestView },
  { path: '/sim', component: SimTradeView },
  { path: '/report', component: ReportView },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
