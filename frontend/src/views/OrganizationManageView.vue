<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import GoogleSelect from '@/components/GoogleSelect.vue'
import { authState } from '@/states/authState'
import { useConfirm } from '@/composables/useConfirm'
import { useToast } from '@/composables/useToast'

const { confirm } = useConfirm()
const { showToast } = useToast()

interface Organization {
  id: string
  name: string
  code: string
  type: 'SCHOOL' | 'DEPARTMENT' | 'CLASS'
  parentId: string | null
  sortOrder: number
  status: string
  inviteCode?: string
  children?: Organization[]
}

interface University {
  code: string
  name: string
  province: string
  city: string
  type: string
  departments: Department[]
}

interface Department {
  code: string
  name: string
}

interface ClassConfig {
  name: string
  code: string
}

interface DepartmentConfig {
  code: string
  name: string
  selected: boolean
  classes: ClassConfig[]
}

const organizations = ref<Organization[]>([])
const universities = ref<University[]>([])
const loading = ref(false)
const showForm = ref(false)
const showUniversityPicker = ref(false)
const editingOrg = ref<Organization | null>(null)

// 高校选择相关
const searchKeyword = ref('')
const selectedUniversity = ref<University | null>(null)
const departmentConfigs = ref<DepartmentConfig[]>([])
const creatingFromTemplate = ref(false)

const form = ref({
  name: '',
  code: '',
  type: 'SCHOOL' as 'SCHOOL' | 'DEPARTMENT' | 'CLASS',
  parentId: null as string | null,
  sortOrder: 0,
  status: 'ACTIVE'
})

