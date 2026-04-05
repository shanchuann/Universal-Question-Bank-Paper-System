<template>
  <div class="analytics-view container mt-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2>考试统计分析报告</h2>
      <button class="btn btn-secondary" @click="$router.back()">返回</button>
    </div>

    <div v-if="loading" class="text-center my-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">加载中...</span>
      </div>
      <p class="mt-2">正在加载分析数据...</p>
    </div>

    <div v-else-if="error" class="alert alert-danger">
      {{ error }}
    </div>

    <div v-else-if="analyticsData">
      <div class="insight-bar mb-4">
        <div class="insight-item">
          <span class="insight-label">最高错误率题目</span>
          <span class="insight-value">{{ topErrorQuestionTitle }}</span>
        </div>
        <div class="insight-item">
          <span class="insight-label">最薄弱知识点</span>
          <span class="insight-value">{{ weakestKnowledgePoint }}</span>
        </div>
      </div>

      <!-- Summary Cards -->
      <div class="row mb-4">
        <div class="col-md-3">
          <div class="card text-center h-100 border-primary">
            <div class="card-body">
              <h5 class="card-title text-muted">平均分</h5>
              <h2 class="display-4 text-primary">{{ formatNumber(analyticsData.averageScore) }}</h2>
            </div>
          </div>
        </div>
        <div class="col-md-3">
          <div class="card text-center h-100 border-success">
            <div class="card-body">
              <h5 class="card-title text-muted">及格率</h5>
              <h2 class="display-4 text-success">{{ formatPercent(analyticsData.passRate) }}</h2>
            </div>
          </div>
        </div>
        <div class="col-md-3">
          <div class="card text-center h-100 border-info">
            <div class="card-body">
              <h5 class="card-title text-muted">最高分</h5>
              <h2 class="display-4 text-info">{{ formatNumber(analyticsData.highestScore) }}</h2>
            </div>
          </div>
        </div>
        <div class="col-md-3">
          <div class="card text-center h-100 border-secondary">
            <div class="card-body">
              <h5 class="card-title text-muted">参考人数</h5>
              <!-- Assuming we can infer this or add it later, for now maybe just show something else or omit -->
              <h2 class="display-4 text-secondary">-</h2>
            </div>
          </div>
        </div>
      </div>

      <!-- Charts Row -->
      <div class="row mb-4">
        <!-- Score Distribution -->
        <div class="col-md-6">
          <div class="card h-100">
            <div class="card-header">成绩分布</div>
            <div class="card-body">
              <Bar
                v-if="scoreDistributionData"
                :data="scoreDistributionData"
                :options="barOptions"
              />
            </div>
          </div>
        </div>
        <!-- Knowledge Mastery -->
        <div class="col-md-6">
          <div class="card h-100">
            <div class="card-header">知识点掌握情况</div>
            <div class="card-body">
              <Radar
                v-if="knowledgeMasteryData"
                :data="knowledgeMasteryData"
                :options="radarOptions"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Error Rates Table -->
      <div class="card mb-4">
        <div class="card-header">高频错题 (Top 10)</div>
        <div class="card-body">
          <div class="table-responsive">
            <table class="table table-striped table-hover">
              <thead>
                <tr>
                  <th>题目名称</th>
                  <th>错误率</th>
                  <th>错误次数</th>
                  <th>总尝试次数</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in analyticsData.errorRates" :key="item.questionId">
                  <td>
                    <span
                      class="question-name-link"
                      @click="showQuestionDetail(item.questionId)"
                      :title="getQuestionStem(item.questionId)"
                    >
                      {{ truncateText(getQuestionStem(item.questionId), 10) }}
                    </span>
                  </td>
                  <td>
                    <div class="d-flex align-items-center">
                      <div class="progress flex-grow-1 me-2" style="height: 10px">
                        <div
                          class="progress-bar bg-danger"
                          role="progressbar"
                          :style="{ width: (item.errorRate ?? 0) * 100 + '%' }"
                          :aria-valuenow="(item.errorRate ?? 0) * 100"
                          aria-valuemin="0"
                          aria-valuemax="100"
                        ></div>
                      </div>
                      <span>{{ formatPercent(item.errorRate) }}</span>
                    </div>
                  </td>
                  <td>{{ item.errorCount ?? item.incorrect }}</td>
                  <td>{{ item.totalAttempts ?? item.attempts }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- Question Detail Modal -->
    <div v-if="showDetailModal" class="modal-overlay" @click.self="closeDetailModal">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">题目详情</h5>
          <button type="button" class="close-btn" @click="closeDetailModal">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        <div class="modal-body" v-if="selectedQuestion">
          <div class="detail-section">
            <label>题干：</label>
            <div class="detail-value stem-content" v-html="renderHtml(selectedQuestion.stem)"></div>
          </div>
          <div class="detail-row">
            <div class="detail-section">
              <label>题型：</label>
              <span class="detail-value">{{ getTypeLabel(selectedQuestion.type) }}</span>
            </div>
            <div class="detail-section">
              <label>难度：</label>
              <span class="detail-value" :class="getDifficultyClass(selectedQuestion.difficulty)">
                {{ getDifficultyLabel(selectedQuestion.difficulty) }}
              </span>
            </div>
          </div>
          <div
            class="detail-section"
            v-if="selectedQuestion.options && selectedQuestion.options.length > 0"
          >
            <label>选项：</label>
            <div class="options-list">
              <div v-for="(opt, idx) in selectedQuestion.options" :key="idx" class="option-item">
                <span class="option-label">{{ String.fromCharCode(65 + idx) }}.</span>
                <span class="option-content" v-html="renderHtml(opt.text || '')"></span>
              </div>
            </div>
          </div>
          <div class="detail-section" v-if="selectedQuestion.answerSchema">
            <label>答案：</label>
            <div class="detail-value answer-content">
              {{ formatAnswer(selectedQuestion.answerSchema) }}
            </div>
          </div>
          <div class="detail-section" v-if="selectedQuestion.analysis">
            <label>解析：</label>
            <div
              class="detail-value analysis-content"
              v-html="renderHtml(selectedQuestion.analysis)"
            ></div>
          </div>
        </div>
        <div class="modal-body" v-else>
          <div class="text-center py-4">
            <div class="spinner-border spinner-border-sm text-primary" role="status">
              <span class="visually-hidden">加载中...</span>
            </div>
            <span class="ms-2">加载中...</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { analyticsApi, questionApi } from '@/api/client'
import type { ExamAnalyticsResponse, QuestionResponse } from '@/api/generated'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale,
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler
} from 'chart.js'
import { Bar, Radar } from 'vue-chartjs'

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Title,
  Tooltip,
  Legend
)

