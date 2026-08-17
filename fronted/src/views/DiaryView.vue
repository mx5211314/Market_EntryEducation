<template>
  <div class="diary-container">
    <div class="page-head">
      <div class="head-inner">
        <div class="head-title">
          <h1>投资日记</h1>
          <span class="subtitle">写下决策与卖出条件，到期回来对账</span>
        </div>
        <button class="primary-btn small" @click="openCreate">
          <el-icon><EditPen /></el-icon>写日记
        </button>
      </div>
    </div>

    <div class="content" ref="contentRef">
      <div class="inner">
        <div class="kpi-row">
          <div class="kpi-card">
            <span class="kpi-num" :class="scoreClass(stats.disciplineScore)">{{ stats.disciplineScore || 0 }}</span>
            <span class="kpi-label">纪律分</span>
            <span class="kpi-hint">全部记录的平均分</span>
          </div>
          <div class="kpi-card">
            <span class="kpi-num">{{ stats.total || 0 }}</span>
            <span class="kpi-label">记录数</span>
            <span class="kpi-hint">已完成回顾 {{ stats.reviewedCount || 0 }} 条</span>
          </div>
          <div class="kpi-card">
            <span class="kpi-num">{{ stats.planRate || 0 }}%</span>
            <span class="kpi-label">写了卖出条件</span>
            <span class="kpi-hint">下单前就定好退出方式的比例</span>
          </div>
          <div class="kpi-card">
            <span class="kpi-num">{{ executeRateText }}</span>
            <span class="kpi-label">触发后执行</span>
            <span class="kpi-hint">条件触发时真的照做的比例</span>
          </div>
        </div>

        <!-- 到期提醒：这条横条是闭环的入口，不点开就不知道自己说到没做到 -->
        <div class="review-bar" v-if="pending.length">
          <div class="review-info">
            <el-icon class="bell"><BellFilled /></el-icon>
            <span class="review-main"><b>{{ pending.length }}</b> 条记录到了回顾时间</span>
            <span class="review-hint">
              {{ pending[0].symbol || pending[0].title || '未命名' }}：{{ pending[0].sellPlan }}
            </span>
          </div>
          <button class="primary-btn small" @click="openReview(pending[0])">开始对账</button>
        </div>

        <div class="main-grid">
          <div class="col-left">
            <div class="col-head">
              <span class="col-title">时间线</span>
              <span class="col-note">共 {{ total }} 条<template v-if="total > pageSize">，第 {{ pageNum }} / {{ pageCount }} 页</template></span>
            </div>

            <div class="empty-state" v-if="!diaries.length">
              <div class="empty-icon">📝</div>
              <p>还没有记录。下次下单前先写一条，把卖出条件定下来。</p>
              <button class="primary-btn small" @click="openCreate">写第一条</button>
            </div>

            <div v-for="d in diaries" :key="d.id" class="diary-card">
              <div class="card-head">
                <div class="card-main">
                  <span class="dir-tag" :class="dirClass(d.direction)">{{ d.direction || '记录' }}</span>
                  <b class="card-symbol">{{ d.symbol || d.title || '未命名' }}</b>
                  <span class="card-ratio" v-if="d.positionRatio">投入 {{ d.positionRatio }}%</span>
                </div>
                <div class="card-side">
                  <span class="score-chip" :class="scoreClass(d.disciplineScore)">
                    纪律 {{ d.disciplineScore === null || d.disciplineScore === undefined ? '—' : d.disciplineScore }}
                  </span>
                  <span class="card-time">{{ formatDate(d.createdAt) }}</span>
                </div>
              </div>

              <div class="card-sub" v-if="d.symbol && d.title">{{ d.title }}</div>
              <p class="card-content" v-if="d.content">{{ d.content }}</p>

              <div class="tag-row" v-if="reasonList(d).length">
                <span v-for="t in reasonList(d)" :key="t" class="reason-tag" :class="{ bad: IRRATIONAL.includes(t) }">
                  {{ t }}
                </span>
                <span class="hold-tag" v-if="d.expectHoldDays">计划持有 {{ d.expectHoldDays }} 天</span>
              </div>

              <div class="plan-box" v-if="d.sellPlan">
                <span class="plan-label">卖出条件</span>
                <span class="plan-text">{{ d.sellPlan }}</span>
              </div>
              <div class="bias-row" v-if="biasList(d).length">
                <el-icon><WarningFilled /></el-icon>
                <span v-for="b in biasList(d)" :key="b" class="bias-tag" @click="goLearn(b)">{{ b }}</span>
              </div>

              <div class="review-box" v-if="d.reviewedAt" :class="reviewClass(d)">
                <div class="rb-head">
                  <span class="rb-title">{{ reviewTitle(d) }}</span>
                  <span class="rb-time">{{ formatDate(d.reviewedAt) }} 回顾</span>
                </div>
                <div class="rb-meta">
                  <span>自报结果：{{ d.resultTag || '未填' }}</span>
                  <span v-if="d.mood">当时情绪：{{ d.mood }}<template v-if="d.moodScore"> {{ d.moodScore }}/10</template></span>
                </div>
                <p class="rb-note" v-if="d.reviewNote">{{ d.reviewNote }}</p>
              </div>

              <div class="ai-box" v-if="d.aiReview">
                <div class="ai-head"><el-icon><MagicStick /></el-icon>复盘教练</div>
                <p class="ai-text">{{ d.aiReview }}</p>
              </div>

              <div class="card-foot">
                <span class="due-text" v-if="d.reviewDueAt && !d.reviewedAt" :class="{ over: isDue(d) }">
                  {{ dueText(d) }}
                </span>
                <span class="due-text" v-else-if="!d.sellPlan">快速记录，未进入对账流程</span>
                <div class="foot-btns">
                  <button class="ghost-btn tiny" v-if="d.reviewDueAt && !d.reviewedAt" @click="openReview(d)">去对账</button>
                  <button class="ghost-btn tiny" :disabled="coaching === d.id" @click="askCoach(d)">
                    {{ coaching === d.id ? '分析中…' : 'AI 复盘' }}
                  </button>
                  <button class="ghost-btn tiny" @click="openEdit(d)">编辑</button>
                  <button class="ghost-btn tiny danger" @click="removeItem(d)">删除</button>
                </div>
              </div>

            </div>

            <div class="pager" v-if="total > pageSize">
              <el-pagination
                background
                layout="prev, pager, next"
                :page-size="pageSize"
                :total="total"
                :current-page="pageNum"
                @current-change="changePage" />
            </div>
          </div>
          <div class="col-right">
            <div class="side-card">
              <div class="side-title">纪律分</div>
              <div class="ring-row">
                <div class="ring" :class="scoreClass(stats.disciplineScore)">
                  <span class="ring-num">{{ stats.disciplineScore || 0 }}</span>
                  <span class="ring-unit">分</span>
                </div>
                <div class="ring-desc">
                  <p class="ring-level">{{ scoreText(stats.disciplineScore) }}</p>
                  <p class="ring-note">写了卖出条件 +20，条件触发后照做 +20，理由含听消息/跟风/情绪冲动要扣分。</p>
                </div>
              </div>
              <div ref="trendRef" class="trend-chart" v-show="(stats.trend || []).length > 1"></div>
            </div>

            <div class="side-card" v-if="dirBars.length">
              <div class="side-title">
                操作分布
                <span class="side-count">近 {{ total }} 条</span>
              </div>
              <div v-for="d in dirBars" :key="d.name" class="dir-bar">
                <span class="db-name">{{ d.name }}</span>
                <div class="db-track">
                  <div class="db-fill" :class="dirClass(d.name)" :style="{ width: d.pct + '%' }"></div>
                </div>
                <b class="db-num">{{ d.count }}</b>
              </div>
            </div>

            <div class="side-card" v-if="(stats.bias || []).length">
              <div class="side-title">
                常见偏差
                <span class="side-count">{{ stats.bias.length }} 类</span>
              </div>
              <div v-for="b in stats.bias" :key="b.name" class="side-bias" @click="goLearn(b.name)">
                <div class="sb-head">
                  <span class="sb-name">{{ b.name }}</span>
                  <b class="sb-count">{{ b.count }} 次</b>
                </div>
                <p class="sb-advice">{{ b.advice }}</p>
                <span class="sb-link">去知识库看「{{ b.keyword || b.name }}」</span>
              </div>
            </div>
          </div>


        </div>
      </div>
    </div>
    <!-- 记录抽屉：快速档只要一句话，完整档才展开结构化字段，避免每次都填一大张表 -->
    <el-drawer v-model="drawer" :title="form.id ? '编辑记录' : '写投资日记'" size="520px">
      <div class="form-body">
        <div class="mode-row">
          <el-radio-group v-model="form.recordType" size="small">
            <el-radio-button label="quick">快速记录</el-radio-button>
            <el-radio-button label="full">完整复盘</el-radio-button>
          </el-radio-group>
          <span class="mode-note">
            {{ form.recordType === 'full' ? '会按预期持有期安排到期对账' : '只记一句想法，不进入对账流程' }}
          </span>
        </div>

        <div class="field">
          <label>标题</label>
          <el-input v-model="form.title" maxlength="60" placeholder="一句话概括这次的决定" />
        </div>

        <div class="field-row">
          <div class="field">
            <label>标的</label>
            <el-input v-model="form.symbol" maxlength="40" placeholder="如 沪深300ETF" />
          </div>
          <div class="field">
            <label>操作</label>
            <el-select v-model="form.direction" placeholder="选择操作">
              <el-option v-for="d in DIRECTIONS" :key="d" :label="d" :value="d" />
            </el-select>
          </div>
        </div>

        <template v-if="form.recordType === 'full'">
          <div class="field-row">
            <div class="field">
              <label>投入占比（%）</label>
              <el-input-number v-model="form.positionRatio" :min="0" :max="100" :step="5"
                controls-position="right" class="wide" />
            </div>
            <div class="field">
              <label>计划持有（天）</label>
              <el-input-number v-model="form.expectHoldDays" :min="1" :max="1825" :step="7"
                controls-position="right" class="wide" />
            </div>
          </div>

          <div class="field">
            <label>这次买入/卖出的理由</label>
            <el-checkbox-group v-model="form.reasonTags" class="reason-group">
              <el-checkbox v-for="t in reasonOptions" :key="t" :label="t" :class="{ bad: IRRATIONAL.includes(t) }" />
            </el-checkbox-group>
            <p class="field-hint">后三项是非理性来源，选了会扣纪律分——不是不让选，是让你事后看得见。</p>
          </div>

          <div class="field">
            <label>卖出条件<span class="req">必填</span></label>
            <el-input v-model="form.sellPlan" type="textarea" :rows="3" maxlength="300" show-word-limit
              placeholder="写成可判断的条件，例如：跌破 20 日均线离场，或浮亏超过 10% 无条件止损" />
            <p class="field-hint">到期时系统会拿这句话问你：触发了吗？你照做了吗？</p>
          </div>
        </template>

        <div class="field">
          <label>当时情绪</label>
          <div class="mood-grid">
            <div v-for="m in MOODS" :key="m.name" class="mood-chip" :class="{ on: form.mood === m.name }"
              @click="pickMood(m)">
              <span class="mood-icon">{{ m.icon }}</span>{{ m.name }}
            </div>
          </div>
          <div class="mood-score">
            <span class="ms-label">情绪强度</span>
            <el-slider v-model="form.moodScore" :min="1" :max="10" :marks="{ 1: '低落', 10: '亢奋' }" />
          </div>
        </div>

        <div class="field">
          <label>过程与想法</label>
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="2000" show-word-limit
            placeholder="当时看到了什么、担心什么、和上次的判断有什么不同" />
        </div>
      </div>
      <template #footer>
        <button class="ghost-btn" @click="drawer = false">取消</button>
        <button class="primary-btn small" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </template>
    </el-drawer>

    <el-dialog v-model="reviewDialog" title="到期对账" width="480px">
      <div class="review-form" v-if="reviewTarget">
        <div class="rf-plan">
          <span class="rf-label">你当时写的卖出条件</span>
          <p>{{ reviewTarget.sellPlan }}</p>
          <span class="rf-meta">
            记于 {{ formatDate(reviewTarget.createdAt) }}，计划持有 {{ reviewTarget.expectHoldDays || '—' }} 天
          </span>
        </div>

        <div class="field">
          <label>这个条件触发了吗？</label>
          <el-radio-group v-model="reviewForm.triggered">
            <el-radio-button :label="true">触发了</el-radio-button>
            <el-radio-button :label="false">没触发</el-radio-button>
          </el-radio-group>
        </div>

        <div class="field" v-if="reviewForm.triggered">
          <label>你按条件执行了吗？</label>
          <el-radio-group v-model="reviewForm.executed">
            <el-radio-button :label="true">照做了</el-radio-button>
            <el-radio-button :label="false">没照做</el-radio-button>
          </el-radio-group>
          <p class="field-hint" v-if="reviewForm.executed === false">
            这一条会记为「说到做不到」并扣 20 分。它不是惩罚，是让你下次把条件定得更能执行。
          </p>
        </div>

        <div class="field">
          <label>这笔的结果</label>
          <el-radio-group v-model="reviewForm.resultTag">
            <el-radio-button v-for="r in RESULTS" :key="r" :label="r">{{ r }}</el-radio-button>
          </el-radio-group>
        </div>

        <div class="field">
          <label>补充说明</label>
          <el-input v-model="reviewForm.note" type="textarea" :rows="3" maxlength="300"
            placeholder="当时为什么这么做，下次要改什么" />
        </div>
      </div>
      <template #footer>
        <button class="ghost-btn" @click="reviewDialog = false">稍后再说</button>
        <button class="primary-btn small" :disabled="reviewing" @click="submitReview">
          {{ reviewing ? '提交中…' : '提交对账' }}
        </button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, BellFilled, WarningFilled, MagicStick } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getDiaryList, getDiaryStats, getDiaryPending, getDiaryOptions,
  createDiary, updateDiary, removeDiary, reviewDiary, coachDiary
} from '@/api/frontend'

