import { createRouter, createWebHistory } from 'vue-router'
import ChatView from '../views/ChatView.vue'
import RiskTestView from '../views/RiskTestView.vue'
import SimTradeView from '../views/SimTradeView.vue'
import ReportView from '../views/ReportView.vue'

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
