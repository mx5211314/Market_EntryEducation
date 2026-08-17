import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建公开API实例（无需登录）
const publicService = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// 响应拦截器
publicService.interceptors.response.use(
  (response) => {
    const { data } = response
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return data
    }
    // 处理后端返回格式
    if (data && typeof data === 'object' && 'code' in data) {
      if (data.code === 200) {
        return data.data
      } else if (data.code === 401 || data.code === 403) {
        ElMessage.error('请先登录')
        return Promise.reject(new Error(data.message || '需要登录'))
      } else {
        ElMessage.error(data.message || '请求失败')
        return Promise.reject(new Error(data.message || '请求失败'))
      }
    }
    return data
  },
  (error) => {
    const status = error.response?.status
    if (status === 401 || status === 403) {
      ElMessage.error('此内容需要登录后查看')
    } else {
      // 后端全局异常处理带的中文 message 比 axios 自带的英文提示有用
      ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default publicService