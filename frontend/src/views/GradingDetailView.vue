<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft, Check, X, User } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import { examApi } from '@/api/client'
import type { ExamSessionResponse, ManualGradeRequest } from '@/api/generated'
import { useToast } from '@/composables/useToast'
import axios from 'axios'

const { showToast } = useToast()
const route = useRoute()
const router = useRouter()
const examId = route.params.id as string

const exam = ref<ExamSessionResponse | null>(null)
const paperTitle = ref('')
const loading = ref(false)
const error = ref('')
const saving = ref(false)
const aiSuggesting = ref<Record<string, boolean>>({})
const aiBatchSuggesting = ref(false)

// Local state for grades to allow editing
const grades = ref<Record<string, { score: number | null; notes: string; maxScore: number }>>({})

// helper to safely get grade by questionId (coerces id to string)
const gradeOf = (questionId?: string | number) => {
  if (questionId === undefined || questionId === null) return undefined
  return grades.value[String(questionId)]
}

// 计算总得分
const totalScore = computed(() => {
  if (!exam.value?.questions) return 0
  return Object.values(grades.value).reduce((sum, g) => sum + (g.score ?? 0), 0)
})

// 计算满分
const maxTotalScore = computed(() => {
  if (!exam.value?.questions) return 0
  return Object.values(grades.value).reduce((sum, g) => sum + (g.maxScore ?? 0), 0)
})

// 判断题目是否需要手动评分（非选择题）
const needsManualGrading = (type: string | undefined) => {
  const autoTypes = ['SINGLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE']
  return !autoTypes.includes(type || '')
}

// 获取题型中文名
const getTypeName = (type: string | undefined) => {
  const typeMap: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTI_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题'
  }
  return typeMap[type || ''] || type || '未知类型'
}

// 获取正确答案文本
const getCorrectAnswer = (options: any[] | undefined) => {
  if (!options || options.length === 0) return null
  const correct = options.filter((o) => o.isCorrect)
  if (correct.length === 0) return null
  return correct.map((o) => sanitizePlainText(String(o.key || o.text || ''))).join(', ')
}

const sanitizePlainText = (value: string) => {
  if (!value) return ''
  return value
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/锟斤拷/g, '?')
    .trim()
}

const sanitizeRichText = (value: string) => {
  if (!value) return ''
  return value
    .replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
    .replace(/<style[^>]*>[\s\S]*?<\/style>/gi, '')
    .replace(/锟斤拷/g, '?')
}

const handleQuestionDragStart = (q: any, event: DragEvent) => {
  if (!event.dataTransfer) return
  const payload = {
    questionId: q?.questionId,
    type: q?.type,
    stem: sanitizePlainText(String(q?.stem || '')),
    userAnswer: sanitizePlainText(String(q?.userAnswer || '')),
    reference: sanitizePlainText(String(getCorrectAnswer(q?.options) || ''))
  }
  const plain = `题目ID: ${payload.questionId || '-'}\n题型: ${getTypeName(payload.type)}\n题干: ${payload.stem}\n学生答案: ${payload.userAnswer || '(未作答)'}\n参考答案: ${payload.reference || '(无)'}\n`
  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData('application/x-uqbank-question', JSON.stringify(payload))
  event.dataTransfer.setData('text/plain', plain)
}

const fetchExam = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await examApi.apiExamsIdGet(examId)
    exam.value = response.data

    if (exam.value?.paperVersionId) {
      paperTitle.value = `试卷 ${exam.value.paperVersionId}`
    }

    // Initialize grades from existing data
    if (exam.value?.questions) {
      exam.value.questions.forEach((q) => {
        if (q.questionId) {
          const maxScore = Number(q.score) || 1
          grades.value[q.questionId] = {
            score:
              q.awardedScore !== undefined && q.awardedScore !== null
                ? Number(q.awardedScore)
                : q.isCorrect
                  ? maxScore
                  : 0,
            notes: q.graderNotes ?? '',
            maxScore: maxScore
          }
        }
      })
    }
  } catch (err: any) {
    error.value = '加载考试详情失败: ' + (err?.message || '未知错误')
    console.error(err)
  } finally {
    loading.value = false
  }
}