const route = useRoute()
const paperId = route.params.paperId as string

const loading = ref(true)
const error = ref('')
const analyticsData = ref<ExamAnalyticsResponse | null>(null)
const questionDetailsMap = ref<Map<string, QuestionResponse>>(new Map())
const showDetailModal = ref(false)
const selectedQuestion = ref<QuestionResponse | null>(null)

const formatNumber = (num?: number) => {
  return num !== undefined ? num.toFixed(1) : '-'
}

const formatPercent = (num?: number) => {
  return num !== undefined ? (num * 100).toFixed(1) + '%' : '-'
}

// Truncate text to specified length
const truncateText = (text: string | undefined, maxLength: number) => {
  if (!text) return '加载中...'
  // Remove HTML tags for display
  const plainText = text.replace(/<[^>]*>/g, '')
  if (plainText.length <= maxLength) return plainText
  return plainText.substring(0, maxLength) + '...'
}

// Get question stem from map
const getQuestionStem = (questionId?: string) => {
  if (!questionId) return '未知题目'
  const question = questionDetailsMap.value.get(questionId)
  return question?.stem || '加载中...'
}

// Type label mapping
const typeLabels: Record<string, string> = {
  SINGLE_CHOICE: '单选题',
  MULTI_CHOICE: '多选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题'
}

