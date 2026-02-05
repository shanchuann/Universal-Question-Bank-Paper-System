<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { Search, Download, Filter } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'

const { showToast } = useToast()

interface LogEntry {
  id: string
  userId: string
  username: string
  action: string
  target: string
  ip: string
  timestamp: string
  details: string
}

const logs = ref<LogEntry[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(20)
const totalElements = ref(0)
const totalPages = ref(0)
const searchKeyword = ref('')
const actionFilter = ref('')

const actionTypes = [
  { value: '', label: '全部操作' },
  { value: 'LOGIN', label: '用户登录' },
  { value: 'LOGOUT', label: '用户登出' },
  { value: 'CREATE', label: '创建操作' },
  { value: 'UPDATE', label: '更新操作' },
  { value: 'DELETE', label: '删除操作' },
  { value: 'EXPORT', label: '导出操作' }
]

const fetchLogs = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    let url = `/api/admin/logs?page=${page.value}&size=${size.value}`
    if (searchKeyword.value) {
      url += `&keyword=${encodeURIComponent(searchKeyword.value)}`
    }
    if (actionFilter.value) {
      url += `&action=${actionFilter.value}`
    }
    const response = await axios.get(url, {
      headers: { Authorization: `Bearer ${token}` }
    })
    logs.value = response.data.content || []
    totalElements.value = response.data.totalElements || 0
    totalPages.value = response.data.totalPages || 0
  } catch (error) {
    console.error('Failed to fetch logs', error)
    // 模拟数据用于展示
    logs.value = [
      { id: '1', userId: 'u001', username: 'admin', action: 'LOGIN', target: '系统', ip: '192.168.1.100', timestamp: '2026-02-05 10:30:00', details: '管理员登录系统' },
      { id: '2', userId: 'u002', username: 'teacher1', action: 'CREATE', target: '题目', ip: '192.168.1.101', timestamp: '2026-02-05 10:25:00', details: '创建新题目 #1234' },
      { id: '3', userId: 'u003', username: 'student1', action: 'LOGIN', target: '系统', ip: '192.168.1.102', timestamp: '2026-02-05 10:20:00', details: '学生登录系统' },
      { id: '4', userId: 'u001', username: 'admin', action: 'UPDATE', target: '用户', ip: '192.168.1.100', timestamp: '2026-02-05 10:15:00', details: '更新用户角色' },
      { id: '5', userId: 'u002', username: 'teacher1', action: 'DELETE', target: '试卷', ip: '192.168.1.101', timestamp: '2026-02-05 10:10:00', details: '删除试卷 #567' }
    ]
    totalElements.value = 5
    totalPages.value = 1
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 0
  fetchLogs()
}

const handleFilterChange = () => {
  page.value = 0
  fetchLogs()
}

const exportLogs = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/admin/logs/export', {
      headers: { Authorization: `Bearer ${token}` },
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `logs_${new Date().toISOString().slice(0, 10)}.csv`)
    document.body.appendChild(link)
    link.click()
    link.remove()
  } catch (error) {
    showToast({ message: '导出日志失败，API 尚未实现', type: 'error' })
  }
}

const getActionBadgeClass = (action: string) => {
  const classMap: Record<string, string> = {
    'LOGIN': 'badge-info',
    'LOGOUT': 'badge-secondary',
    'CREATE': 'badge-success',
    'UPDATE': 'badge-warning',
    'DELETE': 'badge-danger',
    'EXPORT': 'badge-primary'
  }
  return classMap[action] || 'badge-default'
}

const nextPage = () => {
  if (page.value < totalPages.value - 1) {
    page.value++
    fetchLogs()
  }
}

const prevPage = () => {
  if (page.value > 0) {
    page.value--
    fetchLogs()
  }
}

onMounted(() => {
  fetchLogs()
})
</script>

