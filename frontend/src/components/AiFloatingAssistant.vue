<script setup lang="ts">
import { Bot, Clock3, Send, Trash2 } from 'lucide-vue-next'
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
const dragging = ref(false)
const moved = ref(false)
const showHistoryMenu = ref(false)
const renamingTitle = ref(false)
const editingTitle = ref('')
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

const iconX = ref(window.innerWidth - 104)
const iconY = ref(window.innerHeight - 138)
const panelX = ref(0)
const panelY = ref(0)
const dragOffsetX = ref(0)
const dragOffsetY = ref(0)

const endpoint = computed(() => (props.mode === 'teacher' ? '/api/ai/teacher/ask' : '/api/ai/student/ask'))
const title = computed(() => (props.mode === 'teacher' ? 'AI 教师助手' : 'AI 学习助手'))
const placeholder = computed(() => (props.mode === 'teacher' ? '例如：给出这次考试讲评提纲' : '例如：解释一下最小生成树'))
const nowTime = () => new Date().toISOString()
const sessionStorageKey = computed(() => (props.mode === 'teacher' ? 'ai.float.teacher.sessions' : 'ai.float.student.sessions'))
const panelStorageKey = computed(() => (props.mode === 'teacher' ? 'ai.float.teacher.panel.position' : 'ai.float.student.panel.position'))

