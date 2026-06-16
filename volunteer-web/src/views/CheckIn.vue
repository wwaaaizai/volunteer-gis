<template>
  <div class="checkin-page">
    <h2>活动签到</h2>
    <el-card>
      <el-form :model="form">
        <el-form-item label="活动ID">
          <el-input-number v-model="form.activityId" :min="1" style="width: 200px" />
        </el-form-item>

        <el-form-item label="签到方式">
          <el-radio-group v-model="checkInMode">
            <el-radio value="location">定位签到</el-radio>
            <el-radio value="qr">扫码签到</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="checkInMode === 'qr'">
          <el-form-item label="签到码">
            <el-input v-model="form.qrCode" placeholder="请输入或扫描签到二维码" />
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" @click="handleCheckIn" :loading="checkingIn">
            签到
          </el-button>
          <el-button @click="handleCheckOut" :loading="checkingOut" style="margin-left: 12px">
            签退
          </el-button>
        </el-form-item>
      </el-form>

      <el-divider />
      <p class="hint">定位签到使用浏览器 GPS 定位，需授权位置权限</p>
      <p class="hint">请在活动地点附近操作签到</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api'

const checkInMode = ref('location')
const checkingIn = ref(false)
const checkingOut = ref(false)

const form = reactive({
  activityId: 1,
  qrCode: '',
})

function getLocation(): Promise<{ lng: number; lat: number }> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('浏览器不支持定位'))
      return
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ lng: pos.coords.longitude, lat: pos.coords.latitude }),
      () => reject(new Error('获取位置失败，请检查定位权限')),
      { timeout: 10000, enableHighAccuracy: true }
    )
  })
}

async function handleCheckIn() {
  checkingIn.value = true
  try {
    if (checkInMode.value === 'location') {
      const pos = await getLocation()
      await request.post('/checkin/location', null, {
        params: { activityId: form.activityId, lng: pos.lng, lat: pos.lat }
      })
    } else {
      await request.post('/checkin/qr', null, {
        params: { activityId: form.activityId, code: form.qrCode }
      })
    }
    ElMessage.success('签到成功')
  } catch (e: any) {
    ElMessage.error(e.message || '签到失败')
  } finally {
    checkingIn.value = false
  }
}

async function handleCheckOut() {
  checkingOut.value = true
  try {
    const pos = await getLocation()
    await request.post('/checkin/out', null, {
      params: { activityId: form.activityId, lng: pos.lng, lat: pos.lat }
    })
    ElMessage.success('签退成功')
  } catch (e: any) {
    ElMessage.error(e.message || '签退失败')
  } finally {
    checkingOut.value = false
  }
}
</script>

<style scoped>
.checkin-page {
  max-width: 600px;
  margin: 20px auto;
  padding: 0 16px;
}
.hint {
  color: #909399;
  font-size: 13px;
  margin: 4px 0;
}
</style>
