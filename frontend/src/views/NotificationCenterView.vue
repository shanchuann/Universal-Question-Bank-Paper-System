<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import axios from 'axios'
import { useToast } from '@/composables/useToast'
import { useNotifications } from '@/composables/useNotifications'

interface ConversationItem {
  peerId: string
  peerName: string
  peerRole?: string
  peerAvatarUrl?: string
  lastMessage?: string
  lastTime?: string
  unreadCount: number
}

interface ChatMessage {
  id: string
  senderId?: string
  senderName?: string
  senderRole?: string
  receiverId: string
  type: string
  content: string
  createdAt: string
  isRead: boolean
}

interface RecipientItem {
  id: string
  username?: string
  nickname?: string
  role?: string
  avatarUrl?: string
}

interface SidebarItem {
  peerId: string
  peerName: string
  peerRole?: string
  peerAvatarUrl?: string
  lastMessage?: string
  lastTime?: string
  unreadCount: number
  hasHistory: boolean
}

const { showToast } = useToast()
const { refreshUnreadCount } = useNotifications()

const loadingConversations = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)

const conversations = ref<ConversationItem[]>([])
const messages = ref<ChatMessage[]>([])
const recipients = ref<RecipientItem[]>([])

const activePeerId = ref('')
const messageContent = ref('')
const chatScrollRef = ref<HTMLElement | null>(null)

const getAuthHeaders = () => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const sidebarItems = computed<SidebarItem[]>(() => {
  const known = new Set<string>()
  const list: SidebarItem[] = []

  for (const item of conversations.value) {
    known.add(item.peerId)
    list.push({ ...item, hasHistory: true })
  }

  for (const user of recipients.value) {
    if (known.has(user.id)) continue
    list.push({
      peerId: user.id,
      peerName: user.nickname || user.username || '未命名用户',
      peerRole: user.role,
      peerAvatarUrl: user.avatarUrl,
      lastMessage: '',
      lastTime: '',
      unreadCount: 0,
      hasHistory: false
    })
  }

  return list.sort((a, b) => {
    if (a.hasHistory !== b.hasHistory) {
      return a.hasHistory ? -1 : 1
    }
    const aTime = a.lastTime ? new Date(a.lastTime).getTime() : 0
    const bTime = b.lastTime ? new Date(b.lastTime).getTime() : 0
    if (aTime !== bTime) {
      return bTime - aTime
    }
    return a.peerName.localeCompare(b.peerName, 'zh-CN')
  })
})

const activeConversation = computed(() => {
  return sidebarItems.value.find(item => item.peerId === activePeerId.value)
})

