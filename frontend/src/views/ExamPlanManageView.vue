<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface ExamPlan {
  id: string
  name: string
  paperId: number | null
  examType: 'FORMAL' | 'MAKEUP' | 'RETAKE'
  status: 'DRAFT' | 'PUBLISHED' | 'ONGOING' | 'FINISHED' | 'CANCELLED'
  startTime: string
  endTime: string
  durationMins: number
  passScore: number
  createdBy: string
  createdAt: string
}

interface Paper {
  id: number
  title: string
}

const examPlans = ref<ExamPlan[]>([])
const papers = ref<Paper[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(10)
const total = ref(0)

const showForm = ref(false)
const editingPlan = ref<ExamPlan | null>(null)

const form = ref({
  name: '',
  paperId: null as number | null,
  examType: 'FORMAL' as 'FORMAL' | 'MAKEUP' | 'RETAKE',
  startTime: '',
  endTime: '',
  durationMins: 120,
  passScore: 60,
  maxAttempts: 1
})

const getAuthHeaders = () => ({
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${localStorage.getItem('token')}`
})

onMounted(() => {
  fetchExamPlans()
  fetchPapers()
})

async function fetchPapers() {
  try {
    const response = await fetch('/api/papers', {
      headers: getAuthHeaders()
    })
    const data = await response.json()
    papers.value = data || []
  } catch (error) {
    console.error('Failed to fetch papers:', error)
  }
}

async function fetchExamPlans() {
  loading.value = true
  try {
    const response = await fetch(`/api/exam-plans?page=${page.value}&size=${size.value}`, {
      headers: getAuthHeaders()
    })
    const data = await response.json()
    examPlans.value = data.content || []
    total.value = data.totalElements || 0
  } catch (error) {
    console.error('Failed to fetch exam plans:', error)
  } finally {
    loading.value = false
  }
}

function openAddForm() {
  editingPlan.value = null
  form.value = {
    name: '',
    paperId: null,
    examType: 'FORMAL',
    startTime: '',
    endTime: '',
    durationMins: 120,
    passScore: 60,
    maxAttempts: 1
  }
  showForm.value = true
}

function openEditForm(plan: ExamPlan) {
  editingPlan.value = plan
  form.value = {
    name: plan.name,
    paperId: plan.paperId,
    examType: plan.examType,
    startTime: plan.startTime ? formatDateTimeLocal(plan.startTime) : '',
    endTime: plan.endTime ? formatDateTimeLocal(plan.endTime) : '',
    durationMins: plan.durationMins,
    passScore: plan.passScore,
    maxAttempts: 1
  }
  showForm.value = true
}

// 格式化为datetime-local输入框需要的格式
function formatDateTimeLocal(isoString: string): string {
  if (!isoString) return ''
  const date = new Date(isoString)
  return date.toISOString().slice(0, 16)
}

// 将datetime-local格式转换为ISO 8601格式(带时区)
function toISODateTime(localDateTime: string): string {
  if (!localDateTime) return ''
  const date = new Date(localDateTime)
  return date.toISOString()
}

// 获取试卷名称
function getPaperName(paperId: number | null): string {
  if (!paperId) return '未选择'
  const paper = papers.value.find(p => p.id === paperId)
  return paper ? paper.title : `试卷#${paperId}`
}

async function submitForm() {
  try {
    const url = editingPlan.value ? `/api/exam-plans/${editingPlan.value.id}` : '/api/exam-plans'
    const method = editingPlan.value ? 'PUT' : 'POST'

    // 构造请求体，转换时间格式
    const requestBody = {
      ...form.value,
      startTime: toISODateTime(form.value.startTime),
      endTime: toISODateTime(form.value.endTime)
    }

    const response = await fetch(url, {
      method,
      headers: getAuthHeaders(),
      body: JSON.stringify(requestBody)
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(errorText || `请求失败: ${response.status}`)
    }

    showForm.value = false
    await fetchExamPlans()
    alert(editingPlan.value ? '考试计划更新成功' : '考试计划创建成功')
  } catch (error) {
    console.error('Failed to save exam plan:', error)
    alert('保存失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

async function publishPlan(id: string) {
  const plan = examPlans.value.find(p => p.id === id)
  if (!plan?.paperId) {
    alert('请先选择试卷后再发布')
    return
  }
  if (!confirm('确定要发布该考试计划吗？发布后学生可以在考试时间内答题。')) return

  try {
    const response = await fetch(`/api/exam-plans/${id}/publish`, { 
      method: 'POST',
      headers: getAuthHeaders()
    })
    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(errorText || `发布失败: ${response.status}`)
    }
    await fetchExamPlans()
    alert('考试计划发布成功')
  } catch (error) {
    console.error('Failed to publish plan:', error)
    alert('发布失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

async function cancelPlan(id: string) {
  if (!confirm('确定要取消该考试计划吗？')) return

  try {
    await fetch(`/api/exam-plans/${id}/cancel`, { 
      method: 'POST',
      headers: getAuthHeaders()
    })
    fetchExamPlans()
  } catch (error) {
    console.error('Failed to cancel plan:', error)
  }
}

function goToPage(newPage: number) {
  page.value = newPage
  fetchExamPlans()
}

const examTypeLabels: Record<string, string> = {
  FORMAL: '正式考试',
  MAKEUP: '补考',
  RETAKE: '重考'
}

const statusLabels: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ONGOING: '进行中',
  FINISHED: '已完成',
  CANCELLED: '已取消'
}
</script>

<template>
  <div class="exam-plan-view">
    <div class="page-header">
      <h1>考试计划管理</h1>
      <p class="subtitle">创建和管理考试计划、报名和排考</p>
    </div>

    <div class="toolbar">
      <button class="google-btn primary-btn" @click="openAddForm">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
        创建考试计划
      </button>
    </div>

    <div class="content-card">
      <div v-if="loading" class="loading">加载中...</div>

      <div v-else-if="examPlans.length === 0" class="empty-state">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#9aa0a6" stroke-width="1">
          <path d="M8 3H3c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2v-5"/>
          <path d="M21 5H7M21 9H7M21 13H7"/>
        </svg>
        <p>暂无考试计划</p>
      </div>

      <table v-else class="plan-table">
        <thead>
          <tr>
            <th>计划名称</th>
            <th>关联试卷</th>
            <th>考试类型</th>
            <th>开始时间</th>
            <th>持续时长</th>
            <th>及格分数</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="plan in examPlans" :key="plan.id">
            <td>{{ plan.name }}</td>
            <td>{{ getPaperName(plan.paperId) }}</td>
            <td>{{ examTypeLabels[plan.examType] || plan.examType }}</td>
            <td>{{ plan.startTime ? new Date(plan.startTime).toLocaleString('zh-CN') : '-' }}</td>
            <td>{{ plan.durationMins }} 分钟</td>
            <td>{{ plan.passScore }} 分</td>
            <td>
              <span :class="['status-badge', (plan.status || 'draft').toLowerCase()]">
                {{ statusLabels[plan.status] || plan.status }}
              </span>
            </td>
            <td>
              <div class="action-buttons">
                <button class="icon-btn" @click="openEditForm(plan)" title="编辑" :disabled="plan.status !== 'DRAFT'">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                </button>
                <button 
                  v-if="plan.status === 'DRAFT'"
                  class="icon-btn success"
                  @click="publishPlan(plan.id)"
                  title="发布"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                </button>
                <button 
                  v-if="plan.status === 'PUBLISHED' || plan.status === 'ONGOING'"
                  class="icon-btn danger"
                  @click="cancelPlan(plan.id)"
                  title="取消"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="total > size" class="pagination">
        <button 
          :disabled="page === 0"
          class="icon-btn"
          @click="goToPage(page - 1)"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
        </button>
        <span class="page-info">第 {{ page + 1 }} 页，共 {{ Math.ceil(total / size) }} 页</span>
        <button 
          :disabled="page >= Math.ceil(total / size) - 1"
          class="icon-btn"
          @click="goToPage(page + 1)"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </button>
      </div>
    </div>

    <!-- Form Modal -->
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal-content">
        <h2>{{ editingPlan ? '编辑考试计划' : '创建考试计划' }}</h2>
        <form @submit.prevent="submitForm">
          <div class="form-group">
            <label>计划名称</label>
            <input v-model="form.name" type="text" class="google-input" required placeholder="例如：期末考试" />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>关联试卷 <span class="required">*</span></label>
              <select v-model="form.paperId" class="google-input" required>
                <option :value="null" disabled>请选择试卷</option>
                <option v-for="paper in papers" :key="paper.id" :value="paper.id">
                  {{ paper.title }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>考试类型</label>
              <select v-model="form.examType" class="google-input">
                <option value="FORMAL">正式考试</option>
                <option value="MAKEUP">补考</option>
                <option value="RETAKE">重考</option>
              </select>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>开始时间 <span class="required">*</span></label>
              <input v-model="form.startTime" type="datetime-local" class="google-input" required />
              <small class="hint">学生只能在开始时间后答题</small>
            </div>
            <div class="form-group">
              <label>结束时间 <span class="required">*</span></label>
              <input v-model="form.endTime" type="datetime-local" class="google-input" required />
              <small class="hint">结束后自动提交并判分</small>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>考试时长（分钟）</label>
              <input v-model.number="form.durationMins" type="number" class="google-input" required min="1" />
            </div>
            <div class="form-group">
              <label>及格分数</label>
              <input v-model.number="form.passScore" type="number" class="google-input" required min="0" />
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="google-btn text-btn" @click="showForm = false">取消</button>
            <button type="submit" class="google-btn primary-btn">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.exam-plan-view {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 400;
  color: #202124;
  margin: 0 0 8px 0;
}

.subtitle {
  color: #5f6368;
  margin: 0;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.content-card {
  background: #fff;
  border: 1px solid #dadce0;
  border-radius: 8px;
  padding: 24px;
}

.loading, .empty-state {
  text-align: center;
  padding: 48px;
  color: #5f6368;
}

.empty-state svg {
  margin-bottom: 16px;
}

.plan-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.plan-table thead {
  background: #f8f9fa;
  border-bottom: 1px solid #dadce0;
}

.plan-table th {
  padding: 12px 16px;
  text-align: left;
  font-weight: 500;
  color: #5f6368;
}

.plan-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #dadce0;
}

.status-badge {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 12px;
}

.status-badge.draft {
  background: #f1f3f4;
  color: #5f6368;
}

.status-badge.published,
.status-badge.ongoing {
  background: #e6f4ea;
  color: #1e8e3e;
}

.status-badge.finished {
  background: #e8f0fe;
  color: #1a73e8;
}

.status-badge.cancelled {
  background: #fce8e6;
  color: #d93025;
}

.action-buttons {
  display: flex;
  gap: 4px;
}

.icon-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5f6368;
  transition: background 0.2s;
}

.icon-btn:hover {
  background: rgba(0,0,0,0.08);
}

.icon-btn.success:hover {
  background: #e6f4ea;
  color: #1e8e3e;
}

.icon-btn.danger:hover {
  background: #fce8e6;
  color: #d93025;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #dadce0;
}

.page-info {
  color: #5f6368;
  font-size: 14px;
}

.icon-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  width: 100%;
  max-width: 600px;
}

.modal-content h2 {
  margin: 0 0 24px 0;
  font-size: 20px;
  font-weight: 500;
}

.form-group {
  margin-bottom: 16px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #5f6368;
  font-weight: 500;
}

.google-input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.google-input:focus {
  outline: none;
  border-color: #1a73e8;
  box-shadow: 0 0 0 2px rgba(26,115,232,0.2);
}

.form-group .hint {
  display: block;
  font-size: 12px;
  color: #5f6368;
  margin-top: 4px;
}

.form-group .required {
  color: #d93025;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.google-btn {
  padding: 8px 24px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.primary-btn {
  background: #1a73e8;
  color: #fff;
}

.primary-btn:hover {
  background: #1557b0;
}

.text-btn {
  background: transparent;
  color: #1a73e8;
}

.text-btn:hover {
  background: #e8f0fe;
}

.icon-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
