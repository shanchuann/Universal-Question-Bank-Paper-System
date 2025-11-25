<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { authState } from '@/states/authState'
import axios from 'axios'

const user = ref({
  username: '',
  nickname: '',
  email: '',
  role: '',
  avatarUrl: ''
})

const loading = ref(false)
const message = ref('')

onMounted(async () => {
  // Initialize from global state
  user.value = { ...authState.user }
  
  // Fetch fresh data from API
  try {
    const token = localStorage.getItem('token')
    if (token) {
      const response = await axios.get('/api/user/profile', {
        headers: { Authorization: `Bearer ${token}` }
      })
      // Update local and global state with fetched data
      // Note: Backend might return nulls for new fields, handle gracefully
      const data = response.data
      user.value = {
        username: data.username || user.value.username,
        nickname: data.nickname || data.username || user.value.username,
        email: data.email || '',
        role: data.role || user.value.role,
        avatarUrl: data.avatarUrl || ''
      }
      authState.updateProfile(user.value)
    }
  } catch (e) {
    console.error('Failed to fetch profile', e)
  }
})

const handleSave = async () => {
  loading.value = true
  message.value = ''
  try {
    const token = localStorage.getItem('token')
    const response = await axios.put('/api/user/profile', user.value, {
      headers: { Authorization: `Bearer ${token}` }
    })
    
    authState.updateProfile(response.data)
    message.value = 'Profile updated successfully'
  } catch (e) {
    console.error('Failed to save profile', e)
    message.value = 'Failed to save profile'
  } finally {
    loading.value = false
  }
}

const triggerFileInput = () => {
  document.getElementById('avatar-input')?.click()
}

const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    const file = target.files[0]
    const formData = new FormData()
    formData.append('file', file)

    try {
      loading.value = true
      // Upload to backend
      const res = await axios.post('/api/files/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      user.value.avatarUrl = res.data.fileUrl
    } catch (e) {
      console.error('Failed to upload avatar', e)
      message.value = 'Failed to upload avatar'
    } finally {
      loading.value = false
    }
  }
}
</script>

<template>
  <div class="container">
    <div class="google-card profile-card">
      <div class="card-header">
        <h1>Personal Info</h1>
        <p class="subtitle">Basic info, like your name and photo</p>
      </div>

      <div class="profile-section">
        <div class="section-row avatar-row">
          <div class="row-label">
            <h3>Photo</h3>
            <p>A photo helps personalize your account</p>
          </div>
          <div class="row-content" @click="triggerFileInput">
            <div class="avatar-preview" :style="{ backgroundImage: user.avatarUrl ? `url(${user.avatarUrl})` : '' }">
              <span v-if="!user.avatarUrl" class="avatar-placeholder">{{ user.username.charAt(0).toUpperCase() }}</span>
            </div>
            <input type="file" id="avatar-input" @change="handleFileChange" accept="image/*" style="display: none" />
          </div>
        </div>

        <div class="section-row">
          <div class="row-label">
            <h3>Nickname</h3>
          </div>
          <div class="row-content">
            <input v-model="user.nickname" type="text" class="google-input-plain" placeholder="Enter your nickname" />
          </div>
        </div>

        <div class="section-row">
          <div class="row-label">
            <h3>Email</h3>
          </div>
          <div class="row-content">
            <input v-model="user.email" type="email" class="google-input-plain" />
          </div>
        </div>

        <div class="section-row">
          <div class="row-label">
            <h3>Role</h3>
          </div>
          <div class="row-content">
            <span class="read-only-text">{{ user.role }}</span>
          </div>
        </div>
      </div>

      <div class="form-actions">
        <button @click="handleSave" :disabled="loading" class="google-btn primary-btn">
          {{ loading ? 'Saving...' : 'Save' }}
        </button>
      </div>

      <div v-if="message" class="message success">
        {{ message }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.container {
  padding: 40px 20px;
}

.profile-card {
  max-width: 800px;
  margin: 0 auto;
  padding: 0;
  border: 1px solid #dadce0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.card-header {
  padding: 24px;
  text-align: center;
  border-bottom: 1px solid #dadce0;
}

.card-header h1 {
  font-family: 'Google Sans', sans-serif;
  font-size: 28px;
  font-weight: 400;
  margin-bottom: 8px;
  color: #202124;
}

.subtitle {
  color: #5f6368;
  font-size: 16px;
}

.profile-section {
  padding: 0;
}

.section-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid #dadce0;
  transition: background-color 0.2s;
}

.section-row:hover {
  background-color: #f8f9fa;
}

.section-row:last-child {
  border-bottom: none;
}

.row-label h3 {
  font-size: 14px;
  font-weight: 500;
  color: #5f6368;
  text-transform: uppercase;
  margin: 0 0 4px 0;
}

.row-label p {
  font-size: 14px;
  color: #5f6368;
  margin: 0;
}

.row-content {
  flex: 1;
  max-width: 60%;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.avatar-preview {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background-color: #1a73e8;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 500;
  cursor: pointer;
  background-size: cover;
  background-position: center;
}

.google-input-plain {
  width: 100%;
  border: none;
  background: transparent;
  font-size: 16px;
  color: #202124;
  text-align: right;
  font-family: inherit;
}

.google-input-plain:focus {
  outline: none;
  border-bottom: 2px solid #1a73e8;
}

.read-only-text {
  font-size: 16px;
  color: #5f6368;
}

.form-actions {
  padding: 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #dadce0;
}

.google-btn {
  border: none;
  border-radius: 4px;
  padding: 8px 24px;
  font-family: 'Google Sans', sans-serif;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.primary-btn {
  background-color: #1a73e8;
  color: white;
}

.primary-btn:hover {
  background-color: #1557b0;
}

.message {
  margin: 0 24px 24px;
  padding: 12px;
  border-radius: 4px;
  font-size: 14px;
  text-align: center;
}

.success {
  background-color: #e6f4ea;
  color: #137333;
}

@media (max-width: 600px) {
  .section-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .row-content {
    max-width: 100%;
    width: 100%;
    justify-content: flex-start;
  }
  
  .google-input-plain {
    text-align: left;
    border-bottom: 1px solid #dadce0;
  }
}
</style>