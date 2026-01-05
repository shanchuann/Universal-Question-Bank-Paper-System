<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import axios from 'axios'
import { authState } from '@/states/authState'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import GoogleSelect from '@/components/GoogleSelect.vue'

interface Question {
  id: string
  stem: string
  type: string
  difficulty: string
  status: string
  version: number
  createdBy: string
  createdAt: string
  optionsJson?: string
  answerSchema?: string
  analysis?: string
  reviewNotes?: string
  creatorNickname?: string
}

const questions = ref<Question[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(20)
const total = ref(0)

const stats = ref({ total: 0, pending: 0, approved: 0, rejected: 0 })
const currentFilter = ref<string>('')

const reviewForm = ref({ action: 'APPROVE' as 'APPROVE' | 'REJECT', notes: '' })
const selectedQuestion = ref<Question | null>(null)
const showReviewModal = ref(false)
const showDetailModal = ref(false)

onMounted(() => {
  fetchStats()
  fetchQuestions()
})

async function fetchStats() {
  try {
    // 使用学生题目专用API获取统计
    const [pendingRes, approvedRes, rejectedRes, totalRes] = await Promise.all([
      axios.get('/api/questions/student-questions', { params: { status: 'PENDING_REVIEW', size: 1 } }),
      axios.get('/api/questions/student-questions', { params: { status: 'APPROVED', size: 1 } }),
      axios.get('/api/questions/student-questions', { params: { status: 'REJECTED', size: 1 } }),
      axios.get('/api/questions/student-questions', { params: { size: 1 } })
    ])
    stats.value = {
      pending: pendingRes.data.totalElements || 0,
      approved: approvedRes.data.totalElements || 0,
      rejected: rejectedRes.data.totalElements || 0,
      total: totalRes.data.totalElements || 0
    }
  } catch (error) {
    console.error('Failed to fetch stats:', error)
  }
}

async function fetchQuestions() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (currentFilter.value) params.status = currentFilter.value
    // 使用学生题目专用API，只显示学生创建的题目
    const response = await axios.get('/api/questions/student-questions', { params })
    questions.value = response.data.content || []
    total.value = response.data.totalElements || 0
  } catch (error) {
    console.error('Failed to fetch questions:', error)
  } finally {
    loading.value = false
  }
}

function filterByStatus(status: string) {
  currentFilter.value = status
  page.value = 0
  fetchQuestions()
}

function openReviewModal(question: Question) {
  selectedQuestion.value = question
  reviewForm.value = { action: 'APPROVE', notes: '' }
  showReviewModal.value = true
}

function openDetailModal(question: Question) {
  selectedQuestion.value = question
  showDetailModal.value = true
}

function openEditModal(question: Question) {
  selectedQuestion.value = question
  loadQuestionToForm(question)
  showEditModal.value = true
}

// 编辑弹窗相关
const showEditModal = ref(false)
const editLoading = ref(false)
const editQuillEditor = ref<any>(null)
const multiAnswer = ref<string[]>([])

const editForm = ref({
  type: 'SINGLE_CHOICE',
  difficulty: 'EASY',
  stem: '',
  options: ['', '', '', ''],
  answer: '',
  analysis: ''
})

const typeOptions = [
  { label: '单选题', value: 'SINGLE_CHOICE' },
  { label: '多选题', value: 'MULTIPLE_CHOICE' },
  { label: '判断题', value: 'TRUE_FALSE' },
  { label: '填空题', value: 'FILL_BLANK' },
  { label: '简答题', value: 'SHORT_ANSWER' }
]

const difficultyOptions = [
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '困难', value: 'HARD' }
]

const toolbarOptions = [
  ['bold', 'italic', 'underline'],
  [{ 'list': 'ordered'}, { 'list': 'bullet' }],
  [{ 'size': ['small', false, 'large'] }],
  [{ 'color': [] }],
  ['link', 'image'],
  ['clean']
]

watch(multiAnswer, (val) => {
  editForm.value.answer = val.join(',')
}, { deep: true })

