<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue'
import axios from 'axios'
import { Plus, Edit, Trash2, Eye, Clock, Send, AlertCircle, Check, FileText, Archive, X, Megaphone } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'

const { showToast } = useToast()

interface Announcement {
  id: string
  title: string
  content: string
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
  createdAt: string
  publishedAt: string | null
  author: string
  viewCount: number
}

const announcements = ref<Announcement[]>([])
const loading = ref(false)
const errorMessage = ref('')
const showModal = ref(false)
const editingAnnouncement = ref<Announcement | null>(null)
const currentTab = ref('ALL')

// 确认弹窗
const confirmDialog = reactive({
  show: false,
  title: '',
  message: '',
  confirmText: '确定',
  cancelText: '取消',
  type: 'warning' as 'warning' | 'danger',
  onConfirm: () => {}
})

const showConfirm = (options: {
  title: string
  message: string
  confirmText?: string
  type?: 'warning' | 'danger'
  onConfirm: () => void
}) => {
  confirmDialog.show = true
  confirmDialog.title = options.title
  confirmDialog.message = options.message
  confirmDialog.confirmText = options.confirmText || '确定'
  confirmDialog.type = options.type || 'warning'
  confirmDialog.onConfirm = options.onConfirm
}

const closeConfirm = () => {
  confirmDialog.show = false
}

const handleConfirm = () => {
  confirmDialog.onConfirm()
  closeConfirm()
}

const form = ref({
  title: '',
  content: '',
  priority: 'NORMAL' as 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
})

const priorityOptions = [
  { value: 'LOW', label: '低', class: 'priority-low' },
  { value: 'NORMAL', label: '普通', class: 'priority-normal' },
  { value: 'HIGH', label: '高', class: 'priority-high' },
  { value: 'URGENT', label: '紧急', class: 'priority-urgent' }
]

// 格式化时间
const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  try {
    const date = new Date(dateStr)
    if (isNaN(date.getTime())) return dateStr
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return dateStr
  }
}

const fetchAnnouncements = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const token = localStorage.getItem('token')
    let url = '/api/admin/announcements'
    if (currentTab.value !== 'ALL') {
      url += `?status=${currentTab.value}`
    }
    const response = await axios.get(url, {
      headers: { Authorization: `Bearer ${token}` }
    })
    announcements.value = response.data.content || response.data || []
  } catch (error) {
    console.error('Failed to fetch announcements', error)
    announcements.value = []
    errorMessage.value = '获取公告失败，请检查管理员权限或后端服务状态'
    showToast({ message: errorMessage.value, type: 'error' })
  } finally {
    loading.value = false
  }
}

const filteredAnnouncements = computed(() => {
  if (currentTab.value === 'ALL') return announcements.value
  return announcements.value.filter(a => a.status === currentTab.value)
})

const tabCounts = computed(() => ({
  ALL: announcements.value.length,
  DRAFT: announcements.value.filter(a => a.status === 'DRAFT').length,
  PUBLISHED: announcements.value.filter(a => a.status === 'PUBLISHED').length,
  ARCHIVED: announcements.value.filter(a => a.status === 'ARCHIVED').length
}))

const setTab = (tab: string) => {
  currentTab.value = tab
}

const openCreateModal = () => {
  editingAnnouncement.value = null
  form.value = { title: '', content: '', priority: 'NORMAL' }
  showModal.value = true
}

