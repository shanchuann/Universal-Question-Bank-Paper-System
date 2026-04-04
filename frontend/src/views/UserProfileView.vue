<script setup lang="ts">
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { authState, type UserRole } from '@/states/authState'
import { useToast } from '@/composables/useToast'
import axios from 'axios'

const { showToast } = useToast()

const user = ref({
  username: '',
  nickname: '',
  email: '',
  role: '' as UserRole,
  avatarUrl: ''
})


const originalUser = ref({ ...user.value })

const loading = ref(false)
const showPasswordModal = ref(false)
const showVerifyModal = ref(false)
const verifyPassword = ref('')
const showEmailVerifyModal = ref(false)
const emailVerifyCode = ref('')
const emailVerifyPassword = ref('')
const emailVerifyLoading = ref(false)
const emailVerifySent = ref(false)
const emailVerifyCooldown = ref(0)
let emailCooldownTimer: ReturnType<typeof setInterval> | null = null
const showAvatarPreviewModal = ref(false)
const avatarPreviewUrl = ref('')
const avatarZoom = ref(1)
const avatarRotate = ref(0)
const avatarOffsetX = ref(0)
const avatarOffsetY = ref(0)
const isDraggingAvatar = ref(false)
const dragStartX = ref(0)
const dragStartY = ref(0)
const dragOriginOffsetX = ref(0)
const dragOriginOffsetY = ref(0)
let pendingAvatarFile: File | null = null

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
      preferences.value = {
        emailNotification: data.emailNotification ?? true,
        systemNotification: data.systemNotification ?? true,
        publicProfile: data.publicProfile ?? false,
        showActivity: data.showActivity ?? true
      }
      originalUser.value = { ...user.value }
      authState.updateProfile(user.value)
    }
  } catch (e) {
    console.error('Failed to fetch profile', e)
  }
})

onUnmounted(() => {
  if (emailCooldownTimer) {
    clearInterval(emailCooldownTimer)
    emailCooldownTimer = null
  }
})

const hasSensitiveChanges = computed(() => {
  return user.value.email !== originalUser.value.email || user.value.username !== originalUser.value.username
})

const validateForm = () => {
  if (user.value.nickname.length < 2 || user.value.nickname.length > 20) {
    showToast({ message: '昵称长度需在2-20个字符之间', type: 'error' })
    return false
  }
  if (!/^[\u4e00-\u9fa5a-zA-Z0-9 _-]+$/.test(user.value.nickname)) {
    showToast({ message: '昵称只能包含中文、英文、数字、空格、下划线和短横线', type: 'error' })
    return false
  }
  if (user.value.email && !/^[A-Za-z0-9+_.-]+@(.+)$/.test(user.value.email)) {
    showToast({ message: '邮箱格式不正确', type: 'error' })
    return false
  }
  return true
}

const handleSaveClick = () => {
  if (!validateForm()) return
  if (user.value.email !== originalUser.value.email) {
    showEmailVerifyModal.value = true
  } else if (hasSensitiveChanges.value) {
    showVerifyModal.value = true
  } else {
    submitProfileUpdate()
  }
}

const submitProfileUpdate = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const payload = {
      ...user.value,
      ...preferences.value,
      currentPassword: verifyPassword.value
    }
    
    const response = await axios.put('/api/user/profile', payload, {
      headers: { Authorization: `Bearer ${token}` }
    })
    
    authState.updateProfile(response.data)
    originalUser.value = { ...user.value }
    showToast({ message: '个人信息已更新', type: 'success' })
    showVerifyModal.value = false
    verifyPassword.value = ''
  } catch (e: any) {
    console.error('Failed to save profile', e)
    showToast({ message: e.response?.data || '保存失败', type: 'error' })
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  user.value = { ...originalUser.value }
  showToast({ message: '已重置修改', type: 'success' })
}

const triggerFileInput = () => {
  document.getElementById('avatar-input')?.click()
}

