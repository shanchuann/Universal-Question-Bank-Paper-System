<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { authState } from '@/states/authState'
import axios from 'axios'

defineProps<{ msg: string }>()

const isTeacher = computed(() => {
  return authState.user.role === 'TEACHER'
})

const isAdmin = computed(() => {
  return authState.user.role === 'ADMIN'
})

const stats = ref({
  totalQuestionsAnswered: 0,
  correctAnswers: 0,
  currentStreak: 0,
  accuracy: 0
})

const leaderboard = ref<any[]>([])

const fetchStats = async () => {
  if (authState.isAuthenticated && !isTeacher.value && !isAdmin.value && authState.user.id) {
    try {
      const res = await axios.get(`/api/stats/me?userId=${authState.user.id}`)
      stats.value = res.data
    } catch (e) {
      console.error(e)
    }
  }
}

const fetchLeaderboard = async () => {
  if (authState.isAuthenticated && !isTeacher.value && !isAdmin.value) {
    try {
      const res = await axios.get('/api/stats/leaderboard?limit=5')
      leaderboard.value = res.data
    } catch (e) {
      console.error(e)
    }
  }
}

onMounted(() => {
  if (authState.user.id) {
    fetchStats()
  }
  fetchLeaderboard()
})

watch(() => authState.user.id, (newId) => {
  if (newId) {
    fetchStats()
  }
})
</script>

