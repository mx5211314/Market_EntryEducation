<template>
    <div class="consultation-container">
        <div class="sidebar">
            <!-- AI 助手信息 -->
            <div class="ai-assistant-info">
                <div class="breathing-circle">
                    <el-icon size="20"><Connection /></el-icon>
                </div>
                <h3 class="assistant-name">入市教育 AI 助手</h3>
                <div class="online-status">
                    <div class="status-dot"></div>
                    在线服务中
                </div>
            </div>

            <!-- 会话列表：单行标题，时间信息交给分组标题承担 -->
            <div class="session-panel">
                <button class="new-chat" @click="createNewSession">
                    <el-icon><Plus /></el-icon>
                    <span>新对话</span>
                </button>

                <el-input
                    v-model="sessionKeyword"
                    class="session-search"
                    placeholder="搜索会话"
                    clearable
                    :prefix-icon="Search" />

                <div class="session-list">
                    <template v-for="group in groupedSessions" :key="group.label">
                        <div class="group-label">{{ group.label }}</div>
                        <div
                            v-for="session in group.items"
                            :key="session.sessionId || session.id"
                            class="session-item"
                            :class="{ active: currentSession?.sessionId === session.sessionId }"
                            :title="session.title || '新对话'"
                            @click="switchSession(session)">
                            <span class="session-title">{{ session.title || '新对话' }}</span>
                            <el-dropdown
                                trigger="click"
                                placement="bottom-end"
                                @command="(cmd) => handleSessionCommand(cmd, session)">
                                <span class="ops-btn" @click.stop>
                                    <el-icon><MoreFilled /></el-icon>
                                </span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item command="rename" :icon="EditPen">重命名</el-dropdown-item>
                                        <el-dropdown-item command="delete" :icon="Delete" divided>删除</el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </div>
                    </template>

                    <div v-if="!groupedSessions.length" class="empty">
                        {{ sessionKeyword ? '没有匹配的会话' : '暂无会话' }}
                    </div>
                </div>
            </div>
        </div>

        <!-- 主聊天区域 -->
        <div class="chat-main">
            <div class="chat-header">
                <div class="header-left">
                    <div class="chat-avatar">
                        <el-icon size="18"><Connection /></el-icon>
                    </div>
                    <div class="chat-info">
                        <h2>入市教育 AI 助手</h2>
                        <p>您的金融法规智能助手</p>
                    </div>
                </div>
                <!-- 情绪分析收成一枚 chip：不占消息区高度，点开才看细节 -->
                <div
                    v-if="currentEmotion.primaryEmotion"
                    class="emotion-chip"
                    :class="'risk-' + currentEmotion.riskLevel"
                    @click="emotionDrawer = true">
                    <span class="chip-emoji">{{ emotionIcon }}</span>
                    <span class="chip-text">{{ currentEmotion.primaryEmotion }} · {{ getRiskLevelText(currentEmotion.riskLevel) }}</span>
                    <el-icon class="chip-arrow"><ArrowRight /></el-icon>
                </div>
            </div>

            <div class="chat-messages" ref="messagesContainer">
                <div class="messages-inner">
                    <div v-for="msg in messages" :key="msg.id" class="message-row" :class="msg.role">
                        <div v-if="msg.role === 'assistant'" class="msg-avatar">
                            <el-icon size="16"><Connection /></el-icon>
                        </div>

                        <div class="msg-body">
                            <!-- 只在还没有任何内容时显示等待点，有内容就实时渲染，这样流式输出看得见 -->
                            <div v-if="msg.role === 'assistant' && !msg.content" class="typing-indicator">
                                <span></span><span></span><span></span>
                            </div>
                            <div v-else class="msg-content">
                                <MarkdownRenderer :content="msg.content" :on-color-bg="msg.role === 'user'" />
                            </div>

                            <div class="msg-actions" v-if="msg.content">
                                <span class="act-btn" @click="copyMessage(msg.content)">
                                    <el-icon><DocumentCopy /></el-icon>
                                    复制
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 输入区：大圆角容器 + 底部操作行，提示语放在框下方 -->
            <div class="chat-input-wrapper">
                <div class="input-box">
                    <textarea
                        v-model="inputText"
                        :style="{ height: inputHeight + 'px' }"
                        placeholder="给 AI 发送消息，Enter 发送，Shift + Enter 换行"
                        :disabled="isAiTyping"
                        @keydown="handleKeyDown"
                        @input="handleInput"
                        rows="1"
                        maxlength="5000" />
                    <div class="input-actions">
                        <span class="input-count" v-if="inputText.length">{{ inputText.length }} / 5000</span>
                        <button class="send-btn" :disabled="!inputText.trim() || isAiTyping" @click="sendMessage">
                            <el-icon size="16"><Promotion /></el-icon>
                        </button>
                    </div>
                </div>
                <div class="input-hint">AI 可能会犯错，请核实重要信息</div>
            </div>
        </div>

        <!-- 分析详情抽屉：从右侧滑出，不挤压消息流 -->
        <el-drawer v-model="emotionDrawer" title="本轮对话分析" size="340px" append-to-body>
            <div class="emotion-detail">
                <div class="detail-head">
                    <span class="head-emoji">{{ emotionIcon }}</span>
                    <div class="head-text">
                        <div class="head-name">{{ currentEmotion.primaryEmotion || '等待分析' }}</div>
                        <div class="head-risk" :class="'risk-' + currentEmotion.riskLevel">
                            风险等级 · {{ getRiskLevelText(currentEmotion.riskLevel) }}
                        </div>
                    </div>
                </div>

                <div class="detail-block">
                    <div class="block-label">情绪评分</div>
                    <el-progress
                        :percentage="currentEmotion.emotionScore"
                        :stroke-width="10"
                        :color="emotionProgressColor"
                        striped />
                </div>

                <div class="detail-block" v-if="currentEmotion.riskDescription">
                    <div class="block-label">评估说明</div>
                    <p class="block-text">{{ currentEmotion.riskDescription }}</p>
                </div>

                <div class="detail-block suggestion" v-if="currentEmotion.suggestion">
                    <div class="block-label">AI 建议</div>
                    <p class="block-text">{{ currentEmotion.suggestion }}</p>
                </div>

                <div class="detail-block" v-if="currentEmotion.improvementSuggestions?.length">
                    <div class="block-label">建议行动</div>
                    <div v-for="(action, i) in currentEmotion.improvementSuggestions" :key="i" class="action-item">
                        <el-icon class="check-icon"><Checked /></el-icon>
                        <span>{{ action }}</span>
                    </div>
                </div>
            </div>
        </el-drawer>
    </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { ArrowRight, Connection, Plus, Promotion, Checked, Search, MoreFilled, EditPen, Delete, DocumentCopy } from '@element-plus/icons-vue'