const resetAvatarPreview = () => {
  if (avatarPreviewUrl.value) {
    URL.revokeObjectURL(avatarPreviewUrl.value)
  }
  avatarPreviewUrl.value = ''
  avatarZoom.value = 1
  avatarRotate.value = 0
  avatarOffsetX.value = 0
  avatarOffsetY.value = 0
  isDraggingAvatar.value = false
  pendingAvatarFile = null
  showAvatarPreviewModal.value = false
}

const onAvatarDragStart = (event: MouseEvent) => {
  if (!showAvatarPreviewModal.value) return
  isDraggingAvatar.value = true
  dragStartX.value = event.clientX
  dragStartY.value = event.clientY
  dragOriginOffsetX.value = avatarOffsetX.value
  dragOriginOffsetY.value = avatarOffsetY.value
}

const onAvatarDragMove = (event: MouseEvent) => {
  if (!isDraggingAvatar.value) return
  const deltaX = event.clientX - dragStartX.value
  const deltaY = event.clientY - dragStartY.value
  avatarOffsetX.value = dragOriginOffsetX.value + deltaX
  avatarOffsetY.value = dragOriginOffsetY.value + deltaY
}

const onAvatarDragEnd = () => {
  isDraggingAvatar.value = false
}

const resetAvatarAdjustments = () => {
  avatarZoom.value = 1
  avatarRotate.value = 0
  avatarOffsetX.value = 0
  avatarOffsetY.value = 0
}

const avatarPreviewViewportWidth = 260
const avatarPreviewViewportHeight = 260

const buildTransformedAvatarFile = async (file: File) => {
  const imageUrl = URL.createObjectURL(file)
  try {
    const image = await new Promise<HTMLImageElement>((resolve, reject) => {
      const img = new Image()
      img.onload = () => resolve(img)
      img.onerror = () => reject(new Error('头像图片加载失败'))
      img.src = imageUrl
    })

    const canvas = document.createElement('canvas')
    canvas.width = image.width
    canvas.height = image.height
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      throw new Error('无法创建头像画布')
    }

    const previewScale = Math.min(
      avatarPreviewViewportWidth / image.width,
      avatarPreviewViewportHeight / image.height
    )
    const offsetScale = previewScale > 0 ? 1 / previewScale : 1

    ctx.clearRect(0, 0, image.width, image.height)
    ctx.save()
    ctx.translate(image.width / 2, image.height / 2)
    ctx.translate(avatarOffsetX.value * offsetScale, avatarOffsetY.value * offsetScale)
    ctx.rotate((avatarRotate.value * Math.PI) / 180)
    ctx.scale(avatarZoom.value, avatarZoom.value)
    ctx.drawImage(image, -image.width / 2, -image.height / 2, image.width, image.height)
    ctx.restore()

    const outputType = file.type === 'image/png' ? 'image/png' : 'image/jpeg'
    const quality = outputType === 'image/jpeg' ? 0.92 : undefined

    const blob = await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob((result) => {
        if (result) {
          resolve(result)
          return
        }
        reject(new Error('头像导出失败'))
      }, outputType, quality)
    })

    const extension = outputType === 'image/png' ? 'png' : 'jpg'
    return new File([blob], `avatar-${Date.now()}.${extension}`, { type: outputType })
  } finally {
    URL.revokeObjectURL(imageUrl)
  }
}

