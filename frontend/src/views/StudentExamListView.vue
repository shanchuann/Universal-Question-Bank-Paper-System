<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

interface ExamPlan {
  id: string
  name: string
  paperId: number
  examType: string
  status: string
  startTime: string
  endTime: string
  durationMins: number
  passScore: number
}

const router = useRouter()
const examPlans = ref<ExamPlan[]>([])
const loading = ref(false)

const getAuthHeaders = () => ({
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${localStorage.getItem('token')}`
})

onMounted(() => {
  fetchAvailableExams()
})

async function fetchAvailableExams() {
  loading.value = true
  try {
    // 获取已发布和进行中的考试
    const response = await fetch('/api/exam-plans?page=0&size=100', {
      headers: getAuthHeaders()
    })
    const data = await response.json()
    // 只显示已发布和进行中的考试
    examPlans.value = (data.content || []).filter((p: ExamPlan) => 
      p.status === 'PUBLISHED' || p.status === 'ONGOING'
    )
  } catch (error) {
    console.error('Failed to fetch exams:', error)
  } finally {
    loading.value = false
  }
}

// 计算考试状态
function getExamStatus(plan: ExamPlan) {
  const now = new Date()
  const start = new Date(plan.startTime)
  const end = new Date(plan.endTime)
  
  if (now < start) {
    return { status: 'upcoming', label: '未开始', canEnter: false }
  } else if (now > end) {
    return { status: 'ended', label: '已结束', canEnter: false }
  } else {
    return { status: 'ongoing', label: '进行中', canEnter: true }
  }
}

// 格式化时间
function formatDateTime(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 计算剩余时间
function getTimeRemaining(plan: ExamPlan) {
  const now = new Date()
  const start = new Date(plan.startTime)
  const end = new Date(plan.endTime)
  
  if (now < start) {
    const diff = start.getTime() - now.getTime()
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    if (hours > 24) {
      const days = Math.floor(hours / 24)
      return `${days}天后开始`
    }
    return `${hours}小时${minutes}分钟后开始`
  } else if (now > end) {
    return '已结束'
  } else {
    const diff = end.getTime() - now.getTime()
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    return `剩余 ${hours}小时${minutes}分钟`
  }
}

// 进入考试
function enterExam(plan: ExamPlan) {
  const status = getExamStatus(plan)
  if (!status.canEnter) {
    alert(status.status === 'upcoming' ? '考试尚未开始' : '考试已结束')
    return
  }
  // 跳转到考试页面，传递考试计划ID和试卷ID
  router.push({ 
    path: '/exam', 
    query: { 
      planId: plan.id,
      paperId: plan.paperId.toString()
    } 
  })
}

const examTypeLabels: Record<string, string> = {
  FORMAL: '正式考试',
  MAKEUP: '补考',
  RETAKE: '重考'
}
</script>

<template>
  <div class="student-exam-view">
    <div class="page-header">
      <h1>我的考试</h1>
      <p class="subtitle">查看可参加的考试，在规定时间内完成答题</p>
    </div>

    <div class="content-card">
      <div v-if="loading" class="loading">
        <div class="spinner"></div>
        <span>加载中...</span>
      </div>

      <div v-else-if="examPlans.length === 0" class="empty-state">
        <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="#9aa0a6" stroke-width="1">
          <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
          <line x1="16" y1="2" x2="16" y2="6"></line>
          <line x1="8" y1="2" x2="8" y2="6"></line>
          <line x1="3" y1="10" x2="21" y2="10"></line>
        </svg>
        <h3>暂无可参加的考试</h3>
        <p>当教师发布考试后，您将在这里看到考试安排</p>
      </div>

      <div v-else class="exam-list">
        <div 
          v-for="plan in examPlans" 
          :key="plan.id" 
          class="exam-card"
          :class="getExamStatus(plan).status"
        >
          <div class="exam-header">
            <h3 class="exam-name">{{ plan.name }}</h3>
            <span :class="['status-tag', getExamStatus(plan).status]">
              {{ getExamStatus(plan).label }}
            </span>
          </div>
          
          <div class="exam-info">
            <div class="info-row">
              <span class="label">考试类型</span>
              <span class="value">{{ examTypeLabels[plan.examType] || plan.examType }}</span>
            </div>
            <div class="info-row">
              <span class="label">开始时间</span>
              <span class="value">{{ formatDateTime(plan.startTime) }}</span>
            </div>
            <div class="info-row">
              <span class="label">结束时间</span>
              <span class="value">{{ formatDateTime(plan.endTime) }}</span>
            </div>
            <div class="info-row">
              <span class="label">考试时长</span>
              <span class="value">{{ plan.durationMins }} 分钟</span>
            </div>
            <div class="info-row">
              <span class="label">及格分数</span>
              <span class="value">{{ plan.passScore }} 分</span>
            </div>
          </div>

          <div class="exam-footer">
            <span class="time-remaining">{{ getTimeRemaining(plan) }}</span>
            <button 
              class="enter-btn"
              :class="{ disabled: !getExamStatus(plan).canEnter }"
              :disabled="!getExamStatus(plan).canEnter"
              @click="enterExam(plan)"
            >
              {{ getExamStatus(plan).canEnter ? '进入考试' : (getExamStatus(plan).status === 'upcoming' ? '等待开始' : '已结束') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.student-exam-view {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 400;
  color: #202124;
  margin: 0 0 8px 0;
}

.subtitle {
  color: #5f6368;
  margin: 0;
  font-size: 14px;
}

.content-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  padding: 24px;
  min-height: 400px;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: #5f6368;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e8eaed;
  border-top-color: #1a73e8;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-state svg {
  margin-bottom: 16px;
}

.empty-state h3 {
  font-size: 18px;
  font-weight: 500;
  color: #202124;
  margin: 0 0 8px 0;
}

.empty-state p {
  color: #5f6368;
  margin: 0;
}

.exam-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.exam-card {
  background: #fff;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s;
}

.exam-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.exam-card.ongoing {
  border-color: #34a853;
  background: linear-gradient(to bottom, #f0fdf4, #fff);
}

.exam-card.upcoming {
  border-color: #fbbc04;
  background: linear-gradient(to bottom, #fffbeb, #fff);
}

.exam-card.ended {
  border-color: #dadce0;
  background: #f8f9fa;
  opacity: 0.8;
}

.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.exam-name {
  font-size: 18px;
  font-weight: 500;
  color: #202124;
  margin: 0;
  flex: 1;
}

.status-tag {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.ongoing {
  background: #e6f4ea;
  color: #137333;
}

.status-tag.upcoming {
  background: #fef7e0;
  color: #b06000;
}

.status-tag.ended {
  background: #f1f3f4;
  color: #5f6368;
}

.exam-info {
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f1f3f4;
}

.info-row:last-child {
  border-bottom: none;
}

.info-row .label {
  color: #5f6368;
  font-size: 14px;
}

.info-row .value {
  color: #202124;
  font-size: 14px;
  font-weight: 500;
}

.exam-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #e8eaed;
}

.time-remaining {
  font-size: 13px;
  color: #5f6368;
}

.enter-btn {
  padding: 10px 24px;
  background: #1a73e8;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.enter-btn:hover:not(.disabled) {
  background: #1557b0;
}

.enter-btn.disabled {
  background: #dadce0;
  color: #80868b;
  cursor: not-allowed;
}
</style>
