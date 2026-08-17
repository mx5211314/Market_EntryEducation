<template>
  <div class="navbar">
    <div class="left">
      <el-button text @click="toggleCollapse">
        <el-icon><Expand /></el-icon>
      </el-button>
      <span class="page-title">{{ currentTitle }}</span>
    </div>
    <div class="right">
      <button v-if="isAdmin" class="admin-btn" @click="goAdminUsers">
        用户管理
      </button>
      <span class="username" @click="goProfile">{{
        nickname || username
      }}</span>
      <el-button text class="logout-btn" @click="handleLogout">退出</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const store = useAppStore()

const username = sessionStorage.getItem('username') || ''
const nickname = sessionStorage.getItem('nickname') || ''
const role = sessionStorage.getItem('role') || ''
const isAdmin = computed(() => role === 'ADMIN')

const currentTitle = computed(() => {
  return route.meta?.title || '入市教育智慧助手'
})

const toggleCollapse = () => {
  store.toggleCollapse()
}

const goAdminUsers = () => {
  router.push('/admin/users')
}
const goProfile = () => {
  router.push('/profile')
}

const handleLogout = () => {
  ElMessageBox.confirm('确定退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('username')
    sessionStorage.removeItem('nickname')
    sessionStorage.removeItem('role')
    sessionStorage.removeItem('userInfo')
    router.push('/auth/login')
  })
}
</script>

<style scoped>
.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}
.left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-dark);
}
.right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.admin-btn {
  padding: 6px 14px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #ff9a8b, #a4508b);
  color: white;
  font-size: 13px;
  cursor: pointer;
  transition: transform 0.2s;
}
.admin-btn:hover {
  transform: scale(1.02);
}
.username {
  font-weight: 600;
  color: var(--text-dark);
  cursor: pointer;
}
.username:hover {
  color: #ff6a88;
}
.logout-btn {
  border: 1px solid var(--border-soft);
  border-radius: 8px;
  color: var(--text-muted);
}
.logout-btn:hover {
  border-color: #ff8a9b;
  color: #ff6a88;
}
</style>
