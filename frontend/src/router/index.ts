import { createRouter, createWebHistory } from 'vue-router'
import { authState } from '../states/authState'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../components/HelloWorld.vue'),
      props: { msg: 'Welcome to Universal Question Bank' }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue')
    },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('../views/ForgotPasswordView.vue')
    },
    {
      path: '/questions',
      name: 'questions',
      component: () => import('../views/QuestionListView.vue'),
      meta: { roles: ['TEACHER', 'ADMIN'] }
    },
    {
      path: '/questions/add',
      name: 'add-question',
      component: () => import('../views/QuestionAddView.vue'),
      // 学生也可以提交题目（待审核），允许 STUDENT 访问
      meta: { roles: ['TEACHER', 'ADMIN', 'STUDENT'] }
    },
    {
      path: '/questions/:id/edit',
      name: 'edit-question',
      component: () => import('../views/QuestionAddView.vue'),
      meta: { roles: ['TEACHER', 'ADMIN'] },
      props: true
    },
    {
      path: '/import',
      name: 'import',
      component: () => import('../views/ImportView.vue'),
      meta: { roles: ['TEACHER', 'ADMIN'] }
    },
    {
      path: '/paper-generation',
      name: 'paper-generation',
      component: () => import('../views/PaperGenerationView.vue'),
      meta: { roles: ['TEACHER', 'ADMIN'] }
    },
    {
      path: '/papers/manual',
      name: 'paper-manual',
      component: () => import('../views/PaperCreateManualView.vue'),
      meta: { roles: ['TEACHER', 'ADMIN'] }
    },
    {
      path: '/papers',
      name: 'paper-list',
      component: () => import('../views/PaperListView.vue'),
      meta: { roles: ['TEACHER', 'ADMIN'] }
    },
    {
      path: '/analytics/:paperId',
      name: 'analytics',
      component: () => import('../views/AnalyticsView.vue'),
      meta: { roles: ['TEACHER', 'ADMIN'] }
    },
    {
      path: '/papers/:id/preview',
      name: 'paper-preview',
      component: () => import('../views/PaperPreviewView.vue'),
      meta: { roles: ['TEACHER', 'ADMIN'] }
    },
    {
      path: '/papers/:id/edit',
      name: 'paper-edit',
      component: () => import('../views/PaperEditView.vue'),
      meta: { roles: ['TEACHER', 'ADMIN'] }
    },
    {
      path: '/exam',
      name: 'exam',
      component: () => import('../views/ExamView.vue')
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
      component: () => import('../views/GradingListView.vue'),
      meta: { roles: ['TEACHER', 'ADMIN'] }
    },
    {
      path: '/grading/:id',
      name: 'grading-detail',
      component: () => import('../views/GradingDetailView.vue'),
      meta: { roles: ['TEACHER', 'ADMIN'] }
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
      path: '/messages',
      name: 'messages',
      component: () => import('../views/NotificationCenterView.vue'),
      meta: { requiresAuth: true, roles: ['USER', 'TEACHER'] }
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
      path: '/my-scores',
      name: 'my-scores',
      component: () => import('../views/MyScoresView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/my-scores/:id',
      name: 'my-score-detail',
      component: () => import('../views/MyScoreDetailView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/my-organizations',
      name: 'my-organizations',
      component: () => import('../views/MyOrganizationsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin/logs',
      name: 'admin-logs',
      component: () => import('../views/AdminLogsView.vue'),
      meta: { roles: ['ADMIN'] }
    },
    {
      path: '/admin/statistics',
      name: 'admin-statistics',
      component: () => import('../views/AdminStatisticsView.vue'),
      meta: { roles: ['ADMIN'] }
    },
    {
      path: '/admin/monitor',
      name: 'admin-monitor',
      component: () => import('../views/AdminMonitorView.vue'),
      meta: { roles: ['ADMIN'] }
    },
    {
      path: '/admin/announcements',
      name: 'admin-announcements',
      component: () => import('../views/AdminAnnouncementsView.vue'),
      meta: { roles: ['ADMIN'] }
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

  const userRole = (authState.user.role || '').toUpperCase()

  const roleMatches = (required: string[], role: string) => {
    const req = required.map((r) => String(r).toUpperCase())
    if (req.includes(role)) return true
    // 兼容老数据/命名：把 'USER' 与 'STUDENT' 视为等价
    if (role === 'USER' && req.includes('STUDENT')) return true
    if (role === 'STUDENT' && req.includes('USER')) return true
    return false
  }

  if (!roleMatches(requiredRoles, userRole)) {
    return next({ name: 'home' })
  }

  return next()
})

export default router
