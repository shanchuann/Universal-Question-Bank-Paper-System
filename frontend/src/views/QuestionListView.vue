<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { questionApi, knowledgePointApi } from '@/api/client'
import type { QuestionSummary, KnowledgePoint } from '@/api/generated'
import { useBasket } from '@/stores/basket'
import { useRouter } from 'vue-router'
import axios from 'axios'
import GoogleSelect from '@/components/GoogleSelect.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useToast } from '@/composables/useToast'

const { confirm } = useConfirm()
const { showToast } = useToast()

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
const kpKeyword = ref('')
const expandedNodes = ref<Set<string>>(new Set())

const difficultyOptions = [
  { label: '全部难度', value: '' },
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '困难', value: 'HARD' }
]

type KpTreeNode = {
  id: string
  name: string
  level: string
  parentId: string | null
  children: KpTreeNode[]
}

const levelLabels: Record<string, string> = {
  CHAPTER: '章',
  SECTION: '节',
  POINT: '知识点'
}

const knowledgeTree = computed<KpTreeNode[]>(() => {
  const map = new Map<string, KpTreeNode>()
  const roots: KpTreeNode[] = []

  for (const kp of knowledgePoints.value) {
    if (!kp.id) continue
    map.set(kp.id, {
      id: kp.id,
      name: kp.name || '未命名',
      level: (kp as any).level || 'POINT',
      parentId: kp.parentId || null,
      children: []
    })
  }

  for (const node of map.values()) {
    if (node.parentId && map.has(node.parentId)) {
      map.get(node.parentId)!.children.push(node)
    } else {
      roots.push(node)
    }
  }

  const sortNode = (nodes: KpTreeNode[]) => {
    nodes.sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
    nodes.forEach((n) => sortNode(n.children))
  }
  sortNode(roots)
  return roots
})

const findNodePath = (nodes: KpTreeNode[], id: string, path: KpTreeNode[] = []): KpTreeNode[] => {
  for (const node of nodes) {
    const nextPath = [...path, node]
    if (node.id === id) return nextPath
    const found = findNodePath(node.children, id, nextPath)
    if (found.length > 0) return found
  }
  return []
}

const selectedNodePathText = computed(() => {
  if (!filterKnowledgePoint.value) return '全部知识点'
  const path = findNodePath(knowledgeTree.value, filterKnowledgePoint.value)
  return path.map((p) => p.name).join(' / ') || '全部知识点'
})

const expandPathToNode = (id: string) => {
  const path = findNodePath(knowledgeTree.value, id)
  path.forEach((p) => expandedNodes.value.add(p.id))
}

const isExpanded = (id: string) => expandedNodes.value.has(id)

const toggleNodeExpand = (id: string) => {
  if (expandedNodes.value.has(id)) {
    expandedNodes.value.delete(id)
  } else {
    expandedNodes.value.add(id)
  }
}

const selectKnowledgeNode = (id: string) => {
  filterKnowledgePoint.value = id
  expandPathToNode(id)
}

const shouldShowChildren = (id: string) => {
  return kpKeyword.value.trim().length > 0 || isExpanded(id)
}

const filterTreeByKeyword = (nodes: KpTreeNode[], keyword: string): KpTreeNode[] => {
  if (!keyword.trim()) return nodes
  const lower = keyword.trim().toLowerCase()
  const dfs = (list: KpTreeNode[]): KpTreeNode[] => {
    const result: KpTreeNode[] = []
    for (const node of list) {
      const children = dfs(node.children)
      const hit = node.name.toLowerCase().includes(lower)
      if (hit || children.length > 0) {
        result.push({ ...node, children })
      }
    }
    return result
  }
  return dfs(nodes)
}

const filteredKnowledgeTree = computed(() =>
  filterTreeByKeyword(knowledgeTree.value, kpKeyword.value)
)

const fetchKnowledgePoints = async () => {
  try {
    const response = await knowledgePointApi.apiKnowledgePointsGet()
    knowledgePoints.value = response.data
    if (filterKnowledgePoint.value) {
      expandPathToNode(filterKnowledgePoint.value)
    } else {
      knowledgeTree.value.forEach((root) => expandedNodes.value.add(root.id))
    }
  } catch (err) {
    console.error('Failed to load knowledge points', err)
  }
}

