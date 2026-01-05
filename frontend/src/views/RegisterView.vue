<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const role = ref('USER')
const error = ref('')
const loading = ref(false)

const handleRegister = async () => {
  if (password.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  error.value = ''

  try {
    await axios.post('/api/auth/register', {
      username: username.value,
      password: password.value,
      role: role.value
    })
    alert('注册成功！请登录')
    router.push('/login')
  } catch (err: any) {
    error.value = err.response?.data || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-wrapper">
    <div class="register-container google-card">
      <div class="logo-area">
        <span class="google-logo">G</span>
      </div>
      <h1>创建账号</h1>
      <p class="subtitle">继续使用题库系统</p>
      
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <input v-model="username" type="text" required placeholder="用户名" class="google-input" />
        </div>
        
        <div class="form-row">
          <div class="form-group half">
            <input v-model="password" type="password" required placeholder="密码" class="google-input" />
          </div>
          <div class="form-group half">
            <input v-model="confirmPassword" type="password" required placeholder="确认密码" class="google-input" />
          </div>
        </div>
        <p class="hint-text">密码至少8位，包含字母、数字和符号</p>

        <div class="form-group">
          <div class="select-wrapper">
            <select v-model="role" class="google-input">
              <option value="USER">学生</option>
              <option value="TEACHER">教师</option>
            </select>
          </div>
        </div>

        <div class="actions">
          <router-link to="/login" class="sign-in-link">已有账号？登录</router-link>
          <button type="submit" :disabled="loading" class="primary-btn">
            {{ loading ? '创建中...' : '注册' }}
          </button>
        </div>
        
        <p v-if="error" class="error">{{ error }}</p>
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
  background-color: #fff;
}

.register-container {
  width: 100%;
  max-width: 450px;
  padding: 48px 40px 36px;
  text-align: center;
  border: 1px solid #dadce0;
  border-radius: 8px;
}

.logo-area {
  margin-bottom: 10px;
}

.google-logo {
  font-family: 'Product Sans', 'Google Sans', sans-serif;
  font-size: 24px;
  font-weight: bold;
  color: #4285f4;
}

h1 {
  font-family: 'Google Sans', sans-serif;
  font-size: 24px;
  font-weight: 400;
  color: #202124;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 16px;
  color: #202124;
  margin-bottom: 40px;
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
  border: 1px solid #dadce0;
  border-radius: 4px;
  color: #202124;
  transition: border-color 0.2s;
  background-color: #fff;
}

.google-input:focus {
  border-color: #1a73e8;
  outline: none;
  border-width: 2px;
  padding: 12px 14px;
}

.hint-text {
  font-size: 12px;
  color: #5f6368;
  text-align: left;
  margin-top: 0;
  margin-bottom: 24px;
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 40px;
}

.sign-in-link {
  color: #1a73e8;
  font-weight: 500;
  text-decoration: none;
  font-size: 14px;
  border-radius: 4px;
  padding: 6px 8px;
}

.sign-in-link:hover {
  background-color: #f6fafe;
}

.primary-btn {
  background-color: #1a73e8;
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
  background-color: #1557b0;
  box-shadow: 0 1px 2px 0 rgba(60,64,67,0.3), 0 1px 3px 1px rgba(60,64,67,0.15);
}

.primary-btn:disabled {
  background-color: #dadce0;
  color: #fff;
  cursor: default;
  box-shadow: none;
}

.error {
  color: #d93025;
  margin-top: 20px;
  font-size: 12px;
  text-align: left;
}
</style>