const openEditModal = (announcement: Announcement) => {
  editingAnnouncement.value = announcement
  form.value = {
    title: announcement.title,
    content: announcement.content,
    priority: announcement.priority
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingAnnouncement.value = null
}

const saveAnnouncement = async (publish: boolean = false) => {
  if (!form.value.title.trim() || !form.value.content.trim()) {
    showToast({ message: '请填写标题和内容', type: 'error' })
    return
  }

  try {
    const token = localStorage.getItem('token')
    const data = {
      ...form.value,
      status: publish ? 'PUBLISHED' : 'DRAFT'
    }

    if (editingAnnouncement.value) {
      await axios.put(`/api/admin/announcements/${editingAnnouncement.value.id}`, data, {
        headers: { Authorization: `Bearer ${token}` }
      })
      showToast({ message: publish ? '公告已更新并发布' : '公告已保存', type: 'success' })
    } else {
      await axios.post('/api/admin/announcements', data, {
        headers: { Authorization: `Bearer ${token}` }
      })
      showToast({ message: publish ? '公告已创建并发布' : '公告已保存为草稿', type: 'success' })
    }
    
    closeModal()
    fetchAnnouncements()
  } catch (error) {
    console.error('Failed to save announcement', error)
    showToast({ message: '保存失败，请稍后重试', type: 'error' })
  }
}

const publishAnnouncement = async (id: string) => {
  const announcement = announcements.value.find(a => a.id === id)
  if (!announcement) return

  showConfirm({
    title: '发布公告',
    message: `确定要发布公告「${announcement.title}」吗？发布后所有用户都可以看到。`,
    confirmText: '发布',
    type: 'warning',
    onConfirm: async () => {
      try {
        const token = localStorage.getItem('token')
        await axios.put(`/api/admin/announcements/${id}/publish`, {}, {
          headers: { Authorization: `Bearer ${token}` }
        })
        showToast({ message: '公告已发布', type: 'success' })
        fetchAnnouncements()
      } catch (error) {
        console.error('Failed to publish announcement', error)
        showToast({ message: '发布失败，请稍后重试', type: 'error' })
      }
    }
  })
}

const archiveAnnouncement = async (id: string) => {
  const announcement = announcements.value.find(a => a.id === id)
  if (!announcement) return

  showConfirm({
    title: '归档公告',
    message: `确定要归档公告「${announcement.title}」吗？归档后用户将不再看到此公告。`,
    confirmText: '归档',
    type: 'warning',
    onConfirm: async () => {
      try {
        const token = localStorage.getItem('token')
        await axios.put(`/api/admin/announcements/${id}/archive`, {}, {
          headers: { Authorization: `Bearer ${token}` }
        })
        showToast({ message: '公告已归档', type: 'success' })
        fetchAnnouncements()
      } catch (error) {
        console.error('Failed to archive announcement', error)
        showToast({ message: '归档失败，请稍后重试', type: 'error' })
      }
    }
  })
}

const deleteAnnouncement = async (id: string) => {
  const announcement = announcements.value.find(a => a.id === id)
  if (!announcement) return

  showConfirm({
    title: '删除公告',
    message: `确定要删除公告「${announcement.title}」吗？此操作不可恢复。`,
    confirmText: '删除',
    type: 'danger',
    onConfirm: async () => {
      try {
        const token = localStorage.getItem('token')
        await axios.delete(`/api/admin/announcements/${id}`, {
          headers: { Authorization: `Bearer ${token}` }
        })
        showToast({ message: '公告已删除', type: 'success' })
        fetchAnnouncements()
      } catch (error) {
        console.error('Failed to delete announcement', error)
        showToast({ message: '删除失败，请稍后重试', type: 'error' })
      }
    }
  })
}

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    'DRAFT': '草稿',
    'PUBLISHED': '已发布',
    'ARCHIVED': '已归档'
  }
  return map[status] || status
}

const getPriorityLabel = (priority: string) => {
  const option = priorityOptions.find(p => p.value === priority)
  return option?.label || priority
}

const getStatusIcon = (status: string) => {
  switch (status) {
    case 'DRAFT': return FileText
    case 'PUBLISHED': return Check
    case 'ARCHIVED': return Archive
    default: return FileText
  }
}

onMounted(() => {
  fetchAnnouncements()
})
</script>

