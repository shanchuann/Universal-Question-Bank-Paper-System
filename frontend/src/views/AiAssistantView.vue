<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import axios from 'axios'
import { useToast } from '@/composables/useToast'

const props = defineProps<{ mode: 'teacher' | 'student' }>()
const { showToast } = useToast()

interface ChatItem {
  role: 'user' | 'assistant'
  content: string
  timestamp: string
}

interface WeakPointItem {
  knowledgePointId: string
  knowledgePointName: string
  wrongCount: number
  attemptCount: number
  wrongRate: number
}

const loading = ref(false)
const checkingStatus = ref(false)
const aiEnabled = ref(false)
const modelName = ref('gemma4')
const messages = ref<ChatItem[]>([])
const question = ref('')
const contextText = ref('')
const streamRef = ref<HTMLElement | null>(null)
const weakPoints = ref<WeakPointItem[]>([])
const recentWrongQuestions = ref<string[]>([])
const autoContext = ref('')
const auditSummary = ref<Record<string, any> | null>(null)

const pageTitle = computed(() => (props.mode === 'teacher' ? 'AI教师助手' : 'AI学习助手'))

const placeholder = computed(() =>
  props.mode === 'teacher'
    ? '例如：请给我一套“离散数学-关系与函数”30分钟测验出题建议'
    : '例如：请帮我理解二叉树中序遍历，并给两个练习题'
)

const getAuthHeaders = () => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const askEndpoint = computed(() =>
  props.mode === 'teacher' ? '/api/ai/teacher/ask' : '/api/ai/student/ask'
)

const saveHistory = () => {
  const key = props.mode === 'teacher' ? 'ai.teacher.history' : 'ai.student.history'
  localStorage.setItem(key, JSON.stringify(messages.value.slice(-40)))
}

const loadHistory = () => {
  const key = props.mode === 'teacher' ? 'ai.teacher.history' : 'ai.student.history'
  try {
    const raw = localStorage.getItem(key)
    messages.value = raw ? JSON.parse(raw) : []
  } catch {
    messages.value = []
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (streamRef.value) {
    streamRef.value.scrollTop = streamRef.value.scrollHeight
  }
}

const checkStatus = async () => {
  checkingStatus.value = true
  try {
    const response = await axios.get('/api/ai/status', {
      headers: getAuthHeaders()
    })
    aiEnabled.value = Boolean(response.data?.enabled)
    modelName.value = response.data?.model || 'gemma4'
  } catch {
    aiEnabled.value = false
  } finally {
    checkingStatus.value = false
  }
}

const clearMessages = () => {
  messages.value = []
  saveHistory()
}

const loadAuditSummary = async () => {
  if (props.mode !== 'teacher') {
    return
  }
  try {
    const response = await axios.get('/api/ai/audit/summary?days=30', {
      headers: getAuthHeaders()
    })
    auditSummary.value = response.data || null
  } catch {
    auditSummary.value = null
  }
}

const sendQuestion = async () => {
  const q = question.value.trim()
  if (!q) {
    showToast({ message: '请输入你的问题', type: 'warning' })
    return
  }

  const userMessage: ChatItem = {
    role: 'user',
    content: q,
    timestamp: new Date().toISOString()
  }
  messages.value.push(userMessage)
  question.value = ''
  await scrollToBottom()

  loading.value = true
  try {
    const response = await axios.post(
      askEndpoint.value,
      {
        question: q,
        context: contextText.value
      },
      { headers: getAuthHeaders() }
    )

    messages.value.push({
      role: 'assistant',
      content: response.data?.answer || 'AI 未返回有效内容',
      timestamp: new Date().toISOString()
    })

    if (props.mode === 'student') {
      weakPoints.value = response.data?.weakPoints || []
      recentWrongQuestions.value = response.data?.recentWrongQuestions || []
      autoContext.value = response.data?.autoContext || ''
    }

    if (props.mode === 'teacher') {
      await loadAuditSummary()
    }

    saveHistory()
    await scrollToBottom()
  } catch (error: any) {
    const msg = error?.response?.data?.error || 'AI 请求失败，请稍后重试'
    messages.value.push({
      role: 'assistant',
      content: `请求失败：${msg}`,
      timestamp: new Date().toISOString()
    })
    showToast({ message: msg, type: 'error' })
    saveHistory()
    await scrollToBottom()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  loadHistory()
  await checkStatus()
  await loadAuditSummary()
  await scrollToBottom()
})
</script>

<template>
  <div class="ai-assistant-page">
    <section class="line-card header-card">
      <div>
        <h1>{{ pageTitle }}</h1>
        <p>
          当前模型：{{ modelName }}
          <span v-if="checkingStatus">（检测中）</span>
          <span v-else-if="!aiEnabled" class="warning">（未启用）</span>
        </p>
      </div>
      <div class="header-actions">
        <button class="line-btn" @click="checkStatus">刷新状态</button>
        <button class="line-btn" @click="clearMessages">清空会话</button>
      </div>
    </section>

    <section v-if="props.mode === 'teacher' && auditSummary" class="line-card summary-card">
      <h3>AI 采纳率（最近 {{ auditSummary.days }} 天）</h3>
      <div class="summary-grid">
        <div class="summary-item">
          <span class="label">教师问答</span>
          <span class="value">{{ auditSummary.teacherAsk?.total || 0 }}</span>
        </div>
        <div class="summary-item">
          <span class="label">主观题单题建议</span>
          <span class="value">{{ auditSummary.subjectiveSingle?.total || 0 }}</span>
        </div>
        <div class="summary-item">
          <span class="label">主观题批量建议</span>
          <span class="value">{{ auditSummary.subjectiveBatch?.total || 0 }}</span>
        </div>
        <div class="summary-item">
          <span class="label">评分建议采纳率</span>
          <span class="value">
            {{
              Number(auditSummary.subjectiveSingle?.acceptRate || 0) >
              Number(auditSummary.subjectiveBatch?.acceptRate || 0)
                ? auditSummary.subjectiveSingle?.acceptRate
                : auditSummary.subjectiveBatch?.acceptRate
            }}%
          </span>
        </div>
      </div>
    </section>

    <section
      v-if="props.mode === 'student' && (weakPoints.length > 0 || recentWrongQuestions.length > 0)"
      class="line-card weak-card"
    >
      <h3>自动注入的学习弱点上下文</h3>
      <p class="muted">提问时系统会自动拼接以下薄弱点信息，回答会更贴近你的问题。</p>

      <div v-if="weakPoints.length > 0" class="weak-point-list">
        <div v-for="item in weakPoints" :key="item.knowledgePointId" class="weak-item">
          <span class="name">{{ item.knowledgePointName }}</span>
          <span class="meta"
            >错误 {{ item.wrongCount }}/{{ item.attemptCount }}（{{ item.wrongRate }}%）</span
          >
        </div>
      </div>

      <div v-if="recentWrongQuestions.length > 0" class="wrong-question-list">
        <p class="title">近期高频错题摘录</p>
        <p v-for="(stem, idx) in recentWrongQuestions" :key="idx" class="stem">- {{ stem }}</p>
      </div>

      <details v-if="autoContext" class="auto-context">
        <summary>查看拼接后的上下文</summary>
        <pre>{{ autoContext }}</pre>
      </details>
    </section>

    <section class="line-card content-card">
      <div class="context-box">
        <label>上下文（可选）</label>
        <textarea
          v-model="contextText"
          class="line-input"
          rows="3"
          placeholder="可填写课程、班级、知识点、评分标准等背景信息"
        ></textarea>
      </div>

      <div ref="streamRef" class="chat-stream">
        <div v-if="messages.length === 0" class="empty">
          开始提问吧，AI 会根据你的上下文给出建议。
        </div>
        <div
          v-for="(item, index) in messages"
          :key="`${item.timestamp}-${index}`"
          class="chat-row"
          :class="{ mine: item.role === 'user' }"
        >
          <div class="bubble" :class="item.role">
            <p>{{ item.content }}</p>
          </div>
        </div>
      </div>

      <div class="composer">
        <textarea
          v-model="question"
          class="line-input"
          rows="4"
          :placeholder="placeholder"
          :disabled="loading"
        ></textarea>
        <button
          class="line-btn primary-btn send-btn"
          :disabled="loading || !aiEnabled"
          @click="sendQuestion"
        >
          {{ loading ? 'AI 思考中...' : '发送问题' }}
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.ai-assistant-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.header-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
}