// 一键给满分
const giveFullScore = (questionId: string) => {
  if (grades.value[questionId]) {
    grades.value[questionId].score = grades.value[questionId].maxScore
  }
}

// 一键给零分
const giveZeroScore = (questionId: string) => {
  if (grades.value[questionId]) {
    grades.value[questionId].score = 0
  }
}

const askAiSuggestion = async (q: any) => {
  const questionId = q?.questionId
  if (!questionId || !grades.value[questionId]) {
    return
  }

  aiSuggesting.value[questionId] = true
  try {
    const token = localStorage.getItem('token')
    const response = await axios.post(
      '/api/ai/teacher/subjective-grade',
      {
        questionId,
        studentAnswer: q?.userAnswer || '',
        maxScore: grades.value[questionId].maxScore,
        rubric: grades.value[questionId].notes || ''
      },
      {
        headers: token ? { Authorization: `Bearer ${token}` } : {}
      }
    )

    const aiScoreRaw = Number(response.data?.score)
    const aiScore = Number.isFinite(aiScoreRaw)
      ? Math.max(0, Math.min(grades.value[questionId].maxScore, aiScoreRaw))
      : 0
    grades.value[questionId].score = Number(aiScore.toFixed(1))

    const reason = response.data?.reason || ''
    const feedback = response.data?.feedback || ''
    const mergedNotes = [
      reason ? `AI评分依据：${reason}` : '',
      feedback ? `AI建议评语：${feedback}` : ''
    ]
      .filter(Boolean)
      .join('\n')
      .trim()

    if (mergedNotes) {
      grades.value[questionId].notes = mergedNotes
    }

    const auditId = response.data?.auditId
    if (auditId) {
      await axios.post(
        '/api/ai/audit/accept',
        { auditIds: [auditId] },
        {
          headers: token ? { Authorization: `Bearer ${token}` } : {}
        }
      )
    }

    showToast({ message: 'AI 已生成评分建议，可继续人工微调', type: 'success' })
  } catch (error: any) {
    const msg = error?.response?.data?.error || 'AI 评分建议失败'
    showToast({ message: msg, type: 'error' })
  } finally {
    aiSuggesting.value[questionId] = false
  }
}

const batchAiSuggest = async () => {
  const token = localStorage.getItem('token')
  aiBatchSuggesting.value = true
  try {
    const response = await axios.post(
      '/api/ai/teacher/batch-subjective-grade',
      {
        examId: Number(examId),
        includeAlreadyGraded: false,
        maxQuestions: 120
      },
      {
        headers: token ? { Authorization: `Bearer ${token}` } : {}
      }
    )

    const suggestions: any[] = response.data?.suggestions || []
    const acceptedAuditIds: string[] = []
    suggestions.forEach((item) => {
      const questionId = item?.questionId
      if (!questionId || !grades.value[questionId]) return

      const scoreRaw = Number(item?.score)
      const boundedScore = Number.isFinite(scoreRaw)
        ? Math.max(0, Math.min(grades.value[questionId].maxScore, scoreRaw))
        : 0
      grades.value[questionId].score = Number(boundedScore.toFixed(1))

      const notes = [
        item?.reason ? `AI评分依据：${item.reason}` : '',
        item?.feedback ? `AI建议评语：${item.feedback}` : ''
      ]
        .filter(Boolean)
        .join('\n')
        .trim()

      if (notes) {
        grades.value[questionId].notes = notes
      }

      if (item?.auditId) {
        acceptedAuditIds.push(String(item.auditId))
      }
    })

    if (acceptedAuditIds.length > 0) {
      await axios.post(
        '/api/ai/audit/accept',
        { auditIds: acceptedAuditIds },
        {
          headers: token ? { Authorization: `Bearer ${token}` } : {}
        }
      )
    }

    showToast({ message: `批量 AI 预评分完成：${suggestions.length} 题`, type: 'success' })
  } catch (error: any) {
    const msg = error?.response?.data?.error || '批量 AI 预评分失败'
    showToast({ message: msg, type: 'error' })
  } finally {
    aiBatchSuggesting.value = false
  }
}

