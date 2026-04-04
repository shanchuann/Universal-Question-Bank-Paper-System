<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import axios from 'axios'
import { Users, FileText, BookOpen, GraduationCap, TrendingUp, TrendingDown, Activity } from 'lucide-vue-next'
import GoogleSelect from '@/components/GoogleSelect.vue'

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

type RangeOption = '7d' | '30d' | '90d' | '180d'

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
const selectedRange = ref<RangeOption>('30d')

const rangeLabelMap: Record<RangeOption, string> = {
  '7d': '近7天',
  '30d': '近30天',
  '90d': '近90天',
  '180d': '近180天'
}

const rangeOptions: Array<{ value: RangeOption; label: string }> = [
  { value: '7d', label: '近7天' },
  { value: '30d', label: '近30天' },
  { value: '90d', label: '近90天' },
  { value: '180d', label: '近180天' }
]

const fetchStatistics = async () => {
  loading.value = true
  error.value = ''
  try {
    const token = localStorage.getItem('token')
    const params = { range: selectedRange.value }
    
    // 获取概览统计
    const statsResponse = await axios.get('/api/admin/statistics', {
      headers: { Authorization: `Bearer ${token}` },
      params
    })
    stats.value = statsResponse.data
    
    // 获取趋势数据
    try {
      const trendsResponse = await axios.get('/api/admin/statistics/trends', {
        headers: { Authorization: `Bearer ${token}` },
        params
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
const rangeLabel = computed(() => rangeLabelMap[selectedRange.value])

type TrendDirection = 'up' | 'down' | 'flat'

const calcTrendMeta = (series: TrendData[]) => {
  if (!series || series.length < 2) {
    return { direction: 'flat' as TrendDirection, text: '数据不足' }
  }

  const firstPoint = series[0]
  const lastPoint = series[series.length - 1]
  if (!firstPoint || !lastPoint) {
    return { direction: 'flat' as TrendDirection, text: '数据不足' }
  }

  const first = firstPoint.value
  const last = lastPoint.value
  if (first === 0) {
    if (last === 0) return { direction: 'flat' as TrendDirection, text: '无变化' }
    return { direction: 'up' as TrendDirection, text: '新增趋势' }
  }

  const delta = ((last - first) / first) * 100
  if (Math.abs(delta) < 0.1) {
    return { direction: 'flat' as TrendDirection, text: '无变化' }
  }
  const sign = delta > 0 ? '+' : ''
  return {
    direction: delta > 0 ? ('up' as TrendDirection) : ('down' as TrendDirection),
    text: `${sign}${delta.toFixed(1)}%`
  }
}

const questionTrendMeta = computed(() => calcTrendMeta(questionTrend.value))
const examTrendMeta = computed(() => calcTrendMeta(examTrend.value))

const calcBarHeight = (value: number, max: number) => {
  if (max <= 0) {
    return '0%'
  }
  return `${Math.max(6, (value / max) * 100)}%`
}

onMounted(() => {
  fetchStatistics()
})

watch(selectedRange, () => {
  fetchStatistics()
})
</script>

<template>
  <div class="admin-container container">
    <div class="header-row">
      <div>
        <h1 class="page-title">数据统计</h1>
        <p class="page-subtitle">全局数据分析概览</p>
      </div>
      <div class="header-actions">
        <GoogleSelect
          v-model="selectedRange"
          :options="rangeOptions"
          placeholder="选择时间范围"
        />
      </div>
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
          <div class="stat-trend" :class="questionTrendMeta.direction">
            <TrendingUp v-if="questionTrendMeta.direction === 'up'" :size="16" />
            <TrendingDown v-else-if="questionTrendMeta.direction === 'down'" :size="16" />
            <Activity v-else :size="16" />
            <span>{{ questionTrendMeta.text }}</span>
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
          <div class="stat-note">当前范围累计</div>
        </div>

        <div class="stat-card">
          <div class="stat-icon exams-icon">
            <GraduationCap :size="24" />
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.totalExams }}</span>
            <span class="stat-label">考试场次</span>
          </div>
          <div class="stat-trend" :class="examTrendMeta.direction">
            <TrendingUp v-if="examTrendMeta.direction === 'up'" :size="16" />
            <TrendingDown v-else-if="examTrendMeta.direction === 'down'" :size="16" />
            <Activity v-else :size="16" />
            <span>{{ examTrendMeta.text }}</span>
          </div>
        </div>

        <div class="stat-card highlight">
          <div class="stat-icon active-icon">
            <Activity :size="24" />
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.activeUsersToday }}</span>
            <span class="stat-label">{{ rangeLabel }}活跃用户</span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon new-icon">
            <Users :size="24" />
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.newUsersThisWeek }}</span>
            <span class="stat-label">{{ rangeLabel }}新增用户</span>
          </div>
        </div>
      </div>

      <!-- 趋势图表 -->
      <div class="charts-grid">
        <div class="google-card chart-card">
          <h3 class="chart-title">用户活跃趋势 ({{ rangeLabel }})</h3>
          <div v-if="userTrend.length > 0" class="bar-chart">
            <div 
              v-for="item in userTrend" 
              :key="item.label" 
              class="bar-item"
            >
              <div class="bar" :style="{ height: calcBarHeight(item.value, maxUserTrend) }">
                <span class="bar-value">{{ item.value }}</span>
              </div>
              <span class="bar-label">{{ item.label }}</span>
            </div>
          </div>
          <div v-else class="chart-empty">暂无用户趋势数据</div>
        </div>

        <div class="google-card chart-card">
          <h3 class="chart-title">题目增长趋势 ({{ rangeLabel }})</h3>
          <div v-if="questionTrend.length > 0" class="bar-chart">
            <div 
              v-for="item in questionTrend" 
              :key="item.label" 
              class="bar-item"
            >
              <div class="bar success" :style="{ height: calcBarHeight(item.value, maxQuestionTrend) }">
                <span class="bar-value">{{ item.value }}</span>
              </div>
              <span class="bar-label">{{ item.label }}</span>
            </div>
          </div>
          <div v-else class="chart-empty">暂无题目趋势数据</div>
        </div>

        <div class="google-card chart-card">
          <h3 class="chart-title">考试场次趋势 ({{ rangeLabel }})</h3>
          <div v-if="examTrend.length > 0" class="bar-chart">
            <div 
              v-for="item in examTrend" 
              :key="item.label" 
              class="bar-item"
            >
              <div class="bar warning" :style="{ height: calcBarHeight(item.value, maxExamTrend) }">
                <span class="bar-value">{{ item.value }}</span>
              </div>
              <span class="bar-label">{{ item.label }}</span>
            </div>
          </div>
          <div v-else class="chart-empty">暂无考试趋势数据</div>
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
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 32px;
}

