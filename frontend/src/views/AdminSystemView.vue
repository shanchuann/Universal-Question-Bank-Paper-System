<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import axios from 'axios'
import { Settings, Power, Clock, Shield, Bell, Mail, Upload, Download, Save, RefreshCw } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'

const { showToast } = useToast()

interface SystemSettings {
  systemEnabled: boolean
  maintenanceMode: boolean
  maintenanceMessage: string
  allowRegistration: boolean
  requireEmailVerification: boolean
  sessionTimeout: number
  maxLoginAttempts: number
  passwordMinLength: number
  allowPasswordReset: boolean
  maxFileSize: number
  allowedFileTypes: string
  examAutoSaveInterval: number
  showLeaderboard: boolean
  enableNotifications: boolean
  systemEmail: string
  siteName: string
  siteDescription: string
}

const loading = ref(false)
const saving = ref(false)
const activeSection = ref('general')

// 确认弹窗
const confirmDialog = reactive({
  show: false,
  title: '',
  message: '',
  onConfirm: () => {}
})

const showConfirm = (title: string, message: string, onConfirm: () => void) => {
  confirmDialog.show = true
  confirmDialog.title = title
  confirmDialog.message = message
  confirmDialog.onConfirm = onConfirm
}

const closeConfirm = () => {
  confirmDialog.show = false
}

const handleConfirm = () => {
  confirmDialog.onConfirm()
  closeConfirm()
}

const settings = reactive<SystemSettings>({
  systemEnabled: true,
  maintenanceMode: false,
  maintenanceMessage: '系统正在维护中，请稍后再试...',
  allowRegistration: true,
  requireEmailVerification: false,
  sessionTimeout: 30,
  maxLoginAttempts: 5,
  passwordMinLength: 6,
  allowPasswordReset: true,
  maxFileSize: 10,
  allowedFileTypes: 'jpg,jpeg,png,gif,pdf,doc,docx',
  examAutoSaveInterval: 60,
  showLeaderboard: true,
  enableNotifications: true,
  systemEmail: 'admin@example.com',
  siteName: 'UQBank 题库系统',
  siteDescription: '通用题库与组卷系统'
})

const sections = [
  { id: 'general', label: '基本设置', icon: Settings },
  { id: 'security', label: '安全设置', icon: Shield },
  { id: 'exam', label: '考试设置', icon: Clock },
  { id: 'upload', label: '上传设置', icon: Upload },
  { id: 'notification', label: '通知设置', icon: Bell }
]

const fetchSettings = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/admin/system/settings', {
      headers: { Authorization: `Bearer ${token}` }
    })
    Object.assign(settings, response.data)
  } catch (error) {
    console.error('Failed to fetch settings', error)
    showToast({ message: '加载设置失败', type: 'error' })
  } finally {
    loading.value = false
  }
}

const saveSettings = async () => {
  saving.value = true
  try {
    const token = localStorage.getItem('token')
    await axios.put('/api/admin/system/settings', settings, {
      headers: { Authorization: `Bearer ${token}` }
    })
    showToast({ message: '设置已保存成功，页面即将刷新...', type: 'success' })
    // 延迟刷新页面以应用新设置（如站点名称）
    setTimeout(() => {
      window.location.reload()
    }, 1000)
  } catch (error) {
    console.error('Failed to save settings', error)
    showToast({ message: '保存设置失败，请重试', type: 'error' })
  } finally {
    saving.value = false
  }
}

// 记录切换前的状态
let previousSystemEnabled = true
let previousMaintenanceMode = false
let pendingToggleType: 'system' | 'maintenance' | null = null

const toggleSystem = () => {
  // v-model 已经改变了值，所以 previousSystemEnabled 是改变之前的值
  previousSystemEnabled = !settings.systemEnabled
  pendingToggleType = 'system'
  // 当前值是新状态，所以 true 表示要启用，false 表示要停用
  const action = settings.systemEnabled ? '启用' : '停用'
  showConfirm(
    `${action}系统`,
    `确定要${action}系统吗？${!settings.systemEnabled ? '停用后非管理员将无法访问系统。' : ''}`,
    () => {
      pendingToggleType = null
    }
  )
}

const toggleMaintenance = () => {
  // v-model 已经改变了值，所以 previousMaintenanceMode 是改变之前的值
  previousMaintenanceMode = !settings.maintenanceMode
  pendingToggleType = 'maintenance'
  // 当前值是新状态，所以 true 表示要开启，false 表示要关闭
  const action = settings.maintenanceMode ? '开启' : '关闭'
  showConfirm(
    `${action}维护模式`,
    `确定要${action}维护模式吗？${settings.maintenanceMode ? '开启后用户将看到维护提示。' : ''}`,
    () => {
      pendingToggleType = null
    }
  )
}

