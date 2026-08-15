<template>
  <div class="diary-container">
    <div class="header-section">
      <div class="header-content">
        <div class="header-icon">📝</div>
        <h1>投资日记</h1>
      </div>
    </div>

    <div class="content">
      <!-- 统计面板 -->
      <div class="stats-panel" v-if="stats">
        <div class="section-title">统计概览（共 {{ stats.total }} 篇日记）</div>
        <div class="charts-row">
          <div ref="pieChart" class="chart-box"></div>
          <div ref="barChart" class="chart-box"></div>
          <div ref="lineChart" class="chart-box"></div>
        </div>
      </div>

      <!-- 新建/编辑表单 -->
      <div class="diary-card form-card">
        <div class="card-title">
          <span>{{ form.id ? '编辑' : '新建' }}日记</span>
          <el-button v-if="form.id" text type="danger" @click="resetForm">取消编辑</el-button>
        </div>

        <el-form label-width="100px">
          <el-form-item label="日记标题">
            <el-input v-model="form.title" placeholder="给今日的投资记录起个标题..." />
          </el-form-item>

          <el-form-item label="今日情绪">
            <div class="emotion-rate">
              <el-rate
                v-model="form.moodScore"
                :texts="emotionStatus"
                show-texts
                :max="10"
                size="large" />
            </div>
          </el-form-item>

          <el-form-item label="主要情绪">
            <div class="emotion-grid">
              <div
                v-for="emotion in emotionOptions"
                :key="emotion.name"
                class="emotion-card"
                :class="{ selected: form.mood === emotion.name }"
                @click="selectEmotion(emotion.name)">
                <div class="emotion-icon">{{ emotion.icon }}</div>
                <div class="emotion-name">{{ emotion.name }}</div>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="交易动作">
            <el-radio-group v-model="form.tradeAction">
              <el-radio-button label="买入">买入</el-radio-button>
              <el-radio-button label="卖出">卖出</el-radio-button>
              <el-radio-button label="持仓">持仓</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="详细内容">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="5"
              placeholder="记录今天的投资心得、决策过程、市场观察..."
              show-word-limit
              maxlength="2000" />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="saving"
              @click="saveDiary">
              {{ saving ? '保存中...' : (form.id ? '更新' : '保存') }}
            </el-button>
            <el-button size="large" @click="resetForm">清空</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 日记列表 -->
      <div v-if="diaries.length > 0" class="diary-list">
        <div class="section-title">历史记录</div>
        <div
          v-for="diary in diaries"
          :key="diary.id"
          class="diary-card">
          <div class="diary-header">
            <div class="diary-main">
              <h3 class="diary-title">{{ diary.title }}</h3>
              <div class="diary-tags">
                <el-tag :type="sentimentType(diary.sentiment)" size="small">
                  {{ diary.sentiment || '未分析' }}
                </el-tag>
                <el-tag :type="actionType(diary.tradeAction)" size="small">
                  {{ diary.tradeAction || '未记录' }}
                </el-tag>
              </div>
            </div>
            <div class="diary-time">{{ formatFullDate(diary.createdAt) }}</div>
          </div>
          <div class="diary-content">{{ diary.content }}</div>
          <div class="diary-footer">
            <el-button
              text
              type="primary"
              size="small"
              @click="openEdit(diary)">
              编辑
            </el-button>
            <el-button
              text
              type="danger"
              size="small"
              @click="deleteDiary(diary)">
              删除
            </el-button>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <div class="empty-icon">📝</div>
        <p>还没有投资日记，开始记录吧</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

const diaries = ref([])
const stats = ref(null)
const saving = ref(false)
const form = ref({
  id: null,
  title: '',
  content: '',
  mood: '',
  moodScore: 5,
  tradeAction: ''
})

const pieChart = ref(null)
const barChart = ref(null)
const lineChart = ref(null)
let pieInstance = null
let barInstance = null
let lineInstance = null

const emotionStatus = ['很差', '差', '一般', '一般', '还可以', '不错', '不错', '好', '很好', '完美']

const emotionOptions = [
  { name: '开心', icon: '😊', score: 8 },
  { name: '平静', icon: '😌', score: 6 },
  { name: '焦虑', icon: '😰', score: 4 },
  { name: '失望', icon: '😞', score: 2 },
  { name: '兴奋', icon: '🤩', score: 9 },
  { name: '担忧', icon: '😟', score: 3 },
  { name: '乐观', icon: '😄', score: 7 },
  { name: '平静', icon: '😐', score: 5 },
]

const selectEmotion = (name) => {
  form.value.mood = name
}

const sentimentType = (s) => {
  if (s === '积极') return 'success'
  if (s === '消极') return 'danger'
  return ''
}

const actionType = (a) => {
  if (a === '买入') return 'success'
  if (a === '卖出') return 'danger'
  return 'warning'
}

const loadData = async () => {
  try {
    const [diaryRes, statsRes] = await Promise.all([
      axios.get('/api/user/diary/list'),
      axios.get('/api/user/diary/stats'),
    ])
    diaries.value = diaryRes.data
    stats.value = statsRes.data
    await nextTick()
    renderCharts()
  } catch (e) {
    console.error(e)
  }
}

