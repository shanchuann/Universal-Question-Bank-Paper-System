<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ChevronDown, X, Plus, Search } from 'lucide-vue-next'

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
          <span class="chip-remove" @click.stop="removeOption(opt.value)">
            <X :size="14" />
          </span>
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
      <div class="combo-arrow" :class="{ 'is-open': isOpen }">
        <ChevronDown :size="20" />
      </div>
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
          <Plus :size="16" class="create-icon" />
          创建 "{{ searchQuery }}"
        </div>

        <div v-if="filteredOptions.length === 0 && !showCreateOption" class="no-results">
          <Search :size="16" class="search-icon" />
          未找到匹配选项
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.google-combobox-container {
  position: relative;
  width: 100%;
  font-family: inherit;
}

.combo-label {
  display: block;
  font-size: 14px;
  color: var(--line-text-primary);
  margin-bottom: 6px;
  font-weight: 500;
}

.combo-wrapper {
  min-height: 48px;
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-md);
  background-color: var(--line-bg);
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: text;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-sizing: border-box;
}

.combo-wrapper:hover {
  border-color: var(--line-primary-hover);
  background-color: var(--line-bg-hover);
}

.combo-wrapper.is-focused {
  border-color: var(--line-primary);
  background-color: var(--line-bg);
  box-shadow: 0 0 0 3px rgba(15, 23, 42, 0.12);
}

.chips-area {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.chip {
  background-color: var(--line-bg-soft);
  color: var(--line-text-primary);
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-full);
  padding: 4px 8px 4px 12px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 2px 0;
  transition: all 0.2s;
}

.chip:hover {
  border-color: var(--line-primary);
  background-color: var(--line-bg-hover);
}

.chip-remove {
  cursor: pointer;
  font-size: 16px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  color: var(--line-text-secondary);
  transition: all 0.2s;
}

.chip-remove:hover {
  background-color: var(--line-error);
  color: white;
}

.combo-input {
  border: none;
  outline: none;
  font-size: 15px;
  color: var(--line-text-primary);
  flex: 1;
  min-width: 80px;
  padding: 4px 0;
  background: transparent;
  font-family: inherit;
}

.combo-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--line-text-secondary);
  margin-left: 8px;
  cursor: pointer;
  transition: transform 0.2s ease, color 0.2s ease;
  flex-shrink: 0;
}

.combo-arrow.is-open {
  transform: rotate(180deg);
  color: var(--line-primary);
}

.options-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background-color: var(--line-bg-elevated);
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-md);
  box-shadow: var(--line-shadow-lg);
  z-index: var(--line-layer-dropdown);
  max-height: 250px;
  overflow-y: auto;
  padding: 6px;
  animation: slideDown 0.2s ease-out;
}

.dropdown-item {
  padding: 10px 12px;
  font-size: 14px;
  color: var(--line-text-primary);
  cursor: pointer;
  border-radius: var(--line-radius-sm);
  transition: background-color 0.1s;
}

.dropdown-item:hover {
  background-color: var(--line-bg-hover);
}

.create-item {
  color: var(--line-primary);
  font-weight: 500;
  border-top: none;
  margin-top: 4px;
  padding-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.create-item:hover {
  background-color: rgba(26, 115, 232, 0.1);
}

.create-icon {
  flex-shrink: 0;
}

.no-results {
  padding: 16px 12px;
  font-size: 14px;
  color: var(--line-text-secondary);
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.search-icon {
  opacity: 0.5;
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

