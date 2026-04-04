<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { authState } from '@/states/authState'
import { useToast } from '@/composables/useToast'
import 'katex/dist/katex.min.css'
import katex from 'katex'

const { showToast } = useToast()
const route = useRoute()
const router = useRouter()
const examId = route.params.id as string

interface QuestionScore {
  questionId: string
  stem?: string
  type?: string
  userAnswer?: string
  isCorrect?: boolean
  score?: number
  maxScore?: number
  notes?: string
}

interface ScoreDetail {
  examId: string
  paperId: string
  paperTitle?: string
  score?: number
  gradingStatus?: string
  startTime?: string
  endTime?: string
  questionScores: QuestionScore[]
}

const detail = ref<ScoreDetail | null>(null)
const loading = ref(false)
const error = ref('')

const getAuthHeaders = () => ({
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${localStorage.getItem('token')}`
})

const fetchDetail = async () => {
  if (!authState.isAuthenticated || !authState.user.id) {
    showToast({ message: '请先登录', type: 'warning' })
    router.push('/login')
    return
  }

  loading.value = true
  error.value = ''
  try {
    const response = await axios.get(
      `/api/exams/${examId}/my-score?userId=${authState.user.id}`,
      { headers: getAuthHeaders() }
    )
    detail.value = response.data
  } catch (err: any) {
    if (err.response?.status === 403) {
      error.value = '您没有权限查看这次考试的成绩'
    } else {
      error.value = '加载成绩详情失败'
    }
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

const getScoreClass = (score?: number) => {
  if (score === undefined || score === null) return ''
  if (score >= 90) return 'excellent'
  if (score >= 80) return 'good'
  if (score >= 60) return 'pass'
  return 'fail'
}

const typeLabels: Record<string, string> = {
  'SINGLE_CHOICE': '单选题',
  'MULTIPLE_CHOICE': '多选题',
  'MULTI_CHOICE': '多选题',
  'TRUE_FALSE': '判断题',
  'FILL_BLANK': '填空题',
  'SHORT_ANSWER': '简答题',
  'ESSAY': '论述题'
}

const totalMaxScore = computed(() => {
  if (!detail.value?.questionScores) return 0
  return detail.value.questionScores.reduce((sum, q) => sum + (q.maxScore || 0), 0)
})

const totalGotScore = computed(() => {
  if (!detail.value?.questionScores) return 0
  return detail.value.questionScores.reduce((sum, q) => sum + (q.score || 0), 0)
})

const isQuestionPending = (q: QuestionScore) => {
  const rawPending = q.isCorrect === null || q.isCorrect === undefined
  return detail.value?.gradingStatus !== 'GRADED' && rawPending
}

const isQuestionCorrect = (q: QuestionScore) => {
  if (q.isCorrect === true) return true
  if (isQuestionPending(q)) return false
  if (detail.value?.gradingStatus === 'GRADED') {
    if (typeof q.score === 'number' && typeof q.maxScore === 'number') {
      return q.score >= q.maxScore
    }
  }
  return false
}

const isQuestionIncorrect = (q: QuestionScore) => {
  return !isQuestionPending(q) && !isQuestionCorrect(q)
}

const renderMath = () => {
  setTimeout(() => {
    const formulas = document.querySelectorAll('.ql-formula')
    formulas.forEach((el) => {
      const latex = el.getAttribute('data-value')
      if (latex && !el.hasAttribute('data-rendered')) {
        try {
          katex.render(latex, el as HTMLElement, { throwOnError: false })
          el.setAttribute('data-rendered', 'true')
        } catch (e) {
          console.error(e)
        }
      }
    })
  }, 100)
}

onMounted(() => {
  if (authState.isAuthenticated) {
    fetchDetail().then(renderMath)
  } else {
    setTimeout(() => {
      if (authState.isAuthenticated) {
        fetchDetail().then(renderMath)
      }
    }, 500)
  }
})
</script>

<template>
  <div class="score-detail-view">
    <div class="page-header">
      <h1 class="page-title">成绩详情</h1>
      <button @click="router.back()" class="line-btn outline-btn">← 返回</button>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button @click="router.back()" class="primary-btn">返回</button>
    </div>

    <div v-else-if="detail" class="detail-content">
      <!-- 成绩概览 -->
      <div class="line-card overview-card">
        <div class="overview-left">
          <h2 class="paper-title">{{ detail.paperTitle || `试卷 #${detail.paperId}` }}</h2>
          <div class="meta-info">
            <span class="meta-item">考试时间：{{ formatDateTime(detail.startTime) }} - {{ formatDateTime(detail.endTime) }}</span>
          </div>
        </div>
        <div class="overview-right">
          <div v-if="detail.gradingStatus === 'GRADED'" class="score-circle" :class="getScoreClass(detail.score)">
            <span class="score-number">{{ detail.score }}</span>
            <span class="score-label">分</span>
          </div>
          <div v-else class="pending-badge">
            <span>阅卷中</span>
          </div>
        </div>
      </div>

      <!-- 成绩统计 -->
      <div class="stats-row" v-if="detail.gradingStatus === 'GRADED'">
        <div class="stat-item">
          <span class="stat-label">总题数</span>
          <span class="stat-value">{{ detail.questionScores?.length || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">得分</span>
          <span class="stat-value">{{ totalGotScore.toFixed(1) }} / {{ totalMaxScore.toFixed(1) }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">正确率</span>
          <span class="stat-value">
            {{ totalMaxScore > 0 ? ((totalGotScore / totalMaxScore) * 100).toFixed(0) : 0 }}%
          </span>
        </div>
      </div>

      <!-- 题目详情 -->
      <div class="questions-section">
        <h3 class="section-title">答题详情</h3>
        
        <div 
          v-for="(q, index) in detail.questionScores" 
          :key="q.questionId" 
          class="question-card"
          :class="{ correct: isQuestionCorrect(q), incorrect: isQuestionIncorrect(q), pending: isQuestionPending(q) }"
        >
          <div class="question-header">
            <div class="question-info">
              <span class="q-number">第 {{ index + 1 }} 题</span>
              <span class="q-type">{{ typeLabels[q.type || ''] || q.type || '未知' }}</span>
            </div>
            <div class="question-score">
              <template v-if="q.score !== null && q.score !== undefined">
                <span class="got-score" :class="{ full: q.score === q.maxScore }">{{ q.score }}</span>
                <span class="max-score">/ {{ q.maxScore || 0 }} 分</span>
              </template>
              <span v-else class="pending-score">待评分</span>
            </div>
          </div>

          <div class="question-body">
            <div class="question-stem" v-html="q.stem || '(题目内容缺失)'"></div>
            
            <div class="answer-section">
              <div class="answer-row">
                <span class="answer-label">您的答案：</span>
                <span class="answer-content">{{ q.userAnswer || '(未作答)' }}</span>
              </div>
            </div>

            <div class="result-indicator">
              <template v-if="isQuestionCorrect(q)">
                <span class="correct-badge">✔ 回答正确</span>
              </template>
              <template v-else-if="isQuestionIncorrect(q)">
                <span class="incorrect-badge">✖ 回答错误</span>
              </template>
              <template v-else>
                <span class="pending-badge-inline">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="10"></circle>
                    <polyline points="12 6 12 12 16 14"></polyline>
                  </svg>
                  等待批阅
                </span>
              </template>
            </div>

            <!-- 教师评语 -->
            <div v-if="q.notes" class="teacher-notes">
              <div class="notes-header">教师评语</div>
              <div class="notes-content">{{ q.notes }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.score-detail-view {
  padding: 24px;
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--line-text);
  margin: 0;
}

.outline-btn {
  background: transparent;
  border: 1px solid var(--line-border);
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  color: var(--line-text);
  transition: all 0.2s;
}

.outline-btn:hover {
  border-color: var(--line-primary);
  color: var(--line-primary);
}

.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  background: var(--line-bg);
  border-radius: 12px;
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
  to { transform: rotate(360deg); }
}

.primary-btn {
  background: var(--line-primary);
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  margin-top: 16px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.overview-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
}

.overview-left {
  flex: 1;
}

.paper-title {
  font-size: 20px;
  font-weight: 500;
  color: var(--line-text);
  margin: 0 0 12px 0;
}

.meta-info {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  font-size: 13px;
  color: var(--line-text-secondary);
}

.score-circle {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: #f8f9fa;
  border: 4px solid;
}

.score-circle.excellent { border-color: #34a853; }
.score-circle.good { border-color: #1a73e8; }
.score-circle.pass { border-color: #fbbc04; }
.score-circle.fail { border-color: #ea4335; }

.score-number {
  font-size: 32px;
  font-weight: 500;
  line-height: 1;
}

.score-circle.excellent .score-number { color: #34a853; }
.score-circle.good .score-number { color: #1a73e8; }
.score-circle.pass .score-number { color: #fbbc04; }
.score-circle.fail .score-number { color: #ea4335; }

.score-label {
  font-size: 14px;
  color: var(--line-text-secondary);
}

.pending-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--line-text-secondary);
  font-size: 14px;
  background: #fef7e0;
  padding: 16px 24px;
  border-radius: 8px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stat-item {
  background: var(--line-bg);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  border: 1px solid var(--line-border);
}

.stat-label {
  display: block;
  font-size: 13px;
  color: var(--line-text-secondary);
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 500;
  color: var(--line-text);
}

.questions-section {
  background: var(--line-bg);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  border: 1px solid var(--line-border);
}

.section-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--line-text);
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--line-border);
}

.question-card {
  background: var(--line-bg-soft, #f8f9fa);
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  border-left: 4px solid var(--line-border);
}

.question-card.correct {
  border-left-color: #34a853;
}

.question-card.incorrect {
  border-left-color: #ea4335;
}

.question-card.pending {
  border-left-color: #fbbc04;
}

.question-card:last-child {
  margin-bottom: 0;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.q-number {
  font-weight: 500;
  color: var(--line-text);
}

.q-type {
  background: var(--line-bg);
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  color: var(--line-text-secondary);
}

.question-score {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.got-score {
  font-size: 20px;
  font-weight: 500;
  color: var(--line-text);
}

.got-score.full {
  color: #34a853;
}

.max-score {
  font-size: 14px;
  color: var(--line-text-secondary);
}

.pending-score {
  font-size: 14px;
  color: #f9ab00;
}

.question-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-stem {
  font-size: 15px;
  line-height: 1.6;
  color: var(--line-text);
}

.answer-section {
  background: var(--line-bg);
  padding: 12px 16px;
  border-radius: 8px;
}

.answer-row {
  display: flex;
  gap: 8px;
}

.answer-label {
  font-size: 13px;
  color: var(--line-text-secondary);
  white-space: nowrap;
}

.answer-content {
  font-size: 14px;
  color: var(--line-text);
  word-break: break-word;
}

.result-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.correct-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 16px;
  background: #e6f4ea;
  color: #34a853;
  font-size: 13px;
  font-weight: 500;
}

.incorrect-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 16px;
  background: #fce8e6;
  color: #ea4335;
  font-size: 13px;
  font-weight: 500;
}

.pending-badge-inline {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 16px;
  background: #fef7e0;
  color: #f9ab00;
  font-size: 13px;
  font-weight: 500;
}

.teacher-notes {
  background: #fffbf0;
  border: 1px solid #ffeeba;
  border-radius: 8px;
  padding: 12px 16px;
  margin-top: 8px;
}

.notes-header {
  font-size: 13px;
  font-weight: 500;
  color: #856404;
  margin-bottom: 8px;
}

.notes-content {
  font-size: 14px;
  color: #856404;
  line-height: 1.5;
}

@media (max-width: 600px) {
  .overview-card {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }

  .stats-row {
    grid-template-columns: 1fr;
  }
  
  .page-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .breadcrumb {
    font-size: 13px;
  }
}
</style>
