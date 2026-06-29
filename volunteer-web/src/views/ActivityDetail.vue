<template>
  <div class="activity-detail">
    <el-card v-if="activity" v-loading="loading">
      <template #header>
        <div class="detail-header">
          <h2>{{ activity.title }}</h2>
          <el-tag :type="statusTag">{{ statusText }}</el-tag>
        </div>
      </template>

      <!-- 封面图 -->
      <div class="cover-section" v-if="activity.coverImage">
        <img :src="activity.coverImage" alt="活动封面" class="cover-img" />
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="活动地点">{{ activity.locationName }}</el-descriptions-item>
        <el-descriptions-item label="报名人数">
          {{ activity.signedCount }} / {{ activity.maxParticipants }}
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ activity.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ activity.endTime }}</el-descriptions-item>
      </el-descriptions>

      <div class="desc-section">
        <h3>活动描述</h3>
        <p>{{ activity.description }}</p>
      </div>

      <div class="action-section">
        <el-button type="primary" size="large" @click="handleSignup" :disabled="!canSignup" :loading="signing">
          {{ signupBtnText }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api'

const route = useRoute()
const activity = ref<any>(null)
const loading = ref(true)
const signing = ref(false)

const statusTag = computed(() => {
  const status = activity.value?.status
  if (status === 'published') return 'success'
  if (status === 'ongoing') return ''
  return 'info'
})

const statusText = computed(() => {
  const map: Record<string, string> = { published: '报名中', ongoing: '进行中', ended: '已结束' }
  return map[activity.value?.status] || activity.value?.status
})

const canSignup = computed(() => {
  if (!activity.value) return false
  return activity.value.status === 'published' &&
         activity.value.signedCount < activity.value.maxParticipants
})

const signupBtnText = computed(() => {
  if (!activity.value) return '加载中'
  if (activity.value.status !== 'published') return '不在报名期'
  if (activity.value.signedCount >= activity.value.maxParticipants) return '名额已满'
  return '立即报名'
})

async function loadActivity() {
  try {
    loading.value = true
    activity.value = await request.get(`/activities/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

async function handleSignup() {
  signing.value = true
  try {
    await request.post('/signups', null, {
      params: { activityId: route.params.id }
    })
    ElMessage.success('报名成功')
    await loadActivity()
  } catch {
    // 错误已在拦截器中提示
  } finally {
    signing.value = false
  }
}

loadActivity()
</script>

<style scoped>
.activity-detail {
  max-width: 800px;
  margin: 20px auto;
  padding: 0 16px;
}
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.detail-header h2 {
  margin: 0;
}
.cover-section {
  margin-bottom: 16px;
}
.cover-img {
  width: 100%;
  max-height: 300px;
  object-fit: cover;
  border-radius: 8px;
}
.desc-section {
  margin-top: 20px;
}
.desc-section h3 {
  margin-bottom: 8px;
}
.action-section {
  margin-top: 24px;
  text-align: center;
}
</style>
