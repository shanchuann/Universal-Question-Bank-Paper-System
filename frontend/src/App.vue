<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { RouterView, useRouter, useRoute } from 'vue-router'
import { authState, type UserRole } from '@/states/authState'

const router = useRouter()
const route = useRoute()

onMounted(() => {
  if (authState.isAuthenticated) authState.fetchUser()
})

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
    // 学生专属菜单
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
  return { backgroundColor: 'var(--line-primary)' }
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

// 优化的下拉菜单交互
let closeTimer: ReturnType<typeof setTimeout> | null = null

const openDropdown = (label: string) => {
  if (closeTimer) {
    clearTimeout(closeTimer)
    closeTimer = null
  }
  activeDropdown.value = label
}

const toggleDropdown = (label: string) => {
  if (activeDropdown.value === label) {
    activeDropdown.value = null
  } else {
    openDropdown(label)
  }
}

const closeDropdown = () => {
  closeTimer = setTimeout(() => {
    activeDropdown.value = null
    closeTimer = null
  }, 200)
}

const navigateTo = (to: string) => {
  router.push(to)
  activeDropdown.value = null // 即时关闭无需延迟
  if (closeTimer) clearTimeout(closeTimer)
}

const isRouteActive = (path: string) => {
  if (!path) return false
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const isExactActive = (path: string) => {
  return route.path === path
}
</script>

<template>
  <div class="app-background">
    <div class="interactive-circle circle-1"></div>
    <div class="interactive-circle circle-2"></div>
    <div class="grid-decoration"></div>
  </div>

  <header class="line-header">
    <div class="header-content">
      <!-- Logo -->
      <div class="logo" @click="$router.push('/')">
        <span class="logo-primary">UQ</span><span class="logo-sub">Bank</span>
      </div>

      <!-- Navigation Links -->
      <nav class="nav-links" v-if="authState.isAuthenticated">
        <template v-for="group in navGroups" :key="group.label">
          <template v-if="!group.roles || (authState.user.role && group.roles.includes(authState.user.role as Exclude<UserRole, ''>))">
            <!-- 单个链接 -->
            <div v-if="group.to" class="nav-item" :class="{ 'active-nav': isRouteActive(group.to) }" @click="navigateTo(group.to)">
              {{ group.label }}
            </div>
            
            <!-- 下拉菜单 -->
            <div 
              v-else-if="group.children" 
              class="nav-dropdown" 
              @mouseleave="closeDropdown"
              @mouseenter="openDropdown(group.label)"
            >
              <div 
                class="nav-item dropdown-trigger" 
                :class="{ active: activeDropdown === group.label }"
                @click="toggleDropdown(group.label)"
                @mouseenter="openDropdown(group.label)"
              >
                {{ group.label }}
                <!-- SVG Chevron Down -->
                <svg class="dropdown-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </div>
              <div v-show="activeDropdown === group.label" class="dropdown-menu">
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
          <button class="line-btn text-btn" @click="$router.push('/login')">登录</button>
          <button class="line-btn primary-btn" @click="$router.push('/register')">注册</button>
        </template>
        <template v-else>
          <div class="user-profile">
            <div class="avatar" :style="avatarStyle" @click="goProfile" title="个人设置">
              <span v-if="!authState.user.avatarUrl">{{ initial }}</span>
            </div>
            <button @click="handleLogout" class="logout-btn" title="退出登录">
               <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
               <span>退出</span>
            </button>
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
/* App Background & Interactive Elements */
.app-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 0;
  background-color: var(--line-bg-soft);
  overflow: hidden;
  pointer-events: none;
}

.grid-decoration {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(rgba(15, 23, 42, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 23, 42, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(circle at center, black 40%, transparent 100%);
}

.interactive-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px); /* Increased blur for softer look */
  opacity: 0.45; /* Reduced opacity */
  mix-blend-mode: multiply;
  will-change: transform;
}

.circle-1 {
  width: 700px;
  height: 700px;
  background: radial-gradient(circle at 30% 30%, rgba(14, 165, 233, 0.4), rgba(14, 165, 233, 0)); /* Much lighter */
  top: -100px;
  left: -100px;
  animation: float-1 30s infinite ease-in-out;
}

