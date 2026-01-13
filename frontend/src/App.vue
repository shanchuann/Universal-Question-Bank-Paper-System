<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { RouterView, useRouter, useRoute } from 'vue-router'
import { authState, type UserRole } from '@/states/authState'

const router = useRouter()
const route = useRoute()

type NavChild = { to: string; label: string }
type NavGroup = {
  label: string
  roles?: Array<Exclude<UserRole, ''>>
  children?: NavChild[]
  to?: string
}

const activeDropdown = ref<string | null>(null)

const navGroups = computed<NavGroup[]>(() => {
  const role = authState.user.role
  const isStudent = role !== 'TEACHER' && role !== 'ADMIN'
  const isTeacher = role === 'TEACHER'
  const isAdmin = role === 'ADMIN'

  const groups: NavGroup[] = [
    { to: '/', label: '首页' },
    {
      label: '题库管理',
      roles: ['TEACHER', 'ADMIN'],
      children: [
        { to: '/questions', label: '题目列表' },
        { to: '/questions/add', label: '添加题目' },
        { to: '/import', label: '批量导入' },
        { to: '/questions/review', label: '题目审核' },
        { to: '/knowledge-point-manage', label: '知识点管理' }
      ]
    },
    {
      label: '试卷管理',
      roles: ['TEACHER', 'ADMIN'],
      children: [
        { to: '/papers', label: '试卷列表' },
        { to: '/papers/manual', label: '手动组卷' },
        { to: '/paper-generation', label: '智能组卷' }
      ]
    },
    {
      label: '考试管理',
      to: '/exams/manage',
      roles: ['TEACHER', 'ADMIN']
    },
    {
      label: '阅卷评分',
      to: '/grading',
      roles: ['TEACHER', 'ADMIN']
    },
    // 学生专属菜单（直接展示在导航栏）
    ...(isStudent ? [
      { to: '/exams/list', label: '我的考试' },
      { to: '/practice', label: '练习模式' },
      { to: '/leaderboard', label: '排行榜' },
      { to: '/questions/add', label: '我要出题' },
      { to: '/my-organizations', label: '我的班级' }
    ] : []),
    // 教师的班级管理
    ...(isTeacher ? [{
      label: '班级管理',
      children: [
        { to: '/admin/organizations', label: '我的班级' },
        { to: '/leaderboard', label: '排行榜' }
      ]
    }] : []),
    // 管理员的系统管理
    ...(isAdmin ? [{
      label: '系统管理',
      children: [
        { to: '/admin/organizations', label: '组织架构' },
        { to: '/admin/roles', label: '角色权限' },
        { to: '/admin/users', label: '用户管理' },
        { to: '/admin/system', label: '系统设置' }
      ]
    }] : [])
  ]

  return groups
})

const initial = computed(() => {
  const name = authState.user.nickname || authState.user.username || ''
  return name.length > 0 ? name.charAt(0).toUpperCase() : 'U'
})

const avatarStyle = computed(() => {
  if (authState.user.avatarUrl) {
    return { backgroundImage: `url(${authState.user.avatarUrl})` }
  }
  return { backgroundColor: '#1a73e8' }
})

onMounted(() => {
  if (authState.isAuthenticated) authState.fetchUser()
})

const handleLogout = () => {
  authState.logout()
  router.push('/login')
}

const goProfile = () => {
  router.push('/profile')
}

const toggleDropdown = (label: string) => {
  activeDropdown.value = activeDropdown.value === label ? null : label
}

const closeDropdown = () => {
  activeDropdown.value = null
}

const navigateTo = (to: string) => {
  router.push(to)
  closeDropdown()
}