const currentSession = computed(() => sessions.value.find(item => item.id === currentSessionId.value) || null)
const displayTitle = computed(() => currentSession.value?.title || title.value)

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
      .map(item => {
        const validMessages = (item.messages || []).filter(msg => {
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
      .filter(item => item.messages.length > 0 || (item.contextText && item.contextText.trim()))

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
  const found = sessions.value.find(item => item.id === id)
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
  dragging.value = true
  moved.value = false
  dragOffsetX.value = clientX - iconX.value
  dragOffsetY.value = clientY - iconY.value
}

const onPointerDown = (event: PointerEvent) => {
  startDrag(event.clientX, event.clientY)
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
}

const onPointerMove = (event: PointerEvent) => {
  if (!dragging.value) return
  moved.value = true
  clampPosition(event.clientX - dragOffsetX.value, event.clientY - dragOffsetY.value)
}

const onPointerUp = () => {
  if (!dragging.value) return
  dragging.value = false
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
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
  const prefix = '\n\n[已关联拖拽题目]\n'
  contextText.value = (contextText.value + prefix + dataText.trim()).trim()
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
      const plain = [
        `题目ID: ${obj.questionId || '-'}`,
        `题型: ${obj.type || '-'}`,
        `题干: ${obj.stem || ''}`,
        `学生答案: ${obj.userAnswer || '(未作答)'}`,
        `参考答案: ${obj.reference || '(无)'}`
      ].join('\n')
      applyDroppedQuestion(plain)
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

const startRenameTitle = () => {
  if (!currentSession.value) return
  editingTitle.value = currentSession.value.title
  renamingTitle.value = true
  showHistoryMenu.value = false
}

const saveCustomTitle = () => {
  const selected = currentSession.value
  const custom = editingTitle.value.trim()
  if (!selected) return
  if (!custom) {
    showToast({ message: '对话名称不能为空', type: 'warning' })
    return
  }
  selected.title = custom
  renamingTitle.value = false
  saveHistory()
}

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
  if (!q) {
    showToast({ message: '请输入问题', type: 'warning' })
    return
  }

  messages.value.push({ role: 'user', content: q, timestamp: nowTime() })
  renameCurrentSessionIfNeeded(q)
  question.value = ''
  saveHistory()
  await scrollBottom()

  sending.value = true
  try {
    const resp = await axios.post(
      endpoint.value,
      {
        question: q,
        context: contextText.value
      },
      { headers: getHeaders() }
    )
    messages.value.push({ role: 'assistant', content: resp.data?.answer || 'AI 未返回有效内容', timestamp: nowTime() })
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
      class="floating-icon"
      :style="iconStyle"
      @pointerdown="onPointerDown"
      @click="toggleExpanded"
      @dragover="handleDragOver"
      @drop="handleDropPayload"
      :aria-label="title"
      title="AI 助手"
    >
      <Bot :size="45" />
    </button>

    <div
      v-if="expanded"
      class="chat-panel"
      :style="panelStyle"
      @dragover="handleDragOver"
      @drop="handleDropPayload"
    >
      <div class="panel-header" @pointerdown="startPanelDrag">
        <div class="title-group">
          <Bot :size="16" />
          <h3
            v-if="!renamingTitle"
            class="title-text"
            @pointerdown.stop
            @dblclick.stop="startRenameTitle"
            title="双击可重命名"
          >
            {{ displayTitle }}
          </h3>
          <input
            v-else
            v-model="editingTitle"
            class="title-input"
            maxlength="30"
            @pointerdown.stop
            @keydown.enter.prevent="saveCustomTitle"
            @keydown.esc.prevent="renamingTitle = false"
            @blur="saveCustomTitle"
          />
        </div>
        <div class="actions-group">
          <div class="history-entry">
            <button class="ghost-btn icon-btn" @pointerdown.stop @click="toggleHistoryMenu" title="对话历史">
              <Clock3 :size="14" />
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
            </div>
          </div>
          <button class="ghost-btn" @pointerdown.stop @click="createSession">新建</button>
          <button class="ghost-btn" @pointerdown.stop @click="clearMessages">
            <Trash2 :size="14" />
            清空
          </button>
        </div>
      </div>

      <div class="limit-tip">对话仅保留3天，超过会提示消息过期。</div>
      <div v-if="expireNotice" class="expire-tip">{{ expireNotice }}</div>

      <div class="context-box">
        <textarea
          v-model="contextText"
          rows="2"
          class="context-input"
          placeholder="可选：输入课程背景、要求或评分关注点"
        ></textarea>
      </div>

      <div ref="streamRef" class="stream">
        <div v-if="messages.length === 0" class="empty">在这里提问，助手会在附近弹窗回复。</div>
        <div v-for="(msg, idx) in messages" :key="idx" class="msg-row" :class="msg.role">
          <div class="avatar-dot">{{ msg.role === 'user' ? '你' : (msg.role === 'assistant' ? 'AI' : '!') }}</div>
          <div class="msg" :class="msg.role">
            {{ msg.content }}
          </div>
        </div>
      </div>

      <div class="composer">
        <div class="ask-wrap">
          <textarea
            v-model="question"
            rows="3"
            class="ask-input"
            :placeholder="placeholder"
            :disabled="sending"
            @keydown="onAskInputKeydown"
          ></textarea>
          <button class="send-btn" :disabled="sending" @click="sendQuestion" :title="sending ? '发送中' : '发送'">
            <Send :size="16" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.floating-icon {
  position: fixed;
  width: 64px;
  height: 64px;
  border-radius: 999px;
  border: 1px solid #10a37f;
  background: linear-gradient(155deg, #10a37f, #0e8f70);
  color: #ffffff;
  cursor: grab;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 22px rgba(16, 163, 127, 0.35);
}

.floating-icon:active {
  cursor: grabbing;
}

.chat-panel {
  position: fixed;
  width: min(390px, calc(100vw - 26px));
  height: min(620px, calc(100vh - 32px));
  border-radius: 18px;
  border: 1px solid #d9d9e3;
  background: #ffffff;
  z-index: 3001;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 24px 48px rgba(30, 41, 59, 0.2);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid #ececf1;
  background: #f7f7f8;
  cursor: move;
}

.title-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
  color: #202123;
}

.panel-header h3 {
  margin: 0;
  font-size: 14px;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #202123;
}

.title-text {
  cursor: text;
}

.title-input {
  width: 100%;
  max-width: 220px;
  min-width: 0;
  border: 1px solid #d9d9e3;
  border-radius: 8px;
  padding: 4px 8px;
  font-size: 13px;
  color: #202123;
  outline: none;
}

.ghost-btn {
  border: 1px solid #d9d9e3;
  background: #ffffff;
  border-radius: 999px;
  padding: 6px 10px;
  min-height: 34px;
  font-size: 12px;
  color: #4b5563;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  gap: 6px;
}

.actions-group {
  display: inline-flex;
  align-items: center;
  flex-wrap: nowrap;
  flex-shrink: 0;
  gap: 8px;
}

.icon-btn {
  padding: 6px 8px;
}

.limit-tip {
  font-size: 12px;
  color: #6b7280;
  padding: 8px 12px 0;
}

.expire-tip {
  margin: 8px 12px 0;
  font-size: 12px;
  color: #92400e;
  background: #fef3c7;
  border: 1px solid #fcd34d;
  border-radius: 10px;
  padding: 6px 10px;
}

.history-entry {
  position: relative;
}

.history-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  width: 220px;
  max-height: 240px;
  overflow: auto;
  border: 1px solid #d9d9e3;
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.16);
  z-index: 5;
}

.history-item {
  width: 100%;
  border: none;
  background: transparent;
  display: flex;
  justify-content: space-between;
  align-items: center;
  text-align: left;
  padding: 9px 10px;
  cursor: pointer;
}

.history-item:hover {
  background: #f3f4f6;
}

.history-item.active {
  background: #e8f0fe;
}

.history-title {
  font-size: 12px;
  color: #1f2937;
  max-width: 132px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-time {
  font-size: 11px;
  color: #6b7280;
}

.context-box {
  padding: 10px 12px;
  border-bottom: 1px solid #ececf1;
}

.context-input,
.ask-input {
  width: 100%;
  box-sizing: border-box;
  border-radius: 10px;
  border: 1px solid #d9d9e3;
  background: #ffffff;
  padding: 10px;
  font-size: 13px;
  resize: none;
}

.ask-input {
  padding-right: 52px;
  padding-bottom: 40px;
}

.stream {
  flex: 1;
  overflow: auto;
  padding: 12px;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.empty {
  color: #6b7280;
  font-size: 13px;
}

.msg-row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.msg-row.user {
  justify-content: flex-end;
}

.avatar-dot {
  min-width: 24px;
  height: 24px;
  border-radius: 999px;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  background: #10a37f;
}

.msg-row.user .avatar-dot {
  order: 2;
  background: #64748b;
}

.msg-row.system .avatar-dot {
  background: #f59e0b;
}

.msg {
  max-width: 84%;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.msg.user {
  background: #f7f7f8;
  color: #111827;
  border: 1px solid #ececf1;
}

.msg.assistant {
  background: #10a37f;
  color: #ffffff;
}

.msg.system {
  background: #fffbeb;
  border: 1px solid #fcd34d;
  color: #92400e;
}

.composer {
  padding: 12px;
  border-top: 1px solid #ececf1;
  background: #f7f7f8;
}

.ask-wrap {
  position: relative;
}

.send-btn {
  position: absolute;
  right: 12px;
  bottom: 12px;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 999px;
  background: #10a37f;
  color: #ffffff;
  padding: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
</style>
