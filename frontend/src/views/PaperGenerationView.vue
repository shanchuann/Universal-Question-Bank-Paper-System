<script setup lang="ts">
import { ref } from 'vue'
import { paperApi } from '@/api/client'
import type { AutoPaperCreateRequest } from '@/api/generated'

const form = ref<AutoPaperCreateRequest>({
  subjectId: '',
  title: '',
  durationMinutes: 60,
  totalScore: 100,
  typeRatio: {
    SINGLE_CHOICE: 0.4,
    MULTI_CHOICE: 0.3,
    TRUE_FALSE: 0.1,
    FILL_BLANK: 0.1,
    SHORT_ANSWER: 0.1
  },
  difficultyDistribution: {
    EASY: 0.3,
    MEDIUM: 0.5,
    HARD: 0.2
  }
})

const loading = ref(false)
const message = ref('')
const error = ref('')

const handleSubmit = async () => {
  loading.value = true
  message.value = ''
  error.value = ''
  try {
    await paperApi.papersAutoPost(form.value)
    message.value = 'Paper generated successfully!'
  } catch (err) {
    error.value = 'Failed to generate paper.'
    console.error(err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="paper-generation-container">
    <h1>Generate Paper</h1>
    <form @submit.prevent="handleSubmit">
      <div class="form-group">
        <label>Subject ID</label>
        <input v-model="form.subjectId" type="text" required />
      </div>
      <div class="form-group">
        <label>Title</label>
        <input v-model="form.title" type="text" required />
      </div>
      <div class="form-group">
        <label>Duration (minutes)</label>
        <input v-model.number="form.durationMinutes" type="number" required />
      </div>
      <div class="form-group">
        <label>Total Score</label>
        <input v-model.number="form.totalScore" type="number" required />
      </div>
      
      <fieldset>
        <legend>Type Ratio (must sum to 1.0)</legend>
        <div v-for="(_, type) in form.typeRatio" :key="type" class="form-group">
          <label>{{ type }}</label>
          <input v-model.number="form.typeRatio[type]" type="number" step="0.1" min="0" max="1" />
        </div>
      </fieldset>

      <fieldset>
        <legend>Difficulty Distribution (must sum to 1.0)</legend>
        <div class="form-group">
          <label>Easy</label>
          <input v-model.number="form.difficultyDistribution.EASY" type="number" step="0.1" min="0" max="1" />
        </div>
        <div class="form-group">
          <label>Medium</label>
          <input v-model.number="form.difficultyDistribution.MEDIUM" type="number" step="0.1" min="0" max="1" />
        </div>
        <div class="form-group">
          <label>Hard</label>
          <input v-model.number="form.difficultyDistribution.HARD" type="number" step="0.1" min="0" max="1" />
        </div>
      </fieldset>

      <button type="submit" :disabled="loading">Generate</button>
    </form>
    <p v-if="message" class="success">{{ message }}</p>
    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<style scoped>
.paper-generation-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 2rem;
}
.form-group {
  margin-bottom: 1rem;
}
label {
  display: block;
  margin-bottom: 0.5rem;
}
input {
  width: 100%;
  padding: 0.5rem;
}
fieldset {
  margin-bottom: 1rem;
  border: 1px solid #ccc;
  padding: 1rem;
}
button {
  padding: 0.5rem 1rem;
  background-color: #42b983;
  color: white;
  border: none;
  cursor: pointer;
}
button:disabled {
  background-color: #ccc;
}
.success {
  color: green;
  margin-top: 1rem;
}
.error {
  color: red;
  margin-top: 1rem;
}
</style>
