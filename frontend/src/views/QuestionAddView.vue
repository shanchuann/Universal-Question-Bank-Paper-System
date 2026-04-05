<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { QuillEditor } from '@vueup/vue-quill'
import { useToast } from '@/composables/useToast'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import 'katex/dist/katex.min.css'
import katex from 'katex'
import GoogleSelect from '@/components/GoogleSelect.vue'
import GoogleCombobox from '@/components/GoogleCombobox.vue'
import KnowledgePointDialog from '@/components/KnowledgePointDialog.vue'

// Expose katex to window for Quill's formula module
;(window as any).katex = katex

const { showToast: globalToast } = useToast()

const route = useRoute()
const router = useRouter()

// 编辑模式判断
const isEditMode = computed(() => !!route.params.id)
const questionId = computed(() => route.params.id as string)

const form = ref({
  subjectId: '',
  type: 'SINGLE_CHOICE',
  difficulty: 'EASY',
  stem: '',
  options: ['', '', '', ''],
  answer: '',
  tags: '',
  knowledgePointIds: [] as string[]
})

const knowledgePoints = ref<any[]>([])
const flatPoints = ref<any[]>([])
const showKPDialog = ref(false)
const newKPName = ref('')

const typeOptions = [
  { label: '单选题', value: 'SINGLE_CHOICE' },
  { label: '多选题', value: 'MULTIPLE_CHOICE' },
  { label: '判断题', value: 'TRUE_FALSE' },
  { label: '填空题', value: 'FILL_BLANK' },
  { label: '简答题', value: 'SHORT_ANSWER' }
]

const difficultyOptions = [
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '困难', value: 'HARD' }
]

const kpOptions = computed(() => {
  return flatPoints.value.map((p) => ({
    label: p.name,
    value: p.id
  }))
})

const parentOptions = computed(() => {
  // Filter for potential parents (Chapters and Sections)
  return knowledgePoints.value
    .filter((p: any) => ['CHAPTER', 'SECTION'].includes(p.level))
    .map((p: any) => ({ id: p.id, label: p.name }))
})

const fetchKnowledgePoints = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/knowledge-points', {
      headers: { Authorization: `Bearer ${token}` }
    })
    knowledgePoints.value = response.data

    // Flatten for select
    const flat: any[] = []
    const map = new Map(response.data.map((p: any) => [p.id, p]))

    const buildPath = (p: any): string => {
      if (p.parentId && map.has(p.parentId)) {
        return buildPath(map.get(p.parentId)) + ' > ' + p.name
      }
      return p.name
    }

    response.data.forEach((p: any) => {
      if (p.level === 'POINT') {
        flat.push({
          id: p.id,
          name: buildPath(p)
        })
      }
    })
    flatPoints.value = flat
  } catch (err) {
    console.error(err)
  }
}

