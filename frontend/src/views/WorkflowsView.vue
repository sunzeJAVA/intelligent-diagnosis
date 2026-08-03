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
          <div class="flex items-center gap-1.5">
            <span class="h-1.5 w-1.5 rounded-full bg-blue-500"></span>
            <p class="text-xs text-dark-500 dark:text-dark-400 font-medium">运行中</p>
          </div>
          <p class="text-2xl font-semibold text-dark-900 dark:text-white mt-2">{{ statusCount('RUNNING') }}</p>
        </div>
        <div class="card dark:card-dark p-4">
          <div class="flex items-center gap-1.5">
            <span class="h-1.5 w-1.5 rounded-full bg-green-500"></span>
            <p class="text-xs text-dark-500 dark:text-dark-400 font-medium">已完成</p>
          </div>
          <p class="text-2xl font-semibold text-dark-900 dark:text-white mt-2">{{ statusCount('COMPLETED') }}</p>
        </div>
        <div class="card dark:card-dark p-4">
          <div class="flex items-center gap-1.5">
            <span class="h-1.5 w-1.5 rounded-full bg-red-500"></span>
            <p class="text-xs text-dark-500 dark:text-dark-400 font-medium">失败</p>
          </div>
          <p class="text-2xl font-semibold text-dark-900 dark:text-white mt-2">{{ statusCount('FAILED') }}</p>
        </div>
        <div class="card dark:card-dark p-4">
          <div class="flex items-center gap-1.5">
            <span class="h-1.5 w-1.5 rounded-full bg-amber-500"></span>
            <p class="text-xs text-dark-500 dark:text-dark-400 font-medium">待处理</p>
          </div>
          <p class="text-2xl font-semibold text-dark-900 dark:text-white mt-2">{{ statusCount('PENDING') + statusCount('PAUSED') }}</p>
        </div>
        <div class="card dark:card-dark p-4">
          <div class="flex items-center gap-1.5">
            <span class="h-1.5 w-1.5 rounded-full bg-dark-400 dark:bg-dark-500"></span>
            <p class="text-xs text-dark-500 dark:text-dark-400 font-medium">总数</p>
          </div>
          <p class="text-2xl font-semibold text-dark-900 dark:text-white mt-2">{{ workflows.length }}</p>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading && workflows.length === 0" class="flex items-center justify-center py-20">
        <Loader2 class="h-6 w-6 animate-spin text-primary-500" />
      </div>

      <!-- Empty State -->
      <div v-else-if="workflows.length === 0" class="card dark:card-dark p-8 text-center">
        <Workflow class="h-8 w-8 text-dark-300 dark:text-dark-600 mx-auto mb-3" />
        <h3 class="text-base font-semibold text-dark-700 dark:text-white mb-1">暂无工作流记录</h3>
        <p class="text-dark-400 dark:text-dark-500 text-sm">当仓库同步或代码索引时，工作流将在此显示</p>
      </div>

      <!-- Workflow Table -->
      <div v-else class="card dark:card-dark overflow-hidden">
        <div class="overflow-x-auto scrollbar-thin">
          <table class="w-full">
            <thead>
              <tr class="border-b border-dark-200 dark:border-dark-700 bg-dark-50 dark:bg-dark-800">
                <th class="px-4 py-3 text-left text-xs font-medium text-dark-500 dark:text-dark-400">工作流</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-dark-500 dark:text-dark-400">类型</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-dark-500 dark:text-dark-400">状态</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-dark-500 dark:text-dark-400">当前步骤</th>
                <th class="px-4 py-3 text-left text-xs font-medium text-dark-500 dark:text-dark-400">开始时间</th>
                <th class="px-4 py-3 text-right text-xs font-medium text-dark-500 dark:text-dark-400">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-dark-100 dark:divide-dark-700">
              <tr v-for="wf in workflows" :key="wf.workflowId" class="hover:bg-dark-50 dark:hover:bg-dark-800/50 transition-colors">
                <td class="px-4 py-3">
                  <span class="text-sm font-mono text-dark-800 dark:text-white">{{ wf.workflowId.substring(0, 8) }}</span>
                </td>
                <td class="px-4 py-3">
                  <span class="badge badge-info dark:badge-info-dark">{{ wf.workflowType }}</span>
                </td>
                <td class="px-4 py-3">
                  <span class="flex items-center gap-1.5">
                    <span class="h-1.5 w-1.5 rounded-full" :class="statusConfig[wf.status].dot"></span>
                    <span class="text-sm font-medium" :class="statusConfig[wf.status].text">{{ wf.status }}</span>
                  </span>
                </td>
                <td class="px-4 py-3">
                  <span class="text-sm text-dark-600 dark:text-dark-300">{{ wf.currentStep || '-' }}</span>
                </td>
                <td class="px-4 py-3 text-sm text-dark-500 dark:text-dark-400">{{ formatTime(wf.startedAt) }}</td>
                <td class="px-4 py-3 text-right">
                  <div class="flex items-center justify-end gap-1">
                    <button
                      v-if="wf.status === 'PAUSED'"
                      @click="handleResume(wf.workflowId)"
                      class="rounded-md p-1 text-dark-400 dark:text-dark-500 hover:bg-green-50 dark:hover:bg-green-600/10 hover:text-green-600 dark:hover:text-green-400 transition-colors"
                      title="恢复"
                    >
                      <Play class="h-4 w-4" />
                    </button>
                    <button
                      v-if="wf.status === 'RUNNING'"
                      @click="handlePause(wf.workflowId)"
                      class="rounded-md p-1 text-dark-400 dark:text-dark-500 hover:bg-amber-50 dark:hover:bg-amber-600/10 hover:text-amber-600 dark:hover:text-amber-400 transition-colors"
                      title="暂停"
                    >
                      <Pause class="h-4 w-4" />
                    </button>
                    <button
                      v-if="wf.status === 'RUNNING' || wf.status === 'PAUSED'"
                      @click="handleRollback(wf.workflowId)"
                      class="rounded-md p-1 text-dark-400 dark:text-dark-500 hover:bg-red-50 dark:hover:bg-red-600/10 hover:text-red-600 dark:hover:text-red-400 transition-colors"
                      title="回滚"
                    >
                      <RotateCcw class="h-4 w-4" />
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
import {
  Loader2, RefreshCw, Play, Pause, RotateCcw, Workflow
} from 'lucide-vue-next'
import type { WorkflowDto } from '@/api/workflow'
import { listWorkflows, pauseWorkflow, resumeWorkflow, rollbackWorkflow } from '@/api/workflow'

