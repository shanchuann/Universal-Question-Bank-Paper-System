<script setup lang="ts">
import { ref, watch } from 'vue'
import GoogleSelect from '@/components/GoogleSelect.vue'

interface KnowledgePoint {
  id?: string
  name: string
  level: string
  parentId: string
  sortOrder?: number
}

const props = defineProps<{
  isOpen: boolean
  initialName: string
  parentOptions: { id: string, label: string }[]
}>()

const emit = defineEmits(['close', 'save'])

const form = ref<KnowledgePoint>({
  name: '',
  level: 'POINT',
  parentId: '',
  sortOrder: 0
})

const levelOptions = [
  { value: 'CHAPTER', label: 'Chapter' },
  { value: 'SECTION', label: 'Section' },
  { value: 'POINT', label: 'Point' }
]

const parentSelectOptions = ref<Array<{ value: string, label: string }>>([])

watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    form.value.name = props.initialName
    form.value.level = 'POINT'
    form.value.parentId = ''
    form.value.sortOrder = 0
    parentSelectOptions.value = [
      { value: '', label: 'None (Top Level)' },
      ...props.parentOptions.map(opt => ({ value: opt.id, label: opt.label }))
    ]
  }
})

const handleSave = () => {
  emit('save', {
    ...form.value,
    parentId: form.value.parentId || undefined
  })
}
</script>

<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-card google-card">
      <div class="modal-header">
        <h2>Create Knowledge Point</h2>
        <button class="icon-btn" @click="$emit('close')">×</button>
      </div>
      
      <div class="modal-body">
        <div class="form-group">
          <label>Name</label>
          <input v-model="form.name" type="text" class="google-input" placeholder="e.g. Quadratic Formula" autofocus />
        </div>

        <div class="form-group">
          <label>Level</label>
          <GoogleSelect v-model="form.level" :options="levelOptions" placeholder="Select level" />
        </div>

        <div class="form-group">
          <label>Parent</label>
          <GoogleSelect v-model="form.parentId" :options="parentSelectOptions" placeholder="Select parent" />
        </div>
      </div>

      <div class="modal-actions">
        <button class="google-btn text-btn" @click="$emit('close')">Cancel</button>
        <button class="google-btn primary-btn" @click="handleSave">Create</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10010;
}

.modal-card {
  width: 400px;
  max-width: 90%;
  padding: 0;
  overflow: hidden;
  animation: slideUp 0.2s ease-out;
}

.modal-header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--line-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.modal-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.modal-actions {
  padding: 16px 24px;
  background-color: var(--line-bg-soft);
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: none;
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

/* Reuse styles from global or scoped */
.google-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--line-border);
  border-radius: 4px;
  font-size: 14px;
}
.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--line-text-secondary);
  font-weight: 500;
}
</style>

