<script setup lang="ts">
import { ref, onMounted, onUpdated, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import 'katex/dist/katex.min.css';
import katex from 'katex';

interface Question {
  id: string
  stem: string
  type: string
  difficulty: string
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

type DisplayItem = 
  | { type: 'SECTION'; title?: string }
  | { type: 'QUESTION'; data?: Question; score?: number };

const route = useRoute()
const router = useRouter()
const paper = ref<Paper | null>(null)
const loading = ref(true)
const error = ref('')

const displayItems = computed<DisplayItem[]>(() => {
  if (paper.value?.items && paper.value.items.length > 0) {
    return paper.value.items.map(item => {
      if (item.type === 'SECTION') {
        return { type: 'SECTION', title: item.sectionTitle };
      } else {
        const q = paper.value?.questions.find(q => q.id === item.id);
        return { type: 'QUESTION', data: q, score: item.score };
      }
    });
  } else if (paper.value?.questions) {
    return paper.value.questions.map(q => ({ type: 'QUESTION', data: q }));
  }
  return [];
});

const getQuestionIndex = (currentIndex: number) => {
  let count = 0;
  for (let i = 0; i <= currentIndex; i++) {
    if (displayItems.value[i]?.type === 'QUESTION') {
      count++;
    }
  }
  return count;
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
    renderMath();
  } catch (err) {
    error.value = 'Failed to load paper.'
    console.error(err)
  } finally {
    loading.value = false
  }
})

onUpdated(() => {
    renderMath();
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
    const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `paper_${id}_${type}.docx`
    link.click()
    URL.revokeObjectURL(link.href)
  } catch (err) {
    console.error('Download failed', err)
    alert('Download failed')
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
        <div class="header-content">
          <h1>{{ paper.title }}</h1>
          <p class="subtitle">{{ paper.questions.length }} Questions</p>
        </div>
        <div class="header-actions">
          <div class="export-menu">
            <button @click="download('student')" class="google-btn text-btn" title="Export Student Version">
              <span class="material-icons">description</span> Student
            </button>
            <button @click="download('teacher')" class="google-btn text-btn" title="Export Teacher Version">
              <span class="material-icons">school</span> Teacher
            </button>
            <button @click="download('answer-sheet')" class="google-btn text-btn" title="Export Answer Sheet">
              <span class="material-icons">grid_on</span> Sheet
            </button>
          </div>
          <button @click="editPaper" class="google-btn text-btn">Edit Paper</button>
          <button @click="startExam" class="google-btn primary-btn">Start Exam</button>
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
                <span class="chip type">{{ item.data.type }}</span>
                <span class="chip difficulty" :class="item.data.difficulty.toLowerCase()">{{ item.data.difficulty }}</span>
                <span v-if="item.score" class="chip score">{{ item.score }} pts</span>
              </div>
              <div class="stem" v-html="item.data.stem"></div>
            </div>
          </div>
        </template>
      </div>

      <div class="paper-footer">
        <button @click="startExam" class="google-btn primary-btn start-exam-btn">Start Exam</button>
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
  border: 1px solid #dadce0;
  border-top: 8px solid #1a73e8;
}

.paper-header {
  padding: 32px;
  background-color: #fff;
  border-bottom: 1px solid #dadce0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.paper-header h1 {
  font-family: 'Google Sans', sans-serif;
  font-size: 28px;
  font-weight: 400;
  margin-bottom: 4px;
  color: #202124;
}

.subtitle {
  color: #5f6368;
  font-size: 15px;
}

.questions-list {
  padding: 0;
}

.question-item {
  display: flex;
  gap: 16px;
  padding: 24px 32px;
  border-bottom: 1px solid #f1f3f4;
  background: #fff;
}

.question-item:last-child {
  border-bottom: none;
}

.q-index {
  font-size: 16px;
  font-weight: 500;
  color: #5f6368;
  min-width: 24px;
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
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  background-color: #f1f3f4;
  color: #3c4043;
}

.section-header {
  padding: 24px 32px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #f1f3f4;
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: #202124;
}

.chip.difficulty.easy { background-color: #e6f4ea; color: #137333; }
.chip.difficulty.medium { background-color: #fef7e0; color: #b06000; }
.chip.difficulty.hard { background-color: #fce8e6; color: #c5221f; }

.stem {
  font-size: 16px;
  line-height: 1.5;
  color: #202124;
}

.loading-state, .error-state {
  text-align: center;
  padding: 40px;
  color: #5f6368;
}

.paper-footer {
  padding: 16px 32px;
  background-color: #fff;
  border-top: 1px solid #dadce0;
  display: flex;
  justify-content: flex-end;
}

.start-exam-btn {
  width: 100%;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.export-menu {
  display: flex;
  gap: 8px;
  margin-right: 16px;
  border-right: 1px solid #dadce0;
  padding-right: 16px;
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
