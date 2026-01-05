<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { questionApi, knowledgePointApi } from '@/api/client'
import type { QuestionSummary, KnowledgePoint } from '@/api/generated'
import { useBasket } from '@/stores/basket'
import { useRouter } from 'vue-router'
import axios from 'axios'
import GoogleSelect from '@/components/GoogleSelect.vue'

const questions = ref<QuestionSummary[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(0)
const size = ref(10)
const totalElements = ref(0)
const { basket, addToBasket, removeFromBasket, isInBasket, clearBasket } = useBasket()
const router = useRouter()
const showRestorePrompt = ref(false)

// 分类统计
const categoryStats = ref({
  total: 0,
  singleChoice: 0,
  multiChoice: 0,
  trueFalse: 0,
  fillBlank: 0,
  shortAnswer: 0
})

// Filters
const filterKnowledgePoint = ref<string>('')
const filterType = ref<string>('')
const filterDifficulty = ref<string>('')
const knowledgePoints = ref<KnowledgePoint[]>([])
const flattenedKnowledgePoints = ref<{ id: string, label: string }[]>([])

const difficultyOptions = [
  { label: '全部难度', value: '' },
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '困难', value: 'HARD' }
]

const kpOptions = computed(() => {
  return [
    { label: '全部知识点', value: '' },
    ...flattenedKnowledgePoints.value.map(kp => ({ label: kp.label, value: kp.id }))
  ]
})

const fetchKnowledgePoints = async () => {
  try {
    const response = await knowledgePointApi.apiKnowledgePointsGet()
    knowledgePoints.value = response.data
    flattenedKnowledgePoints.value = flattenPoints(knowledgePoints.value)
  } catch (err) {
    console.error('Failed to load knowledge points', err)
  }
}

const flattenPoints = (points: KnowledgePoint[], level = 0): { id: string, label: string }[] => {
  let result: { id: string, label: string }[] = []
  for (const point of points) {
    const prefix = '\u00A0'.repeat(level * 4)
    result.push({ id: point.id!, label: `${prefix}${point.name}` })
    if (point.children && point.children.length > 0) {
      result = result.concat(flattenPoints(point.children, level + 1))
    }
  }
  return result
}

const getDescendantIds = (points: KnowledgePoint[], targetId: string): string[] => {
  let ids: string[] = []
  for (const point of points) {
    if (point.id === targetId) {
      ids.push(point.id)
      if (point.children) {
        ids = ids.concat(getAllIds(point.children))
      }
      return ids
    }
    if (point.children) {
      const found = getDescendantIds(point.children, targetId)
      if (found.length > 0) {
        return found
      }
    }
  }
  return []
}

const getAllIds = (points: KnowledgePoint[]): string[] => {
  let ids: string[] = []
  for (const point of points) {
    if (point.id) ids.push(point.id)
    if (point.children) {
      ids = ids.concat(getAllIds(point.children))
    }
  }
  return ids
}

// 获取分类统计数据
const fetchCategoryStats = async () => {
  try {
    // 获取各类型的数量 (只统计 APPROVED 状态)
    const types = ['SINGLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE', 'FILL_BLANK', 'SHORT_ANSWER']
    const responses = await Promise.all([
      // 总数
      questionApi.apiQuestionsGet(0, 1, undefined, undefined, undefined, undefined, undefined, 'APPROVED'),
      // 各类型数量
      ...types.map(type => 
        questionApi.apiQuestionsGet(0, 1, undefined, undefined, type as any, undefined, undefined, 'APPROVED')
      )
    ])
    
    categoryStats.value = {
      total: responses[0].data.totalElements || 0,
      singleChoice: responses[1].data.totalElements || 0,
      multiChoice: responses[2].data.totalElements || 0,
      trueFalse: responses[3].data.totalElements || 0,
      fillBlank: responses[4].data.totalElements || 0,
      shortAnswer: responses[5].data.totalElements || 0
    }
  } catch (err) {
    console.error('Failed to fetch category stats', err)
  }
}

