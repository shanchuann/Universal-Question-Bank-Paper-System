<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

const systemEnabled = ref(true)
const loading = ref(false)

const fetchSystemStatus = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/admin/system/status', {
      headers: { Authorization: `Bearer ${token}` }
    })
    systemEnabled.value = response.data.enabled
  } catch (error) {
    console.error('Failed to fetch system status', error)
  } finally {
    loading.value = false
  }
}

const toggleSystem = async () => {
  const newState = !systemEnabled.value
  if (!confirm(`Are you sure you want to ${newState ? 'enable' : 'disable'} the system?`)) return
  
  try {
    const token = localStorage.getItem('token')
    await axios.put('/api/admin/system/status', { enabled: newState }, {
      headers: { Authorization: `Bearer ${token}` }
    })
    systemEnabled.value = newState
  } catch (error) {
    console.error('Failed to update system status', error)
    alert('Failed to update system status')
  }
}

onMounted(() => {
  fetchSystemStatus()
})
</script>

<template>
  <div class="admin-container container">
    <div class="header-row">
      <h1>System Settings</h1>
    </div>

    <div class="google-card settings-card">
      <div class="setting-item">
        <div class="setting-info">
          <h3>System Availability</h3>
          <p>Enable or disable the system for all users. Admins can still access.</p>
        </div>
        <div class="setting-control">
          <label class="switch">
            <input type="checkbox" :checked="systemEnabled" @change="toggleSystem">
            <span class="slider round"></span>
          </label>
          <span class="status-label">{{ systemEnabled ? 'Enabled' : 'Disabled' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-container {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
}

.settings-card {
  padding: 24px;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
}

.setting-info h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 500;
}

.setting-info p {
  margin: 0;
  color: #5f6368;
}

.setting-control {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* Switch Styles */
.switch {
  position: relative;
  display: inline-block;
  width: 48px;
  height: 24px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 24px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .slider {
  background-color: #1a73e8;
}

input:focus + .slider {
  box-shadow: 0 0 1px #1a73e8;
}

input:checked + .slider:before {
  transform: translateX(24px);
}

.status-label {
  font-weight: 500;
  min-width: 60px;
}
</style>
