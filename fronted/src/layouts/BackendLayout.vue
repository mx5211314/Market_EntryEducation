<template>
  <div class="backend-layout">
    <el-container>
      <el-aside width="200px">
        <div class="logo">
          <el-icon size="24" color="#fff"><Monitor /></el-icon>
          <span>管理后台</span>
        </div>
        <el-menu
          :default-active="$route.path"
          :router="true"
          class="sidebar-menu"
        >
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataLine /></el-icon>
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
        <el-header height="60px">
          <div class="header-content">
            <div class="header-title">{{ route.meta.title || '管理后台' }}</div>
            <div class="user-actions">
              <el-dropdown @command="handleCommand">
                <span class="user-info">
                  <el-avatar :size="32">{{ nickname }}</el-avatar>
                  <span class="nickname">{{ nickname }}</span>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </el-header>

        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Monitor, DataLine, User, Document, Folder } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const nickname = ref('管理员')

onMounted(() => {
  const userInfo = sessionStorage.getItem('userInfo')
  if (userInfo) {
    const user = JSON.parse(userInfo)
    nickname.value = user.nickname || user.username || '管理员'
  }
})

const handleCommand = (command) => {
  switch (command) {
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        sessionStorage.clear()
        router.push('/login')
      })
      break
  }
}
</script>

<style scoped lang="scss">
.backend-layout {
  .el-container {
    height: 100vh;
  }

  .el-aside {
    background-color: #001529;
    color: #fff;
    display: flex;
    flex-direction: column;

    .logo {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 20px;
      font-size: 18px;
      font-weight: bold;
      border-bottom: 1px solid #002140;
    }

    .sidebar-menu {
      border: none;
      background-color: transparent;

      .el-menu-item {
        color: #fff;
        &:hover {
          background-color: #002140;
        }
        &.is-active {
          background-color: #1890ff;
        }
      }
    }
  }

  .el-header {
    background-color: #fff;
    border-bottom: 1px solid #e4e7ed;

    .header-content {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 100%;
      padding: 0 20px;

      .header-title {
        font-size: 18px;
        font-weight: 500;
      }

      .user-actions {
        .user-info {
          display: flex;
          align-items: center;
          gap: 8px;
          cursor: pointer;

          .nickname {
            font-size: 14px;
            color: #606266;
          }
        }
      }
    }
  }

  .el-main {
    background-color: #f5f7fa;
    padding: 20px;
  }
}
</style>