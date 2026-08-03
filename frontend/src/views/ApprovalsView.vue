<template>
  <div class="w-full p-4 sm:p-6 lg:p-8 space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-2xl font-bold text-dark-900 dark:text-white">审批工作台</h1>
      <p class="text-dark-500 dark:text-dark-400 text-sm mt-1">审核高风险操作和变更发布</p>
    </div>

    <!-- Tabs -->
    <div class="flex items-center gap-1 border-b border-dark-200 dark:border-dark-700">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        @click="activeTab = tab.value"
        class="flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors"
        :class="activeTab === tab.value
          ? 'border-primary-600 text-primary-600'
          : 'border-transparent text-dark-500 dark:text-dark-400 hover:text-dark-700 dark:hover:text-white'"
      >
        {{ tab.label }}
        <span
          class="text-xs px-1.5 py-0.5 rounded-md"
          :class="activeTab === tab.value ? 'bg-primary-100 text-primary-700 dark:bg-primary-600/20 dark:text-primary-300' : 'bg-dark-100 text-dark-500 dark:bg-dark-800 dark:text-dark-400'"
        >
          {{ getCountByStatus(tab.value) }}
        </span>
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-20">
      <Loader2 class="h-8 w-8 animate-spin text-primary-500" />
    </div>

    <!-- Empty State -->
    <div v-else-if="filteredApprovals.length === 0" class="card dark:card-dark p-8 text-center">
      <div class="mx-auto flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-50 dark:bg-emerald-600/10 mb-3">
        <ShieldCheck class="h-5 w-5 text-emerald-500" />
      </div>
      <h3 class="text-base font-semibold text-dark-700 dark:text-white mb-1">暂无待审批事项</h3>
      <p class="text-dark-400 dark:text-dark-500 text-sm">所有审批已处理完毕</p>
    </div>

    <!-- Approval Cards -->
    <div v-else class="space-y-3">
      <div
        v-for="approval in filteredApprovals"
        :key="approval.workflowId"
        class="card dark:card-dark p-5 border-l-2 hover:shadow-elevated dark:hover:shadow-dark-elevated transition-shadow duration-300 animate-slide-up"
        :class="riskConfig[approval.riskLevel].border"
      >
        <!-- Header -->
        <div class="flex items-start justify-between mb-3">
          <div>
            <h3 class="font-semibold text-dark-800 dark:text-white">代码索引更新</h3>
            <div class="flex items-center gap-2 mt-1">
              <span :class="riskConfig[approval.riskLevel].badge">{{ approval.riskLevel }}</span>
              <span :class="statusConfig[approval.status].badge">{{ statusConfig[approval.status].label }}</span>
            </div>
          </div>
          <span class="text-xs text-dark-400 dark:text-dark-500 font-mono">{{ approval.workflowId.substring(0, 12) }}</span>
        </div>

        <!-- Details -->
        <div class="space-y-1.5 text-sm mb-3">
          <div class="flex items-center gap-2">
            <span class="text-dark-400 dark:text-dark-500 w-20 flex-shrink-0">仓库</span>
            <span class="font-mono text-dark-700 dark:text-dark-200">{{ approval.repository || '-' }}</span>
          </div>
          <div v-if="approval.commitHash" class="flex items-center gap-2">
            <span class="text-dark-400 dark:text-dark-500 w-20 flex-shrink-0">Commit</span>
            <span class="font-mono text-dark-700 dark:text-dark-200">{{ approval.commitHash.substring(0, 12) }}</span>
          </div>
        </div>

        <!-- Actions -->
        <div v-if="approval.status === 'PENDING'" class="flex gap-2">
          <button @click="handleApprove(approval.workflowId)" class="btn-success btn-sm flex-1">
            <Check class="h-4 w-4" />
            批准
          </button>
          <button @click="handleReject(approval.workflowId)" class="btn-danger btn-sm flex-1">
            <X class="h-4 w-4" />
            拒绝
          </button>
        </div>

        <!-- Reason for rejection -->
        <div v-if="approval.status === 'REJECTED'" class="rounded-lg bg-red-50 dark:bg-red-600/10 p-3 mt-2">
          <p class="text-xs text-red-600 dark:text-red-400">该审批已被拒绝</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  Loader2, Check, X, ShieldCheck
} from 'lucide-vue-next'
import type { ApprovalDto } from '@/api/approval'
import { listPendingApprovals, approveWorkflow, rejectWorkflow } from '@/api/approval'

const approvals = ref<ApprovalDto[]>([])
const loading = ref(false)
const activeTab = ref('PENDING')

const tabs = [
  { label: '待审批', value: 'PENDING' },
  { label: '已批准', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '全部', value: 'ALL' },
]

const riskConfig: Record<string, { border: string; badge: string }> = {
  HIGH: { border: 'border-l-red-500', badge: 'badge badge-danger' },
  MEDIUM: { border: 'border-l-amber-500', badge: 'badge badge-warning' },
  LOW: { border: 'border-l-emerald-500', badge: 'badge badge-success' },
}

const statusConfig: Record<string, { label: string; badge: string }> = {
  PENDING: { label: '待审批', badge: 'badge badge-warning' },
  APPROVED: { label: '已批准', badge: 'badge badge-success' },
  REJECTED: { label: '已拒绝', badge: 'badge badge-danger' },
}

const filteredApprovals = computed(() => {
  if (activeTab.value === 'ALL') return approvals.value
  return approvals.value.filter(a => a.status === activeTab.value)
})

function getCountByStatus(status: string): number {
  if (status === 'ALL') return approvals.value.length
  return approvals.value.filter(a => a.status === status).length
}

async function loadApprovals() {
  loading.value = true
  try {
    approvals.value = await listPendingApprovals()
  } catch {
    approvals.value = []
  } finally {
    loading.value = false
  }
}

async function handleApprove(workflowId: string) {
  await approveWorkflow(workflowId, { approver: 'admin', comment: '批准' })
  await loadApprovals()
}

async function handleReject(workflowId: string) {
  const reason = prompt('请输入拒绝原因')
  if (reason === null) return
  await rejectWorkflow(workflowId, { reason })
  await loadApprovals()
}

onMounted(loadApprovals)
</script>