const getTypeLabel = (type?: string) => {
  return type ? typeLabels[type] || type : '未知'
}

// Difficulty label mapping
const difficultyLabels: Record<string, string> = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难'
}

const getDifficultyLabel = (difficulty?: string) => {
  return difficulty ? difficultyLabels[difficulty] || difficulty : '未知'
}

const getDifficultyClass = (difficulty?: string) => {
  switch (difficulty) {
    case 'EASY':
      return 'difficulty-easy'
    case 'MEDIUM':
      return 'difficulty-medium'
    case 'HARD':
      return 'difficulty-hard'
    default:
      return ''
  }
}

// Format answer for display
const formatAnswer = (answerSchema: any) => {
  if (!answerSchema) return '无'
  if (typeof answerSchema === 'string') return stripHtml(answerSchema)
  if (answerSchema.correctAnswer) return stripHtml(answerSchema.correctAnswer)
  if (answerSchema.correctAnswers)
    return answerSchema.correctAnswers.map((v: string) => stripHtml(v)).join(', ')
  if (Array.isArray(answerSchema)) return answerSchema.map((v: string) => stripHtml(v)).join(', ')
  return stripHtml(JSON.stringify(answerSchema))
}

const decodeEntities = (raw: string) => {
  const txt = document.createElement('textarea')
  txt.innerHTML = raw
  return txt.value
}

const renderHtml = (raw?: string) => {
  if (!raw) return ''
  const decoded = decodeEntities(raw)
  return decoded
    .replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
    .replace(/<style[^>]*>[\s\S]*?<\/style>/gi, '')
}

const stripHtml = (raw?: string) => {
  if (!raw) return ''
  return decodeEntities(raw)
    .replace(/<[^>]*>/g, '')
    .trim()
}

// Show question detail modal
const showQuestionDetail = async (questionId?: string) => {
  if (!questionId) return

  showDetailModal.value = true
  selectedQuestion.value = questionDetailsMap.value.get(questionId) || null

  // If not loaded yet, fetch it
  if (!selectedQuestion.value) {
    try {
      const response = await questionApi.apiQuestionsQuestionIdGet(questionId)
      selectedQuestion.value = response.data
      questionDetailsMap.value.set(questionId, response.data)
    } catch (err) {
      console.error('Failed to fetch question details:', err)
    }
  }
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedQuestion.value = null
}

const topErrorQuestionTitle = computed(() => {
  if (!analyticsData.value?.errorRates?.length) return '暂无数据'
  const top = analyticsData.value.errorRates[0]
  if (!top?.questionId) return '暂无数据'
  return truncateText(getQuestionStem(top.questionId), 16)
})

const weakestKnowledgePoint = computed(() => {
  if (!analyticsData.value?.knowledgeMastery?.length) return '暂无数据'
  const sorted = [...analyticsData.value.knowledgeMastery].sort(
    (a, b) => (a.masteryRate || 0) - (b.masteryRate || 0)
  )
  const point = sorted[0]
  if (!point?.knowledgePointId) return '暂无数据'
  return `${point.knowledgePointId} (${formatPercent(point.masteryRate)})`
})

// Fetch question details for error rate items
const fetchQuestionDetails = async (questionIds: string[]) => {
  const promises = questionIds.map(async (id) => {
    try {
      const response = await questionApi.apiQuestionsQuestionIdGet(id)
      questionDetailsMap.value.set(id, response.data)
    } catch (err) {
      console.error(`Failed to fetch question ${id}:`, err)
    }
  })
  await Promise.all(promises)
}

