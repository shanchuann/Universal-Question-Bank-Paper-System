<script setup lang="ts">
import { ref, onMounted, computed, watch, onUnmounted } from 'vue'
import { RouterView, useRouter, useRoute } from 'vue-router'
import { authState, type UserRole } from '@/states/authState'
import { ChevronDown, LogOut, Menu, X } from 'lucide-vue-next'
import { useNotifications } from '@/composables/useNotifications'
import GlobalToast from '@/components/GlobalToast.vue'
import AnnouncementModal from '@/components/AnnouncementModal.vue'
import Breadcrumb from '@/components/Breadcrumb.vue'
import AiFloatingAssistant from '@/components/AiFloatingAssistant.vue'
import axios from 'axios'

const router = useRouter()
const route = useRoute()

// 站点名称
const siteName = ref('UQBank')
const siteLogoUrl = ref('')
const copyrightText = ref('© UQBank')
const aiEnabled = ref(false)
const aiAssistantEnabled = ref(false)

// 公告弹窗引用
const announcementModalRef = ref<InstanceType<typeof AnnouncementModal> | null>(null)
let pollingTimer: ReturnType<typeof setInterval> | null = null
let announcementCheckTick = 0
const { unreadCount, refreshUnreadCount } = useNotifications()

const fetchSiteSettings = async () => {
  try {
    const response = await axios.get('/api/system/public-settings')
    if (response.data.siteName) {
      siteName.value = response.data.siteName
      document.title = response.data.siteName
    }
    siteLogoUrl.value = response.data.siteLogoUrl || ''
    copyrightText.value = response.data.copyrightText || '© UQBank'
    aiEnabled.value = Boolean(response.data.aiEnabled)
    aiAssistantEnabled.value = Boolean(response.data.aiAssistantEnabled)
  } catch (error) {
    // 如果获取失败，使用默认值
  }
}

// 检查并显示公告
const checkAnnouncements = () => {
  if (announcementModalRef.value) {
    announcementModalRef.value.checkAnnouncements()
  }
}

const startNotificationPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
  }
  announcementCheckTick = 0
  pollingTimer = setInterval(() => {
    refreshUnreadCount()
    announcementCheckTick += 1
    if (announcementCheckTick >= 4) {
      checkAnnouncements()
      announcementCheckTick = 0
    }
  }, 30000)
}

const stopNotificationPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

onUnmounted(() => {
  stopNotificationPolling()
})

onMounted(() => {
  fetchSiteSettings()
  if (authState.isAuthenticated) {
    authState.fetchUser()
    // 登录后检查公告
    checkAnnouncements()
    refreshUnreadCount()
    startNotificationPolling()
  }
})

// 当窗口尺寸变化时，关闭移动导航（避免移动菜单在放大窗口后残留）
onMounted(() => {
  const handleResize = () => {
    if (typeof window === 'undefined') return
    if (window.innerWidth > 1200 && mobileNavOpen.value) {
      mobileNavOpen.value = false
      mobileActiveGroup.value = null
    }
  }
  window.addEventListener('resize', handleResize)
  // 初始化检查一次
  handleResize()
  onUnmounted(() => window.removeEventListener('resize', handleResize))
})

// 监听登录状态变化，登录后显示公告
watch(
  () => authState.isAuthenticated,
  (isAuth) => {
    if (isAuth) {
      // 延迟一点点确保路由跳转完成
      setTimeout(() => {
        checkAnnouncements()
        refreshUnreadCount()
        startNotificationPolling()
      }, 500)
    } else {
      unreadCount.value = 0
      stopNotificationPolling()
    }
  }
)

type NavChild = { to: string; label: string }
type NavGroup = {
  label: string
  roles?: Array<Exclude<UserRole, ''>>
  children?: NavChild[]
  to?: string
}

const activeDropdown = ref<string | null>(null)
const mobileNavOpen = ref(false)
const mobileActiveGroup = ref<string | null>(null)

const mobileToggleGroup = (label: string) => {
  if (mobileActiveGroup.value === label) {
    mobileActiveGroup.value = null
  } else {
    mobileActiveGroup.value = label
  }
}

