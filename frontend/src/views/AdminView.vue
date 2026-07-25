<template>
  <div class="w-full p-4 sm:p-6 lg:p-8 space-y-8">
      <!-- Header -->
      <div>
        <h1 class="text-2xl font-bold text-dark-900 dark:text-white">系统管理</h1>
        <p class="text-dark-500 dark:text-dark-400 text-sm mt-1">基础设施状态、配置管理和系统监控</p>
      </div>

      <!-- Infrastructure Status -->
      <div>
        <h2 class="text-sm font-semibold text-dark-700 dark:text-dark-200 uppercase tracking-wider mb-3">基础设施状态</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div v-for="infra in infrastructures" :key="infra.name" class="card dark:card-dark p-5">
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-3">
                <div
                  class="flex h-10 w-10 items-center justify-center rounded-lg"
                  :class="infra.connected ? 'bg-green-50 dark:bg-green-600/20' : 'bg-red-50 dark:bg-red-600/20'"
                >
                  <component :is="infra.icon" class="h-5 w-5" :class="infra.connected ? 'text-green-600' : 'text-red-600'" />
                </div>
                <div>
                  <h3 class="font-semibold text-dark-800 dark:text-white">{{ infra.name }}</h3>
                  <p class="text-xs text-dark-400 dark:text-dark-500">{{ infra.type }}</p>
                </div>
              </div>
              <span
                class="flex items-center gap-1.5 text-xs font-medium"
                :class="infra.connected ? 'text-green-600' : 'text-red-600'"
              >
                <span class="h-1.5 w-1.5 rounded-full" :class="infra.connected ? 'bg-green-500 animate-pulse' : 'bg-red-500'"></span>
                {{ infra.connected ? '已连接' : '未连接' }}
              </span>
            </div>
            <div class="space-y-1.5 text-sm">
              <div class="flex items-center justify-between">
                <span class="text-dark-400 dark:text-dark-500">地址</span>
                <span class="font-mono text-xs text-dark-600 dark:text-dark-300">{{ infra.url }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-dark-400 dark:text-dark-500">延迟</span>
                <span class="font-mono text-xs" :class="infra.latency < 50 ? 'text-green-600' : infra.latency < 200 ? 'text-amber-600' : 'text-red-600'">
                  {{ infra.latency }}ms
                </span>
              </div>
              <div v-if="infra.version" class="flex items-center justify-between">
                <span class="text-dark-400 dark:text-dark-500">版本</span>
                <span class="font-mono text-xs text-dark-600 dark:text-dark-300">{{ infra.version }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Parse Workers -->
      <div>
        <h2 class="text-sm font-semibold text-dark-700 dark:text-dark-200 uppercase tracking-wider mb-3">Parse Workers</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div v-for="worker in workers" :key="worker.name" class="card dark:card-dark p-5">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div
                  class="flex h-10 w-10 items-center justify-center rounded-lg"
                  :class="worker.healthy ? 'bg-green-50 dark:bg-green-600/20' : 'bg-red-50 dark:bg-red-600/20'"
                >
                  <Code class="h-5 w-5" :class="worker.healthy ? 'text-green-600' : 'text-red-600'" />
                </div>
                <div>
                  <h3 class="font-semibold text-dark-800 dark:text-white">{{ worker.name }}</h3>
                  <p class="text-xs text-dark-400 dark:text-dark-500">{{ worker.language }}</p>
                </div>
              </div>
              <div class="text-right">
                <span
                  class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium"
                  :class="worker.healthy ? 'bg-green-100 text-green-700 dark:bg-green-600/20 dark:text-green-400' : 'bg-red-100 text-red-700 dark:bg-red-600/20 dark:text-red-400'"
                >
                  <span class="h-1.5 w-1.5 rounded-full" :class="worker.healthy ? 'bg-green-500' : 'bg-red-500'"></span>
                  {{ worker.healthy ? '健康' : '异常' }}
                </span>
                <p class="font-mono text-xs text-dark-400 dark:text-dark-500 mt-1">{{ worker.address }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- System Metrics -->
      <div>
        <h2 class="text-sm font-semibold text-dark-700 dark:text-dark-200 uppercase tracking-wider mb-3">系统指标</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div class="card dark:card-dark p-5">
            <div class="flex items-center gap-2 mb-2">
              <Database class="h-4 w-4 text-primary-600" />
              <p class="text-xs font-medium text-dark-500 dark:text-dark-400">向量索引总数</p>
            </div>
            <p class="text-2xl font-bold text-dark-900 dark:text-white">{{ formatNumber(metrics.vectorCount) }}</p>
            <p class="text-xs text-dark-400 dark:text-dark-500 mt-1">Qdrant</p>
          </div>
          <div class="card dark:card-dark p-5">
            <div class="flex items-center gap-2 mb-2">
              <Share2 class="h-4 w-4 text-blue-600" />
              <p class="text-xs font-medium text-dark-500 dark:text-dark-400">图谱节点数</p>
            </div>
            <p class="text-2xl font-bold text-dark-900 dark:text-white">{{ formatNumber(metrics.graphNodes) }}</p>
            <p class="text-xs text-dark-400 dark:text-dark-500 mt-1">Neo4j</p>
          </div>
          <div class="card dark:card-dark p-5">
            <div class="flex items-center gap-2 mb-2">
              <Link class="h-4 w-4 text-purple-600" />
              <p class="text-xs font-medium text-dark-500 dark:text-dark-400">图谱关系数</p>
            </div>
            <p class="text-2xl font-bold text-dark-900 dark:text-white">{{ formatNumber(metrics.graphRelations) }}</p>
            <p class="text-xs text-dark-400 dark:text-dark-500 mt-1">Neo4j</p>
          </div>
          <div class="card dark:card-dark p-5">
            <div class="flex items-center gap-2 mb-2">
              <Activity class="h-4 w-4 text-green-600" />
              <p class="text-xs font-medium text-dark-500 dark:text-dark-400">诊断总次数</p>
            </div>
            <p class="text-2xl font-bold text-dark-900 dark:text-white">{{ formatNumber(metrics.diagnosisCount) }}</p>
            <p class="text-xs text-dark-400 dark:text-dark-500 mt-1">累计</p>
          </div>
        </div>
      </div>

      <!-- Configuration -->
      <div>
        <h2 class="text-sm font-semibold text-dark-700 dark:text-dark-200 uppercase tracking-wider mb-3">系统配置</h2>
        <div class="card dark:card-dark overflow-hidden">
          <div class="divide-y divide-dark-100 dark:divide-dark-700">
            <div v-for="config in configurations" :key="config.key" class="flex items-center justify-between px-5 py-4 hover:bg-dark-50 dark:hover:bg-dark-800/50 transition-colors">
              <div class="flex items-center gap-3">
                <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-dark-100 dark:bg-dark-800">
                  <component :is="config.icon" class="h-4 w-4 text-dark-500 dark:text-dark-400" />
                </div>
                <div>
                  <p class="text-sm font-medium text-dark-800 dark:text-white">{{ config.label }}</p>
                  <p class="text-xs text-dark-400 dark:text-dark-500 font-mono">{{ config.key }}</p>
                </div>
              </div>
              <div class="flex items-center gap-3">
                <span class="text-sm text-dark-600 dark:text-dark-300 font-mono">{{ config.value }}</span>
                <button class="text-dark-400 dark:text-dark-500 hover:text-primary-600 dark:hover:text-primary-400 transition-colors">
                  <Pencil class="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { controlApi } from '@/api/client'
import {
  Database, Share2, Link, Activity, Code, Server, Network,
  Cpu, Pencil, Settings, Brain, Zap
} from 'lucide-vue-next'

const infrastructures = ref([
  { name: 'PostgreSQL', type: '元数据存储', url: 'localhost:5432', connected: true, latency: 2, version: '16.2', icon: Database },
  { name: 'Qdrant', type: '向量数据库', url: 'localhost:6333', connected: true, latency: 5, version: '1.10.0', icon: Database },
  { name: 'Neo4j', type: '图数据库', url: 'localhost:7687', connected: true, latency: 8, version: '5.20.0', icon: Network },
  { name: 'Temporal', type: '工作流引擎', url: 'localhost:7233', connected: true, latency: 3, version: '1.23.0', icon: Server },
  { name: 'LLM Gateway', type: 'AI 模型', url: 'api.openai.com', connected: true, latency: 120, version: 'gpt-4o', icon: Brain },
  { name: 'Redis', type: '缓存', url: 'localhost:6379', connected: false, latency: 0, version: '-', icon: Zap },
])

const workers = ref([
  { name: 'java-parser', language: 'Java 21', address: 'localhost:9093', healthy: true },
  { name: 'csharp-parser', language: 'C# 12', address: 'localhost:9094', healthy: true },
])

const metrics = ref({
  vectorCount: 0,
  graphNodes: 0,
  graphRelations: 0,
  diagnosisCount: 0,
})

const configurations = ref([
  { key: 'diagnosis.llm.model', label: 'LLM 模型', value: 'gpt-4o', icon: Brain },
  { key: 'diagnosis.llm.temperature', label: 'LLM 温度', value: '0.3', icon: Settings },
  { key: 'diagnosis.llm.timeout', label: 'LLM 超时', value: '30s', icon: Settings },
  { key: 'rag.vector.topK', label: '向量检索 TopK', value: '10', icon: Database },
  { key: 'rag.graph.maxDepth', label: '图检索最大深度', value: '3', icon: Network },
  { key: 'index.batchSize', label: '索引批量大小', value: '100', icon: Cpu },
  { key: 'security.maxQuerySize', label: '最大查询大小', value: '4096', icon: Settings },
])

/**
 * 格式化数字为千分位格式
 * @param n - 数字
 * @returns 格式化后的字符串
 */
function formatNumber(n: number): string {
  return n.toLocaleString()
}

/**
 * 加载系统指标数据
 * 失败时使用 mock 数据
 */
async function loadMetrics() {
  try {
    const response = await controlApi.get<typeof metrics.value>('/admin/metrics')
    metrics.value = response.data
  } catch {
    metrics.value = { vectorCount: 15482, graphNodes: 8934, graphRelations: 23107, diagnosisCount: 1267 }
  }
}

onMounted(loadMetrics)
</script>