.circle-2 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle at 70% 70%, rgba(139, 92, 246, 0.4), rgba(139, 92, 246, 0)); /* Much lighter */
  bottom: -100px;
  right: -100px;
  animation: float-2 35s infinite ease-in-out reverse;
}

@keyframes float-1 {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  25% { transform: translate(40vw, 10vh) rotate(10deg); }
  50% { transform: translate(20vw, 40vh) rotate(-5deg); }
  75% { transform: translate(-10vw, 20vh) rotate(5deg); }
}

@keyframes float-2 {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  25% { transform: translate(-30vw, -20vh) rotate(-10deg); }
  50% { transform: translate(-10vw, -40vh) rotate(5deg); }
  75% { transform: translate(20vw, -10vh) rotate(10deg); }
}

.line-header {
  background-color: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(12px) saturate(180%);
  -webkit-backdrop-filter: blur(12px) saturate(180%);
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 1000;
  transition: all 0.3s ease;
}

.line-header:hover {
  background-color: rgba(255, 255, 255, 0.85);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
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
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: -0.05em;
  gap: 2px;
}

.logo-primary { color: var(--line-primary); }
.logo-sub { color: var(--line-text-secondary); font-weight: 400; }

.nav-links {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-left: 32px;
  flex: 1;
}

.nav-item {
  color: var(--line-text-secondary);
  font-size: 14px;
  font-weight: 500;
  padding: 8px 12px;
  border-radius: var(--radius);
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid transparent;
}

.nav-item:hover {
  background-color: rgba(15, 23, 42, 0.04);
  color: var(--line-text);
  transform: translateY(-1px);
}

.nav-item.active-nav {
  background-color: rgba(15, 23, 42, 0.06);
  color: var(--line-primary);
  font-weight: 600;
}

.nav-dropdown {
  position: relative;
}

.dropdown-arrow {
  transition: transform 0.2s;
  opacity: 0.5;
}

.nav-item:hover .dropdown-arrow {
  opacity: 1;
}

.dropdown-trigger.active .dropdown-arrow {
  transform: rotate(180deg);
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 4px);
  right: 0;
  background: var(--line-bg);
  border: 1px solid var(--line-border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-hover);
  min-width: 160px;
  padding: 4px;
  z-index: 1001;
  animation: fadeIn 0.15s ease;
}

/* 增加隐形区域填补缝隙，防止鼠标移动时菜单消失 */
.dropdown-menu::before {
  content: '';
  position: absolute;
  top: -10px;
  left: 0;
  width: 100%;
  height: 10px;
  background: transparent;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.dropdown-item {
  padding: 8px 12px;
  font-size: 14px;
  color: var(--line-text);
  cursor: pointer;
  border-radius: var(--radius);
  transition: background-color 0.15s;
}

.dropdown-item:hover {
  background-color: var(--line-bg-soft);
}

.dropdown-item.active-nav {
  background-color: var(--line-bg-hover);
  color: var(--line-primary);
  font-weight: 600;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-left: 24px;
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
  border: 2px solid white;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  box-shadow: 0 0 0 1px rgba(0,0,0,0.05), 0 2px 5px rgba(0,0,0,0.1);
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.avatar:hover {
  transform: scale(1.1);
  box-shadow: 0 0 0 2px var(--line-primary-20), 0 4px 12px rgba(0,0,0,0.15);
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  height: 34px;
  border-radius: 99px;
  font-size: 13px;
  font-weight: 500;
  color: var(--line-text-secondary);
  background: transparent;
  border: 1px solid var(--line-border);
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: #FFF1F2; /* Rose 50 */
  color: #E11D48; /* Rose 600 */
  border-color: #FECDD3; /* Rose 200 */
  box-shadow: 0 2px 4px rgba(225, 29, 72, 0.1);
}

.logout-btn svg {
  transition: transform 0.2s;
}

.logout-btn:hover svg {
  transform: translateX(2px);
}

.main-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px 24px;
  position: relative;
  z-index: 1;
}

@media (max-width: 1024px) {
  .nav-links { gap: 4px; }
  .header-content { padding: 0 16px; }
}

@media (max-width: 768px) {
  .nav-links { display: none; }
}
</style>
