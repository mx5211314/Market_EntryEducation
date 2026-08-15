<template>
  <router-view />
</template>

<script setup>
// App.vue 保持简洁，所有路由逻辑都在路由文件中处理
</script>

<style>
/* 全局样式 */
#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 移除默认边距 */
* {
  box-sizing: border-box;
}

/* 平滑滚动 */
html {
  scroll-behavior: smooth;
}

/* Element Plus 全局样式覆盖 */
.el-button {
  transition: all 0.3s ease;
}

.el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.el-card {
  transition: all 0.3s ease;
}

.el-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}
</style>
