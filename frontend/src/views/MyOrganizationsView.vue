<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface Organization {
  id: string
  name: string
  code: string
  type: string
  status: string
  inviteCode?: string
  memberRole?: string
  memberCount?: number
  joinedAt?: string
  parentName?: string
  parentType?: string
  schoolName?: string
}

const myOrganizations = ref<Organization[]>([])
const loading = ref(false)
const joinCode = ref('')
const showJoinModal = ref(false)
const joinError = ref('')
const joinSuccess = ref('')

onMounted(() => {
  fetchMyOrganizations()
})

async function fetchMyOrganizations() {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await fetch('/api/my-organizations', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    myOrganizations.value = await response.json()
  } catch (error) {
    console.error('Failed to fetch organizations:', error)
  } finally {
    loading.value = false
  }
}

async function joinOrganization() {
  joinError.value = ''
  joinSuccess.value = ''
  
  if (!joinCode.value.trim()) {
    joinError.value = '请输入班级识别码'
    return
  }

  try {
    const token = localStorage.getItem('token')
    const response = await fetch('/api/organizations/join', {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ inviteCode: joinCode.value.trim() })
    })

    if (response.ok) {
      joinSuccess.value = '成功加入班级！'
      joinCode.value = ''
      fetchMyOrganizations()
      setTimeout(() => {
        showJoinModal.value = false
        joinSuccess.value = ''
      }, 1500)
    } else {
      const data = await response.json()
      joinError.value = data.message || '加入失败，请检查识别码是否正确'
    }
  } catch (error) {
    joinError.value = '网络错误，请重试'
  }
}

async function leaveOrganization(orgId: string) {
  if (!confirm('确定要退出该班级吗？')) return

  try {
    const token = localStorage.getItem('token')
    await fetch(`/api/my-organizations/${orgId}/leave`, { 
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    fetchMyOrganizations()
  } catch (error) {
    console.error('Failed to leave organization:', error)
  }
}

const orgTypeLabels: Record<string, string> = {
  SCHOOL: '学校',
  DEPARTMENT: '学院',
  CLASS: '班级'
}

function formatDate(dateStr?: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}
</script>

<template>
  <div class="my-organizations-view">
    <div class="page-header">
      <h1>我的班级</h1>
      <p class="subtitle">管理您加入的班级组织</p>
    </div>

    <div class="toolbar">
      <button class="google-btn primary-btn" @click="showJoinModal = true">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
        加入班级
      </button>
    </div>

    <div class="content-card">
      <div v-if="loading" class="loading">加载中...</div>

      <div v-else-if="myOrganizations.length === 0" class="empty-state">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#9aa0a6" stroke-width="1">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
          <circle cx="9" cy="7" r="4"></circle>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
          <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
        </svg>
        <p>您还没有加入任何班级</p>
        <p class="hint">向教师获取班级识别码，点击上方"加入班级"按钮加入</p>
      </div>

      <div v-else class="org-list">
        <div v-for="org in myOrganizations" :key="org.id" class="org-card">
          <div class="org-icon" :class="org.type?.toLowerCase()">
            <svg v-if="org.type === 'CLASS'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
              <circle cx="9" cy="7" r="4"></circle>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
            </svg>
            <svg v-else-if="org.type === 'DEPARTMENT'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
              <polyline points="9 22 9 12 15 12 15 22"></polyline>
            </svg>
            <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="4" y="2" width="16" height="20" rx="2" ry="2"></rect>
              <line x1="12" y1="18" x2="12.01" y2="18"></line>
            </svg>
          </div>
          <div class="org-info">
            <h3>{{ org.name }}</h3>
            <div class="org-meta">
              <span class="org-type-badge" :class="org.type?.toLowerCase()">{{ orgTypeLabels[org.type] || org.type }}</span>
              <span v-if="org.schoolName" class="org-school">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                </svg>
                {{ org.schoolName }}
              </span>
              <span v-if="org.parentName && org.parentType === 'DEPARTMENT'" class="org-department">
                {{ org.parentName }}
              </span>
            </div>
            <div class="org-details">
              <span v-if="org.memberCount !== undefined" class="detail-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                  <circle cx="9" cy="7" r="4"></circle>
                </svg>
                {{ org.memberCount }} 名成员
              </span>
              <span v-if="org.joinedAt" class="detail-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                  <line x1="16" y1="2" x2="16" y2="6"></line>
                  <line x1="8" y1="2" x2="8" y2="6"></line>
                  <line x1="3" y1="10" x2="21" y2="10"></line>
                </svg>
                {{ formatDate(org.joinedAt) }} 加入
              </span>
              <span v-if="org.memberRole" class="role-badge" :class="org.memberRole?.toLowerCase()">
                {{ org.memberRole === 'MANAGER' ? '管理员' : org.memberRole === 'OWNER' ? '创建者' : '成员' }}
              </span>
            </div>
          </div>
          <div class="org-actions">
            <button class="icon-btn danger" @click="leaveOrganization(org.id)" title="退出班级">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                <polyline points="16 17 21 12 16 7"></polyline>
                <line x1="21" y1="12" x2="9" y2="12"></line>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Join Modal -->
    <div v-if="showJoinModal" class="modal-overlay" @click.self="showJoinModal = false">
      <div class="modal-content">
        <h2>加入班级</h2>
        <p class="modal-hint">请输入教师提供的班级识别码</p>

        <div class="form-group">
          <label>班级识别码</label>
          <input 
            v-model="joinCode" 
            type="text" 
            class="google-input" 
            placeholder="例如：ABC123"
            @keyup.enter="joinOrganization"
          />
        </div>

        <div v-if="joinError" class="error-message">{{ joinError }}</div>
        <div v-if="joinSuccess" class="success-message">{{ joinSuccess }}</div>

        <div class="form-actions">
          <button type="button" class="google-btn text-btn" @click="showJoinModal = false">取消</button>
          <button type="button" class="google-btn primary-btn" @click="joinOrganization">加入</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.my-organizations-view {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 400;
  color: #202124;
  margin: 0 0 8px 0;
}

.subtitle {
  color: #5f6368;
  margin: 0;
}

.toolbar {
  margin-bottom: 24px;
}

.content-card {
  background: #fff;
  border: 1px solid #dadce0;
  border-radius: 8px;
  padding: 24px;
}

.loading, .empty-state {
  text-align: center;
  padding: 48px;
  color: #5f6368;
}

.empty-state svg {
  margin-bottom: 16px;
}

.empty-state .hint {
  font-size: 13px;
  margin-top: 8px;
}

.org-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.org-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid #dadce0;
  border-radius: 8px;
  transition: box-shadow 0.2s;
}

.org-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.org-icon {
  width: 48px;
  height: 48px;
  background: #e8f0fe;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1a73e8;
  flex-shrink: 0;
}

.org-icon.class {
  background: #e8f0fe;
  color: #1a73e8;
}

.org-icon.department {
  background: #fef7e0;
  color: #f9ab00;
}

.org-icon.school {
  background: #e6f4ea;
  color: #1e8e3e;
}

.org-info {
  flex: 1;
  min-width: 0;
}

.org-info h3 {
  margin: 0 0 6px 0;
  font-size: 16px;
  font-weight: 500;
  color: #202124;
}

.org-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #5f6368;
  margin-bottom: 8px;
}

