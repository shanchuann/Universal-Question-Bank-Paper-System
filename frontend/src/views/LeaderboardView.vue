<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { authState } from '@/states/authState'

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
      <h1>排行榜</h1>
      <p class="subtitle">查看班级同学的学习排名</p>
    </div>

    <!-- 班级选择器 - 总是显示（教师看所有班级，学生看自己的班级） -->
    <div class="filter-bar" v-if="allClasses.length > 0">
      <div class="filter-label">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
          <circle cx="9" cy="7" r="4"></circle>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
          <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
        </svg>
        选择班级
      </div>
      <div class="filter-options">
        <button 
          v-for="org in allClasses" 
          :key="org.id"
          class="filter-btn"
          :class="{ active: selectedOrgId === org.id }"
          @click="selectOrganization(org.id)"
        >
          {{ org.name }}
        </button>
      </div>
    </div>

    <!-- 没有班级时显示的提示 -->
    <div class="content-card" v-if="allClasses.length === 0 && !loading">
      <div class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#9aa0a6" stroke-width="1.5">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
          <circle cx="9" cy="7" r="4"></circle>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
          <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
        </svg>
        <p v-if="isTeacher">暂无班级数据</p>
        <p v-else>您还没有加入任何班级</p>
        <span class="hint" v-if="isTeacher">请先在组织架构中创建班级</span>
        <span class="hint" v-else>请先加入班级后查看班级排行榜</span>
        <router-link v-if="isTeacher" to="/admin/organizations" class="join-btn">去创建班级</router-link>
        <router-link v-else to="/my-organizations" class="join-btn">去加入班级</router-link>
      </div>
    </div>

    <div class="content-card" v-else>
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="leaderboard.length === 0" class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#9aa0a6" stroke-width="1.5">
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
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
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
              <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"></path>
              </svg>
            </div>
            <div class="avatar gold">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
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
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
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
        <div class="rank-table" v-if="leaderboard.length > 3">
          <div class="table-header">
            <span class="col-rank">排名</span>
            <span class="col-name">学生</span>
            <span class="col-correct">正确数</span>
            <span class="col-total">总题数</span>
            <span class="col-accuracy">正确率</span>
            <span class="col-streak">连胜</span>
          </div>
          <div 
            v-for="(user, index) in leaderboard.slice(3)" 
            :key="user.id"
            class="table-row"
          >
            <span class="col-rank">
              <span class="rank-badge">{{ index + 4 }}</span>
            </span>
            <span class="col-name">
              <div class="user-info">
                <div class="user-avatar">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                  </svg>
                </div>
                <span>{{ user.nickname || user.userId }}</span>
              </div>
            </span>
            <span class="col-correct">{{ user.correctAnswers }}</span>
            <span class="col-total">{{ user.totalQuestionsAnswered }}</span>
            <span class="col-accuracy">{{ (user.accuracy * 100).toFixed(1) }}%</span>
            <span class="col-streak">
              <span v-if="user.currentStreak > 0" class="streak-badge">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"></path>
                </svg>
                {{ user.currentStreak }}
              </span>
              <span v-else class="no-streak">-</span>
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.leaderboard-view {
  padding: 24px;
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 400;
  color: #202124;
  margin: 0 0 4px 0;
}

.subtitle {
  color: #5f6368;
  margin: 0;
  font-size: 14px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding: 16px;
  background: #fff;
  border: 1px solid #dadce0;
  border-radius: 8px;
}

.filter-label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #5f6368;
  font-size: 14px;
  font-weight: 500;
}

