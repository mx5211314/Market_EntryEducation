<template>
  <div class="assess-container">
    <div class="page-head">
      <div class="head-inner">
        <div class="head-title">
          <h1>投资者风险测评</h1>
          <span class="subtitle">依据《证券期货投资者适当性管理办法》</span>
        </div>
        <div
          v-if="latest?.exists && stage !== 'result'"
          class="head-chip"
          :class="'lv-' + latest.levelIndex"
          @click="viewLatest">
          <span class="chip-code">{{ latest.levelCode }}</span>
          <span>{{ latest.level }}</span>
          <el-icon class="chip-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </div>

    <div class="content">
      <div class="inner">
        <!-- ===== 开始页 ===== -->
        <template v-if="stage === 'intro'">
          <div class="intro-card">
            <div class="intro-icon">📊</div>
            <h2>{{ latest?.exists ? '重新评估您的风险承受能力' : '开始您的风险承受能力评估' }}</h2>
            <p class="intro-desc">
              共 {{ questions.length }} 道题，约 2 分钟完成。请如实作答，测评结果将用于判断您适合参与的业务范围，
              并作为 AI 助手给出建议时的依据。
            </p>
            <div class="intro-meta">
              <span><el-icon><Tickets /></el-icon> {{ questions.length }} 题</span>
              <span><el-icon><Clock /></el-icon> 约 2 分钟</span>
              <span><el-icon><Lock /></el-icon> 结果仅您可见</span>
            </div>

            <!-- 风险揭示书：正式业务里必须先签署才能出具评估结果，这里做成折叠+勾选 -->
            <div class="disclosure">
              <div class="disclosure-head" @click="disclosureOpen = !disclosureOpen">
                <el-icon><Document /></el-icon>
                <span class="disclosure-title">《投资者风险揭示书》</span>
                <span class="disclosure-toggle">{{ disclosureOpen ? '收起' : '展开阅读' }}</span>
              </div>
              <div class="disclosure-body" v-show="disclosureOpen">
                <p v-for="(t, i) in DISCLOSURE" :key="i"><b>{{ i + 1 }}.</b> {{ t }}</p>
              </div>
              <el-checkbox v-model="agreed" class="disclosure-check">
                我已阅读并理解上述风险揭示内容，本人所填信息均真实、准确
              </el-checkbox>
            </div>

            <button class="primary-btn" @click="startQuiz" :disabled="!questions.length || !agreed">
              {{ latest?.exists ? '重新测评' : '开始测评' }}
            </button>
            <p class="intro-foot" v-if="!agreed">请先确认风险揭示书后开始测评</p>
          </div>

          <!-- 到期提醒：券商的做法是过期后必须重测才能继续交易，这里做成醒目横条 -->
          <el-alert
            v-if="latest?.exists && latest.expired"
            type="warning"
            show-icon
            :closable="false"
            class="expire-alert"
            title="您的风险测评结果已过期"
            :description="`测评结果有效期为 ${latest.validMonths || 12} 个月，请重新完成测评以获得准确的建议。`" />
        </template>

        <!-- ===== 答题：一屏一题 ===== -->
        <template v-else-if="stage === 'quiz'">
          <div class="quiz-progress">
            <div class="progress-text">
              <span class="cur">第 {{ current + 1 }} 题</span>
              <span class="total">/ {{ questions.length }}</span>
              <span class="answered">已答 {{ answeredCount }} 题</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            </div>
            <!-- 题号格子：漏答时不用一题一题点回去，直接点格子跳 -->
            <div class="quiz-nav">
              <span
                v-for="(q, i) in questions"
                :key="q.id"
                class="nav-dot"
                :class="{
                  done: answers[i] != null,
                  now: i === current,
                  miss: showMissing && answers[i] == null
                }"
                @click="goTo(i)">
                {{ i + 1 }}
              </span>
            </div>
          </div>

          <el-alert
            v-if="showMissing && missingList.length"
            type="warning"
            show-icon
            :closable="false"
            class="miss-alert">
            <template #title>
              还有 {{ missingList.length }} 道题没作答，点题号直接跳过去：
              <span class="miss-link" v-for="n in missingList" :key="n" @click="goTo(n - 1)">
                第 {{ n }} 题
              </span>
            </template>
          </el-alert>

          <div class="quiz-card" :key="current" :class="{ warn: showMissing && answers[current] == null }">
            <span class="quiz-dim" v-if="currentQuestion.dimension">{{ currentQuestion.dimension }}</span>
            <p class="quiz-text">{{ currentQuestion.text }}</p>
            <div
              v-for="opt in currentQuestion.options"
              :key="opt.value"
              class="option-item"
              :class="{ picked: answers[current] === opt.value }"
              @click="pick(opt.value)">
              <span class="option-mark">{{ optionLabel(opt) }}</span>
              <span class="option-text">{{ opt.label }}</span>
              <el-icon class="option-check"><Select /></el-icon>
            </div>
          </div>

          <div class="quiz-actions">
            <div class="act-left">
              <button class="ghost-btn" @click="prev" :disabled="current === 0">上一题</button>
              <button class="ghost-btn" @click="next" :disabled="current === questions.length - 1">下一题</button>
            </div>
            <div class="act-right">
              <span class="quiz-tip">键盘 A-E 或 1-5 作答，← → 切题</span>
              <button
                v-if="allAnswered || current === questions.length - 1"
                class="primary-btn small"
                :disabled="submitting"
                @click="submit">
                {{ submitting ? '提交中...' : '提交测评' }}
              </button>
            </div>
          </div>
        </template>

        <!-- ===== 结果页 ===== -->
        <template v-else>
          <!-- 报告抬头：编号+时间+有效期，出了问题客服能靠编号定位到这份报告 -->
          <div class="report-bar">
            <div class="report-meta">
              <span class="report-no">报告编号 {{ result.reportNo || '—' }}</span>
              <span>生成时间 {{ formatDateTime(result.createdAt) }}</span>
              <span>有效期至 {{ formatDate(result.expiresAt) }}</span>
            </div>
            <span class="report-seal" :class="{ off: result.expired }">
              {{ result.expired ? '已过期' : '有效' }}
            </span>
          </div>

          <div class="result-hero" :class="'lv-' + result.levelIndex">
            <div class="hero-left">
              <div class="hero-code">{{ result.levelCode }}</div>
              <div class="hero-name">{{ result.level }}</div>
            </div>
            <div class="hero-right">
              <p class="hero-summary">{{ result.summary }}</p>
              <div class="hero-score">
                <span>得分 <b>{{ result.score }}</b> / {{ result.maxScore }}</span>
                <span v-if="result.expiresAt">有效期至 {{ formatDate(result.expiresAt) }}</span>
              </div>
            </div>
          </div>

          <!-- 四维雷达：解释「为什么我是这个等级」，比只给一个分数有说服力 -->
          <div class="result-block" v-if="(result.dimensions || []).length">
            <div class="block-title">
              四维能力分析
              <span class="block-note">得分率越高，可承受的风险越大</span>
            </div>
            <div class="radar-wrap">
              <div ref="radarRef" class="radar-chart"></div>
              <div class="dim-list">
                <div v-for="d in result.dimensions" :key="d.name" class="dim-item">
                  <div class="dim-head">
                    <span class="dim-name">{{ d.name }}</span>
                    <span class="dim-percent" :class="dimClass(d.percent)">{{ d.percent }}%</span>
                  </div>
                  <div class="dim-bar">
                    <div class="dim-fill" :class="dimClass(d.percent)" :style="{ width: d.percent + '%' }"></div>
                  </div>
                  <p class="dim-comment">{{ d.comment }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 五级刻度尺：让用户看到自己在整条风险谱上的位置 -->
          <div class="result-block">
            <div class="block-title">风险承受能力等级</div>
            <div class="level-scale">
              <div
                v-for="lv in LEVEL_SCALE"
                :key="lv.index"
                class="scale-item"
                :class="{ on: lv.index === result.levelIndex }">
                <span class="scale-code">{{ lv.code }}</span>
                <span class="scale-name">{{ lv.name }}</span>
              </div>
            </div>
          </div>

          <div class="result-block">
            <div class="block-title">
              可参与的产品风险等级
              <span class="block-note">最高 {{ result.maxProductLevel }}</span>
            </div>
            <div
              v-for="p in result.productLevels || []"
              :key="p.code"
              class="product-row"
              :class="{ off: !p.allowed }">
              <span class="product-code">{{ p.code }}</span>
              <span class="product-name">{{ p.name }}</span>
              <span class="product-flag">{{ p.allowed ? '可参与' : '超出等级' }}</span>
            </div>
            <p class="block-text">{{ result.suitableBiz }}</p>
          </div>

          <!-- 推荐学习路径：券商 App 到这里就结束了，我们把等级接回自己的知识库 -->
          <div class="result-block" v-if="(result.recommendCategories || []).length">
            <div class="block-title">为您推荐的学习路径</div>
            <div class="path-list">
              <div
                v-for="(cat, i) in result.recommendCategories"
                :key="cat"
                class="path-item"
                @click="goKnowledge(cat)">
                <span class="path-index">{{ i + 1 }}</span>
                <div class="path-main">
                  <span class="path-name">{{ cat }}</span>
                  <span class="path-desc">{{ PATH_DESC[cat] || '结合您的风险等级，优先阅读该分类文章' }}</span>
                </div>
                <el-icon class="path-arrow"><ArrowRight /></el-icon>
              </div>
            </div>
          </div>

          <div class="result-actions">
            <button class="primary-btn small" @click="askAi">
              <el-icon><ChatDotRound /></el-icon>
              问 AI 助手
            </button>
            <button class="ghost-btn" @click="restart">重新测评</button>
          </div>

          <div class="result-block" v-if="history.length">
            <div class="block-title">历史测评记录</div>
            <div class="history-row" v-for="h in history" :key="h.id">
              <span class="history-date">{{ formatDate(h.createdAt) }}</span>
              <span class="history-level">{{ h.level }}</span>
              <span class="history-score">{{ h.score }}{{ h.maxScore ? ' / ' + h.maxScore : '' }} 分</span>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Tickets, Clock, Lock, Select, ChatDotRound, Document } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getAssessmentQuestions,
  getAssessmentLatest,
  getAssessmentHistory,
  submitAssessment
} from '@/api/frontend'
import { useSuitabilityStore } from '@/stores/suitability'

