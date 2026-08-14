<template>
  <div class="chat-page">
    <!-- 左侧会话列表 -->
    <div class="session-panel">
      <div class="session-header">
        <el-button type="primary" size="small" @click="createNewSession"
          >＋ 新建会话</el-button
        >
      </div>
      <div class="session-list">
        <div
          v-for="session in sessions"
          :key="session.sessionId"
          :class="[
            'session-item',
            { active: session.sessionId === currentSessionId },
          ]"
          @click="switchSession(session.sessionId)">
          <div class="session-title">{{ session.title || '新对话' }}</div>
          <div class="session-time">{{ formatTime(session.updatedAt) }}</div>
          <el-button
            type="danger"
            text
            size="small"
            class="delete-btn"
            @click.stop="deleteSession(session.sessionId)"
            >删除</el-button
          >
        </div>
        <div v-if="sessions.length === 0" class="empty-sessions">暂无会话</div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-area">
      <div class="chat-window" ref="chatWindow">
        <div v-if="messages.length === 0" class="empty-state">
          <strong>开始新的对话</strong>
          <span>例如：融资融券交易中哪些情况会被强制平仓？</span>
        </div>

        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          :class="['message', msg.role]">
          <div class="avatar">{{ msg.role === 'user' ? '我' : '答' }}</div>
          <div class="text">{{ msg.content }}</div>
        </div>

        <div v-if="loading" class="message assistant">
          <div class="avatar">答</div>
          <div class="text typing">正在生成回答...</div>
        </div>
      </div>

      <div class="input-area">
        <el-input
          v-model="inputText"
          placeholder="请输入投资问题..."
          @keyup.enter="sendMessage"
          :disabled="loading"
          size="large">
          <template #append>
            <el-button @click="sendMessage" :loading="loading" type="primary"
              >发送</el-button
            >
          </template>
        </el-input>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const sessions = ref([])
const currentSessionId = ref('')
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const chatWindow = ref(null)
let requestId = 0

// 加载会话列表
const loadSessions = async () => {
  try {
    const res = await axios.get('/api/session/list')
    sessions.value = res.data
    if (sessions.value.length > 0) {
      currentSessionId.value = sessions.value[0].sessionId
      await loadMessages(currentSessionId.value)
    }
  } catch (e) {
    ElMessage.error('获取会话列表失败')
    console.error(e)
  }
}

// 新建会话
const createNewSession = async () => {
  requestId++ // 使旧请求失效
  loading.value = false
  try {
    const res = await axios.post('/api/session/create', { title: '新对话' })
    currentSessionId.value = res.data.sessionId
    messages.value = []
    inputText.value = ''
    await loadSessions()
  } catch (e) {
    ElMessage.error('新建会话失败')
    console.error(e)
  }
}

// 切换会话
const switchSession = async (sessionId) => {
  if (sessionId === currentSessionId.value) return
  requestId++ // 使旧请求失效
  loading.value = false
  currentSessionId.value = sessionId
  messages.value = []
  await loadMessages(sessionId)
}

// 加载指定会话的消息
const loadMessages = async (sessionId) => {
  try {
    const res = await axios.get(`/api/session/${sessionId}/messages`)
    messages.value = res.data.map((item) => ({
      role: item.role,
      content: item.content,
    }))
    scrollToBottom()
  } catch (e) {
    ElMessage.error('加载消息失败')
    console.error(e)
  }
}

// 删除会话
const deleteSession = async (sessionId) => {
  try {
    await axios.delete(`/api/session/${sessionId}`)
    ElMessage.success('删除成功')
    if (sessionId === currentSessionId.value) {
      requestId++
      loading.value = false
      currentSessionId.value = ''
      messages.value = []
    }
    await loadSessions()
  } catch (e) {
    ElMessage.error('删除失败')
    console.error(e)
  }
}

// 发送消息（流式）
const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  if (!currentSessionId.value) {
    try {
      const res = await axios.post('/api/session/create', {
        title: text.substring(0, 20),
      })
      currentSessionId.value = res.data.sessionId
      await loadSessions()
    } catch (e) {
      ElMessage.error('创建会话失败')
      return
    }
  }

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  const currentRequestId = ++requestId
  const token = localStorage.getItem('token')

  try {
    const response = await fetch('/api/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ' + token,
      },
      body: JSON.stringify({
        message: text,
        sessionId: currentSessionId.value,
      }),
    })

    if (!response.ok || !response.body)
      throw new Error(`HTTP ${response.status}`)

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    const botMsg = { role: 'assistant', content: '' }
    messages.value.push(botMsg)
    const botIndex = messages.value.length - 1

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      if (currentRequestId !== requestId) {
        reader.cancel()
        return
      }

      const chunk = decoder.decode(value, { stream: true })
      const lines = chunk.split('\n')
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (data && data !== '[DONE]') {
            botMsg.content += data
            if (messages.value[botIndex]) {
              messages.value[botIndex].content = botMsg.content
            }
            scrollToBottom()
          }
        }
      }
    }
  } catch (e) {
    if (currentRequestId === requestId) {
      ElMessage.error('请求失败，请检查网络或后端服务')
      console.error(e)
      messages.value = messages.value.filter(
        (m) => m.role !== 'assistant' || m.content !== '',
      )
    }
  } finally {
    if (currentRequestId === requestId) {
      loading.value = false
      scrollToBottom()
    }
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatWindow.value) {
      chatWindow.value.scrollTop = chatWindow.value.scrollHeight
    }
  })
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${date.getMinutes()}`
}

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.chat-page {
  display: flex;
  height: 100%;
  background: #f5f7fa;
}

/* 左侧会话列表 */
.session-panel {
  width: 240px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.session-header {
  padding: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  position: relative;
  transition: background 0.2s;
}

.session-item:hover {
  background: #f0f2f5;
}

.session-item.active {
  background: #e6f0fa;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 30px;
}

.session-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.delete-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
}

.empty-sessions {
  text-align: center;
  color: #999;
  margin-top: 40px;
}

/* 右侧聊天区域 */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-window {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fff;
  margin: 0 10px 10px 10px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.empty-state strong {
  font-size: 20px;
  margin-bottom: 8px;
  color: #555;
}

.message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 14px 0;
}

.message.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #e8eef4;
  color: #40576a;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.message.user .avatar {
  background: #0b4f82;
  color: white;
}

.text {
  max-width: 70%;
  padding: 12px 15px;
  border-radius: 8px;
  background: #f4f7fa;
  color: #263847;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message.user .text {
  background: #0b4f82;
  color: white;
}

.typing {
  color: #738699;
}

.input-area {
  padding: 0 10px 10px 10px;
  background: transparent;
}
</style>
