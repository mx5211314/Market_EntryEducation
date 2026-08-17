<template>
  <div class="sim-container">
    <div class="page-head">
      <div class="head-inner">
        <div class="head-title">
          <h1>模拟引导</h1>
          <span class="subtitle">用虚拟资金演练配置，不涉及真实交易</span>
        </div>
        <div class="head-steps">
          <span v-for="(s, i) in STEPS" :key="s" class="step-chip" :class="{ on: step === i, done: step > i }">
            <b>{{ i + 1 }}</b>{{ s }}
          </span>
        </div>
      </div>
    </div>

    <div class="content">
      <div class="inner">
        <!-- ===== 第一步：风险画像 ===== -->
        <template v-if="step === 0">
          <div class="intro-card" v-if="profile">
            <div class="intro-icon">🎮</div>
            <h2>{{ profile.hasProfile ? '按您的风险等级配一个模拟组合' : '先完成风险测评才能开始' }}</h2>
            <p class="intro-desc" v-if="profile.hasProfile">
              系统会用您的
              <b>{{ profile.levelCode }} {{ profile.level }}</b>
              等级做适当性校验，配置里出现超过
              <b>{{ profile.maxProductLevel }}</b>
              的标的会被标出来。全程虚拟资金，不产生任何真实委托。
            </p>
            <p class="intro-desc" v-else>{{ profile.reason }}</p>

            <div class="intro-meta">
              <span><el-icon><Coin /></el-icon> 虚拟本金 10 万</span>
              <span><el-icon><Histogram /></el-icon> 18 个标的</span>
              <span><el-icon><Lock /></el-icon> 不接行情、不撮合</span>
            </div>

            <button v-if="profile.hasProfile" class="primary-btn" @click="step = 1">开始配置</button>
            <button v-else class="primary-btn" @click="router.push('/assessment')">去做风险测评</button>
          </div>

          <div class="result-block" v-if="portfolios.length">
            <div class="block-title">我保存过的模拟组合</div>
            <div class="saved-row" v-for="p in portfolios" :key="p.id">
              <span class="saved-name">{{ p.name }}</span>
              <span class="saved-tag">{{ p.levelCode }}</span>
              <span class="saved-num">预期年化 {{ p.expectedReturn }}%</span>
              <span class="saved-num">波动 {{ p.volatility }}%</span>
              <span class="saved-date">{{ formatDate(p.createdAt) }}</span>
              <el-icon class="saved-del" @click="removePortfolio(p.id)"><Delete /></el-icon>
            </div>
          </div>
        </template>

        <!-- ===== 第二步：配置权重 ===== -->
        <template v-else-if="step === 1">
          <div class="alloc-bar" :class="{ ok: Math.abs(totalWeight - 100) < 0.5, over: totalWeight > 100 }">
            <div class="alloc-text">
              <span>已分配 <b>{{ round1(totalWeight) }}%</b></span>
              <span class="alloc-hint">{{ allocHint }}</span>
            </div>
            <div class="alloc-track">
              <div class="alloc-fill" :style="{ width: Math.min(totalWeight, 100) + '%' }"></div>
            </div>
          </div>

          <div class="tool-row">
            <span class="tool-label">虚拟本金</span>
            <el-select v-model="amount" size="small" style="width: 130px">
              <el-option :value="100000" label="10 万" />
              <el-option :value="500000" label="50 万" />
              <el-option :value="1000000" label="100 万" />
            </el-select>
            <button class="ghost-btn tiny" @click="applyTemplate">按我的等级一键配置</button>
            <button class="ghost-btn tiny" @click="clearAll">清空</button>
          </div>

          <div class="pool-group" v-for="lv in [1, 2, 3, 4, 5]" :key="lv">
            <div class="group-head">
              <span class="group-code" :class="'r-' + lv">R{{ lv }}</span>
              <span class="group-name">{{ RISK_NAMES[lv - 1] }}</span>
              <span class="group-warn" v-if="lv > (profile?.levelIndex || 0)">超出您的 {{ profile?.maxProductLevel }} 上限</span>
            </div>
            <div
              v-for="p in productsByLevel(lv)"
              :key="p.code"
              class="pool-item"
              :class="{ picked: (weights[p.code] || 0) > 0, exceed: lv > (profile?.levelIndex || 0) }">
              <div class="pool-main">
                <span class="pool-name">{{ p.name }}</span>
                <span class="pool-feature">{{ p.feature }}</span>
                <span class="pool-num">参考年化 {{ p.annualReturn }}% · 波动 {{ p.volatility }}%</span>
              </div>
              <el-input-number
                v-model="weights[p.code]"
                :min="0"
                :max="100"
                :step="5"
                size="small"
                controls-position="right"
                class="pool-input" />
              <span class="pool-unit">%</span>
            </div>
          </div>

          <div class="quiz-actions">
            <button class="ghost-btn" @click="step = 0">上一步</button>
            <button class="primary-btn small" :disabled="analyzing" @click="runAnalyze">
              {{ analyzing ? '诊断中...' : '生成诊断报告' }}
            </button>
          </div>
        </template>

        <!-- ===== 第三步：诊断报告 ===== -->
        <template v-else>
          <div class="result-hero" :class="report.compliant ? 'ok' : 'bad'">
            <div class="hero-left">
              <div class="hero-code">{{ report.riskScore }}</div>
              <div class="hero-name">组合风险分</div>
            </div>
            <div class="hero-right">
              <p class="hero-summary">
                {{ report.compliant
                  ? `这份配置没有超出您的 ${report.maxProductLevel} 上限，加权风险等级约 R${Math.round(report.riskScore)}。`
                  : `配置中有标的超出您的 ${report.maxProductLevel} 上限，正式业务中会被拦下或要求另行签署警示书。` }}
              </p>
              <div class="hero-score">
                <span>预期年化 <b>{{ report.expectedReturn }}%</b></span>
                <span>年化波动 <b>{{ report.volatility }}%</b></span>
                <span>本金 {{ (report.amount / 10000).toFixed(0) }} 万</span>
              </div>
            </div>
          </div>

          <div class="result-block">
            <div class="block-title">诊断结论</div>
            <div v-for="(d, i) in report.diagnostics || []" :key="i" class="diag-item" :class="d.type">
              <span class="diag-dot"></span>
              <div class="diag-main">
                <span class="diag-title">{{ d.title }}</span>
                <p class="diag-text">{{ d.text }}</p>
              </div>
            </div>
          </div>

          <div class="result-block">
            <div class="block-title">
              12 个月收益区间推演
              <span class="block-note">±1 倍标准差</span>
            </div>
            <div ref="chartRef" class="proj-chart"></div>
            <p class="block-text">{{ report.projection?.note }}</p>
          </div>

          <div class="result-block">
            <div class="block-title">持仓明细</div>
            <div v-for="h in report.holdings || []" :key="h.code" class="hold-row" :class="{ bad: h.exceed }">
              <span class="hold-code" :class="'r-' + h.riskLevel">R{{ h.riskLevel }}</span>
              <span class="hold-name">{{ h.name }}</span>
              <span class="hold-cat">{{ h.category }}</span>
              <span class="hold-weight">{{ round1(h.weight) }}%</span>
              <span class="hold-amount">{{ h.amount.toLocaleString() }} 元</span>
            </div>
          </div>

          <div class="result-actions">
            <button class="primary-btn small" :disabled="saving" @click="save">
              {{ saving ? '保存中...' : '保存这份组合' }}
            </button>
            <button class="ghost-btn" @click="step = 1">回去调整</button>
            <button class="ghost-btn" @click="askAi">让 AI 点评</button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Coin, Histogram, Lock, Delete } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getSimProducts,
  getSimProfile,
  analyzeSimPortfolio,
  saveSimPortfolio,
  getSimPortfolios,
  deleteSimPortfolio
} from '@/api/frontend'

