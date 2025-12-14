<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import axios from 'axios'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import 'katex/dist/katex.min.css';
import katex from 'katex';
import GoogleSelect from '@/components/GoogleSelect.vue'
import GoogleCombobox from '@/components/GoogleCombobox.vue'
import KnowledgePointDialog from '@/components/KnowledgePointDialog.vue'

// Expose katex to window for Quill's formula module
(window as any).katex = katex;

const form = ref({
  subjectId: '',
  type: 'SINGLE_CHOICE',
  difficulty: 'EASY',
  stem: '',
  options: ['', '', '', ''],
  answer: '',
  tags: '',
  knowledgePointIds: [] as string[]
})

const knowledgePoints = ref<any[]>([])
const flatPoints = ref<any[]>([])
const showKPDialog = ref(false)
const newKPName = ref('')

const typeOptions = [
  { label: 'Single Choice', value: 'SINGLE_CHOICE' },
  { label: 'Multiple Choice', value: 'MULTIPLE_CHOICE' },
  { label: 'True/False', value: 'TRUE_FALSE' },
  { label: 'Fill Blank', value: 'FILL_BLANK' },
  { label: 'Short Answer', value: 'SHORT_ANSWER' }
]

const difficultyOptions = [
  { label: 'Easy', value: 'EASY' },
  { label: 'Medium', value: 'MEDIUM' },
  { label: 'Hard', value: 'HARD' }
]

const kpOptions = computed(() => {
  return flatPoints.value.map(p => ({
    label: p.name,
    value: p.id
  }))
})

const parentOptions = computed(() => {
  // Filter for potential parents (Chapters and Sections)
  return knowledgePoints.value
    .filter((p: any) => ['CHAPTER', 'SECTION'].includes(p.level))
    .map((p: any) => ({ id: p.id, label: p.name }))
})

const fetchKnowledgePoints = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/knowledge-points', {
      headers: { Authorization: `Bearer ${token}` }
    })
    knowledgePoints.value = response.data
    
    // Flatten for select
    const flat: any[] = []
    const map = new Map(response.data.map((p: any) => [p.id, p]))
    
    const buildPath = (p: any): string => {
      if (p.parentId && map.has(p.parentId)) {
        return buildPath(map.get(p.parentId)) + ' > ' + p.name
      }
      return p.name
    }
    
    response.data.forEach((p: any) => {
      if (p.level === 'POINT') {
        flat.push({
          id: p.id,
          name: buildPath(p)
        })
      }
    })
    flatPoints.value = flat
  } catch (err) {
    console.error(err)
  }
}

const handleCreateKP = (name: string) => {
  newKPName.value = name
  showKPDialog.value = true
}

const saveNewKP = async (kpData: any) => {
  try {
    const token = localStorage.getItem('token')
    const res = await axios.post('/api/knowledge-points', {
      ...kpData,
      subjectId: form.value.subjectId || 'general'
    }, {
      headers: { Authorization: `Bearer ${token}` }
    })
    
    // Refresh list
    await fetchKnowledgePoints()
    
    // Add new ID to selection
    if (res.data && res.data.id) {
      form.value.knowledgePointIds.push(res.data.id)
    }
    
    showKPDialog.value = false
  } catch (err) {
    console.error('Failed to create KP', err)
    alert('Failed to create knowledge point')
  }
}

onMounted(fetchKnowledgePoints)


const loading = ref(false)
const message = ref('')
const error = ref('')
const quillEditor = ref<any>(null)

const toolbarOptions = [
  ['bold', 'italic', 'underline', 'strike'],
  ['blockquote', 'code-block'],
  [{ 'header': 1 }, { 'header': 2 }],
  [{ 'list': 'ordered'}, { 'list': 'bullet' }],
  [{ 'script': 'sub'}, { 'script': 'super' }],
  [{ 'indent': '-1'}, { 'indent': '+1' }],
  [{ 'direction': 'rtl' }],
  [{ 'size': ['small', false, 'large', 'huge'] }],
  [{ 'header': [1, 2, 3, 4, 5, 6, false] }],
  [{ 'color': [] }, { 'background': [] }],
  [{ 'font': [] }],
  [{ 'align': [] }],
  ['clean'],
  ['link', 'image', 'video', 'formula']
];

