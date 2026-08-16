<template>
  <div class="article-detail-container">
    <!-- 头部区域 -->
    <div class="header-section">
      <div class="header-content">
        <div class="back-btn" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
        </div>
        <div class="header-icon">📚</div>
        <div class="header-text">
          <h1>文章详情</h1>
          <p>深入学习金融法规知识</p>
        </div>
      </div>
    </div>

    <div class="content" v-if="article.id">
      <!-- 文章信息卡片 -->
      <div class="article-card">
        <div class="article-header">
          <el-tag size="large" effect="plain">{{ article.category }}</el-tag>
          <span class="publish-date">{{ formatDate(article.createdAt) }}</span>
        </div>
        <h1 class="article-title">{{ article.title }}</h1>

        <!-- 摘要 -->
        <div class="article-summary" v-if="article.summary">
          <el-icon><InfoFilled /></el-icon>
          <p>{{ article.summary }}</p>
        </div>

        <!-- 作者和统计信息 -->
        <div class="article-meta">
          <div class="meta-item">
            <el-icon><User /></el-icon>
            <span>{{ article.author || '管理员' }}</span>
          </div>
          <div class="meta-item">
            <el-icon><View /></el-icon>
            <span>{{ formatNumber(article.readCount || 0) }} 次阅读</span>
          </div>
          <div class="meta-item" v-if="article.updatedAt">
            <el-icon><Edit /></el-icon>
            <span>更新于 {{ formatUpdateTime(article.updatedAt) }}</span>
          </div>
        </div>
      </div>

      <!-- 正文内容 -->
      <div class="article-card content-card">
        <div class="content-wrapper" v-html="formatContent(article.content)"></div>
      </div>

      <!-- 标签区域 -->
      <div class="article-card tags-card" v-if="article.tagArray && article.tagArray.length">
        <div class="tags-title">
          <el-icon><PriceTag /></el-icon>
          相关标签
        </div>
        <div class="tags-list">
          <el-tag
            v-for="tag in article.tagArray"
            :key="tag"
            size="large"
            effect="light"
            class="tag-item">
            {{ tag }}
          </el-tag>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button type="primary" @click="toggleFavorite">
          <el-icon><Star :class="{ filled: favorited }" /></el-icon>
          {{ favorited ? '取消收藏' : '收藏文章' }}
        </el-button>
        <el-button @click="router.push('/knowledge')">
          <el-icon><List /></el-icon>
          返回列表
        </el-button>
        <el-button @click="handleShare">
          <el-icon><Share /></el-icon>
          分享文章
        </el-button>
      </div>

      <!-- 推荐阅读 -->
      <div class="recommend-section" v-if="recommendList.length">
        <div class="section-title">
          <el-icon><TrendCharts /></el-icon>
          相关推荐
        </div>
        <div class="recommend-grid">
          <div
            v-for="item in recommendList"
            :key="item.id"
            class="recommend-card"
            @click="goToArticle(item.id)">
            <div class="rec-cover" v-if="item.coverImage">
              <el-image :src="item.coverImage" fit="cover" lazy />
            </div>
            <div class="rec-info">
              <h4>{{ item.title }}</h4>
              <div class="rec-meta">
                <el-tag size="small" effect="plain">{{ item.category }}</el-tag>
                <span class="read-count">
                  <el-icon><View /></el-icon>
                  {{ item.readCount || 0 }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-else class="loading-state">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p>加载中...</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeRouteUpdate } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getKnowledgeDetail, getRecommendList } from '@/api/frontend';
import {
  ArrowLeft, User, View, Edit, Star, List, Share,
  Loading, InfoFilled, PriceTag, TrendCharts
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const article = ref({})
const favorited = ref(false)
const recommendList = ref([])

// 跳转文章详情
const goToArticle = (id) => {
  router.push(`/knowledge/${id}`)
}

// 切换收藏
const toggleFavorite = async () => {
  try {
    // 检查是否已登录
    const token = sessionStorage.getItem('token');
    if (!token) {
      ElMessage.warning('请先登录后操作');
      return;
    }

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
    if (e.response?.status === 401) {
      ElMessage.error('请先登录');
    } else {
      ElMessage.error('操作失败');
    }
  }
}

const loadArticle = async () => {
  try {
    const res = await getKnowledgeDetail(route.params.id);
    article.value = res;

    // 检查收藏状态（需要登录）
    const token = sessionStorage.getItem('token');
    if (token) {
      try {
        const checkRes = await axios.get(`/api/user/favorite/check/${route.params.id}`);
        console.log('收藏状态检查:', checkRes.data);
        favorited.value = checkRes.data.favorited || false;
      } catch (e) {
        console.log('检查收藏状态失败（可能未登录或文章未收藏）');
      }
    } else {
      console.log('未登录，跳过收藏状态检查');
    }

    // 加载相关文章（同一分类）
    loadRecommend(article.value.category);
  } catch (e) {
    console.error('加载文章失败:', e);
    ElMessage.error('加载文章失败');
  }
}

// 加载相关文章
const loadRecommend = async (category) => {
  try {
    const params = {
      pageNum: 1,
      pageSize: 4,
      category,
      sortField: 'readCount',
      sortDirection: 'desc'
    };
    const res = await getRecommendList(params);
    recommendList.value = (res.records || []).filter(a => a.id !== article.value.id);
  } catch (e) {
    console.error('加载推荐失败:', e);
  }
}

// 监听路由参数变化
onBeforeRouteUpdate((to, from) => {
  if (to.params.id !== from.params.id) {
    loadArticle();
  }
})

// 分享文章
const handleShare = () => {
  const url = window.location.href
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败，请手动复制链接')
  })
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

