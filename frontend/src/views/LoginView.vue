<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/client'
import { authState } from '@/states/authState'

const username = ref('')
const password = ref('')
const error = ref('')
const router = useRouter()

const handleLogin = async () => {
  try {
    const response = await authApi.authLoginPost({
      username: username.value,
      password: password.value,
    })
    const token = response.data.accessToken
    if (token) {
      authState.login(token)
      router.push('/')
    }
  } catch (err) {
    error.value = 'Login failed. Please check your credentials.'
    console.error(err)
  }
}
</script>

<template>
  <div class="login-wrapper">
    <div class="login-container google-card">
      <div class="logo-area">
        <span class="google-logo">G</span>
      </div>
      <h1>Sign in</h1>
      <p class="subtitle">to continue to Question Bank</p>
      
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <input id="username" v-model="username" type="text" required placeholder="Username" class="google-input" />
        </div>
        <div class="form-group">
          <input id="password" v-model="password" type="password" required placeholder="Password" class="google-input" />
        </div>
        
        <div class="actions">
          <router-link to="/register" class="create-account">Create account</router-link>
          <button type="submit" class="primary-btn">Next</button>
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
  background-color: #fff; /* Google login page is usually white or very light grey */
}

.login-container {
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
  color: #4285f4; /* Or multi-color if we want to get fancy */
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

.google-input {
  width: 100%;
  padding: 13px 15px;
  font-size: 16px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  color: #202124;
  transition: border-color 0.2s;
}

.google-input:focus {
  border-color: #1a73e8;
  outline: none;
  border-width: 2px;
  padding: 12px 14px; /* Adjust for border width */
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 40px;
}

.create-account {
  color: #1a73e8;
  font-weight: 500;
  text-decoration: none;
  font-size: 14px;
  border-radius: 4px;
  padding: 6px 8px;
}

.create-account:hover {
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

.error {
  color: #d93025;
  margin-top: 20px;
  font-size: 12px;
  text-align: left;
}
</style>
