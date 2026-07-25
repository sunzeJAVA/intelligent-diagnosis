<template>
  <div class="w-full p-4 sm:p-6 lg:p-8 space-y-8">
    <!-- Header -->
    <div>
      <h1 class="text-3xl font-bold text-dark-900 dark:text-white">智能代码诊断</h1>
      <p class="mt-2 text-dark-500 dark:text-dark-400">输入错误信息或异常堆栈，AI 将基于知识图谱和向量检索为您定位根因</p>
      <div class="flex items-center gap-2 mt-4">
        <span class="badge badge-primary dark:badge-primary-dark">AI 驱动</span>
        <span class="badge badge-info dark:badge-info-dark">RAG 检索</span>
        <span class="badge badge-success dark:badge-success-dark">知识图谱</span>
      </div>
    </div>

    <!-- Input Card -->
    <div class="card dark:card-dark w-full p-8 overflow-x-hidden">
      <div class="grid grid-cols-1 xl:grid-cols-12 gap-6">
        <!-- Left Column - Main Input -->
        <div class="xl:col-span-8">
          <label class="flex items-center gap-3 text-sm font-semibold text-dark-700 dark:text-dark-200 mb-4">
            <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-primary-100 dark:bg-primary-600/20">
              <AlertCircle class="h-4 w-4 text-primary-600" />
            </div>
            问题描述 / 错误信息
            <span class="ml-auto text-xs font-normal text-dark-400 dark:text-dark-500">* 必填</span>
          </label>
          <textarea
            v-model="query"
            rows="8"
            class="input dark:input-dark resize-none font-mono text-sm w-full"
            placeholder="例如：NullPointerException at com.example.service.UserService.findById(UserService.java:45)&#10;&#10;请详细描述您遇到的问题，包括错误发生的场景和具体表现..."
          ></textarea>
        </div>

        <!-- Right Column - Side Panel -->
        <div class="xl:col-span-4 space-y-6">
          <!-- Error Info -->
          <div>
            <label class="flex items-center gap-3 text-sm font-semibold text-dark-700 dark:text-dark-200 mb-4">
              <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-dark-100 dark:bg-dark-800">
                <FileWarning class="h-4 w-4 text-dark-500 dark:text-dark-400" />
              </div>
              异常堆栈 / 上下文
              <span class="text-xs font-normal text-dark-400 dark:text-dark-500">（可选）</span>
            </label>
            <textarea
              v-model="errorInfo"
              rows="4"
              class="input dark:input-dark resize-none font-mono text-sm w-full"
              placeholder="粘贴完整的异常堆栈..."
            ></textarea>
          </div>

          <!-- Service Selection -->
          <div>
            <label class="flex items-center gap-3 text-sm font-semibold text-dark-700 dark:text-dark-200 mb-4">
              <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-dark-100 dark:bg-dark-800">
                <Server class="h-4 w-4 text-dark-500 dark:text-dark-400" />
              </div>
              目标服务
              <span class="text-xs font-normal text-dark-400 dark:text-dark-500">* 必填</span>
            </label>
            <select v-model="selectedService" class="select dark:select-dark w-full">
              <option value="">选择服务...</option>
              <option value="payment-service">payment-service</option>
              <option value="user-service">user-service</option>
              <option value="order-service">order-service</option>
              <option value="skykiwi-news-server">skykiwi-news-server</option>
            </select>
          </div>

          <!-- Diagnose Button -->
          <button
            @click="handleDiagnose"
            :disabled="isLoading || !selectedService || !query"
            class="btn-primary w-full py-3 text-sm font-semibold mt-2"
          >
            <Loader2 v-if="isLoading" class="h-4 w-4 animate-spin" />
            <Sparkles v-else class="h-4 w-4" />
            {{ isLoading ? '诊断中...' : '开始诊断' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Error Message -->
    <div v-if="error" class="card dark:card-dark border-red-200 dark:border-red-900/50 bg-red-50 dark:bg-red-900/10 p-6">
      <div class="flex items-start gap-4">
        <div class="flex h-10 w-10 items-center justify-center rounded-full bg-red-100 dark:bg-red-600/20 flex-shrink-0">
          <XCircle class="h-5 w-5 text-red-600" />
        </div>
        <div>
          <p class="text-base font-semibold text-red-800 dark:text-red-400">诊断失败</p>
          <p class="text-sm text-red-600 dark:text-red-400 mt-1">{{ error }}</p>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="space-y-6">
      <div class="card dark:card-dark p-8">
        <div class="flex items-center gap-4 mb-6">
          <div class="skeleton dark:skeleton-dark h-10 w-10 rounded-lg"></div>
          <div>
            <div class="skeleton dark:skeleton-dark h-5 w-40 mb-1"></div>
            <div class="skeleton dark:skeleton-dark h-3 w-24"></div>
          </div>
        </div>
        <div class="space-y-3">
          <div class="skeleton dark:skeleton-dark h-4 w-full"></div>
          <div class="skeleton dark:skeleton-dark h-4 w-5/6"></div>
          <div class="skeleton dark:skeleton-dark h-4 w-4/6"></div>
        </div>
      </div>
      <div class="card dark:card-dark p-8">
        <div class="skeleton dark:skeleton-dark h-5 w-32 mb-6"></div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="skeleton dark:skeleton-dark h-32 rounded-lg"></div>
          <div class="skeleton dark:skeleton-dark h-32 rounded-lg"></div>
        </div>
      </div>
    </div>

    <!-- Results -->
    <div v-if="result && !isLoading" class="space-y-8">
      <!-- Summary -->
      <div class="card dark:card-dark p-8 border-l-4 border-l-primary-500">
        <div class="flex items-start gap-4 mb-4">
          <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-100 dark:bg-primary-600/20 flex-shrink-0">
            <Lightbulb class="h-5 w-5 text-primary-600" />
          </div>
          <div>
            <h3 class="text-lg font-semibold text-dark-900 dark:text-white">诊断摘要</h3>
            <p class="text-sm text-dark-500 dark:text-dark-400 mt-0.5">AI 生成的问题概览</p>
          </div>
        </div>
        <p class="text-dark-600 dark:text-dark-300 leading-relaxed text-base">{{ result.summary }}</p>
      </div>

      <!-- Root Cause & Suggestions Grid -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Root Cause -->
        <div class="card dark:card-dark p-8 border-l-4 border-l-amber-500">
          <div class="flex items-start gap-4 mb-4">
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-amber-100 dark:bg-amber-600/20 flex-shrink-0">
              <Target class="h-5 w-5 text-amber-600" />
            </div>
            <div>
              <h3 class="text-lg font-semibold text-dark-900 dark:text-white">根因分析</h3>
              <p class="text-sm text-dark-500 dark:text-dark-400 mt-0.5">问题的根本原因</p>
            </div>
          </div>
          <div class="rounded-lg bg-amber-50 dark:bg-amber-600/10 p-5">
            <p class="text-dark-700 dark:text-dark-300 leading-relaxed text-base">{{ result.rootCause }}</p>
          </div>
        </div>

        <!-- Suggestions -->
        <div class="card dark:card-dark p-8">
          <div class="flex items-start gap-4 mb-5">
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-green-100 dark:bg-green-600/20 flex-shrink-0">
              <CheckCircle2 class="h-5 w-5 text-green-600" />
            </div>
            <div>
              <h3 class="text-lg font-semibold text-dark-900 dark:text-white">修复建议</h3>
              <p class="text-sm text-dark-500 dark:text-dark-400 mt-0.5">{{ result.suggestions.length }} 条建议</p>
            </div>
          </div>
          <ol class="space-y-4">
            <li
              v-for="(suggestion, index) in result.suggestions"
              :key="index"
              class="flex items-start gap-4"
            >
              <span class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-green-100 dark:bg-green-600/20 text-sm font-semibold text-green-700 dark:text-green-400">
                {{ index + 1 }}
              </span>
              <p class="text-dark-600 dark:text-dark-300 leading-relaxed pt-0.5">{{ suggestion }}</p>
            </li>
          </ol>
        </div>
      </div>

      <!-- Related Code -->
      <div v-if="result.relatedCode.length > 0" class="card dark:card-dark p-8">
        <div class="flex items-center gap-4 mb-6">
          <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-100 dark:bg-blue-600/20 flex-shrink-0">
            <Code2 class="h-5 w-5 text-blue-600" />
          </div>
          <div>
            <h3 class="text-lg font-semibold text-dark-900 dark:text-white">相关代码</h3>
            <p class="text-sm text-dark-500 dark:text-dark-400 mt-0.5">{{ result.relatedCode.length }} 个相关代码片段</p>
          </div>
        </div>
        <div class="space-y-6">
          <div
            v-for="(snippet, index) in result.relatedCode"
            :key="index"
            class="rounded-xl border border-dark-200 dark:border-dark-700 overflow-hidden shadow-sm"
          >
            <!-- Snippet Header -->
            <div class="flex items-center justify-between bg-dark-800 px-5 py-3">
              <div class="flex items-center gap-2">
                <FileCode class="h-4 w-4 text-dark-300" />
                <span class="font-mono text-sm text-dark-200">{{ snippet.filePath }}</span>
              </div>
              <span class="font-mono text-xs text-dark-400 bg-dark-900 px-2 py-1 rounded">L{{ snippet.startLine }}-{{ snippet.endLine }}</span>
            </div>
            <!-- Snippet Code -->
            <pre class="overflow-x-auto scrollbar-thin bg-dark-900 p-5 text-sm font-mono text-dark-200 leading-relaxed"><code>{{ snippet.content }}</code></pre>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!result && !isLoading && !error" class="card dark:card-dark p-16 text-center">
      <div class="mx-auto flex h-24 w-24 items-center justify-center rounded-full bg-gradient-to-br from-primary-50 dark:from-primary-600/20 to-primary-100 dark:to-primary-600/10 mb-8">
        <Stethoscope class="h-12 w-12 text-primary-500" />
      </div>
      <h3 class="text-2xl font-bold text-dark-800 dark:text-white mb-3">开始你的第一次诊断</h3>
      <p class="text-dark-500 dark:text-dark-400 max-w-xl mx-auto text-base">
        输入错误信息和异常堆栈，系统将基于知识图谱和 RAG 检索技术，为你提供精准的根因分析和修复建议
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { diagnose } from '@/api/diagnosis'
import type { DiagnosisResponse } from '@/types'
import {
  AlertCircle, FileWarning, Server, Loader2, Sparkles, XCircle,
  Lightbulb, Target, CheckCircle2, Code2, FileCode, Stethoscope
} from 'lucide-vue-next'

const query = ref('')
const errorInfo = ref('')
const selectedService = ref('')
const isLoading = ref(false)
const result = ref<DiagnosisResponse | null>(null)
const error = ref('')

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
</script>
