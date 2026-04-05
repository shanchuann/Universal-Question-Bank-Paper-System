import { ref } from 'vue'
import axios from 'axios'

const unreadCount = ref(0)

const getAuthHeaders = () => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const refreshUnreadCount = async () => {
  const token = localStorage.getItem('token')
  if (!token) {
    unreadCount.value = 0
    return
  }

  try {
    const response = await axios.get('/api/notifications/conversations?limit=80', {
      headers: getAuthHeaders()
    })
    const conversations = Array.isArray(response.data) ? response.data : []
    unreadCount.value = conversations.reduce((sum: number, item: any) => {
      const count = Number(item?.unreadCount ?? 0)
      return sum + (Number.isFinite(count) ? Math.max(0, count) : 0)
    }, 0)
  } catch (_error) {
    unreadCount.value = 0
  }
}

const setUnreadCount = (value: number) => {
  unreadCount.value = Math.max(0, value)
}

export function useNotifications() {
  return {
    unreadCount,
    refreshUnreadCount,
    setUnreadCount
  }
}
