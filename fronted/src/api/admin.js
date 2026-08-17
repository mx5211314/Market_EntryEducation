import request from '@/utils/request'

// 数据看板
export const getDashboardStats = () => request.get('/admin/dashboard/stats')

// 用户管理
export const getAdminUserList = (params) => request.get('/admin/user/list', { params })

export const updateUserRole = (userId, role) => request.put(`/admin/user/${userId}/role`, { role })

export const updateUserStatus = (userId, status) => request.put(`/admin/user/${userId}/status`, { status })

export const deleteUser = (userId) => request.delete(`/admin/user/${userId}`)

// 文章管理
export const getAdminArticleList = (params) => request.get('/admin/article/list', { params })

export const createArticle = (data) => request.post('/admin/article', data)

export const updateArticle = (id, data) => request.put(`/admin/article/${id}`, data)

export const updateArticleStatus = (id, status) => request.put(`/admin/article/${id}/status`, { status })

export const deleteArticle = (id) => request.delete(`/admin/article/${id}`)