const router = useRouter()

const STEPS = ['确认风险画像', '配置模拟组合', '查看诊断报告']
const RISK_NAMES = ['低风险', '中低风险', '中风险', '中高风险', '高风险']

// 一键配置的模板权重：等级越高，权益仓位越重，防守仓位相应减少
const TEMPLATES = {
  1: { HB001: 40, NHG01: 20, GZ001: 40 },
  2: { HB001: 20, CD001: 20, DZ001: 40, LC002: 20 },
  3: { HB001: 10, DZ001: 25, EJ001: 25, PZ001: 25, HJ001: 15 },
  4: { DZ001: 20, EJ001: 15, HS300: 35, ZZ500: 15, HJ001: 15 },
  5: { DZ001: 10, HS300: 30, ZZ500: 20, CY001: 20, BD001: 10, HJ001: 10 }
}

const step = ref(0)
const products = ref([])
const profile = ref(null)
const portfolios = ref([])
const weights = reactive({})
const amount = ref(100000)
const report = ref({})
const analyzing = ref(false)
const saving = ref(false)
const chartRef = ref(null)
let chart = null

const round1 = (v) => Math.round((Number(v) || 0) * 10) / 10

const totalWeight = computed(() =>
  Object.values(weights).reduce((sum, v) => sum + (Number(v) || 0), 0))

