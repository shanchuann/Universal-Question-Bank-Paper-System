<script setup lang="ts">
import { ArrowUp, X } from 'lucide-vue-next'
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { useToast } from '@/composables/useToast'

const props = defineProps<{
  mode: 'teacher' | 'student'
}>()

interface ChatItem {
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp: string
}

interface ChatSession {
  id: string
  title: string
  createdAt: string
  updatedAt: string
  messages: ChatItem[]
  contextText: string
}

const { showToast } = useToast()
const route = useRoute()

const expanded = ref(false)
// iconDragging: when user drags the floating icon itself
const iconDragging = ref(false)
// draggingQuestion: when a question is being dragged from the practice page onto the AI panel
const draggingQuestion = ref(false)
let dragEnterCounter = 0
const moved = ref(false)
const showHistoryMenu = ref(false)
const renamingTitle = ref(false)
// const editingTitle = ref('')
const panelDragging = ref(false)
const panelDragOffsetX = ref(0)
const panelDragOffsetY = ref(0)
const panelMovedByUser = ref(false)
const sending = ref(false)
const question = ref('')
const contextText = ref('')
const messages = ref<ChatItem[]>([])
const sessions = ref<ChatSession[]>([])
const currentSessionId = ref('')
const streamRef = ref<HTMLElement | null>(null)
const expireNotice = ref('')

const MESSAGE_TTL_MS = 3 * 24 * 60 * 60 * 1000
const FLOAT_ICON_SIZE = 64
const assistantMountainIcon = '/favicon.png'

const iconX = ref(window.innerWidth - 104)
const iconY = ref(window.innerHeight - 138)
const panelX = ref(0)
const panelY = ref(0)
const dragOffsetX = ref(0)
const dragOffsetY = ref(0)
const floatingEl = ref<HTMLElement | null>(null)
const pointerIdRef = ref<number | null>(null)

const endpoint = computed(() =>
  props.mode === 'teacher' ? '/api/ai/teacher/ask' : '/api/ai/student/ask'
)
const title = computed(() => (props.mode === 'teacher' ? 'AI 教师助手' : 'AI 学习助手'))
// const placeholder = computed(() => (props.mode === 'teacher' ? '例如：给出这次考试讲评提纲' : '例如：解释一下最小生成树'))
const nowTime = () => new Date().toISOString()
const getStorageUserId = () => {
  const token = localStorage.getItem('token') || ''
  const prefix = 'dummy-jwt-token-'
  if (token.startsWith(prefix)) {
    return token.slice(prefix.length)
  }
  return 'anonymous'
}
const sessionStorageKey = computed(() => `ai.float.${getStorageUserId()}.${props.mode}.sessions`)
const panelStorageKey = computed(
  () => `ai.float.${getStorageUserId()}.${props.mode}.panel.position`
)

const currentSession = computed(
  () => sessions.value.find((item) => item.id === currentSessionId.value) || null
)
// const displayTitle = computed(() => currentSession.value?.title || title.value)

const panelWidth = 390
const panelHeight = 620
const panelMargin = 16

const getAutoPanelPosition = () => {
  let left = iconX.value - panelWidth - 14
  if (left < panelMargin) {
    left = iconX.value + FLOAT_ICON_SIZE + 14
  }
  if (left + panelWidth > window.innerWidth - panelMargin) {
    left = Math.max(panelMargin, window.innerWidth - panelWidth - panelMargin)
  }

  let top = iconY.value - panelHeight + 72
  if (top < panelMargin) {
    top = panelMargin
  }
  if (top + panelHeight > window.innerHeight - panelMargin) {
    top = Math.max(panelMargin, window.innerHeight - panelHeight - panelMargin)
  }

  return { left, top }
}

const panelStyle = computed(() => {
  const autoPos = getAutoPanelPosition()
  const left = panelMovedByUser.value ? panelX.value : autoPos.left
  const top = panelMovedByUser.value ? panelY.value : autoPos.top

  return {
    left: `${left}px`,
    top: `${top}px`
  }
})

const iconStyle = computed(() => ({
  left: `${iconX.value}px`,
  top: `${iconY.value}px`
}))

const getHeaders = () => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const savePosition = () => {
  localStorage.setItem('ai.floating.position', JSON.stringify({ x: iconX.value, y: iconY.value }))
}

