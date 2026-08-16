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

            <!-- 投资情绪花园 - 紧凑折叠版 -->
            <div class="emotion-garden">
                <div class="garden-header" @click="gardenCollapsed = !gardenCollapsed" style="cursor: pointer;">
                    <span class="garden-title">投资情绪花园</span>
                    <el-icon class="toggle-icon" :class="{ collapsed: gardenCollapsed }"><ArrowDown /></el-icon>
                </div>

                <transition name="slide-fade">
                    <div class="garden-core" v-show="!gardenCollapsed">
                        <div class="emotion-badge" :style="{ background: emotionGradient }">
                            <span class="emotion-icon">{{ emotionIcon }}</span>
                        </div>
                        <div class="emotion-summary">
                            <div class="emotion-name">{{ currentEmotion.primaryEmotion || '等待分析...' }}</div>
                            <el-progress
                                type="dashboard"
                                :percentage="currentEmotion.emotionScore"
                                :stroke-width="8"
                                :width="60"
                                :color="emotionProgressColor"
                            />
                            <div class="emotion-desc" v-if="currentEmotion.riskDescription">{{ currentEmotion.riskDescription }}</div>
                        </div>
                        <div class="risk-tag" :class="'level-' + currentEmotion.riskLevel">
                            风险等级：{{ getRiskLevelText(currentEmotion.riskLevel) }}
                        </div>
                    </div>
                </transition>

                <transition name="slide-fade">
                    <div class="garden-details" v-show="!gardenCollapsed && currentEmotion.primaryEmotion">
                        <div class="status-text">
                            {{ currentEmotion.isNegative ? '⚠️ 需要关注' : '✨ 状态良好' }}
                        </div>
                        <div class="suggestion-card" v-if="currentEmotion.suggestion">
                            <span class="icon">💡</span>
                            <span class="text">{{ currentEmotion.suggestion }}</span>
                        </div>
                        <div class="actions-list" v-if="currentEmotion.improvementSuggestions?.length">
                            <div class="actions-title">建议行动：</div>
                            <div v-for="(action, i) in currentEmotion.improvementSuggestions" :key="i" class="action-item">
                                <el-icon class="check-icon"><Checked /></el-icon>
                                <span>{{ action }}</span>
                            </div>
                        </div>
                    </div>
                </transition>
            </div>

            <!-- 会话列表 -->
            <div class="session-history">
                <div class="section-header">
                    <h4 class="section-title">会话列表</h4>
                    <el-button size="small" class="new-btn" @click="createNewSession">
                        <el-icon><Plus /></el-icon>
                    </el-button>
                </div>
                <div class="session-list">
                    <div
                      v-for="session in sessions"
                      :key="session.sessionId || session.id"
                      @click="handleSessionClick($event, session)"
                      class="session-item-wrapper"
                    >
                      <div class="session-item" :class="{ active: currentSession?.sessionId === session.sessionId }">
                        <div class="session-content">
                          <div class="session-title">{{ session.title || '新对话' }}</div>
                          <div class="session-preview">{{ session.lastMessage || '暂无消息' }}</div>
                        </div>
                        <div class="session-time">{{ formatTime(session.updatedAt || session.createdAt) }}</div>
                      </div>
                    </div>
                    <div v-if="sessions.length === 0" class="empty">
                      <el-empty :image-size="50" description="暂无会话" />
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
                <el-button circle class="new-chat-btn" @click="createNewSession" title="新建会话">
                    <el-icon><Plus /></el-icon>
                </el-button>
            </div>

            <div class="chat-messages" ref="messagesContainer">
                <div v-for="msg in messages" :key="msg.id" class="message-item" :class="msg.role">
                    <div class="msg-content">
                        <div v-if="isAiTyping && msg.role === 'assistant' && messages.at(-1) === msg" class="typing-indicator">
                            <span></span><span></span><span></span>
                        </div>
                        <div v-else v-html="formatMessage(msg.content)"></div>
                    </div>
                </div>
            </div>

            <!-- 底部提示文本 -->
            <div class="input-hint">AI 可能会犯错，请核实重要信息</div>

            <!-- 输入框容器 -->
            <div class="chat-input-wrapper">
                <div class="input-box" :style="{ height: inputHeight + 'px' }">
                    <textarea
                        v-model="inputText"
                        placeholder="给 AI 发送消息..."
                        :disabled="isAiTyping"
                        @keydown="handleKeyDown"
                        @input="handleInput"
                        :rows="1"
                        maxlength="5000" />
                    <button class="send-btn" :disabled="!inputText.trim() || isAiTyping" @click="sendMessage">
                        <el-icon><Promotion /></el-icon>
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { ArrowDown, Connection, Plus, Promotion, User, Checked } from '@element-plus/icons-vue'
import { startSession, getSessionList, deleteSession, getSessionDetail, sendMessageStream, getSessionEmotion } from '@/api/frontend'

