<template>
  <div class="admin-layout">
    <el-container class="main-container">
      <el-aside :width="isCollapse ? '64px' : '264px'" class="admin-aside">
        <el-menu
          :collapse="isCollapse"
          :collapse-transition="false"
          :default-active="activeMenu"
          router
          class="menu-style">
          <div class="brand">
            <div class="brand-logo">📈</div>
            <div v-show="!isCollapse" class="info-card">
              <h1 class="brand-title">入市教育助手</h1>
              <p class="brand-subtitle">管理后台</p>
            </div>
          </div>
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataBoard /></el-icon>
            <span>数据看板</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/articles">
            <el-icon><Document /></el-icon>
            <span>文章管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/files">
            <el-icon><Folder /></el-icon>
            <span>文件管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="admin-header">
          <el-button @click="toggleCollapse" text>
            <el-icon><Expand /></el-icon>
          </el-button>
          <p class="page-title">{{ pageTitle }}</p>
          <div class="header-right">
            <el-dropdown @command="handleCommand">
              <div class="user-info">
                <el-avatar class="user-avatar">{{ adminName.charAt(0) }}</el-avatar>
                <span class="user-name">{{ adminName }}</span>
                <el-icon><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>
        <el-main class="main-content">
          <router-view class="content-container" v-slot="{ Component }">
            <transition name="fade-slide" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()

const isCollapse = ref(false)
const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '管理后台')

const adminName = computed(
  () =>
    sessionStorage.getItem('nickname') ||
    sessionStorage.getItem('username') ||
    '管理员'
)

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleCommand = (command) => {
  if (command === 'logout') {
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
      router.push('/auth/login')
    })
  }
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

.main-container {
  height: 100%;
}

.admin-aside {
  background: #fff;
  border-right: 1px solid #e5e7eb;
  transition: width 0.3s ease;
}

.menu-style {
  height: 100%;
  border-right: none;
}

.menu-style :deep(.el-menu-item) {
  border-radius: 8px;
  margin: 4px 12px;
  color: #4b5563;
}

.menu-style :deep(.el-menu-item:hover) {
  background: #f3f4f6;
  color: #4a9c8c;
}

.menu-style :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgb(74, 156, 140) 0%, rgb(61, 138, 122) 100%);
  color: #fff;
}

.brand {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 16px 12px;
  background-color: #fff;
  border-bottom: 1px solid #e5e7eb;
  gap: 12px;
}

.brand-logo {
  font-size: 28px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(90deg, rgb(74, 156, 140) 0%, rgb(61, 138, 122) 100%);
  border-radius: 10px;
  color: #fff;
}

.info-card {
  flex: 1;
}

.brand-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
  color: #1f2937;
}

.brand-subtitle {
  font-size: 12px;
  color: #6b7280;
}

.admin-header {
  height: 64px !important;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  border-bottom: 1px solid #e5e7eb;
}

.page-title {
  margin-left: 20px;
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.3s ease;
}

.user-info:hover {
  background: #f3f4f6;
}

.user-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(90deg, rgb(74, 156, 140) 0%, rgb(61, 138, 122) 100%);
}

.user-name {
  font-size: 14px;
  color: #4b5563;
  font-weight: 500;
}

.main-content {
  background: #f9fafb;
  padding: 0;
}

.content-container {
  padding: 20px;
  min-height: calc(100vh - 64px);
  max-width: 1400px;
  margin: 0 auto;
}
</style>