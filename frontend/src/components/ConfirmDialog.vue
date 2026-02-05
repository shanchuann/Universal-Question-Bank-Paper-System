<script setup lang="ts">
import { ref, computed } from 'vue'

interface DialogState {
  visible: boolean
  title: string
  message: string
  confirmText: string
  cancelText: string
  type: 'info' | 'warning' | 'danger'
  onConfirm: () => void
  onCancel: () => void
}

const state = ref<DialogState>({
  visible: false,
  title: '确认',
  message: '',
  confirmText: '确定',
  cancelText: '取消',
  type: 'info',
  onConfirm: () => {},
  onCancel: () => {}
})

const confirmBtnClass = computed(() => {
  switch (state.value.type) {
    case 'danger': return 'danger-btn'
    case 'warning': return 'warning-btn'
    default: return 'primary-btn'
  }
})

function show(options: {
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
  type?: 'info' | 'warning' | 'danger'
}): Promise<boolean> {
  return new Promise((resolve) => {
    state.value = {
      visible: true,
      title: options.title || '确认',
      message: options.message,
      confirmText: options.confirmText || '确定',
      cancelText: options.cancelText || '取消',
      type: options.type || 'info',
      onConfirm: () => {
        state.value.visible = false
        resolve(true)
      },
      onCancel: () => {
        state.value.visible = false
        resolve(false)
      }
    }
  })
}

function close() {
  state.value.visible = false
}

defineExpose({ show, close })
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="state.visible" class="confirm-overlay" @click.self="state.onCancel">
        <div class="confirm-dialog">
          <div class="dialog-header">
            <div class="dialog-icon" :class="state.type">
              <svg v-if="state.type === 'info'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="16" x2="12" y2="12"></line>
                <line x1="12" y1="8" x2="12.01" y2="8"></line>
              </svg>
              <svg v-else-if="state.type === 'warning'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                <line x1="12" y1="9" x2="12" y2="13"></line>
                <line x1="12" y1="17" x2="12.01" y2="17"></line>
              </svg>
              <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="15" y1="9" x2="9" y2="15"></line>
                <line x1="9" y1="9" x2="15" y2="15"></line>
              </svg>
            </div>
            <h3 class="dialog-title">{{ state.title }}</h3>
          </div>
          <div class="dialog-content">
            <p>{{ state.message }}</p>
          </div>
          <div class="dialog-actions">
            <button class="dialog-btn cancel-btn" @click="state.onCancel">
              {{ state.cancelText }}
            </button>
            <button :class="['dialog-btn', confirmBtnClass]" @click="state.onConfirm">
              {{ state.confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.confirm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10002;
}

.confirm-dialog {
  background: var(--line-card-bg, #fff);
  border-radius: 16px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  animation: dialogSlideIn 0.2s ease-out;
}

@keyframes dialogSlideIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.dialog-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 24px 16px;
}

.dialog-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dialog-icon.info {
  background: rgba(26, 115, 232, 0.1);
  color: #1a73e8;
}

.dialog-icon.warning {
  background: rgba(249, 171, 0, 0.1);
  color: #f9ab00;
}

.dialog-icon.danger {
  background: rgba(217, 48, 37, 0.1);
  color: #d93025;
}

.dialog-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--line-text-primary, #1e293b);
}

.dialog-content {
  padding: 0 24px 24px;
}

.dialog-content p {
  margin: 0;
  font-size: 15px;
  line-height: 1.6;
  color: var(--line-text-secondary, #64748b);
}

.dialog-actions {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  background: var(--line-bg-soft, #f8fafc);
  border-radius: 0 0 16px 16px;
  justify-content: flex-end;
}

.dialog-btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
  min-width: 80px;
}

.cancel-btn {
  background: transparent;
  color: var(--line-text-secondary, #64748b);
  border: 1px solid var(--line-border, #e2e8f0);
}

.cancel-btn:hover {
  background: var(--line-bg-hover, #f1f5f9);
  color: var(--line-text-primary, #1e293b);
}

.primary-btn {
  background: var(--line-primary, #1a73e8);
  color: #fff;
}

.primary-btn:hover {
  background: #1557b0;
}

.warning-btn {
  background: #f9ab00;
  color: #fff;
}

.warning-btn:hover {
  background: #e69900;
}

.danger-btn {
  background: #d93025;
  color: #fff;
}

.danger-btn:hover {
  background: #b3261e;
}

.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.2s ease;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}
</style>
