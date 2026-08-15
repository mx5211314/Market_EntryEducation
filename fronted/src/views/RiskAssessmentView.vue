<template>
  <div class="risk-page">
    <div class="page-card">
      <h2>📊 投资者风险测评</h2>
      <p class="desc">
        依据《投资者适当性管理办法》，请如实完成以下测评问卷，帮助我们判断您的风险承受等级。
      </p>

      <!-- 最新测评结果 -->
      <el-alert
        v-if="latest"
        :title="`您最近一次测评等级：${latest.level}（${latest.score} 分 · ${formatDate(latest.createdAt)}）`"
        :type="levelType(latest.level)"
        show-icon
        :closable="false"
        class="latest-alert" />

      <!-- 答题区 -->
      <el-card v-for="(q, idx) in questions" :key="q.id" class="question-card">
        <p class="question-text">{{ idx + 1 }}. {{ q.text }}</p>
        <el-radio-group v-model="answers[idx]">
          <el-radio v-for="opt in q.options" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </el-radio>
        </el-radio-group>
      </el-card>

      <button class="submit-btn" @click="submit" :disabled="!allAnswered || submitting">
        {{ submitting ? '提交中...' : '提交测评' }}
      </button>

      <!-- 历史记录 -->
      <div v-if="history.length > 0" class="history-section">
        <h3>测评历史</h3>
        <el-table :data="history" size="small" border>
          <el-table-column prop="level" label="等级" width="100">
            <template #default="{ row }">
              <el-tag :type="levelType(row.level)">{{ row.level }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="得分" width="80" />
          <el-table-column prop="createdAt" label="测评时间" />
        </el-table>
      </div>
    </div>

    <!-- 结果弹窗 -->
    <el-dialog v-model="resultVisible" title="测评结果" width="420px">
      <div class="result-content">
        <p class="result-level">
          您的风险等级：<strong>{{ result.level }}</strong>
        </p>
        <p class="result-score">得分：{{ result.score }} 分</p>
        <p class="result-biz">可参与业务：{{ result.suitableBiz }}</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="resultVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const questions = ref([])
const answers = ref([])
const submitting = ref(false)
const resultVisible = ref(false)
const result = ref({})
const history = ref([])
const latest = ref(null)

const load = async () => {
  try {
    const qRes = await axios.get('/api/user/assessment/questions')
    questions.value = qRes.data
    answers.value = questions.value.map(() => null)

    const [latestRes, historyRes] = await Promise.all([
      axios.get('/api/user/assessment/latest'),
      axios.get('/api/user/assessment/history'),
    ])
    latest.value = latestRes.data.exists ? latestRes.data : null
    history.value = historyRes.data.records || []
  } catch (e) {
    console.error(e)
  }
}

const allAnswered = computed(() =>
  answers.value.every((a) => a !== null && a !== undefined),
)

const submit = async () => {
  if (!allAnswered.value || submitting.value) return
  submitting.value = true
  try {
    const res = await axios.post('/api/user/assessment/submit', {
      answers: answers.value,
    })
    result.value = res.data
    resultVisible.value = true
    load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '提交失败，请重试')
  }
  submitting.value = false
}

const levelType = (level) => {
  if (level === '积极型') return 'success'
  if (level === '稳健型') return 'warning'
  return 'info'
}

const formatDate = (date) => {
  if (!date) return ''
  return date.replace('T', ' ').slice(0, 16)
}

onMounted(load)
</script>

<style scoped>
.risk-page {
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
.latest-alert {
  margin-bottom: 16px;
}
.question-card {
  margin-bottom: 14px;
}
.question-text {
  font-weight: 600;
  color: var(--text-dark);
  margin-bottom: 10px;
}
.submit-btn {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff9a8b, #ff6a88);
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  margin-top: 8px;
}
.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(255, 106, 136, 0.4);
}
.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.history-section {
  margin-top: 24px;
}
.history-section h3 {
  margin-bottom: 10px;
}
.result-content p {
  margin-bottom: 8px;
  font-size: 14px;
}
.result-level {
  font-size: 16px;
}
</style>