<template>
  <div class="admin-announcements">
    <!-- 确认弹窗 -->
    <Transition name="modal">
      <div v-if="confirmDialog.show" class="confirm-overlay" @click.self="closeConfirm">
        <div class="confirm-dialog">
          <div class="confirm-header">
            <AlertCircle :size="24" :class="confirmDialog.type === 'danger' ? 'text-danger' : 'text-warning'" />
            <h3>{{ confirmDialog.title }}</h3>
          </div>
          <p class="confirm-message">{{ confirmDialog.message }}</p>
          <div class="confirm-actions">
            <button class="btn-secondary" @click="closeConfirm">取消</button>
            <button 
              :class="['btn-primary', confirmDialog.type === 'danger' ? 'btn-danger' : '']" 
              @click="handleConfirm"
            >
              {{ confirmDialog.confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-text">
          <h1>公告管理</h1>
          <p>系统公告发布与管理，向用户推送重要信息</p>
        </div>
      </div>
      <button class="btn-primary" @click="openCreateModal">
        <Plus :size="18" />
        新建公告
      </button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon total">
          <Megaphone :size="20" />
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ tabCounts.ALL }}</span>
          <span class="stat-label">全部公告</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon draft">
          <FileText :size="20" />
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ tabCounts.DRAFT }}</span>
          <span class="stat-label">草稿</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon published">
          <Check :size="20" />
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ tabCounts.PUBLISHED }}</span>
          <span class="stat-label">已发布</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon archived">
          <Archive :size="20" />
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ tabCounts.ARCHIVED }}</span>
          <span class="stat-label">已归档</span>
        </div>
      </div>
    </div>

    <!-- 选项卡 -->
    <div class="tabs-container">
      <div class="tabs">
        <button 
          :class="['tab-btn', currentTab === 'ALL' ? 'active' : '']" 
          @click="setTab('ALL')"
        >
          全部
        </button>
        <button 
          :class="['tab-btn', currentTab === 'DRAFT' ? 'active' : '']" 
          @click="setTab('DRAFT')"
        >
          草稿
        </button>
        <button 
          :class="['tab-btn', currentTab === 'PUBLISHED' ? 'active' : '']" 
          @click="setTab('PUBLISHED')"
        >
          已发布
        </button>
        <button 
          :class="['tab-btn', currentTab === 'ARCHIVED' ? 'active' : '']" 
          @click="setTab('ARCHIVED')"
        >
          已归档
        </button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="errorMessage" class="empty-state">
      <div class="empty-icon">
        <AlertCircle :size="48" />
      </div>
      <h3>加载失败</h3>
      <p>{{ errorMessage }}</p>
      <button class="btn-primary" @click="fetchAnnouncements">重试</button>
    </div>

    <!-- 空状态 -->
    <div v-else-if="filteredAnnouncements.length === 0" class="empty-state">
      <div class="empty-icon">
        <Megaphone :size="48" />
      </div>
      <h3>暂无公告</h3>
      <p>点击"新建公告"按钮创建第一条公告</p>
      <button class="btn-primary" @click="openCreateModal">
        <Plus :size="18" />
        创建第一条公告
      </button>
    </div>

    <!-- 公告列表 -->
    <div v-else class="announcements-list">
      <TransitionGroup name="list">
        <div 
          v-for="announcement in filteredAnnouncements" 
          :key="announcement.id" 
          class="announcement-card"
          :class="'status-' + announcement.status.toLowerCase()"
        >
          <div class="card-left-border" :class="'border-' + announcement.status.toLowerCase()"></div>
          <div class="card-content">
            <div class="card-header">
              <div class="header-left">
                <span :class="['priority-badge', 'priority-' + announcement.priority.toLowerCase()]">
                  {{ getPriorityLabel(announcement.priority) }}
                </span>
                <h3 class="announcement-title">{{ announcement.title }}</h3>
              </div>
              <div class="header-right">
                <span :class="['status-badge', 'status-' + announcement.status.toLowerCase()]">
                  <component :is="getStatusIcon(announcement.status)" :size="12" />
                  {{ getStatusLabel(announcement.status) }}
                </span>
              </div>
            </div>

            <p class="announcement-content">{{ announcement.content }}</p>

            <div class="card-footer">
              <div class="meta-info">
                <span class="meta-item">
                  <Clock :size="14" />
                  {{ formatDateTime(announcement.createdAt) }}
                </span>
                <span v-if="announcement.status === 'PUBLISHED'" class="meta-item">
                  <Eye :size="14" />
                  {{ announcement.viewCount }} 次浏览
                </span>
              </div>
              
              <div class="card-actions">
                <button 
                  v-if="announcement.status === 'DRAFT'" 
                  class="action-btn success"
                  @click="publishAnnouncement(announcement.id)"
                  title="发布"
                >
                  <Send :size="16" />
                  <span>发布</span>
                </button>
                <button 
                  v-if="announcement.status === 'PUBLISHED'" 
                  class="action-btn warning"
                  @click="archiveAnnouncement(announcement.id)"
                  title="归档"
                >
                  <Archive :size="16" />
                  <span>归档</span>
                </button>
                <button 
                  class="action-btn"
                  @click="openEditModal(announcement)"
                  title="编辑"
                >
                  <Edit :size="16" />
                </button>
                <button 
                  class="action-btn danger"
                  @click="deleteAnnouncement(announcement.id)"
                  title="删除"
                >
                  <Trash2 :size="16" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </TransitionGroup>
    </div>

    <!-- 创建/编辑弹窗 -->
    <Transition name="modal">
      <div v-if="showModal" class="modal-backdrop" @click.self="closeModal">
        <div class="modal-content">
          <div class="modal-header">
            <h2>{{ editingAnnouncement ? '编辑公告' : '新建公告' }}</h2>
            <button class="close-btn" @click="closeModal">
              <X :size="20" />
            </button>
          </div>
          
          <div class="modal-body">
            <div class="form-group">
              <label class="field-label">标题</label>
              <input 
                v-model="form.title" 
                type="text" 
                class="form-input" 
                placeholder="输入公告标题"
              />
            </div>

            <div class="form-group">
              <label class="field-label">优先级</label>
              <div class="priority-options">
                <label 
                  v-for="option in priorityOptions" 
                  :key="option.value"
                  :class="['priority-option', option.class, form.priority === option.value ? 'selected' : '']"
                >
                  <input 
                    type="radio" 
                    :value="option.value" 
                    v-model="form.priority"
                  />
                  {{ option.label }}
                </label>
              </div>
            </div>

            <div class="form-group">
              <label class="field-label">内容</label>
              <textarea 
                v-model="form.content" 
                class="form-input content-input" 
                placeholder="输入公告内容..."
                rows="6"
              ></textarea>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn-secondary" @click="closeModal">取消</button>
            <button class="btn-outline" @click="saveAnnouncement(false)">
              <FileText :size="16" />
              保存草稿
            </button>
            <button class="btn-primary" @click="saveAnnouncement(true)">
              <Send :size="16" />
              发布公告
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.admin-announcements {
  padding: 24px 32px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 确认弹窗 */
.confirm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: var(--line-layer-modal-overlay);
  backdrop-filter: blur(2px);
}

