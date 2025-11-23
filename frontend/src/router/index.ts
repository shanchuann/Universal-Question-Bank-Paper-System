import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import QuestionListView from '../views/QuestionListView.vue'
import PaperGenerationView from '../views/PaperGenerationView.vue'
import ExamView from '../views/ExamView.vue'
import HelloWorld from '../components/HelloWorld.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HelloWorld,
      props: { msg: 'Welcome to Universal Question Bank' }
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/questions',
      name: 'questions',
      component: QuestionListView
    },
    {
      path: '/paper-generation',
      name: 'paper-generation',
      component: PaperGenerationView
    },
    {
      path: '/exam',
      name: 'exam',
      component: ExamView
    }
  ]
})

export default router
