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
  border-radius: 99px;
  color: white;
  font-weight: 500;
  font-size: 14px;
  z-index: 99999;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.toast-success {
  background-color: #10B981;
}

.toast-error {
  background-color: #EF4444;
}

.toast-warning {
  background-color: #F59E0B;
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
