<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { RouterView, RouterLink, useRouter } from 'vue-router'
import { authState } from '@/states/authState'

const router = useRouter()

const isTeacher = computed(() => authState.user.role === 'TEACHER')
const isAdmin = computed(() => authState.user.role === 'ADMIN')

const initial = computed(() => {
  const name = authState.user.nickname || authState.user.username || ''
  return name.length > 0 ? name.charAt(0).toUpperCase() : 'U'
})

const avatarStyle = computed(() => {
  if (authState.user.avatarUrl) {
    return { backgroundImage: `url(${authState.user.avatarUrl})` }
  }
  return { backgroundColor: '#1a73e8' } // Google Blue default
})

onMounted(() => {
  if (authState.isAuthenticated) authState.fetchUser()
})

const handleLogout = () => {
  authState.logout()
  router.push('/login')
}
</script>

<template>
  <header class="google-header">
    <div class="header-content">
      <!-- Logo -->
      <div class="logo" @click="$router.push('/')">
        <span class="logo-blue">U</span>niversal&nbsp;<span class="logo-grey">QBank</span>
      </div>

      <!-- Navigation Links -->
      <nav class="nav-links" v-if="authState.isAuthenticated">
        <RouterLink to="/" class="nav-item">Home</RouterLink>
        <template v-if="isTeacher">
          <RouterLink to="/questions" class="nav-item">Questions</RouterLink>
          <RouterLink to="/papers" class="nav-item">Papers</RouterLink>
          <RouterLink to="/paper-generation" class="nav-item">Generate</RouterLink>
          <RouterLink to="/grading" class="nav-item">Grading</RouterLink>
          <RouterLink to="/knowledge-point-manage" class="nav-item">Knowledge Points</RouterLink>
        </template>
        <template v-if="isAdmin">
          <RouterLink to="/admin/users" class="nav-item">Users</RouterLink>
          <RouterLink to="/admin/system" class="nav-item">System</RouterLink>
        </template>
      </nav>

      <!-- User Actions -->
      <div class="user-actions">
        <template v-if="!authState.isAuthenticated">
          <RouterLink to="/login" class="google-btn text-btn">Login</RouterLink>
          <RouterLink to="/register" class="google-btn primary-btn">Register</RouterLink>
        </template>
        <template v-else>
          <div class="user-profile">
            <div class="avatar" :style="avatarStyle">
              <span v-if="!authState.user.avatarUrl">{{ initial }}</span>
            </div>
            <button @click="handleLogout" class="google-btn text-btn logout-btn">Logout</button>
          </div>
        </template>
      </div>
    </div>
  </header>

  <main class="main-container">
    <RouterView />
  </main>
</template>

<style scoped>
.google-header {
  background-color: #fff;
  border-bottom: 1px solid #dadce0;
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 1000;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.logo {
  font-family: 'Google Sans', sans-serif;
  font-size: 22px;
  color: #5f6368;
  cursor: pointer;
  display: flex;
  align-items: center;
  font-weight: 500;
}

.logo-blue { color: #4285f4; }
.logo-grey { color: #5f6368; }

.nav-links {
  display: flex;
  gap: 8px;
  margin-left: 40px;
  flex: 1;
  overflow-x: auto;
}

.nav-item {
  color: #5f6368;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  padding: 8px 12px;
  border-radius: 4px;
  white-space: nowrap;
  transition: background-color 0.2s, color 0.2s;
}

.nav-item:hover {
  background-color: #f1f3f4;
  color: #202124;
}

.nav-item.router-link-active {
  color: #1a73e8;
  background-color: #e8f0fe;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 500;
  font-size: 16px;
  cursor: pointer;
}

.logout-btn {
  font-size: 14px;
  padding: 6px 12px;
}

.main-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

@media (max-width: 768px) {
  .nav-links {
    display: none;
  }
  
  .header-content {
    padding: 0 16px;
  }
}
</style>
