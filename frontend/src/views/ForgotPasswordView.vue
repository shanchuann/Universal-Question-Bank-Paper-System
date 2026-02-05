<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { Mail, Lock, ArrowLeft, Send, Check } from 'lucide-vue-next'

const router = useRouter()
const step = ref<'email' | 'verify' | 'reset' | 'success'>('email')

const email = ref('')
const verificationCode = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const error = ref('')
const loading = ref(false)
const countdown = ref(0)

let countdownTimer: ReturnType<typeof setInterval> | null = null

const canSendCode = computed(() => {
  return email.value && email.value.includes('@') && countdown.value === 0
})

const startCountdown = () => {
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      if (countdownTimer) clearInterval(countdownTimer)
    }
  }, 1000)
}

const sendCode = async () => {
  if (!canSendCode.value) return
  
  error.value = ''
  loading.value = true
  
  try {
    await axios.post('/api/auth/send-code', {
      email: email.value,
      type: 'reset_password'
    })
    startCountdown()
    step.value = 'verify'
  } catch (err: any) {
    const errorData = err.response?.data
    if (typeof errorData === 'object' && errorData.error) {
      error.value = errorData.error
    } else {
      error.value = '发送验证码失败，请稍后再试'
    }
  } finally {
    loading.value = false
  }
}

const verifyAndReset = async () => {
  if (newPassword.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致'
    return
  }
  
  error.value = ''
  loading.value = true
  
  try {
    await axios.post('/api/auth/reset-password', {
      email: email.value,
      verificationCode: verificationCode.value,
      newPassword: newPassword.value
    })
    step.value = 'success'
  } catch (err: any) {
    const errorData = err.response?.data
    if (typeof errorData === 'object' && errorData.error) {
      error.value = errorData.error
    } else {
      error.value = '重置密码失败，请稍后再试'
    }
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="forgot-wrapper">
    <div class="forgot-container line-card">
      <div class="logo-area">
        <span class="app-logo">UQ</span>
      </div>
      
      <!-- Step 1: 输入邮箱 -->
      <template v-if="step === 'email'">
        <h1 class="page-title">找回密码</h1>
        <p class="subtitle">输入您的注册邮箱，我们将发送验证码</p>
        
        <form @submit.prevent="sendCode">
          <div class="form-group">
            <div class="input-with-icon">
              <Mail :size="18" class="input-icon" />
              <input 
                v-model="email" 
                type="email" 
                required 
                placeholder="注册邮箱" 
                class="google-input icon-input" 
              />
            </div>
          </div>
          
          <div class="actions">
            <button type="button" class="text-link" @click="goToLogin">
              <ArrowLeft :size="16" />
              返回登录
            </button>
            <button 
              type="submit" 
              class="primary-btn" 
              :disabled="!canSendCode || loading"
            >
              <Send :size="16" v-if="!loading" />
              {{ loading ? '发送中...' : '发送验证码' }}
            </button>
          </div>
          
          <p v-if="error" class="error">{{ error }}</p>
        </form>
      </template>

      <!-- Step 2: 输入验证码和新密码 -->
      <template v-else-if="step === 'verify'">
        <h1 class="page-title">重置密码</h1>
        <p class="subtitle">验证码已发送至 {{ email }}</p>
        
        <form @submit.prevent="verifyAndReset">
          <div class="form-group">
            <input 
              v-model="verificationCode" 
              type="text" 
              required 
              placeholder="6位验证码" 
              class="google-input code-input"
              maxlength="6"
            />
            <button 
              type="button" 
              class="resend-btn"
              :disabled="countdown > 0"
              @click="sendCode"
            >
              {{ countdown > 0 ? `${countdown}s 后重发` : '重新发送' }}
            </button>
          </div>
          
          <div class="form-group">
            <div class="input-with-icon">
              <Lock :size="18" class="input-icon" />
              <input 
                v-model="newPassword" 
                type="password" 
                required 
                placeholder="新密码" 
                class="google-input icon-input" 
              />
            </div>
          </div>
          
          <div class="form-group">
            <div class="input-with-icon">
              <Lock :size="18" class="input-icon" />
              <input 
                v-model="confirmPassword" 
                type="password" 
                required 
                placeholder="确认新密码" 
                class="google-input icon-input" 
              />
            </div>
          </div>
          
          <div class="actions">
            <button type="button" class="text-link" @click="step = 'email'">
              <ArrowLeft :size="16" />
              更换邮箱
            </button>
            <button 
              type="submit" 
              class="primary-btn" 
              :disabled="loading || !verificationCode || !newPassword"
            >
              {{ loading ? '提交中...' : '重置密码' }}
            </button>
          </div>
          
          <p v-if="error" class="error">{{ error }}</p>
        </form>
      </template>

      <!-- Step 3: 成功 -->
      <template v-else-if="step === 'success'">
        <div class="success-container">
          <div class="success-icon">
            <Check :size="48" />
          </div>
          <h1 class="page-title">密码重置成功</h1>
          <p class="subtitle">您现在可以使用新密码登录了</p>
          <button class="primary-btn full-width" @click="goToLogin">
            返回登录
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.forgot-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
  background-color: #fff;
}

.forgot-container {
  width: 100%;
  max-width: 420px;
  padding: 48px 40px;
  text-align: center;
}

.logo-area {
  margin-bottom: 24px;
}

.app-logo {
  font-family: inherit;
  font-size: 32px;
  font-weight: 700;
  color: var(--line-primary);
  letter-spacing: -0.05em;
  border: 2px solid var(--line-primary);
  padding: 8px 12px;
  border-radius: 8px;
}

.page-title {
  color: var(--line-text);
  margin: 0 0 8px 0;
  font-size: 24px;
}

.subtitle {
  font-size: 14px;
  color: var(--line-text-secondary);
  margin: 0 0 32px 0;
}

.form-group {
  margin-bottom: 16px;
  position: relative;
}

.google-input {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  border: 1px solid var(--line-border);
  border-radius: 8px;
  font-size: 15px;
  background: var(--line-bg);
  color: var(--line-text);
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.google-input:focus {
  outline: none;
  border-color: var(--line-primary);
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.15);
}

.input-with-icon {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--line-text-secondary);
}

.icon-input {
  padding-left: 44px;
}

.code-input {
  text-align: center;
  font-size: 20px;
  letter-spacing: 8px;
  font-weight: 600;
}

.resend-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  color: var(--line-primary);
  font-size: 13px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 4px;
}

.resend-btn:hover:not(:disabled) {
  background: rgba(26, 115, 232, 0.1);
}

.resend-btn:disabled {
  color: var(--line-text-secondary);
  cursor: default;
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 32px;
}

.text-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--line-primary);
  font-weight: 500;
  text-decoration: none;
  font-size: 14px;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
}

.text-link:hover {
  background-color: rgba(26, 115, 232, 0.1);
}

.primary-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background-color: var(--line-primary);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 500;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.primary-btn:hover:not(:disabled) {
  background-color: #1557b0;
  box-shadow: 0 2px 8px rgba(26, 115, 232, 0.3);
}

.primary-btn:disabled {
  background-color: var(--line-border);
  cursor: default;
}

.primary-btn.full-width {
  width: 100%;
  margin-top: 24px;
}

.error {
  color: #d93025;
  margin-top: 20px;
  font-size: 13px;
  text-align: left;
}

.success-container {
  padding: 20px 0;
}

.success-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #34a853;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
}
</style>
