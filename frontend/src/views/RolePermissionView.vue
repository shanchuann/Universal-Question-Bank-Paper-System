<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface Role {
  id: string
  name: string
  code: string
  description: string
  isSystem: boolean
}

interface Permission {
  id: string
  name: string
  code: string
  resource: string
  action: string
  scope: string
  description: string
}

const roles = ref<Role[]>([])
const permissions = ref<Permission[]>([])
const showRoleForm = ref(false)
const showPermissionAssign = ref(false)
const selectedRole = ref<Role | null>(null)
const selectedPermissions = ref<Set<string>>(new Set())

const roleForm = ref({
  name: '',
  code: '',
  description: ''
})

onMounted(() => {
  fetchRoles()
  fetchPermissions()
})

async function fetchRoles() {
  try {
    const response = await fetch('/api/roles')
    roles.value = await response.json()
  } catch (error) {
    console.error('Failed to fetch roles:', error)
  }
}

async function fetchPermissions() {
  try {
    const response = await fetch('/api/permissions')
    permissions.value = await response.json()
  } catch (error) {
    console.error('Failed to fetch permissions:', error)
  }
}

function openRoleForm() {
  roleForm.value = { name: '', code: '', description: '' }
  showRoleForm.value = true
}

function openPermissionAssign(role: Role) {
  selectedRole.value = role
  selectedPermissions.value = new Set()
  showPermissionAssign.value = true
  fetchRolePermissions(role.id)
}

async function fetchRolePermissions(roleId: string) {
  try {
    const response = await fetch(`/api/roles/${roleId}/permissions`)
    const perms = await response.json()
    selectedPermissions.value = new Set(perms.map((p: Permission) => p.id))
  } catch (error) {
    console.error('Failed to fetch role permissions:', error)
  }
}

async function submitRole() {
  try {
    await fetch('/api/roles', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(roleForm.value)
    })
    showRoleForm.value = false
    fetchRoles()
  } catch (error) {
    console.error('Failed to create role:', error)
  }
}

async function submitPermissions() {
  if (!selectedRole.value) return
  try {
    await fetch(`/api/roles/${selectedRole.value.id}/permissions`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(Array.from(selectedPermissions.value))
    })
    showPermissionAssign.value = false
    fetchRoles()
  } catch (error) {
    console.error('Failed to update permissions:', error)
  }
}

function togglePermission(permId: string) {
  if (selectedPermissions.value.has(permId)) {
    selectedPermissions.value.delete(permId)
  } else {
    selectedPermissions.value.add(permId)
  }
}

function groupPermissionsByResource() {
  const grouped: Record<string, Permission[]> = {}
  for (const perm of permissions.value) {
    const resource = perm.resource
    if (!grouped[resource]) {
      grouped[resource] = []
    }
    grouped[resource].push(perm)
  }
  return grouped
}

const resourceLabels: Record<string, string> = {
  question: '题目管理',
  paper: '试卷管理',
  exam: '考试管理',
  user: '用户管理',
  org: '组织管理',
  system: '系统管理'
}
</script>

