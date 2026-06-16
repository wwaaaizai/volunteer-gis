<template>
  <div class="my-signups">
    <h2>我的报名</h2>
    <el-table :data="signups" v-loading="loading" empty-text="暂无报名记录">
      <el-table-column prop="activityId" label="活动ID" width="80" />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'signed'">已报名</el-tag>
          <el-tag v-else-if="row.status === 'signed_in'" type="success">已签到</el-tag>
          <el-tag v-else-if="row.status === 'signed_out'" type="info">已签退</el-tag>
          <el-tag v-else type="danger">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="signInTime" label="签到时间" />
      <el-table-column prop="signOutTime" label="签退时间" />
      <el-table-column prop="volunteerHours" label="志愿时长(h)" />
      <el-table-column prop="hourVerified" label="审核状态">
        <template #default="{ row }">
          <span v-if="row.hourVerified" style="color: #67c23a">已审核</span>
          <span v-else style="color: #e6a23c">待审核</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import request from '@/api'

const signups = ref([])
const loading = ref(true)

async function loadSignups() {
  try {
    signups.value = await request.get('/signups/my')
  } finally {
    loading.value = false
  }
}

loadSignups()
</script>

<style scoped>
.my-signups {
  max-width: 1000px;
  margin: 20px auto;
  padding: 0 16px;
}
</style>
