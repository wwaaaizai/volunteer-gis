<template>
  <div class="organizer-activity-detail">
    <!-- 返回 -->
    <el-button text @click="$router.push('/organizer')" style="margin-bottom: 12px">
      ← 返回组织者后台
    </el-button>

    <el-card v-loading="loading">
      <!-- 基本信息 -->
      <template #header>
        <div class="card-header">
          <h2>{{ activity?.title || '加载中...' }}</h2>
          <el-tag :type="statusType" size="large">{{ statusLabel }}</el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border v-if="activity">
        <el-descriptions-item label="活动ID">{{ activity.id }}</el-descriptions-item>
        <el-descriptions-item label="分类">
          <el-tag size="small">{{ categoryLabel }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="标签">
          <el-tag
            v-for="t in tags"
            :key="t"
            size="small"
            type="info"
            style="margin-right: 4px"
          >
            {{ t }}
          </el-tag>
          <span v-if="tags.length === 0" style="color: #999">—</span>
        </el-descriptions-item>
        <el-descriptions-item label="地点">{{ activity.locationName }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ activity.startTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ activity.endTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="报名人数">
          <strong>{{ activity.signedCount }}</strong> / {{ activity.maxParticipants }}
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          {{ activity.description || '无描述' }}
        </el-descriptions-item>
        <el-descriptions-item label="封面" :span="2" v-if="activity.coverImage">
          <img :src="activity.coverImage" class="cover-img" alt="封面" />
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 报名名单 & 签到统计 -->
    <el-card v-if="activity" style="margin-top: 16px">
      <el-tabs v-model="activeTab">
        <!-- Tab1: 报名名单 -->
        <el-tab-pane label="报名名单" name="signups">
          <el-table :data="signups" v-loading="loadingSignups" empty-text="暂无报名">
            <el-table-column label="序号" width="60" type="index" />
            <el-table-column prop="userId" label="用户ID" width="80" />
            <el-table-column label="审核状态" width="110">
              <template #default="{ row }">
                <el-tag :type="signupStatusType(row.status)" size="small">
                  {{ signupStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="签到时间" width="170">
              <template #default="{ row }">{{ row.signInTime || '—' }}</template>
            </el-table-column>
            <el-table-column label="签退时间" width="170">
              <template #default="{ row }">{{ row.signOutTime || '—' }}</template>
            </el-table-column>
            <el-table-column prop="volunteerHours" label="时长(h)" width="75" />
            <el-table-column label="操作" min-width="180">
              <template #default="{ row }">
                <template v-if="row.status === 'signed'">
                  <el-button size="small" type="success" @click="reviewSignup(row.id, 'approve')">
                    ✓ 通过
                  </el-button>
                  <el-button size="small" type="danger" @click="rejectSignup(row.id)">
                    ✗ 拒绝
                  </el-button>
                </template>
                <template v-else-if="row.status === 'rejected'">
                  <el-tooltip :content="row.reviewReason || '无理由'" placement="top">
                    <el-tag type="danger" size="small">已拒绝</el-tag>
                  </el-tooltip>
                </template>
                <template v-else>
                  <span style="color:#909399">—</span>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab2: 报名统计 -->
        <el-tab-pane label="报名统计" name="stats">
          <el-row :gutter="16">
            <el-col :span="4">
              <el-statistic title="总计" :value="signupStats.total" />
            </el-col>
            <el-col :span="4">
              <el-statistic title="待审核" :value="signupStats.toReview">
                <template #suffix>
                  <span v-if="signupStats.toReview > 0" style="font-size:13px;color:#E6A23C">⚠</span>
                </template>
              </el-statistic>
            </el-col>
            <el-col :span="4">
              <el-statistic title="已通过" :value="signupStats.approved" />
            </el-col>
            <el-col :span="4">
              <el-statistic title="已拒绝" :value="signupStats.rejected" />
            </el-col>
            <el-col :span="4">
              <el-statistic title="已签到" :value="signupStats.signedIn" />
            </el-col>
            <el-col :span="4">
              <el-statistic title="已签退" :value="signupStats.signedOut" />
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api'

const route = useRoute()
const activityId = computed(() => Number(route.params.id))

const activity = ref<any>(null)
const signups = ref<any[]>([])
const loading = ref(true)
const loadingSignups = ref(false)
const activeTab = ref('signups')

/** 分类映射 */
const categoryMap: Record<string, string> = {
  environmental: '环保', support: '助学', education: '支教',
  community: '社区', campus: '校园', other: '其他',
}
const categoryLabel = computed(() => categoryMap[activity.value?.category] || '未分类')

/** 标签拆分 */
const tags = computed(() => {
  if (!activity.value?.tags) return []
  return activity.value.tags.split(',').filter(Boolean)
})

/** 状态样式 */
const statusType = computed(() => {
  const map: Record<string, string> = {
    draft: 'info', published: 'success', ongoing: 'warning', ended: '', cancelled: 'danger',
  }
  return map[activity.value?.status] || 'info'
})
const statusLabel = computed(() => {
  const map: Record<string, string> = {
    draft: '草稿', published: '已发布', ongoing: '进行中', ended: '已结束', cancelled: '已取消',
  }
  return map[activity.value?.status] || activity.value?.status
})

/** 签到状态 */
function signupStatusType(s: string) {
  const map: Record<string, string> = {
    signed: '', signed_in: 'warning', signed_out: 'success', cancelled: 'danger',
    approved: 'success', rejected: 'danger',
  }
  return map[s] || 'info'
}
function signupStatusLabel(s: string) {
  const map: Record<string, string> = {
    signed: '待审核', signed_in: '已签到', signed_out: '已签退', cancelled: '已取消',
    approved: '已通过', rejected: '已拒绝',
  }
  return map[s] || s
}

/** 审核报名 — 通过 */
async function reviewSignup(id: number, action: string) {
  try {
    await request.put(`/signups/${id}/review`, { action })
    ElMessage.success(action === 'approve' ? '已通过' : '已拒绝')
    loadSignups()
  } catch {
    /* error handled by interceptor */
  }
}

/** 审核报名 — 拒绝（弹出理由输入框） */
async function rejectSignup(id: number) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝理由', '拒绝报名', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '选填：拒绝理由...',
    })
    await request.put(`/signups/${id}/review`, { action: 'reject', reason: reason || '' })
    ElMessage.success('已拒绝')
    loadSignups()
  } catch {
    /* user cancelled or error */
    if (typeof arguments[0] === 'string') { /* ignore cancel */ }
  }
}

