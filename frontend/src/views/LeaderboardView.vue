<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

interface StudentStats {
  id: number
  userId: string
  nickname?: string
  totalQuestionsAnswered: number
  correctAnswers: number
  currentStreak: number
  accuracy: number
}

const leaderboard = ref<StudentStats[]>([])
const loading = ref(false)

const fetchLeaderboard = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/stats/leaderboard?limit=50')
    leaderboard.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(fetchLeaderboard)
</script>

<template>
  <div class="container mt-4">
    <h2 class="mb-4">Class Leaderboard</h2>
    
    <div v-if="loading" class="text-center">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <div v-else class="card shadow-sm">
      <div class="card-body p-0">
        <table class="table table-hover mb-0">
          <thead class="table-light">
            <tr>
              <th scope="col" class="text-center" style="width: 80px">Rank</th>
              <th scope="col">Student</th>
              <th scope="col" class="text-center">Correct Answers</th>
              <th scope="col" class="text-center">Total Questions</th>
              <th scope="col" class="text-center">Accuracy</th>
              <th scope="col" class="text-center">Streak</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(user, index) in leaderboard" :key="user.id">
              <td class="text-center">
                <span class="rank-badge" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
              </td>
              <td>
                <div class="d-flex align-items-center">
                  <span class="fw-bold">{{ user.nickname || user.userId }}</span>
                </div>
              </td>
              <td class="text-center fw-bold text-success">{{ user.correctAnswers }}</td>
              <td class="text-center">{{ user.totalQuestionsAnswered }}</td>
              <td class="text-center">{{ (user.accuracy * 100).toFixed(1) }}%</td>
              <td class="text-center">
                <span v-if="user.currentStreak > 0" class="badge bg-warning text-dark">
                  {{ user.currentStreak }} 🔥
                </span>
                <span v-else>-</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.container { max-width: 1000px; }
.rank-badge {
  display: inline-block;
  width: 28px;
  height: 28px;
  line-height: 28px;
  border-radius: 50%;
  background: #f1f3f4;
  font-weight: bold;
  color: #5f6368;
}
.rank-1 { background: #fbbc04; color: white; }
.rank-2 { background: #9aa0a6; color: white; }
.rank-3 { background: #b06000; color: white; }
</style>
