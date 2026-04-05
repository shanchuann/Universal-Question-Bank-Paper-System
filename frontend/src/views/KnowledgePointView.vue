<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { useConfirm } from '@/composables/useConfirm'
import { useToast } from '@/composables/useToast'
import GoogleSelect from '@/components/GoogleSelect.vue'

const { confirm } = useConfirm()
const { showToast } = useToast()

interface KnowledgePoint {
  id: string
  name: string
  parentId: string | null
  subjectId: string
  level: 'CHAPTER' | 'SECTION' | 'POINT'
  sortOrder: number
  children?: KnowledgePoint[]
}

const points = ref<KnowledgePoint[]>([])
const loading = ref(false)
const error = ref('')
const subjectId = ref('general') // Default subject

// Dialog state
const showDialog = ref(false)
const editingPoint = ref<KnowledgePoint | null>(null)
const form = ref({
  name: '',
  parentId: '' as string | null,
  level: 'CHAPTER' as 'CHAPTER' | 'SECTION' | 'POINT',
  sortOrder: 0
})

const levelOptions = [
  { value: 'CHAPTER', label: '章' },
  { value: 'SECTION', label: '节' },
  { value: 'POINT', label: '知识点' }
]

const fetchPoints = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/knowledge-points', {
      params: { subjectId: subjectId.value },
      headers: { Authorization: `Bearer ${token}` }
    })
    points.value = response.data
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load knowledge points'
  } finally {
    loading.value = false
  }
}

const treeData = computed(() => {
  const map = new Map<string, KnowledgePoint>()
  const roots: KnowledgePoint[] = []

  // Clone and map
  points.value.forEach((p) => {
    map.set(p.id, { ...p, children: [] })
  })

  // Build tree
  map.forEach((p) => {
    if (p.parentId && map.has(p.parentId)) {
      map.get(p.parentId)!.children!.push(p)
    } else {
      roots.push(p)
    }
  })

  // Sort
  const sortFn = (a: KnowledgePoint, b: KnowledgePoint) => (a.sortOrder || 0) - (b.sortOrder || 0)
  const sortRecursive = (list: KnowledgePoint[]) => {
    list.sort(sortFn)
    list.forEach((item) => {
      if (item.children) sortRecursive(item.children)
    })
  }

  sortRecursive(roots)
  return roots
})

const openAddDialog = (parent?: KnowledgePoint) => {
  editingPoint.value = null
  form.value = {
    name: '',
    parentId: parent ? parent.id : null,
    level: parent ? (parent.level === 'CHAPTER' ? 'SECTION' : 'POINT') : 'CHAPTER',
    sortOrder: 0
  }
  showDialog.value = true
}

const openEditDialog = (point: KnowledgePoint) => {
  editingPoint.value = point
  form.value = {
    name: point.name,
    parentId: point.parentId,
    level: point.level,
    sortOrder: point.sortOrder
  }
  showDialog.value = true
}

const savePoint = async () => {
  const token = localStorage.getItem('token')
  try {
    const payload = {
      ...form.value,
      subjectId: subjectId.value
    }

    if (editingPoint.value) {
      await axios.put(`/api/knowledge-points/${editingPoint.value.id}`, payload, {
        headers: { Authorization: `Bearer ${token}` }
      })
    } else {
      await axios.post('/api/knowledge-points', payload, {
        headers: { Authorization: `Bearer ${token}` }
      })
    }
    showDialog.value = false
    fetchPoints()
  } catch (err) {
    console.error(err)
    showToast({ message: '保存失败', type: 'error' })
  }
}

