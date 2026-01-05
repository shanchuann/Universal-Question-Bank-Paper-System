<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { examApi } from '@/api/client'
import type { ExamSessionResponse, ManualGradeRequest } from '@/api/generated'

const route = useRoute()
const router = useRouter()
const examId = route.params.id as string

const exam = ref<ExamSessionResponse | null>(null)
const loading = ref(false)
const error = ref('')
const saving = ref(false)

// Local state for grades to allow editing
const grades = ref<Record<string, { score: number | null, notes: string }>>({})

const fetchExam = async () => {
  loading.value = true
  try {
    const response = await examApi.apiExamsIdGet(examId)
    exam.value = response.data
    
    // Initialize grades from existing data
    if (exam.value?.questions) {
      exam.value.questions.forEach(q => {
        if (q.questionId) {
          grades.value[q.questionId] = {
            score: q.awardedScore ?? null,
            notes: q.graderNotes ?? ''
          }
        }
      })
    }
  } catch (err) {
    error.value = '加载考试详情失败'
    console.error(err)
  } finally {
    loading.value = false
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
    alert('评分提交成功')
    router.push('/grading')
  } catch (err) {
    alert('提交评分失败')
    console.error(err)
  } finally {
    saving.value = false
  }
}

onMounted(fetchExam)
</script>

<template>
  <div class="container">
    <div class="header-row">
      <button @click="router.back()" class="google-btn secondary">返回</button>
      <h1>阅卷: 考试 {{ examId }}</h1>
      <button @click="submitGrades" class="google-btn primary" :disabled="saving">
        {{ saving ? '保存中...' : '提交评分' }}
      </button>
    </div>

    <div v-if="loading" class="loading-state">加载中...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    
    <div v-else-if="exam" class="exam-content">
      <div class="info-card google-card">
        <p><strong>用户:</strong> {{ (exam as any).nickname || (exam as any).username || (exam as any).userId }}</p>
        <p><strong>试卷:</strong> {{ exam.paperVersionId }}</p>
        <p><strong>总分:</strong> {{ (exam as any).score ?? '待评分' }}</p>
      </div>

      <div v-for="(q, index) in exam.questions" :key="q.questionId" class="question-card google-card">
        <div class="q-header">
          <span class="q-num">第{{ index + 1 }}题</span>
          <span class="q-type">{{ q.type }}</span>
          <span class="q-score-badge">满分: {{ q.score }}</span>
        </div>
        
        <div class="q-stem" v-html="q.stem"></div>
        
        <div class="q-options" v-if="q.options && q.options.length > 0">
          <div v-for="opt in q.options" :key="opt.text" 
               class="option-item"
               :class="{ 
                 'correct': opt.isCorrect,
                 'selected': q.userAnswer === opt.text
               }">
            {{ opt.text }}
            <span v-if="opt.isCorrect" class="badge-correct">正确答案</span>
            <span v-if="q.userAnswer === opt.text" class="badge-user">用户答案</span>
          </div>
        </div>
        
        <div class="grading-section">
           <div class="user-answer-display" v-if="!q.options || q.options.length === 0">
             <strong>用户答案:</strong>
             <pre>{{ q.userAnswer || '(未作答)' }}</pre>
           </div>
           
           <div class="grading-controls" v-if="q.questionId && grades[q.questionId]">
             <div class="control-group">
               <label>得分</label>
               <input type="number" v-model.number="grades[q.questionId!]!.score" class="google-input" step="0.5" />
             </div>
             <div class="control-group">
               <label>评语</label>
               <textarea v-model="grades[q.questionId!]!.notes" class="google-input" rows="2"></textarea>
             </div>
           </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.container { max-width: 800px; margin: 0 auto; padding: 20px; }
.header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.google-card { background: white; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
.q-header { display: flex; gap: 10px; margin-bottom: 10px; font-weight: bold; color: #5f6368; }
.q-stem { font-size: 1.1em; margin-bottom: 15px; }
.option-item { padding: 10px; border: 1px solid #dadce0; border-radius: 4px; margin-bottom: 5px; display: flex; justify-content: space-between; }
.option-item.correct { background-color: #e6f4ea; border-color: #34a853; }
.option-item.selected { border-color: #1a73e8; background-color: #e8f0fe; }
.badge-correct { color: #34a853; font-size: 0.8em; font-weight: bold; }
.badge-user { color: #1a73e8; font-size: 0.8em; font-weight: bold; }
.grading-section { margin-top: 20px; padding-top: 20px; border-top: 1px solid #eee; background: #f8f9fa; padding: 15px; border-radius: 4px; }
.grading-controls { display: flex; gap: 20px; margin-top: 10px; }
.control-group { flex: 1; }
.control-group label { display: block; margin-bottom: 5px; font-weight: 500; }
.google-input { width: 100%; padding: 8px; border: 1px solid #dadce0; border-radius: 4px; }
.google-btn { padding: 8px 16px; border-radius: 4px; border: none; cursor: pointer; font-weight: 500; }
.google-btn.primary { background: #1a73e8; color: white; }
.google-btn.secondary { background: #f1f3f4; color: #3c4043; }
.loading-state, .error-state { text-align: center; padding: 40px; color: #5f6368; }
.error-state { color: #d93025; }
</style>