const typeIcons = {
  SCHOOL: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 21h18M9 8h1M9 12h1M9 16h1M14 8h1M14 12h1M14 16h1M5 21V5a2 2 0 012-2h10a2 2 0 012 2v16"/></svg>`,
  DEPARTMENT: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 3h6a4 4 0 014 4v14a3 3 0 00-3-3H2V3zM22 3h-6a4 4 0 00-4 4v14a3 3 0 013-3h7V3z"/></svg>`,
  CLASS: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M9 11a4 4 0 100-8 4 4 0 000 8zM23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/></svg>`
}

const typeOptions = [
  { label: '学校', value: 'SCHOOL' },
  { label: '学院', value: 'DEPARTMENT' },
  { label: '班级', value: 'CLASS' }
]

const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '禁用', value: 'INACTIVE' }
]

// 判断是否为管理员
const isAdmin = computed(() => authState.user.role === 'ADMIN')

// 过滤后的高校列表
const filteredUniversities = computed(() => {
  if (!searchKeyword.value) return universities.value
  const keyword = searchKeyword.value.toLowerCase()
  return universities.value.filter(
    (u) =>
      u.name.toLowerCase().includes(keyword) ||
      u.code.toLowerCase().includes(keyword) ||
      u.city.includes(searchKeyword.value) ||
      u.province.includes(searchKeyword.value)
  )
})

// 按省份分组的高校
const universitiesByProvince = computed(() => {
  const groups: Record<string, University[]> = {}
  for (const uni of filteredUniversities.value) {
    const group = groups[uni.province] ?? (groups[uni.province] = [])
    group.push(uni)
  }
  return groups
})

// 已选择的院系数量
const selectedDeptCount = computed(() => departmentConfigs.value.filter((d) => d.selected).length)

// 总班级数量
const totalClassCount = computed(() =>
  departmentConfigs.value.filter((d) => d.selected).reduce((sum, d) => sum + d.classes.length, 0)
)

onMounted(() => {
  fetchOrganizations()
  fetchUniversities()
})

async function fetchOrganizations() {
  loading.value = true
  try {
    const response = await fetch('/api/organizations')
    organizations.value = await response.json()
  } catch (error) {
    console.error('Failed to fetch organizations:', error)
  } finally {
    loading.value = false
  }
}

async function fetchUniversities() {
  try {
    const response = await fetch('/api/universities')
    universities.value = await response.json()
  } catch (error) {
    console.error('Failed to fetch universities:', error)
  }
}

function openAddForm(
  parentId: string | null = null,
  type: 'SCHOOL' | 'DEPARTMENT' | 'CLASS' = 'SCHOOL'
) {
  editingOrg.value = null
  form.value = {
    name: '',
    code: '',
    type,
    parentId,
    sortOrder: 0,
    status: 'ACTIVE'
  }
  showForm.value = true
}

function openEditForm(org: Organization) {
  editingOrg.value = org
  form.value = {
    name: org.name,
    code: org.code,
    type: org.type,
    parentId: org.parentId,
    sortOrder: org.sortOrder,
    status: org.status
  }
  showForm.value = true
}

function openUniversityPicker() {
  searchKeyword.value = ''
  selectedUniversity.value = null
  departmentConfigs.value = []
  showUniversityPicker.value = true
}

function selectUniversity(uni: University) {
  selectedUniversity.value = uni
  // 初始化院系配置，默认都选中，每个院系添加3个默认班级
  departmentConfigs.value = uni.departments.map((dept) => ({
    code: dept.code,
    name: dept.name,
    selected: true,
    classes: [
      { name: `${new Date().getFullYear()}级1班`, code: `${dept.code}_C1` },
      { name: `${new Date().getFullYear()}级2班`, code: `${dept.code}_C2` },
      { name: `${new Date().getFullYear()}级3班`, code: `${dept.code}_C3` }
    ]
  }))
}

function backToUniversityList() {
  selectedUniversity.value = null
  departmentConfigs.value = []
}

function toggleDepartment(deptCode: string) {
  const dept = departmentConfigs.value.find((d) => d.code === deptCode)
  if (dept) {
    dept.selected = !dept.selected
  }
}

function addClass(deptCode: string) {
  const dept = departmentConfigs.value.find((d) => d.code === deptCode)
  if (dept) {
    const classNum = dept.classes.length + 1
    dept.classes.push({
      name: `${new Date().getFullYear()}级${classNum}班`,
      code: `${deptCode}_C${classNum}`
    })
  }
}

function removeClass(deptCode: string, index: number) {
  const dept = departmentConfigs.value.find((d) => d.code === deptCode)
  if (dept && dept.classes.length > 1) {
    dept.classes.splice(index, 1)
  }
}

function updateClassName(deptCode: string, index: number, name: string) {
  const dept = departmentConfigs.value.find((d) => d.code === deptCode)
  if (dept && dept.classes[index]) {
    dept.classes[index].name = name
  }
}

async function createFromTemplate() {
  if (!selectedUniversity.value) return

  const selectedDepts = departmentConfigs.value
    .filter((d) => d.selected)
    .map((d) => ({
      code: d.code,
      classes: d.classes
    }))

  if (selectedDepts.length === 0) {
    showToast({ message: '请至少选择一个院系', type: 'warning' })
    return
  }

  creatingFromTemplate.value = true
  try {
    const response = await fetch(
      `/api/universities/${selectedUniversity.value.code}/create-organization`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ departments: selectedDepts })
      }
    )

    const result = await response.json()

    if (response.ok) {
      showToast({ message: result.message, type: 'success' })
      showUniversityPicker.value = false
      fetchOrganizations()
    } else {
      showToast({ message: result.message || '创建失败', type: 'error' })
    }
  } catch (error) {
    console.error('Failed to create from template:', error)
    showToast({ message: '创建失败', type: 'error' })
  } finally {
    creatingFromTemplate.value = false
  }
}

async function handleSubmit() {
  try {
    const url = editingOrg.value
      ? `/api/organizations/${editingOrg.value.id}`
      : '/api/organizations'
    const method = editingOrg.value ? 'PUT' : 'POST'

    const response = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form.value)
    })

    if (response.ok) {
      showForm.value = false
      fetchOrganizations()
    } else {
      const error = await response.json()
      showToast({ message: error.message || '保存失败', type: 'error' })
    }
  } catch (error) {
    console.error('Failed to save organization:', error)
    showToast({ message: '保存失败', type: 'error' })
  }
}

async function handleDelete(id: string) {
  const confirmed = await confirm({
    title: '删除组织',
    message: '确定要删除该组织吗？删除后其下属组织也会被删除。',
    type: 'danger',
    confirmText: '删除',
    cancelText: '取消'
  })
  if (!confirmed) return

  try {
    await fetch(`/api/organizations/${id}`, { method: 'DELETE' })
    fetchOrganizations()
  } catch (error) {
    console.error('Failed to delete organization:', error)
  }
}

async function refreshInviteCode(orgId: string) {
  try {
    const response = await fetch(`/api/organizations/${orgId}/refresh-invite-code`, {
      method: 'POST'
    })
    const data = await response.json()
    updateOrgInviteCode(organizations.value, orgId, data.inviteCode)
    showToast({ message: `新的邀请码: ${data.inviteCode}`, type: 'success' })
  } catch (error) {
    console.error('Failed to refresh invite code:', error)
  }
}

function updateOrgInviteCode(orgs: Organization[], orgId: string, newCode: string) {
  for (const org of orgs) {
    if (org.id === orgId) {
      org.inviteCode = newCode
      return true
    }
    if (org.children && updateOrgInviteCode(org.children, orgId, newCode)) {
      return true
    }
  }
  return false
}

function fallbackCopyText(text: string): boolean {
  try {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.setAttribute('readonly', 'true')
    textarea.style.position = 'fixed'
    textarea.style.top = '-9999px'
    textarea.style.left = '-9999px'
    document.body.appendChild(textarea)
    textarea.focus()
    textarea.select()
    const copied = document.execCommand('copy')
    document.body.removeChild(textarea)
    return copied
  } catch {
    return false
  }
}

async function copyInviteCode(code: string) {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(code)
      showToast({ message: '邀请码已复制到剪贴板', type: 'success' })
      return
    }

    const copied = fallbackCopyText(code)
    if (copied) {
      showToast({ message: '邀请码已复制到剪贴板', type: 'success' })
      return
    }

    showToast({ message: '复制失败，请手动选择邀请码复制', type: 'warning' })
  } catch {
    const copied = fallbackCopyText(code)
    if (copied) {
      showToast({ message: '邀请码已复制到剪贴板', type: 'success' })
      return
    }
    showToast({ message: '复制失败，请手动选择邀请码复制', type: 'warning' })
  }
}
</script>

<template>
  <div class="line-container page-container">
    <div class="page-header center-header">
      <h1 class="page-title">组织架构管理</h1>
      <p class="subtitle">管理学校、学院、班级的层级结构</p>
    </div>

    <div class="toolbar-actions" v-if="isAdmin">
      <button class="line-btn primary-btn" @click="openUniversityPicker">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
          <path d="M6 12v5c0 1 2 3 6 3s6-2 6-3v-5" />
        </svg>
        从高校模板创建
      </button>
      <button class="line-btn outline-btn" @click="openAddForm(null, 'SCHOOL')">
        手动添加学校
      </button>
    </div>

    <div class="line-card table-card">
      <div v-if="loading" class="line-loading">
        <div class="spinner"></div>
      </div>

      <div v-else-if="organizations.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg
            width="64"
            height="64"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1"
          >
            <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
            <path d="M6 12v5c0 1 2 3 6 3s6-2 6-3v-5" />
          </svg>
        </div>
        <h3>暂无组织数据</h3>
        <p v-if="isAdmin">您可以从高校模板快速创建组织架构，或手动添加学校</p>
        <p v-else>暂无组织架构信息，请联系管理员创建</p>
        <div class="empty-actions" v-if="isAdmin">
          <button class="line-btn primary-btn" @click="openUniversityPicker">从高校模板创建</button>
          <button class="line-btn text-btn" @click="openAddForm(null, 'SCHOOL')">
            手动添加学校
          </button>
        </div>
      </div>

      <div v-else class="org-tree-list">
        <template v-for="school in organizations" :key="school.id">
          <!-- School Row -->
          <div class="org-row school-row">
            <div class="row-content">
              <span class="row-icon school-icon" v-html="typeIcons.SCHOOL"></span>
              <div class="row-info">
                <span class="row-name">{{ school.name }}</span>
                <span class="row-code">{{ school.code }}</span>
              </div>
              <span :class="['status-badge', school.status.toLowerCase()]">{{
                school.status === 'ACTIVE' ? '启用' : '停用'
              }}</span>
              <div class="row-actions" v-if="isAdmin">
                <button
                  class="action-btn"
                  @click="openAddForm(school.id, 'DEPARTMENT')"
                  title="添加学院"
                >
                  <svg
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <line x1="12" y1="5" x2="12" y2="19"></line>
                    <line x1="5" y1="12" x2="19" y2="12"></line>
                  </svg>
                </button>
                <button class="action-btn" @click="openEditForm(school)" title="编辑">
                  <svg
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                    <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                  </svg>
                </button>
                <button class="action-btn danger" @click="handleDelete(school.id)" title="删除">
                  <svg
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path
                      d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"
                    />
                  </svg>
                </button>
              </div>
            </div>

            <!-- Departments -->
            <div v-if="school.children?.length" class="children-block">
              <template v-for="dept in school.children" :key="dept.id">
                <div class="org-row dept-row">
                  <div class="row-content">
                    <span class="tree-line"></span>
                    <span class="row-icon dept-icon" v-html="typeIcons.DEPARTMENT"></span>
                    <div class="row-info">
                      <span class="row-name">{{ dept.name }}</span>
                      <span class="row-code">{{ dept.code }}</span>
                    </div>
                    <div class="row-actions" v-if="isAdmin">
                      <button
                        class="action-btn"
                        @click="openAddForm(dept.id, 'CLASS')"
                        title="添加班级"
                      >
                        <svg
                          width="16"
                          height="16"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="2"
                        >
                          <line x1="12" y1="5" x2="12" y2="19"></line>
                          <line x1="5" y1="12" x2="19" y2="12"></line>
                        </svg>
                      </button>
                      <button class="action-btn" @click="openEditForm(dept)" title="编辑">
                        <svg
                          width="16"
                          height="16"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="2"
                        >
                          <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                          <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                        </svg>
                      </button>
                      <button class="action-btn danger" @click="handleDelete(dept.id)" title="删除">
                        <svg
                          width="16"
                          height="16"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="2"
                        >
                          <polyline points="3 6 5 6 21 6"></polyline>
                          <path
                            d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"
                          />
                        </svg>
                      </button>
                    </div>
                  </div>

                  <!-- Classes -->
                  <div v-if="dept.children?.length" class="children-block">
                    <div v-for="cls in dept.children" :key="cls.id" class="org-row class-row">
                      <div class="row-content">
                        <span class="tree-line"></span>
                        <span class="row-icon class-icon" v-html="typeIcons.CLASS"></span>
                        <div class="row-info">
                          <span class="row-name">{{ cls.name }}</span>
                          <span class="row-code">{{ cls.code }}</span>
                        </div>
                        <span
                          v-if="cls.inviteCode"
                          class="invite-code-pill"
                          @click="copyInviteCode(cls.inviteCode!)"
                          title="点击复制"
                        >
                          <svg
                            width="12"
                            height="12"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                          >
                            <path
                              d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"
                            />
                          </svg>
                          {{ cls.inviteCode }}
                        </span>
                        <div class="row-actions" v-if="isAdmin">
                          <button
                            v-if="cls.inviteCode"
                            class="action-btn"
                            @click="refreshInviteCode(cls.id)"
                            title="刷新邀请码"
                          >
                            <svg
                              width="16"
                              height="16"
                              viewBox="0 0 24 24"
                              fill="none"
                              stroke="currentColor"
                              stroke-width="2"
                            >
                              <polyline points="23 4 23 10 17 10"></polyline>
                              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                            </svg>
                          </button>
                          <button class="action-btn" @click="openEditForm(cls)" title="编辑">
                            <svg
                              width="16"
                              height="16"
                              viewBox="0 0 24 24"
                              fill="none"
                              stroke="currentColor"
                              stroke-width="2"
                            >
                              <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                              <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                            </svg>
                          </button>
                          <button
                            class="action-btn danger"
                            @click="handleDelete(cls.id)"
                            title="删除"
                          >
                            <svg
                              width="16"
                              height="16"
                              viewBox="0 0 24 24"
                              fill="none"
                              stroke="currentColor"
                              stroke-width="2"
                            >
                              <polyline points="3 6 5 6 21 6"></polyline>
                              <path
                                d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"
                              />
                            </svg>
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- Modals (Add/Edit Form) -->
    <div v-if="showForm" class="modal-backdrop" @click.self="showForm = false">
      <div class="line-modal sm-modal">
        <div class="modal-header">
          <h2>{{ editingOrg ? '编辑组织' : '添加组织' }}</h2>
          <button class="close-btn" @click="showForm = false">×</button>
        </div>
        <form @submit.prevent="handleSubmit">
          <div class="modal-body">
            <div class="form-group">
              <GoogleSelect
                v-model="form.type"
                :options="typeOptions"
                label="组织类型"
                placeholder="请选择组织类型"
                :disabled="!!editingOrg"
              />
            </div>
            <div class="form-group">
              <label>名称</label>
              <input
                v-model="form.name"
                type="text"
                class="line-input"
                required
                placeholder="请输入名称"
              />
            </div>
            <div class="form-group">
              <label>编码</label>
              <input
                v-model="form.code"
                type="text"
                class="line-input"
                required
                placeholder="唯一编码"
              />
            </div>
            <div class="form-group">
              <label>排序</label>
              <input v-model.number="form.sortOrder" type="number" class="line-input" />
            </div>
            <div class="form-group">
              <GoogleSelect
                v-model="form.status"
                :options="statusOptions"
                label="状态"
                placeholder="请选择状态"
              />
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="line-btn text-btn" @click="showForm = false">取消</button>
            <button type="submit" class="line-btn primary-btn">保存</button>
          </div>
        </form>
      </div>
    </div>

    <!-- University Picker Modal -->
    <div
      v-if="showUniversityPicker"
      class="modal-backdrop"
      @click.self="showUniversityPicker = false"
    >
      <div class="line-modal lg-modal">
        <template v-if="!selectedUniversity">
          <div class="modal-header">
            <h2>选择高校</h2>
          </div>
          <div class="modal-body">
            <div class="search-box">
              <input
                v-model="searchKeyword"
                type="text"
                placeholder="搜索高校名称、代码或城市..."
                class="line-input search-input"
              />
            </div>
            <div class="university-list">
              <template v-for="(unis, province) in universitiesByProvince" :key="province">
                <div class="province-group">
                  <div class="province-name">{{ province }}</div>
                  <div class="university-grid">
                    <div
                      v-for="uni in unis"
                      :key="uni.code"
                      class="university-card"
                      @click="selectUniversity(uni)"
                    >
                      <div class="uni-name">{{ uni.name }}</div>
                      <div class="uni-info">
                        <span class="uni-city">{{ uni.city }}</span>
                        <span :class="['uni-type', uni.type.toLowerCase()]">{{ uni.type }}</span>
                      </div>
                      <div class="uni-depts">{{ uni.departments.length }} 个院系</div>
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </div>
          <div class="modal-footer">
            <button class="line-btn text-btn" @click="showUniversityPicker = false">取消</button>
          </div>
        </template>

        <template v-else>
          <div class="modal-header">
            <div class="header-with-back">
              <button class="icon-btn" @click="backToUniversityList">
                <svg
                  width="24"
                  height="24"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <polyline points="15 18 9 12 15 6"></polyline>
                </svg>
              </button>
              <h2>{{ selectedUniversity.name }}</h2>
            </div>
          </div>

          <div class="modal-body">
            <div class="config-summary">
              <div class="summary-item">
                <span class="label">已选院系</span> <span class="val">{{ selectedDeptCount }}</span>
              </div>
              <div class="summary-item">
                <span class="label">班级总数</span> <span class="val">{{ totalClassCount }}</span>
              </div>
            </div>

            <div class="department-list">
              <div
                v-for="dept in departmentConfigs"
                :key="dept.code"
                :class="['department-item', { selected: dept.selected }]"
              >
                <div class="dept-header" @click="toggleDepartment(dept.code)">
                  <div class="checkbox-wrapper">
                    <div v-if="dept.selected" class="line-checkbox checked">✓</div>
                    <div v-else class="line-checkbox"></div>
                  </div>
                  <span class="dept-name">{{ dept.name }}</span>
                  <span class="dept-count">{{ dept.classes.length }} 班</span>
                </div>
                <div v-if="dept.selected" class="class-list">
                  <div v-for="(cls, index) in dept.classes" :key="index" class="class-item">
                    <input
                      :value="cls.name"
                      @input="
                        updateClassName(dept.code, index, ($event.target as HTMLInputElement).value)
                      "
                      class="line-input sm-input"
                    />
                    <button
                      v-if="dept.classes.length > 1"
                      class="icon-btn danger sm"
                      @click="removeClass(dept.code, index)"
                    >
                      ×
                    </button>
                  </div>
                  <button
                    class="line-btn outline-btn sm-btn full-width"
                    @click="addClass(dept.code)"
                  >
                    + 添加班级
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="line-btn text-btn" @click="showUniversityPicker = false">取消</button>
            <button
              class="line-btn primary-btn"
              @click="createFromTemplate"
              :disabled="creatingFromTemplate || selectedDeptCount === 0"
            >
              {{ creatingFromTemplate ? '创建中...' : '创建组织架构' }}
            </button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Page Layout */
.toolbar-actions {
  display: flex;
  gap: 16px;
  margin-bottom: 32px;
}

/* Table / Tree */
.table-card {
  padding: 0;
  overflow: hidden;
  min-height: 400px;
}

.line-loading {
  padding: 64px;
  display: flex;
  justify-content: center;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--line-border);
  border-top-color: var(--line-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* Empty State */
.empty-state {
  padding: 64px 20px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--line-bg-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--line-text-secondary);
  margin-bottom: 24px;
}

.empty-state h3 {
  font-size: 1.1rem;
  font-weight: 600;
  margin: 0 0 8px;
  color: var(--line-text);
}

.empty-state p {
  color: var(--line-text-secondary);
  margin: 0 0 32px;
  max-width: 400px;
  line-height: 1.5;
}

.empty-actions {
  display: flex;
  gap: 12px;
}

/* Org Tree */
.org-tree-list {
  display: flex;
  flex-direction: column;
}

.org-row {
  border-bottom: 1px solid var(--line-border);
}

.org-row:last-child {
  border-bottom: none;
}

.row-content {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  gap: 16px;
  transition: background-color 0.2s;
  background: var(--line-card-bg);
}

.row-content:hover {
  background-color: var(--line-bg-soft);
}

/* Indentation via Blocks */
.children-block {
  display: flex;
  flex-direction: column;
}

.dept-row .row-content {
  padding-left: 48px;
  background: #fafafa;
}
:root[class~='dark'] .dept-row .row-content {
  background: rgba(255, 255, 255, 0.02);
}

.class-row .row-content {
  padding-left: 72px;
  background: #fcfcfc;
}
:root[class~='dark'] .class-row .row-content {
  background: rgba(255, 255, 255, 0.01);
}

.tree-line {
  width: 12px;
  height: 1px;
  background: var(--line-border);
  margin-right: -8px;
}

/* Icons */
.row-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.row-icon svg {
  width: 18px;
  height: 18px;
  stroke-width: 2px;
}

.school-icon {
  background: #e0f2fe;
  color: #0284c7;
}
.dept-icon {
  background: #dcfce7;
  color: #059669;
}
.class-icon {
  background: #fef3c7;
  color: #d97706;
}

.row-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.row-name {
  font-weight: 600;
  color: var(--line-text);
  font-size: 0.95rem;
}

.row-code {
  font-size: 0.8rem;
  color: var(--line-text-secondary);
  font-family: 'SF Mono', Consolas, monospace;
}

.status-badge {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 99px;
  font-weight: 600;
}
.status-badge.active {
  background: #dcfce7;
  color: #166534;
}
.status-badge.inactive {
  background: #fef2f2;
  color: #991b1b;
}

.invite-code-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--line-bg-soft);
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.8rem;
  font-family: monospace;
  color: var(--line-text-secondary);
  border: 1px solid var(--line-border);
  cursor: pointer;
  transition: all 0.2s;
  margin-right: 16px;
}
.invite-code-pill svg {
  width: 12px;
  height: 12px;
}

.invite-code-pill:hover {
  background: var(--line-bg);
  border-color: var(--line-primary);
  color: var(--line-primary);
}

.row-actions {
  display: flex;
  gap: 4px;
  margin-left: auto;
  flex-shrink: 0; /* Prevent shrinking */
}

.action-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--line-border);
  background: var(--line-bg);
  color: var(--line-text-secondary);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  opacity: 1;
  padding: 0; /* Clear padding to center SVG perfect */
}

/* Explicitly style SVGs to ensure visibility with super high specificity and force important props */
.action-btn svg {
  width: 16px !important;
  height: 16px !important;
  stroke: var(--line-text-secondary);
  stroke-width: 2px;
  fill: none !important;
  display: block !important;
  opacity: 1 !important;
  visibility: visible !important;
}

.action-btn:hover {
  background: var(--line-bg-soft);
  color: var(--line-primary);
  border-color: var(--line-text-secondary);
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.action-btn:hover svg {
  stroke: var(--line-primary);
}

.action-btn.danger {
  color: #ef4444;
  border-color: #fee2e2;
  background: #fffafa;
}

.action-btn.danger svg {
  stroke: #ef4444;
}

.action-btn.danger:hover {
  background: #fee2e2;
  border-color: #ef4444;
  color: #b91c1c;
}

.action-btn.danger:hover svg {
  stroke: #b91c1c;
}

/* Modal tweaks specific to this page */
.header-with-back {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-with-back h2 {
  margin: 0;
  font-size: 1.1rem;
  display: flex;
  align-items: center;
}

/* University Picker List */
.province-group {
  margin-bottom: 24px;
}
.province-name {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--line-text-secondary);
  margin-bottom: 12px;
}

.university-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.university-card {
  background: var(--line-card-bg);
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-md);
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.university-card:hover {
  border-color: var(--line-primary);
  box-shadow: var(--line-shadow-md);
  transform: translateY(-2px);
}

.uni-name {
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--line-text);
}
.uni-info {
  display: flex;
  gap: 8px;
  font-size: 0.8rem;
  color: var(--line-text-secondary);
  margin-bottom: 8px;
}
.uni-type {
  padding: 2px 6px;
  background: var(--line-bg-soft);
  border-radius: 4px;
  font-weight: 500;
}
.uni-depts {
  font-size: 0.8rem;
  color: var(--line-text-secondary);
}

/* Picker Config */
.config-summary {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 16px;
  background: var(--line-bg-soft);
  border-radius: var(--line-radius-md);
  margin-bottom: 24px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.summary-item .label {
  font-size: 0.8rem;
  color: var(--line-text-secondary);
  margin-bottom: 4px;
}
.summary-item .val {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--line-primary);
}

.department-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
  padding-right: 8px;
}

.department-item {
  border: 1px solid var(--line-border);
  border-radius: var(--line-radius-md);
  overflow: hidden;
  transition: all 0.2s;
}

.department-item.selected {
  border-color: var(--line-primary);
  background: #f0f9ff;
} /* Light blue tint for selected */
:root[class~='dark'] .department-item.selected {
  background: rgba(14, 165, 233, 0.1);
}

.dept-header {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  user-select: none;
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
}
.line-checkbox {
  width: 18px;
  height: 18px;
  border: 2px solid var(--line-border);
  border-radius: 4px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
}
.department-item.selected .line-checkbox {
  background: var(--line-primary);
  border-color: var(--line-primary);
}

.dept-name {
  flex: 1;
  font-weight: 500;
  font-size: 0.95rem;
}
.dept-count {
  font-size: 0.8rem;
  color: var(--line-text-secondary);
}

.class-list {
  padding: 16px;
  background: rgba(255, 255, 255, 0.5);
  border-top: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.class-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.sm {
  width: 28px;
  height: 28px;
}

.sm-btn {
  padding: 6px 12px;
  font-size: 0.8rem;
}
.full-width {
  width: 100%;
  margin-top: 8px;
  border-style: dashed;
}

/* Modal Styles */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: fadeIn 0.2s ease;
}

.line-modal {
  background: var(--line-bg);
  width: 100%;
  max-width: 500px;
  border-radius: var(--line-radius-lg);
  box-shadow: var(--line-shadow-xl);
  border: 1px solid var(--line-border);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.line-modal.lg-modal {
  max-width: 800px;
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--line-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-header h2 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--line-text);
}

.close-btn {
  background: transparent;
  border: none;
  font-size: 1.5rem;
  line-height: 1;
  color: var(--line-text-secondary);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: var(--line-bg-soft);
  color: var(--line-error);
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  padding: 16px 24px;
  border-top: none;
  background: transparent;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  border-bottom-left-radius: var(--line-radius-lg);
  border-bottom-right-radius: var(--line-radius-lg);
}

/* Modal Form Elements */
.modal-body .form-group {
  margin-bottom: 20px;
}

.modal-body .form-group label {
  display: block;
  font-size: 14px;
  color: var(--line-text);
  margin-bottom: 6px;
  font-weight: 500;
}

.modal-body .form-group:last-child {
  margin-bottom: 0;
}

/* Custom Select Style */
select.line-select {
  appearance: none !important;
  -webkit-appearance: none !important;
  -moz-appearance: none !important;
  background-color: var(--line-bg) !important;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2364748B' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e") !important;
  background-repeat: no-repeat !important;
  background-position: right 12px center !important;
  background-size: 16px !important;
  padding: 12px 40px 12px 16px !important;
  border: 1px solid var(--line-border) !important;
  border-radius: var(--radius) !important;
  color: var(--line-text) !important;
  cursor: pointer;
  line-height: normal;
}

select.line-select:focus {
  border-color: var(--line-primary) !important;
  outline: none !important;
  box-shadow: 0 0 0 2px var(--line-primary-10) !important;
}
</style>
