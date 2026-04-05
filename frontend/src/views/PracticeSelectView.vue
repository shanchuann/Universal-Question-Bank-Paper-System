<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { authState } from '@/states/authState'

interface ExamHistory {
  sessionId: string
  paperVersionId: string
  paperId?: string
  paperTitle?: string
  score: number
  startAt: string
  endAt: string
  status: string
}

interface Paper {
  id: string
  title: string
  createdAt: string
}

const examHistory = ref<ExamHistory[]>([])
const allPapers = ref<Paper[]>([])
const loading = ref(false)
const router = useRouter()
const favoritePaperIds = ref<string[]>([])

const FAVORITES_KEY = 'practice.favorite.papers'

const loadFavorites = () => {
  try {
    const raw = localStorage.getItem(FAVORITES_KEY)
    favoritePaperIds.value = raw ? JSON.parse(raw) : []
  } catch {
    favoritePaperIds.value = []
  }
}

const saveFavorites = () => {
  localStorage.setItem(FAVORITES_KEY, JSON.stringify(favoritePaperIds.value))
}

const isFavorite = (paperId: string) => favoritePaperIds.value.includes(String(paperId))

const toggleFavorite = (paperId: string) => {
  const id = String(paperId)
  const idx = favoritePaperIds.value.indexOf(id)
  if (idx >= 0) {
    favoritePaperIds.value.splice(idx, 1)
  } else {
    favoritePaperIds.value.unshift(id)
  }
  saveFavorites()
}

const getAuthHeaders = () => ({
  'Content-Type': 'application/json',
  Authorization: `Bearer ${localStorage.getItem('token')}`
})

// 获取用户做过的考试记录
const fetchExamHistory = async () => {
  const userId = authState.user?.id
  if (!userId) return

  try {
    const response = await axios.get(`/api/exams?userId=${userId}&size=100`, {
      headers: getAuthHeaders()
    })
    examHistory.value = response.data.content || []
  } catch (err) {
    console.error('Failed to fetch exam history:', err)
  }
}

// 获取所有可用试卷（用于没有考试记录时的备选）
const fetchAllPapers = async () => {
  try {
    const response = await axios.get('/api/papers', {
      headers: getAuthHeaders()
    })
    allPapers.value = response.data || []
  } catch (err) {
    console.error('Failed to fetch papers:', err)
  }
}

const startPractice = (paperId: string) => {
  router.push(`/practice/${paperId}`)
}

// 按试卷去重的考试记录
const uniqueExams = computed(() => {
  const seen = new Set<string>()
  return examHistory.value.filter((exam) => {
    const paperId = exam.paperVersionId || exam.paperId
    if (paperId && !seen.has(paperId)) {
      seen.add(paperId)
      return true
    }
    return false
  })
})

const hasExamHistory = computed(() => uniqueExams.value.length > 0)

const favoritePapers = computed(() => {
  const map = new Map(allPapers.value.map((p) => [String(p.id), p]))
  return favoritePaperIds.value.map((id) => map.get(String(id))).filter((p): p is Paper => !!p)
})