const uploadAvatarFile = async (file: File) => {
  const formData = new FormData()
  formData.append('file', file)

  loading.value = true
  try {
    const res = await axios.post('/api/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    const uploadedUrl = res.data.fileUrl
    user.value.avatarUrl = uploadedUrl

    const token = localStorage.getItem('token')
    if (token) {
      const updateRes = await axios.put('/api/user/profile', { avatarUrl: uploadedUrl }, {
        headers: { Authorization: `Bearer ${token}` }
      })
      authState.updateProfile(updateRes.data)
      user.value = { ...user.value, avatarUrl: updateRes.data.avatarUrl || uploadedUrl }
      originalUser.value = { ...user.value }
    }

    showToast({ message: '头像上传成功', type: 'success' })
  } catch (e: any) {
    console.error('Failed to upload avatar', e)
    showToast({ message: e.response?.data?.error || '头像上传失败', type: 'error' })
  } finally {
    loading.value = false
  }
}

const confirmAvatarUpload = async () => {
  if (!pendingAvatarFile) return
  try {
    const transformed = await buildTransformedAvatarFile(pendingAvatarFile)
    await uploadAvatarFile(transformed)
  } catch (error) {
    console.error('Failed to process avatar transform', error)
    showToast({ message: '头像处理失败，已使用原图上传', type: 'error' })
    await uploadAvatarFile(pendingAvatarFile)
  }
  resetAvatarPreview()
}

const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    const file = target.files[0]
    
    // Client-side validation
    if (!file.type.startsWith('image/')) {
        showToast({ message: '只能上传图片文件', type: 'error' })
        return
    }
    if (file.size > 5 * 1024 * 1024) {
        showToast({ message: '文件大小不能超过5MB', type: 'error' })
        return
    }

    pendingAvatarFile = file
    avatarPreviewUrl.value = URL.createObjectURL(file)
    avatarZoom.value = 1
    avatarRotate.value = 0
    avatarOffsetX.value = 0
    avatarOffsetY.value = 0
    showAvatarPreviewModal.value = true
    target.value = ''
  }
}

const handlePasswordUpdate = async () => {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    showToast({ message: '两次输入的新密码不一致', type: 'error' })
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
    
    showToast({ message: '密码已更新', type: 'success' })
    showPasswordModal.value = false
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e: any) {
    console.error('Failed to update password', e)
    showToast({ message: e.response?.data || '密码更新失败', type: 'error' })
  } finally {
    loading.value = false
  }
}

const handleSendEmailVerify = async () => {
  if (!user.value.email || !/^[A-Za-z0-9+_.-]+@(.+)$/.test(user.value.email)) {
    showToast({ message: '请输入有效的邮箱地址', type: 'error' })
    return
  }
  if (!emailVerifyPassword.value) {
    showToast({ message: '请输入当前密码后再发送验证码', type: 'error' })
    return
  }
  if (emailVerifyCooldown.value > 0) {
    showToast({ message: `请 ${emailVerifyCooldown.value} 秒后再试`, type: 'warning' })
    return
  }
  emailVerifyLoading.value = true
  try {
    const token = localStorage.getItem('token')
    await axios.post('/api/user/send-email-verification', {
      email: user.value.email,
      currentPassword: emailVerifyPassword.value
    }, {
      headers: { Authorization: `Bearer ${token}` }
    })
    emailVerifySent.value = true
    emailVerifyCooldown.value = 60
    if (emailCooldownTimer) {
      clearInterval(emailCooldownTimer)
    }
    emailCooldownTimer = setInterval(() => {
      if (emailVerifyCooldown.value <= 1) {
        emailVerifyCooldown.value = 0
        if (emailCooldownTimer) {
          clearInterval(emailCooldownTimer)
          emailCooldownTimer = null
        }
        return
      }
      emailVerifyCooldown.value -= 1
    }, 1000)
    showToast({ message: '验证码已发送到邮箱，请查收', type: 'success' })
  } catch (e: any) {
    const serverData = e.response?.data
    const message =
      typeof serverData === 'string'
        ? serverData
        : serverData?.message || serverData?.error || '验证码发送失败'
    showToast({ message, type: 'error' })
  } finally {
    emailVerifyLoading.value = false
  }
}

