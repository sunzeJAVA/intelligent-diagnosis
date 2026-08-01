/**
 * 轻量级全局 Toast 提示
 * 不依赖外部 UI 库，直接操作 DOM，支持暗色模式
 */

let toastContainer: HTMLDivElement | null = null

function ensureContainer(): HTMLDivElement {
  if (toastContainer) return toastContainer
  toastContainer = document.createElement('div')
  toastContainer.className =
    'fixed top-4 left-1/2 -translate-x-1/2 z-[9999] flex flex-col gap-3 pointer-events-none'
  document.body.appendChild(toastContainer)
  return toastContainer
}

export type ToastType = 'error' | 'warning' | 'info' | 'success'

const typeStyles: Record<ToastType, string> = {
  error:
    'bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800 text-red-800 dark:text-red-200',
  warning:
    'bg-amber-50 dark:bg-amber-900/20 border-amber-200 dark:border-amber-800 text-amber-800 dark:text-amber-200',
  info:
    'bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800 text-blue-800 dark:text-blue-200',
  success:
    'bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800 text-green-800 dark:text-green-200'
}

/**
 * 显示一个全局 Toast，相同消息会替换旧的提示，避免堆叠
 * @param message 提示文本
 * @param type 提示类型
 * @param duration 自动关闭毫秒数，默认 4000
 */
export function showToast(
  message: string,
  type: ToastType = 'error',
  duration = 4000
): void {
  if (typeof document === 'undefined') return

  const container = ensureContainer()

  // 移除相同消息的已有提示
  const existing = container.querySelector(`[data-toast-message="${CSS.escape(message)}"]`)
  if (existing) {
    existing.remove()
  }

  const el = document.createElement('div')
  el.dataset.toastMessage = message
  el.className = [
    'pointer-events-auto',
    'max-w-md',
    'px-4 py-3',
    'rounded-lg',
    'border',
    'shadow-lg',
    'text-sm font-medium',
    'transition-all duration-300',
    'translate-y-0 opacity-100',
    typeStyles[type]
  ].join(' ')
  el.textContent = message

  container.appendChild(el)

  const remove = () => {
    el.classList.add('-translate-y-2', 'opacity-0')
    setTimeout(() => el.remove(), 300)
  }

  const timer = setTimeout(remove, duration)

  el.addEventListener('click', () => {
    clearTimeout(timer)
    remove()
  })
}
