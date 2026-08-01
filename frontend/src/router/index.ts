import { createRouter, createWebHistory } from 'vue-router'
import DiagnosisView from '@/views/DiagnosisView.vue'
import ApprovalsView from '@/views/ApprovalsView.vue'
import WorkflowsView from '@/views/WorkflowsView.vue'
import RepositoriesView from '@/views/RepositoriesView.vue'
import AdminView from '@/views/AdminView.vue'
import SnapshotsView from '@/views/SnapshotsView.vue'
import LoginView from '@/views/LoginView.vue'

const TOKEN_KEY = 'id_token'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'diagnosis', component: DiagnosisView },
    { path: '/repositories', name: 'repositories', component: RepositoriesView },
    { path: '/approvals', name: 'approvals', component: ApprovalsView },
    { path: '/workflows', name: 'workflows', component: WorkflowsView },
    { path: '/snapshots', name: 'snapshots', component: SnapshotsView },
    { path: '/admin', name: 'admin', component: AdminView },
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } }
  ]
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!to.meta.public && !token) {
    next('/login')
    return
  }
  if (to.path === '/login' && token) {
    next('/')
    return
  }
  next()
})

export default router
