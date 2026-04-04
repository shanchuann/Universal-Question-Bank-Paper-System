<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ChevronDown, Check } from 'lucide-vue-next'

interface Option {
  label: string
  value: string | number
}

const props = defineProps<{
  modelValue: string | number
  options: Option[]
  label?: string
  placeholder?: string
  disabled?: boolean
}>()

const emit = defineEmits(['update:modelValue'])

const isOpen = ref(false)
const containerRef = ref<HTMLElement | null>(null)
const alignRight = ref(false)

const selectedLabel = computed(() => {
  const option = props.options.find(o => o.value === props.modelValue)
  return option ? option.label : (props.placeholder || 'Select')
})

const toggleDropdown = () => {
  if (props.disabled) return
  if (!isOpen.value) {
    updateDropdownAlignment()
  }
  isOpen.value = !isOpen.value
}

const updateDropdownAlignment = () => {
  if (!containerRef.value) return
  const rect = containerRef.value.getBoundingClientRect()
  const dropdownWidth = Math.max(rect.width, 180)
  alignRight.value = rect.left + dropdownWidth > window.innerWidth - 8
}

const selectOption = (value: string | number) => {
  emit('update:modelValue', value)
  isOpen.value = false
}

const handleClickOutside = (event: MouseEvent) => {
  if (containerRef.value && !containerRef.value.contains(event.target as Node)) {
    isOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="google-select-container" ref="containerRef">
    <label v-if="label" class="select-label">{{ label }}</label>
    <div 
      class="select-trigger" 
      :class="{ 'is-open': isOpen, 'has-value': !!modelValue, 'is-disabled': disabled }"
      @click="toggleDropdown"
    >
      <span class="selected-text">{{ selectedLabel }}</span>
      <span class="arrow-icon">
        <ChevronDown :size="20" />
      </span>
    </div>
    
    <transition name="fade">
      <div v-if="isOpen" class="options-list" :class="{ 'align-right': alignRight }">
        <div 
          v-for="option in options" 
          :key="option.value"
          class="option-item"
          :class="{ 'is-selected': option.value === modelValue }"
          @click="selectOption(option.value)"
        >
          {{ option.label }}
          <span v-if="option.value === modelValue" class="check-icon">
            <Check :size="18" />
          </span>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.google-select-container {
  position: relative;
  width: 100%;
  font-family: inherit;
}

.select-label {
  display: block;
  font-size: 14px;
  color: var(--line-text-primary);
  margin-bottom: 6px;
  font-weight: 500;
}

.select-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background-color: var(--line-bg);
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-md);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  min-height: 48px;
  box-sizing: border-box;
  color: var(--line-text-primary);
}

.select-trigger:hover {
  border-color: var(--line-primary-hover);
  background-color: var(--line-bg-hover);
}

.select-trigger.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
  pointer-events: none;
  background-color: var(--line-bg-soft);
}

.select-trigger.is-open {
  border-color: var(--line-primary);
  box-shadow: 0 0 0 3px rgba(15, 23, 42, 0.12);
  background-color: var(--line-bg);
}

.selected-text {
  font-size: 15px;
  color: var(--line-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.arrow-icon {
  display: flex;
  align-items: center;
  color: var(--line-text-secondary);
  transition: transform 0.2s;
}

.select-trigger.is-open .arrow-icon {
  transform: rotate(180deg);
  color: var(--line-primary);
}

.options-list {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: auto;
  background-color: var(--line-bg-elevated);
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-md);
  box-shadow: var(--line-shadow-lg);
  z-index: var(--line-layer-dropdown);
  max-height: 300px;
  overflow-y: auto;
  padding: 6px;
  animation: slideDown 0.2s ease-out;
  min-width: max(100%, 180px);
}

.options-list.align-right {
  left: auto;
  right: 0;
}

.option-item {
  padding: 10px 12px;
  font-size: 14px;
  color: var(--line-text-primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: var(--line-radius-sm);
  transition: background-color 0.1s;
  gap: 10px;
  white-space: nowrap;
}

.option-item:hover {
  background-color: var(--line-bg-hover);
}

.option-item.is-selected {
  background-color: rgba(15, 23, 42, 0.08);
  color: var(--line-primary);
  font-weight: 600;
}

.check-icon {
  color: var(--line-primary);
  display: flex;
  align-items: center;
}

/* Scrollbar styling */
.options-list::-webkit-scrollbar {
  width: 6px;
}
.options-list::-webkit-scrollbar-track {
  background: transparent;
}
.options-list::-webkit-scrollbar-thumb {
  background: var(--line-border);
  border-radius: 4px;
}
.options-list::-webkit-scrollbar-thumb:hover {
  background: var(--line-text-secondary);
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