const fetchQuestions = async () => {
  loading.value = true
  error.value = ''
  try {
    let kpIds: string[] | undefined = undefined
    if (filterKnowledgePoint.value) {
      kpIds = getDescendantIds(knowledgePoints.value, filterKnowledgePoint.value)
    }

    const response = await questionApi.apiQuestionsGet(
      page.value, 
      size.value, 
      undefined, // subjectId
      kpIds,
      filterType.value as any,
      filterDifficulty.value as any,
      undefined, // keywords
      'APPROVED' // status - 只显示已通过的题目
    )
    questions.value = response.data.content || []
    totalElements.value = response.data.totalElements || 0
  } catch (err) {
    error.value = '加载题目失败'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const toggleBasket = (id: string) => {
  if (isInBasket(id)) {
    removeFromBasket(id)
  } else {
    addToBasket(id)
  }
}

const isAllSelected = computed(() => {
  if (questions.value.length === 0) return false
  return questions.value.every(q => q.id && isInBasket(q.id))
})

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    questions.value.forEach(q => {
      if (q.id) removeFromBasket(q.id)
    })
  } else {
    questions.value.forEach(q => {
      if (q.id) addToBasket(q.id)
    })
  }
}

const handleRestoreChoice = (restore: boolean) => {
  if (!restore) {
    clearBasket()
  }
  showRestorePrompt.value = false
}

const goToBasket = () => {
  router.push('/papers/manual')
}

const editQuestion = (id: string) => {
  router.push(`/questions/${id}/edit`)
}

