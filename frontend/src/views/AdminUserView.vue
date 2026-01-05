<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

interface User {
  id: string
  username: string
  nickname: string
  email: string
  role: string
  status: string
}

const users = ref<User[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)
const currentTab = ref('ALL')

const fetchUsers = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    let url = `/api/admin/users?page=${page.value}&size=${size.value}`
    if (currentTab.value !== 'ALL') {
      url += `&role=${currentTab.value}`
    }
    const response = await axios.get(url, {
      headers: { Authorization: `Bearer ${token}` }
    })
    users.value = response.data.content
    totalElements.value = response.data.totalElements
    totalPages.value = response.data.totalPages
  } catch (error) {
    console.error('Failed to fetch users', error)
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
  if (!confirm('确定要删除该用户吗？')) return
  try {
    const token = localStorage.getItem('token')
    await axios.delete(`/api/admin/users/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    fetchUsers()
  } catch (error) {
    console.error('Failed to delete user', error)
    alert('删除用户失败')
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
    alert('更新状态失败')
  }
}

const toggleRole = async (user: User) => {
  const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
  if (!confirm(`确定要将角色更改为 ${newRole} 吗？`)) return
  try {
    const token = localStorage.getItem('token')
    await axios.put(`/api/admin/users/${user.id}/role`, { role: newRole }, {
      headers: { Authorization: `Bearer ${token}` }
    })
    user.role = newRole
  } catch (error) {
    console.error('Failed to update role', error)
    alert('更新角色失败')
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

onMounted(() => {
  fetchUsers()
})
</script>

<template>
  <div class="admin-container container">
    <div class="header-row">
      <h1>用户管理</h1>
    </div>

    <div class="tabs">
      <button :class="['tab-btn', currentTab === 'ALL' ? 'active' : '']" @click="setTab('ALL')">全部用户</button>
      <button :class="['tab-btn', currentTab === 'TEACHER' ? 'active' : '']" @click="setTab('TEACHER')">教师</button>
      <button :class="['tab-btn', currentTab === 'USER' ? 'active' : '']" @click="setTab('USER')">学生</button>
      <button :class="['tab-btn', currentTab === 'ADMIN' ? 'active' : '']" @click="setTab('ADMIN')">管理员</button>
    </div>

    <div class="google-card table-card">
      <div v-if="loading" class="loading">加载中...</div>
      <table v-else-if="users.length > 0" class="google-table">
        <thead>
          <tr>
            <th>用户名</th>
            <th>昵称</th>
            <th>角色</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.username }}</td>
            <td>{{ user.nickname }}</td>
            <td>
              <span :class="['badge', user.role === 'ADMIN' ? 'badge-admin' : 'badge-user']">
                {{ user.role }}
              </span>
            </td>
            <td>
              <span :class="['badge', user.status === 'ACTIVE' ? 'badge-active' : 'badge-banned']">
                {{ user.status || 'ACTIVE' }}
              </span>
            </td>
            <td class="actions">
              <button class="action-btn" :class="user.status === 'ACTIVE' ? 'warning' : 'success'" @click="toggleStatus(user)">
                {{ user.status === 'ACTIVE' ? '禁用' : '启用' }}
              </button>
              <button class="action-btn primary" @click="toggleRole(user)">
                更改角色
              </button>
              <button class="action-btn danger" @click="deleteUser(user.id)">
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-state">
        <span class="material-icon">people_outline</span>
        <p>未找到用户。</p>
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
  max-width: 1200px;
  margin: 0 auto;
}

.header-row {
  margin-bottom: 24px;
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
  color: #5f6368;
  font-size: 12px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.google-table th,
.google-table td {
  padding: 12px 24px;
  text-align: left;
  border-bottom: 1px solid #dadce0;
  vertical-align: middle;
}

.google-table tbody tr:hover {
  background-color: #f8f9fa;
}

.empty-state {
  padding: 48px;
  text-align: center;
  color: #5f6368;
}

.empty-state .material-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #dadce0;
}

.badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.badge-admin { background-color: #e8f0fe; color: #1a73e8; }
.badge-user { background-color: #f1f3f4; color: #5f6368; }
.badge-active { background-color: #e6f4ea; color: #1e8e3e; }
.badge-banned { background-color: #fce8e6; color: #d93025; }

.pagination {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  border-top: 1px solid #dadce0;
}

.tabs {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #dadce0;
}

.tab-btn {
  background: none;
  border: none;
  padding: 12px 16px;
  font-family: 'Google Sans', Roboto, Arial, sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: #5f6368;
  cursor: pointer;
  position: relative;
}

.tab-btn:hover {
  color: #202124;
  background-color: #f1f3f4;
  border-radius: 4px 4px 0 0;
}

.tab-btn.active {
  color: #1a73e8;
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 3px;
  background-color: #1a73e8;
  border-radius: 3px 3px 0 0;
}

.icon-btn.danger {
  color: #d93025;
}

.actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 0 16px;
  height: 32px;
  border-radius: 16px;
  border: none;
  background-color: transparent;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: background-color 0.2s, box-shadow 0.2s;
  font-family: 'Google Sans', Roboto, Arial, sans-serif;
}

.action-btn:hover {
  background-color: rgba(95, 99, 104, 0.04);
}

.action-btn.primary {
  color: #1a73e8;
  background-color: #e8f0fe;
}
.action-btn.primary:hover {
  background-color: #d2e3fc;
  box-shadow: 0 1px 2px rgba(60, 64, 67, 0.3), 0 1px 3px 1px rgba(60, 64, 67, 0.15);
}

.action-btn.warning {
  color: #e37400;
  background-color: #fef7e0;
}
.action-btn.warning:hover {
  background-color: #feefc3;
  box-shadow: 0 1px 2px rgba(60, 64, 67, 0.3), 0 1px 3px 1px rgba(60, 64, 67, 0.15);
}

.action-btn.success {
  color: #1e8e3e;
  background-color: #e6f4ea;
}
.action-btn.success:hover {
  background-color: #ceead6;
  box-shadow: 0 1px 2px rgba(60, 64, 67, 0.3), 0 1px 3px 1px rgba(60, 64, 67, 0.15);
}

.action-btn.danger {
  color: #d93025;
  background-color: #fce8e6;
}
.action-btn.danger:hover {
  background-color: #fad2cf;
  box-shadow: 0 1px 2px rgba(60, 64, 67, 0.3), 0 1px 3px 1px rgba(60, 64, 67, 0.15);
}

.empty-state {
  padding: 24px;
  text-align: center;
  color: #5f6368;
  font-size: 14px;
}

.empty-state .material-icon {
  font-size: 48px;
  margin-bottom: 8px;
}
</style>
