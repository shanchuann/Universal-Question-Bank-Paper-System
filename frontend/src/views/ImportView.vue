<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import GoogleSelect from '@/components/GoogleSelect.vue'

interface QuestionOption {
  key?: string
  text: string
  isCorrect: boolean
}

interface Question {
  stem: string
  type: string
  difficulty: string
  subjectId: string
  score: number
  options: QuestionOption[]
  answerSchema?: any
  analysis?: string
}

/* Import mode fixed to batch; UI no longer has a mode toggle */

const router = useRouter()
const batchFileInput = ref<HTMLInputElement | null>(null)
const parsedQuestions = ref<Question[]>([])
const loading = ref(false)
const importing = ref(false)
const error = ref('')
const successMessage = ref('')
const dragOver = ref(false)
// importMode 已移除，界面始终显示批量导入

const questionTypeOptions = [
  { value: 'SINGLE_CHOICE', label: '单选题' },
  { value: 'MULTI_CHOICE', label: '多选题' },
  { value: 'MULTIPLE_CHOICE', label: '多选题(旧版)' },
  { value: 'TRUE_FALSE', label: '判断题' },
  { value: 'FILL_BLANK', label: '填空题' },
  { value: 'SHORT_ANSWER', label: '简答题' }
]

const difficultyOptions = [
  { value: 'EASY', label: '简单' },
  { value: 'MEDIUM', label: '中等' },
  { value: 'HARD', label: '困难' }
]

const clearStateForNewParse = () => {
  error.value = ''
  successMessage.value = ''
  parsedQuestions.value = []
}

const normalizeQuestions = (rows: any[]): Question[] => {
  return (rows || [])
    .map((item: any) => {
      const options = Array.isArray(item?.options)
        ? item.options.map((opt: any, idx: number) => ({
            key: String(opt?.key || String.fromCharCode(65 + idx)),
            text: String(opt?.text || ''),
            isCorrect: Boolean(opt?.isCorrect)
          }))
        : []

      return {
        stem: String(item?.stem || '').trim(),
        type: String(item?.type || 'SINGLE_CHOICE'),
        difficulty: String(item?.difficulty || 'MEDIUM'),
        subjectId: String(item?.subjectId || 'general'),
        score: Number(item?.score ?? 5),
        options,
        answerSchema: item?.answerSchema,
        analysis: String(item?.analysis || '')
      } as Question
    })
    .filter((q) => q.stem)
}

const triggerBatchFileInput = () => {
  batchFileInput.value?.click()
}

// 拍照导入功能已移除；不再需要触发拍照文件输入

const parseWordFile = async (file: File) => {
  loading.value = true
  clearStateForNewParse()

  const formData = new FormData()
  formData.append('file', file)

  try {
    const token = localStorage.getItem('token')
    const response = await axios.post('/api/import/word', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        Authorization: `Bearer ${token}`
      }
    })
    parsedQuestions.value = normalizeQuestions(response.data)
    if (parsedQuestions.value.length === 0) {
      error.value = '文档已上传，但未识别出有效题目。'
    }
  } catch (err: any) {
    if (err.response) {
      error.value = `导入失败: ${err.response.status} ${err.response.statusText}。请确认后端服务已启动。`
    } else {
      error.value = '解析文件失败。请确保文档格式正确。'
    }
  } finally {
    loading.value = false
  }
}

// 拍照导入相关功能已移除：只保留批量（Word）导入
const handleBatchFileUpload = async () => {
  const file = batchFileInput.value?.files?.[0]
  if (!file) return
  await parseWordFile(file)
}

const handleDrop = (e: DragEvent) => {
  dragOver.value = false
  const files = e.dataTransfer?.files
  if (!files || files.length === 0) return

  const firstFile = files.item(0)
  if (!firstFile) return

  if (!batchFileInput.value) return
  const dataTransfer = new DataTransfer()
  dataTransfer.items.add(firstFile)
  batchFileInput.value.files = dataTransfer.files
  void handleBatchFileUpload()
}

const removeQuestion = (index: number) => {
  parsedQuestions.value.splice(index, 1)
}

const addOption = (q: Question) => {
  if (!q.options) q.options = []
  q.options.push({ text: '', isCorrect: false })
}

const removeOption = (q: Question, index: number) => {
  q.options.splice(index, 1)
}

