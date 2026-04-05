<script setup lang="ts">
import { ref, onMounted, onUpdated, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { useToast } from '@/composables/useToast'
import 'katex/dist/katex.min.css'
import katex from 'katex'

interface Question {
  id: string
  stem: string
  type: string
  difficulty: string
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

type DisplayItem =
  | { type: 'SECTION'; title?: string }
  | { type: 'QUESTION'; data?: Question; score?: number }

const route = useRoute()
const router = useRouter()
const { showToast } = useToast()
const paper = ref<Paper | null>(null)
const loading = ref(true)
const error = ref('')

// 题型映射
const typeLabels: Record<string, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题',
  ESSAY: '论述题'
}

// 难度映射
const difficultyLabels: Record<string, string> = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难'
}

const displayItems = computed<DisplayItem[]>(() => {
  if (paper.value?.items && paper.value.items.length > 0) {
    return paper.value.items.map((item) => {
      if (item.type === 'SECTION') {
        return { type: 'SECTION', title: item.sectionTitle }
      } else {
        const q = paper.value?.questions.find((q) => q.id === item.id)
        return { type: 'QUESTION', data: q, score: item.score }
      }
    })
  } else if (paper.value?.questions) {
    return paper.value.questions.map((q) => ({ type: 'QUESTION', data: q }))
  }
  return []
})

