<template>
  <div class="w-full p-4 sm:p-6 lg:p-8 space-y-10">
    <!-- Header -->
    <div>
      <h1 class="text-2xl sm:text-3xl font-bold text-dark-900 dark:text-white tracking-tight">智能代码诊断</h1>
      <p class="mt-2 text-sm text-dark-500 dark:text-dark-400">输入错误信息或异常堆栈，AI 将基于知识图谱和向量检索为您定位根因</p>
    </div>

    <!-- Input Section -->
    <div class="w-full">
      <div class="grid grid-cols-1 xl:grid-cols-12 gap-8 xl:gap-10">
        <!-- Left Column - Main Input -->
        <div class="xl:col-span-8">
          <div class="flex items-baseline justify-between mb-3">
            <label class="text-sm font-medium text-dark-700 dark:text-dark-200">问题描述 / 错误信息</label>
            <span class="text-xs text-dark-400 dark:text-dark-500">* 必填</span>
          </div>
          <textarea
            v-model="query"
            rows="12"
            class="input dark:input-dark resize-none font-mono text-sm w-full"
            placeholder="例如：NullPointerException at com.example.service.UserService.findById(UserService.java:45)&#10;&#10;请详细描述您遇到的问题，包括错误发生的场景和具体表现..."
          ></textarea>
        </div>

        <!-- Right Column - Side Panel -->
        <div class="xl:col-span-4 xl:border-l xl:border-dark-200 xl:dark:border-dark-800 xl:pl-10 space-y-6">
          <!-- Error Info -->
          <div>
            <div class="flex items-baseline justify-between mb-3">
              <label class="text-sm font-medium text-dark-700 dark:text-dark-200">异常堆栈 / 上下文</label>
              <span class="text-xs text-dark-400 dark:text-dark-500">可选</span>
            </div>
            <textarea
              v-model="errorInfo"
              rows="5"
              class="input dark:input-dark resize-none font-mono text-sm w-full"
              placeholder="粘贴完整的异常堆栈..."
            ></textarea>
          </div>

          <!-- Service Selection -->
          <div>
            <div class="flex items-baseline justify-between mb-3">
              <label class="text-sm font-medium text-dark-700 dark:text-dark-200">目标服务</label>
              <span class="text-xs text-dark-400 dark:text-dark-500">* 必填</span>
            </div>
            <select v-model="selectedService" class="select dark:select-dark w-full" :disabled="servicesLoading">
              <option value="">{{ servicesLoading ? '加载中...' : (services.length === 0 ? '暂无可用仓库' : '选择服务...') }}</option>
              <option v-for="repo in services" :key="repo.name" :value="repo.name">
                {{ repo.displayName || repo.name }}
              </option>
            </select>
          </div>

          <!-- Diagnose Button -->
          <button
            @click="handleDiagnose"
            :disabled="isLoading || !selectedService || !query"
            class="btn-primary w-full py-2.5 text-sm font-semibold mt-2"
          >
            <Loader2 v-if="isLoading" class="h-4 w-4 animate-spin" />
            <Sparkles v-else class="h-4 w-4" />
            {{ isLoading ? '诊断中...' : '开始诊断' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Error Message -->
    <div v-if="error" class="flex items-start gap-3 rounded-lg border border-red-200 dark:border-red-900/50 bg-red-50 dark:bg-red-900/10 px-4 py-3">
      <XCircle class="h-5 w-5 text-red-600 dark:text-red-400 flex-shrink-0 mt-0.5" />
      <div>
        <p class="text-sm font-semibold text-red-800 dark:text-red-400">诊断失败</p>
        <p class="text-sm text-red-600 dark:text-red-400 mt-0.5">{{ error }}</p>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="space-y-6">
      <div class="card dark:card-dark p-6 border-l-2 border-l-primary-500">
        <div class="skeleton dark:skeleton-dark h-5 w-32 mb-4"></div>
        <div class="space-y-3">
          <div class="skeleton dark:skeleton-dark h-4 w-full"></div>
          <div class="skeleton dark:skeleton-dark h-4 w-5/6"></div>
          <div class="skeleton dark:skeleton-dark h-4 w-4/6"></div>
        </div>
      </div>
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="card dark:card-dark p-6 border-l-2 border-l-amber-500">
          <div class="skeleton dark:skeleton-dark h-5 w-28 mb-4"></div>
          <div class="skeleton dark:skeleton-dark h-20 w-full rounded-lg"></div>
        </div>
        <div class="card dark:card-dark p-6">
          <div class="skeleton dark:skeleton-dark h-5 w-28 mb-4"></div>
          <div class="space-y-3">
            <div class="skeleton dark:skeleton-dark h-4 w-full"></div>
            <div class="skeleton dark:skeleton-dark h-4 w-3/4"></div>
            <div class="skeleton dark:skeleton-dark h-4 w-5/6"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Results -->
    <div v-if="result && !isLoading" class="space-y-6">
      <!-- Summary -->
      <div class="card dark:card-dark p-6 border-l-2 border-l-primary-500">
        <h3 class="text-base font-semibold text-dark-900 dark:text-white mb-3">诊断摘要</h3>
        <p class="text-sm text-dark-600 dark:text-dark-300 leading-relaxed">{{ result.summary }}</p>
      </div>

      <!-- Intent Recognition Result -->
      <div v-if="result.intent" class="card dark:card-dark p-5">
        <div class="flex flex-wrap items-center gap-4">
          <div class="flex items-center gap-2">
            <Sparkles class="h-4 w-4 text-primary-500" />
            <span class="text-xs font-medium text-dark-500 dark:text-dark-400">意图识别</span>
          </div>
          <!-- Intent Type Badge -->
          <span class="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-medium bg-primary-50 text-primary-700 dark:bg-primary-600/15 dark:text-primary-400">
            {{ result.intent.displayName }}
          </span>
          <!-- Confidence -->
          <div class="flex items-center gap-1.5">
            <span class="text-xs text-dark-400 dark:text-dark-500">置信度</span>
            <div class="w-16 h-1.5 rounded-full bg-dark-100 dark:bg-dark-700 overflow-hidden">
              <div class="h-full rounded-full bg-primary-500 transition-all" :style="{ width: `${Math.round(result.intent.confidence * 100)}%` }"></div>
            </div>
            <span class="text-xs font-mono text-dark-600 dark:text-dark-300">{{ (result.intent.confidence * 100).toFixed(0) }}%</span>
          </div>
          <!-- Entities -->
          <div v-if="result.intent.entities.length > 0" class="flex items-center gap-1.5 flex-wrap">
            <span class="text-xs text-dark-400 dark:text-dark-500">关键实体</span>
            <span
              v-for="entity in result.intent.entities"
              :key="entity"
              class="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-mono bg-dark-100 text-dark-600 dark:bg-dark-800 dark:text-dark-300"
            >
              {{ entity }}
            </span>
          </div>
        </div>
      </div>

      <!-- Root Cause & Suggestions Grid -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Root Cause -->
        <div class="card dark:card-dark p-6 border-l-2 border-l-amber-500">
          <h3 class="text-base font-semibold text-dark-900 dark:text-white mb-3">根因分析</h3>
          <p class="text-sm text-dark-600 dark:text-dark-300 leading-relaxed">{{ result.rootCause }}</p>
        </div>

        <!-- Suggestions -->
        <div class="card dark:card-dark p-6">
          <h3 class="text-base font-semibold text-dark-900 dark:text-white mb-4">
            修复建议
            <span class="ml-2 text-xs font-normal text-dark-400 dark:text-dark-500">{{ result.suggestions.length }} 条</span>
          </h3>
          <ol class="space-y-3">
            <li
              v-for="(suggestion, index) in result.suggestions"
              :key="index"
              class="flex items-start gap-3"
            >
              <span class="text-xs font-semibold font-mono text-primary-600 dark:text-primary-400 mt-0.5 flex-shrink-0">
                {{ String(index + 1).padStart(2, '0') }}
              </span>
              <p class="text-sm text-dark-600 dark:text-dark-300 leading-relaxed">{{ suggestion }}</p>
            </li>
          </ol>
        </div>
      </div>

      <!-- Related Code -->
      <div v-if="result.relatedCode.length > 0" class="card dark:card-dark p-6">
        <div class="flex items-baseline justify-between mb-4">
          <h3 class="text-base font-semibold text-dark-900 dark:text-white">相关代码</h3>
          <span class="text-xs text-dark-400 dark:text-dark-500">{{ result.relatedCode.length }} 个片段</span>
        </div>
        <div class="space-y-4">
          <div
            v-for="(snippet, index) in result.relatedCode"
            :key="index"
            class="rounded-lg border border-dark-200 dark:border-dark-800 overflow-hidden"
          >
            <!-- Snippet Header -->
            <div class="flex items-center justify-between bg-dark-900 px-4 py-2.5 border-b border-dark-800">
              <div class="flex items-center gap-2">
                <FileCode class="h-4 w-4 text-dark-400" />
                <span class="font-mono text-xs text-dark-300">{{ snippet.filePath }}</span>
              </div>
              <span class="font-mono text-xs text-dark-500">L{{ snippet.startLine }}-{{ snippet.endLine }}</span>
            </div>
            <!-- Snippet Code -->
            <pre class="overflow-x-auto scrollbar-thin bg-dark-950 p-4 text-xs font-mono text-dark-200 leading-relaxed"><code>{{ snippet.content }}</code></pre>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!result && !isLoading && !error" class="card dark:card-dark p-10 text-center">
      <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-lg bg-primary-50 dark:bg-primary-600/10 mb-4">
        <Stethoscope class="h-6 w-6 text-primary-500" />
      </div>
      <h3 class="text-base font-semibold text-dark-800 dark:text-white mb-1.5">开始你的第一次诊断</h3>
      <p class="text-sm text-dark-500 dark:text-dark-400 max-w-md mx-auto">
        输入错误信息和异常堆栈，系统将基于知识图谱和 RAG 检索技术，为你提供精准的根因分析和修复建议
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { diagnose } from '@/api/diagnosis'
import { listRepositories, type RepositoryConfig } from '@/api/repository'
import type { DiagnosisResponse } from '@/types'
import {
  Loader2, Sparkles, XCircle, FileCode, Stethoscope
} from 'lucide-vue-next'

const query = ref('')
const errorInfo = ref('')
const selectedService = ref('')
const isLoading = ref(false)
const result = ref<DiagnosisResponse | null>(null)
const error = ref('')

const services = ref<RepositoryConfig[]>([])
const servicesLoading = ref(false)

/**
 * 加载已配置的仓库列表作为目标服务选项
 */
async function loadServices() {
  servicesLoading.value = true
  try {
    services.value = (await listRepositories()).filter(r => r.enabled)
  } catch {
    services.value = []
  } finally {
    servicesLoading.value = false
  }
}

/**
 * 执行智能诊断
 * 发送诊断请求，处理响应结果或错误
 */
const handleDiagnose = async () => {
  error.value = ''
  result.value = null
  isLoading.value = true
  try {
    const response = await diagnose({
      query: query.value,
      errorInfo: errorInfo.value,
      service: selectedService.value
    })
    if (response.success && response.data) {
      result.value = response.data
    } else {
      error.value = response.error || '诊断失败'
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '请求失败'
  } finally {
    isLoading.value = false
  }
}

onMounted(loadServices)
</script>
