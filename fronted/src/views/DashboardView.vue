<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="card-content">
            <div class="icon-box users">
              <span class="icon">👥</span>
            </div>
            <div class="info">
              <div class="label">总用户数</div>
              <div class="value">{{ stats.totalUsers || 0 }}</div>
              <div class="sub">今日新增：{{ stats.todayUsers || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="card-content">
            <div class="icon-box messages">
              <span class="icon">💬</span>
            </div>
            <div class="info">
              <div class="label">问答消息</div>
              <div class="value">{{ stats.totalMessages || 0 }}</div>
              <div class="sub">今日：{{ stats.todayMessages || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="card-content">
            <div class="icon-box articles">
              <span class="icon">📚</span>
            </div>
            <div class="info">
              <div class="label">文章总数</div>
              <div class="value">{{ stats.totalArticles || 0 }}</div>
              <div class="sub">已发布：{{ stats.publishedArticles || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="card-content">
            <div class="icon-box assessments">
              <span class="icon">📊</span>
            </div>
            <div class="info">
              <div class="label">风险测评</div>
              <div class="value">{{ stats.totalAssessments || 0 }}</div>
              <div class="sub">活跃用户：{{ stats.activeUsers || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :md="14">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">近 7 天问答趋势</div>
          </template>
          <div ref="trendChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">风险测评等级分布</div>
          </template>
          <div ref="levelChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :md="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">投资日记情绪分布</div>
          </template>
          <div ref="sentimentChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">用户构成</div>
          </template>
          <div class="user-compose">
            <div class="user-block">
              <div class="user-num">{{ stats.adminUsers || 0 }}</div>
              <div class="user-label">管理员</div>
              <div class="user-bar admin"></div>
            </div>
            <div class="user-block">
              <div class="user-num">{{ stats.normalUsers || 0 }}</div>
              <div class="user-label">普通用户</div>
              <div class="user-bar normal"></div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据概览 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">系统概览</div>
          </template>
          <el-row :gutter="20" class="overview-stats">
            <el-col :xs="12" :sm="6">
              <div class="overview-item">
                <div class="overview-label">会话总数</div>
                <div class="overview-value">{{ stats.totalSessions || 0 }}</div>
              </div>
            </el-col>
            <el-col :xs="12" :sm="6">
              <div class="overview-item">
                <div class="overview-label">投资日记</div>
                <div class="overview-value">{{ stats.totalDiaries || 0 }}</div>
              </div>
            </el-col>
            <el-col :xs="12" :sm="6">
              <div class="overview-item">
                <div class="overview-label">今日日记</div>
                <div class="overview-value">{{ stats.todayNewDiaries || 0 }}</div>
              </div>
            </el-col>
            <el-col :xs="12" :sm="6">
              <div class="overview-item">
                <div class="overview-label">今日消息</div>
                <div class="overview-value">{{ stats.todayMessages || 0 }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'

const stats = ref({})
const trendChart = ref(null)
const levelChart = ref(null)
const sentimentChart = ref(null)
let trendInstance = null
let levelInstance = null
let sentimentInstance = null

const loadStats = async () => {
  try {
    const res = await axios.get('/api/admin/dashboard/stats')
    stats.value = res.data
    await nextTick()
    renderCharts()
  } catch (e) {
    console.error(e)
  }
}

const renderCharts = () => {
  const s = stats.value

  // 消息趋势
  if (trendChart.value) {
    if (trendInstance) trendInstance.dispose()
    trendInstance = echarts.init(trendChart.value)
    const dates = Object.keys(s.messageTrend || {})
    trendInstance.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#fab1a0',
        borderWidth: 1,
        textStyle: { color: '#2d3436' },
      },
      grid: { left: '3%', right: '4%', top: '10%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: dates,
        axisLine: { lineStyle: { color: '#b2bec3' } },
        axisLabel: { color: '#636e72' },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLine: { lineStyle: { color: '#b2bec3' } },
        axisLabel: { color: '#636e72' },
        splitLine: { lineStyle: { color: 'rgba(180, 180, 180, 0.2)' } },
      },
      series: [{
        type: 'line',
        smooth: true,
        data: dates.map((d) => s.messageTrend[d]),
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(102, 126, 234, 0.4)' },
              { offset: 1, color: 'rgba(102, 126, 234, 0.1)' },
            ],
          },
        },
        lineStyle: { width: 3, color: '#667eea' },
        itemStyle: { color: '#667eea' },
      }],
    })
  }

  // 测评等级分布
  if (levelChart.value) {
    if (levelInstance) levelInstance.dispose()
    levelInstance = echarts.init(levelChart.value)
    const levelMap = { 保守型: '#ffeaa7', 稳健型: '#74b9ff', 积极型: '#fd79a8' }
    const levelData = Object.entries(s.levelDist || {}).map(([name, value]) => ({
      name,
      value,
      itemStyle: { color: levelMap[name] || '#a29bfe' },
    }))
    levelInstance.setOption({
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#fab1a0',
        borderWidth: 1,
        textStyle: { color: '#2d3436' },
      },
      legend: { bottom: 0, textStyle: { color: '#636e72' } },
      series: [{
        type: 'pie',
        radius: ['35%', '65%'],
        center: ['50%', '45%'],
        data: levelData,
        label: { show: true, formatter: '{b}\n{c}', color: '#636e72' },
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      }],
    })
  }

  // 日记情绪分布
  if (sentimentChart.value) {
    if (sentimentInstance) sentimentInstance.dispose()
    sentimentInstance = echarts.init(sentimentChart.value)
    const sentimentMap = { 积极: '#00b894', 中性: '#a4b0be', 消极: '#d63031' }
    const names = Object.keys(s.sentimentDist || {})
    sentimentInstance.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#fab1a0',
        borderWidth: 1,
        textStyle: { color: '#2d3436' },
      },
      grid: { left: '3%', right: '4%', top: '10%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: names,
        axisLine: { lineStyle: { color: '#b2bec3' } },
        axisLabel: { color: '#636e72' },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLine: { lineStyle: { color: '#b2bec3' } },
        axisLabel: { color: '#636e72' },
        splitLine: { lineStyle: { color: 'rgba(180, 180, 180, 0.2)' } },
      },
      series: [{
        type: 'bar',
        barMaxWidth: 60,
        data: names.map((n) => ({
          value: s.sentimentDist[n],
          itemStyle: {
            color: sentimentMap[n] || '#a29bfe',
            borderRadius: [8, 8, 0, 0],
          },
        })),
      }],
    })
  }
}