// 加载题目数据（编辑模式）
const loadQuestion = async () => {
  if (!isEditMode.value) return

  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get(`/api/questions/${questionId.value}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    const question = response.data

    form.value.subjectId = question.subjectId || ''
    form.value.type = question.type || 'SINGLE_CHOICE'
    form.value.difficulty = question.difficulty || 'EASY'
    form.value.stem = question.stem || ''
    form.value.tags = question.tags ? question.tags.join(', ') : ''
    form.value.knowledgePointIds = question.knowledgePointIds || []

    // 解析选项 - 支持多种格式
    let parsedOptions: any[] = []
    if (question.options && Array.isArray(question.options)) {
      parsedOptions = question.options
    } else if (question.optionsJson) {
      try {
        parsedOptions = JSON.parse(question.optionsJson)
      } catch {
        parsedOptions = []
      }
    }

    // 处理选项和答案
    if (['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(question.type)) {
      if (parsedOptions.length > 0) {
        form.value.options = parsedOptions.map((opt: any) =>
          typeof opt === 'string' ? opt : opt.text || opt.content || ''
        )
        // 确保至少4个选项
        while (form.value.options.length < 4) form.value.options.push('')

        // 找到正确答案 - 支持多种格式
        if (question.answerSchema) {
          // 如果有 answerSchema，直接使用
          form.value.answer = question.answerSchema
          if (question.type === 'MULTIPLE_CHOICE') {
            multiAnswer.value = question.answerSchema.split(',').map((a: string) => a.trim())
          }
        } else {
          // 从选项中找正确答案
          const correctOptions = parsedOptions.filter((opt: any) => opt.isCorrect)
          if (question.type === 'SINGLE_CHOICE' && correctOptions.length > 0) {
            const idx = parsedOptions.indexOf(correctOptions[0])
            form.value.answer = String.fromCharCode(65 + idx)
          } else if (question.type === 'MULTIPLE_CHOICE') {
            multiAnswer.value = correctOptions.map((opt: any) => {
              const idx = parsedOptions.indexOf(opt)
              return String.fromCharCode(65 + idx)
            })
          }
        }
      } else {
        form.value.options = ['', '', '', '']
      }
    } else if (question.type === 'TRUE_FALSE') {
      form.value.answer = question.answerSchema || 'True'
    } else if (question.type === 'FILL_BLANK') {
      form.value.options = parsedOptions.map((opt: any) =>
        typeof opt === 'string' ? opt : opt.text || ''
      )
      if (form.value.options.length === 0) form.value.options = ['']
    } else if (question.type === 'SHORT_ANSWER') {
      form.value.answer = question.answerSchema || ''
    }
  } catch (err) {
    console.error('加载题目失败', err)
    showToast('加载题目失败', 'error')
  } finally {
    loading.value = false
  }
}

// 多选题答案
const multiAnswer = ref<string[]>([])

watch(
  multiAnswer,
  (val) => {
    form.value.answer = val.join(',')
  },
  { deep: true }
)

const handleCreateKP = (name: string) => {
  newKPName.value = name
  showKPDialog.value = true
}

const saveNewKP = async (kpData: any) => {
  try {
    const token = localStorage.getItem('token')
    const res = await axios.post(
      '/api/knowledge-points',
      {
        ...kpData,
        subjectId: form.value.subjectId || 'general'
      },
      {
        headers: { Authorization: `Bearer ${token}` }
      }
    )

    // Refresh list
    await fetchKnowledgePoints()

    // Add new ID to selection
    if (res.data && res.data.id) {
      form.value.knowledgePointIds.push(res.data.id)
    }

    showKPDialog.value = false
  } catch (err) {
    console.error('Failed to create KP', err)
    globalToast({ message: '创建知识点失败', type: 'error' })
  }
}

onMounted(() => {
  fetchKnowledgePoints()
  if (isEditMode.value) {
    loadQuestion()
  }
})

const loading = ref(false)
const message = ref('')
const error = ref('')
const quillEditor = ref<any>(null)

// Toast 提示函数
const showToast = (msg: string, type: 'success' | 'error') => {
  if (type === 'success') {
    message.value = msg
    error.value = ''
  } else {
    error.value = msg
    message.value = ''
  }
  // 3秒后自动消失
  setTimeout(() => {
    message.value = ''
    error.value = ''
  }, 3000)
}

const toolbarOptions = [
  ['bold', 'italic', 'underline', 'strike'],
  ['blockquote', 'code-block'],
  [{ header: 1 }, { header: 2 }],
  [{ list: 'ordered' }, { list: 'bullet' }],
  [{ script: 'sub' }, { script: 'super' }],
  [{ indent: '-1' }, { indent: '+1' }],
  [{ direction: 'rtl' }],
  [{ size: ['small', false, 'large', 'huge'] }],
  [{ header: [1, 2, 3, 4, 5, 6, false] }],
  [{ color: [] }, { background: [] }],
  [{ font: [] }],
  [{ align: [] }],
  ['clean'],
  ['link', 'image', 'video', 'formula']
]

const imageHandler = () => {
  const input = document.createElement('input')
  input.setAttribute('type', 'file')
  input.setAttribute('accept', 'image/*')
  input.click()

  input.onchange = async () => {
    const file = input.files ? input.files[0] : null
    if (file) {
      const formData = new FormData()
      formData.append('file', file)

      try {
        const res = await axios.post('/api/files/upload', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        const url = res.data.fileUrl

        // Insert image into editor
        if (quillEditor.value) {
          const quill = quillEditor.value.getQuill()
          const range = quill.getSelection()
          quill.insertEmbed(range ? range.index : 0, 'image', url)
        }
      } catch (err) {
        console.error('Image upload failed', err)
        globalToast({ message: '图片上传失败', type: 'error' })
      }
    }
  }
}

const onEditorReady = (quill: any) => {
  const toolbar = quill.getModule('toolbar')
  toolbar.addHandler('image', imageHandler)
}

const addOption = () => {
  form.value.options.push('')
}

const removeOption = (index: number) => {
  form.value.options.splice(index, 1)
}

const insertBlank = () => {
  if (quillEditor.value) {
    const quill = quillEditor.value.getQuill()
    const range = quill.getSelection(true)
    quill.insertText(range.index, ' _____ ')
    quill.setSelection(range.index + 7)
  }
}

watch(
  () => form.value.stem,
  (newVal) => {
    if (form.value.type === 'FILL_BLANK') {
      const matches = newVal.match(/_____/g)
      const count = matches ? matches.length : 0
      // Adjust options array size
      if (count > form.value.options.length) {
        for (let i = form.value.options.length; i < count; i++) {
          form.value.options.push('')
        }
      } else if (count < form.value.options.length) {
        form.value.options.splice(count)
      }
    }
  }
)

const handleSubmit = async () => {
  loading.value = true
  message.value = ''
  error.value = ''

  try {
    let apiOptions: { text: string; isCorrect: boolean }[] = []

    // 解析答案字母列表
    const answerLetters = form.value.answer.split(',').map((a) => a.trim())

    if (['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(form.value.type)) {
      apiOptions = form.value.options.map((optText, idx) => ({
        text: optText,
        isCorrect: answerLetters.includes(String.fromCharCode(65 + idx))
      }))
    } else if (form.value.type === 'TRUE_FALSE') {
      apiOptions = [
        { text: 'True', isCorrect: form.value.answer === 'True' },
        { text: 'False', isCorrect: form.value.answer === 'False' }
      ]
    } else if (form.value.type === 'FILL_BLANK') {
      // 填空题：允许答案为空（不设标准答案）
      apiOptions = form.value.options.map((optText) => ({
        text: optText,
        isCorrect: true
      }))
    } else if (form.value.type === 'SHORT_ANSWER') {
      // 简答题：允许答案为空，默认为需人工判卷
      apiOptions = [{ text: form.value.answer || '', isCorrect: true }]
    }

    // Construct payload strictly matching backend expectations
    const payload = {
      subjectId: form.value.subjectId,
      type: form.value.type,
      difficulty: form.value.difficulty,
      stem: form.value.stem,
      options: apiOptions,
      answerSchema: form.value.answer, // 保存实际答案字母
      score: 1.0,
      tags: form.value.tags ? form.value.tags.split(',').map((t) => t.trim()) : [],
      knowledgePointIds: form.value.knowledgePointIds
    }
    const token = localStorage.getItem('token')

    if (isEditMode.value) {
      // 编辑模式 - PUT 请求
      await axios.put(`/api/questions/${questionId.value}`, payload, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      })
      showToast('题目更新成功！', 'success')
      // 延迟返回列表
      setTimeout(() => {
        router.push('/questions')
      }, 1500)
    } else {
      // 新建模式 - POST 请求
      await axios.post('/api/questions', payload, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      })
      showToast('题目添加成功！', 'success')
      // Reset form
      form.value.stem = ''
      form.value.options = ['', '', '', '']
      form.value.answer = ''
      form.value.tags = ''
      form.value.knowledgePointIds = []
    }
  } catch (err: any) {
    if (err.response?.status === 403) {
      showToast('请先加入班级后才能添加题目', 'error')
    } else if (err.response?.status === 401) {
      showToast('请先登录', 'error')
    } else {
      showToast(err.response?.data?.message || err.message || '操作失败', 'error')
    }
    console.error(err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="container add-question-container">
    <div class="google-card form-card">
      <div class="card-header">
        <h1 class="page-title">{{ isEditMode ? '编辑题目' : '添加题目' }}</h1>
        <p class="subtitle">{{ isEditMode ? '修改题目信息' : '创建新题目加入题库' }}</p>
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="form-grid">
          <div class="form-group">
            <label>科目</label>
            <input
              v-model="form.subjectId"
              type="text"
              required
              placeholder="例如：数学"
              class="google-input"
            />
          </div>

          <div class="form-group">
            <label>题型</label>
            <GoogleSelect v-model="form.type" :options="typeOptions" />
          </div>

          <div class="form-group">
            <label>难度</label>
            <GoogleSelect v-model="form.difficulty" :options="difficultyOptions" />
          </div>

          <div class="form-group">
            <label>知识点</label>
            <GoogleCombobox
              v-model="form.knowledgePointIds"
              :options="kpOptions"
              placeholder="选择或创建知识点..."
              :allow-create="true"
              @create="handleCreateKP"
            />
            <small class="helper-text">输入搜索，找不到可点击“创建”添加新知识点</small>
          </div>
        </div>

        <div class="form-group full-width">
          <label>题干</label>
          <div class="editor-wrapper">
            <QuillEditor
              ref="quillEditor"
              theme="snow"
              v-model:content="form.stem"
              contentType="html"
              :toolbar="toolbarOptions"
              @ready="onEditorReady"
            />
          </div>
          <button
            v-if="form.type === 'FILL_BLANK'"
            type="button"
            @click="insertBlank"
            class="google-btn text-btn small-btn mt-2"
          >
            插入空 (_____)
          </button>
        </div>

        <!-- Options for Choice Questions -->
        <div
          v-if="['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(form.type)"
          class="options-section"
        >
          <label>选项</label>
          <div v-for="(_opt, idx) in form.options" :key="idx" class="option-row">
            <span class="option-label">{{ String.fromCharCode(65 + idx) }}.</span>
            <input
              v-model="form.options[idx]"
              type="text"
              required
              placeholder="选项内容"
              class="google-input"
            />
            <button
              type="button"
              @click="removeOption(idx)"
              class="icon-btn remove-opt"
              v-if="form.options.length > 2"
            >
              <svg
                width="20"
                height="20"
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
          <button type="button" @click="addOption" class="google-btn text-btn small-btn">
            添加选项
          </button>
        </div>

        <!-- Fill Blank Answers -->
        <div v-if="form.type === 'FILL_BLANK'" class="options-section">
          <label>正确答案（按顺序）</label>
          <div v-for="(_opt, idx) in form.options" :key="idx" class="option-row">
            <span class="option-label">{{ idx + 1 }}.</span>
            <input
              v-model="form.options[idx]"
              type="text"
              placeholder="填空答案（可选）"
              class="google-input"
            />
          </div>
          <p v-if="form.options.length === 0" class="helper-text">在题干中输入"_____"添加空</p>
        </div>

        <div class="form-group full-width">
          <label>正确答案</label>

          <!-- Single Choice Answer -->
          <div v-if="form.type === 'SINGLE_CHOICE'" class="radio-group">
            <label v-for="(_opt, idx) in form.options" :key="idx" class="radio-label">
              <input
                type="radio"
                :value="String.fromCharCode(65 + idx)"
                v-model="form.answer"
                name="singleAnswer"
              />
              <span class="radio-text">{{ String.fromCharCode(65 + idx) }}</span>
            </label>
          </div>

          <!-- Multiple Choice Answer -->
          <div v-else-if="form.type === 'MULTIPLE_CHOICE'" class="checkbox-group">
            <label v-for="(_opt, idx) in form.options" :key="idx" class="checkbox-label">
              <input type="checkbox" :value="String.fromCharCode(65 + idx)" v-model="multiAnswer" />
              <span class="checkbox-text">{{ String.fromCharCode(65 + idx) }}</span>
            </label>
          </div>

          <!-- True/False Answer -->
          <div v-else-if="form.type === 'TRUE_FALSE'" class="radio-group">
            <label class="radio-label">
              <input type="radio" value="True" v-model="form.answer" name="tfAnswer" />
              <span class="radio-text">正确</span>
            </label>
            <label class="radio-label">
              <input type="radio" value="False" v-model="form.answer" name="tfAnswer" />
              <span class="radio-text">错误</span>
            </label>
          </div>

          <!-- Fill Blank (handled above) -->
          <div v-else-if="form.type === 'FILL_BLANK'">
            <!-- Answers are input in the options section above -->
          </div>

          <!-- Text Answer -->
          <input
            v-else
            v-model="form.answer"
            type="text"
            placeholder="参考答案（可选，用于自动评分或阅卷参考）"
            class="google-input"
          />
        </div>

        <div class="form-group full-width">
          <label>标签（用逗号分隔）</label>
          <input
            v-model="form.tags"
            type="text"
            placeholder="数学, 代数, 一年级"
            class="google-input"
          />
        </div>

        <div class="form-actions">
          <button type="submit" class="google-btn primary-btn" :disabled="loading">
            {{ loading ? '保存中...' : isEditMode ? '保存修改' : '创建题目' }}
          </button>
          <button type="button" @click="$router.back()" class="google-btn text-btn">取消</button>
        </div>
      </form>

      <!-- Toast 提示 -->
      <Transition name="toast">
        <div
          v-if="message || error"
          class="toast"
          :class="{ 'toast-error': error, 'toast-success': message }"
        >
          <svg
            v-if="message"
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
            <polyline points="22 4 12 14.01 9 11.01"></polyline>
          </svg>
          <svg
            v-if="error"
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="15" y1="9" x2="9" y2="15"></line>
            <line x1="9" y1="9" x2="15" y2="15"></line>
          </svg>
          <span>{{ message || error }}</span>
        </div>
      </Transition>
    </div>
    <KnowledgePointDialog
      :is-open="showKPDialog"
      :initial-name="newKPName"
      :parent-options="parentOptions"
      @close="showKPDialog = false"
      @save="saveNewKP"
    />
  </div>
</template>

<style scoped>
.add-question-container {
  max-width: 800px;
  margin: 32px auto;
  animation: slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.form-card {
  padding: 40px;
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.card-header h1 {
  color: var(--line-text-primary);
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.subtitle {
  color: var(--line-text-secondary);
  font-size: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 32px;
}

.form-group {
  margin-bottom: 24px;
}

.full-width {
  grid-column: 1 / -1;
}

label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: var(--line-text-primary);
  font-size: 14px;
  transition: color 0.2s;
}

.helper-text {
  display: block;
  font-size: 12px;
  color: var(--line-text-secondary);
  margin-top: 6px;
}

.editor-wrapper {
  background: var(--line-card-bg);
  border-radius: var(--line-radius-md);
  overflow: hidden;
  transition: box-shadow 0.2s;
  border: 1px solid var(--line-border);
}

.editor-wrapper:focus-within {
  border-color: var(--line-primary);
  box-shadow: 0 0 0 2px var(--line-primary-10);
}

/* Quill Overrides */
:deep(.ql-toolbar) {
  border: none !important;
  border-bottom: 1px solid var(--line-border) !important;
  background-color: var(--line-bg-soft);
  font-family: inherit;
}

:deep(.ql-container) {
  border: none !important;
  font-family: inherit;
  font-size: 16px;
  min-height: 150px;
  color: var(--line-text-primary);
}

:deep(.ql-editor) {
  color: var(--line-text-primary);
}

:deep(.ql-stroke) {
  stroke: var(--line-text-secondary) !important;
}

:deep(.ql-fill) {
  fill: var(--line-text-secondary) !important;
}

:deep(.ql-picker-label) {
  color: var(--line-text-secondary) !important;
}

.options-section {
  margin-bottom: 32px;
  background: var(--line-bg-soft);
  padding: 24px;
  border-radius: var(--line-radius-lg);
  border: 1px dashed var(--line-border);
}

.option-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.option-label {
  font-weight: 600;
  color: var(--line-text-secondary);
  width: 24px;
}

.radio-group,
.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 0;
}

.radio-label,
.checkbox-label {
  position: relative;
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  height: 48px;
  padding: 0 16px;
  border-radius: var(--line-radius-md);
  border: 1px solid var(--line-border);
  background: var(--line-bg-soft);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
}

.radio-label:hover,
.checkbox-label:hover {
  background: var(--line-bg-hover);
  border-color: var(--line-text-secondary);
  color: var(--line-text-primary);
  transform: translateY(-1px);
}

/* Hide native input but keep accessibility if possible (here mostly generic) */
.radio-label input,
.checkbox-label input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

/* Active State */
.radio-label:has(input:checked),
.checkbox-label:has(input:checked) {
  background: var(--line-primary);
  border-color: var(--line-primary);
  box-shadow: var(--line-shadow-md);
}

.radio-text,
.checkbox-text {
  font-size: 16px;
  font-weight: 500;
  color: var(--line-text-secondary);
  transition: color 0.2s;
}

.radio-label:has(input:checked) .radio-text,
.checkbox-label:has(input:checked) .checkbox-text {
  color: #ffffff;
  font-weight: 600;
}

.form-actions {
  margin-top: 32px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
  padding: 16px 0 0;
  border-top: none;
}

.form-actions .google-btn {
  width: 100%;
  height: 44px;
  padding: 0 20px;
}

/* Toast 提示样式 */
.toast {
  position: fixed;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  border-radius: var(--line-radius-full);
  font-size: 14px;
  font-weight: 500;
  box-shadow: var(--line-shadow-lg);
  z-index: 10000;
  background: var(--line-card-bg);
  border: 1px solid var(--line-border);
  color: var(--line-text-primary);
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.toast-success {
  border-color: #10b981;
  color: #059669;
  background: #ecfdf5;
}

.toast-error {
  border-color: #ef4444;
  color: #dc2626;
  background: #fef2f2;
}

.toast svg {
  flex-shrink: 0;
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(20px);
}

.multi-select {
  height: 120px;
  padding: 8px;
}

.mt-2 {
  margin-top: 8px;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px) translateX(-50%);
  }
  to {
    opacity: 1;
    transform: translateY(0) translateX(-50%);
  }
}

.add-question-container {
  animation: fadeIn 0.5s ease-out;
}
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