import { startSession, getSessionList, deleteSession, renameSession, getSessionDetail, sendMessageStream, getSessionEmotion } from '@/api/frontend'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const route = useRoute()

const sessions = ref([])
const currentSession = ref(null)
const messages = ref([])
const inputText = ref('')
const isAiTyping = ref(false)
const messagesContainer = ref(null)
const emotionDrawer = ref(false)
const sessionKeyword = ref('')
const inputHeight = ref(24) // textarea 单行高度，随输入增长

const WELCOME_MSG = '您好！我是您的入市教育智慧助手，很高兴为您提供金融法规相关的服务。请告诉我，您想了解什么内容？'

const currentEmotion = ref({
  primaryEmotion: '',
  emotionScore: 50,
  isNegative: false,
  riskLevel: 0,
  riskDescription: '',
  suggestion: '',
  improvementSuggestions: []
})

const emotionIcon = computed(() => {
  const score = currentEmotion.value.emotionScore
  if (score >= 80) return '🤩'
  if (score >= 60) return '😊'
  if (score >= 40) return '😐'
  if (score >= 20) return '😟'
  return '😨'
})

const emotionProgressColor = computed(() => {
  const score = currentEmotion.value.emotionScore
  if (score >= 60) return '#67c23a'
  if (score >= 40) return '#e6a23c'
  return '#f56c6c'
})

