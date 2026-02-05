<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { Users, FileText, BookOpen, GraduationCap, TrendingUp, TrendingDown, Activity } from 'lucide-vue-next'

interface StatsOverview {
  totalUsers: number
  totalTeachers: number
  totalStudents: number
  totalQuestions: number
  totalPapers: number
  totalExams: number
  activeUsersToday: number
  newUsersThisWeek: number
}

interface TrendData {
  label: string
  value: number
}

const loading = ref(false)
const stats = ref<StatsOverview>({
  totalUsers: 0,
  totalTeachers: 0,
  totalStudents: 0,
  totalQuestions: 0,
  totalPapers: 0,
  totalExams: 0,
  activeUsersToday: 0,
  newUsersThisWeek: 0
})

const userTrend = ref<TrendData[]>([])
const questionTrend = ref<TrendData[]>([])
const examTrend = ref<TrendData[]>([])
const error = ref('')

const fetchStatistics = async () => {
  loading.value = true
  error.value = ''
  try {
    const token = localStorage.getItem('token')
    
    // 获取概览统计
    const statsResponse = await axios.get('/api/admin/statistics', {
      headers: { Authorization: `Bearer ${token}` }
    })
    stats.value = statsResponse.data
    
    // 获取趋势数据
    try {
      const trendsResponse = await axios.get('/api/admin/statistics/trends', {
        headers: { Authorization: `Bearer ${token}` }
      })
      userTrend.value = trendsResponse.data.userTrend || []
      questionTrend.value = trendsResponse.data.questionTrend || []
      examTrend.value = trendsResponse.data.examTrend || []
    } catch {
      // 趋势数据获取失败时使用空数组
      userTrend.value = []
      questionTrend.value = []
      examTrend.value = []
    }
  } catch (err) {
    console.error('Failed to fetch statistics', err)
    error.value = '获取统计数据失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const maxUserTrend = computed(() => userTrend.value.length > 0 ? Math.max(...userTrend.value.map(d => d.value)) : 0)
const maxQuestionTrend = computed(() => questionTrend.value.length > 0 ? Math.max(...questionTrend.value.map(d => d.value)) : 0)
const maxExamTrend = computed(() => examTrend.value.length > 0 ? Math.max(...examTrend.value.map(d => d.value)) : 0)

onMounted(() => {
  fetchStatistics()
})
</script>

<template>
  <div class="admin-container container">
    <div class="header-row">
      <h1 class="page-title">数据统计</h1>
      <p class="page-subtitle">全局数据分析概览</p>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button class="google-btn primary-btn" @click="fetchStatistics">重试</button>
    </div>

    <template v-else>
      <!-- 统计卡片 -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon users-icon">
            <Users :size="24" />
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.totalUsers.toLocaleString() }}</span>
            <span class="stat-label">总用户数</span>
          </div>
          <div class="stat-detail">
            <span class="detail-item">
              <span class="detail-label">教师</span>
              <span class="detail-value">{{ stats.totalTeachers }}</span>
            </span>
            <span class="detail-item">
              <span class="detail-label">学生</span>
              <span class="detail-value">{{ stats.totalStudents }}</span>
            </span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon questions-icon">
            <BookOpen :size="24" />
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.totalQuestions.toLocaleString() }}</span>
            <span class="stat-label">题目总数</span>
          </div>
          <div class="stat-trend up">
            <TrendingUp :size="16" />
            <span>+12.5%</span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon papers-icon">
            <FileText :size="24" />
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.totalPapers }}</span>
            <span class="stat-label">试卷总数</span>
          </div>
          <div class="stat-trend up">
            <TrendingUp :size="16" />
            <span>+8.3%</span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon exams-icon">
            <GraduationCap :size="24" />
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.totalExams }}</span>
            <span class="stat-label">考试场次</span>
          </div>
          <div class="stat-trend down">
            <TrendingDown :size="16" />
            <span>-3.2%</span>
          </div>
        </div>

        <div class="stat-card highlight">
          <div class="stat-icon active-icon">
            <Activity :size="24" />
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.activeUsersToday }}</span>
            <span class="stat-label">今日活跃用户</span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon new-icon">
            <Users :size="24" />
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.newUsersThisWeek }}</span>
            <span class="stat-label">本周新增用户</span>
          </div>
        </div>
      </div>

      <!-- 趋势图表 -->
      <div class="charts-grid">
        <div class="google-card chart-card">
          <h3 class="chart-title">用户活跃趋势 (本周)</h3>
          <div class="bar-chart">
            <div 
              v-for="item in userTrend" 
              :key="item.label" 
              class="bar-item"
            >
              <div class="bar" :style="{ height: (item.value / maxUserTrend * 100) + '%' }">
                <span class="bar-value">{{ item.value }}</span>
              </div>
              <span class="bar-label">{{ item.label }}</span>
            </div>
          </div>
        </div>

        <div class="google-card chart-card">
          <h3 class="chart-title">题目增长趋势 (近6月)</h3>
          <div class="bar-chart">
            <div 
              v-for="item in questionTrend" 
              :key="item.label" 
              class="bar-item"
            >
              <div class="bar success" :style="{ height: (item.value / maxQuestionTrend * 100) + '%' }">
                <span class="bar-value">{{ item.value }}</span>
              </div>
              <span class="bar-label">{{ item.label }}</span>
            </div>
          </div>
        </div>

        <div class="google-card chart-card">
          <h3 class="chart-title">考试场次趋势 (近6月)</h3>
          <div class="bar-chart">
            <div 
              v-for="item in examTrend" 
              :key="item.label" 
              class="bar-item"
            >
              <div class="bar warning" :style="{ height: (item.value / maxExamTrend * 100) + '%' }">
                <span class="bar-value">{{ item.value }}</span>
              </div>
              <span class="bar-label">{{ item.label }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.admin-container {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-row {
  margin-bottom: 32px;
}

.page-subtitle {
  color: var(--line-text-secondary);
  margin-top: 8px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px;
  color: var(--line-text-secondary);
}

.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px;
  color: var(--line-text-secondary);
  gap: 16px;
}

.error-state p {
  margin: 0;
  color: #ea4335;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--line-border);
  border-top-color: var(--line-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  background: var(--line-card-bg);
  border: 1px solid var(--line-border);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.stat-card.highlight {
  background: linear-gradient(135deg, #1a73e8 0%, #4285f4 100%);
  color: white;
}

.stat-card.highlight .stat-label {
  color: rgba(255, 255, 255, 0.8);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.users-icon { background: #e8f0fe; color: #1a73e8; }
.questions-icon { background: #e6f4ea; color: #1e8e3e; }
.papers-icon { background: #fef7e0; color: #f9ab00; }
.exams-icon { background: #fce8e6; color: #d93025; }
.active-icon { background: rgba(255, 255, 255, 0.2); color: white; }
.new-icon { background: #f3e8fd; color: #9334e6; }

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 32px;
  font-weight: 600;
  color: var(--line-text);
}

.stat-card.highlight .stat-value {
  color: white;
}

.stat-label {
  font-size: 14px;
  color: var(--line-text-secondary);
}

.stat-detail {
  display: flex;
  gap: 24px;
  padding-top: 12px;
  border-top: 1px solid var(--line-border);
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-label {
  font-size: 12px;
  color: var(--line-text-secondary);
}

.detail-value {
  font-size: 16px;
  font-weight: 500;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 500;
}

.stat-trend.up {
  color: #1e8e3e;
}

.stat-trend.down {
  color: #d93025;
}

/* Charts Grid */
.charts-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

@media (max-width: 1024px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  padding: 24px;
}

.chart-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 24px;
  color: var(--line-text);
}

.bar-chart {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  height: 200px;
  gap: 12px;
}

.bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.bar {
  width: 100%;
  max-width: 40px;
  background: linear-gradient(180deg, #1a73e8 0%, #4285f4 100%);
  border-radius: 6px 6px 0 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 8px;
  min-height: 20px;
  transition: height 0.3s ease;
}

.bar.success {
  background: linear-gradient(180deg, #1e8e3e 0%, #34a853 100%);
}

.bar.warning {
  background: linear-gradient(180deg, #f9ab00 0%, #fbbc04 100%);
}

.bar-value {
  font-size: 11px;
  font-weight: 500;
  color: white;
}

.bar-label {
  margin-top: 8px;
  font-size: 12px;
  color: var(--line-text-secondary);
}
</style>