.confirm-dialog {
  background: var(--line-card-bg);
  border-radius: 16px;
  padding: 28px;
  width: 100%;
  max-width: 420px;
  box-shadow: var(--line-shadow-xl);
}

.confirm-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.confirm-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--line-text);
  margin: 0;
}

.text-warning { color: var(--line-warning); }
.text-danger { color: var(--line-error); }

.confirm-message {
  color: var(--line-text-secondary);
  line-height: 1.6;
  margin-bottom: 24px;
}

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--line-primary) 14%, white) 0%, color-mix(in srgb, var(--line-primary) 8%, white) 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--line-primary);
}

.header-text h1 {
  font-size: 26px;
  font-weight: 600;
  color: var(--line-text);
  margin: 0 0 4px 0;
}

.header-text p {
  color: var(--line-text-secondary);
  margin: 0;
  font-size: 14px;
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 28px;
}

.stat-card {
  background: var(--line-card-bg);
  border-radius: 14px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--line-shadow-sm);
  border: 1px solid var(--line-border);
  transition: all 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--line-shadow-md);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.total {
  background: linear-gradient(135deg, color-mix(in srgb, var(--line-primary) 14%, white) 0%, color-mix(in srgb, var(--line-primary) 8%, white) 100%);
  color: var(--line-primary);
}

