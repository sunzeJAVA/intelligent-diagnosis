<template>
  <div class="min-h-screen flex items-center justify-center bg-dark-950 px-4">
    <div class="w-full max-w-md bg-dark-900 rounded-xl border border-dark-800 p-8 animate-fade-in">
      <div class="flex items-center justify-center gap-2.5 mb-8">
        <Stethoscope class="h-6 w-6 text-primary-500" />
        <h1 class="text-lg font-semibold text-white tracking-tight">智能诊断</h1>
      </div>

      <div class="mb-6">
        <h2 class="text-xl font-semibold text-white mb-1">欢迎回来</h2>
        <p class="text-sm text-dark-400">请登录以继续使用系统</p>
      </div>

      <form class="space-y-4" @submit.prevent="handleLogin">
        <div>
          <label class="block text-sm font-medium text-dark-300 mb-1.5">用户名</label>
          <input
            v-model="username"
            type="text"
            required
            autocomplete="username"
            class="input-dark"
            placeholder="请输入用户名"
            :disabled="loading"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-dark-300 mb-1.5">密码</label>
          <input
            v-model="password"
            type="password"
            required
            autocomplete="current-password"
            class="input-dark"
            placeholder="请输入密码"
            :disabled="loading"
          />
        </div>

        <button
          type="submit"
          class="btn-primary w-full shadow-none"
          :disabled="loading || !username || !password"
        >
          <Loader2 v-if="loading" class="h-4 w-4 animate-spin" />
          <span>{{ loading ? '登录中...' : '登录' }}</span>
        </button>
      </form>

      <p class="mt-8 text-center text-xs text-dark-500">
        Code Intelligence Platform
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import axios from 'axios'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Stethoscope, Loader2 } from 'lucide-vue-next'
import { controlApi, TOKEN_KEY } from '@/api/client'
import { showToast } from '@/utils/toast'

const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  if (loading.value) return
  loading.value = true

  try {
    const response = await controlApi.post('/auth/login', {
      username: username.value,
      password: password.value
    })

    const token = response.data?.token
    if (!token) {
      showToast('登录失败：未返回 token', 'error')
      return
    }

    localStorage.setItem(TOKEN_KEY, token)
    showToast('登录成功', 'success')
    await router.replace('/')
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const message = error.response?.data?.message || error.response?.data?.error || '登录失败'
      showToast(message, 'error')
    } else {
      showToast('登录失败', 'error')
    }
  } finally {
    loading.value = false
  }
}
</script>
