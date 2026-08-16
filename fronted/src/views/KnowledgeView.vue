<template>
  <div class="knowledge-container">
    <!-- 头部区域 -->
    <div class="header-section">
      <div class="header-content">
        <div class="header-icon">📚</div>
        <div class="header-text">
          <h1>金融法规知识库</h1>
          <p>学习投资知识，掌握交易规则</p>
        </div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-input-wrapper">
        <el-icon class="search-icon"><Search /></el-icon>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文章标题或关键词..."
          clearable
          @keydown.enter="handleSearch"
          style="width: 100%" />
      </div>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <div class="content">
      <!-- 左侧菜单 -->
      <div class="sidebar">
        <!-- 分类筛选 -->
        <div class="filter-section">
          <div class="section-title">
            <el-icon><Menu /></el-icon>
            <span>分类筛选</span>
          </div>
          <div class="category-list">
            <div
              :class="{ active: category === cat }"
              class="category-item"
              @click="selectCategory(cat)"
              v-for="cat in ['全部', '证券交易规则', '融资融券', '投资者适当性']"
              :key="cat">
              {{ cat }}
            </div>
          </div>
        </div>

        <!-- 热门文章 -->
        <div class="recommend-section">
          <div class="section-title">
            <el-icon><TrendCharts /></el-icon>
            <span>热门文章</span>
          </div>
          <div class="recommend-list">
            <div
              v-for="(item, index) in recommendList"
              :key="item.id"
              class="recommend-item"
              @click="goDetail(item.id)">
              <span class="rank" v-if="index < 3">{{ index + 1 }}</span>
              <div class="item-content">
                <h4>{{ item.title }}</h4>
                <div class="read-meta">
                  <el-icon><View /></el-icon>
                  <span>{{ item.readCount || 0 }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧文章列表 -->
      <div class="article-list">
        <!-- 文章卡片 -->
        <div
          v-for="article in articles"
          :key="article.id"
          class="article-card"
          @click="goDetail(article.id)">
          <div class="card-cover">
            <el-image
              v-if="article.coverImage"
              :src="article.coverImage"
              fit="cover"
              lazy />
            <div class="cover-default" v-else>
              <span class="cover-icon">📄</span>
            </div>
          </div>
          <div class="card-body">
            <div class="card-header">
              <el-tag size="small" effect="plain">{{ article.category }}</el-tag>
              <span class="publish-time">{{ formatDate(article.createdAt) }}</span>
            </div>
            <h3 class="card-title">{{ article.title }}</h3>
            <p class="card-summary">{{ article.summary || '点击查看详情...' }}</p>
            <div class="card-footer">
              <div class="author-info">
                <el-icon><User /></el-icon>
                <span>{{ article.author || '管理员' }}</span>
              </div>
              <div class="stat-info">
                <span class="stat-item">
                  <el-icon><View /></el-icon>
                  {{ formatNumber(article.readCount || 0) }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="articles.length === 0" class="empty-state">
          <el-empty
            :image-size="120"
            :description="searchKeyword ? '换个关键词试试' : '暂无文章'" />
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > pageSize">
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
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getKnowledgeList } from '@/api/frontend'
import { Search, Menu, TrendCharts, View, User } from '@element-plus/icons-vue'

const router = useRouter()

// 数据状态
const articles = ref([])
const recommendList = ref([])
const category = ref('全部')
const searchKeyword = ref('')
const pageNum = ref(1)
const pageSize = ref(8)
const total = ref(0)

// 加载文章列表
const loadArticles = async () => {
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      category: category.value !== '全部' ? category.value : undefined,
      keyword: searchKeyword.value || undefined,
      sortField: 'createdAt',
      sortDirection: 'desc'
    }
    const res = await getKnowledgeList(params)
    articles.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error('加载文章失败:', e)
  }
}

// 加载热门文章
const loadRecommend = async () => {
  try {
    const params = {
      pageNum: 1,
      pageSize: 6,
      sortField: 'readCount',
      sortDirection: 'desc'
    }
    const res = await getKnowledgeList(params)
    recommendList.value = res.records || []
  } catch (e) {
    console.error('加载推荐失败:', e)
  }
}

// 选择分类
const selectCategory = (cat) => {
  category.value = cat
  pageNum.value = 1
  loadArticles()
}

// 搜索
const handleSearch = () => {
  pageNum.value = 1
  loadArticles()
}

// 分页切换
const handlePageChange = (page) => {
  pageNum.value = page
  loadArticles()
}

// 跳转详情
const goDetail = (id) => {
  router.push(`/knowledge/${id}`)
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

// 格式化数字
const formatNumber = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num
}

// 监听路由参数变化
watch(() => router.currentRoute.value.query.keyword, (val) => {
  if (val) {
    searchKeyword.value = val
    handleSearch()
  }
})

onMounted(() => {
  loadArticles()
  loadRecommend()
})
</script>