const saveDiary = async () => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  saving.value = true
  try {
    if (form.value.id) {
      await axios.put(`/api/user/diary/${form.value.id}`, form.value)
    } else {
      await axios.post('/api/user/diary', form.value)
    }
    ElMessage.success('保存成功')
    resetForm()
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  }
  saving.value = false
}

const openEdit = (diary) => {
  form.value = {
    id: diary.id,
    title: diary.title,
    content: diary.content,
    mood: diary.mood,
    moodScore: diary.moodScore || 5,
    tradeAction: diary.tradeAction || '',
  }
  document.querySelector('.form-card')?.scrollIntoView({ behavior: 'smooth' })
}

const deleteDiary = async (diary) => {
  await ElMessageBox.confirm('确定删除该日记吗？', '提示', { type: 'warning' })
  try {
    await axios.delete(`/api/user/diary/${diary.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const resetForm = () => {
  form.value = {
    id: null,
    title: '',
    content: '',
    mood: '',
    moodScore: 5,
    tradeAction: '',
  }
}

const renderCharts = () => {
  if (!stats.value) return

  const s = stats.value.sentiment || {}
  const a = stats.value.tradeAction || {}
  const d = stats.value.dailyCount || {}

  // 情绪饼图
  if (pieChart.value) {
    if (pieInstance) pieInstance.dispose()
    pieInstance = echarts.init(pieChart.value)
    pieInstance.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['35%', '65%'],
        center: ['50%', '45%'],
        data: [
          { value: s['积极'] || 0, name: '积极', itemStyle: { color: '#10b981' } },
          { value: s['中性'] || 0, name: '中性', itemStyle: { color: '#6b7280' } },
          { value: s['消极'] || 0, name: '消极', itemStyle: { color: '#ef4444' } },
        ],
        label: { show: true, formatter: '{b}: {c} ({d}%)' },
      }],
    })
  }

  // 交易动作柱状图
  if (barChart.value) {
    if (barInstance) barInstance.dispose()
    barInstance = echarts.init(barChart.value)
    const actionColors = {
      买入: '#10b981',
      卖出: '#ef4444',
      持仓: '#6b7280'
    }
    barInstance.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 20, top: 40, bottom: 30 },
      xAxis: {
        type: 'category',
        data: Object.keys(a),
      },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{
        type: 'bar',
        data: Object.entries(a).map(([name, value]) => ({
          value,
          itemStyle: { color: actionColors[name] || '#9ca3af', borderRadius: 6 },
        })),
        barMaxWidth: 40,
      }],
    })
  }

  // 日记趋势折线图
  if (lineChart.value) {
    if (lineInstance) lineInstance.dispose()
    lineInstance = echarts.init(lineChart.value)
    const days = Object.keys(d).sort()
    lineInstance.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: days },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{
        type: 'line',
        smooth: true,
        data: days.map(k => d[k]),
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(245, 158, 11, 0.3)' },
              { offset: 1, color: 'rgba(139, 92, 246, 0.05)' },
            ],
          },
        },
        itemStyle: { color: '#f59e0b' },
      }],
    })
  }
}

const formatFullDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const handleResize = () => {
  pieInstance?.resize()
  barInstance?.resize()
  lineInstance?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  pieInstance?.dispose()
  barInstance?.dispose()
  lineInstance?.dispose()
})
</script>

<style scoped>
.diary-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 72px);
}

.header-section {
  background: var(--primary-gradient);
  color: white;
  padding: 48px 32px;
  margin-bottom: 24px;
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(245, 158, 11, 0.15);
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  font-size: 48px;
  width: 64px;
  height: 64px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-content h1 {
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 统计面板 */
.stats-panel {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.charts-row {
  display: flex;
  gap: 16px;
}

.chart-box {
  flex: 1;
  height: 280px;
}

/* 表单卡片 */
.form-card {
  border-left: 4px solid var(--primary-orange);
}

.form-card .card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.form-card .card-title span {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.emotion-rate {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin: 16px 0;
}

.emotion-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 12px;
  margin: 16px 0;
}

.emotion-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 12px;
  background: #fafafa;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.emotion-card:hover {
  background: #f0f0f0;
}

.emotion-card.selected {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.1), rgba(139, 92, 246, 0.1));
  border-color: var(--primary-orange);
}

.emotion-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.emotion-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

/* 日记列表 */
.diary-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.diary-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.diary-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.diary-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.diary-main {
  flex: 1;
  min-width: 0;
}

.diary-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.diary-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.diary-time {
  font-size: 12px;
  color: var(--text-muted);
}

.diary-content {
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 16px;
  white-space: pre-wrap;
  word-break: break-word;
}

.diary-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--text-muted);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

/* 响应式 */
@media (max-width: 768px) {
  .header-section {
    padding: 32px 20px;
  }

  .header-content h1 {
    font-size: 22px;
  }

  .charts-row {
    flex-direction: column;
  }

  .chart-box {
    height: 240px;
  }

  .emotion-grid {
    grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  }

  .form-card {
    padding: 16px;
  }

  .emotion-icon {
    font-size: 24px;
  }

  .diary-header {
    flex-direction: column;
    gap: 8px;
  }

  .diary-footer {
    justify-content: flex-start;
  }
}
</style>