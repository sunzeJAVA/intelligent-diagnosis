<template>
  <div class="workflows-view">
    <h2>工作流状态</h2>
    <table class="workflow-table">
      <thead>
        <tr>
          <th>工作流ID</th>
          <th>类型</th>
          <th>状态</th>
          <th>当前步骤</th>
          <th>开始时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="i in 5" :key="i">
          <td>wf-{{ i }}</td>
          <td>INDEX_UPDATE</td>
          <td><span class="status running">运行中</span></td>
          <td>CANARY_VERIFY</td>
          <td>2026-07-16 10:00:00</td>
          <td>
            <button>暂停</button>
            <button>回滚</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
// 工作流状态监控
</script>

<style scoped>
.workflows-view {
  max-width: 1200px;
}

.workflow-table {
  width: 100%;
  background: white;
  border-radius: 8px;
  border-collapse: collapse;
}

.workflow-table th,
.workflow-table td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.status {
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.875rem;
}

.status.running {
  background: #fef3c7;
  color: #92400e;
}

button {
  margin-right: 0.5rem;
  padding: 0.25rem 0.75rem;
}
</style>
