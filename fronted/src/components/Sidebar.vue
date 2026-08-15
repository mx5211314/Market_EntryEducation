<template>
  <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar-container">
    <div class="logo-area">
      <span class="logo-icon">📈</span>
      <span v-show="!isCollapse" class="logo-text">入市教育</span>
    </div>
    <el-menu
      :collapse="isCollapse"
      :collapse-transition="false"
      :default-active="activeMenu"
      class="sidebar-menu"
      router>
      <el-menu-item
        v-for="item in filteredMenus"
        :key="item.path"
        :index="item.path">
        <el-icon><component :is="item.meta.icon" /></el-icon>
        <span>{{ item.meta.title }}</span>
      </el-menu-item>
    </el-menu>
    <div class="sidebar-footer" v-show="!isCollapse">入市教育智慧助手 v1.0</div>
  </el-aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const route = useRoute()
const store = useAppStore()
const isCollapse = computed(() => store.isCollapse)

// 获取当前用户角色
const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}')
const userRole = userInfo.role || 'USER' // 默认普通用户

// 从路由中过滤出后台菜单（只取 / 下的子路由，且 meta 中有 title）
const allRoutes =
  router.options.routes.find((r) => r.path === '/')?.children || []
const filteredMenus = computed(() => {
  return allRoutes.filter((route) => {
    // 必须有 meta.title 才显示在菜单
    if (!route.meta || !route.meta.title) return false
    // 检查角色权限：如果 meta 没有 roles，默认所有人可见
    if (!route.meta.roles) return true
    return route.meta.roles.includes(userRole)
  })
})

const activeMenu = computed(() => route.path)
</script>

<style scoped>
.sidebar-container {
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
  transition: width 0.3s;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.logo-icon {
  font-size: 28px;
}
.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
  white-space: nowrap;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  background: transparent;
  padding: 8px 0;
}
.sidebar-menu :deep(.el-menu-item) {
  color: #d4c8d4;
  border-radius: 10px;
  margin: 0 12px 4px;
  height: 44px;
  line-height: 44px;
}
.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--sidebar-active);
  color: #fff;
}
.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}
.sidebar-menu :deep(.el-menu-item .el-icon) {
  font-size: 18px;
  margin-right: 8px;
}

.sidebar-footer {
  text-align: center;
  color: #7d6b7d;
  font-size: 12px;
  padding: 16px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}
</style>
