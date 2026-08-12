<template>
  <div>
    <div class="chat-window" ref="chatWindow">
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        :class="['message', msg.role]">
        <div class="avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
        <div class="text" v-html="formatContent(msg.content)"></div>
      </div>
      <div v-if="loading" class="message assistant">
        <div class="avatar">🤖</div>
        <div class="text typing">正在思考...</div>
      </div>
    </div>
    <div class="input-area">
      <el-input
        v-model="inputText"
        placeholder="请输入投资问题..."
        @keyup.enter="sendMessage"
        :disabled="loading">
        <template #append>
          <el-button @click="sendMessage" :loading="loading" type="primary"
            >发送</el-button
          >
        </template>
      </el-input>
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
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text, sessionId: sessionId.value }),
    })
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let botMsg = { role: 'assistant', content: '' }
    messages.value.push(botMsg)
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      const chunk = decoder.decode(value)
      const lines = chunk.split('\n')
      for (let line of lines) {
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
  }
  loading.value = false
  scrollToBottom()
}

const formatContent = (content) => {
  if (!content) return ''
  return content
    .replace(/\n/g, '<br/>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
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
.chat-window {
  height: calc(100vh - 240px);
  overflow-y: auto;
  margin-bottom: 10px;
  padding: 15px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.message {
  display: flex;
  margin: 12px 0;
}
.message.user {
  flex-direction: row-reverse;
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #eee;
  text-align: center;
  line-height: 36px;
  font-size: 18px;
  margin: 0 10px;
  flex-shrink: 0;
}
.message.user .avatar {
  background: #1e3c72;
  color: white;
}
.text {
  background: #f0f2f5;
  padding: 10px 15px;
  border-radius: 12px;
  max-width: 70%;
  word-break: break-word;
  line-height: 1.5;
}
.message.user .text {
  background: #1e3c72;
  color: white;
}
.typing {
  color: #999;
  font-style: italic;
}
.input-area {
  margin-top: 10px;
}
</style>
