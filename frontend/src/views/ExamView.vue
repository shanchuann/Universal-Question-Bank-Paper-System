<script setup lang="ts">
import { ref, onUpdated, nextTick, computed, onMounted, onUnmounted, watch } from 'vue'
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
const paperId = ref(route.query.paperId?.toString() || '')
const userId = ref(authState.isAuthenticated && authState.user.id ? authState.user.id : 'guest-' + Math.floor(Math.random() * 10000))

// Update userId when profile is loaded
watch(() => authState.user.id, (newId) => {
  if (authState.isAuthenticated && newId) {
    userId.value = newId
  }
})

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
      alert('You have switched tabs too many times. Your exam has been automatically submitted.')
    } else {
      securityWarningMessage.value = `Warning: Tab switching is not allowed. You have ${maxSwitches - switchCount.value} attempts left.`
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
  console.log('Exam Paper Items:', exam.value?.paper.items);
  
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

const currentSection = computed(() => {
    if (sections.value.length === 0) return null;
    return sections.value[currentSectionIndex.value];
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

const startExam = async () => {
  loading.value = true
  error.value = ''
  try {
    await enterFullScreen()

    const pId = parseInt(paperId.value)
    if (isNaN(pId)) {
      error.value = 'Invalid Paper ID. Please enter a number.'
      return
    }

    const token = localStorage.getItem('token')
    const response = await axios.post('/api/exams/start', {
      paperId: pId,
      userId: userId.value
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
        // Initialize with empty strings for each blank found in stem
        // Use a more flexible regex for blanks (2 or more underscores)
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
    error.value = 'Failed to submit exam.'
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
</script>

<template>
  <div class="container">
    <div v-if="!exam" class="google-card start-card">
      <div class="card-header">
        <h1>Start Exam</h1>
        <p class="subtitle">Enter your details to begin</p>
      </div>
      
      <form @submit.prevent="startExam">
        <div class="form-group">
          <label class="field-label">Paper ID</label>
          <input v-model="paperId" type="text" required placeholder="Enter Paper ID" class="google-input" />
        </div>
        <div class="form-group">
          <label class="field-label">User</label>
          <input 
            v-if="authState.isAuthenticated"
            :value="authState.user.nickname || authState.user.username"
            type="text" 
            class="google-input" 
            disabled 
          />
          <input 
            v-else
            v-model="userId" 
            type="text" 
            required 
            class="google-input" 
          />
        </div>
        
        <div class="form-actions">
          <button type="submit" :disabled="loading" class="google-btn primary-btn full-width">
            {{ loading ? 'Starting...' : 'Start Exam' }}
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
          <p class="subtitle">Answer all questions below</p>
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
                <span class="q-number">Question {{ itemData.questionNumber }}</span>
                <span class="q-type-badge">{{ itemData.item.data.type }}</span>
                <span v-if="submitted" class="status-badge" :class="getQuestionStatus(itemData.item.data.id)">
                  {{ getQuestionStatus(itemData.item.data.id) === 'correct' ? 'Correct' : 'Incorrect' }}
                </span>
              </div>
              
              <div class="question-content" v-if="itemData.item.data.stem" v-html="itemData.item.data.stem"></div>
              <div class="question-content error-text" v-else>
                <span v-if="itemData.item.data.type === 'FILL_BLANK'">[Fill in the blank question]</span>
                <span v-else>(Error: Question content is missing)</span>
              </div>
              
              <div class="options-grid">
                <template v-if="['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(itemData.item.data.type)">
                  <div v-if="(!itemData.item.data.options || itemData.item.data.options.length === 0) && itemData.item.data.type !== 'TRUE_FALSE'" class="no-options-warning">
                    (Error: No options available)
                  </div>
                  <template v-else>
                      <label v-for="option in (itemData.item.data.options && itemData.item.data.options.length > 0 ? itemData.item.data.options : ['True', 'False'])" :key="option" class="option-item" :class="{ 'selected': isSelected(itemData.item.data.id, option) }">
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
          {{ loading ? 'Submitting...' : 'Submit Exam' }}
        </button>
        <button v-else @click="exitExam" class="google-btn text-btn large-btn">Exit Exam</button>
      </div>

      <!-- Security Overlays -->
      <div v-if="exam && !submitted && !isFullScreen" class="security-overlay">
        <div class="security-dialog google-card">
          <h3>Exam Security Violation</h3>
          <p>Full screen mode is required to continue the exam.</p>
          <button @click="enterFullScreen" class="google-btn primary-btn">Return to Full Screen</button>
        </div>
      </div>

      <div v-if="showSecurityWarning" class="security-overlay">
        <div class="security-dialog google-card warning">
          <h3>Security Warning</h3>
          <p>{{ securityWarningMessage }}</p>
          <button @click="showSecurityWarning = false" class="google-btn primary-btn">I Understand</button>
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
  border-top: 8px solid #1a73e8;
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.card-header h1 {
  font-family: 'Google Sans', sans-serif;
  font-size: 28px;
  font-weight: 400;
  margin-bottom: 8px;
  color: #202124;
}

.subtitle {
  color: #5f6368;
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
  color: #202124;
}

.google-input {
  width: 100%;
  padding: 10px 12px;
  font-size: 16px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  transition: border-color 0.2s;
}

.google-input:focus {
  border-color: #1a73e8;
  outline: none;
  border-width: 2px;
  padding: 9px 11px;
}

.full-width {
  width: 100%;
  justify-content: center;
  padding: 12px;
  font-size: 16px;
}

.message {
  margin-top: 20px;
  padding: 12px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.error {
  background-color: #fce8e6;
  color: #c5221f;
}

.exam-screen {
  max-width: 800px;
  margin: 0 auto;
}

.exam-header-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px;
  margin-bottom: 24px;
  border-top: 8px solid #1a73e8;
}

.exam-header-card h2 {
  font-family: 'Google Sans', sans-serif;
  font-size: 24px;
  font-weight: 400;
  margin-bottom: 4px;
  color: #202124;
}

.score-badge {
  background-color: #f8f9fa;
  padding: 12px 20px;
  border-radius: 8px;
  text-align: center;
  border: 1px solid #dadce0;
}

.score-label {
  display: block;
  font-size: 12px;
  color: #5f6368;
  text-transform: uppercase;
  font-weight: 500;
}

.score-value {
  font-size: 24px;
  font-weight: 400;
  color: #1a73e8;
}

.score-total {
  font-size: 14px;
  color: #5f6368;
}

.question-card {
  padding: 24px;
  margin-bottom: 16px;
}

.section-header {
  padding: 24px 0 16px 0;
  margin-bottom: 8px;
}

.section-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
  color: #202124;
  border-left: 4px solid #1a73e8;
  padding-left: 12px;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.q-number {
  font-size: 14px;
  font-weight: 500;
  color: #5f6368;
}

.q-type-badge {
  font-size: 12px;
  background: #e8f0fe;
  color: #1a73e8;
  padding: 2px 8px;
  border-radius: 12px;
  margin-left: 8px;
  font-weight: 500;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.correct {
  background-color: #e6f4ea;
  color: #137333;
}

.status-badge.incorrect {
  background-color: #fce8e6;
  color: #c5221f;
}

.question-content {
  font-size: 16px;
  line-height: 1.5;
  color: #202124;
  margin-bottom: 16px;
}

.options-grid {
  display: grid;
  gap: 4px;
}

.option-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid transparent;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.option-item:hover {
  background-color: #f1f3f4;
}

.option-item.selected {
  background-color: #e8f0fe;
  color: #1a73e8;
}

.input-wrapper {
  margin-right: 16px;
  display: flex;
  align-items: center;
}

.option-text {
  font-size: 14px;
  color: #202124;
}

.google-checkbox {
  width: 18px;
  height: 18px;
  accent-color: #1a73e8;
}

.google-radio {
  width: 18px;
  height: 18px;
  accent-color: #1a73e8;
}

.exam-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 32px;
  padding-bottom: 48px;
}

.spacer {
  flex: 1;
}

.large-btn {
  padding: 12px 32px;
  font-size: 16px;
}

.google-btn {
  border: none;
  border-radius: 4px;
  padding: 8px 24px;
  font-family: 'Google Sans', sans-serif;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.primary-btn {
  background-color: #1a73e8;
  color: white;
}

.primary-btn:hover {
  background-color: #1557b0;
  box-shadow: 0 1px 2px 0 rgba(60,64,67,0.3), 0 1px 3px 1px rgba(60,64,67,0.15);
}

.text-btn {
  background-color: transparent;
  color: #1a73e8;
}

.text-btn:hover {
  background-color: #f6fafe;
}

.large-btn {
  padding: 12px 32px;
  font-size: 16px;
}

.fill-blank-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.blank-input-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.blank-label {
  font-weight: 500;
  color: #5f6368;
  min-width: 24px;
}

.blank-input {
  max-width: 300px;
}

.security-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.security-dialog {
  background: white;
  padding: 32px;
  border-radius: 8px;
  text-align: center;
  max-width: 400px;
}

.security-dialog.warning h3 {
  color: #d93025;
}

.security-dialog h3 {
  margin-top: 0;
  margin-bottom: 16px;
}

.security-dialog p {
  margin-bottom: 24px;
  color: #5f6368;
}

.error-text {
  color: #d93025;
  font-style: italic;
  padding: 10px;
  background: #fce8e6;
  border-radius: 4px;
}

.no-options-warning {
  color: #e37400;
  font-style: italic;
  padding: 8px;
  background: #fef7e0;
  border-radius: 4px;
  margin-bottom: 8px;
}

@media (max-width: 768px) {
  .exam-header-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .score-badge {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .score-label {
    display: inline;
    margin-right: 8px;
  }
}
</style>