// determine whether the nav group should be visible for current user
const groupVisible = (group: NavGroup) => {
  if (!group.roles) return true
  const role = authState.user.role
  if (!role) return false
  return group.roles.includes(role as Exclude<UserRole, ''>)
}

const navGroups = computed<NavGroup[]>(() => {
  const role = authState.user.role
  const isStudent = role !== 'TEACHER' && role !== 'ADMIN'
  const isTeacher = role === 'TEACHER'
  const isAdmin = role === 'ADMIN'

  const groups: NavGroup[] = [
    { to: '/', label: '首页' },
    ...(!isAdmin ? [{ to: '/messages', label: '消息中心' }] : []),
    // 教师专属业务菜单
    {
      label: '题库管理',
      roles: ['TEACHER'],
      children: [
        { to: '/questions', label: '题目列表' },
        { to: '/questions/add', label: '添加题目' },
        { to: '/import', label: '导入题目' },
        { to: '/questions/review', label: '题目审核' },
        { to: '/knowledge-point-manage', label: '知识点管理' }
      ]
    },
    {
      label: '试卷管理',
      roles: ['TEACHER'],
      children: [
        { to: '/papers', label: '试卷列表' },
        { to: '/papers/manual', label: '手动组卷' },
        { to: '/paper-generation', label: '智能组卷' }
      ]
    },
    {
      label: '考试管理',
      to: '/exams/manage',
      roles: ['TEACHER']
    },
    {
      label: '阅卷评分',
      to: '/grading',
      roles: ['TEACHER']
    },
    // 学生专属菜单
    ...(isStudent
      ? [
          { to: '/exams/list', label: '我的考试' },
          { to: '/my-scores', label: '我的成绩' },
          { to: '/practice', label: '练习模式' },
          { to: '/leaderboard', label: '排行榜' },
          { to: '/questions/add', label: '我要出题' },
          { to: '/my-organizations', label: '我的班级' }
        ]
      : []),
    // 教师的班级管理
    ...(isTeacher
      ? [
          {
            label: '班级管理',
            children: [
              { to: '/admin/organizations', label: '我的班级' },
              { to: '/leaderboard', label: '排行榜' }
            ]
          }
        ]
      : []),
    // 管理员的系统管理 - 直接展示在导航栏
    ...(isAdmin
      ? [
          { to: '/admin/users', label: '用户管理' },
          { to: '/admin/organizations', label: '组织架构' },
          { to: '/admin/statistics', label: '数据统计' },
          { to: '/admin/system', label: '系统设置' },
          { to: '/admin/logs', label: '操作日志' },
          { to: '/admin/monitor', label: '系统监控' },
          { to: '/admin/announcements', label: '公告管理' }
        ]
      : [])
  ]

  return groups
})

const avatarUrl = computed(() => authState.user.avatarUrl || '')
const avatarError = ref(false)

watch(avatarUrl, () => {
  avatarError.value = false
})