const loadPosition = () => {
  try {
    const raw = localStorage.getItem('ai.floating.position')
    if (!raw) return
    const pos = JSON.parse(raw)
    if (typeof pos.x === 'number' && typeof pos.y === 'number') {
      iconX.value = Math.max(10, Math.min(window.innerWidth - FLOAT_ICON_SIZE - 10, pos.x))
      iconY.value = Math.max(10, Math.min(window.innerHeight - FLOAT_ICON_SIZE - 10, pos.y))
    }
  } catch {
    // ignore invalid cache
  }
}

const saveHistory = () => {
  const selected = currentSession.value
  if (!selected) return

  selected.messages = messages.value.slice(-60)
  selected.contextText = contextText.value
  selected.updatedAt = nowTime()
  localStorage.setItem(sessionStorageKey.value, JSON.stringify(sessions.value))
}

const loadHistory = () => {
  try {
    const raw = localStorage.getItem(sessionStorageKey.value)
    const parsed = raw ? (JSON.parse(raw) as ChatSession[]) : []
    const now = Date.now()

    const validSessions = parsed
      .map((item) => {
        const validMessages = (item.messages || []).filter((msg) => {
          const t = new Date(msg.timestamp || 0).getTime()
          return t > 0 && now - t <= MESSAGE_TTL_MS
        })
        const hasExpired = (item.messages || []).length > validMessages.length
        return {
          ...item,
          messages: validMessages,
          updatedAt: item.updatedAt || item.createdAt || nowTime(),
          contextText: item.contextText || '',
          hasExpired
        }
      })
      .filter((item) => item.messages.length > 0 || (item.contextText && item.contextText.trim()))

    sessions.value = validSessions
      .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
      .slice(0, 20)
      .map(({ hasExpired, ...rest }) => {
        if (hasExpired) {
          expireNotice.value = '部分历史消息已超过3天，已自动过期。'
        }
        return rest
      })

    if (sessions.value.length === 0) {
      createSession()
    } else {
      const first = sessions.value[0]
      if (first) {
        switchSession(first.id)
      }
    }
  } catch {
    sessions.value = []
    createSession()
  }
}

const createSession = () => {
  const now = nowTime()
  const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  const session: ChatSession = {
    id,
    title: '新对话',
    createdAt: now,
    updatedAt: now,
    messages: [],
    contextText: ''
  }
  sessions.value = [session, ...sessions.value].slice(0, 20)
  showHistoryMenu.value = false
  renamingTitle.value = false
  switchSession(id)
  saveHistory()
}

const switchSession = (id: string) => {
  const found = sessions.value.find((item) => item.id === id)
  if (!found) return
  currentSessionId.value = id
  showHistoryMenu.value = false
  renamingTitle.value = false
  messages.value = [...(found.messages || [])]
  contextText.value = found.contextText || ''
  expireNotice.value = ''
  scrollBottom()
}

const renameCurrentSessionIfNeeded = (q: string) => {
  const selected = currentSession.value
  if (!selected) return
  if (selected.title !== '新对话') return
  selected.title = q.length > 16 ? `${q.slice(0, 16)}...` : q
}

const savePanelPosition = () => {
  if (!panelMovedByUser.value) return
  localStorage.setItem(panelStorageKey.value, JSON.stringify({ x: panelX.value, y: panelY.value }))
}

const loadPanelPosition = () => {
  try {
    const raw = localStorage.getItem(panelStorageKey.value)
    if (!raw) return
    const pos = JSON.parse(raw)
    if (typeof pos.x === 'number' && typeof pos.y === 'number') {
      panelMovedByUser.value = true
      clampPanelPosition(pos.x, pos.y)
    }
  } catch {
    // ignore invalid cache
  }
}

const scrollBottom = async () => {
  await nextTick()
  if (streamRef.value) {
    streamRef.value.scrollTop = streamRef.value.scrollHeight
  }
}

const clampPosition = (x: number, y: number) => {
  iconX.value = Math.max(10, Math.min(window.innerWidth - FLOAT_ICON_SIZE - 10, x))
  iconY.value = Math.max(10, Math.min(window.innerHeight - FLOAT_ICON_SIZE - 10, y))
}

