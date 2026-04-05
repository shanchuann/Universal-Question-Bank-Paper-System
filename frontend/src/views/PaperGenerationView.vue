<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import GoogleSelect from '@/components/GoogleSelect.vue'

interface QuestionSummary {
  id: string
  subjectId: string
  type: string
  difficulty: string
  status: string
  stem?: string
}

interface PaperResponse {
  id: number
  title: string
  createdAt: string
  questions: QuestionSummary[]
}

const router = useRouter()

// 难度选项
const difficultyOptions = [
  { label: '全部难度', value: '' },
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '困难', value: 'HARD' }
]

const form = ref({
  title: '自动生成试卷',
  total: 5,
  difficulty: '', // Optional: EASY, MEDIUM, HARD
  keepGeneratedPaper: true,
  typeCounts: {
    SINGLE_CHOICE: 0,
    MULTI_CHOICE: 0,
    TRUE_FALSE: 0,
    FILL_BLANK: 0,
    SHORT_ANSWER: 0
  }
})

const generatedPaper = ref<PaperResponse | null>(null)
const loading = ref(false)
const message = ref('')
const error = ref('')

const handleSubmit = async () => {
  loading.value = true
  message.value = ''
  error.value = ''
  generatedPaper.value = null

  try {
    const token = localStorage.getItem('token')

    // Filter out zero counts
    const counts: Record<string, number> = {}
    let hasSpecificCounts = false
    for (const [type, count] of Object.entries(form.value.typeCounts)) {
      if (count > 0) {
        counts[type] = count
        hasSpecificCounts = true
      }
    }

    const payload = {
      title: form.value.title,
      difficulty: form.value.difficulty,
      total: form.value.total,
      typeCounts: hasSpecificCounts ? counts : undefined
    }

    const response = await axios.post<PaperResponse>('/api/papers/generate', payload, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    const paper = response.data
    if (form.value.keepGeneratedPaper) {
      generatedPaper.value = paper
      message.value = `试卷「${paper.title}」已生成，共 ${paper.questions.length} 道题目！`
    } else {
      generatedPaper.value = null
      try {
        await axios.delete(`/api/papers/${paper.id}`, {
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        message.value = `已生成并清理临时试卷，共 ${paper.questions.length} 道题目。`
      } catch {
        message.value = `试卷已生成（未保留模式），共 ${paper.questions.length} 道题目。`
      }
    }
  } catch (err) {
    error.value = '生成试卷失败，请重试。'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const takeExam = () => {
  if (generatedPaper.value) {
    router.push({ name: 'exam', query: { paperId: generatedPaper.value.id } })
  }
}

const editPaper = () => {
  if (generatedPaper.value) {
    router.push(`/papers/${generatedPaper.value.id}/edit`)
  }
}
</script>

<template>
  <div class="container">
    <div class="google-card generation-card">
      <div class="card-header">
        <h1 class="page-title">智能组卷</h1>
        <p class="subtitle">快速生成自定义试卷</p>
      </div>

      <form @submit.prevent="handleSubmit" class="generation-form">
        <div class="form-grid">
          <div class="form-group">
            <label class="field-label">试卷标题</label>
            <input
              v-model="form.title"
              type="text"
              required
              placeholder="请输入试卷标题"
              class="google-input"
            />
          </div>

          <div class="form-group">
            <label class="field-label">题目总数</label>
            <input
              v-model.number="form.total"
              type="number"
              min="1"
              max="50"
              required
              class="google-input"
            />
          </div>

          <div class="form-group">
            <label class="field-label">难度筛选</label>
            <GoogleSelect
              v-model="form.difficulty"
              :options="difficultyOptions"
              placeholder="全部难度"
            />
          </div>
        </div>

        <div class="advanced-settings">
          <h3>高级设置：按题型指定数量</h3>
          <p class="hint">设置后将忽略上方的「题目总数」设置。</p>
          <div class="type-grid">
            <div class="form-group">
              <label class="field-label">单选题</label>
              <input
                v-model.number="form.typeCounts.SINGLE_CHOICE"
                type="number"
                min="0"
                class="google-input"
              />
            </div>
            <div class="form-group">
              <label class="field-label">多选题</label>
              <input
                v-model.number="form.typeCounts.MULTI_CHOICE"
                type="number"
                min="0"
                class="google-input"
              />
            </div>
            <div class="form-group">
              <label class="field-label">判断题</label>
              <input
                v-model.number="form.typeCounts.TRUE_FALSE"
                type="number"
                min="0"
                class="google-input"
              />
            </div>
            <div class="form-group">
              <label class="field-label">填空题</label>
              <input
                v-model.number="form.typeCounts.FILL_BLANK"
                type="number"
                min="0"
                class="google-input"
              />
            </div>
            <div class="form-group">
              <label class="field-label">简答题</label>
              <input
                v-model.number="form.typeCounts.SHORT_ANSWER"
                type="number"
                min="0"
                class="google-input"
              />
            </div>
          </div>
        </div>

        <div class="advanced-settings keep-setting">
          <label class="keep-paper-label">
            <input v-model="form.keepGeneratedPaper" type="checkbox" />
            保留生成后的试卷
          </label>
          <p class="hint">关闭后系统会在生成完成后尝试自动清理该试卷。</p>
        </div>

        <div class="form-actions">
          <button type="submit" :disabled="loading" class="google-btn primary-btn full-width">
            {{ loading ? '正在生成...' : '生成试卷' }}
          </button>
        </div>
      </form>

      <div v-if="message" class="message success">
        <svg
          class="status-icon"
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M20 6L9 17l-5-5"></path>
        </svg>
        {{ message }}
      </div>
      <div v-if="error" class="message error">
        <span class="material-icon">error</span>
        {{ error }}
      </div>
    </div>

    <div v-if="generatedPaper" class="google-card result-card">
      <div class="result-header">
        <div class="header-content">
          <h2>{{ generatedPaper.title }}</h2>
          <p class="meta">创建时间: {{ new Date(generatedPaper.createdAt).toLocaleString() }}</p>
        </div>
        <div class="header-actions">
          <button @click="editPaper" class="google-btn text-btn">编辑试卷</button>
          <button @click="takeExam" class="google-btn primary-btn">开始答题</button>
        </div>
      </div>

      <div class="questions-list">
        <div v-for="(q, index) in generatedPaper.questions" :key="q.id" class="question-item">
          <div class="q-index">{{ index + 1 }}</div>
          <div class="q-content">
            <div class="q-meta">
              <span class="chip type">{{ q.type }}</span>
              <span class="chip difficulty" :class="q.difficulty.toLowerCase()">{{
                q.difficulty
              }}</span>
              <span class="subject">{{ q.subjectId }}</span>
            </div>
            <p class="q-stem" v-if="q.stem">{{ q.stem }}</p>
            <p class="q-id">ID: {{ q.id }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.container {
  padding: 32px;
  max-width: 900px;
  margin: 0 auto;
  animation: fadeIn 0.5s ease-out;
}

.generation-card {
  max-width: 900px;
  margin: 0 auto 32px;
  padding: 40px;
  background: var(--line-card-bg);
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-lg);
  box-shadow: var(--line-shadow-sm);
  transition: all 0.2s;
}

.generation-card:hover {
  box-shadow: var(--line-shadow-md);
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.card-header h1 {
  margin-bottom: 8px;
  color: var(--line-text-primary);
  letter-spacing: -0.5px;
}

.subtitle {
  color: var(--line-text-secondary);
  font-size: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.field-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  font-size: 14px;
  color: var(--line-text-primary);
}

.google-input {
  width: 100%;
  padding: 12px 16px;
  font-size: 16px;
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-md);
  transition: all 0.2s;
  background: var(--line-bg-soft);
  color: var(--line-text-primary);
}

.google-input:focus {
  border-color: var(--line-primary);
  background: var(--line-card-bg);
  box-shadow: 0 0 0 2px var(--line-primary-10);
  outline: none;
}

.full-width {
  width: 100%;
  height: 48px;
  justify-content: center;
  padding: 0 24px;
  font-size: 16px;
}

.form-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 12px;
}

.message {
  margin-top: 24px;
  padding: 16px;
  border-radius: var(--line-radius-md);
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  font-weight: 500;
}

.status-icon {
  flex-shrink: 0;
}

.success {
  background-color: #ecfdf5;
  color: #059669;
  border: 1px solid #d1fae5;
}

.error {
  background-color: #fef2f2;
  color: #dc2626;
  border: 1px solid #fee2e2;
}

.result-card {
  max-width: 900px;
  margin: 0 auto;
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-lg);
  background: var(--line-card-bg);
  box-shadow: var(--line-shadow-sm);
  animation: slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.result-header {
  padding: 24px 32px;
  background-color: var(--line-card-bg);
  border-bottom: 1px solid var(--line-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.result-header h2 {
  font-family:
    system-ui,
    -apple-system,
    sans-serif;
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 4px;
  color: var(--line-text-primary);
}

.meta {
  color: var(--line-text-secondary);
  font-size: 13px;
}

.questions-list {
  padding: 0;
}

.question-item {
  display: flex;
  gap: 20px;
  padding: 24px 32px;
  border-bottom: 1px solid var(--line-border);
  background: var(--line-card-bg);
  transition: background 0.2s;
}

.question-item:hover {
  background: var(--line-bg-soft);
}

.question-item:last-child {
  border-bottom: none;
}

.q-index {
  font-size: 18px;
  font-weight: 600;
  color: var(--line-text-secondary);
  min-width: 32px;
}

.q-content {
  flex: 1;
}

.q-meta {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  align-items: center;
}

.chip {
  padding: 4px 10px;
  border-radius: var(--line-radius-full);
  font-size: 12px;
  font-weight: 600;
  background-color: var(--line-bg-soft);
  color: var(--line-text-secondary);
  border: 1px solid var(--line-border);
}

.chip.difficulty.easy {
  background-color: #ecfdf5;
  color: #059669;
  border-color: #d1fae5;
}
.chip.difficulty.medium {
  background-color: #fffbeb;
  color: #b45309;
  border-color: #fef3c7;
}
.chip.difficulty.hard {
  background-color: #fef2f2;
  color: #dc2626;
  border-color: #fee2e2;
}

.subject {
  color: var(--line-text-secondary);
  font-size: 12px;
  font-weight: 500;
  margin-left: auto;
  font-family: monospace;
}

.q-stem {
  font-size: 16px;
  line-height: 1.6;
  color: var(--line-text-primary);
  margin-bottom: 8px;
}

.q-id {
  font-size: 11px;
  color: var(--line-text-secondary);
  font-family: monospace;
  opacity: 0.7;
}

.advanced-settings {
  margin-bottom: 32px;
  padding: 24px;
  background: var(--line-bg-soft);
  border-radius: var(--line-radius-lg);
  border: 1px solid var(--line-border);
}

.keep-setting {
  margin-top: -8px;
}

.keep-paper-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--line-text-primary);
}

.keep-paper-label input {
  width: 16px;
  height: 16px;
}

.advanced-settings h3 {
  font-size: 16px;
  margin-bottom: 4px;
  color: var(--line-text-primary);
  font-weight: 600;
}

.hint {
  font-size: 13px;
  color: var(--line-text-secondary);
  margin-bottom: 20px;
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
}

@media (max-width: 768px) {
  .type-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.google-btn {
  border: none;
  border-radius: var(--line-radius-md);
  padding: 10px 24px;
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.primary-btn {
  background-color: var(--line-primary);
  color: white;
  box-shadow: 0 2px 4px rgba(14, 165, 233, 0.2);
}

.primary-btn:hover {
  background-color: var(--line-primary-hover);
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.3);
  transform: translateY(-1px);
}

.text-btn {
  background-color: transparent;
  color: var(--line-text-secondary);
}

.text-btn:hover {
  background-color: var(--line-bg-soft);
  color: var(--line-primary);
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .result-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .result-header button {
    width: 100%;
  }

  .full-width {
    width: 100%;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
