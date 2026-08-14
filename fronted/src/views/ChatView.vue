<template>
  <section class="page-panel chat-page">
    <div class="page-intro">
      <div>
        <h2>智能法规问答</h2>
        <p>围绕证券交易规则、融资融券、投资者适当性等问题进行咨询，回答会保留风险提示和法规来源。</p>
      </div>
      <el-button text @click="clearChat" :disabled="loading || messages.length === 0">清空对话</el-button>
    </div>

    <div class="chat-window surface" ref="chatWindow">
      <div v-if="messages.length === 0" class="empty-state">
        <strong>可以直接提问</strong>
        <span>例如：融资融券交易中哪些情况会被强制平仓？</span>
      </div>

      <div v-for="(msg, idx) in messages" :key="idx" :class="['message', msg.role]">
        <div class="avatar">{{ msg.role === 'user' ? '我' : '答' }}</div>
        <div class="text">{{ msg.content }}</div>
      </div>

      <div v-if="loading" class="message assistant">
        <div class="avatar">答</div>
        <div class="text typing">正在检索法规并生成回答...</div>
      </div>
    </div>

    <div class="input-area surface">
      <el-input
        v-model="inputText"
        placeholder="请输入投资问题..."
        @keyup.enter="sendMessage"
        :disabled="loading"
        size="large">
        <template #append>
          <el-button @click="sendMessage" :loading="loading" type="primary">发送</el-button>
        </template>
      </el-input>
    </div>
  </section>
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
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text, sessionId: sessionId.value }),
    })
    if (!response.ok || !response.body) throw new Error(`HTTP ${response.status}`)
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
  height: calc(100vh - 144px);
}

.chat-window {
  flex: 1;
  min-height: 360px;
  overflow-y: auto;
  padding: 22px;
}

.empty-state {
  min-height: 220px;
  display: grid;
  place-content: center;
  text-align: center;
  color: #6f8294;
}

.empty-state strong {
  display: block;
  margin-bottom: 8px;
  color: #132a3a;
  font-size: 20px;
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
  border-radius: 8px;
  background: #e8eef4;
  color: #40576a;
  font-weight: 700;
  flex-shrink: 0;
}

.message.user .avatar {
  background: #0b4f82;
  color: white;
}

.text {
  max-width: min(760px, 78%);
  padding: 12px 15px;
  border-radius: 8px;
  background: #f4f7fa;
  color: #263847;
  line-height: 1.7;
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
  padding: 12px;
}

@media (max-width: 820px) {
  .chat-page {
    height: auto;
  }

  .chat-window {
    min-height: 420px;
  }

  .text {
    max-width: 82%;
  }
}
</style>
