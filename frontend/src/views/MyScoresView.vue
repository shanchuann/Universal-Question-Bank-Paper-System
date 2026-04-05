<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { authState } from '@/states/authState'
import { useToast } from '@/composables/useToast'

const { showToast } = useToast()
const router = useRouter()

interface ScoreItem {
  sessionId: string
  paperVersionId: string
  paperTitle?: string
  score?: number
  status: string
  startAt?: string
  endAt?: string
}

const scores = ref<ScoreItem[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(10)
const totalElements = ref(0)

const getAuthHeaders = () => ({
  'Content-Type': 'application/json',
  Authorization: `Bearer ${localStorage.getItem('token')}`
})

const fetchScores = async () => {
  if (!authState.isAuthenticated || !authState.user.id) {
    showToast({ message: '请先登录', type: 'warning' })
    router.push('/login')
    return
  }

  loading.value = true
  try {
    const params = new URLSearchParams()
    params.append('userId', authState.user.id)
    params.append('page', String(page.value))
    params.append('size', String(size.value))

    const response = await axios.get(`/api/exams/my-scores?${params.toString()}`, {
      headers: getAuthHeaders()
    })
    scores.value = response.data.content || []
    totalElements.value = response.data.totalElements || 0
  } catch (err) {
    showToast({ message: '加载成绩失败', type: 'error' })
    console.error(err)
  } finally {
    loading.value = false
  }
}

const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getStatusInfo = (status: string) => {
  switch (status) {
    case '已阅卷':
      return { label: '已出分', class: 'graded' }
    case '待阅卷':
      return { label: '阅卷中', class: 'pending' }
    case '阅卷中':
      return { label: '阅卷中', class: 'grading' }
    default:
      return { label: '进行中', class: 'in-progress' }
  }
}

const getScoreClass = (score?: number) => {
  if (score === undefined || score === null) return ''
  if (score >= 90) return 'excellent'
  if (score >= 80) return 'good'
  if (score >= 60) return 'pass'
  return 'fail'
}

const viewDetail = (sessionId: string) => {
  router.push(`/my-scores/${sessionId}`)
}

const totalPages = computed(() => Math.ceil(totalElements.value / size.value) || 1)

onMounted(() => {
  if (authState.isAuthenticated) {
    fetchScores()
  } else {
    // 延迟加载，等待认证状态
    setTimeout(() => {
      if (authState.isAuthenticated) {
        fetchScores()
      }
    }, 500)
  }
})
</script>

<template>
  <div class="my-scores-view">
    <div class="page-header">
      <h1 class="page-title">我的成绩</h1>
      <p class="page-subtitle">查看您的考试成绩和答题详情</p>
    </div>

    <div class="line-card content-card">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="scores.length === 0" class="empty-state">
        <svg
          width="80"
          height="80"
          viewBox="0 0 24 24"
          fill="none"
          stroke="#9aa0a6"
          stroke-width="1"
        >
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
          <polyline points="14 2 14 8 20 8"></polyline>
          <line x1="16" y1="13" x2="8" y2="13"></line>
          <line x1="16" y1="17" x2="8" y2="17"></line>
        </svg>
        <h3>暂无成绩记录</h3>
        <p>您还没有完成任何考试，去参加考试吧！</p>
        <button @click="router.push('/exams/list')" class="line-btn primary-btn">去考试</button>
      </div>

      <div v-else class="scores-list">
        <div
          v-for="item in scores"
          :key="item.sessionId"
          class="score-card"
          :class="getScoreClass(item.score)"
        >
          <div class="card-main">
            <div class="exam-info">
              <h3 class="exam-title">{{ item.paperTitle || `试卷 #${item.paperVersionId}` }}</h3>
              <div class="exam-meta">
                <span class="meta-item">考试时间：{{ formatDateTime(item.endAt) }}</span>
              </div>
            </div>

            <div class="score-section">
              <div v-if="item.status === '已阅卷' && item.score !== null" class="score-display">
                <span class="score-value" :class="getScoreClass(item.score)">{{ item.score }}</span>
                <span class="score-label">分</span>
              </div>
              <div v-else class="status-display">
                <span>{{ getStatusInfo(item.status).label }}</span>
              </div>
            </div>
          </div>

          <div class="card-footer">
            <span class="status-badge" :class="getStatusInfo(item.status).class">
              {{ getStatusInfo(item.status).label }}
            </span>
            <button
              v-if="item.status === '已阅卷'"
              @click="viewDetail(item.sessionId)"
              class="line-btn text-btn"
            >
              查看详情 →
            </button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="scores.length > 0" class="pagination-bar">
        <button
          :disabled="page === 0"
          @click="page--; fetchScores()"
          class="line-btn outline-btn sm"
        >
          上一页
        </button>
        <span class="page-info">第 {{ page + 1 }} 页 / 共 {{ totalPages }} 页</span>
        <button
          :disabled="(page + 1) * size >= totalElements"
          @click="page++; fetchScores()"
          class="line-btn outline-btn sm"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.my-scores-view {
  padding: 28px 24px;
  max-width: 980px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  color: var(--line-text);
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 500;
}

.page-subtitle {
  color: var(--line-text-secondary);
  margin: 0;
  font-size: 14px;
}

.content-card {
  padding: 24px;
  min-height: 400px;
  border-radius: 16px;
  border: 1px solid var(--line-border);
  background:
    radial-gradient(circle at top right, rgba(26, 115, 232, 0.06), transparent 40%), var(--line-bg);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: var(--line-text-secondary);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--line-bg-soft);
  border-top-color: var(--line-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  color: var(--line-text-secondary);
}

.empty-state h3 {
  font-size: 18px;
  font-weight: 500;
  color: var(--line-text);
  margin: 16px 0 8px 0;
}

.empty-state p {
  margin: 0 0 20px 0;
}

.primary-btn {
  background: var(--line-primary);
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.primary-btn:hover {
  background: var(--line-primary-dark, #1557b0);
}

.scores-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.score-card {
  background: color-mix(in srgb, var(--line-bg) 92%, white 8%);
  border: 1px solid var(--line-border);
  border-radius: 14px;
  padding: 22px;
  transition: all 0.2s;
}

.score-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: var(--line-primary);
  transform: translateY(-1px);
}

.score-card.excellent {
  border-left: 4px solid #34a853;
}

.score-card.good {
  border-left: 4px solid #1a73e8;
}

.score-card.pass {
  border-left: 4px solid #fbbc04;
}

.score-card.fail {
  border-left: 4px solid #ea4335;
}

.card-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.exam-info {
  flex: 1;
}

.exam-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--line-text);
  margin: 0 0 8px 0;
}