const initial = computed(() => {
  const name = authState.user.nickname || authState.user.username || ''
  return name.length > 0 ? name.charAt(0).toUpperCase() : 'U'
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
  mobileNavOpen.value = false
  mobileActiveGroup.value = null
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

const breadcrumbHiddenRoutes = ['/login', '/register', '/forgot-password']
const authEntryRoutes = ['/login', '/register', '/forgot-password']
const showBreadcrumb = computed(() => {
  if (!authState.isAuthenticated) return false
  if (route.path === '/') return false
  return !breadcrumbHiddenRoutes.includes(route.path)
})

const showGuestActions = computed(() => {
  if (authState.isAuthenticated) return false
  return !authEntryRoutes.includes(route.path)
})

const showUserProfileActions = computed(() => {
  if (!authState.isAuthenticated) return false
  return !authEntryRoutes.includes(route.path)
})

const showHeaderActions = computed(() => showGuestActions.value || showUserProfileActions.value)

const floatingAiMode = computed<'teacher' | 'student'>(() => {
  return authState.user.role === 'TEACHER' ? 'teacher' : 'student'
})

const showFloatingAi = computed(() => {
  if (!authState.isAuthenticated) return false
  if (!authState.user.role) return false
  if (authState.user.role === 'ADMIN') return false
  return aiEnabled.value && aiAssistantEnabled.value
})

// 当路由切换到登录/注册/找回密码页面时，确保移动导航关闭，避免侧栏残留
watch(
  () => route.path,
  (p) => {
    if (authEntryRoutes.includes(p)) {
      mobileNavOpen.value = false
      mobileActiveGroup.value = null
      activeDropdown.value = null
    }
  }
)
</script>

<template>
  <div class="app-background">
    <div class="interactive-circle circle-1"></div>
    <div class="interactive-circle circle-2"></div>
  </div>

  <header class="line-header">
    <div class="header-content">
      <!-- Logo -->
      <div class="logo" @click="$router.push('/')">
        <img v-if="siteLogoUrl" :src="siteLogoUrl" alt="site-logo" class="logo-image" />
        <span class="logo-text">{{ siteName }}</span>
      </div>

      <!-- Navigation Links -->
      <nav class="nav-links" v-if="authState.isAuthenticated && !authEntryRoutes.includes(route.path)">
        <template v-for="group in navGroups" :key="group.label">
          <template v-if="groupVisible(group)">
            <!-- 单个链接 -->
            <div
              v-if="group.to"
              class="nav-item"
              :class="{ 'active-nav': isRouteActive(group.to) }"
              @click="navigateTo(group.to)"
            >
              {{ group.label }}
              <span
                v-if="group.to === '/messages' && unreadCount > 0"
                class="nav-dot"
                aria-label="有未读消息"
              ></span>
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
                <ChevronDown class="dropdown-arrow" :size="16" />
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

      <!-- Mobile menu button (visible on tablet/phone via CSS) -->
      <button
        v-if="!authEntryRoutes.includes(route.path) && (route.name !== 'home' || authState.isAuthenticated)"
        class="mobile-menu-button"
        @click="mobileNavOpen = !mobileNavOpen"
        aria-label="打开菜单"
        type="button"
      >
        <Menu v-if="!mobileNavOpen" :size="18" />
        <X v-else :size="18" />
      </button>

      <!-- User Actions -->
      <div v-if="showHeaderActions" class="user-actions">
        <template v-if="showGuestActions">
          <button class="line-btn text-btn" @click="$router.push('/login')">登录</button>
          <button class="line-btn primary-btn" @click="$router.push('/register')">注册</button>
        </template>
        <template v-else-if="showUserProfileActions">
          <div class="user-profile">
            <div
              class="avatar"
              :style="{
                backgroundColor: avatarUrl && !avatarError ? 'transparent' : 'var(--line-primary)'
              }"
              @click="goProfile"
              title="个人设置"
            >
              <img
                v-if="avatarUrl && !avatarError"
                :src="avatarUrl"
                alt="avatar"
                @error="avatarError = true"
              />
              <span v-else>{{ initial }}</span>
            </div>
            <button @click="handleLogout" class="logout-btn" title="退出登录">
              <LogOut :size="16" />
              <span>退出</span>
            </button>
          </div>
        </template>
      </div>
    </div>
  </header>

  <!-- Mobile navigation dropdown (for tablet/phone) -->
  <div class="mobile-nav-dropdown" v-if="mobileNavOpen && !authEntryRoutes.includes(route.path) && (route.name !== 'home' || authState.isAuthenticated)">
    <div class="mobile-nav-inner">
      <template v-for="group in navGroups" :key="group.label">
        <template v-if="groupVisible(group)">
          <div v-if="group.to" class="mobile-nav-link" @click="navigateTo(group.to)">
            {{ group.label }}
          </div>

          <div v-else class="mobile-nav-accordion">
            <div class="mobile-nav-link" @click="mobileToggleGroup(group.label)">
              <span>{{ group.label }}</span>
              <ChevronDown :size="14" />
            </div>
            <div v-show="mobileActiveGroup === group.label" class="mobile-children">
              <div
                v-for="child in group.children"
                :key="child.to"
                class="mobile-child"
                @click="navigateTo(child.to)"
              >
                {{ child.label }}
              </div>
            </div>
          </div>
        </template>
      </template>
    </div>
  </div>

  <!-- 面包屑导航 -->
  <Breadcrumb v-if="showBreadcrumb" />

  <main class="main-container" :class="{ 'with-breadcrumb': showBreadcrumb }">
    <RouterView />
  </main>

  <footer class="app-footer">
    <div class="footer-inner">
      <div class="footer-left">{{ copyrightText }}</div>
      <div class="footer-record">
        <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">备案号：陕ICP备2026009958号-1</a>
        <img class="icp-icon" src="https://files.seeusercontent.com/2026/04/28/gEu1/a20583c.png" alt="警徽" />
        <a href="https://beian.mps.gov.cn/#/query/webSearch?code=61040202000893" rel="noreferrer" target="_blank">陕公网安备61040202000893号</a>
      </div>
    </div>
  </footer>

  <!-- 全局 Toast 通知 -->
  <GlobalToast />

  <!-- 公告弹窗 -->
  <AnnouncementModal ref="announcementModalRef" />

  <AiFloatingAssistant v-if="showFloatingAi" :mode="floatingAiMode" />
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
  background: radial-gradient(
    circle at 30% 30%,
    rgba(14, 165, 233, 0.4),
    rgba(14, 165, 233, 0)
  ); /* Much lighter */
  top: -100px;
  left: -100px;
  animation: float-1 30s infinite ease-in-out;
}