<template>
  <div class="home-container">
    <div class="hero-section">
      <h1 class="hero-title">
        <span class="brand-text">Universal</span>
        <span class="brand-sub">Question Bank PaperSYS</span>
      </h1>
      <p v-if="authState.isAuthenticated" class="welcome-text">
        欢迎回来，{{ authState.user.nickname || authState.user.username }}
      </p>
      <p class="hero-subtitle">题目管理、智能组卷、在线考试一站式平台</p>
    </div>

    <!-- 学生仪表盘 -->
    <div v-if="authState.isAuthenticated && !isTeacher && !isAdmin" class="dashboard-grid">
      <div class="line-card stats-card">
        <div class="card-header-clean">
          <h3>我的进度</h3>
        </div>
        <div class="stat-row">
          <div class="stat-item">
            <span class="stat-value">{{ stats.totalQuestionsAnswered }}</span>
            <span class="stat-label">总答题</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ (stats.accuracy * 100).toFixed(1) }}%</span>
            <span class="stat-label">正确率</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.currentStreak }}</span>
            <span class="stat-label">连续打卡</span>
          </div>
        </div>
      </div>

      <div class="line-card leaderboard-card">
        <div class="card-header-clean">
          <h3>排行榜</h3>
          <router-link to="/leaderboard" class="link-btn">全部</router-link>
        </div>
        <ul class="leaderboard-list">
          <li v-for="(user, index) in leaderboard" :key="user.id" class="leaderboard-item">
            <div class="rank-badge" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
            <span class="name">{{ user.nickname || user.userId }}</span>
            <span class="score">{{ user.correctAnswers }} pts</span>
          </li>
        </ul>
      </div>
    </div>

    <!-- 功能导航网格 -->
    <div class="nav-grid">
      <template v-if="!authState.isAuthenticated">
        <router-link to="/login" class="nav-card">
          <div class="icon-box">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path><polyline points="10 17 15 12 10 7"></polyline><line x1="15" y1="12" x2="3" y2="12"></line></svg>
          </div>
          <div class="nav-content">
            <h3>登录</h3>
            <p>登录您的账户</p>
          </div>
          <div class="arrow-icon">→</div>
        </router-link>

        <router-link to="/register" class="nav-card">
          <div class="icon-box">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="8.5" cy="7" r="4"></circle><line x1="20" y1="8" x2="20" y2="14"></line><line x1="23" y1="11" x2="17" y2="11"></line></svg>
          </div>
          <div class="nav-content">
            <h3>注册</h3>
            <p>创建新账户</p>
          </div>
          <div class="arrow-icon">→</div>
        </router-link>
      </template>

      <template v-if="isTeacher">
        <router-link to="/questions" class="nav-card">
          <div class="icon-box">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>
          </div>
          <div class="nav-content">
            <h3>题库管理</h3>
            <p>浏览所有题目</p>
          </div>
        </router-link>

        <router-link to="/questions/add" class="nav-card">
          <div class="icon-box">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
          </div>
          <div class="nav-content">
            <h3>添加题目</h3>
            <p>创建新题目</p>
          </div>
        </router-link>

        <router-link to="/paper-generation" class="nav-card">
          <div class="icon-box">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
          </div>
          <div class="nav-content">
            <h3>智能组卷</h3>
            <p>创建试卷</p>
          </div>
        </router-link>

        <router-link to="/questions/review" class="nav-card">
          <div class="icon-box">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
          </div>
          <div class="nav-content">
            <h3>题目审核</h3>
            <p>审核学生提交的题目</p>
          </div>
        </router-link>
      </template>

      <router-link v-if="authState.isAuthenticated && !isAdmin" to="/exam" class="nav-card">
        <div class="icon-box">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><circle cx="16" cy="16" r="4"></circle><path d="M16 14v2l1 1"></path></svg>
        </div>
        <div class="nav-content">
          <h3>在线考试</h3>
          <p>进入考试</p>
        </div>
      </router-link>

      <template v-if="isAdmin">
        <router-link to="/admin/users" class="nav-card">
          <div class="icon-box">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
          </div>
          <div class="nav-content">
            <h3>用户管理</h3>
            <p>管理系统用户</p>
          </div>
        </router-link>

        <router-link to="/admin/system" class="nav-card">
          <div class="icon-box">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
          </div>
          <div class="nav-content">
            <h3>系统设置</h3>
            <p>配置系统参数</p>
          </div>
        </router-link>

        <router-link to="/admin/organizations" class="nav-card">
          <div class="icon-box">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>
          </div>
          <div class="nav-content">
            <h3>组织管理</h3>
            <p>管理班级组织架构</p>
          </div>
        </router-link>
      </template>

      <template v-if="!isTeacher && !isAdmin && authState.isAuthenticated">
        <router-link to="/practice" class="nav-card">
          <div class="icon-box">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
          </div>
          <div class="nav-content">
            <h3>自主练习</h3>
            <p>历史试卷练习</p>
          </div>
        </router-link>

        <router-link to="/questions/add" class="nav-card">
          <div class="icon-box">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
          </div>
          <div class="nav-content">
            <h3>我要出题</h3>
            <p>提交题目供教师审核</p>
          </div>
        </router-link>

        <router-link to="/my-organizations" class="nav-card">
          <div class="icon-box">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
          </div>
          <div class="nav-content">
            <h3>我的班级</h3>
            <p>加入班级组织</p>
          </div>
        </router-link>
      </template>

      <router-link v-if="isTeacher" to="/knowledge-point-manage" class="nav-card">
        <div class="icon-box">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><circle cx="19" cy="5" r="2"></circle><circle cx="5" cy="5" r="2"></circle><circle cx="19" cy="19" r="2"></circle><circle cx="5" cy="19" r="2"></circle><line x1="12" y1="9" x2="12" y2="5"></line><line x1="6.5" y1="6.5" x2="9.5" y2="9.5"></line><line x1="17.5" y1="6.5" x2="14.5" y2="9.5"></line><line x1="6.5" y1="17.5" x2="9.5" y2="14.5"></line><line x1="17.5" y1="17.5" x2="14.5" y2="14.5"></line></svg>
        </div>
        <div class="nav-content">
          <h3>知识点管理</h3>
          <p>管理知识点体系</p>
        </div>
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.home-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 60px 24px;
  animation: fadeIn 0.8s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.hero-section {
  text-align: center;
  margin-bottom: 64px;
}

