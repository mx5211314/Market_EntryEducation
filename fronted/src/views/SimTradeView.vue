<template>
  <section class="page-panel">
    <div class="page-intro">
      <div>
        <h2>入市模拟引导</h2>
        <p>输入投资目标后，系统会结合风险等级、交易规则和模拟步骤生成操作建议。</p>
      </div>
    </div>

    <div class="guide-layout">
      <div class="input-card surface">
        <el-input
          v-model="query"
          type="textarea"
          :rows="5"
          placeholder="例如：我想融资买入贵州茅台"
          resize="none" />
        <el-button type="primary" size="large" @click="startGuidance" :loading="loading">开始引导</el-button>
      </div>

      <div class="report-card surface">
        <div v-if="!report" class="empty-report">
          <strong>等待生成模拟指引</strong>
          <span>建议会展示风险匹配、规则约束、模拟交易步骤和复盘要点。</span>
        </div>
        <div v-else class="report-content">{{ report }}</div>
      </div>
    </div>
  </section>
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
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.guide-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
  align-items: stretch;
}

.input-card,
.report-card {
  padding: 18px;
}

.input-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.report-card {
  min-height: 360px;
}

.empty-report {
  height: 100%;
  min-height: 300px;
  display: grid;
  place-content: center;
  text-align: center;
  color: #738699;
}

.empty-report strong {
  display: block;
  margin-bottom: 8px;
  color: #132a3a;
  font-size: 18px;
}

.report-content {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #263847;
}

@media (max-width: 900px) {
  .guide-layout {
    grid-template-columns: 1fr;
  }
}
</style>
