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
                    <div v-for="session in sessions" :key="session.id" @click="switchSession(session)" class="session-item">
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
                <!-- 欢迎用语 -->
                <div class="message-item ai-message" v-if="messages.length === 0">
                    <div class="message-avatar">
                        <el-icon size="16"><Connection /></el-icon>
                    </div>
                    <div class="message-content">
                        <div class="message-bubble">
                            <p>您好！我是您的入市教育智慧助手，很高兴为您提供金融法规相关的服务。请告诉我，您想了解什么内容？</p>
                        </div>
                        <div class="message-time">刚刚</div>
                    </div>
                </div>
                <!-- 消息列表 -->
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
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Connection, Plus, Promotion, User } from '@element-plus/icons-vue'
import { startSession, getSessionList, deleteSession, sendMessageStream } from '@/api/frontend'

const router = useRouter()
const sessions = ref([])
const currentSession = ref(null)
const messages = ref([])
const inputText = ref('')
const isAiTyping = ref(false)
const messagesContainer = ref(null)
const typingMessageId = ref(null)

const loadSessions = async () => {
    try {
        const res = await getSessionList()
        sessions.value = res || []
        if (sessions.value.length > 0 && !currentSession.value) {
            switchSession(sessions.value[0])
        }
    } catch (error) {
        console.error('加载会话失败:', error)
    }
}

const createNewSession = async () => {
    try {
        const res = await startSession({ title: '新对话' })
        currentSession.value = res
        sessions.value.unshift(res)
        messages.value = []
        await nextTick()
        scrollToBottom()
    } catch (error) {
        console.error('创建会话失败:', error)
        ElMessage.error('创建会话失败')
    }
}

const switchSession = async (session) => {
    currentSession.value = session
    try {
        // 加载会话消息
        messages.value = [
            {
                id: Date.now(),
                role: 'assistant',
                content: '您好！我是您的入市教育智慧助手，很高兴为您服务。请问有什么可以帮助您的？',
                createdAt: new Date()
            }
        ]
        await nextTick()
        scrollToBottom()
    } catch (error) {
        console.error('加载消息失败:', error)
    }
}

const sendMessage = async () => {
    if (!inputText.value.trim() || isAiTyping.value) return
    if (!currentSession.value) {
        await createNewSession()
    }

    const userMessage = inputText.value.trim()
    inputText.value = ''

    const newUserMessage = {
        id: Date.now(),
        role: 'user',
        content: userMessage,
        createdAt: new Date()
    }
    messages.value.push(newUserMessage)
    await nextTick()
    scrollToBottom()

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
            sessionId: currentSession.value.id,
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
                    console.error('解析响应失败:', e)
                }
            }
        }

        isAiTyping.value = false
        typingMessageId.value = null
        await loadSessions()
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