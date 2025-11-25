<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import axios from 'axios'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import 'katex/dist/katex.min.css';
import katex from 'katex';

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
  <div class="container">
    <div class="google-card form-card">
      <div class="card-header">
        <h1>Add New Question</h1>
        <p class="subtitle">Create a new question for the bank</p>
      </div>
      
      <form @submit.prevent="handleSubmit">
        <div class="form-grid">
          <div class="form-group">
            <label>Subject ID</label>
            <input v-model="form.subjectId" type="text" required placeholder="e.g. math" />
          </div>
          
          <div class="form-group">
            <label>Type</label>
            <div class="select-wrapper">
              <select v-model="form.type">
                <option value="SINGLE_CHOICE">Single Choice</option>
                <option value="MULTIPLE_CHOICE">Multiple Choice</option>
                <option value="TRUE_FALSE">True / False</option>
                <option value="FILL_BLANK">Fill in the Blank</option>
                <option value="SHORT_ANSWER">Short Answer</option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label>Difficulty</label>
            <div class="select-wrapper">
              <select v-model="form.difficulty">
                <option value="EASY">Easy</option>
                <option value="MEDIUM">Medium</option>
                <option value="HARD">Hard</option>
              </select>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label>Question Stem</label>
          <div class="editor-toolbar-extra" v-if="form.type === 'FILL_BLANK'">
             <button type="button" @click="insertBlank" class="secondary-btn small-btn">Insert Blank (_____)</button>
          </div>
          <div class="editor-container">
            <QuillEditor 
              ref="quillEditor"
              theme="snow" 
              v-model:content="form.stem" 
              contentType="html" 
              :toolbar="toolbarOptions"
              @ready="onEditorReady"
            />
          </div>
        </div>

        <div class="form-group" v-if="['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(form.type)">
          <label>Options</label>
          <div class="options-container">
            <div v-for="(_, index) in form.options" :key="index" class="option-row">
              <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
              <input v-model="form.options[index]" type="text" :placeholder="'Option ' + (index + 1)" required />
              <button type="button" @click="removeOption(index)" class="secondary-btn remove-btn" v-if="form.options.length > 2" title="Remove option">
                Remove
              </button>
            </div>
            <button type="button" @click="addOption" class="secondary-btn small-btn">+ Add Option</button>
          </div>
        </div>

        <div class="form-group" v-if="form.type === 'FILL_BLANK'">
          <label>Blank Answers</label>
          <div class="options-container">
             <div v-for="(_, index) in form.options" :key="index" class="option-row">
                <span class="option-label">{{ index + 1 }}.</span>
                <input v-model="form.options[index]" type="text" :placeholder="'Answer for blank ' + (index + 1)" />
             </div>
             <div v-if="form.options.length === 0" class="hint">
                Click "Insert Blank" above the editor to add blanks.
             </div>
          </div>
        </div>

        <div class="form-group" v-if="form.type !== 'FILL_BLANK'">
          <label>Correct Answer</label>
          
          <div class="select-wrapper" v-if="['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(form.type)">
            <select v-model="form.answer" required>
              <option value="" disabled>Select the correct answer</option>
              <option v-for="(opt, index) in form.options" :key="index" :value="opt">
                {{ String.fromCharCode(65 + index) }}. {{ opt }}
              </option>
            </select>
          </div>

          <div class="select-wrapper" v-else-if="form.type === 'TRUE_FALSE'">
             <select v-model="form.answer" required>
                <option value="" disabled>Select True or False</option>
                <option value="True">True</option>
                <option value="False">False</option>
             </select>
          </div>

          <div v-else>
             <input v-model="form.answer" type="text" required placeholder="Enter the correct answer" />
          </div>
          
          <small class="hint" v-if="['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(form.type)">Select the option that is the correct answer.</small>
        </div>

        <div class="form-group">
          <label>Tags</label>
          <input v-model="form.tags" type="text" placeholder="e.g. algebra, basics" />
        </div>

        <div class="form-group">
          <label>Knowledge Points</label>
          <select v-model="form.knowledgePointIds" multiple class="google-select" style="height: 100px;">
            <option v-for="kp in flatPoints" :key="kp.id" :value="kp.id">
              {{ kp.name }}
            </option>
          </select>
          <small class="hint">Hold Ctrl/Cmd to select multiple</small>
        </div>

        <div class="form-actions">
          <button type="submit" :disabled="loading" class="primary-btn full-width">
            {{ loading ? 'Adding...' : 'Add Question' }}
          </button>
        </div>
        
        <div v-if="message" class="message success">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
          {{ message }}
        </div>
        <div v-if="error" class="message error">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
          {{ error }}
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.form-card {
  max-width: 700px;
  margin: 0 auto;
  padding: 40px;
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.card-header h1 {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.subtitle {
  color: #86868b;
  font-size: 17px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 20px;
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  font-size: 14px;
  color: #1d1d1f;
}

.select-wrapper {
  position: relative;
}

.options-container {
  background-color: #f9f9f9;
  padding: 20px;
  border-radius: 12px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.option-label {
  font-weight: 600;
  color: #86868b;
  width: 24px;
}

.icon-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.delete-btn {
  color: #ff3b30;
}

.delete-btn:hover {
  background-color: #ffe5e5;
}

.small-btn {
  font-size: 13px;
  padding: 6px 12px;
}

.hint {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #86868b;
}

.full-width {
  width: 100%;
  justify-content: center;
  padding: 12px;
  font-size: 16px;
}

.message {
  margin-top: 20px;
  padding: 12px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.success {
  background-color: #e8f5e9;
  color: #2e7d32;
}

.error {
  background-color: #ffebee;
  color: #c62828;
}

.editor-container {
  border: 1px solid #d2d2d7;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.remove-btn {
  color: #ff3b30;
  border: 1px solid #ff3b30;
  background: transparent;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.remove-btn:hover {
  background-color: #fff0f0;
}

.editor-toolbar-extra {
  margin-bottom: 8px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  
  .form-card {
    padding: 24px;
  }
}
</style>