const getRiskLevelText = (level) => {
  const texts = ['正常', '关注', '预警', '危机']
  return texts[level] || '正常'
}

const loadSessionEmotion = async (sessionId) => {
  try {
    const res = await getSessionEmotion(sessionId)
    currentEmotion.value = res || {}
  } catch (error) {
    console.error('加载情绪分析失败:', error)
  }
}

// 时间信息由分组标题承担，所以会话项本身不再显示时间戳
const startOfDay = (value) => {
  const d = new Date(value)
  d.setHours(0, 0, 0, 0)
  return d
}

const groupedSessions = computed(() => {
  const kw = sessionKeyword.value.trim().toLowerCase()
  const buckets = [
    { label: '今天', items: [] },
    { label: '昨天', items: [] },
    { label: '7 天内', items: [] },
    { label: '30 天内', items: [] },
    { label: '更早', items: [] }
  ]
  const today = startOfDay(new Date())
  for (const s of sessions.value) {
    const title = s.title || '新对话'
    if (kw && !title.toLowerCase().includes(kw)) continue
    const stamp = s.updatedAt || s.createdAt
    const days = stamp ? Math.round((today - startOfDay(stamp)) / 86400000) : 9999
    const idx = days <= 0 ? 0 : days === 1 ? 1 : days <= 7 ? 2 : days <= 30 ? 3 : 4
    buckets[idx].items.push(s)
  }
  return buckets.filter(b => b.items.length)
})

const loadSessions = async () => {
    try {
        const res = await getSessionList()
        sessions.value = res || []
        if (sessions.value.length > 0 && !currentSession.value) {
            switchSession(sessions.value[0])
        } else if (sessions.value.length === 0) {
            createNewSession()
        }
    } catch (error) {
        console.error('加载会话失败:', error)
        createNewSession()
    }
}

const createNewSession = () => {
    currentSession.value = {
        sessionId: `temp_${Date.now()}`,
        status: 'TEMP',
        title: '新对话'
    }
    messages.value = [{
        id: Date.now(),
        role: 'assistant',
        content: WELCOME_MSG,
        createdAt: new Date()
    }]
    isAiTyping.value = false
}

const switchSession = async (session) => {
  if (!session || !session.sessionId) return
  if (currentSession.value?.sessionId === session.sessionId) return

  isAiTyping.value = false
  currentSession.value = session

  try {
    const res = await getSessionDetail(session.sessionId)
    messages.value = (res || []).map(msg => ({
      id: msg.id || Math.random(),
      role: msg.role || (msg.senderType === 1 ? 'user' : 'assistant'),
      content: msg.content,
      createdAt: msg.createdAt || msg.createAt
    }))
    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('加载消息失败:', error)
    messages.value = [{
      id: Date.now(),
      role: 'assistant',
      content: WELCOME_MSG,
      createdAt: new Date()
    }]
  }
  if (session.sessionId && !session.sessionId.startsWith('temp_')) {
    loadSessionEmotion(session.sessionId).catch(e => console.error('情绪加载失败:', e))
  }
}

