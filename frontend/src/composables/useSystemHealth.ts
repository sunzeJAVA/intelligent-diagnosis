import { ref, computed, onMounted, onUnmounted } from 'vue'
import { controlApi } from '@/api/client'

export interface SystemHealthItem {
  name: string
  type: string
  url: string
  connected: boolean
  latency: number
}

const services = ref<SystemHealthItem[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
let intervalId: number | null = null

/**
 * 系统健康状态管理组合式函数
 * 定时轮询 /api/control/system/health，供侧边栏展示基础设施状态
 */
export function useSystemHealth(options: { pollInterval?: number } = {}) {
  const { pollInterval = 10000 } = options

  const overallStatus = computed(() => {
    if (services.value.length === 0) return 'unknown'
    return services.value.every((s) => s.connected) ? 'running' : 'degraded'
  })

  async function refresh() {
    loading.value = true
    try {
      const response = await controlApi.get<SystemHealthItem[]>('/system/health')
      services.value = response.data
      error.value = null
    } catch (err) {
      error.value = '获取系统状态失败'
    } finally {
      loading.value = false
    }
  }

  function startPolling() {
    stopPolling()
    refresh()
    intervalId = window.setInterval(refresh, pollInterval)
  }

  function stopPolling() {
    if (intervalId !== null) {
      window.clearInterval(intervalId)
      intervalId = null
    }
  }

  onMounted(startPolling)
  onUnmounted(stopPolling)

  return {
    services,
    loading,
    error,
    overallStatus,
    refresh
  }
}
