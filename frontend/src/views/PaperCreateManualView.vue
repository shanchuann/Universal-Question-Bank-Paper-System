<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useBasket } from '@/stores/basket'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { questionApi } from '@/api/client'

const { basket, clearBasket } = useBasket()
const router = useRouter()
const title = ref('手动组卷')
const loading = ref(false)
const error = ref('')
const debugMsg = ref('')

interface PaperItem {
  uuid: string;
  type: 'QUESTION' | 'SECTION';
  id?: string;
  data?: any;
  sectionTitle?: string;
  score: number;
}

const items = ref<PaperItem[]>([])

const generateUuid = () => Math.random().toString(36).substring(2, 15)

const totalScore = computed(() => items.value.reduce((sum, item) => sum + (item.score || 0), 0))

const createPaper = async () => {
  if (items.value.length === 0) return;

  loading.value = true
  error.value = ''
  try {
    const token = localStorage.getItem('token')
    const itemsPayload = items.value.map(item => ({
      type: item.type,
      id: item.id,
      sectionTitle: item.sectionTitle,
      score: item.score
    }));

    const response = await axios.post('/api/papers/manual', {
      title: title.value,
      items: itemsPayload
    }, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    
    clearBasket()
    router.push(`/papers/${response.data.id}/preview`)
  } catch (err) {
    error.value = '创建试卷失败。'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const loadQuestions = async () => {
  if (basket.value.length === 0) {
    debugMsg.value = '题篮为空'
    return;
  }
  
  loading.value = true;
  error.value = '';
  try {
    const promises = basket.value.map(id => 
      questionApi.apiQuestionsQuestionIdGet(id)
        .then((res: any) => ({
          uuid: generateUuid(),
          type: 'QUESTION' as const,
          id: res.data.id,
          data: res.data,
          score: 5
        }))
        .catch((err: any) => {
          console.error(`Failed to load question ${id}`, err);
          return null;
        })
    );
    
    const results = await Promise.all(promises);
    items.value = results.filter((i: any) => i !== null) as PaperItem[];
    
    if (items.value.length === 0 && basket.value.length > 0) {
      error.value = '加载选中的题目失败，请重试。';
    }
    
  } catch (err) {
    console.error(err);
    error.value = '加载题目失败。';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadQuestions()
});

const removeItem = (index: number) => {
  items.value.splice(index, 1);
};

const addSection = () => {
  items.value.push({
    uuid: generateUuid(),
    type: 'SECTION',
    sectionTitle: '新分区',
    score: 0
  });
};

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
    if (row) {
       event.dataTransfer.setDragImage(row, 0, 0);
    }
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
  
  if (offset < rect.height / 2) {
    dropPosition.value = 'top';
  } else {
    dropPosition.value = 'bottom';
  }
  dragOverIndex.value = index;
};

const onDrop = (event: DragEvent, index: number) => {
  event.preventDefault();
  if (dragIndex.value === null) return;
  
  const fromIndex = dragIndex.value;
  let toIndex = index;
  
  if (dropPosition.value === 'bottom') {
    toIndex = index + 1;
  }
  
  // Adjust index if moving downwards because removal shifts indices
  if (toIndex > fromIndex) {
    toIndex--;
  }
  
  const itemToMove = items.value[fromIndex];
  if (!itemToMove) return;
  items.value.splice(fromIndex, 1);
  items.value.splice(toIndex, 0, itemToMove);
  
  resetDragState();
};

const onDragEnd = () => {
  resetDragState();
};

const resetDragState = () => {
  dragIndex.value = null;
  dragOverIndex.value = null;
  dropPosition.value = null;
};
</script>

<template>
  <div class="container">
    <div class="google-card form-card">
      <div class="card-header">
        <h1>手动组卷</h1>
        <p class="subtitle">排列题目、设置分数、组织分区。</p>
      </div>

      <div class="form-group">
        <label class="field-label">试卷标题</label>
        <input v-model="title" type="text" class="google-input" placeholder="如：期中考试" />
      </div>

      <div class="toolbar">
        <span class="total-score">总分: {{ totalScore }}</span>
        <button @click="addSection" class="google-btn text-btn">
          添加分区标题
        </button>
      </div>

      <div class="paper-content">
        <div v-if="loading" class="loading-state">正在加载题目...</div>
        
        <div v-else-if="items.length === 0" class="empty-state">
          未选择题目。请先到题库中添加题目。
        </div>

        <div v-else class="items-list">
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
                title="拖动排序"
                draggable="true"
                @dragstart="onDragStart($event, index)"
                @dragend="onDragEnd"
              >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="12" r="1"></circle><circle cx="9" cy="5" r="1"></circle><circle cx="9" cy="19" r="1"></circle><circle cx="15" cy="12" r="1"></circle><circle cx="15" cy="5" r="1"></circle><circle cx="15" cy="19" r="1"></circle></svg>
              </span>
              <button @click="removeItem(index)" class="control-btn delete-btn" title="移除">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
              </button>
            </div>

            <div v-if="item.type === 'SECTION'" class="item-body section-body">
              <input v-model="item.sectionTitle" type="text" class="section-input" placeholder="分区标题（如：第一部分）" />
            </div>

            <div v-else class="item-body question-body">
              <div class="question-preview" v-html="item.data?.stem"></div>
              <div class="item-meta">
                <span class="chip">{{ item.data?.type }}</span>
                <span class="chip">{{ item.data?.difficulty }}</span>
              </div>
              <div class="score-input-wrapper">
                <label>分数:</label>
                <input v-model.number="item.score" type="number" class="score-input" min="0" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="form-actions">
        <button @click="createPaper" :disabled="loading || items.length === 0" class="google-btn primary-btn full-width">
          创建试卷
        </button>
      </div>
      
      <div v-if="error" class="message error">{{ error }}</div>
    </div>
  </div>
</template>

<style scoped>
.form-card {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px;
  /* border-top: 8px solid #1a73e8; Google Forms style top border */
}

.card-header {
  margin-bottom: 32px;
}

h1 {
  font-family: 'Google Sans', sans-serif;
  font-size: 32px;
  font-weight: 400;
  color: #202124;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 14px;
  color: #5f6368;
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
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-item .item-controls {
  position: static;
  margin-left: 12px;
  order: 1;
}

.item-controls {
  position: absolute;
  right: 8px;
  top: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.section-item .item-body {
  flex: 1;
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

.control-btn:disabled {
  color: #dadce0;
  cursor: default;
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
  padding-right: 80px; /* Space for controls */
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

.google-btn {
  border: none;
  border-radius: 4px;
  padding: 8px 24px;
  font-family: 'Google Sans', sans-serif;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
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

.full-width {
  width: 100%;
  padding: 12px;
  font-size: 16px;
}

.error {
  color: #d93025;
  margin-top: 16px;
  font-size: 14px;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #5f6368;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px dashed #dadce0;
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
  position: relative;
}

.drop-target-top::before {
  content: "";
  position: absolute;
  top: -10px;
  left: 0;
  right: 0;
  height: 4px;
  background: #1a73e8;
  border-radius: 2px;
  pointer-events: none;
  z-index: 10;
}

.drop-target-bottom {
  position: relative;
}

.drop-target-bottom::after {
  content: "";
  position: absolute;
  bottom: -10px;
  left: 0;
  right: 0;
  height: 4px;
  background: #1a73e8;
  border-radius: 2px;
  pointer-events: none;
  z-index: 10;
}
</style>
