<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { authState } from '@/states/authState'
import axios from 'axios'
import { 
  LogIn, UserPlus, Book, Plus, FileText, CheckCircle, 
  FileClock, Users, Settings, Home, Network, ClipboardList,
  GraduationCap, ArrowRight, ScrollText, BarChart3, Activity, Megaphone, Shield
} from 'lucide-vue-next'

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
const myOrgId = ref<string>('')

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

// 获取用户的班级
const fetchMyOrganization = async () => {
  if (authState.isAuthenticated && !isTeacher.value && !isAdmin.value) {
    try {
      const token = localStorage.getItem('token')
      const res = await axios.get('/api/my-organizations', {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (res.data && res.data.length > 0) {
        myOrgId.value = res.data[0].id
      }
    } catch (e) {
      console.error(e)
    }
  }
}

const fetchLeaderboard = async () => {
  if (authState.isAuthenticated && !isTeacher.value && !isAdmin.value) {
    try {
      // 先确保获取了班级ID
      if (!myOrgId.value) {
        await fetchMyOrganization()
      }
      
      const token = localStorage.getItem('token')
      let url = '/api/stats/leaderboard?limit=5'
      if (myOrgId.value) {
        url += `&orgId=${myOrgId.value}`
      }
      const res = await axios.get(url, {
        headers: { Authorization: `Bearer ${token}` }
      })
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

      <div class="line-card leaderboard-card clickable" @click="$router.push('/leaderboard')">
        <div class="card-header-clean">
          <h3>排行榜</h3>
          <router-link to="/leaderboard" class="link-btn" @click.stop>全部</router-link>
        </div>
        <ul class="leaderboard-list" v-if="leaderboard.length > 0">
          <li v-for="(user, index) in leaderboard" :key="user.id" class="leaderboard-item">
            <div class="rank-badge" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
            <span class="name">{{ user.nickname || user.userId }}</span>
            <span class="score">{{ user.correctAnswers }} pts</span>
          </li>
        </ul>
        <div v-else class="empty-leaderboard">
          <p>暂无排行数据</p>
          <small>加入班级后可查看班级排名</small>
        </div>
      </div>
    </div>

    <!-- 功能导航网格 -->
    <div class="nav-grid">
      <template v-if="!authState.isAuthenticated">
        <router-link to="/login" class="nav-card">
          <div class="icon-box">
            <LogIn :size="24" />
          </div>
          <div class="nav-content">
            <h3>登录</h3>
            <p>登录您的账户</p>
          </div>
          <div class="arrow-icon"><ArrowRight :size="20" /></div>
        </router-link>

        <router-link to="/register" class="nav-card">
          <div class="icon-box">
            <UserPlus :size="24" />
          </div>
          <div class="nav-content">
            <h3>注册</h3>
            <p>创建新账户</p>
          </div>
          <div class="arrow-icon"><ArrowRight :size="20" /></div>
        </router-link>
      </template>

      <template v-if="isTeacher">
        <router-link to="/questions" class="nav-card">
          <div class="icon-box">
            <Book :size="24" />
          </div>
          <div class="nav-content">
            <h3>题库管理</h3>
            <p>浏览所有题目</p>
          </div>
        </router-link>

        <router-link to="/questions/add" class="nav-card">
          <div class="icon-box">
            <Plus :size="24" />
          </div>
          <div class="nav-content">
            <h3>添加题目</h3>
            <p>创建新题目</p>
          </div>
        </router-link>

        <router-link to="/paper-generation" class="nav-card">
          <div class="icon-box">
            <FileText :size="24" />
          </div>
          <div class="nav-content">
            <h3>智能组卷</h3>
            <p>创建试卷</p>
          </div>
        </router-link>

        <router-link to="/questions/review" class="nav-card">
          <div class="icon-box">
            <CheckCircle :size="24" />
          </div>
          <div class="nav-content">
            <h3>题目审核</h3>
            <p>审核学生提交的题目</p>
          </div>
        </router-link>
      </template>

      <router-link v-if="authState.isAuthenticated && !isAdmin" to="/exam" class="nav-card">
        <div class="icon-box">
          <FileClock :size="24" />
        </div>
        <div class="nav-content">
          <h3>在线考试</h3>
          <p>进入考试</p>
        </div>
      </router-link>

      <template v-if="isAdmin">
        <router-link to="/admin/users" class="nav-card">
          <div class="icon-box">
            <Users :size="24" />
          </div>
          <div class="nav-content">
            <h3>用户管理</h3>
            <p>管理系统用户</p>
          </div>
        </router-link>

        <router-link to="/admin/organizations" class="nav-card">
          <div class="icon-box">
            <Home :size="24" />
          </div>
          <div class="nav-content">
            <h3>组织架构</h3>
            <p>管理班级组织</p>
          </div>
        </router-link>

        <router-link to="/admin/statistics" class="nav-card">
          <div class="icon-box">
            <BarChart3 :size="24" />
          </div>
          <div class="nav-content">
            <h3>数据统计</h3>
            <p>全局数据分析</p>
          </div>
        </router-link>

        <router-link to="/admin/monitor" class="nav-card">
          <div class="icon-box">
            <Activity :size="24" />
          </div>
          <div class="nav-content">
            <h3>系统监控</h3>
            <p>服务健康状态</p>
          </div>
        </router-link>

        <router-link to="/admin/logs" class="nav-card">
          <div class="icon-box">
            <ScrollText :size="24" />
          </div>
          <div class="nav-content">
            <h3>操作日志</h3>
            <p>系统操作审计</p>
          </div>
        </router-link>

        <router-link to="/admin/announcements" class="nav-card">
          <div class="icon-box">
            <Megaphone :size="24" />
          </div>
          <div class="nav-content">
            <h3>公告管理</h3>
            <p>系统公告发布</p>
          </div>
        </router-link>
      </template>

      <template v-if="!isTeacher && !isAdmin && authState.isAuthenticated">
        <router-link to="/practice" class="nav-card">
          <div class="icon-box">
            <ClipboardList :size="24" />
          </div>
          <div class="nav-content">
            <h3>自主练习</h3>
            <p>历史试卷练习</p>
          </div>
        </router-link>

        <router-link to="/questions/add" class="nav-card">
          <div class="icon-box">
            <Plus :size="24" />
          </div>
          <div class="nav-content">
            <h3>我要出题</h3>
            <p>提交题目供教师审核</p>
          </div>
        </router-link>

        <router-link to="/my-organizations" class="nav-card">
          <div class="icon-box">
            <Users :size="24" />
          </div>
          <div class="nav-content">
            <h3>我的班级</h3>
            <p>加入班级组织</p>
          </div>
        </router-link>
      </template>

      <router-link v-if="isTeacher" to="/knowledge-point-manage" class="nav-card">
        <div class="icon-box">
          <Network :size="24" />
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

/* Clickable card */
.clickable {
  cursor: pointer;
  transition: all 0.2s;
}
.clickable:hover {
  border-color: var(--line-primary);
  box-shadow: var(--line-shadow-md);
}

/* Empty leaderboard */
.empty-leaderboard {
  text-align: center;
  padding: 24px 0;
  color: var(--line-text-secondary);
}
.empty-leaderboard p {
  margin: 0 0 4px 0;
  font-size: 14px;
}
.empty-leaderboard small {
  font-size: 12px;
  opacity: 0.8;
}

/* Nav Grid */
.nav-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

@media (max-width: 768px) {
  .nav-grid {
    grid-template-columns: 1fr;
  }
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