.filter-options {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-btn {
  padding: 8px 16px;
  border: 1px solid #dadce0;
  border-radius: 20px;
  background: #fff;
  color: #5f6368;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-btn:hover {
  background: #f1f3f4;
}

.filter-btn.active {
  background: #e8f0fe;
  border-color: #1a73e8;
  color: #1a73e8;
}

.content-card {
  background: #fff;
  border: 1px solid #dadce0;
  border-radius: 8px;
  overflow: hidden;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 64px 24px;
  color: #5f6368;
}

.empty-state svg {
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state .hint {
  font-size: 13px;
  color: #9aa0a6;
  display: block;
  margin-bottom: 16px;
}

.empty-state .join-btn {
  display: inline-block;
  padding: 10px 24px;
  background: #1a73e8;
  color: #fff;
  text-decoration: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  transition: background 0.2s;
}

.empty-state .join-btn:hover {
  background: #1557b0;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #dadce0;
  border-top-color: #1a73e8;
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
  gap: 12px;
  padding: 40px 24px 32px;
  background: linear-gradient(180deg, #fafbfc 0%, #fff 100%);
  border-bottom: 1px solid #e8eaed;
}

.podium-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 16px;
  border-radius: 12px;
  background: #fff;
  min-width: 130px;
  position: relative;
  border: 1px solid #e8eaed;
  transition: transform 0.2s, box-shadow 0.2s;
}

.podium-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}

.podium-item.first {
  transform: translateY(-20px);
  padding: 24px 20px;
  min-width: 150px;
  border: none;
  background: linear-gradient(135deg, #fff9e6 0%, #fff 100%);
  box-shadow: 0 4px 20px rgba(251, 188, 4, 0.2);
}

.podium-item.first:hover {
  transform: translateY(-24px);
  box-shadow: 0 12px 32px rgba(251, 188, 4, 0.3);
}

.podium-item.second {
  background: linear-gradient(135deg, #f5f7fa 0%, #fff 100%);
}

.podium-item.third {
  background: linear-gradient(135deg, #fdf5ef 0%, #fff 100%);
}

.rank-medal {
  position: absolute;
  top: -12px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
}

.rank-medal.gold {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #ffd54f 0%, #ffb300 100%);
  box-shadow: 0 2px 8px rgba(255, 179, 0, 0.4);
}

.rank-medal.silver {
  background: linear-gradient(135deg, #b0bec5 0%, #78909c 100%);
  box-shadow: 0 2px 8px rgba(120, 144, 156, 0.4);
}

.rank-medal.bronze {
  background: linear-gradient(135deg, #d7a86e 0%, #a1887f 100%);
  box-shadow: 0 2px 8px rgba(161, 136, 127, 0.4);
}

.avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  border: 2px solid transparent;
}

.avatar.gold {
  width: 64px;
  height: 64px;
  background: #fff9e6;
  border-color: #ffd54f;
  color: #f9a825;
}

.avatar.silver {
  background: #f5f7fa;
  border-color: #b0bec5;
  color: #607d8b;
}

.avatar.bronze {
  background: #fdf5ef;
  border-color: #d7a86e;
  color: #8d6e63;
}

.podium-item .name {
  font-size: 14px;
  font-weight: 600;
  color: #202124;
  margin-bottom: 8px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.podium-item.first .name {
  font-size: 16px;
}

.podium-item .score {
  font-size: 24px;
  font-weight: 700;
  color: #1a73e8;
  line-height: 1;
}

.podium-item.first .score {
  font-size: 28px;
}

.podium-item .score .unit {
  font-size: 14px;
  font-weight: 400;
  color: #5f6368;
  margin-left: 2px;
}

.podium-item .accuracy {
  font-size: 12px;
  color: #5f6368;
  margin-top: 4px;
}

/* Rank Table */
.rank-table {
  padding: 0 16px 16px;
}

.table-header {
  display: grid;
  grid-template-columns: 60px 1fr 80px 80px 80px 80px;
  padding: 12px 8px;
  font-size: 12px;
  font-weight: 500;
  color: #5f6368;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid #e8eaed;
}

.table-row {
  display: grid;
  grid-template-columns: 60px 1fr 80px 80px 80px 80px;
  padding: 12px 8px;
  align-items: center;
  border-bottom: 1px solid #f1f3f4;
  transition: background 0.2s;
}

.table-row:hover {
  background: #f8f9fa;
}

.table-row:last-child {
  border-bottom: none;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f1f3f4;
  font-size: 13px;
  font-weight: 600;
  color: #5f6368;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e8f0fe;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1a73e8;
}

.col-correct {
  font-weight: 600;
  color: #1e8e3e;
}

.col-accuracy {
  color: #1a73e8;
}

.streak-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #fef7e0;
  color: #f9a825;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.no-streak {
  color: #9aa0a6;
}

.col-rank, .col-correct, .col-total, .col-accuracy, .col-streak {
  text-align: center;
}
</style>