const clampPanelPosition = (x: number, y: number) => {
  panelX.value = Math.max(panelMargin, Math.min(window.innerWidth - panelWidth - panelMargin, x))
  panelY.value = Math.max(panelMargin, Math.min(window.innerHeight - panelHeight - panelMargin, y))
}

const startDrag = (clientX: number, clientY: number) => {
  iconDragging.value = true
  moved.value = false
  dragOffsetX.value = clientX - iconX.value
  dragOffsetY.value = clientY - iconY.value
}

const onPointerDown = (event: PointerEvent) => {
  startDrag(event.clientX, event.clientY)
  // try to capture pointer to avoid event loss and make movement more immediate
  try {
    if (floatingEl.value && typeof floatingEl.value.setPointerCapture === 'function') {
      floatingEl.value.setPointerCapture(event.pointerId)
      pointerIdRef.value = event.pointerId
    }
  } catch (e) {
    // ignore if not supported
  }

  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
}

const onPointerMove = (event: PointerEvent) => {
  if (!iconDragging.value) return
  moved.value = true
  clampPosition(event.clientX - dragOffsetX.value, event.clientY - dragOffsetY.value)
}

const onPointerUp = (event?: PointerEvent) => {
  if (!iconDragging.value) return
  iconDragging.value = false
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
  // release pointer capture if set
  try {
    if (floatingEl.value && pointerIdRef.value !== null && typeof floatingEl.value.releasePointerCapture === 'function') {
      floatingEl.value.releasePointerCapture(pointerIdRef.value)
    }
  } catch (e) {
    // ignore
  }
  pointerIdRef.value = null
  savePosition()
}

const toggleExpanded = () => {
  if (moved.value) {
    moved.value = false
    return
  }
  expanded.value = !expanded.value
  if (expanded.value) {
    if (!panelMovedByUser.value) {
      const autoPos = getAutoPanelPosition()
      panelX.value = autoPos.left
      panelY.value = autoPos.top
    }
    scrollBottom()
  }
}

const closePanel = () => {
  expanded.value = false
}

const startPanelDrag = (event: PointerEvent) => {
  panelDragging.value = true
  panelDragOffsetX.value = event.clientX - panelX.value
  panelDragOffsetY.value = event.clientY - panelY.value
  window.addEventListener('pointermove', onPanelPointerMove)
  window.addEventListener('pointerup', onPanelPointerUp)
}

const onPanelPointerMove = (event: PointerEvent) => {
  if (!panelDragging.value) return
  panelMovedByUser.value = true
  clampPanelPosition(event.clientX - panelDragOffsetX.value, event.clientY - panelDragOffsetY.value)
}

const onPanelPointerUp = () => {
  if (!panelDragging.value) return
  panelDragging.value = false
  window.removeEventListener('pointermove', onPanelPointerMove)
  window.removeEventListener('pointerup', onPanelPointerUp)
  savePanelPosition()
}

const applyDroppedQuestion = (dataText: string) => {
  if (!dataText.trim()) return
  const cleanText = dataText
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .trim()
  contextText.value = (contextText.value + '\n\n' + cleanText).trim()
  if (!expanded.value) {
    expanded.value = true
  }
  showToast({ message: '已关联拖拽题目到 AI 上下文', type: 'success' })
}

const handleDropPayload = (event: DragEvent) => {
  event.preventDefault()
  if (!route.path.startsWith('/practice')) {
    showToast({ message: '仅练习界面的题目可添加进 AI 上下文', type: 'warning' })
    return
  }
  const transfer = event.dataTransfer
  if (!transfer) return

  const custom = transfer.getData('application/x-uqbank-question')
  if (custom) {
    try {
      const obj = JSON.parse(custom)
      applyDroppedQuestion(obj.stem || '')
      return
    } catch {
      // fallback to plain text
    }
  }

  const plain = transfer.getData('text/plain')
  if (plain) {
    applyDroppedQuestion(plain)
  }
}

const handleDragOver = (event: DragEvent) => {
  event.preventDefault()
}

const onDragEnter = (event?: DragEvent) => {
  dragEnterCounter++
  draggingQuestion.value = true
}

