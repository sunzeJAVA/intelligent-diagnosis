<template>
  <div class="approvals-view">
    <h2>审批工作台</h2>
    <div class="filters">
      <button class="active">待审批</button>
      <button>已审批</button>
      <button>已拒绝</button>
    </div>
    <div class="approval-list">
      <div class="approval-card" v-for="i in 3" :key="i">
        <h3>代码变更审批 #{{ i }}</h3>
        <p>仓库: payment-service</p>
        <p>提交: abc1234</p>
        <p>风险等级: 高</p>
        <div class="actions">
          <button class="approve">批准</button>
          <button class="reject">拒绝</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 审批工作台
</script>

<style scoped>
.approvals-view {
  max-width: 900px;
}

.filters {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.filters button {
  padding: 0.5rem 1rem;
  border: 1px solid #ddd;
  background: white;
  cursor: pointer;
}

.filters button.active {
  background: #1a1a2e;
  color: white;
}

.approval-card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  margin-bottom: 1rem;
}

.approval-card h3 {
  margin-top: 0;
}

.approval-card .actions {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
}

.approve {
  background: #38a169;
}

.reject {
  background: #e53e3e;
}
</style>