const submitGrades = async () => {
  saving.value = true
  try {
    const request: ManualGradeRequest = {
      grades: Object.entries(grades.value).map(([qId, data]) => ({
        questionId: qId,
        score: data.score ?? 0,
        notes: data.notes
      }))
    }

    await examApi.apiExamsSessionIdGradePost(examId, request)
    showToast({ message: '评分提交成功！', type: 'success' })
    router.push('/grading')
  } catch (err) {
    showToast({ message: '提交评分失败', type: 'error' })
    console.error(err)
  } finally {
    saving.value = false
  }
}

onMounted(fetchExam)
</script>

<template>
  <div class="grading-container">
    <div class="header-row">
      <button @click="router.back()" class="line-btn outline-btn back-btn">
        <ArrowLeft :size="16" aria-hidden="true" />
        返回
      </button>
      <h1 class="page-title">阅卷: 考试 #{{ examId }}</h1>
      <div class="top-actions">
        <button
          @click="batchAiSuggest"
          class="line-btn outline-btn sm"
          :disabled="aiBatchSuggesting || !exam?.questions?.length"
        >
          {{ aiBatchSuggesting ? 'AI批量建议中...' : 'AI批量预评分' }}
        </button>
        <button
          @click="submitGrades"
          class="line-btn primary-btn"
          :disabled="saving || !exam?.questions?.length"
        >
          {{ saving ? '保存中...' : '提交评分' }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button @click="fetchExam" class="google-btn secondary-btn">重试</button>
    </div>

    <div v-else-if="exam" class="exam-content">
      <!-- 基本信息卡片 -->
      <div class="info-card google-card">
        <div class="info-grid">
          <div class="info-item">
            <span class="label">考生</span>
            <span class="value">{{
              (exam as any).nickname || (exam as any).username || (exam as any).userId
            }}</span>
          </div>
          <div class="info-item">
            <span class="label">试卷</span>
            <span class="value">{{ paperTitle || exam.paperVersionId }}</span>
          </div>
          <div class="info-item">
            <span class="label">开始时间</span>
            <span class="value">{{
              exam.startAt ? new Date(exam.startAt).toLocaleString() : '-'
            }}</span>
          </div>
          <div class="info-item">
            <span class="label">提交时间</span>
            <span class="value">{{
              exam.endAt ? new Date(exam.endAt).toLocaleString() : '-'
            }}</span>
          </div>
        </div>
        <div class="score-summary">
          <span class="current-score"
            >当前得分: <strong>{{ totalScore.toFixed(1) }}</strong></span
          >
          <span class="max-score">/ {{ maxTotalScore }} 分</span>
        </div>
      </div>

      <!-- 无题目提示 -->
      <div v-if="!exam.questions || exam.questions.length === 0" class="empty-state google-card">
        <p class="empty-title">
          <svg
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
            <path d="M9 2h6"></path>
            <path d="M12 17v-6"></path>
            <path d="M9 22h6"></path>
            <path d="M17 2l-5 5-5-5"></path>
            <path d="M17 22l-5-5-5 5"></path>
          </svg>
          该考试暂无题目数据
        </p>
        <p class="hint">可能的原因：试卷数据已被删除或考试未完成</p>
      </div>

      <!-- 题目列表 -->
      <div
        v-for="(q, index) in exam.questions"
        :key="q.questionId"
        class="question-card google-card"
        draggable="true"
        @dragstart="handleQuestionDragStart(q, $event)"
      >
        <div class="q-header">
          <span class="q-num">第{{ index + 1 }}题</span>
          <span class="q-type" :class="q.type?.toLowerCase()">{{ getTypeName(q.type) }}</span>
          <span class="q-score-badge">满分: {{ q.score }} 分</span>
          <!-- 对于需人工评分的题目，优先根据本地 grades 显示是否已评分 -->
          <template v-if="needsManualGrading(q.type)">
            <span v-if="gradeOf(q.questionId)?.score !== null && gradeOf(q.questionId)?.score !== undefined" class="q-result scored">
              <Check :size="14" /> 已评分
            </span>
            <span v-else class="q-result pending">待评分</span>
          </template>
          <!-- 自动判分题仍按 q.isCorrect 展示 -->
          <template v-else>
            <span v-if="q.isCorrect === true" class="q-result correct"><Check :size="14" /> 正确</span>
            <span v-else-if="q.isCorrect === false" class="q-result wrong"><X :size="14" /> 错误</span>
            <span v-else class="q-result pending">待评分</span>
          </template>
        </div>

        <div class="q-stem" v-html="q.stem"></div>

        <!-- 选择题选项 -->
        <div class="q-options" v-if="q.options && q.options.length > 0">
          <div
            v-for="opt in q.options"
            :key="opt.text || opt.key"
            class="option-item"
            :class="{
              correct: opt.isCorrect,
              selected: q.userAnswer === opt.text || q.userAnswer === opt.key,
              'wrong-selected':
                (q.userAnswer === opt.text || q.userAnswer === opt.key) && !opt.isCorrect
            }"
          >
            <span class="option-key" v-if="opt.key">{{ opt.key }}.</span>
            <span class="option-text" v-html="sanitizeRichText(String(opt.text || ''))"></span>
            <span v-if="opt.isCorrect" class="badge-correct"><Check :size="14" /> 正确答案</span>
            <span v-if="q.userAnswer === opt.text || q.userAnswer === opt.key" class="badge-user">
              <User :size="14" /> 用户选择
            </span>
          </div>
        </div>

        <!-- 非选择题答案 -->
        <div class="answer-section" v-if="!q.options || q.options.length === 0">
          <div class="correct-answer">
            <strong>参考答案:</strong>
            <span v-if="getCorrectAnswer(q.options)">{{ getCorrectAnswer(q.options) }}</span>
            <span v-else class="text-gray-500 italic">（无参考答案，请根据实际情况评分）</span>
          </div>
          <div class="user-answer-box">
            <strong>学生答案:</strong>
            <pre class="user-answer-content">{{ q.userAnswer || '(未作答)' }}</pre>
          </div>
        </div>

        <!-- 评分区域 -->
        <div class="grading-section" v-if="gradeOf(q.questionId)">
          <div class="grading-header">
            <span class="grading-title">评分</span>
            <div class="quick-actions" v-if="needsManualGrading(q.type)">
              <button
                @click="askAiSuggestion(q)"
                :disabled="!!aiSuggesting[q.questionId!]"
                class="line-btn outline-btn sm ai-btn action-btn"
              >
                {{ aiSuggesting[q.questionId!] ? 'AI建议中...' : 'AI建议' }}
              </button>
              <button @click="giveFullScore(q.questionId!)" class="line-btn outline-btn sm full-btn action-btn">满分</button>
              <button @click="giveZeroScore(q.questionId!)" class="line-btn outline-btn sm zero-btn action-btn">零分</button>
            </div>
          </div>

          <div class="grading-layout">
            <div class="score-column">
              <label class="score-label">得分</label>
              <div class="score-box">
                <input
                  type="number"
                  v-model.number="grades[q.questionId!]!.score"
                  class="score-input google-input"
                  step="0.5"
                  :min="0"
                  :max="grades[q.questionId!]?.maxScore"
                />
                <div class="score-max">/ {{ grades[q.questionId!]?.maxScore }}</div>
              </div>
            </div>

            <div class="notes-column">
              <label class="notes-label">评语 (可选)</label>
              <textarea
                v-model="grades[q.questionId!]!.notes"
                class="notes-textarea google-input"
                rows="1"
                wrap="off"
                placeholder="输入评语或批注..."
              ></textarea>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部提交按钮 -->
      <div class="bottom-actions" v-if="exam.questions && exam.questions.length > 0">
        <div class="final-score">
          总得分: <strong>{{ totalScore.toFixed(1) }}</strong> / {{ maxTotalScore }} 分
        </div>
        <button @click="submitGrades" class="google-btn primary-btn" :disabled="saving">
          {{ saving ? '保存中...' : '提交评分' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.grading-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}

.header-row h1 {
  margin: 0;
  color: var(--line-text);
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.google-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.question-card[draggable='true'] {
  cursor: grab;
}

.question-card[draggable='true']:active {
  cursor: grabbing;
}

.info-card .info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
  margin-bottom: 15px;
}

.info-item {
  display: flex;
  flex-direction: column;
}

.info-item .label {
  font-size: 0.85em;
  color: var(--line-text-secondary);
  margin-bottom: 4px;
}

.info-item .value {
  font-weight: 500;
  color: var(--line-text);
}

.score-summary {
  padding-top: 15px;
  border-top: 1px solid #e0e0e0;
  font-size: 1.1em;
}

.current-score strong {
  color: var(--line-primary);
  font-size: 1.3em;
}

.max-score {
  color: var(--line-text-secondary);
}

.q-header {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.q-num {
  font-weight: bold;
  color: var(--line-text);
  font-size: 1.1em;
}

.q-type {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.85em;
  background: rgba(26, 115, 232, 0.1);
  color: var(--line-primary);
}

.q-type.single_choice {
  background: rgba(26, 115, 232, 0.1);
  color: var(--line-primary);
}
.q-type.multi_choice {
  background: #fce8e6;
  color: #d93025;
}
.q-type.true_false {
  background: #e6f4ea;
  color: #34a853;
}
.q-type.fill_blank {
  background: #fef7e0;
  color: #f9ab00;
}
.q-type.short_answer {
  background: #f3e8fd;
  color: #9334e6;
}

.q-score-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.85em;
  background: var(--line-bg-soft);
  color: var(--line-text-secondary);
}

/* Result badge - themed and compact */
.q-result {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 0.9em;
  font-weight: 600;
  border: 1px solid transparent;
  min-width: 76px;
}

.q-result.correct {
  background: color-mix(in srgb, var(--line-success) 10%, white);
  color: var(--line-success);
  border-color: color-mix(in srgb, var(--line-success) 16%, transparent);
}

.q-result.wrong {
  background: color-mix(in srgb, var(--line-error) 10%, white);
  color: var(--line-error);
  border-color: color-mix(in srgb, var(--line-error) 16%, transparent);
}

.q-result.pending {
  background: color-mix(in srgb, var(--line-warning) 12%, white);
  color: var(--line-warning);
  border-color: color-mix(in srgb, var(--line-warning) 16%, transparent);
}

.q-result.scored {
  background: color-mix(in srgb, var(--line-primary) 8%, white);
  color: var(--line-primary);
  border-color: color-mix(in srgb, var(--line-primary) 16%, transparent);
}

.q-stem {
  font-size: 1.05em;
  margin-bottom: 15px;
  line-height: 1.6;
  color: var(--line-text);
}

.q-options {
  margin-bottom: 15px;
}

.option-item {
  padding: 12px 15px;
  border: 1px solid var(--line-border);
  border-radius: 8px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: all 0.2s;
}

.option-key {
  font-weight: bold;
  color: var(--line-text-secondary);
  min-width: 24px;
}

.option-text {
  flex: 1;
}

.option-item.correct {
  background-color: #e6f4ea;
  border-color: #34a853;
}

.option-item.selected {
  border-color: var(--line-primary);
  background-color: rgba(26, 115, 232, 0.1);
}

.option-item.wrong-selected {
  background-color: #fce8e6;
  border-color: #d93025;
}

.badge-correct {
  color: #34a853;
  font-size: 0.8em;
  font-weight: bold;
  margin-left: auto;
}

.badge-user {
  color: var(--line-primary);
  font-size: 0.8em;
  font-weight: bold;
}

.answer-section {
  margin-bottom: 15px;
}

.correct-answer {
  padding: 10px 15px;
  background: #e6f4ea;
  border-radius: 6px;
  margin-bottom: 10px;
}

.user-answer-box {
  padding: 10px 15px;
  background: var(--line-bg-soft);
  border-radius: 6px;
  border: 1px solid var(--line-border);
}

.user-answer-content {
  margin: 8px 0 0 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  background: white;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
}

.grading-section {
  margin-top: 15px;
  padding: 15px;
  background: var(--line-bg-soft);
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.grading-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 500;
  color: var(--line-text);
}

.quick-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

/* Small outline action buttons (reuse global outline-btn behavior) */
.line-btn.outline-btn.sm {
  padding: 6px 12px;
  font-size: 13px;
}

.back-btn {
  padding: 8px 12px;
}

.back-btn .icon {
  font-weight: 700;
  display: inline-block;
  line-height: 1;
}

.ai-btn {
  border-color: var(--line-primary) !important;
  color: var(--line-primary) !important;
}
.ai-btn:hover:not(:disabled) {
  background: var(--line-primary) !important;
  color: #fff !important;
}

.full-btn {
  border-color: var(--line-success) !important;
  color: var(--line-success) !important;
  background: color-mix(in srgb, var(--line-success) 6%, white);
}
.full-btn:hover:not(:disabled) {
  background: var(--line-success) !important;
  color: #fff !important;
}

.zero-btn {
  border-color: var(--line-error) !important;
  color: var(--line-error) !important;
  background: color-mix(in srgb, var(--line-error) 6%, white);
}
.zero-btn:hover:not(:disabled) {
  background: var(--line-error) !important;
  color: #fff !important;
}

.grading-controls {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.control-group {
  flex: 1;
  min-width: 150px;
}

.control-group.score-input {
  flex: 0 0 150px;
}

.control-group.notes-input {
  flex: 2;
  min-width: 250px;
}

.control-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  font-size: 0.9em;
  color: var(--line-text-secondary);
}

.score-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-wrapper input {
  width: 80px;
}

.score-max {
  color: var(--line-text-secondary);
  font-size: 0.9em;
}

.google-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--line-border);
  border-radius: 4px;
  font-size: 1em;
  transition: border-color 0.2s;
}

