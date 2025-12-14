<script setup lang="ts">
import { ref, watch, computed } from 'vue'

interface KnowledgePoint {
  id?: string
  name: string
  level: string
  parentId?: string
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

watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    form.value.name = props.initialName
    form.value.level = 'POINT'
    form.value.parentId = ''
    form.value.sortOrder = 0
  }
})

const handleSave = () => {
  emit('save', { ...form.value })
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
          <select v-model="form.level" class="google-select">
            <option value="CHAPTER">Chapter</option>
            <option value="SECTION">Section</option>
            <option value="POINT">Point</option>
          </select>
        </div>

        <div class="form-group">
          <label>Parent</label>
          <select v-model="form.parentId" class="google-select">
            <option value="">None (Top Level)</option>
            <option v-for="opt in parentOptions" :key="opt.id" :value="opt.id">
              {{ opt.label }}
            </option>
          </select>
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
  z-index: 2000;
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
  border-bottom: 1px solid #dadce0;
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
  background-color: #f8f9fa;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid #dadce0;
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

/* Reuse styles from global or scoped */
.google-input, .google-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  font-size: 14px;
}
.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: #5f6368;
  font-weight: 500;
}
</style>
