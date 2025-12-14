<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

interface QuestionSummary {
  id: string
  subjectId: string
  type: string
  difficulty: string
  status: string
  stem?: string
}

interface PaperResponse {
  id: number
  title: string
  createdAt: string
  questions: QuestionSummary[]
}

const router = useRouter()
const form = ref({
  title: 'My Generated Paper',
  total: 5,
  difficulty: '', // Optional: EASY, MEDIUM, HARD
  typeCounts: {
    SINGLE_CHOICE: 0,
    MULTI_CHOICE: 0,
    TRUE_FALSE: 0,
    FILL_BLANK: 0,
    SHORT_ANSWER: 0
  }
})

const generatedPaper = ref<PaperResponse | null>(null)
const loading = ref(false)
const message = ref('')
const error = ref('')

const handleSubmit = async () => {
  loading.value = true
  message.value = ''
  error.value = ''
  generatedPaper.value = null
  
  try {
    const token = localStorage.getItem('token')
    
    // Filter out zero counts
    const counts: Record<string, number> = {}
    let hasSpecificCounts = false
    for (const [type, count] of Object.entries(form.value.typeCounts)) {
      if (count > 0) {
        counts[type] = count
        hasSpecificCounts = true
      }
    }

    const payload = {
      title: form.value.title,
      difficulty: form.value.difficulty,
      total: form.value.total,
      typeCounts: hasSpecificCounts ? counts : undefined
    }

    const response = await axios.post<PaperResponse>('/api/papers/generate', payload, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    generatedPaper.value = response.data
    message.value = `Paper "${response.data.title}" generated with ${response.data.questions.length} questions!`
  } catch (err) {
    error.value = 'Failed to generate paper. Please try again.'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const takeExam = () => {
  if (generatedPaper.value) {
    router.push({ name: 'exam', query: { paperId: generatedPaper.value.id } })
  }
}

const editPaper = () => {
  if (generatedPaper.value) {
    router.push(`/papers/${generatedPaper.value.id}/edit`)
  }
}
</script>

<template>
  <div class="container">
    <div class="google-card generation-card">
      <div class="card-header">
        <h1>Generate Paper</h1>
        <p class="subtitle">Create a custom exam paper instantly</p>
      </div>
      
      <form @submit.prevent="handleSubmit" class="generation-form">
        <div class="form-grid">
          <div class="form-group">
            <label class="field-label">Paper Title</label>
            <input v-model="form.title" type="text" required placeholder="Enter paper title" class="google-input" />
          </div>
          
          <div class="form-group">
            <label class="field-label">Total Questions</label>
            <input v-model.number="form.total" type="number" min="1" max="50" required class="google-input" />
          </div>

          <div class="form-group">
            <label class="field-label">Difficulty Filter</label>
            <div class="select-wrapper">
              <select v-model="form.difficulty" class="google-input">
                <option value="">Any Difficulty</option>
                <option value="EASY">Easy</option>
                <option value="MEDIUM">Medium</option>
                <option value="HARD">Hard</option>
              </select>
            </div>
          </div>
        </div>

        <div class="advanced-settings">
          <h3>Advanced: Questions by Type</h3>
          <p class="hint">If set, "Total Questions" above is ignored.</p>
          <div class="type-grid">
            <div class="form-group">
              <label class="field-label">Single Choice</label>
              <input v-model.number="form.typeCounts.SINGLE_CHOICE" type="number" min="0" class="google-input" />
            </div>
            <div class="form-group">
              <label class="field-label">Multi Choice</label>
              <input v-model.number="form.typeCounts.MULTI_CHOICE" type="number" min="0" class="google-input" />
            </div>
            <div class="form-group">
              <label class="field-label">True/False</label>
              <input v-model.number="form.typeCounts.TRUE_FALSE" type="number" min="0" class="google-input" />
            </div>
            <div class="form-group">
              <label class="field-label">Fill Blank</label>
              <input v-model.number="form.typeCounts.FILL_BLANK" type="number" min="0" class="google-input" />
            </div>
            <div class="form-group">
              <label class="field-label">Short Answer</label>
              <input v-model.number="form.typeCounts.SHORT_ANSWER" type="number" min="0" class="google-input" />
            </div>
          </div>
        </div>

        <div class="form-actions">
          <button type="submit" :disabled="loading" class="google-btn primary-btn full-width">
            {{ loading ? 'Generating...' : 'Generate Paper' }}
          </button>
        </div>
      </form>

      <div v-if="message" class="message success">
        <span class="material-icon">check_circle</span>
        {{ message }}
      </div>
      <div v-if="error" class="message error">
        <span class="material-icon">error</span>
        {{ error }}
      </div>
    </div>

    <div v-if="generatedPaper" class="google-card result-card">
      <div class="result-header">
        <div class="header-content">
          <h2>{{ generatedPaper.title }}</h2>
          <p class="meta">Created at: {{ new Date(generatedPaper.createdAt).toLocaleString() }}</p>
        </div>
        <div class="header-actions">
          <button @click="editPaper" class="google-btn text-btn">Edit Paper</button>
          <button @click="takeExam" class="google-btn primary-btn">Take This Exam</button>
        </div>
      </div>
      
      <div class="questions-list">
        <div v-for="(q, index) in generatedPaper.questions" :key="q.id" class="question-item">
          <div class="q-index">{{ index + 1 }}</div>
          <div class="q-content">
            <div class="q-meta">
              <span class="chip type">{{ q.type }}</span>
              <span class="chip difficulty" :class="q.difficulty.toLowerCase()">{{ q.difficulty }}</span>
              <span class="subject">{{ q.subjectId }}</span>
            </div>
            <p class="q-stem" v-if="q.stem">{{ q.stem }}</p> 
            <p class="q-id">ID: {{ q.id }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.generation-card {
  max-width: 800px;
  margin: 0 auto 32px;
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
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

.success {
  background-color: #e6f4ea;
  color: #137333;
}

.error {
  background-color: #fce8e6;
  color: #c5221f;
}

.result-card {
  max-width: 800px;
  margin: 0 auto;
  padding: 0;
  overflow: hidden;
  border: 1px solid #dadce0;
}

.result-header {
  padding: 24px;
  background-color: #fff;
  border-bottom: 1px solid #dadce0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.result-header h2 {
  font-family: 'Google Sans', sans-serif;
  font-size: 20px;
  font-weight: 400;
  margin-bottom: 4px;
  color: #202124;
}

.meta {
  color: #5f6368;
  font-size: 13px;
}

.questions-list {
  padding: 0;
}

.question-item {
  display: flex;
  gap: 16px;
  padding: 20px 24px;
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
}

.q-content {
  flex: 1;
}

.q-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}

.chip {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  background-color: #f1f3f4;
  color: #3c4043;
}

.chip.difficulty.easy { background-color: #e6f4ea; color: #137333; }
.chip.difficulty.medium { background-color: #fef7e0; color: #b06000; }
.chip.difficulty.hard { background-color: #fce8e6; color: #c5221f; }

.subject {
  color: #5f6368;
  font-size: 12px;
  font-weight: 500;
  margin-left: auto;
}

.q-stem {
  font-size: 15px;
  line-height: 1.5;
  color: #202124;
  margin-bottom: 8px;
}

.q-id {
  font-size: 11px;
  color: #9aa0a6;
  font-family: monospace;
}

.advanced-settings {
  margin-bottom: 24px;
  padding: 24px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #dadce0;
}

.advanced-settings h3 {
  font-size: 16px;
  margin-bottom: 4px;
  color: #202124;
  font-weight: 500;
}

.hint {
  font-size: 12px;
  color: #5f6368;
  margin-bottom: 16px;
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

@media (max-width: 768px) {
  .type-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
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

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  
  .result-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .result-header button {
    width: 100%;
  }
}
</style>