.stat-icon.draft {
  background: linear-gradient(135deg, color-mix(in srgb, var(--line-warning) 18%, white) 0%, color-mix(in srgb, var(--line-warning) 10%, white) 100%);
  color: color-mix(in srgb, var(--line-warning) 85%, black);
}

.stat-icon.published {
  background: linear-gradient(135deg, color-mix(in srgb, var(--line-success) 16%, white) 0%, color-mix(in srgb, var(--line-success) 10%, white) 100%);
  color: var(--line-success);
}

.stat-icon.archived {
  background: linear-gradient(135deg, var(--line-bg-hover) 0%, var(--line-bg-soft) 100%);
  color: var(--line-text-secondary);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--line-text);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--line-text-secondary);
  margin-top: 2px;
}

/* 选项卡 */
.tabs-container {
  background: var(--line-card-bg);
  border-radius: 12px;
  padding: 8px;
  margin-bottom: 24px;
  border: 1px solid var(--line-border);
}

.tabs {
  display: flex;
  gap: 4px;
}

.tab-btn {
  flex: 1;
  padding: 12px 20px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: var(--line-text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: var(--line-text);
  background: var(--line-bg-soft);
}

.tab-btn.active {
  color: var(--line-primary);
  background: color-mix(in srgb, var(--line-primary) 10%, transparent);
}

/* 加载和空状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px;
  color: var(--line-text-secondary);
}

.spinner {
  width: 48px;
  height: 48px;
  border: 3px solid var(--line-border);
  border-top-color: var(--line-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: var(--line-card-bg);
  border-radius: 16px;
  border: 1px solid var(--line-border);
}

.empty-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, var(--line-bg-hover) 0%, var(--line-bg-soft) 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  color: var(--line-text-secondary);
}

.empty-state h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--line-text);
  margin: 0 0 8px 0;
}

.empty-state p {
  color: var(--line-text-secondary);
  margin: 0 0 24px 0;
}

/* 公告列表 */
.announcements-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.announcement-card {
  background: var(--line-card-bg);
  border-radius: 14px;
  display: flex;
  overflow: hidden;
  box-shadow: var(--line-shadow-sm);
  border: 1px solid var(--line-border);
  transition: all 0.2s;
}

.announcement-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--line-shadow-md);
}

.card-left-border {
  width: 4px;
  flex-shrink: 0;
}

.border-published { background: linear-gradient(180deg, var(--line-success), color-mix(in srgb, var(--line-success) 80%, white)); }
.border-draft { background: linear-gradient(180deg, var(--line-primary), var(--line-primary-hover)); }
.border-archived { background: linear-gradient(180deg, #94a3b8, #cbd5e1); }

.card-content {
  flex: 1;
  padding: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.card-header .header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.announcement-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--line-text);
  margin: 0;
}

.priority-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.priority-low { background: var(--line-bg-hover); color: var(--line-text-secondary); }
.priority-normal { background: color-mix(in srgb, var(--line-primary) 12%, white); color: var(--line-primary); }
.priority-high { background: color-mix(in srgb, var(--line-warning) 18%, white); color: color-mix(in srgb, var(--line-warning) 85%, black); }
.priority-urgent { background: color-mix(in srgb, var(--line-error) 14%, white); color: var(--line-error); }

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.status-draft { background: color-mix(in srgb, var(--line-warning) 18%, white); color: color-mix(in srgb, var(--line-warning) 85%, black); }
.status-badge.status-published { background: color-mix(in srgb, var(--line-success) 16%, white); color: var(--line-success); }
.status-badge.status-archived { background: var(--line-bg-hover); color: var(--line-text-secondary); }

.announcement-content {
  color: var(--line-text-secondary);
  line-height: 1.7;
  margin-bottom: 18px;
  font-size: 14px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 18px;
  border-top: none;
}

.meta-info {
  display: flex;
  gap: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--line-text-secondary);
}

.card-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--line-bg-soft);
  color: var(--line-text-secondary);
}

.action-btn:hover {
  background: var(--line-border);
  color: var(--line-text);
}

