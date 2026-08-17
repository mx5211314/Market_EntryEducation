<template>
  <div class="article-detail-container">
    <!-- 细头部：跟知识库列表页同一条白底导航条，不再用大渐变 banner -->
    <div class="page-head">
      <div class="head-inner">
        <div class="back-btn" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </div>
        <span class="head-sep">/</span>
        <span class="head-crumb" @click="router.push('/knowledge')">金融法规知识库</span>
        <span class="head-sep" v-if="article.category">/</span>
        <span class="head-current" v-if="article.category">{{ article.category }}</span>
      </div>
    </div>

    <div class="content" v-if="article.id">
      <!-- 文章信息卡片 -->
      <div class="article-card">
        <div class="cover-banner" v-if="article.coverImage">
          <el-image :src="article.coverImage" fit="cover">
            <template #error>
              <div class="cover-fallback">封面加载失败</div>
            </template>
          </el-image>
        </div>
        <h1 class="article-title">{{ article.title }}</h1>
        <div class="article-header">
          <span class="publish-date">{{ formatDate(article.publishedAt || article.createdAt) }}</span>
          <span class="dot">·</span>
          <span>{{ article.author || '管理员' }}</span>
          <span class="dot">·</span>
          <span>{{ formatNumber(article.readCount || 0) }} 次阅读</span>
          <template v-if="article.updatedAt">
            <span class="dot">·</span>
            <span>更新于 {{ formatUpdateTime(article.updatedAt) }}</span>
          </template>
        </div>

        <!-- 摘要 -->
        <div class="article-summary" v-if="article.summary">
          <el-icon><InfoFilled /></el-icon>
          <p>{{ article.summary }}</p>
        </div>
      </div>

      <!-- 正文内容 -->
      <div class="article-card content-card">
        <!-- 后台用 wangEditor 存的是 HTML，SQL 里灌的种子文章是 Markdown，两种都要能渲染 -->
        <div v-if="isHtmlContent" class="content-wrapper" v-html="safeContent"></div>
        <MarkdownRenderer v-else class="content-wrapper" :content="article.content" />
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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteUpdate } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getKnowledgeDetail, getRecommendList,
  checkFavorite, addFavorite, removeFavorite
} from '@/api/frontend';
import DOMPurify from 'dompurify'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import {
  ArrowLeft, View, Star, List, Share,
  Loading, InfoFilled, PriceTag, TrendCharts
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const article = ref({})
const favorited = ref(false)
const recommendList = ref([])

const isHtmlContent = computed(() => /<(p|div|h[1-6]|ul|ol|img|blockquote|pre)[\s>]/i.test(article.value.content || ''))

// 正文是后台富文本编辑器存的原始 HTML，必须过一遍消毒再进 v-html，否则文章内容就是 XSS 入口。
// 不禁用 style：wangEditor 的排版靠内联样式，DOMPurify 本身会清掉里面的危险声明
const safeContent = computed(() => DOMPurify.sanitize(article.value.content || '', {
  ADD_ATTR: ['target'],
  FORBID_TAGS: ['form', 'input', 'button']
}))

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
      await removeFavorite(route.params.id)
      favorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(route.params.id)
      favorited.value = true
      ElMessage.success('收藏成功')
    }
  } catch (e) {
    // 拦截器已提示后端返回的原因
  }
}

