<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { authState } from '@/states/authState'
import GoogleSelect from '@/components/GoogleSelect.vue'

interface StudentStats {
  id: number
  oderId?: string  // legacy field
  userId?: string
  nickname?: string
  totalQuestionsAnswered: number
  correctAnswers: number
  currentStreak: number
  accuracy: number
  organizationId?: string
  organizationName?: string
}

interface Organization {
  id: string
  name: string
  parentName?: string
}

const leaderboard = ref<StudentStats[]>([])
const allClasses = ref<Organization[]>([])
const selectedOrgId = ref<string>('')
const loading = ref(false)

// Convert to options for GoogleSelect
const classOptions = computed(() => {
  return allClasses.value.map(org => ({
    label: org.name,
    value: org.id
  }))
})

// 判断是否是教师/管理员
const isTeacher = computed(() => {
  return authState.user?.role === 'TEACHER' || authState.user?.role === 'ADMIN'
})

// 获取带认证的请求头
const getAuthHeaders = () => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const fetchLeaderboard = async (orgId?: string) => {
  loading.value = true
  try {
    // 获取排行榜数据（只获取学生，可按班级过滤）
    const params = new URLSearchParams({ limit: '50' })
    if (orgId) params.append('orgId', orgId)
    const res = await axios.get(`/api/stats/leaderboard?${params.toString()}`, {
      headers: getAuthHeaders()
    })
    leaderboard.value = res.data
  } catch (e) {
    console.error(e)
    leaderboard.value = []
  } finally {
    loading.value = false
  }
}

// 教师获取有学生的班级
const fetchAllClasses = async () => {
  try {
    const res = await axios.get('/api/organizations/classes-with-members', {
      headers: getAuthHeaders()
    })
    allClasses.value = res.data.map((org: any) => ({
      id: org.id,
      name: org.name,
      parentName: org.parentName
    }))
    // 默认选择第一个班级
    if (res.data.length > 0) {
      selectedOrgId.value = res.data[0].id
      fetchLeaderboard(res.data[0].id)
    }
  } catch (e) {
    console.error(e)
    allClasses.value = []
  }
}

// 学生获取自己加入的班级
const fetchMyOrganizations = async () => {
  try {
    const res = await axios.get('/api/my-organizations', {
      headers: getAuthHeaders()
    })
    allClasses.value = res.data.map((org: any) => ({
      id: org.id,
      name: org.name,
      parentName: org.parentName
    }))
    // 默认选择第一个班级并加载该班级的排行榜
    if (res.data.length > 0) {
      selectedOrgId.value = res.data[0].id
      fetchLeaderboard(res.data[0].id)
    }
  } catch (e) {
    console.error(e)
    allClasses.value = []
  }
}

const selectOrganization = (orgId: string) => {
  selectedOrgId.value = orgId
  fetchLeaderboard(orgId)
}

onMounted(() => {
  // 教师直接获取所有班级，学生获取自己加入的班级
  if (isTeacher.value) {
    fetchAllClasses()
  } else {
    fetchMyOrganizations()
  }
})
</script>

