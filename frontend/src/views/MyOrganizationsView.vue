<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useConfirm } from '@/composables/useConfirm'

const { confirm } = useConfirm()

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
  const confirmed = await confirm({
    title: '退出班级',
    message: '确定要退出该班级吗？退出后您将无法查看该班级的考试和消息。',
    type: 'warning',
    confirmText: '退出',
    cancelText: '取消'
  })
  if (!confirmed) return

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
      <h1 class="page-title">我的班级</h1>
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
  padding: 32px;
  max-width: 900px;
  margin: 0 auto;
  animation: fadeIn 0.5s ease-out;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  color: var(--line-text-primary);
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.subtitle {
  color: var(--line-text-secondary);
  font-size: 14px;
}

.toolbar {
  margin-bottom: 32px;
}

.content-card {
  background: var(--line-card-bg);
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-lg);
  padding: 32px;
  box-shadow: var(--line-shadow-sm);
}

.loading, .empty-state {
  text-align: center;
  padding: 64px;
  color: var(--line-text-secondary);
}

.empty-state svg {
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state .hint {
  font-size: 13px;
  margin-top: 8px;
}

.org-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.org-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-lg);
  background: var(--line-card-bg);
  transition: all 0.2s;
}

.org-card:hover {
  box-shadow: var(--line-shadow-md);
  border-color: var(--line-primary-30);
  transform: translateY(-1px);
}

.org-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--line-radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid rgba(0,0,0,0.05);
}

.org-icon.class {
  background: var(--line-primary-10);
  color: var(--line-primary);
}

.org-icon.department {
  background: #fffbeb;
  color: #b45309;
}

.org-icon.school {
  background: #ecfdf5;
  color: #059669;
}

.org-info {
  flex: 1;
  min-width: 0;
}

.org-info h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--line-text-primary);
}

.org-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--line-text-secondary);
  margin-bottom: 8px;
}

.org-type-badge {
  padding: 2px 10px;
  border-radius: var(--line-radius-full);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.org-type-badge.class {
  background: var(--line-primary-10);
  color: var(--line-primary);
}

.org-type-badge.department {
  background: #fffbeb;
  color: #b45309;
}

.org-type-badge.school {
  background: #ecfdf5;
  color: #059669;
}

.org-school, .org-department {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.org-details {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: var(--line-text-secondary);
}

.detail-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.role-badge {
  padding: 2px 8px;
  border-radius: var(--line-radius-md);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.role-badge.member {
  background: var(--line-bg-soft);
  color: var(--line-text-secondary);
}

.role-badge.manager {
  background: #fffbeb;
  color: #b45309;
}

.role-badge.owner {
  background: #ecfdf5;
  color: #059669;
}

.org-type {
  background: var(--line-bg-soft);
  padding: 2px 8px;
  border-radius: var(--line-radius-md);
}

.icon-btn {
  width: 40px;
  height: 40px;
  border: 1px solid transparent;
  background: transparent;
  border-radius: var(--line-radius-full);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--line-text-secondary);
  transition: all 0.2s;
}

.icon-btn:hover {
  background: var(--line-bg-soft);
  color: var(--line-text-primary);
  border-color: var(--line-border);
}

.icon-btn.danger:hover {
  background: #fef2f2;
  color: #dc2626;
  border-color: #fee2e2;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease-out;
}

.modal-content {
  background: var(--line-card-bg);
  border-radius: var(--line-radius-lg);
  padding: 32px;
  width: 100%;
  max-width: 450px;
  border: 1px solid var(--line-border);
  box-shadow: var(--line-shadow-xl);
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-content h2 {
  margin: 0 0 8px 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--line-text-primary);
  letter-spacing: -0.5px;
}

.modal-hint {
  color: var(--line-text-secondary);
  font-size: 14px;
  margin: 0 0 24px 0;
  line-height: 1.5;
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--line-text-primary);
  font-weight: 500;
}

.google-input {
  width: 100%;
  height: 56px;
  padding: 0 16px;
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-md);
  font-size: 18px;
  box-sizing: border-box;
  text-align: center;
  letter-spacing: 4px;
  text-transform: uppercase;
  background: var(--line-bg-soft);
  color: var(--line-text-primary);
  transition: all 0.2s;
  font-weight: 600;
}

.google-input:focus {
  outline: none;
  border-color: var(--line-primary);
  background: var(--line-card-bg);
  box-shadow: 0 0 0 2px var(--line-primary-10);
}

.error-message {
  background: #fef2f2;
  color: #dc2626;
  padding: 12px;
  border-radius: var(--line-radius-md);
  font-size: 14px;
  margin-bottom: 24px;
  border: 1px solid #fee2e2;
  text-align: center;
}

.success-message {
  background: #ecfdf5;
  color: #059669;
  padding: 12px;
  border-radius: var(--line-radius-md);
  font-size: 14px;
  margin-bottom: 24px;
  border: 1px solid #d1fae5;
  text-align: center;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}

.google-btn {
  padding: 10px 24px;
  border-radius: var(--line-radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-family: inherit;
  transition: all 0.2s;
}

.primary-btn {
  background: var(--line-primary);
  color: #fff;
  box-shadow: 0 2px 4px rgba(14, 165, 233, 0.2);
}

.primary-btn:hover {
  background: var(--line-primary-hover);
  transform: translateY(-1px);
}

.text-btn {
  background: transparent;
  color: var(--line-text-secondary);
}

.text-btn:hover {
  background: var(--line-bg-soft);
  color: var(--line-primary);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
