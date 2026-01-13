<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'

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

// 过滤后的高校列表
const filteredUniversities = computed(() => {
  if (!searchKeyword.value) return universities.value
  const keyword = searchKeyword.value.toLowerCase()
  return universities.value.filter(u => 
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
    if (!groups[uni.province]) {
      groups[uni.province] = []
    }
    groups[uni.province].push(uni)
  }
  return groups
})

// 已选择的院系数量
const selectedDeptCount = computed(() => 
  departmentConfigs.value.filter(d => d.selected).length
)

// 总班级数量
const totalClassCount = computed(() => 
  departmentConfigs.value
    .filter(d => d.selected)
    .reduce((sum, d) => sum + d.classes.length, 0)
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

function openAddForm(parentId: string | null = null, type: 'SCHOOL' | 'DEPARTMENT' | 'CLASS' = 'SCHOOL') {
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
  departmentConfigs.value = uni.departments.map(dept => ({
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
  const dept = departmentConfigs.value.find(d => d.code === deptCode)
  if (dept) {
    dept.selected = !dept.selected
  }
}

function addClass(deptCode: string) {
  const dept = departmentConfigs.value.find(d => d.code === deptCode)
  if (dept) {
    const classNum = dept.classes.length + 1
    dept.classes.push({
      name: `${new Date().getFullYear()}级${classNum}班`,
      code: `${deptCode}_C${classNum}`
    })
  }
}

function removeClass(deptCode: string, index: number) {
  const dept = departmentConfigs.value.find(d => d.code === deptCode)
  if (dept && dept.classes.length > 1) {
    dept.classes.splice(index, 1)
  }
}

function updateClassName(deptCode: string, index: number, name: string) {
  const dept = departmentConfigs.value.find(d => d.code === deptCode)
  if (dept) {
    dept.classes[index].name = name
  }
}

async function createFromTemplate() {
  if (!selectedUniversity.value) return
  
  const selectedDepts = departmentConfigs.value
    .filter(d => d.selected)
    .map(d => ({
      code: d.code,
      classes: d.classes
    }))

  if (selectedDepts.length === 0) {
    alert('请至少选择一个院系')
    return
  }

  creatingFromTemplate.value = true
  try {
    const response = await fetch(`/api/universities/${selectedUniversity.value.code}/create-organization`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ departments: selectedDepts })
    })
    
    const result = await response.json()
    
    if (response.ok) {
      alert(result.message)
      showUniversityPicker.value = false
      fetchOrganizations()
    } else {
      alert(result.message || '创建失败')
    }
  } catch (error) {
    console.error('Failed to create from template:', error)
    alert('创建失败')
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
      alert(error.message || '保存失败')
    }
  } catch (error) {
    console.error('Failed to save organization:', error)
    alert('保存失败')
  }
}