const formatTime = (value?: string) => {
  if (!value) return '--'
  const date = new Date(value)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

const isMyMessage = (item: ChatMessage) => {
  const token = localStorage.getItem('token') || ''
  const myUserId = token.startsWith('dummy-jwt-token-') ? token.replace('dummy-jwt-token-', '') : ''
  return item.senderId === myUserId
}

const ensureActivePeer = () => {
  if (activePeerId.value && sidebarItems.value.some(item => item.peerId === activePeerId.value)) {
    return
  }
  activePeerId.value = sidebarItems.value[0]?.peerId || ''
}

const scrollToBottom = async () => {
  await nextTick()
  if (chatScrollRef.value) {
    chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
  }
}

const fetchRecipients = async () => {
  try {
    const response = await axios.get('/api/notifications/recipients', {
      headers: getAuthHeaders()
    })
    recipients.value = response.data || []
  } catch (_error) {
    recipients.value = []
  }
}

const fetchConversations = async () => {
  loadingConversations.value = true
  try {
    const response = await axios.get('/api/notifications/conversations?limit=80', {
      headers: getAuthHeaders()
    })
    conversations.value = response.data || []
    ensureActivePeer()
  } catch (_error) {
    conversations.value = []
    showToast({ message: '加载会话失败', type: 'error' })
  } finally {
    loadingConversations.value = false
  }
}

const fetchMessages = async (peerId: string) => {
  if (!peerId) {
    messages.value = []
    return
  }

  loadingMessages.value = true
  try {
    const response = await axios.get(`/api/notifications/chat/${peerId}?limit=300`, {
      headers: getAuthHeaders()
    })
    messages.value = response.data || []
    await markConversationRead(peerId)
    await scrollToBottom()
  } catch (error: any) {
    messages.value = []
    const backendMessage = error?.response?.data?.error
    showToast({ message: backendMessage || '加载聊天记录失败', type: 'error' })
  } finally {
    loadingMessages.value = false
  }
}

const markConversationRead = async (peerId: string) => {
  if (!peerId) return
  try {
    await axios.post(`/api/notifications/conversation/${peerId}/read`, {}, {
      headers: getAuthHeaders()
    })

    const target = conversations.value.find(item => item.peerId === peerId)
    if (target) {
      target.unreadCount = 0
    }
    refreshUnreadCount()
  } catch (_error) {
    // ignore read state sync failure
  }
}

const openConversation = async (peerId: string) => {
  if (!peerId) return
  activePeerId.value = peerId
  await fetchMessages(peerId)
}

const sendMessage = async () => {
  const peerId = activePeerId.value
  if (!peerId) {
    showToast({ message: '请先在左侧选择联系人', type: 'warning' })
    return
  }

  if (!messageContent.value.trim()) {
    showToast({ message: '请输入消息内容', type: 'warning' })
    return
  }

  sending.value = true
  try {
    await axios.post('/api/notifications/private', {
      receiverId: peerId,
      title: '普通消息',
      content: messageContent.value,
      type: 'MESSAGE'
    }, {
      headers: getAuthHeaders()
    })

    messageContent.value = ''
    await fetchConversations()
    await fetchMessages(peerId)
    refreshUnreadCount()
  } catch (error: any) {
    const backendMessage = error?.response?.data?.error
    showToast({ message: backendMessage || '发送失败', type: 'error' })
  } finally {
    sending.value = false
  }
}

onMounted(async () => {
  await Promise.all([fetchRecipients(), fetchConversations()])
  ensureActivePeer()
  if (activePeerId.value) {
    await fetchMessages(activePeerId.value)
  }
  refreshUnreadCount()
})
</script>

<template>
  <div class="message-center-view">
    <div class="page-header line-card">
      <div>
        <h1>消息中心</h1>
        <p>系统消息仅限管理员公告，其他均为普通消息（保留3天）</p>
      </div>
      <div class="header-actions">
        <button class="line-btn" @click="fetchConversations">刷新会话</button>
      </div>
    </div>

    <div class="wechat-layout line-card">
      <aside class="conversation-sidebar">
        <div class="sidebar-title">
          <span>会话与联系人</span>
        </div>
        <div v-if="loadingConversations && sidebarItems.length === 0" class="loading">加载中...</div>
        <div v-else-if="sidebarItems.length === 0" class="empty">暂无可聊天用户</div>
        <div v-else class="conversation-list">
          <button
            v-for="item in sidebarItems"
            :key="item.peerId"
            class="conversation-item"
            :class="{ active: item.peerId === activePeerId }"
            @click="openConversation(item.peerId)"
          >
            <div class="avatar">
              <img v-if="item.peerAvatarUrl" :src="item.peerAvatarUrl" alt="avatar" />
              <span v-else>{{ (item.peerName || 'U').charAt(0).toUpperCase() }}</span>
            </div>
            <div class="content">
              <div class="top-row">
                <span class="name">{{ item.peerName }}</span>
                <span class="time">{{ formatTime(item.lastTime) }}</span>
              </div>
              <div class="bottom-row">
                <span class="last">{{ item.lastMessage || (item.hasHistory ? '开始聊天吧' : '可直接发起聊天') }}</span>
                <span v-if="item.unreadCount > 0" class="badge">{{ item.unreadCount > 99 ? '99+' : item.unreadCount }}</span>
              </div>
            </div>
          </button>
        </div>
      </aside>

      <section class="chat-main">
        <div class="chat-head">
          <div>
            <div class="title">{{ activeConversation?.peerName || '请选择左侧联系人' }}</div>
            <div class="subtitle">左侧选人即可开始聊天</div>
          </div>
        </div>

        <div ref="chatScrollRef" class="chat-body">
          <div v-if="!activePeerId" class="empty">请先从左侧选择联系人</div>
          <div v-else-if="loadingMessages" class="loading">加载消息中...</div>
          <div v-else-if="messages.length === 0" class="empty">暂无消息，发送第一条开始沟通</div>

          <div v-else class="message-stream">
            <div
              v-for="item in messages"
              :key="item.id"
              class="bubble-row"
              :class="{ mine: isMyMessage(item) }"
            >
              <div class="bubble">
                <p>{{ item.content }}</p>
                <span class="meta">{{ formatTime(item.createdAt) }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-composer">
          <textarea
            v-model="messageContent"
            class="line-input"
            rows="4"
            placeholder="输入普通消息内容"
          ></textarea>
          <button class="line-btn primary-btn send-btn" :disabled="sending" @click="sendMessage">
            {{ sending ? '发送中...' : '发送消息' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.message-center-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-radius: 16px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
}

.page-header p {
  margin: 4px 0 0;
  color: var(--line-text-secondary);
}

.wechat-layout {
  display: grid;
  grid-template-columns: 340px 1fr;
  min-height: 640px;
  padding: 0;
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid var(--line-border);
  background: var(--line-bg);
}

.conversation-sidebar {
  border-right: 1px solid var(--line-border);
  background: linear-gradient(180deg, var(--line-bg) 0%, var(--line-bg-soft) 100%);
  display: flex;
  flex-direction: column;
}

.sidebar-title {
  padding: 16px;
  border-bottom: 1px solid var(--line-border);
  font-weight: 700;
}

.conversation-list {
  overflow: auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.conversation-item {
  width: 100%;
  border: 1px solid var(--line-border);
  border-radius: 14px;
  background: var(--line-bg);
  padding: 12px;
  text-align: left;
  display: flex;
  gap: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.conversation-item:hover {
  border-color: var(--line-text-secondary);
  background: var(--line-bg-hover);
}

.conversation-item.active {
  border-color: var(--line-primary);
  background: color-mix(in srgb, var(--line-primary) 8%, white);
  box-shadow: var(--line-shadow-sm);
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--line-primary) 14%, white);
  color: var(--line-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.content {
  min-width: 0;
  flex: 1;
}

.top-row,
.bottom-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.name {
  font-weight: 600;
  color: var(--line-text);
}

.time {
  font-size: 12px;
  color: var(--line-text-secondary);
}

.last {
  font-size: 13px;
  color: var(--line-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 196px;
}

.badge {
  min-width: 18px;
  height: 18px;
  border-radius: 999px;
  background: var(--line-error);
  color: white;
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.chat-main {
  display: grid;
  grid-template-rows: auto 1fr auto;
  background: var(--line-bg-soft);
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--line-border);
  background: var(--line-bg);
}

.chat-head .title {
  font-size: 17px;
  font-weight: 700;
}

.subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: var(--line-text-secondary);
}

.chat-body {
  padding: 14px;
  overflow: auto;
}

.message-stream {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bubble-row {
  display: flex;
  justify-content: flex-start;
}

.bubble-row.mine {
  justify-content: flex-end;
}

.bubble {
  max-width: 62%;
  border-radius: 14px;
  background: var(--line-bg);
  border: 1px solid var(--line-border);
  padding: 10px 12px;
}

.bubble-row.mine .bubble {
  background: var(--line-primary);
  border-color: var(--line-primary);
  color: white;
}

.bubble p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.45;
}

.meta {
  display: inline-block;
  margin-top: 6px;
  font-size: 11px;
  color: var(--line-text-secondary);
}

.bubble-row.mine .meta {
  color: rgba(255, 255, 255, 0.75);
}

.chat-composer {
  border-top: none;
  padding: 12px 14px;
  background: var(--line-bg);
}

.send-btn {
  margin-top: 10px;
  width: 100%;
  justify-content: center;
}

.loading,
.empty {
  color: var(--line-text-secondary);
  padding: 14px;
}

@media (max-width: 1024px) {
  .wechat-layout {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .conversation-sidebar {
    border-right: none;
    border-bottom: 1px solid var(--line-border);
    max-height: 320px;
  }

  .chat-main {
    min-height: 520px;
  }

  .bubble {
    max-width: 82%;
  }
}
</style>

