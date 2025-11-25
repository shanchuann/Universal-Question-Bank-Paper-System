<script setup lang="ts">
import { ref, onUpdated, nextTick, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import 'katex/dist/katex.min.css';
import katex from 'katex';
import { authState } from '@/states/authState'

interface Question {
  id: string
  stem: string
  type: string
  options: string[]
}

interface PaperItem {
  type: 'QUESTION' | 'SECTION';
  id?: string;
  sectionTitle?: string;
  score?: number;
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
  | { type: 'QUESTION'; data?: Question; score?: number };

const route = useRoute()
const router = useRouter()
const paperId = ref(route.params.id?.toString() || '')
const userId = ref(authState.isAuthenticated ? authState.user.username : 'user-' + Math.floor(Math.random() * 1000))
const exam = ref<Exam | null>(null)
const answers = ref<Record<string, string | string[]>>({})
const flaggedQuestions = ref<Set<string>>(new Set())
const loading = ref(false)
const error = ref('')
const submitted = ref(false)
const currentSectionIndex = ref(0)

const displayItems = computed<DisplayItem[]>(() => {
  if (exam.value?.paper.items && exam.value.paper.items.length > 0) {
    return exam.value.paper.items.map(item => {
      if (item.type === 'SECTION') {
        return { type: 'SECTION', title: item.sectionTitle || 'Section' };
      } else {
        const q = exam.value?.paper.questions.find(q => q.id === item.id);
        return { type: 'QUESTION', data: q, score: item.score };
      }
    });
  } else if (exam.value?.paper.questions) {
    return exam.value.paper.questions.map(q => ({ type: 'QUESTION', data: q }));
  }
  return [];
});

interface SectionData {
  title: string;
  items: { item: DisplayItem; questionNumber: number }[];
}

const sections = computed<SectionData[]>(() => {
  const result: SectionData[] = [];
  const allItems = displayItems.value;
  if (!allItems || allItems.length === 0) return [];

  let currentTitle = 'General';
  let currentItems: { item: DisplayItem; questionNumber: number }[] = [];
  let questionCounter = 0;
  
  let i = 0;
  const firstItem = allItems[0];
  if (firstItem && firstItem.type === 'SECTION') {
      currentTitle = firstItem.title || 'Section 1';
      i = 1;
  }

  for (; i < allItems.length; i++) {
      const item = allItems[i];
      if (!item) continue;
      
      if (item.type === 'SECTION') {
          result.push({ title: currentTitle, items: currentItems });
          currentTitle = item.title || 'Section';
          currentItems = [];
      } else {
          questionCounter++;
          if (item.type === 'QUESTION' && item.data) {
             currentItems.push({ item: item, questionNumber: questionCounter });
          }
      }
  }
  result.push({ title: currentTitle, items: currentItems });
  
  return result;
});

const displayedSections = computed(() => {
    if (submitted.value) {
        return sections.value;
    }
    if (sections.value.length === 0) return [];
    return sections.value[currentSectionIndex.value] ? [sections.value[currentSectionIndex.value]] : [];
});

const nextSection = () => {
    if (currentSectionIndex.value < sections.value.length - 1) {
        currentSectionIndex.value++;
        window.scrollTo(0, 0);
    }
}

const prevSection = () => {
    if (currentSectionIndex.value > 0) {
        currentSectionIndex.value--;
        window.scrollTo(0, 0);
    }
}

const renderMath = () => {
  nextTick(() => {
    const formulas = document.querySelectorAll('.ql-formula');
    formulas.forEach((el) => {
      const latex = el.getAttribute('data-value');
      if (latex && !el.hasAttribute('data-rendered')) {
        try {
            katex.render(latex, el as HTMLElement, {
            throwOnError: false
            });
            el.setAttribute('data-rendered', 'true');
        } catch (e) {
            console.error(e);
        }
      }
    });
  });
}

onUpdated(() => {
    renderMath();
})

const startPractice = async () => {
  loading.value = true
  error.value = ''
  try {
    const pId = parseInt(paperId.value)
    if (isNaN(pId)) {
      error.value = 'Invalid Paper ID.'
      return
    }

    const token = localStorage.getItem('token')
    const response = await axios.post('/api/exams/start', {
      paperId: pId,
      userId: userId.value,
      type: 'PRACTICE'
    }, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    exam.value = response.data
    // Initialize answers
    exam.value?.paper.questions.forEach(q => {
      if (q.type === 'MULTIPLE_CHOICE') {
        answers.value[q.id] = []
      } else if (q.type === 'FILL_BLANK') {
        const stem = q.stem || '';
        const matches = stem.match(/__+/g);
        const blankCount = matches ? matches.length : 1;
        answers.value[q.id] = new Array(blankCount).fill('');
      } else {
        answers.value[q.id] = ''
      }
    })
    renderMath();
  } catch (err) {
    error.value = 'Failed to start practice session.'
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
  const record = exam.value.records.find(r => r.questionId === qId)
  return record ? (record.isCorrect ? 'correct' : 'incorrect') : 'unanswered'
}

const isSelected = (qId: string, option: string) => {
  const ans = answers.value[qId]
  if (Array.isArray(ans)) {
    return ans.includes(option)
  }
  return ans === option
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
        const record = exam.value?.records?.find(r => r.questionId === qId)
        return record?.isFlagged
    }
    return flaggedQuestions.value.has(qId)
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
            <span class="visually-hidden">Loading...</span>
        </div>
    </div>
    
    <div v-else-if="error" class="message error">
        {{ error }}
    </div>

    <div v-else-if="exam" class="exam-screen">
      <div class="google-card exam-header-card">
        <div class="header-content">
          <h2>{{ exam.paper.title }} (Practice Mode)</h2>
          <p class="subtitle">Practice at your own pace. Flag questions for review.</p>
        </div>
        <div v-if="submitted" class="score-badge">
          <span class="score-label">Score</span>
          <span class="score-value">{{ exam.score }}</span>
          <span class="score-total">/ 100</span>
        </div>
      </div>

      <div class="questions-list">
        <div v-for="(section, sIndex) in displayedSections" :key="sIndex" class="section-container">
          <div class="section-header">
            <h3>{{ section.title }}</h3>
          </div>
          
          <template v-for="(itemData, index) in section.items" :key="index">
            <div v-if="itemData.item.type === 'QUESTION' && itemData.item.data" class="google-card question-card" :class="submitted ? getQuestionStatus(itemData.item.data.id) : ''">
              <div class="question-header">
                <div class="q-info">
                    <span class="q-number">Question {{ itemData.questionNumber }}</span>
                    <span class="q-type-badge">{{ itemData.item.data.type }}</span>
                    <span v-if="submitted" class="status-badge" :class="getQuestionStatus(itemData.item.data.id)">
                    {{ getQuestionStatus(itemData.item.data.id) === 'correct' ? 'Correct' : 'Incorrect' }}
                    </span>
                </div>
                <button class="flag-btn" :class="{ 'flagged': isFlagged(itemData.item.data.id) }" @click="toggleFlag(itemData.item.data.id)" :disabled="submitted">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"></path><line x1="4" y1="22" x2="4" y2="15"></line></svg>
                    {{ isFlagged(itemData.item.data.id) ? 'Flagged' : 'Flag' }}
                </button>
              </div>
              
              <div class="question-content" v-if="itemData.item.data.stem" v-html="itemData.item.data.stem"></div>
              
              <div class="options-grid">
                <template v-if="['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(itemData.item.data.type)">
                  <template v-if="itemData.item.data.options && itemData.item.data.options.length > 0">
                      <label v-for="option in itemData.item.data.options" :key="option" class="option-item" :class="{ 'selected': isSelected(itemData.item.data.id, option) }">
                        <div class="input-wrapper">
                          <input 
                            v-if="itemData.item.data.type === 'MULTIPLE_CHOICE'"
                            type="checkbox" 
                            :name="'q-' + itemData.item.data.id" 
                            :value="option" 
                            v-model="answers[itemData.item.data.id]"
                            :disabled="submitted"
                            class="google-checkbox"
                          >
                          <input 
                            v-else
                            type="radio" 
                            :name="'q-' + itemData.item.data.id" 
                            :value="option" 
                            v-model="answers[itemData.item.data.id]"
                            :disabled="submitted"
                            class="google-radio"
                          >
                        </div>
                        <span class="option-text">{{ option }}</span>
                      </label>
                  </template>
                  <template v-else-if="itemData.item.data.type === 'TRUE_FALSE'">
                      <label v-for="option in ['True', 'False']" :key="option" class="option-item" :class="{ 'selected': isSelected(itemData.item.data.id, option) }">
                        <div class="input-wrapper">
                          <input 
                            type="radio" 
                            :name="'q-' + itemData.item.data.id" 
                            :value="option" 
                            v-model="answers[itemData.item.data.id]"
                            :disabled="submitted"
                            class="google-radio"
                          >
                        </div>
                        <span class="option-text">{{ option }}</span>
                      </label>
                  </template>
                </template>
                <template v-else-if="itemData.item.data.type === 'FILL_BLANK'">
                   <div class="fill-blank-container">
                     <div v-for="(ans, idx) in answers[itemData.item.data.id]" :key="idx" class="blank-input-row">
                       <span class="blank-label">{{ idx + 1 }}.</span>
                       <input 
                         type="text" 
                         v-model="answers[itemData.item.data.id][idx]" 
                         class="google-input blank-input" 
                         :disabled="submitted"
                         placeholder="Fill in the blank"
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
                      placeholder="Type your answer here..."
                      style="width: 100%; margin-top: 10px;"
                   ></textarea>
                </template>
              </div>
            </div>
          </template>
        </div>
      </div>

      <div class="exam-actions">
        <button v-if="currentSectionIndex > 0 && !submitted" @click="prevSection" class="google-btn text-btn large-btn">Previous Section</button>
        <div class="spacer"></div>
        <button v-if="currentSectionIndex < sections.length - 1 && !submitted" @click="nextSection" class="google-btn primary-btn large-btn">Next Section</button>
        <button v-else-if="!submitted" @click="submitExam" :disabled="loading" class="google-btn primary-btn large-btn">
          {{ loading ? 'Submitting...' : 'Submit Practice' }}
        </button>
        <button v-else @click="exitExam" class="google-btn text-btn large-btn">Exit Practice</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Reuse styles from ExamView mostly */
.container { max-width: 800px; margin: 0 auto; padding: 20px; }
.exam-header-card { background: white; padding: 24px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.12); margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center; border-top: 8px solid #fbbc04; }
.question-card { background: white; padding: 24px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.12); margin-bottom: 16px; }
.question-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.q-info { display: flex; align-items: center; }
.q-number { font-weight: 500; color: #5f6368; margin-right: 10px; }
.q-type-badge { background: #e8f0fe; color: #1a73e8; padding: 2px 8px; border-radius: 12px; font-size: 12px; }
.status-badge { margin-left: 10px; padding: 2px 8px; border-radius: 12px; font-size: 12px; }
.status-badge.correct { background: #e6f4ea; color: #137333; }
.status-badge.incorrect { background: #fce8e6; color: #c5221f; }
.flag-btn { background: none; border: 1px solid #dadce0; border-radius: 4px; padding: 4px 12px; display: flex; align-items: center; gap: 6px; cursor: pointer; color: #5f6368; }
.flag-btn.flagged { background: #ffe0b2; color: #e37400; border-color: #fbbc04; }
.flag-btn:hover { background: #f1f3f4; }
.option-item { display: flex; align-items: center; padding: 8px; border-radius: 4px; cursor: pointer; }
.option-item:hover { background: #f1f3f4; }
.option-item.selected { background: #e8f0fe; color: #1a73e8; }
.google-btn { padding: 10px 24px; border: none; border-radius: 4px; cursor: pointer; font-weight: 500; }
.primary-btn { background: #1a73e8; color: white; }
.text-btn { background: transparent; color: #1a73e8; }
.exam-actions { display: flex; margin-top: 32px; padding-bottom: 48px; }
.spacer { flex: 1; }
.score-badge { text-align: center; }
.score-value { font-size: 24px; color: #1a73e8; font-weight: bold; }
.score-total { color: #5f6368; }
.section-header h3 { margin: 0 0 16px 0; font-size: 18px; color: #202124; border-left: 4px solid #fbbc04; padding-left: 12px; }
</style>