const workflows = ref<WorkflowDto[]>([])
const loading = ref(false)
let pollTimer: ReturnType<typeof setInterval>

const statusConfig: Record<string, { dot: string; text: string }> = {
  RUNNING: { dot: 'bg-blue-500 animate-pulse', text: 'text-blue-600' },
  COMPLETED: { dot: 'bg-green-500', text: 'text-green-600' },
  FAILED: { dot: 'bg-red-500', text: 'text-red-600' },
  PAUSED: { dot: 'bg-amber-500', text: 'text-amber-600' },
  PENDING: { dot: 'bg-amber-500', text: 'text-amber-600' },
  CANCELLED: { dot: 'bg-dark-400', text: 'text-dark-500' },
}

function statusCount(status: string): number {
  return workflows.value.filter(w => w.status === status).length
}

function formatTime(time?: string | null): string {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

async function loadWorkflows() {
  loading.value = true
  try {
    workflows.value = await listWorkflows()
  } catch {
    workflows.value = []
  } finally {
    loading.value = false
  }
}

async function handlePause(workflowId: string) {
  await pauseWorkflow(workflowId)
  await loadWorkflows()
}

async function handleResume(workflowId: string) {
  await resumeWorkflow(workflowId)
  await loadWorkflows()
}

async function handleRollback(workflowId: string) {
  if (!confirm('确定回滚此工作流吗？这将撤销已完成的变更。')) return
  await rollbackWorkflow(workflowId)
  await loadWorkflows()
}

onMounted(() => {
  loadWorkflows()
  pollTimer = setInterval(loadWorkflows, 10000)
})

onUnmounted(() => clearInterval(pollTimer))
</script>
