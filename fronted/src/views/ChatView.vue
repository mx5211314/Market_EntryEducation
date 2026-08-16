<template>
    <div class="consultation-container">
        <div class="sidebar">
            <!-- AI助手信息 -->
             <div class="ai-assistant-info">
                <div class="breathing-circle">
                    <el-icon size="24"><Connection /></el-icon>
                </div>
                <h3 class="assistant-name">入市教育AI助手</h3>
                <div class="online-status">
                    <div class="status-dot"></div>
                    在线服务中
                </div>
             </div>
             <!-- 投资情绪花园 -->
             <div class="investment-emotion-garden" v-if="currentEmotion.primaryEmotion">
                <div class="garden-header">
                    <span class="garden-title">投资情绪花园</span>
                </div>
                <div class="emotion-info">
                    <div class="emotion-circle" :style="{ background: emotionGradient }">
                        <span class="emotion-icon">{{ emotionIcon }}</span>
                    </div>
                    <div class="emotion-name">{{ currentEmotion.primaryEmotion }}</div>
                    <div class="emotion-score">
                        <el-progress
                            type="dashboard"
                            :percentage="currentEmotion.emotionScore"
                            :stroke-width="6"
                            :width="80"
                            :color="emotionProgressColor" />
                    </div>
                </div>
                <div class="warm-tips">
                    <div class="emotion-status-text">
                        <span class="status-label">今日状态</span>
                        <span class="status-emotion" :class="{ negative: currentEmotion.isNegative }">
                            {{ currentEmotion.isNegative ? '需要关注' : '很不错' }}
                        </span>
                    </div>
                    <div class="emotion-intensity">
                        <span class="intensity-dots">
                            <span
                                v-for="dot in 3"
                                :key="dot"
                                class="dot"
                                :class="{ active: getIntensityLevel(currentEmotion.emotionScore) >= dot }" />
                        </span>
                        <span class="intensity-text">{{ getRiskLevelText(currentEmotion.riskLevel) }}</span>
                    </div>
                    <!-- AI 建议卡片 -->
                    <div class="ai-suggestion" v-if="currentEmotion.suggestion">
                        <div class="suggestion-icon">💡</div>
                        <div class="suggestion-content">
                            <div class="suggestion-title">AI 建议</div>
                            <div class="suggestion-text">{{ currentEmotion.suggestion }}</div>
                        </div>
                    </div>
                    <!-- 改善建议 -->
                    <div class="improvement-actions" v-if="currentEmotion.improvementSuggestions?.length > 0">
                        <div class="actions-title">成长建议</div>
                        <div class="actions-list">
                            <div
                                v-for="(action, index) in currentEmotion.improvementSuggestions"
                                :key="index"
                                class="action-item">
                                <span class="action-icon">🌱</span>
                                <span class="action-text">{{ action }}</span>
                            </div>
                        </div>
                    </div>
                    <!-- 风险提示 -->
                    <div class="risk-notice" v-if="currentEmotion.riskLevel >= 2">
                        <div class="notice-icon">⚠️</div>
                        <div class="notice-content">
                            <div class="notice-title">风险提示</div>
                            <div class="notice-text">{{ currentEmotion.riskDescription }}</div>
                        </div>
                    </div>
                </div>
             </div>
             <!-- 会话列表 -->
             <div class="session-history">
                <div class="section-header">
                    <h4 class="section-title">会话列表</h4>
                    <el-button size="small" type="primary" @click="createNewSession">
                        <el-icon><Plus /></el-icon>
                        新建会话
                    </el-button>
                </div>
                <div class="session-list">
                    <div v-for="session in sessions" :key="session.sessionId || session.id" @click="switchSession(session)" class="session-item" :class="{ active: currentSession?.sessionId === session.sessionId }">
                        <div class="session-info">
                            <div class="session-title">
                                <span>{{ session.title || '新对话' }}</span>
                            </div>
                            <div class="session-meta">
                                <span>{{ formatTime(session.updatedAt || session.createdAt) }}</span>
                            </div>
                            <div class="session-preview">
                                {{ session.lastMessage || '暂无消息' }}
                            </div>
                        </div>
                    </div>
                    <div v-if="sessions.length === 0" class="empty-sessions">
                        <el-empty description="暂无会话" />
                    </div>
                </div>
             </div>
        </div>
        <div class="chat-main">
            <div class="chat-header">
                <div class="header-left">
                    <div class="chat-avatar">
                        <el-icon size="20"><Connection /></el-icon>
                    </div>
                    <div class="chat-info">
                        <h2>入市教育AI助手</h2>
                        <p>您的金融法规智能助手</p>
                    </div>
                </div>
                <el-button circle @click="createNewSession" title="新建会话">
                    <el-icon><Plus /></el-icon>
                </el-button>
            </div>
            <!-- 聊天消息区域 -->
            <div class="chat-messages" ref="messagesContainer">
                <!-- 消息列表（欢迎语已包含在 messages 数组中） -->
                <div v-for="msg in messages" :key="msg.id" class="message-item" :class="msg.role === 'user' ? 'user-message' : 'ai-message'">
                    <div class="message-avatar">
                        <el-icon v-if="msg.role === 'user'" size="16"><User /></el-icon>
                        <el-icon v-else size="16"><Connection /></el-icon>
                    </div>
                    <div class="message-content">
                        <div class="message-bubble">
                            <!-- AI正在思考中 -->
                            <div v-if="msg.role === 'assistant' && isAiTyping && msg.id === typingMessageId" class="typing-indicator">
                                <div class="typing-dot"></div>
                                <div class="typing-dot"></div>
                                <div class="typing-dot"></div>
                            </div>
                            <!-- 正常消息 -->
                            <div v-else v-html="formatMessage(msg.content)"></div>
                        </div>
                        <div class="message-time">{{ formatMessageTime(msg.createdAt) }}</div>
                    </div>
                </div>
            </div>
            <!-- 消息输入区域 -->
            <div class="chat-input">
                <div class="input-container">
                    <el-input
                        v-model="inputText"
                        placeholder="请输入您想要了解的内容..."
                        type="textarea"
                        :rows="3"
                        :disabled="isAiTyping"
                        @keydown="handleKeyDown"
                        class="message-input"
                        clearable
                        maxlength="1000"
                        show-word-limit />
                </div>
                <el-button :disabled="!inputText.trim() || isAiTyping" type="primary" class="send-btn" @click="sendMessage">
                    <el-icon><Promotion /></el-icon>
                </el-button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Connection, Plus, Promotion, User } from '@element-plus/icons-vue'
