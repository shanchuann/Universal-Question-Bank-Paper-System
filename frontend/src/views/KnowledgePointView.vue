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
    alert('Failed to save')
  }
}

const deletePoint = async (id: string) => {
  if (!confirm('Are you sure? This will delete the point and potentially its children.')) return
  
  const token = localStorage.getItem('token')
  try {
    await axios.delete(`/api/knowledge-points/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    fetchPoints()
  } catch (err) {
    console.error(err)
    alert('Failed to delete')
  }
}

onMounted(fetchPoints)
</script>

<template>
  <div class="container">
    <div class="google-card">
      <div class="header">
        <h1>Knowledge Graph</h1>
        <button @click="openAddDialog()" class="google-btn primary-btn">+ Add Chapter</button>
      </div>
      
      <div class="tree-view">
        <div v-if="loading">Loading...</div>
        <div v-else-if="treeData.length === 0">No knowledge points found.</div>
        
        <ul class="tree-root">
          <li v-for="chapter in treeData" :key="chapter.id" class="tree-node chapter">
            <div class="node-content">
              <span class="node-title">{{ chapter.name }}</span>
              <div class="actions">
                <button @click="openEditDialog(chapter)" class="icon-btn">✎</button>
                <button @click="openAddDialog(chapter)" class="icon-btn" title="Add Section">+</button>
                <button @click="deletePoint(chapter.id)" class="icon-btn delete">×</button>
              </div>
            </div>
            
            <ul v-if="chapter.children && chapter.children.length > 0" class="tree-children">
              <li v-for="section in chapter.children" :key="section.id" class="tree-node section">
                <div class="node-content">
                  <span class="node-title">{{ section.name }}</span>
                  <div class="actions">
                    <button @click="openEditDialog(section)" class="icon-btn">✎</button>
                    <button @click="openAddDialog(section)" class="icon-btn" title="Add Point">+</button>
                    <button @click="deletePoint(section.id)" class="icon-btn delete">×</button>
                  </div>
                </div>
                
                <ul v-if="section.children && section.children.length > 0" class="tree-children">
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
        <h2>{{ editingPoint ? 'Edit' : 'Add' }} Knowledge Point</h2>
        <div class="form-group">
          <label>Name</label>
          <input v-model="form.name" class="google-input" />
        </div>
        <div class="form-group">
          <label>Level</label>
          <select v-model="form.level" class="google-select" disabled>
            <option value="CHAPTER">Chapter</option>
            <option value="SECTION">Section</option>
            <option value="POINT">Point</option>
          </select>
        </div>
        <div class="form-group">
          <label>Sort Order</label>
          <input type="number" v-model="form.sortOrder" class="google-input" />
        </div>
        <div class="modal-actions">
          <button @click="showDialog = false" class="google-btn text-btn">Cancel</button>
          <button @click="savePoint" class="google-btn primary-btn">Save</button>
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