<template>
  <div class="role-permission-view">
    <div class="page-header">
      <h1>角色与权限管理</h1>
      <p class="subtitle">配置系统角色和权限点</p>
    </div>

    <div class="container">
      <div class="roles-section">
        <div class="section-header">
          <h2>角色列表</h2>
          <button class="google-btn primary-btn" @click="openRoleForm">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            新建角色
          </button>
        </div>

        <div class="content-card">
          <table class="role-table">
            <thead>
              <tr>
                <th>角色名称</th>
                <th>编码</th>
                <th>描述</th>
                <th>类型</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="role in roles" :key="role.id">
                <td>{{ role.name }}</td>
                <td><code>{{ role.code }}</code></td>
                <td>{{ role.description }}</td>
                <td>
                  <span :class="['role-type', role.isSystem ? 'system' : 'custom']">
                    {{ role.isSystem ? '系统' : '自定义' }}
                  </span>
                </td>
                <td>
                  <button class="icon-btn" @click="openPermissionAssign(role)" title="分配权限">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                      <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                    </svg>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="permissions-section">
        <div class="section-header">
          <h2>权限点清单</h2>
        </div>

        <div class="content-card">
          <div v-for="(perms, resource) in groupPermissionsByResource()" :key="resource" class="permission-group">
            <h3>{{ resourceLabels[resource] || resource }}</h3>
            <div class="permission-list">
              <div v-for="perm in perms" :key="perm.id" class="permission-item">
                <div class="perm-info">
                  <code class="perm-code">{{ perm.code }}</code>
                  <span class="perm-name">{{ perm.name }}</span>
                  <span class="perm-scope">{{ perm.scope }}</span>
                </div>
                <p class="perm-desc">{{ perm.description }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Role Form Modal -->
    <div v-if="showRoleForm" class="modal-overlay" @click.self="showRoleForm = false">
      <div class="modal-content">
        <h2>新建角色</h2>
        <form @submit.prevent="submitRole">
          <div class="form-group">
            <label>角色名称</label>
            <input v-model="roleForm.name" type="text" class="google-input" required />
          </div>
          <div class="form-group">
            <label>角色编码</label>
            <input v-model="roleForm.code" type="text" class="google-input" required />
          </div>
          <div class="form-group">
            <label>描述</label>
            <textarea v-model="roleForm.description" class="google-input" rows="3"></textarea>
          </div>
          <div class="form-actions">
            <button type="button" class="google-btn text-btn" @click="showRoleForm = false">取消</button>
            <button type="submit" class="google-btn primary-btn">创建</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Permission Assignment Modal -->
    <div v-if="showPermissionAssign && selectedRole" class="modal-overlay" @click.self="showPermissionAssign = false">
      <div class="modal-content permission-modal">
        <h2>为 {{ selectedRole.name }} 分配权限</h2>
        <div class="permission-selection">
          <div v-for="(perms, resource) in groupPermissionsByResource()" :key="resource" class="permission-group">
            <h3>{{ resourceLabels[resource] || resource }}</h3>
            <div class="checkbox-list">
              <label v-for="perm in perms" :key="perm.id" class="checkbox-item">
                <input
                  type="checkbox"
                  :checked="selectedPermissions.has(perm.id)"
                  @change="togglePermission(perm.id)"
                />
                <span>{{ perm.code }}</span>
              </label>
            </div>
          </div>
        </div>
        <div class="form-actions">
          <button type="button" class="google-btn text-btn" @click="showPermissionAssign = false">取消</button>
          <button type="button" class="google-btn primary-btn" @click="submitPermissions">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.role-permission-view {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 400;
  color: #202124;
  margin: 0 0 8px 0;
}

.subtitle {
  color: #5f6368;
  margin: 0;
}

.container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.content-card {
  background: #fff;
  border: 1px solid #dadce0;
  border-radius: 8px;
  overflow: hidden;
}

.role-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.role-table thead {
  background: #f8f9fa;
  border-bottom: 1px solid #dadce0;
}

.role-table th {
  padding: 12px 16px;
  text-align: left;
  font-weight: 500;
  color: #5f6368;
}

.role-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #dadce0;
}

.role-table code {
  background: #f8f9fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

.role-type {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 12px;
}

.role-type.system {
  background: #e8f0fe;
  color: #1a73e8;
}

.role-type.custom {
  background: #e6f4ea;
  color: #1e8e3e;
}

.permission-group {
  margin-bottom: 24px;
}

.permission-group h3 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 500;
  color: #202124;
  padding: 0 16px;
}

.permission-list {
  padding: 16px;
}

.permission-item {
  padding: 12px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  margin-bottom: 8px;
  font-size: 13px;
}

.perm-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.perm-code {
  background: #f8f9fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 11px;
  color: #202124;
}

.perm-name {
  font-weight: 500;
  color: #202124;
}

.perm-scope {
  margin-left: auto;
  background: #f1f3f4;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
}

.perm-desc {
  margin: 4px 0 0 0;
  color: #5f6368;
}

.checkbox-list {
  padding: 12px 16px;
}

.checkbox-item {
  display: flex;
  align-items: center;
  padding: 8px;
  margin-bottom: 4px;
  cursor: pointer;
}

.checkbox-item input {
  margin-right: 8px;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  width: 100%;
  max-width: 480px;
  max-height: 80vh;
  overflow-y: auto;
}

.permission-modal {
  max-width: 600px;
}

.modal-content h2 {
  margin: 0 0 24px 0;
  font-size: 20px;
  font-weight: 500;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #5f6368;
}

.google-input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
  font-family: inherit;
}

textarea.google-input {
  height: auto;
  padding: 8px 12px;
  resize: vertical;
}

.google-input:focus {
  outline: none;
  border-color: #1a73e8;
  box-shadow: 0 0 0 2px rgba(26,115,232,0.2);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.google-btn {
  padding: 8px 24px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.primary-btn {
  background: #1a73e8;
  color: #fff;
}

.primary-btn:hover {
  background: #1557b0;
}

.text-btn {
  background: transparent;
  color: #1a73e8;
}

.text-btn:hover {
  background: #e8f0fe;
}

.icon-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5f6368;
  transition: background 0.2s;
}

.icon-btn:hover {
  background: rgba(0,0,0,0.08);
  color: #202124;
}

@media (max-width: 1024px) {
  .container {
    grid-template-columns: 1fr;
  }
}
</style>
