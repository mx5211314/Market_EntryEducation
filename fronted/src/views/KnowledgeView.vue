<template>
  <div class="knowledge-container">
    <div class="header-section">
      <div class="header-content">
        <div class="header-icon">📚</div>
        <h1>金融法规知识库</h1>
      </div>
    </div>

    <div class="content">
      <!-- 左侧推荐阅读 -->
      <div class="recommend-section">
        <div class="section-title">热门文章</div>
        <div class="recommend-list">
          <div
            v-for="item in recommendList"
            :key="item.id"
            class="recommend-item"
            @click="goDetail(item.id)">
            <h4>{{ item.title }}</h4>
            <div class="recommend-meta">
              <el-icon><Reading /></el-icon>
              <span>{{ item.readCount || 0 }} 阅读</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧文章列表 -->
      <div class="article-list">
        <el-radio-group v-model="category" @change="loadArticles" class="filter-tabs">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="证券交易规则">证券交易规则</el-radio-button>
          <el-radio-button label="融资融券">融资融券</el-radio-button>
          <el-radio-button label="投资者适当性">投资者适当性</el-radio-button>
        </el-radio-group>

        <div v-for="article in articles" :key="article.id" class="article-item" @click="goDetail(article.id)">
          <div class="article-cover">
            <div class="cover-icon">📄</div>
          </div>
          <div class="article-info">
            <div class="article-header">
              <h3>{{ article.title }}</h3>
              <el-tag type="primary" size="small" effect="plain">{{ article.category }}</el-tag>
            </div>
            <div class="article-meta">
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

        <div v-if="articles.length === 0" class="empty-state">
          <el-icon><Document /></el-icon>
          <p>暂无文章</p>
        </div>

        <div class="pagination-wrapper">
          <el-pagination
            background
            layout="prev, pager, next"
            :page-size="pageSize"
            :total="total"
            :current-page="pageNum"
            @current-change="handlePageChange" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const articles = ref([])
const category = ref('')
const pageNum = ref(1)
const pageSize = ref(8)
const total = ref(0)
const recommendList = ref([])

const loadArticles = async () => {
  try {
    const res = await axios.get('/api/user/article/list', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        category: category.value || undefined,
      },
    })
    articles.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    console.error(e)
  }
}

const loadRecommend = async () => {
  try {
    const res = await axios.get('/api/user/article/list', {
      params: {
        pageNum: 1,
        pageSize: 5,
        sortField: 'readCount',
        sortDirection: 'desc',
      },
    })
    recommendList.value = res.data.records
  } catch (e) {
    console.error(e)
  }
}

const goDetail = (id) => {
  router.push(`/knowledge/${id}`)
}

const handlePageChange = (page) => {
  pageNum.value = page
  loadArticles()
}

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  loadArticles()
  loadRecommend()
})
</script>

<style scoped>
.knowledge-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 72px);
}

.flex-box {
  display: flex;
  align-items: center;
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

.header-icon {
  font-size: 48px;
  background: rgba(255, 255, 255, 0.2);
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-content h1 {
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}

.content {
  display: flex;
  gap: 24px;
}

/* 推荐阅读 */
.recommend-section {
  width: 280px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 20px;
  height: fit-content;
  position: sticky;
  top: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recommend-item {
  padding: 12px;
  border-left: 4px solid var(--primary-orange);
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 0 12px 12px 0;
}

.recommend-item:hover {
  background: #fef3e2;
  padding-left: 16px;
}

.recommend-item h4 {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.recommend-meta {
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 文章列表 */
.article-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filter-tabs {
  margin-bottom: 20px;
}

.filter-tabs :deep(.el-radio-button) {
  margin-right: 12px;
  border-radius: 20px !important;
}

.article-item {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  gap: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.article-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.article-cover {
  width: 160px;
  height: 100px;
  background: linear-gradient(135deg, #fef3e2, #fce7f6);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cover-icon {
  font-size: 32px;
}

.article-info {
  flex: 1;
  min-width: 0;
}

.article-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.article-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  flex: 1;
  line-height: 1.4;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.meta-item {
  font-size: 13px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--text-muted);
}

.empty-state .el-icon {
  font-size: 64px;
  margin-bottom: 16px;
  color: #e5e7eb;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .content {
    flex-direction: column;
  }

  .recommend-section {
    width: 100%;
    position: static;
  }

  .article-item {
    flex-direction: column;
  }

  .article-cover {
    width: 100%;
    height: 120px;
  }

  .header-section {
    padding: 32px 20px;
  }

  .header-content h1 {
    font-size: 22px;
  }

  .header-icon {
    width: 48px;
    height: 48px;
    font-size: 32px;
  }
}
</style>