<template>
  <div class="container">
    <div class="google-card form-card">
      <div class="card-header">
        <h1>Knowledge Point Management</h1>
        <p class="subtitle">Add, edit, and delete knowledge points</p>
      </div>
      <form @submit.prevent="handleAdd">
        <div class="form-group">
          <label>Name</label>
          <input v-model="newPoint.name" type="text" required placeholder="e.g. Algebra" />
        </div>
        <div class="form-group">
          <label>Parent</label>
          <select v-model="newPoint.parentId" class="google-select">
            <option value="">None (Top Level)</option>
            <option v-for="kp in points" :key="kp.id" :value="kp.id">{{ kp.name }}</option>
          </select>
        </div>
        <button type="submit" class="primary-btn">Add Knowledge Point</button>
      </form>
      <div class="list-section">
        <h2>Knowledge Points</h2>
        <ul>
          <li v-for="kp in points" :key="kp.id">
            <span>{{ kp.name }}</span>
            <button @click="handleDelete(kp.id)" class="remove-btn">Delete</button>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
const points = ref<any[]>([])
const newPoint = ref({ name: '', parentId: '' })
const fetchPoints = async () => {
  const token = localStorage.getItem('token')
  const res = await axios.get('/api/knowledge-points', { headers: { Authorization: `Bearer ${token}` } })
  points.value = res.data.filter((p:any)=>p.level==='POINT')
}
onMounted(fetchPoints)
const handleAdd = async () => {
  const token = localStorage.getItem('token')
  await axios.post('/api/knowledge-points', newPoint.value, { headers: { Authorization: `Bearer ${token}` } })
  newPoint.value = { name: '', parentId: '' }
  fetchPoints()
}
const handleDelete = async (id:string) => {
  const token = localStorage.getItem('token')
  await axios.delete(`/api/knowledge-points/${id}`, { headers: { Authorization: `Bearer ${token}` } })
  fetchPoints()
}
</script>
<style scoped>
.form-card { max-width: 600px; margin: 32px auto; padding: 32px; background: #fff; border-radius: 18px; box-shadow: 0 4px 24px rgba(60,64,67,0.12), 0 1.5px 6px rgba(60,64,67,0.08); }
.card-header { text-align: center; margin-bottom: 24px; }
.primary-btn { width: 100%; padding: 12px; font-size: 16px; color: #fff; background: linear-gradient(90deg,#4285f4 0%,#1a73e8 100%); border: none; border-radius: 10px; box-shadow: 0 2px 8px rgba(66,133,244,0.12); cursor: pointer; margin-top: 12px; }
.remove-btn { color: #fff; background: #ea4335; border: none; padding: 6px 14px; border-radius: 8px; font-size: 14px; cursor: pointer; margin-left: 12px; }
.list-section { margin-top: 32px; }
.list-section ul { list-style: none; padding: 0; }
.list-section li { display: flex; align-items: center; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #eee; }
</style>
