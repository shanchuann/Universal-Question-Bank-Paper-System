<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { Users, Search, UserCheck, UserX, Trash2, RefreshCw, Shield, GraduationCap, User as UserIcon } from 'lucide-vue-next'
import { useConfirm } from '@/composables/useConfirm'
import { useToast } from '@/composables/useToast'

const { confirm } = useConfirm()
const { showToast } = useToast()

interface User {
  id: string
  username: string
  nickname: string
  email: string
  role: string
  status: string
  avatarUrl?: string
}

const users = ref<User[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)
const currentTab = ref('ALL')
const searchKeyword = ref('')
const errorMessage = ref('')
const roleCounts = ref({
  ALL: 0,
  TEACHER: 0,
  USER: 0
})

const getDisplayedCount = () => {
  if (currentTab.value === 'ALL') return roleCounts.value.ALL
  if (currentTab.value === 'TEACHER') return roleCounts.value.TEACHER
  if (currentTab.value === 'USER') return roleCounts.value.USER
  return users.value.length
}

const applyClientFilters = (list: User[]) => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return list.filter((u) => {
    if (u.role === 'ADMIN') return false
    if (!keyword) return true
    return (u.username || '').toLowerCase().includes(keyword) || (u.nickname || '').toLowerCase().includes(keyword)
  })
}

const fetchRoleCounts = async () => {
  try {
    const token = localStorage.getItem('token')
    const [teacherRes, userRes] = await Promise.all([
      axios.get('/api/admin/users?page=0&size=1&role=TEACHER', {
        headers: { Authorization: `Bearer ${token}` }
      }),
      axios.get('/api/admin/users?page=0&size=1&role=USER', {
        headers: { Authorization: `Bearer ${token}` }
      })
    ])

    const teacherCount = teacherRes.data?.totalElements || 0
    const userCount = userRes.data?.totalElements || 0
    roleCounts.value = {
      ALL: teacherCount + userCount,
      TEACHER: teacherCount,
      USER: userCount
    }
  } catch (error) {
    console.error('Failed to fetch role counts', error)
  }
}

const fetchUsers = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const token = localStorage.getItem('token')
    let url = `/api/admin/users?page=${page.value}&size=${size.value}`
    if (currentTab.value !== 'ALL' && currentTab.value !== 'ADMIN') {
      url += `&role=${currentTab.value}`
    }
    const response = await axios.get(url, {
      headers: { Authorization: `Bearer ${token}` }
    })
    users.value = applyClientFilters(response.data.content || [])
    totalElements.value = getDisplayedCount()
    totalPages.value = response.data.totalPages
  } catch (error) {
    console.error('Failed to fetch users', error)
    users.value = []
    totalElements.value = 0
    totalPages.value = 0
    errorMessage.value = '获取用户列表失败，请检查权限或后端服务状态'
    showToast({ message: errorMessage.value, type: 'error' })
  } finally {
    loading.value = false
  }
}

const setTab = (tab: string) => {
  currentTab.value = tab
  page.value = 0
  fetchUsers()
}