const onDragLeave = (event?: DragEvent) => {
  dragEnterCounter = Math.max(0, dragEnterCounter - 1)
  if (dragEnterCounter === 0) {
    draggingQuestion.value = false
  }
}

const clearMessages = () => {
  if (!currentSession.value) return
  messages.value = []
  contextText.value = ''
  currentSession.value.title = '新对话'
  expireNotice.value = ''
  saveHistory()
}

const toggleHistoryMenu = () => {
  showHistoryMenu.value = !showHistoryMenu.value
}

// const startRenameTitle = () => {
//   if (!currentSession.value) return
//   editingTitle.value = currentSession.value.title
//   renamingTitle.value = true
//   showHistoryMenu.value = false
// }

// const saveCustomTitle = () => {
//   const selected = currentSession.value
//   const custom = editingTitle.value.trim()
//   if (!selected) return
//   if (!custom) {
//     showToast({ message: '对话名称不能为空', type: 'warning' })
//     return
//   }
//   selected.title = custom
//   renamingTitle.value = false
//   saveHistory()
// }

const onAskInputKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    if (!sending.value) {
      void sendQuestion()
    }
  }
}

const sendQuestion = async () => {
  const q = question.value.trim()
  if (!q && !contextText.value.trim()) {
    showToast({ message: '请输入问题', type: 'warning' })
    return
  }

  const actQ = q || '请解答引用题目'
  const currentContext = contextText.value
  const displayQ = currentContext ? `[关联题目]\n${currentContext}\n\n${actQ}` : actQ

  messages.value.push({ role: 'user', content: displayQ, timestamp: nowTime() })
  renameCurrentSessionIfNeeded(actQ)
  question.value = ''
  contextText.value = ''
  saveHistory()
  await scrollBottom()

  sending.value = true
  try {
    const actQForApi = actQ + '\n\n【系统提示：请用中文，尽量简短地回复】'
    const resp = await axios.post(
      endpoint.value,
      {
        question: actQForApi,
        context: currentContext
      },
      { headers: getHeaders() }
    )
    messages.value.push({
      role: 'assistant',
      content: resp.data?.answer || 'AI 未返回有效内容',
      timestamp: nowTime()
    })
  } catch (error: any) {
    const msg = error?.response?.data?.error || 'AI 请求失败'
    messages.value.push({ role: 'assistant', content: `请求失败：${msg}`, timestamp: nowTime() })
    showToast({ message: msg, type: 'error' })
  } finally {
    sending.value = false
    saveHistory()
    await scrollBottom()
  }
}

onMounted(() => {
  loadPosition()
  loadPanelPosition()
  loadHistory()
  window.addEventListener('resize', () => {
    clampPosition(iconX.value, iconY.value)
    if (panelMovedByUser.value) {
      clampPanelPosition(panelX.value, panelY.value)
      savePanelPosition()
    }
  })
})
</script>

