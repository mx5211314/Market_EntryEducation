<template>
  <div class="dashboard-page">
    <div class="page-head">
      <div class="head-left">
        <h2>数据看板</h2>
        <span class="head-note">
          趋势类指标统计近 7 天{{ updatedAt ? '，数据更新于 ' + updatedAt : '' }}
        </span>
      </div>
      <el-button :loading="loading" @click="loadStats">刷新数据</el-button>
    </div>

    <div class="kpi-grid" v-loading="loading">
      <div class="kpi" v-for="k in kpis" :key="k.label">
        <div class="kpi-icon" :class="k.tone">
          <el-icon><component :is="k.icon" /></el-icon>
        </div>
        <div class="kpi-body">
          <span class="kpi-label">{{ k.label }}</span>
          <b class="kpi-value">{{ k.value }}</b>
          <span class="kpi-sub">{{ k.sub }}</span>
        </div>
      </div>
    </div>

    <div class="grid-2 uneven">
      <div class="panel">
        <div class="panel-head">
          <h3>近 7 天问答趋势</h3>
          <span>每天的问答消息条数</span>
        </div>
        <div ref="trendChart" class="chart-box"></div>
      </div>
      <div class="panel">
        <div class="panel-head">
          <h3>风险测评等级分布</h3>
          <span>累计 {{ stats.totalAssessments || 0 }} 份</span>
        </div>
        <div ref="levelChart" class="chart-box"></div>
      </div>
    </div>
    <div class="panel">
      <div class="panel-head">
        <h3>近 7 天平台活跃度</h3>
        <span>活跃用户按当天真实发过问的人数去重统计</span>
      </div>
      <div ref="activityChart" class="chart-box tall"></div>
    </div>

    <div class="grid-2">
      <div class="panel">
        <div class="panel-head">
          <h3>投资日记情绪分布</h3>
          <span>累计 {{ stats.totalDiaries || 0 }} 篇，今日新增 {{ stats.todayNewDiaries || 0 }} 篇</span>
        </div>
        <div ref="sentimentChart" class="chart-box"></div>
      </div>
      <div class="panel">
        <div class="panel-head">
          <h3>用户构成</h3>
          <span>共 {{ stats.totalUsers || 0 }} 个账号</span>
        </div>
        <div class="user-compose">
          <div class="user-block">
            <b>{{ stats.adminUsers || 0 }}</b>
            <span>管理员</span>
            <i class="user-bar admin" />
          </div>
          <div class="user-block">
            <b>{{ stats.normalUsers || 0 }}</b>
            <span>普通用户</span>
            <i class="user-bar normal" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { User, TrendCharts, ChatDotRound, Document, DataAnalysis } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDashboardStats } from '@/api/admin'

// 全站统一配色，图表不再用另一套紫粉色
const BLUE = '#409eff'
const GREEN = '#67c23a'
const ORANGE = '#e6a23c'
const RED = '#f56c6c'
const GREY = '#909399'

const stats = ref({})
const loading = ref(false)
const updatedAt = ref('')
const trendChart = ref(null)
const levelChart = ref(null)
const sentimentChart = ref(null)
const activityChart = ref(null)
let trendInstance = null
let levelInstance = null
let sentimentInstance = null
let activityInstance = null

const kpis = computed(() => {
  const s = stats.value
  return [
    { label: '总用户数', value: s.totalUsers || 0, sub: `今日新增 ${s.todayUsers || 0}`, tone: 'blue', icon: User },
    { label: '近 7 天活跃用户', value: s.activeUsers || 0, sub: `新建会话 ${s.weekSessions || 0} 个`, tone: 'green', icon: TrendCharts },
    { label: '问答消息', value: s.totalMessages || 0, sub: `今日 ${s.todayMessages || 0} 条`, tone: 'cyan', icon: ChatDotRound },
    { label: '知识库文章', value: s.totalArticles || 0, sub: `已发布 ${s.publishedArticles || 0} 篇`, tone: 'orange', icon: Document },
    { label: '风险测评', value: s.totalAssessments || 0, sub: `投资日记 ${s.totalDiaries || 0} 篇`, tone: 'red', icon: DataAnalysis }
  ]
})