<template>
  <div class="leaderboard-view">
    <div class="page-header">
      <h1 class="page-title">排行榜</h1>
      <p class="page-subtitle">查看班级同学的学习排名</p>
    </div>

    <!-- 班级选择器 -->
    <div class="filter-section" v-if="allClasses.length > 0">
      <div class="select-wrapper">
        <GoogleSelect
          v-model="selectedOrgId"
          :options="classOptions"
          label="选择班级"
          placeholder="请选择查看的班级"
          @update:modelValue="fetchLeaderboard"
        />
      </div>
    </div>

    <!-- 没有班级时显示的提示 -->
    <div class="line-card empty-card" v-if="allClasses.length === 0 && !loading">
      <div class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
          <circle cx="9" cy="7" r="4"></circle>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
          <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
        </svg>
        <p v-if="isTeacher">暂无班级数据</p>
        <p v-else>您还没有加入任何班级</p>
        <span class="hint" v-if="isTeacher">请先在组织架构中创建班级</span>
        <span class="hint" v-else>请先加入班级后查看班级排行榜</span>
        <router-link v-if="isTeacher" to="/admin/organizations" class="line-btn primary-btn mt-4">去创建班级</router-link>
        <router-link v-else to="/my-organizations" class="line-btn primary-btn mt-4">去加入班级</router-link>
      </div>
    </div>

    <div class="line-card content-card" v-else>
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="leaderboard.length === 0" class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M12 15l-2 5l9-13h-5l2-5l-9 13h5z"></path>
        </svg>
        <p>暂无排行数据</p>
        <span class="hint">该班级暂无学生答题记录</span>
      </div>

      <div v-else class="leaderboard-list">
        <!-- Top 3 Podium -->
        <div class="podium" v-if="leaderboard.length >= 1">
          <!-- Second Place -->
          <div class="podium-item second" v-if="leaderboard.length >= 2">
            <div class="rank-medal silver">2</div>
            <div class="avatar silver">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </div>
            <div class="name">{{ leaderboard[1]?.nickname || leaderboard[1]?.userId }}</div>
            <div class="score">{{ leaderboard[1]?.correctAnswers }} <span class="unit">题</span></div>
            <div class="accuracy">正确率 {{ leaderboard[1]?.accuracy !== undefined ? (leaderboard[1]?.accuracy * 100).toFixed(0) : 0 }}%</div>
          </div>
          
          <!-- First Place -->
          <div class="podium-item first">
            <div class="rank-medal gold">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"></path>
              </svg>
            </div>
            <div class="avatar gold">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </div>
            <div class="name">{{ leaderboard[0]?.nickname || leaderboard[0]?.userId }}</div>
            <div class="score">{{ leaderboard[0]?.correctAnswers }} <span class="unit">题</span></div>
            <div class="accuracy">正确率 {{ leaderboard[0]?.accuracy !== undefined ? (leaderboard[0]?.accuracy * 100).toFixed(0) : 0 }}%</div>
          </div>
          
          <!-- Third Place -->
          <div class="podium-item third" v-if="leaderboard.length >= 3">
            <div class="rank-medal bronze">3</div>
            <div class="avatar bronze">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </div>
            <div class="name">{{ leaderboard[2]?.nickname || leaderboard[2]?.userId }}</div>
            <div class="score">{{ leaderboard[2]?.correctAnswers }} <span class="unit">题</span></div>
            <div class="accuracy">正确率 {{ leaderboard[2]?.accuracy !== undefined ? (leaderboard[2]?.accuracy * 100).toFixed(0) : 0 }}%</div>
          </div>
        </div>

        <!-- Rest of the list (4th place and below) -->
        <div class="rank-table-container" v-if="leaderboard.length > 3">
          <table class="rank-table">
            <thead>
              <tr>
                <th class="col-rank">排名</th>
                <th class="col-name">学生</th>
                <th class="col-correct">正确数</th>
                <th class="col-total">总题数</th>
                <th class="col-accuracy">正确率</th>
                <th class="col-streak">连胜</th>
              </tr>
            </thead>
            <tbody>
              <tr 
                v-for="(user, index) in leaderboard.slice(3)" 
                :key="user.id"
                class="table-row"
              >
                <td class="col-rank">
                  <span class="rank-badge">{{ index + 4 }}</span>
                </td>
                <td class="col-name">
                  <div class="user-info">
                    <div class="user-avatar-sm">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                        <circle cx="12" cy="7" r="4"></circle>
                      </svg>
                    </div>
                    <span class="user-name">{{ user.nickname || user.userId }}</span>
                  </div>
                </td>
                <td class="col-correct">{{ user.correctAnswers }}</td>
                <td class="col-total">{{ user.totalQuestionsAnswered }}</td>
                <td class="col-accuracy">{{ (user.accuracy * 100).toFixed(1) }}%</td>
                <td class="col-streak">
                  <span v-if="user.currentStreak > 0" class="streak-badge">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"></path>
                    </svg>
                    {{ user.currentStreak }}
                  </span>
                  <span v-else class="no-streak">-</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.leaderboard-view {
  padding: 32px 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--line-text);
  margin: 0 0 8px 0;
}

.page-subtitle {
  color: var(--line-text-secondary);
  margin: 0;
  font-size: 14px;
}

.filter-section {
  margin-bottom: 32px;
}

.select-wrapper {
  max-width: 300px;
}

.content-card {
  padding: 0;
  overflow: hidden;
}

.empty-card {
  padding: 64px 24px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 64px 24px;
  color: var(--line-text-secondary);
}

.empty-state svg {
  margin-bottom: 16px;
  opacity: 0.5;
  color: var(--line-text-secondary);
}

.empty-state .hint {
  font-size: 13px;
  color: var(--line-text-secondary);
  display: block;
  margin-bottom: 24px;
}