.hero-title {
  margin-bottom: 16px;
  letter-spacing: -0.03em;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.brand-text {
  font-size: 2.5rem;
  font-weight: 800;
  background: linear-gradient(135deg, var(--line-primary) 0%, #475569 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  line-height: 1.2;
}

.brand-sub {
  font-size: 1rem;
  font-weight: 500;
  color: var(--line-text-secondary);
  letter-spacing: 0.2em;
  text-transform: uppercase;
  opacity: 0.8;
}

.welcome-text {
  font-size: 1.1em;
  color: var(--line-text-primary);
  font-weight: 500;
  margin: 0 0 8px 0;
}

.hero-subtitle {
  font-size: 1.1em;
  color: var(--line-text-secondary);
  font-weight: 400;
  max-width: 600px;
  margin: 0 auto;
}

/* Dashboard Grid for Student Stats/Leaderboard */
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
  margin-bottom: 48px;
}

.line-card {
  background: var(--line-card-bg);
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-lg);
  padding: 24px;
  box-shadow: var(--line-shadow-sm);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.line-card:hover {
  box-shadow: var(--line-shadow-md);
  transform: translateY(-2px);
  border-color: var(--line-primary-20);
}

.card-header-clean {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--line-border);
  padding-bottom: 16px;
}

.card-header-clean h3 {
  font-size: 1.1rem;
  font-weight: 600;
  margin: 0;
  color: var(--line-text-primary);
}

.link-btn {
  font-size: 0.9rem;
  color: var(--line-text-secondary);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}

.link-btn:hover {
  color: var(--line-primary);
}

.stat-row {
  display: flex;
  justify-content: space-around;
  text-align: center;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--line-primary);
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 0.85rem;
  color: var(--line-text-secondary);
  font-weight: 500;
}

/* Leaderboard Specifics */
.leaderboard-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.leaderboard-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--line-bg-soft);
}

.leaderboard-item:last-child {
  border-bottom: none;
}

.rank-badge {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: var(--line-bg-soft);
  color: var(--line-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 600;
  margin-right: 12px;
}

.rank-1 { background: #FCD34D; color: #92400E; } /* Amber 300 */
.rank-2 { background: #E5E7EB; color: #374151; } /* Gray 200 */
.rank-3 { background: #FDBA74; color: #9A3412; } /* Orange 300 */

.name {
  flex: 1;
  font-size: 0.95rem;
  font-weight: 500;
  color: var(--line-text-primary);
}

.score {
  font-weight: 600;
  color: var(--line-text-primary);
  font-family: monospace;
}

/* Nav Grid */
.nav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.nav-card {
  display: flex;
  align-items: center;
  padding: 24px;
  background: var(--line-card-bg);
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-lg);
  text-decoration: none;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.nav-card:hover {
  border-color: var(--line-primary);
  transform: translateY(-2px);
  box-shadow: var(--line-shadow-md);
}

.icon-box {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--line-bg-soft);
  color: var(--line-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  transition: all 0.2s;
}

.nav-card:hover .icon-box {
  background: var(--line-primary);
  color: white;
}

.nav-content {
  flex: 1;
}

.nav-content h3 {
  margin: 0 0 6px 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--line-text-primary);
}

.nav-content p {
  margin: 0;
  font-size: 0.9rem;
  color: var(--line-text-secondary);
}

.arrow-icon {
  opacity: 0;
  transform: translateX(-10px);
  transition: all 0.2s;
  color: var(--line-primary);
  font-weight: bold;
}

.nav-card:hover .arrow-icon {
  opacity: 1;
  transform: translateX(0);
}

@media (max-width: 640px) {
  .brand-text { font-size: 2rem; }
  .nav-grid { grid-template-columns: 1fr; }
  .dashboard-grid { grid-template-columns: 1fr; }
}
</style>
