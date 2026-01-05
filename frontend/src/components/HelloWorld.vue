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
        <span class="g-blue">U</span>
        <span class="g-red">n</span>
        <span class="g-yellow">i</span>
        <span class="g-blue">v</span>
        <span class="g-green">e</span>
        <span class="g-red">r</span>
        <span class="g-blue">s</span>
        <span class="g-yellow">a</span>
        <span class="g-green">l</span>
        <span class="g-grey"> Question Bank PaperSYS</span>
      </h1>
      <p v-if="authState.isAuthenticated" class="welcome-text">
        欢迎回来，{{ authState.user.nickname || authState.user.username }}
      </p>
      <p class="hero-subtitle">题目管理、智能组卷、在线考试一站式平台</p>
    </div>

    <div v-if="authState.isAuthenticated && !isTeacher && !isAdmin" class="student-dashboard">
      <div class="stats-card">
        <h3>我的进度</h3>
        <div class="stat-row">
          <div class="stat-item">
            <span class="stat-value">{{ stats.totalQuestionsAnswered }}</span>
            <span class="stat-label">答题数</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ (stats.accuracy * 100).toFixed(1) }}%</span>
            <span class="stat-label">正确率</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.currentStreak }}</span>
            <span class="stat-label">连续天数</span>
          </div>
        </div>
      </div>

      <div class="leaderboard-card">
        <div class="leaderboard-header">
          <h3>排行榜</h3>
          <router-link to="/leaderboard" class="view-all">查看全部</router-link>
        </div>
        <ul class="leaderboard-list">
          <li v-for="(user, index) in leaderboard" :key="user.id" class="leaderboard-item">
            <span class="rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
            <span class="name">{{ user.nickname || user.userId }}</span>
            <span class="score">{{ user.correctAnswers }} pts</span>
          </li>
        </ul>
      </div>
    </div>

    <div class="nav-grid">
      <template v-if="!authState.isAuthenticated">
        <router-link to="/login" class="nav-card google-card">
          <div class="icon-circle blue-bg">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path><polyline points="10 17 15 12 10 7"></polyline><line x1="15" y1="12" x2="3" y2="12"></line></svg>
          </div>
          <div class="nav-text">
            <h3>登录</h3>
            <p>登录您的账户</p>
          </div>
        </router-link>

        <router-link to="/register" class="nav-card google-card">
          <div class="icon-circle green-bg">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="8.5" cy="7" r="4"></circle><line x1="20" y1="8" x2="20" y2="14"></line><line x1="23" y1="11" x2="17" y2="11"></line></svg>
          </div>
          <div class="nav-text">
            <h3>注册</h3>
            <p>创建新账户</p>
          </div>
        </router-link>
      </template>

      <template v-if="isTeacher">
        <router-link to="/questions" class="nav-card google-card">
          <div class="icon-circle blue-bg">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>
          </div>
          <div class="nav-text">
            <h3>题库管理</h3>
            <p>浏览所有题目</p>
          </div>
        </router-link>

        <router-link to="/questions/add" class="nav-card google-card">
          <div class="icon-circle red-bg">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
          </div>
          <div class="nav-text">
            <h3>添加题目</h3>
            <p>创建新题目</p>
          </div>
        </router-link>

        <router-link to="/paper-generation" class="nav-card google-card">
          <div class="icon-circle yellow-bg">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
          </div>
          <div class="nav-text">
            <h3>智能组卷</h3>
            <p>创建试卷</p>
          </div>
        </router-link>

        <router-link to="/questions/review" class="nav-card google-card">
          <div class="icon-circle purple-bg">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
          </div>
          <div class="nav-text">
            <h3>题目审核</h3>
            <p>审核学生提交的题目</p>
          </div>
        </router-link>
      </template>

      <router-link v-if="authState.isAuthenticated && !isAdmin" to="/exam" class="nav-card google-card">
        <div class="icon-circle green-bg">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><circle cx="16" cy="16" r="4"></circle><path d="M16 14v2l1 1"></path></svg>
        </div>
        <div class="nav-text">
          <h3>在线考试</h3>
          <p>进入考试</p>
        </div>
      </router-link>

      <template v-if="isAdmin">
        <router-link to="/admin/users" class="nav-card google-card">
          <div class="icon-circle blue-bg">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
          </div>
          <div class="nav-text">
            <h3>用户管理</h3>
            <p>管理系统用户</p>
          </div>
        </router-link>

        <router-link to="/admin/system" class="nav-card google-card">
          <div class="icon-circle red-bg">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
          </div>
          <div class="nav-text">
            <h3>系统设置</h3>
            <p>配置系统参数</p>
          </div>
        </router-link>

        <router-link to="/admin/organizations" class="nav-card google-card">
          <div class="icon-circle purple-bg">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>
          </div>
          <div class="nav-text">
            <h3>组织管理</h3>
            <p>管理班级组织架构</p>
          </div>
        </router-link>
      </template>

      <template v-if="!isTeacher && !isAdmin && authState.isAuthenticated">
        <router-link to="/practice" class="nav-card google-card">
          <div class="icon-circle yellow-bg">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
          </div>
          <div class="nav-text">
            <h3>自主练习</h3>
            <p>历史试卷练习</p>
          </div>
        </router-link>

        <router-link to="/questions/add" class="nav-card google-card">
          <div class="icon-circle red-bg">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
          </div>
          <div class="nav-text">
            <h3>我要出题</h3>
            <p>提交题目供教师审核</p>
          </div>
        </router-link>

        <router-link to="/my-organizations" class="nav-card google-card">
          <div class="icon-circle blue-bg">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
          </div>
          <div class="nav-text">
            <h3>我的班级</h3>
            <p>加入班级组织</p>
          </div>
        </router-link>
      </template>

      <router-link v-if="isTeacher" to="/knowledge-point-manage" class="nav-card google-card">
        <div class="icon-circle purple-bg">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><circle cx="19" cy="5" r="2"></circle><circle cx="5" cy="5" r="2"></circle><circle cx="19" cy="19" r="2"></circle><circle cx="5" cy="19" r="2"></circle><line x1="12" y1="9" x2="12" y2="5"></line><line x1="6.5" y1="6.5" x2="9.5" y2="9.5"></line><line x1="17.5" y1="6.5" x2="14.5" y2="9.5"></line><line x1="6.5" y1="17.5" x2="9.5" y2="14.5"></line><line x1="17.5" y1="17.5" x2="14.5" y2="14.5"></line></svg>
        </div>
        <div class="nav-text">
          <h3>知识点管理</h3>
          <p>管理知识点体系</p>
        </div>
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
  text-align: center;
  font-family: 'Google Sans', Roboto, Arial, sans-serif;
}

