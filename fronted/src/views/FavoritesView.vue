<template>
  <div class="favorites-page">
    <div class="page-head">
      <div class="head-left">
        <h2>我的收藏</h2>
        <span class="head-note" v-if="total">共 {{ total }} 篇</span>
      </div>
      <el-button text bg @click="router.push('/knowledge')">去知识库找文章</el-button>
    </div>

    <div class="fav-list" v-loading="loading">
      <div
        v-for="item in favorites"
        :key="item.id"
        class="fav-card"
        :class="{ gone: !item.available }"
        @click="goDetail(item)">
        <div class="fav-info">
          <div class="fav-title">
            {{ item.title || '文章已被删除' }}
            <el-tag v-if="!item.available" size="small" type="info" effect="plain">不可查看</el-tag>
          </div>
          <div class="fav-summary" v-if="item.summary">{{ item.summary }}</div>
          <div class="fav-meta">
            <el-tag v-if="item.category" size="small" effect="light">{{ item.category }}</el-tag>
            <span class="fav-time">收藏于 {{ shortDate(item.createdAt) }}</span>
          </div>
        </div>
        <el-button type="danger" size="small" text bg @click.stop="unfavorite(item)">取消收藏</el-button>
      </div>

      <el-empty v-if="!loading && !favorites.length" description="还没有收藏，去知识库挑几篇看看" />
    </div>

    <div class="pager" v-if="total > pageSize">
      <el-pagination
        background
        layout="prev, pager, next"
        :page-size="pageSize"
        :total="total"
        :current-page="pageNum"
        @current-change="changePage" />
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getFavoriteList, removeFavorite } from '@/api/frontend'

const router = useRouter()
const favorites = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadFavorites = async () => {
  loading.value = true
  try {
    const page = await getFavoriteList({ pageNum: pageNum.value, pageSize: pageSize.value })
    favorites.value = page?.records || []
    total.value = page?.total || 0
    // 取消收藏把当前页清空了就退一页
    if (!favorites.value.length && pageNum.value > 1) {
      pageNum.value--
      return loadFavorites()
    }
  } catch (e) {
    favorites.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const changePage = (p) => {
  pageNum.value = p
  loadFavorites()
}

const unfavorite = async (item) => {
  try {
    await removeFavorite(item.articleId)
    ElMessage.success('已取消收藏')
    loadFavorites()
  } catch (e) {
    // 拦截器已提示原因
  }
}

// 文章详情在 /knowledge/:id，之前写的 /user/knowledge/:id 没有这条路由，点了直接被兜底重定向回首页
const goDetail = (item) => {
  if (!item.available) {
    ElMessage.warning('这篇文章已下架或被删除')
    return
  }
  router.push(`/knowledge/${item.articleId}`)
}

const shortDate = (v) => (v ? String(v).replace('T', ' ').slice(0, 10) : '—')

onMounted(loadFavorites)
</script>
<style scoped lang="scss">
.favorites-page {
  max-width: 860px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;

  .head-left {
    display: flex;
    align-items: baseline;
    gap: 10px;

    h2 {
      margin: 0;
      font-size: 19px;
      color: #1f2329;
    }

    .head-note {
      font-size: 12.5px;
      color: #a8adb7;
    }
  }
}

.fav-list {
  min-height: 200px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.fav-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(31, 45, 61, 0.06);
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.3s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(64, 158, 255, 0.12);
  }

  &.gone {
    cursor: default;

    .fav-title {
      color: #a8adb7;
    }

    &:hover {
      transform: none;
      box-shadow: 0 2px 12px rgba(31, 45, 61, 0.06);
    }
  }
}

.fav-info {
  flex: 1;
  min-width: 0;
}

.fav-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15.5px;
  font-weight: 600;
  color: #1f2329;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fav-summary {
  margin: 6px 0 8px;
  font-size: 13px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fav-meta {
  display: flex;
  align-items: center;
  gap: 10px;

  .fav-time {
    font-size: 12.5px;
    color: #b7bcc6;
  }
}

.pager {
  display: flex;
  justify-content: center;
  padding: 20px 0 6px;
}
</style>

