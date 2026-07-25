<template>
  <div class="p-4 sm:p-6 lg:p-8 space-y-6">
      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-dark-900 dark:text-white">仓库管理</h1>
          <p class="text-dark-500 dark:text-dark-400 text-sm mt-1">管理代码仓库配置、同步状态和索引</p>
        </div>
        <button @click="showAddForm = !showAddForm" class="btn-primary">
          <Plus class="h-4 w-4" />
          {{ showAddForm ? '收起' : '添加仓库' }}
        </button>
      </div>

      <!-- Stats -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div class="card dark:card-dark p-4">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-xs text-dark-500 dark:text-dark-400 font-medium">仓库总数</p>
              <p class="text-2xl font-bold text-dark-900 dark:text-white mt-1">{{ repositories.length }}</p>
            </div>
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-50 dark:bg-primary-600/20">
              <GitBranch class="h-5 w-5 text-primary-600" />
            </div>
          </div>
        </div>
        <div class="card dark:card-dark p-4">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-xs text-dark-500 dark:text-dark-400 font-medium">已启用</p>
              <p class="text-2xl font-bold text-green-600 mt-1">{{ enabledCount }}</p>
            </div>
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-green-50 dark:bg-green-600/20">
              <CheckCircle class="h-5 w-5 text-green-600" />
            </div>
          </div>
        </div>
        <div class="card dark:card-dark p-4">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-xs text-dark-500 dark:text-dark-400 font-medium">同步中</p>
              <p class="text-2xl font-bold text-blue-600 mt-1">{{ syncingCount }}</p>
            </div>
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-50 dark:bg-blue-600/20">
              <RefreshCw class="h-5 w-5 text-blue-600 animate-spin" />
            </div>
          </div>
        </div>
        <div class="card dark:card-dark p-4">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-xs text-dark-500 dark:text-dark-400 font-medium">同步失败</p>
              <p class="text-2xl font-bold text-red-600 mt-1">{{ failedCount }}</p>
            </div>
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-red-50 dark:bg-red-600/20">
              <AlertTriangle class="h-5 w-5 text-red-600" />
            </div>
          </div>
        </div>
      </div>

      <!-- Add Form -->
      <transition name="page">
        <div v-if="showAddForm" class="card dark:card-dark p-6 animate-slide-up">
          <h2 class="text-lg font-semibold text-dark-800 dark:text-white mb-4">添加仓库</h2>
          <form @submit.prevent="handleCreate" class="space-y-4">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-dark-700 dark:text-dark-200 mb-1.5">名称 *</label>
                <input v-model="form.name" required placeholder="my-service" class="input dark:input-dark" />
              </div>
              <div>
                <label class="block text-sm font-medium text-dark-700 dark:text-dark-200 mb-1.5">显示名称</label>
                <input v-model="form.displayName" placeholder="My Service" class="input dark:input-dark" />
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-dark-700 dark:text-dark-200 mb-1.5">类型 *</label>
                <select v-model="form.type" required class="select dark:select-dark">
                  <option value="GIT">通用 Git</option>
                  <option value="GITHUB">GitHub</option>
                  <option value="GITLAB">GitLab</option>
                  <option value="GITEE">Gitee</option>
                  <option value="BITBUCKET">Bitbucket</option>
                  <option value="LOCAL">本地仓库</option>
                </select>
              </div>
              <div>
                <label class="block text-sm font-medium text-dark-700 dark:text-dark-200 mb-1.5">分支 *</label>
                <input v-model="form.branch" required placeholder="main" class="input dark:input-dark" />
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-dark-700 dark:text-dark-200 mb-1.5">Git URL *</label>
              <input v-model="form.url" required placeholder="https://github.com/company/repo.git" class="input dark:input-dark font-mono text-sm" />
            </div>

            <div>
              <label class="block text-sm font-medium text-dark-700 dark:text-dark-200 mb-1.5">本地缓存路径 *</label>
              <input v-model="form.localPath" required placeholder="/tmp/repos/my-service" class="input dark:input-dark font-mono text-sm" />
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-dark-700 dark:text-dark-200 mb-1.5">认证方式</label>
                <select v-model="form.authType" class="select dark:select-dark">
                  <option value="NONE">无认证</option>
                  <option value="TOKEN">Token</option>
                  <option value="USERNAME_PASSWORD">用户名密码</option>
                  <option value="SSH_KEY">SSH Key</option>
                </select>
              </div>
              <div class="flex items-end gap-3">
                <label class="flex items-center gap-2 text-sm font-medium text-dark-700 dark:text-dark-200 cursor-pointer">
                  <input type="checkbox" v-model="form.enabled" class="h-4 w-4 rounded border-dark-300 dark:border-dark-600 text-primary-600 focus:ring-primary-500 bg-white dark:bg-dark-800" />
                  启用仓库
                </label>
              </div>
            </div>

            <div v-if="form.authType === 'TOKEN'">
              <label class="block text-sm font-medium text-dark-700 dark:text-dark-200 mb-1.5">Token</label>
              <input v-model="form.authToken" type="password" class="input dark:input-dark" />
            </div>

            <div v-if="form.authType === 'USERNAME_PASSWORD'" class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-dark-700 dark:text-dark-200 mb-1.5">用户名</label>
                <input v-model="form.authUsername" class="input dark:input-dark" />
              </div>
              <div>
                <label class="block text-sm font-medium text-dark-700 dark:text-dark-200 mb-1.5">密码</label>
                <input v-model="form.authPassword" type="password" class="input dark:input-dark" />
              </div>
            </div>

            <div v-if="form.authType === 'SSH_KEY'">
              <label class="block text-sm font-medium text-dark-700 dark:text-dark-200 mb-1.5">SSH Key 路径</label>
              <input v-model="form.authSshKeyPath" placeholder="/Users/xxx/.ssh/id_rsa" class="input dark:input-dark font-mono text-sm" />
            </div>

            <div class="flex gap-3">
              <button type="submit" :disabled="creating" class="btn-primary">
                <Loader2 v-if="creating" class="h-4 w-4 animate-spin" />
                {{ creating ? '创建中...' : '创建仓库' }}
              </button>
              <button type="button" @click="showAddForm = false" class="btn-secondary dark:btn-secondary-dark">取消</button>
            </div>
          </form>
        </div>
      </transition>

      <!-- Repository List -->
      <div v-if="loading" class="flex items-center justify-center py-20">
        <Loader2 class="h-8 w-8 animate-spin text-primary-500" />
      </div>

      <div v-else-if="repositories.length === 0" class="card dark:card-dark p-12 text-center">
        <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-dark-100 dark:bg-dark-800 mb-4">
          <GitBranch class="h-8 w-8 text-dark-400" />
        </div>
        <h3 class="text-lg font-semibold text-dark-700 dark:text-white mb-1">暂无仓库配置</h3>
        <p class="text-dark-400 dark:text-dark-500 text-sm">点击右上角"添加仓库"开始配置</p>
      </div>

      <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div v-for="repo in repositories" :key="repo.id" class="card dark:card-dark p-5 hover:shadow-elevated dark:hover:shadow-dark-elevated transition-shadow duration-300">
          <!-- Header -->
          <div class="flex items-start justify-between mb-3">
            <div class="flex items-center gap-3">
              <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-gradient-to-br from-primary-400 to-primary-600">
                <GitBranch class="h-5 w-5 text-white" />
              </div>
              <div>
                <h3 class="font-semibold text-dark-800 dark:text-white">{{ repo.displayName || repo.name }}</h3>
                <div class="flex items-center gap-2 mt-0.5">
                  <span class="badge badge-info dark:badge-info-dark">{{ repo.type }}</span>
                  <span :class="repo.enabled ? 'badge badge-success dark:badge-success-dark' : 'badge badge-danger dark:badge-danger-dark'">
                    {{ repo.enabled ? '启用' : '禁用' }}
                  </span>
                </div>
              </div>
            </div>
            <div class="flex gap-1">
              <button
                @click="syncRepo(repo.id!)"
                :disabled="syncingIds.has(repo.id!)"
                class="rounded-lg p-2 text-dark-400 dark:text-dark-500 hover:bg-primary-50 dark:hover:bg-primary-600/10 hover:text-primary-600 dark:hover:text-primary-400 transition-colors disabled:opacity-50"
                title="同步"
              >
                <RefreshCw class="h-4 w-4" :class="syncingIds.has(repo.id!) ? 'animate-spin' : ''" />
              </button>
              <button
                @click="deleteRepo(repo.id!)"
                class="rounded-lg p-2 text-dark-400 dark:text-dark-500 hover:bg-red-50 dark:hover:bg-red-600/10 hover:text-red-600 dark:hover:text-red-400 transition-colors"
                title="删除"
              >
                <Trash2 class="h-4 w-4" />
              </button>
            </div>
          </div>

          <!-- Info -->
          <div class="space-y-1.5 text-sm">
            <div class="flex items-center gap-2 text-dark-500 dark:text-dark-400">
              <Link class="h-3.5 w-3.5 flex-shrink-0" />
              <span class="font-mono text-xs truncate">{{ repo.url }}</span>
            </div>
            <div class="flex items-center gap-2 text-dark-500 dark:text-dark-400">
              <GitCommit class="h-3.5 w-3.5 flex-shrink-0" />
              <span class="font-mono text-xs">{{ repo.branch }}</span>
            </div>
            <div class="flex items-center gap-2 text-dark-500 dark:text-dark-400">
              <Folder class="h-3.5 w-3.5 flex-shrink-0" />
              <span class="font-mono text-xs truncate">{{ repo.localPath }}</span>
            </div>
          </div>

          <!-- Sync History -->
          <div v-if="syncHistory[repo.id!]?.length" class="mt-4 pt-4 border-t border-dark-100 dark:border-dark-700">
            <p class="text-xs font-semibold text-dark-500 dark:text-dark-400 uppercase tracking-wide mb-2">同步历史</p>
            <div class="space-y-1.5">
              <div
                v-for="state in syncHistory[repo.id!].slice(0, 3)"
                :key="state.id"
                class="flex items-center gap-3 text-xs rounded-md px-2 py-1.5"
                :class="{
                  'bg-green-50 dark:bg-green-600/10': state.status === 'SUCCESS',
                  'bg-red-50 dark:bg-red-600/10': state.status === 'FAILED',
                  'bg-blue-50 dark:bg-blue-600/10': state.status === 'SYNCING' || state.status === 'PENDING'
                }"
              >
                <span
                  class="flex h-1.5 w-1.5 rounded-full"
                  :class="{
                    'bg-green-500': state.status === 'SUCCESS',
                    'bg-red-500': state.status === 'FAILED',
                    'bg-blue-500 animate-pulse': state.status === 'SYNCING' || state.status === 'PENDING'
                  }"
                ></span>
                <span class="font-medium text-dark-600 dark:text-dark-300 min-w-[60px]">{{ state.status }}</span>
                <span class="text-dark-400 dark:text-dark-500">{{ formatTime(state.createdAt) }}</span>
                <span v-if="state.latestCommit" class="font-mono text-dark-400 dark:text-dark-500">{{ state.latestCommit.substring(0, 8) }}</span>
                <span v-if="state.changedFiles !== undefined" class="text-dark-400 dark:text-dark-500">{{ state.changedFiles }} 文件</span>
                <span v-if="state.errorMessage" class="text-red-500 truncate flex-1">{{ state.errorMessage }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import {
  listRepositories, createRepository, syncRepository,
  deleteRepository, getSyncHistory,
  type RepositoryConfig, type SyncState
} from '@/api/repository'
import {
  Plus, GitBranch, CheckCircle, RefreshCw, AlertTriangle,
  Loader2, Trash2, Link, GitCommit, Folder
} from 'lucide-vue-next'

const repositories = ref<RepositoryConfig[]>([])
const loading = ref(false)
const creating = ref(false)
const showAddForm = ref(false)
const syncingIds = ref<Set<string>>(new Set())
const syncHistory = ref<Record<string, SyncState[]>>({})

const form = reactive<RepositoryConfig>({
  name: '', displayName: '', type: 'GIT', url: '', branch: 'main',
  localPath: '', enabled: true, authType: 'NONE', authToken: '',
  authUsername: '', authPassword: '', authSshKeyPath: ''
})

const enabledCount = computed(() => repositories.value.filter(r => r.enabled).length)
const syncingCount = computed(() => Object.values(syncHistory.value).flat().filter(s => s.status === 'SYNCING').length)
const failedCount = computed(() => Object.values(syncHistory.value).flat().filter(s => s.status === 'FAILED').length)

/**
 * 加载仓库列表及同步历史
 */
async function loadRepositories() {
  loading.value = true
  try {
    repositories.value = await listRepositories()
    for (const repo of repositories.value) {
      if (repo.id) {
        syncHistory.value[repo.id] = await getSyncHistory(repo.id)
      }
    }
  } finally {
    loading.value = false
  }
}

/**
 * 创建新仓库配置
 */
async function handleCreate() {
  creating.value = true
  try {
    await createRepository({ ...form })
    resetForm()
    showAddForm.value = false
    await loadRepositories()
  } finally {
    creating.value = false
  }
}

/**
 * 同步指定仓库
 * @param id - 仓库 ID
 */
async function syncRepo(id: string) {
  syncingIds.value.add(id)
  try {
    await syncRepository(id)
    syncHistory.value[id] = await getSyncHistory(id)
  } finally {
    syncingIds.value.delete(id)
  }
}

/**
 * 删除指定仓库配置（需用户确认）
 * @param id - 仓库 ID
 */
async function deleteRepo(id: string) {
  if (!confirm('确定删除这个仓库配置吗？')) return
  await deleteRepository(id)
  await loadRepositories()
}

/**
 * 重置表单为默认值
 */
function resetForm() {
  Object.assign(form, {
    name: '', displayName: '', type: 'GIT', url: '', branch: 'main',
    localPath: '', enabled: true, authType: 'NONE', authToken: '',
    authUsername: '', authPassword: '', authSshKeyPath: ''
  })
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

onMounted(loadRepositories)
</script>
