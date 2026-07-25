<template>
  <div class="w-full p-4 sm:p-6 lg:p-8 space-y-6">
      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-dark-900 dark:text-white">工作流监控</h1>
          <p class="text-dark-500 dark:text-dark-400 text-sm mt-1">实时查看工作流执行状态和 Temporal 任务</p>
        </div>
        <button @click="loadWorkflows" class="btn-secondary dark:btn-secondary-dark btn-sm">
          <RefreshCw class="h-4 w-4" :class="loading ? 'animate-spin' : ''" />
          刷新
        </button>
      </div>

      <!-- Stats -->
      <div class="grid grid-cols-2 lg:grid-cols-5 gap-4">
        <div class="card dark:card-dark p-4">
          <div class="flex items-center justify-between mb-1">
            <p class="text-xs text-dark-500 dark:text-dark-400 font-medium">运行中</p>
            <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-50 dark:bg-blue-600/20">
              <Play class="h-4 w-4 text-blue-500" />
            </div>
          </div>
          <p class="text-2xl font-bold text-blue-600">{{ statusCount('RUNNING') }}</p>
        </div>
        <div class="card dark:card-dark p-4">
          <div class="flex items-center justify-between mb-1">
            <p class="text-xs text-dark-500 dark:text-dark-400 font-medium">已完成</p>
            <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-green-50 dark:bg-green-600/20">
              <CheckCircle2 class="h-4 w-4 text-green-500" />
            </div>
          </div>
          <p class="text-2xl font-bold text-green-600">{{ statusCount('COMPLETED') }}</p>
        </div>
        <div class="card dark:card-dark p-4">
          <div class="flex items-center justify-between mb-1">
            <p class="text-xs text-dark-500 dark:text-dark-400 font-medium">失败</p>
            <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-red-50 dark:bg-red-600/20">
              <XCircle class="h-4 w-4 text-red-500" />
            </div>
          </div>
          <p class="text-2xl font-bold text-red-600">{{ statusCount('FAILED') }}</p>
        </div>
        <div class="card dark:card-dark p-4">
          <div class="flex items-center justify-between mb-1">
            <p class="text-xs text-dark-500 dark:text-dark-400 font-medium">待处理</p>
            <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-50 dark:bg-amber-600/20">
              <Clock class="h-4 w-4 text-amber-500" />
            </div>
          </div>
          <p class="text-2xl font-bold text-amber-600">{{ statusCount('PENDING') + statusCount('PAUSED') }}</p>
        </div>
        <div class="card dark:card-dark p-4">
          <div class="flex items-center justify-between mb-1">
            <p class="text-xs text-dark-500 dark:text-dark-400 font-medium">总数</p>
            <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-dark-100 dark:bg-dark-800">
              <Layers class="h-4 w-4 text-dark-500 dark:text-dark-400" />
            </div>
          </div>
          <p class="text-2xl font-bold text-dark-800 dark:text-white">{{ workflows.length }}</p>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading && workflows.length === 0" class="flex items-center justify-center py-20">
        <Loader2 class="h-8 w-8 animate-spin text-primary-500" />
      </div>

      <!-- Empty State -->
      <div v-else-if="workflows.length === 0" class="card dark:card-dark p-12 text-center">
        <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-dark-100 dark:bg-dark-800 mb-4">
          <Workflow class="h-8 w-8 text-dark-400" />
        </div>
        <h3 class="text-lg font-semibold text-dark-700 dark:text-white mb-1">暂无工作流记录</h3>
        <p class="text-dark-400 dark:text-dark-500 text-sm">当仓库同步或代码索引时，工作流将在此显示</p>
      </div>

      <!-- Workflow Table -->
      <div v-else class="card dark:card-dark overflow-hidden">
        <div class="overflow-x-auto scrollbar-thin">
          <table class="w-full">
            <thead>
              <tr class="border-b border-dark-200 dark:border-dark-700 bg-dark-50 dark:bg-dark-800">
                <th class="px-4 py-3 text-left text-xs font-semibold text-dark-500 dark:text-dark-400 uppercase tracking-wider">工作流</th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-dark-500 dark:text-dark-400 uppercase tracking-wider">类型</th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-dark-500 dark:text-dark-400 uppercase tracking-wider">状态</th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-dark-500 dark:text-dark-400 uppercase tracking-wider">进度</th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-dark-500 dark:text-dark-400 uppercase tracking-wider">开始时间</th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-dark-500 dark:text-dark-400 uppercase tracking-wider">耗时</th>
                <th class="px-4 py-3 text-right text-xs font-semibold text-dark-500 dark:text-dark-400 uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-dark-100 dark:divide-dark-700">
              <tr v-for="wf in workflows" :key="wf.id" class="hover:bg-dark-50 dark:hover:bg-dark-800/50 transition-colors">
                <td class="px-4 py-3">
                  <div class="flex items-center gap-2">
                    <div
                      class="flex h-8 w-8 items-center justify-center rounded-lg"
                      :class="typeConfig[wf.type]?.bg || 'bg-dark-100 dark:bg-dark-800'"
                    >
                      <component :is="typeConfig[wf.type]?.icon || Workflow" class="h-4 w-4" :class="typeConfig[wf.type]?.text || 'text-dark-500'" />
                    </div>
                    <div>
                      <p class="text-sm font-medium text-dark-800 dark:text-white">{{ wf.name || wf.id.substring(0, 8) }}</p>
                      <p class="text-xs text-dark-400 dark:text-dark-500 font-mono">{{ wf.id.substring(0, 18) }}</p>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-3">
                  <span class="badge badge-info dark:badge-info-dark">{{ wf.type }}</span>
                </td>
                <td class="px-4 py-3">
                  <span class="flex items-center gap-1.5">
                    <span class="h-1.5 w-1.5 rounded-full" :class="statusConfig[wf.status].dot"></span>
                    <span class="text-sm font-medium" :class="statusConfig[wf.status].text">{{ wf.status }}</span>
                  </span>
                </td>
                <td class="px-4 py-3">
                  <div class="flex items-center gap-2">
                    <div class="h-1.5 w-20 rounded-full bg-dark-100 dark:bg-dark-700 overflow-hidden">
                      <div
                        class="h-full rounded-full transition-all duration-500"
                        :class="statusConfig[wf.status].bar"
                        :style="{ width: getProgress(wf) + '%' }"
                      ></div>
                    </div>
                    <span class="text-xs text-dark-500 dark:text-dark-400">{{ getProgress(wf) }}%</span>
                  </div>
                </td>
                <td class="px-4 py-3 text-sm text-dark-500 dark:text-dark-400">{{ formatTime(wf.startedAt) }}</td>
                <td class="px-4 py-3 text-sm text-dark-500 dark:text-dark-400 font-mono">{{ formatDuration(wf) }}</td>
                <td class="px-4 py-3 text-right">
                  <div class="flex items-center justify-end gap-1">
                    <button
                      v-if="wf.status === 'PAUSED'"
                      @click="resumeWorkflow(wf.id)"
                      class="rounded-lg p-1.5 text-dark-400 dark:text-dark-500 hover:bg-green-50 dark:hover:bg-green-600/10 hover:text-green-600 dark:hover:text-green-400 transition-colors"
                      title="恢复"
                    >
                      <Play class="h-4 w-4" />
                    </button>
                    <button
                      v-if="wf.status === 'RUNNING'"
                      @click="pauseWorkflow(wf.id)"
                      class="rounded-lg p-1.5 text-dark-400 dark:text-dark-500 hover:bg-amber-50 dark:hover:bg-amber-600/10 hover:text-amber-600 dark:hover:text-amber-400 transition-colors"
                      title="暂停"
                    >
                      <Pause class="h-4 w-4" />
                    </button>
                    <button
                      v-if="wf.status === 'RUNNING' || wf.status === 'PAUSED'"
                      @click="cancelWorkflow(wf.id)"
                      class="rounded-lg p-1.5 text-dark-400 dark:text-dark-500 hover:bg-red-50 dark:hover:bg-red-600/10 hover:text-red-600 dark:hover:text-red-400 transition-colors"
                      title="取消"
                    >
                      <Square class="h-4 w-4" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { controlApi } from '@/api/client'
