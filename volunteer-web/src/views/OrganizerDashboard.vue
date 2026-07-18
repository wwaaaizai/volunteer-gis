<template>
  <div class="organizer-dashboard">
    <div class="dashboard-header">
      <h2>组织者后台</h2>
      <el-button type="primary" @click="$router.push('/organizer/create')">
        + 创建活动
      </el-button>
    </div>

    <!-- 按状态分 Tab 展示我的活动 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="草稿" name="draft" />
      <el-tab-pane label="已发布" name="published" />
      <el-tab-pane label="进行中" name="ongoing" />
      <el-tab-pane label="已结束" name="ended" />
    </el-tabs>

    <el-table :data="activities" v-loading="loading" empty-text="暂无活动">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="活动标题" min-width="140" />
      <el-table-column prop="category" label="分类" width="80">
        <template #default="{ row }">
          <el-tag size="small">{{ categoryLabel(row.category) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="locationName" label="地点" width="120" />
      <el-table-column prop="signedCount" label="报名" width="60" />
      <el-table-column prop="maxParticipants" label="上限" width="60" />
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/organizer/activity/${row.id}`)">
            详情
          </el-button>
          <el-button
            v-if="row.status === 'draft'"
            size="small"
            type="primary"
            @click="editActivity(row.id)"
          >
            编辑
          </el-button>
          <el-button
            v-if="row.status === 'draft'"
            size="small"
            type="success"
            @click="publishActivity(row.id)"
          >
            发布
          </el-button>
          <el-button size="small" @click="duplicateActivity(row)">
            📋 复制
          </el-button>
          <el-button size="small" type="warning"
            @click="$router.push(`/organizer/geofence/${row.id}`)">
            围栏
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api'

const router = useRouter()
const activities = ref<any[]>([])
const loading = ref(false)
const activeTab = ref('draft')

/** 分类中文名 */
const categoryMap: Record<string, string> = {
  environmental: '环保',
  support: '助学',
  education: '支教',
  community: '社区',
  campus: '校园',
  other: '其他',
}
function categoryLabel(cat: string) {
  return categoryMap[cat] || cat || '未分类'
}

/** 加载我的活动（按状态筛选） */
async function loadActivities(status: string) {
  loading.value = true
  try {
    activities.value = await request.get('/activities/my', { params: { status } })
  } catch {
    activities.value = []
  } finally {
    loading.value = false
  }
}

function handleTabChange(tab: string) {
  loadActivities(tab)
}

/** 发布活动 */
async function publishActivity(id: number) {
  try {
    await ElMessageBox.confirm('确认发布该活动？发布后学生即可报名。', '确认发布', {
      confirmButtonText: '确认发布',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await request.put(`/activities/${id}/publish`)
    ElMessage.success('发布成功')
    loadActivities(activeTab.value)
  } catch {
    // 取消或出错
  }
}

/** 编辑活动 — 跳转到创建页（编辑模式） */
function editActivity(id: number) {
  router.push({ path: '/organizer/create', query: { edit: id } })
}

/** 一键复制活动 — 复制为新草稿并跳转编辑 */
async function duplicateActivity(row: any) {
  try {
    await ElMessageBox.confirm(`确认复制活动「${row.title}」为新草稿？`, '复制活动', {
      confirmButtonText: '确认复制',
      type: 'info',
    })
    // 创建新活动（复制原活动数据）
    const payload = {
      title: row.title + '（副本）',
      description: row.description || '',
      category: row.category || '',
      tags: row.tags || '',
      locationName: row.locationName || '',
      longitude: row.longitude,
      latitude: row.latitude,
      startTime: row.startTime,
      endTime: row.endTime,
      maxParticipants: row.maxParticipants,
      coverImage: row.coverImage || '',
      extraLocations: row.extraLocations || '',
    }
    await request.post('/activities', payload)
    ElMessage.success('复制成功，跳转到编辑页')
    loadActivities(activeTab.value)
  } catch {
    // 取消或出错
  }
}

// 初始加载草稿列表
loadActivities('draft')
</script>

<style scoped>
.organizer-dashboard {
  max-width: 1100px;
  margin: 20px auto;
  padding: 0 16px;
}
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.dashboard-header h2 {
  margin: 0;
}
</style>
