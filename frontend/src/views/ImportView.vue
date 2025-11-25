<script setup lang="ts">
import { ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

interface QuestionOption {
  text: string
  isCorrect: boolean
}

interface Question {
  stem: string
  type: string
  difficulty: string
  subjectId: string
  score: number
  options: QuestionOption[]
  answerSchema?: any
  analysis?: string
}

const router = useRouter()
const fileInput = ref<HTMLInputElement | null>(null)
const parsedQuestions = ref<Question[]>([])
const loading = ref(false)
const importing = ref(false)
const error = ref('')
const successMessage = ref('')

const handleFileUpload = async () => {
  const file = fileInput.value?.files?.[0]
  if (!file) return

  loading.value = true
  error.value = ''
  parsedQuestions.value = []

  const formData = new FormData()
  formData.append('file', file)

  try {
    const token = localStorage.getItem('token')
    const response = await axios.post('/api/import/word', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        Authorization: `Bearer ${token}`
      }
    })
    console.log('Import response:', response.data)
    parsedQuestions.value = response.data
  } catch (err: any) {
    console.error(err)
    if (err.response) {
        error.value = `Import failed: ${err.response.status} ${err.response.statusText}. Is the backend running?`
    } else {
        error.value = 'Failed to parse file. Please ensure it is a valid Word document and the backend is running.'
    }
  } finally {
    loading.value = false
  }
}

const removeQuestion = (index: number) => {
  parsedQuestions.value.splice(index, 1)
}

const addOption = (q: Question) => {
  if (!q.options) q.options = []
  q.options.push({ text: '', isCorrect: false })
}

const removeOption = (q: Question, index: number) => {
  q.options.splice(index, 1)
}