import { startSession, getSessionList, deleteSession, getSessionDetail, sendMessageStream, getSessionEmotion } from '@/api/frontend'

const router = useRouter()
const sessions = ref([])
const currentSession = ref(null)
const messages = ref([])
const inputText = ref('')
const isAiTyping = ref(false)
const messagesContainer = ref(null)
const typingMessageId = ref(null)

// 欢迎消息常量
const WELCOME_MSG = '您好！我是您的入市教育智慧助手，很高兴为您提供金融法规相关的服务。请告诉我，您想了解什么内容？'

// 投资情绪花园
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
  if (score >= 80) return 'linear-gradient(135deg, #00b894, #55efc4)'
  if (score >= 60) return 'linear-gradient(135deg, #0984e3, #74b9ff)'
  if (score >= 40) return 'linear-gradient(135deg, #fdcb6e, #ffeaa7)'
  if (score >= 20) return 'linear-gradient(135deg, #e17055, #fab1a0)'
  return 'linear-gradient(135deg, #d63031, #ff7675)'
})

const emotionProgressColor = computed(() => {
  const score = currentEmotion.value.emotionScore
  if (score >= 60) return '#67c23a'
  if (score >= 40) return '#e6a23c'
  return '#f56c6c'
})

const getIntensityLevel = (score) => {
  if (score >= 61) return 3
  if (score >= 31) return 2
  return 1
}

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

// 加载会话列表
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

// 新建临时会话（仅本地，不调后端）
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
}

// 切换到已有会话 — 从后端加载真实历史消息
const switchSession = async (session) => {
    // 如果已经是当前会话，跳过
    if (currentSession.value?.sessionId === session.sessionId) return
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
    // 加载情绪分析
    if (session.sessionId && !session.sessionId.startsWith('temp_')) {
        loadSessionEmotion(session.sessionId)
    }
}

