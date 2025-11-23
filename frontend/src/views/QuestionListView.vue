<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { questionApi } from '@/api/client'
import type { QuestionSummary } from '@/api/generated'

const questions = ref<QuestionSummary[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(0)
const size = ref(10)
const totalElements = ref(0)

const fetchQuestions = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await questionApi.questionsGet(page.value, size.value)
    questions.value = response.data.items || []
    totalElements.value = response.data.totalElements || 0
  } catch (err) {
    error.value = 'Failed to load questions.'
    console.error(err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchQuestions()
})
</script>

<template>
  <div class="question-list-container">
    <h1>Question Bank</h1>
    <div v-if="loading" class="loading">Loading...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else>
      <table class="question-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Type</th>
            <th>Difficulty</th>
            <th>Status</th>
            <th>Subject ID</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="question in questions" :key="question.id">
            <td>{{ question.id }}</td>
            <td>{{ question.type }}</td>
            <td>{{ question.difficulty }}</td>
            <td>{{ question.status }}</td>
            <td>{{ question.subjectId }}</td>
          </tr>
        </tbody>
      </table>
      <div class="pagination">
        <button :disabled="page === 0" @click="page--; fetchQuestions()">Previous</button>
        <span>Page {{ page + 1 }}</span>
        <button :disabled="(page + 1) * size >= totalElements" @click="page++; fetchQuestions()">Next</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.question-list-container {
  padding: 2rem;
}
.question-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 1rem;
}
.question-table th,
.question-table td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}
.question-table th {
  background-color: #f2f2f2;
}
.pagination {
  margin-top: 1rem;
  display: flex;
  gap: 1rem;
  align-items: center;
}
.error {
  color: red;
}
</style>
