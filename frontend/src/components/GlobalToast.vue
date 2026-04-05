<script setup lang="ts">
import { useToast } from '@/composables/useToast'
import { Check, AlertCircle, AlertTriangle } from 'lucide-vue-next'

const { toastMessage, toastType, toastVisible } = useToast()
</script>

<template>
  <Teleport to="body">
    <Transition name="toast">
      <div v-if="toastVisible" :class="['global-toast', 'toast-' + toastType]">
        <span class="toast-icon">
          <Check v-if="toastType === 'success'" :size="14" />
          <AlertCircle v-else-if="toastType === 'error'" :size="14" />
          <AlertTriangle v-else :size="14" />
        </span>
        <span class="toast-text">{{ toastMessage }}</span>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.global-toast {
  position: fixed;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  border-radius: var(--line-radius-full);
  color: white;
  font-weight: 600;
  font-size: 14px;
  z-index: var(--line-layer-toast);
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: var(--line-shadow-lg);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(6px);
}

.toast-success {
  background-color: color-mix(in srgb, var(--line-success) 86%, black 14%);
}

.toast-error {
  background-color: color-mix(in srgb, var(--line-error) 86%, black 14%);
}

.toast-warning {
  background-color: color-mix(in srgb, var(--line-warning) 86%, black 14%);
}

.toast-icon {
  width: 20px;
  height: 20px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toast-text {
  white-space: nowrap;
}

.toast-enter-active {
  animation: slideDownToast 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.toast-leave-active {
  animation: slideUpToast 0.2s ease-out forwards;
}

@keyframes slideDownToast {
  from {
    opacity: 0;
    transform: translate(-50%, -20px);
  }
  to {
    opacity: 1;
    transform: translate(-50%, 0);
  }
}

@keyframes slideUpToast {
  from {
    opacity: 1;
    transform: translate(-50%, 0);
  }
  to {
    opacity: 0;
    transform: translate(-50%, -20px);
  }
}
</style>
