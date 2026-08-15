<template>
  <div class="user-layout">
    <div class="navbar-container">
      <div class="brand-section">
        <div class="brand-logo">
          <el-icon size="24"><Connection /></el-icon>
        </div>
        <h1 class="brand-name">入市教育智慧助手</h1>
      </div>
      <div class="nav-section">
        <router-link to="/user/home" class="nav-link" :class="{ active: isActive('/user/home') }">首页</router-link>
        <router-link to="/user/chat" class="nav-link" :class="{ active: isActive('/user/chat') }">智能问答</router-link>
        <router-link to="/user/knowledge" class="nav-link" :class="{ active: isActive('/user/knowledge') }">知识库</router-link>
        <router-link to="/user/assessment" class="nav-link" :class="{ active: isActive('/user/assessment') }">风险测评</router-link>
        <router-link to="/user/diary" class="nav-link" :class="{ active: isActive('/user/diary') }">投资日记</router-link>
        <router-link to="/user/favorites" class="nav-link" :class="{ active: isActive('/user/favorites') }">我的收藏</router-link>
        <el-dropdown @command="handleCommand">
          <div class="user-dropdown">
            <el-avatar class="user-avatar">{{ nickname.charAt(0) }}</el-avatar>
            <span class="user-name">{{ nickname }}</span>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    <div class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Connection, ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const nickname = computed(() => sessionStorage.getItem('nickname') || sessionStorage.getItem('username') || '用户')

const isActive = (path) => {
  return route.path.startsWith(path)
}

const handleCommand = (command) => {
  if (command === 'profile') {
    router.push('/user/profile')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('username')
      sessionStorage.removeItem('nickname')
      sessionStorage.removeItem('role')
      sessionStorage.removeItem('userInfo')
      sessionStorage.removeItem('redirect')
      router.push('/login')
    })
  }
}
</script>

<style scoped>
.user-layout {
  height: 100vh;
  background: white;
}

.navbar-container {
  max-width: 1200px;
  height: 60px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0px 1px 1px rgba(0,0,0,0.05);
  background: white;
}

.brand-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #409eff;
  border-radius: 8px;
  color: white;
}

.brand-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.nav-section {
  display: flex;
  align-items: center;
  gap: 32px;
}

.nav-link {
  color: #606266;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  padding: 8px 12px;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.nav-link:hover {
  color: #409eff;
  background: #ecf5ff;
}

.nav-link.active {
  color: #409eff;
  background: #ecf5ff;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: background 0.3s ease;
}

.user-dropdown:hover {
  background: #f3f4f6;
}

.user-avatar {
  width: 32px;
  height: 32px;
  background: #909399;
}

.user-name {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background: #f1f4f6;
  min-height: calc(100vh - 60px);
}
</style>