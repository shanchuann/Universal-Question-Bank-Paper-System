<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { authState } from '@/states/authState'
import axios from 'axios'

const user = ref({
  username: '',
  nickname: '',
  email: '',
  role: '',
  avatarUrl: ''
})

const originalUser = ref({ ...user.value })

const loading = ref(false)
const toastMessage = ref('')
const toastType = ref('success') // 'success' | 'error'
const showPasswordModal = ref(false)
const showVerifyModal = ref(false)
const verifyPassword = ref('')

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const showToast = (msg: string, type: 'success' | 'error' = 'success') => {
  toastMessage.value = msg
  toastType.value = type
  setTimeout(() => {
    toastMessage.value = ''
  }, 3000)
}

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
      const data = response.data
      user.value = {
        username: data.username || user.value.username,
        nickname: data.nickname || data.username || user.value.username,
        email: data.email || '',
        role: data.role || user.value.role,
        avatarUrl: data.avatarUrl || ''
      }
      originalUser.value = { ...user.value }
      authState.updateProfile(user.value)
    }
  } catch (e) {
    console.error('Failed to fetch profile', e)
  }
})

const hasSensitiveChanges = computed(() => {
  return user.value.email !== originalUser.value.email || user.value.username !== originalUser.value.username
})

const validateForm = () => {
  if (user.value.nickname.length < 2 || user.value.nickname.length > 20) {
    showToast('Nickname must be between 2 and 20 characters', 'error')
    return false
  }
  if (!/^[a-zA-Z0-9 _-]+$/.test(user.value.nickname)) {
    showToast('Nickname contains invalid characters', 'error')
    return false
  }
  if (user.value.email && !/^[A-Za-z0-9+_.-]+@(.+)$/.test(user.value.email)) {
    showToast('Invalid email format', 'error')
    return false
  }
  return true
}

const handleSaveClick = () => {
  if (!validateForm()) return

  if (hasSensitiveChanges.value) {
    showVerifyModal.value = true
  } else {
    submitProfileUpdate()
  }
}

const submitProfileUpdate = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const payload = { ...user.value, currentPassword: verifyPassword.value }
    
    const response = await axios.put('/api/user/profile', payload, {
      headers: { Authorization: `Bearer ${token}` }
    })
    
    authState.updateProfile(response.data)
    originalUser.value = { ...user.value }
    showToast('Profile updated successfully')
    showVerifyModal.value = false
    verifyPassword.value = ''
  } catch (e: any) {
    console.error('Failed to save profile', e)
    showToast(e.response?.data || 'Failed to save profile', 'error')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  user.value = { ...originalUser.value }
  showToast('Changes reset', 'success')
}

const triggerFileInput = () => {
  document.getElementById('avatar-input')?.click()
}

const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    const file = target.files[0]
    
    // Client-side validation
    if (!file.type.startsWith('image/')) {
        showToast('Only image files are allowed', 'error')
        return
    }
    if (file.size > 5 * 1024 * 1024) {
        showToast('File size must be less than 5MB', 'error')
        return
    }

    const formData = new FormData()
    formData.append('file', file)

    try {
      loading.value = true
      const res = await axios.post('/api/files/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      user.value.avatarUrl = res.data.fileUrl
      showToast('Avatar uploaded successfully')
    } catch (e: any) {
      console.error('Failed to upload avatar', e)
      showToast(e.response?.data?.error || 'Failed to upload avatar', 'error')
    } finally {
      loading.value = false
    }
  }
}