// 格式化更新时间
const formatUpdateTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

// 格式化数字
const formatNumber = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num
}

// 格式化内容
const formatContent = (content) => {
  if (!content) return ''

  let formatted = content

  // 处理标题
  formatted = formatted.replace(/^# (.*)$/gm, '<h1>$1</h1>')
  formatted = formatted.replace(/^## (.*)$/gm, '<h2>$1</h2>')
  formatted = formatted.replace(/^### (.*)$/gm, '<h3>$1</h3>')

  // 处理加粗和斜体
  formatted = formatted.replace(/\*\*\*(.*?)\*\*\*/g, '<strong><em>$1</em></strong>')
  formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
  formatted = formatted.replace(/\*(.*?)\*/g, '<em>$1</em>')

  // 处理引用
  formatted = formatted.replace(/^> (.*)$/gm, '<blockquote>$1</blockquote>')

  // 处理代码块
  formatted = formatted.replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
  formatted = formatted.replace(/`(.*?)`/g, '<code>$1</code>')

  // 处理列表
  formatted = formatted.replace(/^\- (.*)$/gm, '<li>$1</li>')
  formatted = formatted.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>')

  // 处理换行
  formatted = formatted.replace(/\n/g, '<br>')

  // 清理连续的 br 标签
  formatted = formatted.replace(/(<br>){3,}/g, '<br><br>')

  return formatted
}

onMounted(() => {
  console.log('文章详情页加载');
  console.log('当前参数:', route.params);
  loadArticle();
});
</script>

<style scoped lang="scss">
.article-detail-container {
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #fafbfc 0%, #f7f9fc 50%, #f2f6fa 100%);
  padding-bottom: 40px;

  /* 头部区域 */
  .header-section {
    background: linear-gradient(135deg, #f59e0b 0%, #8b5cf6 100%);
    padding: 32px 24px;
    margin-bottom: 24px;
    box-shadow: 0 8px 32px rgba(245, 158, 11, 0.15);

    .header-content {
      max-width: 980px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      gap: 16px;

      .back-btn {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 44px;
        height: 44px;
        background: rgba(255, 255, 255, 0.2);
        border-radius: 12px;
        cursor: pointer;
        transition: all 0.3s ease;

        &:hover {
          background: rgba(255, 255, 255, 0.3);
        }
      }

      .header-icon {
        font-size: 44px;
        background: rgba(255, 255, 255, 0.2);
        width: 60px;
        height: 60px;
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .header-text {
        h1 {
          font-size: 24px;
          font-weight: 700;
          color: white;
          margin: 0;
        }

        p {
          font-size: 14px;
          color: rgba(255, 255, 255, 0.9);
          margin: 4px 0 0 0;
        }
      }
    }
  }

  /* 主体内容 */
  .content {
    max-width: 980px;
    margin: 0 auto;
    padding: 0 24px;

    .article-card {
      background: white;
      border-radius: 16px;
      padding: 28px 32px;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
      margin-bottom: 20px;
    }

    /* 文章头部 */
    .article-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;

      .publish-date {
        font-size: 13px;
        color: #9ca3af;
      }
    }

    .article-title {
      font-size: 32px;
      font-weight: 700;
      color: #111827;
      margin: 0 0 20px 0;
      line-height: 1.4;
    }

    .article-summary {
      background: linear-gradient(135deg, rgba(126, 211, 33, 0.08), rgba(126, 211, 33, 0.04));
      border-left: 4px solid #7ed321;
      border-radius: 0 12px 12px 0;
      padding: 16px 20px;
      margin-bottom: 24px;
      display: flex;
      gap: 12px;
      align-items: flex-start;

      .el-icon {
        color: #7ed321;
        font-size: 18px;
        margin-top: 2px;
      }

      p {
        margin: 0;
        font-size: 15px;
        color: #374151;
        line-height: 1.7;
      }
    }

    .article-meta {
      display: flex;
      flex-wrap: wrap;
      gap: 24px;
      padding-top: 20px;
      border-top: 1px solid #f3f4f6;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 14px;
        color: #6b7280;

        .el-icon {
          color: #9ca3af;
        }
      }
    }

    /* 内容区域 */
    .content-card {
      padding: 32px;

      .content-wrapper {
        font-size: 16px;
        color: #374151;
        line-height: 1.8;

        :deep(h1) {
          font-size: 28px;
          font-weight: 700;
          color: #111827;
          margin: 32px 0 16px;
          line-height: 1.4;
        }

        :deep(h2) {
          font-size: 24px;
          font-weight: 600;
          color: #1f2937;
          margin: 28px 0 14px;
          padding-bottom: 10px;
          border-bottom: 2px solid #f3f4f6;
        }

        :deep(h3) {
          font-size: 20px;
          font-weight: 600;
          color: #374151;
          margin: 24px 0 12px;
        }

        :deep(p) {
          margin-bottom: 18px;
          text-align: justify;
        }

        :deep(strong) {
          font-weight: 600;
          color: #1f2937;
        }

        :deep(em) {
          font-style: italic;
        }

        :deep(blockquote) {
          border-left: 4px solid #f59e0b;
          background: linear-gradient(135deg, rgba(245, 158, 11, 0.05), rgba(245, 158, 11, 0.02));
          padding: 16px 20px;
          margin: 20px 0;
          border-radius: 0 12px 12px 0;
          color: #6b7280;
          font-style: italic;
        }

        :deep(pre) {
          background: #1f2937;
          color: #f3f4f6;
          padding: 16px;
          border-radius: 12px;
          overflow-x: auto;
          margin: 20px 0;
        }

        :deep(code) {
          font-family: 'Courier New', monospace;
          font-size: 0.9em;
        }

        :deep(.inline-code) {
          background: #f3f4f6;
          padding: 2px 8px;
          border-radius: 6px;
          color: #e11d48;
        }

        :deep(ul),
        :deep(ol) {
          padding-left: 28px;
          margin-bottom: 18px;

          li {
            margin-bottom: 10px;
            line-height: 1.7;
          }
        }
      }
    }

    /* 标签区域 */
    .tags-card {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .tags-title {
        font-size: 14px;
        font-weight: 600;
        color: #374151;
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .tags-list {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;

        .tag-item {
          cursor: pointer;
          transition: all 0.2s ease;

          &:hover {
            transform: translateY(-2px);
          }
        }
      }
    }

    /* 操作按钮 */
    .action-buttons {
      display: flex;
      gap: 12px;
      margin-top: 24px;
      flex-wrap: wrap;

      .el-button {
        display: flex;
        align-items: center;
        gap: 8px;
        border-radius: 12px;
        padding: 12px 24px;
      }
    }

    /* 推荐阅读 */
    .recommend-section {
      margin-top: 32px;

      .section-title {
        font-size: 18px;
        font-weight: 600;
        color: #374151;
        margin-bottom: 20px;
        display: flex;
        align-items: center;
        gap: 10px;
      }

      .recommend-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 16px;

        .recommend-card {
          background: white;
          border-radius: 12px;
          overflow: hidden;
          box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
          cursor: pointer;
          transition: all 0.3s ease;
          display: flex;
          flex-direction: column;

          &:hover {
            transform: translateY(-4px);
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
          }

          .rec-cover {
            height: 120px;
            background: linear-gradient(135deg, #fef3c7, #fce7f3);

            :deep(.el-image) {
              width: 100%;
              height: 100%;
            }
          }

          .rec-info {
            flex: 1;
            padding: 16px;

            h4 {
              font-size: 15px;
              font-weight: 600;
              color: #1f2937;
              margin: 0 0 12px 0;
              line-height: 1.4;
              overflow: hidden;
              text-overflow: ellipsis;
              display: -webkit-box;
              -webkit-line-clamp: 2;
              -webkit-box-orient: vertical;
            }

            .rec-meta {
              display: flex;
              justify-content: space-between;
              align-items: center;

              .read-count {
                font-size: 12px;
                color: #9ca3af;
                display: flex;
                align-items: center;
                gap: 4px;
              }
            }
          }
        }
      }
    }

    /* 加载状态 */
    .loading-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 80px 20px;
      color: #9ca3af;

      .is-loading {
        animation: rotating 2s infinite;
        color: #f59e0b;
        margin-bottom: 16px;
      }
    }
  }
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 响应式 */
@media (max-width: 768px) {
  .content {
    padding: 0 16px;

    .article-card {
      padding: 20px;
    }

    .content-card {
      padding: 20px;

      .content-wrapper {
        font-size: 15px;

        :deep(h1) { font-size: 22px; }
        :deep(h2) { font-size: 20px; }
        :deep(h3) { font-size: 18px; }
      }
    }

    .article-title {
      font-size: 24px;
    }

    .recommend-grid {
      grid-template-columns: 1fr;
    }

    .action-buttons {
      .el-button {
        flex: 1;
        justify-content: center;
      }
    }
  }

  .header-section {
    .header-content {
      .header-icon {
        width: 50px;
        height: 50px;
        font-size: 36px;
      }

      .header-text h1 {
        font-size: 20px;
      }
    }
  }
}
</style>
