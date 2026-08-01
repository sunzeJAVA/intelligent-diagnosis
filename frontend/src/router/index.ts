import { createRouter, createWebHistory } from 'vue-router'
import DiagnosisView from '@/views/DiagnosisView.vue'
import ApprovalsView from '@/views/ApprovalsView.vue'
import WorkflowsView from '@/views/WorkflowsView.vue'
import RepositoriesView from '@/views/RepositoriesView.vue'
import AdminView from '@/views/AdminView.vue'
import SnapshotsView from '@/views/SnapshotsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'diagnosis', component: DiagnosisView },
    { path: '/repositories', name: 'repositories', component: RepositoriesView },
    { path: '/approvals', name: 'approvals', component: ApprovalsView },
    { path: '/workflows', name: 'workflows', component: WorkflowsView },
    { path: '/snapshots', name: 'snapshots', component: SnapshotsView },
    { path: '/admin', name: 'admin', component: AdminView }
  ]
})

export default router
