import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import PublicLayout from '@/layouts/PublicLayout.vue'
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
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', name: 'diagnosis', component: DiagnosisView },
        { path: 'repositories', name: 'repositories', component: RepositoriesView },
        { path: 'approvals', name: 'approvals', component: ApprovalsView },
        { path: 'workflows', name: 'workflows', component: WorkflowsView },
        { path: 'snapshots', name: 'snapshots', component: SnapshotsView },
        { path: 'admin', name: 'admin', component: AdminView }
      ]
    },
    {
      path: '/login',
      component: PublicLayout,
      meta: { public: true },
      children: [
        { path: '', name: 'login', component: LoginView }
      ]
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem(TOKEN_KEY)
  const isPublic = to.matched.some((record) => record.meta.public)

  if (!isPublic && !token) {
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
