import request from '@/utils/request'

// 用户相关接口
export const login = (data) => {
  return request.post('/auth/login', data)
}

export const register = (data) => {
  return request.post('/auth/register', data)
}

export const getCurrentUser = () => {
  return request.get('/auth/me')
}

// 聊天相关接口
export const startSession = (data) => {
  return request.post('/chat', data)
}

export const getSessionList = (params) => {
  return request.get('/chat', { params })
}

export const deleteSession = (sessionId) => {
  return request.delete(`/chat/sessions/${sessionId}`)
}

export const getSessionDetail = (sessionId) => {
  return request.get(`/chat/sessions/${sessionId}/messages`)
}

export const sendMessageStream = (data) => {
  return fetch('/api/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + sessionStorage.getItem('token'),
    },
    body: JSON.stringify(data),
  })
}

// 文章相关接口
export const getKnowledgeList = (params) => {
  return request.get('/user/article/list', { params })
}

export const getKnowledgeDetail = (articleId) => {
  return request.get(`/user/article/${articleId}`)
}