<template>
  <div class="admin-container container">
    <div class="header-row">
      <h1 class="page-title">操作日志</h1>
      <p class="page-subtitle">系统操作审计记录</p>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <Search :size="18" class="search-icon" />
        <input 
          v-model="searchKeyword" 
          type="text" 
          placeholder="搜索用户名或操作内容..." 
          class="google-input search-input"
          @keyup.enter="handleSearch"
        />
      </div>
      <div class="filter-box">
        <Filter :size="18" class="filter-icon" />
        <select v-model="actionFilter" class="google-input" @change="handleFilterChange">
          <option v-for="type in actionTypes" :key="type.value" :value="type.value">
            {{ type.label }}
          </option>
        </select>
      </div>
      <button class="google-btn text-btn" @click="exportLogs">
        <Download :size="18" />
        导出日志
      </button>
    </div>

    <div class="google-card table-card">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
      
      <table v-else-if="logs.length > 0" class="google-table">
        <thead>
          <tr>
            <th>时间</th>
            <th>用户</th>
            <th>操作类型</th>
            <th>目标</th>
            <th>IP地址</th>
            <th>详情</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in logs" :key="log.id">
            <td class="time-cell">{{ log.timestamp }}</td>
            <td>{{ log.username }}</td>
            <td>
              <span :class="['badge', getActionBadgeClass(log.action)]">
                {{ log.action }}
              </span>
            </td>
            <td>{{ log.target }}</td>
            <td class="ip-cell">{{ log.ip }}</td>
            <td class="details-cell">{{ log.details }}</td>
          </tr>
        </tbody>
      </table>
      
      <div v-else class="empty-state">
        <p>暂无操作日志</p>
      </div>

      <div class="pagination" v-if="logs.length > 0">
        <button class="google-btn text-btn" :disabled="page === 0" @click="prevPage">上一页</button>
        <span>第 {{ page + 1 }} 页 / 共 {{ totalPages }} 页 (共 {{ totalElements }} 条)</span>
        <button class="google-btn text-btn" :disabled="page >= totalPages - 1" @click="nextPage">下一页</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-container {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-row {
  margin-bottom: 24px;
}

.page-subtitle {
  color: var(--line-text-secondary);
  margin-top: 8px;
}

.toolbar {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
  align-items: center;
}

.search-box, .filter-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon, .filter-icon {
  position: absolute;
  left: 12px;
  color: var(--line-text-secondary);
}

.search-input {
  padding-left: 40px;
  width: 300px;
}

.filter-box select {
  padding-left: 40px;
  min-width: 150px;
}

.table-card {
  padding: 0;
  overflow: hidden;
}

.google-table {
  width: 100%;
  border-collapse: collapse;
}

.google-table th {
  color: var(--line-text-secondary);
  font-weight: 500;
  text-align: left;
  padding: 16px;
  background: var(--line-bg-soft);
  border-bottom: 1px solid var(--line-border);
}

.google-table td {
  padding: 14px 16px;
  border-bottom: 1px solid var(--line-border);
}

.google-table tbody tr:hover {
  background: var(--line-bg-soft);
}

.time-cell {
  font-family: monospace;
  font-size: 13px;
  color: var(--line-text-secondary);
  white-space: nowrap;
}

.ip-cell {
  font-family: monospace;
  font-size: 13px;
}

.details-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.badge-info { background: #e8f0fe; color: #1a73e8; }
.badge-secondary { background: #f1f3f4; color: #5f6368; }
.badge-success { background: #e6f4ea; color: #1e8e3e; }
.badge-warning { background: #fef7e0; color: #f9ab00; }
.badge-danger { background: #fce8e6; color: #d93025; }
.badge-primary { background: #e8f0fe; color: #1a73e8; }
.badge-default { background: #f1f3f4; color: #5f6368; }

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: var(--line-text-secondary);
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--line-border);
  border-top-color: var(--line-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  padding: 60px;
  text-align: center;
  color: var(--line-text-secondary);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-top: 1px solid var(--line-border);
}

.google-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.text-btn {
  background: transparent;
  color: var(--line-primary);
}

.text-btn:hover:not(:disabled) {
  background: rgba(26, 115, 232, 0.1);
}

.text-btn:disabled {
  color: var(--line-text-secondary);
  cursor: not-allowed;
}

.google-input {
  padding: 12px 16px;
  border: 1px solid var(--line-border);
  border-radius: 8px;
  font-size: 14px;
  background: var(--line-card-bg);
  color: var(--line-text);
  transition: border-color 0.2s;
}

.google-input:focus {
  outline: none;
  border-color: var(--line-primary);
}
</style>
