<template>
  <div class="knowledge-container">
    <!-- 细头部：原来是 40px padding 的渐变 banner，太抢戏。标题和搜索并到一行 -->
    <div class="page-head">
      <div class="head-inner">
        <div class="head-title">
          <h1>金融法规知识库</h1>
          <span class="subtitle">投资知识与交易规则</span>
        </div>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索标题、摘要或标签"
          clearable
          class="head-search"
          :prefix-icon="Search"
          @keydown.enter="handleSearch"
          @clear="handleSearch" />
      </div>
    </div>

    <div class="content">
      <!-- 左侧目录：靠字号和灰阶分层级，不用卡片和渐变 -->
      <aside class="sidebar">
        <nav class="nav-group">
          <div class="group-label">分类</div>
          <div
            v-for="cat in categories"
            :key="cat"
            class="nav-item"
            :class="{ active: category === cat }"
            @click="selectCategory(cat)">
            {{ cat }}
          </div>
        </nav>

        <div class="nav-group" v-if="recommendList.length">
          <div class="group-label">热门阅读</div>
          <div
            v-for="(item, index) in recommendList"
            :key="item.id"
            class="hot-item"
            :title="item.title"
            @click="goDetail(item.id)">
            <span class="hot-rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
            <span class="hot-title">{{ item.title }}</span>
          </div>
        </div>
      </aside>

      <main class="main">
        <!-- 从投资日记的偏差标签跳进来时说明来路，否则用户不知道搜索框里的词是哪来的 -->
        <div class="from-bar" v-if="fromBias">
          <el-icon><WarningFilled /></el-icon>
          <span>你的投资日记里出现过 <b>{{ fromBias }}</b>，这些内容讲的是相关主题</span>
          <span class="from-back" @click="router.push('/diary')">回到日记</span>
        </div>
        <div class="list-toolbar">
          <span class="result-count">
            共 <b>{{ total }}</b> 篇
            <template v-if="category !== '全部'"> · {{ category }}</template>
            <template v-if="searchKeyword"> · 匹配「{{ searchKeyword }}」</template>
          </span>
          <span class="clear-filter" v-if="category !== '全部' || searchKeyword" @click="resetFilter">
            清除筛选
          </span>
        </div>
        <!-- 文档列表：整段包在一张白卡里，行之间用 1px 分割线，比一堆独立卡片安静 -->
        <div class="doc-list" v-if="articles.length">
          <div
            v-for="article in articles"
            :key="article.id"
            class="doc-row"
            @click="goDetail(article.id)">
            <div class="doc-main">
              <h3 class="doc-title" v-html="highlight(article.title)"></h3>
              <p class="doc-summary" v-html="highlight(article.summary || '暂无摘要，点击查看全文')"></p>
              <div class="doc-meta">
                <span class="meta-cat" v-if="article.category">{{ article.category }}</span>
                <span>{{ article.author || '管理员' }}</span>
                <span>{{ displayDate(article) }}</span>
                <span class="meta-read">
                  <el-icon><View /></el-icon>
                  {{ formatNumber(article.readCount || 0) }}
                </span>
              </div>
            </div>
            <div class="doc-thumb" v-if="article.coverImage">
              <el-image :src="article.coverImage" fit="cover" lazy>
                <template #error>
                  <div class="thumb-fallback">📄</div>
                </template>
              </el-image>
            </div>
          </div>
        </div>

        <el-empty
          v-else
          :image-size="100"
          :description="emptyText" />

        <div class="pagination-wrapper" v-if="total > pageSize">
          <el-pagination
            background
            layout="prev, pager, next"
            :page-size="pageSize"
            :total="total"
            :current-page="pageNum"
            @current-change="handlePageChange" />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getKnowledgeList, getArticleCategories } from '@/api/frontend'
import { Search, View, WarningFilled } from '@element-plus/icons-vue'

const router = useRouter()

const articles = ref([])
const recommendList = ref([])
const categories = ref(['全部'])
const category = ref('全部')
const fromBias = ref('')
const searchKeyword = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const emptyText = computed(() => {
  if (fromBias.value) return `知识库里还没有讲「${searchKeyword.value}」的文章，可以先看看其他主题`
  return searchKeyword.value ? '换个关键词试试' : '暂无文章'
})

// 分类来自数据库，后台新增分类前台立刻能筛
const loadCategories = async () => {
  try {
    const res = await getArticleCategories()
    categories.value = ['全部', ...(res || [])]
  } catch (e) {
    console.error('加载分类失败:', e)
  }
}

const loadArticles = async () => {
  try {
    const res = await getKnowledgeList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      category: category.value !== '全部' ? category.value : undefined,
      keyword: searchKeyword.value || undefined,
      sortField: 'publishedAt',
      sortDirection: 'desc'
    })
    articles.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error('加载文章失败:', e)
  }
}

// 热门只放 5 条、标题单行，保证侧栏在 768p 的笔记本上也能一眼看全，不用滚
const loadRecommend = async () => {
  try {
    const res = await getKnowledgeList({
      pageNum: 1,
      pageSize: 5,
      sortField: 'readCount',
      sortDirection: 'desc'
    })
    recommendList.value = res.records || []
  } catch (e) {
    console.error('加载推荐失败:', e)
  }
}

