<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useConfirm } from '@/composables/useConfirm'
import { useToast } from '@/composables/useToast'
import GoogleSelect from '@/components/GoogleSelect.vue'

const { confirm } = useConfirm()
const { showToast } = useToast()

interface ExamPlan {
  id: string
  name: string
  paperId: number | null
  examType: 'FORMAL' | 'MAKEUP' | 'RETAKE'
  status: 'DRAFT' | 'PUBLISHED' | 'ONGOING' | 'FINISHED' | 'CANCELLED'
  startTime: string
  endTime: string
  durationMins: number
  maxAttempts?: number
  passScore: number
  createdBy: string
  createdAt: string
}

interface Paper {
  id: number
  title: string
}

interface Organization {
  id: string
  name: string
  code: string
  type: 'SCHOOL' | 'DEPARTMENT' | 'CLASS'
}

interface EnrollmentInfo {
  id: string
  studentId: string
  studentName?: string
  avatarUrl?: string
  status: string
  statusLabel?: string
}

const examPlans = ref<ExamPlan[]>([])
const papers = ref<Paper[]>([])
const organizations = ref<Organization[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(10)
const total = ref(0)

const showForm = ref(false)
const showEnrollModal = ref(false)
const editingPlan = ref<ExamPlan | null>(null)
const enrollingPlan = ref<ExamPlan | null>(null)
const enrollments = ref<EnrollmentInfo[]>([])
const selectedClasses = ref<string[]>([])
const enrollLoading = ref(false)

const form = ref({
  name: '',
  paperId: null as number | null,
  examType: 'FORMAL' as 'FORMAL' | 'MAKEUP' | 'RETAKE',
  startTime: '',
  endTime: '',
  durationMins: 120,
  passScore: 60,
  maxAttempts: 1,
  classIds: [] as string[]
})

const paperOptions = computed(() => {
  return [
    { label: '请选择试卷', value: '' },
    ...papers.value.map(paper => ({ label: paper.title, value: String(paper.id) }))
  ]
})

const examTypeOptions = [
  { label: '正式考试', value: 'FORMAL' },
  { label: '补考', value: 'MAKEUP' },
  { label: '重考', value: 'RETAKE' }
]

const paperSelectValue = computed({
  get: () => (form.value.paperId == null ? '' : String(form.value.paperId)),
  set: (value: string | number) => {
    const selected = String(value)
    form.value.paperId = selected ? Number(selected) : null
  }
})

const examTypeSelectValue = computed({
  get: () => form.value.examType,
  set: (value: string | number) => {
    form.value.examType = String(value) as 'FORMAL' | 'MAKEUP' | 'RETAKE'
  }
})

const getAuthHeaders = () => ({
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${localStorage.getItem('token')}`
})

onMounted(() => {
  fetchExamPlans()
  fetchPapers()
  fetchOrganizations()
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

async function fetchOrganizations() {
  try {
    // 直接获取所有班级类型的组织
    const response = await fetch('/api/organizations/by-type/CLASS', {
      headers: getAuthHeaders()
    })
    const data = await response.json()
    organizations.value = data || []
  } catch (error) {
    console.error('Failed to fetch organizations:', error)
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
    maxAttempts: 1,
    classIds: []
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
    maxAttempts: plan.maxAttempts ?? 1,
    classIds: []
  }
  // 获取已关联的班级
  fetchPlanClasses(plan.id)
  showForm.value = true
}

// 获取考试计划已关联的班级
async function fetchPlanClasses(planId: string) {
  try {
    const response = await fetch(`/api/exam-plans/${planId}`, {
      headers: getAuthHeaders()
    })
    const data = await response.json()
    form.value.classIds = data.classIds || []
  } catch (error) {
    console.error('Failed to fetch plan classes:', error)
  }
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
    if (!form.value.paperId) {
      showToast({ message: '请选择关联试卷', type: 'warning' })
      return
    }

    const url = editingPlan.value ? `/api/exam-plans/${editingPlan.value.id}` : '/api/exam-plans'
    const method = editingPlan.value ? 'PUT' : 'POST'

    // 构造请求体，转换时间格式
    const requestBody = {
      ...form.value,
      startTime: toISODateTime(form.value.startTime),
      endTime: toISODateTime(form.value.endTime),
      classIds: form.value.classIds
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
    showToast({ message: editingPlan.value ? '考试计划更新成功' : '考试计划创建成功', type: 'success' })
  } catch (error) {
    console.error('Failed to save exam plan:', error)
    showToast({ message: '保存失败: ' + (error instanceof Error ? error.message : '未知错误'), type: 'error' })
  }
}

async function publishPlan(id: string) {
  const plan = examPlans.value.find(p => p.id === id)
  if (!plan?.paperId) {
    showToast({ message: '请先选择试卷后再发布', type: 'warning' })
    return
  }
  const confirmed = await confirm({
    title: '发布考试计划',
    message: '确定要发布该考试计划吗？发布后学生可以在考试时间内答题。',
    type: 'info',
    confirmText: '发布',
    cancelText: '取消'
  })
  if (!confirmed) return

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
    showToast({ message: '考试计划发布成功', type: 'success' })
  } catch (error) {
    console.error('Failed to publish plan:', error)
    showToast({ message: '发布失败: ' + (error instanceof Error ? error.message : '未知错误'), type: 'error' })
  }
}

async function cancelPlan(id: string) {
  const confirmed = await confirm({
    title: '取消考试计划',
    message: '确定要取消该考试计划吗？',
    type: 'warning',
    confirmText: '取消计划',
    cancelText: '返回'
  })
  if (!confirmed) return

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

// 打开报名管理弹窗
async function openEnrollModal(plan: ExamPlan) {
  enrollingPlan.value = plan
  selectedClasses.value = []
  enrollments.value = []
  showEnrollModal.value = true
  await fetchEnrollments(plan.id)
}

// 获取已报名学生列表
async function fetchEnrollments(planId: string) {
  enrollLoading.value = true
  try {
    const response = await fetch(`/api/exam-plans/${planId}/enrollments`, {
      headers: getAuthHeaders()
    })
    const data = await response.json()
    enrollments.value = data || []
  } catch (error) {
    console.error('Failed to fetch enrollments:', error)
  } finally {
    enrollLoading.value = false
  }
}

// 按班级批量报名
async function enrollClasses() {
  if (!enrollingPlan.value || selectedClasses.value.length === 0) {
    showToast({ message: '请选择要报名的班级', type: 'warning' })
    return
  }

  enrollLoading.value = true
  try {
    const response = await fetch(`/api/exam-plans/${enrollingPlan.value.id}/enroll-classes`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ classIds: selectedClasses.value })
    })
    const result = await response.json()
    showToast({ message: `成功报名 ${result.enrolled} 名学生`, type: 'success' })
    selectedClasses.value = []
    await fetchEnrollments(enrollingPlan.value.id)
  } catch (error) {
    console.error('Failed to enroll classes:', error)
    showToast({ message: '报名失败', type: 'error' })
  } finally {
    enrollLoading.value = false
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
      <h1 class="page-title">考试计划管理</h1>
      <p class="subtitle">创建和管理考试计划、报名和排考</p>
    </div>

    <div class="toolbar">
      <button class="google-btn primary-btn" @click="openAddForm">
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
            <th>可进入次数</th>
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
            <td>{{ plan.maxAttempts ?? 1 }} 次</td>
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
                  class="icon-btn primary"
                  @click="openEnrollModal(plan)"
                  title="报名管理"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 00-3-3.87"/>
                    <path d="M16 3.13a4 4 0 010 7.75"/>
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

    <Teleport to="body">
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
              <GoogleSelect
                v-model="paperSelectValue"
                :options="paperOptions"
                placeholder="请选择试卷"
              />
            </div>
            <div class="form-group">
              <label>考试类型</label>
              <GoogleSelect
                v-model="examTypeSelectValue"
                :options="examTypeOptions"
                placeholder="请选择考试类型"
              />
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

          <div class="form-row">
            <div class="form-group">
              <label>允许进入答题次数</label>
              <input v-model.number="form.maxAttempts" type="number" class="google-input" required min="1" />
              <small class="hint">达到次数上限后不可再次进入考试</small>
            </div>
            <div class="form-group"></div>
          </div>

          <div class="form-group">
            <label>下发班级</label>
            <div class="class-select-box">
              <div v-if="organizations.length === 0" class="empty-hint-inline">
                暂无班级，请先创建班级
              </div>
              <div v-else class="class-checkboxes-form">
                <label v-for="org in organizations" :key="org.id" class="checkbox-label-form">
                  <input 
                    type="checkbox" 
                    :value="org.id" 
                    v-model="form.classIds"
                  />
                  {{ org.name }}
                </label>
              </div>
            </div>
            <small class="hint">选择班级后，发布考试时自动将班级学生报名到考试</small>
          </div>

          <div class="form-actions">
            <button type="button" class="google-btn text-btn" @click="showForm = false">取消</button>
            <button type="submit" class="google-btn primary-btn">保存</button>
          </div>
        </form>
        </div>
      </div>

      <!-- Enroll Modal -->
      <div v-if="showEnrollModal" class="modal-overlay" @click.self="showEnrollModal = false">
        <div class="modal-content enroll-modal">
        <h2>报名管理 - {{ enrollingPlan?.name }}</h2>
        
        <div class="enroll-section">
          <h3>按班级批量报名</h3>
          <div class="class-select-group">
            <div v-if="organizations.length === 0" class="empty-hint">
              暂无班级，请先在组织管理中创建班级
            </div>
            <div v-else class="class-checkboxes">
              <label v-for="org in organizations" :key="org.id" class="checkbox-label">
                <input 
                  type="checkbox" 
                  :value="org.id" 
                  v-model="selectedClasses"
                />
                {{ org.name }} ({{ org.code }})
              </label>
            </div>
            <button 
              v-if="organizations.length > 0"
              class="google-btn primary-btn"
              :disabled="selectedClasses.length === 0 || enrollLoading"
              @click="enrollClasses"
            >
              {{ enrollLoading ? '报名中...' : '批量报名' }}
            </button>
          </div>
        </div>

        <div class="enroll-section">
          <h3>已报名学生 ({{ enrollments.length }}人)</h3>
          <div v-if="enrollLoading" class="loading-hint">加载中...</div>
          <div v-else-if="enrollments.length === 0" class="empty-hint">
            暂无学生报名
          </div>
          <div v-else class="enrollment-list">
            <div v-for="e in enrollments" :key="e.id" class="enrollment-item">
              <div class="student-info">
                <div class="student-avatar" :class="{ 'has-img': e.avatarUrl }">
                  <img v-if="e.avatarUrl" :src="e.avatarUrl" :alt="e.studentName || ''" @error="($event.target as HTMLImageElement).style.display = 'none'" />
                  <span v-else>{{ (e.studentName || e.studentId || 'U').charAt(0).toUpperCase() }}</span>
                </div>
                <span class="student-name">{{ e.studentName || e.studentId }}</span>
              </div>
              <span :class="['enroll-status', e.status.toLowerCase()]">{{ e.statusLabel || e.status }}</span>
            </div>
          </div>
        </div>

          <div class="form-actions">
            <button type="button" class="google-btn text-btn" @click="showEnrollModal = false">关闭</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.exam-plan-view {
  padding: 32px;
  max-width: 1400px;
  margin: 0 auto;
  animation: fadeIn 0.5s ease-out;
}


.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  color: var(--line-text-primary);
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.subtitle {
  color: var(--line-text-secondary);
  font-size: 14px;
}

.toolbar {
  display: flex;
  gap: 16px;
  margin-bottom: 32px;
}

.content-card {
  background: var(--line-card-bg);
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-lg);
  padding: 24px;
  box-shadow: var(--line-shadow-sm);
  overflow: hidden;
}

.loading, .empty-state {
  text-align: center;
  padding: 64px;
  color: var(--line-text-secondary);
}

.empty-state svg {
  margin-bottom: 16px;
  opacity: 0.5;
}

.plan-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 14px;
}

.plan-table thead {
  background: var(--line-bg-soft);
}

.plan-table th {
  padding: 16px 20px;
  text-align: center;
  font-weight: 600;
  color: var(--line-text-primary);
  border-bottom: 1px solid var(--line-border);
  white-space: nowrap;
}

.plan-table td {
  padding: 16px 20px;
  border-bottom: 1px solid var(--line-border);
  color: var(--line-text-secondary);
  vertical-align: middle;
  text-align: center;
}

.plan-table tr:last-child td {
  border-bottom: none;
}

.plan-table tr:hover td {
  background-color: var(--line-bg-soft);
}

.status-badge {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: var(--line-radius-full);
  font-weight: 600;
  letter-spacing: 0.5px;
  display: inline-block;
}

.status-badge.draft {
  background: var(--line-bg-soft);
  color: var(--line-text-secondary);
  border: 1px solid var(--line-border);
}

.status-badge.published,
.status-badge.ongoing {
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #d1fae5;
}

.status-badge.finished {
  background: var(--line-primary-10);
  color: var(--line-primary);
  border: 1px solid var(--line-primary-border);
}

.status-badge.cancelled {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fee2e2;
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.icon-btn {
  width: 32px;
  height: 32px;
  border: 1px solid transparent;
  background: transparent;
  border-radius: var(--line-radius-md);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--line-text-secondary);
  transition: all 0.2s;
}

.icon-btn:hover {
  background: var(--line-bg-soft);
  color: var(--line-text-primary);
  border-color: var(--line-border);
}

.icon-btn.success:hover {
  background: #ecfdf5;
  color: #059669;
  border-color: #d1fae5;
}

.icon-btn.danger:hover {
  background: #fef2f2;
  color: #dc2626;
  border-color: #fee2e2;
}

.icon-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 24px;
  margin-top: 32px;
  padding-top: 32px;
  border-top: none;
}

.page-info {
  color: var(--line-text-secondary);
  font-size: 14px;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease-out;
}

.modal-content {
  background: var(--line-card-bg);
  border-radius: var(--line-radius-lg);
  padding: 32px;
  width: 100%;
  max-width: 600px;
  border: 1px solid var(--line-border);
  box-shadow: var(--line-shadow-xl);
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-content h2 {
  margin: 0 0 32px 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--line-text-primary);
  letter-spacing: -0.5px;
}

.form-group {
  margin-bottom: 24px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--line-text-primary);
  font-weight: 500;
}

.google-input {
  width: 100%;
  height: 42px;
  padding: 0 16px;
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-md);
  font-size: 14px;
  box-sizing: border-box;
  background: var(--line-bg-soft);
  color: var(--line-text-primary);
  transition: all 0.2s;
}

.google-input:focus {
  outline: none;
  border-color: var(--line-primary);
  background: var(--line-card-bg);
  box-shadow: 0 0 0 2px var(--line-primary-10);
}

.form-group .hint {
  display: block;
  font-size: 12px;
  color: var(--line-text-secondary);
  margin-top: 6px;
}

.form-group .required {
  color: #dc2626;
  margin-left: 4px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 32px;
}

.google-btn {
  padding: 10px 24px;
  border-radius: var(--line-radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
  font-family: inherit;
}

.primary-btn {
  background: var(--line-primary);
  color: #fff;
  box-shadow: 0 2px 4px rgba(14, 165, 233, 0.2);
}

.primary-btn:hover {
  background: var(--line-primary-hover);
  transform: translateY(-1px);
}

.text-btn {
  background: transparent;
  color: var(--line-text-secondary);
}

.text-btn:hover {
  background: var(--line-bg-soft);
  color: var(--line-primary);
}

/* Enroll Modal Styles */
.enroll-modal {
  max-width: 600px;
}

.enroll-section {
  margin-bottom: 24px;
}

.enroll-section h3 {
  font-size: 14px;
  font-weight: 500;
  color: var(--line-text);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--line-border);
}

.class-select-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.class-checkboxes {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  max-height: 150px;
  overflow-y: auto;
  padding: 8px;
  background: var(--line-bg-soft);
  border-radius: 8px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--line-bg);
  border: 1px solid var(--line-border);
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.checkbox-label:hover {
  border-color: var(--line-primary);
}

.checkbox-label input[type="checkbox"]:checked + span,
.checkbox-label:has(input:checked) {
  color: var(--line-primary);
}

/* 表单中的班级选择样式 */
.class-select-box {
  border: 1px solid var(--line-border);
  border-radius: 8px;
  padding: 12px;
  background: var(--line-bg-soft);
  max-height: 150px;
  overflow-y: auto;
}

.class-checkboxes-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.checkbox-label-form {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: var(--line-bg);
  border: 1px solid var(--line-border);
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.checkbox-label-form:hover {
  border-color: var(--line-primary);
}

.checkbox-label-form:has(input:checked) {
  background: var(--line-primary-10);
  border-color: var(--line-primary);
  color: var(--line-primary);
}

.empty-hint-inline {
  color: var(--line-text-secondary);
  font-size: 13px;
}

.empty-hint,
.loading-hint {
  color: var(--line-text-secondary);
  font-size: 14px;
  text-align: center;
  padding: 20px;
}

.enrollment-list {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid var(--line-border);
  border-radius: 8px;
}

.enrollment-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid var(--line-border);
}

.enrollment-item:last-child {
  border-bottom: none;
}

.student-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.student-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--line-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: white;
  overflow: hidden;
  flex-shrink: 0;
}

.student-avatar.has-img {
  background: transparent;
}

.student-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.student-name {
  font-size: 14px;
  color: var(--line-text);
}

.enroll-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.enroll-status.enrolled {
  background: #ecfdf5;
  color: #059669;
}

.enroll-status.exempted {
  background: #fefce8;
  color: #ca8a04;
}

.enroll-status.absent {
  background: #fef2f2;
  color: #dc2626;
}

.enroll-status.completed {
  background: #eff6ff;
  color: #2563eb;
}

.icon-btn.primary {
  color: var(--line-primary);
}

.icon-btn.primary:hover {
  background: rgba(26, 115, 232, 0.1);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>