const handleEmailVerifySubmit = async () => {
  if (!emailVerifyCode.value) {
    showToast({ message: '请输入验证码', type: 'error' })
    return
  }
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const payload = {
      ...user.value,
      ...preferences.value,
      currentPassword: emailVerifyPassword.value,
      emailVerifyCode: emailVerifyCode.value
    }
    const response = await axios.put('/api/user/profile', payload, {
      headers: { Authorization: `Bearer ${token}` }
    })
    authState.updateProfile(response.data)
    originalUser.value = { ...user.value }
    showToast({ message: '邮箱已验证并更新', type: 'success' })
    showEmailVerifyModal.value = false
    emailVerifyCode.value = ''
    emailVerifyPassword.value = ''
    emailVerifySent.value = false
    emailVerifyCooldown.value = 0
    if (emailCooldownTimer) {
      clearInterval(emailCooldownTimer)
      emailCooldownTimer = null
    }
  } catch (e: any) {
    const serverData = e.response?.data
    const message =
      typeof serverData === 'string'
        ? serverData
        : serverData?.message || serverData?.error || '邮箱验证失败'
    showToast({ message, type: 'error' })
  } finally {
    loading.value = false
  }
}

const closeEmailVerifyModal = () => {
  showEmailVerifyModal.value = false
  emailVerifyPassword.value = ''
  emailVerifyCode.value = ''
  emailVerifySent.value = false
  emailVerifyCooldown.value = 0
  if (emailCooldownTimer) {
    clearInterval(emailCooldownTimer)
    emailCooldownTimer = null
  }
}
</script>

<template>
  <div class="line-container page-container">
    <!-- Page Header -->
    <div class="page-header center-header">
      <h1 class="page-title">个人信息</h1>
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

    <Teleport to="body">
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

      <!-- Email Verify Modal -->
      <div v-if="showEmailVerifyModal" class="modal-overlay">
        <div class="modal-card email-verify-card">
          <h2>邮箱验证</h2>
          <p>请输入发送到新邮箱的验证码以完成邮箱修改。</p>
          <div class="form-group compact-group">
            <input
              v-model="emailVerifyPassword"
              type="password"
              class="line-input"
              placeholder="输入当前密码"
            />
          </div>
          <div class="form-group compact-group verify-code-row">
            <input v-model="emailVerifyCode" type="text" class="line-input" placeholder="输入验证码" />
            <button
              class="line-btn outline-btn send-code-btn"
              :disabled="emailVerifyLoading || emailVerifyCooldown > 0"
              @click="handleSendEmailVerify"
            >
              {{
                emailVerifyCooldown > 0
                  ? `重新发送(${emailVerifyCooldown}s)`
                  : (emailVerifySent ? '重新发送' : '发送验证码')
              }}
            </button>
          </div>
          <div class="form-actions">
            <button class="line-btn primary-btn" :disabled="loading" @click="handleEmailVerifySubmit">验证并保存</button>
            <button
              class="line-btn text-btn"
              @click="closeEmailVerifyModal"
            >
              取消
            </button>
          </div>
        </div>
      </div>

      <!-- Avatar Preview Modal -->
      <div v-if="showAvatarPreviewModal" class="modal-overlay" @click.self="resetAvatarPreview" @mouseup="onAvatarDragEnd">
        <div class="modal-card avatar-modal">
          <h2>头像预览编辑</h2>
          <p>这里展示上传后的圆形头像效果，可拖拽微调位置。</p>
          <div class="avatar-preview-stage">
            <div class="avatar-preview-frame" @mousemove="onAvatarDragMove" @mouseup="onAvatarDragEnd" @mouseleave="onAvatarDragEnd">
              <img
                v-if="avatarPreviewUrl"
                :src="avatarPreviewUrl"
                alt="avatar preview"
                class="avatar-preview-img"
                :style="{ transform: `translate(${avatarOffsetX}px, ${avatarOffsetY}px) scale(${avatarZoom}) rotate(${avatarRotate}deg)` }"
                @mousedown="onAvatarDragStart"
              />
            </div>
          </div>
          <div class="form-group">
            <label>缩放</label>
            <input v-model.number="avatarZoom" type="range" min="0.5" max="3" step="0.01" class="line-range" />
          </div>
          <div class="form-group">
            <label>旋转</label>
            <input v-model.number="avatarRotate" type="range" min="-180" max="180" step="1" class="line-range" />
          </div>
          <div class="form-group">
            <label>水平位置</label>
            <input v-model.number="avatarOffsetX" type="range" min="-180" max="180" step="1" class="line-range" />
          </div>
          <div class="form-group">
            <label>垂直位置</label>
            <input v-model.number="avatarOffsetY" type="range" min="-180" max="180" step="1" class="line-range" />
          </div>
          <div class="form-actions">
            <button class="line-btn outline-btn" @click="resetAvatarAdjustments">重置调整</button>
            <button class="line-btn primary-btn" :disabled="loading" @click="confirmAvatarUpload">确认上传</button>
            <button class="line-btn text-btn" @click="resetAvatarPreview">取消</button>
          </div>
        </div>
      </div>
    </Teleport>
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
  border-top: none;
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
  z-index: 2147483000;
  animation: fadeIn 0.2s ease;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2147483000;
  padding: 16px;
}