const sendMessage = async () => {
    if (!inputText.value.trim() || isAiTyping.value) return

    const userMessage = inputText.value.trim()
    inputText.value = ''

    // 添加用户消息到界面
    messages.value.push({
        id: Date.now(),
        role: 'user',
        content: userMessage,
        createdAt: new Date()
    })
    await nextTick()
    scrollToBottom()

    // 如果是临时会话，先在后端创建真实会话
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

    // 准备 AI 回复占位
    isAiTyping.value = true
    const aiMessageId = Date.now() + 1
    typingMessageId.value = aiMessageId
    const aiMessage = {
        id: aiMessageId,
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
                    // 后端可能发送纯文本，尝试直接使用
                    if (data.trim()) {
                        aiMessage.content += data
                        await nextTick()
                        scrollToBottom()
                    }
                }
            }
        }

        isAiTyping.value = false
        typingMessageId.value = null

        // 重新加载会话列表（不调用 switchSession 避免清空当前消息）
        try {
            const res = await getSessionList()
            sessions.value = res || []
            // 如果当前会话是刚创建的（列表中没有），自动选中第一条
            const currentId = currentSession.value?.sessionId
            const exists = sessions.value.some(s => s.sessionId === currentId)
            if (!exists && sessions.value.length > 0) {
                currentSession.value = sessions.value[0]
            }
        } catch (e) {
            console.error('加载会话列表失败:', e)
        }

        // 加载情绪分析
        if (currentSession.value?.sessionId && !currentSession.value.sessionId.startsWith('temp_')) {
            loadSessionEmotion(currentSession.value.sessionId)
        }
    } catch (error) {
        console.error('发送消息失败:', error)
        isAiTyping.value = false
        typingMessageId.value = null
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

    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const formatMessageTime = (date) => {
    if (!date) return ''
    const d = new Date(date)
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
    loadSessions()
})
</script>

<style scoped lang="scss">
.consultation-container {
    height: calc(100vh - 60px);
    background-color: #f1f4f6;
    display: flex;
}

.sidebar {
    width: 320px;
    background: white;
    display: flex;
    flex-direction: column;
    box-shadow: 0px 0px 12px 0px rgba(0,0,0,0.08);
    flex-shrink: 0;
    overflow-y: auto;
}

/* AI助手信息 */
.ai-assistant-info {
    background: white;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0px 1px 1px rgba(0,0,0,0.05);
    border-bottom: 1px solid #ebeef5;
    flex-shrink: 0;
}

.breathing-circle {
    width: 64px;
    height: 64px;
    background: #409eff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 12px;
    animation: breathing 4s ease-in-out infinite;
    box-shadow: 0 6px 16px rgba(64, 158, 255, 0.2);
}

.assistant-name {
    font-size: 16px;
    font-weight: 700;
    color: #303133;
    text-align: center;
    margin: 0 0 8px 0;
}

.online-status {
    display: flex;
    align-items: center;
    justify-content: center;
    color: #67c23a;
    font-size: 12px;
    font-weight: 600;
}

.status-dot {
    width: 8px;
    height: 8px;
    background: #67c23a;
    border-radius: 50%;
    margin-right: 8px;
    animation: pulse 2s infinite;
    box-shadow: 0 0 8px rgba(103, 194, 58, 0.4);
}

