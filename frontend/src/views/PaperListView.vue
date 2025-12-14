<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

interface PaperResponse {
  id: string
  title: string
  createdAt: string
  status?: string
  questionCount?: number
  totalScore?: number
}

const papers = ref<PaperResponse[]>([])
const loading = ref(false)
const error = ref('')
const router = useRouter()

const fetchPapers = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/papers', {
      headers: { Authorization: `Bearer ${token}` }
    })
    papers.value = response.data.map((p: any) => ({
      ...p,
      id: String(p.id)
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
  if (!confirm('Are you sure you want to delete this paper?')) return
  try {
    const token = localStorage.getItem('token')
    await axios.delete(`/api/papers/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    await fetchPapers()
  } catch (err) {
    console.error('Failed to delete paper:', err)
    alert('Failed to delete paper')
  }
}

onMounted(fetchPapers)
</script>

<template>
  <div class="container paper-list-container">
    <div class="header-row">
      <h1>Paper Management</h1>
      <div class="header-actions">
        <button class="google-btn primary-btn" @click="createPaper">
          <span class="icon-wrapper"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg></span> New Paper
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading papers...</p>
    </div>

    <div v-else-if="error" class="error-state">{{ error }}</div>

    <div v-else>
      <div v-if="papers.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
        </div>
        <h3>No papers yet</h3>
        <p>Create your first paper to get started</p>
        <button class="google-btn text-btn" @click="createPaper">Create Paper</button>
      </div>

      <div v-else class="paper-grid">
        <div v-for="paper in papers" :key="paper.id" class="google-card paper-card">
          <div class="card-content">
            <div class="paper-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
            </div>
            <div class="paper-info">
              <h3 class="paper-title" :title="paper.title">{{ paper.title }}</h3>
              <div class="paper-meta">
                <span class="meta-item">ID: {{ paper.id }}</span>
                <span class="meta-item">{{ new Date(paper.createdAt).toLocaleDateString() }}</span>
              </div>
              <div class="paper-stats" v-if="paper.questionCount || paper.totalScore">
                <span class="badge bg-secondary" v-if="paper.questionCount">{{ paper.questionCount }} Qs</span>
                <span class="badge bg-secondary" v-if="paper.totalScore">{{ paper.totalScore }} Pts</span>
              </div>
            </div>
          </div>
          
          <div class="card-actions">
            <div class="action-group">
              <button class="icon-btn" title="Preview" @click="$router.push(`/papers/${paper.id}/preview`)">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
              </button>
              <button class="icon-btn" title="Edit" @click="$router.push(`/papers/${paper.id}/edit`)">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
              </button>
              <button class="icon-btn" title="Analytics" @click="viewAnalytics(paper.id)">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
              </button>
            </div>
            <button class="icon-btn danger" title="Delete" @click="deletePaper(paper.id)">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-top: 24px;
}

.header-row h1 {
  font-family: 'Google Sans', sans-serif;
  font-size: 24px;
  color: #202124;
  margin: 0;
}

.icon-wrapper {
  display: inline-flex;
  align-items: center;
  margin-right: 8px;
}

.paper-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.paper-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
  transition: box-shadow 0.2s, transform 0.2s;
  cursor: default;
}

.paper-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(60,64,67,0.15), 0 1px 3px rgba(60,64,67,0.3);
}

.card-content {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.paper-icon {
  width: 48px;
  height: 48px;
  background-color: #e8f0fe;
  color: #1a73e8;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.paper-info {
  flex: 1;
  min-width: 0; /* For text truncation */
}

.paper-title {
  font-size: 16px;
  font-weight: 500;
  color: #202124;
  margin: 0 0 4px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.paper-meta {
  font-size: 12px;
  color: #5f6368;
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.paper-stats {
  display: flex;
  gap: 8px;
}

.card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f1f3f4;
  padding-top: 12px;
  margin-top: auto;
}

.action-group {
  display: flex;
  gap: 4px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #5f6368;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state h3 {
  font-family: 'Google Sans', sans-serif;
  font-size: 18px;
  color: #202124;
  margin: 0 0 8px 0;
}

.empty-state p {
  margin: 0 0 24px 0;
}
</style>