const deleteUser = async (id: string) => {
  const confirmed = await confirm({
    title: '删除用户',
    message: '确定要删除该用户吗？此操作无法撤销。',
    type: 'danger',
    confirmText: '删除',
    cancelText: '取消'
  })
  if (!confirmed) return
  try {
    const token = localStorage.getItem('token')
    await axios.delete(`/api/admin/users/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    fetchUsers()
  } catch (error) {
    console.error('Failed to delete user', error)
    showToast({ message: '删除用户失败', type: 'error' })
  }
}

const toggleStatus = async (user: User) => {
  const newStatus = user.status === 'ACTIVE' ? 'BANNED' : 'ACTIVE'
  try {
    const token = localStorage.getItem('token')
    await axios.put(`/api/admin/users/${user.id}/status`, { status: newStatus }, {
      headers: { Authorization: `Bearer ${token}` }
    })
    user.status = newStatus
  } catch (error) {
    console.error('Failed to update status', error)
    showToast({ message: '更新状态失败', type: 'error' })
  }
}

const toggleRole = async (user: User) => {
  const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
  const roleText = newRole === 'ADMIN' ? '管理员' : '普通用户'
  const confirmed = await confirm({
    title: '更改角色',
    message: `确定要将该用户的角色更改为 ${roleText} 吗？`,
    type: 'warning',
    confirmText: '确认',
    cancelText: '取消'
  })
  if (!confirmed) return
  try {
    const token = localStorage.getItem('token')
    await axios.put(`/api/admin/users/${user.id}/role`, { role: newRole }, {
      headers: { Authorization: `Bearer ${token}` }
    })
    user.role = newRole
  } catch (error) {
    console.error('Failed to update role', error)
    showToast({ message: '更新角色失败', type: 'error' })
  }
}

const nextPage = () => {
  if (page.value < totalPages.value - 1) {
    page.value++
    fetchUsers()
  }
}

const prevPage = () => {
  if (page.value > 0) {
    page.value--
    fetchUsers()
  }
}

const handleSearch = () => {
  page.value = 0
  fetchUsers()
}

const refresh = () => {
  fetchRoleCounts()
  fetchUsers()
}

const getRoleLabel = (role: string) => {
  const labels: Record<string, string> = {
    'ADMIN': '管理员',
    'TEACHER': '教师',
    'USER': '学生'
  }
  return labels[role] || role
}

const getStatusLabel = (status: string) => {
  return status === 'ACTIVE' ? '正常' : '已禁用'
}

onMounted(() => {
  fetchRoleCounts()
  fetchUsers()
})
</script>

<template>
  <div class="admin-container container">
    <div class="header-row">
      <div class="header-content">
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">管理系统中的所有用户账户</p>
      </div>
      <div class="header-actions">
        <button class="google-btn icon-btn" @click="refresh" title="刷新">
          <RefreshCw :size="18" :class="{ spinning: loading }" />
        </button>
      </div>
    </div>

    <div class="toolbar">
      <div class="tabs">
        <button :class="['tab-btn', currentTab === 'ALL' ? 'active' : '']" @click="setTab('ALL')">
          <Users :size="16" />
          全部
          <span class="tab-count">{{ roleCounts.ALL }}</span>
        </button>
        <button :class="['tab-btn', currentTab === 'TEACHER' ? 'active' : '']" @click="setTab('TEACHER')">
          <GraduationCap :size="16" />
          教师
          <span class="tab-count">{{ roleCounts.TEACHER }}</span>
        </button>
        <button :class="['tab-btn', currentTab === 'USER' ? 'active' : '']" @click="setTab('USER')">
          <UserIcon :size="16" />
          学生
          <span class="tab-count">{{ roleCounts.USER }}</span>
        </button>
      </div>
      <div class="search-box">
        <Search :size="18" class="search-icon" />
        <input 
          v-model="searchKeyword" 
          type="text" 
          placeholder="搜索用户名或昵称..." 
          class="google-input search-input"
          @keyup.enter="handleSearch"
        />
      </div>
    </div>

    <div v-if="errorMessage" class="error-banner">
      {{ errorMessage }}
    </div>

    <div class="google-card table-card">
      <div class="count-hint">当前筛选共 {{ getDisplayedCount() }} 人（不含管理员）</div>
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
      <table v-else-if="users.length > 0" class="google-table">
        <thead>
          <tr>
            <th class="col-user">用户信息</th>
            <th class="col-role">角色</th>
            <th class="col-status">状态</th>
            <th class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td class="col-user">
              <div class="user-info">
                <img 
                  v-if="user.avatarUrl" 
                  :src="user.avatarUrl" 
                  :alt="user.username" 
                  class="user-avatar-img"
                />
                <div v-else class="user-avatar">
                  {{ user.username.charAt(0).toUpperCase() }}
                </div>
                <div class="user-details">
                  <span class="user-name">{{ user.username }}</span>
                  <span class="user-nickname">{{ user.nickname || '未设置昵称' }}</span>
                </div>
              </div>
            </td>
            <td class="col-role">
              <span :class="['role-badge', `role-${user.role.toLowerCase()}`]">
                <component :is="user.role === 'ADMIN' ? Shield : user.role === 'TEACHER' ? GraduationCap : UserIcon" :size="14" />
                {{ getRoleLabel(user.role) }}
              </span>
            </td>
            <td class="col-status">
              <span :class="['status-badge', user.status === 'ACTIVE' ? 'status-active' : 'status-banned']">
                <span class="status-dot"></span>
                {{ getStatusLabel(user.status || 'ACTIVE') }}
              </span>
            </td>
            <td class="col-actions">
              <div class="actions">
                <button 
                  class="action-btn" 
                  :class="user.status === 'ACTIVE' ? 'warning' : 'success'" 
                  @click="toggleStatus(user)"
                  :title="user.status === 'ACTIVE' ? '禁用用户' : '启用用户'"
                >
                  <component :is="user.status === 'ACTIVE' ? UserX : UserCheck" :size="16" />
                  {{ user.status === 'ACTIVE' ? '禁用' : '启用' }}
                </button>
                <button class="action-btn primary" @click="toggleRole(user)" title="更改角色">
                  <RefreshCw :size="16" />
                  更改角色
                </button>
                <button class="action-btn danger" @click="deleteUser(user.id)" title="删除用户">
                  <Trash2 :size="16" />
                  删除
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-state">
        <Users :size="48" class="empty-icon" />
        <p>未找到用户</p>
        <span class="empty-hint">尝试更改筛选条件或刷新页面</span>
      </div>
      
      <div class="pagination" v-if="users.length > 0">
        <button class="google-btn text-btn" :disabled="page === 0" @click="prevPage">上一页</button>
        <span>第 {{ page + 1 }} 页 / 共 {{ totalPages }} 页</span>
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
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-content {
  flex: 1;
}

.page-title {
  margin: 0 0 4px 0;
  font-size: 28px;
  font-weight: 600;
  color: var(--line-text);
}

.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--line-text-secondary);
}

.header-actions {
  display: flex;
  gap: 8px;
}

.icon-btn {
  width: 40px;
  height: 40px;
  border: 1px solid var(--line-border);
  background: var(--line-bg);
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--line-text-secondary);
  transition: all 0.2s ease;
}

.icon-btn:hover {
  background: var(--line-bg-soft);
  color: var(--line-primary);
  border-color: var(--line-primary);
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Toolbar */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;
}

.tabs {
  display: flex;
  gap: 4px;
  background: var(--line-bg-soft);
  padding: 4px;
  border-radius: 10px;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  padding: 10px 16px;
  font-family: inherit;
  font-size: 14px;
  font-weight: 500;
  color: var(--line-text-secondary);
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  color: var(--line-text);
  background: var(--line-bg);
}

.tab-btn.active {
  color: var(--line-primary);
  background: var(--line-bg);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.tab-count {
  background: var(--line-primary);
  color: white;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

.search-box {
  position: relative;
  width: 280px;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--line-text-secondary);
}

.search-input {
  width: 100%;
  height: 40px;
  padding: 0 12px 0 40px;
  border: 1px solid var(--line-border);
  border-radius: 8px;
  font-size: 14px;
  background: var(--line-bg);
  color: var(--line-text);
  transition: all 0.2s ease;
}

.search-input:focus {
  outline: none;
  border-color: var(--line-primary);
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.15);
}

.error-banner {
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 10px;
  color: var(--line-error);
  border: 1px solid color-mix(in srgb, var(--line-error) 30%, transparent);
  background: color-mix(in srgb, var(--line-error) 10%, white);
}

/* Table Card */
.google-card {
  background: var(--line-bg);
  border: 1px solid var(--line-border);
  border-radius: 12px;
  overflow: hidden;
}

.table-card {
  padding: 0;
}

.count-hint {
  padding: 12px 24px;
  border-bottom: 1px solid var(--line-border);
  color: var(--line-text-secondary);
  font-size: 13px;
  background: var(--line-bg-soft);
}

.google-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.google-table th {
  color: var(--line-text-secondary);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  background: var(--line-bg-soft);
}

.google-table th,
.google-table td {
  padding: 16px 24px;
  text-align: center;
  border-bottom: 1px solid var(--line-border);
  vertical-align: middle;
}

.col-user {
  width: 32%;
  text-align: left !important;
}

.col-role {
  width: 12%;
}

.col-status {
  width: 12%;
}

.col-actions {
  width: 44%;
  text-align: center;
  white-space: nowrap;
}

.google-table tbody tr {
  transition: background-color 0.15s ease;
}

.google-table tbody tr:hover {
  background-color: var(--line-hover);
}

.google-table tbody tr:last-child td {
  border-bottom: none;
}

/* User Info Cell */
.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--line-border);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--line-primary) 0%, var(--line-primary-hover) 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  font-weight: 600;
  color: var(--line-text);
}

.user-nickname {
  font-size: 13px;
  color: var(--line-text-secondary);
}

/* Role Badge */
.role-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.role-admin {
  background: rgba(26, 115, 232, 0.1);
  color: var(--line-primary);
}

.role-teacher {
  background: color-mix(in srgb, var(--line-warning) 18%, white);
  color: color-mix(in srgb, var(--line-warning) 85%, black);
}

.role-user {
  background: var(--line-bg-soft);
  color: var(--line-text-secondary);
}

/* Status Badge */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-active {
  background: color-mix(in srgb, var(--line-success) 16%, white);
  color: var(--line-success);
}

.status-active .status-dot {
  background: var(--line-success);
}

.status-banned {
  background: color-mix(in srgb, var(--line-error) 14%, white);
  color: var(--line-error);
}

.status-banned .status-dot {
  background: var(--line-error);
}

/* Actions */
.actions {
  display: flex;
  gap: 6px;
  justify-content: center;
  flex-wrap: nowrap;
  white-space: nowrap;
  width: 100%;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  height: auto;
  border-radius: 8px;
  border: none;
  background-color: transparent;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s ease;
  font-family: inherit;
}

.action-btn:hover {
  transform: translateY(-1px);
}

.action-btn.primary {
  color: var(--line-primary);
  background-color: color-mix(in srgb, var(--line-primary) 10%, transparent);
}
.action-btn.primary:hover {
  background-color: color-mix(in srgb, var(--line-primary) 18%, transparent);
  box-shadow: var(--line-shadow-sm);
}

.action-btn.warning {
  color: color-mix(in srgb, var(--line-warning) 85%, black);
  background-color: color-mix(in srgb, var(--line-warning) 18%, white);
}
.action-btn.warning:hover {
  background-color: color-mix(in srgb, var(--line-warning) 26%, white);
  box-shadow: var(--line-shadow-sm);
}

.action-btn.success {
  color: var(--line-success);
  background-color: color-mix(in srgb, var(--line-success) 16%, white);
}
.action-btn.success:hover {
  background-color: color-mix(in srgb, var(--line-success) 24%, white);
  box-shadow: var(--line-shadow-sm);
}

.action-btn.danger {
  color: var(--line-error);
  background-color: color-mix(in srgb, var(--line-error) 14%, white);
}
.action-btn.danger:hover {
  background-color: color-mix(in srgb, var(--line-error) 22%, white);
  box-shadow: var(--line-shadow-sm);
}

/* Pagination */
.pagination {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  border-top: none;
  background: var(--line-bg-soft);
}

.pagination span {
  font-size: 14px;
  color: var(--line-text-secondary);
}

.google-btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid var(--line-border);
  background: var(--line-bg);
  color: var(--line-text);
  transition: all 0.2s ease;
}

.google-btn:hover:not(:disabled) {
  background: var(--line-bg-soft);
  border-color: var(--line-primary);
  color: var(--line-primary);
}

.google-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* States */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  color: var(--line-text-secondary);
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--line-border);
  border-top-color: var(--line-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  color: var(--line-text-secondary);
}

.empty-icon {
  color: var(--line-border);
  margin-bottom: 16px;
}

.empty-state p {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 500;
}

.empty-hint {
  font-size: 14px;
  color: var(--line-text-secondary);
}

/* Responsive */
@media (max-width: 1024px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-box {
    width: 100%;
  }
  
  .actions {
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .admin-container {
    padding: 16px;
  }
  
  .tabs {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  
  .google-table th,
  .google-table td {
    padding: 12px 16px;
  }
  
  .action-btn span {
    display: none;
  }
}
</style>