.modal-card {
  position: relative;
  z-index: 2147483001;
  width: 100%;
  max-width: 460px;
  background: var(--line-bg);
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-lg);
  box-shadow: var(--line-shadow-xl);
  padding: 24px;
}

.modal-card h2 {
  margin: 0 0 8px;
  color: var(--line-text);
}

.modal-card p {
  margin: 0 0 16px;
  color: var(--line-text-secondary);
  font-size: 14px;
}

.avatar-modal {
  max-width: 560px;
}

.avatar-preview-stage {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.avatar-preview-frame {
  width: 260px;
  height: 260px;
  border-radius: 50%;
  border: 2px solid var(--line-border);
  overflow: hidden;
  background:
    linear-gradient(45deg, #f6f8fb 25%, transparent 25%),
    linear-gradient(-45deg, #f6f8fb 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, #f6f8fb 75%),
    linear-gradient(-45deg, transparent 75%, #f6f8fb 75%);
  background-size: 18px 18px;
  background-position: 0 0, 0 9px, 9px -9px, -9px 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-preview-img {
  width: auto;
  height: auto;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  transition: transform 0.15s ease;
  cursor: grab;
  user-select: none;
}

.avatar-preview-img:active {
  cursor: grabbing;
}

.line-modal {
  position: relative;
  z-index: 2147483001;
  background: var(--line-bg);
  border-radius: var(--line-radius-lg);
  width: 100%;
  max-width: 400px;
  box-shadow: var(--line-shadow-xl);
  border: 1px solid var(--line-border);
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.email-verify-card {
  max-width: 640px;
}

.compact-group {
  margin-bottom: 10px;
}

.verify-code-row {
  display: grid;
  grid-template-columns: 1fr 132px;
  gap: 10px;
  align-items: center;
}

.send-code-btn {
  height: 42px;
  white-space: nowrap;
}

.line-range {
  -webkit-appearance: none;
  appearance: none;
  width: 100%;
  height: 24px;
  background: transparent;
  border: none;
  outline: none;
  padding: 0;
  box-shadow: none;
}

.line-range:focus,
.line-range:focus-visible {
  outline: none;
  box-shadow: none;
}

.line-range::-webkit-slider-runnable-track {
  height: 6px;
  border-radius: 999px;
  background: #d7dee8;
}

.line-range::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #0f8b8d;
  border: 2px solid #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
  margin-top: -6px;
}

.line-range::-moz-range-track {
  height: 6px;
  border-radius: 999px;
  background: #d7dee8;
}

.line-range::-moz-range-thumb {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #0f8b8d;
  border: 2px solid #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
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
  background-color: transparent;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  border-top: none;
  border-bottom-left-radius: var(--line-radius-lg);
  border-bottom-right-radius: var(--line-radius-lg);
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes slideUp { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

@media (max-width: 768px) {
  .profile-layout { grid-template-columns: 1fr; }
  .form-grid { grid-template-columns: 1fr; }
  .full-width { grid-column: auto; }
}
</style>
