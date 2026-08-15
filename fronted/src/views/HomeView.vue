<template>
  <div class="home">
    <!-- 背景网格 -->
    <div class="bg-grid"></div>

    <!-- 视差光斑层 -->
    <div class="orb-layer" :style="parallax(36)">
      <div class="orb orb-blue"></div>
      <div class="orb orb-green"></div>
    </div>
    <div class="orb-layer" :style="parallax(-22)">
      <div class="orb orb-orange"></div>
    </div>

    <!-- 顶部横向进度条 -->
    <div class="progress" :style="{ width: progressPct + '%', backgroundColor: 'blue' }"></div>

    <!-- 横向滚动视口 -->
    <div
      class="h-viewport"
      ref="viewport"
      @wheel="onWheel"
      @pointerdown="onPointerDown"
      @pointermove="onPointerMove"
      @pointerup="onPointerUp"
      @pointercancel="onPointerUp">
      <div class="h-track" ref="track">
        <!-- ========== 分区一：首屏 ========== -->
        <section class="h-section" :class="{ active: active === 0 }">
          <div class="sec-inner hero-inner">
            <div class="hero-left">
              <div class="badge reveal" style="--d: 0.05s">
                <span class="pulse-dot"></span>
                AI POWERED · 入市教育智慧助手
              </div>

              <h1 class="title">
                <span class="t-row reveal" style="--d: 0.15s">先懂，</span>
                <span class="t-row t-outline reveal" style="--d: 0.28s"
                  >再投。</span
                >
                <span class="t-row reveal" style="--d: 0.41s">
                  <span class="t-accent">AI 陪你走全程</span
                  ><em class="t-dot">.</em>
                </span>
              </h1>

              <p class="desc reveal" style="--d: 0.54s">
                用对话式 AI、结构化知识库与模拟引导，帮助每一位新手投资者<br />
                在真实入市之前，建立理性、系统的投资认知。
              </p>

              <div class="cta reveal" style="--d: 0.66s">
                <el-button
                  size="large"
                  type="primary"
                  @click="handleFirstSectionLogin"
                  >开始对话</el-button
                >
                <el-button
                  size="large"
                  plain
                  @click="handleFirstSectionKnowledge"
                  >探索知识库</el-button
                >
              </div>

              <div class="stats reveal" style="--d: 0.78s">
                <div class="stat">
                  <b>{{ display(5) }}+</b>
                  <span>核心功能模块</span>
                </div>
                <div class="stat">
                  <b>{{ display(1000) }}+</b>
                  <span>金融知识条目</span>
                </div>
                <div class="stat">
                  <b>24/7</b>
                  <span>智能助手在线</span>
                </div>
              </div>
            </div>

            <div class="hero-right reveal" style="--d: 0.5s">
              <div class="chat-card">
                <div class="chat-head">
                  <div class="avatar">AI</div>
                  <div class="chat-meta">
                    <div class="chat-name">入市教育智慧助手</div>
                    <div class="chat-status"><i></i>在线 · 随时答疑</div>
                  </div>
                </div>
                <div class="chat-body">
                  <div class="msg msg-user">新手入市，第一笔钱该怎么规划？</div>
                  <div class="msg msg-ai">
                    先做三件事：① 完成风险测评 ② 只用闲钱投资 ③
                    在知识库理解基金与股票的区别。我们一步步来～
                  </div>
                  <div class="typing">
                    <span></span><span></span><span></span>
                  </div>
                </div>
                <div class="chat-input">
                  <span>把你的问题交给 AI…</span>
                  <el-icon><TopRight /></el-icon>
                </div>
              </div>

              <div class="chip chip-1">📊 风险测评结果 · 稳健型 C3</div>
              <div class="chip chip-2">📚 已收录 1000+ 金融知识</div>
            </div>
          </div>

          <div class="scroll-hint reveal" style="--d: 1.1s">
            滚动或拖动，探索更多
            <span class="hint-arrow">⟶</span>
          </div>
        </section>

        <!-- ========== 分区二：五大核心模块（Bento 风格） ========== -->
        <section class="h-section" :class="{ active: active === 1 }">
          <div class="sec-inner">
            <div class="sec-head reveal">
              <span class="sec-label">CORE MODULES</span>
              <h2 class="sec-title">
                五大核心<span class="sec-accent">功能模块</span>
              </h2>
              <p class="sec-sub">从认知到实践，一站式投资教育平台</p>
            </div>
            <div class="bento-grid">
              <div
                class="bento-card bento-main reveal"
                :style="{ '--d': '0.12s', '--ac': features[0].color }"
                @click="$router.push(features[0].path)">
                <div class="bento-icon">{{ features[0].icon }}</div>
                <h3>{{ features[0].title }}</h3>
                <p>{{ features[0].desc }}</p>
                <div class="b-link">
                  进入模块 <el-icon><ArrowRight /></el-icon>
                </div>
              </div>
              <div
                class="bento-card reveal"
                v-for="(f, i) in features.slice(1)"
                :key="f.title"
                :style="{ '--d': 0.2 + i * 0.08 + 's', '--ac': f.color }"
                @click="$router.push(f.path)">
                <div class="b-icon">{{ f.icon }}</div>
                <h3>{{ f.title }}</h3>
                <p>{{ f.desc }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- ========== 分区三：三步流程 ========== -->
        <section class="h-section" :class="{ active: active === 2 }">
          <div class="sec-inner">
            <div class="sec-head reveal">
              <span class="sec-label">HOW IT WORKS</span>
              <h2 class="sec-title">
                三步开启<span class="sec-accent">投资学习</span>之旅
              </h2>
              <p class="sec-sub">不必急于实盘，按自己的节奏循序渐进</p>
            </div>
            <div class="steps-row">
              <div
                class="step reveal"
                v-for="(s, i) in steps"
                :key="s.num"
                :style="{ '--d': 0.15 + i * 0.13 + 's', '--ac': s.color }">
                <div class="step-num">{{ s.num }}</div>
                <h3>{{ s.title }}</h3>
                <p>{{ s.desc }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- ========== 分区四：开始行动 ========== -->
        <section class="h-section" :class="{ active: active === 3 }">
          <div class="sec-inner cta-inner">
            <div class="badge reveal">
              <span class="pulse-dot"></span>
              READY TO START
            </div>
            <h2 class="cta-title reveal" style="--d: 0.12s">
              让第一笔投资<br />
              <span class="sec-accent">建立在认知之上</span>
            </h2>
            <p class="cta-sub reveal" style="--d: 0.24s">
              不必独自摸索。先学会理解市场、理解自己，AI 会全程陪伴你的每一步。
            </p>
            <div class="cta-buttons reveal" style="--d: 0.36s">
              <el-button
                size="large"
                type="primary"
                @click="handleFourthSectionLogin">
                开始对话
                <el-icon class="btn-arrow"><ArrowRight /></el-icon>
              </el-button>
              <el-button
                size="large"
                @click="handleFourthSectionKnowledge"
                plain>
                先看看知识库
              </el-button>
            </div>
          </div>
        </section>
      </div>

      <!-- 分区指示点 -->
      <div class="dots">
        <span
          v-for="i in 4"
          :key="i"
          :class="{ on: active === i - 1 }"
          @click="goTo(i - 1)"></span>
      </div>
    </div>

    <!-- 底部跑马灯 -->
    <div class="ticker">
      <div class="ticker-track">
        <template v-for="n in 2" :key="n">
          <span class="tk" v-for="t in terms" :key="t + n">
            <em>{{ t }}</em
            ><i>✦</i>
          </span>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, TopRight } from '@element-plus/icons-vue'

const router = useRouter()

// ===== 跳转处理函数 =====
const handleFirstSectionLogin = () => {
  if (localStorage.getItem('token')) {
    router.push('/user/chat')
  } else {
    router.push('/login')
  }
}

const handleFirstSectionKnowledge = () => {
  if (localStorage.getItem('token')) {
    router.push('/user/knowledge')
  } else {
    router.push('/login')
  }
}

const handleFourthSectionLogin = () => {
  handleFirstSectionLogin()
}

const handleFourthSectionKnowledge = () => {
  handleFirstSectionKnowledge()
}

// ===== 数据 =====
const terms = [
  '理性投资',
  '风险管理',
  '资产配置',
  '复利思维',
  '情绪纪律',
  '分散投资',
  '基本面分析',
  '长期主义',
]

const features = [
  {
    icon: '💬',
    title: '智能问答',
    desc: 'AI 对话即时解答投资疑问，24/7 全天候在线陪伴',
    path: '/user/chat',
    color: '#409eff',
  },
  {
    icon: '📚',
    title: '知识库',
    desc: '1000+ 结构化金融知识条目，从入门到进阶体系化学习',
    path: '/user/knowledge',
    color: '#67c23a',
  },
  {
    icon: '📊',
    title: '风险测评',
    desc: '科学问卷定位风险承受等级，认清适合自己的投资方式',
    path: '/user/assessment',
    color: '#e6a23c',
  },
  {
    icon: '🎮',
    title: '模拟引导',
    desc: '在模拟场景中演练决策，零风险建立真实投资认知',
    path: '/user/simulation',
    color: '#409eff',
  },
  {
    icon: '📝',
    title: '投资日记',
    desc: '记录每一次思考与决策，复盘沉淀为长期成长',
    path: '/user/diary',
    color: '#67c23a',
  },
]

const steps = [
  {
    num: '01',
    title: '学 · 懂市场',
    desc: '深入知识库，系统学习股票、基金、债券的核心概念与底层逻辑，搭建完整的金融认知框架。',
    color: '#409eff',
  },
  {
    num: '02',
    title: '测 · 知自己',
    desc: '完成风险测评，明确自己的风险承受能力与投资偏好，找到真正适合自己的投资风格。',
    color: '#e6a23c',
  },
  {
    num: '03',
    title: '练 · 验真知',
    desc: '通过模拟引导与 AI 问答反复演练验证，在安全环境中把知识内化为自己的投资方法论。',
    color: '#67c23a',
  },
]

const SECTION_COUNT = 4

// ===== 鼠标视差 =====
const mx = ref(0)
const my = ref(0)
const onMouseMove = (e) => {
  mx.value = e.clientX / window.innerWidth - 0.5
  my.value = e.clientY / window.innerHeight - 0.5
}
const parallax = (depth) => ({
  transform: `translate3d(${mx.value * depth}px, ${my.value * depth}px, 0)`,
})

// ===== 数字滚动 =====
const progress = ref(0)
const display = (v) => Math.round(v * progress.value)

// ===== 横向滚动 =====
const viewport = ref(null)
const track = ref(null)
const active = ref(0)
const progressPct = ref(0)

let target = 0
let currentX = 0
let maxScroll = 0
let raf = null

const clampTarget = () => {
  if (!track.value || !viewport.value) return
  maxScroll = Math.max(0, track.value.scrollWidth - viewport.value.clientWidth)
  target = Math.max(0, Math.min(target, maxScroll))
}

const loop = () => {
  if (track.value) {
    currentX += (target - currentX) * 0.075
    if (Math.abs(target - currentX) < 0.1) currentX = target
    track.value.style.transform = `translate3d(${-currentX}px, 0, 0)`

    const stepW = SECTION_COUNT > 1 ? maxScroll / (SECTION_COUNT - 1) : 0
    const idx = stepW > 0 ? Math.round(currentX / stepW) : 0
    const clamped = Math.max(0, Math.min(SECTION_COUNT - 1, idx))
    if (clamped !== active.value) active.value = clamped

    progressPct.value = maxScroll > 0 ? (currentX / maxScroll) * 100 : 0
  }
  raf = requestAnimationFrame(loop)
}

const goTo = (i) => {
  clampTarget()
  const stepW = SECTION_COUNT > 1 ? maxScroll / (SECTION_COUNT - 1) : 0
  target = stepW * i
}

// 滚轮：纵向滚动 → 横向位移
const onWheel = (e) => {
  e.preventDefault()
  const delta = Math.abs(e.deltaY) >= Math.abs(e.deltaX) ? e.deltaY : e.deltaX
  target += delta
  clampTarget()
}

// 拖拽
let dragging = false
let dragStartX = 0
let dragStartTarget = 0

const onPointerDown = (e) => {
  dragging = true
  dragStartX = e.clientX
  dragStartTarget = target
  try {
    viewport.value.setPointerCapture(e.pointerId)
  } catch (_) {
    /* ignore */
  }
}
const onPointerMove = (e) => {
  if (!dragging) return
  const dx = e.clientX - dragStartX
  target = dragStartTarget - dx
  clampTarget()
}
const onPointerUp = () => {
  dragging = false
}

onMounted(() => {
  // 数字滚动动画
  const start = performance.now()
  const duration = 1600
  const tick = (now) => {
    const p = Math.min(1, (now - start) / duration)
    progress.value = 1 - Math.pow(1 - p, 3)
    if (p < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)

  // 横向滚动初始化
  clampTarget()
  raf = requestAnimationFrame(loop)
  window.addEventListener('resize', clampTarget)

  // 添加事件监听器
  if (viewport.value) {
    viewport.value.addEventListener('wheel', onWheel, { passive: false })
    viewport.value.addEventListener('pointerdown', onPointerDown)
    viewport.value.addEventListener('pointermove', onPointerMove)
    viewport.value.addEventListener('pointerup', onPointerUp)
    viewport.value.addEventListener('pointercancel', onPointerUp)
  }

  window.addEventListener('mousemove', onMouseMove)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  if (viewport.value) {
    viewport.value.removeEventListener('wheel', onWheel)
    viewport.value.removeEventListener('pointerdown', onPointerDown)
    viewport.value.removeEventListener('pointermove', onPointerMove)
    viewport.value.removeEventListener('pointerup', onPointerUp)
    viewport.value.removeEventListener('pointercancel', onPointerUp)
  }
  window.removeEventListener('resize', clampTarget)
  window.removeEventListener('mousemove', onMouseMove)
})
</script>

<style scoped lang="scss">
.home {
  position: relative;
  width: 100%;
  height: calc(100vh - 60px);
  background: #ffffff;
  color: #333;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  /* ===== 背景网格 ===== */
  .bg-grid {
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(rgba(64, 158, 255, 0.06) 1px, transparent 1px),
      linear-gradient(90deg, rgba(64, 158, 255, 0.06) 1px, transparent 1px);
    background-size: 72px 72px;
    mask-image: radial-gradient(
      ellipse 90% 80% at 50% 40%,
      #000 30%,
      transparent 78%
    );
    z-index: 0;
    pointer-events: none;
  }

  /* ===== 光斑 ===== */
  .orb-layer {
    position: absolute;
    inset: 0;
    z-index: 0;
    pointer-events: none;
    transition: transform 0.6s cubic-bezier(0.22, 1, 0.36, 1);
    will-change: transform;
  }

  .orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(110px);
  }

  .orb-blue {
    width: 560px;
    height: 560px;
    top: -180px;
    right: 4%;
    background: radial-gradient(
      circle,
      rgba(64, 158, 255, 0.22),
      transparent 65%
    );
    animation: drift 22s ease-in-out infinite alternate;
  }

  .orb-green {
    width: 640px;
    height: 640px;
    bottom: -280px;
    left: -160px;
    background: radial-gradient(
      circle,
      rgba(103, 194, 58, 0.2),
      transparent 65%
    );
    animation: drift 26s ease-in-out infinite alternate-reverse;
  }

  .orb-orange {
    width: 320px;
    height: 320px;
    top: 42%;
    left: 44%;
    background: radial-gradient(
      circle,
      rgba(230, 162, 60, 0.14),
      transparent 65%
    );
    animation: drift 18s ease-in-out infinite alternate;
  }

  /* ===== 顶部进度条 ===== */
  .progress {
    position: absolute;
    top: 0;
    left: 0;
    height: 5px;
    z-index: 100;
    background: linear-gradient(90deg, #409eff, #67c23a, #e6a23c);
    border-radius: 0 3px 3px 0;
    width: 0%; /* 初始宽度为0 */
  }

  /* ===== 横向滚动视口 ===== */
  .h-viewport {
    position: relative;
    z-index: 2;
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  .h-track {
    display: flex;
    height: 100%;
    will-change: transform;
  }

  .h-section {
    flex: 0 0 100%;
    height: 100%;
    position: relative;
    z-index: 1;
  }

  .sec-inner {
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding: 24px 7vw 64px;
    position: relative;
    z-index: 2;
  }

  /* 分区入场动画 */
  .reveal {
    opacity: 0;
    transform: translateY(36px);
    transition:
      opacity 0.9s cubic-bezier(0.22, 1, 0.36, 1) var(--d, 0s),
      transform 0.9s cubic-bezier(0.22, 1, 0.36, 1) var(--d, 0s);
  }

  .h-section.active .reveal {
    opacity: 1;
    transform: translateY(0);
  }

  /* ===== 通用元素 ===== */
  .badge {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    padding: 9px 20px;
    border: 1px solid rgba(64, 158, 255, 0.2);
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(12px);
    font-size: 12px;
    letter-spacing: 3px;
    color: rgba(64, 158, 255, 0.8);
    width: fit-content;

    .pulse-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #409eff;
      position: relative;

      &::after {
        content: '';
        position: absolute;
        inset: -4px;
        border-radius: 50%;
        border: 1px solid rgba(64, 158, 255, 0.6);
        animation: pulse-ring 2s ease-out infinite;
      }
    }
  }

  .sec-head {
    margin-bottom: 36px;

    .sec-label {
      display: block;
      font-size: 12px;
      letter-spacing: 4px;
      color: rgba(64, 158, 255, 0.7);
      margin-bottom: 14px;
    }

    .sec-title {
      font-size: clamp(30px, 3.2vw, 48px);
      font-weight: 800;
      line-height: 1.2;
      margin-bottom: 10px;
      color: #333;
    }

    .sec-sub {
      font-size: 14px;
      color: #666;
    }
  }

  .sec-accent {
    background: linear-gradient(94deg, #409eff 5%, #67c23a 60%, #e6a23c 105%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  /* ===== 分区一：首屏 ===== */
  .hero-inner {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 48px;
  }

  .hero-left {
    max-width: 560px;

    .title {
      font-size: clamp(46px, 5.2vw, 80px);
      font-weight: 800;
      line-height: 1.12;
      letter-spacing: 0.01em;
      margin: 24px 0 20px;
      color: #333;

      .t-row {
        display: block;
      }

      .t-outline {
        color: transparent;
        -webkit-text-stroke: 1.5px rgba(64, 158, 255, 0.85);
      }

      .t-dot {
        font-style: normal;
        color: #409eff;
      }
    }

    .desc {
      font-size: 15px;
      line-height: 1.8;
      color: #666;
      margin-bottom: 28px;
    }

    .cta {
      display: flex;
      gap: 18px;

      :deep(.el-button) {
        cursor: pointer; /* 修复鼠标手型 */
      }
    }

    .stats {
      display: flex;
      gap: 44px;
      margin-top: 32px;
      padding-top: 22px;
      border-top: 1px solid rgba(64, 158, 255, 0.15);

      .stat {
        b {
          display: block;
          font-size: 28px;
          font-weight: 800;
          background: linear-gradient(94deg, #409eff, #67c23a);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
          margin-bottom: 4px;
        }

        span {
          font-size: 12px;
          letter-spacing: 1px;
          color: #999;
        }
      }
    }
  }

  .hero-right {
    position: relative;
    flex-shrink: 0;
    width: 400px;
    display: flex;
    justify-content: center;

    .chat-card {
      width: 340px;
      background: linear-gradient(
        160deg,
        rgba(255, 255, 255, 0.95),
        rgba(255, 255, 255, 0.85)
      );
      border: 1px solid rgba(64, 158, 255, 0.15);
      border-radius: 22px;
      box-shadow: 0 30px 80px rgba(64, 158, 255, 0.15);
      transform: rotate(2deg);
      transition: transform 0.5s cubic-bezier(0.22, 1, 0.36, 1);

      &:hover {
        transform: rotate(0deg) scale(1.02);
      }

      .chat-head {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 16px 18px;
        border-bottom: 1px solid rgba(64, 158, 255, 0.1);

        .avatar {
          width: 36px;
          height: 36px;
          border-radius: 50%;
          background: linear-gradient(135deg, #409eff, #67c23a);
          color: #fff;
          font-weight: 800;
          font-size: 13px;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }

        .chat-meta {
          .chat-name {
            font-size: 13px;
            font-weight: 600;
            color: #333;
          }

          .chat-status {
            font-size: 11px;
            color: #999;
            display: flex;
            align-items: center;
            gap: 6px;
            margin-top: 3px;

            i {
              width: 5px;
              height: 5px;
              border-radius: 50%;
              background: #67c23a;
            }
          }
        }
      }

      .chat-body {
        padding: 16px;
        display: flex;
        flex-direction: column;
        gap: 10px;

        .msg {
          max-width: 86%;
          padding: 10px 14px;
          border-radius: 14px;
          font-size: 12px;
          line-height: 1.7;
        }

        .msg-user {
          align-self: flex-end;
          background: rgba(64, 158, 255, 0.12);
          border: 1px solid rgba(64, 158, 255, 0.2);
          border-bottom-right-radius: 4px;
          color: #333;
        }

        .msg-ai {
          align-self: flex-start;
          background: rgba(103, 194, 58, 0.08);
          border: 1px solid rgba(103, 194, 58, 0.15);
          border-bottom-left-radius: 4px;
          color: #333;
        }

        .typing {
          align-self: flex-start;
          display: flex;
          gap: 5px;
          padding: 9px 13px;
          background: rgba(64, 158, 255, 0.08);
          border-radius: 14px;
          border-bottom-left-radius: 4px;
          width: fit-content;

          span {
            width: 5px;
            height: 5px;
            border-radius: 50%;
            background: rgba(64, 158, 255, 0.6);
            animation: blink 1.2s ease-in-out infinite;

            &:nth-child(2) {
              animation-delay: 0.2s;
            }

            &:nth-child(3) {
              animation-delay: 0.4s;
            }
          }
        }
      }

      .chat-input {
        margin: 0 18px 18px;
        padding: 10px 14px;
        border-radius: 12px;
        border: 1px dashed rgba(64, 158, 255, 0.3);
        color: #999;
        font-size: 12px;
        display: flex;
        justify-content: space-between;
        align-items: center;

        :deep(.el-icon) {
          width: 24px;
          height: 24px;
          border-radius: 50%;
          background: #409eff;
          color: #fff;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 12px;
        }
      }
    }

    .chip {
      position: absolute;
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 14px;
      background: rgba(255, 255, 255, 0.85);
      border: 1px solid rgba(64, 158, 255, 0.2);
      border-radius: 12px;
      box-shadow: 0 16px 40px rgba(64, 158, 255, 0.15);
      font-size: 12px;
      color: #333;
      white-space: nowrap;
      animation: bob 6s ease-in-out infinite;
    }

    .chip-1 {
      top: 2%;
      right: -10px;
      animation-delay: -2s;
    }

    .chip-2 {
      bottom: 4%;
      left: -14px;
    }
  }

  .scroll-hint {
    position: absolute;
    right: 7vw;
    bottom: 22px;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    letter-spacing: 2px;
    color: #999;

    .hint-arrow {
      animation: hint-slide 1.8s ease-in-out infinite;
    }
  }

  /* ===== 分区二：Bento 功能模块 ===== */
  .bento-grid {
    display: grid;
    grid-template-columns: 2fr 1fr 1fr;
    grid-template-rows: 1fr 1fr;
    gap: 18px;
    max-width: 1000px;
    margin: 0 auto;
    height: 420px;

    .bento-card {
      padding: 26px;
      border-radius: 20px;
      background: rgba(255, 255, 255, 0.9);
      border: 1px solid rgba(64, 158, 255, 0.12);
      box-shadow: 0 10px 30px rgba(64, 158, 255, 0.08);
      transition: all 0.4s cubic-bezier(0.22, 1, 0.36, 1);
      display: flex;
      flex-direction: column;
      justify-content: space-between;

      &:hover {
        transform: translateY(-8px);
        border-color: color-mix(in srgb, var(--ac) 50%, transparent);
        box-shadow: 0 22px 48px color-mix(in srgb, var(--ac) 20%, transparent);
      }

      .b-icon {
        width: 44px;
        height: 44px;
        border-radius: 14px;
        background: color-mix(in srgb, var(--ac) 16%, transparent);
        border: 1px solid color-mix(in srgb, var(--ac) 30%, transparent);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22px;
        color: var(--ac);
        margin-bottom: 14px;
      }

      h3 {
        font-size: 17px;
        font-weight: 700;
        color: #333;
        margin-bottom: 8px;
      }

      p {
        font-size: 13px;
        line-height: 1.7;
        color: #666;
        flex: 1;
      }
    }

    .bento-main {
      grid-row: span 2;
      padding: 34px;
      background: linear-gradient(
        160deg,
        rgba(255, 255, 255, 0.95),
        color-mix(in srgb, var(--ac) 5%, white)
      );
      border: 1px solid
        color-mix(in srgb, var(--ac) 30%, rgba(64, 158, 255, 0.12));

      .bento-icon {
        width: 64px;
        height: 64px;
        border-radius: 20px;
        background: color-mix(in srgb, var(--ac) 20%, transparent);
        border: 1px solid color-mix(in srgb, var(--ac) 35%, transparent);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 32px;
        color: var(--ac);
        margin-bottom: 20px;
      }

      h3 {
        font-size: 24px;
        font-weight: 800;
        color: #333;
        margin-bottom: 12px;
      }

      p {
        font-size: 14px;
        line-height: 1.8;
        color: #555;
        margin-bottom: 18px;
      }

      .b-link {
        font-size: 14px;
        font-weight: 600;
        color: var(--ac);
        display: flex;
        align-items: center;
        gap: 6px;
        width: fit-content;
        transition: gap 0.3s ease;

        &:hover {
          gap: 12px;
        }
      }
    }
  }

  /* ===== 分区三：三步流程 ===== */
  .steps-row {
    display: flex;
    justify-content: center;
    gap: 36px;
    flex-wrap: wrap;
    max-width: 1100px;
    margin: 0 auto;

    .step {
      flex: 0 1 320px;
      border-top: 1px solid rgba(64, 158, 255, 0.2);
      padding-top: 24px;
      transition: border-color 0.4s ease;

      &:hover {
        border-top-color: color-mix(in srgb, var(--ac) 70%, transparent);
      }

      .step-num {
        font-size: 72px;
        font-weight: 800;
        line-height: 1;
        color: transparent;
        -webkit-text-stroke: 1.5px color-mix(in srgb, var(--ac) 75%, #333);
        margin-bottom: 18px;
      }

      h3 {
        font-size: 20px;
        font-weight: 700;
        margin-bottom: 10px;
        color: #333;
      }

      p {
        font-size: 13px;
        line-height: 1.8;
        color: #666;
      }
    }
  }

  /* ===== 分区四：CTA ===== */
  .cta-inner {
    align-items: center;
    text-align: center;

    .cta-title {
      font-size: clamp(40px, 4.2vw, 72px);
      font-weight: 800;
      line-height: 1.18;
      margin: 24px 0 20px;
      color: #333;
    }

    .cta-sub {
      font-size: 15px;
      line-height: 1.9;
      color: #666;
      margin-bottom: 36px;
    }

    .cta-buttons {
      display: flex;
      gap: 18px;
      justify-content: center;

      :deep(.el-button) {
        cursor: pointer; /* 修复鼠标手型 */
      }
    }
  }

  /* ===== 分区指示点 ===== */
  .dots {
    position: absolute;
    bottom: 18px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    gap: 10px;
    z-index: 6;

    span {
      width: 8px;
      height: 8px;
      border-radius: 999px;
      background: rgba(64, 158, 255, 0.3);
      transition: all 0.4s cubic-bezier(0.22, 1, 0.36, 1);

      &:hover {
        background: rgba(64, 158, 255, 0.6);
      }

      &.on {
        width: 28px;
        background: #409eff;

        &:hover {
          background: #409eff;
        }
      }
    }
  }

  /* ===== 底部跑马灯 ===== */
  .ticker {
    position: relative;
    z-index: 2;
    border-top: 1px solid rgba(64, 158, 255, 0.15);
    background: rgba(255, 255, 255, 0.8);
    padding: 12px 0;
    overflow: hidden;
    flex-shrink: 0;

    .ticker-track {
      display: flex;
      width: max-content;
      animation: scroll-x 32s linear infinite;

      .tk {
        display: inline-flex;
        align-items: center;
        gap: 30px;
        padding-right: 30px;

        em {
          font-style: normal;
          font-size: 13px;
          letter-spacing: 5px;
          color: #666;
        }

        i {
          font-style: normal;
          font-size: 9px;
          color: #409eff;
        }

        &:nth-child(3n + 2) i {
          color: #67c23a;
        }

        &:nth-child(3n) i {
          color: #e6a23c;
        }
      }
    }
  }

  /* ===== 动画定义 ===== */
  @keyframes bob {
    0%,
    100% {
      transform: translateY(0);
    }
    50% {
      transform: translateY(-12px);
    }
  }

  @keyframes drift {
    0%,
    100% {
      transform: translate(0, 0) scale(1);
    }
    50% {
      transform: translate(50px, -36px) scale(1.1);
    }
  }

  @keyframes pulse-ring {
    0% {
      transform: scale(0.6);
      opacity: 1;
    }
    100% {
      transform: scale(1.8);
      opacity: 0;
    }
  }

  @keyframes blink {
    0%,
    100% {
      opacity: 0.25;
      transform: translateY(0);
    }
    50% {
      opacity: 1;
      transform: translateY(-2px);
    }
  }

  @keyframes scroll-x {
    to {
      transform: translateX(-50%);
    }
  }

  @keyframes hint-slide {
    0%,
    100% {
      transform: translateX(0);
      opacity: 0.4;
    }
    50% {
      transform: translateX(8px);
      opacity: 1;
    }
  }

  /* ===== 响应式 ===== */
  @media (max-width: 1180px) {
    .hero-right {
      display: none;
    }
  }

  @media (max-width: 900px) {
    .bento-grid {
      grid-template-columns: 1fr 1fr;
      grid-template-rows: auto;
      height: auto;

      .bento-main {
        grid-column: span 2;
        grid-row: span 1;
      }
    }

    .steps-row {
      gap: 28px;

      .step {
        flex: 1 1 100%;
      }
    }
  }

  @media (max-width: 768px) {
    .sec-inner {
      padding: 20px 28px 60px;
    }

    .hero-left {
      .title {
        font-size: 40px;
      }

      .desc {
        font-size: 14px;

        br {
          display: none;
        }
      }

      .cta {
        flex-direction: column;

        .el-button {
          justify-content: center;
        }
      }

      .stats {
        gap: 28px;
        flex-wrap: wrap;
      }
    }

    .scroll-hint {
      display: none;
    }

    .bento-grid {
      grid-template-columns: 1fr;
      grid-template-rows: auto;
      height: auto;

      .bento-main {
        grid-column: span 1;
      }
    }

    .cta-inner .cta-buttons {
      flex-direction: column;
      width: 100%;

      .el-button {
        justify-content: center;
      }
    }
  }
}
</style>
