<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@/composables/useToast'
import axios from 'axios'
import GoogleSelect from '@/components/GoogleSelect.vue'

const router = useRouter()
const { showToast } = useToast()

// 表单数据
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const email = ref('')
const verificationCode = ref('')
const role = ref('USER')
const roleOptions = [
  { value: 'USER', label: '学生' },
  { value: 'TEACHER', label: '教师' }
]

// 状态
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)

// 注册设置
const settings = ref({
  allowRegistration: true,
  requireEmailVerification: false,
  passwordMinLength: 6
})

// 获取注册设置
const fetchSettings = async () => {
  try {
    const response = await axios.get('/api/auth/registration-settings')
    settings.value = response.data
  } catch (error) {
    console.error('Failed to fetch registration settings', error)
  }
}

onMounted(() => {
  fetchSettings()
})

// 发送验证码
const sendCode = async () => {
  if (!email.value) {
    showToast({ message: '请输入邮箱地址', type: 'error' })
    return
  }

  if (!/^[A-Za-z0-9+_.-]+@(.+)$/.test(email.value)) {
    showToast({ message: '邮箱格式不正确', type: 'error' })
    return
  }

  sendingCode.value = true
  try {
    await axios.post('/api/auth/send-code', {
      email: email.value,
      type: 'register'
    })
    showToast({ message: '验证码已发送到您的邮箱', type: 'success' })

    // 开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (err: any) {
    const errorMsg = err.response?.data?.error || '发送验证码失败'
    showToast({ message: errorMsg, type: 'error' })
  } finally {
    sendingCode.value = false
  }
}

// 验证表单
const validateForm = () => {
  if (!username.value.trim()) {
    showToast({ message: '请输入用户名', type: 'error' })
    return false
  }

  if (username.value.length < 3 || username.value.length > 20) {
    showToast({ message: '用户名长度需在3-20个字符之间', type: 'error' })
    return false
  }

  if (password.value.length < settings.value.passwordMinLength) {
    showToast({ message: `密码长度不能少于${settings.value.passwordMinLength}位`, type: 'error' })
    return false
  }

  if (password.value !== confirmPassword.value) {
    showToast({ message: '两次输入的密码不一致', type: 'error' })
    return false
  }

  if (settings.value.requireEmailVerification) {
    if (!email.value) {
      showToast({ message: '请输入邮箱地址', type: 'error' })
      return false
    }
    if (!verificationCode.value) {
      showToast({ message: '请输入验证码', type: 'error' })
      return false
    }
  }

  return true
}

// 注册
const handleRegister = async () => {
  if (!validateForm()) return

  loading.value = true
  try {
    await axios.post('/api/auth/register', {
      username: username.value,
      password: password.value,
      role: role.value,
      email: email.value || undefined,
      verificationCode: verificationCode.value || undefined
    })

    showToast({ message: '注册成功！即将跳转到登录页面...', type: 'success' })
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  } catch (err: any) {
    const errorData = err.response?.data
    const errorMsg =
      typeof errorData === 'object' ? errorData.error : errorData || '注册失败，请稍后再试'
    showToast({ message: errorMsg, type: 'error' })
  } finally {
    loading.value = false
  }
}

const passwordHint = computed(() => {
  return `密码至少${settings.value.passwordMinLength}位`
})
</script>

