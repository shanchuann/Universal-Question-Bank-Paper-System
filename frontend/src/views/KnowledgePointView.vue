<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'

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
  points.value.forEach(p => {
    map.set(p.id, { ...p, children: [] })
  })
  
  // Build tree
  map.forEach(p => {
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
    list.forEach(item => {
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
    alert('保存失败')
  }
}

const deletePoint = async (id: string) => {
  if (!confirm('确定要删除吗？这将删除该知识点及其子节点。')) return
  
  const token = localStorage.getItem('token')
  try {
    await axios.delete(`/api/knowledge-points/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    fetchPoints()
  } catch (err) {
    console.error(err)
    alert('删除失败')
  }
}

onMounted(fetchPoints)
</script>

<template>
  <div class="container">
    <div class="google-card">
      <div class="header">
        <h1>知识图谱</h1>
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
                  <svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 0 24 24" width="24px" fill="#5f6368"><path d="M0 0h24v24H0V0z" fill="none"/><path d="M14.06 9.02l.92.92L5.92 19H5v-.92l9.06-9.06M17.66 3c-.25 0-.51.1-.7.29l-1.83 1.83 3.75 3.75 1.83-1.83c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.2-.2-.45-.29-.71-.29zm-3.6 3.19L3 17.25V21h3.75L17.81 9.94l-3.75-3.75z"/></svg>
                </button>
                <button @click="openAddDialog(chapter)" class="icon-btn" title="添加节">
                  <svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 0 24 24" width="24px" fill="#5f6368"><path d="M0 0h24v24H0V0z" fill="none"/><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
                </button>
                <button @click="deletePoint(chapter.id)" class="icon-btn delete" title="删除">
                  <svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 0 24 24" width="24px" fill="#5f6368"><path d="M0 0h24v24H0V0z" fill="none"/><path d="M16 9v10H8V9h8m-1.5-6h-5l-1 1H5v2h14V4h-3.5l-1-1zM18 7H6v12c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7z"/></svg>
                </button>
              </div>
            </div>
            
            <ul v-if="chapter.children && chapter.children.length > 0" class="tree-children">
              <li v-for="section in chapter.children" :key="section.id" class="tree-node section">
                <div class="node-content">
                  <span class="node-title">{{ section.name }}</span>
                  <div class="actions">
                    <button @click="openEditDialog(section)" class="icon-btn" title="编辑">
                      <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 0 24 24" width="20px" fill="#5f6368"><path d="M0 0h24v24H0V0z" fill="none"/><path d="M14.06 9.02l.92.92L5.92 19H5v-.92l9.06-9.06M17.66 3c-.25 0-.51.1-.7.29l-1.83 1.83 3.75 3.75 1.83-1.83c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.2-.2-.45-.29-.71-.29zm-3.6 3.19L3 17.25V21h3.75L17.81 9.94l-3.75-3.75z"/></svg>
                    </button>
                    <button @click="openAddDialog(section)" class="icon-btn" title="添加知识点">
                      <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 0 24 24" width="20px" fill="#5f6368"><path d="M0 0h24v24H0V0z" fill="none"/><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
                    </button>
                    <button @click="deletePoint(section.id)" class="icon-btn delete" title="删除">
                      <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 0 24 24" width="20px" fill="#5f6368"><path d="M0 0h24v24H0V0z" fill="none"/><path d="M16 9v10H8V9h8m-1.5-6h-5l-1 1H5v2h14V4h-3.5l-1-1zM18 7H6v12c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7z"/></svg>
                    </button>
                  </div>
                </div>
                      <button @click="openEditDialog(point)" class="icon-btn" title="编辑">
                        <svg xmlns="http://www.w3.org/2000/svg" height="18px" viewBox="0 0 24 24" width="18px" fill="#5f6368"><path d="M0 0h24v24H0V0z" fill="none"/><path d="M14.06 9.02l.92.92L5.92 19H5v-.92l9.06-9.06M17.66 3c-.25 0-.51.1-.7.29l-1.83 1.83 3.75 3.75 1.83-1.83c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.2-.2-.45-.29-.71-.29zm-3.6 3.19L3 17.25V21h3.75L17.81 9.94l-3.75-3.75z"/></svg>
                      </button>
                      <button @click="deletePoint(point.id)" class="icon-btn delete" title="删除">
                        <svg xmlns="http://www.w3.org/2000/svg" height="18px" viewBox="0 0 24 24" width="18px" fill="#5f6368"><path d="M0 0h24v24H0V0z" fill="none"/><path d="M16 9v10H8V9h8m-1.5-6h-5l-1 1H5v2h14V4h-3.5l-1-1zM18 7H6v12c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7z"/></svg>
                      </button>
                  <li v-for="point in section.children" :key="point.id" class="tree-node point">
                    <div class="node-content">
                      <span class="node-title">{{ point.name }}</span>
                      <div class="actions">
                        <button @click="openEditDialog(point)" class="icon-btn">✎</button>
                        <button @click="deletePoint(point.id)" class="icon-btn delete">×</button>
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
          <select v-model="form.level" class="google-select" disabled>
            <option value="CHAPTER">章</option>
            <option value="SECTION">节</option>
            <option value="POINT">知识点</option>
          </select>
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
  border-left: 1px solid #dadce0;
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
  background: #f8f9fa;
  border: 1px solid transparent;
}

.node-content:hover {
  background: #e8f0fe;
  border-color: #d2e3fc;
}

.node-title {
  flex: 1;
  font-weight: 500;
}

.chapter .node-title { font-size: 16px; color: #1a73e8; }
.section .node-title { font-size: 14px; color: #202124; }
.point .node-title { font-size: 13px; color: #5f6368; }

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
  color: #5f6368;
}

.icon-btn:hover {
  background: rgba(0,0,0,0.05);
  color: #1a73e8;
}

.icon-btn.delete:hover {
  color: #d93025;
}

.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
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

.google-input, .google-select {
  width: 100%;
  padding: 8px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  margin-top: 4px;
}
</style>
