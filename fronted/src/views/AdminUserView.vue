<template>
  <div class="admin-page">
    <div class="page-card">
      <h2>用户管理</h2>
      <el-table :data="users" style="width: 100%" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{
              row.role
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button size="small" @click="toggleRole(row)">
              {{ row.role === 'ADMIN' ? '设为用户' : '设为管理员' }}
            </el-button>
            <el-button size="small" type="warning" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" @click="deleteUser(row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])

const loadUsers = async () => {
  const res = await axios.get('/api/user/list')
  users.value = res.data
}

const toggleRole = async (row) => {
  const newRole = row.role === 'ADMIN' ? 'USER' : 'ADMIN'
  await axios.put(`/api/user/${row.id}/role`, { role: newRole })
  ElMessage.success('角色已更新')
  loadUsers()
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  await axios.put(`/api/user/${row.id}/status`, { status: newStatus })
  ElMessage.success('状态已更新')
  loadUsers()
}

const deleteUser = async (row) => {
  await ElMessageBox.confirm('确定删除该用户吗？', '提示', { type: 'warning' })
  await axios.delete(`/api/user/${row.id}`)
  ElMessage.success('已删除')
  loadUsers()
}

onMounted(loadUsers)
</script>

<style scoped>
.admin-page {
  max-width: 1000px;
  margin: 0 auto;
}
.page-card {
  background: var(--card-bg);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-soft);
  padding: 24px;
}
h2 {
  color: var(--text-dark);
  margin-bottom: 18px;
}
</style>