const handleSessionCommand = async (command, session) => {
  if (command === 'rename') {
    try {
      const { value } = await ElMessageBox.prompt('输入新的会话名称', '重命名会话', {
        inputValue: session.title || '新对话',
        inputValidator: (v) => (v && v.trim() ? true : '名称不能为空'),
        confirmButtonText: '保存',
        cancelButtonText: '取消'
      })
      await renameSession(session.sessionId, value.trim())
      session.title = value.trim()
      if (currentSession.value?.sessionId === session.sessionId) {
        currentSession.value.title = value.trim()
      }
      ElMessage.success('已重命名')
    } catch (e) {
      if (e !== 'cancel' && e !== 'close') ElMessage.error('重命名失败')
    }
    return
  }

  if (command === 'delete') {
    try {
      await ElMessageBox.confirm(`确定删除「${session.title || '新对话'}」吗？消息会一并删除。`, '删除会话', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
    try {
      await deleteSession(session.sessionId)
      sessions.value = sessions.value.filter(s => s.sessionId !== session.sessionId)
      ElMessage.success('已删除')
      // 删掉的正是当前会话时要另起一个，否则消息区还留着已删会话的内容
      if (currentSession.value?.sessionId === session.sessionId) {
        currentSession.value = null
        if (sessions.value.length) {
          switchSession(sessions.value[0])
        } else {
          createNewSession()
        }
      }
    } catch (e) {
      console.error('删除会话失败:', e)
      ElMessage.error('删除失败')
    }
  }
}

// 处理输入框自动高度
const handleInput = (e) => {
    const textarea = e.target
    textarea.style.height = 'auto'
    inputHeight.value = Math.max(24, Math.min(textarea.scrollHeight, 180))
}

const copyMessage = async (text) => {
    try {
        await navigator.clipboard.writeText(text)
        ElMessage.success('已复制')
    } catch {
        ElMessage.error('复制失败，请手动选择文本')
    }
}

// 列表刷新后要把 currentSession 换成列表里的同一个对象，否则重命名等操作改不到同一份数据
const refreshSessions = async () => {
    try {
        const res = await getSessionList()
        sessions.value = res || []
        const currentId = currentSession.value?.sessionId
        const hit = sessions.value.find(s => s.sessionId === currentId)
        if (hit) {
            currentSession.value = hit
        } else if (sessions.value.length && currentId && !currentId.startsWith('temp_')) {
            currentSession.value = sessions.value[0]
        }
    } catch (e) {
        console.error('加载会话列表失败:', e)
    }
}

const sendMessage = async () => {
    if (!inputText.value.trim() || isAiTyping.value) return

    const userMessage = inputText.value.trim()
    inputText.value = ''
    inputHeight.value = 24 // 重置高度

    messages.value.push({
        id: Date.now(),
        role: 'user',
        content: userMessage,
        createdAt: new Date()
    })
    await nextTick()
    scrollToBottom()

    if (currentSession.value?.status === 'TEMP') {
        try {
            const sessionParams = {
                title: userMessage.length > 20 ? userMessage.substring(0, 20) + '...' : userMessage
            }
            const res = await startSession(sessionParams)
            const created = {
                sessionId: res.sessionId,
                status: 'ACTIVE',
                title: res.title || sessionParams.title,
                createdAt: res.createdAt || new Date(),
                updatedAt: res.updatedAt || new Date()
            }
            currentSession.value = created
            // 立刻插到列表最前面：等流式回答结束再刷新的话，回答失败就永远看不到这个会话
            sessions.value = [created, ...sessions.value.filter(s => s.sessionId !== created.sessionId)]
        } catch (error) {
            console.error('创建会话失败:', error)
            ElMessage.error('创建会话失败')
            return
        }
    }

    isAiTyping.value = true

    const aiMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: '',
        createdAt: new Date()
    }
    messages.value.push(aiMessage)
    await nextTick()
    scrollToBottom()

    try {
        const response = await sendMessageStream({
            sessionId: currentSession.value?.sessionId || '',
            message: userMessage
        })
        if (!response.ok) throw new Error('HTTP ' + response.status)

        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')
        // 中文是多字节，一个汉字可能被拆到两个 chunk 里；必须 stream:true 解码并缓存未完成的行
        let buffer = ''

        const consume = async (chunk) => {
            buffer += chunk
            const lines = buffer.split('\n')
            buffer = lines.pop() ?? ''
            for (const line of lines) {
                if (!line.startsWith('data:')) continue
                const data = line.slice(5).replace(/^ /, '')
                if (!data) continue
                let piece = data
                try {
                    const parsed = JSON.parse(data)
                    piece = parsed.content ?? ''
                } catch {
                    // 后端直接推纯文本，解析失败就按原文追加
                }
                if (piece) {
                    aiMessage.content += piece
                    await nextTick()
                    scrollToBottom()
                }
            }
        }

        while (true) {
            const { done, value } = await reader.read()
            if (done) break
            await consume(decoder.decode(value, { stream: true }))
        }
        await consume(decoder.decode() + '\n')

        isAiTyping.value = false
        if (!aiMessage.content) aiMessage.content = '抱歉，本次没有收到回复，请重试。'
    } catch (error) {
        console.error('发送消息失败:', error)
        aiMessage.content = aiMessage.content || '抱歉，回复失败，请重试。'
        ElMessage.error('发送消息失败')
    } finally {
        isAiTyping.value = false
        // 无论回答成功与否都刷新列表，让侧栏标题和排序跟上后端
        await refreshSessions()

        const sid = currentSession.value?.sessionId
        if (sid && !sid.startsWith('temp_')) {
            try {
                await axios.post('/api/chat/analyze-session', { sessionId: sid })
            } catch (e) {
                console.error('情绪分析失败:', e)
            }
            loadSessionEmotion(sid)
        }
    }
}

const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        sendMessage()
    }
}