const sessions = ref([])
const currentSession = ref(null)
const messages = ref([])
const inputText = ref('')
const isAiTyping = ref(false)
const messagesContainer = ref(null)
const gardenCollapsed = ref(false)
const inputHeight = ref(44) // 初始高度

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

const emotionGradient = computed(() => {
  const score = currentEmotion.value.emotionScore
  if (score >= 80) return 'linear-gradient(135deg, #10b981, #34d399)'
  if (score >= 60) return 'linear-gradient(135deg, #3b82f6, #60a5fa)'
  if (score >= 40) return 'linear-gradient(135deg, #f59e0b, #fbbf24)'
  if (score >= 20) return 'linear-gradient(135deg, #ef4444, #f87171)'
  return 'linear-gradient(135deg, #dc2626, #fb923c)'
})

const emotionProgressColor = computed(() => {
  const score = currentEmotion.value.emotionScore
  if (score >= 60) return '#10b981'
  if (score >= 40) return '#f59e0b'
  return '#ef4444'
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

const handleSessionClick = (event, session) => {
  event.preventDefault()
  event.stopPropagation()
  switchSession(session)
}

// 处理输入框自动高度
const handleInput = (e) => {
    const textarea = e.target
    textarea.style.height = 'auto'
    const scrollHeight = textarea.scrollHeight
    const newHeight = Math.max(44, Math.min(scrollHeight, 200)) // 最小 44，最大 200
    inputHeight.value = newHeight
}

const sendMessage = async () => {
    if (!inputText.value.trim() || isAiTyping.value) return

    const userMessage = inputText.value.trim()
    inputText.value = ''
    inputHeight.value = 44 // 重置高度

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
            currentSession.value = {
                sessionId: res.sessionId,
                status: 'ACTIVE',
                title: res.title || sessionParams.title
            }
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

        const reader = response.body.getReader()
        const decoder = new TextDecoder()

        while (true) {
            const { done, value } = await reader.read()
            if (done) break

            const text = decoder.decode(value)
            const lines = text.split('\n')

            for (const line of lines) {
                if (!line.startsWith('data: ')) continue
                const data = line.slice(6)
                if (!data) continue

                try {
                    const parsed = JSON.parse(data)
                    if (parsed.content) {
                        aiMessage.content += parsed.content
                        await nextTick()
                        scrollToBottom()
                    }
                } catch (e) {
                    if (data.trim()) {
                        aiMessage.content += data
                        await nextTick()
                        scrollToBottom()
                    }
                }
            }
        }

        isAiTyping.value = false

        try {
            const res = await getSessionList()
            sessions.value = res || []
            const currentId = currentSession.value?.sessionId
            const exists = sessions.value.some(s => s.sessionId === currentId)
            if (!exists && sessions.value.length > 0) {
                currentSession.value = sessions.value[0]
            }
        } catch (e) {
            console.error('加载会话列表失败:', e)
        }

        if (currentSession.value?.sessionId && !currentSession.value.sessionId.startsWith('temp_')) {
            loadSessionEmotion(currentSession.value.sessionId)
        }

        // 自动触发情绪分析
        try {
            await axios.post('/api/chat/analyze-session', {
                sessionId: currentSession.value.sessionId
            })
            await loadSessionEmotion(currentSession.value.sessionId)
        } catch (e) {
            console.error('情绪分析失败:', e)
        }
    } catch (error) {
        console.error('发送消息失败:', error)
        isAiTyping.value = false
        aiMessage.content = '抱歉，回复失败，请重试。'
        ElMessage.error('发送消息失败')
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

const formatMessage = (content) => {
    if (!content) return ''
    return content
        .replace(/\n/g, '<br>')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>')
}

const formatTime = (date) => {
    if (!date) return ''
    const d = new Date(date)
    const now = new Date()
    const diff = now - d

    if (diff < 60000) return '刚刚'
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`

    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
    loadSessions()
})
</script>

<style scoped lang="scss">
.consultation-container {
    height: calc(100vh - 60px);
    display: flex;
    overflow: hidden;

    /* ===== 左侧边栏 ===== */
    .sidebar {
        width: 320px;
        background: #f7f7f8;
        border-right: 1px solid #ebebeb;
        display: flex;
        flex-direction: column;
        flex-shrink: 0;
        overflow-y: auto;
        position: relative;
        z-index: 10;

        .ai-assistant-info {
            padding: 16px;
            border-bottom: 1px solid #ebebeb;
            text-align: center;

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
                color: #1f2937;
                margin: 0 0 8px;
            }

            .online-status {
                font-size: 11px;
                color: #10b981;
                display: inline-flex;
                align-items: center;
                gap: 4px;

                .status-dot {
                    width: 6px;
                    height: 6px;
                    background: #10b981;
                    border-radius: 50%;
                    animation: pulse 2s infinite;
                }
            }
        }

        .emotion-garden {
            padding: 12px;
            border-bottom: 1px solid #ebebeb;

            .garden-header {
                display: flex;
                align-items: center;
                justify-content: space-between;
                padding: 0 4px;
                margin-bottom: 8px;

                .garden-title {
                    font-size: 12px;
                    font-weight: 600;
                    color: #6b7280;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }

                .toggle-icon {
                    transition: transform 0.3s ease;
                    color: #9ca3af;

                    &.collapsed {
                        transform: rotate(-90deg);
                    }
                }
            }

            .garden-core {
                background: linear-gradient(135deg, rgba(64, 158, 255, 0.08), rgba(103, 194, 58, 0.05));
                border-radius: 12px;
                padding: 16px;
                border: 1px solid rgba(64, 158, 255, 0.15);
                box-shadow: 0 2px 8px rgba(64, 158, 255, 0.08);

                .emotion-badge {
                    width: 56px;
                    height: 56px;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    margin: 0 auto 10px;
                    box-shadow: 0 3px 12px rgba(0,0,0,0.12);
                    transition: transform 0.3s ease;

                    &:hover {
                        transform: scale(1.1);
                    }

                    .emotion-icon {
                        font-size: 26px;
                    }
                }

                .emotion-summary {
                    text-align: center;
                    margin-bottom: 10px;

                    .emotion-name {
                        font-size: 14px;
                        font-weight: 600;
                        color: #1f2937;
                        margin-bottom: 6px;
                    }

                    .emotion-desc {
                        font-size: 11px;
                        color: #6b7280;
                        margin-top: 6px;
                        line-height: 1.4;
                    }
                }

                .risk-tag {
                    display: inline-block;
                    font-size: 11px;
                    padding: 3px 12px;
                    border-radius: 12px;
                    background: #ecfdf5;
                    color: #059669;
                    font-weight: 500;
                    transition: all 0.2s ease;

                    &.level-0 { background: #ecfdf5; color: #059669; }
                    &.level-1 { background: #fffbeb; color: #d97706; }
                    &.level-2 { background: #fee2e2; color: #dc2626; }
                    &.level-3 { background: #fef2f2; color: #b91c1c; }
                }
            }

            .garden-details {
                margin-top: 12px;
                padding-top: 12px;
                border-top: 1px dashed rgba(64, 158, 255, 0.2);

                .status-text {
                    font-size: 12px;
                    color: #6b7280;
                    text-align: center;
                    margin-bottom: 10px;
                    padding: 6px;
                    border-radius: 6px;
                    background: #f8f9fa;
                }

                .suggestion-card {
                    background: linear-gradient(135deg, rgba(64, 158, 255, 0.1), rgba(103, 194, 58, 0.05));
                    border-left: 3px solid #409eff;
                    border-radius: 8px;
                    padding: 10px 12px;
                    margin-bottom: 10px;
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    font-size: 12px;
                    color: #1e40af;
                    box-shadow: 0 1px 3px rgba(64, 158, 255, 0.08);

                    .icon { font-size: 16px; flex-shrink: 0; }
                    .text { flex: 1; line-height: 1.5; }
                }

                .actions-list {
                    .actions-title {
                        font-size: 11px;
                        font-weight: 600;
                        color: #6b7280;
                        margin-bottom: 6px;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                    }

                    .action-item {
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        font-size: 12px;
                        color: #4b5563;
                        padding: 6px 0;
                        transition: all 0.2s ease;

                        &:hover {
                            color: #409eff;
                        }

                        .check-icon {
                            color: #10b981;
                            font-size: 14px;
                            flex-shrink: 0;
                        }
                    }
                }
            }
        }

        .session-history {
            flex: 1;
            display: flex;
            flex-direction: column;
            min-height: 0;

            .section-header {
                display: flex;
                align-items: center;
                justify-content: space-between;
                padding: 12px 16px;
                background: white;
                border-bottom: 1px solid #ebebeb;
                flex-shrink: 0;

                .section-title {
                    font-size: 13px;
                    font-weight: 600;
                    color: #6b7280;
                    margin: 0;
                }

                .new-btn {
                    padding: 4px 10px;
                    font-size: 12px;
                    height: 26px;
                }
            }

            .session-list {
                flex: 1;
                overflow-y: auto;
                padding: 8px;

                .session-item {
                    padding: 10px 12px;
                    border-radius: 8px;
                    cursor: pointer;
                    transition: all 0.2s ease;
                    margin-bottom: 4px;
                    display: flex;
                    flex-direction: column;
                    gap: 4px;

                    &:hover { background: #ececec; }

                    &.active {
                        background: #eef2ff;
                        border-left: 3px solid #409eff;
                    }

                    .session-content {
                        display: flex;
                        flex-direction: column;
                        gap: 2px;

                        .session-title {
                            font-size: 13px;
                            font-weight: 500;
                            color: #1f2937;
                            white-space: nowrap;
                            overflow: hidden;
                            text-overflow: ellipsis;
                        }

                        .session-preview {
                            font-size: 11px;
                            color: #9ca3af;
                            white-space: nowrap;
                            overflow: hidden;
                            text-overflow: ellipsis;
                        }
                    }

                    .session-time {
                        font-size: 11px;
                        color: #9ca3af;
                        text-align: right;
                    }
                }

                .empty { padding: 20px; text-align: center; }
            }
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
            border-bottom: 1px solid #ebebeb;
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-shrink: 0;
            background: #fafafa;

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
                        color: #1f2937;
                        margin: 0;
                    }

                    p {
                        font-size: 11px;
                        color: #9ca3af;
                        margin: 0;
                    }
                }
            }

            .new-chat-btn {
                width: 28px;
                height: 28px;
                padding: 0;
                border-radius: 50%;
            }
        }

        .chat-messages {
            flex: 1;
            overflow-y: auto;
            padding: 20px;
            display: flex;
            flex-direction: column;
            gap: 16px;
            background: #ffffff;

            .message-item {
                display: flex;
                flex-direction: column;
                max-width: 85%;
                padding: 12px 16px;
                border-radius: 12px;

                &.user {
                    align-self: flex-end;
                    background: #f2f4f7;

                    .msg-content {
                        color: #1f2937;
                    }
                }

                &.assistant {
                    align-self: flex-start;
                    background: transparent;

                    .msg-content {
                        color: #1f2937;
                    }
                }

                .msg-content {
                    font-size: 14px;
                    line-height: 1.6;
                    word-wrap: break-word;

                    p {
                        margin: 0 0 8px;
                        &:last-child { margin-bottom: 0; }
                    }
                }
            }

            .typing-indicator {
                display: flex;
                gap: 4px;
                padding: 4px 0;

                span {
                    width: 6px;
                    height: 6px;
                    border-radius: 50%;
                    background: #9ca3af;
                    animation: typing 1.4s infinite;

                    &:nth-child(2) { animation-delay: 0.2s; }
                    &:nth-child(3) { animation-delay: 0.4s; }
                }
            }
        }

        .input-hint {
            text-align: center;
            font-size: 11px;
            color: #9ca3af;
            padding: 6px;
            flex-shrink: 0;
        }

        .chat-input-wrapper {
            background: #fafafa;
            border-top: 1px solid #ebebeb;
            padding: 12px 20px;
            flex-shrink: 0;

            .input-box {
                max-width: 800px;
                margin: 0 auto;
                position: relative;
                background: white;
                border-radius: 12px;
                border: 1px solid #e5e5e5;
                box-shadow: 0 2px 8px rgba(0,0,0,0.04);
                display: flex;
                align-items: flex-end;
                transition: border-color 0.2s, box-shadow 0.2s;

                &:focus-within {
                    border-color: #409eff;
                    box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
                }

                textarea {
                    flex: 1;
                    border: none;
                    outline: none;
                    resize: none;
                    padding: 10px 12px;
                    font-size: 14px;
                    line-height: 1.5;
                    background: transparent;
                    font-family: inherit;
                    overflow-y: hidden;

                    &:disabled {
                        opacity: 0.6;
                    }

                    ::placeholder {
                        color: #9ca3af;
                    }
                }

                .send-btn {
                    width: 36px;
                    height: 36px;
                    border: none;
                    background: #409eff;
                    color: white;
                    border-radius: 8px;
                    margin: 6px 8px;
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    transition: all 0.2s;
                    flex-shrink: 0;

                    &:hover:not(:disabled) {
                        background: #66b1ff;
                    }

                    &:disabled {
                        background: #b3d8ff;
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

.slide-fade-enter-active,
.slide-fade-leave-active {
    transition: all 0.3s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
    transform: translateY(-10px);
    opacity: 0;
}
</style>
