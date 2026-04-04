<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import { Server, Cpu, HardDrive, Wifi, Clock, CheckCircle, AlertTriangle, XCircle, RefreshCw } from 'lucide-vue-next'

interface ServiceStatus {
  name: string
  status: 'online' | 'warning' | 'offline'
  responseTime: number
  uptime: string
  lastCheck: string
}

interface SystemMetrics {
  cpuUsage: number
  memoryUsage: number
  memoryTotal: string
  memoryUsed: string
  diskUsage: number
  diskTotal: string
  diskUsed: string
  networkIn: string
  networkOut: string
}

const loading = ref(false)
const autoRefresh = ref(true)
const lastUpdate = ref('')
const error = ref('')
let refreshInterval: ReturnType<typeof setInterval> | null = null

const services = ref<ServiceStatus[]>([])
const metrics = ref<SystemMetrics>({
  cpuUsage: 0,
  memoryUsage: 0,
  memoryTotal: '0 GB',
  memoryUsed: '0 GB',
  diskUsage: 0,
  diskTotal: '0 GB',
  diskUsed: '0 GB',
  networkIn: '0 KB/s',
  networkOut: '0 KB/s'
})

const fetchMonitorData = async () => {
  loading.value = true
  error.value = ''
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/admin/monitor', {
      headers: { Authorization: `Bearer ${token}` }
    })
    services.value = response.data.services
    metrics.value = response.data.metrics
  } catch (err) {
    console.error('Failed to fetch monitor data', err)
    error.value = '获取系统监控数据失败，请检查后端服务状态后重试'
  } finally {
    loading.value = false
    lastUpdate.value = new Date().toLocaleTimeString()
  }
}

const toggleAutoRefresh = () => {
  autoRefresh.value = !autoRefresh.value
  if (autoRefresh.value) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

const startAutoRefresh = () => {
  refreshInterval = setInterval(fetchMonitorData, 10000) // 每10秒刷新
}

const stopAutoRefresh = () => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
    refreshInterval = null
  }
}

const getStatusIcon = (status: string) => {
  switch (status) {
    case 'online': return CheckCircle
    case 'warning': return AlertTriangle
    case 'offline': return XCircle
    default: return CheckCircle
  }
}

const getStatusClass = (status: string) => {
  return `status-${status}`
}

const getProgressClass = (value: number) => {
  if (value >= 90) return 'danger'
  if (value >= 70) return 'warning'
  return 'normal'
}

