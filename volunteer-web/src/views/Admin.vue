<template>
  <div class="admin-page">

    <el-tabs v-model="activeTab">
      <!-- ========== Tab 1: 活动管理 ========== -->
      <el-tab-pane label="活动管理" name="activities">
        <el-button type="primary" @click="$router.push('/admin/create-activity')" style="margin-bottom: 16px">
          创建活动
        </el-button>
        <el-table :data="activities" v-loading="loading">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="title" label="活动标题" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="signedCount" label="报名人数" width="100" />
          <el-table-column label="操作" min-width="420">
            <template #default="{ row }">
              <el-button v-if="row.status === 'draft'" size="small" @click="publishActivity(row.id)">
                发布
              </el-button>
              <el-button size="small" @click="openEditActivity(row)">编辑</el-button>
              <el-button size="small" @click="$router.push(`/activity/${row.id}`)">详情</el-button>
              <el-button size="small" type="success" @click="showQRCode(row)">签到码</el-button>
              <el-button size="small" type="warning" @click="$router.push(`/admin/geofence/${row.id}`)">
                围栏
              </el-button>
              <el-button size="small" type="danger" @click="handleDeleteActivity(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ========== Tab 2: 组织者审批（P2-UPM-04） ========== -->
      <el-tab-pane label="组织者审批" name="organizer">
        <el-table :data="applies" v-loading="loadingApplies">
          <el-table-column prop="id" label="申请ID" width="80" />
          <el-table-column prop="userId" label="用户ID" width="80" />
          <el-table-column prop="organization" label="所属机构" width="150" />
          <el-table-column prop="reason" label="申请理由" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'pending' ? 'warning' : row.status === 'approved' ? 'success' : 'danger'">
                {{ row.status === 'pending' ? '待审批' : row.status === 'approved' ? '已通过' : '已拒绝' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <template v-if="row.status === 'pending'">
                <el-button size="small" type="success" @click="reviewApply(row.id, true)">通过</el-button>
                <el-button size="small" type="danger" @click="reviewApply(row.id, false)">拒绝</el-button>
              </template>
              <span v-else style="color: #999">已处理</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="applies.length === 0 && !loadingApplies" description="暂无待审批的组织者申请" />
      </el-tab-pane>

      <!-- ========== Tab 3: 时长审核 ========== -->
      <el-tab-pane label="时长审核" name="hours">
        <div class="verify-section">
          <!-- 第一步：选择活动 -->
          <el-select v-model="selectedActivityId" placeholder="请选择活动" @change="loadSignups" clearable>
            <el-option
              v-for="act in activities"
              :key="act.id"
              :label="act.title"
              :value="act.id"
            />
          </el-select>

          <!-- 第二步：显示报名列表 -->
          <el-table v-if="signups.length > 0" :data="signups" v-loading="loadingSignups" style="margin-top: 16px">
            <el-table-column prop="id" label="记录ID" width="80" />
            <el-table-column prop="activityId" label="活动ID" width="80" />
            <el-table-column prop="userId" label="用户ID" width="80" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="volunteerHours" label="志愿时长(h)" width="120">
              <template #default="{ row }">
                {{ row.volunteerHours ?? '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="signInTime" label="签到时间" width="180" />
            <el-table-column prop="signOutTime" label="签退时间" width="180" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'signed_out' && !row.hourVerified"
                  size="small"
                  type="warning"
                  @click="verifyHours(row.id)"
                >
                  审核通过
                </el-button>
                <el-tag v-else-if="row.hourVerified" type="success">已审核</el-tag>
                <span v-else style="color: #999">—</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else-if="selectedActivityId" description="该活动暂无报名记录" />
          <el-empty v-else description="请先选择一个活动" />
        </div>
      </el-tab-pane>

      <!-- ========== Tab 4: 账号管理 ========== -->
      <el-tab-pane label="账号管理" name="users">
        <el-button type="primary" @click="openCreateUser" style="margin-bottom: 16px">新增用户</el-button>
        <el-table :data="users" v-loading="loadingUsers">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="studentId" label="学号" width="120" />
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="role" label="角色" width="90">
            <template #default="{ row }">
              <el-tag :type="row.role === 'admin' ? 'danger' : row.role === 'organizer' ? 'warning' : ''" size="small">
                {{ row.role === 'admin' ? '管理员' : row.role === 'organizer' ? '组织者' : '学生' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column prop="totalHours" label="志愿时长(h)" width="100" />
          <el-table-column label="操作" min-width="240">
            <template #default="{ row }">
              <el-button size="small" @click="openEditUser(row)">编辑</el-button>
              <el-button size="small" type="warning" @click="openResetPwd(row)">重置密码</el-button>
              <el-button size="small" type="danger" @click="handleDeleteUser(row)"
                :disabled="row.role === 'admin'">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- ========== 活动编辑弹窗 ========== -->
    <el-dialog v-model="activityDialogVisible" :title="'编辑活动：' + editingActivity?.title" width="560px" top="5vh">
      <el-form :model="activityForm" label-width="100px">
        <el-form-item label="活动标题">
          <el-input v-model="activityForm.title" placeholder="请输入活动标题"
            :disabled="!canEditAllFields" />
        </el-form-item>
        <el-form-item label="活动描述">
          <el-input v-model="activityForm.description" type="textarea" :rows="3"
            placeholder="请输入活动描述" />
        </el-form-item>
        <el-form-item label="活动地点">
          <el-input v-model="activityForm.locationName" placeholder="如：图书馆一楼"
            :disabled="!canEditAllFields" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input-number v-model="activityForm.longitude" :precision="6" :step="0.001"
            :disabled="!canEditAllFields" style="width: 100%" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input-number v-model="activityForm.latitude" :precision="6" :step="0.001"
            :disabled="!canEditAllFields" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="activityForm.startTime" type="datetime" placeholder="选择开始时间"
            :disabled="!canEditAllFields" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="activityForm.endTime" type="datetime" placeholder="选择结束时间"
            :disabled="!canEditAllFields" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="报名上限">
          <el-input-number v-model="activityForm.maxParticipants" :min="1" :step="1"
            :disabled="!canEditAllFields" style="width: 100%" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="activityForm.category" :disabled="!canEditAllFields" style="width: 100%">
            <el-option label="校园服务" value="campus" />
            <el-option label="环保公益" value="environmental" />
            <el-option label="支教助学" value="education" />
            <el-option label="社区服务" value="community" />
            <el-option label="后勤保障" value="support" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" v-if="canEditAllFields">
          <el-input v-model="activityForm.tags" placeholder="逗号分隔，如：室内,整理" />
        </el-form-item>
        <el-alert v-if="!canEditAllFields" type="info" :closable="false" show-icon
          title="已发布/进行中的活动仅支持修改描述信息" style="margin-bottom: 8px" />
      </el-form>
      <template #footer>
        <el-button @click="activityDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveActivity">保存</el-button>
      </template>
    </el-dialog>

    <!-- ========== 二维码弹窗 ========== -->
    <el-dialog v-model="qrDialogVisible" title="签到二维码" width="400px">
      <div style="text-align: center;">
        <p style="margin-bottom: 12px; color: #666;">
          活动：<strong>{{ qrActivity?.title }}</strong>（ID: {{ qrActivity?.id }}）
        </p>
        <canvas ref="qrCanvasRef" style="border: 1px solid #eee;"></canvas>
        <p style="margin-top: 12px; color: #999; font-size: 13px;">
          学生可使用扫码签到功能扫描此二维码
        </p>
      </div>
    </el-dialog>

    <!-- ========== 用户编辑/新增弹窗 ========== -->
    <el-dialog v-model="userDialogVisible" :title="isEditingUser ? '编辑用户' : '新增用户'" width="450px">
      <el-form :model="userForm" label-width="80px">
        <el-form-item label="学号" v-if="!isEditingUser">
          <el-input v-model="userForm.studentId" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="密码" v-if="!isEditingUser">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="userForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.role">
            <el-option label="学生" value="student" />
            <el-option label="组织者" value="organizer" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属机构">
          <el-input v-model="userForm.organization" placeholder="如：校团委" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveUser">保存</el-button>
      </template>
    </el-dialog>

    <!-- ========== 重置密码弹窗 ========== -->
    <el-dialog v-model="passwordDialogVisible" title="重置密码" width="350px">
      <el-form :model="resetPwdForm" label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="resetPwdForm.password" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPwd">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api'

const route = useRoute()
const activeTab = ref((route.query.tab as string) || 'activities')
const activities = ref<any[]>([])
const loading = ref(true)

// ===== 组织者审批（P2-UPM-04） =====
const applies = ref<any[]>([])
const loadingApplies = ref(false)

async function loadApplies() {
  loadingApplies.value = true
  try {
    applies.value = await request.get('/auth/organizer-applies')
  } catch {
    applies.value = []
  } finally {
    loadingApplies.value = false
  }
}

async function reviewApply(applyId: number, approved: boolean) {
  try {
    await request.put(`/auth/organizer-applies/${applyId}/review`, { approved })
    ElMessage.success(approved ? '已通过' : '已拒绝')
    loadApplies()
  } catch {
    // 错误已在拦截器中提示
  }
}

// 切换到特定 Tab 时自动加载数据（immediate 确保直接带 ?tab= 进入时也能触发）
watch(activeTab, (tab) => {
  if (tab === 'organizer') loadApplies()
  if (tab === 'users') loadUsers()
}, { immediate: true })

// ===== 活动管理 =====
async function loadActivities() {
  try {
    activities.value = await request.get('/activities', { params: { showAll: true } })
  } finally {
    loading.value = false
  }
}

async function publishActivity(id: number) {
  try {
    await request.put(`/activities/${id}/publish`)
    ElMessage.success('发布成功')
    loadActivities()
  } catch {
    // 错误已在拦截器中提示
  }
}

// ===== 活动编辑 =====
const activityDialogVisible = ref(false)
const editingActivity = ref<any>(null)
const activityForm = reactive({
  title: '',
  description: '',
  locationName: '',
  longitude: 117.205,
  latitude: 34.2173,
  startTime: '',
  endTime: '',
  maxParticipants: 50,
  category: '',
  tags: '',
  coverImage: '',
})

/** 草稿可以编辑全部字段，已发布/进行中仅可编辑描述 */
const canEditAllFields = ref(false)

function openEditActivity(row: any) {
  editingActivity.value = row
  canEditAllFields.value = row.status === 'draft'
  activityForm.title = row.title || ''
  activityForm.description = row.description || ''
  activityForm.locationName = row.locationName || ''
  activityForm.longitude = row.longitude ?? 117.205
  activityForm.latitude = row.latitude ?? 34.2173
  activityForm.startTime = row.startTime || ''
  activityForm.endTime = row.endTime || ''
  activityForm.maxParticipants = row.maxParticipants ?? 50
  activityForm.category = row.category || ''
  activityForm.tags = row.tags || ''
  activityForm.coverImage = row.coverImage || ''
  activityDialogVisible.value = true
}

async function handleSaveActivity() {
  if (!editingActivity.value) return
  try {
    await request.put(`/activities/${editingActivity.value.id}`, { ...activityForm })
    ElMessage.success('更新成功')
    activityDialogVisible.value = false
    loadActivities()
  } catch {
    // 错误已在拦截器中提示
  }
}

// ===== 活动删除 =====
async function handleDeleteActivity(row: any) {
  try {
    await ElMessageBox.confirm(
      `确认删除活动「${row.title}」？删除后无法恢复。`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
    await request.delete(`/activities/${row.id}`)
    ElMessage.success('删除成功')
    loadActivities()
  } catch {
    // 取消或错误
  }
}

// ===== 二维码生成 =====
const qrDialogVisible = ref(false)
const qrActivity = ref<any>(null)
const qrCanvasRef = ref<HTMLCanvasElement | null>(null)

async function showQRCode(activity: any) {
  qrActivity.value = activity
  qrDialogVisible.value = true
  await nextTick()
  try {
    // 后端返回 Base64 编码的签到码
    const qrCodeStr: string = await request.get(`/checkin/qrcode/${activity.id}`)
    // 使用 Canvas 手动绘制二维码（用 QRCode 编码后的 DataMatrix 原理，这里简化用文本展示）
    // 前端通过动态 import 使用 qrcode 库生成
    drawQRCode(qrCanvasRef.value!, qrCodeStr, 200)
  } catch {
    ElMessage.error('获取签到码失败')
  }
}

/**
 * 使用 Canvas 绘制二维码
 * 将签到码文本以二维码形式展示
 * 原理：将 Base64 字符串内容（如 "CHECKIN:5"）转化为视觉二维码
 */
async function drawQRCode(canvas: HTMLCanvasElement, text: string, size: number) {
  // 动态加载 QRCode 库
  const QRCode = (await import('qrcode')).default
  canvas.width = size
  canvas.height = size
  await QRCode.toCanvas(canvas, text, { width: size })
}

// ===== 时长审核 =====
const selectedActivityId = ref<number | null>(null)
const signups = ref<any[]>([])
const loadingSignups = ref(false)

async function loadSignups() {
  if (!selectedActivityId.value) {
    signups.value = []
    return
  }
  loadingSignups.value = true
  try {
    signups.value = await request.get(`/signups/activity/${selectedActivityId.value}`)
  } catch {
    signups.value = []
  } finally {
    loadingSignups.value = false
  }
}

async function verifyHours(signupId: number) {
  try {
    await ElMessageBox.confirm('确认审核通过该志愿时长？', '审核确认', {
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await request.put(`/checkin/verify-hours/${signupId}`)
    ElMessage.success('审核通过，时长已累计至用户账户')
    loadSignups() // 刷新列表
    loadActivities() // 刷新活动列表
  } catch {
    // 取消或错误
  }
}

// ===== 账号管理 =====
const users = ref<any[]>([])
const loadingUsers = ref(false)
const userDialogVisible = ref(false)
const isEditingUser = ref(false)
const editingUserId = ref<number | null>(null)
const passwordDialogVisible = ref(false)
const resetPwdUserId = ref<number | null>(null)
const resetPwdForm = reactive({ password: '' })
const userForm = reactive({
  studentId: '',
  password: '',
  name: '',
  phone: '',
  role: 'student',
  organization: '',
})

async function loadUsers() {
  loadingUsers.value = true
  try {
    users.value = await request.get('/admin/users')
  } catch(e: any) {
    // 区分 403（权限不足）和其他错误（端点不可用等）
    if (e?.response?.status === 403) {
      ElMessage.warning('账号管理功能暂不可用，请确认后端已重新编译并重启（UserManagementController 为新文件）')
    }
    users.value = []
  } finally {
    loadingUsers.value = false
  }
}

function openCreateUser() {
  isEditingUser.value = false
  editingUserId.value = null
  userForm.studentId = ''
  userForm.password = ''
  userForm.name = ''
  userForm.phone = ''
  userForm.role = 'student'
  userForm.organization = ''
  userDialogVisible.value = true
}

function openEditUser(row: any) {
  isEditingUser.value = true
  editingUserId.value = row.id
  userForm.studentId = row.studentId
  userForm.password = ''
  userForm.name = row.name || ''
  userForm.phone = row.phone || ''
  userForm.role = row.role || 'student'
  userForm.organization = row.organization || ''
  userDialogVisible.value = true
}

async function handleSaveUser() {
  try {
    if (isEditingUser.value && editingUserId.value) {
      await request.put(`/admin/users/${editingUserId.value}`, {
        name: userForm.name,
        phone: userForm.phone,
        role: userForm.role,
        organization: userForm.organization,
      })
      ElMessage.success('更新成功')
    } else {
      await request.post('/admin/users', { ...userForm })
      ElMessage.success('创建成功')
    }
    userDialogVisible.value = false
    loadUsers()
  } catch {
    // 错误已在拦截器中提示
  }
}

async function handleDeleteUser(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除用户「${row.name || row.studentId}」？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await request.delete(`/admin/users/${row.id}`)
    ElMessage.success('删除成功')
    loadUsers()
  } catch {
    // 取消或错误
  }
}

function openResetPwd(row: any) {
  resetPwdUserId.value = row.id
  resetPwdForm.password = ''
  passwordDialogVisible.value = true
}

async function handleResetPwd() {
  if (!resetPwdUserId.value || !resetPwdForm.password) return
  try {
    await request.put(`/admin/users/${resetPwdUserId.value}/password`, {
      password: resetPwdForm.password,
    })
    ElMessage.success('密码重置成功')
    passwordDialogVisible.value = false
  } catch {
    // 错误已在拦截器中提示
  }
}

loadActivities()
</script>

<style scoped>
.admin-page {
  max-width: 1000px;
  margin: 20px auto;
  padding: 0 16px;
}
.verify-section {
  padding: 8px 0;
}
</style>
