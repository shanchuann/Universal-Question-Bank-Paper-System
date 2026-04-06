<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted, nextTick } from 'vue'
import axios from 'axios'
import { Search, Download, Filter } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'
import GoogleSelect from '@/components/GoogleSelect.vue'

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
const errorMessage = ref('')

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
  errorMessage.value = ''
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
    logs.value = []
    totalElements.value = 0
    totalPages.value = 0
    errorMessage.value = '获取操作日志失败，请检查管理员权限或后端服务状态'
    showToast({ message: errorMessage.value, type: 'error' })
  } finally {
    loading.value = false
  }
}

const formatLogTime = (value: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const handleSearch = () => {
  page.value = 0
  fetchLogs()
}

const handleFilterChange = () => {
  page.value = 0
  fetchLogs()
}

watch(actionFilter, () => {
  handleFilterChange()
})

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
    showToast({ message: '导出日志失败，请检查后端服务或权限后重试', type: 'error' })
  }
}

const getActionBadgeClass = (action: string) => {
  const classMap: Record<string, string> = {
    LOGIN: 'badge-info',
    LOGOUT: 'badge-secondary',
    CREATE: 'badge-success',
    UPDATE: 'badge-warning',
    DELETE: 'badge-danger',
    EXPORT: 'badge-primary'
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

// 表格容器：支持横向拖拽平移（与试题列表、用户列表一致的交互）
const tableWrapRef = ref<HTMLElement | null>(null)
let isDragging = false
let dragStartX = 0
let dragStartScroll = 0
let tableListenersAttached = false

const onPointerDownTable = (e: PointerEvent) => {
  const el = tableWrapRef.value
  if (!el) return
  const target = e.target as HTMLElement | null
  if (target && target.closest && target.closest('button, a, input, select, textarea, .icon-btn, .badge')) {
    return
  }
  isDragging = true
  dragStartX = e.clientX
  dragStartScroll = el.scrollLeft
  try {
    el.setPointerCapture?.(e.pointerId)
  } catch (err) {}
  el.classList.add('dragging')
  e.preventDefault()
}

const onPointerMoveTable = (e: PointerEvent) => {
  const el = tableWrapRef.value
  if (!el || !isDragging) return
  const walk = e.clientX - dragStartX
  el.scrollLeft = dragStartScroll - walk
}

const onPointerUpTable = (e: PointerEvent) => {
  const el = tableWrapRef.value
  if (!el) return
  isDragging = false
  try {
    el.releasePointerCapture?.(e.pointerId)
  } catch (err) {}
  el.classList.remove('dragging')
}

const attachTableListeners = () => {
  const el = tableWrapRef.value
  if (!el || tableListenersAttached) return
  el.addEventListener('pointerdown', onPointerDownTable)
  el.addEventListener('pointermove', onPointerMoveTable)
  el.addEventListener('pointerup', onPointerUpTable)
  el.addEventListener('pointerleave', onPointerUpTable)
  tableListenersAttached = true
}

const detachTableListeners = () => {
  const el = tableWrapRef.value
  if (!el || !tableListenersAttached) return
  el.removeEventListener('pointerdown', onPointerDownTable)
  el.removeEventListener('pointermove', onPointerMoveTable)
  el.removeEventListener('pointerup', onPointerUpTable)
  el.removeEventListener('pointerleave', onPointerUpTable)
  tableListenersAttached = false
}

onMounted(() => nextTick(() => attachTableListeners()))
onUnmounted(() => detachTableListeners())

watch(logs, (val) => {
  if (val && val.length > 0) {
    nextTick(() => attachTableListeners())
  }
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
        <GoogleSelect v-model="actionFilter" :options="actionTypes" placeholder="全部操作" />
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

      <div v-else-if="errorMessage" class="empty-state">
        <p>{{ errorMessage }}</p>
        <button class="google-btn text-btn" @click="fetchLogs">重试</button>
      </div>

      <div v-else-if="logs.length > 0" ref="tableWrapRef" class="table-responsive">
        <table class="google-table">
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
              <td class="time-cell">{{ formatLogTime(log.timestamp) }}</td>
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
      </div>

      <div v-else class="empty-state">
        <p>暂无操作日志</p>
      </div>

      <div class="pagination" v-if="logs.length > 0">
        <button class="google-btn text-btn" :disabled="page === 0" @click="prevPage">上一页</button>
        <span>第 {{ page + 1 }} 页 / 共 {{ totalPages }} 页 (共 {{ totalElements }} 条)</span>
        <button class="google-btn text-btn" :disabled="page >= totalPages - 1" @click="nextPage">
          下一页
        </button>
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

.search-box,
.filter-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon,
.filter-icon {
  position: absolute;
  left: 12px;
  color: var(--line-text-secondary);
}

.search-input {
  padding-left: 40px;
  width: 300px;
}

.filter-box :deep(.google-select-container) {
  min-width: 170px;
}

.filter-box :deep(.select-trigger) {
  padding-left: 40px;
}

.table-card {
  padding: 0;
  overflow: hidden;
}

.google-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 920px;
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

.google-table th,
.google-table td {
  white-space: nowrap;
  text-align: center;
  vertical-align: middle;
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
  white-space: nowrap;
}

.table-responsive {
  overflow: auto;
  cursor: grab;
}

.table-responsive.dragging {
  cursor: grabbing;
  user-select: none;
}

.badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.badge-info {
  background: color-mix(in srgb, var(--line-accent) 14%, white);
  color: var(--line-accent);
}
.badge-secondary {
  background: var(--line-bg-hover);
  color: var(--line-text-secondary);
}
.badge-success {
  background: color-mix(in srgb, var(--line-success) 14%, white);
  color: var(--line-success);
}
.badge-warning {
  background: color-mix(in srgb, var(--line-warning) 18%, white);
  color: color-mix(in srgb, var(--line-warning) 85%, black);
}
.badge-danger {
  background: color-mix(in srgb, var(--line-error) 14%, white);
  color: var(--line-error);
}
.badge-primary {
  background: color-mix(in srgb, var(--line-primary) 10%, white);
  color: var(--line-primary);
}
.badge-default {
  background: var(--line-bg-hover);
  color: var(--line-text-secondary);
}

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
  to {
    transform: rotate(360deg);
  }
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
  border-top: none;
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
  background: color-mix(in srgb, var(--line-primary) 10%, transparent);
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