onMounted(() => {
  fetchMonitorData()
  if (autoRefresh.value) {
    startAutoRefresh()
  }
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<template>
  <div class="admin-container container">
    <div class="header-row">
      <div class="header-left">
        <h1 class="page-title">系统监控</h1>
        <p class="page-subtitle">服务健康状态实时监控</p>
        <p class="data-note">数据来源：CPU/内存/磁盘为实时采集；网络吞吐为基于近 1 分钟行为日志的估算值。</p>
      </div>
      <div class="header-actions">
        <span class="last-update">最后更新: {{ lastUpdate }}</span>
        <button class="google-btn text-btn" @click="fetchMonitorData" :disabled="loading">
          <RefreshCw :size="18" :class="{ spinning: loading }" />
          刷新
        </button>
        <button 
          class="google-btn" 
          :class="autoRefresh ? 'primary-btn' : 'text-btn'"
          @click="toggleAutoRefresh"
        >
          {{ autoRefresh ? '自动刷新: 开' : '自动刷新: 关' }}
        </button>
      </div>
    </div>

    <!-- 系统资源 -->
    <div v-if="error" class="error-banner">{{ error }}</div>

    <div class="google-card metrics-card">
      <h3 class="section-title">系统资源</h3>
      <div class="metrics-grid">
        <div class="metric-item">
          <div class="metric-header">
            <Cpu :size="20" />
            <span>CPU 使用率</span>
          </div>
          <div class="metric-value">{{ metrics.cpuUsage.toFixed(1) }}%</div>
          <div class="metric-detail">&nbsp;</div>
          <div class="progress-bar">
            <div 
              class="progress-fill" 
              :class="getProgressClass(metrics.cpuUsage)"
              :style="{ width: metrics.cpuUsage + '%' }"
            ></div>
          </div>
        </div>

        <div class="metric-item">
          <div class="metric-header">
            <Server :size="20" />
            <span>内存使用率</span>
          </div>
          <div class="metric-value">{{ metrics.memoryUsage.toFixed(1) }}%</div>
          <div class="metric-detail">{{ metrics.memoryUsed }} / {{ metrics.memoryTotal }}</div>
          <div class="progress-bar">
            <div 
              class="progress-fill" 
              :class="getProgressClass(metrics.memoryUsage)"
              :style="{ width: metrics.memoryUsage + '%' }"
            ></div>
          </div>
        </div>

        <div class="metric-item">
          <div class="metric-header">
            <HardDrive :size="20" />
            <span>磁盘使用率</span>
          </div>
          <div class="metric-value">{{ metrics.diskUsage.toFixed(2) }}%</div>
          <div class="metric-detail">{{ metrics.diskUsed }} / {{ metrics.diskTotal }}</div>
          <div class="progress-bar">
            <div 
              class="progress-fill" 
              :class="getProgressClass(metrics.diskUsage)"
              :style="{ width: metrics.diskUsage.toFixed(2) + '%' }"
            ></div>
          </div>
        </div>

        <div class="metric-item">
          <div class="metric-header">
            <Wifi :size="20" />
            <span>网络流量</span>
          </div>
          <div class="network-stats">
            <div class="network-item">
              <span class="network-label">入站</span>
              <span class="network-value in">↓ {{ metrics.networkIn }}</span>
            </div>
            <div class="network-item">
              <span class="network-label">出站</span>
              <span class="network-value out">↑ {{ metrics.networkOut }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 服务状态 -->
    <div class="google-card services-card">
      <h3 class="section-title">服务状态</h3>
      <div class="services-grid">
        <div 
          v-for="service in services" 
          :key="service.name" 
          class="service-item"
          :class="getStatusClass(service.status)"
        >
          <div class="service-header">
            <component :is="getStatusIcon(service.status)" :size="20" class="status-icon" />
            <span class="service-name">{{ service.name }}</span>
          </div>
          <div class="service-stats">
            <div class="service-stat">
              <span class="stat-label">响应时间</span>
              <span class="stat-value">{{ service.responseTime }}ms</span>
            </div>
            <div class="service-stat">
              <span class="stat-label">运行信息</span>
              <span class="stat-value">{{ service.uptime }}</span>
            </div>
          </div>
          <div class="service-footer">
            <Clock :size="14" />
            <span>检查于 {{ service.lastCheck }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 状态说明 -->
    <div class="status-legend">
      <div class="legend-item">
        <span class="legend-dot online"></span>
        <span>正常运行</span>
      </div>
      <div class="legend-item">
        <span class="legend-dot warning"></span>
        <span>性能警告</span>
      </div>
      <div class="legend-item">
        <span class="legend-dot offline"></span>
        <span>服务离线</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-container {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
  flex-wrap: wrap;
  gap: 16px;
}

.header-left {
  flex: 1;
}

.page-subtitle {
  color: var(--line-text-secondary);
  margin-top: 8px;
}

.data-note {
  margin-top: 6px;
  font-size: 12px;
  color: var(--line-text-secondary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.last-update {
  font-size: 13px;
  color: var(--line-text-secondary);
}

.error-banner {
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 10px;
  color: var(--line-error);
  border: 1px solid color-mix(in srgb, var(--line-error) 30%, transparent);
  background: color-mix(in srgb, var(--line-error) 10%, white);
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Section Title */
.section-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 24px;
  color: var(--line-text);
}

/* Metrics Card */
.metrics-card {
  padding: 24px;
  margin-bottom: 24px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

@media (max-width: 1024px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}

.metric-item {
  padding: 20px;
  background: var(--line-bg-soft);
  border-radius: 12px;
}

.metric-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--line-text-secondary);
  margin-bottom: 12px;
}

.metric-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--line-text);
  margin-bottom: 4px;
}

.metric-detail {
  font-size: 13px;
  color: var(--line-text-secondary);
  margin-bottom: 12px;
}

.progress-bar {
  height: 8px;
  background: var(--line-border);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-fill.normal {
  background: linear-gradient(90deg, var(--line-success), color-mix(in srgb, var(--line-success) 78%, white));
}

.progress-fill.warning {
  background: linear-gradient(90deg, var(--line-warning), color-mix(in srgb, var(--line-warning) 80%, white));
}

.progress-fill.danger {
  background: linear-gradient(90deg, var(--line-error), color-mix(in srgb, var(--line-error) 80%, white));
}

.network-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.network-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.network-label {
  font-size: 13px;
  color: var(--line-text-secondary);
}

.network-value {
  font-size: 16px;
  font-weight: 500;
}

.network-value.in { color: var(--line-success); }
.network-value.out { color: var(--line-primary); }

/* Services Card */
.services-card {
  padding: 24px;
  margin-bottom: 24px;
}

.services-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

@media (max-width: 1024px) {
  .services-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .services-grid {
    grid-template-columns: 1fr;
  }
}

.service-item {
  padding: 20px;
  border: 1px solid var(--line-border);
  border-radius: 12px;
  background: var(--line-card-bg);
  transition: transform 0.2s, box-shadow 0.2s;
}

.service-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--line-shadow-md);
}

.service-item.status-online {
  border-left: 4px solid var(--line-success);
}

.service-item.status-warning {
  border-left: 4px solid var(--line-warning);
}

.service-item.status-offline {
  border-left: 4px solid var(--line-error);
}

.service-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.status-icon {
  flex-shrink: 0;
}

.status-online .status-icon { color: var(--line-success); }
.status-warning .status-icon { color: var(--line-warning); }
.status-offline .status-icon { color: var(--line-error); }

.service-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--line-text);
}

.service-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
}

.service-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-label {
  font-size: 12px;
  color: var(--line-text-secondary);
}

.stat-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--line-text);
}

.service-footer {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--line-text-secondary);
  padding-top: 12px;
  border-top: none;
}

/* Status Legend */
.status-legend {
  display: flex;
  justify-content: center;
  gap: 32px;
  padding: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--line-text-secondary);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-dot.online { background: var(--line-success); }
.legend-dot.warning { background: var(--line-warning); }
.legend-dot.offline { background: var(--line-error); }

/* Buttons */
.google-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.text-btn {
  background: transparent;
  color: var(--line-primary);
}

.text-btn:hover:not(:disabled) {
  background: color-mix(in srgb, var(--line-primary) 10%, transparent);
}

.text-btn:disabled {
  color: var(--line-text-secondary);
  cursor: not-allowed;
}

.primary-btn {
  background: var(--line-primary);
  color: white;
}

.primary-btn:hover {
  background: var(--line-primary-hover);
}
</style>