onMounted(async () => {
  loadFavorites()
  loading.value = true
  try {
    await Promise.all([fetchExamHistory(), fetchAllPapers()])
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="practice-view">
    <div class="page-header">
      <h1 class="page-title">自主练习</h1>
      <p class="page-subtitle">选择试卷进行自主练习，巩固所学知识</p>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 有考试历史时显示做过的试卷 -->
    <template v-else-if="hasExamHistory">
      <template v-if="favoritePapers.length > 0">
        <div class="section-title">
          <span>我的收藏</span>
          <small>快速进入常用练习试卷</small>
        </div>
        <div class="papers-grid">
          <div
            v-for="paper in favoritePapers"
            :key="`fav-${paper.id}`"
            class="paper-card favorite-card"
          >
            <div class="paper-info">
              <div class="paper-title-row">
                <h3 class="paper-title">{{ paper.title }}</h3>
                <button class="favorite-btn active" @click="toggleFavorite(paper.id)">★</button>
              </div>
              <div class="paper-meta">
                <span class="meta-item">
                  <svg
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path d="M12 8v4l3 3" />
                    <circle cx="12" cy="12" r="10" />
                  </svg>
                  {{ new Date(paper.createdAt).toLocaleDateString('zh-CN') }}
                </span>
              </div>
            </div>
            <button class="practice-btn" @click="startPractice(paper.id)">开始练习</button>
          </div>
        </div>
      </template>

      <div class="section-title">
        <span>您做过的试卷</span>
        <small>选择试卷重新练习</small>
      </div>
      <div class="papers-grid">
        <div v-for="exam in uniqueExams" :key="exam.sessionId" class="paper-card">
          <div class="paper-info">
            <div class="paper-title-row">
              <h3 class="paper-title">{{ exam.paperTitle || `试卷 #${exam.paperVersionId}` }}</h3>
              <button
                class="favorite-btn"
                :class="{ active: isFavorite(exam.paperVersionId) }"
                @click="toggleFavorite(exam.paperVersionId)"
              >
                {{ isFavorite(exam.paperVersionId) ? '★' : '☆' }}
              </button>
            </div>
            <div class="paper-meta">
              <span class="meta-item">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M12 8v4l3 3" />
                  <circle cx="12" cy="12" r="10" />
                </svg>
                {{ new Date(exam.startAt).toLocaleDateString('zh-CN') }}
              </span>
              <span class="meta-item score"> 得分: {{ exam.score ?? '-' }} </span>
            </div>
          </div>
          <button class="practice-btn" @click="startPractice(exam.paperVersionId)">再次练习</button>
        </div>
      </div>

      <!-- 同时显示所有可用试卷 -->
      <template v-if="allPapers.length > 0">
        <div class="section-title" style="margin-top: 40px">
          <span>所有可用试卷</span>
          <small>选择试卷开始新的练习</small>
        </div>
        <div class="papers-grid">
          <div v-for="paper in allPapers" :key="paper.id" class="paper-card">
            <div class="paper-info">
              <div class="paper-title-row">
                <h3 class="paper-title">{{ paper.title }}</h3>
                <button
                  class="favorite-btn"
                  :class="{ active: isFavorite(paper.id) }"
                  @click="toggleFavorite(paper.id)"
                >
                  {{ isFavorite(paper.id) ? '★' : '☆' }}
                </button>
              </div>
              <div class="paper-meta">
                <span class="meta-item">
                  <svg
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path d="M12 8v4l3 3" />
                    <circle cx="12" cy="12" r="10" />
                  </svg>
                  {{ new Date(paper.createdAt).toLocaleDateString('zh-CN') }}
                </span>
              </div>
            </div>
            <button class="practice-btn" @click="startPractice(paper.id)">开始练习</button>
          </div>
        </div>
      </template>
    </template>

    <!-- 没有考试历史时显示提示和所有试卷 -->
    <template v-else>
      <div class="empty-state-card">
        <div class="empty-icon">
          <svg
            width="64"
            height="64"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
          >
            <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2" />
            <rect x="9" y="3" width="6" height="4" rx="2" />
            <path d="M9 14l2 2 4-4" />
          </svg>
        </div>
        <h3>暂无练习记录</h3>
        <p>您还没有参加过考试。参加考试后，可以在这里重新练习做过的试卷。</p>
      </div>

      <template v-if="allPapers.length > 0">
        <div class="section-title">
          <span>所有可用试卷</span>
          <small>选择试卷开始练习</small>
        </div>
        <div class="papers-grid">
          <div v-for="paper in allPapers" :key="paper.id" class="paper-card">
            <div class="paper-info">
              <div class="paper-title-row">
                <h3 class="paper-title">{{ paper.title }}</h3>
                <button
                  class="favorite-btn"
                  :class="{ active: isFavorite(paper.id) }"
                  @click="toggleFavorite(paper.id)"
                >
                  {{ isFavorite(paper.id) ? '★' : '☆' }}
                </button>
              </div>
              <div class="paper-meta">
                <span class="meta-item">
                  <svg
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path d="M12 8v4l3 3" />
                    <circle cx="12" cy="12" r="10" />
                  </svg>
                  {{ new Date(paper.createdAt).toLocaleDateString('zh-CN') }}
                </span>
              </div>
            </div>
            <button class="practice-btn" @click="startPractice(paper.id)">开始练习</button>
          </div>
        </div>
      </template>

      <div v-else class="no-papers-hint">
        <p>系统中暂无可用试卷。请等待教师创建试卷后再来练习。</p>
        <router-link to="/exams/list" class="link-btn">查看考试安排</router-link>
      </div>
    </template>
  </div>
</template>

<style scoped>
.practice-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--line-text);
  margin: 0 0 8px 0;
}

.page-subtitle {
  color: var(--line-text-secondary);
  font-size: 14px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--line-text-secondary);
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--line-border);
  border-top-color: var(--line-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.section-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--line-border);
}

.section-title span {
  font-size: 18px;
  font-weight: 600;
  color: var(--line-text);
}

.section-title small {
  font-size: 13px;
  color: var(--line-text-secondary);
}

.papers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
}

.paper-card {
  background: var(--line-bg);
  border: 1px solid var(--line-border);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: all 0.2s;
}

.paper-card:hover {
  border-color: var(--line-primary);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.paper-info {
  flex: 1;
}

.paper-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--line-text);
  margin: 0 0 12px 0;
}

.paper-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.favorite-btn {
  border: none;
  background: transparent;
  color: var(--line-text-secondary);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  padding: 2px 4px;
}

.favorite-btn.active {
  color: #f59e0b;
}

.favorite-card {
  border-color: rgba(245, 158, 11, 0.4);
  background: linear-gradient(180deg, rgba(245, 158, 11, 0.06) 0%, transparent 100%);
}

.paper-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--line-text-secondary);
}

.meta-item.score {
  color: var(--line-primary);
  font-weight: 500;
}

.practice-btn {
  padding: 10px 20px;
  background: var(--line-primary);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.practice-btn:hover {
  background: #1557b0;
}

.empty-state-card {
  background: var(--line-bg-soft);
  border: 1px solid var(--line-border);
  border-radius: 12px;
  padding: 48px 32px;
  text-align: center;
  margin-bottom: 32px;
}

.empty-icon {
  color: var(--line-text-secondary);
  margin-bottom: 16px;
}

.empty-state-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--line-text);
  margin: 0 0 8px 0;
}

.empty-state-card p {
  color: var(--line-text-secondary);
  font-size: 14px;
  margin: 0;
  max-width: 400px;
  margin: 0 auto;
}

.no-papers-hint {
  text-align: center;
  padding: 40px;
  color: var(--line-text-secondary);
}

.no-papers-hint p {
  margin-bottom: 16px;
}

.link-btn {
  display: inline-block;
  padding: 10px 24px;
  background: var(--line-primary);
  color: white;
  border-radius: 8px;
  text-decoration: none;
  font-weight: 500;
  transition: background 0.2s;
}

.link-btn:hover {
  background: #1557b0;
}
</style>
