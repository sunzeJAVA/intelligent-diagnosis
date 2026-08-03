<template>
  <div class="w-full p-4 sm:p-6 lg:p-8 space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-dark-900 dark:text-white">快照管理</h1>
        <p class="text-dark-500 dark:text-dark-400 text-sm mt-1">查看索引快照、校验状态、版本差异与物理回滚</p>
      </div>
      <button @click="loadSnapshots" class="btn-secondary dark:btn-secondary-dark btn-sm">
        <RefreshCw class="h-4 w-4" :class="loading ? 'animate-spin' : ''" />
        刷新
      </button>
    </div>

    <div class="flex gap-4">
      <input
        v-model="repository"
        placeholder="输入仓库名称"
        class="input dark:input-dark flex-1 max-w-sm"
        @keyup.enter="loadSnapshots"
      />
      <button @click="loadSnapshots" class="btn-primary dark:btn-primary-dark">查询</button>
    </div>

    <div v-if="diff" class="card dark:card-dark p-4">
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-sm font-semibold text-dark-800 dark:text-white">差异报告</h3>
        <span class="text-xs text-dark-400 dark:text-dark-500 font-mono">
          {{ diff.leftCommitHash.substring(0, 8) }} → {{ diff.rightCommitHash.substring(0, 8) }}
        </span>
      </div>
      <div class="grid grid-cols-2 gap-6 text-sm">
        <div class="flex items-baseline gap-2">
          <span class="text-dark-500 dark:text-dark-400 text-xs">元素变化</span>
          <span class="font-medium font-mono" :class="diff.elementDelta >= 0 ? 'text-emerald-600' : 'text-red-600'">
            {{ diff.elementDelta > 0 ? '+' : '' }}{{ diff.elementDelta }}
          </span>
        </div>
        <div class="flex items-baseline gap-2">
          <span class="text-dark-500 dark:text-dark-400 text-xs">关系变化</span>
          <span class="font-medium font-mono" :class="diff.relationDelta >= 0 ? 'text-emerald-600' : 'text-red-600'">
            {{ diff.relationDelta > 0 ? '+' : '' }}{{ diff.relationDelta }}
          </span>
        </div>
      </div>
    </div>

    <div v-if="loading && snapshots.length === 0" class="flex items-center justify-center py-16">
      <Loader2 class="h-6 w-6 animate-spin text-primary-500" />
    </div>

    <div v-else-if="snapshots.length === 0" class="card dark:card-dark p-8 text-center">
      <h3 class="text-sm font-semibold text-dark-700 dark:text-white">暂无快照</h3>
      <p class="text-dark-400 dark:text-dark-500 text-xs mt-1">触发索引更新工作流后将生成快照</p>
    </div>

    <div v-else class="card dark:card-dark overflow-hidden">
      <div class="overflow-x-auto scrollbar-thin">
        <table class="w-full">
          <thead>
            <tr class="border-b border-dark-200 dark:border-dark-700 bg-dark-50 dark:bg-dark-800">
              <th class="px-4 py-2.5 text-left text-xs font-medium text-dark-500 dark:text-dark-400">快照</th>
              <th class="px-4 py-2.5 text-left text-xs font-medium text-dark-500 dark:text-dark-400">Commit</th>
              <th class="px-4 py-2.5 text-left text-xs font-medium text-dark-500 dark:text-dark-400">状态</th>
              <th class="px-4 py-2.5 text-left text-xs font-medium text-dark-500 dark:text-dark-400">元素/关系</th>
              <th class="px-4 py-2.5 text-left text-xs font-medium text-dark-500 dark:text-dark-400">物理备份</th>
              <th class="px-4 py-2.5 text-left text-xs font-medium text-dark-500 dark:text-dark-400">时间</th>
              <th class="px-4 py-2.5 text-right text-xs font-medium text-dark-500 dark:text-dark-400">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-dark-100 dark:divide-dark-700">
            <tr v-for="snapshot in snapshots" :key="snapshot.id" class="hover:bg-dark-50 dark:hover:bg-dark-800/50 transition-colors">
              <td class="px-4 py-3">
                <p class="text-sm font-medium text-dark-800 dark:text-white font-mono">{{ snapshot.id.substring(0, 8) }}</p>
                <p class="text-xs text-dark-400 dark:text-dark-500">{{ snapshot.workflowId || '-' }}</p>
              </td>
              <td class="px-4 py-3 text-sm text-dark-600 dark:text-dark-300 font-mono">
                {{ snapshot.commitHash ? snapshot.commitHash.substring(0, 8) : '-' }}
              </td>
              <td class="px-4 py-3">
                <span class="badge" :class="statusBadge(snapshot.status)">{{ snapshot.status }}</span>
              </td>
              <td class="px-4 py-3 text-sm text-dark-600 dark:text-dark-300 font-mono">
                {{ snapshot.elementCount }} / {{ snapshot.relationCount }}
              </td>
              <td class="px-4 py-3">
                <div class="flex items-center gap-1">
                  <span
                    class="inline-flex items-center justify-center h-4 w-4 rounded text-[10px] font-medium leading-none"
                    :class="snapshot.qdrantSnapshotPath ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400' : 'bg-dark-100 text-dark-400 dark:bg-dark-800 dark:text-dark-500'"
                    title="Qdrant 向量快照"
                  >
                    Q
                  </span>
                  <span
                    class="inline-flex items-center justify-center h-4 w-4 rounded text-[10px] font-medium leading-none"
                    :class="snapshot.neo4jBackupPath ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400' : 'bg-dark-100 text-dark-400 dark:bg-dark-800 dark:text-dark-500'"
                    title="Neo4j 图备份"
                  >
                    N
                  </span>
                </div>
              </td>
              <td class="px-4 py-3 text-sm text-dark-500 dark:text-dark-400">
                {{ formatTime(snapshot.createdAt) }}
              </td>
              <td class="px-4 py-3 text-right">
                <div class="inline-flex items-center gap-2 justify-end">
                  <button
                    v-if="selectedLeft && selectedLeft !== snapshot.id"
                    @click="diffWithSelected(snapshot.id)"
                    class="btn-primary dark:btn-primary-dark btn-sm"
                  >
                    对比
                  </button>
                  <button
                    v-else
                    @click="selectedLeft = snapshot.id"
                    class="btn-secondary dark:btn-secondary-dark btn-sm"
                  >
                    选为左侧
                  </button>
                  <button
                    v-if="canPreview(snapshot)"
                    @click="previewDiff(snapshot.id)"
                    class="btn-secondary dark:btn-secondary-dark btn-sm"
                  >
                    <Eye class="h-3 w-3" />
                    预览
                  </button>
                  <button
                    v-if="canRollback(snapshot)"
                    @click="handleRollback(snapshot)"
                    :disabled="rollingBack === snapshot.id"
                    class="btn-danger btn-sm"
                  >
                    <RotateCcw v-if="rollingBack !== snapshot.id" class="h-3 w-3" />
                    <Loader2 v-else class="h-3 w-3 animate-spin" />
                    回滚
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
import { ref, computed } from 'vue'
import { Loader2, RefreshCw, RotateCcw, Eye } from 'lucide-vue-next'
import type { SnapshotDto, SnapshotDiff } from '@/api/snapshot'
import { listSnapshots, diffSnapshots, rollbackSnapshot } from '@/api/snapshot'
import { showToast } from '@/utils/toast'

