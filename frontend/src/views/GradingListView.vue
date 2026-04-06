<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

interface ExamListItem {
  sessionId: string
  paperVersionId: string
  userId: string
  nickname?: string
  username?: string
  avatarUrl?: string
  score?: number
  status?: string
  startAt?: string
  endAt?: string
}

const exams = ref<ExamListItem[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(0)
const size = ref(10)
const totalElements = ref(0)
const filterPaperId = ref('')
const filterUserId = ref('')

const router = useRouter()

const getAuthHeaders = () => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const fetchExams = async () => {
  loading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams()
    if (filterPaperId.value) params.append('paperId', filterPaperId.value)
    if (filterUserId.value) params.append('userId', filterUserId.value)
    params.append('page', String(page.value))
    params.append('size', String(size.value))

    const response = await axios.get(`/api/exams?${params.toString()}`, {
      headers: getAuthHeaders()
    })
    exams.value = response.data.content || []
    totalElements.value = response.data.totalElements || 0
  } catch (err) {
    error.value = '加载考试记录失败'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const goToGrade = (examId: string) => {
  router.push(`/grading/${examId}`)
}

watch([filterPaperId, filterUserId], () => {
  page.value = 0
  fetchExams()
})

const getStatusClass = (status?: string) => {
  if (!status || status === '待阅卷') return 'pending'
  if (status === '已阅卷') return 'completed'
  if (status === '进行中') return 'grading'
  return 'pending'
}

onMounted(fetchExams)
</script>

<template>
  <div class="grading-list-view">
    <div class="page-header">
      <h1 class="page-title">阅卷管理</h1>
      <p class="page-subtitle">管理所有学生提交的试卷并进行评分</p>
    </div>

    <div class="line-card filter-card">
      <div class="filter-row">
        <div class="filter-group">
          <label class="filter-label">试卷ID</label>
          <input v-model="filterPaperId" class="line-input" placeholder="输入试卷ID筛选" />
        </div>
        <div class="filter-group">
          <label class="filter-label">用户ID</label>
          <input v-model="filterUserId" class="line-input" placeholder="输入用户ID筛选" />
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="line-card error-state">
      <div class="error-content">
        <svg
          width="48"
          height="48"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
        >
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="8" x2="12" y2="12"></line>
          <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
        <p>{{ error }}</p>
        <button @click="fetchExams" class="line-btn primary-btn">重试</button>
      </div>
    </div>

    <div v-else class="line-card table-card">
      <div class="table-responsive">
        <table class="line-table">
        <thead>
          <tr>
            <th class="col-id">考试ID</th>
            <th class="col-id">试卷ID</th>
            <th class="col-user">学生</th>
            <th class="col-score">得分</th>
            <th class="col-status">状态</th>
            <th class="col-action">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="exam in exams" :key="exam.sessionId">
            <td class="col-id font-mono">{{ exam.sessionId }}</td>
            <td class="col-id font-mono">{{ exam.paperVersionId }}</td>
            <td class="col-user">
              <div class="user-info">
                <div class="avatar-sm" :class="{ 'has-img': exam.avatarUrl }">
                  <img
                    v-if="exam.avatarUrl"
                    :src="exam.avatarUrl"
                    :alt="exam.nickname || exam.username || ''"
                    @error="($event.target as HTMLImageElement).style.display = 'none'"
                  />
                  <span v-else>{{
                    (exam.nickname || exam.username || exam.userId || 'U').charAt(0).toUpperCase()
                  }}</span>
                </div>
                <span>{{ exam.nickname || exam.username || exam.userId }}</span>
              </div>
            </td>
            <td class="col-score font-mono">{{ exam.score ?? '-' }}</td>
            <td class="col-status">
              <span class="status-badge" :class="getStatusClass(exam.status)">
                {{ exam.status || '待阅卷' }}
              </span>
            </td>
            <td class="col-action">
              <button @click="goToGrade(exam.sessionId)" class="line-btn text-btn sm">阅卷</button>
            </td>
          </tr>
          <tr v-if="exams.length === 0">
            <td colspan="6" class="empty-state-cell">
              <div class="empty-state-content">
                <svg
                  width="48"
                  height="48"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                >
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                  <line x1="16" y1="13" x2="8" y2="13"></line>
                  <line x1="16" y1="17" x2="8" y2="17"></line>
                  <polyline points="10 9 9 9 8 9"></polyline>
                </svg>
                <p>暂无相关考试记录</p>
              </div>
            </td>
          </tr>
        </tbody>
        </table>
      </div>

      <div class="pagination-bar">
        <button
          :disabled="page === 0"
          @click="page--; fetchExams()"
          class="line-btn outline-btn sm"
        >
          上一页
        </button>
        <span class="page-info"
          >第 {{ page + 1 }} 页 / 共 {{ Math.ceil(totalElements / size) || 1 }} 页</span
        >
        <button
          :disabled="(page + 1) * size >= totalElements"
          @click="page++; fetchExams()"
          class="line-btn outline-btn sm"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.grading-list-view {
  padding: 32px 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  color: var(--line-text);
  margin: 0 0 8px 0;
}

.page-subtitle {
  color: var(--line-text-secondary);
  font-size: 14px;
  margin: 0;
}

.filter-card {
  margin-bottom: 24px;
  padding: 24px;
}

.filter-row {
  display: flex;
  gap: 24px;
  align-items: flex-end;
}

.filter-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--line-text-secondary);
}

