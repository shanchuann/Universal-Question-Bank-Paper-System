<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authState } from '@/states/authState'

const route = useRoute()
const router = useRouter()

// 面包屑配置映射
const breadcrumbConfig: Record<string, { label: string; parent?: string }> = {
  // 首页
  '/': { label: '首页' },

  // 题库管理
  '/questions': { label: '题目列表', parent: '/' },
  '/questions/add': { label: '添加题目', parent: '/questions' },
  '/import': { label: '导入题目', parent: '/questions' },
  '/questions/review': { label: '题目审核', parent: '/questions' },
  '/knowledge-point-manage': { label: '知识点管理', parent: '/questions' },
  '/knowledge-points': { label: '知识点', parent: '/' },

  // 试卷管理
  '/papers': { label: '试卷列表', parent: '/' },
  '/papers/manual': { label: '手动组卷', parent: '/papers' },
  '/paper-generation': { label: '智能组卷', parent: '/papers' },

  // 考试相关
  '/exams/manage': { label: '考试管理', parent: '/' },
  '/exams/list': { label: '我的考试', parent: '/' },
  '/exam': { label: '考试中', parent: '/exams/list' },
  '/my-scores': { label: '我的成绩', parent: '/' },

  // 阅卷
  '/grading': { label: '阅卷列表', parent: '/' },

  // 练习模式
  '/practice': { label: '练习模式', parent: '/' },

  // 排行榜
  '/leaderboard': { label: '排行榜', parent: '/' },
  '/messages': { label: '消息中心', parent: '/' },

  // 班级/组织
  '/my-organizations': { label: '我的班级', parent: '/' },
  '/admin/organizations': { label: '组织架构', parent: '/' },

  // 个人中心
  '/profile': { label: '个人设置', parent: '/' },

  // 管理员功能
  '/admin/users': { label: '用户管理', parent: '/' },
  '/admin/system': { label: '系统设置', parent: '/' },
  '/admin/logs': { label: '操作日志', parent: '/' },
  '/admin/statistics': { label: '数据统计', parent: '/' },
  '/admin/monitor': { label: '系统监控', parent: '/' },
  '/admin/announcements': { label: '公告管理', parent: '/' }
}

// 动态路由的配置（带参数的路由）
const getDynamicConfig = (path: string): { label: string; parent?: string } | null => {
  // 编辑题目
  if (/^\/questions\/\d+\/edit$/.test(path)) {
    return { label: '编辑题目', parent: '/questions' }
  }
  // 试卷预览
  if (/^\/papers\/\d+\/preview$/.test(path)) {
    return { label: '试卷预览', parent: '/papers' }
  }
  // 试卷编辑
  if (/^\/papers\/\d+\/edit$/.test(path)) {
    return { label: '编辑试卷', parent: '/papers' }
  }
  // 试卷分析
  if (/^\/analytics\/\d+$/.test(path)) {
    return { label: '试卷分析', parent: '/papers' }
  }
  // 阅卷详情
  if (/^\/grading\/\d+$/.test(path)) {
    return { label: '阅卷详情', parent: '/grading' }
  }
  // 练习进行中
  if (/^\/practice\/\d+$/.test(path)) {
    return { label: '练习中', parent: '/practice' }
  }
  // 成绩详情
  if (/^\/my-scores\/\d+$/.test(path)) {
    return { label: '成绩详情', parent: '/my-scores' }
  }
  return null
}

interface BreadcrumbItem {
  path: string
  label: string
  isLast: boolean
}

const breadcrumbs = computed<BreadcrumbItem[]>(() => {
  const path = route.path

  // 首页不显示面包屑
  if (path === '/') {
    return []
  }

  // 登录、注册、忘记密码页面不显示面包屑
  if (['/login', '/register', '/forgot-password'].includes(path)) {
    return []
  }

  const items: BreadcrumbItem[] = []
  let currentPath = path

  // 构建面包屑链
  while (currentPath) {
    const config = breadcrumbConfig[currentPath] || getDynamicConfig(currentPath)
    if (config) {
      // Special case: student-facing "我要出题" should show 首页 / 我要出题
      if (currentPath === '/questions/add') {
        const role = (authState.user?.role || '').toString().toUpperCase();
        if (role !== 'TEACHER' && role !== 'ADMIN') {
          // override parent to homepage for students
          config.parent = '/'
          config.label = '我要出题'
        }
      }
      // 基于路由定义和用户角色进行权限检查，避免面包屑泄露不可见页面
      const resolved = router.resolve(currentPath)
      let allowed = true
      if (resolved && resolved.matched && resolved.matched.length > 0) {
        for (const record of resolved.matched) {
          const requiredRoles = (record.meta as any)?.roles as string[] | undefined
          if (requiredRoles && requiredRoles.length > 0) {
            const role = (authState.user.role || '').toUpperCase()
            const req = requiredRoles.map((r) => String(r).toUpperCase())
            const roleMatches = (r: string) => {
              if (req.includes(r)) return true
              if (r === 'USER' && req.includes('STUDENT')) return true
              if (r === 'STUDENT' && req.includes('USER')) return true
              return false
            }
            // 管理员始终可见
            if (role !== 'ADMIN' && !roleMatches(role)) {
              allowed = false
              break
            }
          }
        }
      }

      if (!allowed) {
        // 当前路径或其上级被权限限制，停止构建（不展示受限项）
        break
      }

      items.unshift({
        path: currentPath,
        label: config.label,
        isLast: currentPath === path
      })
      currentPath = config.parent || ''
    } else {
      // 如果没有配置，尝试使用路由名称或跳过
      break
    }
  }

  return items
})

const navigateTo = (path: string) => {
  router.push(path)
}

// 暴露给模板使用
defineExpose({ navigateTo })
</script>

<template>
  <nav v-if="breadcrumbs.length > 0" class="breadcrumb-nav">
    <div class="breadcrumb-container">
      <template v-for="item in breadcrumbs" :key="item.path">
        <template v-if="item.isLast">
          <span class="breadcrumb-current">{{ item.label }}</span>
        </template>
        <template v-else>
          <router-link :to="item.path" class="breadcrumb-link">{{ item.label }}</router-link>
          <span class="breadcrumb-separator">/</span>
        </template>
      </template>
    </div>
  </nav>
</template>

<style scoped>
.breadcrumb-nav {
  background: rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(10px) saturate(160%);
  -webkit-backdrop-filter: blur(10px) saturate(160%);
  border-bottom: 1px solid rgba(226, 232, 240, 0.72);
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.05);
  padding: 12px 24px;
  position: fixed;
  top: 64px;
  left: 0;
  right: 0;
  z-index: 999;
}

.breadcrumb-container {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.breadcrumb-link {
  color: var(--line-primary);
  text-decoration: none;
  transition: color 0.2s;
}

.breadcrumb-link:hover {
  color: var(--line-primary-dark, #1557b0);
  text-decoration: underline;
}

.breadcrumb-separator {
  color: var(--line-text-muted, #999);
  user-select: none;
}

.breadcrumb-current {
  color: var(--line-text);
  font-weight: 500;
}

@media (max-width: 768px) {
  .breadcrumb-nav {
    padding: 10px 16px;
  }

  .breadcrumb-container {
    font-size: 13px;
    gap: 6px;
  }
}
</style>
