<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/client'
import { authState } from '@/states/authState'
import axios from 'axios'

const username = ref('')
const password = ref('')
const error = ref('')
const router = useRouter()

const handleLogin = async () => {
  error.value = ''
  try {
    const response = await authApi.apiAuthLoginPost({
      username: username.value,
      password: password.value
    })
    const token = response.data.accessToken
    if (token) {
      authState.login(token)
      router.push('/')
    }
  } catch (err) {
    if (axios.isAxiosError(err)) {
      if (err.response?.status === 429) {
        error.value = '登录失败次数过多，账号已被临时锁定15分钟'
      } else if (err.response?.status === 503) {
        error.value = '系统维护中，请稍后再试'
      } else if (err.response?.status === 401) {
        error.value = '用户名或密码错误'
      } else {
        error.value = '登录失败，请稍后再试'
      }
    } else {
      error.value = '网络连接失败，请检查网络'
    }
    console.error(err)
  }
}
</script>

<template>
  <div class="login-wrapper">
    <div class="login-container line-card auth-card">
      <div class="logo-area">
        <span class="app-logo">UQ</span>
      </div>
      <h1 class="page-title">欢迎回来</h1>
      <p class="subtitle">登录以继续</p>

      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <input
            id="username"
            v-model="username"
            type="text"
            required
            placeholder="用户名"
            class="google-input"
          />
        </div>
        <div class="form-group">
          <input
            id="password"
            v-model="password"
            type="password"
            required
            placeholder="密码"
            class="google-input"
          />
        </div>

        <div class="forgot-password">
          <router-link to="/forgot-password">忘记密码？</router-link>
        </div>

        <div class="actions">
          <router-link to="/register" class="create-account">创建账号</router-link>
          <button type="submit" class="primary-btn">登录</button>
        </div>

        <p v-if="error" class="error">{{ error }}</p>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
  background: transparent;
}

.login-container {
  width: 100%;
  max-width: 400px;
  padding: 48px 40px;
  text-align: center;
  /* visual card styles provided by shared .auth-card */
}

.login-container:hover {
  transform: none;
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

h1 {
  color: var(--line-text);
  margin-bottom: 8px;
}

.subtitle {
  font-size: 15px;
  color: var(--line-text-secondary);
  margin-bottom: 40px;
}

.form-group {
  margin-bottom: 20px;
  text-align: left;
}

/* google-input is handled globally, but ensuring specific overrides if needed */
.google-input {
  /* Inherits from global styles */
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 32px;
}

.forgot-password {
  text-align: right;
  margin-top: 8px;
}

.forgot-password a {
  color: var(--line-text-secondary);
  font-size: 13px;
  text-decoration: none;
  transition: color 0.2s;
}

.forgot-password a:hover {
  color: var(--line-primary);
}

.create-account {
  color: var(--line-primary);
  font-weight: 500;
  text-decoration: none;
  font-size: 14px;
  transition: opacity 0.2s;
}

.create-account:hover {
  opacity: 0.8;
  text-decoration: underline;
  background: none;
}

.primary-btn {
  /* Inherits from global styles */
}

.error {
  color: #d93025;
  margin-top: 20px;
  font-size: 12px;
  text-align: left;
}
</style>