<style scoped lang="scss">
.knowledge-container {
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #fafbfc 0%, #f7f9fc 50%, #f2f6fa 100%);
  padding-bottom: 40px;

  /* 头部区域 */
  .header-section {
    background: linear-gradient(135deg, #f59e0b 0%, #8b5cf6 100%);
    padding: 40px 24px;
    margin-bottom: 20px;
    box-shadow: 0 8px 32px rgba(245, 158, 11, 0.15);

    .header-content {
      max-width: 1200px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      gap: 20px;

      .header-icon {
        font-size: 56px;
        background: rgba(255, 255, 255, 0.2);
        width: 72px;
        height: 72px;
        border-radius: 16px;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .header-text {
        h1 {
          font-size: 32px;
          font-weight: 700;
          color: white;
          margin: 0 0 8px 0;
        }

        p {
          font-size: 15px;
          color: rgba(255, 255, 255, 0.9);
          margin: 0;
        }
      }
    }
  }

  /* 搜索栏 */
  .search-bar {
    max-width: 1200px;
    margin: -30px auto 24px;
    padding: 0 24px;
    display: flex;
    gap: 12px;
    align-items: center;
    position: relative;
    z-index: 10;

    .search-input-wrapper {
      flex: 1;
      position: relative;

      .search-icon {
        position: absolute;
        left: 16px;
        top: 50%;
        transform: translateY(-50%);
        color: #9ca3af;
        font-size: 18px;
        pointer-events: none;
      }

      :deep(.el-input__wrapper) {
        padding-left: 44px;
        border-radius: 12px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
        padding: 0;

        input {
          padding: 14px 0;
        }
      }
    }
  }

  /* 主体内容 */
  .content {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
    display: flex;
    gap: 24px;

    /* 左侧边栏 */
    .sidebar {
      width: 280px;
      flex-shrink: 0;
      display: flex;
      flex-direction: column;
      gap: 20px;

      .filter-section,
      .recommend-section {
        background: white;
        border-radius: 16px;
        padding: 20px;
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
      }

      .section-title {
        font-size: 14px;
        font-weight: 600;
        color: #374151;
        margin-bottom: 16px;
        display: flex;
        align-items: center;
        gap: 8px;
      }

      /* 分类筛选 */
      .category-list {
        display: flex;
        flex-direction: column;
        gap: 8px;

        .category-item {
          padding: 10px 14px;
          border-radius: 8px;
          cursor: pointer;
          font-size: 14px;
          color: #6b7280;
          transition: all 0.2s ease;

          &:hover {
            background: #f3f4f6;
            color: #1f2937;
          }

          &.active {
            background: linear-gradient(135deg, #fef3c7, #fce7f3);
            color: #c2410c;
            font-weight: 500;
          }
        }
      }

      /* 热门推荐 */
      .recommend-list {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .recommend-item {
          padding: 10px 12px;
          border-left: 3px solid #e5e7eb;
          cursor: pointer;
          transition: all 0.2s ease;
          display: flex;
          gap: 10px;
          border-radius: 0 8px 8px 0;

          &:hover {
            background: #fffbeb;
            border-left-color: #f59e0b;
            padding-left: 16px;
          }

          .rank {
            font-size: 20px;
            font-weight: bold;
            color: #f59e0b;
            line-height: 1;
            flex-shrink: 0;
          }

          .item-content {
            flex: 1;
            min-width: 0;

            h4 {
              font-size: 13px;
              font-weight: 500;
              color: #374151;
              margin: 0 0 6px 0;
              line-height: 1.4;
              overflow: hidden;
              text-overflow: ellipsis;
              display: -webkit-box;
              -webkit-line-clamp: 2;
              -webkit-box-orient: vertical;
            }

            .read-meta {
              font-size: 11px;
              color: #9ca3af;
              display: flex;
              align-items: center;
              gap: 4px;
            }
          }
        }
      }
    }

    /* 文章列表 */
    .article-list {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 16px;

      .article-card {
        background: white;
        border-radius: 16px;
        overflow: hidden;
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
        cursor: pointer;
        transition: all 0.3s ease;
        display: flex;
        height: 180px;

        &:hover {
          transform: translateY(-4px);
          box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
        }

        .card-cover {
          width: 200px;
          height: 100%;
          background: linear-gradient(135deg, #fef3c7, #fce7f3);
          flex-shrink: 0;
          position: relative;
          overflow: hidden;

          :deep(.el-image) {
            width: 100%;
            height: 100%;
          }

          .cover-default {
            width: 100%;
            height: 100%;
            display: flex;
            align-items: center;
            justify-content: center;

            .cover-icon {
              font-size: 48px;
            }
          }
        }

        .card-body {
          flex: 1;
          padding: 20px;
          display: flex;
          flex-direction: column;
          min-width: 0;

          .card-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;

            .publish-time {
              font-size: 12px;
              color: #9ca3af;
            }
          }

          .card-title {
            font-size: 18px;
            font-weight: 600;
            color: #1f2937;
            margin: 0 0 10px 0;
            line-height: 1.4;
            overflow: hidden;
            text-overflow: ellipsis;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
          }

          .card-summary {
            font-size: 14px;
            color: #6b7280;
            margin: 0 0 16px 0;
            line-height: 1.5;
            overflow: hidden;
            text-overflow: ellipsis;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            flex: 1;
          }

          .card-footer {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding-top: 12px;
            border-top: 1px solid #f3f4f6;

            .author-info {
              font-size: 13px;
              color: #6b7280;
              display: flex;
              align-items: center;
              gap: 6px;
            }

            .stat-info {
              .stat-item {
                font-size: 13px;
                color: #9ca3af;
                display: inline-flex;
                align-items: center;
                gap: 4px;
              }
            }
          }
        }
      }

      /* 空状态 */
      .empty-state {
        padding: 60px 20px;
      }

      /* 分页 */
      .pagination-wrapper {
        display: flex;
        justify-content: center;
        padding: 24px 0;
      }
    }
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .content {
    flex-direction: column;

    .sidebar {
      width: 100%;

      .filter-section {
        display: none;
      }
    }

    .article-list {
      .article-card {
        flex-direction: column;
        height: auto;

        .card-cover {
          width: 100%;
          height: 140px;
        }
      }
    }
  }

  .header-section {
    .header-content {
      .header-icon {
        width: 56px;
        height: 56px;
        font-size: 40px;
      }

      .header-text h1 {
        font-size: 24px;
      }
    }
  }

  .search-bar {
    flex-direction: column;
    margin: -20px auto 20px;
    padding: 0 16px;
  }
}
</style>
