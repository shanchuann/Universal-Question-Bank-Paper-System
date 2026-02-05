<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { X, Megaphone, ChevronLeft, ChevronRight, Clock, Eye, AlertCircle, Info, AlertTriangle } from 'lucide-vue-next'
import axios from 'axios'

interface Announcement {
  id: string
  title: string
  content: string
  priority: 'HIGH' | 'NORMAL' | 'LOW'
  publishedAt: string
  viewCount: number
}

const visible = ref(false)
const announcements = ref<Announcement[]>([])
const currentIndex = ref(0)
const loading = ref(false)

// 存储已读公告ID的 localStorage key
const READ_ANNOUNCEMENTS_KEY = 'read_announcements'

const currentAnnouncement = computed(() => {
  return announcements.value[currentIndex.value] || null
})

const hasMultiple = computed(() => announcements.value.length > 1)

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getPriorityIcon = (priority: string) => {
  switch (priority) {
    case 'HIGH':
      return AlertCircle
    case 'LOW':
      return Info
    default:
      return Megaphone
  }
}

const getPriorityClass = (priority: string) => {
  switch (priority) {
    case 'HIGH':
      return 'priority-high'
    case 'LOW':
      return 'priority-low'
    default:
      return 'priority-normal'
  }
}

const getPriorityLabel = (priority: string) => {
  switch (priority) {
    case 'HIGH':
      return '重要'
    case 'LOW':
      return '普通'
    default:
      return '一般'
  }
}

const prevAnnouncement = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
  }
}

const nextAnnouncement = () => {
  if (currentIndex.value < announcements.value.length - 1) {
    currentIndex.value++
  }
}

const closeModal = () => {
  // 记录所有公告为已读
  const readIds = JSON.parse(localStorage.getItem(READ_ANNOUNCEMENTS_KEY) || '[]')
  const newReadIds = [...new Set([...readIds, ...announcements.value.map(a => a.id)])]
  localStorage.setItem(READ_ANNOUNCEMENTS_KEY, JSON.stringify(newReadIds))
  
  visible.value = false
}

const fetchAnnouncements = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    if (!token) return
    
    const response = await axios.get('/api/announcements', {
      headers: { Authorization: `Bearer ${token}` }
    })
    
    const allAnnouncements = response.data || []
    
    // 过滤出未读的公告
    const readIds = JSON.parse(localStorage.getItem(READ_ANNOUNCEMENTS_KEY) || '[]')
    const unreadAnnouncements = allAnnouncements.filter(
      (a: Announcement) => !readIds.includes(a.id)
    )
    
    if (unreadAnnouncements.length > 0) {
      announcements.value = unreadAnnouncements
      visible.value = true
    }
  } catch (error) {
    console.error('Failed to fetch announcements', error)
  } finally {
    loading.value = false
  }
}

// 提供一个方法供外部调用来检查并显示公告
const checkAnnouncements = () => {
  fetchAnnouncements()
}

defineExpose({ checkAnnouncements })
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="announcement-overlay" @click.self="closeModal">
      <div class="announcement-modal">
        <!-- 头部 -->
        <div class="modal-header">
          <div class="header-title">
            <Megaphone class="title-icon" :size="24" />
            <span>系统公告</span>
            <span v-if="hasMultiple" class="announcement-count">
              {{ currentIndex + 1 }} / {{ announcements.length }}
            </span>
          </div>
          <button class="close-btn" @click="closeModal">
            <X :size="20" />
          </button>
        </div>

        <!-- 内容 -->
        <div v-if="currentAnnouncement" class="modal-content">
          <div class="announcement-header">
            <h2 class="announcement-title">{{ currentAnnouncement.title }}</h2>
            <span :class="['priority-badge', getPriorityClass(currentAnnouncement.priority)]">
              <component :is="getPriorityIcon(currentAnnouncement.priority)" :size="14" />
              {{ getPriorityLabel(currentAnnouncement.priority) }}
            </span>
          </div>
          
          <div class="announcement-meta">
            <span class="meta-item">
              <Clock :size="14" />
              {{ formatDate(currentAnnouncement.publishedAt) }}
            </span>
            <span class="meta-item">
              <Eye :size="14" />
              {{ currentAnnouncement.viewCount }} 次浏览
            </span>
          </div>
          
          <div class="announcement-body" v-html="currentAnnouncement.content"></div>
        </div>

        <!-- 底部导航 -->
        <div class="modal-footer">
          <div class="nav-buttons" v-if="hasMultiple">
            <button 
              class="nav-btn" 
              :disabled="currentIndex === 0"
              @click="prevAnnouncement"
            >
              <ChevronLeft :size="18" />
              上一条
            </button>
            <button 
              class="nav-btn" 
              :disabled="currentIndex === announcements.length - 1"
              @click="nextAnnouncement"
            >
              下一条
              <ChevronRight :size="18" />
            </button>
          </div>
          <button class="confirm-btn" @click="closeModal">
            我知道了
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.announcement-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.announcement-modal {
  background: var(--line-bg);
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  animation: modalSlideIn 0.3s ease-out;
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--line-border);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: var(--line-text);
}

.title-icon {
  color: var(--line-primary);
}

.announcement-count {
  font-size: 14px;
  font-weight: normal;
  color: var(--line-text-secondary);
  margin-left: 8px;
}

.close-btn {
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
  color: var(--line-text-secondary);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  background: var(--line-bg-soft);
  color: var(--line-text);
}

.modal-content {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.announcement-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.announcement-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--line-text);
  margin: 0;
  line-height: 1.4;
}

.priority-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.priority-high {
  background: #fef2f2;
  color: #dc2626;
}

.priority-normal {
  background: #fef3c7;
  color: #d97706;
}

.priority-low {
  background: #ecfdf5;
  color: #059669;
}

.announcement-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--line-text-secondary);
}

.announcement-body {
  font-size: 15px;
  line-height: 1.8;
  color: var(--line-text);
}

.announcement-body :deep(p) {
  margin-bottom: 12px;
}

.announcement-body :deep(a) {
  color: var(--line-primary);
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-top: 1px solid var(--line-border);
  background: var(--line-bg-soft);
  border-radius: 0 0 12px 12px;
}

.nav-buttons {
  display: flex;
  gap: 8px;
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border: 1px solid var(--line-border);
  background: var(--line-bg);
  color: var(--line-text-secondary);
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.nav-btn:hover:not(:disabled) {
  border-color: var(--line-primary);
  color: var(--line-primary);
}

.nav-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.confirm-btn {
  padding: 10px 24px;
  background: var(--line-primary);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-left: auto;
}

.confirm-btn:hover {
  background: #1557b0;
}
</style>