.action-btn.success { color: var(--line-success); }
.action-btn.success:hover { background: color-mix(in srgb, var(--line-success) 16%, white); }

.action-btn.warning { color: color-mix(in srgb, var(--line-warning) 85%, black); }
.action-btn.warning:hover { background: color-mix(in srgb, var(--line-warning) 18%, white); }

.action-btn.danger { color: var(--line-error); }
.action-btn.danger:hover { background: color-mix(in srgb, var(--line-error) 14%, white); }

/* 按钮样式 */
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: var(--line-primary);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover {
  background: var(--line-primary-hover);
  transform: translateY(-1px);
}

.btn-secondary {
  padding: 10px 20px;
  background: var(--line-bg-soft);
  color: var(--line-text);
  border: 1px solid var(--line-border);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background: var(--line-border);
}

.btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: transparent;
  color: var(--line-primary);
  border: 1px solid var(--line-primary);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-outline:hover {
  background: color-mix(in srgb, var(--line-primary) 10%, transparent);
}

.btn-danger {
  background: var(--line-error);
}

.btn-danger:hover {
  background: color-mix(in srgb, var(--line-error) 86%, black);
}

/* 弹窗样式 */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: var(--line-layer-modal-overlay);
  padding: 24px;
  backdrop-filter: blur(2px);
  animation: fadeIn 0.2s ease-out;
}

.modal-content {
  background: var(--line-card-bg);
  border-radius: 18px;
  width: 100%;
  max-width: 600px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: var(--line-shadow-xl);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
  border-bottom: 1px solid var(--line-border);
}

.modal-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--line-text);
  margin: 0;
}

.close-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: var(--line-bg-soft);
  color: var(--line-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.close-btn:hover {
  background: var(--line-border);
  color: var(--line-text);
}

.modal-body {
  padding: 28px;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 28px;
  border-top: none;
  background: transparent;
}

/* 表单样式 */
.form-group {
  margin-bottom: 24px;
}

.field-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--line-text);
  margin-bottom: 10px;
}

.form-input {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid var(--line-border);
  border-radius: 10px;
  font-size: 14px;
  background: var(--line-card-bg);
  color: var(--line-text);
  transition: all 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: var(--line-primary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--line-primary) 14%, transparent);
}

.content-input {
  resize: vertical;
  min-height: 140px;
  line-height: 1.6;
}

.priority-options {
  display: flex;
  gap: 12px;
}

.priority-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 18px;
  border: 2px solid var(--line-border);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.priority-option input {
  display: none;
}

.priority-option:hover {
  border-color: var(--line-text-secondary);
}

.priority-option.selected {
  border-color: var(--line-primary);
  background: color-mix(in srgb, var(--line-primary) 10%, transparent);
}

.priority-option.priority-low.selected { border-color: #94a3b8; background: var(--line-bg-hover); }
.priority-option.priority-normal.selected { border-color: var(--line-primary); background: color-mix(in srgb, var(--line-primary) 12%, white); }
.priority-option.priority-high.selected { border-color: color-mix(in srgb, var(--line-warning) 85%, black); background: color-mix(in srgb, var(--line-warning) 18%, white); }
.priority-option.priority-urgent.selected { border-color: var(--line-error); background: color-mix(in srgb, var(--line-error) 14%, white); }

/* 动画 */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-content,
.modal-leave-to .modal-content,
.modal-enter-from .confirm-dialog,
.modal-leave-to .confirm-dialog {
  transform: scale(0.95) translateY(-20px);
}

.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* 响应式 */
@media (max-width: 1024px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .admin-announcements {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .stats-row {
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .stat-card {
    padding: 16px;
  }

  .stat-value {
    font-size: 22px;
  }

  .tabs {
    flex-wrap: wrap;
  }

  .tab-btn {
    flex: none;
    padding: 10px 16px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .card-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .card-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .priority-options {
    flex-wrap: wrap;
  }

  .priority-option {
    flex: 1;
    min-width: 80px;
    justify-content: center;
  }
}
</style>