.circle-2 {
  width: 600px;
  height: 600px;
  background: radial-gradient(
    circle at 70% 70%,
    rgba(139, 92, 246, 0.4),
    rgba(139, 92, 246, 0)
  ); /* Much lighter */
  bottom: -100px;
  right: -100px;
  animation: float-2 35s infinite ease-in-out reverse;
}

@keyframes float-1 {
  0%,
  100% {
    transform: translate(0, 0) rotate(0deg);
  }
  25% {
    transform: translate(40vw, 10vh) rotate(10deg);
  }
  50% {
    transform: translate(20vw, 40vh) rotate(-5deg);
  }
  75% {
    transform: translate(-10vw, 20vh) rotate(5deg);
  }
}

@keyframes float-2 {
  0%,
  100% {
    transform: translate(0, 0) rotate(0deg);
  }
  25% {
    transform: translate(-30vw, -20vh) rotate(-10deg);
  }
  50% {
    transform: translate(-10vw, -40vh) rotate(5deg);
  }
  75% {
    transform: translate(20vw, -10vh) rotate(10deg);
  }
}

.line-header {
  background-color: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(12px) saturate(180%);
  -webkit-backdrop-filter: blur(12px) saturate(180%);
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  height: 64px;
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  z-index: 1000;
  transition: all 0.3s ease;
}

