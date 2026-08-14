<template>
  <el-container class="app-shell">
    <el-aside class="app-sidebar" width="260px">
      <div class="brand">
        <div class="brand-mark">入</div>
        <div>
          <h1>入市教育智慧助手</h1>
          <p>法规问答 · 风险测评 · 模拟引导</p>
        </div>
      </div>

      <el-menu :default-active="activeMenu" router class="side-menu">
        <el-menu-item index="/chat">
          <span class="menu-icon">问</span>
          <span>智能问答</span>
        </el-menu-item>
        <el-menu-item index="/risk">
          <span class="menu-icon">测</span>
          <span>风险测评</span>
        </el-menu-item>
        <el-menu-item index="/sim">
          <span class="menu-icon">拟</span>
          <span>模拟引导</span>
        </el-menu-item>
        <el-menu-item index="/report">
          <span class="menu-icon">报</span>
          <span>历史报告</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-card">
        <span>知识库状态</span>
        <strong>证券规则 / 融资融券 / 适当性</strong>
      </div>
    </el-aside>

    <el-container class="main-shell">
      <el-header class="topbar">
        <div>
          <p class="eyebrow">Investment Education Assistant</p>
          <h2>{{ pageTitle }}</h2>
        </div>
        <el-tag type="success" effect="plain">合规提示已启用</el-tag>
      </el-header>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
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

const pageTitle = computed(() => {
  const titles = {
    '/chat': '智能法规问答',
    '/risk': '投资者风险测评',
    '/sim': '入市模拟引导',
    '/report': '风险报告归档',
  }
  return titles[activeMenu.value]
})
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #eef3f7;
}

.app-sidebar {
  display: flex;
  flex-direction: column;
  gap: 22px;
  padding: 24px 18px;
  background: #102331;
  color: #ffffff;
}

.brand {
  display: flex;
  gap: 12px;
  align-items: center;
}

.brand-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: #d9a441;
  color: #102331;
  font-weight: 800;
  font-size: 22px;
}

.brand h1 {
  margin: 0;
  font-size: 18px;
  line-height: 1.25;
}

.brand p,
.eyebrow {
  margin: 4px 0 0;
  color: #91a4b3;
  font-size: 12px;
}

.side-menu {
  border-right: 0;
  background: transparent;
}

.side-menu :deep(.el-menu-item) {
  height: 46px;
  margin: 4px 0;
  border-radius: 8px;
  color: #d9e2ea;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: #e9f3ff;
  color: #0b4f82;
}

.menu-icon {
  width: 26px;
  height: 26px;
  display: inline-grid;
  place-items: center;
  margin-right: 8px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.1);
  font-size: 13px;
}

.sidebar-card {
  margin-top: auto;
  padding: 14px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
}

.sidebar-card span,
.sidebar-card strong {
  display: block;
}

.sidebar-card span {
  color: #91a4b3;
  font-size: 12px;
}

.sidebar-card strong {
  margin-top: 6px;
  font-size: 14px;
  line-height: 1.5;
}

.main-shell {
  min-width: 0;
}

.topbar {
  height: 88px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  border-bottom: 1px solid #d8e0e7;
  background: #f9fbfd;
}

.topbar h2 {
  margin: 3px 0 0;
  color: #132a3a;
  font-size: 24px;
}

.app-main {
  padding: 28px 32px;
  overflow: auto;
}

@media (max-width: 820px) {
  .app-shell {
    display: block;
  }

  .app-sidebar {
    width: auto !important;
    padding: 16px;
  }

  .side-menu {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 8px;
  }

  .side-menu :deep(.el-menu-item) {
    justify-content: center;
    padding: 0 8px;
  }

  .menu-icon,
  .sidebar-card,
  .brand p {
    display: none;
  }

  .topbar {
    height: auto;
    padding: 18px;
    align-items: flex-start;
    gap: 12px;
  }

  .app-main {
    padding: 18px;
  }
}
</style>