const importAll = async () => {
  if (parsedQuestions.value.length === 0) return

  importing.value = true
  error.value = ''
  successMessage.value = ''
  
  const token = localStorage.getItem('token')

  try {
    const response = await axios.post('/api/import/save', parsedQuestions.value, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    successMessage.value = `Successfully imported ${response.data.length} questions!`
    parsedQuestions.value = []
    setTimeout(() => router.push('/questions'), 2000)
  } catch (err: any) {
    console.error('Failed to import questions:', err)
    error.value = 'Failed to save questions. ' + (err.response?.data?.message || err.message)
  } finally {
    importing.value = false
  }
}
</script>

<template>
  <div class="container">
    <div class="google-card import-card">
      <div class="card-header">
        <h1>Batch Import Questions</h1>
        <p class="subtitle">Upload a Word document (.docx) to import questions.</p>
      </div>

      <div class="upload-section">
        <input 
          type="file" 
          ref="fileInput" 
          accept=".docx" 
          @change="handleFileUpload" 
          class="file-input"
        />
        <div v-if="loading" class="loading-spinner">Parsing document...</div>
      </div>

      <div v-if="error" class="message error">{{ error }}</div>
      <div v-if="successMessage" class="message success">{{ successMessage }}</div>

      <div v-if="parsedQuestions.length > 0" class="preview-section">
        <div class="preview-header">
          <h2>Preview ({{ parsedQuestions.length }} questions)</h2>
          <button @click="importAll" :disabled="importing" class="google-btn primary-btn">
            {{ importing ? 'Importing...' : 'Import All' }}
          </button>
        </div>

        <div class="questions-list">
          <div v-for="(q, index) in parsedQuestions" :key="index" class="question-preview-item">
            <div class="item-header">
              <span class="index">#{{ index + 1 }}</span>
              <button @click="removeQuestion(index)" class="google-btn text-btn delete-btn">Remove</button>
            </div>
            
            <div class="form-group">
              <label>Stem</label>
              <textarea v-model="q.stem" class="google-input" rows="2"></textarea>
            </div>

            <div class="form-group">
              <label>Analysis</label>
              <textarea v-model="q.analysis" class="google-input" rows="2" placeholder="Explanation for the answer..."></textarea>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>Type</label>
                <select v-model="q.type" class="google-select">
                  <option value="SINGLE_CHOICE">Single Choice</option>
                  <option value="MULTI_CHOICE">Multiple Choice</option>
                  <option value="MULTIPLE_CHOICE">Multiple Choice (Legacy)</option>
                  <option value="TRUE_FALSE">True/False</option>
                  <option value="FILL_BLANK">Fill Blank</option>
                  <option value="SHORT_ANSWER">Short Answer</option>
                </select>
              </div>
              <div class="form-group">
                <label>Difficulty</label>
                <select v-model="q.difficulty" class="google-select">
                  <option value="EASY">Easy</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HARD">Hard</option>
                </select>
              </div>
              <div class="form-group">
                <label>Score</label>
                <input type="number" v-model="q.score" class="google-input" step="0.5" />
              </div>
            </div>

            <!-- Options for Choice Questions -->
            <div class="options-preview" v-if="['SINGLE_CHOICE', 'MULTI_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(q.type)">
              <label>Options</label>
              <div v-for="(opt, optIndex) in q.options" :key="optIndex" class="option-row">
                <input 
                  :type="q.type === 'SINGLE_CHOICE' || q.type === 'TRUE_FALSE' ? 'radio' : 'checkbox'" 
                  :name="'q-' + index"
                  :checked="opt.isCorrect"
                  @change="opt.isCorrect = ($event.target as HTMLInputElement).checked; if(q.type === 'SINGLE_CHOICE' || q.type === 'TRUE_FALSE') { q.options.forEach((o, i) => o.isCorrect = i === optIndex) }"
                  class="option-check"
                />
                <input type="text" v-model="opt.text" class="google-input option-input" placeholder="Option text" />
                <button @click="removeOption(q, optIndex)" class="google-btn icon-btn" title="Remove Option">×</button>
              </div>
              <button @click="addOption(q)" class="google-btn text-btn small-btn">+ Add Option</button>
            </div>

            <!-- Answer for Fill Blank / Short Answer -->
            <div class="answer-preview" v-if="['FILL_BLANK', 'SHORT_ANSWER'].includes(q.type)">
              <div class="form-group">
                <label>Correct Answer</label>
                <textarea 
                  class="google-input" 
                  rows="2" 
                  placeholder="Enter the correct answer here..."
                  :value="q.answerSchema?.correctAnswer || (q.options.find(o => o.isCorrect)?.text || '')"
                  @input="(e) => {
                    const val = (e.target as HTMLTextAreaElement).value;
                    q.answerSchema = { correctAnswer: val };
                    // Also sync to options for compatibility if needed, or just rely on answerSchema
                    q.options = [{ text: val, isCorrect: true }];
                  }"
                ></textarea>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.import-card {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px;
  border-top: 8px solid #1a73e8;
}

.upload-section {
  margin: 32px 0;
  padding: 32px;
  border: 2px dashed #dadce0;
  border-radius: 8px;
  text-align: center;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #dadce0;
}

.question-preview-item {
  background: #f8f9fa;
  border: 1px solid #dadce0;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.item-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.index {
  font-weight: bold;
  color: #1a73e8;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-group {
  margin-bottom: 12px;
  flex: 1;
}

.google-input, .google-select {
  width: 100%;
  padding: 8px;
  border: 1px solid #dadce0;
  border-radius: 4px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.option-check {
  margin-right: 8px;
  width: 20px;
  height: 20px;
}

.option-input {
  flex: 1;
  min-width: 200px;
  background-color: #fff;
}

.icon-btn {
  padding: 0 8px;
  font-size: 18px;
  color: #5f6368;
}

.small-btn {
  font-size: 13px;
  padding: 4px 8px;
}

.message {
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.error {
  background-color: #fce8e6;
  color: #c5221f;
}

.success {
  background-color: #e6f4ea;
  color: #137333;
}
</style>
