<template>
  <div class="w-full min-h-screen bg-dark-50 dark:bg-dark-900 transition-colors duration-300">
    <!-- Desktop Sidebar -->
    <aside class="hidden lg:flex fixed left-0 top-0 z-40 h-screen w-60 flex-col bg-dark-950 border-r border-dark-800/60">
      <div class="flex h-12 items-center gap-2.5 px-4 border-b border-dark-800/60">
        <Stethoscope class="h-5 w-5 text-primary-400" />
        <span class="text-[13px] font-semibold tracking-tight text-white">智能诊断</span>
      </div>

      <nav class="flex-1 space-y-0.5 overflow-y-auto px-2 py-3">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="flex items-center gap-2.5 rounded-md px-2.5 py-1.5 text-[13px] font-medium transition-colors"
          :class="isActive(item.path)
            ? 'bg-primary-500/10 text-primary-400'
            : 'text-dark-400 hover:bg-dark-800/60 hover:text-white'"
        >
          <component
            :is="item.icon"
            class="h-4 w-4 flex-shrink-0"
            :class="isActive(item.path) ? 'text-primary-400' : 'text-dark-500'"
          />
          <span class="truncate">{{ item.label }}</span>
          <span
            v-if="item.badge"
            class="ml-auto rounded-full bg-primary-500/15 px-1.5 py-0.5 text-[11px] font-medium text-primary-300"
          >
            {{ item.badge }}
          </span>
        </router-link>
      </nav>

      <div class="border-t border-dark-800/60 px-2 py-2.5">
        <div class="flex items-center gap-2.5 px-2 py-1">
          <div class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-dark-800 text-[11px] font-semibold text-dark-200">
            {{ initials }}
          </div>
          <div class="min-w-0 flex-1">
            <p class="truncate text-[13px] font-medium text-white">{{ user?.username ?? '未登录' }}</p>
          </div>
          <button
            type="button"
            class="text-dark-500 transition-colors hover:text-white"
            title="退出登录"
            @click="handleLogout"
          >
            <LogOut class="h-4 w-4" />
          </button>
        </div>
      </div>
    </aside>

    <!-- Mobile Sidebar -->
    <aside
      class="fixed inset-y-0 left-0 z-50 flex w-60 flex-col transform bg-dark-950 border-r border-dark-800/60 transition-transform duration-300 lg:hidden"
      :class="sidebarOpen ? 'translate-x-0' : '-translate-x-full'"
    >
      <div class="flex h-12 items-center gap-2.5 px-4 border-b border-dark-800/60">
        <Stethoscope class="h-5 w-5 text-primary-400" />
        <span class="text-[13px] font-semibold tracking-tight text-white">智能诊断</span>
      </div>

      <nav class="flex-1 space-y-0.5 overflow-y-auto px-2 py-3">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="flex items-center gap-2.5 rounded-md px-2.5 py-1.5 text-[13px] font-medium transition-colors"
          :class="isActive(item.path)
            ? 'bg-primary-500/10 text-primary-400'
            : 'text-dark-400 hover:bg-dark-800/60 hover:text-white'"
          @click="sidebarOpen = false"
        >
          <component
            :is="item.icon"
            class="h-4 w-4 flex-shrink-0"
            :class="isActive(item.path) ? 'text-primary-400' : 'text-dark-500'"
          />
          <span class="truncate">{{ item.label }}</span>
          <span
            v-if="item.badge"
            class="ml-auto rounded-full bg-primary-500/15 px-1.5 py-0.5 text-[11px] font-medium text-primary-300"
          >
            {{ item.badge }}
          </span>
        </router-link>
      </nav>

      <div class="border-t border-dark-800/60 px-2 py-2.5">
        <div class="flex items-center gap-2.5 px-2 py-1">
          <div class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-dark-800 text-[11px] font-semibold text-dark-200">
            {{ initials }}
          </div>
          <div class="min-w-0 flex-1">
            <p class="truncate text-[13px] font-medium text-white">{{ user?.username ?? '未登录' }}</p>
          </div>
          <button
            type="button"
            class="text-dark-500 transition-colors hover:text-white"
            title="退出登录"
            @click="handleLogout"
          >
            <LogOut class="h-4 w-4" />
          </button>
        </div>
      </div>
    </aside>

    <div
      v-if="sidebarOpen"
      class="fixed inset-0 z-40 bg-black/50 lg:hidden"
      @click="sidebarOpen = false"
    ></div>

    <!-- Header -->
    <header class="fixed left-0 top-0 z-30 flex h-12 w-full items-center justify-between border-b border-dark-200/70 bg-dark-50 px-4 transition-colors duration-300 dark:border-dark-800 dark:bg-dark-900 sm:px-6 lg:pl-60">
      <div class="flex items-center gap-3">
        <button
          type="button"
          class="text-dark-600 transition-colors hover:text-dark-900 dark:text-dark-400 dark:hover:text-white lg:hidden"
          @click="sidebarOpen = true"
        >
          <Menu class="h-5 w-5" />
        </button>
        <span class="text-[13px] font-semibold tracking-tight text-dark-900 dark:text-white lg:hidden">智能诊断</span>
      </div>

      <button
        type="button"
        class="rounded-md p-1.5 text-dark-500 transition-colors hover:bg-dark-100 hover:text-dark-900 dark:text-dark-400 dark:hover:bg-dark-800 dark:hover:text-white"
        title="切换主题"
        @click="cycleTheme"
      >
        <Moon v-if="theme === 'light'" class="h-4 w-4" />
        <Sun v-else-if="theme === 'dark'" class="h-4 w-4" />
        <Monitor v-else class="h-4 w-4" />
      </button>
    </header>

    <!-- Main Content -->
    <main class="min-h-screen overflow-x-hidden pt-12 lg:ml-60">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Stethoscope, Menu, Sun, Moon, Monitor,
  Activity, GitBranch, ShieldCheck, Workflow, LayoutDashboard, Camera, LogOut,
} from 'lucide-vue-next'
import { useTheme } from '@/composables/useTheme'
import { useAuth } from '@/composables/useAuth'
import { useSystemHealth } from '@/composables/useSystemHealth'
import { TOKEN_KEY } from '@/api/client'

const route = useRoute()
const router = useRouter()
const sidebarOpen = ref(false)
const { theme, cycleTheme } = useTheme()
const { user, initials, loadUser, clearUser, error: authError } = useAuth()
// 保留轮询逻辑：useSystemHealth 在挂载时启动 /system/health 轮询，
// 其模块级状态由 AdminView 消费，侧边栏不再展示状态卡片。
useSystemHealth()

const navItems = [
  { path: '/', label: '智能诊断', icon: Activity },
  { path: '/repositories', label: '仓库管理', icon: GitBranch },
  { path: '/approvals', label: '审批工作台', icon: ShieldCheck, badge: '3' },
  { path: '/workflows', label: '工作流监控', icon: Workflow },
  { path: '/snapshots', label: '快照管理', icon: Camera },
  { path: '/admin', label: '系统管理', icon: LayoutDashboard },
]

const isActive = (path: string) => route.path === path

function handleLogout() {
  localStorage.removeItem(TOKEN_KEY)
  clearUser()
  router.replace('/login')
}

onMounted(async () => {
  await loadUser()
  if (authError.value) {
    localStorage.removeItem(TOKEN_KEY)
    router.replace('/login')
  }
})
</script>

<style>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
