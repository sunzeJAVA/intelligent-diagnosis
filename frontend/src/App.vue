<template>
  <div class="app">
    <header class="app-header">
      <h1>智能代码诊断系统</h1>
      <nav>
        <router-link to="/">诊断</router-link>
        <router-link to="/repositories">仓库</router-link>
        <router-link to="/approvals">审批</router-link>
        <router-link to="/workflows">工作流</router-link>
        <router-link to="/admin">管理</router-link>
      </nav>
    </header>
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
// App root
</script>

<style scoped>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  padding: 1rem 2rem;
  background: #1a1a2e;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.app-header nav {
  display: flex;
  gap: 1.5rem;
}

.app-header a {
  color: #e0e0e0;
  text-decoration: none;
}

.app-header a.router-link-active {
  color: #4fd1c5;
}

.app-main {
  flex: 1;
  padding: 2rem;
  background: #f5f5f5;
}
</style>
