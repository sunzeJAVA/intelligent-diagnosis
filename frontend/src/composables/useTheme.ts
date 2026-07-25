import { ref, onMounted, watch } from 'vue'

type Theme = 'light' | 'dark' | 'system'

const theme = ref<Theme>('system')
const isDark = ref(false)

/**
 * 主题状态管理组合式函数
 * 支持 light、dark、system 三种主题模式
 * 通过 localStorage 持久化用户偏好
 * @returns 主题相关状态和方法
 */
export function useTheme() {
  /**
   * 应用主题到 DOM
   * 根据当前主题模式设置或移除 dark class
   */
  const applyTheme = () => {
    if (theme.value === 'system') {
      isDark.value = window.matchMedia('(prefers-color-scheme: dark)').matches
    } else {
      isDark.value = theme.value === 'dark'
    }

    if (isDark.value) {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  }

  /**
   * 设置主题模式
   * @param newTheme - 新的主题模式
   */
  const setTheme = (newTheme: Theme) => {
    theme.value = newTheme
    localStorage.setItem('theme', newTheme)
    applyTheme()
  }

  /**
   * 在 light 和 dark 之间切换主题
   */
  const toggleTheme = () => {
    if (theme.value === 'light') {
      setTheme('dark')
    } else {
      setTheme('light')
    }
  }

  /**
   * 在三种主题模式间循环切换
   * 顺序：light → dark → system → light
   */
  const cycleTheme = () => {
    const themes: Theme[] = ['light', 'dark', 'system']
    const currentIndex = themes.indexOf(theme.value)
    setTheme(themes[(currentIndex + 1) % themes.length])
  }

  onMounted(() => {
    const storedTheme = localStorage.getItem('theme') as Theme
    if (storedTheme) {
      theme.value = storedTheme
    }

    applyTheme()

    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    mediaQuery.addEventListener('change', applyTheme)
  })

  watch(theme, () => {
    applyTheme()
  })

  return {
    theme,
    isDark,
    setTheme,
    toggleTheme,
    cycleTheme,
  }
}