const router = useRouter()

const DIRECTIONS = ['买入', '加仓', '减仓', '卖出', '持仓观察']
const RESULTS = ['盈利', '亏损', '持平']
// 与后端 DiaryService.IRRATIONAL_REASONS 对应，选中会扣纪律分
const IRRATIONAL = ['听消息', '跟风热点', '情绪冲动']
const MOODS = [
  { name: '冷静', icon: '😌', score: 6 },
  { name: '乐观', icon: '🙂', score: 7 },
  { name: '兴奋', icon: '🤩', score: 9 },
  { name: '犹豫', icon: '🤔', score: 5 },
  { name: '焦虑', icon: '😰', score: 3 },
  { name: '后悔', icon: '😣', score: 2 }
]

const diaries = ref([])
const pending = ref([])
const stats = ref({})
const pageNum = ref(1)
const pageSize = ref(5)
const total = ref(0)
const contentRef = ref(null)
const reasonOptions = ref(['基本面', '技术面', '估值便宜', '长期配置', ...IRRATIONAL])
const drawer = ref(false)
const saving = ref(false)
const coaching = ref(null)
const reviewDialog = ref(false)
const reviewTarget = ref(null)
const reviewing = ref(false)
const trendRef = ref(null)
let trendChart = null

const emptyForm = () => ({
  id: null, recordType: 'full', title: '', symbol: '', direction: '买入',
  positionRatio: 10, expectHoldDays: 30, reasonTags: [], sellPlan: '',
  mood: '冷静', moodScore: 6, content: ''
})
const form = ref(emptyForm())
const reviewForm = reactive({ triggered: true, executed: true, resultTag: '持平', note: '' })
const executeRateText = computed(() => {
  const r = stats.value.executeRate
  return r === undefined || r === null || r < 0 ? '暂无' : r + '%'
})
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
// 横条比饼图省一半高度，侧栏要钉住就不能太长；条宽按最多的那种操作归一化
const dirBars = computed(() => {
  const dir = stats.value.direction || {}
  const rows = Object.entries(dir).map(([name, count]) => ({ name, count }))
  if (!rows.length) return []
  const max = Math.max(...rows.map(r => r.count))
  return rows.sort((a, b) => b.count - a.count)
    .map(r => ({ ...r, pct: Math.round(r.count * 100 / max) }))
})

