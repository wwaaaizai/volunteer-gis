<template>
  <div class="admin-page">
    <h2>管理员后台</h2>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="活动管理" name="activities">
        <el-button type="primary" @click="$router.push('/admin/create-activity')" style="margin-bottom: 16px">
          创建活动
        </el-button>
        <el-table :data="activities" v-loading="loading">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="title" label="活动标题" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="signedCount" label="报名人数" width="100" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button v-if="row.status === 'draft'" size="small" @click="publishActivity(row.id)">
                发布
              </el-button>
              <el-button size="small" @click="$router.push(`/activity/${row.id}`)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="时长审核" name="hours">
        <el-empty description="暂无需审核的时长" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api'

const activeTab = ref('activities')
const activities = ref([])
const loading = ref(true)

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

loadActivities()
</script>

<style scoped>
.admin-page {
  max-width: 1000px;
  margin: 20px auto;
  padding: 0 16px;
}
</style>
