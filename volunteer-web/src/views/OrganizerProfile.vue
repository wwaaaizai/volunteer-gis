<template>
  <div class="profile-page">
    <h2>个人信息管理</h2>
    <el-card>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" v-loading="loading">
        <el-form-item label="学号">
          <el-input :model-value="userInfo?.studentId" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag>{{ userInfo?.role === 'organizer' ? '活动组织者' : userInfo?.role === 'admin' ? '管理员' : '学生' }}</el-tag>
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="所属机构" prop="organization" v-if="userInfo?.role !== 'student'">
          <el-input v-model="form.organization" placeholder="如：校团委" />
        </el-form-item>
        <el-form-item label="累计志愿时长">
          <span>{{ userInfo?.totalHours ?? 0 }} 小时</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import request from '@/api'

const userStore = useUserStore()
const userInfo = ref<any>(null)
const formRef = ref()
const loading = ref(true)
const saving = ref(false)

const form = reactive({
  name: '',
  phone: '',
  organization: '',
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
}

onMounted(async () => {
  try {
    userInfo.value = await request.get('/auth/me')
    form.name = userInfo.value.name || ''
    form.phone = userInfo.value.phone || ''
    form.organization = userInfo.value.organization || ''
  } catch {
    ElMessage.error('加载用户信息失败')
  } finally {
    loading.value = false
  }
})

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await request.put('/auth/profile', { ...form })
    ElMessage.success('保存成功')
    await userStore.fetchUser()
  } catch {
    // 错误已在拦截器中提示
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 600px;
  margin: 20px auto;
  padding: 0 16px;
}
</style>