const scoreClass = (s) => {
  if (!s) return 'mid'
  if (s >= 80) return 'high'
  if (s >= 60) return 'mid'
  return 'low'
}
const scoreText = (s) => {
  if (!s) return '还没有足够记录'
  if (s >= 80) return '纪律良好，计划基本能落地'
  if (s >= 60) return '有计划，但执行还有空间'
  return '决策偏随性，先从写卖出条件开始'
}
const dirClass = (d) => ({ 买入: 'buy', 加仓: 'buy', 减仓: 'sell', 卖出: 'sell' }[d] || 'hold')
const reasonList = (d) => (d.reasonTags || '').split(',').filter(Boolean)
const biasList = (d) => (d.biasTags || '').split(',').filter(Boolean)

const pad = (n) => String(n).padStart(2, '0')
const formatDate = (s) => {
  if (!s) return '—'
  const d = new Date(s)
  return isNaN(d.getTime()) ? '—' : `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
const isDue = (d) => {
  if (!d.reviewDueAt) return false
  const due = new Date(d.reviewDueAt)
  return !isNaN(due.getTime()) && due <= new Date()
}
const dueText = (d) => {
  const due = new Date(d.reviewDueAt)
  if (isNaN(due.getTime())) return ''
  const days = Math.ceil((due - new Date()) / 86400000)
  return days <= 0 ? `已到回顾时间 ${Math.abs(days)} 天` : `${days} 天后回顾`
}
const reviewTitle = (d) => {
  if (!d.reviewTriggered) return '条件未触发，按计划继续持有'
  return d.reviewExecuted ? '条件触发，照计划执行了' : '条件触发，但没有执行'
}
const reviewClass = (d) => {
  if (!d.reviewTriggered) return 'ok'
  return d.reviewExecuted ? 'ok' : 'bad'
}
const pickMood = (m) => {
  form.value.mood = m.name
  form.value.moodScore = m.score
}
// 偏差 -> 知识库检索词的映射在后端 DiaryService.BIAS_KEYWORD，前端只负责带过去
const goLearn = (biasName) => {
  const hit = (stats.value.bias || []).find(b => b.name === biasName)
  router.push({
    path: '/knowledge',
    query: { keyword: hit?.keyword || biasName, from: 'diary', bias: biasName }
  })
}
const loadAll = async () => {
  try {
    const [page, s, p] = await Promise.all([
      getDiaryList({ pageNum: pageNum.value, pageSize: pageSize.value }),
      getDiaryStats(),
      getDiaryPending()
    ])
    diaries.value = page?.records || []
    total.value = page?.total || 0
    // 删到当前页空了就退回上一页，否则页面看着像没数据
    if (!diaries.value.length && pageNum.value > 1) {
      pageNum.value = Math.min(pageNum.value - 1, pageCount.value)
      return loadAll()
    }
    stats.value = s || {}
    pending.value = p || []
    await nextTick()
    renderCharts()
  } catch (e) {
    ElMessage.error('加载日记失败，请稍后重试')
  }
}

const changePage = async (p) => {
  pageNum.value = p
  await loadAll()
  contentRef.value?.scrollTo({ top: 0, behavior: 'smooth' })
}

const openCreate = () => {
  form.value = emptyForm()
  drawer.value = true
}

const openEdit = (d) => {
  form.value = {
    id: d.id,
    recordType: d.recordType === 'full' ? 'full' : 'quick',
    title: d.title || '',
    symbol: d.symbol || '',
    direction: d.direction || '买入',
    positionRatio: d.positionRatio ?? 10,
    expectHoldDays: d.expectHoldDays ?? 30,
    reasonTags: reasonList(d),
    sellPlan: d.sellPlan || '',
    mood: d.mood || '冷静',
    moodScore: d.moodScore ?? 6,
    content: d.content || ''
  }
  drawer.value = true
}

const save = async () => {
  const f = form.value
  if (!f.title && !f.content) {
    ElMessage.warning('至少写个标题或内容')
    return
  }
  if (f.recordType === 'full' && !f.sellPlan.trim()) {
    ElMessage.warning('完整复盘必须写卖出条件，否则到期没法对账')
    return
  }
  saving.value = true
  try {
    if (f.id) await updateDiary(f.id, f)
    else await createDiary(f)
    ElMessage.success('保存成功')
    drawer.value = false
    // 新记录排在最前，编辑则留在当前页
    if (!f.id) pageNum.value = 1
    await loadAll()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
const removeItem = async (d) => {
  const ok = await ElMessageBox.confirm('删除后这条决策的留痕就没有了，确定删除？', '提示', { type: 'warning' })
    .then(() => true).catch(() => false)
  if (!ok) return
  try {
    await removeDiary(d.id)
    ElMessage.success('已删除')
    await loadAll()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const askCoach = async (d) => {
  coaching.value = d.id
  try {
    const res = await coachDiary(d.id)
    d.aiReview = res.review
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || 'AI 复盘失败，请稍后再试')
  } finally {
    coaching.value = null
  }
}

const openReview = (d) => {
  reviewTarget.value = d
  reviewForm.triggered = true
  reviewForm.executed = true
  reviewForm.resultTag = '持平'
  reviewForm.note = ''
  reviewDialog.value = true
}

const submitReview = async () => {
  reviewing.value = true
  try {
    const saved = await reviewDiary(reviewTarget.value.id, { ...reviewForm })
    ElMessage.success(`对账完成，这条记录的纪律分 ${saved.disciplineScore} 分`)
    reviewDialog.value = false
    await loadAll()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '提交失败')
  } finally {
    reviewing.value = false
  }
}
const renderCharts = () => {
  const trend = stats.value.trend || []
  if (trendRef.value && trend.length > 1) {
    if (!trendChart) trendChart = echarts.init(trendRef.value)
    trendChart.setOption({
      grid: { left: 32, right: 12, top: 16, bottom: 24 },
      tooltip: { trigger: 'axis', valueFormatter: (v) => v + ' 分' },
      xAxis: {
        type: 'category',
        data: trend.map(t => t.date),
        axisLabel: { fontSize: 10, color: '#999' },
        axisLine: { lineStyle: { color: 'rgba(64,158,255,0.2)' } }
      },
      yAxis: {
        type: 'value', min: 0, max: 100,
        axisLabel: { fontSize: 10, color: '#999' },
        splitLine: { lineStyle: { color: 'rgba(64,158,255,0.08)' } }
      },
      series: [{
        type: 'line', smooth: true, symbolSize: 5,
        data: trend.map(t => t.score),
        lineStyle: { width: 2, color: '#409eff' },
        itemStyle: { color: '#409eff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.25)' },
            { offset: 1, color: 'rgba(64,158,255,0.02)' }
          ])
        }
      }]
    })
  }
}
const onResize = () => {
  trendChart?.resize()
}

// 图表容器挂在 v-if 的侧栏里，数据变了要等 DOM 出来再画
watch(stats, async () => {
  await nextTick()
  renderCharts()
})

onMounted(async () => {
  window.addEventListener('resize', onResize)
  try {
    const opts = await getDiaryOptions()
    if (opts?.reasonTags?.length) reasonOptions.value = opts.reasonTags
  } catch (e) {
    // 选项拿不到就用前端的默认清单，不影响记录
  }
  await loadAll()
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  trendChart?.dispose()
  trendChart = null
})
</script>

<style scoped lang="scss">
/* 配色沿用风险测评与模拟引导两页：蓝为主，绿=守住纪律，橙红=需要注意 */
$brand: #409eff;
$green: #67c23a;
$orange: #e6a23c;
$red: #f56c6c;
$line: rgba(64, 158, 255, 0.15);

.diary-container {
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
    mask-image: radial-gradient(ellipse 90% 80% at 50% 20%, #000 30%, transparent 80%);
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
    flex: 1;

    h1 { font-size: 22px; font-weight: 800; color: #333; margin: 0; white-space: nowrap; }
    .subtitle { font-size: 13px; color: #999; }
  }
}
/* KPI 从顶栏移到内容区第一屏：顶栏只留标题和入口，四个数字才有地方把口径写清楚 */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;

  .kpi-card {
    padding: 16px 18px;
    border-radius: 14px;
    background: linear-gradient(135deg, rgba(64, 158, 255, 0.06), rgba(103, 194, 58, 0.03));
    border: 1px solid $line;
    transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);

    &:hover { box-shadow: 0 4px 16px rgba(64, 158, 255, 0.1); transform: translateY(-1px); }
  }

  .kpi-num {
    display: block;
    font-size: 28px;
    font-weight: 800;
    color: #333;
    line-height: 1.1;

    &.high { color: $green; }
    &.mid { color: $brand; }
    &.low { color: $orange; }
  }

  .kpi-label {
    display: block;
    margin-top: 4px;
    font-size: 13px;
    font-weight: 600;
    color: #555;
  }

  .kpi-hint { display: block; margin-top: 2px; font-size: 11.5px; color: #b0b4bc; }
}

.content {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px 7vw 48px;

  .inner { max-width: 1180px; margin: 0 auto; }
}

.main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 336px;
  gap: 22px;
  align-items: start;
}
/* 侧栏跟着滚动条钉住：时间线可能很长，纪律分和偏差是边看边对照的参照物 */
.col-right {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

@media (max-width: 1100px) {
  .main-grid { grid-template-columns: minmax(0, 1fr); }
  .col-right { position: static; }
  .kpi-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
.review-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding: 14px 18px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(230, 162, 60, 0.12), rgba(230, 162, 60, 0.04));
  border: 1px solid rgba(230, 162, 60, 0.3);

  .review-info {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
  }

  .bell { color: $orange; font-size: 17px; }

  .review-main {
    font-size: 14px;
    color: #333;
    font-weight: 600;
    white-space: nowrap;

    b { color: $orange; font-size: 16px; }
  }

  .review-hint {
    font-size: 12.5px;
    color: #999;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.col-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;

  .col-title { font-size: 15px; font-weight: 700; color: #333; }
  .col-note { font-size: 12.5px; color: #999; }
}
.diary-card {
  background: #fff;
  border: 1px solid $line;
  border-radius: 16px;
  padding: 16px 18px;
  margin-bottom: 14px;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);

  &:hover { box-shadow: 0 4px 16px rgba(64, 158, 255, 0.1); transform: translateY(-1px); }

  .card-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .card-main {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  .card-symbol { font-size: 15.5px; font-weight: 700; color: #333; }
  .card-ratio { font-size: 12px; color: #999; }

  .card-side { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
  .card-time { font-size: 12px; color: #c0c4cc; }

  .card-sub { margin-top: 6px; font-size: 13.5px; color: #666; font-weight: 600; }

  .card-content {
    margin: 8px 0 0;
    font-size: 13.5px;
    line-height: 1.85;
    color: #666;
    white-space: pre-wrap;
  }
}

.dir-tag {
  padding: 3px 9px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;

  &.buy { background: rgba(103, 194, 58, 0.12); color: $green; }
  &.sell { background: rgba(245, 108, 108, 0.12); color: $red; }
  &.hold { background: rgba(64, 158, 255, 0.1); color: $brand; }
}
.score-chip {
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;

  &.high { background: rgba(103, 194, 58, 0.12); color: $green; }
  &.mid { background: rgba(64, 158, 255, 0.1); color: $brand; }
  &.low { background: rgba(230, 162, 60, 0.14); color: $orange; }
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;

  .reason-tag {
    padding: 3px 9px;
    border-radius: 6px;
    font-size: 12px;
    background: #f5f7fa;
    color: #666;
    border: 1px solid rgba(64, 158, 255, 0.1);

    &.bad { background: rgba(245, 108, 108, 0.08); color: $red; border-color: rgba(245, 108, 108, 0.2); }
  }

  .hold-tag {
    padding: 3px 9px;
    border-radius: 6px;
    font-size: 12px;
    color: $brand;
    background: rgba(64, 158, 255, 0.08);
  }
}

.plan-box {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(64, 158, 255, 0.05);
  border-left: 3px solid $brand;

  .plan-label { display: block; font-size: 11.5px; color: $brand; font-weight: 600; margin-bottom: 3px; }
  .plan-text { font-size: 13px; color: #555; line-height: 1.7; }
}
.bias-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
  color: $orange;
  font-size: 13px;

  .bias-tag {
    padding: 3px 9px;
    border-radius: 6px;
    font-size: 12px;
    background: rgba(230, 162, 60, 0.12);
    color: #b8801f;
    cursor: pointer;
    transition: background 0.2s;

    &:hover { background: rgba(230, 162, 60, 0.24); }
  }
}

.review-box {
  margin-top: 12px;
  padding: 11px 13px;
  border-radius: 10px;
  border: 1px solid transparent;

  &.ok { background: rgba(103, 194, 58, 0.07); border-color: rgba(103, 194, 58, 0.22); }
  &.bad { background: rgba(245, 108, 108, 0.07); border-color: rgba(245, 108, 108, 0.22); }

  .rb-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 10px;
  }

  .rb-title { font-size: 13.5px; font-weight: 600; color: #333; }
  .rb-time { font-size: 11.5px; color: #999; flex-shrink: 0; }

  .rb-meta {
    display: flex;
    gap: 14px;
    margin-top: 5px;
    font-size: 12.5px;
    color: #666;
  }

  .rb-note { margin: 6px 0 0; font-size: 12.5px; color: #666; line-height: 1.7; }
}
.ai-box {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.07), rgba(103, 194, 58, 0.04));
  border: 1px solid rgba(64, 158, 255, 0.16);

  .ai-head {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 700;
    color: $brand;
    margin-bottom: 6px;
  }

  .ai-text {
    margin: 0;
    font-size: 13px;
    line-height: 1.9;
    color: #555;
    white-space: pre-wrap;
  }
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  padding-top: 11px;
  border-top: 1px dashed rgba(64, 158, 255, 0.14);

  .due-text {
    font-size: 12.5px;
    color: #999;

    &.over { color: $orange; font-weight: 600; }
  }

  .foot-btns { display: flex; gap: 8px; flex-shrink: 0; }
}

.side-card {
  background: #fff;
  border: 1px solid $line;
  border-radius: 16px;
  padding: 16px;

  .side-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 14px;
    font-weight: 700;
    color: #333;
    margin-bottom: 14px;
  }

  .side-count { font-size: 12px; color: #999; font-weight: 400; }
}

.ring-row {
  display: flex;
  align-items: center;
  gap: 14px;

  .ring {
    width: 66px;
    height: 66px;
    flex-shrink: 0;
    border-radius: 50%;
    display: grid;
    place-items: center;
    align-content: center;
    border: 4px solid rgba(64, 158, 255, 0.15);

    &.high { border-color: rgba(103, 194, 58, 0.4); .ring-num { color: $green; } }
    &.mid { border-color: rgba(64, 158, 255, 0.4); .ring-num { color: $brand; } }
    &.low { border-color: rgba(230, 162, 60, 0.45); .ring-num { color: $orange; } }
  }

  .ring-num { font-size: 22px; font-weight: 800; line-height: 1; }
  .ring-unit { font-size: 11px; color: #999; }

  .ring-level { margin: 0 0 4px; font-size: 13px; font-weight: 600; color: #333; }
  .ring-note { margin: 0; font-size: 11.5px; line-height: 1.7; color: #999; }
}

.trend-chart { height: 120px; margin-top: 12px; }
.dir-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 9px;

  &:last-child { margin-bottom: 0; }

  .db-name { width: 52px; flex-shrink: 0; font-size: 12.5px; color: #666; }

  .db-track {
    flex: 1;
    height: 8px;
    border-radius: 4px;
    background: rgba(64, 158, 255, 0.08);
    overflow: hidden;
  }

  .db-fill {
    height: 100%;
    border-radius: 4px;
    transition: width 0.4s cubic-bezier(0.22, 1, 0.36, 1);
    background: $brand;

    &.buy { background: $green; }
    &.sell { background: $red; }
    &.hold { background: $brand; }
  }

  .db-num { width: 22px; flex-shrink: 0; text-align: right; font-size: 12.5px; color: #333; }
}

.pager {
  display: flex;
  justify-content: center;
  padding: 6px 0 4px;

  :deep(.el-pagination.is-background .el-pager li) {
    border-radius: 8px;
    background: #fff;
    border: 1px solid $line;
    color: #666;
  }

  :deep(.el-pagination.is-background .el-pager li.is-active) {
    background: $brand;
    border-color: $brand;
    color: #fff;
  }

  :deep(.el-pagination.is-background .btn-prev),
  :deep(.el-pagination.is-background .btn-next) {
    border-radius: 8px;
    background: #fff;
    border: 1px solid $line;
  }
}
.side-bias {
  padding: 10px 0;
  border-bottom: 1px dashed rgba(64, 158, 255, 0.12);
  cursor: pointer;

  &:first-of-type { padding-top: 0; }
  &:last-of-type { border-bottom: none; }
  &:hover .sb-link { color: $brand; }
  &:hover .sb-name { color: $brand; }

  .sb-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 8px;
  }

  .sb-name { font-size: 13px; font-weight: 600; color: #333; }
  .sb-count { font-size: 12px; color: $orange; }
  .sb-advice { margin: 5px 0 0; font-size: 12px; line-height: 1.75; color: #999; }
  .sb-link {
    display: inline-block;
    margin-top: 6px;
    font-size: 12px;
    color: #a7abb3;
    transition: color 0.2s;

    &::after { content: ' →'; }
  }
}

.empty-state {
  padding: 48px 24px;
  text-align: center;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.05), rgba(103, 194, 58, 0.03));
  border: 1px solid $line;
  border-radius: 16px;

  .empty-icon { font-size: 34px; }
  p { margin: 12px 0 18px; font-size: 13.5px; color: #999; }
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
    padding: 9px 18px;
    font-size: 14px;
    flex-shrink: 0;
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

  &.tiny { padding: 5px 12px; font-size: 12.5px; border-radius: 8px; }
  &.full { width: 100%; margin-top: 12px; }
  &.danger:hover:not(:disabled) { color: $red; border-color: $red; }
}

.form-body {
  .field { margin-bottom: 18px; }

  .field-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  label {
    display: block;
    font-size: 13px;
    font-weight: 600;
    color: #333;
    margin-bottom: 7px;

    .req { margin-left: 6px; font-size: 11.5px; color: $red; font-weight: 400; }
  }

  .field-hint { margin: 6px 0 0; font-size: 11.5px; line-height: 1.7; color: #999; }

  .wide { width: 100%; }

  .mode-row {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;
    flex-wrap: wrap;

    .mode-note { font-size: 12px; color: #999; }
  }
}
.reason-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;

  :deep(.el-checkbox.bad .el-checkbox__label) { color: $red; }
}

.mood-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 6px;

  .mood-chip {
    padding: 8px 4px;
    border-radius: 10px;
    text-align: center;
    font-size: 12px;
    color: #666;
    background: #fafbfc;
    border: 1px solid rgba(64, 158, 255, 0.12);
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);

    &:hover { border-color: $brand; }
    &.on { border-color: $brand; background: rgba(64, 158, 255, 0.08); color: $brand; font-weight: 600; }

    .mood-icon { display: block; font-size: 17px; margin-bottom: 2px; }
  }
}

.mood-score {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 14px;
  padding: 0 6px;

  .ms-label { font-size: 12.5px; color: #999; flex-shrink: 0; }
  :deep(.el-slider) { flex: 1; }
}

.review-form {
  .field { margin-bottom: 18px; }

  label {
    display: block;
    font-size: 13px;
    font-weight: 600;
    color: #333;
    margin-bottom: 7px;
  }

  .field-hint { margin: 7px 0 0; font-size: 11.5px; line-height: 1.7; color: $orange; }
}
.rf-plan {
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(64, 158, 255, 0.05);
  border-left: 3px solid $brand;
  margin-bottom: 20px;

  .rf-label { font-size: 11.5px; color: $brand; font-weight: 600; }
  p { margin: 5px 0; font-size: 13.5px; line-height: 1.75; color: #333; }
  .rf-meta { font-size: 11.5px; color: #999; }
}

@media (max-width: 820px) {
  .page-head .head-inner { flex-wrap: wrap; gap: 12px; padding: 14px 5vw; }
  .content { padding: 18px 5vw 40px; }
  .kpi-row { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
  .kpi-row .kpi-card { padding: 12px 14px; }
  .kpi-row .kpi-num { font-size: 24px; }
  .review-bar { flex-direction: column; align-items: flex-start; }
  .review-bar .review-hint { max-width: 100%; }
  .form-body .field-row { grid-template-columns: 1fr; }
  .mood-grid { grid-template-columns: repeat(3, 1fr); }
}











</style>