const allocHint = computed(() => {
  const diff = round1(100 - totalWeight.value)
  if (Math.abs(diff) < 0.5) return '权重已配满，可以生成报告'
  return diff > 0 ? `还差 ${diff}%` : `超出 ${Math.abs(diff)}%，请调低`
})

const productsByLevel = (lv) => products.value.filter(p => p.riskLevel === lv)

const formatDate = (val) => {
  if (!val) return ''
  const d = new Date(String(val).replace('T', ' ').replace(/-/g, '/'))
  if (Number.isNaN(d.getTime())) return String(val).slice(0, 10)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const clearAll = () => {
  products.value.forEach(p => { weights[p.code] = 0 })
}

const applyTemplate = () => {
  const lv = profile.value?.levelIndex || 1
  clearAll()
  Object.entries(TEMPLATES[lv] || {}).forEach(([code, w]) => { weights[code] = w })
  ElMessage.success(`已按 ${profile.value?.levelCode} 等级填入参考权重，可自行调整`)
}

const runAnalyze = async () => {
  const holdings = Object.entries(weights)
    .filter(([, w]) => Number(w) > 0)
    .map(([code, w]) => ({ code, weight: Number(w) }))
  if (!holdings.length) {
    ElMessage.warning('请先给标的分配权重')
    return
  }
  if (Math.abs(totalWeight.value - 100) > 0.5) {
    ElMessage.warning(`权重合计需为 100%，当前 ${round1(totalWeight.value)}%`)
    return
  }
  analyzing.value = true
  try {
    report.value = await analyzeSimPortfolio(holdings, amount.value)
    step.value = 2
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '诊断失败，请稍后重试')
  } finally {
    analyzing.value = false
  }
}

const save = async () => {
  saving.value = true
  try {
    const holdings = (report.value.holdings || []).map(h => ({ code: h.code, weight: h.weight }))
    const name = await ElMessageBox.prompt('给这份组合起个名字', '保存模拟组合', {
      inputValue: `${profile.value?.levelCode} 参考组合`,
      inputPattern: /\S+/,
      inputErrorMessage: '名字不能为空'
    }).then(r => r.value).catch(() => null)
    if (!name) return
    await saveSimPortfolio(name, holdings, report.value.amount)
    ElMessage.success('已保存')
    await loadPortfolios()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const removePortfolio = async (id) => {
  try {
    await deleteSimPortfolio(id)
    await loadPortfolios()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// 把诊断结论带去问答页，AI 就不用再问一遍配置是什么
const askAi = () => {
  const lines = (report.value.holdings || [])
    .map(h => `${h.name} ${round1(h.weight)}%`).join('、')
  const q = `我的风险等级是 ${report.value.levelCode}，模拟组合配置为：${lines}。`
    + `预期年化 ${report.value.expectedReturn}%、年化波动 ${report.value.volatility}%。`
    + `请点评这个配置的问题，并说明我应该提前定好哪些卖出条件。`
  router.push({ path: '/chat', query: { q } })
}

const renderChart = () => {
  const proj = report.value.projection
  if (!chartRef.value || !proj) return
  if (!chart) chart = echarts.init(chartRef.value)
  const line = (name, data, color, dashed) => ({
    name,
    type: 'line',
    smooth: true,
    symbol: 'none',
    data,
    lineStyle: { color, width: dashed ? 1.5 : 2.5, type: dashed ? 'dashed' : 'solid' },
    itemStyle: { color }
  })
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v) => Number(v).toLocaleString() + ' 元'
    },
    legend: { data: ['乐观', '中性', '悲观'], top: 0, textStyle: { color: '#666', fontSize: 12 } },
    grid: { left: 60, right: 16, top: 34, bottom: 28 },
    xAxis: {
      type: 'category',
      data: proj.labels,
      axisLine: { lineStyle: { color: 'rgba(64,158,255,0.2)' } },
      axisLabel: { color: '#999', fontSize: 11, interval: 1 }
    },
    yAxis: {
      type: 'value',
      scale: true,
      axisLabel: { color: '#999', fontSize: 11, formatter: (v) => (v / 10000).toFixed(1) + '万' },
      splitLine: { lineStyle: { color: 'rgba(64,158,255,0.1)' } }
    },
    series: [
      line('乐观', proj.optimistic, '#67c23a', true),
      line('中性', proj.neutral, '#409eff', false),
      line('悲观', proj.pessimistic, '#f56c6c', true)
    ]
  })
  chart.resize()
}