.mt-4 { margin-top: 16px; }

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--line-border);
  border-top-color: var(--line-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Podium */
.podium {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 16px;
  padding: 48px 24px 40px;
  background: linear-gradient(180deg, var(--line-bg-soft) 0%, var(--line-bg) 100%);
  border-bottom: 1px solid var(--line-border);
}

.podium-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 20px;
  border-radius: var(--line-radius-lg);
  background: var(--line-bg);
  min-width: 140px;
  position: relative;
  border: 1px solid var(--line-border);
  box-shadow: var(--line-shadow-sm);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.podium-item:hover {
  transform: translateY(-8px);
  box-shadow: var(--line-shadow-lg);
  border-color: transparent;
}

.podium-item.first {
  transform: translateY(-20px);
  padding: 32px 24px;
  min-width: 160px;
  border: none;
  background: var(--line-bg);
  box-shadow: 0 10px 40px -10px rgba(251, 191, 36, 0.4); /* Custom gold glow */
  z-index: 2;
}

.podium-item.first:hover {
  transform: translateY(-28px);
  box-shadow: 0 20px 40px -10px rgba(251, 191, 36, 0.5);
}

.podium-item.second { z-index: 1; }
.podium-item.third { z-index: 1; }

.rank-medal {
  position: absolute;
  top: -14px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  z-index: 3;
}

.rank-medal.gold {
  width: 40px;
  height: 40px;
  top: -20px;
  background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
  box-shadow: 0 4px 6px -1px rgba(245, 158, 11, 0.4);
}

.rank-medal.silver {
  background: linear-gradient(135deg, #94A3B8 0%, #64748B 100%);
  box-shadow: 0 4px 6px -1px rgba(100, 116, 139, 0.4);
}

.rank-medal.bronze {
  background: linear-gradient(135deg, #D97706 0%, #92400E 100%);
  box-shadow: 0 4px 6px -1px rgba(180, 83, 9, 0.4);
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  border: 1px solid var(--line-border);
  background: var(--line-bg-soft);
  color: var(--line-text-secondary);
  transition: all 0.3s;
}

.podium-item:hover .avatar {
  background: #fff;
}

.avatar.gold {
  width: 72px;
  height: 72px;
  color: #F59E0B;
  background: #FEF3C7;
  border-color: #F59E0B;
}

.avatar.silver { color: #64748B; background: #F1F5F9; border-color: #CBD5E1; }
.avatar.bronze { color: #D97706; background: #FFF7ED; border-color: #FED7AA; }

.podium-item .name {
  font-size: 15px;
  font-weight: 600;
  color: var(--line-text);
  margin-bottom: 4px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.podium-item.first .name { font-size: 18px; }

.podium-item .score {
  font-size: 24px;
  font-weight: 700;
  color: var(--line-primary);
  line-height: 1;
  margin-bottom: 4px;
}

.podium-item.first .score { font-size: 32px; }

.podium-item .score .unit {
  font-size: 14px;
  font-weight: 500;
  color: var(--line-text-secondary);
  margin-left: 2px;
}

.podium-item .accuracy {
  font-size: 13px;
  color: var(--line-text-secondary);
}

/* Rank Table */
.rank-table-container {
  padding: 0;
}

.rank-table {
  width: 100%;
  border-collapse: collapse;
}

.rank-table th {
  padding: 16px;
  font-size: 13px;
  font-weight: 600;
  color: var(--line-text-secondary);
  text-align: left;
  border-bottom: 1px solid var(--line-border);
  background: var(--line-bg-soft);
}

.rank-table td {
  padding: 16px;
  font-size: 14px;
  color: var(--line-text);
  border-bottom: 1px solid var(--line-border);
  vertical-align: middle;
}

.rank-table tr:last-child td {
  border-bottom: none;
}

.rank-table tr:hover td {
  background: var(--line-bg-hover);
}

.col-rank, .col-correct, .col-total, .col-accuracy, .col-streak {
  text-align: center;
}

.col-rank { width: 80px; }
.col-name { text-align: left; }

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--line-bg-soft);
  font-size: 13px;
  font-weight: 600;
  color: var(--line-text-secondary);
  border: 1px solid var(--line-border);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar-sm {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--line-bg-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--line-text-secondary);
  border: 1px solid var(--line-border);
}

.user-name {
  font-weight: 500;
}

.col-correct {
  font-weight: 600;
  color: var(--line-success);
}

.col-accuracy {
  font-family: monospace;
  font-weight: 500;
}

.streak-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #FEF3C7;
  color: #D97706;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.no-streak {
  color: var(--line-border);
}
</style>
