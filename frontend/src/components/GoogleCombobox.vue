<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'

interface Option {
  label: string
  value: string | number
}

const props = defineProps<{
  modelValue: (string | number)[]
  options: Option[]
  label?: string
  placeholder?: string
  allowCreate?: boolean
}>()

const emit = defineEmits(['update:modelValue', 'create'])

const searchQuery = ref('')
const isOpen = ref(false)
const containerRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)

const selectedOptions = computed(() => {
  return props.options.filter(opt => props.modelValue.includes(opt.value))
})

const filteredOptions = computed(() => {
  const query = searchQuery.value.toLowerCase()
  return props.options.filter(opt => 
    opt.label.toLowerCase().includes(query) && 
    !props.modelValue.includes(opt.value)
  )
})

const showCreateOption = computed(() => {
  if (!props.allowCreate || !searchQuery.value) return false
  const exists = props.options.some(opt => opt.label.toLowerCase() === searchQuery.value.toLowerCase())
  return !exists
})

const toggleDropdown = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    nextTick(() => inputRef.value?.focus())
  }
}

const selectOption = (option: Option) => {
  const newValue = [...props.modelValue, option.value]
  emit('update:modelValue', newValue)
  searchQuery.value = ''
  // Keep open for multiple selection or close? Google usually keeps open or clears input.
  // Let's keep focus on input
  inputRef.value?.focus()
}

const removeOption = (value: string | number) => {
  const newValue = props.modelValue.filter(v => v !== value)
  emit('update:modelValue', newValue)
}

const handleCreate = () => {
  emit('create', searchQuery.value)
  searchQuery.value = ''
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
  <div class="google-combobox-container" ref="containerRef">
    <label v-if="label" class="combo-label">{{ label }}</label>
    
    <div class="combo-wrapper" :class="{ 'is-focused': isOpen }" @click="toggleDropdown">
      <div class="chips-area">
        <div v-for="opt in selectedOptions" :key="opt.value" class="chip">
          <span>{{ opt.label }}</span>
          <span class="chip-remove" @click.stop="removeOption(opt.value)">×</span>
        </div>
        <input 
          ref="inputRef"
          v-model="searchQuery"
          type="text" 
          class="combo-input"
          :placeholder="selectedOptions.length === 0 ? (placeholder || 'Select...') : ''"
          @focus="isOpen = true"
        />
      </div>
      <div class="combo-arrow">▼</div>
    </div>

    <transition name="fade">
      <div v-if="isOpen" class="options-dropdown">
        <div 
          v-for="option in filteredOptions" 
          :key="option.value"
          class="dropdown-item"
          @click.stop="selectOption(option)"
        >
          {{ option.label }}
        </div>
        
        <div 
          v-if="showCreateOption" 
          class="dropdown-item create-item"
          @click.stop="handleCreate"
        >
          <span class="plus-icon">+</span> Create "{{ searchQuery }}"
        </div>

        <div v-if="filteredOptions.length === 0 && !showCreateOption" class="no-results">
          No options found
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.google-combobox-container {
  position: relative;
  width: 100%;
  font-family: 'Roboto', 'Google Sans', sans-serif;
}

.combo-label {
  display: block;
  font-size: 12px;
  color: #5f6368;
  margin-bottom: 4px;
  font-weight: 500;
}

.combo-wrapper {
  min-height: 40px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  background-color: #fff;
  display: flex;
  align-items: flex-start; /* Align top for multi-line chips */
  padding: 4px 12px; /* Changed from 4px 8px to match input/select */
  cursor: text;
  transition: all 0.2s;
  flex-wrap: wrap;
  box-sizing: border-box;
}

.combo-wrapper.is-focused {
  border-color: #1a73e8;
  border-width: 2px;
  padding: 3px 11px; /* Adjust for border width */
}

.chips-area {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.chip {
  background-color: #e8f0fe;
  color: #1a73e8;
  border-radius: 16px;
  padding: 4px 8px 4px 12px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 2px 0;
}

.chip-remove {
  cursor: pointer;
  font-size: 16px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
}

.chip-remove:hover {
  background-color: rgba(26, 115, 232, 0.1);
}

.combo-input {
  border: none;
  outline: none;
  font-size: 14px;
  color: #202124;
  flex: 1;
  min-width: 60px;
  padding: 6px 0;
  background: transparent;
}

.combo-arrow {
  font-size: 10px;
  color: #5f6368;
  padding: 10px 4px;
  cursor: pointer;
}

.options-dropdown {
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
  max-height: 250px;
  overflow-y: auto;
  margin-top: 1px;
}

.dropdown-item {
  padding: 10px 16px;
  font-size: 14px;
  color: #202124;
  cursor: pointer;
}

.dropdown-item:hover {
  background-color: #f1f3f4;
}

.create-item {
  color: #1a73e8;
  font-weight: 500;
  border-top: 1px solid #f1f3f4;
}

.no-results {
  padding: 10px 16px;
  font-size: 14px;
  color: #5f6368;
  font-style: italic;
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