const isRouteActive = (path: string) => {
  if (!path) return false
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const isExactActive = (path: string) => {
  return route.path === path
}

const isGroupActive = (group: NavGroup) => {
  if (group.to) return isRouteActive(group.to)
  if (group.children) {
    return group.children.some(child => isRouteActive(child.to))
  }
  return false
}
</script>

<template>
  <header class="google-header">
    <div class="header-content">
      <!-- Logo -->
      <div class="logo" @click="$router.push('/')">
        <span class="logo-blue">UQ</span><span class="logo-gray">Bank</span>
      </div>

      <!-- Navigation Links -->
      <nav class="nav-links" v-if="authState.isAuthenticated">
        <template v-for="group in navGroups" :key="group.label">
          <!-- 角色检查：如果有roles限制，检查当前用户角色 -->
          <template v-if="!group.roles || (authState.user.role && group.roles.includes(authState.user.role as Exclude<UserRole, ''>))">
            <!-- 单个链接 -->
            <div v-if="group.to" class="nav-item" @click="navigateTo(group.to)">
              {{ group.label }}
            </div>
            
            <!-- 下拉菜单 -->
            <div v-else-if="group.children" class="nav-dropdown" @mouseleave="closeDropdown">
              <div 
                class="nav-item dropdown-trigger" 
                :class="{ active: activeDropdown === group.label }"
                @click="toggleDropdown(group.label)"
                @mouseenter="activeDropdown = group.label"
              >
                {{ group.label }}
                <svg class="dropdown-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </div>
              <div
                   v-show="activeDropdown === group.label" class="dropdown-menu">
                <div 
                  v-for="child in group.children" 
                  :key="child.to" 
                  class="dropdown-item"
                  :class="{ 'active-nav': isExactActive(child.to) }"
                  @click="navigateTo(child.to)"
                >
                  {{ child.label }}
                </div>
              </div>
            </div>
          </template>
        </template>
      </nav>

      <!-- User Actions -->
      <div class="user-actions">
        <template v-if="!authState.isAuthenticated">
          <button class="google-btn text-btn" @click="$router.push('/login')">登录</button>
          <button class="google-btn primary-btn" @click="$router.push('/register')">注册</button>
        </template>
        <template v-else>
          <div class="user-profile">
            <div class="avatar" :style="avatarStyle" @click="goProfile" title="个人设置">
              <span v-if="!authState.user.avatarUrl">{{ initial }}</span>
            </div>
            <button @click="handleLogout" class="google-btn text-btn logout-btn">退出</button>
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
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.logo {
  font-family: 'Google Sans', sans-serif;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  font-weight: 500;
  white-space: nowrap;
}

.logo-blue { color: #1a73e8; }
.logo-grey { color: #5f6368; }

.nav-links {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  margin-left: 32px;
  flex: 1;
}

.nav-item {
  color: #5f6368;
  font-size: 14px;
  font-weight: 500;
  padding: 10px 16px;
  border-radius: 4px;
  white-space: nowrap;
  cursor: pointer;
  transition: background-color 0.2s, color 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}
active-nav {
  background-color: #e8f0fe !important;
  color: #1a73e8 !important;
  font-weight: 500;
}

.
.nav-item:hover {
  background-color: #f1f3f4;
  color: #202124;
}

.nav-dropdown {
  position: relative;
}

.dropdown-trigger.active {
  background-color: #e8f0fe;
  color: #1a73e8;
}

.dropdown-arrow {
  transition: transform 0.2s;
}

.dropdown-trigger.active .dropdown-arrow {
  transform: rotate(180deg);
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  background: #fff;
  border: 1px solid #dadce0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  min-width: 160px;
  padding: 8px 0;
  z-index: 1001;
  animation: fadeIn 0.15s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.dropdown-item {
  padding: 10px 16px;
  font-size: 14px;
  color: #202124;
  cursor: pointer;
  transition: background-color 0.15s;
}

.dropdown-item:hover {
  background-color: #f1f3f4;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 16px;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 36px;
  height: 36px;
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
  transition: box-shadow 0.2s;
}

.avatar:hover {
  box-shadow: 0 0 0 4px rgba(26,115,232,0.2);
}

.google-btn {
  padding: 8px 20px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: background-color 0.2s;
}

.primary-btn {
  background: #1a73e8;
  color: #fff;
}

.primary-btn:hover {
  background: #1557b0;
}

.text-btn {
  background: transparent;
  color: #1a73e8;
}

.text-btn:hover {
  background: #e8f0fe;
}

.logout-btn {
  font-size: 14px;
  padding: 6px 12px;
}

.main-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

@media (max-width: 1024px) {
  .nav-links {
    gap: 0;
  }
  
  .nav-item {
    padding: 8px 12px;
    font-size: 13px;
  }
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