/* 投资情绪花园 */
.investment-emotion-garden {
    background: linear-gradient(135deg, #fef9e7 0%, #fcf4e6 50%, #f6f0e8 100%);
    border-radius: 16px;
    padding: 16px;
    margin: 16px 20px;
    box-shadow: 0 8px 32px rgba(252, 244, 230, 0.8);
    border: 1px solid rgba(255, 255, 255, 0.2);
    position: relative;
    overflow: hidden;
    flex-shrink: 0;

    .garden-header {
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 16px;

        .garden-title {
            font-size: 14px;
            font-weight: 600;
            color: #8b4513;
            letter-spacing: 1px;
        }
    }

    .emotion-info {
        text-align: center;
        margin-bottom: 16px;

        .emotion-circle {
            width: 60px;
            height: 60px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 8px;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
            border: 2px solid rgba(255, 255, 255, 0.8);

            .emotion-icon {
                font-size: 28px;
            }
        }

        .emotion-name {
            font-size: 14px;
            font-weight: 600;
            color: #303133;
            margin-bottom: 8px;
        }

        .emotion-score {
            :deep(.el-progress__text) {
                font-size: 12px !important;
                color: #606266;
            }
        }
    }

    .warm-tips {
        .emotion-status-text {
            text-align: center;
            margin-bottom: 12px;

            .status-label {
                font-size: 12px;
                color: #909399;
                margin-right: 8px;
            }

            .status-emotion {
                font-size: 13px;
                font-weight: 600;
                padding: 2px 8px;
                border-radius: 12px;
                background: #e8f5e9;
                color: #4caf50;

                &.negative {
                    background: #ffebee;
                    color: #e53935;
                }
            }
        }

        .emotion-intensity {
            margin-bottom: 16px;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;

            .intensity-dots {
                display: flex;
                gap: 4px;

                .dot {
                    width: 8px;
                    height: 8px;
                    border-radius: 50%;
                    background: #e0e0e0;
                    transition: all 0.3s ease;

                    &.active {
                        background: linear-gradient(135deg, #ff9a9e, #fecfef);
                        transform: scale(1.2);
                        box-shadow: 0 2px 8px rgba(255, 154, 158, 0.4);
                    }
                }
            }

            .intensity-text {
                font-size: 11px;
                color: #8b7355;
                font-weight: 500;
            }
        }

        .ai-suggestion {
            background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.8));
            border-radius: 12px;
            padding: 12px;
            margin-bottom: 12px;
            display: flex;
            align-items: flex-start;
            gap: 8px;
            border: 1px solid rgba(255, 255, 255, 0.6);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);

            .suggestion-icon {
                font-size: 16px;
                flex-shrink: 0;
                margin-top: 2px;
            }

            .suggestion-content {
                flex: 1;

                .suggestion-title {
                    font-size: 11px;
                    font-weight: 600;
                    color: #8b7355;
                    margin-bottom: 4px;
                }

                .suggestion-text {
                    font-size: 11px;
                    color: #6b5b47;
                    line-height: 1.5;
                }
            }
        }

        .improvement-actions {
            margin-bottom: 12px;

            .actions-title {
                font-size: 11px;
                font-weight: 600;
                color: #8b7355;
                margin-bottom: 8px;
                display: flex;
                align-items: center;
                gap: 4px;
            }

            .actions-list {
                display: flex;
                flex-direction: column;
                gap: 6px;

                .action-item {
                    background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.7));
                    border-radius: 8px;
                    padding: 8px 10px;
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    border: 1px solid rgba(255, 255, 255, 0.5);
                    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

                    .action-icon {
                        font-size: 12px;
                        flex-shrink: 0;
                    }

                    .action-text {
                        font-size: 11px;
                        color: #6b5b47;
                        line-height: 1.4;
                        flex: 1;
                    }
                }
            }
        }

        .risk-notice {
            background: linear-gradient(135deg, #fff9e6, #ffeaa7);
            border-radius: 12px;
            padding: 12px;
            display: flex;
            align-items: flex-start;
            gap: 10px;
            border: 1px solid rgba(255, 234, 167, 0.6);
            box-shadow: 0 4px 16px rgba(255, 234, 167, 0.3);

            .notice-icon {
                font-size: 16px;
                flex-shrink: 0;
                margin-top: 2px;
            }

            .notice-content {
                flex: 1;

                .notice-title {
                    font-size: 11px;
                    font-weight: 600;
                    color: #b8740c;
                    margin-bottom: 4px;
                }

                .notice-text {
                    font-size: 11px;
                    color: #b8740c;
                    line-height: 1.5;
                }
            }
        }
    }
}

/* 会话列表 */
.session-history {
    background: white;
    border-radius: 12px;
    padding: 16px;
    flex: 1;
    display: flex;
    flex-direction: column;
    border: 1px solid #ebeef5;
    margin-top: 16px;
    overflow: hidden;
    flex-shrink: 1;
}