.table-card {
  padding: 0;
  overflow: hidden;
  background: var(--line-bg);
}

.line-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 920px; /* ensure horizontal scroll when container is narrower */
}

.line-table th {
  padding: 16px;
  text-align: center;
  font-weight: 600;
  font-size: 13px;
  color: var(--line-text-secondary);
  background: var(--line-bg-soft);
  border-bottom: 1px solid var(--line-border);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.line-table td {
  padding: 16px;
  font-size: 14px;
  color: var(--line-text);
  border-bottom: 1px solid var(--line-border);
  vertical-align: middle;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.line-table tr:hover td {
  background: var(--line-bg-hover);
}

.line-table tr:last-child td {
  border-bottom: none;
}

.col-id {
  width: 100px;
  color: var(--line-text-secondary);
}
.col-score {
  width: 100px;
  font-weight: 600;
}
.col-status {
  width: 120px;
}
.col-action {
  width: 100px;
}

.font-mono {
  font-family: 'SF Mono', 'Roboto Mono', Menlo, monospace;
  font-size: 13px;
}

.user-info {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  justify-content: flex-start;
}

.avatar-sm {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--line-primary);
  border: 1px solid var(--line-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: white;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar-sm.has-img {
  background: transparent;
}

.avatar-sm img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 99px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.status-badge.completed {
  background: #dcfce7; /* Green 100 */
  color: #166534; /* Green 800 */
}

.status-badge.pending {
  background: #fef9c3; /* Yellow 100 */
  color: #854d0e; /* Yellow 800 */
}

.status-badge.grading {
  background: #dbeafe; /* Blue 100 */
  color: #1e40af; /* Blue 800 */
}

.pagination-bar {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  border-top: none;
  background: var(--line-bg);
}

/* Responsive horizontal scroll wrapper */
.table-responsive {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
.table-responsive::-webkit-scrollbar {
  height: 8px;
}
.table-responsive::-webkit-scrollbar-thumb {
  background: rgba(0,0,0,0.08);
  border-radius: 8px;
}

.page-info {
  font-size: 14px;
  color: var(--line-text-secondary);
}

/* Loading & Empty States */
.loading-state {
  padding: 64px;
  text-align: center;
  color: var(--line-text-secondary);
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--line-border);
  border-top-color: var(--line-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-state-cell {
  padding: 64px 0 !important;
  text-align: center;
}

.empty-state-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: var(--line-text-secondary);
}

.empty-state-content svg {
  opacity: 0.5;
}

.error-state {
  padding: 48px;
  display: flex;
  justify-content: center;
}

.error-content {
  text-align: center;
  color: var(--line-error);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
</style>