const handleResize = () => {
  trendInstance?.resize()
  levelInstance?.resize()
  sentimentInstance?.resize()
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
})
</script>

<style scoped>
.dashboard-page {
  padding: 4px;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  margin-bottom: 16px;
  transition: transform 0.3s, box-shadow 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.card-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-box {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-box.users {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.icon-box.messages {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.icon-box.articles {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.icon-box.assessments {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.icon {
  font-size: 26px;
}

.info {
  flex: 1;
}

.info .label {
  font-size: 13px;
  color: #95a5a6;
  margin-bottom: 4px;
}

.info .value {
  font-size: 22px;
  font-weight: 700;
  color: #2d3436;
  margin-bottom: 4px;
}

.info .sub {
  font-size: 12px;
  color: #b2bec3;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  margin-bottom: 16px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
  color: #2d3436;
}

.chart-box {
  height: 320px;
}

.user-compose {
  height: 320px;
  display: flex;
  justify-content: space-around;
  align-items: center;
  gap: 24px;
}

.user-block {
  text-align: center;
  flex: 1;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 12px;
}

.user-num {
  font-size: 32px;
  font-weight: 700;
  color: #667eea;
  margin-bottom: 8px;
}

.user-label {
  font-size: 14px;
  color: #636e72;
  margin-bottom: 12px;
}

.user-bar {
  height: 6px;
  border-radius: 3px;
  max-width: 120px;
  margin: 0 auto;
  background: linear-gradient(90deg, #667eea, #764ba2);
}

.user-bar.normal {
  background: linear-gradient(90deg, #4facfe, #00f2fe);
}

.overview-stats {
  padding: 16px 0;
}

.overview-item {
  text-align: center;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 12px;
  transition: transform 0.3s;
}

.overview-item:hover {
  transform: translateY(-2px);
}

.overview-label {
  font-size: 13px;
  color: #95a5a6;
  margin-bottom: 8px;
}

.overview-value {
  font-size: 20px;
  font-weight: 700;
  color: #2d3436;
}

@media (max-width: 768px) {
  .icon-box {
    width: 48px;
    height: 48px;
  }

  .icon {
    font-size: 22px;
  }

  .info .value {
    font-size: 18px;
  }

  .chart-box {
    height: 260px;
  }

  .user-compose {
    height: 260px;
  }

  .user-num {
    font-size: 26px;
  }
}
</style>