const scrollToBottom = () => {
    if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
}

onMounted(() => {
    loadSessions()
    // 风险测评结果页的「问 AI 助手」带 q 跳过来，预填第一句让用户确认后再发
    const preset = route.query.q
    if (preset) {
        inputText.value = String(preset)
        nextTick(() => { inputHeight.value = 72 })
    }
})
</script>

<style scoped lang="scss">
.consultation-container {
    height: calc(100vh - 60px);
    display: flex;
    overflow: hidden;

    /* ===== 左侧边栏 ===== */
    .sidebar {
        width: 280px;
        background: #f7f8fa;
        border-right: 1px solid rgba(64, 158, 255, 0.12);
        display: flex;
        flex-direction: column;
        flex-shrink: 0;
        overflow: hidden;
        position: relative;
        z-index: 10;

        .ai-assistant-info {
            padding: 14px 16px;
            border-bottom: 1px solid rgba(64, 158, 255, 0.12);
            text-align: center;
            flex-shrink: 0;

            .breathing-circle {
                width: 44px;
                height: 44px;
                background: linear-gradient(135deg, #409eff, #66b1ff);
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                margin: 0 auto 10px;
                animation: breathing 4s ease-in-out infinite;
            }

            .assistant-name {
                font-size: 14px;
                font-weight: 600;
                color: #333;
                margin: 0 0 8px;
            }

            .online-status {
                font-size: 11px;
                color: #67c23a;
                display: inline-flex;
                align-items: center;
                gap: 4px;

                .status-dot {
                    width: 6px;
                    height: 6px;
                    background: #67c23a;
                    border-radius: 50%;
                    animation: pulse 2s infinite;
                }
            }
        }

        /* 会话面板：新建按钮和搜索固定在顶部，只有列表滚动 */
        .session-panel {
            flex: 1;
            min-height: 0;
            display: flex;
            flex-direction: column;
            padding: 12px 10px 8px;

            .new-chat {
                display: flex;
                align-items: center;
                justify-content: center;
                gap: 6px;
                width: 100%;
                height: 38px;
                flex-shrink: 0;
                border: 1px solid rgba(64, 158, 255, 0.25);
                border-radius: 10px;
                background: #fff;
                color: #409eff;
                font-size: 14px;
                font-weight: 600;
                cursor: pointer;
                transition: background 0.25s cubic-bezier(0.22, 1, 0.36, 1), border-color 0.25s ease;

                &:hover {
                    background: rgba(64, 158, 255, 0.08);
                    border-color: rgba(64, 158, 255, 0.45);
                }
            }

            .session-search {
                margin-top: 10px;
                flex-shrink: 0;

                :deep(.el-input__wrapper) {
                    border-radius: 10px;
                    background: #fff;
                    box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.15) inset;

                    &:hover,
                    &.is-focus {
                        box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.45) inset;
                    }
                }
            }
        }
        .session-panel .session-list {
            flex: 1;
            min-height: 0;
            overflow-y: auto;
            margin-top: 12px;

            .group-label {
                font-size: 12px;
                color: #999;
                padding: 8px 8px 4px;

                &:not(:first-child) {
                    margin-top: 6px;
                }
            }

            /* 单行标题，高度从原来三行的 60px 降到 36px */
            .session-item {
                display: flex;
                align-items: center;
                gap: 6px;
                height: 36px;
                padding: 0 8px;
                border-radius: 8px;
                cursor: pointer;
                transition: background 0.25s cubic-bezier(0.22, 1, 0.36, 1);

                &:hover {
                    background: rgba(64, 158, 255, 0.06);

                    .ops-btn {
                        opacity: 1;
                    }
                }

                &.active {
                    background: rgba(64, 158, 255, 0.12);

                    .session-title {
                        color: #409eff;
                        font-weight: 600;
                    }
                }

                .session-title {
                    flex: 1;
                    min-width: 0;
                    font-size: 13px;
                    color: #333;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }
            }
        }
        /* 操作入口平时隐形，hover 或菜单展开时才出现，避免列表看起来很碎 */
        .session-panel .session-list .session-item {
            .ops-btn {
                display: flex;
                align-items: center;
                justify-content: center;
                width: 22px;
                height: 22px;
                border-radius: 6px;
                color: #999;
                opacity: 0;
                flex-shrink: 0;
                transition: opacity 0.2s ease, background 0.2s ease, color 0.2s ease;

                &:hover {
                    background: rgba(64, 158, 255, 0.14);
                    color: #409eff;
                }
            }

            :deep(.el-dropdown) {
                display: flex;
                flex-shrink: 0;
            }
        }

        .session-panel .session-list .empty {
            padding: 28px 12px;
            text-align: center;
            font-size: 13px;
            color: #999;
        }
    }

    /* ===== 主聊天区域 ===== */
    .chat-main {
        flex: 1;
        display: flex;
        flex-direction: column;
        background: #ffffff;
        overflow: hidden;
        position: relative;
        z-index: 1;

        .chat-header {
            padding: 14px 20px;
            border-bottom: 1px solid rgba(64, 158, 255, 0.12);
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-shrink: 0;
            background: #fafbfc;

            .header-left {
                display: flex;
                align-items: center;
                gap: 10px;

                .chat-avatar {
                    width: 32px;
                    height: 32px;
                    background: #409eff;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: white;
                }

                .chat-info {
                    h2 {
                        font-size: 14px;
                        font-weight: 600;
                        color: #333;
                        margin: 0;
                    }

                    p {
                        font-size: 11px;
                        color: #999;
                        margin: 0;
                    }
                }
            }

            /* 情绪 chip：常驻但只占一个胶囊的宽度，不吃消息区高度 */
            .emotion-chip {
                display: flex;
                align-items: center;
                gap: 6px;
                height: 28px;
                padding: 0 10px;
                border-radius: 14px;
                font-size: 12px;
                cursor: pointer;
                border: 1px solid transparent;
                transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.25s ease;

                &:hover {
                    transform: translateY(-1px);
                    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.16);
                }

                .chip-emoji {
                    font-size: 14px;
                }

                .chip-arrow {
                    font-size: 11px;
                    opacity: 0.65;
                }

                /* 等级 0-3 对应 正常/关注/预警/危机 */
                &.risk-0 { background: rgba(103, 194, 58, 0.12); border-color: rgba(103, 194, 58, 0.3); color: #529b2e; }
                &.risk-1 { background: rgba(230, 162, 60, 0.12); border-color: rgba(230, 162, 60, 0.3); color: #b88230; }
                &.risk-2 { background: rgba(245, 108, 108, 0.12); border-color: rgba(245, 108, 108, 0.3); color: #c45656; }
                &.risk-3 { background: rgba(245, 108, 108, 0.2); border-color: rgba(245, 108, 108, 0.45); color: #a83232; }
            }
        }

        /* ===== 消息区：内容居中限宽，长文不会横跨整个宽屏 ===== */
        .chat-messages {
            flex: 1;
            overflow-y: auto;
            background: #ffffff;

            .messages-inner {
                max-width: 820px;
                margin: 0 auto;
                padding: 24px 20px 8px;
                display: flex;
                flex-direction: column;
                gap: 22px;
            }
        }

        .chat-messages .message-row {
            display: flex;
            gap: 10px;
            align-items: flex-start;

            /* 用户消息：右对齐蓝色气泡，一眼能和 AI 区分开 */
            &.user {
                justify-content: flex-end;

                .msg-body { max-width: 78%; }

                .msg-content {
                    background: #409eff;
                    color: #fff;
                    padding: 10px 14px;
                    border-radius: 14px 14px 4px 14px;
                    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.22);
                }

                .msg-actions { justify-content: flex-end; }
            }

            /* AI 消息：不套气泡，靠头像和留白区分，长回答读起来更顺 */
            &.assistant .msg-body { max-width: calc(100% - 42px); }

            .msg-avatar {
                width: 32px;
                height: 32px;
                flex-shrink: 0;
                margin-top: 2px;
                border-radius: 50%;
                background: linear-gradient(135deg, #409eff, #66b1ff);
                color: #fff;
                display: flex;
                align-items: center;
                justify-content: center;
                box-shadow: 0 4px 10px rgba(64, 158, 255, 0.25);
            }

            .msg-body { min-width: 0; }
        }
        .chat-messages .message-row {
            .msg-content {
                font-size: 15px;
                line-height: 1.8;
                color: #333;
                word-break: break-word;

                :deep(p) {
                    margin: 0 0 10px;
                    &:last-child { margin-bottom: 0; }
                }
            }

            /* 复制按钮平时不显示，避免消息流里挂一排灰色小字 */
            .msg-actions {
                display: flex;
                gap: 12px;
                margin-top: 6px;
                opacity: 0;
                transition: opacity 0.2s ease;

                .act-btn {
                    display: inline-flex;
                    align-items: center;
                    gap: 4px;
                    font-size: 12px;
                    color: #999;
                    cursor: pointer;

                    &:hover { color: #409eff; }
                }
            }

            &:hover .msg-actions { opacity: 1; }

            .typing-indicator {
                display: flex;
                gap: 5px;
                padding: 10px 0;

                span {
                    width: 7px;
                    height: 7px;
                    border-radius: 50%;
                    background: #409eff;
                    opacity: 0.55;
                    animation: typing 1.4s infinite;

                    &:nth-child(2) { animation-delay: 0.2s; }
                    &:nth-child(3) { animation-delay: 0.4s; }
                }
            }
        }
        /* ===== 输入区：大圆角容器，操作行在下方，参考 DeepSeek ===== */
        .chat-input-wrapper {
            flex-shrink: 0;
            background: #ffffff;
            padding: 8px 20px 12px;

            .input-box {
                max-width: 820px;
                margin: 0 auto;
                padding: 10px 12px 8px;
                background: #f7f8fa;
                border: 1px solid rgba(64, 158, 255, 0.18);
                border-radius: 20px;
                transition: border-color 0.25s ease, box-shadow 0.25s ease, background 0.25s ease;

                &:focus-within {
                    background: #fff;
                    border-color: rgba(64, 158, 255, 0.55);
                    box-shadow: 0 6px 20px rgba(64, 158, 255, 0.12);
                }
            }

            .input-hint {
                text-align: center;
                font-size: 11px;
                color: #c0c4cc;
                padding-top: 8px;
            }
        }
        .chat-input-wrapper .input-box {
            textarea {
                display: block;
                width: 100%;
                border: none;
                outline: none;
                resize: none;
                padding: 0 4px;
                font-size: 15px;
                line-height: 24px;
                color: #333;
                background: transparent;
                font-family: inherit;
                overflow-y: auto;
                max-height: 180px;

                &:disabled { opacity: 0.6; cursor: not-allowed; }

                &::placeholder { color: #c0c4cc; }
            }

            .input-actions {
                display: flex;
                align-items: center;
                justify-content: flex-end;
                gap: 10px;
                margin-top: 6px;

                .input-count {
                    font-size: 11px;
                    color: #c0c4cc;
                }

                /* 圆形发送按钮，禁用时降到浅灰而不是浅蓝，能不能发一眼就看出来 */
                .send-btn {
                    width: 32px;
                    height: 32px;
                    border: none;
                    border-radius: 50%;
                    background: #409eff;
                    color: #fff;
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    flex-shrink: 0;
                    transition: background 0.25s ease, transform 0.25s cubic-bezier(0.22, 1, 0.36, 1);

                    &:hover:not(:disabled) {
                        background: #66b1ff;
                        transform: scale(1.06);
                    }

                    &:disabled {
                        background: #dcdfe6;
                        color: #fff;
                        cursor: not-allowed;
                    }
                }
            }
        }
    }
}

/* ===== 动画 ===== */
@keyframes breathing {
    0%, 100% { transform: scale(1); opacity: 0.8; }
    50% { transform: scale(1.1); opacity: 1; }
}

@keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
}

@keyframes typing {
    0%, 60%, 100% { transform: translateY(0); }
    30% { transform: translateY(-6px); }
}

/* ===== 分析抽屉 ===== */
/* 抽屉挂在 body 上，scoped 选择器命中不了内部，样式写在下面的全局块里 */
</style>

<style lang="scss">
.emotion-detail {
    .detail-head {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 14px 16px;
        border-radius: 14px;
        background: linear-gradient(135deg, rgba(64, 158, 255, 0.1), rgba(103, 194, 58, 0.05));
        border: 1px solid rgba(64, 158, 255, 0.15);

        .head-emoji {
            font-size: 30px;
            line-height: 1;
        }

        .head-name {
            font-size: 17px;
            font-weight: 700;
            color: #333;
        }

        .head-risk {
            margin-top: 4px;
            font-size: 12px;

            &.risk-0 { color: #529b2e; }
            &.risk-1 { color: #b88230; }
            &.risk-2 { color: #c45656; }
            &.risk-3 { color: #a83232; }
        }
    }

    .detail-block {
        margin-top: 18px;

        .block-label {
            font-size: 12px;
            color: #999;
            letter-spacing: 1px;
            margin-bottom: 8px;
        }

        .block-text {
            margin: 0;
            font-size: 13px;
            line-height: 1.75;
            color: #666;
        }

        &.suggestion {
            padding: 12px 14px;
            border-radius: 12px;
            background: rgba(103, 194, 58, 0.08);
            border: 1px solid rgba(103, 194, 58, 0.2);

            .block-text { color: #333; }
        }
    }

    .action-item {
        display: flex;
        align-items: flex-start;
        gap: 8px;
        padding: 8px 10px;
        border-radius: 10px;
        font-size: 13px;
        color: #666;
        background: rgba(64, 158, 255, 0.05);
        transition: background 0.25s cubic-bezier(0.22, 1, 0.36, 1);

        & + .action-item { margin-top: 8px; }

        &:hover { background: rgba(64, 158, 255, 0.1); }

        .check-icon {
            color: #67c23a;
            font-size: 14px;
            margin-top: 2px;
            flex-shrink: 0;
        }
    }
}
</style>