<template>
  <div class="ai-floating">
    <button
      ref="floatingEl"
      :class="{ dragging: iconDragging || draggingQuestion }"
      class="floating-icon"
      :style="iconStyle"
      @pointerdown="onPointerDown"
      @click="toggleExpanded"
      @dragenter.prevent="onDragEnter"
      @dragleave.prevent="onDragLeave"
      @dragover="handleDragOver"
      @drop="handleDropPayload"
      :aria-label="title"
      title="AI 助手"
    >
      <img :src="assistantMountainIcon" alt="AI 助手图标" class="floating-icon-image" />
    </button>

    <div
      v-if="expanded"
      class="chat-panel ds-panel"
      :style="panelStyle"
      @dragenter.prevent="onDragEnter"
      @dragleave.prevent="onDragLeave"
      @dragover="handleDragOver"
      @drop="handleDropPayload"
    >
      <div class="ds-header" @pointerdown="startPanelDrag">
        <div class="history-entry">
          <button class="ds-icon-btn" @pointerdown.stop @click="toggleHistoryMenu" title="对话历史">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <line x1="4" x2="20" y1="12" y2="12" />
              <line x1="4" x2="20" y1="6" y2="6" />
              <line x1="4" x2="20" y1="18" y2="18" />
            </svg>
          </button>
          <div v-if="showHistoryMenu" class="history-menu" @pointerdown.stop>
            <button
              v-for="item in sessions"
              :key="item.id"
              class="history-item"
              :class="{ active: item.id === currentSessionId }"
              @click="switchSession(item.id)"
            >
              <span class="history-title">{{ item.title }}</span>
              <span class="history-time">{{ new Date(item.updatedAt).toLocaleDateString() }}</span>
            </button>
            <button class="history-item clear-all" @click="clearMessages">清空当前对话</button>
          </div>
        </div>
        <div class="ds-header-actions">
          <button class="ds-icon-btn" @pointerdown.stop @click="createSession" title="新建对话">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M5 12h14" />
              <path d="M12 5v14" />
            </svg>
          </button>
          <button class="ds-icon-btn" @pointerdown.stop @click="closePanel" title="关闭" aria-label="关闭">
            <X :size="18" />
          </button>
        </div>
      </div>

      <div v-if="expireNotice" class="expire-tip">{{ expireNotice }}</div>

      <div ref="streamRef" class="ds-stream">
        <div v-if="messages.length === 0" class="ds-empty-state">
          <h2 class="ds-empty-title">今天有什么可以帮到你？</h2>
        </div>
        <div v-for="(msg, idx) in messages" :key="idx" class="msg-row" :class="msg.role">
          <img
            class="avatar-dot"
            :src="assistantMountainIcon"
            v-if="msg.role !== 'user'"
            alt="AI"
          />
          <div class="msg" :class="msg.role">
            {{ msg.content }}
          </div>
        </div>
        <div v-if="sending" class="msg-row assistant">
          <img class="avatar-dot" :src="assistantMountainIcon" alt="AI" />
          <div class="msg assistant typing-indicator">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </div>
        </div>
      </div>

      <div class="ds-composer-wrapper">
        <div class="ds-composer">
          <div class="ds-context-box" v-if="contextText || draggingQuestion">
            <textarea
              v-model="contextText"
              rows="1"
              class="ds-context-input"
              placeholder="拖动题目到此处作为关联背景"
            ></textarea>
            <button class="ds-clear-context" @click="contextText = ''" title="清除关联题目">
              <X :size="14" />
            </button>
          </div>
          <textarea
            v-model="question"
            rows="1"
            class="ds-ask-input"
            placeholder="拖拽题目添加对话背景"
            :disabled="sending"
            @keydown="onAskInputKeydown"
            style="color: #333"
          ></textarea>

          <div class="ds-composer-tools">
            <button
              class="ds-send-btn"
              :class="{ active: question.trim() || contextText.trim() }"
              :disabled="sending"
              @click="sendQuestion"
              :title="sending ? '发送中' : '发送'"
            >
              <ArrowUp :size="18" />
            </button>
          </div>
        </div>
      </div>

      <div class="resize-handle"></div>
    </div>
  </div>
</template>

<style scoped>
.floating-icon {
  position: fixed;
  width: 64px;
  height: 64px;
  border-radius: 999px;
  border: none;
  background: transparent;
  color: #fff;
  cursor: grab;
  transition: none; /* ensure immediate position updates without CSS smoothing */
  z-index: 3000;
  padding: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}
.floating-icon.dragging {
  cursor: grabbing;
}
.floating-icon-image {
  width: 100%;
  height: 100%;
  border-radius: 999px;
  object-fit: cover;
  pointer-events: none;
}
.chat-panel.ds-panel {
  position: fixed;
  width: 380px;
  height: 660px;
  background: #fdfdfd;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
  z-index: 3001;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.ds-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  background: transparent;
  cursor: default;
}
.ds-header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
.ds-icon-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  color: #111;
  padding: 4px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ds-icon-btn:hover {
  background: #f3f4f6;
}
.history-entry {
  position: relative;
}
.history-menu {
  position: absolute;
  left: 0;
  top: calc(100% + 4px);
  width: 200px;
  max-height: 240px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  z-index: 10;
}
.history-item {
  width: 100%;
  border: none;
  background: none;
  display: flex;
  justify-content: space-between;
  padding: 10px 12px;
  cursor: pointer;
  text-align: left;
  font-size: 13px;
  color: #374151;
}
.history-item:hover,
.history-item.active {
  background: #f3f4f6;
}
.history-item.clear-all {
  color: #dc2626;
  border-top: 1px solid #e5e7eb;
  justify-content: center;
  font-weight: 500;
}
.history-time {
  font-size: 11px;
  color: #9ca3af;
}
.expire-tip {
  padding: 4px 12px;
  font-size: 12px;
  color: #b45309;
  background: #fef3c7;
  text-align: center;
}