const disposeChart = () => {
  if (chart) {
    chart.dispose()
    chart = null
  }
}

const onResize = () => chart?.resize()

watch([step, report], async () => {
  if (step.value !== 2) {
    disposeChart()
    return
  }
  await nextTick()
  renderChart()
})

const loadPortfolios = async () => {
  try {
    portfolios.value = await getSimPortfolios() || []
  } catch (e) {
    portfolios.value = []
  }
}

onMounted(async () => {
  window.addEventListener('resize', onResize)
  try {
    const [list, prof] = await Promise.all([getSimProducts(), getSimProfile()])
    products.value = list || []
    profile.value = prof
    clearAll()
  } catch (e) {
    ElMessage.error('标的池加载失败，请稍后重试')
    profile.value = { hasProfile: false, reason: '数据加载失败，请刷新页面重试' }
  }
  loadPortfolios()
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  disposeChart()
})
</script>

<style scoped lang="scss">
/* 配色沿用风险测评页：蓝为主，绿橙红表示风险由低到高 */
$brand: #409eff;
$green: #67c23a;
$orange: #e6a23c;
$red: #f56c6c;
$line: rgba(64, 158, 255, 0.15);

.sim-container {
  position: relative;
  height: calc(100vh - 60px);
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(rgba(64, 158, 255, 0.05) 1px, transparent 1px),
      linear-gradient(90deg, rgba(64, 158, 255, 0.05) 1px, transparent 1px);
    background-size: 72px 72px;
    mask-image: radial-gradient(ellipse 90% 80% at 50% 30%, #000 30%, transparent 80%);
    pointer-events: none;
    z-index: 0;
  }
}

.page-head {
  position: relative;
  z-index: 2;
  flex-shrink: 0;
  border-bottom: 1px solid $line;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);

  .head-inner {
    padding: 16px 7vw;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
  }

  .head-title {
    display: flex;
    align-items: baseline;
    gap: 12px;
    min-width: 0;

    h1 { font-size: 22px; font-weight: 800; color: #333; margin: 0; white-space: nowrap; }
    .subtitle { font-size: 13px; color: #999; white-space: nowrap; }
  }
}

.head-steps {
  display: flex;
  gap: 8px;
  flex-shrink: 0;

  .step-chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 5px 12px;
    border-radius: 20px;
    font-size: 12.5px;
    color: #999;
    background: #fafbfc;
    border: 1px solid rgba(64, 158, 255, 0.12);
    transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);

    b {
      width: 17px;
      height: 17px;
      border-radius: 50%;
      display: grid;
      place-items: center;
      font-size: 11px;
      color: #fff;
      background: #c0c4cc;
    }

    &.done { color: $green; border-color: rgba(103, 194, 58, 0.3); b { background: $green; } }
    &.on {
      color: $brand;
      border-color: $brand;
      background: rgba(64, 158, 255, 0.08);
      b { background: $brand; }
    }
  }
}

.content {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px 7vw 48px;

  .inner { max-width: 760px; margin: 0 auto; }
}
.primary-btn {
  border: none;
  border-radius: 10px;
  padding: 12px 32px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(135deg, $brand, #66b1ff);
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.25);
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);

  &:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 22px rgba(64, 158, 255, 0.32); }
  &:disabled { background: #dcdfe6; box-shadow: none; cursor: not-allowed; }

  &.small { display: inline-flex; align-items: center; gap: 6px; padding: 9px 20px; font-size: 14px; }
}