const router = useRouter()
const suitability = useSuitabilityStore()

const DISCLOSURE = [
  '证券市场价格受宏观经济、政策、公司经营等多重因素影响，任何投资均可能出现本金损失，历史业绩不代表未来收益。',
  '本测评结果基于您所填写的信息生成，仅用于判断您的风险承受能力等级，不构成对任何具体产品的推荐或收益承诺。',
  '若您填写的信息不真实或发生重大变化（收入、资产、投资目标等），测评结果可能失真，请及时重新测评。',
  '测评结果有效期为 12 个月，超过有效期后请重新评估。',
  '本平台为投资者教育服务，不代客理财、不代客下单，请勿将本平台内容作为唯一投资依据。'
]

const LEVEL_SCALE = [
  { index: 1, code: 'C1', name: '保守型' },
  { index: 2, code: 'C2', name: '谨慎型' },
  { index: 3, code: 'C3', name: '稳健型' },
  { index: 4, code: 'C4', name: '积极型' },
  { index: 5, code: 'C5', name: '激进型' }
]

const PATH_DESC = {
  投资基础: '账户开立、交易时间、委托方式等入门规则',
  法规解读: '适当性管理、信息披露等监管要求',
  风险管理: '仓位控制、止损纪律与常见风险识别',
  产品分析: '基金、债券、可转债的收益与风险结构',
  交易规则: '涨跌幅限制、融资融券与衍生品交易机制'
}

