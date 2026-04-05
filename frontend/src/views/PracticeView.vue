<script setup lang="ts">
import { ref, onUpdated, nextTick, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import 'katex/dist/katex.min.css'
import katex from 'katex'
import { authState } from '@/states/authState'
import { Flag, Square, CheckSquare } from 'lucide-vue-next'

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
  isFlagged?: boolean
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

// 题目类型标签映射
const typeLabels: Record<string, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题'
}
const paperId = ref(route.params.id?.toString() || '')
const userId = ref(
  authState.isAuthenticated ? authState.user.username : 'user-' + Math.floor(Math.random() * 1000)
)
const exam = ref<Exam | null>(null)
const answers = ref<Record<string, string | string[]>>({})
const flaggedQuestions = ref<Set<string>>(new Set())
const loading = ref(false)
const error = ref('')
const submitted = ref(false)
const currentSectionIndex = ref(0)

const displayItems = computed<DisplayItem[]>(() => {
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

const startPractice = async () => {
  loading.value = true
  error.value = ''
  try {
    const pId = parseInt(paperId.value)
    if (isNaN(pId)) {
      error.value = '无效的试卷ID'
      return
    }

    const token = localStorage.getItem('token')
    const response = await axios.post(
      '/api/exams/start',
      {
        paperId: pId,
        userId: userId.value,
        type: 'PRACTICE'
      },
      {
        headers: {
          Authorization: `Bearer ${token}`
        }
      }
    )
    exam.value = response.data
    // Initialize answers
    exam.value?.paper.questions.forEach((q) => {
      if (q.type === 'MULTIPLE_CHOICE') {
        answers.value[q.id] = []
      } else if (q.type === 'FILL_BLANK') {
        const stem = q.stem || ''
        const matches = stem.match(/__+/g)
        const blankCount = matches ? matches.length : 1
        answers.value[q.id] = new Array(blankCount).fill('')
      } else {
        answers.value[q.id] = ''
      }
    })
    renderMath()
  } catch (err: any) {
    if (err.response?.status === 404) {
      error.value = '试卷不存在，请返回选择其他试卷'
    } else if (err.response?.status === 401) {
      error.value = '请先登录后再开始练习'
    } else {
      error.value = '无法开始练习，请稍后重试'
    }
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
  } catch (err) {
    error.value = 'Failed to submit practice.'
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
  return record ? (record.isCorrect ? 'correct' : 'incorrect') : 'unanswered'
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

const toggleFlag = (qId: string) => {
  if (flaggedQuestions.value.has(qId)) {
    flaggedQuestions.value.delete(qId)
  } else {
    flaggedQuestions.value.add(qId)
  }
}

const isFlagged = (qId: string) => {
  if (submitted.value) {
    const record = exam.value?.records?.find((r) => r.questionId === qId)
    return record?.isFlagged
  }
  return flaggedQuestions.value.has(qId)
}

const buildQuestionDragPayload = (question: Question) => {
  const userRaw = answers.value[question.id]
  const userAnswer = Array.isArray(userRaw) ? userRaw.join(', ') : userRaw || ''
  return {
    source: 'practice',
    questionId: question.id,
    type: question.type,
    stem: question.stem,
    userAnswer: userAnswer || '(未作答)',
    reference: question.options?.length ? question.options.join('\n') : '(无)'
  }
}

const onQuestionDragStart = (event: DragEvent, question: Question) => {
  const dt = event.dataTransfer
  if (!dt) return
  dt.effectAllowed = 'copy'
  const payload = buildQuestionDragPayload(question)
  dt.setData('application/x-uqbank-question', JSON.stringify(payload))
  dt.setData(
    'text/plain',
    `题目ID: ${payload.questionId}\n题型: ${payload.type}\n题干: ${payload.stem}\n学生答案: ${payload.userAnswer}`
  )
}

onMounted(() => {
  if (paperId.value) {
    startPractice()
  }
})
</script>

<template>
  <div class="container">
    <div v-if="loading && !exam" class="text-center mt-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">加载中...</span>
      </div>
    </div>

    <div v-else-if="error" class="error-container">
      <div class="error-card">
        <div class="error-icon">
          <svg
            width="48"
            height="48"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
          >
            <circle cx="12" cy="12" r="10" />
            <path d="M12 8v4" />
            <path d="M12 16h.01" />
          </svg>
        </div>
        <h3 class="error-title">{{ error }}</h3>
        <div class="error-actions">
          <button class="back-btn" @click="router.push('/practice')">
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            返回试卷列表
          </button>
          <button class="retry-btn" @click="startPractice">重试</button>
        </div>
      </div>
    </div>

    <div v-else-if="exam" class="exam-screen">
      <div class="google-card exam-header-card">
        <div class="header-content">
          <h2>{{ exam.paper.title }} (练习模式)</h2>
          <p class="subtitle">按自己的节奏练习。可以标记题目留待复习。</p>
        </div>
        <div v-if="submitted" class="score-badge">
          <span class="score-label">得分</span>
          <span class="score-value">{{ exam.score }}</span>
          <span class="score-total">/ 100</span>
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
              draggable="true"
              @dragstart="onQuestionDragStart($event, itemData.item.data)"
            >
              <div class="question-header">
                <div class="q-info">
                  <span class="q-number">第 {{ itemData.questionNumber }} 题</span>
                  <span class="q-type-badge">{{
                    typeLabels[itemData.item.data.type] || itemData.item.data.type
                  }}</span>
                  <span
                    v-if="submitted"
                    class="status-badge"
                    :class="getQuestionStatus(itemData.item.data.id)"
                  >
                    {{ getQuestionStatus(itemData.item.data.id) === 'correct' ? '正确' : '错误' }}
                  </span>
                </div>
                <button
                  class="flag-btn"
                  :class="{ flagged: isFlagged(itemData.item.data.id) }"
                  @click="toggleFlag(itemData.item.data.id)"
                  :disabled="submitted"
                >
                  <Flag :size="18" />
                  {{ isFlagged(itemData.item.data.id) ? '已标记' : '标记' }}
                </button>
              </div>

              <div
                class="question-content"
                v-if="itemData.item.data.stem"
                v-html="itemData.item.data.stem"
              ></div>

              <div class="options-grid">
                <template
                  v-if="
                    ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(
                      itemData.item.data.type
                    )
                  "
                >
                  <template
                    v-if="itemData.item.data.options && itemData.item.data.options.length > 0"
                  >
                    <div
                      v-for="option in itemData.item.data.options"
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
                      <span class="option-text">{{ option }}</span>
                    </div>
                  </template>
                  <template v-else-if="itemData.item.data.type === 'TRUE_FALSE'">
                    <div
                      v-for="option in ['True', 'False']"
                      :key="option"
                      class="option-item"
                      :class="{
                        selected: isSelected(itemData.item.data.id, option),
                        disabled: submitted
                      }"
                      @click="
                        !submitted && selectOption(itemData.item.data.id, option, 'SINGLE_CHOICE')
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
                      <span class="option-text">{{ option === 'True' ? '正确' : '错误' }}</span>
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
                    placeholder="请输入你的答案..."
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
          {{ loading ? '提交中...' : '提交练习' }}
        </button>
        <button v-else @click="exitExam" class="google-btn text-btn large-btn">退出练习</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Reuse styles from ExamView mostly */
.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}
.exam-header-card {
  background: var(--line-bg);
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.question-card {
  background: var(--line-bg);
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
  margin-bottom: 16px;
}
.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.q-info {
  display: flex;
  align-items: center;
}
.q-number {
  font-weight: 500;
  color: var(--line-text-secondary);
  margin-right: 10px;
}
.q-type-badge {
  background: rgba(26, 115, 232, 0.1);
  color: var(--line-primary);
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}
.status-badge {
  margin-left: 10px;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}
.status-badge.correct {
  background: #e6f4ea;
  color: #137333;
}
.status-badge.incorrect {
  background: #fce8e6;
  color: #c5221f;
}
.flag-btn {
  background: none;
  border: 1px solid var(--line-border);
  border-radius: 4px;
  padding: 4px 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--line-text-secondary);
}
.flag-btn.flagged {
  background: #ffe0b2;
  color: #e37400;
  border-color: var(--line-warning);
}
.flag-btn:hover {
  background: var(--line-bg-soft);
}
.options-grid {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.option-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid var(--line-border);
  margin-bottom: 8px;
  transition: all 0.2s;
  background: var(--line-bg);
  user-select: none;
}
.option-item:hover {
  background: #f8f9fa;
  border-color: var(--line-primary);
}
.option-item.selected {
  background: #e8f0fe;
  border-color: var(--line-primary);
}
.option-item.disabled {
  cursor: not-allowed;
  opacity: 0.7;
}
.input-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
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
.option-item.selected .option-text {
  color: var(--line-primary);
}
.option-text {
  color: var(--line-text);
  font-size: 14px;
  line-height: 1.4;
}
.google-btn {
  padding: 10px 24px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
}
.primary-btn {
  background: var(--line-primary);
  color: white;
}
.text-btn {
  background: transparent;
  color: var(--line-primary);
}
.exam-actions {
  display: flex;
  margin-top: 32px;
  padding-bottom: 48px;
}
.spacer {
  flex: 1;
}
.score-badge {
  text-align: center;
}
.score-value {
  font-size: 24px;
  color: var(--line-primary);
  font-weight: bold;
}
.score-total {
  color: var(--line-text-secondary);
}
.section-header h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: var(--line-text);
  border-left: 4px solid var(--line-text);
  padding-left: 12px;
}

/* Error state styles */
.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  padding: 40px 20px;
}
.error-card {
  background: var(--line-bg);
  border: 1px solid var(--line-border);
  border-radius: 16px;
  padding: 48px;
  text-align: center;
  max-width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.error-icon {
  color: var(--line-text-secondary);
  margin-bottom: 20px;
}
.error-title {
  font-size: 16px;
  color: var(--line-text);
  margin: 0 0 24px 0;
  font-weight: 500;
}
.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: var(--line-primary);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.back-btn:hover {
  background: #1557b0;
}
.retry-btn {
  padding: 10px 20px;
  background: transparent;
  color: var(--line-primary);
  border: 1px solid var(--line-border);
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.retry-btn:hover {
  background: var(--line-bg-soft);
  border-color: var(--line-primary);
}
</style>