.line-header:hover {
  background-color: rgba(255, 255, 255, 0.85);
  box-shadow:
    0 4px 6px -1px rgba(0, 0, 0, 0.05),
    0 2px 4px -1px rgba(0, 0, 0, 0.03);
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

.logo-image {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid var(--line-border);
  background: #fff;
}

.logo-text {
  color: var(--line-primary);
  background: linear-gradient(135deg, var(--line-primary) 0%, #4a90d9 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

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
  position: relative;
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

.nav-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  position: absolute;
  top: 6px;
  right: 6px;
  border-radius: 999px;
  background: #ef4444;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.95);
}

/* Mobile / Tablet navigation styles */
.mobile-menu-button {
  display: none;
  border: none;
  background: transparent;
  align-items: center;
  justify-content: center;
  padding: 8px;
  margin-left: 12px;
  cursor: pointer;
  border-radius: 8px;
}

.mobile-nav-dropdown {
  display: none;
}

@media (max-width: 1200px) {
  .nav-links {
    display: none;
  }
  .mobile-menu-button {
    display: inline-flex;
    color: var(--line-text-secondary);
    margin-left: auto; /* push button to the right */
  }
  .mobile-nav-dropdown {
    display: block;
    /* 让下拉面板跟随文档流并根据内容高度扩展 */
    position: absolute;
    top: 64px;
    right: 0;
    width: min(300px, calc(100% - 24px));
    max-width: 340px;
    background: var(--line-bg);
    border-left: 1px solid var(--line-border);
    box-shadow: -4px 0 16px rgba(15, 23, 42, 0.06);
    z-index: 1100;
    /* 不强制最大高度，允许面板高度随内容自适应 */
    overflow-y: visible;
    overflow-x: hidden;
    border-top-left-radius: 8px;
    border-bottom-left-radius: 8px;
    margin-right: 8px;
  }
  .mobile-nav-inner {
    padding: 12px 12px 20px 12px;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .mobile-nav-link {
    padding: 10px 14px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 8px;
    cursor: pointer;
    color: var(--line-text);
    background: transparent;
    white-space: normal;
    word-break: break-word;
  }
  .mobile-nav-link:hover {
    background: var(--line-bg-soft);
  }
  .mobile-child {
    padding: 8px 18px;
    border-radius: 6px;
    color: var(--line-text-secondary);
    cursor: pointer;
    white-space: normal;
    word-break: break-word;
  }
  .mobile-child:hover {
    color: var(--line-text);
    background: color-mix(in srgb, var(--line-primary) 6%, transparent);
  }
  .mobile-children {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding-left: 6px;
  }
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
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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

/* Ensure user actions keep horizontal layout and only shrink content on small screens */
.user-actions,
.user-profile {
  flex-wrap: nowrap;
}
.user-actions > *,
.user-profile > * {
  flex: 0 0 auto; /* prevent children from shrinking to a new line */
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
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.05),
    0 2px 5px rgba(0, 0, 0, 0.1);
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  display: block;
}

.avatar:hover {
  transform: scale(1.1);
  box-shadow:
    0 0 0 2px var(--line-primary-20),
    0 4px 12px rgba(0, 0, 0, 0.15);
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
  background: #fff1f2; /* Rose 50 */
  color: #e11d48; /* Rose 600 */
  border-color: #fecdd3; /* Rose 200 */
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
  padding: 96px 24px 120px; /* bottom padding increased to avoid fixed footer overlap */
  position: relative;
}

.main-container.with-breadcrumb {
  padding-top: 136px;
}

.app-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1200;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.36);
  -webkit-backdrop-filter: blur(10px) saturate(140%);
  backdrop-filter: blur(10px) saturate(140%);
  border-top: 1px solid rgba(255,255,255,0.12);
  text-align: center;
  color: var(--line-text);
  font-size: 12px;
  padding: 8px 16px;
}
.app-footer .footer-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: nowrap;
}
.app-footer .footer-record {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.app-footer a {
  color: var(--line-text-secondary);
  text-decoration: none;
  font-size: 12px;
}
.app-footer .icp-icon {
  width: 16px;
  height: 16px;
}

/* 页脚在小屏幕时字体减小且记录项保持横排 */
@media (max-width: 480px) {
  .app-footer {
    font-size: 12px;
    padding: 10px 12px;
  }
  .app-footer .footer-record {
    gap: 8px;
    white-space: nowrap;
  }
  .app-footer .footer-record a {
    font-size: 12px;
  }
}

@media (max-width: 1024px) {
  .nav-links {
    gap: 4px;
  }
  .header-content {
    padding: 0 16px;
  }
}

@media (max-width: 768px) {
  .nav-links {
    display: none;
  }
}

/* 小屏幕：保持登录/注册按钮尺寸不变，只缩小文字并保持横向 */
@media (max-width: 480px) {
  .user-actions {
    flex-wrap: nowrap;
  }
  .user-actions .line-btn {
    padding: 10px 20px; /* 保持按钮大小感受 */
    font-size: 12px; /* 缩小文字 */
    white-space: nowrap; /* 保持横排 */
  }
  .user-actions .line-btn span {
    display: inline-block;
  }

  /* 缩小头像与退出按钮尺寸，但保持横向排列 */
  .avatar {
    width: 28px;
    height: 28px;
    font-size: 12px;
  }
  .logout-btn {
    padding: 6px 10px;
    height: 30px;
    font-size: 12px;
  }
  .user-actions {
    gap: 8px;
  }
}
</style>