import {
  Loader2, RefreshCw, Play, Pause, Square, CheckCircle2, XCircle,
  Clock, Layers, Workflow, GitBranch, Search, FileCode
} from 'lucide-vue-next'

interface WorkflowInstance {
  id: string
  name?: string
  type: string
  status: string
  startedAt?: string
  completedAt?: string
  currentStep?: string
  totalSteps?: number
  completedSteps?: number
}

const workflows = ref<WorkflowInstance[]>([])
const loading = ref(false)
let pollTimer: ReturnType<typeof setInterval>

const typeConfig: Record<string, { bg: string; text: string; icon: any }> = {
  REPOSITORY_SYNC: { bg: 'bg-primary-50', text: 'text-primary-600', icon: GitBranch },
  CODE_INDEX: { bg: 'bg-blue-50', text: 'text-blue-600', icon: Search },
  DIAGNOSIS: { bg: 'bg-purple-50', text: 'text-purple-600', icon: FileCode },
}

const statusConfig: Record<string, { dot: string; text: string; bar: string }> = {
  RUNNING: { dot: 'bg-blue-500 animate-pulse', text: 'text-blue-600', bar: 'bg-blue-500' },
  COMPLETED: { dot: 'bg-green-500', text: 'text-green-600', bar: 'bg-green-500' },
  FAILED: { dot: 'bg-red-500', text: 'text-red-600', bar: 'bg-red-500' },
  PAUSED: { dot: 'bg-amber-500', text: 'text-amber-600', bar: 'bg-amber-500' },
  PENDING: { dot: 'bg-amber-500', text: 'text-amber-600', bar: 'bg-amber-500' },
  CANCELLED: { dot: 'bg-dark-400', text: 'text-dark-500', bar: 'bg-dark-400' },
}

