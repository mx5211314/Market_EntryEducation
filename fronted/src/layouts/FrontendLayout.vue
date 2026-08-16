<template>
  <div class="frontend-layout">
    <el-container>
      <el-header height="60px">
        <div class="header-content">
          <div class="logo">
            <el-icon size="24" color="#409eff"><Monitor /></el-icon>
            <span>智能投资助手</span>
          </div>
          <el-menu
            mode="horizontal"
            :router="true"
            :default-active="$route.path"
            class="nav-menu"
          >
            <el-menu-item index="/">首页</el-menu-item>
            <el-menu-item index="/chat">智能问答</el-menu-item>
            <el-menu-item index="/knowledge">知识库</el-menu-item>
            <el-menu-item index="/assessment">风险测评</el-menu-item>
            <el-menu-item index="/simulation">模拟引导</el-menu-item>
            <el-menu-item index="/diary">投资日记</el-menu-item>
          </el-menu>
          <div class="user-actions" v-if="isLoggedIn">
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="32">{{ nickname }}</el-avatar>
                <span class="nickname">{{ nickname }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>

      <el-main :class="{ 'no-pad': $route.path === '/' || $route.path === '/chat' }">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Monitor } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const nickname = ref('用户')

const isLoggedIn = ref(false)

const updateUserInfo = () => {
  const token = sessionStorage.getItem('token')
  const userInfo = sessionStorage.getItem('userInfo')

  if (token && userInfo) {
    isLoggedIn.value = true
    const user = JSON.parse(userInfo)
    nickname.value = user.nickname || user.username || '用户'
  } else {
    isLoggedIn.value = false
    nickname.value = '用户'
  }
}

watch(() => sessionStorage.getItem('token'), (newToken) => {
  updateUserInfo()
})

onMounted(() => {
  updateUserInfo()
})

watch(() => route.path, (newPath) => {
  updateUserInfo()
})

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'favorites':
      router.push('/favorites')
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        sessionStorage.clear()
        updateUserInfo()
        router.push('/auth/login')
      })
      break
  }
}
</script>

<style scoped lang="scss">
.frontend-layout {
  .el-container {
    height: 100vh;
  }

  .el-header {
    background-color: #fff;
    border-bottom: 1px solid var(--border-light);
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
    padding: 0 !important;
    height: 60px !important;
    position: relative;
    z-index: 10;

    .header-content {
      display: flex;
      align-items: center;
      height: 100%;
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 20px;
      box-sizing: border-box;

      .logo {
        display: flex;
        align-items: center;
        gap: 10px;
        font-size: 20px;
        font-weight: bold;
        color: #409eff;
        white-space: nowrap;
      }

      .nav-menu {
        justify-content: center;
        border-bottom: none;
        flex: 1;
      }

      .user-actions {
        display: flex;
        align-items: center;
        min-width: 120px;

        :deep(.el-dropdown) {
          display: flex;
          align-items: center;
          height: 100%;
        }

        .user-info {
          display: flex;
          align-items: center;
          gap: 8px;
          cursor: pointer;
          padding: 0 8px;

          .nickname {
            font-size: 14px;
            color: #606266;
          }
        }
      }
    }
  }

  .el-main {
    padding: 24px;
    background: #f5f7fa;

    &.no-pad {
      padding: 0;
      background: transparent;
    }
  }
}
</style>