const getDescendantIdsFromTree = (nodes: KpTreeNode[], targetId: string): string[] => {
  const walk = (node: KpTreeNode): string[] => {
    let ids = [node.id]
    for (const child of node.children) {
      ids = ids.concat(walk(child))
    }
    return ids
  }

  const find = (list: KpTreeNode[]): string[] => {
    for (const node of list) {
      if (node.id === targetId) return walk(node)
      const found = find(node.children)
      if (found.length > 0) return found
    }
    return []
  }

  return find(nodes)
}

// 获取分类统计数据
const fetchCategoryStats = async () => {
  try {
    // 获取各类型的数量 (只统计 APPROVED 状态)
    const types = [
      'SINGLE_CHOICE',
      'MULTI_CHOICE',
      'TRUE_FALSE',
      'FILL_BLANK',
      'SHORT_ANSWER',
      'MULTIPLE_CHOICE'
    ]
    const responses = await Promise.all([
      // 总数
      questionApi.apiQuestionsGet(
        0,
        1,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined
      ),
      // 各类型数量
      ...types.map((type) =>
        questionApi.apiQuestionsGet(
          0,
          1,
          undefined,
          undefined,
          type as any,
          undefined,
          undefined,
          undefined
        )
      )
    ])

    categoryStats.value = {
      total: responses[0]?.data?.totalElements || 0,
      singleChoice: responses[1]?.data?.totalElements || 0,
      multiChoice:
        (responses[2]?.data?.totalElements || 0) + (responses[6]?.data?.totalElements || 0),
      trueFalse: responses[3]?.data?.totalElements || 0,
      fillBlank: responses[4]?.data?.totalElements || 0,
      shortAnswer: responses[5]?.data?.totalElements || 0
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
      kpIds = getDescendantIdsFromTree(knowledgeTree.value, filterKnowledgePoint.value)
    }

    const response = await questionApi.apiQuestionsGet(
      page.value,
      size.value,
      undefined, // subjectId
      kpIds,
      filterType.value as any,
      filterDifficulty.value as any,
      undefined, // keywords
      undefined // status - 默认显示所有有效题目(APPROVED, PUBLISHED, ACTIVE)
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
  return questions.value.every((q) => q.id && isInBasket(q.id))
})

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    questions.value.forEach((q) => {
      if (q.id) removeFromBasket(q.id)
    })
  } else {
    questions.value.forEach((q) => {
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
  const confirmed = await confirm({
    title: '删除确认',
    message: '确定要删除这道题目吗？删除后无法恢复。',
    type: 'danger',
    confirmText: '删除',
    cancelText: '取消'
  })
  if (!confirmed) return
  try {
    const token = localStorage.getItem('token')
    await axios.delete(`/api/questions/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    // Remove from local list or refresh
    questions.value = questions.value.filter((q) => q.id !== id)
    totalElements.value--
  } catch (err) {
    console.error('Failed to delete question', err)
    showToast({ message: '删除失败', type: 'error' })
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
  PUBLISHED: '已通过',
  ACTIVE: '已通过'
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
        <h1 class="page-title">题库管理</h1>
        <span class="total-count">共 {{ categoryStats.total }} 道题目</span>
      </div>
      <div class="header-actions">
        <router-link to="/import" class="add-btn-link">
          <button class="google-btn text-btn">导入题目</button>
        </router-link>
        <router-link to="/questions/add" class="add-btn-link">
          <button class="google-btn primary-btn">添加题目</button>
        </router-link>
      </div>
    </div>

    <!-- 分类统计标签 -->
    <div class="category-tabs">
      <div class="category-tab" :class="{ active: filterType === '' }" @click="filterType = ''">
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
        :class="{ active: filterType === 'MULTIPLE_CHOICE' }"
        @click="filterType = 'MULTIPLE_CHOICE'"
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
      <div class="filter-group tree-group">
        <label>知识结构树筛选</label>
        <div class="tree-filter-panel">
          <div class="tree-filter-top">
            <input v-model="kpKeyword" class="google-input" placeholder="搜索章 / 节 / 知识点" />
            <button class="google-btn text-btn" @click="filterKnowledgePoint = ''">清空</button>
          </div>
          <div class="selected-path">当前：{{ selectedNodePathText }}</div>
          <div class="tree-list">
            <ul class="tree-root">
              <li v-for="chapter in filteredKnowledgeTree" :key="chapter.id" class="tree-node-item">
                <div
                  class="node-row chapter-row"
                  :class="{ active: filterKnowledgePoint === chapter.id }"
                >
                  <div class="node-info">
                    <button
                      v-if="chapter.children.length > 0"
                      class="node-toggle"
                      @click="toggleNodeExpand(chapter.id)"
                    >
                      {{ shouldShowChildren(chapter.id) ? '▾' : '▸' }}
                    </button>
                    <span v-else class="node-toggle-placeholder"></span>
                    <span class="node-icon">
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        width="16"
                        height="16"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path>
                        <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path>
                      </svg>
                    </span>
                    <button class="node-name-btn" @click="selectKnowledgeNode(chapter.id)">
                      {{ chapter.name }}
                    </button>
                    <span class="node-level">{{ levelLabels[chapter.level] || '章' }}</span>
                  </div>
                </div>

                <ul
                  v-if="chapter.children?.length && shouldShowChildren(chapter.id)"
                  class="tree-children"
                >
                  <li v-for="section in chapter.children" :key="section.id" class="tree-node-item">
                    <div
                      class="node-row section-row"
                      :class="{ active: filterKnowledgePoint === section.id }"
                    >
                      <div class="node-info">
                        <button
                          v-if="section.children.length > 0"
                          class="node-toggle"
                          @click="toggleNodeExpand(section.id)"
                        >
                          {{ shouldShowChildren(section.id) ? '▾' : '▸' }}
                        </button>
                        <span v-else class="node-toggle-placeholder"></span>
                        <span class="node-icon">
                          <svg
                            xmlns="http://www.w3.org/2000/svg"
                            width="16"
                            height="16"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <path
                              d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"
                            ></path>
                          </svg>
                        </span>
                        <button class="node-name-btn" @click="selectKnowledgeNode(section.id)">
                          {{ section.name }}
                        </button>
                        <span class="node-level">{{ levelLabels[section.level] || '节' }}</span>
                      </div>
                    </div>

                    <ul
                      v-if="section.children?.length && shouldShowChildren(section.id)"
                      class="tree-children"
                    >
                      <li v-for="point in section.children" :key="point.id" class="tree-node-item">
                        <div
                          class="node-row point-row"
                          :class="{ active: filterKnowledgePoint === point.id }"
                        >
                          <div class="node-info">
                            <span class="node-toggle-placeholder"></span>
                            <span class="node-icon">
                              <svg
                                xmlns="http://www.w3.org/2000/svg"
                                width="16"
                                height="16"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                stroke-width="2"
                                stroke-linecap="round"
                                stroke-linejoin="round"
                              >
                                <path
                                  d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"
                                ></path>
                                <polyline points="14 2 14 8 20 8"></polyline>
                                <line x1="16" y1="13" x2="8" y2="13"></line>
                                <line x1="16" y1="17" x2="8" y2="17"></line>
                                <polyline points="10 9 9 9 8 9"></polyline>
                              </svg>
                            </span>
                            <button class="node-name-btn" @click="selectKnowledgeNode(point.id)">
                              {{ point.name }}
                            </button>
                            <span class="node-level">{{
                              levelLabels[point.level] || '知识点'
                            }}</span>
                          </div>
                        </div>
                      </li>
                    </ul>
                  </li>
                </ul>
              </li>
            </ul>
          </div>
        </div>
      </div>
      <div class="filter-group compact-group">
        <label>难度筛选</label>
        <div class="single-filter-card">
          <GoogleSelect v-model="filterDifficulty" :options="difficultyOptions" label="难度" />
        </div>
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
      <div class="table-responsive">
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
            <td
              class="stem-col clickable"
              :title="stripHtml(question.stem)"
              @click="editQuestion(question.id!)"
            >
              {{ stripHtml(question.stem).substring(0, 50)
              }}{{ stripHtml(question.stem).length > 50 ? '...' : '' }}
            </td>
            <td>
              <span class="chip type-chip">{{
                typeLabels[question.type || ''] || question.type
              }}</span>
            </td>
            <td>
              <span class="chip" :class="question.difficulty?.toLowerCase()">
                {{ difficultyLabels[question.difficulty || ''] || question.difficulty }}
              </span>
            </td>
            <td>
              <span
                class="status-dot"
                :class="
                  ['active', 'published'].includes(question.status?.toLowerCase() || '')
                    ? 'active'
                    : ''
                "
              ></span>
              {{ statusLabels[question.status || ''] || question.status }}
            </td>
            <td class="action-col">
              <button @click="editQuestion(question.id!)" class="google-btn text-btn">编辑</button>
              <button @click="deleteQuestion(question.id!)" class="google-btn text-btn danger-btn">
                删除
              </button>
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
      </div>

      <div class="pagination">
        <button
          :disabled="page === 0"
          @click="page--; fetchQuestions()"
          class="google-btn text-btn"
        >
          上一页
        </button>
        <span class="page-info"
          >第 {{ page + 1 }} 页 / 共 {{ Math.ceil(totalElements / size) || 1 }} 页</span
        >
        <button
          :disabled="(page + 1) * size >= totalElements"
          @click="page++; fetchQuestions()"
          class="google-btn text-btn"
        >
          下一页
        </button>
      </div>
    </div>

    <div v-if="basket.length > 0" class="basket-float">
      <div class="basket-content">
        <span>已选 {{ basket.length }} 道题目</span>
        <button @click="clearBasket" class="google-btn text-btn small-btn">全部取消</button>
        <button @click="goToBasket" class="google-btn primary-btn small-btn">创建试卷</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.basket-float {
  position: fixed;
  bottom: 32px;
  right: 32px;
  background: var(--line-bg);
  padding: 16px 24px;
  border-radius: var(--radius);
  box-shadow: var(--shadow-hover);
  z-index: 100;
  animation: slideUp 0.3s ease;
  border: 1px solid var(--line-border);
}

.basket-content {
  display: flex;
  align-items: center;
  gap: 16px;
  font-weight: 500;
  color: var(--line-text);
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
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

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-title-section {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.total-count {
  font-size: 14px;
  color: var(--line-text-secondary);
}

.category-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.category-tab {
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid var(--line-border);
  background: var(--line-bg);
  color: var(--line-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-tab:hover {
  border-color: var(--line-text);
  color: var(--line-text);
}

.category-tab.active {
  background: var(--line-primary);
  color: white;
  border-color: var(--line-primary);
}

.category-tab .count {
  background: rgba(255, 255, 255, 0.2);
  padding: 1px 6px;
  border-radius: 12px;
  font-size: 11px;
}

.category-tab:not(.active) .count {
  background: var(--line-bg-soft);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.stem-col.clickable {
  cursor: pointer;
  color: var(--line-primary);
  text-decoration: none;
  font-weight: 500;
}

.stem-col.clickable:hover {
  text-decoration: underline;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--line-text-secondary);
  margin-right: 8px;
}

.status-dot.active {
  background-color: var(--line-success);
}

.pagination {
  padding: 16px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  border-top: none;
  background-color: var(--line-bg);
}

.danger-btn {
  color: var(--line-error) !important;
}

.restore-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(2px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease-out;
}

.restore-dialog {
  background: var(--line-bg);
  padding: 32px;
  border-radius: var(--radius);
  width: 400px;
  box-shadow: var(--shadow-hover);
  border: 1px solid var(--line-border);
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 24px;
}

.action-col {
  white-space: nowrap;
}
.action-col button {
  margin-right: 8px;
}

.checkbox-col {
  width: 48px;
  text-align: center;
  vertical-align: middle;
}

.checkbox-col input[type='checkbox'] {
  margin: 0 auto;
  display: block;
}

.filter-bar {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  padding: 24px;
  margin-bottom: 24px;
  align-items: start;
  background: var(--line-bg);
  border: 1px solid var(--line-border);
  border-radius: var(--radius);
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.tree-group {
  min-height: 0;
}

.compact-group {
  max-width: 100%;
}

.single-filter-card {
  border: 1px solid var(--line-border);
  border-radius: 10px;
  background: var(--line-bg-soft);
  padding: 10px;
}

.filter-group label {
  font-size: 13px;
  font-weight: 500;
  color: var(--line-text-secondary);
}

.tree-filter-panel {
  border: 1px solid var(--line-border);
  border-radius: 10px;
  padding: 10px;
  background: var(--line-bg-soft);
}

.tree-filter-top {
  display: flex;
  gap: 8px;
  align-items: center;
}

.tree-filter-top .google-input {
  flex: 1;
}

.selected-path {
  margin-top: 8px;
  font-size: 12px;
  color: var(--line-text-secondary);
  text-align: left;
}

.tree-list {
  margin-top: 10px;
  max-height: 240px;
  overflow: auto;
  border-top: 1px dashed var(--line-border);
  padding-top: 8px;
}

.tree-root,
.tree-children {
  list-style: none;
  padding-left: 0;
  margin: 0;
}

.tree-children {
  padding-left: 24px;
  border-left: 1px solid var(--line-bg-soft);
  margin-left: 12px;
}

.tree-children .tree-node-item {
  position: relative;
}

.tree-children .tree-node-item::before {
  content: '';
  position: absolute;
  top: 14px;
  left: -24px;
  width: 20px;
  height: 1px;
  background: var(--line-bg-soft);
}

.tree-node-item {
  margin-bottom: 4px;
}

.node-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: 6px;
  padding: 8px 10px;
  border: 1px solid transparent;
  transition:
    background-color 0.2s,
    border-color 0.2s;
}

.node-row:hover {
  background-color: var(--line-bg-soft);
  border-color: var(--line-bg-soft);
}

.node-row.active {
  background: color-mix(in srgb, var(--line-primary) 12%, white);
  border-color: color-mix(in srgb, var(--line-primary) 20%, transparent);
}

.node-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  width: 100%;
}

.node-toggle,
.node-toggle-placeholder {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.node-toggle {
  border: none;
  background: transparent;
  color: var(--line-text-secondary);
  cursor: pointer;
  padding: 0;
}

.node-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--line-text-secondary);
  flex-shrink: 0;
}

.node-name-btn {
  border: none;
  background: transparent;
  color: var(--line-text);
  cursor: pointer;
  padding: 0;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  text-align: left;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-level {
  font-size: 12px;
  color: var(--line-text-secondary);
  background: var(--line-bg-soft);
  border: 1px solid var(--line-border);
  border-radius: 999px;
  padding: 1px 8px;
  flex-shrink: 0;
}

@media (max-width: 1100px) {
  .compact-group {
    max-width: none;
  }
}

/* Responsive fixes for smaller screens - prefer horizontal scroll over layout break */
.table-responsive {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  -ms-overflow-style: -ms-autohiding-scrollbar;
}
.table-responsive .question-table {
  /* keep a reasonable minimum width so table becomes scrollable instead of wrapping */
  min-width: 1100px;
  table-layout: fixed;
  border-collapse: separate;
}

/* Prevent cells from wrapping; use ellipsis to indicate overflow so layout stays stable */
.table-responsive .question-table th,
.table-responsive .question-table td {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
}

/* Column sizing to keep layout stable when scrolling */
.table-responsive .question-table th.checkbox-col,
.table-responsive .question-table td.checkbox-col {
  width: 48px;
}
.table-responsive .question-table th:nth-child(2),
.table-responsive .question-table td:nth-child(2) { width: 120px; } /* 科目 */
.table-responsive .question-table th:nth-child(3),
.table-responsive .question-table td:nth-child(3) { width: 420px; } /* 题目 */
.table-responsive .question-table th:nth-child(4),
.table-responsive .question-table td:nth-child(4) { width: 120px; } /* 题型 */
.table-responsive .question-table th:nth-child(5),
.table-responsive .question-table td:nth-child(5) { width: 100px; } /* 难度 */
.table-responsive .question-table th:nth-child(6),
.table-responsive .question-table td:nth-child(6) { width: 120px; } /* 状态 */
.table-responsive .question-table th:nth-child(7),
.table-responsive .question-table td:nth-child(7) { width: 200px; } /* 操作 */

@media (max-width: 900px) {
  .header-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .header-actions {
    align-self: stretch;
    display: flex;
    justify-content: flex-start;
    gap: 8px;
    margin-top: 8px;
  }
  .header-title-section {
    align-items: flex-start;
    gap: 8px;
  }
  .page-title {
    font-size: 20px;
  }
  .restore-dialog {
    width: auto;
    max-width: calc(100% - 40px);
    padding: 20px;
  }
  .node-name-btn {
    white-space: normal;
    overflow: visible;
    text-overflow: unset;
  }
  .basket-float {
    right: 12px;
    bottom: 12px;
    padding: 12px 16px;
    left: auto;
    transform: none;
  }
  .category-tab {
    padding: 6px 12px;
    font-size: 12px;
  }
  .question-table th,
  .question-table td {
    padding: 10px;
    font-size: 13px;
  }
  .filter-bar {
    padding: 16px;
  }
  .tree-list {
    max-height: 200px;
  }
}

@media (max-width: 520px) {
  .table-responsive .question-table {
    min-width: 600px;
  }
  .basket-float {
    left: 50%;
    right: auto;
    transform: translateX(-50%);
    bottom: 14px;
  }
  .container {
    margin: 16px auto;
    padding: 0 12px;
  }
  .page-title {
    font-size: 18px;
  }
  .google-card.table-card {
    padding: 12px;
  }
}
</style>
