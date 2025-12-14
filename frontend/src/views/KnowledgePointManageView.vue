<template>
  <div class="container kp-container">
    <div class="header-row">
      <h1>Knowledge Point Management</h1>
    </div>

    <div class="content-grid">
      <!-- Left Column: Form -->
      <div class="google-card form-card">
        <div class="card-header-simple">
          <h2>{{ newPoint.id ? 'Edit Knowledge Point' : 'Add New Point' }}</h2>
        </div>
        
        <form @submit.prevent="handleAdd">
          <div class="form-group">
            <label>Name</label>
            <input v-model="newPoint.name" type="text" required placeholder="e.g. Algebra" class="google-input" />
          </div>

          <div class="form-group">
            <label>Level</label>
            <select v-model="newPoint.level" class="google-select">
              <option value="CHAPTER">Chapter</option>
              <option value="SECTION">Section</option>
              <option value="POINT">Point</option>
            </select>
          </div>

          <div class="form-group">
            <label>Subject</label>
            <input v-model="newPoint.subjectId" type="text" placeholder="general" class="google-input" />
          </div>

          <div class="form-group">
            <label>Parent</label>
            <select v-model="newPoint.parentId" class="google-select">
              <option value="">None (top level)</option>
              <option v-for="kp in parentOptions" :key="kp.id" :value="kp.id">{{ kp.label }}</option>
            </select>
          </div>

          <div class="form-group">
            <label>Sort Order</label>
            <input v-model.number="newPoint.sortOrder" type="number" min="0" placeholder="0" class="google-input" />
          </div>

          <div class="form-actions">
            <button type="submit" class="google-btn primary-btn" :disabled="loading">
              {{ loading ? 'Saving...' : 'Add Knowledge Point' }}
            </button>
            <button type="button" v-if="newPoint.id" @click="resetForm" class="google-btn text-btn">
              Cancel
            </button>
          </div>
          
          <p v-if="error" class="error-text">{{ error }}</p>
          <p v-if="message" class="success-text">{{ message }}</p>
        </form>
      </div>

      <!-- Right Column: Tree View -->
      <div class="google-card tree-card">
        <div class="card-header-simple">
          <h2>Structure</h2>
          <button @click="fetchPoints" class="google-btn text-btn small-btn">Refresh</button>
        </div>

        <div v-if="loading && !points.length" class="loading-state">
          <div class="spinner"></div>
          <p>Loading structure...</p>
        </div>
        
        <div v-else-if="treeData.length === 0" class="empty-state">
          <p>No knowledge points found.</p>
        </div>

        <div class="tree-container" v-else>
          <ul class="tree-root">
            <li v-for="chapter in treeData" :key="chapter.id" class="tree-node-item">
              <div class="node-row chapter-row">
                <div class="node-info">
                  <span class="node-icon">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path></svg>
                  </span>
                  <span class="node-name">{{ chapter.name }}</span>
                  <span class="node-meta" v-if="chapter.sortOrder">#{{ chapter.sortOrder }}</span>
                </div>
                <div class="node-actions">
                  <button @click="handleEdit(chapter)" class="icon-action" title="Edit">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path></svg>
                  </button>
                  <button @click="handleDelete(chapter.id)" class="icon-action" title="Delete">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
                  </button>
                </div>
              </div>
              
              <ul v-if="chapter.children?.length" class="tree-children">
                <li v-for="section in chapter.children" :key="section.id" class="tree-node-item">
                  <div class="node-row section-row">
                    <div class="node-info">
                      <span class="node-icon">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path></svg>
                      </span>
                      <span class="node-name">{{ section.name }}</span>
                      <span class="node-meta" v-if="section.sortOrder">#{{ section.sortOrder }}</span>
                    </div>
                    <div class="node-actions">
                      <button @click="handleEdit(section)" class="icon-action" title="Edit">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path></svg>
                      </button>
                      <button @click="handleDelete(section.id)" class="icon-action" title="Delete">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
                      </button>
                    </div>
                  </div>

                  <ul v-if="section.children?.length" class="tree-children">
                    <li v-for="point in section.children" :key="point.id" class="tree-node-item">
                      <div class="node-row point-row">
                        <div class="node-info">
                          <span class="node-icon">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                          </span>
                          <span class="node-name">{{ point.name }}</span>
                          <span class="node-meta" v-if="point.sortOrder">#{{ point.sortOrder }}</span>
                        </div>
                        <div class="node-actions">
                          <button @click="handleEdit(point)" class="icon-action" title="Edit">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path></svg>
                          </button>
                          <button @click="handleDelete(point.id)" class="icon-action" title="Delete">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
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
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'

interface KnowledgePoint {
  id: string
  name: string
  parentId: string | null
  subjectId?: string
  level: 'CHAPTER' | 'SECTION' | 'POINT'
  sortOrder?: number
  children?: KnowledgePoint[]
}

const points = ref<KnowledgePoint[]>([])
const loading = ref(false)
const error = ref('')
const message = ref('')

const newPoint = ref<{
  id?: string
  name: string
  level: 'CHAPTER' | 'SECTION' | 'POINT'
  subjectId: string
  parentId: string
  sortOrder: number
}>({
  name: '',
  level: 'CHAPTER',
  subjectId: 'general',
  parentId: '',
  sortOrder: 0
})

const fetchPoints = async () => {
  loading.value = true
  error.value = ''
  try {
    const token = localStorage.getItem('token')
    const res = await axios.get('/api/knowledge-points', { headers: { Authorization: `Bearer ${token}` } })
    points.value = res.data
  } catch (e: any) {
    error.value = e?.response?.data || 'Failed to load knowledge points'
  } finally {
    loading.value = false
  }
}
onMounted(fetchPoints)

