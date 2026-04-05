<script setup lang="ts">
import { ref, onUpdated, nextTick, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import 'katex/dist/katex.min.css'
import katex from 'katex'
import { authState } from '@/states/authState'
import { Square, CheckSquare } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'

const { showToast } = useToast()

interface Question {
  id: string
  stem: string
  type: string
  options: string[]
}

interface PaperItem {
  type: 'QUESTION' | 'SECTION'
  id?: string
  sectionTitle?: string
  score?: number
}

interface Paper {
  id: number
  title: string
  questions: Question[]
  items?: PaperItem[]
}

interface ExamRecord {
  questionId: string
  userAnswer: string
  isCorrect: boolean
}

interface Exam {
  id: number
  paper: Paper
  userId: string
  score: number | null
  records?: ExamRecord[]
}

type DisplayItem =
  | { type: 'SECTION'; title?: string }
  | { type: 'QUESTION'; data?: Question; score?: number }

const route = useRoute()
const router = useRouter()
const paperId = ref(route.query.paperId?.toString() || '')
const planId = ref(route.query.planId?.toString() || '')
const userId = ref(
  authState.isAuthenticated && authState.user.id
    ? authState.user.id
    : 'guest-' + Math.floor(Math.random() * 10000)
)

// Update userId when profile is loaded
watch(
  () => authState.user.id,
  (newId) => {
    if (authState.isAuthenticated && newId) {
      userId.value = newId
    }
  }
)

const exam = ref<Exam | null>(null)
const answers = ref<Record<string, string | string[]>>({})
const loading = ref(false)
const error = ref('')
const submitted = ref(false)
const flaggedQuestions = ref<Set<string>>(new Set())
const currentSectionIndex = ref(0)

// Security State
const switchCount = ref(0)
const maxSwitches = 3
const isFullScreen = ref(false)
const showSecurityWarning = ref(false)
const securityWarningMessage = ref('')

const handleVisibilityChange = () => {
  if (document.hidden && exam.value && !submitted.value) {
    switchCount.value++
    if (switchCount.value >= maxSwitches) {
      submitExam()
      showToast({
        message: '您切换标签页次数过多，答卷已自动提交。',
        type: 'error',
        duration: 5000
      })
    } else {
      securityWarningMessage.value = `警告：考试期间禁止切换标签页。您还有 ${maxSwitches - switchCount.value} 次机会。`
      showSecurityWarning.value = true
    }
  }
}

const enterFullScreen = async () => {
  try {
    await document.documentElement.requestFullscreen()
    isFullScreen.value = true
  } catch (e) {
    console.error('Failed to enter full screen', e)
  }
}

const checkFullScreen = () => {
  if (!document.fullscreenElement && exam.value && !submitted.value) {
    isFullScreen.value = false
  } else {
    isFullScreen.value = true
  }
}

onMounted(() => {
  document.addEventListener('visibilitychange', handleVisibilityChange)
  document.addEventListener('fullscreenchange', checkFullScreen)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  document.removeEventListener('fullscreenchange', checkFullScreen)
})

const displayItems = computed<DisplayItem[]>(() => {
  // Log for debugging
  console.log('Exam Paper Items:', exam.value?.paper.items)

  if (exam.value?.paper.items && exam.value.paper.items.length > 0) {
    return exam.value.paper.items.map((item) => {
      if (item.type === 'SECTION') {
        return { type: 'SECTION', title: item.sectionTitle || 'Section' }
      } else {
        const q = exam.value?.paper.questions.find((q) => q.id === item.id)
        return { type: 'QUESTION', data: q, score: item.score }
      }
    })
  } else if (exam.value?.paper.questions) {
    return exam.value.paper.questions.map((q) => ({ type: 'QUESTION', data: q }))
  }
  return []
})

interface SectionData {
  title: string
  items: { item: DisplayItem; questionNumber: number }[]
}

const sections = computed<SectionData[]>(() => {
  const result: SectionData[] = []
  const allItems = displayItems.value
  if (!allItems || allItems.length === 0) return []

  let currentTitle = 'General'
  let currentItems: { item: DisplayItem; questionNumber: number }[] = []
  let questionCounter = 0

  let i = 0
  const firstItem = allItems[0]
  if (firstItem && firstItem.type === 'SECTION') {
    currentTitle = firstItem.title || 'Section 1'
    i = 1
  }

  for (; i < allItems.length; i++) {
    const item = allItems[i]
    if (!item) continue

    if (item.type === 'SECTION') {
      result.push({ title: currentTitle, items: currentItems })
      currentTitle = item.title || 'Section'
      currentItems = []
    } else {
      questionCounter++
      if (item.type === 'QUESTION' && item.data) {
        currentItems.push({ item: item, questionNumber: questionCounter })
      }
    }
  }
  result.push({ title: currentTitle, items: currentItems })

  return result
})

const displayedSections = computed(() => {
  if (submitted.value) {
    return sections.value
  }
  if (sections.value.length === 0) return []
  return sections.value[currentSectionIndex.value]
    ? [sections.value[currentSectionIndex.value]]
    : []
})

const nextSection = () => {
  if (currentSectionIndex.value < sections.value.length - 1) {
    currentSectionIndex.value++
    window.scrollTo(0, 0)
  }
}

const prevSection = () => {
  if (currentSectionIndex.value > 0) {
    currentSectionIndex.value--
    window.scrollTo(0, 0)
  }
}

const renderMath = () => {
  nextTick(() => {
    const formulas = document.querySelectorAll('.ql-formula')
    formulas.forEach((el) => {
      const latex = el.getAttribute('data-value')
      if (latex && !el.hasAttribute('data-rendered')) {
        try {
          katex.render(latex, el as HTMLElement, {
            throwOnError: false
          })
          el.setAttribute('data-rendered', 'true')
        } catch (e) {
          console.error(e)
        }
      }
    })
  })
}

onUpdated(() => {
  renderMath()
})

const startExam = async () => {
  loading.value = true
  error.value = ''
  try {
    await enterFullScreen()

    const pId = parseInt(paperId.value)
    if (isNaN(pId)) {
      error.value = '试卷ID无效，请输入有效数字'
      return
    }

    const token = localStorage.getItem('token')
    const requestBody: { paperId: number; userId: string; planId?: string } = {
      paperId: pId,
      userId: userId.value
    }

    // 如果有考试计划ID，添加到请求中
    if (planId.value) {
      requestBody.planId = planId.value
    }

    const response = await axios.post('/api/exams/start', requestBody, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    exam.value = response.data
    // Initialize answers
    exam.value?.paper.questions.forEach((q) => {
      if (q.type === 'MULTIPLE_CHOICE' || q.type === 'MULTI_CHOICE') {
        answers.value[q.id] = []
      } else if (q.type === 'FILL_BLANK') {
        // Initialize with empty strings for each blank found in stem
        // Use a more flexible regex for blanks (2 or more underscores)
        const stem = q.stem || ''
        const matches = stem.match(/__+/g)
        const blankCount = matches ? matches.length : 1
        answers.value[q.id] = new Array(blankCount).fill('')
      } else {
        answers.value[q.id] = ''
      }
    })
    renderMath()
  } catch (err) {
    error.value = 'Failed to start exam. Please check the Paper ID.'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const submitExam = async () => {
  if (!exam.value) return
  loading.value = true
  try {
    const submission: Record<string, string> = {}
    Object.entries(answers.value).forEach(([qId, ans]) => {
      if (Array.isArray(ans)) {
        submission[qId] = ans.join(',')
      } else {
        submission[qId] = ans as string
      }
    })

    const token = localStorage.getItem('token')
    const payload = {
      answers: submission,
      flaggedQuestions: Array.from(flaggedQuestions.value)
    }
    const response = await axios.post(`/api/exams/${exam.value.id}/submit`, payload, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    exam.value = response.data
    submitted.value = true

    // Exit full screen
    if (document.fullscreenElement) {
      await document.exitFullscreen()
    }
  } catch (err) {
    error.value = '提交失败，请重试'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const exitExam = () => {
  router.push('/')
}

const getQuestionStatus = (qId: string) => {
  if (!exam.value?.records) return null
  const record = exam.value.records.find((r) => r.questionId === qId)
  if (!record) return 'unanswered'
  if (record.isCorrect === null || record.isCorrect === undefined) return 'pending'
  return record.isCorrect ? 'correct' : 'incorrect'
}

const isSelected = (qId: string, option: string) => {
  const ans = answers.value[qId]
  if (Array.isArray(ans)) {
    return ans.includes(option)
  }
  return ans === option
}

const selectOption = (qId: string, option: string, type: string) => {
  if (type === 'MULTIPLE_CHOICE') {
    // 多选题：切换选项
    if (!Array.isArray(answers.value[qId])) {
      answers.value[qId] = []
    }
    const arr = answers.value[qId] as string[]
    const idx = arr.indexOf(option)
    if (idx >= 0) {
      arr.splice(idx, 1)
    } else {
      arr.push(option)
    }
  } else {
    // 单选题/判断题：直接赋值
    answers.value[qId] = option
  }
}

const questionTypeLabels: Record<string, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  MULTI_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题',
  ESSAY: '论述题'
}
</script>

<template>
  <div class="container">
    <div v-if="!exam" class="google-card start-card">
      <div class="card-header">
        <h1 class="page-title">开始考试</h1>
        <p class="subtitle">{{ planId ? '点击下方按钮开始答题' : '输入试卷编号开始考试' }}</p>
      </div>

      <form @submit.prevent="startExam">
        <div class="form-group" v-if="!planId">
          <label class="field-label">试卷编号</label>
          <input
            v-model="paperId"
            type="text"
            required
            placeholder="请输入试卷编号"
            class="google-input"
          />
        </div>
        <div class="form-group">
          <label class="field-label">考生</label>
          <input
            v-if="authState.isAuthenticated"
            :value="authState.user.nickname || authState.user.username"
            type="text"
            class="google-input"
            disabled
          />
          <input v-else v-model="userId" type="text" required class="google-input" />
        </div>

        <div class="form-actions">
          <button type="submit" :disabled="loading" class="google-btn primary-btn full-width">
            {{ loading ? '正在加载...' : '开始答题' }}
          </button>
        </div>

        <div v-if="error" class="message error">
          <span class="material-icon">error</span>
          {{ error }}
        </div>
      </form>
    </div>

    <div v-else class="exam-screen">
      <div class="google-card exam-header-card">
        <div class="header-content">
          <h2>{{ exam.paper.title }}</h2>
        </div>
        <div v-if="submitted" class="score-badge">
          <template v-if="exam.score !== null && exam.score !== undefined">
            <span class="score-label">得分</span>
            <span class="score-value">{{ exam.score }}</span>
            <span class="score-total">/ 100</span>
          </template>
          <template v-else>
            <span class="score-label">状态</span>
            <span class="score-pending">待阅卷</span>
          </template>
        </div>
      </div>

      <div class="questions-list">
        <div v-for="(section, sIndex) in displayedSections" :key="sIndex" class="section-container">
          <div class="section-header" v-if="section">
            <h3>{{ section.title }}</h3>
          </div>

          <template v-for="(itemData, index) in section?.items" :key="index">
            <div
              v-if="itemData.item.type === 'QUESTION' && itemData.item.data"
              class="google-card question-card"
              :class="submitted ? getQuestionStatus(itemData.item.data.id) : ''"
            >
              <div class="question-header">
                <span class="q-number">第 {{ itemData.questionNumber }} 题</span>
                <span class="q-type-badge">{{
                  questionTypeLabels[itemData.item.data.type] || itemData.item.data.type
                }}</span>
                <span
                  v-if="submitted && getQuestionStatus(itemData.item.data.id)"
                  class="status-badge"
                  :class="getQuestionStatus(itemData.item.data.id)"
                >
                  {{
                    getQuestionStatus(itemData.item.data.id) === 'correct'
                      ? '正确'
                      : getQuestionStatus(itemData.item.data.id) === 'pending'
                        ? '待批阅'
                        : '错误'
                  }}
                </span>
              </div>

              <div
                class="question-content"
                v-if="itemData.item.data.stem"
                v-html="itemData.item.data.stem"
              ></div>
              <div class="question-content error-text" v-else>
                <span v-if="itemData.item.data.type === 'FILL_BLANK'">[填空题]</span>
                <span v-else>(错误：题目内容缺失)</span>
              </div>

              <div class="options-grid">
                <template
                  v-if="
                    ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE'].includes(
                      itemData.item.data.type
                    )
                  "
                >
                  <div
                    v-if="
                      (!itemData.item.data.options || itemData.item.data.options.length === 0) &&
                      itemData.item.data.type !== 'TRUE_FALSE'
                    "
                    class="no-options-warning"
                  >
                    (错误：没有可用选项)
                  </div>
                  <template v-else>
                    <div
                      v-for="option in itemData.item.data.options &&
                      itemData.item.data.options.length > 0
                        ? itemData.item.data.options
                        : ['True', 'False']"
                      :key="option"
                      class="option-item"
                      :class="{
                        selected: isSelected(itemData.item.data.id, option),
                        disabled: submitted
                      }"
                      @click="
                        !submitted &&
                        selectOption(itemData.item.data.id, option, itemData.item.data.type)
                      "
                    >
                      <div class="input-wrapper">
                        <CheckSquare
                          v-if="isSelected(itemData.item.data.id, option)"
                          :size="20"
                          class="check-icon checked"
                        />
                        <Square v-else :size="20" class="check-icon" />
                      </div>
                      <span class="option-text">{{
                        itemData.item.data.type === 'TRUE_FALSE'
                          ? option === 'True'
                            ? '正确'
                            : '错误'
                          : option
                      }}</span>
                    </div>
                  </template>
                </template>
                <template v-else-if="itemData.item.data.type === 'FILL_BLANK'">
                  <div class="fill-blank-container">
                    <div
                      v-for="(_ans, idx) in answers[itemData.item.data.id]"
                      :key="idx"
                      class="blank-input-row"
                    >
                      <span class="blank-label">{{ idx + 1 }}.</span>
                      <input
                        type="text"
                        v-model="(answers[itemData.item.data.id] as string[])[idx]"
                        class="google-input blank-input"
                        :disabled="submitted"
                        placeholder="请填写答案"
                      />
                    </div>
                  </div>
                </template>
                <template v-else>
                  <textarea
                    v-model="answers[itemData.item.data.id]"
                    class="google-input"
                    rows="3"
                    :disabled="submitted"
                    placeholder="请输入您的答案..."
                    style="width: 100%; margin-top: 10px"
                  ></textarea>
                </template>
              </div>
            </div>
          </template>
        </div>
      </div>

      <div class="exam-actions">
        <button
          v-if="currentSectionIndex > 0 && !submitted"
          @click="prevSection"
          class="google-btn text-btn large-btn"
        >
          上一部分
        </button>
        <div class="spacer"></div>
        <button
          v-if="currentSectionIndex < sections.length - 1 && !submitted"
          @click="nextSection"
          class="google-btn primary-btn large-btn"
        >
          下一部分
        </button>
        <button
          v-else-if="!submitted"
          @click="submitExam"
          :disabled="loading"
          class="google-btn primary-btn large-btn"
        >
          {{ loading ? '正在提交...' : '提交答卷' }}
        </button>
        <button v-else @click="exitExam" class="google-btn text-btn large-btn">退出考试</button>
      </div>

      <!-- Security Overlays -->
      <div v-if="exam && !submitted && !isFullScreen" class="security-overlay">
        <div class="security-dialog google-card">
          <h3>考试安全提示</h3>
          <p>请保持全屏模式继续考试</p>
          <button @click="enterFullScreen" class="google-btn primary-btn">返回全屏</button>
        </div>
      </div>

      <div v-if="showSecurityWarning" class="security-overlay">
        <div class="security-dialog google-card warning">
          <h3>安全警告</h3>
          <p>{{ securityWarningMessage }}</p>
          <button @click="showSecurityWarning = false" class="google-btn primary-btn">
            我知道了
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.start-card {
  max-width: 500px;
  margin: 40px auto;
  padding: 40px;
  background: var(--line-card-bg);
  border-radius: var(--line-radius-lg);
  box-shadow: var(--line-shadow-md);
  border: 1px solid var(--line-border);
  animation: slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.card-header h1 {
  margin-bottom: 8px;
  color: var(--line-text-primary);
  letter-spacing: -0.5px;
}

.subtitle {
  color: var(--line-text-secondary);
  font-size: 16px;
}

.form-group {
  margin-bottom: 24px;
}

.field-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  font-size: 14px;
  color: var(--line-text-primary);
}

.google-input {
  width: 100%;
  padding: 12px 16px;
  font-size: 16px;
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-md);
  transition: all 0.2s;
  background: var(--line-bg-soft);
  color: var(--line-text-primary);
}

.google-input:focus {
  border-color: var(--line-primary);
  background: var(--line-card-bg);
  box-shadow: 0 0 0 2px var(--line-primary-10);
  outline: none;
}

.full-width {
  width: 100%;
  justify-content: center;
  padding: 14px;
  font-size: 16px;
}

.message {
  margin-top: 24px;
  padding: 16px;
  border-radius: var(--line-radius-md);
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  font-weight: 500;
}

.error {
  background-color: #fef2f2;
  color: #ef4444;
  border: 1px solid #fee2e2;
}

.exam-screen {
  max-width: 900px;
  margin: 0 auto;
  animation: fadeIn 0.5s ease-out;
}

.exam-header-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px;
  margin-bottom: 24px;
  background: var(--line-card-bg);
  border-radius: var(--line-radius-lg);
  border: 1px solid var(--line-border);
  box-shadow: var(--line-shadow-sm);
}

.exam-header-card h2 {
  font-family:
    system-ui,
    -apple-system,
    sans-serif;
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 4px;
  color: var(--line-text-primary);
}

.score-badge {
  background-color: var(--line-bg-soft);
  padding: 16px 32px;
  border-radius: var(--line-radius-lg);
  text-align: center;
  border: 1px solid var(--line-border);
  box-shadow: none;
}

.score-label {
  display: block;
  font-size: 14px;
  color: var(--line-text-secondary);
  font-weight: 600;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.score-value {
  font-size: 36px;
  font-weight: 700;
  color: var(--line-primary);
  line-height: 1;
}

.score-total {
  font-size: 18px;
  color: var(--line-text-secondary);
  font-weight: 400;
}

.score-pending {
  font-size: 20px;
  font-weight: 600;
  color: #f9ab00;
}

.question-card {
  padding: 40px;
  margin-bottom: 24px;
  background: var(--line-card-bg);
  border-radius: var(--line-radius-lg);
  border: 1px solid var(--line-border);
  box-shadow: var(--line-shadow-sm);
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}

.section-header {
  padding: 32px 0 16px 0;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--line-text-primary);
  border-left: 4px solid var(--line-primary);
  padding-left: 16px;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.q-number {
  font-size: 16px;
  font-weight: 600;
  color: var(--line-text-secondary);
}

.q-type-badge {
  font-size: 12px;
  background: var(--line-primary-10);
  color: var(--line-primary);
  padding: 4px 10px;
  border-radius: var(--line-radius-full);
  margin-left: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.status-badge {
  padding: 6px 14px;
  border-radius: var(--line-radius-full);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.status-badge.correct {
  background-color: #d1fae5;
  color: #059669;
}

.status-badge.incorrect {
  background-color: #fee2e2;
  color: #dc2626;
}

.status-badge.pending {
  background-color: #fef3c7;
  color: #d97706;
}

.question-content {
  font-size: 18px;
  line-height: 1.6;
  color: var(--line-text-primary);
  margin-bottom: 24px;
}

.options-grid {
  display: grid;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-md);
  cursor: pointer;
  transition: all 0.2s;
  background: var(--line-bg-soft);
}

.option-item:hover {
  border-color: var(--line-primary);
  background: var(--line-card-bg);
  transform: translateY(-1px);
}

.option-item.selected {
  background-color: var(--line-primary-10);
  border-color: var(--line-primary);
  color: var(--line-primary);
}

.input-wrapper {
  margin-right: 16px;
  display: flex;
  align-items: center;
}

.option-text {
  font-size: 16px;
  color: var(--line-text-primary);
}

.option-item.selected .option-text {
  color: var(--line-primary);
}

.option-item.disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.check-icon {
  color: #5f6368;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.check-icon.checked {
  color: var(--line-primary);
}

.option-item:hover .check-icon {
  color: var(--line-primary);
}

.exam-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 40px;
  padding-bottom: 64px;
}

.spacer {
  flex: 1;
}

.large-btn {
  padding: 14px 36px;
  font-size: 16px;
  border-radius: var(--line-radius-full);
  letter-spacing: 0.5px;
}

.google-btn {
  border: none;
  border-radius: var(--line-radius-md);
  padding: 10px 24px;
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.primary-btn {
  background-color: var(--line-primary);
  color: white;
  box-shadow: 0 2px 4px rgba(14, 165, 233, 0.2);
}

.primary-btn:hover {
  background-color: var(--line-primary-hover);
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.3);
  transform: translateY(-1px);
}

.text-btn {
  background-color: transparent;
  color: var(--line-text-secondary);
}

.text-btn:hover {
  color: var(--line-primary);
  background-color: var(--line-bg-soft);
}

.fill-blank-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 24px;
}

.blank-input-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.blank-label {
  font-weight: 600;
  color: var(--line-text-secondary);
  min-width: 32px;
}

.blank-input {
  max-width: 400px;
  border: 1px solid var(--line-border);
  padding: 12px;
  border-radius: var(--line-radius-md);
  background: var(--line-bg-soft);
}

.blank-input:focus {
  border-color: var(--line-primary);
  background: var(--line-card-bg);
  outline: none;
}

.security-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.9);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.security-dialog {
  background: var(--line-card-bg);
  padding: 40px;
  border-radius: var(--line-radius-lg);
  text-align: center;
  max-width: 450px;
  border: 1px solid var(--line-border);
  box-shadow: var(--line-shadow-xl);
}

.security-dialog.warning h3 {
  color: #dc2626;
}

.security-dialog h3 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 22px;
  font-weight: 600;
  color: var(--line-text-primary);
}

.security-dialog p {
  margin-bottom: 32px;
  color: var(--line-text-secondary);
  line-height: 1.6;
}

.error-text {
  color: #dc2626;
  font-style: italic;
  padding: 12px 16px;
  background: #fef2f2;
  border-radius: var(--line-radius-md);
  margin-top: 8px;
  border: 1px solid #fee2e2;
}

.no-options-warning {
  color: #b45309;
  font-style: italic;
  padding: 12px 16px;
  background: #fffbeb;
  border-radius: var(--line-radius-md);
  margin-bottom: 12px;
  border: 1px solid #fef3c7;
}

@media (max-width: 768px) {
  .exam-header-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
    padding: 20px;
  }

  .score-badge {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 20px;
  }

  .score-label {
    display: inline;
    margin-right: 8px;
    margin-bottom: 0;
  }

  .question-card {
    padding: 24px;
  }

  .exam-actions {
    flex-direction: column;
    gap: 16px;
  }

  .large-btn {
    width: 100%;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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
