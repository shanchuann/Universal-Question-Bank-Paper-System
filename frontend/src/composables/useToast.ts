import { ref } from 'vue'

export interface ToastOptions {
  message: string
  type?: 'success' | 'error' | 'warning'
  duration?: number
}

const toastMessage = ref('')
const toastType = ref<'success' | 'error' | 'warning'>('success')
const toastVisible = ref(false)

let toastTimer: ReturnType<typeof setTimeout> | null = null

export function useToast() {
  const showToast = (options: string | ToastOptions) => {
    if (toastTimer) {
      clearTimeout(toastTimer)
    }

    if (typeof options === 'string') {
      toastMessage.value = options
      toastType.value = 'success'
    } else {
      toastMessage.value = options.message
      toastType.value = options.type || 'success'
    }

    toastVisible.value = true

    const duration = typeof options === 'object' ? options.duration || 3000 : 3000
    toastTimer = setTimeout(() => {
      toastVisible.value = false
    }, duration)
  }

  const hideToast = () => {
    toastVisible.value = false
    if (toastTimer) {
      clearTimeout(toastTimer)
      toastTimer = null
    }
  }

  return {
    toastMessage,
    toastType,
    toastVisible,
    showToast,
    hideToast
  }
}
