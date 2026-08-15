<template>
  <div class="article-detail-container">
    <div class="header-section">
      <div class="header-content">
        <div class="back-btn" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
        </div>
        <div class="header-icon">📚</div>
        <h1>文章详情</h1>
      </div>
    </div>

    <div class="content" v-if="article.id">
      <!-- 文章信息卡片 -->
      <div class="diary-card">
        <div class="article-header">
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-meta">
            <el-tag type="warning" effect="plain" size="large">{{ article.category }}</el-tag>
            <div class="meta-item">
              <el-icon><User /></el-icon>
              <span>{{ article.author || '管理员' }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Clock /></el-icon>
              <span>{{ formatDate(article.createdAt) }}</span>
            </div>
            <div class="meta-item">
              <el-icon><View /></el-icon>
              <span>{{ article.readCount || 0 }} 阅读</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 正文内容 -->
      <div class="diary-card">
        <div class="content-title">正文内容</div>
        <div class="content-wrapper" v-html="formatContent(article.content)"></div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button type="primary" @click="toggleFavorite">
          <el-icon><Star :class="{ filled: favorited }" /></el-icon>
          {{ favorited ? '取消收藏' : '收藏文章' }}
        </el-button>
        <el-button @click="router.push('/user/knowledge')">
          <el-icon><List /></el-icon>
          返回列表
        </el-button>
      </div>
    </div>

    <div v-else class="loading-state">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p>加载中...</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const article = ref({})
const favorited = ref(false)

const toggleFavorite = async () => {
  try {
    if (favorited.value) {
      await axios.delete(`/api/user/favorite/${route.params.id}`)
      favorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await axios.post(`/api/user/favorite/${route.params.id}`)
      favorited.value = true
      ElMessage.success('收藏成功')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

onMounted(async () => {
  try {
    const res = await axios.get(`/api/user/article/${route.params.id}`)
    article.value = res.data
    const checkRes = await axios.get(`/api/user/favorite/check/${route.params.id}`)
    favorited.value = checkRes.data.favorited
  } catch (e) {
    console.error(e)
  }
})

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

const formatContent = (content) => {
  if (!content) return ''
  return content
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
}
</script>

<style scoped>
.article-detail-container {
  max-width: 980px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 72px);
}

.header-section {
  background: var(--primary-gradient);
  color: white;
  padding: 48px 32px;
  margin-bottom: 24px;
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(245, 158, 11, 0.15);
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.header-icon {
  font-size: 48px;
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-content h1 {
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}

/* 文章头部 */
.article-header {
  margin-bottom: 24px;
}

.article-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px 0;
  line-height: 1.4;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 14px;
}

/* 内容区域 */
.content-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.content-wrapper {
  font-size: 15px;
  color: var(--text-secondary);
  line-height: 1.8;
}

.content-wrapper :deep(h1),
.content-wrapper :deep(h2),
.content-wrapper :deep(h3),
.content-wrapper :deep(h4),
.content-wrapper :deep(h5),
.content-wrapper :deep(h6) {
  margin: 24px 0 16px 0;
  color: var(--text-primary);
  font-weight: 600;
  line-height: 1.4;
}

.content-wrapper :deep(h1) {
  font-size: 24px;
  border-bottom: 2px solid var(--border-color);
  padding-bottom: 8px;
}

.content-wrapper :deep(h2) {
  font-size: 20px;
  color: var(--primary-orange);
}

.content-wrapper :deep(p) {
  margin-bottom: 16px;
}

.content-wrapper :deep(ul),
.content-wrapper :deep(ol) {
  padding-left: 24px;
  margin-bottom: 16px;
}

.content-wrapper :deep(li) {
  margin-bottom: 8px;
  line-height: 1.6;
}

.content-wrapper :deep(code) {
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  color: #e11d48;
}

.content-wrapper :deep(blockquote) {
  border-left: 4px solid var(--primary-orange);
  padding-left: 16px;
  margin: 16px 0;
  color: var(--text-secondary);
  font-style: italic;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 16px;
  margin-top: 24px;
}

.action-buttons .el-button {
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-buttons .el-button--primary {
  background: linear-gradient(135deg, #f59e0b, #8b5cf6);
  border: none;
}

.filled {
  color: #ffd700 !important;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: var(--text-muted);
}

.is-loading {
  margin-bottom: 16px;
  color: var(--primary-orange);
}

/* 响应式 */
@media (max-width: 768px) {
  .header-section {
    padding: 32px 20px;
  }

  .header-content h1 {
    font-size: 22px;
  }

  .article-title {
    font-size: 22px;
  }

  .article-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .action-buttons {
    flex-direction: column;
  }

  .content-wrapper :deep(h1) {
    font-size: 20px;
  }

  .content-wrapper :deep(h2) {
    font-size: 18px;
  }
}
</style>