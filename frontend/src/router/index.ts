import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import QuestionListView from '../views/QuestionListView.vue'
import QuestionAddView from '../views/QuestionAddView.vue'
import PaperGenerationView from '../views/PaperGenerationView.vue'
import PaperPreviewView from '../views/PaperPreviewView.vue'
import PaperEditView from '../views/PaperEditView.vue'
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
      path: '/register',
      name: 'register',
      component: RegisterView
    },
    {
      path: '/questions',
      name: 'questions',
      component: QuestionListView
    },
    {
      path: '/questions/add',
      name: 'add-question',
      component: QuestionAddView
    },
    {
      path: '/import',
      name: 'import',
      component: () => import('../views/ImportView.vue')
    },
    {
      path: '/paper-generation',
      name: 'paper-generation',
      component: PaperGenerationView
    },
    {
      path: '/papers/manual',
      name: 'paper-manual',
      component: () => import('../views/PaperCreateManualView.vue')
    },
    {
      path: '/papers',
      name: 'paper-list',
      component: () => import('../views/PaperListView.vue')
    },
    {
      path: '/analytics/:paperId',
      name: 'analytics',
      component: () => import('../views/AnalyticsView.vue')
    },
    {
      path: '/papers/:id/preview',
      name: 'paper-preview',
      component: PaperPreviewView
    },
    {
      path: '/papers/:id/edit',
      name: 'paper-edit',
      component: PaperEditView
    },
    {
      path: '/exam',
      name: 'exam',
      component: ExamView
    },
    {
      path: '/knowledge-points',
      name: 'knowledge-points',
      component: () => import('../views/KnowledgePointView.vue')
    },
    {
      path: '/grading',
      name: 'grading-list',
      component: () => import('../views/GradingListView.vue')
    },
    {
      path: '/grading/:id',
      name: 'grading-detail',
      component: () => import('../views/GradingDetailView.vue')
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/UserProfileView.vue')
    },
    {
      path: '/practice',
      name: 'practice-select',
      component: () => import('../views/PracticeSelectView.vue')
    },
    {
      path: '/practice/:id',
      name: 'practice-session',
      component: () => import('../views/PracticeView.vue')
    },
    {
      path: '/leaderboard',
      name: 'leaderboard',
      component: () => import('../views/LeaderboardView.vue')
    }
  ]
})

export default router
