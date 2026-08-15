<template>
  <div>
    <div class="page-header">
      <h2>📄 文章管理</h2>
      <el-button type="primary" @click="openCreate">新建文章</el-button>
    </div>

    <el-table :data="articles" style="width: 100%" border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{
            statusText(row.status)
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="author" label="作者" width="100" />
      <el-table-column prop="createdAt" label="创建时间" width="160" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="toggleStatus(row)">
            {{ row.status === 1 ? '下架' : '发布' }}
          </el-button>
          <el-button size="small" type="danger" @click="deleteArticle(row)"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="950px">
      <el-form label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="选择分类">
            <el-option label="证券交易规则" value="证券交易规则" />
            <el-option label="融资融券" value="融资融券" />
            <el-option label="投资者适当性" value="投资者适当性" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
            <div class="editor-wrapper">
              <Toolbar
                :editor="editorRef"
                :defaultConfig="toolbarConfig"
                mode="default" />
              <Editor
                v-model="form.content"
                :defaultConfig="editorConfig"
                mode="default"
                @onCreated="handleCreated" />
            </div>
          </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="form.author" placeholder="默认管理员" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveArticle">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, shallowRef, watch, onBeforeUnmount } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

const articles = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = ref({ id: null, title: '', category: '', content: '', author: '' })
const editorRef = shallowRef()
const toolbarConfig = { excludeKeys: ['group-video', 'fullScreen'] }
const editorConfig = {
  placeholder: '请输入文章内容...',
  MENU_CONF: {
    uploadImage: {
      server: '/api/upload',
      fieldName: 'file',
      maxFileSize: 5 * 1024 * 1024,
      headers: { Authorization: 'Bearer ' + (sessionStorage.getItem('token') || '') },
      allowedFileTypes: ['image/*'],
    },
  },
}
const handleCreated = (editor) => { editorRef.value = editor }

const loadArticles = async () => {
  const res = await axios.get('/api/admin/article/list', {
    params: { pageNum: 1, pageSize: 100 },
  })
  articles.value = res.data.records
}

const openCreate = () => {
  dialogTitle.value = '新建文章'
  form.value = { id: null, title: '', category: '', content: '', author: '' }
  dialogVisible.value = true
}

const openEdit = (row) => {
  dialogTitle.value = '编辑文章'
  form.value = { ...row }
  dialogVisible.value = true
}

const saveArticle = async () => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  try {
    if (form.value.id) {
      await axios.put(`/api/admin/article/${form.value.id}`, form.value)
    } else {
      await axios.post('/api/admin/article', form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadArticles()
  } catch (e) {
    ElMessage.error('保存失败')
    console.error(e)
  }
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 2 : 1
  await axios.put(`/api/admin/article/${row.id}/status`, { status: newStatus })
  ElMessage.success('状态已更新')
  loadArticles()
}

const deleteArticle = async (row) => {
  await ElMessageBox.confirm('确定删除该文章吗？', '提示', { type: 'warning' })
  await axios.delete(`/api/admin/article/${row.id}`)
  ElMessage.success('删除成功')
  loadArticles()
}

const statusText = (status) => ['草稿', '已发布', '已下架'][status] || '未知'
const statusType = (status) =>
  status === 1 ? 'success' : status === 2 ? 'info' : 'warning'

watch(dialogVisible, (val) => {
  if (!val && editorRef.value) {
    editorRef.value.destroy()
    editorRef.value = null
  }
})
onBeforeUnmount(() => {
  if (editorRef.value) editorRef.value.destroy()
})
onMounted(loadArticles)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.editor-wrapper {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  z-index: 0;
}
.editor-wrapper :deep(.w-e-toolbar) {
  border-bottom: 1px solid #e4e7ed;
}
.editor-wrapper :deep(.w-e-text-container) {
  min-height: 300px;
}
</style>
