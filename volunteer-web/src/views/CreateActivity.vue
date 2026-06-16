<template>
  <div class="create-activity">
    <h2>创建活动</h2>
    <el-card>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="活动描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="活动地点" prop="locationName">
          <el-input v-model="form.locationName" placeholder="如：博学楼101" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="经度" prop="longitude">
              <el-input-number v-model="form.longitude" :precision="6" :step="0.001" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度" prop="latitude">
              <el-input-number v-model="form.latitude" :precision="6" :step="0.001" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="报名上限" prop="maxParticipants">
          <el-input-number v-model="form.maxParticipants" :min="1" :max="9999" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">创建活动</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api'
import { DEFAULT_CENTER } from '@/config/map'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)

const form = reactive({
  title: '',
  description: '',
  locationName: '',
  longitude: DEFAULT_CENTER[0],
  latitude: DEFAULT_CENTER[1],
  startTime: '',
  endTime: '',
  maxParticipants: 50,
})

const rules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  locationName: [{ required: true, message: '请输入活动地点', trigger: 'blur' }],
  maxParticipants: [{ required: true, message: '请设置报名上限', trigger: 'blur' }],
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await request.post('/activities', { ...form })
    ElMessage.success('创建成功')
    router.push('/admin')
  } catch {
    // 错误已在拦截器中提示
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.create-activity {
  max-width: 700px;
  margin: 20px auto;
  padding: 0 16px;
}
</style>