const loadArticle = async () => {
  try {
    const res = await getKnowledgeDetail(route.params.id);
    article.value = res;

    // 检查收藏状态（需要登录）
    if (sessionStorage.getItem('token')) {
      try {
        const checkRes = await checkFavorite(route.params.id);
        favorited.value = checkRes?.favorited || false;
      } catch (e) {
        favorited.value = false;
      }
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

onMounted(() => {
  console.log('文章详情页加载');
  console.log('当前参数:', route.params);
  loadArticle();
});
</script>

<style scoped lang="scss">
/* 配色与首页、知识库列表页统一 */
$brand: #409eff;
$green: #67c23a;
$orange: #e6a23c;
$line: rgba(64, 158, 255, 0.15);

.article-detail-container {
  position: relative;
  min-height: calc(100vh - 60px);
  background: #fff;
  padding-bottom: 56px;

  &::before {
    content: '';
    position: fixed;
    inset: 60px 0 0;
    background-image:
      linear-gradient(rgba(64, 158, 255, 0.05) 1px, transparent 1px),
      linear-gradient(90deg, rgba(64, 158, 255, 0.05) 1px, transparent 1px);
    background-size: 72px 72px;
    mask-image: radial-gradient(ellipse 90% 80% at 50% 30%, #000 30%, transparent 80%);
    pointer-events: none;
    z-index: 0;
  }
}
/* 面包屑式细头部 */
.article-detail-container .page-head {
  position: sticky;
  top: 0;
  z-index: 5;
  border-bottom: 1px solid $line;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  margin-bottom: 28px;

  .head-inner {
    max-width: 860px;
    margin: 0 auto;
    padding: 14px 24px;
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 13px;
    color: #999;
  }

  .back-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    color: $brand;
    cursor: pointer;

    &:hover {
      opacity: 0.75;
    }
  }

  .head-crumb {
    color: #666;
    cursor: pointer;

    &:hover {
      color: $brand;
    }
  }

  .head-current {
    color: #333;
  }

  .head-sep {
    color: #ddd;
  }
}
/* 正文宽度收到 860，中文长行更好读 */
.article-detail-container .content {
  position: relative;
  z-index: 1;
  max-width: 860px;
  margin: 0 auto;
  padding: 0 24px;

  .article-card {
    background: rgba(255, 255, 255, 0.9);
    border: 1px solid rgba(64, 158, 255, 0.12);
    border-radius: 20px;
    padding: 28px 32px;
    box-shadow: 0 10px 30px rgba(64, 158, 255, 0.08);
    margin-bottom: 20px;
  }

  /* 16:9 撑满正文宽度，服务端已经把封面统一裁成这个比例，这里不会再二次裁剪 */
  .cover-banner {
    aspect-ratio: 16 / 9;
    border-radius: 14px;
    overflow: hidden;
    margin-bottom: 22px;
    background: rgba(64, 158, 255, 0.06);

    :deep(.el-image) {
      width: 100%;
      height: 100%;
    }

    .cover-fallback {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      color: #bbb;
    }
  }

  .article-title {
    font-size: 30px;
    font-weight: 800;
    color: #333;
    margin: 0 0 14px;
    line-height: 1.35;
  }

  /* 作者、日期、阅读数串成一行灰字，不再各占一个图标块 */
  .article-header {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    font-size: 13px;
    color: #999;

    .dot {
      color: #ddd;
    }
  }
}
.article-detail-container .content {
  .article-summary {
    background: rgba(103, 194, 58, 0.06);
    border-left: 3px solid $green;
    border-radius: 0 12px 12px 0;
    padding: 14px 18px;
    margin: 20px 0 0;
    display: flex;
    gap: 12px;
    align-items: flex-start;

    .el-icon {
      color: $green;
      font-size: 17px;
      margin-top: 3px;
      flex-shrink: 0;
    }

    p {
      margin: 0;
      font-size: 14px;
      color: #666;
      line-height: 1.8;
    }
  }

  .tags-card {
    display: flex;
    align-items: center;
    gap: 14px;

    .tags-title {
      font-size: 13px;
      font-weight: 600;
      color: #999;
      display: flex;
      align-items: center;
      gap: 6px;
      flex-shrink: 0;
    }

    .tags-list {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
  }
}
.article-detail-container .content-card {
  padding: 32px 36px;

  .content-wrapper {
    font-size: 16px;
    color: #444;
    line-height: 1.9;

    :deep(h1) {
      font-size: 26px;
      font-weight: 800;
      color: #333;
      margin: 32px 0 16px;
      line-height: 1.4;
    }

    :deep(h2) {
      font-size: 22px;
      font-weight: 700;
      color: #333;
      margin: 28px 0 14px;
      padding-left: 12px;
      border-left: 3px solid $brand;
    }

    :deep(h3) {
      font-size: 18px;
      font-weight: 700;
      color: #333;
      margin: 24px 0 12px;
    }

    :deep(p) {
      margin: 0 0 18px;
    }

    :deep(strong) {
      font-weight: 700;
      color: #333;
    }

    :deep(img) {
      max-width: 100%;
      border-radius: 12px;
      margin: 12px 0;
    }
  }
}
.article-detail-container .content-card .content-wrapper {
  :deep(blockquote) {
    border-left: 3px solid $brand;
    background: rgba(64, 158, 255, 0.05);
    padding: 14px 18px;
    margin: 20px 0;
    border-radius: 0 12px 12px 0;
    color: #666;
  }

  :deep(pre) {
    background: #2b303b;
    color: #e6e9ef;
    padding: 16px;
    border-radius: 12px;
    overflow-x: auto;
    margin: 20px 0;
    font-size: 13px;
    line-height: 1.7;
  }

  :deep(code) {
    font-family: 'Courier New', monospace;
    font-size: 0.9em;
  }

  :deep(.inline-code) {
    background: rgba(64, 158, 255, 0.1);
    padding: 2px 7px;
    border-radius: 5px;
    color: $brand;
  }

  :deep(ul),
  :deep(ol) {
    padding-left: 26px;
    margin: 0 0 18px;

    li {
      margin-bottom: 8px;
      line-height: 1.85;
    }
  }

  :deep(a) {
    color: $brand;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}
.article-detail-container .content {
  .action-buttons {
    display: flex;
    gap: 12px;
    margin-top: 4px;
    flex-wrap: wrap;

    .el-button {
      display: flex;
      align-items: center;
      gap: 6px;
      border-radius: 12px;
      padding: 12px 22px;
    }
  }

  .recommend-section {
    margin-top: 36px;

    .section-title {
      font-size: 16px;
      font-weight: 700;
      color: #333;
      margin-bottom: 16px;
      display: flex;
      align-items: center;
      gap: 8px;

      .el-icon {
        color: $orange;
      }
    }

    .recommend-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 16px;
    }
  }
}
.article-detail-container .recommend-card {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(64, 158, 255, 0.12);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.06);
  cursor: pointer;
  transition: transform 0.4s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.4s ease;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 18px 40px rgba(64, 158, 255, 0.14);

    h4 {
      color: $brand;
    }
  }

  .rec-cover {
    aspect-ratio: 16 / 9;
    background: rgba(64, 158, 255, 0.06);

    :deep(.el-image) {
      width: 100%;
      height: 100%;
    }
  }

  .rec-info {
    flex: 1;
    padding: 14px 16px;

    h4 {
      font-size: 14px;
      font-weight: 700;
      color: #333;
      margin: 0 0 10px;
      line-height: 1.5;
      transition: color 0.25s ease;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .rec-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .read-count {
        font-size: 12px;
        color: #999;
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}
.article-detail-container .loading-state {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
  color: #999;

  .is-loading {
    animation: rotating 1.4s linear infinite;
    color: $brand;
    margin-bottom: 14px;
  }
}

@keyframes rotating {
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .article-detail-container {
    .page-head .head-inner {
      padding: 12px 16px;
    }

    .content {
      padding: 0 16px;

      .article-card {
        padding: 20px;
      }

      .article-title {
        font-size: 23px;
      }

      .recommend-grid {
        grid-template-columns: 1fr;
      }

      .action-buttons .el-button {
        flex: 1;
        justify-content: center;
      }
    }

    .content-card {
      padding: 20px;

      .content-wrapper {
        font-size: 15px;

        :deep(h1) { font-size: 21px; }
        :deep(h2) { font-size: 19px; }
        :deep(h3) { font-size: 17px; }
      }
    }
  }
}
</style>
