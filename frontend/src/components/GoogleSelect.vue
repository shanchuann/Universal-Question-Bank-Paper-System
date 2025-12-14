<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

interface Option {
  label: string
  value: string | number
}

const props = defineProps<{
  modelValue: string | number
  options: Option[]
  label?: string
  placeholder?: string
}>()

const emit = defineEmits(['update:modelValue'])

const isOpen = ref(false)
const containerRef = ref<HTMLElement | null>(null)

const selectedLabel = computed(() => {
  const option = props.options.find(o => o.value === props.modelValue)
  return option ? option.label : (props.placeholder || 'Select')
})

const toggleDropdown = () => {
  isOpen.value = !isOpen.value
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
      :class="{ 'is-open': isOpen, 'has-value': !!modelValue }"
      @click="toggleDropdown"
    >
      <span class="selected-text">{{ selectedLabel }}</span>
      <span class="arrow-icon">▼</span>
    </div>
    
    <transition name="fade">
      <div v-if="isOpen" class="options-list">
        <div 
          v-for="option in options" 
          :key="option.value"
          class="option-item"
          :class="{ 'is-selected': option.value === modelValue }"
          @click="selectOption(option.value)"
        >
          {{ option.label }}
          <span v-if="option.value === modelValue" class="check-icon">✓</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.google-select-container {
  position: relative;
  width: 100%;
  font-family: 'Roboto', 'Google Sans', sans-serif;
}

.select-label {
  display: block;
  font-size: 12px;
  color: #5f6368;
  margin-bottom: 4px;
  font-weight: 500;
}

.select-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background-color: #fff;
  border: 1px solid #dadce0;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 40px;
  box-sizing: border-box;
}

.select-trigger:hover {
  border-color: #202124;
}

.select-trigger.is-open {
  border-color: #1a73e8;
  border-width: 2px;
  padding: 9px 11px; /* Adjust for border width */
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}

.selected-text {
  font-size: 14px;
  color: #202124;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.arrow-icon {
  font-size: 10px;
  color: #5f6368;
  transition: transform 0.2s;
}

.select-trigger.is-open .arrow-icon {
  transform: rotate(180deg);
  color: #1a73e8;
}

.options-list {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background-color: #fff;
  border: 1px solid #dadce0;
  border-top: none;
  border-bottom-left-radius: 4px;
  border-bottom-right-radius: 4px;
  box-shadow: 0 4px 6px rgba(32, 33, 36, 0.28);
  z-index: 1000;
  max-height: 300px;
  overflow-y: auto;
  padding: 4px 0;
}

.option-item {
  padding: 10px 16px;
  font-size: 14px;
  color: #202124;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: background-color 0.1s;
}

.option-item:hover {
  background-color: #f1f3f4;
}

.option-item.is-selected {
  background-color: #e8f0fe;
  color: #1a73e8;
  font-weight: 500;
}

.check-icon {
  font-size: 14px;
  color: #1a73e8;
}

/* Scrollbar styling */
.options-list::-webkit-scrollbar {
  width: 8px;
}
.options-list::-webkit-scrollbar-track {
  background: #f1f1f1;
}
.options-list::-webkit-scrollbar-thumb {
  background: #dadce0;
  border-radius: 4px;
}
.options-list::-webkit-scrollbar-thumb:hover {
  background: #bdc1c6;
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
