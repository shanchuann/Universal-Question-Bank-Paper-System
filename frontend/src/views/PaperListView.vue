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
    error.value = '加载试卷失败'
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
  if (!confirm('确定要删除这份试卷吗？')) return
  try {
    const token = localStorage.getItem('token')
    await axios.delete(`/api/papers/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    await fetchPapers()
  } catch (err) {
    console.error('Failed to delete paper:', err)
    alert('删除试卷失败')
  }
}

onMounted(fetchPapers)
</script>

<template>
  <div class="container paper-list-container">
    <div class="header-row">
      <h1>试卷管理</h1>
      <div class="header-actions">
        <button class="google-btn primary-btn" @click="createPaper">
          新建试卷
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="error-state">{{ error }}</div>

    <div v-else>
      <div v-if="papers.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
        </div>
        <h3>暂无试卷</h3>
        <p>创建第一份试卷开始使用</p>
        <button class="google-btn text-btn" @click="createPaper">创建试卷</button>
      </div>

      <div v-else class="paper-grid">
        <div v-for="paper in papers" :key="paper.id" class="google-card paper-card">
          <div class="card-content" @click="$router.push(`/papers/${paper.id}/preview`)">
            <div class="paper-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
            </div>
            <div class="paper-info">
              <h3 class="paper-title" :title="paper.title">{{ paper.title }}</h3>
              <div class="paper-meta">
                <span class="meta-item">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                  {{ new Date(paper.createdAt).toLocaleDateString('zh-CN') }}
                </span>
              </div>
              <div class="paper-stats">
                <span class="stat-badge" v-if="paper.questionCount">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
                  {{ paper.questionCount }} 题
                </span>
                <span class="stat-badge" v-if="paper.totalScore">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
                  {{ paper.totalScore }} 分
                </span>
              </div>
            </div>
          </div>
          
          <div class="card-actions">
            <div class="action-group">
              <button class="icon-btn" title="预览" @click="$router.push(`/papers/${paper.id}/preview`)">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
              </button>
              <button class="icon-btn" title="编辑" @click="$router.push(`/papers/${paper.id}/edit`)">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
              </button>
              <button class="icon-btn" title="统计" @click="viewAnalytics(paper.id)">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
              </button>
            </div>
            <button class="icon-btn danger" title="删除" @click="deletePaper(paper.id)">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
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
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.paper-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
  transition: box-shadow 0.2s, transform 0.2s;
  cursor: default;
  border: 1px solid #dadce0;
  border-radius: 12px;
  overflow: hidden;
  padding: 0; /* Override google-card padding */
  margin: 0; /* Override google-card margin */
  background: white;
}

.paper-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(60,64,67,0.15), 0 1px 3px rgba(60,64,67,0.3);
}

.card-content {
  display: flex;
  gap: 16px;
  padding: 20px;
  cursor: pointer;
}

.card-content:hover {
  background-color: #f8f9fa;
}

.paper-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #e8f0fe 0%, #d2e3fc 100%);
  color: #1a73e8;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.paper-info {
  flex: 1;
  min-width: 0;
}

.paper-title {
  font-size: 16px;
  font-weight: 500;
  color: #202124;
  margin: 0 0 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.paper-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #5f6368;
}

.meta-item svg {
  opacity: 0.7;
}

.paper-stats {
  display: flex;
  gap: 8px;
}

.stat-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #f1f3f4;
  border-radius: 12px;
  font-size: 12px;
  color: #3c4043;
}

.stat-badge svg {
  opacity: 0.7;
}

.card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #f8f9fa;
  border-top: 1px solid #f1f3f4;
}

.action-group {
  display: flex;
  gap: 4px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  color: #5f6368;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: #e8f0fe;
  color: #1a73e8;
}

.icon-btn.danger:hover {
  background: #fce8e6;
  color: #d93025;
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