const parentOptions = computed(() => {
  const lvl = newPoint.value.level
  if (lvl === 'CHAPTER') return []
  if (lvl === 'SECTION') return points.value.filter(p => p.level === 'CHAPTER').map(p => ({ id: p.id, label: p.name }))
  return points.value.filter(p => p.level === 'SECTION').map(p => ({ id: p.id, label: p.name }))
})

const treeData = computed(() => {
  const map = new Map<string, KnowledgePoint>()
  const roots: KnowledgePoint[] = []
  // Deep copy to avoid mutating original list during tree construction if re-computed
  const pointsCopy = JSON.parse(JSON.stringify(points.value))
  
  pointsCopy.forEach((p: KnowledgePoint) => map.set(p.id, { ...p, children: [] }))
  
  map.forEach(p => {
    if (p.parentId && map.has(p.parentId)) {
      map.get(p.parentId)!.children!.push(p)
    } else {
      roots.push(p)
    }
  })
  
  const sortFn = (a: KnowledgePoint, b: KnowledgePoint) => (a.sortOrder || 0) - (b.sortOrder || 0)
  const sortRecursive = (arr: KnowledgePoint[]) => {
    arr.sort(sortFn)
    arr.forEach(child => child.children && sortRecursive(child.children))
  }
  sortRecursive(roots)
  return roots
})

const resetForm = () => {
  newPoint.value = { name: '', level: 'CHAPTER', subjectId: 'general', parentId: '', sortOrder: 0 }
  error.value = ''
  message.value = ''
}

const handleAdd = async () => {
  error.value = ''
  message.value = ''
  if (!newPoint.value.name.trim()) {
    error.value = 'Name is required'
    return
  }
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const payload = {
      name: newPoint.value.name.trim(),
      parentId: newPoint.value.parentId || null,
      level: newPoint.value.level,
      sortOrder: newPoint.value.sortOrder ?? 0,
      subjectId: newPoint.value.subjectId || 'general'
    }

    if (newPoint.value.id) {
      await axios.put(`/api/knowledge-points/${newPoint.value.id}`, payload, { headers: { Authorization: `Bearer ${token}` } })
      message.value = 'Updated successfully'
    } else {
      await axios.post('/api/knowledge-points', payload, { headers: { Authorization: `Bearer ${token}` } })
      message.value = 'Created successfully'
    }
    
    resetForm()
    await fetchPoints()
  } catch (e: any) {
    error.value = e?.response?.data || 'Failed to save'
  } finally {
    loading.value = false
  }
}

const handleEdit = (point: KnowledgePoint) => {
  newPoint.value = {
    id: point.id,
    name: point.name,
    level: point.level,
    subjectId: point.subjectId || 'general',
    parentId: point.parentId || '',
    sortOrder: point.sortOrder || 0
  }
  // Scroll to top to see form
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handleDelete = async (id: string) => {
  if (!confirm('Delete this knowledge point (and its children if any)?')) return
  loading.value = true
  error.value = ''
  try {
    const token = localStorage.getItem('token')
    await axios.delete(`/api/knowledge-points/${id}`, { headers: { Authorization: `Bearer ${token}` } })
    await fetchPoints()
  } catch (e: any) {
    error.value = e?.response?.data || 'Failed to delete'
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.kp-container {
  max-width: 1200px;
  margin: 32px auto;
}

.header-row {
  margin-bottom: 24px;
}
.header-row h1 {
  font-family: 'Google Sans', sans-serif;
  font-size: 24px;
  color: #202124;
  font-weight: 400;
}

.content-grid {
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: 24px;
  align-items: start;
}

@media (max-width: 900px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

.card-header-simple {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f3f4;
}

.card-header-simple h2 {
  font-size: 18px;
  font-weight: 500;
  color: #202124;
  margin: 0;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #3c4043;
  font-size: 14px;
}

.form-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

.error-text { color: #d93025; margin-top: 12px; font-size: 14px; }
.success-text { color: #188038; margin-top: 12px; font-size: 14px; }

/* Tree Styles */
.tree-container {
  margin-top: 12px;
}

.tree-root, .tree-children {
  list-style: none;
  padding-left: 0;
}

.tree-children {
  padding-left: 24px;
  border-left: 1px solid #e8eaed;
  margin-left: 12px;
}

.tree-node-item {
  margin-bottom: 4px;
}

.node-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background-color 0.2s;
  border: 1px solid transparent;
}

.node-row:hover {
  background-color: #f8f9fa;
  border-color: #f1f3f4;
}

.chapter-row { background-color: #fff; }
.section-row { }
.point-row { }

.node-info {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #3c4043;
  height: 28px; /* Fixed height for better vertical alignment */
}

.node-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: #5f6368;
}

.node-name {
  font-weight: 500;
  line-height: 1; /* Reset line height to let flexbox handle centering */
  padding-top: 1px; /* Micro-adjustment for visual center */
}

.node-meta {
  font-size: 12px;
  color: #9aa0a6;
  background: #f1f3f4;
  padding: 2px 6px;
  border-radius: 4px;
}

.node-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.node-row:hover .node-actions {
  opacity: 1;
}

.icon-action {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 4px;
  border-radius: 4px;
  opacity: 0.6;
  transition: all 0.2s;
}

.icon-action:hover {
  background-color: #fce8e6;
  opacity: 1;
}

.loading-state {
  padding: 40px;
  text-align: center;
  color: #5f6368;
}

.spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #f1f3f4;
  border-top-color: #1a73e8;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