.exam-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  font-size: 13px;
  color: var(--line-text-secondary);
}

.score-section {
  text-align: right;
}

.score-display {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.score-value {
  font-size: 32px;
  font-weight: 500;
  line-height: 1;
}

.score-value.excellent {
  color: #34a853;
}
.score-value.good {
  color: #1a73e8;
}
.score-value.pass {
  color: #fbbc04;
}
.score-value.fail {
  color: #ea4335;
}

.score-label {
  font-size: 14px;
  color: var(--line-text-secondary);
}

.status-display {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--line-text-secondary);
  font-size: 14px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px dashed var(--line-border);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.2px;
}

.status-badge.graded {
  background: #e6f4ea;
  color: #34a853;
}

.status-badge.pending,
.status-badge.grading {
  background: #fef7e0;
  color: #f9ab00;
}

.status-badge.in-progress {
  background: #e8f0fe;
  color: #1a73e8;
}

.text-btn {
  background: transparent;
  border: none;
  color: var(--line-primary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  padding: 6px 0;
}

.text-btn:hover {
  color: var(--line-primary-dark, #1557b0);
}

.outline-btn {
  background: transparent;
  border: 1px solid var(--line-border);
  color: var(--line-text);
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.outline-btn:hover:not(:disabled) {
  border-color: var(--line-primary);
  color: var(--line-primary);
}

.outline-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.outline-btn.sm {
  padding: 6px 12px;
  font-size: 12px;
}

.pagination-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 28px;
  padding-top: 20px;
  border-top: none;
}

.page-info {
  font-size: 13px;
  color: var(--line-text-secondary);
}

@media (max-width: 600px) {
  .card-main {
    flex-direction: column;
    gap: 16px;
  }

  .score-section {
    text-align: left;
  }

  .breadcrumb {
    font-size: 13px;
  }
}
</style>
