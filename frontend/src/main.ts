import { createApp } from 'vue'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import './style.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)

app.component('QuillEditor', QuillEditor)

app.use(router)

app.mount('#app')
