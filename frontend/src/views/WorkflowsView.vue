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
                <th class="px-4 py-3 text-left text-xs font-semibold text-dark-500 dark:text-dark-400 uppercase tracking-wider">当前步骤</th>
                <th class="px-4 py-3 text-left text-xs font-semibold text-dark-500 dark:text-dark-400 uppercase tracking-wider">开始时间</th>
                <th class="px-4 py-3 text-right text-xs font-semibold text-dark-500 dark:text-dark-400 uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-dark-100 dark:divide-dark-700">
              <tr v-for="wf in workflows" :key="wf.workflowId" class="hover:bg-dark-50 dark:hover:bg-dark-800/50 transition-colors">
                <td class="px-4 py-3">
                  <div class="flex items-center gap-2">
                    <div
                      class="flex h-8 w-8 items-center justify-center rounded-lg"
                      :class="typeConfig[wf.workflowType]?.bg || 'bg-dark-100 dark:bg-dark-800'"
                    >
                      <component :is="typeConfig[wf.workflowType]?.icon || Workflow" class="h-4 w-4" :class="typeConfig[wf.workflowType]?.text || 'text-dark-500'" />
                    </div>
                    <div>
                      <p class="text-sm font-medium text-dark-800 dark:text-white">{{ wf.workflowId.substring(0, 8) }}</p>
                      <p class="text-xs text-dark-400 dark:text-dark-500 font-mono">{{ wf.workflowId.substring(0, 18) }}...</p>
                    </div>
                  </div>
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
                      class="rounded-lg p-1.5 text-dark-400 dark:text-dark-500 hover:bg-green-50 dark:hover:bg-green-600/10 hover:text-green-600 dark:hover:text-green-400 transition-colors"
                      title="恢复"
                    >
                      <Play class="h-4 w-4" />
                    </button>
                    <button
                      v-if="wf.status === 'RUNNING'"
                      @click="handlePause(wf.workflowId)"
                      class="rounded-lg p-1.5 text-dark-400 dark:text-dark-500 hover:bg-amber-50 dark:hover:bg-amber-600/10 hover:text-amber-600 dark:hover:text-amber-400 transition-colors"
                      title="暂停"
                    >
                      <Pause class="h-4 w-4" />
                    </button>
                    <button
                      v-if="wf.status === 'RUNNING' || wf.status === 'PAUSED'"
                      @click="handleRollback(wf.workflowId)"
                      class="rounded-lg p-1.5 text-dark-400 dark:text-dark-500 hover:bg-red-50 dark:hover:bg-red-600/10 hover:text-red-600 dark:hover:text-red-400 transition-colors"
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
  Loader2, RefreshCw, Play, Pause, RotateCcw, CheckCircle2, XCircle,
  Clock, Layers, Workflow, GitBranch, Search, FileCode
} from 'lucide-vue-next'
import type { WorkflowDto } from '@/api/workflow'
import { listWorkflows, pauseWorkflow, resumeWorkflow, rollbackWorkflow } from '@/api/workflow'

const workflows = ref<WorkflowDto[]>([])
const loading = ref(false)
let pollTimer: ReturnType<typeof setInterval>

const typeConfig: Record<string, { bg: string; text: string; icon: any }> = {
  REPOSITORY_SYNC: { bg: 'bg-primary-50', text: 'text-primary-600', icon: GitBranch },
  CODE_INDEX: { bg: 'bg-blue-50', text: 'text-blue-600', icon: Search },
  DIAGNOSIS: { bg: 'bg-purple-50', text: 'text-purple-600', icon: FileCode },
}

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