.hero-section {
  margin-bottom: 60px;
}

.hero-title {
  font-size: 3.5em;
  font-weight: 500;
  margin-bottom: 16px;
  letter-spacing: -1px;
  white-space: nowrap;
}

.g-blue { color: #4285F4; }
.g-red { color: #EA4335; }
.g-yellow { color: #FBBC05; }
.g-green { color: #34A853; }
.g-grey { color: #5f6368; }

.hero-subtitle {
  font-size: 1.5em;
  color: #5f6368;
  font-weight: 300;
}

.welcome-text {
  font-size: 1.8em;
  color: #202124;
  font-weight: 400;
  margin-bottom: 8px;
}

.nav-grid {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  max-width: 800px;
  margin: 0 auto;
}

.nav-card {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 24px;
  text-decoration: none;
  color: inherit;
  background: #fff;
  border: 1px solid #dadce0;
  border-radius: 8px;
  width: 100%;
  box-sizing: border-box;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  text-align: left;
}

.nav-card:hover {
  box-shadow: 0 1px 3px 0 rgba(60,64,67,0.3), 0 4px 8px 3px rgba(60,64,67,0.15);
  border-color: transparent;
  transform: translateY(-1px);
  background-color: #f8f9fa;
}

.icon-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0;
  margin-right: 24px;
  flex-shrink: 0;
}

.icon-circle svg {
  width: 24px;
  height: 24px;
}

.blue-bg { background-color: #e8f0fe; color: #1967d2; }
.red-bg { background-color: #fce8e6; color: #c5221f; }
.yellow-bg { background-color: #fef7e0; color: #ea8600; }
.green-bg { background-color: #e6f4ea; color: #137333; }
.purple-bg { background-color: #f3e5ff; color: #6f2c91; }

.nav-card h3 {
  margin: 0 0 8px 0;
  color: #202124;
  font-size: 1.25em;
  font-weight: 500;
}

.nav-card p {
  margin: 0;
  color: #5f6368;
  font-size: 0.95em;
  line-height: 1.5;
}

.nav-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.student-dashboard {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
  margin-bottom: 2rem;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
}

.stats-card, .leaderboard-card {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24);
}

.stat-row {
  display: flex;
  justify-content: space-around;
  margin-top: 1rem;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: bold;
  color: #4285f4;
}

.stat-label {
  font-size: 0.9rem;
  color: #5f6368;
}

.leaderboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.leaderboard-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.leaderboard-item {
  display: flex;
  align-items: center;
  padding: 0.5rem 0;
  border-bottom: 1px solid #eee;
}

.leaderboard-item:last-child {
  border-bottom: none;
}

.rank {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #eee;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  margin-right: 1rem;
  font-weight: bold;
}

.rank-1 { background: #fbbc04; color: white; }
.rank-2 { background: #9aa0a6; color: white; }
.rank-3 { background: #b06000; color: white; } /* Bronze-ish */

.name {
  flex: 1;
  font-weight: 500;
}

.score {
  font-weight: bold;
  color: #34a853;
}

.view-all {
  font-size: 0.9rem;
  color: #1a73e8;
  text-decoration: none;
}

@media (max-width: 768px) {
  .student-dashboard {
    grid-template-columns: 1fr;
  }
}
</style>
