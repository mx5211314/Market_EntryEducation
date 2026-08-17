<template>
  <div class="admin-page">
    <div class="page-card">
      <div class="card-head">
        <div class="head-left">
          <h2>文章管理</h2>
          <span class="head-note">共 {{ total }} 篇</span>
        </div>
        <div class="filters">
          <el-input
            v-model="keyword"
            placeholder="搜索标题 / 摘要 / 分类 / 标签"
            clearable
            class="f-search"
            @keyup.enter="applyFilter"
            @clear="applyFilter">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="status" placeholder="状态" clearable class="f-select" @change="applyFilter">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下架" :value="2" />
          </el-select>
          <el-button @click="applyFilter">查询</el-button>
          <el-button type="primary" @click="openCreate">新建文章</el-button>
        </div>
      </div>

      <el-table :data="articles" v-loading="loading" class="article-table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="文章" min-width="300">
          <template #default="{ row }">
            <div class="cell-article">
              <el-image v-if="row.coverImage" :src="row.coverImage" fit="cover" class="ca-cover" />
              <div v-else class="ca-cover ca-placeholder"><el-icon><Picture /></el-icon></div>
              <div class="ca-text">
                <b>{{ row.title }}</b>
                <span>{{ row.summary || '暂无摘要' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="110">
          <template #default="{ row }">{{ row.category || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="95">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="readCount" label="阅读" width="80">
          <template #default="{ row }">{{ row.readCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="120">
          <template #default="{ row }">{{ shortDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text bg @click="openEdit(row)">编辑</el-button>
            <el-button size="small" text bg type="warning" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '发布' }}
            </el-button>
            <el-button size="small" text bg type="danger" @click="removeArticle(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="hasFilter ? '没有符合条件的文章' : '还没有文章，先新建一篇'" />
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

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="950px">
      <el-form label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="分类">
          <!-- 分类直接读库里已有的，输入新词也能建，前台侧边栏会自动多出一项 -->
          <el-select v-model="form.category" placeholder="选择或输入分类" filterable allow-create default-first-option>
            <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="500" show-word-limit
            placeholder="列表页和详情页顶部展示，留空则不显示摘要卡片" />
        </el-form-item>
        <el-form-item label="封面图">
          <div class="cover-field">
            <el-upload
              :show-file-list="false"
              action="/api/upload?cover=true"
              accept="image/*"
              :headers="uploadHeaders"
              :on-success="handleCoverSuccess"
              :on-error="handleCoverError">
              <el-button>上传图片</el-button>
            </el-upload>
            <el-input v-model="form.coverImage" placeholder="也可直接粘贴图片 URL，留空则用默认渐变背景" />
            <el-image v-if="form.coverImage" :src="form.coverImage" fit="cover" class="cover-preview" />
          </div>
          <div class="field-tip">
            建议 16:9 横图、宽度 1200px 以上（如 1200×675）。上传后服务端会自动从中心裁成 16:9 并压到 1200×675，比例不对的图会被截掉多余的边。
          </div>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="多个标签用英文逗号分隔，如：开户,风险,新手" />
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
        <el-button type="primary" :loading="saving" @click="saveArticle">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, shallowRef, watch, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Picture } from '@element-plus/icons-vue'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { getArticleCategories } from '@/api/frontend'
import {
  getAdminArticleList, createArticle, updateArticle,
  updateArticleStatus, deleteArticle
} from '@/api/admin'

const articles = ref([])
const categories = ref([])
const loading = ref(false)
const keyword = ref('')
const status = ref(null)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = ref({ id: null, title: '', category: '', summary: '', coverImage: '', tags: '', content: '', author: '' })
const hasFilter = computed(() => !!keyword.value || status.value !== null)
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

// el-upload 走原生请求，不经过 axios 拦截器，得自己带上 token
const uploadHeaders = computed(() => ({
  Authorization: 'Bearer ' + sessionStorage.getItem('token')
}))

const handleCoverSuccess = (res) => {
  if (res?.errno === 0 && res.data?.url) {
    form.value.coverImage = res.data.url
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(res?.msg || '封面上传失败')
  }
}