const importAll = async () => {
  if (parsedQuestions.value.length === 0) return

  importing.value = true
  error.value = ''
  successMessage.value = ''

  const token = localStorage.getItem('token')

  try {
    const response = await axios.post('/api/import/save', parsedQuestions.value, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    const count = Array.isArray(response.data) ? response.data.length : parsedQuestions.value.length
    successMessage.value = `已确认并添加 ${count} 道题目到题目列表。`
    parsedQuestions.value = []
  } catch (err: any) {
    error.value = '保存题目失败。' + (err.response?.data?.message || err.message)
  } finally {
    importing.value = false
  }
}

// 切换导入模式已移除

const goQuestionList = () => {
  router.push('/questions')
}
</script>

<template>
  <div class="container">
    <div class="google-card import-card">
      <div class="card-header">
        <h1 class="page-title">导入题目</h1>
        <p class="subtitle">支持批量导入（Word），确认后再添加到题目列表。</p>
      </div>

      <div class="mode-card">
        <p class="mode-title">上传 Word 文档（.docx）</p>
        <div
          class="upload-section"
          @click="triggerBatchFileInput"
          @dragover.prevent="dragOver = true"
          @dragleave="dragOver = false"
          @drop.prevent="handleDrop"
          :class="{ 'drag-over': dragOver }"
        >
          <input
            type="file"
            ref="batchFileInput"
            accept=".docx"
            @change="handleBatchFileUpload"
            class="file-input-hidden"
          />
          <div class="upload-content">
            <svg
              class="upload-icon"
              width="48"
              height="48"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="17 8 12 3 7 8" />
              <line x1="12" y1="3" x2="12" y2="15" />
            </svg>
            <p class="upload-text">点击或拖拽文档到此处上传</p>
            <p class="upload-hint">支持 .docx 格式</p>
          </div>
          <div v-if="loading" class="loading-spinner">正在解析文档...</div>
        </div>
      </div>

      <!-- 拍照导入已移除，界面仅保留批量 Word 导入 -->

      <div v-if="error" class="message error">{{ error }}</div>
      <div v-if="successMessage" class="message success">
        <span>{{ successMessage }}</span>
        <button class="google-btn text-btn success-action" @click="goQuestionList">
          查看题目列表
        </button>
      </div>

      <div v-if="parsedQuestions.length > 0" class="preview-section">
        <div class="preview-header">
          <h2>解析预览（{{ parsedQuestions.length }} 道题）</h2>
          <button @click="importAll" :disabled="importing" class="google-btn primary-btn">
            {{ importing ? '提交中...' : '确认并添加到题目列表' }}
          </button>
        </div>

        <div class="questions-list">
          <div v-for="(q, index) in parsedQuestions" :key="index" class="question-preview-item">
            <div class="item-header">
              <span class="index">#{{ index + 1 }}</span>
              <button @click="removeQuestion(index)" class="google-btn text-btn delete-btn">
                移除
              </button>
            </div>

            <div class="form-group">
              <label>题干</label>
              <textarea v-model="q.stem" class="google-input" rows="2"></textarea>
            </div>

            <div class="form-group">
              <label>解析</label>
              <textarea
                v-model="q.analysis"
                class="google-input"
                rows="2"
                placeholder="答案解析..."
              ></textarea>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>题型</label>
                <GoogleSelect
                  v-model="q.type"
                  :options="questionTypeOptions"
                  placeholder="选择题型"
                />
              </div>
              <div class="form-group">
                <label>难度</label>
                <GoogleSelect
                  v-model="q.difficulty"
                  :options="difficultyOptions"
                  placeholder="选择难度"
                />
              </div>
              <div class="form-group">
                <label>分数</label>
                <input type="number" v-model="q.score" class="google-input" step="0.5" />
              </div>
            </div>

            <div
              class="options-preview"
              v-if="
                ['SINGLE_CHOICE', 'MULTI_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(q.type)
              "
            >
              <label>选项</label>
              <div v-for="(opt, optIndex) in q.options" :key="optIndex" class="option-row">
                <input
                  :type="
                    q.type === 'SINGLE_CHOICE' || q.type === 'TRUE_FALSE' ? 'radio' : 'checkbox'
                  "
                  :name="'q-' + index"
                  :checked="opt.isCorrect"
                  @change="
                    opt.isCorrect = ($event.target as HTMLInputElement).checked;
                    if (q.type === 'SINGLE_CHOICE' || q.type === 'TRUE_FALSE') {
                      q.options.forEach((o, i) => (o.isCorrect = i === optIndex));
                    }
                  "
                  class="option-check"
                />
                <input
                  type="text"
                  v-model="opt.text"
                  class="google-input option-input"
                  placeholder="选项内容"
                />
                <button
                  @click="removeOption(q, optIndex)"
                  class="google-btn icon-btn"
                  title="移除选项"
                >
                  <svg
                    width="18"
                    height="18"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                </button>
              </div>
              <button @click="addOption(q)" class="google-btn text-btn small-btn">添加选项</button>
            </div>

            <div class="answer-preview" v-if="['FILL_BLANK', 'SHORT_ANSWER'].includes(q.type)">
              <div class="form-group">
                <label>参考答案（可选）</label>
                <textarea
                  class="google-input"
                  rows="2"
                  placeholder="在此输入正确答案..."
                  :value="
                    q.answerSchema?.correctAnswer || q.options.find((o) => o.isCorrect)?.text || ''
                  "
                  @input="
                    (e) => {
                      const val = (e.target as HTMLTextAreaElement).value
                      q.answerSchema = { correctAnswer: val }
                      q.options = [{ text: val, isCorrect: true }]
                    }
                  "
                ></textarea>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.import-card {
  max-width: 860px;
  margin: 0 auto;
  padding: 40px;
}

.import-mode-switch {
  display: inline-flex;
  gap: 8px;
  margin-top: 8px;
  padding: 4px;
  border-radius: 999px;
  background: var(--line-bg-soft);
  border: 1px solid var(--line-border);
}

.mode-btn {
  border: none;
  border-radius: 999px;
  padding: 8px 14px;
  background: transparent;
  color: var(--line-text-secondary);
  cursor: pointer;
  font-weight: 600;
}

.mode-btn.active {
  color: var(--line-primary);
  background: color-mix(in srgb, var(--line-primary) 12%, white);
}

.mode-card {
  margin-top: 20px;
}

.mode-title {
  margin: 0 0 10px;
  color: var(--line-text);
  font-weight: 600;
}

.photo-mode-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.photo-parse-mode {
  display: inline-flex;
  gap: 8px;
}

.small-mode-btn {
  border: 1px solid var(--line-border);
  border-radius: 999px;
  padding: 6px 12px;
  background: var(--line-bg);
  color: var(--line-text-secondary);
  cursor: pointer;
}

.small-mode-btn.active {
  border-color: var(--line-primary);
  color: var(--line-primary);
  background: color-mix(in srgb, var(--line-primary) 8%, white);
}

.upload-section {
  margin: 8px 0 24px;
  padding: 48px 32px;
  border: 2px dashed var(--line-border);
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--line-bg);
}

.upload-section:hover {
  border-color: var(--line-primary);
  background: color-mix(in srgb, var(--line-primary) 4%, transparent);
}

.upload-section.drag-over {
  border-color: var(--line-primary);
  background: color-mix(in srgb, var(--line-primary) 10%, transparent);
  transform: scale(1.01);
}

.file-input-hidden {
  display: none;
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.upload-icon {
  color: var(--line-text-secondary);
  transition: color 0.2s ease;
}

.upload-section:hover .upload-icon {
  color: var(--line-primary);
}

.upload-text {
  font-size: 16px;
  font-weight: 500;
  color: var(--line-text);
  margin: 0;
}

.upload-hint {
  font-size: 13px;
  color: var(--line-text-secondary);
  margin: 0;
}

.loading-spinner {
  margin-top: 16px;
  color: var(--line-primary);
  font-weight: 500;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line-border);
}

.question-preview-item {
  background: var(--line-bg-soft);
  border: 1px solid var(--line-border);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.item-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.index {
  font-weight: bold;
  color: var(--line-primary);
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-group {
  margin-bottom: 12px;
  flex: 1;
}

.google-input,
.google-select {
  width: 100%;
  padding: 8px;
  border: 1px solid var(--line-border);
  border-radius: 4px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.option-check {
  margin-right: 8px;
  width: 20px;
  height: 20px;
}

.option-input {
  flex: 1;
  min-width: 200px;
  background-color: var(--line-bg);
}

.icon-btn {
  padding: 0 8px;
  font-size: 18px;
  color: var(--line-text-secondary);
}

.small-btn {
  font-size: 13px;
  padding: 4px 8px;
}

.message {
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.error {
  background-color: color-mix(in srgb, var(--line-error) 14%, white);
  color: color-mix(in srgb, var(--line-error) 80%, black);
}

.success {
  background-color: color-mix(in srgb, var(--line-success) 14%, white);
  color: color-mix(in srgb, var(--line-success) 80%, black);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.success-action {
  white-space: nowrap;
}

@media (max-width: 768px) {
  .import-card {
    padding: 18px;
  }

  .form-row {
    flex-direction: column;
    gap: 8px;
  }

  .preview-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
