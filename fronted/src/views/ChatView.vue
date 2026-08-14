<template>
  <div class="chat-page">
    <div class="chat-layout">
      <!-- 对话主区 -->
      <div class="chat-main">
        <div class="chat-header">
          <div>
            <h2>💬 智能法规问答</h2>
            <p>
              围绕交易规则、融资融券、适当性等问题咨询，回答保留风险提示和法规来源。
            </p>
          </div>
          <button
            class="clear-btn"
            @click="clearChat"
            :disabled="loading || messages.length === 0">
            清空对话
          </button>
        </div>

        <div class="chat-window" ref="chatWindow">
          <div v-if="messages.length === 0" class="empty-state">
            <span class="empty-icon">🎯</span>
            <strong>可以直接提问</strong>
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
            <div class="text typing">正在检索法规并生成回答...</div>
          </div>
        </div>

        <div class="input-bar">
          <el-input
            v-model="inputText"
            placeholder="请输入投资问题..."
            @keyup.enter="sendMessage"
            :disabled="loading"
            size="large"
            class="chat-input">
            <template #append>
              <el-button @click="sendMessage" :loading="loading" type="primary"
                >发送</el-button
              >
            </template>
          </el-input>
        </div>
      </div>

      <!-- 右侧统计卡片 -->
      <div class="stats-side">
        <div class="stat-card">
          <div class="stat-number">128</div>
          <div class="stat-label">今日问答数</div>
        </div>
        <div class="stat-card">
          <div class="stat-number">86%</div>
          <div class="stat-label">测评完成率</div>
        </div>
        <div class="stat-card">
          <div class="stat-number">32</div>
          <div class="stat-label">模拟次数</div>
        </div>
        <div class="quick-links">
          <div class="quick-title">快捷入口</div>
          <div class="quick-item" @click="$router.push('/risk')">
            📊 风险测评
          </div>
          <div class="quick-item" @click="$router.push('/sim')">
            📈 模拟引导
          </div>
          <div class="quick-item" @click="$router.push('/report')">
            📋 风险报告
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const sessionId = ref('')
const inputText = ref('')
const loading = ref(false)
const messages = ref([])
const chatWindow = ref(null)

const initSession = () => {
  let sid = localStorage.getItem('sessionId')
  if (!sid) {
    sid = Date.now().toString(36) + Math.random().toString(36).slice(2, 8)
    localStorage.setItem('sessionId', sid)
  }
  sessionId.value = sid
}

onMounted(initSession)

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || text.length < 2) {
    ElMessage.warning('请输入完整问题')
    return
  }
  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const response = await fetch('/api/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ' + localStorage.getItem('token'),
      },
      body: JSON.stringify({ message: text, sessionId: sessionId.value }),
    })
    if (!response.ok || !response.body)
      throw new Error(`HTTP ${response.status}`)
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    const botMsg = { role: 'assistant', content: '' }
    messages.value.push(botMsg)
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      const chunk = decoder.decode(value, { stream: true })
      const lines = chunk.split('\n')
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const token = line.slice(5).trim()
          if (token && token !== '[DONE]') {
            botMsg.content += token
            scrollToBottom()
          }
        }
      }
    }
  } catch (e) {
    ElMessage.error('请求失败，请检查网络或后端服务')
    console.error(e)
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const clearChat = () => {
  messages.value = []
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatWindow.value) {
      chatWindow.value.scrollTop = chatWindow.value.scrollHeight
    }
  })
}
</script>

<style scoped>
.chat-page {
  height: 100%;
}
.chat-layout {
  display: flex;
  gap: 18px;
  height: 100%;
}
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--card-bg);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
}
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 22px;
  border-bottom: 1px solid var(--border-soft);
}
.chat-header h2 {
  font-size: 20px;
  color: var(--text-dark);
  margin-bottom: 4px;
}
.chat-header p {
  font-size: 13px;
  color: var(--text-muted);
}
.clear-btn {
  padding: 8px 16px;
  border: 1px solid var(--border-soft);
  border-radius: 8px;
  background: #fff;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.3s;
}
.clear-btn:hover:not(:disabled) {
  border-color: #ff8a9b;
  color: #ff6a88;
}
.clear-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.chat-window {
  flex: 1;
  overflow-y: auto;
  padding: 22px;
  background: #fefafb;
}
.empty-state {
  height: 100%;
  min-height: 250px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-muted);
}
.empty-icon {
  font-size: 40px;
}
.empty-state strong {
  font-size: 18px;
  color: var(--text-dark);
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
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #fce4e8;
  color: #ff6a88;
  font-weight: 700;
  flex-shrink: 0;
}
.message.user .avatar {
  background: #a4508b;
  color: white;
}
.text {
  max-width: min(600px, 75%);
  padding: 12px 16px;
  border-radius: 14px;
  background: #f4f7fa;
  color: var(--text-dark);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.message.user .text {
  background: linear-gradient(135deg, #ff9a8b, #ff6a88);
  color: white;
}
.typing {
  color: var(--text-muted);
  font-style: italic;
}

.input-bar {
  padding: 14px 18px;
  border-top: 1px solid var(--border-soft);
  background: #fff;
}
.chat-input :deep(.el-input__wrapper) {
  border-radius: 12px;
}

/* 右侧统计 */
.stats-side {
  width: 200px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex-shrink: 0;
}
.stat-card {
  background: var(--card-bg);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-soft);
  padding: 18px;
  text-align: center;
}
.stat-number {
  font-size: 26px;
  font-weight: 700;
  color: #ff6a88;
}
.stat-label {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.quick-links {
  background: var(--card-bg);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-soft);
  padding: 14px;
}
.quick-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-dark);
  margin-bottom: 8px;
}
.quick-item {
  padding: 8px 10px;
  border-radius: 8px;
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}
.quick-item:hover {
  background: #fce4e8;
  color: #ff6a88;
}
</style>
