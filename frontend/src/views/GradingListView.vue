<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { examApi } from '@/api/client'
import type { ExamSessionResponse } from '@/api/generated'
import { useRouter } from 'vue-router'

const exams = ref<ExamSessionResponse[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(0)
const size = ref(10)
const totalElements = ref(0)
const filterPaperId = ref('')
const filterUserId = ref('')

const router = useRouter()

const fetchExams = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await examApi.examsGet(
      filterPaperId.value || undefined,
      filterUserId.value || undefined,
      page.value,
      size.value
    )
    exams.value = response.data.content || []
    totalElements.value = response.data.totalElements || 0
  } catch (err) {
    error.value = 'Failed to load exams.'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const goToGrade = (examId: string) => {
  router.push(`/grading/${examId}`)
}

watch([filterPaperId, filterUserId], () => {
  page.value = 0
  fetchExams()
})

onMounted(fetchExams)
</script>

<template>
  <div class="container">
    <div class="header-row">
      <h1>Grading Workbench</h1>
    </div>

    <div class="filter-bar google-card">
      <div class="filter-group">
        <label>Paper ID</label>
        <input v-model="filterPaperId" class="google-input" placeholder="Filter by Paper ID" />
      </div>
      <div class="filter-group">
        <label>User ID</label>
        <input v-model="filterUserId" class="google-input" placeholder="Filter by User ID" />
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading exams...</p>
    </div>

    <div v-else-if="error" class="error-state google-card">
      <p>{{ error }}</p>
      <button @click="fetchExams" class="google-btn">Try Again</button>
    </div>

    <div v-else class="google-card table-card">
      <table class="question-table">
        <thead>
          <tr>
            <th>Exam ID</th>
            <th>Paper ID</th>
            <th>User</th>
            <th>Score</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="exam in exams" :key="exam.sessionId">
            <td class="id-col">{{ exam.sessionId }}</td>
            <td>{{ exam.paperVersionId }}</td>
            <td>{{ (exam as any).userId }}</td>
            <td>{{ (exam as any).score }}%</td>
            <td>
                <span class="chip" :class="(exam as any).score !== null ? 'easy' : 'hard'">
                    {{ (exam as any).score !== null ? 'Graded' : 'Pending' }}
                </span>
            </td>
            <td>
              <button @click="goToGrade(exam.sessionId!)" class="google-btn text-btn">Grade</button>
            </td>
          </tr>
          <tr v-if="exams.length === 0">
            <td colspan="6" class="empty-state">No exams found.</td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <button :disabled="page === 0" @click="page--; fetchExams()" class="google-btn text-btn">Previous</button>
        <span class="page-info">Page {{ page + 1 }}</span>
        <button :disabled="(page + 1) * size >= totalElements" @click="page++; fetchExams()" class="google-btn text-btn">Next</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.header-row {
  margin-bottom: 24px;
}

.filter-bar {
  display: flex;
  gap: 24px;
  padding: 16px 24px;
  margin-bottom: 24px;
  align-items: flex-end;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.filter-group label {
  font-size: 12px;
  font-weight: 500;
  color: #5f6368;
}

.google-input {
  padding: 8px 12px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  font-family: 'Google Sans', sans-serif;
  font-size: 14px;
  color: #3c4043;
  outline: none;
}

.google-input:focus {
  border-color: #1a73e8;
  box-shadow: 0 0 0 1px #1a73e8;
}

.table-card {
  padding: 0;
  overflow: hidden;
  border: 1px solid #dadce0;
  border-radius: 8px;
  background: white;
}

.question-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.question-table th {
  background-color: #fff;
  padding: 12px 16px;
  font-weight: 500;
  color: #5f6368;
  font-size: 14px;
  border-bottom: 1px solid #dadce0;
}

.question-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #f1f3f4;
  color: #3c4043;
  font-size: 14px;
}

.question-table tr:last-child td {
  border-bottom: none;
}

.question-table tr:hover {
  background-color: #f8f9fa;
}

.id-col {
  font-family: monospace;
  color: #5f6368;
}

.chip {
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  height: 24px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  background-color: #f1f3f4;
  color: #3c4043;
}

.chip.easy { background-color: #e6f4ea; color: #137333; }
.chip.hard { background-color: #fce8e6; color: #c5221f; }

.pagination {
  padding: 12px 16px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  border-top: 1px solid #dadce0;
  background-color: #fff;
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

.text-btn {
  background-color: transparent;
  color: #1a73e8;
}

.text-btn:hover:not(:disabled) {
  background-color: #f6fafe;
}

.text-btn:disabled {
  color: #dadce0;
  cursor: default;
}

.loading-state, .error-state {
  text-align: center;
  padding: 40px;
  color: #5f6368;
}

.empty-state {
  text-align: center;
  color: #5f6368;
  padding: 40px;
}
</style>