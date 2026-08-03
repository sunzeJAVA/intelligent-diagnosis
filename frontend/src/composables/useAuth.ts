import { ref, computed } from 'vue'
import { fetchCurrentUser, type UserInfo } from '@/api/auth'

const user = ref<UserInfo | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

/**
 * 认证状态管理组合式函数
 * 在模块级别维护当前登录用户信息，供导航栏等全局组件共享
 */
export function useAuth() {
  const isAuthenticated = computed(() => !!user.value)

  const initials = computed(() => {
    return user.value?.username?.slice(0, 2).toUpperCase() ?? 'U'
  })

  const roleLabel = computed(() => {
    if (!user.value) return ''
    const authorities = user.value.authorities
    if (authorities.includes('admin:read') || authorities.includes('ROLE_ADMIN')) return '管理员'
    if (authorities.includes('operator:read') || authorities.includes('ROLE_OPERATOR')) return '操作员'
    return '访客'
  })

  async function loadUser() {
    loading.value = true
    error.value = null
    try {
      user.value = await fetchCurrentUser()
    } catch (err) {
      user.value = null
      error.value = '获取用户信息失败'
    } finally {
      loading.value = false
    }
  }

  function clearUser() {
    user.value = null
  }

  return {
    user,
    loading,
    error,
    isAuthenticated,
    initials,
    roleLabel,
    loadUser,
    clearUser
  }
}
