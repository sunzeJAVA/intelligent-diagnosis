<template>
  <div class="w-full p-4 sm:p-6 lg:p-8 space-y-8">
    <!-- Header -->
    <div>
      <h1 class="text-2xl font-bold text-dark-900 dark:text-white">系统管理</h1>
      <p class="text-dark-500 dark:text-dark-400 text-sm mt-1">基础设施状态、配置管理和系统监控</p>
    </div>

    <!-- Infrastructure Status -->
    <div>
      <h2 class="text-base font-semibold text-dark-900 dark:text-white mb-3">基础设施状态</h2>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div v-for="infra in infrastructures" :key="infra.name" class="card dark:card-dark p-5">
          <div class="flex items-center justify-between mb-3">
            <div>
              <h3 class="text-sm font-semibold text-dark-900 dark:text-white">{{ infra.name }}</h3>
              <p class="text-xs text-dark-500 dark:text-dark-400 mt-0.5">{{ infra.type }}</p>
            </div>
            <span class="flex items-center gap-1.5 text-xs font-medium" :class="infra.connected ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-600 dark:text-red-400'">
              <span class="h-2 w-2 rounded-full" :class="infra.connected ? 'bg-emerald-500' : 'bg-red-500'"></span>
              {{ infra.connected ? '已连接' : '未连接' }}
            </span>
          </div>
          <div class="space-y-1.5 text-sm">
            <div class="flex items-center justify-between">
              <span class="text-dark-500 dark:text-dark-400">地址</span>
              <span class="font-mono text-xs text-dark-700 dark:text-dark-200">{{ infra.url }}</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-dark-500 dark:text-dark-400">延迟</span>
              <span class="font-mono text-xs" :class="infra.latency < 50 ? 'text-emerald-600' : infra.latency < 200 ? 'text-amber-600' : 'text-red-600'">
                {{ infra.connected ? infra.latency + 'ms' : '—' }}
              </span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-dark-500 dark:text-dark-400">版本</span>
              <span class="font-mono text-xs text-dark-700 dark:text-dark-200">{{ infra.version || '—' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Parse Workers -->
    <div>
      <h2 class="text-base font-semibold text-dark-900 dark:text-white mb-3">Parse Workers</h2>
      <div v-if="workers.length === 0" class="card dark:card-dark p-5 text-center">
        <p class="text-sm text-dark-400 dark:text-dark-500">未配置 Parse Worker 端点</p>
      </div>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div v-for="worker in workers" :key="worker.name" class="card dark:card-dark p-5">
          <div class="flex items-center justify-between">
            <div>
              <h3 class="text-sm font-semibold text-dark-900 dark:text-white">{{ worker.name }}</h3>
              <p class="text-xs text-dark-500 dark:text-dark-400 mt-0.5">{{ worker.language }}</p>
            </div>
            <div class="flex flex-col items-end gap-1">
              <span class="flex items-center gap-1.5 text-xs font-medium" :class="worker.healthy ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-600 dark:text-red-400'">
                <span class="h-2 w-2 rounded-full" :class="worker.healthy ? 'bg-emerald-500' : 'bg-red-500'"></span>
                {{ worker.healthy ? '健康' : '异常' }}
              </span>
              <p class="font-mono text-xs text-dark-500 dark:text-dark-400">{{ worker.address }}</p>
              <p v-if="worker.healthy" class="font-mono text-xs text-dark-400 dark:text-dark-500">{{ worker.latency }}ms</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- System Metrics -->
    <div>
      <h2 class="text-base font-semibold text-dark-900 dark:text-white mb-3">系统指标</h2>
      <div v-if="metricsUnavailable" class="card dark:card-dark p-5 text-center">
        <p class="text-sm text-amber-600 dark:text-amber-400">数据平面暂时不可用，指标无法获取</p>
      </div>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div class="card dark:card-dark p-5">
          <p class="text-xs font-medium text-dark-500 dark:text-dark-400">向量索引总数</p>
          <p class="text-2xl font-bold text-dark-900 dark:text-white mt-2">{{ formatNumber(metrics.vectorCount) }}</p>
          <p class="text-xs text-dark-500 dark:text-dark-400 mt-1">Qdrant</p>
        </div>
        <div class="card dark:card-dark p-5">
          <p class="text-xs font-medium text-dark-500 dark:text-dark-400">图谱节点数</p>
          <p class="text-2xl font-bold text-dark-900 dark:text-white mt-2">{{ formatNumber(metrics.graphNodes) }}</p>
          <p class="text-xs text-dark-500 dark:text-dark-400 mt-1">Neo4j</p>
        </div>
        <div class="card dark:card-dark p-5">
          <p class="text-xs font-medium text-dark-500 dark:text-dark-400">图谱关系数</p>
          <p class="text-2xl font-bold text-dark-900 dark:text-white mt-2">{{ formatNumber(metrics.graphRelations) }}</p>
          <p class="text-xs text-dark-500 dark:text-dark-400 mt-1">Neo4j</p>
        </div>
        <div class="card dark:card-dark p-5">
          <p class="text-xs font-medium text-dark-500 dark:text-dark-400">诊断总次数</p>
          <p class="text-2xl font-bold text-dark-900 dark:text-white mt-2">{{ formatNumber(metrics.diagnosisCount) }}</p>
          <p class="text-xs text-dark-500 dark:text-dark-400 mt-1">累计</p>
        </div>
      </div>
    </div>

    <!-- Configuration -->
    <div>
      <div class="flex items-center justify-between mb-3">
        <h2 class="text-base font-semibold text-dark-900 dark:text-white">系统配置</h2>
        <span class="text-xs text-dark-400 dark:text-dark-500">只读展示 · 修改请编辑 application.yml 后重启</span>
      </div>
      <div class="card dark:card-dark overflow-hidden">
        <div class="divide-y divide-dark-100 dark:divide-dark-800">
          <div v-for="config in configurations" :key="config.key" class="flex items-center justify-between px-5 py-4 hover:bg-dark-50 dark:hover:bg-dark-800/50 transition-colors">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <p class="text-sm font-medium text-dark-900 dark:text-white">{{ config.label }}</p>
                <span class="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium" :class="config.source === 'control-plane' ? 'bg-blue-50 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400' : 'bg-purple-50 text-purple-600 dark:bg-purple-900/30 dark:text-purple-400'">
                  {{ config.source }}
                </span>
              </div>
              <p class="text-xs text-dark-500 dark:text-dark-400 font-mono mt-0.5 truncate">{{ config.key }}</p>
            </div>
            <div class="ml-4">
              <span class="text-sm text-dark-700 dark:text-dark-200 font-mono">{{ config.value }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { controlApi } from '@/api/client'

interface Infrastructure {
  name: string
  type: string
  url: string
  connected: boolean
  latency: number
  version: string
}

interface ParseWorker {
  name: string
  language: string
  address: string
  healthy: boolean
  latency: number
}

interface Metrics {
  vectorCount: number
  graphNodes: number
  graphRelations: number
  diagnosisCount: number
}

interface Configuration {
  key: string
  label: string
  value: string
  source: string
}

const infrastructures = ref<Infrastructure[]>([])
const workers = ref<ParseWorker[]>([])
const metrics = ref<Metrics>({ vectorCount: 0, graphNodes: 0, graphRelations: 0, diagnosisCount: 0 })
const metricsUnavailable = ref(false)
const configurations = ref<Configuration[]>([])

function formatNumber(n: number): string {
  if (n < 0) return '—'
  return n.toLocaleString()
}

async function loadMetrics() {
  try {
    const response = await controlApi.get<Metrics>('/admin/metrics')
    metrics.value = response.data
    metricsUnavailable.value = (response.data.vectorCount === 0 && response.data.diagnosisCount === 0)
  } catch {
    metricsUnavailable.value = true
  }
}

async function loadInfrastructures() {
  try {
    const response = await controlApi.get<Infrastructure[]>('/admin/infrastructures')
    infrastructures.value = response.data
  } catch {
    infrastructures.value = []
  }
}

async function loadWorkers() {
  try {
    const response = await controlApi.get<ParseWorker[]>('/admin/parse-workers')
    workers.value = response.data
  } catch {
    workers.value = []
  }
}

async function loadConfigurations() {
  try {
    const response = await controlApi.get<Configuration[]>('/admin/configurations')
    configurations.value = response.data
  } catch {
    configurations.value = []
  }
}

async function refreshAll() {
  await Promise.all([loadMetrics(), loadInfrastructures(), loadWorkers(), loadConfigurations()])
}

let refreshTimer: ReturnType<typeof setInterval>

onMounted(() => {
  refreshAll()
  refreshTimer = setInterval(refreshAll, 30000)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
})
</script>