<template>
  <div class="register-wrapper">
    <div class="register-container google-card auth-card">
      <div class="logo-area">
        <span class="app-logo">UQ</span>
      </div>
      <h1 class="page-title">创建账号</h1>
      <p class="subtitle">继续使用题库系统</p>

      <!-- 注册已关闭提示 -->
      <div v-if="!settings.allowRegistration" class="registration-closed">
        <p>系统当前已关闭注册，请联系管理员。</p>
        <router-link to="/login" class="back-link">返回登录</router-link>
      </div>

      <form v-else @submit.prevent="handleRegister">
        <div class="form-group">
          <input
            v-model="username"
            type="text"
            required
            placeholder="用户名"
            class="google-input"
          />
        </div>

        <div class="form-row">
          <div class="form-group half">
            <input
              v-model="password"
              type="password"
              required
              placeholder="密码"
              class="google-input"
            />
          </div>
          <div class="form-group half">
            <input
              v-model="confirmPassword"
              type="password"
              required
              placeholder="确认密码"
              class="google-input"
            />
          </div>
        </div>
        <p class="hint-text">{{ passwordHint }}</p>

        <!-- 邮箱验证区域 -->
        <template v-if="settings.requireEmailVerification">
          <div class="form-group">
            <input
              v-model="email"
              type="email"
              required
              placeholder="邮箱地址"
              class="google-input"
            />
          </div>

          <div class="form-group verification-group">
            <input
              v-model="verificationCode"
              type="text"
              required
              placeholder="验证码"
              class="google-input verification-input"
              maxlength="6"
            />
            <button
              type="button"
              class="send-code-btn"
              :disabled="sendingCode || countdown > 0"
              @click="sendCode"
            >
              {{
                countdown > 0 ? `${countdown}秒后重发` : sendingCode ? '发送中...' : '发送验证码'
              }}
            </button>
          </div>
        </template>

        <!-- 可选邮箱（不需要验证时） -->
        <div v-else class="form-group">
          <input v-model="email" type="email" placeholder="邮箱地址（选填）" class="google-input" />
        </div>

        <div class="form-group">
          <GoogleSelect v-model="role" :options="roleOptions" placeholder="选择角色" />
        </div>

        <div class="actions">
          <router-link to="/login" class="sign-in-link">已有账号？登录</router-link>
          <button type="submit" :disabled="loading" class="primary-btn">
            {{ loading ? '创建中...' : '注册' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.register-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
  background-color: transparent;
}

.register-container {
  width: 100%;
  max-width: 450px;
  padding: 48px 40px 36px;
  text-align: center;
  /* 视觉样式由全局 .auth-card 提供，避免在组件内覆盖 */
  border-radius: var(--line-radius-md);
}

.logo-area {
  margin-bottom: 10px;
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

h1 {
  color: var(--line-text);
  margin-bottom: 8px;
}

.subtitle {
  font-size: 16px;
  color: var(--line-text);
  margin-bottom: 40px;
}

.registration-closed {
  padding: 40px 20px;
  text-align: center;
}

.registration-closed p {
  color: var(--line-text-secondary);
  margin-bottom: 20px;
}

.back-link {
  color: var(--line-primary);
  font-weight: 500;
  text-decoration: none;
}

.form-group {
  margin-bottom: 24px;
  text-align: left;
}

.form-row {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
}

.half {
  flex: 1;
  margin-bottom: 0;
}

.google-input {
  width: 100%;
  padding: 13px 15px;
  font-size: 16px;
  border: 1px solid var(--line-border);
  border-radius: 4px;
  color: var(--line-text);
  transition: border-color 0.2s;
  background-color: var(--line-bg);
}

.google-input:focus {
  border-color: var(--line-primary);
  outline: none;
  border-width: 2px;
  padding: 12px 14px;
}

.hint-text {
  font-size: 12px;
  color: var(--line-text-secondary);
  text-align: left;
  margin-top: 0;
  margin-bottom: 24px;
}

.verification-group {
  display: flex;
  gap: 10px;
}

.verification-input {
  flex: 1;
}

.send-code-btn {
  padding: 0 16px;
  background-color: var(--line-bg-soft);
  color: var(--line-primary);
  border: 1px solid var(--line-primary);
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.send-code-btn:hover:not(:disabled) {
  background-color: rgba(26, 115, 232, 0.1);
}

.send-code-btn:disabled {
  color: var(--line-text-secondary);
  border-color: var(--line-border);
  cursor: not-allowed;
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 40px;
}

.sign-in-link {
  color: var(--line-primary);
  font-weight: 500;
  text-decoration: none;
  font-size: 14px;
  border-radius: 4px;
  padding: 6px 8px;
}

.sign-in-link:hover {
  background-color: var(--line-bg-soft);
}

.primary-btn {
  background-color: var(--line-primary);
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 4px;
  font-weight: 500;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.primary-btn:hover {
  background-color: var(--line-primary-hover);
  box-shadow: var(--line-shadow-sm);
}

.primary-btn:disabled {
  background-color: var(--line-border);
  color: var(--line-bg);
  cursor: default;
  box-shadow: none;
}
</style>