const fetchData = async () => {
  try {
    loading.value = true
    const response = await analyticsApi.apiAnalyticsExamsPaperVersionIdSummaryGet(paperId)
    analyticsData.value = response.data

    // Fetch question details for error rates
    if (response.data.errorRates && response.data.errorRates.length > 0) {
      const questionIds = response.data.errorRates
        .map((item) => item.questionId)
        .filter((id): id is string => !!id)
      await fetchQuestionDetails(questionIds)
    }
  } catch (err: any) {
    console.error('Failed to fetch analytics:', err)
    error.value = '获取分析数据失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

// Chart Data: Score Distribution
const scoreDistributionData = computed(() => {
  if (!analyticsData.value?.scoreDistribution) return null

  const labels = analyticsData.value.scoreDistribution.map((b) => b.rangeLabel || b.range || '')
  const data = analyticsData.value.scoreDistribution.map((b) => b.count ?? b.percentage ?? 0)

  return {
    labels,
    datasets: [
      {
        label: '人数',
        backgroundColor: '#3b82f6',
        data
      }
    ]
  }
})

const barOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: false
    }
  }
}

// Chart Data: Knowledge Mastery
const knowledgeMasteryData = computed(() => {
  if (!analyticsData.value?.knowledgeMastery) return null

  const labels = analyticsData.value.knowledgeMastery.map((k) => k.knowledgePointId) // Ideally map to name if available
  const data = analyticsData.value.knowledgeMastery.map((k) => (k.masteryRate || 0) * 100)

  return {
    labels,
    datasets: [
      {
        label: '掌握率 (%)',
        backgroundColor: 'rgba(16, 185, 129, 0.2)',
        borderColor: '#10b981',
        pointBackgroundColor: '#10b981',
        pointBorderColor: '#fff',
        pointHoverBackgroundColor: '#fff',
        pointHoverBorderColor: '#10b981',
        data
      }
    ]
  }
})

const radarOptions = {
  responsive: true,
  maintainAspectRatio: false,
  scales: {
    r: {
      angleLines: {
        display: false
      },
      suggestedMin: 0,
      suggestedMax: 100
    }
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.card {
  box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
}

.insight-bar {
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 1fr));
  gap: 12px;
}

.insight-item {
  border: 1px solid var(--line-border);
  border-radius: 10px;
  padding: 12px 14px;
  background: var(--line-bg-soft);
}

.insight-label {
  display: block;
  font-size: 12px;
  color: var(--line-text-secondary);
  margin-bottom: 4px;
}

.insight-value {
  font-size: 14px;
  color: var(--line-text);
  font-weight: 600;
}

.question-name-link {
  color: var(--line-primary);
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s;
}

.question-name-link:hover {
  color: #1557b0;
  text-decoration: underline;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
  animation: fadeIn 0.2s ease-out;
}

.modal-content {
  background: var(--line-bg);
  border-radius: 12px;
  width: 100%;
  max-width: 700px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--line-border);
}

.modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--line-text);
}

.close-btn {
  background: none;
  border: none;
  padding: 4px;
  cursor: pointer;
  color: var(--line-text-secondary);
  border-radius: 4px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: var(--line-bg-soft);
  color: var(--line-text);
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 16px;
}

.detail-section label {
  display: block;
  font-weight: 500;
  color: var(--line-text-secondary);
  margin-bottom: 6px;
  font-size: 14px;
}

.detail-value {
  color: var(--line-text);
  font-size: 15px;
  line-height: 1.6;
}

.detail-row {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}

.detail-row .detail-section {
  margin-bottom: 0;
}

.stem-content {
  padding: 12px 16px;
  background: var(--line-bg-soft);
  border-radius: 8px;
  border-left: 3px solid var(--line-primary);
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  background: var(--line-bg-soft);
  border-radius: 6px;
}

.option-label {
  font-weight: 500;
  color: var(--line-primary);
  min-width: 24px;
}

.option-content {
  color: var(--line-text);
}

.answer-content {
  padding: 8px 12px;
  background: rgba(30, 142, 62, 0.1);
  border-radius: 6px;
  color: #1e8e3e;
  font-weight: 500;
}

.analysis-content {
  padding: 12px 16px;
  background: var(--line-bg-soft);
  border-radius: 8px;
}

.difficulty-easy {
  color: #1e8e3e;
}

.difficulty-medium {
  color: #f9ab00;
}

.difficulty-hard {
  color: #d93025;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
