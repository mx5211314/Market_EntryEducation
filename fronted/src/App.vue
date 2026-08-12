<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <h1>📈 入市教育智慧助手</h1>
      <p>基于金融法规的智能伴学系统</p>
    </el-header>

    <el-main class="app-main">
      <router-view />
    </el-main>

    <el-footer class="app-footer">
      <el-menu
        :default-active="activeMenu"
        mode="horizontal"
        router
        class="bottom-menu">
        <el-menu-item index="/chat">💬 问答</el-menu-item>
        <el-menu-item index="/risk">📊 测评</el-menu-item>
        <el-menu-item index="/sim">📈 模拟</el-menu-item>
        <el-menu-item index="/report">📋 报告</el-menu-item>
      </el-menu>
    </el-footer>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/risk')) return '/risk'
  if (path.startsWith('/sim')) return '/sim'
  if (path.startsWith('/report')) return '/report'
  return '/chat'
})
</script>

<style scoped>
.app-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: #1e3c72;
  color: white;
  text-align: center;
  padding: 15px 0;
  flex-shrink: 0;
}

.app-header h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
}

.app-header p {
  margin: 5px 0 0;
  font-size: 14px;
  opacity: 0.85;
}

.app-main {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f0f2f5;
}

.app-footer {
  padding: 0;
  border-top: 1px solid #dcdfe6;
  flex-shrink: 0;
}

.bottom-menu {
  justify-content: space-around;
}
</style>
