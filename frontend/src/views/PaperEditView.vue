<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import 'katex/dist/katex.min.css';
import katex from 'katex';
import { questionApi } from '@/api/client'

interface Question {
  id: string
  stem: string
  type: string
  difficulty: string
}

interface PaperItem {
  uuid: string;
  type: 'QUESTION' | 'SECTION';
  id?: string;
  data?: Question;
  sectionTitle?: string;
  score: number;
}

interface Paper {
  id: number
  title: string
  questions: Question[]
  items?: {
    type: 'QUESTION' | 'SECTION';
    id?: string;
    sectionTitle?: string;
    score?: number;
  }[]
}

const route = useRoute()
const router = useRouter()
const paper = ref<Paper | null>(null)
const items = ref<PaperItem[]>([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const showQuestionModal = ref(false)
const availableQuestions = ref<any[]>([])
const selectedQuestionIds = ref<Set<string>>(new Set())
const modalLoading = ref(false)
const modalPage = ref(0)
const modalSize = ref(5)
const modalTotalElements = ref(0)

const generateUuid = () => Math.random().toString(36).substring(2, 15)

const totalScore = computed(() => items.value.reduce((sum, item) => sum + (item.score || 0), 0))

const renderMath = () => {
  nextTick(() => {
    const formulas = document.querySelectorAll('.ql-formula');
    formulas.forEach((el) => {
      const latex = el.getAttribute('data-value');
      if (latex && !el.hasAttribute('data-rendered')) {
        try {
            katex.render(latex, el as HTMLElement, {
            throwOnError: false
            });
            el.setAttribute('data-rendered', 'true');
        } catch (e) {
            console.error(e);
        }
      }
    });
  });
}

onMounted(async () => {
  const id = route.params.id
  if (!id || id === 'undefined' || id === 'null') {
      error.value = 'Invalid Paper ID'
      loading.value = false
      return
  }
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get(`/api/papers/${id}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    paper.value = response.data
    
    if (paper.value) {
        if (paper.value.items && paper.value.items.length > 0) {
            items.value = paper.value.items.map(item => {
                const q = item.type === 'QUESTION' ? paper.value?.questions.find(q => q.id === item.id) : undefined;
                return {
                    uuid: generateUuid(),
                    type: item.type,
                    id: item.id,
                    data: q,
                    sectionTitle: item.sectionTitle,
                    score: item.score || 0
                };
            });
        } else {
            // Legacy fallback
            items.value = paper.value.questions.map(q => ({
                uuid: generateUuid(),
                type: 'QUESTION',
                id: q.id,
                data: q,
                score: 0 
            }));
        }
    }
    
    renderMath();
  } catch (err) {
    error.value = 'Failed to load paper.'
    console.error(err)
  } finally {
    loading.value = false
  }
})

const removeItem = (index: number) => {
  items.value.splice(index, 1);
};

const addSection = () => {
  items.value.push({
    uuid: generateUuid(),
    type: 'SECTION',
    sectionTitle: 'New Section',
    score: 0
  });
};

const openQuestionModal = async () => {
  showQuestionModal.value = true;
  selectedQuestionIds.value.clear();
  await fetchAvailableQuestions();
};

const closeQuestionModal = () => {
  showQuestionModal.value = false;
};

const fetchAvailableQuestions = async () => {
  modalLoading.value = true;
  try {
    const response = await questionApi.questionsGet(modalPage.value, modalSize.value);
    availableQuestions.value = response.data.content || [];
    modalTotalElements.value = response.data.totalElements || 0;
  } catch (err) {
    console.error('Failed to load questions', err);
  } finally {
    modalLoading.value = false;
  }
};

const toggleQuestionSelection = (id: string) => {
  if (selectedQuestionIds.value.has(id)) {
    selectedQuestionIds.value.delete(id);
  } else {
    selectedQuestionIds.value.add(id);
  }
};

const addSelectedQuestions = () => {
  const selectedQuestions = availableQuestions.value.filter(q => selectedQuestionIds.value.has(q.id));
  
  selectedQuestions.forEach(q => {
    items.value.push({
      uuid: generateUuid(),
      type: 'QUESTION',
      id: q.id,
      data: q,
      score: 5 // Default score
    });
  });
  
  closeQuestionModal();
};

const changeModalPage = (newPage: number) => {
  modalPage.value = newPage;
  fetchAvailableQuestions();
};

const stripHtml = (html: string | undefined) => {
  if (!html) return ''
  const tmp = document.createElement('DIV')
  tmp.innerHTML = html
  return tmp.textContent || tmp.innerText || ''
}

const savePaper = async () => {
  if (!paper.value) return
  saving.value = true
  try {
    const token = localStorage.getItem('token')
    
    const itemsPayload = items.value.map(item => ({
      type: item.type,
      id: item.id,
      sectionTitle: item.sectionTitle,
      score: item.score
    }));

    await axios.put(`/api/papers/${paper.value.id}`, {
      title: paper.value.title,
      items: itemsPayload
    }, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    router.push(`/papers/${paper.value.id}/preview`)
  } catch (err) {
    alert('Failed to save paper.')
    console.error(err)
  } finally {
    saving.value = false
  }
}

const cancelEdit = () => {
  if (paper.value) {
    router.push(`/papers/${paper.value.id}/preview`)
  }
}

// Drag and drop logic
const dragIndex = ref<number | null>(null);
const dragOverIndex = ref<number | null>(null);
const dropPosition = ref<'top' | 'bottom' | null>(null);

const onDragStart = (event: DragEvent, index: number) => {
  dragIndex.value = index;
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move';
    event.dataTransfer.dropEffect = 'move';
    const target = event.target as HTMLElement;
    const row = target.closest('.paper-item');
    if (row) event.dataTransfer.setDragImage(row, 0, 0);
  }
};

const onDragOver = (event: DragEvent, index: number) => {
  event.preventDefault();
  if (dragIndex.value === null || dragIndex.value === index) {
    dragOverIndex.value = null;
    dropPosition.value = null;
    return;
  }
  const target = event.currentTarget as HTMLElement;
  const rect = target.getBoundingClientRect();
  const offset = event.clientY - rect.top;
  if (offset < rect.height / 2) dropPosition.value = 'top';
  else dropPosition.value = 'bottom';
  dragOverIndex.value = index;
};

const onDrop = (event: DragEvent, index: number) => {
  event.preventDefault();
  if (dragIndex.value === null) return;
  const fromIndex = dragIndex.value;
  let toIndex = index;
  if (dropPosition.value === 'bottom') toIndex = index + 1;
  if (toIndex > fromIndex) toIndex--;
  const itemToMove = items.value[fromIndex];
  if (!itemToMove) return;
  items.value.splice(fromIndex, 1);
  items.value.splice(toIndex, 0, itemToMove);
  resetDragState();
};

const onDragEnd = () => resetDragState();
const resetDragState = () => {
  dragIndex.value = null;
  dragOverIndex.value = null;
  dropPosition.value = null;
};
</script>

<template>
  <div class="container">
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading paper...</p>
    </div>
    
    <div v-else-if="error" class="error-state google-card">
      <p>{{ error }}</p>
      <button @click="router.go(0)" class="google-btn">Try Again</button>
    </div>
    
    <div v-else-if="paper" class="google-card edit-card">
      <div class="card-header">
        <h1>Edit Paper</h1>
        <p class="subtitle">Modify paper details and questions</p>
      </div>
      
      <div class="form-group">
        <label class="field-label">Paper Title</label>
        <input v-model="paper.title" type="text" class="google-input" />
      </div>

      <div class="toolbar">
        <span class="total-score">Total Score: {{ totalScore }}</span>
        <div class="toolbar-actions">
          <button @click="openQuestionModal" class="google-btn text-btn">
            <span class="material-icon">+</span> Add Question
          </button>
          <button @click="addSection" class="google-btn text-btn">
            <span class="material-icon">+</span> Add Section Header
          </button>
        </div>
      </div>

      <div class="items-list">
          <div 
            v-for="(item, index) in items" 
            :key="item.uuid" 
            class="paper-item"
            :class="{ 
              'section-item': item.type === 'SECTION', 
              'dragging': dragIndex === index,
              'drop-target-top': dragOverIndex === index && dropPosition === 'top',
              'drop-target-bottom': dragOverIndex === index && dropPosition === 'bottom'
            }"
            @dragover="onDragOver($event, index)"
            @drop="onDrop($event, index)"
          >
            <div class="item-controls">
              <span 
                class="drag-handle" 
                title="Drag to reorder"
                draggable="true"
                @dragstart="onDragStart($event, index)"
                @dragend="onDragEnd"
              >⋮⋮</span>
              <button @click="removeItem(index)" class="control-btn delete-btn" title="Remove">×</button>
            </div>

            <div v-if="item.type === 'SECTION'" class="item-body section-body">
              <input v-model="item.sectionTitle" type="text" class="section-input" placeholder="Section Title (e.g. Part I)" />
            </div>

            <div v-else class="item-body question-body">
              <div class="question-preview" v-html="item.data?.stem"></div>
              <div class="item-meta">
                <span class="chip">{{ item.data?.type }}</span>
                <span class="chip">{{ item.data?.difficulty }}</span>
              </div>
              <div class="score-input-wrapper">
                <label>Score:</label>
                <input v-model.number="item.score" type="number" class="score-input" min="0" />
              </div>
            </div>
          </div>
      </div>

      <div class="form-actions">
        <button @click="cancelEdit" class="google-btn text-btn">Cancel</button>
        <button @click="savePaper" :disabled="saving" class="google-btn primary-btn">
          {{ saving ? 'Saving...' : 'Save Changes' }}
        </button>
      </div>
    </div>

    <!-- Add Question Modal -->
    <div v-if="showQuestionModal" class="modal-backdrop">
      <div class="question-modal">
        <div class="modal-header">
          <h2>Select Questions</h2>
          <button @click="closeQuestionModal" class="close-btn">×</button>
        </div>
        
        <div class="modal-content">
          <div class="search-bar">
            <input type="text" placeholder="Search questions..." class="google-input" />
          </div>
          
          <div v-if="modalLoading" class="loading-state">
            <div class="spinner"></div>
            <p>Loading questions...</p>
          </div>
          
          <div v-else-if="availableQuestions.length === 0" class="no-results">
            <p>No questions found.</p>
          </div>
          
          <div v-else class="questions-list">
            <div 
              v-for="question in availableQuestions" 
              :key="question.id" 
              class="question-item"
              @click="toggleQuestionSelection(question.id)"
              :class="{ 'selected': selectedQuestionIds.has(question.id) }"
            >
              <div class="question-info">
                <div class="question-stem" v-html="question.stem"></div>
                <div class="question-meta">
                  <span class="chip">{{ question.type }}</span>
                  <span class="chip">{{ question.difficulty }}</span>
                </div>
              </div>
              
              <div class="question-actions">
                <button 
                  @click.stop="toggleQuestionSelection(question.id)" 
                  class="select-btn"
                  :class="{ 'active': selectedQuestionIds.has(question.id) }"
                >
                  {{ selectedQuestionIds.has(question.id) ? 'Selected' : 'Select' }}
                </button>
              </div>
            </div>
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="addSelectedQuestions" class="google-btn primary-btn">
            Add Selected Questions
          </button>
        </div>

        <div class="pagination-controls">
          <button 
            @click="changeModalPage(modalPage - 1)" 
            class="pagination-btn"
            :disabled="modalPage === 0 || modalLoading"
          >
            Previous
          </button>
          <span class="page-info">
            Page {{ modalPage + 1 }} of {{ Math.ceil(modalTotalElements / modalSize) }}
          </span>
          <button 
            @click="changeModalPage(modalPage + 1)" 
            class="pagination-btn"
            :disabled="modalLoading || modalPage >= Math.ceil(modalTotalElements / modalSize) - 1"
          >
            Next
          </button>
        </div>
      </div>
    </div>
    <div v-if="showQuestionModal" class="modal-overlay">
      <div class="modal-card google-card">
        <div class="modal-header">
          <h2>Select Questions</h2>
          <button @click="closeQuestionModal" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <div v-if="modalLoading" class="loading-state">Loading questions...</div>
          <div v-else-if="availableQuestions.length === 0" class="empty-state">No questions found.</div>
          <div v-else class="question-selection-list">
            <div v-for="q in availableQuestions" :key="q.id" class="question-select-item" @click="toggleQuestionSelection(q.id)">
              <div class="checkbox-wrapper">
                <input type="checkbox" :checked="selectedQuestionIds.has(q.id)" readonly />
              </div>
              <div class="question-content">
                <div class="q-stem" v-html="stripHtml(q.stem)"></div>
                <div class="q-meta">
                  <span class="chip">{{ q.type }}</span>
                  <span class="chip">{{ q.difficulty }}</span>
                </div>
              </div>
            </div>
          </div>
          
          <div class="pagination-controls" v-if="modalTotalElements > modalSize">
            <button :disabled="modalPage === 0" @click="changeModalPage(modalPage - 1)" class="google-btn text-btn">Previous</button>
            <span>Page {{ modalPage + 1 }}</span>
            <button :disabled="(modalPage + 1) * modalSize >= modalTotalElements" @click="changeModalPage(modalPage + 1)" class="google-btn text-btn">Next</button>
          </div>
        </div>
        
        <div class="modal-actions">
          <button @click="closeQuestionModal" class="google-btn text-btn">Cancel</button>
          <button @click="addSelectedQuestions" class="google-btn primary-btn" :disabled="selectedQuestionIds.size === 0">
            Add {{ selectedQuestionIds.size }} Questions
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.edit-card {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px;
  border-top: 8px solid #1a73e8;
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.card-header h1 {
  font-family: 'Google Sans', sans-serif;
  font-size: 28px;
  font-weight: 400;
  margin-bottom: 8px;
  color: #202124;
}

.subtitle {
  color: #5f6368;
  font-size: 16px;
}

.form-group {
  margin-bottom: 24px;
}

.field-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #202124;
  margin-bottom: 8px;
}

.google-input {
  width: 100%;
  padding: 10px 12px;
  font-size: 16px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  transition: border-color 0.2s;
}

.google-input:focus {
  border-color: #1a73e8;
  outline: none;
  border-width: 2px;
  padding: 9px 11px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #dadce0;
}

.total-score {
  font-weight: 500;
  color: #1a73e8;
  font-size: 16px;
}

.items-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 32px;
}

.paper-item {
  border: 1px solid #dadce0;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
  position: relative;
  transition: box-shadow 0.2s;
}

.paper-item:hover {
  box-shadow: 0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24);
}

.section-item {
  background-color: #f8f9fa;
  border-left: 4px solid #1a73e8;
}

.item-controls {
  position: absolute;
  right: 8px;
  top: 8px;
  display: flex;
  gap: 4px;
}

.control-btn {
  background: transparent;
  border: none;
  color: #5f6368;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.control-btn:hover:not(:disabled) {
  background-color: #f1f3f4;
  color: #202124;
}

.delete-btn:hover {
  background-color: #fce8e6;
  color: #d93025;
}

.section-input {
  width: 100%;
  border: none;
  background: transparent;
  font-size: 18px;
  font-weight: 500;
  color: #202124;
  border-bottom: 1px solid transparent;
  padding: 4px 0;
}

.section-input:focus {
  outline: none;
  border-bottom-color: #1a73e8;
}

.question-preview {
  font-size: 14px;
  color: #202124;
  margin-bottom: 12px;
  padding-right: 80px;
}

.item-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.chip {
  background-color: #f1f3f4;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  color: #5f6368;
}

.score-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #5f6368;
}

.score-input {
  width: 60px;
  padding: 4px 8px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  text-align: right;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #dadce0;
}

.loading-state, .error-state {
  text-align: center;
  padding: 40px;
  color: #5f6368;
}

.google-btn {
  border: none;
  border-radius: 4px;
  padding: 8px 24px;
  font-family: 'Google Sans', sans-serif;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.primary-btn {
  background-color: #1a73e8;
  color: white;
}

.primary-btn:hover {
  background-color: #1557b0;
  box-shadow: 0 1px 2px 0 rgba(60,64,67,0.3), 0 1px 3px 1px rgba(60,64,67,0.15);
}

.text-btn {
  background-color: transparent;
  color: #1a73e8;
}

.text-btn:hover {
  background-color: #f6fafe;
}

.drag-handle {
  cursor: grab;
  font-size: 20px;
  color: #9aa0a6;
  margin-right: 8px;
  user-select: none;
}

.drag-handle:active {
  cursor: grabbing;
}

.paper-item.dragging {
  opacity: 0.5;
  background: #f1f3f4;
  border: 2px dashed #1a73e8;
}

.drop-target-top {
  border-top: 2px solid #1a73e8;
  margin-top: -2px;
}

.drop-target-bottom {
  border-bottom: 2px solid #1a73e8;
  margin-bottom: -2px;
}

.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.question-modal {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  width: 90%;
  max-width: 600px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.modal-header {
  background: #f1f3f4;
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h2 {
  font-size: 18px;
  font-weight: 500;
  margin: 0;
  color: #202124;
}

.close-btn {
  background: transparent;
  border: none;
  color: #5f6368;
  font-size: 18px;
  cursor: pointer;
}

.search-bar {
  padding: 16px;
  border-bottom: 1px solid #dadce0;
}

.questions-list {
  max-height: 400px;
  overflow-y: auto;
  padding: 8px 0;
}

.question-item {
  padding: 12px 16px;
  border-bottom: 1px solid #dadce0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: background 0.2s;
}

.question-item:hover {
  background: #f1f3f4;
}

.question-info {
  flex: 1;
}

.question-stem {
  font-size: 14px;
  color: #202124;
  margin-bottom: 4px;
}

.question-meta {
  display: flex;
  gap: 8px;
}

.select-btn {
  background: #1a73e8;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.select-btn:hover {
  background: #1557b0;
}

.selected {
  background: #e8f0fe;
  color: #1a73e8;
}

.modal-footer {
  padding: 16px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #dadce0;
}

.pagination-controls {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #dadce0;
}

.pagination-btn {
  background: #f1f3f4;
  color: #202124;
  border: none;
  border-radius: 4px;
  padding: 8px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.pagination-btn:hover {
  background: #e8f0fe;
}

.page-info {
  font-size: 14px;
  color: #5f6368;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-card {
  width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.modal-header {
  padding: 24px;
  border-bottom: 1px solid #dadce0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 400;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #5f6368;
}

.modal-body {
  padding: 0;
  overflow-y: auto;
  flex: 1;
}

.question-selection-list {
  display: flex;
  flex-direction: column;
}

.question-select-item {
  display: flex;
  padding: 16px 24px;
  border-bottom: 1px solid #f1f3f4;
  cursor: pointer;
  align-items: flex-start;
  gap: 16px;
}

.question-select-item:hover {
  background-color: #f8f9fa;
}

.checkbox-wrapper input {
  width: 18px;
  height: 18px;
  margin-top: 4px;
}

.question-content {
  flex: 1;
}

.q-stem {
  margin-bottom: 8px;
  color: #202124;
  font-size: 14px;
  line-height: 1.5;
}

.q-meta {
  display: flex;
  gap: 8px;
}

.modal-actions {
  padding: 16px 24px;
  border-top: 1px solid #dadce0;
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  background: #fff;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}
</style>
