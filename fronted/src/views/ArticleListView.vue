<template>
  <div class="article-page">
    <h2>📚 法规科普知识库</h2>
    <el-select
      v-model="category"
      placeholder="全部分类"
      style="width: 200px; margin-bottom: 20px"
      @change="loadArticles">
      <el-option label="全部分类" value="" />
      <el-option label="证券交易规则" value="证券交易规则" />
      <el-option label="融资融券" value="融资融券" />
      <el-option label="投资者适当性" value="投资者适当性" />
    </el-select>

    <el-row :gutter="20">
      <el-col v-for="article in articles" :key="article.id" :span="8">
        <el-card class="article-card" @click="goDetail(article.id)">
          <h3>{{ article.title }}</h3>
          <p class="article-meta">
            {{ article.category }} · {{ formatDate(article.createdAt) }}
          </p>
        </el-card>
      </el-col>
    </el-row>

    <el-pagination
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="pageNum"
      @current-change="handlePageChange"
      style="margin-top: 20px; justify-content: center" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

const router = useRouter()
const articles = ref([])
const category = ref('')
const pageNum = ref(1)
const pageSize = ref(9)
const total = ref(0)

const loadArticles = async () => {
  const res = await axios.get('/api/article/list', {
    params: {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      category: category.value,
    },
  })
  articles.value = res.data.records
  total.value = res.data.total
}

const goDetail = (id) => {
  router.push(`/article/${id}`)
}

const handlePageChange = (page) => {
  pageNum.value = page
  loadArticles()
}

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`
}

onMounted(loadArticles)
</script>

<style scoped>
.article-card {
  cursor: pointer;
  transition: all 0.3s;
}
.article-card:hover {
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  transform: translateY(-4px);
}
.article-meta {
  color: #999;
  font-size: 13px;
  margin-top: 8px;
}
</style>