const stage = ref('intro')
const questions = ref([])
const answers = ref([])
const current = ref(0)
const submitting = ref(false)
const result = ref({})
const latest = ref(null)
const history = ref([])
const showMissing = ref(false)
const agreed = ref(false)
const disclosureOpen = ref(false)
const radarRef = ref(null)
let radarChart = null

const currentQuestion = computed(() => questions.value[current.value] || { text: '', options: [] })
const progressPercent = computed(() => {
  if (!questions.value.length) return 0
  return Math.round((answeredCount.value / questions.value.length) * 100)
})
const answeredCount = computed(() => answers.value.filter(a => a != null).length)
const allAnswered = computed(() => questions.value.length > 0 && answeredCount.value === questions.value.length)
const missingList = computed(() =>
  answers.value.reduce((acc, a, i) => (a == null ? [...acc, i + 1] : acc), []))

const optionLabel = (opt) => {
  const idx = currentQuestion.value.options.findIndex(o => o.value === opt.value)
  return String.fromCharCode(65 + (idx < 0 ? 0 : idx))
}

const formatDate = (val) => {
  if (!val) return ''
  const d = new Date(String(val).replace('T', ' ').replace(/-/g, '/'))
  if (Number.isNaN(d.getTime())) return String(val).slice(0, 10)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const formatDateTime = (val) => {
  if (!val) return ''
  const d = new Date(String(val).replace('T', ' ').replace(/-/g, '/'))
  if (Number.isNaN(d.getTime())) return String(val).slice(0, 16).replace('T', ' ')
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const dimClass = (percent) => {
  if (percent < 40) return 'low'
  if (percent < 70) return 'mid'
  return 'high'
}

// 雷达图放在结果页，切回答题页时要销毁，否则第二次渲染会挂在已卸载的 DOM 上
const renderRadar = () => {
  const dims = result.value.dimensions || []
  if (!radarRef.value || !dims.length) return
  if (!radarChart) radarChart = echarts.init(radarRef.value)
  radarChart.setOption({
    tooltip: { trigger: 'item' },
    radar: {
      indicator: dims.map(d => ({ name: d.name, max: 100 })),
      radius: '68%',
      splitNumber: 4,
      axisName: { color: '#666', fontSize: 12 },
      splitLine: { lineStyle: { color: 'rgba(64, 158, 255, 0.18)' } },
      splitArea: { areaStyle: { color: ['rgba(64,158,255,0.03)', 'rgba(64,158,255,0.07)'] } },
      axisLine: { lineStyle: { color: 'rgba(64, 158, 255, 0.2)' } }
    },
    series: [{
      type: 'radar',
      symbolSize: 5,
      data: [{
        value: dims.map(d => d.percent),
        name: '得分率',
        itemStyle: { color: '#409eff' },
        lineStyle: { color: '#409eff', width: 2 },
        areaStyle: { color: 'rgba(64, 158, 255, 0.22)' }
      }]
    }]
  })
  radarChart.resize()
}

const disposeRadar = () => {
  if (radarChart) {
    radarChart.dispose()
    radarChart = null
  }
}

const onResize = () => radarChart?.resize()

watch([stage, result], async () => {
  if (stage.value !== 'result') {
    disposeRadar()
    return
  }
  await nextTick()
  renderRadar()
})

const loadQuestions = async () => {
  try {
    questions.value = await getAssessmentQuestions() || []
    answers.value = new Array(questions.value.length).fill(null)
  } catch (e) {
    ElMessage.error('题库加载失败，请稍后重试')
  }
}

const loadLatest = async () => {
  try {
    const res = await getAssessmentLatest()
    latest.value = res
  } catch (e) {
    latest.value = null
  }
}

const loadHistory = async () => {
  try {
    const res = await getAssessmentHistory({ pageNum: 1, pageSize: 5 })
    history.value = res?.records || []
  } catch (e) {
    history.value = []
  }
}

const startQuiz = () => {
  answers.value = new Array(questions.value.length).fill(null)
  current.value = 0
  showMissing.value = false
  stage.value = 'quiz'
}

// 每次自动跳题只保留一个定时器：点得快时两次点击会排两个定时器，各自 +1 就会整题跳过
let advanceTimer = null

const pick = (value) => {
  answers.value[current.value] = value
  if (advanceTimer) clearTimeout(advanceTimer)
  if (current.value < questions.value.length - 1) {
    advanceTimer = setTimeout(() => {
      advanceTimer = null
      // 已经全部答完时不再自动推进，避免把用户从正在改的题上带走
      if (!allAnswered.value) current.value += 1
    }, 180)
  }
}

const goTo = (index) => {
  if (index < 0 || index >= questions.value.length) return
  if (advanceTimer) clearTimeout(advanceTimer)
  advanceTimer = null
  current.value = index
}

const prev = () => goTo(current.value - 1)
const next = () => goTo(current.value + 1)

const submit = async () => {
  if (missingList.value.length) {
    showMissing.value = true
    goTo(missingList.value[0] - 1)
    ElMessage.warning(`还有 ${missingList.value.length} 道题未作答，已跳到第 ${missingList.value[0]} 题`)
    return
  }
  submitting.value = true
  try {
    result.value = await submitAssessment(answers.value, agreed.value)
    stage.value = 'result'
    // 结果刚出来就刷新全局适当性状态：导航红点和模拟引导的准入要立刻跟上
    suitability.load(true)
    await Promise.all([loadLatest(), loadHistory()])
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// PC 上用键盘答 12 道题比鼠标快得多：A-E / 1-5 选项，左右方向键切题
const onKeydown = (e) => {
  if (stage.value !== 'quiz' || submitting.value) return
  const key = e.key
  if (key === 'ArrowLeft') { prev(); return }
  if (key === 'ArrowRight') { next(); return }
  if (key === 'Enter' && allAnswered.value) { submit(); return }

  const opts = currentQuestion.value.options || []
  let idx = -1
  if (/^[1-9]$/.test(key)) idx = Number(key) - 1
  else if (/^[a-zA-Z]$/.test(key)) idx = key.toUpperCase().charCodeAt(0) - 65
  if (idx >= 0 && idx < opts.length) {
    e.preventDefault()
    pick(opts[idx].value)
  }
}

const viewLatest = () => {
  if (!latest.value?.exists) return
  result.value = latest.value
  stage.value = 'result'
  loadHistory()
}

// 重测要重新签一次风险揭示书，避免绕过签署环节直接出报告
const restart = () => {
  agreed.value = false
  disclosureOpen.value = false
  stage.value = 'intro'
}

const goKnowledge = (category) => {
  router.push({ path: '/knowledge', query: { category } })
}

// 带着等级跳到问答页并预填第一句，让 AI 的回答从一开始就贴合测评结果
const askAi = () => {
  const q = `我的风险测评结果是${result.value.level}（${result.value.levelCode}），`
    + `可参与的产品风险等级不超过 ${result.value.maxProductLevel}。请结合这个等级，给我一份适合的投资学习和资产配置建议。`
  router.push({ path: '/chat', query: { q } })
}

onMounted(async () => {
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('resize', onResize)
  await Promise.all([loadQuestions(), loadLatest()])
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('resize', onResize)
  disposeRadar()
  if (advanceTimer) clearTimeout(advanceTimer)
})
</script>

<style scoped lang="scss">
/* 配色与知识库、首页一致：蓝为主，绿橙点缀 */
$brand: #409eff;
$green: #67c23a;
$orange: #e6a23c;
$red: #f56c6c;
$line: rgba(64, 158, 255, 0.15);

.assess-container {
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

    h1 {
      font-size: 22px;
      font-weight: 800;
      color: #333;
      margin: 0;
      white-space: nowrap;
    }

    .subtitle {
      font-size: 13px;
      color: #999;
      white-space: nowrap;
    }
  }
}
/* 顶栏右侧的等级胶囊：已测过的用户一眼能看到自己的等级，点一下直接看结果 */
.head-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
  border: 1px solid rgba(64, 158, 255, 0.25);
  background: rgba(64, 158, 255, 0.08);
  color: $brand;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);

  &:hover {
    background: rgba(64, 158, 255, 0.14);
    transform: translateY(-1px);
  }

  .chip-code {
    font-size: 12px;
    font-weight: 800;
    padding: 1px 6px;
    border-radius: 8px;
    background: currentColor;
    color: #fff;
  }

  .chip-arrow { font-size: 12px; }

  &.lv-1, &.lv-2 { color: $green; border-color: rgba(103, 194, 58, 0.3); background: rgba(103, 194, 58, 0.08); }
  &.lv-4 { color: $orange; border-color: rgba(230, 162, 60, 0.3); background: rgba(230, 162, 60, 0.08); }
  &.lv-5 { color: $red; border-color: rgba(245, 108, 108, 0.3); background: rgba(245, 108, 108, 0.08); }
}

.content {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px 7vw 48px;

  .inner {
    max-width: 720px;
    margin: 0 auto;
  }
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

  &.small {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 9px 20px;
    font-size: 14px;
  }
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
}

.intro-card {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.06), rgba(103, 194, 58, 0.04));
  border: 1px solid $line;
  border-radius: 20px;
  padding: 40px 32px;
  text-align: center;

  .intro-icon { font-size: 40px; line-height: 1; }

  h2 {
    margin: 16px 0 10px;
    font-size: 20px;
    font-weight: 700;
    color: #333;
  }

  .intro-desc {
    margin: 0 auto 20px;
    max-width: 520px;
    font-size: 14px;
    line-height: 1.9;
    color: #666;
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

.expire-alert {
  margin-top: 16px;
  border-radius: 12px;
}

.intro-foot {
  margin: 10px 0 0;
  font-size: 12.5px;
  color: #c0c4cc;
}

.disclosure {
  margin: 0 0 22px;
  text-align: left;
  border: 1px solid rgba(64, 158, 255, 0.18);
  border-radius: 12px;
  background: #fff;
  overflow: hidden;

  .disclosure-head {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 14px;
    cursor: pointer;
    font-size: 13.5px;
    color: #333;
    background: rgba(64, 158, 255, 0.05);
    transition: background 0.3s cubic-bezier(0.22, 1, 0.36, 1);

    &:hover { background: rgba(64, 158, 255, 0.1); }

    .disclosure-title { font-weight: 600; }
    .disclosure-toggle { margin-left: auto; font-size: 12.5px; color: $brand; }
  }

  .disclosure-body {
    padding: 6px 16px 12px;
    max-height: 190px;
    overflow-y: auto;
    border-top: 1px dashed rgba(64, 158, 255, 0.15);

    p {
      margin: 10px 0 0;
      font-size: 13px;
      line-height: 1.85;
      color: #666;

      b { color: $brand; margin-right: 4px; }
    }
  }

  .disclosure-check {
    display: flex;
    align-items: flex-start;
    padding: 10px 14px 12px;
    border-top: 1px dashed rgba(64, 158, 255, 0.15);
    height: auto;

    :deep(.el-checkbox__label) {
      font-size: 13px;
      line-height: 1.7;
      white-space: normal;
      color: #666;
    }
  }
}
.report-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 16px;
  border-radius: 12px;
  border: 1px dashed rgba(64, 158, 255, 0.28);
  background: #fafbfc;

  .report-meta {
    flex: 1;
    display: flex;
    flex-wrap: wrap;
    gap: 6px 18px;
    font-size: 12.5px;
    color: #999;

    .report-no {
      font-weight: 700;
      color: #666;
      letter-spacing: 0.3px;
    }
  }

  .report-seal {
    flex-shrink: 0;
    padding: 3px 10px;
    border-radius: 8px;
    font-size: 12px;
    font-weight: 700;
    color: $green;
    border: 1px solid rgba(103, 194, 58, 0.35);
    background: rgba(103, 194, 58, 0.08);

    &.off {
      color: $red;
      border-color: rgba(245, 108, 108, 0.35);
      background: rgba(245, 108, 108, 0.08);
    }
  }
}