const selectCategory = (cat) => {
  category.value = cat
  pageNum.value = 1
  loadArticles()
}

const handleSearch = () => {
  pageNum.value = 1
  loadArticles()
}

const resetFilter = () => {
  category.value = '全部'
  searchKeyword.value = ''
  fromBias.value = ''
  pageNum.value = 1
  loadArticles()
}

const handlePageChange = (page) => {
  pageNum.value = page
  loadArticles()
}

const goDetail = (id) => {
  router.push(`/knowledge/${id}`)
}

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

// 草稿可能建了很久才发布，列表显示发布时间更合理
const displayDate = (article) => formatDate(article.publishedAt || article.createdAt)

// 搜索命中处高亮。先转义再插标签，否则标题里的尖括号会被当成 HTML
const highlight = (text) => {
  const raw = String(text ?? '')
  const escaped = raw.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const kw = searchKeyword.value.trim()
  if (!kw) return escaped
  const safeKw = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return escaped.replace(new RegExp(safeKw, 'gi'), (m) => `<mark>${m}</mark>`)
}

const formatNumber = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num
}

watch(() => router.currentRoute.value.query.keyword, (val) => {
  if (val) {
    searchKeyword.value = String(val)
    fromBias.value = String(router.currentRoute.value.query.bias || '')
    handleSearch()
  }
})

// 风险测评结果页的「推荐学习路径」带 category 跳过来，直接落到对应分类
watch(() => router.currentRoute.value.query.category, (val) => {
  if (val) selectCategory(val)
})

onMounted(() => {
  // 之前只在 onMounted 读了 category：keyword 靠 watch，而 watch 在首次挂载不触发，
  // 从日记跳过来时搜索词会被丢掉，所以这里一并读出来
  const q = router.currentRoute.value.query
  if (q.category) category.value = String(q.category)
  if (q.keyword) searchKeyword.value = String(q.keyword)
  if (q.bias) fromBias.value = String(q.bias)
  loadCategories()
  loadArticles()
  loadRecommend()
})
</script>

<style scoped lang="scss">
/* 配色跟首页对齐：蓝为主，绿橙点缀，文字走 #333/#666/#999 */
$brand: #409eff;
$green: #67c23a;
$orange: #e6a23c;
$line: rgba(64, 158, 255, 0.15);

/* 整页不滚：高度锁死一屏，只让文章列表区域内部滚动 */
.knowledge-container {
  position: relative;
  height: calc(100vh - 60px);
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  /* 首页同款网格底纹 */
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(rgba(64, 158, 255, 0.05) 1px, transparent 1px),
      linear-gradient(90deg, rgba(64, 158, 255, 0.05) 1px, transparent 1px);
    background-size: 72px 72px;
    mask-image: radial-gradient(ellipse 90% 80% at 50% 30%, #000 30%, transparent 80%);
    pointer-events: none;
    z-index: 0;
  }

  :deep(mark) {
    background: rgba(64, 158, 255, 0.2);
    color: inherit;
    border-radius: 2px;
    padding: 0 1px;
  }
}
.knowledge-container .page-head {
  position: relative;
  z-index: 2;
  flex-shrink: 0;
  border-bottom: 1px solid $line;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);

  /* 横向内边距用 vw，和首页 sec-inner 的 7vw 一致，页面才铺满浏览器 */
  .head-inner {
    padding: 16px 7vw;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
  }

  .head-title {
    display: flex;
    align-items: baseline;
    gap: 12px;
    min-width: 0;

    h1 {
      font-size: 22px;
      font-weight: 800;
      color: #333;
      margin: 0;
      white-space: nowrap;
    }

    .subtitle {
      font-size: 13px;
      color: #999;
      white-space: nowrap;
    }
  }

  .head-search {
    width: 300px;
    flex-shrink: 0;

    :deep(.el-input__wrapper) {
      border-radius: 12px;
      box-shadow: 0 0 0 1px $line inset;

      &:hover,
      &.is-focus {
        box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.55) inset;
      }
    }
  }
}
/* 这一层负责吃掉剩余高度，min-height:0 少了子元素就撑破容器 */
.knowledge-container .content {
  position: relative;
  z-index: 2;
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: stretch;
  gap: 40px;
  padding: 0 7vw;
}