/**
 * 统计指定状态的工作流数量
 * @param status - 工作流状态（RUNNING/COMPLETED/FAILED/PAUSED/PENDING/CANCELLED）
 * @returns 该状态的工作流数量
 */
function statusCount(status: string): number {
  return workflows.value.filter(w => w.status === status).length
}

/**
 * 计算工作流进度百分比
 * @param wf - 工作流实例
 * @returns 进度百分比（0-100）
 */
function getProgress(wf: WorkflowInstance): number {
  if (wf.status === 'COMPLETED') return 100
  if (wf.status === 'FAILED' || wf.status === 'CANCELLED') return 0
  if (wf.totalSteps && wf.completedSteps !== undefined) {
    return Math.round((wf.completedSteps / wf.totalSteps) * 100)
  }
  return wf.status === 'RUNNING' ? 50 : 0
}

/**
 * 格式化时间戳为可读字符串
 * @param time - 时间字符串（ISO 格式）
 * @returns 格式化后的时间字符串，无时间时返回 '-'
 */
function formatTime(time?: string): string {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

/**
 * 计算并格式化工作流执行时长
 * @param wf - 工作流实例
 * @returns 格式化的时长字符串（如 "2m 30s"），无开始时间时返回 '-'
 */
function formatDuration(wf: WorkflowInstance): string {
  if (!wf.startedAt) return '-'
  const end = wf.completedAt ? new Date(wf.completedAt).getTime() : Date.now()
  const ms = end - new Date(wf.startedAt).getTime()
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${Math.floor(ms / 60000)}m ${Math.floor((ms % 60000) / 1000)}s`
}

/**
 * 加载工作流列表数据
 * 通过 API 获取工作流实例列表，失败时返回空数组
 */
async function loadWorkflows() {
  loading.value = true
  try {
    const response = await controlApi.get<WorkflowInstance[]>('/workflows')
    workflows.value = response.data
  } catch {
    workflows.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 暂停指定工作流
 * @param id - 工作流 ID
 */
async function pauseWorkflow(id: string) {
  await controlApi.post(`/workflows/${id}/pause`)
  await loadWorkflows()
}

/**
 * 恢复指定工作流
 * @param id - 工作流 ID
 */
async function resumeWorkflow(id: string) {
  await controlApi.post(`/workflows/${id}/resume`)
  await loadWorkflows()
}

/**
 * 取消指定工作流（需用户确认）
 * @param id - 工作流 ID
 */
async function cancelWorkflow(id: string) {
  if (!confirm('确定取消此工作流吗？')) return
  await controlApi.post(`/workflows/${id}/cancel`)
  await loadWorkflows()
}

onMounted(() => {
  loadWorkflows()
  pollTimer = setInterval(loadWorkflows, 10000)
})

onUnmounted(() => clearInterval(pollTimer))
</script>