.quiz-dim {
  display: inline-block;
  margin-bottom: 10px;
  padding: 2px 9px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  color: $brand;
  background: rgba(64, 158, 255, 0.1);
}
.radar-wrap {
  display: flex;
  align-items: center;
  gap: 18px;

  .radar-chart {
    flex-shrink: 0;
    width: 260px;
    height: 240px;
  }

  .dim-list {
    flex: 1;
    min-width: 0;
  }
}

.dim-item {
  margin-bottom: 14px;

  &:last-child { margin-bottom: 0; }

  .dim-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-bottom: 5px;

    .dim-name { font-size: 13.5px; font-weight: 600; color: #333; }

    .dim-percent {
      font-size: 13px;
      font-weight: 700;
      color: $brand;

      &.low { color: $red; }
      &.mid { color: $orange; }
      &.high { color: $green; }
    }
  }

  .dim-bar {
    height: 5px;
    border-radius: 3px;
    background: rgba(64, 158, 255, 0.1);
    overflow: hidden;

    .dim-fill {
      height: 100%;
      border-radius: 3px;
      background: $brand;
      transition: width 0.5s cubic-bezier(0.22, 1, 0.36, 1);

      &.low { background: $red; }
      &.mid { background: $orange; }
      &.high { background: $green; }
    }
  }

  .dim-comment {
    margin: 6px 0 0;
    font-size: 12.5px;
    line-height: 1.7;
    color: #999;
  }
}

