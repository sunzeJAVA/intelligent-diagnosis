import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { showToast } from '@/utils/toast'

const TOKEN_KEY = 'id_token'

const controlApi = axios.create({
  baseURL: '/api/control',
  timeout: 10000
})

const dataApi = axios.create({
  baseURL: '/api/data',
  timeout: 30000
})

function addAuthInterceptor(instance: typeof controlApi) {
  instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })
}

addAuthInterceptor(controlApi)
addAuthInterceptor(dataApi)

/**
 * 统一处理限流/熔断导致的繁忙状态
 * 429 = RateLimiter, 503 = CircuitBreaker open / 服务不可用
 */
function handleServiceBusy(error: AxiosError): void {
  const status = error.response?.status
  if (status === 429 || status === 503) {
    showToast('服务繁忙，请稍后重试', 'warning')
  }
}

function addBusyInterceptor(instance: typeof controlApi) {
  instance.interceptors.response.use(
    (response) => response,
    (error) => {
      if (axios.isAxiosError(error)) {
        handleServiceBusy(error)
      }
      return Promise.reject(error)
    }
  )
}

addBusyInterceptor(controlApi)
addBusyInterceptor(dataApi)

export { controlApi, dataApi, TOKEN_KEY }
