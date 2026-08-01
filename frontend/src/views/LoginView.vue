<template>
  <div class="min-h-screen flex items-center justify-center bg-dark-900 px-4">
    <div class="w-full max-w-md card-dark p-8 animate-fade-in">
      <div class="flex items-center justify-center gap-3 mb-8">
        <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-primary-400 to-primary-600">
          <Stethoscope class="h-6 w-6 text-white" />
        </div>
        <div>
          <h1 class="text-xl font-semibold text-white">智能诊断</h1>
          <p class="text-sm text-dark-400">Code Intelligence</p>
        </div>
      </div>

      <h2 class="text-2xl font-bold text-white mb-2">欢迎回来</h2>
      <p class="text-dark-400 mb-6">请登录以继续使用系统</p>

      <form class="space-y-5" @submit.prevent="handleLogin">
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
          class="btn-primary w-full"
          :disabled="loading || !username || !password"
        >
          <Loader2 v-if="loading" class="h-4 w-4 animate-spin" />
          <span>{{ loading ? '登录中...' : '登录' }}</span>
        </button>
      </form>

      <p class="mt-6 text-center text-xs text-dark-500">
        默认账号：admin / operator / viewer<br />
        密码与用户名相同
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
