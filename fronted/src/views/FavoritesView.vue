<template>
  <div class="favorites-page">
    <div class="page-header">
      <h2>我的收藏</h2>
    </div>

    <div v-if="favorites.length > 0" class="fav-list">
      <el-card
        v-for="item in favorites"
        :key="item.id"
        class="fav-card"
        shadow="hover">
        <div class="fav-info" @click="goDetail(item.articleId)">
          <div class="fav-title">{{ item.title || '未知文章' }}</div>
          <div class="fav-meta">
            <el-tag size="small">{{ item.category }}</el-tag>
            <span class="fav-time">{{ formatDate(item.createdAt) }}</span>
          </div>
        </div>
        <el-button
          type="danger"
          size="small"
          text
          @click="unfavorite(item)">
          取消收藏
        </el-button>
      </el-card>
    </div>

    <el-empty v-else description="暂无收藏" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const favorites = ref([])

const loadFavorites = async () => {
  try {
    const res = await axios.get('/api/user/favorite/list')
    favorites.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const unfavorite = async (item) => {
  try {
    await axios.delete(`/api/user/favorite/${item.articleId}`)
    ElMessage.success('已取消收藏')
    loadFavorites()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const goDetail = (articleId) => {
  router.push(`/user/knowledge/${articleId}`)
}

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(loadFavorites)
</script>

<style scoped>
.favorites-page {
  max-width: 800px;
  margin: 0 auto;
}
.page-header {
  margin-bottom: 20px;
}
.fav-card {
  margin-bottom: 12px;
}
.fav-card :deep(.el-card__body) {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.fav-info {
  flex: 1;
  cursor: pointer;
}
.fav-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fav-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}
.fav-time {
  color: #999;
  font-size: 13px;
}
</style>