const handleCoverError = () => {
  ElMessage.error('封面上传失败，请检查登录状态和图片格式')
}

const loadArticles = async () => {
  loading.value = true
  try {
    const page = await getAdminArticleList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      status: status.value === null ? undefined : status.value
    })
    articles.value = page?.records || []
    total.value = page?.total || 0
    // 删到当前页空了就退一页
    if (!articles.value.length && pageNum.value > 1) {
      pageNum.value--
      return loadArticles()
    }
  } catch (e) {
    articles.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const applyFilter = () => {
  pageNum.value = 1
  loadArticles()
}

const changePage = (p) => {
  pageNum.value = p
  loadArticles()
}

const loadCategories = async () => {
  try {
    categories.value = (await getArticleCategories()) || []
  } catch (e) {
    console.error('加载分类失败:', e)
  }
}

const openCreate = () => {
  dialogTitle.value = '新建文章'
  form.value = { id: null, title: '', category: '', summary: '', coverImage: '', tags: '', content: '', author: '' }
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
  saving.value = true
  try {
    if (form.value.id) {
      await updateArticle(form.value.id, form.value)
    } else {
      await createArticle(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    if (!form.value.id) pageNum.value = 1
    loadArticles()
    loadCategories()
  } catch (e) {
    // 拦截器已提示后端返回的原因
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row) => {
  const next = row.status === 1 ? 2 : 1
  try {
    await updateArticleStatus(row.id, next)
    ElMessage.success(next === 1 ? '已发布' : '已下架')
    loadArticles()
  } catch (e) {
    // 同上
  }
}

const removeArticle = async (row) => {
  try {
    await ElMessageBox.confirm(`删除后无法恢复，确定删除「${row.title}」？`, '删除文章', {
      type: 'warning', confirmButtonText: '确定删除', confirmButtonClass: 'el-button--danger'
    })
  } catch (e) {
    return
  }
  try {
    await deleteArticle(row.id)
    ElMessage.success('已删除')
    loadArticles()
  } catch (e) {
    // 同上
  }
}

const shortDate = (v) => (v ? String(v).replace('T', ' ').slice(0, 10) : '—')
const statusText = (s) => ['草稿', '已发布', '已下架'][s] || '未知'
const statusType = (s) => (s === 1 ? 'success' : s === 2 ? 'info' : 'warning')

watch(dialogVisible, (val) => {
  if (!val && editorRef.value) {
    editorRef.value.destroy()
    editorRef.value = null
  }
})
onBeforeUnmount(() => {
  if (editorRef.value) editorRef.value.destroy()
})
onMounted(() => {
  loadArticles()
  loadCategories()
})
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
      width: 250px;
    }

    .f-select {
      width: 112px;
    }
  }
}

.article-table {
  width: 100%;

  .cell-article {
    display: flex;
    align-items: center;
    gap: 12px;

    .ca-cover {
      width: 72px;
      height: 41px;
      border-radius: 6px;
      flex-shrink: 0;
    }

    .ca-placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      color: #c8cdd6;
      background: linear-gradient(135deg, rgba(64, 158, 255, 0.08), rgba(103, 194, 58, 0.06));
    }

    .ca-text {
      display: flex;
      flex-direction: column;
      gap: 3px;
      min-width: 0;

      b {
        font-size: 13.5px;
        color: #1f2329;
      }

      // 摘要只留一行，否则每行高度不一，表格会像被撑开
      span {
        font-size: 12px;
        color: #a8adb7;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}

.pager {
  display: flex;
  justify-content: center;
  padding: 18px 0 14px;
}

.editor-wrapper {
  width: 100%;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  z-index: 0;

  :deep(.w-e-toolbar) {
    border-bottom: 1px solid #e4e7ed;
  }

  :deep(.w-e-text-container) {
    min-height: 300px;
  }
}

.cover-field {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.cover-preview {
  width: 96px;
  height: 54px;
  border-radius: 6px;
  flex-shrink: 0;
}

.field-tip {
  width: 100%;
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: #999;
}
</style>