.ds-stream {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.ds-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding-bottom: 2rem;
  user-select: none;
}
.ds-empty-logo {
  width: 44px;
  height: 44px;
  margin-bottom: 12px;
  object-fit: cover;
  background: transparent;
  border-radius: 50%;
  padding: 0;
}
.ds-empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #111;
  margin: 0;
  letter-spacing: 0.5px;
}

.msg-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.msg-row.user {
  justify-content: flex-end;
}
.avatar-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  background: transparent;
  padding: 0;
  display: block;
  flex-shrink: 0;
}
.msg-row.system .avatar-dot {
  background: #f59e0b;
}
.msg {
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}
.msg.user {
  background: #f3f4f6;
  color: #111;
  border-radius: 16px;
  border-bottom-right-radius: 4px;
  padding: 8px 16px;
}
.msg.assistant {
  background: transparent;
  color: #111;
  padding: 0;
}
.msg.system {
  background: #fffbeb;
  color: #b45309;
  border: 1px solid #fde68a;
}
.typing-indicator {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 11px 0;
  background: transparent;
}
.typing-indicator .dot {
  width: 6px;
  height: 6px;
  background-color: #3b82f6;
  border-radius: 50%;
  opacity: 0.6;
  animation: typing-bounce 1.4s infinite ease-in-out both;
}
.typing-indicator .dot:nth-child(1) {
  animation-delay: -0.32s;
}
.typing-indicator .dot:nth-child(2) {
  animation-delay: -0.16s;
}
@keyframes typing-bounce {
  0%,
  80%,
  100% {
    transform: scale(0);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.ds-composer-wrapper {
  padding: 0 16px 20px;
  background: transparent;
}
.ds-composer {
  background: #fff;
  border-radius: 20px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: border-color 0.2s;
}
.ds-composer:focus-within {
  border-color: #d1d5db;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}
.ds-context-input,
.ds-ask-input {
  width: 100%;
  border: none;
  background: transparent;
  outline: none;
  resize: none;
  font-size: 14.5px;
  line-height: 1.5;
  color: #111;
  font-family: inherit;
}
.ds-context-box {
  position: relative;
  margin-bottom: 4px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #e5e7eb;
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.ds-context-input {
  font-size: 13px;
  color: #6b7280;
  flex: 1;
  padding: 0;
  margin: 0;
  border: none;
  outline: none;
  resize: none;
  background: transparent;
  font-family: inherit;
  line-height: 1.5;
}
.ds-clear-context {
  background: #f3f4f6;
  color: #9ca3af;
  border: none;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  padding: 0;
  flex-shrink: 0;
  margin-top: 2px;
}
.ds-clear-context:hover {
  background: #e5e7eb;
  color: #4b5563;
}
.ds-ask-input::placeholder {
  color: #9ca3af;
  font-weight: 400;
}

.ds-composer-tools {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 4px;
}
.ds-tools-left,
.ds-tools-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ds-tool-btn {
  border: none;
  background: transparent;
  color: #4b5563;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  border-radius: 6px;
  transition:
    color 0.15s,
    background 0.15s;
}
.ds-tool-btn:hover {
  color: #111;
}
.ds-tool-btn.circular {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  padding: 0;
}
.ds-tool-btn.circular.has-bg {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #dbeafe;
}
.ds-tool-btn.circular.has-bg:hover {
  background: #dbeafe;
}
.ds-send-btn {
  border: none;
  background: #e5e7eb;
  color: #fff;
  cursor: not-allowed;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  padding: 0;
  flex-shrink: 0;
}
.ds-send-btn.active {
  background: #2563eb;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
  transform: translateY(-1px);
}
.ds-send-btn.active:active {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.2);
}
.ds-send-btn.active:hover {
  background: #1d4ed8;
}
.resize-handle {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 12px;
  height: 12px;
  background: linear-gradient(135deg, transparent 50%, #d1d5db 50%);
  cursor: se-resize;
  opacity: 0.5;
}
</style>