const handlePasswordUpdate = async () => {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    showToast('New passwords do not match', 'error')
    return
  }
  
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    await axios.put('/api/user/password', {
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    }, {
      headers: { Authorization: `Bearer ${token}` }
    })
    
    showToast('Password updated successfully')
    showPasswordModal.value = false
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e: any) {
    console.error('Failed to update password', e)
    showToast(e.response?.data || 'Failed to update password', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="container">
    <!-- Toast Notification -->
    <div v-if="toastMessage" :class="['toast', toastType]">
      {{ toastMessage }}
    </div>

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
          <div class="floating-label-group">
            <input v-model="user.username" type="text" id="username" required placeholder=" " />
            <label for="username">Username</label>
          </div>
        </div>

        <div class="section-row">
          <div class="floating-label-group">
            <input v-model="user.nickname" type="text" id="nickname" required placeholder=" " />
            <label for="nickname">Nickname</label>
          </div>
        </div>

        <div class="section-row">
          <div class="floating-label-group">
            <input v-model="user.email" type="email" id="email" required placeholder=" " />
            <label for="email">Email</label>
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
        <button @click="handleSaveClick" :disabled="loading" class="google-btn primary-btn">
          {{ loading ? 'Saving...' : 'Save' }}
        </button>
        <button @click="handleReset" :disabled="loading" class="google-btn text-btn">
          Reset
        </button>
        <button class="google-btn text-btn" @click="showPasswordModal = true">
          Change Password
        </button>
      </div>
    </div>

    <!-- Password Change Modal -->
    <div v-if="showPasswordModal" class="modal-overlay">
      <div class="modal-card">
        <h2>Change Password</h2>
        <div class="floating-label-group">
          <input v-model="passwordForm.oldPassword" type="password" id="oldPass" required placeholder=" " />
          <label for="oldPass">Current Password</label>
        </div>
        <div class="floating-label-group">
          <input v-model="passwordForm.newPassword" type="password" id="newPass" required placeholder=" " />
          <label for="newPass">New Password</label>
        </div>
        <div class="floating-label-group">
          <input v-model="passwordForm.confirmPassword" type="password" id="confirmPass" required placeholder=" " />
          <label for="confirmPass">Confirm New Password</label>
        </div>
        <div class="modal-actions">
          <button @click="showPasswordModal = false" class="google-btn text-btn">Cancel</button>
          <button @click="handlePasswordUpdate" :disabled="loading" class="google-btn primary-btn">Update</button>
        </div>
      </div>
    </div>

    <!-- Verify Password Modal -->
    <div v-if="showVerifyModal" class="modal-overlay">
      <div class="modal-card">
        <h2>Verify Identity</h2>
        <p>Please enter your current password to confirm these changes.</p>
        <div class="floating-label-group">
          <input v-model="verifyPassword" type="password" id="verifyPass" required placeholder=" " />
          <label for="verifyPass">Current Password</label>
        </div>
        <div class="modal-actions">
          <button @click="showVerifyModal = false" class="google-btn text-btn">Cancel</button>
          <button @click="submitProfileUpdate" :disabled="loading" class="google-btn primary-btn">Confirm</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.container {
  display: flex;
  justify-content: center;
  padding: 2rem;
  background-color: #f0f2f5;
  min-height: 100vh;
}

.google-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24);
  width: 100%;
  max-width: 700px;
  padding: 2rem;
  display: flex;
  flex-direction: column;
}

.card-header {
  margin-bottom: 2rem;
  text-align: center;
}

.card-header h1 {
  font-size: 1.75rem;
  font-weight: 400;
  color: #202124;
  margin: 0;
}

.subtitle {
  color: #5f6368;
  margin-top: 0.5rem;
}

.profile-section {
  border: 1px solid #dadce0;
  border-radius: 8px;
  overflow: hidden;
}

.section-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #dadce0;
}

.section-row:last-child {
  border-bottom: none;
}

.row-label h3 {
  font-size: 0.875rem;
  font-weight: 500;
  color: #5f6368;
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.row-label p {
  font-size: 0.8rem;
  color: #80868b;
  margin: 0.25rem 0 0 0;
}

.row-content {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-left: 2rem;
}

.avatar-preview {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background-color: #e8eaed;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 1.5rem;
  color: #5f6368;
}

/* Floating Labels */
.floating-label-group {
  position: relative;
  width: 100%;
  margin-bottom: 0; /* Adjusted for row layout */
}

.floating-label-group input {
  width: 100%;
  padding: 12px 12px 12px 12px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  font-size: 1rem;
  outline: none;
  transition: border-color 0.2s;
}

.floating-label-group input:focus {
  border-color: #1a73e8;
  border-width: 2px;
  padding: 11px 11px 11px 11px; /* Adjust for border width */
}

.floating-label-group label {
  position: absolute;
  left: 12px;
  top: 12px;
  color: #5f6368;
  font-size: 1rem;
  pointer-events: none;
  transition: 0.2s ease all;
  background-color: white;
  padding: 0 4px;
}

.floating-label-group input:focus ~ label,
.floating-label-group input:not(:placeholder-shown) ~ label {
  top: -10px;
  font-size: 0.75rem;
  color: #1a73e8;
}

.read-only-text {
  font-size: 1rem;
  color: #202124;
}

.form-actions {
  margin-top: 2rem;
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
}

.google-btn {
  border: none;
  border-radius: 4px;
  padding: 0.5rem 1.5rem;
  font-size: 0.875rem;
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

.primary-btn:disabled {
  background-color: #80868b;
  cursor: not-allowed;
}

.text-btn {
  background-color: transparent;
  color: #1a73e8;
}

.text-btn:hover {
  background-color: #f6fafe;
}

/* Toast */
.toast {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  border-radius: 4px;
  color: white;
  font-weight: 500;
  z-index: 1000;
  box-shadow: 0 2px 5px rgba(0,0,0,0.2);
  animation: slideDown 0.3s ease-out;
}

.toast.success {
  background-color: #34a853;
}

.toast.error {
  background-color: #d93025;
}

@keyframes slideDown {
  from { top: -50px; opacity: 0; }
  to { top: 20px; opacity: 1; }
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-card {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
}

.modal-card h2 {
  margin-top: 0;
  color: #202124;
}

.modal-card .floating-label-group {
  margin-bottom: 1.5rem;
  margin-top: 1.5rem;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 2rem;
}
</style>