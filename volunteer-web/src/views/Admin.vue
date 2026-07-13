<template>
  <div class="admin-page">
    <h2>管理员后台</h2>

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
          <el-table-column label="操作" width="340">
            <template #default="{ row }">
              <el-button v-if="row.status === 'draft'" size="small" @click="publishActivity(row.id)">
                发布
              </el-button>
              <el-button size="small" @click="$router.push(`/activity/${row.id}`)">详情</el-button>
              <el-button size="small" type="success" @click="showQRCode(row)">签到码</el-button>
              <el-button size="small" type="warning" @click="$router.push(`/admin/geofence/${row.id}`)">
                围栏
              </el-button>
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
    </el-tabs>

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
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api'

const activeTab = ref('activities')
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

// 切换到组织者审批 Tab 时自动加载
import { watch } from 'vue'
watch(activeTab, (tab) => {
  if (tab === 'organizer') loadApplies()
})

// ===== 活动管理 =====
async function loadActivities() {
  try {
    activities.value = await request.get('/activities')
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