const deletePoint = async (id: string) => {
  const confirmed = await confirm({
    title: '删除知识点',
    message: '确定要删除该知识点吗？这将删除该知识点及其子节点。',
    type: 'danger',
    confirmText: '删除',
    cancelText: '取消'
  })
  if (!confirmed) return

  const token = localStorage.getItem('token')
  try {
    await axios.delete(`/api/knowledge-points/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    fetchPoints()
  } catch (err) {
    console.error(err)
    showToast({ message: '删除失败', type: 'error' })
  }
}

onMounted(fetchPoints)
</script>

<template>
  <div class="container">
    <div class="google-card">
      <div class="header">
        <h1 class="page-title">知识图谱</h1>
        <button @click="openAddDialog()" class="google-btn primary-btn">添加章节</button>
      </div>

      <div class="tree-view">
        <div v-if="loading">加载中...</div>
        <div v-else-if="treeData.length === 0">未找到知识点。</div>

        <ul class="tree-root">
          <li v-for="chapter in treeData" :key="chapter.id" class="tree-node chapter">
            <div class="node-content">
              <span class="node-title">{{ chapter.name }}</span>
              <div class="actions">
                <button @click="openEditDialog(chapter)" class="icon-btn" title="编辑">
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
                    <path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z" />
                    <path d="m15 5 4 4" />
                  </svg>
                </button>
                <button @click="openAddDialog(chapter)" class="icon-btn" title="添加节">
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
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                </button>
                <button @click="deletePoint(chapter.id)" class="icon-btn delete" title="删除">
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
                    <polyline points="3 6 5 6 21 6" />
                    <path
                      d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"
                    />
                  </svg>
                </button>
              </div>
            </div>

            <ul v-if="chapter.children && chapter.children.length > 0" class="tree-children">
              <li v-for="section in chapter.children" :key="section.id" class="tree-node section">
                <div class="node-content">
                  <span class="node-title">{{ section.name }}</span>
                  <div class="actions">
                    <button @click="openEditDialog(section)" class="icon-btn" title="编辑">
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
                        <path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z" />
                        <path d="m15 5 4 4" />
                      </svg>
                    </button>
                    <button @click="openAddDialog(section)" class="icon-btn" title="添加知识点">
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
                        <line x1="12" y1="5" x2="12" y2="19" />
                        <line x1="5" y1="12" x2="19" y2="12" />
                      </svg>
                    </button>
                    <button @click="deletePoint(section.id)" class="icon-btn delete" title="删除">
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
                        <polyline points="3 6 5 6 21 6" />
                        <path
                          d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"
                        />
                      </svg>
                    </button>
                  </div>
                </div>

                <ul v-if="section.children && section.children.length > 0" class="tree-children">
                  <li v-for="point in section.children" :key="point.id" class="tree-node point">
                    <div class="node-content">
                      <span class="node-title">{{ point.name }}</span>
                      <div class="actions">
                        <button @click="openEditDialog(point)" class="icon-btn" title="编辑">
                          <svg
                            width="16"
                            height="16"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z" />
                            <path d="m15 5 4 4" />
                          </svg>
                        </button>
                        <button @click="deletePoint(point.id)" class="icon-btn delete" title="删除">
                          <svg
                            width="16"
                            height="16"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <polyline points="3 6 5 6 21 6" />
                            <path
                              d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"
                            />
                          </svg>
                        </button>
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

    <!-- Dialog -->
    <div v-if="showDialog" class="modal-overlay">
      <div class="modal google-card">
        <h2>{{ editingPoint ? '编辑' : '添加' }}知识点</h2>
        <div class="form-group">
          <label>名称</label>
          <input v-model="form.name" class="google-input" />
        </div>
        <div class="form-group">
          <label>级别</label>
          <GoogleSelect
            v-model="form.level"
            :options="levelOptions"
            placeholder="级别"
            :disabled="true"
          />
        </div>
        <div class="form-group">
          <label>排序</label>
          <input type="number" v-model="form.sortOrder" class="google-input" />
        </div>
        <div class="modal-actions">
          <button @click="showDialog = false" class="google-btn text-btn">取消</button>
          <button @click="savePoint" class="google-btn primary-btn">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.tree-root {
  list-style: none;
  padding: 0;
}

.tree-children {
  list-style: none;
  padding-left: 24px;
  border-left: 1px solid var(--line-border);
  margin-left: 12px;
}

.tree-node {
  margin: 8px 0;
}

.node-content {
  display: flex;
  align-items: center;
  padding: 8px;
  border-radius: 4px;
  background: var(--line-bg-soft);
  border: 1px solid transparent;
}

.node-content:hover {
  background: var(--line-bg-hover);
  border-color: var(--line-border);
}

.node-title {
  flex: 1;
  font-weight: 500;
}

.chapter .node-title {
  font-size: 16px;
  color: var(--line-primary);
}
.section .node-title {
  font-size: 14px;
  color: var(--line-text);
}
.point .node-title {
  font-size: 13px;
  color: var(--line-text-secondary);
}

.actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.node-content:hover .actions {
  opacity: 1;
}

.icon-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  color: var(--line-text-secondary);
}

.icon-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: var(--line-primary);
}

.icon-btn.delete:hover {
  color: var(--line-error);
}

.modal-overlay {
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

.modal {
  width: 400px;
  padding: 24px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 24px;
}

.form-group {
  margin-bottom: 16px;
}

.google-input,
.google-select {
  width: 100%;
  padding: 8px;
  border: 1px solid var(--line-border);
  border-radius: 4px;
  margin-top: 4px;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
