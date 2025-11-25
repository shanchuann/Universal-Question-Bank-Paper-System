import { reactive } from 'vue'
import axios from 'axios'

interface UserProfile {
  id?: string
  username: string
  nickname: string
  email: string
  role: string
  avatarUrl: string
}

export const authState = reactive({
  isAuthenticated: !!localStorage.getItem('token'),
  user: {
    id: '',
    username: 'User',
    nickname: 'User',
    email: 'user@example.com',
    role: '',
    avatarUrl: ''
  } as UserProfile,
  login(token: string) {
    localStorage.setItem('token', token)
    this.isAuthenticated = true
    this.fetchUser()
  },
  logout() {
    localStorage.removeItem('token')
    this.isAuthenticated = false
    this.user.avatarUrl = '' // Reset avatar on logout
    this.user.username = 'User'
    this.user.nickname = 'User'
  },
  updateProfile(profile: Partial<UserProfile>) {
    Object.assign(this.user, profile)
  },
  async fetchUser() {
    if (!this.isAuthenticated) return
    try {
      const token = localStorage.getItem('token')
      const response = await axios.get('/api/user/profile', {
        headers: { Authorization: `Bearer ${token}` }
      })
      const data = response.data
      this.updateProfile({
        id: data.id,
        username: data.username || this.user.username,
        nickname: data.nickname || data.username || this.user.username,
        email: data.email || '',
        role: data.role || this.user.role,
        avatarUrl: data.avatarUrl || ''
      })
    } catch (e) {
      console.error('Failed to fetch user profile', e)
    }
  }
})