.header-actions {
  display: flex;
  align-items: center;
  margin-right: 16px;
}

@media (max-width: 768px) {
  .header-actions {
    margin-right: 0;
  }
}

.google-select {
  min-width: 128px;
  padding: 10px 14px;
  border: 1px solid var(--line-border);
  border-radius: 10px;
  background: var(--line-card-bg);
  color: var(--line-text);
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
  color: var(--line-error);
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
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
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
  box-shadow: var(--line-shadow-md);
}

.stat-card.highlight {
  background: linear-gradient(135deg, var(--line-primary) 0%, var(--line-primary-hover) 100%);
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

.users-icon { background: color-mix(in srgb, var(--line-accent) 14%, white); color: var(--line-accent); }
.questions-icon { background: color-mix(in srgb, var(--line-success) 14%, white); color: var(--line-success); }
.papers-icon { background: color-mix(in srgb, var(--line-warning) 18%, white); color: color-mix(in srgb, var(--line-warning) 85%, black); }
.exams-icon { background: color-mix(in srgb, var(--line-error) 14%, white); color: var(--line-error); }
.active-icon { background: rgba(255, 255, 255, 0.2); color: white; }
.new-icon { background: color-mix(in srgb, var(--line-primary) 12%, white); color: var(--line-primary); }

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
  border-top: none;
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
  color: var(--line-success);
}

.stat-trend.down {
  color: var(--line-error);
}

.stat-trend.flat {
  color: var(--line-text-secondary);
}

.stat-note {
  font-size: 13px;
  color: var(--line-text-secondary);
}

/* Charts Grid */
.charts-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

@media (max-width: 1024px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  padding: 24px;
  min-width: 0;
  overflow: hidden;
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
  gap: 6px;
  width: 100%;
  overflow: hidden;
}

.bar-item {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.bar {
  width: 100%;
  max-width: 40px;
  background: linear-gradient(180deg, var(--line-primary) 0%, var(--line-primary-hover) 100%);
  border-radius: 6px 6px 0 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 8px;
  min-height: 20px;
  transition: height 0.3s ease;
}

.bar.success {
  background: linear-gradient(180deg, var(--line-success) 0%, color-mix(in srgb, var(--line-success) 78%, white) 100%);
}

.bar.warning {
  background: linear-gradient(180deg, var(--line-warning) 0%, color-mix(in srgb, var(--line-warning) 80%, white) 100%);
}

.bar-value {
  font-size: 11px;
  font-weight: 500;
  color: white;
}

.bar-label {
  margin-top: 8px;
  font-size: 10px;
  color: var(--line-text-secondary);
  width: 100%;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chart-empty {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--line-text-secondary);
  border: 1px dashed var(--line-border);
  border-radius: 12px;
  background: color-mix(in srgb, var(--line-bg-soft) 60%, white);
}
</style>