// 重写 closeConfirm 以处理取消时恢复状态
const handleCancel = () => {
  if (pendingToggleType === 'system') {
    settings.systemEnabled = previousSystemEnabled
  } else if (pendingToggleType === 'maintenance') {
    settings.maintenanceMode = previousMaintenanceMode
  }
  pendingToggleType = null
  closeConfirm()
}

const exportSettings = () => {
  const blob = new Blob([JSON.stringify(settings, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `system-settings-${new Date().toISOString().slice(0, 10)}.json`
  a.click()
  URL.revokeObjectURL(url)
  showToast({ message: '配置已导出', type: 'success' })
}

onMounted(() => {
  fetchSettings()
})
</script>

<template>
  <div class="admin-container container">
    <div class="header-row">
      <div class="header-content">
        <h1 class="page-title">系统设置</h1>
        <p class="page-subtitle">配置系统运行参数和功能选项</p>
      </div>
      <div class="header-actions">
        <button class="google-btn text-btn" @click="exportSettings">
          <Download :size="18" />
          导出配置
        </button>
        <button class="google-btn primary-btn" @click="saveSettings" :disabled="saving">
          <Save :size="18" />
          {{ saving ? '保存中...' : '保存设置' }}
        </button>
      </div>
    </div>

    <div class="settings-layout">
      <!-- 侧边导航 -->
      <div class="settings-nav">
        <button 
          v-for="section in sections" 
          :key="section.id"
          :class="['nav-item', { active: activeSection === section.id }]"
          @click="activeSection = section.id"
        >
          <component :is="section.icon" :size="18" />
          {{ section.label }}
        </button>
      </div>

      <!-- 设置内容 -->
      <div class="settings-content">
        <div v-if="loading" class="loading-state">
          <div class="spinner"></div>
          <p>加载中...</p>
        </div>

        <template v-else>
          <!-- 基本设置 -->
          <div v-show="activeSection === 'general'" class="settings-section">
            <h2 class="section-title">基本设置</h2>
            
            <div class="setting-card">
              <div class="setting-item">
                <div class="setting-info">
                  <div class="setting-header">
                    <Power :size="20" class="setting-icon" />
                    <h3>系统状态</h3>
                  </div>
                  <p>启用或停用系统，停用后非管理员无法访问</p>
                </div>
                <div class="setting-control">
                  <label class="switch">
                    <input type="checkbox" v-model="settings.systemEnabled" @change="toggleSystem">
                    <span class="slider"></span>
                  </label>
                  <span :class="['status-label', settings.systemEnabled ? 'enabled' : 'disabled']">
                    {{ settings.systemEnabled ? '运行中' : '已停用' }}
                  </span>
                </div>
              </div>

              <div class="setting-item">
                <div class="setting-info">
                  <div class="setting-header">
                    <RefreshCw :size="20" class="setting-icon" />
                    <h3>维护模式</h3>
                  </div>
                  <p>开启后将显示维护通知，用户仍可访问但会看到提示</p>
                </div>
                <div class="setting-control">
                  <label class="switch">
                    <input type="checkbox" v-model="settings.maintenanceMode" @change="toggleMaintenance">
                    <span class="slider"></span>
                  </label>
                  <span :class="['status-label', settings.maintenanceMode ? 'warning' : '']">
                    {{ settings.maintenanceMode ? '维护中' : '正常' }}
                  </span>
                </div>
              </div>

              <div class="setting-item column" v-if="settings.maintenanceMode">
                <label class="input-label">维护提示信息</label>
                <textarea 
                  v-model="settings.maintenanceMessage" 
                  class="google-input"
                  rows="3"
                  placeholder="输入维护期间显示的提示信息..."
                ></textarea>
              </div>

              <div class="setting-item">
                <div class="setting-info">
                  <h3>站点名称</h3>
                  <p>显示在页面标题和导航栏的名称</p>
                </div>
                <input type="text" v-model="settings.siteName" class="google-input short-input" />
              </div>
            </div>
          </div>

          <!-- 安全设置 -->
          <div v-show="activeSection === 'security'" class="settings-section">
            <h2 class="section-title">安全设置</h2>
            
            <div class="setting-card">
              <div class="setting-item">
                <div class="setting-info">
                  <h3>允许用户注册</h3>
                  <p>是否开放用户自行注册账号</p>
                </div>
                <label class="switch">
                  <input type="checkbox" v-model="settings.allowRegistration">
                  <span class="slider"></span>
                </label>
              </div>

              <div class="setting-item">
                <div class="setting-info">
                  <h3>邮箱验证</h3>
                  <p>注册时是否需要验证邮箱</p>
                </div>
                <label class="switch">
                  <input type="checkbox" v-model="settings.requireEmailVerification">
                  <span class="slider"></span>
                </label>
              </div>

              <div class="setting-item">
                <div class="setting-info">
                  <h3>允许密码重置</h3>
                  <p>用户是否可以通过邮箱重置密码</p>
                </div>
                <label class="switch">
                  <input type="checkbox" v-model="settings.allowPasswordReset">
                  <span class="slider"></span>
                </label>
              </div>

              <div class="setting-item">
                <div class="setting-info">
                  <h3>会话超时时间</h3>
                  <p>用户无操作后自动退出的时间（分钟）</p>
                </div>
                <div class="input-with-unit">
                  <input type="number" v-model="settings.sessionTimeout" class="google-input number-input" min="5" max="1440" />
                  <span class="unit">分钟</span>
                </div>
              </div>

              <div class="setting-item">
                <div class="setting-info">
                  <h3>最大登录尝试次数</h3>
                  <p>超过次数后账号将被临时锁定</p>
                </div>
                <div class="input-with-unit">
                  <input type="number" v-model="settings.maxLoginAttempts" class="google-input number-input" min="3" max="10" />
                  <span class="unit">次</span>
                </div>
              </div>

              <div class="setting-item">
                <div class="setting-info">
                  <h3>密码最小长度</h3>
                  <p>用户设置密码时的最小字符数</p>
                </div>
                <div class="input-with-unit">
                  <input type="number" v-model="settings.passwordMinLength" class="google-input number-input" min="6" max="20" />
                  <span class="unit">字符</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 考试设置 -->
          <div v-show="activeSection === 'exam'" class="settings-section">
            <h2 class="section-title">考试设置</h2>
            
            <div class="setting-card">
              <div class="setting-item">
                <div class="setting-info">
                  <h3>自动保存间隔</h3>
                  <p>考试过程中自动保存答案的时间间隔</p>
                </div>
                <div class="input-with-unit">
                  <input type="number" v-model="settings.examAutoSaveInterval" class="google-input number-input" min="10" max="300" />
                  <span class="unit">秒</span>
                </div>
              </div>

              <div class="setting-item">
                <div class="setting-info">
                  <h3>显示排行榜</h3>
                  <p>是否在考试结束后显示成绩排行榜</p>
                </div>
                <label class="switch">
                  <input type="checkbox" v-model="settings.showLeaderboard">
                  <span class="slider"></span>
                </label>
              </div>
            </div>
          </div>

          <!-- 上传设置 -->
          <div v-show="activeSection === 'upload'" class="settings-section">
            <h2 class="section-title">上传设置</h2>
            
            <div class="setting-card">
              <div class="setting-item">
                <div class="setting-info">
                  <h3>最大文件大小</h3>
                  <p>单个文件上传的最大尺寸限制</p>
                </div>
                <div class="input-with-unit">
                  <input type="number" v-model="settings.maxFileSize" class="google-input number-input" min="1" max="100" />
                  <span class="unit">MB</span>
                </div>
              </div>

              <div class="setting-item column">
                <label class="input-label">允许的文件类型</label>
                <input 
                  type="text" 
                  v-model="settings.allowedFileTypes" 
                  class="google-input"
                  placeholder="用逗号分隔，如：jpg,png,pdf"
                />
                <span class="input-hint">用逗号分隔多个文件扩展名</span>
              </div>
            </div>
          </div>

          <!-- 通知设置 -->
          <div v-show="activeSection === 'notification'" class="settings-section">
            <h2 class="section-title">通知设置</h2>
            
            <div class="setting-card">
              <div class="setting-item">
                <div class="setting-info">
                  <h3>启用系统通知</h3>
                  <p>是否向用户发送系统通知</p>
                </div>
                <label class="switch">
                  <input type="checkbox" v-model="settings.enableNotifications">
                  <span class="slider"></span>
                </label>
              </div>

              <div class="setting-item column">
                <label class="input-label">系统邮箱</label>
                <div class="input-with-icon">
                  <Mail :size="18" class="input-icon" />
                  <input 
                    type="email" 
                    v-model="settings.systemEmail" 
                    class="google-input icon-input"
                    placeholder="用于发送系统邮件的邮箱地址"
                  />
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 确认弹窗 -->
    <Transition name="modal">
      <div v-if="confirmDialog.show" class="modal-overlay" @click="handleCancel">
        <div class="modal-content" @click.stop>
          <h3 class="modal-title">{{ confirmDialog.title }}</h3>
          <p class="modal-message">{{ confirmDialog.message }}</p>
          <div class="modal-actions">
            <button class="google-btn text-btn" @click="handleCancel">取消</button>
            <button class="google-btn primary-btn" @click="handleConfirm">确定</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.admin-container {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
}

.page-title {
  margin: 0 0 4px 0;
  font-size: 28px;
  font-weight: 600;
  color: var(--line-text);
}

.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--line-text-secondary);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.google-btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s ease;
}

.primary-btn {
  background: var(--line-primary);
  color: white;
}

.primary-btn:hover:not(:disabled) {
  background: #1557b0;
  box-shadow: 0 2px 8px rgba(26, 115, 232, 0.3);
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.text-btn {
  background: transparent;
  color: var(--line-text-secondary);
  border: 1px solid var(--line-border);
}

.text-btn:hover {
  background: var(--line-bg-soft);
  color: var(--line-text);
}

/* Settings Layout */
.settings-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 24px;
}

/* Settings Navigation */
.settings-nav {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  background: var(--line-bg);
  border: 1px solid var(--line-border);
  border-radius: 12px;
  padding: 12px;
  height: fit-content;
  position: sticky;
  top: 24px;
}

.nav-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 12px 20px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--line-text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: center;
  width: 100%;
}