const repository = ref('')
const snapshots = ref<SnapshotDto[]>([])
const loading = ref(false)
const rollingBack = ref<string | null>(null)
const selectedLeft = ref<string | null>(null)
const diff = ref<SnapshotDiff | null>(null)

const latestPromotedId = computed(() => {
  return snapshots.value.find(s => s.status === 'PROMOTED')?.id ?? null
})

function statusBadge(status: string): string {
  const map: Record<string, string> = {
    CREATING: 'badge-info dark:badge-info-dark',
    VALIDATING: 'badge-warning dark:badge-warning-dark',
    PROMOTED: 'badge-success dark:badge-success-dark',
    FAILED: 'badge-error dark:badge-error-dark',
    ROLLED_BACK: 'badge-neutral dark:badge-neutral-dark'
  }
  return map[status] || 'badge-neutral dark:badge-neutral-dark'
}

function formatTime(time?: string | null): string {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function canRollback(snapshot: SnapshotDto): boolean {
  return snapshot.status === 'PROMOTED' &&
    !!snapshot.qdrantSnapshotPath &&
    !!snapshot.neo4jBackupPath
}

function canPreview(snapshot: SnapshotDto): boolean {
  return !!latestPromotedId.value && latestPromotedId.value !== snapshot.id
}

async function loadSnapshots() {
  if (!repository.value.trim()) return
  loading.value = true
  try {
    snapshots.value = await listSnapshots(repository.value.trim())
    selectedLeft.value = null
    diff.value = null
  } catch {
    snapshots.value = []
  } finally {
    loading.value = false
  }
}

async function diffWithSelected(rightId: string) {
  if (!selectedLeft.value) return
  try {
    diff.value = await diffSnapshots(selectedLeft.value, rightId)
  } catch {
    diff.value = null
  }
}

async function previewDiff(snapshotId: string) {
  if (!latestPromotedId.value) return
  try {
    diff.value = await diffSnapshots(snapshotId, latestPromotedId.value)
  } catch {
    diff.value = null
  }
}

async function handleRollback(snapshot: SnapshotDto) {
  const confirmed = window.confirm(
    `确定要将仓库 "${snapshot.repositoryName}" 回滚到快照 ${snapshot.id.substring(0, 8)} 吗？\n\n` +
    '这会覆盖当前生产环境的向量索引和图索引，且无法撤销。'
  )
  if (!confirmed) return

  rollingBack.value = snapshot.id
  try {
    await rollbackSnapshot(snapshot.id)
    showToast('回滚成功', 'success')
    await loadSnapshots()
  } catch (e) {
    showToast(e instanceof Error ? e.message : '回滚失败', 'error')
  } finally {
    rollingBack.value = null
  }
}
</script>
