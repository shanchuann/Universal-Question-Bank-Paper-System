<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

interface PaperResponse {
  id: string
  title: string
  createdAt: string
  // other fields...
}

const papers = ref<PaperResponse[]>([])
const loading = ref(false)
const error = ref('')
const router = useRouter()

const fetchPapers = async () => {
  loading.value = true
  try {
    // We need to cast or ensure the type matches what the API returns
    // The generated client might return AxiosResponse<PaperResponse[]>
    // But wait, I added listPapers to controller but did I regenerate the API client?
    // I did NOT regenerate the API client.
    // So the frontend client doesn't know about `papersGet` (listPapers).
    
    // I can use axios directly or just assume the user will regenerate the client.
    // Or I can use the existing `paperApi` if it has a list method.
    // Checking grep results earlier... `papersPaperIdVersionsGet` exists.
    // But `listPapers` (GET /papers) was just added by me in Java.
    
    // Since I cannot run the openapi generator easily (it requires java/jar setup which might fail or take time),
    // I will use raw axios for this specific call or try to use `paperApi` if I can mock it, but raw axios is safer here.
    
    // Actually, I should check if there is a generic list method.
    // The grep showed `papersAutoPost`, `papersManualPost`, `papersPaperIdPublishPost`, `papersPaperIdVersionsGet`.
    // No generic `papersGet`.
    
    // So I will use axios directly for now to fetch the list of papers.
    // But wait, I need to import axios.
    
    // Let's check if I can use `paperApi.axios` or just import axios.
    // `paperApi` is an instance of `PaperApi` which extends `BaseAPI`.
    // `BaseAPI` has `axios` instance.
    
    // I'll just import axios.
    const response = await axios.get('/api/papers')
    console.log('Fetched papers:', response.data)
    // Ensure IDs are strings if needed, though JS handles numbers fine in template
    papers.value = response.data.map((p: any) => ({
      ...p,
      id: String(p.id) // Explicitly convert to string to avoid any ambiguity
    }))
  } catch (err) {
    error.value = 'Failed to load papers'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const viewAnalytics = (paperId: string) => {
  router.push(`/analytics/${paperId}`)
}

const createPaper = () => {
  router.push('/paper-generation')
}

const deletePaper = async (id: string) => {
  if (!confirm('确定要删除这张试卷吗？')) return
  try {
    await axios.delete(`/api/papers/${id}`)
    await fetchPapers()
  } catch (err) {
    console.error('Failed to delete paper:', err)
    alert('删除试卷失败')
  }
}

onMounted(fetchPapers)
</script>

<template>
  <div class="paper-list-container container mt-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2>试卷列表</h2>
      <button class="btn btn-primary" @click="createPaper">新建试卷</button>
    </div>

    <div v-if="loading" class="text-center">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <div v-else-if="error" class="alert alert-danger">{{ error }}</div>

    <div v-else class="row">
      <div v-if="papers.length === 0" class="col-12 text-center py-5">
        <div class="text-muted mb-3">
          <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
        </div>
        <p class="text-muted fs-5">暂无试卷</p>
        <p class="text-muted small">点击右上角"新建试卷"开始创建</p>
      </div>
      <div v-for="paper in papers" :key="paper.id" class="col-md-4 mb-4">
        <div class="card h-100 shadow-sm">
          <div class="card-body">
            <h5 class="card-title">{{ paper.title }}</h5>
            <p class="text-muted small">ID: {{ paper.id }}</p>
            <p class="card-text text-muted">
              创建时间: {{ new Date(paper.createdAt).toLocaleDateString() }}
            </p>
            <div class="d-flex justify-content-between mt-3">
              <div class="btn-group">
                <button class="btn btn-outline-secondary btn-sm" @click="$router.push(`/papers/${paper.id}/preview`)">预览</button>
                <button class="btn btn-outline-primary btn-sm" @click="$router.push(`/papers/${paper.id}/edit`)">编辑</button>
                <button class="btn btn-outline-danger btn-sm" @click="deletePaper(paper.id)">删除</button>
              </div>
              <button class="btn btn-info btn-sm text-white" @click="viewAnalytics(paper.id)">统计分析</button>
            </div>
          </div>
        </div>
      </div>
      
      <div v-if="papers.length === 0" class="col-12 text-center text-muted">
        暂无试卷，请点击右上角新建。
      </div>
    </div>
  </div>
</template>

<style scoped>
.card {
  transition: transform 0.2s;
}
.card:hover {
  transform: translateY(-5px);
}
</style>