/** 签到统计 */
const signupStats = computed(() => {
  const total = signups.value.length
  let signed = 0, approved = 0, rejected = 0, signedIn = 0, signedOut = 0
  signups.value.forEach((s: any) => {
    if (s.status === 'signed') signed++
    if (s.status === 'approved') approved++
    if (s.status === 'rejected') rejected++
    if (s.status === 'signed_in') signedIn++
    if (s.status === 'signed_out') signedOut++
  })
  return {
    total,
    toReview: signed,
    approved,
    rejected,
    signedIn,
    signedOut,
  }
})

/** 加载活动详情 */
async function loadActivity() {
  loading.value = true
  try {
    activity.value = await request.get(`/activities/${activityId.value}`)
  } catch {
    activity.value = null
  } finally {
    loading.value = false
  }
}

/** 加载报名名单 */
async function loadSignups() {
  loadingSignups.value = true
  try {
    signups.value = await request.get(`/signups/activity/${activityId.value}`)
  } catch {
    signups.value = []
  } finally {
    loadingSignups.value = false
  }
}

onMounted(async () => {
  await loadActivity()
  await loadSignups()
})
</script>

<style scoped>
.organizer-activity-detail {
  max-width: 960px;
  margin: 20px auto;
  padding: 0 16px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header h2 {
  margin: 0;
}
.cover-img {
  max-width: 300px;
  border-radius: 6px;
}
</style>