.section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    flex-shrink: 0;
}

.section-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin: 0;
}

.session-list {
    flex: 1;
    overflow-y: auto;
}

.session-item {
    position: relative;
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 12px;
    margin-bottom: 8px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s ease;
}

.session-item:hover {
    background: #f5f7fa;
}

.session-item.active {
    background: #ecf5ff;
    color: #409eff;
}

.session-info {
    flex: 1;
    min-width: 0;
}

.session-title span {
    font-weight: 500;
    font-size: 14px;
    color: #303133;
    display: block;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.session-meta {
    font-size: 12px;
    color: #909399;
    margin: 8px 0 6px;
}

.session-preview {
    font-size: 12px;
    color: #606266;
    margin-bottom: 6px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.empty-sessions {
    padding: 20px;
    display: flex;
    justify-content: center;
    align-items: center;
}

/* 聊天主区域 */
.chat-main {
    flex: 1;
    background: white;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.chat-header {
    background: white;
    color: #303133;
    padding: 15px 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-shrink: 0;
    border-bottom: 1px solid #e4e7ed;
}

.header-left {
    display: flex;
    align-items: center;
    gap: 16px;
}

.chat-avatar {
    width: 40px;
    height: 40px;
    background: #409eff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.chat-info h2 {
    font-size: 18px;
    font-weight: 600;
    margin: 0 0 4px 0;
}

.chat-info p {
    font-size: 14px;
    margin: 0;
    color: #909399;
}

/* 消息区域 */
.chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    background: #f5f7fa;
}

.message-item {
    display: flex;
    align-items: flex-start;
    gap: 12px;
}

.message-avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    background: #409eff;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.user-message .message-avatar {
    background: #909399;
    box-shadow: 0 4px 12px rgba(144, 147, 153, 0.2);
}

.message-content {
    max-width: 70%;
}

.message-bubble {
    background: white;
    border-radius: 8px;
    padding: 12px 16px;
    border: 1px solid #e4e7ed;
    box-shadow: 0 2px 12px 0px rgba(0,0,0,0.05);
}

.message-bubble p {
    margin: 0;
    line-height: 1.6;
}

.message-time {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
    padding-left: 4px;
}

/* 打字指示器 */
.typing-indicator {
    display: flex;
    gap: 4px;
    padding: 8px 0;
}

.typing-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #ccc;
    animation: typing 1.5s ease-in-out infinite;
}

.typing-dot:nth-child(2) {
    animation-delay: 0.2s;
}

.typing-dot:nth-child(3) {
    animation-delay: 0.4s;
}

/* 输入区域 */
.chat-input {
    border-top: 1px solid #e4e7ed;
    padding: 16px 24px;
    display: flex;
    gap: 12px;
    align-items: flex-end;
    background: white;
    flex-shrink: 0;
}

.input-container {
    flex: 1;
}

.input-container :deep(.el-textarea__inner) {
    border-radius: 8px;
    border: 1px solid #dcdfe6;
    padding: 12px;
    resize: none;
}

.input-container :deep(.el-textarea__inner):focus {
    border-color: #409eff;
    box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.1);
}

.send-btn {
    height: 80px;
    width: 48px;
    border-radius: 8px;
    background: #409eff !important;
    border: none !important;
    box-shadow: 0 6px 20px rgba(64, 158, 255, 0.25);
    transition: all 0.3s ease;
}

.send-btn:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(64, 158, 255, 0.35);
}

.send-btn:disabled {
    opacity: 0.5;
}

/* 动画 */
@keyframes breathing {
    0%, 100% {
        transform: scale(1);
        opacity: 0.8;
    }
    50% {
        transform: scale(1.1);
        opacity: 1;
    }
}

@keyframes pulse {
    0%, 100% {
        opacity: 1;
        box-shadow: 0 0 8px rgba(103, 194, 58, 0.4);
    }
    50% {
        opacity: 0.5;
        box-shadow: 0 0 4px rgba(103, 194, 58, 0.2);
    }
}

@keyframes typing {
    0%, 100% {
        transform: translateY(0);
    }
    50% {
        transform: translateY(-6px);
    }
}
</style>