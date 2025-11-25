<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { RouterView, useRouter } from 'vue-router'
import { authState } from '@/states/authState'

const router = useRouter()

const isTeacher = computed(() => {
  return authState.user.role === 'TEACHER'
})

const isAdmin = computed(() => {
  return authState.user.role === 'ADMIN'
})

onMounted(() => {
  if (authState.isAuthenticated) {
    authState.fetchUser()
  }
})

const handleLogout = () => {
  authState.logout()
  router.push('/login')
}
</script>

<template>
  <header class="google-header">
    <div class="header-content">
      <div class="logo">
        <RouterLink to="/">
          <span class="logo-blue">U</span>niversal <span class="logo-grey">QBank</span>
        </RouterLink>
      </div>
      <nav>
        <template v-if="authState.isAuthenticated">
          <RouterLink to="/">Home</RouterLink>
          <template v-if="isTeacher">
            <RouterLink to="/questions">Questions</RouterLink>
            <RouterLink to="/papers">Papers</RouterLink>
            <RouterLink to="/paper-generation">Generate</RouterLink>
            <RouterLink to="/grading">Grading</RouterLink>
          </template>
          <template v-if="isAdmin">
            <RouterLink to="/admin/users">Users</RouterLink>
            <RouterLink to="/admin/system">System</RouterLink>
          </template>
          <RouterLink v-if="!isAdmin" to="/exam">Exam</RouterLink>
        </template>
        
        <template v-if="!authState.isAuthenticated">
          <RouterLink to="/login" class="auth-link">Login</RouterLink>
          <RouterLink to="/register" class="auth-link">Register</RouterLink>
        </template>
        <template v-else>
          <div class="user-menu">
            <a href="#" @click.prevent="handleLogout" class="auth-link logout">Logout</a>
            <RouterLink to="/profile" class="avatar-link" title="My Profile">
              <div class="header-avatar" :style="{ backgroundImage: authState.user.avatarUrl ? `url(${authState.user.avatarUrl})` : '' }">
                <span v-if="!authState.user.avatarUrl">{{ authState.user.username.charAt(0).toUpperCase() }}</span>
              </div>
            </RouterLink>
          </div>
        </template>
      </nav>
    </div>
  </header>

  <main class="main-content">
    <RouterView />
  </main>
</template>

<style scoped>
.google-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background-color: #ffffff;
  border-bottom: 1px solid #dadce0;
  height: 64px; /* Standard Google Toolbar height */
  display: flex;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.header-content {
  max-width: 1200px; /* Wider layout */
  width: 100%;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo a {
  font-family: 'Product Sans', 'Google Sans', 'Roboto', sans-serif;
  font-weight: 400;
  font-size: 22px;
  color: #5f6368;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 4px;
}

.logo-blue {
  color: #1a73e8;
  font-weight: 500;
  font-size: 24px;
}

nav {
  display: flex;
  gap: 32px;
  font-size: 14px;
  font-family: 'Google Sans', 'Roboto', sans-serif;
  font-weight: 500;
}

nav a {
  color: #5f6368;
  text-decoration: none;
  transition: color 0.2s;
  padding: 8px 0;
  border-bottom: 2px solid transparent;
}

nav a:hover {
  color: #202124;
}

nav a.router-link-active {
  color: #1a73e8;
  border-bottom-color: #1a73e8;
}

.auth-link {
  font-weight: 500;
}

.logout {
  color: #5f6368;
}
.logout:hover {
  color: #d93025; /* Google Red */
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #1a73e8; /* Google Blue */
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
  background-size: cover;
  background-position: center;
}

.header-avatar:hover {
  background-color: #174ea6;
}

.avatar-link {
  text-decoration: none;
  border-bottom: none; /* Override nav link style */
  padding: 0;
}

.avatar-link:hover {
  background-color: transparent;
}

.main-content {
  padding-top: 20px;
  min-height: calc(100vh - 50px);
}
</style>
