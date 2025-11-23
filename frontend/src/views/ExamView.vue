<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { examApi } from '@/api/client'
import type { ExamSessionResponse } from '@/api/generated'

const accessCode = ref('')
const session = ref<ExamSessionResponse | null>(null)
const currentQuestionIndex = ref(0)
const answers = ref<Record<string, string | string[]>>({})
const loading = ref(false)
const error = ref('')
const timer = ref<number | null>(null)
const timeRemaining = ref(0)

const currentQuestion = computed(() => {
  if (!session.value?.questions) return null
  return session.value.questions[currentQuestionIndex.value]
})

const formatTime = (seconds: number) => {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

const startTimer = () => {
  if (timer.value) clearInterval(timer.value)
  timer.value = window.setInterval(() => {
    if (timeRemaining.value > 0) {
      timeRemaining.value--
    } else {
      submitExam()
    }
  }, 1000)
}

const startExam = async () => {
  if (!accessCode.value) return
  loading.value = true
  error.value = ''
  try {
    const response = await examApi.examsAccessCodeStartPost(accessCode.value)
    session.value = response.data
    timeRemaining.value = session.value.timeRemainingSeconds || 0
    startTimer()
  } catch (err) {
    error.value = 'Failed to start exam. Invalid access code or exam not active.'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const submitExam = async () => {
  if (!session.value?.sessionId) return
  loading.value = true
  try {
    // In a real app, we would send answers here or ensure they are autosaved
    await examApi.examsSessionIdSubmitPost(session.value.sessionId)
    alert('Exam submitted successfully!')
    session.value = null
    if (timer.value) clearInterval(timer.value)
  } catch (err) {
    error.value = 'Failed to submit exam.'
    console.error(err)
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (timer.value) clearInterval(timer.value)
})
</script>

<template>
  <div class="exam-container">
    <div v-if="!session" class="start-screen">
      <h1>Start Exam</h1>
      <form @submit.prevent="startExam">
        <div class="form-group">
          <label>Access Code</label>
          <input v-model="accessCode" type="text" required placeholder="Enter access code" />
        </div>
        <button type="submit" :disabled="loading">Start Exam</button>
        <p v-if="error" class="error">{{ error }}</p>
      </form>
    </div>

    <div v-else class="exam-screen">
      <div class="exam-header">
        <h2>Exam Session</h2>
        <div class="timer">Time Remaining: {{ formatTime(timeRemaining) }}</div>
      </div>

      <div v-if="currentQuestion" class="question-card">
        <div class="question-header">
          <h3>Question {{ currentQuestionIndex + 1 }} / {{ session.questions?.length }}</h3>
          <span class="question-type">{{ currentQuestion.type }}</span>
        </div>
        
        <div class="question-content">
          <p class="stem">{{ currentQuestion.stem }}</p>
          
          <div v-if="currentQuestion.type === 'SINGLE_CHOICE' || currentQuestion.type === 'TRUE_FALSE'" class="options">
            <div v-for="option in currentQuestion.options" :key="option.key" class="option">
              <label>
                <input 
                  type="radio" 
                  :name="currentQuestion.questionId" 
                  :value="option.key"
                  v-model="answers[currentQuestion.questionId!]"
                />
                {{ option.key }}. {{ option.text }}
              </label>
            </div>
          </div>

          <div v-else-if="currentQuestion.type === 'MULTI_CHOICE'" class="options">
            <div v-for="option in currentQuestion.options" :key="option.key" class="option">
              <label>
                <input 
                  type="checkbox" 
                  :value="option.key"
                  v-model="answers[currentQuestion.questionId!]"
                />
                {{ option.key }}. {{ option.text }}
              </label>
            </div>
          </div>

          <div v-else class="text-answer">
            <textarea 
              v-model="answers[currentQuestion.questionId!]" 
              rows="5"
              placeholder="Type your answer here..."
            ></textarea>
          </div>
        </div>

        <div class="navigation">
          <button 
            :disabled="currentQuestionIndex === 0" 
            @click="currentQuestionIndex--"
          >Previous</button>
          
          <button 
            v-if="currentQuestionIndex < (session.questions?.length || 0) - 1" 
            @click="currentQuestionIndex++"
          >Next</button>
          
          <button 
            v-else 
            class="submit-btn"
            @click="submitExam"
          >Submit Exam</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.exam-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem;
}
.start-screen {
  max-width: 400px;
  margin: 0 auto;
  border: 1px solid #ccc;
  padding: 2rem;
  border-radius: 8px;
}
.form-group {
  margin-bottom: 1rem;
}
label {
  display: block;
  margin-bottom: 0.5rem;
}
input[type="text"] {
  width: 100%;
  padding: 0.5rem;
}
.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
}
.timer {
  font-size: 1.2rem;
  font-weight: bold;
  color: #e67e22;
}
.question-card {
  border: 1px solid #ddd;
  padding: 2rem;
  border-radius: 8px;
  background: #fff;
}
.question-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 1rem;
}
.question-type {
  background: #eee;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-size: 0.8rem;
}
.stem {
  font-size: 1.1rem;
  margin-bottom: 1.5rem;
}
.option {
  margin-bottom: 0.5rem;
}
.text-answer textarea {
  width: 100%;
  padding: 0.5rem;
}
.navigation {
  display: flex;
  justify-content: space-between;
  margin-top: 2rem;
}
button {
  padding: 0.5rem 1.5rem;
  cursor: pointer;
}
.submit-btn {
  background-color: #42b983;
  color: white;
  border: none;
}
.error {
  color: red;
  margin-top: 1rem;
}
</style>
