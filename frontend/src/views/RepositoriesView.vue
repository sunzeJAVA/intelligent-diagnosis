<template>
  <div class="repositories-view">
    <h1>仓库管理</h1>

    <section class="add-section">
      <h2>添加仓库</h2>
      <form @submit.prevent="handleCreate" class="repo-form">
        <div class="form-row">
          <div class="form-group">
            <label>名称 *</label>
            <input v-model="form.name" required placeholder="my-service" />
          </div>
          <div class="form-group">
            <label>显示名称</label>
            <input v-model="form.displayName" placeholder="My Service" />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>类型 *</label>
            <select v-model="form.type" required>
              <option value="GIT">通用 Git</option>
              <option value="GITHUB">GitHub</option>
              <option value="GITLAB">GitLab</option>
              <option value="GITEE">Gitee</option>
              <option value="BITBUCKET">Bitbucket</option>
              <option value="LOCAL">本地仓库</option>
            </select>
          </div>
          <div class="form-group">
            <label>分支 *</label>
            <input v-model="form.branch" required placeholder="main" />
          </div>
        </div>

        <div class="form-group">
          <label>Git URL *</label>
          <input v-model="form.url" required placeholder="https://github.com/company/repo.git" />
        </div>

        <div class="form-group">
          <label>本地缓存路径 *</label>
          <input v-model="form.localPath" required placeholder="/tmp/repos/my-service" />
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>认证方式</label>
            <select v-model="form.authType">
              <option value="NONE">无认证</option>
              <option value="TOKEN">Token</option>
              <option value="USERNAME_PASSWORD">用户名密码</option>
              <option value="SSH_KEY">SSH Key</option>
            </select>
          </div>
          <div class="form-group">
            <label>启用</label>
            <input type="checkbox" v-model="form.enabled" />
          </div>
        </div>

        <div v-if="form.authType === 'TOKEN'" class="form-group">
          <label>Token</label>
          <input v-model="form.authToken" type="password" />
        </div>

        <div v-if="form.authType === 'USERNAME_PASSWORD'" class="form-row">
          <div class="form-group">
            <label>用户名</label>
            <input v-model="form.authUsername" />
          </div>
          <div class="form-group">
            <label>密码</label>
            <input v-model="form.authPassword" type="password" />
          </div>
        </div>

        <div v-if="form.authType === 'SSH_KEY'" class="form-group">
          <label>SSH Key 路径</label>
          <input v-model="form.authSshKeyPath" placeholder="/Users/xxx/.ssh/id_rsa" />
        </div>

        <button type="submit" :disabled="creating">{{ creating ? '创建中...' : '创建仓库' }}</button>
      </form>
    </section>

    <section class="list-section">
      <h2>仓库列表</h2>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="repositories.length === 0" class="empty">暂无仓库配置</div>
      <div v-else class="repo-list">
        <div v-for="repo in repositories" :key="repo.id" class="repo-card">
          <div class="repo-header">
            <div class="repo-title">
              <strong>{{ repo.displayName || repo.name }}</strong>
              <span class="repo-type">{{ repo.type }}</span>
              <span :class="['status-badge', repo.enabled ? 'enabled' : 'disabled']">
                {{ repo.enabled ? '启用' : '禁用' }}
              </span>
            </div>
            <div class="repo-actions">
              <button @click="syncRepo(repo.id!)" :disabled="syncingIds.has(repo.id!)">
                {{ syncingIds.has(repo.id!) ? '同步中' : '同步' }}
              </button>
              <button @click="deleteRepo(repo.id!)" class="danger">删除</button>
            </div>
          </div>

          <div class="repo-info">
            <p><strong>URL:</strong> {{ repo.url }}</p>
            <p><strong>分支:</strong> {{ repo.branch }}</p>
            <p><strong>本地路径:</strong> {{ repo.localPath }}</p>
            <p><strong>认证:</strong> {{ repo.authType }}</p>
          </div>

          <div v-if="syncHistory[repo.id!]?.length" class="sync-history">
            <h4>同步历史</h4>
            <div v-for="state in syncHistory[repo.id!].slice(0, 3)" :key="state.id" class="sync-item"
              :class="state.status.toLowerCase()">
              <span class="sync-status">{{ state.status }}</span>
              <span class="sync-time">{{ formatTime(state.createdAt) }}</span>
              <span v-if="state.latestCommit" class="sync-commit">{{ state.latestCommit.substring(0, 8) }}</span>
              <span v-if="state.changedFiles !== undefined" class="sync-files">{{ state.changedFiles }} 文件</span>
              <span v-if="state.errorMessage" class="sync-error">{{ state.errorMessage }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import {
  listRepositories,
  createRepository,
  syncRepository,
  deleteRepository,
  getSyncHistory,
  type RepositoryConfig,
  type SyncState
} from '@/api/repository'

const repositories = ref<RepositoryConfig[]>([])
const loading = ref(false)
const creating = ref(false)
const syncingIds = ref<Set<string>>(new Set())
const syncHistory = ref<Record<string, SyncState[]>>({})

const form = reactive<RepositoryConfig>({
  name: '',
  displayName: '',
  type: 'GIT',
  url: '',
  branch: 'main',
  localPath: '',
  enabled: true,
  authType: 'NONE',
  authToken: '',
  authUsername: '',
  authPassword: '',
  authSshKeyPath: ''
})

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

async function handleCreate() {
  creating.value = true
  try {
    await createRepository({ ...form })
    resetForm()
    await loadRepositories()
  } finally {
    creating.value = false
  }
}

async function syncRepo(id: string) {
  syncingIds.value.add(id)
  try {
    await syncRepository(id)
    syncHistory.value[id] = await getSyncHistory(id)
  } finally {
    syncingIds.value.delete(id)
  }
}

async function deleteRepo(id: string) {
  if (!confirm('确定删除这个仓库配置吗？')) return
  await deleteRepository(id)
  await loadRepositories()
}

function resetForm() {
  form.name = ''
  form.displayName = ''
  form.type = 'GIT'
  form.url = ''
  form.branch = 'main'
  form.localPath = ''
  form.enabled = true
  form.authType = 'NONE'
  form.authToken = ''
  form.authUsername = ''
  form.authPassword = ''
  form.authSshKeyPath = ''
}

function formatTime(time?: string): string {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(loadRepositories)
</script>

<style scoped>
.repositories-view {
  max-width: 1200px;
  margin: 0 auto;
}

h1 {
  margin-bottom: 1.5rem;
  color: #1a1a2e;
}

h2 {
  margin-bottom: 1rem;
  color: #333;
  font-size: 1.2rem;
}

.add-section,
.list-section {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  margin-bottom: 1.5rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.repo-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-group label {
  font-weight: 500;
  color: #555;
  font-size: 0.9rem;
}

.form-group input,
.form-group select {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.95rem;
}

.form-group input[type="checkbox"] {
  width: 20px;
  height: 20px;
}

button {
  padding: 0.6rem 1.2rem;
  background: #4fd1c5;
  color: #1a1a2e;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
}

button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

button.danger {
  background: #fc8181;
  color: white;
}

.repo-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.repo-card {
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 1rem;
}

.repo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.repo-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.repo-type {
  background: #edf2f7;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  color: #4a5568;
}

.status-badge {
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
}

.status-badge.enabled {
  background: #c6f6d5;
  color: #22543d;
}

.status-badge.disabled {
  background: #fed7d7;
  color: #742a2a;
}

.repo-actions {
  display: flex;
  gap: 0.5rem;
}

.repo-info p {
  margin: 0.25rem 0;
  color: #4a5568;
  font-size: 0.9rem;
}

.sync-history {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #e2e8f0;
}

.sync-history h4 {
  margin-bottom: 0.5rem;
  color: #4a5568;
  font-size: 0.9rem;
}

.sync-item {
  display: flex;
  gap: 1rem;
  align-items: center;
  padding: 0.5rem;
  border-radius: 4px;
  font-size: 0.85rem;
  margin-bottom: 0.25rem;
}

.sync-item.success {
  background: #f0fff4;
}

.sync-item.failed {
  background: #fff5f5;
}

.sync-item.syncing {
  background: #ebf8ff;
}

.sync-status {
  font-weight: 500;
  min-width: 70px;
}

.sync-error {
  color: #e53e3e;
  flex: 1;
}

.loading,
.empty {
  text-align: center;
  padding: 2rem;
  color: #718096;
}
</style>