.nav-item:hover {
  background: var(--line-bg-soft);
  color: var(--line-text);
}

.nav-item.active {
  background: rgba(26, 115, 232, 0.1);
  color: var(--line-primary);
}

/* Settings Content */
.settings-content {
  min-height: 500px;
}

.settings-section {
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 20px 0;
  color: var(--line-text);
}

.setting-card {
  background: var(--line-bg);
  border: 1px solid var(--line-border);
  border-radius: 12px;
  padding: 8px;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 16px;
  border-bottom: 1px solid var(--line-border);
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-item.column {
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
}

.setting-info {
  flex: 1;
}

.setting-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.setting-icon {
  color: var(--line-primary);
}

.setting-info h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--line-text);
}

.setting-info p {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: var(--line-text-secondary);
}

.setting-control {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* Switch Styles */
.switch {
  position: relative;
  display: inline-block;
  width: 48px;
  height: 26px;
  flex-shrink: 0;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background-color: #ccc;
  transition: 0.3s;
  border-radius: 26px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 20px;
  width: 20px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.3s;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

input:checked + .slider {
  background-color: var(--line-primary);
}

input:checked + .slider:before {
  transform: translateX(22px);
}

.status-label {
  font-size: 13px;
  font-weight: 500;
  min-width: 60px;
}

.status-label.enabled {
  color: #34a853;
}

.status-label.disabled {
  color: #ea4335;
}

.status-label.warning {
  color: #fbbc04;
}

/* Inputs */
.input-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--line-text);
}

