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
            class="rounded-full px-2 py-0.5 text-xs"
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
      <div v-else-if="filteredApprovals.length === 0" class="card dark:card-dark p-12 text-center">
        <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-50 dark:bg-green-600/20 mb-4">
          <ShieldCheck class="h-8 w-8 text-green-500" />
        </div>
        <h3 class="text-lg font-semibold text-dark-700 dark:text-white mb-1">暂无待审批事项</h3>
        <p class="text-dark-400 dark:text-dark-500 text-sm">所有审批已处理完毕</p>
      </div>

      <!-- Approval Cards -->
      <div v-else class="space-y-4">
        <div
          v-for="approval in filteredApprovals"
          :key="approval.id"
          class="card dark:card-dark p-5 hover:shadow-elevated dark:hover:shadow-dark-elevated transition-shadow duration-300 animate-slide-up"
        >
          <!-- Header -->
          <div class="flex items-start justify-between mb-3">
            <div class="flex items-start gap-3">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-lg"
                :class="riskConfig[approval.riskLevel].bg"
              >
                <component :is="riskConfig[approval.riskLevel].icon" class="h-5 w-5" :class="riskConfig[approval.riskLevel].text" />
              </div>
              <div>
                <h3 class="font-semibold text-dark-800 dark:text-white">{{ approval.operationType }}</h3>
                <div class="flex items-center gap-2 mt-0.5">
                  <span :class="riskConfig[approval.riskLevel].badge">{{ approval.riskLevel }}</span>
                  <span :class="statusConfig[approval.status].badge">{{ statusConfig[approval.status].label }}</span>
                </div>
              </div>
            </div>
            <span class="text-xs text-dark-400 dark:text-dark-500">{{ formatTime(approval.createdAt) }}</span>
          </div>

          <!-- Details -->
          <div class="rounded-lg bg-dark-50 dark:bg-dark-800 p-3 mb-3 space-y-1.5 text-sm">
            <div class="flex items-center gap-2">
              <span class="text-dark-400 dark:text-dark-500 w-20 flex-shrink-0">仓库</span>
              <span class="font-mono text-dark-700 dark:text-dark-200">{{ approval.requestData.repositoryName || '-' }}</span>
            </div>
            <div v-if="approval.requestData.commitId" class="flex items-center gap-2">
              <span class="text-dark-400 dark:text-dark-500 w-20 flex-shrink-0">Commit</span>
              <span class="font-mono text-dark-700 dark:text-dark-200">{{ approval.requestData.commitId.substring(0, 12) }}</span>
            </div>
            <div v-if="approval.requestData.changedFiles !== undefined" class="flex items-center gap-2">
              <span class="text-dark-400 dark:text-dark-500 w-20 flex-shrink-0">变更文件</span>
              <span class="text-dark-700 dark:text-dark-200">{{ approval.requestData.changedFiles }} 个</span>
            </div>
            <div v-if="approval.requestData.description" class="flex items-start gap-2">
              <span class="text-dark-400 dark:text-dark-500 w-20 flex-shrink-0">描述</span>
              <span class="text-dark-700 dark:text-dark-200">{{ approval.requestData.description }}</span>
            </div>
          </div>

          <!-- Reviewer -->
          <div v-if="approval.status !== 'PENDING'" class="flex items-center gap-2 text-xs text-dark-500 dark:text-dark-400 mb-3">
            <User class="h-3.5 w-3.5" />
            <span>{{ approval.reviewerId || '系统' }} 审批于 {{ formatTime(approval.reviewedAt) }}</span>
          </div>

          <!-- Actions -->
          <div v-if="approval.status === 'PENDING'" class="flex gap-2">
            <button @click="handleApprove(approval.id!)" class="btn-success btn-sm flex-1">
              <Check class="h-4 w-4" />
              批准
            </button>
            <button @click="handleReject(approval.id!)" class="btn-danger btn-sm flex-1">
              <X class="h-4 w-4" />
              拒绝
            </button>
          </div>

          <!-- Reason for rejection -->
          <div v-if="approval.status === 'REJECTED' && approval.reviewComment" class="rounded-lg bg-red-50 dark:bg-red-600/10 p-3 mt-2">
            <p class="text-xs text-red-600 dark:text-red-400">{{ approval.reviewComment }}</p>
          </div>
        </div>
      </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { controlApi } from '@/api/client'
import {
  Loader2, Check, X, User, ShieldAlert, ShieldCheck,
  ShieldX
} from 'lucide-vue-next'

interface ApprovalRequest {
  id?: string
  operationType: string
  riskLevel: string
  status: string
  reviewerId?: string
  reviewedAt?: string
  createdAt?: string
  reviewComment?: string
  requestData: {
    repositoryName?: string
    commitId?: string
    changedFiles?: number
    description?: string
    [key: string]: unknown
  }
}

const approvals = ref<ApprovalRequest[]>([])
const loading = ref(false)
const activeTab = ref('PENDING')

const tabs = [
  { label: '待审批', value: 'PENDING' },
  { label: '已批准', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '全部', value: 'ALL' },
]

const riskConfig: Record<string, { bg: string; text: string; badge: string; icon: any }> = {
  HIGH: { bg: 'bg-red-100', text: 'text-red-600', badge: 'badge badge-danger', icon: ShieldX },
  MEDIUM: { bg: 'bg-amber-100', text: 'text-amber-600', badge: 'badge badge-warning', icon: ShieldAlert },
  LOW: { bg: 'bg-green-100', text: 'text-green-600', badge: 'badge badge-success', icon: ShieldCheck },
}

const statusConfig: Record<string, { label: string; badge: string }> = {
  PENDING: { label: '待审批', badge: 'badge badge-warning' },
  APPROVED: { label: '已批准', badge: 'badge badge-success' },
  REJECTED: { label: '已拒绝', badge: 'badge badge-danger' },
  EXPIRED: { label: '已过期', badge: 'badge badge-info' },
}

const filteredApprovals = computed(() => {
  if (activeTab.value === 'ALL') return approvals.value
  return approvals.value.filter(a => a.status === activeTab.value)
})

/**
 * 获取指定状态的审批请求数量
 * @param status - 审批状态（PENDING/APPROVED/REJECTED/ALL）
 * @returns 该状态的审批请求数量
 */
function getCountByStatus(status: string): number {
  if (status === 'ALL') return approvals.value.length
  return approvals.value.filter(a => a.status === status).length
}

/**
 * 加载审批请求列表
 */
async function loadApprovals() {
  loading.value = true
  try {
    const response = await controlApi.get<ApprovalRequest[]>('/approvals')
    approvals.value = response.data
  } catch {
    approvals.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 批准指定审批请求
 * @param id - 审批请求 ID
 */
async function handleApprove(id: string) {
  await controlApi.post(`/approvals/${id}/approve`)
  await loadApprovals()
}

/**
 * 拒绝指定审批请求（需输入原因）
 * @param id - 审批请求 ID
 */
async function handleReject(id: string) {
  const reason = prompt('请输入拒绝原因')
  if (reason === null) return
  await controlApi.post(`/approvals/${id}/reject`, { reason })
  await loadApprovals()
}

/**
 * 格式化时间戳为可读字符串
 * @param time - 时间字符串（ISO 格式）
 * @returns 格式化后的时间字符串，无时间时返回 '-'
 */
function formatTime(time?: string): string {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(loadApprovals)
</script>
