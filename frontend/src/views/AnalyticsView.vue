<template>
  <div class="analytics-view container mt-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2>考试统计分析报告</h2>
      <button class="btn btn-secondary" @click="$router.back()">返回</button>
    </div>

    <div v-if="loading" class="text-center my-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">加载中...</span>
      </div>
      <p class="mt-2">正在加载分析数据...</p>
    </div>

    <div v-else-if="error" class="alert alert-danger">
      {{ error }}
    </div>

    <div v-else-if="analyticsData">
      <!-- Summary Cards -->
      <div class="row mb-4">
        <div class="col-md-3">
          <div class="card text-center h-100 border-primary">
            <div class="card-body">
              <h5 class="card-title text-muted">平均分</h5>
              <h2 class="display-4 text-primary">{{ formatNumber(analyticsData.averageScore) }}</h2>
            </div>
          </div>
        </div>
        <div class="col-md-3">
          <div class="card text-center h-100 border-success">
            <div class="card-body">
              <h5 class="card-title text-muted">及格率</h5>
              <h2 class="display-4 text-success">{{ formatPercent(analyticsData.passRate) }}</h2>
            </div>
          </div>
        </div>
        <div class="col-md-3">
          <div class="card text-center h-100 border-info">
            <div class="card-body">
              <h5 class="card-title text-muted">最高分</h5>
              <h2 class="display-4 text-info">{{ formatNumber(analyticsData.highestScore) }}</h2>
            </div>
          </div>
        </div>
        <div class="col-md-3">
          <div class="card text-center h-100 border-secondary">
            <div class="card-body">
              <h5 class="card-title text-muted">参考人数</h5>
              <!-- Assuming we can infer this or add it later, for now maybe just show something else or omit -->
              <h2 class="display-4 text-secondary">-</h2> 
            </div>
          </div>
        </div>
      </div>

      <!-- Charts Row -->
      <div class="row mb-4">
        <!-- Score Distribution -->
        <div class="col-md-6">
          <div class="card h-100">
            <div class="card-header">成绩分布</div>
            <div class="card-body">
              <Bar v-if="scoreDistributionData" :data="scoreDistributionData" :options="barOptions" />
            </div>
          </div>
        </div>
        <!-- Knowledge Mastery -->
        <div class="col-md-6">
          <div class="card h-100">
            <div class="card-header">知识点掌握情况</div>
            <div class="card-body">
              <Radar v-if="knowledgeMasteryData" :data="knowledgeMasteryData" :options="radarOptions" />
            </div>
          </div>
        </div>
      </div>

      <!-- Error Rates Table -->
      <div class="card mb-4">
        <div class="card-header">高频错题 (Top 10)</div>
        <div class="card-body">
          <div class="table-responsive">
            <table class="table table-striped table-hover">
              <thead>
                <tr>
                  <th>题目ID</th>
                  <th>错误率</th>
                  <th>错误次数</th>
                  <th>总尝试次数</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in analyticsData.errorRates" :key="item.questionId">
                  <td>{{ item.questionId }}</td>
                  <td>
                    <div class="d-flex align-items-center">
                      <div class="progress flex-grow-1 me-2" style="height: 10px;">
                        <div 
                          class="progress-bar bg-danger" 
                          role="progressbar" 
                          :style="{ width: ((item.errorRate ?? 0) * 100) + '%' }"
                          :aria-valuenow="(item.errorRate ?? 0) * 100" 
                          aria-valuemin="0" 
                          aria-valuemax="100">
                        </div>
                      </div>
                      <span>{{ formatPercent(item.errorRate) }}</span>
                    </div>
                  </td>
                  <td>{{ item.errorCount ?? item.incorrect }}</td>
                  <td>{{ item.totalAttempts ?? item.attempts }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { analyticsApi } from '@/api/client'
import type { ExamAnalyticsResponse } from '@/api/generated'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale,
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
} from 'chart.js'
import { Bar, Radar } from 'vue-chartjs'

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Title,
  Tooltip,
  Legend
)

const route = useRoute()
const paperId = route.params.paperId as string

const loading = ref(true)
const error = ref('')
const analyticsData = ref<ExamAnalyticsResponse | null>(null)

const formatNumber = (num?: number) => {
  return num !== undefined ? num.toFixed(1) : '-'
}

const formatPercent = (num?: number) => {
  return num !== undefined ? (num * 100).toFixed(1) + '%' : '-'
}

const fetchData = async () => {
  try {
    loading.value = true
    const response = await analyticsApi.apiAnalyticsExamsPaperVersionIdSummaryGet(paperId)
    analyticsData.value = response.data
  } catch (err: any) {
    console.error('Failed to fetch analytics:', err)
    error.value = '获取分析数据失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

// Chart Data: Score Distribution
const scoreDistributionData = computed(() => {
  if (!analyticsData.value?.scoreDistribution) return null
  
  const labels = analyticsData.value.scoreDistribution.map(b => b.rangeLabel || b.range || '')
  const data = analyticsData.value.scoreDistribution.map(b => b.count ?? b.percentage ?? 0)

  return {
    labels,
    datasets: [
      {
        label: '人数',
        backgroundColor: '#3b82f6',
        data
      }
    ]
  }
})

const barOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: false
    }
  }
}

// Chart Data: Knowledge Mastery
const knowledgeMasteryData = computed(() => {
  if (!analyticsData.value?.knowledgeMastery) return null

  const labels = analyticsData.value.knowledgeMastery.map(k => k.knowledgePointId) // Ideally map to name if available
  const data = analyticsData.value.knowledgeMastery.map(k => (k.masteryRate || 0) * 100)

  return {
    labels,
    datasets: [
      {
        label: '掌握率 (%)',
        backgroundColor: 'rgba(16, 185, 129, 0.2)',
        borderColor: '#10b981',
        pointBackgroundColor: '#10b981',
        pointBorderColor: '#fff',
        pointHoverBackgroundColor: '#fff',
        pointHoverBorderColor: '#10b981',
        data
      }
    ]
  }
})

const radarOptions = {
  responsive: true,
  maintainAspectRatio: false,
  scales: {
    r: {
      angleLines: {
        display: false
      },
      suggestedMin: 0,
      suggestedMax: 100
    }
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.card {
  box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
}
</style>