.header-card h1 {
  margin: 0;
  font-size: 24px;
}

.header-card p {
  margin: 6px 0 0;
  color: var(--line-text-secondary);
}

.header-actions {
  display: flex;
  gap: 10px;
}

.warning {
  color: var(--line-warning);
}

.summary-card h3,
.weak-card h3 {
  margin: 0 0 8px;
  font-size: 18px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 10px;
}

.summary-item {
  border: 1px solid var(--line-border);
  border-radius: 10px;
  padding: 10px;
  background: var(--line-bg-soft);
}

.summary-item .label {
  display: block;
  color: var(--line-text-secondary);
  font-size: 12px;
}

.summary-item .value {
  display: block;
  margin-top: 4px;
  color: var(--line-text);
  font-size: 18px;
  font-weight: 700;
}

.muted {
  margin: 0 0 10px;
  color: var(--line-text-secondary);
  font-size: 13px;
}

.weak-point-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.weak-item {
  border: 1px solid var(--line-border);
  border-radius: 999px;
  padding: 6px 10px;
  display: inline-flex;
  gap: 8px;
  align-items: center;
  background: var(--line-bg-soft);
}

.weak-item .name {
  font-weight: 600;
}

.weak-item .meta {
  color: var(--line-text-secondary);
  font-size: 12px;
}

.wrong-question-list {
  margin-top: 10px;
}

.wrong-question-list .title {
  margin: 0 0 6px;
  color: var(--line-text-secondary);
  font-size: 13px;
}

.wrong-question-list .stem {
  margin: 0 0 4px;
}

.auto-context {
  margin-top: 8px;
}

.auto-context pre {
  white-space: pre-wrap;
  background: var(--line-bg-soft);
  border: 1px solid var(--line-border);
  border-radius: 10px;
  padding: 8px;
  margin-top: 6px;
}

.content-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.context-box label {
  display: block;
  margin-bottom: 8px;
  color: var(--line-text-secondary);
  font-size: 13px;
}

.chat-stream {
  min-height: 360px;
  max-height: 520px;
  overflow: auto;
  border: 1px solid var(--line-border);
  border-radius: 12px;
  background: var(--line-bg-soft);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.empty {
  color: var(--line-text-secondary);
  font-size: 14px;
}

.chat-row {
  display: flex;
}

.chat-row.mine {
  justify-content: flex-end;
}

.bubble {
  max-width: min(78%, 820px);
  border-radius: 12px;
  padding: 10px 12px;
  border: 1px solid var(--line-border);
}

.bubble.user {
  background: var(--line-primary);
  color: white;
  border-color: var(--line-primary);
}

.bubble.assistant {
  background: var(--line-bg);
  color: var(--line-text);
}

.bubble p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.5;
}

.composer {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.send-btn {
  align-self: flex-end;
  min-width: 140px;
}

@media (max-width: 900px) {
  .header-card {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
  }

  .send-btn {
    width: 100%;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }
}
</style>
