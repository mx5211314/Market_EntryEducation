<template>
  <div class="sim-page">
    <div class="page-card">
      <h2>📈 入市全程引导</h2>
      <p class="desc">
        输入投资目标，系统将结合风险等级与法规，生成模拟操作指引。
      </p>

      <el-input
        v-model="query"
        placeholder="例如：我想融资买入贵州茅台"
        size="large"
        class="query-input" />
      <button class="guide-btn" @click="startGuidance" :disabled="loading">
        {{ loading ? '生成中...' : '开始引导' }}
      </button>

      <el-card v-if="report" class="report-card">
        <div class="report-content">{{ report }}</div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const query = ref('')
const report = ref('')
const loading = ref(false)

const startGuidance = async () => {
  if (!query.value.trim()) {
    ElMessage.warning('请输入投资目标')
    return
  }
  loading.value = true
  try {
    const res = await axios.post('/api/agent/guidance', {
      message: query.value,
    })
    report.value = res.data.guidance || '暂无建议'
  } catch (e) {
    ElMessage.error('请求失败，请检查后端服务')
    console.error(e)
  }
  loading.value = false
}
</script>

<style scoped>
.sim-page {
  max-width: 700px;
  margin: 0 auto;
}
.page-card {
  background: var(--card-bg);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-soft);
  padding: 24px;
}
h2 {
  color: var(--text-dark);
  margin-bottom: 6px;
}
.desc {
  color: var(--text-muted);
  font-size: 14px;
  margin-bottom: 18px;
}
.query-input {
  margin-bottom: 14px;
}
.guide-btn {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff9a8b, #a4508b);
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}
.guide-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(164, 80, 139, 0.4);
}
.guide-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.report-card {
  margin-top: 18px;
}
.report-content {
  white-space: pre-wrap;
  line-height: 1.8;
  color: var(--text-dark);
}
</style>