async function handleDelete(id: string) {
  if (!confirm('确定要删除该组织吗？删除后其下属组织也会被删除。')) return
  
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
    alert(`新的邀请码: ${data.inviteCode}`)
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

function copyInviteCode(code: string) {
  navigator.clipboard.writeText(code)
  alert('邀请码已复制到剪贴板')
}
</script>

<template>
  <div class="org-manage-view">
    <div class="page-header">
      <h1>组织架构管理</h1>
      <p class="subtitle">管理学校、学院、班级的层级结构</p>
    </div>

    <div class="toolbar">
      <button class="google-btn primary-btn" @click="openUniversityPicker">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 10v6M2 10l10-5 10 5-10 5z"/>
          <path d="M6 12v5c0 1 2 3 6 3s6-2 6-3v-5"/>
        </svg>
        从高校模板创建
      </button>
      <button class="google-btn secondary-btn" @click="openAddForm(null, 'SCHOOL')">
        手动添加学校
      </button>
    </div>

    <div class="content-card">
      <div v-if="loading" class="loading">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
      
      <div v-else-if="organizations.length === 0" class="empty-state">
        <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="#9aa0a6" stroke-width="1">
          <path d="M22 10v6M2 10l10-5 10 5-10 5z"/>
          <path d="M6 12v5c0 1 2 3 6 3s6-2 6-3v-5"/>
        </svg>
        <h3>暂无组织数据</h3>
        <p>您可以从高校模板快速创建组织架构，或手动添加学校</p>
        <div class="empty-actions">
          <button class="google-btn primary-btn" @click="openUniversityPicker">从高校模板创建</button>
          <button class="google-btn text-btn" @click="openAddForm(null, 'SCHOOL')">手动添加学校</button>
        </div>
      </div>

      <div v-else class="org-tree">
        <template v-for="school in organizations" :key="school.id">
          <div class="org-node school-node">
            <div class="node-content">
              <span class="node-icon" v-html="typeIcons.SCHOOL"></span>
              <span class="node-name">{{ school.name }}</span>
              <span class="node-code">{{ school.code }}</span>
              <span :class="['node-status', school.status.toLowerCase()]">{{ school.status === 'ACTIVE' ? '启用' : '停用' }}</span>
              <div class="node-actions">
                <button class="icon-btn" @click="openAddForm(school.id, 'DEPARTMENT')" title="添加学院">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="12" y1="5" x2="12" y2="19"></line>
                    <line x1="5" y1="12" x2="19" y2="12"></line>
                  </svg>
                </button>
                <button class="icon-btn" @click="openEditForm(school)" title="编辑">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                </button>
                <button class="icon-btn danger" @click="handleDelete(school.id)" title="删除">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
                  </svg>
                </button>
              </div>
            </div>

            <!-- Departments -->
            <div v-if="school.children?.length" class="children">
              <template v-for="dept in school.children" :key="dept.id">
                <div class="org-node dept-node">
                  <div class="node-content">
                    <span class="node-icon" v-html="typeIcons.DEPARTMENT"></span>
                    <span class="node-name">{{ dept.name }}</span>
                    <span class="node-code">{{ dept.code }}</span>
                    <div class="node-actions">
                      <button class="icon-btn" @click="openAddForm(dept.id, 'CLASS')" title="添加班级">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <line x1="12" y1="5" x2="12" y2="19"></line>
                          <line x1="5" y1="12" x2="19" y2="12"></line>
                        </svg>
                      </button>
                      <button class="icon-btn" @click="openEditForm(dept)" title="编辑">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                          <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
                        </svg>
                      </button>
                      <button class="icon-btn danger" @click="handleDelete(dept.id)" title="删除">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="3 6 5 6 21 6"></polyline>
                          <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
                        </svg>
                      </button>
                    </div>
                  </div>

                  <!-- Classes -->
                  <div v-if="dept.children?.length" class="children">
                    <div v-for="cls in dept.children" :key="cls.id" class="org-node class-node">
                      <div class="node-content">
                        <span class="node-icon" v-html="typeIcons.CLASS"></span>
                        <span class="node-name">{{ cls.name }}</span>
                        <span class="node-code">{{ cls.code }}</span>
                        <span v-if="cls.inviteCode" class="invite-code" @click="copyInviteCode(cls.inviteCode!)" title="点击复制邀请码">
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/>
                          </svg>
                          {{ cls.inviteCode }}
                        </span>
                        <div class="node-actions">
                          <button v-if="cls.inviteCode" class="icon-btn" @click="refreshInviteCode(cls.id)" title="刷新邀请码">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                              <polyline points="23 4 23 10 17 10"></polyline>
                              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                            </svg>
                          </button>
                          <button class="icon-btn" @click="openEditForm(cls)" title="编辑">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                              <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                              <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
                            </svg>
                          </button>
                          <button class="icon-btn danger" @click="handleDelete(cls.id)" title="删除">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                              <polyline points="3 6 5 6 21 6"></polyline>
                              <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
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

    <!-- 手动添加/编辑表单 Modal -->
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal-content">
        <h2>{{ editingOrg ? '编辑组织' : '添加组织' }}</h2>
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>组织类型</label>
            <select v-model="form.type" class="google-input" :disabled="!!editingOrg">
              <option value="SCHOOL">学校</option>
              <option value="DEPARTMENT">学院</option>
              <option value="CLASS">班级</option>
            </select>
          </div>
          <div class="form-group">
            <label>名称</label>
            <input v-model="form.name" type="text" class="google-input" required placeholder="请输入名称" />
          </div>
          <div class="form-group">
            <label>编码</label>
            <input v-model="form.code" type="text" class="google-input" required placeholder="唯一编码" />
          </div>
          <div class="form-group">
            <label>排序</label>
            <input v-model.number="form.sortOrder" type="number" class="google-input" />
          </div>
          <div class="form-group">
            <label>状态</label>
            <select v-model="form.status" class="google-input">
              <option value="ACTIVE">启用</option>
              <option value="INACTIVE">禁用</option>
            </select>
          </div>
          <div class="form-actions">
            <button type="button" class="google-btn text-btn" @click="showForm = false">取消</button>
            <button type="submit" class="google-btn primary-btn">保存</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 高校模板选择 Modal -->
    <div v-if="showUniversityPicker" class="modal-overlay" @click.self="showUniversityPicker = false">
      <div class="modal-content university-picker">
        <!-- 高校列表 -->
        <template v-if="!selectedUniversity">
          <div class="picker-header">
            <h2>选择高校</h2>
            <p>从全国知名高校中选择，快速创建组织架构</p>
          </div>
          
          <div class="search-box">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#5f6368" stroke-width="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input 
              v-model="searchKeyword" 
              type="text" 
              placeholder="搜索高校名称、代码或城市..."
              class="search-input"
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

          <div class="picker-footer">
            <button class="google-btn text-btn" @click="showUniversityPicker = false">取消</button>
          </div>
        </template>

        <!-- 院系和班级配置 -->
        <template v-else>
          <div class="picker-header">
            <button class="back-btn" @click="backToUniversityList">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="15 18 9 12 15 6"></polyline>
              </svg>
              返回
            </button>
            <h2>{{ selectedUniversity.name }}</h2>
            <p>选择要创建的院系和班级，每个班级将自动生成唯一识别码</p>
          </div>

          <div class="config-summary">
            <div class="summary-item">
              <span class="summary-label">已选院系</span>
              <span class="summary-value">{{ selectedDeptCount }} 个</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">班级总数</span>
              <span class="summary-value">{{ totalClassCount }} 个</span>
            </div>
          </div>

          <div class="department-list">
            <div 
              v-for="dept in departmentConfigs" 
              :key="dept.code" 
              :class="['department-item', { selected: dept.selected }]"
            >
              <div class="dept-header" @click="toggleDepartment(dept.code)">
                <div class="dept-checkbox">
                  <svg v-if="dept.selected" width="20" height="20" viewBox="0 0 24 24" fill="#1a73e8">
                    <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z"/>
                  </svg>
                  <div v-else class="checkbox-empty"></div>
                </div>
                <span class="dept-name">{{ dept.name }}</span>
                <span class="dept-class-count">{{ dept.classes.length }} 个班级</span>
              </div>

              <div v-if="dept.selected" class="class-list">
                <div v-for="(cls, index) in dept.classes" :key="index" class="class-item">
                  <input 
                    :value="cls.name" 
                    @input="updateClassName(dept.code, index, ($event.target as HTMLInputElement).value)"
                    class="class-input"
                    placeholder="班级名称"
                  />
                  <button 
                    v-if="dept.classes.length > 1"
                    class="remove-class-btn" 
                    @click="removeClass(dept.code, index)"
                    title="删除班级"
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <line x1="18" y1="6" x2="6" y2="18"></line>
                      <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                  </button>
                </div>
                <button class="add-class-btn" @click="addClass(dept.code)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="12" y1="5" x2="12" y2="19"></line>
                    <line x1="5" y1="12" x2="19" y2="12"></line>
                  </svg>
                  添加班级
                </button>
              </div>
            </div>
          </div>

          <div class="picker-footer">
            <button class="google-btn text-btn" @click="showUniversityPicker = false">取消</button>
            <button 
              class="google-btn primary-btn" 
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
.org-manage-view {
  padding: 24px;
  max-width: 1200px;
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

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.content-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #dadce0;
  padding: 24px;
  min-height: 400px;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  color: #5f6368;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #dadce0;
  border-top-color: #1a73e8;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 48px;
}

.empty-state h3 {
  margin: 16px 0 8px;
  color: #202124;
}

.empty-state p {
  color: #5f6368;
  margin: 0 0 24px;
}

.empty-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.org-tree {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.org-node {
  border: 1px solid #dadce0;
  border-radius: 8px;
  overflow: hidden;
}

.school-node { border-color: #4285f4; }
.dept-node { border-color: #34a853; margin-left: 24px; }
.class-node { border-color: #fbbc04; margin-left: 24px; }

.node-content {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f8f9fa;
}

.school-node > .node-content { background: #e8f0fe; }
.dept-node > .node-content { background: #e6f4ea; }
.class-node > .node-content { background: #fef7e0; }

.node-icon { display: flex; align-items: center; color: #5f6368; }
.node-name { font-weight: 500; color: #202124; }
.node-code { color: #5f6368; font-size: 13px; }

.node-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
}

.node-status.active { background: #e6f4ea; color: #1e8e3e; }
.node-status.inactive { background: #fce8e6; color: #d93025; }

.node-actions {
  margin-left: auto;
  display: flex;
  gap: 4px;
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

.icon-btn:hover { background: rgba(0,0,0,0.08); }
.icon-btn.danger:hover { background: #fce8e6; color: #d93025; }

.children {
  padding: 8px 0 8px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.invite-code {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: monospace;
  font-size: 13px;
  background: #e8f0fe;
  color: #1a73e8;
  padding: 4px 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
  font-weight: 500;
}

.invite-code svg {
  flex-shrink: 0;
}

.invite-code:hover { background: #d2e3fc; }

/* Modal styles */
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
  max-height: 90vh;
  overflow-y: auto;
}

.modal-content h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 500;
}

/* University Picker specific styles */
.university-picker {
  max-width: 800px;
}

.picker-header {
  margin-bottom: 20px;
}

.picker-header p {
  color: #5f6368;
  margin: 0;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: #1a73e8;
  cursor: pointer;
  padding: 0;
  margin-bottom: 12px;
  font-size: 14px;
}

.back-btn:hover { text-decoration: underline; }

.search-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid #dadce0;
  border-radius: 24px;
  margin-bottom: 20px;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
}

.university-list {
  max-height: 400px;
  overflow-y: auto;
}

.province-group {
  margin-bottom: 20px;
}

.province-name {
  font-weight: 500;
  color: #5f6368;
  margin-bottom: 12px;
  padding-left: 4px;
}

.university-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.university-card {
  padding: 16px;
  border: 1px solid #dadce0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.university-card:hover {
  border-color: #1a73e8;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.uni-name {
  font-weight: 500;
  margin-bottom: 8px;
  color: #202124;
}

.uni-info {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.uni-city { color: #5f6368; font-size: 13px; }

.uni-type {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.uni-type.985 { background: #fce8e6; color: #d93025; }
.uni-type.211 { background: #e8f0fe; color: #1a73e8; }

.uni-depts { font-size: 12px; color: #5f6368; }

/* Config styles */
.config-summary {
  display: flex;
  gap: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-label { font-size: 12px; color: #5f6368; }
.summary-value { font-size: 20px; font-weight: 500; color: #1a73e8; }

.department-list {
  max-height: 400px;
  overflow-y: auto;
}

.department-item {
  border: 1px solid #dadce0;
  border-radius: 8px;
  margin-bottom: 12px;
  overflow: hidden;
}

.department-item.selected { border-color: #1a73e8; }

.dept-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  background: #f8f9fa;
}

.department-item.selected .dept-header { background: #e8f0fe; }

.dept-checkbox {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.checkbox-empty {
  width: 18px;
  height: 18px;
  border: 2px solid #5f6368;
  border-radius: 3px;
}

.dept-name { flex: 1; font-weight: 500; }
.dept-class-count { font-size: 13px; color: #5f6368; }

.class-list {
  padding: 12px 16px;
  background: #fff;
}

.class-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.class-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  font-size: 14px;
}

.class-input:focus {
  outline: none;
  border-color: #1a73e8;
}

.remove-class-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  color: #5f6368;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-class-btn:hover { background: #fce8e6; color: #d93025; }

.add-class-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border: 1px dashed #dadce0;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  color: #1a73e8;
  font-size: 13px;
}

.add-class-btn:hover { background: #e8f0fe; }

.picker-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #dadce0;
}

/* Form styles */
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

/* Button styles */
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
  transition: background 0.2s;
}

.primary-btn { background: #1a73e8; color: #fff; }
.primary-btn:hover { background: #1557b0; }
.primary-btn:disabled { background: #dadce0; cursor: not-allowed; }

.secondary-btn { background: #fff; color: #1a73e8; border: 1px solid #dadce0; }
.secondary-btn:hover { background: #f8f9fa; }

.text-btn { background: transparent; color: #1a73e8; }
.text-btn:hover { background: #e8f0fe; }
</style>