const imageHandler = () => {
  const input = document.createElement('input');
  input.setAttribute('type', 'file');
  input.setAttribute('accept', 'image/*');
  input.click();

  input.onchange = async () => {
    const file = input.files ? input.files[0] : null;
    if (file) {
      const formData = new FormData();
      formData.append('file', file);

      try {
        const res = await axios.post('/api/files/upload', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
        const url = res.data.fileUrl;
        
        // Insert image into editor
        if (quillEditor.value) {
            const quill = quillEditor.value.getQuill();
            const range = quill.getSelection();
            quill.insertEmbed(range ? range.index : 0, 'image', url);
        }
      } catch (err) {
        console.error('Image upload failed', err);
        alert('Image upload failed');
      }
    }
  };
};

const onEditorReady = (quill: any) => {
  const toolbar = quill.getModule('toolbar');
  toolbar.addHandler('image', imageHandler);
};

const addOption = () => {
  form.value.options.push('')
}

const removeOption = (index: number) => {
  form.value.options.splice(index, 1)
}

const insertBlank = () => {
  if (quillEditor.value) {
    const quill = quillEditor.value.getQuill();
    const range = quill.getSelection(true);
    quill.insertText(range.index, ' _____ ');
    quill.setSelection(range.index + 7);
  }
}

watch(() => form.value.stem, (newVal) => {
  if (form.value.type === 'FILL_BLANK') {
    const matches = newVal.match(/_____/g);
    const count = matches ? matches.length : 0;
    // Adjust options array size
    if (count > form.value.options.length) {
        for (let i = form.value.options.length; i < count; i++) {
            form.value.options.push('');
        }
    } else if (count < form.value.options.length) {
        form.value.options.splice(count);
    }
  }
});

const handleSubmit = async () => {
  loading.value = true
  message.value = ''
  error.value = ''

  try {
    let apiOptions: { text: string; isCorrect: boolean }[] = [];

    if (['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(form.value.type)) {
        apiOptions = form.value.options.map(optText => ({
          text: optText,
          isCorrect: optText === form.value.answer
        }));
    } else if (form.value.type === 'TRUE_FALSE') {
        apiOptions = [
            { text: 'True', isCorrect: form.value.answer === 'True' },
            { text: 'False', isCorrect: form.value.answer === 'False' }
        ];
    } else if (form.value.type === 'FILL_BLANK') {
        apiOptions = form.value.options.map(optText => ({
            text: optText,
            isCorrect: true
        }));
    } else if (form.value.type === 'SHORT_ANSWER') {
         apiOptions = [{ text: form.value.answer, isCorrect: true }];
    }

    // Construct payload strictly matching backend expectations
    const payload = {
      subjectId: form.value.subjectId,
      type: form.value.type,
      difficulty: form.value.difficulty,
      stem: form.value.stem,
      options: apiOptions,
      answerSchema: {}, // Required by backend
      score: 1.0, // Required by backend
      tags: form.value.tags ? form.value.tags.split(',').map(t => t.trim()) : [],
      knowledgePointIds: form.value.knowledgePointIds
    }
    const token = localStorage.getItem('token')
    await axios.post('/api/questions', payload, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    message.value = 'Question added successfully!'
    // Reset form
    form.value.stem = ''
    form.value.options = ['', '', '', '']
    form.value.answer = ''
    form.value.tags = ''
    form.value.knowledgePointIds = []
  } catch (err: any) {
    error.value = err.response?.data?.message || err.message || 'Failed to add question.'
    console.error(err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="container add-question-container">
    <div class="google-card form-card">
      <div class="card-header">
        <h1>Add New Question</h1>
        <p class="subtitle">Create a new question for the bank</p>
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="form-grid">
          <div class="form-group">
            <label>Subject</label>
            <input v-model="form.subjectId" type="text" required placeholder="e.g. Math" class="google-input" />
          </div>

          <div class="form-group">
            <label>Type</label>
            <GoogleSelect
              v-model="form.type"
              :options="typeOptions"
            />
          </div>

          <div class="form-group">
            <label>Difficulty</label>
            <GoogleSelect
              v-model="form.difficulty"
              :options="difficultyOptions"
            />
          </div>

          <div class="form-group">
            <label>Knowledge Points</label>
            <GoogleCombobox
              v-model="form.knowledgePointIds"
              :options="kpOptions"
              placeholder="Select or create knowledge points..."
              :allow-create="true"
              @create="handleCreateKP"
            />
            <small class="helper-text">Type to search. If not found, click 'Create' to add new.</small>
          </div>
        </div>

        <div class="form-group full-width">
          <label>Question Stem</label>
          <div class="editor-wrapper">
            <QuillEditor 
              ref="quillEditor"
              theme="snow" 
              v-model:content="form.stem" 
              contentType="html" 
              :toolbar="toolbarOptions"
              @ready="onEditorReady"
            />
          </div>
          <button v-if="form.type === 'FILL_BLANK'" type="button" @click="insertBlank" class="google-btn text-btn small-btn mt-2">
            Insert Blank (_____)
          </button>
        </div>

        <!-- Options for Choice Questions -->
        <div v-if="['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(form.type)" class="options-section">
          <label>Options</label>
          <div v-for="(opt, idx) in form.options" :key="idx" class="option-row">
            <span class="option-label">{{ String.fromCharCode(65 + idx) }}.</span>
            <input v-model="form.options[idx]" type="text" required placeholder="Option text" class="google-input" />
            <button type="button" @click="removeOption(idx)" class="icon-btn remove-opt" v-if="form.options.length > 2">×</button>
          </div>
          <button type="button" @click="addOption" class="google-btn text-btn small-btn">+ Add Option</button>
        </div>

        <!-- Fill Blank Answers -->
        <div v-if="form.type === 'FILL_BLANK'" class="options-section">
           <label>Correct Answers (in order)</label>
           <div v-for="(opt, idx) in form.options" :key="idx" class="option-row">
              <span class="option-label">{{ idx + 1 }}.</span>
              <input v-model="form.options[idx]" type="text" required placeholder="Answer for blank" class="google-input" />
           </div>
           <p v-if="form.options.length === 0" class="helper-text">Type "_____" in the stem to add blanks.</p>
        </div>

        <div class="form-group full-width">
          <label>Correct Answer</label>
          
          <!-- Single Choice Answer -->
          <div v-if="form.type === 'SINGLE_CHOICE'" class="radio-group">
            <label v-for="(opt, idx) in form.options" :key="idx" class="radio-label">
              <input type="radio" :value="form.options[idx]" v-model="form.answer" class="google-radio" />
              <span class="radio-text">{{ String.fromCharCode(65 + idx) }}</span>
            </label>
          </div>

          <!-- Multiple Choice Answer -->
          <div v-else-if="form.type === 'MULTIPLE_CHOICE'" class="checkbox-group">
            <label v-for="(opt, idx) in form.options" :key="idx" class="checkbox-label">
              <input type="checkbox" :value="form.options[idx]" v-model="multiAnswer" class="google-checkbox" />
              <span class="checkbox-text">{{ String.fromCharCode(65 + idx) }}</span>
            </label>
          </div>

          <!-- True/False Answer -->
          <div v-else-if="form.type === 'TRUE_FALSE'" class="radio-group">
            <label class="radio-label">
              <input type="radio" value="True" v-model="form.answer" class="google-radio" />
              <span class="radio-text">True</span>
            </label>
            <label class="radio-label">
              <input type="radio" value="False" v-model="form.answer" class="google-radio" />
              <span class="radio-text">False</span>
            </label>
          </div>

          <!-- Fill Blank (handled above) -->
          <div v-else-if="form.type === 'FILL_BLANK'">
             <!-- Answers are input in the options section above -->
          </div>

          <!-- Text Answer -->
          <input v-else v-model="form.answer" type="text" required placeholder="Correct answer text" class="google-input" />
        </div>

        <div class="form-group full-width">
          <label>Tags (comma separated)</label>
          <input v-model="form.tags" type="text" placeholder="math, algebra, year-1" class="google-input" />
        </div>

        <div class="form-actions">
          <button type="submit" class="google-btn primary-btn" :disabled="loading">
            {{ loading ? 'Saving...' : 'Create Question' }}
          </button>
          <button type="button" @click="$router.back()" class="google-btn text-btn">Cancel</button>
        </div>

        <p v-if="error" class="error-text">{{ error }}</p>
        <p v-if="message" class="success-text">{{ message }}</p>
      </form>
    </div>
    <KnowledgePointDialog
      :is-open="showKPDialog"
      :initial-name="newKPName"
      :parent-options="parentOptions"
      @close="showKPDialog = false"
      @save="saveNewKP"
    />
  </div>
</template>

<style scoped>
.add-question-container {
  max-width: 800px;
  margin: 32px auto;
}

.form-card {
  padding: 40px;
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.card-header h1 {
  font-family: 'Google Sans', sans-serif;
  font-size: 24px;
  color: #202124;
  margin-bottom: 8px;
}

.subtitle {
  color: #5f6368;
  font-size: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.full-width {
  grid-column: 1 / -1;
}

label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #3c4043;
  font-size: 14px;
}

.helper-text {
  display: block;
  font-size: 12px;
  color: #5f6368;
  margin-top: 4px;
}

.editor-wrapper {
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
}

/* Quill Overrides */
:deep(.ql-toolbar) {
  border-color: #dadce0 !important;
  border-top-left-radius: 4px;
  border-top-right-radius: 4px;
  background-color: #f8f9fa;
}

:deep(.ql-container) {
  border-color: #dadce0 !important;
  border-bottom-left-radius: 4px;
  border-bottom-right-radius: 4px;
  font-family: 'Roboto', sans-serif;
  font-size: 16px;
  min-height: 150px;
}

.options-section {
  margin-bottom: 24px;
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #dadce0;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.option-label {
  font-weight: 500;
  color: #5f6368;
  width: 24px;
}

/* .remove-opt handled globally in style.css */

.radio-group, .checkbox-group {
  display: flex;
  gap: 24px;
  padding: 8px 0;
}

.radio-label, .checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.radio-text, .checkbox-text {
  font-size: 14px;
  color: #3c4043;
}

.form-actions {
  margin-top: 32px;
  display: flex;
  gap: 16px;
  justify-content: flex-end;
}

.error-text { color: #d93025; margin-top: 16px; text-align: center; }
.success-text { color: #188038; margin-top: 16px; text-align: center; }

.multi-select {
  height: 120px;
  padding: 8px;
}

.mt-2 { margin-top: 8px; }
</style>
