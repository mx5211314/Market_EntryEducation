<template>
  <div class="admin-page">
    <div class="page-card">
      <div class="card-head">
        <div class="head-left">
          <h2>用户管理</h2>
          <span class="head-note">共 {{ total }} 个账号</span>
        </div>
        <div class="filters">
          <el-input
            v-model="keyword"
            placeholder="搜索用户名 / 昵称 / 手机号"
            clearable
            class="f-search"
            @keyup.enter="applyFilter"
            @clear="applyFilter">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="role" placeholder="角色" clearable class="f-select" @change="applyFilter">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通用户" value="USER" />
          </el-select>
          <el-select v-model="status" placeholder="状态" clearable class="f-select" @change="applyFilter">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" @click="applyFilter">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </div>
      </div>

      <el-table :data="users" v-loading="loading" class="user-table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="账号" min-width="180">
          <template #default="{ row }">
            <div class="cell-user">
              <el-avatar :size="30">{{ (row.nickname || row.username || '?').slice(0, 1) }}</el-avatar>
              <div class="cu-text">
                <b>{{ row.nickname || row.username }}</b>
                <span>{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" effect="light">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="120">
          <template #default="{ row }">{{ shortDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <template v-if="isSelf(row)">
              <span class="self-note">当前登录账号</span>
            </template>
            <template v-else>
              <el-button size="small" text bg @click="toggleRole(row)">
                {{ row.role === 'ADMIN' ? '设为用户' : '设为管理员' }}
              </el-button>
              <el-button size="small" text bg type="warning" @click="toggleStatus(row)">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button size="small" text bg type="danger" @click="removeUser(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="hasFilter ? '没有符合条件的账号' : '暂无用户'" />
        </template>
      </el-table>

      <div class="pager" v-if="total > pageSize">
        <el-pagination
          background
          layout="prev, pager, next, jumper"
          :page-size="pageSize"
          :total="total"
          :current-page="pageNum"
          @current-change="changePage" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getAdminUserList, updateUserRole, updateUserStatus, deleteUser } from '@/api/admin'

const users = ref([])
const loading = ref(false)
const keyword = ref('')
const role = ref('')
const status = ref(null)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const currentUsername = computed(() => {
  try {
    return JSON.parse(sessionStorage.getItem('userInfo') || '{}').username || ''
  } catch (e) {
    return ''
  }
})
// 服务端也拦了，这里只是不给按钮，省一次失败请求
const isSelf = (row) => row.username && row.username === currentUsername.value
const hasFilter = computed(() => !!keyword.value || !!role.value || status.value !== null)

const loadUsers = async () => {
  loading.value = true
  try {
    const page = await getAdminUserList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      role: role.value || undefined,
      status: status.value === null ? undefined : status.value
    })
    users.value = page?.records || []
    total.value = page?.total || 0
    // 删到当前页空了就退一页，否则看着像没数据
    if (!users.value.length && pageNum.value > 1) {
      pageNum.value--
      return loadUsers()
    }
  } catch (e) {
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const applyFilter = () => {
  pageNum.value = 1
  loadUsers()
}

const resetFilter = () => {
  keyword.value = ''
  role.value = ''
  status.value = null
  applyFilter()
}

const changePage = (p) => {
  pageNum.value = p
  loadUsers()
}

const toggleRole = async (row) => {
  const toAdmin = row.role !== 'ADMIN'
  try {
    await ElMessageBox.confirm(
      toAdmin
        ? `确定把「${row.nickname || row.username}」设为管理员？该账号将能进入后台管理所有数据。`
        : `确定把「${row.nickname || row.username}」降为普通用户？`,
      '修改角色', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await updateUserRole(row.id, toAdmin ? 'ADMIN' : 'USER')
    ElMessage.success('角色已更新')
    loadUsers()
  } catch (e) {
    // 拦截器已提示后端返回的原因
  }
}

const toggleStatus = async (row) => {
  const disable = row.status === 1
  if (disable) {
    try {
      await ElMessageBox.confirm(`禁用后「${row.nickname || row.username}」将无法登录，确定禁用？`, '禁用账号', { type: 'warning' })
    } catch (e) {
      return
    }
  }
  try {
    await updateUserStatus(row.id, disable ? 0 : 1)
    ElMessage.success(disable ? '已禁用' : '已启用')
    loadUsers()
  } catch (e) {
    // 同上
  }
}

const removeUser = async (row) => {
  try {
    await ElMessageBox.confirm(
      `删除后该账号的测评、日记等数据将失去归属且无法恢复，确定删除「${row.nickname || row.username}」？`,
      '删除账号', { type: 'warning', confirmButtonText: '确定删除', confirmButtonClass: 'el-button--danger' })
  } catch (e) {
    return
  }
  try {
    await deleteUser(row.id)
    ElMessage.success('已删除')
    loadUsers()
  } catch (e) {
    // 同上
  }
}

const shortDate = (v) => (v ? String(v).replace('T', ' ').slice(0, 10) : '—')

onMounted(loadUsers)
</script>

<style scoped lang="scss">
.admin-page {
  max-width: 1180px;
  margin: 0 auto;
}

.page-card {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(31, 45, 61, 0.06);
  padding: 20px 22px 8px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;

  .head-left {
    display: flex;
    align-items: baseline;
    gap: 10px;

    h2 {
      margin: 0;
      font-size: 17px;
      color: #1f2329;
    }

    .head-note {
      font-size: 12.5px;
      color: #a8adb7;
    }
  }

  .filters {
    display: flex;
    align-items: center;
    gap: 10px;

    .f-search {
      width: 230px;
    }

    .f-select {
      width: 118px;
    }
  }
}

.user-table {
  width: 100%;

  .cell-user {
    display: flex;
    align-items: center;
    gap: 10px;

    :deep(.el-avatar) {
      background: linear-gradient(135deg, #409eff, #67c23a);
      font-size: 13px;
      flex-shrink: 0;
    }

    .cu-text {
      display: flex;
      flex-direction: column;
      line-height: 1.35;
      min-width: 0;

      b {
        font-size: 13.5px;
        color: #1f2329;
      }

      span {
        font-size: 12px;
        color: #a8adb7;
      }
    }
  }

  .self-note {
    font-size: 12.5px;
    color: #c0c4cc;
  }
}

.pager {
  display: flex;
  justify-content: center;
  padding: 18px 0 14px;
}
</style>