function loadQuestionToForm(question: Question) {
  editForm.value.type = question.type || 'SINGLE_CHOICE'
  editForm.value.difficulty = question.difficulty || 'EASY'
  editForm.value.stem = question.stem || ''
  editForm.value.analysis = question.analysis || ''
  
  // 解析选项
  let parsedOptions: any[] = []
  if (question.optionsJson) {
    try { parsedOptions = JSON.parse(question.optionsJson) } catch { parsedOptions = [] }
  }
  
  if (['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(question.type)) {
    editForm.value.options = parsedOptions.map((opt: any) => 
      typeof opt === 'string' ? opt : (opt.text || opt.content || '')
    )
    while (editForm.value.options.length < 4) editForm.value.options.push('')
    
    if (question.answerSchema) {
      editForm.value.answer = question.answerSchema
      if (question.type === 'MULTIPLE_CHOICE') {
        multiAnswer.value = question.answerSchema.split(',').map((a: string) => a.trim())
      }
    }
  } else if (question.type === 'TRUE_FALSE') {
    editForm.value.answer = question.answerSchema || 'True'
  } else if (question.type === 'FILL_BLANK') {
    editForm.value.options = parsedOptions.map((opt: any) => 
      typeof opt === 'string' ? opt : (opt.text || '')
    )
    if (editForm.value.options.length === 0) editForm.value.options = ['']
  } else if (question.type === 'SHORT_ANSWER') {
    editForm.value.answer = question.answerSchema || ''
  }
}

function addOption() {
  editForm.value.options.push('')
}

function removeOption(index: number) {
  if (editForm.value.options.length > 2) {
    editForm.value.options.splice(index, 1)
  }
}

const imageHandler = () => {
  const input = document.createElement('input')
  input.setAttribute('type', 'file')
  input.setAttribute('accept', 'image/*')
  input.click()

  input.onchange = async () => {
    const file = input.files ? input.files[0] : null
    if (file) {
      const formData = new FormData()
      formData.append('file', file)
      try {
        const res = await axios.post('/api/files/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        const url = res.data.fileUrl
        if (editQuillEditor.value) {
          const quill = editQuillEditor.value.getQuill()
          const range = quill.getSelection()
          quill.insertEmbed(range ? range.index : 0, 'image', url)
        }
      } catch (err) {
        console.error('图片上传失败', err)
      }
    }
  }
}

const onEditorReady = (quill: any) => {
  const toolbar = quill.getModule('toolbar')
  toolbar.addHandler('image', imageHandler)
}

async function submitEdit() {
  if (!selectedQuestion.value) return
  editLoading.value = true
  
  try {
    let apiOptions: { text: string; isCorrect: boolean }[] = []
    
    if (['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(editForm.value.type)) {
      const correctAnswers = editForm.value.answer.split(',').map(a => a.trim())
      apiOptions = editForm.value.options.map((optText, idx) => ({
        text: optText,
        isCorrect: correctAnswers.includes(String.fromCharCode(65 + idx))
      }))
    } else if (editForm.value.type === 'TRUE_FALSE') {
      apiOptions = [
        { text: 'True', isCorrect: editForm.value.answer === 'True' },
        { text: 'False', isCorrect: editForm.value.answer === 'False' }
      ]
    } else if (editForm.value.type === 'FILL_BLANK') {
      apiOptions = editForm.value.options.map(optText => ({ text: optText, isCorrect: true }))
    } else if (editForm.value.type === 'SHORT_ANSWER') {
      apiOptions = [{ text: editForm.value.answer, isCorrect: true }]
    }
    
    const payload = {
      type: editForm.value.type,
      difficulty: editForm.value.difficulty,
      stem: editForm.value.stem,
      options: apiOptions,
      answerSchema: editForm.value.answer,
      analysis: editForm.value.analysis,
      score: 1.0
    }
    
    const token = localStorage.getItem('token')
    await axios.put(`/api/questions/${selectedQuestion.value.id}`, payload, {
      headers: { Authorization: `Bearer ${token}` }
    })
    
    showEditModal.value = false
    fetchQuestions()
    fetchStats()
  } catch (err) {
    console.error('保存失败', err)
    alert('保存失败')
  } finally {
    editLoading.value = false
  }
}

async function submitReview() {
  if (!selectedQuestion.value) return
  try {
    const url = `/api/questions/${selectedQuestion.value.id}/${reviewForm.value.action === 'APPROVE' ? 'approve' : 'reject'}`
    await axios.post(url, { reviewerId: authState.user.id, notes: reviewForm.value.notes })
    showReviewModal.value = false
    fetchStats()
    fetchQuestions()
  } catch (error) {
    console.error('Failed to submit review:', error)
    alert('审核提交失败')
  }
}

async function changeReviewStatus(question: Question, newStatus: 'APPROVE' | 'REJECT') {
  try {
    const url = `/api/questions/${question.id}/${newStatus === 'APPROVE' ? 'approve' : 'reject'}`
    await axios.post(url, { reviewerId: authState.user.id, notes: `状态变更为${newStatus === 'APPROVE' ? '通过' : '未通过'}` })
    fetchStats()
    fetchQuestions()
  } catch (error) {
    console.error('Failed to change status:', error)
    alert('状态修改失败')
  }
}

function goToPage(newPage: number) {
  page.value = newPage
  fetchQuestions()
}

const typeLabels: Record<string, string> = {
  SINGLE_CHOICE: '单选题', MULTI_CHOICE: '多选题', MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题', FILL_BLANK: '填空题', SHORT_ANSWER: '简答题'
}
const difficultyLabels: Record<string, string> = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }
const statusLabels: Record<string, string> = {
  DRAFT: '草稿', PENDING_REVIEW: '待审核', APPROVED: '已通过', REJECTED: '未通过', ACTIVE: '已激活'
}

function parseOptions(optionsJson: string | undefined): any[] {
  if (!optionsJson) return []
  try { return JSON.parse(optionsJson) } catch { return [] }
}

const emptyText = computed(() => {
  const map: Record<string, string> = {
    '': '暂无题目', 'PENDING_REVIEW': '暂无待审核题目',
    'APPROVED': '暂无已通过题目', 'REJECTED': '暂无未通过题目'
  }
  return map[currentFilter.value] || '暂无题目'
})
</script>

<template>
  <div class="question-review-view">
    <div class="page-header">
      <h1>题目审核</h1>
      <p class="subtitle">审核班级同学上传的题目</p>
    </div>

    <div class="content-card">
      <!-- 标签筛选导航 -->
      <div class="filter-tabs">
        <button 
          :class="['filter-tab', { active: currentFilter === '' }]"
          @click="filterByStatus('')"
        >
          全部
          <span class="tab-count">{{ stats.total }}</span>
        </button>
        <button 
          :class="['filter-tab', { active: currentFilter === 'PENDING_REVIEW' }]"
          @click="filterByStatus('PENDING_REVIEW')"
        >
          <span class="status-dot pending"></span>
          待审核
          <span class="tab-count">{{ stats.pending }}</span>
        </button>
        <button 
          :class="['filter-tab', { active: currentFilter === 'APPROVED' }]"
          @click="filterByStatus('APPROVED')"
        >
          <span class="status-dot approved"></span>
          已通过
          <span class="tab-count">{{ stats.approved }}</span>
        </button>
        <button 
          :class="['filter-tab', { active: currentFilter === 'REJECTED' }]"
          @click="filterByStatus('REJECTED')"
        >
          <span class="status-dot rejected"></span>
          未通过
          <span class="tab-count">{{ stats.rejected }}</span>
        </button>
      </div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
      
      <div v-else-if="questions.length === 0" class="empty-state">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#9aa0a6" stroke-width="1.5">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
          <polyline points="14 2 14 8 20 8"></polyline>
          <line x1="16" y1="13" x2="8" y2="13"></line>
          <line x1="16" y1="17" x2="8" y2="17"></line>
        </svg>
        <p>{{ emptyText }}</p>
      </div>

      <div v-else class="question-list">
        <div v-for="question in questions" :key="question.id" class="question-card">
          <div class="question-header">
            <div class="question-meta">
              <span class="question-type">{{ typeLabels[question.type] || question.type }}</span>
              <span :class="['question-difficulty', question.difficulty?.toLowerCase()]">
                {{ difficultyLabels[question.difficulty] || question.difficulty }}
              </span>
              <span :class="['question-status', 'status-' + question.status?.toLowerCase()]">
                {{ statusLabels[question.status] || question.status }}
              </span>
            </div>
            <div class="question-actions">
              <button class="icon-btn" title="查看详情" @click="openDetailModal(question)">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle cx="12" cy="12" r="3"></circle>
                </svg>
              </button>
              <button class="icon-btn" title="编辑题目" @click="openEditModal(question)">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
              </button>
            </div>
          </div>

          <div class="question-stem" @click="openDetailModal(question)">
            <div class="stem-content" v-html="question.stem"></div>
          </div>

          <!-- 选项预览 -->
          <div v-if="parseOptions(question.optionsJson).length > 0" class="question-options-preview">
            <div v-for="(opt, idx) in parseOptions(question.optionsJson).slice(0, 4)" :key="idx" class="option-preview-item">
              <span class="option-letter">{{ String.fromCharCode(65 + idx) }}</span>
              <span class="option-text">{{ opt.text || opt }}</span>
            </div>
          </div>

          <!-- 答案预览 -->
          <div v-if="question.answerSchema" class="question-answer-preview">
            <span class="answer-label">答案：</span>
            <span class="answer-value">{{ question.answerSchema }}</span>
          </div>

          <div class="question-footer">
            <div class="footer-left">
              <span class="question-creator">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
                {{ question.creatorNickname || question.createdBy || '未知' }}
              </span>
              <span class="question-date">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                  <line x1="16" y1="2" x2="16" y2="6"></line>
                  <line x1="8" y1="2" x2="8" y2="6"></line>
                  <line x1="3" y1="10" x2="21" y2="10"></line>
                </svg>
                {{ question.createdAt ? new Date(question.createdAt).toLocaleDateString('zh-CN') : '-' }}
              </span>
            </div>
            <div class="footer-actions">
              <template v-if="question.status === 'PENDING_REVIEW'">
                <button class="action-btn approve-btn" @click="openReviewModal(question)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                  审核
                </button>
              </template>
              <template v-else-if="question.status === 'APPROVED'">
                <button class="action-btn reject-small-btn" @click="changeReviewStatus(question, 'REJECT')">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="15" y1="9" x2="9" y2="15"></line>
                    <line x1="9" y1="9" x2="15" y2="15"></line>
                  </svg>
                  撤回
                </button>
              </template>
              <template v-else-if="question.status === 'REJECTED'">
                <button class="action-btn approve-small-btn" @click="changeReviewStatus(question, 'APPROVE')">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                  通过
                </button>
              </template>
            </div>
          </div>
        </div>
      </div>

      <div v-if="total > size" class="pagination">
        <button :disabled="page === 0" class="icon-btn" @click="goToPage(page - 1)">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
        </button>
        <span class="page-info">第 {{ page + 1 }} 页，共 {{ Math.ceil(total / size) }} 页</span>
        <button :disabled="page >= Math.ceil(total / size) - 1" class="icon-btn" @click="goToPage(page + 1)">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </button>
      </div>
    </div>

    <!-- 审核弹窗 -->
    <div v-if="showReviewModal && selectedQuestion" class="modal-overlay" @click.self="showReviewModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2>审核题目</h2>
          <button class="close-btn" @click="showReviewModal = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        
        <div class="question-preview">
          <div class="preview-label">题干</div>
          <div class="preview-stem" v-html="selectedQuestion.stem"></div>
          
          <div v-if="parseOptions(selectedQuestion.optionsJson).length > 0" class="preview-options">
            <div class="preview-label">选项</div>
            <div class="preview-options-list">
              <div v-for="(opt, idx) in parseOptions(selectedQuestion.optionsJson)" :key="idx" class="preview-option">
                <span class="opt-letter">{{ String.fromCharCode(65 + idx) }}.</span>
                <span>{{ opt.text || opt }}</span>
              </div>
            </div>
          </div>

          <div v-if="selectedQuestion.answerSchema" class="preview-answer">
            <div class="preview-label">答案</div>
            <div class="preview-answer-value">{{ selectedQuestion.answerSchema }}</div>
          </div>
        </div>

        <form @submit.prevent="submitReview">
          <div class="form-group">
            <label>审核结果</label>
            <div class="radio-group">
              <label class="radio-item approve" :class="{ selected: reviewForm.action === 'APPROVE' }">
                <input v-model="reviewForm.action" type="radio" value="APPROVE" />
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                <span>通过</span>
              </label>
              <label class="radio-item reject" :class="{ selected: reviewForm.action === 'REJECT' }">
                <input v-model="reviewForm.action" type="radio" value="REJECT" />
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
                <span>退回</span>
              </label>
            </div>
          </div>

          <div class="form-group">
            <label>审核意见</label>
            <textarea v-model="reviewForm.notes" class="google-input" rows="4" placeholder="请输入审核意见..."></textarea>
          </div>

          <div class="form-actions">
            <button type="button" class="google-btn text-btn" @click="showReviewModal = false">取消</button>
            <button type="submit" :class="['google-btn', reviewForm.action === 'APPROVE' ? 'approve-btn' : 'reject-btn']">
              {{ reviewForm.action === 'APPROVE' ? '审核通过' : '审核退回' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 题目预览弹窗 -->
    <div v-if="showDetailModal && selectedQuestion" class="modal-overlay" @click.self="showDetailModal = false">
      <div class="modal-content preview-modal">
        <div class="modal-header">
          <h2>题目预览</h2>
          <button class="close-btn" @click="showDetailModal = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>

        <div class="preview-body">
          <!-- 题目信息标签 -->
          <div class="preview-tags">
            <span class="tag type-tag">{{ typeLabels[selectedQuestion.type] }}</span>
            <span :class="['tag', 'difficulty-tag', selectedQuestion.difficulty?.toLowerCase()]">
              {{ difficultyLabels[selectedQuestion.difficulty] }}
            </span>
          </div>

          <!-- 题干 -->
          <div class="preview-stem">
            <div class="stem-text" v-html="selectedQuestion.stem"></div>
          </div>

          <!-- 选项 -->
          <div v-if="parseOptions(selectedQuestion.optionsJson).length > 0" class="preview-options">
            <div v-for="(opt, idx) in parseOptions(selectedQuestion.optionsJson)" :key="idx" 
                 :class="['option-row', { 'is-answer': selectedQuestion.answerSchema?.includes(String.fromCharCode(65 + idx)) }]">
              <span class="option-letter">{{ String.fromCharCode(65 + idx) }}</span>
              <span class="option-content">{{ opt.text || opt }}</span>
              <svg v-if="selectedQuestion.answerSchema?.includes(String.fromCharCode(65 + idx))" 
                   class="correct-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
            </div>
          </div>

          <!-- 答案区域 -->
          <div class="preview-answer-section">
            <div class="answer-header">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
              </svg>
              <span>参考答案</span>
            </div>
            <div class="answer-content">{{ selectedQuestion.answerSchema || '暂无答案' }}</div>
          </div>

          <!-- 解析区域 -->
          <div v-if="selectedQuestion.analysis" class="preview-analysis-section">
            <div class="analysis-header">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path>
                <line x1="12" y1="17" x2="12.01" y2="17"></line>
              </svg>
              <span>答案解析</span>
            </div>
            <div class="analysis-content" v-html="selectedQuestion.analysis"></div>
          </div>

          <!-- 审核意见 -->
          <div v-if="selectedQuestion.reviewNotes" class="preview-review-section">
            <div class="review-header">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
              </svg>
              <span>审核意见</span>
            </div>
            <div class="review-content">{{ selectedQuestion.reviewNotes }}</div>
          </div>
        </div>

        <div class="preview-footer">
          <button class="google-btn text-btn" @click="showDetailModal = false">关闭</button>
          <button class="google-btn primary-btn" @click="showDetailModal = false; openEditModal(selectedQuestion)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
            </svg>
            编辑
          </button>
        </div>
      </div>
    </div>

    <!-- 编辑题目弹窗 -->
    <div v-if="showEditModal && selectedQuestion" class="modal-overlay" @click.self="showEditModal = false">
      <div class="modal-content edit-modal">
        <div class="modal-header">
          <h2>编辑题目</h2>
          <button class="close-btn" @click="showEditModal = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>

        <div class="edit-body">
          <!-- 题型和难度 -->
          <div class="edit-row">
            <div class="edit-field">
              <label>题目类型</label>
              <GoogleSelect v-model="editForm.type" :options="typeOptions" />
            </div>
            <div class="edit-field">
              <label>难度</label>
              <GoogleSelect v-model="editForm.difficulty" :options="difficultyOptions" />
            </div>
          </div>

          <!-- 题干编辑器 -->
          <div class="edit-field">
            <label>题干</label>
            <div class="quill-wrapper">
              <QuillEditor
                ref="editQuillEditor"
                v-model:content="editForm.stem"
                contentType="html"
                :toolbar="toolbarOptions"
                theme="snow"
                @ready="onEditorReady"
              />
            </div>
          </div>

          <!-- 选项编辑 (选择题) -->
          <div v-if="['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(editForm.type)" class="edit-field">
            <label>选项</label>
            <div class="options-editor">
              <div v-for="(_opt, idx) in editForm.options" :key="idx" class="option-edit-row">
                <span class="option-letter-badge">{{ String.fromCharCode(65 + idx) }}</span>
                <input v-model="editForm.options[idx]" type="text" class="google-input" placeholder="输入选项内容" />
                <button v-if="editForm.options.length > 2" class="remove-option-btn" @click="removeOption(idx)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                </button>
              </div>
              <button class="add-option-btn" @click="addOption">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
                添加选项
              </button>
            </div>
          </div>

          <!-- 答案选择 (单选题) -->
          <div v-if="editForm.type === 'SINGLE_CHOICE'" class="edit-field">
            <label>正确答案</label>
            <div class="answer-options">
              <label v-for="(_opt, idx) in editForm.options" :key="idx" 
                     :class="['answer-option', { selected: editForm.answer === String.fromCharCode(65 + idx) }]">
                <input type="radio" v-model="editForm.answer" :value="String.fromCharCode(65 + idx)" />
                <span class="answer-letter">{{ String.fromCharCode(65 + idx) }}</span>
              </label>
            </div>
          </div>

          <!-- 答案选择 (多选题) -->
          <div v-if="editForm.type === 'MULTIPLE_CHOICE'" class="edit-field">
            <label>正确答案（可多选）</label>
            <div class="answer-options">
              <label v-for="(_opt, idx) in editForm.options" :key="idx" 
                     :class="['answer-option', { selected: multiAnswer.includes(String.fromCharCode(65 + idx)) }]">
                <input type="checkbox" v-model="multiAnswer" :value="String.fromCharCode(65 + idx)" />
                <span class="answer-letter">{{ String.fromCharCode(65 + idx) }}</span>
              </label>
            </div>
          </div>

          <!-- 判断题答案 -->
          <div v-if="editForm.type === 'TRUE_FALSE'" class="edit-field">
            <label>正确答案</label>
            <div class="tf-options">
              <label :class="['tf-option', { selected: editForm.answer === 'True' }]">
                <input type="radio" v-model="editForm.answer" value="True" />
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                <span>正确</span>
              </label>
              <label :class="['tf-option', { selected: editForm.answer === 'False' }]">
                <input type="radio" v-model="editForm.answer" value="False" />
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
                <span>错误</span>
              </label>
            </div>
          </div>

          <!-- 填空题答案 -->
          <div v-if="editForm.type === 'FILL_BLANK'" class="edit-field">
            <label>填空答案</label>
            <div class="fill-answers">
              <div v-for="(_ans, idx) in editForm.options" :key="idx" class="fill-answer-row">
                <span class="fill-label">空{{ idx + 1 }}</span>
                <input v-model="editForm.options[idx]" type="text" class="google-input" placeholder="输入答案" />
              </div>
            </div>
          </div>

          <!-- 简答题答案 -->
          <div v-if="editForm.type === 'SHORT_ANSWER'" class="edit-field">
            <label>参考答案</label>
            <textarea v-model="editForm.answer" class="google-input" rows="4" placeholder="输入参考答案"></textarea>
          </div>

          <!-- 解析 -->
          <div class="edit-field">
            <label>答案解析（选填）</label>
            <textarea v-model="editForm.analysis" class="google-input" rows="3" placeholder="输入答案解析"></textarea>
          </div>
        </div>

        <div class="edit-footer">
          <button class="google-btn text-btn" @click="showEditModal = false">取消</button>
          <button class="google-btn primary-btn" :disabled="editLoading" @click="submitEdit">
            <svg v-if="!editLoading" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
              <polyline points="17 21 17 13 7 13 7 21"></polyline>
              <polyline points="7 3 7 8 15 8"></polyline>
            </svg>
            <span v-if="editLoading" class="btn-spinner"></span>
            {{ editLoading ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.question-review-view { padding: 24px; max-width: 1200px; margin: 0 auto; }
.page-header { margin-bottom: 24px; text-align: center; }
.page-header h1 { font-size: 24px; font-weight: 500; color: #202124; margin: 0 0 4px 0; }
.subtitle { color: #5f6368; margin: 0; font-size: 14px; }

.content-card { background: #fff; border: 1px solid #dadce0; border-radius: 12px; padding: 24px; }
.content-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.content-header h2 { font-size: 18px; font-weight: 500; color: #202124; margin: 0; }
.count-badge { background: #e8eaed; color: #5f6368; font-size: 12px; padding: 4px 10px; border-radius: 12px; }

/* 标签筛选导航 */
.filter-tabs { display: flex; gap: 8px; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #e8eaed; flex-wrap: wrap; }
.filter-tab { display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px; border: 1px solid #dadce0; border-radius: 20px; background: #fff; font-size: 13px; color: #5f6368; cursor: pointer; transition: all 0.2s; }
.filter-tab:hover { border-color: #1a73e8; color: #1a73e8; }
.filter-tab.active { background: #e8f0fe; border-color: #1a73e8; color: #1a73e8; font-weight: 500; }
.filter-tab .status-dot { width: 8px; height: 8px; border-radius: 50%; }
.filter-tab .status-dot.pending { background: #f9ab00; }
.filter-tab .status-dot.approved { background: #1e8e3e; }
.filter-tab .status-dot.rejected { background: #d93025; }
.filter-tab .tab-count { background: #f1f3f4; padding: 2px 8px; border-radius: 10px; font-size: 12px; }
.filter-tab.active .tab-count { background: rgba(26,115,232,0.2); }

.loading-state { display: flex; flex-direction: column; align-items: center; padding: 48px; color: #5f6368; }
.spinner { width: 32px; height: 32px; border: 3px solid #e8eaed; border-top-color: #1a73e8; border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 16px; }
@keyframes spin { to { transform: rotate(360deg); } }

.empty-state { text-align: center; padding: 48px; color: #5f6368; }
.empty-state svg { margin-bottom: 16px; }
.empty-state p { margin: 0; font-size: 15px; }

.question-list { display: flex; flex-direction: column; gap: 16px; }
.question-card { border: 1px solid #dadce0; border-radius: 8px; padding: 16px; transition: all 0.2s; width: 100%; box-sizing: border-box; }
.question-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); border-color: #1a73e8; }
.question-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.question-meta { display: flex; gap: 8px; flex-wrap: wrap; }
.question-type { background: #e8f0fe; color: #1a73e8; font-size: 12px; padding: 4px 10px; border-radius: 12px; }
.question-difficulty { font-size: 12px; padding: 4px 10px; border-radius: 12px; }
.question-difficulty.easy { background: #e6f4ea; color: #1e8e3e; }
.question-difficulty.medium { background: #fef7e0; color: #f57c00; }
.question-difficulty.hard { background: #fce8e6; color: #d93025; }
.question-status { font-size: 12px; padding: 4px 10px; border-radius: 12px; }
.status-pending_review { background: #fef7e0; color: #f9ab00; }
.status-approved { background: #e6f4ea; color: #1e8e3e; }
.status-rejected { background: #fce8e6; color: #d93025; }
.status-draft { background: #e8eaed; color: #5f6368; }
.status-active { background: #e8f0fe; color: #1a73e8; }

.question-actions { display: flex; gap: 4px; }
.icon-btn { width: 32px; height: 32px; border: none; background: transparent; border-radius: 50%; cursor: pointer; display: flex; align-items: center; justify-content: center; color: #5f6368; transition: all 0.2s; }
.icon-btn:hover { background: #f1f3f4; color: #1a73e8; }
.icon-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.question-stem { margin-bottom: 12px; padding-bottom: 12px; border-bottom: 1px solid #e8eaed; cursor: pointer; overflow: hidden; }
.question-stem .stem-content { color: #202124; line-height: 1.6; max-height: 72px; overflow: hidden; }
.question-stem .stem-content :deep(p) { margin: 0 0 4px 0; }
.question-stem .stem-content :deep(img) { max-width: 100%; max-height: 60px; object-fit: contain; border-radius: 4px; }

/* 选项预览 */
.question-options-preview { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.option-preview-item { display: flex; align-items: center; gap: 4px; background: #f1f3f4; padding: 4px 10px; border-radius: 4px; font-size: 12px; max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.option-preview-item .option-letter { font-weight: 600; color: #1a73e8; }
.option-preview-item .option-text { color: #5f6368; overflow: hidden; text-overflow: ellipsis; }

/* 答案预览 */
.question-answer-preview { display: flex; align-items: center; gap: 6px; margin-bottom: 12px; padding: 6px 10px; background: #e6f4ea; border-radius: 4px; font-size: 12px; }
.question-answer-preview .answer-label { color: #5f6368; }
.question-answer-preview .answer-value { color: #1e8e3e; font-weight: 600; }

.question-footer { display: flex; justify-content: space-between; align-items: center; }
.footer-left { display: flex; gap: 16px; }
.question-creator, .question-date { display: flex; align-items: center; gap: 4px; color: #5f6368; font-size: 13px; }
.footer-actions { display: flex; gap: 8px; }

.action-btn { display: inline-flex; align-items: center; gap: 6px; padding: 6px 14px; border: none; border-radius: 6px; font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.2s; }
.approve-btn { background: #1a73e8; color: white; }
.approve-btn:hover { background: #1557b0; }
.approve-small-btn { background: #e6f4ea; color: #1e8e3e; }
.approve-small-btn:hover { background: #ceead6; }
.reject-small-btn { background: #fce8e6; color: #d93025; }
.reject-small-btn:hover { background: #f5c6c6; }

.pagination { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 24px; padding-top: 24px; border-top: 1px solid #e8eaed; }
.page-info { color: #5f6368; font-size: 14px; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 24px; }
.modal-content { background: #fff; border-radius: 12px; width: 100%; max-width: 500px; max-height: 90vh; overflow-y: auto; box-shadow: 0 24px 48px rgba(0,0,0,0.2); }
.modal-content.large { max-width: 700px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid #e8eaed; position: sticky; top: 0; background: #fff; z-index: 1; }
.modal-header h2 { margin: 0; font-size: 18px; font-weight: 500; color: #202124; }
.close-btn { width: 36px; height: 36px; border: none; background: transparent; border-radius: 50%; cursor: pointer; display: flex; align-items: center; justify-content: center; color: #5f6368; transition: background 0.2s; }
.close-btn:hover { background: #f1f3f4; }

.modal-content form, .detail-content { padding: 24px; }
.question-preview { background: #f8f9fa; border-radius: 8px; padding: 16px; margin: 24px; margin-top: 0; }
.preview-label { font-size: 12px; color: #5f6368; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.5px; }
.question-preview p { margin: 0; color: #202124; line-height: 1.6; }
.preview-options { margin-top: 16px; }
.preview-options-list { display: flex; flex-direction: column; gap: 6px; }
.preview-option { display: flex; gap: 8px; padding: 8px 12px; background: #fff; border-radius: 6px; font-size: 14px; }
.preview-option .opt-letter { font-weight: 600; color: #1a73e8; min-width: 20px; }
.preview-answer { margin-top: 16px; }
.preview-answer-value { display: inline-block; padding: 8px 16px; background: #e6f4ea; color: #1e8e3e; font-weight: 600; border-radius: 6px; font-size: 14px; }

.form-group { margin-bottom: 20px; }
.form-group label { display: block; margin-bottom: 8px; font-size: 14px; color: #5f6368; font-weight: 500; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

/* 选项编辑样式 */
.options-edit .option-row { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.options-edit .option-label { font-weight: 500; color: #5f6368; width: 24px; flex-shrink: 0; }
.options-edit .option-row .google-input { flex: 1; }
.options-edit .remove-opt { width: 32px; height: 32px; border: none; background: #fce8e6; border-radius: 50%; cursor: pointer; display: flex; align-items: center; justify-content: center; color: #d93025; transition: all 0.2s; flex-shrink: 0; }
.options-edit .remove-opt:hover { background: #d93025; color: #fff; }
.google-btn.small { padding: 6px 12px; font-size: 13px; }

/* 单选/多选答案选择 */
.radio-group { display: flex; gap: 16px; padding: 8px 0; }
.radio-label { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 8px 16px; border-radius: 6px; transition: background 0.2s; }
.radio-label:hover { background: #f1f3f4; }
.radio-label input[type="radio"] { width: 18px; height: 18px; accent-color: #1a73e8; cursor: pointer; }
.radio-text { font-size: 14px; color: #3c4043; font-weight: 500; }

.radio-item { flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 12px; border: 2px solid #dadce0; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
.radio-item input { display: none; }
.radio-item.approve:hover, .radio-item.approve.selected { border-color: #1e8e3e; background: #e6f4ea; color: #1e8e3e; }
.radio-item.reject:hover, .radio-item.reject.selected { border-color: #d93025; background: #fce8e6; color: #d93025; }

.google-input { width: 100%; padding: 10px 14px; border: 1px solid #dadce0; border-radius: 8px; font-size: 14px; font-family: inherit; box-sizing: border-box; transition: border-color 0.2s, box-shadow 0.2s; }
.google-input:focus { outline: none; border-color: #1a73e8; box-shadow: 0 0 0 3px rgba(26,115,232,0.1); }
.google-input.mono { font-family: 'Consolas', monospace; font-size: 13px; }
textarea.google-input { resize: vertical; min-height: 80px; }
select.google-input { cursor: pointer; }

.form-actions { display: flex; justify-content: flex-end; gap: 12px; padding: 16px 24px; border-top: 1px solid #e8eaed; background: #f8f9fa; margin: 0 -24px -24px; border-radius: 0 0 12px 12px; }
.modal-content form .form-actions { margin: 0; padding: 24px 0 0; border-top: none; background: transparent; border-radius: 0; }

.google-btn { padding: 10px 20px; border-radius: 6px; font-size: 14px; font-weight: 500; cursor: pointer; border: none; display: inline-flex; align-items: center; gap: 8px; transition: all 0.2s; }
.primary-btn { background: #1a73e8; color: #fff; }
.primary-btn:hover { background: #1557b0; }
.text-btn { background: transparent; color: #5f6368; }
.text-btn:hover { background: #f1f3f4; }
.reject-btn { background: #d93025; color: #fff; }
.reject-btn:hover { background: #b3261e; }

.detail-meta { display: flex; gap: 8px; margin-bottom: 20px; }
.detail-section { margin-bottom: 20px; }
.section-label { font-size: 12px; color: #5f6368; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.5px; font-weight: 500; }
.section-content { background: #f8f9fa; border-radius: 8px; padding: 14px 16px; color: #202124; line-height: 1.6; }
.section-content.stem-html :deep(p) { margin: 0 0 8px 0; }
.section-content.stem-html :deep(p:last-child) { margin-bottom: 0; }
.section-content.stem-html :deep(img) { max-width: 100%; height: auto; border-radius: 6px; margin: 8px 0; }
.section-content.answer { background: #e6f4ea; color: #1e8e3e; font-weight: 500; }
.section-content.review-notes { background: #fef7e0; color: #b36b00; }
.preview-stem :deep(p) { margin: 0 0 8px 0; }
.preview-stem :deep(p:last-child) { margin-bottom: 0; }
.preview-stem :deep(img) { max-width: 100%; height: auto; border-radius: 6px; margin: 8px 0; }
.options-list { background: #f8f9fa; border-radius: 8px; padding: 12px 16px; }
.option-item { display: flex; gap: 8px; padding: 8px 0; border-bottom: 1px solid #e8eaed; }
.option-item:last-child { border-bottom: none; }
.option-label { font-weight: 500; color: #1a73e8; min-width: 24px; }

/* 题目预览弹窗样式 */
.preview-modal { max-width: 640px; }
.preview-body { padding: 24px; }
.preview-tags { display: flex; gap: 8px; margin-bottom: 20px; }
.preview-tags .tag { font-size: 12px; padding: 4px 12px; border-radius: 16px; font-weight: 500; }
.preview-tags .type-tag { background: #e8f0fe; color: #1a73e8; }
.preview-tags .difficulty-tag.easy { background: #e6f4ea; color: #1e8e3e; }
.preview-tags .difficulty-tag.medium { background: #fef7e0; color: #f57c00; }
.preview-tags .difficulty-tag.hard { background: #fce8e6; color: #d93025; }

.preview-stem { margin-bottom: 24px; }
.preview-stem .stem-text { font-size: 16px; line-height: 1.8; color: #202124; }
.preview-stem .stem-text :deep(p) { margin: 0 0 12px 0; }
.preview-stem .stem-text :deep(p:last-child) { margin-bottom: 0; }
.preview-stem .stem-text :deep(img) { max-width: 100%; height: auto; border-radius: 8px; margin: 12px 0; }

.preview-options { display: flex; flex-direction: column; gap: 12px; margin-bottom: 24px; }
.preview-options .option-row { display: flex; align-items: center; gap: 12px; padding: 14px 16px; background: #f8f9fa; border-radius: 10px; border: 2px solid transparent; transition: all 0.2s; }
.preview-options .option-row.is-answer { background: #e6f4ea; border-color: #1e8e3e; }
.preview-options .option-letter { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; background: #fff; border-radius: 50%; font-weight: 600; color: #5f6368; font-size: 14px; flex-shrink: 0; }
.preview-options .option-row.is-answer .option-letter { background: #1e8e3e; color: #fff; }
.preview-options .option-content { flex: 1; font-size: 15px; color: #3c4043; line-height: 1.5; }
.preview-options .correct-icon { color: #1e8e3e; flex-shrink: 0; }

.preview-answer-section { background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%); border-radius: 12px; padding: 16px 20px; margin-bottom: 16px; }
.preview-answer-section .answer-header { display: flex; align-items: center; gap: 8px; color: #2e7d32; font-weight: 600; font-size: 14px; margin-bottom: 10px; }
.preview-answer-section .answer-content { font-size: 18px; font-weight: 600; color: #1b5e20; padding-left: 26px; }

.preview-analysis-section { background: #f3e5f5; border-radius: 12px; padding: 16px 20px; margin-bottom: 16px; }
.preview-analysis-section .analysis-header { display: flex; align-items: center; gap: 8px; color: #7b1fa2; font-weight: 600; font-size: 14px; margin-bottom: 10px; }
.preview-analysis-section .analysis-content { font-size: 14px; color: #4a148c; line-height: 1.7; padding-left: 26px; }
.preview-analysis-section .analysis-content :deep(p) { margin: 0 0 8px 0; }

.preview-review-section { background: #fff3e0; border-radius: 12px; padding: 16px 20px; margin-bottom: 16px; }
.preview-review-section .review-header { display: flex; align-items: center; gap: 8px; color: #e65100; font-weight: 600; font-size: 14px; margin-bottom: 10px; }
.preview-review-section .review-content { font-size: 14px; color: #bf360c; line-height: 1.7; padding-left: 26px; }

.preview-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 16px 24px; border-top: 1px solid #e8eaed; background: #fafafa; border-radius: 0 0 12px 12px; }

/* 编辑弹窗样式 */
.edit-modal { max-width: 720px; }
.edit-body { padding: 24px; max-height: 60vh; overflow-y: auto; }
.edit-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.edit-field { margin-bottom: 20px; }
.edit-field label { display: block; font-size: 13px; font-weight: 500; color: #5f6368; margin-bottom: 8px; }

.quill-wrapper { border: 1px solid #dadce0; border-radius: 8px; overflow: hidden; }
.quill-wrapper :deep(.ql-toolbar) { border: none; border-bottom: 1px solid #dadce0; background: #f8f9fa; }
.quill-wrapper :deep(.ql-container) { border: none; font-size: 15px; min-height: 120px; }
.quill-wrapper :deep(.ql-editor) { min-height: 120px; }

.options-editor { display: flex; flex-direction: column; gap: 10px; }
.option-edit-row { display: flex; align-items: center; gap: 10px; }
.option-letter-badge { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; background: #e8f0fe; color: #1a73e8; border-radius: 50%; font-weight: 600; font-size: 13px; flex-shrink: 0; }
.option-edit-row .google-input { flex: 1; }
.remove-option-btn { width: 32px; height: 32px; border: none; background: #fce8e6; border-radius: 50%; cursor: pointer; display: flex; align-items: center; justify-content: center; color: #d93025; transition: all 0.2s; flex-shrink: 0; }
.remove-option-btn:hover { background: #d93025; color: #fff; }
.add-option-btn { display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px; background: #f1f3f4; border: none; border-radius: 6px; color: #5f6368; font-size: 13px; cursor: pointer; transition: all 0.2s; align-self: flex-start; }
.add-option-btn:hover { background: #e8eaed; color: #202124; }

.answer-options { display: flex; gap: 10px; flex-wrap: wrap; }
.answer-option { display: flex; align-items: center; justify-content: center; width: 44px; height: 44px; border: 2px solid #dadce0; border-radius: 10px; cursor: pointer; transition: all 0.2s; }
.answer-option input { display: none; }
.answer-option .answer-letter { font-size: 16px; font-weight: 600; color: #5f6368; }
.answer-option:hover { border-color: #1a73e8; background: #e8f0fe; }
.answer-option.selected { border-color: #1e8e3e; background: #e6f4ea; }
.answer-option.selected .answer-letter { color: #1e8e3e; }

.tf-options { display: flex; gap: 16px; }
.tf-option { flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 14px; border: 2px solid #dadce0; border-radius: 10px; cursor: pointer; transition: all 0.2s; }
.tf-option input { display: none; }
.tf-option:hover { border-color: #1a73e8; }
.tf-option.selected:first-child { border-color: #1e8e3e; background: #e6f4ea; color: #1e8e3e; }
.tf-option.selected:last-child { border-color: #d93025; background: #fce8e6; color: #d93025; }

.fill-answers { display: flex; flex-direction: column; gap: 10px; }
.fill-answer-row { display: flex; align-items: center; gap: 10px; }
.fill-label { width: 40px; font-size: 13px; color: #5f6368; font-weight: 500; flex-shrink: 0; }

.edit-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 16px 24px; border-top: 1px solid #e8eaed; background: #fafafa; border-radius: 0 0 12px 12px; }
.btn-spinner { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.8s linear infinite; }

@media (max-width: 768px) {
  .stats { grid-template-columns: repeat(2, 1fr); }
  .form-row { grid-template-columns: 1fr; }
  .footer-left { flex-direction: column; gap: 4px; }
  .edit-row { grid-template-columns: 1fr; }
}
</style>
