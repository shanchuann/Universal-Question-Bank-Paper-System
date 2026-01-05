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
import { authState } from '../states/authState'

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
      path: '/questions/:id/edit',
      name: 'edit-question',
      component: QuestionAddView,
      props: true
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
      component: () => import('../views/KnowledgePointView.vue'),
      meta: { roles: ['TEACHER'] }
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
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('../views/AdminUserView.vue')
    },
    {
      path: '/admin/system',
      name: 'admin-system',
      component: () => import('../views/AdminSystemView.vue')
    },
    {
      path: '/knowledge-point-manage',
      name: 'knowledge-point-manage',
      component: () => import('../views/KnowledgePointManageView.vue'),
      meta: { roles: ['TEACHER'] }
    },
    {
      path: '/admin/organizations',
      name: 'organization-manage',
      component: () => import('../views/OrganizationManageView.vue'),
      meta: { roles: ['TEACHER', 'ADMIN'] }
    },
    {
      path: '/admin/roles',
      name: 'role-permission',
      component: () => import('../views/RolePermissionView.vue'),
      meta: { roles: ['ADMIN'] }
    },
    {
      path: '/questions/review',
      name: 'question-review',
      component: () => import('../views/QuestionReviewView.vue'),
      meta: { roles: ['TEACHER', 'ADMIN'] }
    },
    {
      path: '/exams/manage',
      name: 'exam-plan-manage',
      component: () => import('../views/ExamPlanManageView.vue'),
      meta: { roles: ['TEACHER', 'ADMIN'] }
    },
    {
      path: '/exams/list',
      name: 'student-exam-list',
      component: () => import('../views/StudentExamListView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/my-organizations',
      name: 'my-organizations',
      component: () => import('../views/MyOrganizationsView.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach(async (to, _from, next) => {
  const requiredRoles = to.meta?.roles as string[] | undefined
  if (!requiredRoles || requiredRoles.length === 0) {
    return next()
  }

  if (!authState.isAuthenticated) {
    return next({ name: 'login', query: { redirect: to.fullPath } })
  }

  // 如果角色尚未加载，等待用户信息加载
  if (!authState.user.role && authState.isAuthenticated) {
    await authState.fetchUser()
  }

  if (!requiredRoles.includes(authState.user.role)) {
    return next({ name: 'home' })
  }

  return next()
})

export default router
