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
            <button type="button" @click="addOption" class="secondary-btn small-btn">Add Option</button>
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
/* Google Material 风格美化和细节优化 */
.form-card {
  max-width: 700px;
  margin: 32px auto;
  padding: 40px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 4px 24px rgba(60,64,67,0.12), 0 1.5px 6px rgba(60,64,67,0.08);
}
.card-header {
  text-align: center;
  margin-bottom: 32px;
}
.card-header h1 {
  font-size: 30px;
  font-weight: 700;
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}
.subtitle {
  color: #5f6368;
  font-size: 17px;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 24px;
  margin-bottom: 12px;
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  margin-bottom: 6px;
}
.form-group input[type="text"],
.form-group .select-wrapper,
.form-group select,
.form-group .google-select {
  min-height: 44px;
  box-sizing: border-box;
}
.options-container {
  margin-bottom: 12px;
}
.editor-container {
  min-height: 140px;
}
.primary-btn {
  margin-top: 18px;
}
.select-wrapper {
  position: relative;
}
.select-wrapper select {
  width: 100%;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1.5px solid #d2d2d7;
  background: #f7f8fa;
  font-size: 15px;
  transition: border-color 0.2s;
}
.select-wrapper select:focus {
  border-color: #4285f4;
  outline: none;
}
.options-container {
  background-color: #f1f3f4;
  padding: 20px;
  border-radius: 14px;
  box-shadow: 0 1px 4px rgba(60,64,67,0.08);
}
.option-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.option-label {
  font-weight: 600;
  color: #5f6368;
  width: 24px;
}
.option-row input {
  flex: 1;
}
.remove-btn {
  align-self: center;
  min-width: 100px;
  text-align: center;
  margin-left: 8px;
}
.option-row input {
  flex: 1;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #d2d2d7;
  background: #fff;
  font-size: 15px;
  transition: border-color 0.2s;
}
.option-row input:focus {
  border-color: #4285f4;
  outline: none;
}
.remove-btn {
  color: #fff;
  background: #ea4335;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  font-weight: 500;
  box-shadow: 0 1px 2px rgba(60,64,67,0.08);
  transition: background 0.2s, box-shadow 0.2s;
}
.remove-btn:hover {
  background-color: #d93025;
  box-shadow: 0 2px 8px rgba(234,67,53,0.08);
}
.small-btn {
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #d2d2d7;
  color: #4285f4;
  cursor: pointer;
  transition: background 0.2s, box-shadow 0.2s;
}
.small-btn:hover {
  background: #e3f2fd;
  box-shadow: 0 2px 8px rgba(66,133,244,0.08);
}
.primary-btn {
  width: 100%;
  justify-content: center;
  padding: 14px;
  font-size: 17px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(90deg,#4285f4 0%,#1a73e8 100%);
  border: none;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(66,133,244,0.12);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}
.primary-btn:active {
  transform: scale(0.98);
  box-shadow: 0 1px 4px rgba(66,133,244,0.18);
}
.primary-btn:disabled {
  background: #bcdffb;
  color: #fff;
  cursor: not-allowed;
}
.message {
  margin-top: 20px;
  padding: 12px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  box-shadow: 0 1px 4px rgba(60,64,67,0.08);
}
.success {
  background-color: #e6f4ea;
  color: #188038;
}
.error {
  background-color: #fce8e6;
  color: #d93025;
}
.editor-container {
  border: 1.5px solid #d2d2d7;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 1px 4px rgba(60,64,67,0.08);
  min-height: 120px;
}
.ql-toolbar.ql-snow {
  border-radius: 8px 8px 0 0;
  background: #f7f8fa;
  border: none;
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  padding: 0 8px;
}
.ql-toolbar.ql-snow > .ql-formats {
  margin-right: 0;
  margin-bottom: 0;
  display: flex;
  align-items: center;
  gap: 0;
}
.ql-toolbar.ql-snow button,
.ql-toolbar.ql-snow .ql-picker {
  margin: 0 2px;
}
.ql-toolbar.ql-snow .ql-video,
.ql-toolbar.ql-snow .ql-code-block,
.ql-toolbar.ql-snow .ql-color,
.ql-toolbar.ql-snow .ql-background,
.ql-toolbar.ql-snow .ql-size,
.ql-toolbar.ql-snow .ql-font {
  display: none !important;
}
.ql-container.ql-snow {
  border-radius: 0 0 8px 8px;
  min-height: 80px;
  font-size: 16px;
}
.google-select {
  border-radius: 8px;
  border: 1.5px solid #d2d2d7;
  background: #f7f8fa;
  font-size: 15px;
  padding: 10px 14px;
  width: 100%;
  box-sizing: border-box;
  resize: none;
  overflow-y: auto;
}
.google-select option {
  padding: 8px 12px;
}

/* 进一步美化 Subject ID 和 Tags 输入框 */
.form-group input[type="text"] {
  width: 100%;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1.5px solid #d2d2d7;
  background: #f7f8fa;
  font-size: 16px;
  margin-bottom: 6px;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-shadow: 0 1px 4px rgba(60,64,67,0.08);
}
.form-group input[type="text"]:focus {
  border-color: #4285f4;
  outline: none;
  box-shadow: 0 2px 8px rgba(66,133,244,0.12);
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .form-card {
    padding: 18px;
  }
}
</style>