@media (max-width: 760px) {
  .radar-wrap {
    flex-direction: column;

    .radar-chart { width: 100%; }
  }
}
.quiz-progress {
  margin-bottom: 18px;

  .progress-text {
    display: flex;
    align-items: baseline;
    gap: 4px;
    margin-bottom: 8px;

    .cur { font-size: 15px; font-weight: 700; color: $brand; }
    .total { font-size: 13px; color: #999; }
    .answered { margin-left: auto; font-size: 12px; color: #999; }
  }

  .progress-bar {
    height: 6px;
    border-radius: 3px;
    background: rgba(64, 158, 255, 0.12);
    overflow: hidden;

    .progress-fill {
      height: 100%;
      border-radius: 3px;
      background: linear-gradient(90deg, $brand, $green);
      transition: width 0.35s cubic-bezier(0.22, 1, 0.36, 1);
    }
  }

  .quiz-nav {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 12px;

    .nav-dot {
      width: 26px;
      height: 26px;
      border-radius: 8px;
      display: grid;
      place-items: center;
      font-size: 12px;
      cursor: pointer;
      color: #999;
      background: #fafbfc;
      border: 1px solid rgba(64, 158, 255, 0.15);
      transition: all 0.25s cubic-bezier(0.22, 1, 0.36, 1);

      &:hover { border-color: $brand; color: $brand; }

      &.done {
        color: #fff;
        background: rgba(64, 158, 255, 0.75);
        border-color: transparent;
      }

      &.now {
        border-color: $brand;
        border-width: 2px;
        font-weight: 700;
        transform: translateY(-1px);
      }

      &.miss {
        color: $red;
        border-color: $red;
        background: rgba(245, 108, 108, 0.08);
      }
    }
  }
}

.miss-alert {
  margin-bottom: 14px;
  border-radius: 12px;

  .miss-link {
    margin-left: 6px;
    font-weight: 600;
    text-decoration: underline;
    cursor: pointer;

    &:hover { color: #333; }
  }
}

.quiz-card {
  background: #fff;
  border: 1px solid $line;
  border-radius: 16px;
  padding: 28px 24px;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.08);
  animation: fadeUp 0.32s cubic-bezier(0.22, 1, 0.36, 1);

  .quiz-text {
    margin: 0 0 20px;
    font-size: 17px;
    font-weight: 600;
    line-height: 1.7;
    color: #333;
  }

  /* 漏答的题跳回来时描红，用户不用猜是哪一题 */
  &.warn {
    border-color: rgba(245, 108, 108, 0.5);
    box-shadow: 0 4px 16px rgba(245, 108, 108, 0.12);
  }
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: none; }
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 10px;
  border: 1px solid rgba(64, 158, 255, 0.15);
  border-radius: 12px;
  cursor: pointer;
  background: #fafbfc;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);

  &:last-child { margin-bottom: 0; }

  &:hover {
    border-color: $brand;
    background: rgba(64, 158, 255, 0.05);
    transform: translateX(2px);
  }

  .option-mark {
    flex-shrink: 0;
    width: 26px;
    height: 26px;
    border-radius: 50%;
    display: grid;
    place-items: center;
    font-size: 13px;
    font-weight: 700;
    color: #666;
    background: rgba(64, 158, 255, 0.1);
  }

  .option-text { flex: 1; font-size: 14.5px; color: #333; }

  .option-check {
    font-size: 15px;
    color: $brand;
    opacity: 0;
    transition: opacity 0.2s;
  }

  &.picked {
    border-color: $brand;
    background: rgba(64, 158, 255, 0.09);
    box-shadow: 0 2px 10px rgba(64, 158, 255, 0.12);

    .option-mark { background: $brand; color: #fff; }
    .option-check { opacity: 1; }
  }
}

.quiz-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;

  .act-left, .act-right {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .quiz-tip { font-size: 12.5px; color: #c0c4cc; }
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

    .hero-code {
      font-size: 34px;
      font-weight: 800;
      line-height: 1.1;
      color: $brand;
    }

    .hero-name {
      margin-top: 4px;
      font-size: 15px;
      font-weight: 600;
      color: #333;
    }
  }

  .hero-right {
    flex: 1;
    min-width: 0;

    .hero-summary {
      margin: 0 0 12px;
      font-size: 14.5px;
      line-height: 1.85;
      color: #666;
    }

    .hero-score {
      display: flex;
      flex-wrap: wrap;
      gap: 18px;
      font-size: 13px;
      color: #999;

      b { font-size: 16px; color: $brand; }
    }
  }

  /* 等级越高底色越暖，和产品风险的直觉一致 */
  &.lv-1, &.lv-2 {
    background: linear-gradient(135deg, rgba(103, 194, 58, 0.12), rgba(64, 158, 255, 0.05));
    .hero-code { color: $green; }
    .hero-score b { color: $green; }
  }
  &.lv-4 {
    background: linear-gradient(135deg, rgba(230, 162, 60, 0.12), rgba(64, 158, 255, 0.05));
    .hero-code { color: $orange; }
    .hero-score b { color: $orange; }
  }
  &.lv-5 {
    background: linear-gradient(135deg, rgba(245, 108, 108, 0.12), rgba(230, 162, 60, 0.06));
    .hero-code { color: $red; }
    .hero-score b { color: $red; }
  }
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

  .block-text {
    margin: 12px 0 0;
    font-size: 13.5px;
    line-height: 1.85;
    color: #666;
  }
}
.level-scale {
  display: flex;
  gap: 8px;

  .scale-item {
    flex: 1;
    padding: 10px 4px;
    border-radius: 10px;
    text-align: center;
    background: #fafbfc;
    border: 1px solid rgba(64, 158, 255, 0.12);
    transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);

    .scale-code {
      display: block;
      font-size: 14px;
      font-weight: 800;
      color: #c0c4cc;
    }

    .scale-name {
      display: block;
      margin-top: 2px;
      font-size: 12px;
      color: #999;
    }

    &.on {
      background: linear-gradient(135deg, $brand, #66b1ff);
      border-color: $brand;
      box-shadow: 0 4px 14px rgba(64, 158, 255, 0.25);
      transform: translateY(-2px);

      .scale-code, .scale-name { color: #fff; }
    }
  }
}

.product-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 14px;
  margin-bottom: 8px;
  border-radius: 10px;
  background: rgba(103, 194, 58, 0.06);
  border: 1px solid rgba(103, 194, 58, 0.18);

  &:last-of-type { margin-bottom: 0; }

  .product-code {
    flex-shrink: 0;
    font-size: 12px;
    font-weight: 800;
    padding: 2px 8px;
    border-radius: 8px;
    color: #fff;
    background: $green;
  }

  .product-name { flex: 1; font-size: 13.5px; color: #333; }

  .product-flag { font-size: 12px; font-weight: 600; color: $green; }

  /* 超出等级的档位不隐藏，灰掉更能让用户看清边界在哪 */
  &.off {
    background: #fafbfc;
    border-color: rgba(64, 158, 255, 0.1);

    .product-code { background: #c0c4cc; }
    .product-name { color: #999; }
    .product-flag { color: #c0c4cc; font-weight: 500; }
  }
}
.path-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 14px;
  margin-bottom: 8px;
  border-radius: 12px;
  cursor: pointer;
  background: #fafbfc;
  border: 1px solid rgba(64, 158, 255, 0.12);
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);

  &:last-child { margin-bottom: 0; }

  &:hover {
    border-color: $brand;
    background: rgba(64, 158, 255, 0.05);
    transform: translateX(3px);

    .path-arrow { color: $brand; transform: translateX(2px); }
  }

  .path-index {
    flex-shrink: 0;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    display: grid;
    place-items: center;
    font-size: 12px;
    font-weight: 700;
    color: #fff;
    background: linear-gradient(135deg, $brand, #66b1ff);
  }

  .path-main {
    flex: 1;
    min-width: 0;

    .path-name {
      display: block;
      font-size: 14px;
      font-weight: 600;
      color: #333;
    }

    .path-desc {
      display: block;
      margin-top: 2px;
      font-size: 12.5px;
      color: #999;
    }
  }

  .path-arrow {
    font-size: 13px;
    color: #c0c4cc;
    transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  }
}

.result-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

.history-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px dashed rgba(64, 158, 255, 0.12);
  font-size: 13px;

  &:last-child { border-bottom: none; padding-bottom: 0; }

  .history-date { color: #999; }
  .history-level { flex: 1; font-weight: 600; color: #333; }
  .history-score { color: #666; }
}
</style>
