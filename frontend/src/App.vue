<template>
  <div class="w-full min-h-screen bg-dark-50 dark:bg-dark-900 transition-colors duration-300">
    <!-- Desktop Sidebar -->
    <aside class="fixed left-0 top-0 z-40 h-screen w-64 flex flex-col bg-dark-900 border-r border-dark-800">
      <div class="flex items-center gap-3 px-5 py-5 border-b border-dark-800">
        <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-primary-400 to-primary-600">
          <Stethoscope class="h-5 w-5 text-white" />
        </div>
        <div>
          <h1 class="text-sm font-semibold text-white">智能诊断</h1>
          <p class="text-xs text-dark-400">Code Intelligence</p>
        </div>
      </div>

      <nav class="flex-1 space-y-1 px-3 py-4 overflow-y-auto">
        <p class="px-3 py-2 text-xs font-semibold uppercase tracking-wider text-dark-500">核心功能</p>
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-200"
          :class="isActive(item.path) ? 'bg-primary-600/15 text-primary-400' : 'text-dark-300 hover:bg-dark-800 hover:text-white'"
        >
          <component :is="item.icon" class="h-5 w-5 flex-shrink-0" :class="isActive(item.path) ? 'text-primary-400' : 'text-dark-400'" />
          {{ item.label }}
          <span v-if="item.badge" class="ml-auto rounded-full bg-primary-500/20 px-2 py-0.5 text-xs font-medium text-primary-300">
            {{ item.badge }}
          </span>
        </router-link>
      </nav>

      <div class="px-3 py-4 border-t border-dark-800">
        <div class="rounded-lg bg-dark-800/50 p-3">
          <div class="flex items-center justify-between mb-2">
            <span class="text-xs font-medium text-dark-400">系统状态</span>
            <span class="flex items-center gap-1.5">
              <span class="h-1.5 w-1.5 rounded-full bg-green-400 animate-pulse"></span>
              <span class="text-xs text-green-400">运行中</span>
            </span>
          </div>
          <div class="space-y-1.5">
            <div class="flex items-center justify-between text-xs">
              <span class="text-dark-500">Qdrant</span>
              <span class="text-dark-300">就绪</span>
            </div>
            <div class="flex items-center justify-between text-xs">
              <span class="text-dark-500">Neo4j</span>
              <span class="text-dark-300">就绪</span>
            </div>
            <div class="flex items-center justify-between text-xs">
              <span class="text-dark-500">Temporal</span>
              <span class="text-dark-300">就绪</span>
            </div>
          </div>
        </div>
      </div>

      <div class="flex items-center gap-3 px-4 py-3 border-t border-dark-800">
        <div class="flex h-8 w-8 items-center justify-center rounded-full bg-primary-600 text-xs font-semibold text-white">
          ES
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-sm font-medium text-white truncate">Eric Sun</p>
          <p class="text-xs text-dark-500 truncate">管理员</p>
        </div>
        <button class="text-dark-500 hover:text-white transition-colors">
          <Settings class="h-4 w-4" />
        </button>
      </div>
    </aside>

    <!-- Mobile Sidebar -->
    <aside class="fixed inset-y-0 left-0 z-50 w-64 bg-dark-900 transform transition-transform duration-300 lg:hidden" :class="sidebarOpen ? 'translate-x-0' : '-translate-x-full'">
      <div class="flex items-center gap-3 px-5 py-5 border-b border-dark-800">
        <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-primary-400 to-primary-600">
          <Stethoscope class="h-5 w-5 text-white" />
        </div>
        <div>
          <h1 class="text-sm font-semibold text-white">智能诊断</h1>
          <p class="text-xs text-dark-400">Code Intelligence</p>
        </div>
      </div>

      <nav class="flex-1 space-y-1 px-3 py-4 overflow-y-auto">
        <p class="px-3 py-2 text-xs font-semibold uppercase tracking-wider text-dark-500">核心功能</p>
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-200"
          :class="isActive(item.path) ? 'bg-primary-600/15 text-primary-400' : 'text-dark-300 hover:bg-dark-800 hover:text-white'"
          @click="sidebarOpen = false"
        >
          <component :is="item.icon" class="h-5 w-5 flex-shrink-0" :class="isActive(item.path) ? 'text-primary-400' : 'text-dark-400'" />
          {{ item.label }}
        </router-link>
      </nav>

      <div class="flex items-center gap-3 px-4 py-3 border-t border-dark-800">
        <div class="flex h-8 w-8 items-center justify-center rounded-full bg-primary-600 text-xs font-semibold text-white">
          ES
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-sm font-medium text-white truncate">Eric Sun</p>
          <p class="text-xs text-dark-500 truncate">管理员</p>
        </div>
      </div>
    </aside>

    <div v-if="sidebarOpen" class="fixed inset-0 z-40 bg-black/50 lg:hidden" @click="sidebarOpen = false"></div>

    <!-- Header -->
    <header class="fixed top-0 left-0 z-30 h-14 w-full border-b border-dark-200 bg-white dark:border-dark-700 dark:bg-dark-900 px-4 sm:px-6 flex items-center justify-between lg:pl-64 transition-colors duration-300">
      <div class="flex items-center gap-4">
        <button class="lg:hidden text-dark-500 dark:text-dark-400 hover:text-dark-700 dark:hover:text-white p-1.5" @click="sidebarOpen = true">
          <Menu class="h-5 w-5" />
        </button>
        <div class="lg:hidden">
          <h1 class="text-sm font-semibold text-dark-900 dark:text-white">智能诊断</h1>
        </div>
      </div>

      <div class="flex items-center gap-4">
        <button 
            class="rounded-lg p-1.5 text-dark-500 dark:text-dark-400 hover:bg-dark-50 dark:hover:bg-dark-800 hover:text-dark-700 dark:hover:text-white transition-colors relative"
            @click="cycleTheme"
            title="切换主题"
          >
            <Moon v-if="theme === 'light'" class="h-5 w-5" />
            <Sun v-else-if="theme === 'dark'" class="h-5 w-5" />
            <Monitor v-else class="h-5 w-5" />
            <span 
              class="absolute -bottom-6 left-1/2 -translate-x-1/2 text-xs text-dark-400 dark:text-dark-500 whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity"
            >
              {{ theme === 'light' ? '浅色' : theme === 'dark' ? '深色' : '跟随系统' }}
            </span>
          </button>

        <button class="relative rounded-lg p-1.5 text-dark-500 dark:text-dark-400 hover:bg-dark-50 dark:hover:bg-dark-800 hover:text-dark-700 dark:hover:text-white transition-colors">
          <Bell class="h-5 w-5" />
          <span class="absolute top-1 right-1 h-2 w-2 rounded-full bg-red-500"></span>
        </button>

        <div class="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-primary-400 to-primary-600 text-xs font-semibold text-white">
          ES
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="lg:ml-64 pt-14 min-h-screen overflow-x-hidden">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  Stethoscope, Bell, Settings, Menu, Sun, Moon, Monitor,
  Activity, GitBranch, ShieldCheck, Workflow, LayoutDashboard
} from 'lucide-vue-next'
import { useTheme } from '@/composables/useTheme'

const route = useRoute()
const sidebarOpen = ref(false)
const { theme, isDark, cycleTheme } = useTheme()

const navItems = [
  { path: '/', label: '智能诊断', icon: Activity },
  { path: '/repositories', label: '仓库管理', icon: GitBranch },
  { path: '/approvals', label: '审批工作台', icon: ShieldCheck, badge: '3' },
  { path: '/workflows', label: '工作流监控', icon: Workflow },
  { path: '/admin', label: '系统管理', icon: LayoutDashboard },
]

const isActive = (path: string) => route.path === path

onMounted(() => {})
onUnmounted(() => {})
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