const loadStats = async () => {
  loading.value = true
  try {
    stats.value = (await getDashboardStats()) || {}
    updatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
    await nextTick()
    renderCharts()
  } catch (e) {
    ElMessage.error('看板数据加载失败')
  } finally {
    loading.value = false
  }
}
const TOOLTIP = {
  backgroundColor: '#fff',
  borderColor: '#e4e7ed',
  borderWidth: 1,
  padding: [8, 12],
  textStyle: { color: '#1f2329', fontSize: 12 }
}
const AXIS_LABEL = { color: '#a8adb7', fontSize: 11 }
const AXIS_LINE = { lineStyle: { color: '#e4e7ed' } }

const valueAxis = () => ({
  type: 'value',
  minInterval: 1,
  axisLine: AXIS_LINE,
  axisLabel: AXIS_LABEL,
  splitLine: { lineStyle: { color: '#f0f2f5' } }
})

// 横轴只留「月-日」，7 个完整日期会挤在一起看不清，完整日期留给 tooltip
const dayAxis = (dates) => ({
  type: 'category',
  data: dates,
  axisLine: AXIS_LINE,
  axisTick: { show: false },
  axisLabel: { ...AXIS_LABEL, formatter: (v) => String(v).slice(5) }
})

const areaFill = (rgb) => ({
  color: {
    type: 'linear',
    x: 0, y: 0, x2: 0, y2: 1,
    colorStops: [
      { offset: 0, color: `rgba(${rgb}, 0.28)` },
      { offset: 1, color: `rgba(${rgb}, 0.02)` }
    ]
  }
})

const lineSeries = (name, data, color, fillRgb) => ({
  name,
  type: 'line',
  smooth: true,
  symbolSize: 6,
  data,
  lineStyle: { width: fillRgb ? 3 : 2, color },
  itemStyle: { color },
  ...(fillRgb ? { areaStyle: areaFill(fillRgb) } : {})
})
const renderCharts = () => {
  const s = stats.value

  if (trendChart.value) {
    trendInstance?.dispose()
    trendInstance = echarts.init(trendChart.value)
    const dates = Object.keys(s.messageTrend || {})
    trendInstance.setOption({
      tooltip: { trigger: 'axis', ...TOOLTIP },
      grid: { left: 8, right: 16, top: 24, bottom: 4, containLabel: true },
      xAxis: dayAxis(dates),
      yAxis: valueAxis(),
      series: [lineSeries('消息数', dates.map((d) => s.messageTrend[d]), BLUE, '64, 158, 255')]
    })
  }

  if (levelChart.value) {
    levelInstance?.dispose()
    levelInstance = echarts.init(levelChart.value)
    const levelColor = { 保守型: GREEN, 稳健型: BLUE, 积极型: ORANGE, 激进型: RED }
    const levelData = Object.entries(s.levelDist || {}).map(([name, value]) => ({
      name, value, itemStyle: { color: levelColor[name] || GREY }
    }))
    levelInstance.setOption({
      tooltip: { trigger: 'item', ...TOOLTIP },
      legend: { bottom: 0, icon: 'circle', itemWidth: 8, textStyle: { color: '#606266', fontSize: 12 } },
      series: [{
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '44%'],
        data: levelData,
        label: { formatter: '{b}\n{c} 份', color: '#606266', fontSize: 12 },
        labelLine: { length: 8, length2: 8 },
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 }
      }]
    })
  }
  if (sentimentChart.value) {
    sentimentInstance?.dispose()
    sentimentInstance = echarts.init(sentimentChart.value)
    const map = { 积极: GREEN, 中性: GREY, 消极: RED }
    const names = Object.keys(s.sentimentDist || {})
    sentimentInstance.setOption({
      tooltip: { trigger: 'axis', ...TOOLTIP },
      grid: { left: 8, right: 16, top: 24, bottom: 4, containLabel: true },
      xAxis: { type: 'category', data: names, axisLine: AXIS_LINE, axisTick: { show: false }, axisLabel: AXIS_LABEL },
      yAxis: valueAxis(),
      series: [{
        type: 'bar',
        barMaxWidth: 48,
        data: names.map((n) => ({
          value: s.sentimentDist[n],
          itemStyle: { color: map[n] || GREY, borderRadius: [6, 6, 0, 0] }
        }))
      }]
    })
  }

  if (activityChart.value) {
    activityInstance?.dispose()
    activityInstance = echarts.init(activityChart.value)
    const activity = s.userActivity || {}
    const dates = Object.keys(activity)
    const pick = (key) => dates.map((d) => activity[d]?.[key] || 0)
    activityInstance.setOption({
      tooltip: { trigger: 'axis', ...TOOLTIP, axisPointer: { type: 'shadow' } },
      legend: { top: 0, icon: 'circle', itemWidth: 8, textStyle: { color: '#606266', fontSize: 12 } },
      grid: { left: 8, right: 16, top: 44, bottom: 4, containLabel: true },
      xAxis: dayAxis(dates),
      yAxis: valueAxis(),
      series: [
        {
          name: '新建会话',
          type: 'bar',
          barMaxWidth: 26,
          data: pick('sessionCount'),
          itemStyle: { color: 'rgba(64, 158, 255, 0.16)', borderRadius: [4, 4, 0, 0] }
        },
        lineSeries('活跃用户', pick('activeUsers'), BLUE, '64, 158, 255'),
        lineSeries('新增用户', pick('newUsers'), GREEN),
        lineSeries('写日记用户', pick('diaryUsers'), ORANGE)
      ]
    })
  }
}
const handleResize = () => {
  trendInstance?.resize()
  levelInstance?.resize()
  sentimentInstance?.resize()
  activityInstance?.resize()
}