const deleteQuestion = async (id: string) => {
  if (!confirm('确定要删除这道题目吗？')) return
  try {
    const token = localStorage.getItem('token')
    await axios.delete(`/api/questions/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    // Remove from local list or refresh
    questions.value = questions.value.filter(q => q.id !== id)
    totalElements.value--
  } catch (err) {
    console.error('Failed to delete question', err)
    alert('删除失败')
  }
}

const stripHtml = (html: string | undefined) => {
  if (!html) return ''
  const tmp = document.createElement('DIV')
  tmp.innerHTML = html
  return tmp.textContent || tmp.innerText || ''
}

// 中文映射
const typeLabels: Record<string, string> = {
  SINGLE_CHOICE: '单选题',
  MULTI_CHOICE: '多选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题'
}

const difficultyLabels: Record<string, string> = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难'
}

const statusLabels: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  APPROVED: '已通过',
  REJECTED: '未通过',
  PUBLISHED: '已发布',
  ACTIVE: '已激活'
}

onMounted(() => {
  if (basket.value.length > 0) {
    showRestorePrompt.value = true
  }
  fetchKnowledgePoints()
  fetchQuestions()
  fetchCategoryStats()
})

watch([filterKnowledgePoint, filterType, filterDifficulty], () => {
  page.value = 0
  fetchQuestions()
})
</script>

<template>
  <div class="question-list-container container">
    <div class="header-row">
      <div class="header-title-section">
        <h1>题库管理</h1>
        <span class="total-count">共 {{ categoryStats.total }} 道题目</span>
      </div>
      <div class="header-actions">
        <router-link to="/import" class="add-btn-link">
          <button class="google-btn text-btn">
            文件导入
          </button>
        </router-link>
        <router-link to="/questions/add" class="add-btn-link">
          <button class="google-btn primary-btn">
            <span class="material-icon"></span> 添加题目
          </button>
        </router-link>
      </div>
    </div>

    <!-- 分类统计标签 -->
    <div class="category-tabs">
      <div 
        class="category-tab" 
        :class="{ active: filterType === '' }"
        @click="filterType = ''"
      >
        全部 <span class="count">{{ categoryStats.total }}</span>
      </div>
      <div 
        class="category-tab" 
        :class="{ active: filterType === 'SINGLE_CHOICE' }"
        @click="filterType = 'SINGLE_CHOICE'"
      >
        单选题 <span class="count">{{ categoryStats.singleChoice }}</span>
      </div>
      <div 
        class="category-tab" 
        :class="{ active: filterType === 'MULTI_CHOICE' }"
        @click="filterType = 'MULTI_CHOICE'"
      >
        多选题 <span class="count">{{ categoryStats.multiChoice }}</span>
      </div>
      <div 
        class="category-tab" 
        :class="{ active: filterType === 'TRUE_FALSE' }"
        @click="filterType = 'TRUE_FALSE'"
      >
        判断题 <span class="count">{{ categoryStats.trueFalse }}</span>
      </div>
      <div 
        class="category-tab" 
        :class="{ active: filterType === 'FILL_BLANK' }"
        @click="filterType = 'FILL_BLANK'"
      >
        填空题 <span class="count">{{ categoryStats.fillBlank }}</span>
      </div>
      <div 
        class="category-tab" 
        :class="{ active: filterType === 'SHORT_ANSWER' }"
        @click="filterType = 'SHORT_ANSWER'"
      >
        简答题 <span class="count">{{ categoryStats.shortAnswer }}</span>
      </div>
    </div>

    <div class="filter-bar google-card">
      <div class="filter-group">
        <GoogleSelect
          v-model="filterKnowledgePoint"
          :options="kpOptions"
          label="知识点"
        />
      </div>
      <div class="filter-group">
        <GoogleSelect
          v-model="filterDifficulty"
          :options="difficultyOptions"
          label="难度"
        />
      </div>
    </div>

    <div v-if="showRestorePrompt" class="restore-overlay">
      <div class="restore-dialog google-card">
        <h3>恢复之前的选择？</h3>
        <p>您有 {{ basket.length }} 道题目在上次会话中被选中。是否保留？</p>
        <div class="dialog-actions">
          <button @click="handleRestoreChoice(false)" class="google-btn text-btn">清除</button>
          <button @click="handleRestoreChoice(true)" class="google-btn primary-btn">保留</button>
        </div>
      </div>
    </div>
    
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>
    
    <div v-else-if="error" class="error-state google-card">
      <p>{{ error }}</p>
      <button @click="fetchQuestions" class="google-btn">重试</button>
    </div>
    
    <div v-else class="google-card table-card">
      <table class="question-table">
        <thead>
          <tr>
            <th class="checkbox-col">
              <input 
                type="checkbox" 
                :checked="isAllSelected" 
                @change="toggleSelectAll"
                class="google-checkbox"
              />
            </th>
            <th>科目</th>
            <th>题目</th>
            <th>题型</th>
            <th>难度</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="question in questions" :key="question.id">
            <td class="checkbox-col">
              <input 
                type="checkbox" 
                :checked="isInBasket(question.id!)" 
                @change="toggleBasket(question.id!)"
                class="google-checkbox"
              />
            </td>
            <td>{{ question.subjectId }}</td>
            <td class="stem-col clickable" :title="stripHtml(question.stem)" @click="editQuestion(question.id!)">
              {{ stripHtml(question.stem).substring(0, 50) }}{{ stripHtml(question.stem).length > 50 ? '...' : '' }}
            </td>
            <td><span class="chip type-chip">{{ typeLabels[question.type || ''] || question.type }}</span></td>
            <td>
              <span class="chip" :class="question.difficulty?.toLowerCase()">
                {{ difficultyLabels[question.difficulty || ''] || question.difficulty }}
              </span>
            </td>
            <td>
              <span class="status-dot" :class="['active', 'published'].includes(question.status?.toLowerCase() || '') ? 'active' : ''"></span>
              {{ statusLabels[question.status || ''] || question.status }}
            </td>
            <td class="action-col">
              <button @click="editQuestion(question.id!)" class="google-btn text-btn">编辑</button>
              <button @click="deleteQuestion(question.id!)" class="google-btn text-btn danger-btn">删除</button>
            </td>
          </tr>
          <tr v-if="questions.length === 0">
            <td colspan="7" class="empty-state">
              <div class="empty-content">
                <p>暂无试题</p>
                <router-link to="/questions/add" class="google-btn text-btn">添加试题</router-link>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      
      <div class="pagination">
        <button :disabled="page === 0" @click="page--; fetchQuestions()" class="google-btn text-btn">上一页</button>
        <span class="page-info">第 {{ page + 1 }} 页</span>
        <button :disabled="(page + 1) * size >= totalElements" @click="page++; fetchQuestions()" class="google-btn text-btn">下一页</button>
      </div>
    </div>

    <div v-if="basket.length > 0" class="basket-float">
      <div class="basket-content">
        <span>已选 {{ basket.length }} 道题目</span>
        <button @click="goToBasket" class="google-btn primary-btn small-btn">创建试卷</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.basket-float {
  position: fixed;
  bottom: 24px;
  right: 24px;
  background: white;
  padding: 12px 24px;
  border-radius: 28px; /* Material 3 FAB style */
  box-shadow: 0 3px 5px -1px rgba(0,0,0,0.2), 0 6px 10px 0 rgba(0,0,0,0.14), 0 1px 18px 0 rgba(0,0,0,0.12);
  z-index: 100;
  animation: slideUp 0.3s ease;
  border: 1px solid #dadce0;
}

.basket-content {
  display: flex;
  align-items: center;
  gap: 16px;
  font-family: 'Google Sans', sans-serif;
  font-weight: 500;
  color: #3c4043;
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-title-section {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.header-title-section h1 {
  margin: 0;
}

.total-count {
  font-size: 14px;
  color: #5f6368;
  font-family: 'Google Sans', sans-serif;
}

.category-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.category-tab {
  padding: 8px 16px;
  border-radius: 18px;
  background: #f1f3f4;
  color: #5f6368;
  font-size: 14px;
  font-family: 'Google Sans', sans-serif;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.category-tab:hover {
  background: #e8eaed;
}

.category-tab.active {
  background: #e8f0fe;
  color: #1a73e8;
}

.category-tab .count {
  background: rgba(0, 0, 0, 0.08);
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.category-tab.active .count {
  background: rgba(26, 115, 232, 0.15);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.add-btn-link {
  text-decoration: none;
}

h1 {
  font-family: 'Google Sans', sans-serif;
  font-size: 22px;
  color: #202124;
  font-weight: 400;
}

.table-card {
  padding: 0;
  overflow: hidden;
  border: 1px solid #dadce0;
  border-radius: 8px;
  background: white;
}

.question-table {
  width: 100%;
  border-collapse: collapse;
  text-align: center;
}

.question-table th {
  background-color: #fff;
  padding: 12px 16px;
  font-weight: 500;
  color: #5f6368;
  font-size: 14px;
  border-bottom: 1px solid #dadce0;
  text-align: center;
}

.question-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #f1f3f4;
  color: #3c4043;
  font-size: 14px;
  text-align: center;
}

.question-table tr:last-child td {
  border-bottom: none;
}

.question-table tr:hover {
  background-color: #f8f9fa;
}

.id-col {
  font-family: monospace;
  color: #5f6368;
}

.chip {
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  height: 24px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  background-color: #f1f3f4;
  color: #3c4043;
  border: 1px solid transparent;
}

.chip.easy { background-color: #e6f4ea; color: #137333; }
.chip.medium { background-color: #fef7e0; color: #b06000; }
.chip.hard { background-color: #fce8e6; color: #c5221f; }

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #dadce0;
  margin-right: 8px;
}

.status-dot.active {
  background-color: #1e8e3e;
}

.pagination {
  padding: 12px 16px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  border-top: 1px solid #dadce0;
  background-color: #fff;
}

.google-btn {
  border: none;
  border-radius: 4px;
  padding: 8px 24px;
  font-family: 'Google Sans', sans-serif;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s, box-shadow 0.2s;
}

.primary-btn {
  background-color: #1a73e8;
  color: white;
}

.primary-btn:hover {
  background-color: #1557b0;
  box-shadow: 0 1px 2px 0 rgba(60,64,67,0.3), 0 1px 3px 1px rgba(60,64,67,0.15);
}

.text-btn {
  background-color: transparent;
  color: #1a73e8;
}

.text-btn:hover:not(:disabled) {
  background-color: #f6fafe;
}

.text-btn:disabled {
  color: #dadce0;
  cursor: default;
}

.loading-state, .error-state {
  text-align: center;
  padding: 40px;
  color: #5f6368;
}

.empty-state {
  text-align: center;
  color: #5f6368;
  padding: 40px;
}

.google-checkbox {
  width: 18px;
  height: 18px;
  accent-color: #1a73e8;
}

.restore-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.restore-dialog {
  background: white;
  padding: 24px;
  border-radius: 8px;
  width: 400px;
  box-shadow: 0 4px 8px rgba(0,0,0,0.2);
}

.restore-dialog h3 {
  margin-top: 0;
  margin-bottom: 16px;
  font-family: 'Google Sans', sans-serif;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 24px;
}

.filter-bar {
  display: flex;
  gap: 24px;
  padding: 16px 24px;
  margin-bottom: 24px;
  align-items: flex-end;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.filter-group label {
  font-size: 12px;
  color: #5f6368;
  font-weight: 500;
}

.select-wrapper {
  position: relative;
}
.select-wrapper::after {
  /* Optional: Custom arrow if not using background-image on select */
  pointer-events: none;
}

.danger-btn {
  color: #d93025 !important;
}

.danger-btn:hover {
  background-color: #fce8e6 !important;
}

.stem-col.clickable {
  cursor: pointer;
  color: #1a73e8;
  transition: color 0.2s;
}

.stem-col.clickable:hover {
  color: #1557b0;
  text-decoration: underline;
}

.action-col {
  white-space: nowrap;
}

.action-col .google-btn {
  margin-right: 8px;
}

.action-col .google-btn:last-child {
  margin-right: 0;
}
</style>
