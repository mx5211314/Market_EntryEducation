import request from '@/utils/request'

// 管理员用户相关接口
export const adminLogin = (data) => {
  return request.post('/admin/login', data)
}

export const getUserList = (params) => {
  return request.get('/admin/users', { params })
}

export const deleteUser = (userId) => {
  return request.delete(`/admin/users/${userId}`)
}

// 文章管理接口
export const getArticleList = (params) => {
  return request.get('/admin/articles', { params })
}

export const deleteArticle = (articleId) => {
  return request.delete(`/admin/articles/${articleId}`)
}

export const updateArticle = (articleId, data) => {
  return request.put(`/admin/articles/${articleId}`, data)
}