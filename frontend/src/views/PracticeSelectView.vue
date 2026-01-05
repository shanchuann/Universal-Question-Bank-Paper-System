<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

interface PaperResponse {
  id: string
  title: string
  createdAt: string
}

const papers = ref<PaperResponse[]>([])
const loading = ref(false)
const error = ref('')
const router = useRouter()

const fetchPapers = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/papers')
    papers.value = response.data
  } catch (err) {
    error.value = '加载试卷失败'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const startPractice = (paperId: string) => {
  router.push(`/practice/${paperId}`)
}

onMounted(fetchPapers)
</script>

<template>
  <div class="practice-select-container container mt-4">
    <h2 class="mb-4">选择试卷进行练习</h2>

    <div v-if="loading" class="text-center">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">加载中...</span>
      </div>
    </div>

    <div v-else-if="error" class="alert alert-danger">{{ error }}</div>

    <div v-else class="row">
      <div v-for="paper in papers" :key="paper.id" class="col-md-4 mb-4">
        <div class="card h-100 shadow-sm">
          <div class="card-body">
            <h5 class="card-title">{{ paper.title }}</h5>
            <p class="card-text text-muted">
              创建时间: {{ new Date(paper.createdAt).toLocaleDateString() }}
            </p>
            <button class="btn btn-primary w-100 mt-3" @click="startPractice(paper.id)">
              开始练习
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}
</style>
