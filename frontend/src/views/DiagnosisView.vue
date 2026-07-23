<template>
  <div class="diagnosis-view">
    <h2>代码诊断</h2>
    <div class="search-box">
      <textarea
        v-model="query"
        rows="4"
        placeholder="输入错误信息、异常堆栈或代码片段..."
      />
      <textarea
        v-model="errorInfo"
        rows="3"
        placeholder="补充错误堆栈或上下文（可选）"
      />
      <div class="actions">
        <select v-model="selectedService">
          <option value="">选择服务</option>
          <option value="payment-service">payment-service</option>
          <option value="user-service">user-service</option>
          <option value="order-service">order-service</option>
          <option value="skykiwi-news-server">skykiwi-news-server</option>
        </select>
        <button @click="handleDiagnose" :disabled="isLoading || !selectedService">
          {{ isLoading ? '诊断中...' : '开始诊断' }}
        </button>
      </div>
    </div>

    <div v-if="error" class="error">
      {{ error }}
    </div>

    <div v-if="result" class="result">
      <h3>诊断结果</h3>
      <section>
        <h4>摘要</h4>
        <p>{{ result.summary }}</p>
      </section>
      <section>
        <h4>根因</h4>
        <p>{{ result.rootCause }}</p>
      </section>
      <section>
        <h4>建议</h4>
        <ol>
          <li v-for="(suggestion, index) in result.suggestions" :key="index">
            {{ suggestion }}
          </li>
        </ol>
      </section>
      <section v-if="result.relatedCode.length > 0">
        <h4>相关代码</h4>
        <div v-for="(snippet, index) in result.relatedCode" :key="index" class="snippet">
          <div class="snippet-header">
            {{ snippet.filePath }}:{{ snippet.startLine }}-{{ snippet.endLine }}
          </div>
          <pre>{{ snippet.content }}</pre>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { diagnose } from '@/api/diagnosis'
import type { DiagnosisResponse } from '@/types'

const query = ref('')
const errorInfo = ref('')
const selectedService = ref('')
const isLoading = ref(false)
const result = ref<DiagnosisResponse | null>(null)
const error = ref('')

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

<style scoped>
.diagnosis-view {
  max-width: 900px;
}

.search-box {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  margin-bottom: 1.5rem;
}

textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 1rem;
}

.actions {
  display: flex;
  gap: 1rem;
}

select {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

button {
  padding: 0.5rem 1.5rem;
  background: #1a1a2e;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  background: #fff0f0;
  color: #c00;
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1.5rem;
}

.result {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
}

.result section {
  margin-bottom: 1.5rem;
}

.result h4 {
  margin-bottom: 0.5rem;
  color: #1a1a2e;
}

.result p {
  line-height: 1.6;
}

.snippet {
  margin-bottom: 1rem;
  border: 1px solid #eee;
  border-radius: 4px;
  overflow: hidden;
}

.snippet-header {
  background: #f5f5f5;
  padding: 0.5rem 0.75rem;
  font-size: 0.85rem;
  color: #666;
}

.snippet pre {
  margin: 0;
  padding: 0.75rem;
  background: #fafafa;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