onMounted(() => {
  loadStats()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendInstance?.dispose()
  levelInstance?.dispose()
  sentimentInstance?.dispose()
  activityInstance?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard-page {
  max-width: 1180px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;

  .head-left {
    display: flex;
    align-items: baseline;
    gap: 10px;

    h2 {
      margin: 0;
      font-size: 17px;
      color: #1f2329;
    }

    .head-note {
      font-size: 12.5px;
      color: #a8adb7;
    }
  }
}
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.kpi {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 16px 15px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(31, 45, 61, 0.06);
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.3s;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 20px rgba(31, 45, 61, 0.1);
  }

  .kpi-icon {
    width: 42px;
    height: 42px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    color: #fff;
    flex-shrink: 0;

    &.blue { background: linear-gradient(135deg, #409eff, #66b1ff); }
    &.green { background: linear-gradient(135deg, #67c23a, #95d475); }
    &.cyan { background: linear-gradient(135deg, #36cfc9, #5ad8d2); }
    &.orange { background: linear-gradient(135deg, #e6a23c, #f0c078); }
    &.red { background: linear-gradient(135deg, #f56c6c, #f89898); }
  }

  .kpi-body {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }
}
.kpi-label {
  font-size: 12.5px;
  color: #a8adb7;
}

.kpi-value {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.25;
  color: #1f2329;
}

.kpi-sub {
  font-size: 12px;
  color: #b7bcc6;
}

.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;

  .wide {
    grid-column: span 1;
  }
}

.panel {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(31, 45, 61, 0.06);
  padding: 16px 18px 14px;
  margin-bottom: 14px;

  .panel-head {
    display: flex;
    align-items: baseline;
    gap: 10px;
    margin-bottom: 6px;

    h3 {
      margin: 0;
      font-size: 14.5px;
      color: #1f2329;
    }

    span {
      font-size: 12px;
      color: #a8adb7;
    }
  }
}
.grid-2 {
  &.uneven {
    grid-template-columns: 1.35fr minmax(0, 1fr);
  }

  .panel {
    margin-bottom: 0;
  }
}

.chart-box {
  height: 300px;

  &.tall {
    height: 340px;
  }
}

.user-compose {
  height: 300px;
  display: flex;
  align-items: center;
  gap: 18px;

  .user-block {
    flex: 1;
    text-align: center;
    padding: 22px 16px;
    border-radius: 12px;
    background: linear-gradient(135deg, rgba(64, 158, 255, 0.08), rgba(103, 194, 58, 0.04));
    border: 1px solid rgba(64, 158, 255, 0.12);

    b {
      display: block;
      font-size: 30px;
      font-weight: 800;
      color: #409eff;
    }

    span {
      display: block;
      margin: 6px 0 12px;
      font-size: 13px;
      color: #909399;
    }
  }
}
.user-bar {
  display: block;
  height: 6px;
  max-width: 120px;
  margin: 0 auto;
  border-radius: 3px;

  &.admin {
    background: linear-gradient(90deg, #409eff, #79bbff);
  }

  &.normal {
    background: linear-gradient(90deg, #67c23a, #b3e19d);
  }
}

@media (max-width: 1100px) {
  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .grid-2,
  .grid-2.uneven {
    grid-template-columns: minmax(0, 1fr);
  }

  .chart-box {
    height: 240px;

    &.tall {
      height: 280px;
    }
  }

  .user-compose {
    height: auto;
    padding: 6px 0 10px;
  }
}
</style>