.knowledge-container .sidebar {
  width: 190px;
  flex-shrink: 0;
  padding: 24px 0;
  overflow-y: auto;

  .nav-group + .nav-group {
    margin-top: 18px;
    padding-top: 14px;
    border-top: 1px solid $line;
  }

  .group-label {
    font-size: 12px;
    font-weight: 600;
    color: #999;
    letter-spacing: 2px;
    margin-bottom: 10px;
    padding-left: 12px;
  }

  .nav-item {
    padding: 8px 12px;
    border-radius: 10px;
    font-size: 14px;
    color: #666;
    cursor: pointer;
    transition: background 0.25s cubic-bezier(0.22, 1, 0.36, 1), color 0.25s ease;

    &:hover {
      background: rgba(64, 158, 255, 0.06);
      color: #333;
    }

    &.active {
      background: rgba(64, 158, 255, 0.12);
      color: $brand;
      font-weight: 600;
    }
  }
}
.knowledge-container .sidebar .hot-item {
  display: flex;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 10px;
  cursor: pointer;

  &:hover {
    background: rgba(103, 194, 58, 0.07);

    .hot-title {
      color: $brand;
    }
  }

  .hot-rank {
    font-size: 12px;
    font-weight: 700;
    color: #ccc;
    line-height: 1.6;
    flex-shrink: 0;
    width: 12px;

    &.top {
      color: $orange;
    }
  }

  /* 标题收成单行：两行时 5 条就占 280px，矮屏必然要滚。截断的全称挂在 title 上 */
  .hot-title {
    font-size: 13px;
    color: #666;
    line-height: 1.6;
    min-width: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    transition: color 0.25s ease;
  }
}
/* 唯一的滚动容器；文章少的时候这里不会出滚动条 */
.knowledge-container .main {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: 24px 0 32px;

  .from-bar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 14px;
    padding: 10px 14px;
    border-radius: 10px;
    font-size: 13px;
    color: #8a6420;
    background: rgba(230, 162, 60, 0.1);
    border: 1px solid rgba(230, 162, 60, 0.28);

    .el-icon { color: $orange; }
    b { color: #b8801f; }

    .from-back {
      margin-left: auto;
      flex-shrink: 0;
      color: $brand;
      cursor: pointer;

      &:hover { text-decoration: underline; }
    }
  }

  .list-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 14px;
    font-size: 13px;
    color: #999;

    .result-count b {
      color: $brand;
      font-weight: 800;
      font-size: 15px;
      margin: 0 3px;
    }

    .clear-filter {
      color: $brand;
      cursor: pointer;
      flex-shrink: 0;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  .doc-list {
    border: 1px solid rgba(64, 158, 255, 0.12);
    border-radius: 20px;
    background: rgba(255, 255, 255, 0.9);
    box-shadow: 0 10px 30px rgba(64, 158, 255, 0.08);
    overflow: hidden;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: center;
    padding: 24px 0 0;
  }
}
.knowledge-container .doc-row {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 20px 24px;
  cursor: pointer;
  transition: background 0.25s cubic-bezier(0.22, 1, 0.36, 1);

  & + .doc-row {
    border-top: 1px solid rgba(64, 158, 255, 0.1);
  }

  &:hover {
    background: rgba(64, 158, 255, 0.04);

    .doc-title {
      color: $brand;
    }
  }

  .doc-main {
    flex: 1;
    min-width: 0;
  }

  /* 高度不写死，标题该占两行就占两行，不会被裁 */
  .doc-title {
    font-size: 16px;
    font-weight: 700;
    color: #333;
    line-height: 1.55;
    margin: 0 0 6px;
    transition: color 0.25s ease;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .doc-summary {
    font-size: 13px;
    color: #999;
    line-height: 1.7;
    margin: 0 0 10px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}
.knowledge-container .doc-row {
  /* meta 串成一行，比一排彩色标签干净 */
  .doc-meta {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 14px;
    font-size: 12px;
    color: #999;

    .meta-cat {
      color: $green;
      background: rgba(103, 194, 58, 0.1);
      border: 1px solid rgba(103, 194, 58, 0.2);
      border-radius: 6px;
      padding: 2px 9px;
    }

    .meta-read {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
  }

  /* 固定 16:9，配合 fit="cover" 让任何尺寸的原图都裁成同一个框 */
  .doc-thumb {
    width: 128px;
    aspect-ratio: 16 / 9;
    border-radius: 12px;
    overflow: hidden;
    flex-shrink: 0;
    background: rgba(64, 158, 255, 0.06);

    :deep(.el-image) {
      width: 100%;
      height: 100%;
    }

    .thumb-fallback {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22px;
      opacity: 0.45;
    }
  }
}
/* 窄屏：整页恢复正常纵向滚动，目录横向滑动，缩略图隐藏 */
@media (max-width: 900px) {
  .knowledge-container {
    height: auto;
    min-height: calc(100vh - 60px);
    overflow: visible;

    .page-head .head-inner {
      flex-direction: column;
      align-items: stretch;
      gap: 12px;
      padding: 14px 20px;
    }

    .head-search {
      width: 100%;
    }

    .content {
      flex-direction: column;
      gap: 0;
      padding: 0 20px;
    }

    .sidebar {
      width: 100%;
      padding: 16px 0 0;
      overflow: visible;

      .nav-group:first-child {
        display: flex;
        gap: 8px;
        overflow-x: auto;

        .group-label {
          display: none;
        }

        .nav-item {
          flex-shrink: 0;
          background: rgba(64, 158, 255, 0.06);
        }
      }

      .nav-group + .nav-group {
        display: none;
      }
    }

    .main {
      overflow: visible;
      padding: 16px 0 32px;
    }

    .doc-thumb {
      display: none;
    }
  }
}
</style>