.ghost-btn {
  border: 1px solid rgba(64, 158, 255, 0.25);
  background: #fff;
  border-radius: 10px;
  padding: 9px 20px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);

  &:hover:not(:disabled) { color: $brand; border-color: $brand; }
  &:disabled { color: #c0c4cc; border-color: rgba(64, 158, 255, 0.12); cursor: not-allowed; }

  &.tiny { padding: 5px 12px; font-size: 12.5px; border-radius: 8px; }
}

.intro-card {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.06), rgba(103, 194, 58, 0.04));
  border: 1px solid $line;
  border-radius: 20px;
  padding: 40px 32px;
  text-align: center;

  .intro-icon { font-size: 40px; line-height: 1; }

  h2 { margin: 16px 0 10px; font-size: 20px; font-weight: 700; color: #333; }

  .intro-desc {
    margin: 0 auto 20px;
    max-width: 540px;
    font-size: 14px;
    line-height: 1.9;
    color: #666;

    b { color: $brand; }
  }

  .intro-meta {
    display: flex;
    justify-content: center;
    flex-wrap: wrap;
    gap: 20px;
    margin-bottom: 26px;
    font-size: 13px;
    color: #999;

    span { display: inline-flex; align-items: center; gap: 5px; }
  }
}
.alloc-bar {
  padding: 12px 16px;
  margin-bottom: 14px;
  border-radius: 12px;
  border: 1px solid $line;
  background: #fff;

  .alloc-text {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-bottom: 8px;
    font-size: 13px;
    color: #666;

    b { font-size: 16px; color: $brand; }
    .alloc-hint { font-size: 12.5px; color: #999; }
  }

  .alloc-track {
    height: 6px;
    border-radius: 3px;
    background: rgba(64, 158, 255, 0.12);
    overflow: hidden;

    .alloc-fill {
      height: 100%;
      border-radius: 3px;
      background: linear-gradient(90deg, $brand, #66b1ff);
      transition: width 0.35s cubic-bezier(0.22, 1, 0.36, 1);
    }
  }

  &.ok {
    border-color: rgba(103, 194, 58, 0.35);
    .alloc-text b, .alloc-hint { color: $green; }
    .alloc-fill { background: linear-gradient(90deg, $green, #95d475); }
  }

  &.over {
    border-color: rgba(245, 108, 108, 0.35);
    .alloc-text b, .alloc-hint { color: $red; }
    .alloc-fill { background: linear-gradient(90deg, $red, #f89898); }
  }
}

.tool-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;

  .tool-label { font-size: 13px; color: #666; }
}

.pool-group {
  margin-bottom: 18px;

  .group-head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;

    .group-code {
      font-size: 12px;
      font-weight: 800;
      padding: 2px 8px;
      border-radius: 8px;
      color: #fff;
      background: $brand;
    }

    .group-name { font-size: 13.5px; font-weight: 600; color: #333; }
    .group-warn { margin-left: auto; font-size: 12px; color: $red; }
  }
}

/* 风险等级色阶：R1-R2 绿、R3 蓝、R4 橙、R5 红 */
.r-1, .r-2 { background: $green !important; }
.r-3 { background: $brand !important; }
.r-4 { background: $orange !important; }
.r-5 { background: $red !important; }
.pool-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  margin-bottom: 8px;
  border-radius: 12px;
  background: #fafbfc;
  border: 1px solid rgba(64, 158, 255, 0.12);
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);

  &:last-child { margin-bottom: 0; }
  &:hover { border-color: rgba(64, 158, 255, 0.3); }

  .pool-main { flex: 1; min-width: 0; }

  .pool-name { display: block; font-size: 14px; font-weight: 600; color: #333; }
  .pool-feature { display: block; margin-top: 2px; font-size: 12.5px; line-height: 1.6; color: #999; }
  .pool-num { display: block; margin-top: 3px; font-size: 12px; color: #c0c4cc; }

  .pool-input { width: 96px; flex-shrink: 0; }
  .pool-unit { font-size: 12.5px; color: #999; }

  &.picked {
    background: rgba(64, 158, 255, 0.06);
    border-color: $brand;
  }

  /* 超出等级的标的不隐藏：让用户自己撞一次上限，比藏起来更有教育意义 */
  &.exceed {
    border-style: dashed;

    &.picked {
      background: rgba(245, 108, 108, 0.06);
      border-color: $red;
    }
  }
}

.quiz-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
}
.result-hero {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 26px 28px;
  border-radius: 20px;
  border: 1px solid $line;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.1), rgba(103, 194, 58, 0.05));
  animation: fadeUp 0.32s cubic-bezier(0.22, 1, 0.36, 1);

  .hero-left {
    flex-shrink: 0;
    width: 96px;
    text-align: center;

    .hero-code { font-size: 34px; font-weight: 800; line-height: 1.1; color: $brand; }
    .hero-name { margin-top: 4px; font-size: 13px; color: #666; }
  }

  .hero-right {
    flex: 1;
    min-width: 0;

    .hero-summary { margin: 0 0 12px; font-size: 14.5px; line-height: 1.85; color: #666; }

    .hero-score {
      display: flex;
      flex-wrap: wrap;
      gap: 18px;
      font-size: 13px;
      color: #999;

      b { font-size: 16px; color: $brand; }
    }
  }

  &.bad {
    background: linear-gradient(135deg, rgba(245, 108, 108, 0.12), rgba(230, 162, 60, 0.06));
    .hero-code, .hero-score b { color: $red; }
  }
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: none; }
}

.result-block {
  margin-top: 16px;
  padding: 20px 22px;
  background: #fff;
  border: 1px solid $line;
  border-radius: 16px;

  .block-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;
    font-size: 15px;
    font-weight: 700;
    color: #333;

    .block-note { font-size: 12px; font-weight: 500; color: $brand; }
  }

  .block-text { margin: 12px 0 0; font-size: 12.5px; line-height: 1.8; color: #999; }
}

.result-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}
.diag-item {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  margin-bottom: 8px;
  border-radius: 12px;
  background: rgba(64, 158, 255, 0.05);
  border: 1px solid rgba(64, 158, 255, 0.15);

  &:last-child { margin-bottom: 0; }

  .diag-dot {
    flex-shrink: 0;
    width: 7px;
    height: 7px;
    margin-top: 6px;
    border-radius: 50%;
    background: $brand;
  }

  .diag-title { display: block; font-size: 14px; font-weight: 600; color: #333; }
  .diag-text { margin: 4px 0 0; font-size: 13px; line-height: 1.8; color: #666; }

  &.danger {
    background: rgba(245, 108, 108, 0.06);
    border-color: rgba(245, 108, 108, 0.28);
    .diag-dot { background: $red; }
    .diag-title { color: $red; }
  }

  &.warn {
    background: rgba(230, 162, 60, 0.06);
    border-color: rgba(230, 162, 60, 0.28);
    .diag-dot { background: $orange; }
  }

  &.ok {
    background: rgba(103, 194, 58, 0.06);
    border-color: rgba(103, 194, 58, 0.28);
    .diag-dot { background: $green; }
  }
}

.proj-chart {
  width: 100%;
  height: 260px;
}

.hold-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed rgba(64, 158, 255, 0.12);
  font-size: 13px;

  &:last-child { border-bottom: none; padding-bottom: 0; }

  .hold-code {
    flex-shrink: 0;
    font-size: 11px;
    font-weight: 800;
    padding: 2px 7px;
    border-radius: 7px;
    color: #fff;
  }

  .hold-name { flex: 1; font-weight: 600; color: #333; }
  .hold-cat { color: #c0c4cc; }
  .hold-weight { width: 54px; text-align: right; color: $brand; font-weight: 600; }
  .hold-amount { width: 96px; text-align: right; color: #666; }

  &.bad .hold-name { color: $red; }
}

.saved-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 0;
  border-bottom: 1px dashed rgba(64, 158, 255, 0.12);
  font-size: 13px;

  &:last-child { border-bottom: none; padding-bottom: 0; }

  .saved-name { flex: 1; font-weight: 600; color: #333; }

  .saved-tag {
    font-size: 11px;
    font-weight: 800;
    padding: 2px 7px;
    border-radius: 7px;
    color: #fff;
    background: $brand;
  }

  .saved-num { color: #666; }
  .saved-date { color: #c0c4cc; }

  .saved-del {
    color: #c0c4cc;
    cursor: pointer;
    transition: color 0.3s;

    &:hover { color: $red; }
  }
}

@media (max-width: 860px) {
  .head-steps { display: none; }
}
</style>
