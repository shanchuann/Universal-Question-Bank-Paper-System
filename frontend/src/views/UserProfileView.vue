<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { authState, type UserRole } from '@/states/authState'
import axios from 'axios'

const user = ref({
  username: '',
  nickname: '',
  email: '',
  role: '' as UserRole,
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

const preferences = ref({
  emailNotification: true,
  systemNotification: true,
  publicProfile: false,
  showActivity: true
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
        role: (data.role as UserRole) || user.value.role,
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
    showToast('昵称长度需在2-20个字符之间', 'error')
    return false
  }
  if (!/^[\u4e00-\u9fa5a-zA-Z0-9 _-]+$/.test(user.value.nickname)) {
    showToast('昵称只能包含中文、英文、数字、空格、下划线和短横线', 'error')
    return false
  }
  if (user.value.email && !/^[A-Za-z0-9+_.-]+@(.+)$/.test(user.value.email)) {
    showToast('邮箱格式不正确', 'error')
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
    showToast('个人信息已更新')
    showVerifyModal.value = false
    verifyPassword.value = ''
  } catch (e: any) {
    console.error('Failed to save profile', e)
    showToast(e.response?.data || '保存失败', 'error')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  user.value = { ...originalUser.value }
  showToast('已重置修改', 'success')
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
        showToast('只能上传图片文件', 'error')
        return
    }
    if (file.size > 5 * 1024 * 1024) {
        showToast('文件大小不能超过5MB', 'error')
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
      showToast('头像上传成功')
    } catch (e: any) {
      console.error('Failed to upload avatar', e)
      showToast(e.response?.data?.error || '头像上传失败', 'error')
    } finally {
      loading.value = false
    }
  }
}

const handlePasswordUpdate = async () => {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    showToast('两次输入的新密码不一致', 'error')
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
    
    showToast('密码已更新')
    showPasswordModal.value = false
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e: any) {
    console.error('Failed to update password', e)
    showToast(e.response?.data || '密码更新失败', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="line-container page-container">
    <!-- Toast Notification -->
    <div v-if="toastMessage" :class="['line-toast', toastType]">
      <span class="toast-icon">{{ toastType === 'success' ? '✓' : '!' }}</span>
      {{ toastMessage }}
    </div>

    <!-- Page Header -->
    <div class="page-header center-header">
      <h1>个人信息</h1>
      <p class="subtitle">管理您的个人资料、密码和隐私设置</p>
    </div>

    <div class="profile-layout">
      <!-- Left Column: Avatar & Basic Info -->
      <div class="line-card profile-main-card">
        <div class="avatar-section">
          <div class="avatar-large" :style="{ backgroundImage: user.avatarUrl ? `url(${user.avatarUrl})` : '' }" @click="triggerFileInput">
            <span v-if="!user.avatarUrl">{{ user.username.charAt(0).toUpperCase() }}</span>
            <div class="avatar-overlay">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path><circle cx="12" cy="13" r="4"></circle></svg>
            </div>
          </div>
          <input type="file" id="avatar-input" @change="handleFileChange" accept="image/*" style="display: none" />
          <h2 class="profile-name">{{ user.nickname || user.username }}</h2>
          <span class="role-badge">{{ user.role }}</span>
        </div>

        <div class="form-grid">
          <div class="form-group">
            <label for="username">用户名</label>
            <input v-model="user.username" type="text" id="username" class="line-input" required />
          </div>

          <div class="form-group">
            <label for="nickname">昵称</label>
            <input v-model="user.nickname" type="text" id="nickname" class="line-input" required />
            <span class="input-hint">显示给他人的名称</span>
          </div>

          <div class="form-group full-width">
            <label for="email">邮箱地址</label>
            <input v-model="user.email" type="email" id="email" class="line-input" required />
          </div>
        </div>

        <div class="card-actions">
          <button @click="handleReset" :disabled="loading" class="line-btn text-btn">重置更改</button>
          <button @click="handleSaveClick" :disabled="loading" class="line-btn primary-btn">
            {{ loading ? '保存中...' : '保存更改' }}
          </button>
        </div>
      </div>

      <!-- Right Column: Settings & Security -->
      <div class="side-column">
        <!-- Security -->
        <div class="line-card side-card">
          <div class="card-title">
            <h3>安全设置</h3>
          </div>
          <div class="action-item">
            <div class="item-text">
              <h4>登录密码</h4>
              <p>定期修改密码以保护账号安全</p>
            </div>
            <button class="line-btn outline-btn sm-btn" @click="showPasswordModal = true">修改</button>
          </div>
        </div>

        <!-- Notifications -->
        <div class="line-card side-card">
          <div class="card-title">
            <h3>通知偏好</h3>
          </div>
          <div class="action-item">
            <div class="item-text">
              <h4>邮件通知</h4>
              <p>接收考试提醒和成绩通知</p>
            </div>
            <label class="switch">
              <input type="checkbox" v-model="preferences.emailNotification">
              <span class="slider round"></span>
            </label>
          </div>
          <div class="action-item">
            <div class="item-text">
              <h4>系统消息</h4>
              <p>接收系统维护和更新公告</p>
            </div>
            <label class="switch">
              <input type="checkbox" v-model="preferences.systemNotification">
              <span class="slider round"></span>
            </label>
          </div>
        </div>

        <!-- Privacy -->
        <div class="line-card side-card">
          <div class="card-title">
            <h3>隐私设置</h3>
          </div>
          <div class="action-item">
            <div class="item-text">
              <h4>公开资料</h4>
              <p>允许其他用户查看您的基本信息</p>
            </div>
            <label class="switch">
              <input type="checkbox" v-model="preferences.publicProfile">
              <span class="slider round"></span>
            </label>
          </div>
          <div class="action-item">
            <div class="item-text">
              <h4>在线状态</h4>
              <p>显示您的在线状态</p>
            </div>
            <label class="switch">
              <input type="checkbox" v-model="preferences.showActivity">
              <span class="slider round"></span>
            </label>
          </div>
        </div>
      </div>
    </div>

    <!-- Password Change Modal -->
    <div v-if="showPasswordModal" class="modal-backdrop" @click="showPasswordModal = false">
      <div class="line-modal" @click.stop>
        <div class="modal-header">
          <h2>修改密码</h2>
          <button class="close-btn" @click="showPasswordModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label for="oldPass">当前密码</label>
            <input v-model="passwordForm.oldPassword" type="password" id="oldPass" class="line-input" required />
          </div>
          <div class="form-group">
            <label for="newPass">新密码</label>
            <input v-model="passwordForm.newPassword" type="password" id="newPass" class="line-input" required />
          </div>
          <div class="form-group">
            <label for="confirmPass">确认新密码</label>
            <input v-model="passwordForm.confirmPassword" type="password" id="confirmPass" class="line-input" required />
          </div>
        </div>
        <div class="modal-footer">
          <button @click="showPasswordModal = false" class="line-btn text-btn">取消</button>
          <button @click="handlePasswordUpdate" :disabled="loading" class="line-btn primary-btn">更新密码</button>
        </div>
      </div>
    </div>

    <!-- Verify Password Modal -->
    <div v-if="showVerifyModal" class="modal-backdrop" @click="showVerifyModal = false">
      <div class="line-modal" @click.stop>
        <div class="modal-header">
          <h2>身份验证</h2>
          <button class="close-btn" @click="showVerifyModal = false">×</button>
        </div>
        <div class="modal-body">
          <p class="modal-desc">您正在修改敏感信息，请输入当前密码以确认。</p>
          <div class="form-group">
            <label for="verifyPass">当前密码</label>
            <input v-model="verifyPassword" type="password" id="verifyPass" class="line-input" required />
          </div>
        </div>
        <div class="modal-footer">
          <button @click="showVerifyModal = false" class="line-btn text-btn">取消</button>
          <button @click="submitProfileUpdate" :disabled="loading" class="line-btn primary-btn">确认修改</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  padding: 40px 24px;
  max-width: 1000px;
  margin: 0 auto;
}

.center-header {
  text-align: center;
  margin-bottom: 40px;
}

.center-header h1 {
  font-size: 2rem;
  font-weight: 700;
  color: var(--line-text);
  margin-bottom: 8px;
}

.center-header .subtitle {
  color: var(--line-text-secondary);
  font-size: 1rem;
}

.profile-layout {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

/* Main Profile Card */
.profile-main-card {
  padding: 32px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;
  padding-bottom: 32px;
  border-bottom: 1px solid var(--line-border);
}

.avatar-large {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background-color: var(--line-bg-soft);
  color: var(--line-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  font-weight: 600;
  background-size: cover;
  background-position: center;
  position: relative;
  cursor: pointer;
  overflow: hidden;
  border: 2px solid var(--line-bg);
  box-shadow: 0 0 0 2px var(--line-border);
  transition: all 0.2s;
}

.avatar-large:hover .avatar-overlay {
  opacity: 1;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  color: white;
}

.profile-name {
  margin: 16px 0 8px;
  font-size: 1.25rem;
  font-weight: 600;
}

.role-badge {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 99px;
  background-color: var(--line-bg-soft);
  color: var(--line-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* Form Styles */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 32px;
}

.full-width {
  grid-column: span 2;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--line-text);
}

.input-hint {
  font-size: 0.75rem;
  color: var(--line-text-secondary);
  margin-top: 4px;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid var(--line-border);
}

/* Security Connect & Side Cards */
.side-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.side-card {
  padding: 0;
  overflow: hidden;
  height: fit-content;
}

/* Toggle Switch */
.switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: var(--line-bg-soft);
  border: 1px solid var(--line-border);
  transition: .4s;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 2px;
  bottom: 2px;
  background-color: var(--line-text-secondary);
  transition: .4s;
}

.slider.round {
  border-radius: 24px;
}

.slider.round:before {
  border-radius: 50%;
}

input:checked + .slider {
  background-color: var(--line-primary);
  border-color: var(--line-primary);
}

input:checked + .slider:before {
  transform: translateX(20px);
  background-color: white;
}

input:focus + .slider {
  box-shadow: 0 0 1px var(--line-primary);
}

.card-title {
  padding: 16px 20px;
  background-color: var(--line-bg-soft);
  border-bottom: 1px solid var(--line-border);
}

.card-title h3 {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--line-text);
}

.action-item {
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-text h4 {
  margin: 0 0 4px;
  font-size: 0.9rem;
  font-weight: 500;
}

.item-text p {
  margin: 0;
  font-size: 0.8rem;
  color: var(--line-text-secondary);
}

.sm-btn {
  padding: 6px 12px;
  font-size: 0.8rem;
}

/* Modal */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: fadeIn 0.2s ease;
}

.line-modal {
  background: var(--line-bg);
  border-radius: var(--line-radius-lg);
  width: 100%;
  max-width: 400px;
  box-shadow: var(--line-shadow-xl);
  border: 1px solid var(--line-border);
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--line-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h2 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: var(--line-text-secondary);
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.modal-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.modal-desc {
  font-size: 0.9rem;
  color: var(--line-text-secondary);
  margin: 0 0 8px;
}

.modal-footer {
  padding: 16px 24px;
  background-color: var(--line-bg-soft);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  border-top: 1px solid var(--line-border);
  border-bottom-left-radius: var(--line-radius-lg);
  border-bottom-right-radius: var(--line-radius-lg);
}

/* Toast */
.line-toast {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  padding: 10px 16px;
  border-radius: 99px;
  color: white;
  font-weight: 500;
  font-size: 0.9rem;
  z-index: 3000;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: var(--line-shadow-lg);
  animation: slideDownToast 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.line-toast.success { background-color: #10B981; }
.line-toast.error { background-color: #EF4444; }

.toast-icon {
  width: 18px;
  height: 18px;
  background: rgba(255,255,255,0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes slideUp { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
@keyframes slideDownToast { from { opacity: 0; transform: translate(-50%, -10px); } to { opacity: 1; transform: translate(-50%, 0); } }

@media (max-width: 768px) {
  .profile-layout { grid-template-columns: 1fr; }
  .form-grid { grid-template-columns: 1fr; }
  .full-width { grid-column: auto; }
}
</style>