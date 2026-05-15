import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

let activeRequests = 0

const service = axios.create({
  baseURL: '',
  timeout: 30000
})

service.interceptors.request.use(
  (config) => {
    activeRequests++
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    activeRequests = Math.max(0, activeRequests - 1)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response) => {
    activeRequests = Math.max(0, activeRequests - 1)
    if (response.config?.responseType === 'blob') {
      return response.data
    }
    const res = response.data
    if (res.code && res.code !== 0 && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    activeRequests = Math.max(0, activeRequests - 1)
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      ElMessage.error('请求超时，请检查网络后重试')
    } else if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('userInfo')
        const currentPath = router.currentRoute.value?.fullPath
        const redirectParam = currentPath && currentPath !== '/login' ? `?redirect=${encodeURIComponent(currentPath)}` : ''
        router.push(`/login${redirectParam}`)
        ElMessage.error('登录已过期，请重新登录')
      } else if (status === 403) {
        ElMessage.error('没有权限执行此操作')
      } else if (status === 404) {
        ElMessage.error('请求的资源不存在')
      } else if (status === 429) {
        ElMessage.error('请求过于频繁，请稍后再试')
      } else if (status >= 500) {
        ElMessage.error('服务器错误，请稍后再试')
      } else {
        ElMessage.error(data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络连接失败，请检查网络')
    }
    return Promise.reject(error)
  }
)

export const getLoadingCount = () => activeRequests

export const get = (url, paramsOrConfig) => {
  if (paramsOrConfig && (paramsOrConfig.params !== undefined || paramsOrConfig.responseType !== undefined)) {
    return service.get(url, paramsOrConfig)
  }
  return service.get(url, { params: paramsOrConfig })
}
export const post = (url, data) => service.post(url, data)
export const put = (url, data) => service.put(url, data)
export const del = (url) => service.delete(url)

export default service