.google-input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid var(--line-border);
  border-radius: 8px;
  font-size: 14px;
  background: var(--line-bg);
  color: var(--line-text);
  transition: all 0.2s ease;
  box-sizing: border-box;
}

textarea.google-input {
  height: auto;
  padding: 12px 14px;
  resize: vertical;
  font-family: inherit;
}

.google-input:focus {
  outline: none;
  border-color: var(--line-primary);
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.15);
}

.short-input {
  width: 200px;
}

.number-input {
  width: 100px;
  text-align: center;
}

.input-with-unit {
  display: flex;
  align-items: center;
  gap: 8px;
}

.unit {
  font-size: 13px;
  color: var(--line-text-secondary);
}

.input-hint {
  font-size: 12px;
  color: var(--line-text-secondary);
}

.input-with-icon {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--line-text-secondary);
}

.icon-input {
  padding-left: 44px;
}

/* States */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px;
  color: var(--line-text-secondary);
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--line-border);
  border-top-color: var(--line-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Responsive */
@media (max-width: 768px) {
  .settings-layout {
    grid-template-columns: 1fr;
  }
  
  .settings-nav {
    flex-direction: row;
    overflow-x: auto;
    position: static;
    padding: 8px;
    gap: 8px;
  }
  
  .nav-item {
    white-space: nowrap;
    padding: 10px 14px;
  }
  
  .header-row {
    flex-direction: column;
    gap: 16px;
  }
  
  .header-actions {
    width: 100%;
  }
  
  .header-actions .google-btn {
    flex: 1;
    justify-content: center;
  }
  
  .setting-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .short-input {
    width: 100%;
  }
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease-out;
}

.modal-content {
  background: var(--line-bg);
  border-radius: 16px;
  padding: 28px;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.modal-title {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--line-text);
}

.modal-message {
  margin: 0 0 24px 0;
  font-size: 14px;
  color: var(--line-text-secondary);
  line-height: 1.6;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.modal-enter-active,
.modal-leave-active {
  transition: all 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-content,
.modal-leave-to .modal-content {
  transform: scale(0.95);
}
</style>
