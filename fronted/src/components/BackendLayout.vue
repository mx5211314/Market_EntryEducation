<template>
  <el-container class="backend-layout">
    <Sidebar />
    <el-container>
      <el-header height="60px">
        <Navbar />
      </el-header>
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'
</script>

<style lang="scss" scoped>
.backend-layout {
  height: 100vh;
  background: var(--bg-warm);
  .el-header {
    padding: 0 20px;
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(12px);
    border-bottom: 1px solid var(--border-soft);
  }
  .main-content {
    padding: 20px;
    overflow-y: auto;
    .fade-enter-active,
    .fade-leave-active {
      transition: opacity 0.2s ease;
    }
    .fade-enter-from,
    .fade-leave-to {
      opacity: 0;
    }
  }
}
</style>