const getQuestionIndex = (currentIndex: number) => {
  let count = 0
  for (let i = 0; i <= currentIndex; i++) {
    if (displayItems.value[i]?.type === 'QUESTION') {
      count++
    }
  }
  return count
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

onMounted(async () => {
  const id = route.params.id
  if (!id || id === 'undefined' || id === 'null') {
    error.value = 'Invalid Paper ID'
    loading.value = false
    return
  }
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get(`/api/papers/${id}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    paper.value = response.data
    renderMath()
  } catch (err) {
    error.value = 'Failed to load paper.'
    console.error(err)
  } finally {
    loading.value = false
  }
})

onUpdated(() => {
  renderMath()
})

const editPaper = () => {
  if (paper.value) {
    router.push(`/papers/${paper.value.id}/edit`)
  }
}

const startExam = () => {
  if (paper.value) {
    router.push({ name: 'exam', query: { paperId: paper.value.id } })
  }
}

const download = async (type: 'teacher' | 'student' | 'answer-sheet') => {
  const id = route.params.id
  const token = localStorage.getItem('token')
  let url = `/api/papers/${id}/export/`
  if (type === 'answer-sheet') {
    url += 'answer-sheet'
  } else {
    url += `word?teacher=${type === 'teacher'}`
  }

  try {
    const response = await axios.get(url, {
      headers: { Authorization: `Bearer ${token}` },
      responseType: 'blob'
    })
    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `paper_${id}_${type}.docx`
    link.click()
    URL.revokeObjectURL(link.href)
  } catch (err) {
    console.error('Download failed', err)
    showToast({ message: '下载失败', type: 'error' })
  }
}
</script>

<template>
  <div class="container">
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading paper...</p>
    </div>

    <div v-else-if="error" class="error-state google-card">
      <p>{{ error }}</p>
      <button @click="router.go(0)" class="google-btn">Try Again</button>
    </div>

    <div v-else-if="paper" class="google-card paper-card">
      <div class="paper-header">
        <div class="header-top">
          <div class="title-container">
            <span class="paper-title">{{ paper.title }}</span>
          </div>
          <div class="header-actions">
            <span class="paper-count">{{ paper.questions.length }} 道题目</span>
            <button @click="editPaper" class="google-btn text-btn">编辑试卷</button>
          </div>
        </div>
        <div class="export-bar">
          <span class="export-label">导出试卷：</span>
          <div class="export-buttons">
            <button @click="download('student')" class="export-btn" title="导出学生版">
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
              </svg>
              <span>学生版</span>
            </button>
            <button @click="download('teacher')" class="export-btn" title="导出教师版">
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M22 10v6M2 10l10-5 10 5-10 5z"></path>
                <path d="M6 12v5c3 3 9 3 12 0v-5"></path>
              </svg>
              <span>教师版</span>
            </button>
            <button @click="download('answer-sheet')" class="export-btn" title="导出答题卡">
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                <line x1="9" y1="9" x2="15" y2="9"></line>
                <line x1="9" y1="15" x2="15" y2="15"></line>
              </svg>
              <span>答题卡</span>
            </button>
          </div>
        </div>
      </div>

      <div class="questions-list">
        <template v-for="(item, index) in displayItems" :key="index">
          <div v-if="item.type === 'SECTION'" class="section-header">
            <h3>{{ item.title }}</h3>
          </div>
          <div v-else-if="item.type === 'QUESTION' && item.data" class="question-item">
            <div class="q-index">{{ getQuestionIndex(index) }}</div>
            <div class="q-content">
              <div class="q-meta">
                <span class="chip type">{{ typeLabels[item.data.type] || item.data.type }}</span>
                <span class="chip difficulty" :class="item.data.difficulty.toLowerCase()">{{
                  difficultyLabels[item.data.difficulty] || item.data.difficulty
                }}</span>
                <span v-if="item.score" class="chip score">{{ item.score }} 分</span>
              </div>
              <div class="stem" v-html="item.data.stem"></div>
            </div>
          </div>
        </template>
      </div>

      <div class="paper-footer">
        <button @click="startExam" class="google-btn primary-btn start-exam-btn">开始考试</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.paper-card {
  max-width: 800px;
  margin: 0 auto;
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--line-border);
}

.paper-header {
  padding: 24px 32px;
  background-color: var(--line-bg);
  border-bottom: 1px solid var(--line-border);
}

.header-top {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  margin-bottom: 20px;
}

.title-container {
  grid-column: 2;
  text-align: center;
}

.header-actions {
  grid-column: 3;
  justify-self: end;
  display: flex;
  align-items: center;
  gap: 12px;
}

.paper-title {
  font-family: 'Google Sans', sans-serif;
  font-size: 24px;
  font-weight: 500;
  color: var(--line-text);
}

.paper-count {
  background: rgba(26, 115, 232, 0.1);
  color: var(--line-primary);
  padding: 6px 14px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
}

.export-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: var(--line-bg-soft);
  border-radius: 8px;
}

.export-label {
  font-size: 13px;
  color: var(--line-text-secondary);
  white-space: nowrap;
}

.export-buttons {
  display: flex;
  gap: 8px;
}

.export-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--line-bg);
  border: 1px solid var(--line-border);
  border-radius: var(--radius);
  cursor: pointer;
  font-size: 13px;
  color: var(--line-text);
  transition: all 0.2s;
}

.export-btn:hover {
  border-color: var(--line-primary);
  color: var(--line-primary);
  background: rgba(26, 115, 232, 0.04);
}

.export-btn svg {
  flex-shrink: 0;
}

.questions-list {
  padding: 0;
}

.question-item {
  display: flex;
  gap: 16px;
  padding: 24px 32px;
  border-bottom: 1px solid var(--line-border);
  background: var(--line-bg);
}

.question-item:last-child {
  border-bottom: none;
}

.q-index {
  font-size: 18px;
  font-weight: 600;
  color: var(--line-primary);
  min-width: 28px;
  padding-top: 2px;
}

.q-content {
  flex: 1;
}

.q-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.chip {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  background-color: var(--line-bg-soft);
  color: var(--line-text);
}

.chip.type {
  background-color: rgba(26, 115, 232, 0.08);
  color: var(--line-primary);
}

.chip.score {
  background-color: var(--line-bg-soft);
  color: var(--line-text-secondary);
}

.section-header {
  padding: 24px 32px;
  background-color: var(--line-bg-soft);
  border-bottom: 1px solid var(--line-border);
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: var(--line-text);
}

.chip.difficulty.easy {
  background-color: rgba(52, 168, 83, 0.1);
  color: var(--line-success);
}
.chip.difficulty.medium {
  background-color: rgba(251, 188, 4, 0.15);
  color: #b06000;
}
.chip.difficulty.hard {
  background-color: rgba(234, 67, 53, 0.1);
  color: var(--line-error);
}

.stem {
  font-size: 16px;
  line-height: 1.6;
  color: var(--line-text);
}

.loading-state,
.error-state {
  text-align: center;
  padding: 40px;
  color: var(--line-text-secondary);
}

.paper-footer {
  padding: 16px 32px;
  background-color: var(--line-bg);
  border-top: none;
  display: flex;
  justify-content: flex-end;
}

.start-exam-btn {
  width: 100%;
}

.google-btn {
  border: none;
  border-radius: var(--radius);
  padding: 8px 24px;
  font-family: 'Google Sans', sans-serif;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.primary-btn {
  background-color: var(--line-primary);
  color: white;
}

.primary-btn:hover {
  background-color: #1557b0;
  box-shadow:
    0 1px 2px 0 rgba(60, 64, 67, 0.3),
    0 1px 3px 1px rgba(60, 64, 67, 0.15);
}

.text-btn {
  background-color: transparent;
  color: var(--line-primary);
}

.text-btn:hover {
  background-color: rgba(26, 115, 232, 0.04);
}

.material-icons {
  font-size: 18px;
}

@media (max-width: 768px) {
  .paper-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
    padding: 24px;
  }

  .paper-header button {
    width: 100%;
  }

  .question-item {
    padding: 20px 24px;
  }
}
</style>
