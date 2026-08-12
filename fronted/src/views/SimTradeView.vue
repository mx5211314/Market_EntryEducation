<template>
  <div>
    <h2>📈 入市全程引导</h2>
    <p>输入您的投资目标，系统将结合您的风险等级与法规，生成模拟操作指引。</p>
    <el-input
      v-model="query"
      placeholder="例如：我想融资买入贵州茅台"
      class="query-input" />
    <el-button
      type="primary"
      @click="startGuidance"
      :loading="loading"
      style="margin-top: 10px"
      >开始引导</el-button
    >

    <el-card v-if="report" class="report-card">
      <div v-html="formatContent(report)"></div>
    </el-card>
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

const formatContent = (content) => {
  if (!content) return ''
  return content
    .replace(/\n/g, '<br/>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
}
</script>

<style scoped>
.query-input {
  margin-bottom: 15px;
}
.report-card {
  margin-top: 20px;
  white-space: pre-wrap;
  border-radius: 8px;
  background: #fff;
}
h2 {
  color: #1e3c72;
}
</style>