.google-input:focus {
  outline: none;
  border-color: var(--line-primary);
}

/* Grading layout improvements */
.grading-section {
  margin-top: 15px;
  padding: 14px;
  background: var(--line-bg);
  border-radius: 8px;
  border: 1px solid var(--line-border);
}

.grading-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.grading-title {
  font-weight: 600;
  color: var(--line-text);
}

.grading-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.score-column {
  width: 170px;
  min-width: 140px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.score-label {
  font-size: 0.9em;
  color: var(--line-text-secondary);
}

.score-box {
  display: flex;
  align-items: center;
  gap: 10px;
}

.score-input {
  width: 92px;
  padding: 8px 10px;
  height: 40px;
  box-sizing: border-box;
}

.score-max {
  color: var(--line-text-secondary);
  font-size: 0.95em;
}

.notes-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notes-label {
  font-size: 0.9em;
  color: var(--line-text-secondary);
}

.notes-textarea {
  height: 40px;
  min-height: 40px;
  max-height: 40px;
  resize: none;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: pre;
}

.action-btn {
  min-height: 36px;
  padding: 6px 12px;
}

@media (max-width: 700px) {
  .grading-layout {
    flex-direction: column;
  }
  .score-column {
    width: 100%;
    min-width: 0;
    flex-direction: row;
    align-items: center;
  }
  .score-label {
    width: 80px;
  }
  .score-box {
    flex: 1;
  }
}

.google-btn {
  padding: 10px 20px;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  font-weight: 500;
  font-size: 0.95em;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.google-btn.primary {
  background: var(--line-primary);
  color: white;
}

.google-btn.primary:hover {
  background: #1557b0;
}

.google-btn.primary:disabled {
  background: #a0c3ff;
  cursor: not-allowed;
}

.google-btn.secondary {
  background: var(--line-bg-soft);
  color: var(--line-text);
}

.google-btn.secondary:hover {
  background: var(--line-bg-soft);
}

.google-btn.large {
  padding: 12px 32px;
  font-size: 1.05em;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--line-text-secondary);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid var(--line-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 15px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.error-state {
  text-align: center;
  padding: 40px;
  color: #d93025;
  background: #fce8e6;
  border-radius: 8px;
}

.error-state button {
  margin-top: 15px;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: var(--line-text-secondary);
}

.empty-state p {
  margin: 10px 0;
}

.empty-state .empty-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--line-text);
}

.empty-state .hint {
  font-size: 0.9em;
  color: #80868b;
}

.bottom-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  position: sticky;
  bottom: 20px;
}

.final-score {
  font-size: 1.1em;
}

.final-score strong {
  color: var(--line-primary);
  font-size: 1.3em;
}

@media (max-width: 600px) {
  .grading-controls {
    flex-direction: column;
  }

  .control-group.score-input {
    flex: 1;
  }

  .bottom-actions {
    flex-direction: column;
    gap: 15px;
  }
}
</style>
