<template>
  <div class="admin-view">
    <h2>系统管理</h2>
    <div class="admin-grid">
      <div class="admin-card">
        <h3>策略管理</h3>
        <p>配置 OPA/Rego 策略规则</p>
      </div>
      <div class="admin-card">
        <h3>审计查询</h3>
        <p>查询操作审计记录</p>
      </div>
      <div class="admin-card">
        <h3>用户权限</h3>
        <p>管理 RBAC 角色与权限</p>
      </div>
      <div class="admin-card">
        <h3>索引快照</h3>
        <p>查看与管理索引快照</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 系统管理
</script>

<style scoped>
.admin-view {
  max-width: 1200px;
}

.admin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
}

.admin-card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
}

.admin-card h3 {
  margin-top: 0;
}
</style>
