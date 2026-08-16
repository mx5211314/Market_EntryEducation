import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 请求的前缀
  timeout: 60000, // 请求的超时时间（AI 分析接口耗时较长）
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 在发送请求之前做些什么
    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  (error) => {
    // 对请求错误做些什么
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    // 对响应数据做点什么
    const { data } = response
    // 如果是文件流或非JSON响应，直接返回
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return data
    }
    // 后端返回 { code: 200, data: {}, message: "success" } 格式
    if (data && typeof data === 'object' && 'code' in data) {
      if (data.code === 200) {
        return data.data
      } else {
        if (data.code === 401) {
          ElMessage.error('登录过期，请重新登录')
          sessionStorage.removeItem('token')
          sessionStorage.removeItem('username')
          sessionStorage.removeItem('nickname')
          sessionStorage.removeItem('role')
          sessionStorage.removeItem('userInfo')
          window.location.href = '/auth/login'
        } else {
          ElMessage.error(data.message || '请求失败')
        }
        return Promise.reject(new Error(data.message || '请求失败'))
      }
    }
    // 后端直接返回数据，没有code字段
    return data
  },
  (error) => {
    // 对响应错误做点什么
    const status = error.response?.status
    if (status === 401) {
      ElMessage.error('登录过期，请重新登录')
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('username')
      sessionStorage.removeItem('nickname')
      sessionStorage.removeItem('role')
      sessionStorage.removeItem('userInfo')
      window.location.href = '/auth/login'
    } else if (status === 403) {
      ElMessage.error('没有权限访问或登录已过期，请重新登录')
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('username')
      sessionStorage.removeItem('nickname')
      sessionStorage.removeItem('role')
      sessionStorage.removeItem('userInfo')
      window.location.href = '/auth/login'
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default service