.org-type-badge {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.org-type-badge.class {
  background: #e8f0fe;
  color: #1a73e8;
}

.org-type-badge.department {
  background: #fef7e0;
  color: #e37400;
}

.org-type-badge.school {
  background: #e6f4ea;
  color: #1e8e3e;
}

.org-school, .org-department {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.org-details {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: #80868b;
}

.detail-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.role-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
}

.role-badge.member {
  background: #f1f3f4;
  color: #5f6368;
}

.role-badge.manager {
  background: #fef7e0;
  color: #e37400;
}

.role-badge.owner {
  background: #e6f4ea;
  color: #1e8e3e;
}

.org-type {
  background: #f1f3f4;
  padding: 2px 8px;
  border-radius: 12px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5f6368;
  transition: background 0.2s;
}

.icon-btn:hover {
  background: rgba(0,0,0,0.08);
}

.icon-btn.danger:hover {
  background: #fce8e6;
  color: #d93025;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  width: 100%;
  max-width: 400px;
}

.modal-content h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 500;
}

.modal-hint {
  color: #5f6368;
  font-size: 14px;
  margin: 0 0 24px 0;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #5f6368;
  font-weight: 500;
}

.google-input {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  font-size: 16px;
  box-sizing: border-box;
  text-align: center;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.google-input:focus {
  outline: none;
  border-color: #1a73e8;
  box-shadow: 0 0 0 2px rgba(26,115,232,0.2);
}

.error-message {
  background: #fce8e6;
  color: #d93025;
  padding: 12px;
  border-radius: 4px;
  font-size: 14px;
  margin-bottom: 16px;
}

.success-message {
  background: #e6f4ea;
  color: #1e8e3e;
  padding: 12px;
  border-radius: 4px;
  font-size: 14px;
  margin-bottom: 16px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.google-btn {
  padding: 8px 24px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.primary-btn {
  background: #1a73e8;
  color: #fff;
}

.primary-btn:hover {
  background: #1557b0;
}

.text-btn {
  background: transparent;
  color: #1a73e8;
}

.text-btn:hover {
  background: #e8f0fe;
}
</style>
