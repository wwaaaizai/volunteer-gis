<template>
  <div class="profile-page">
    <h2>组织信息</h2>
    <el-card>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px" v-loading="loading">
        <el-form-item label="组织账号">
          <el-input :model-value="userInfo?.studentId" disabled />
          <span class="form-tip">登录使用的账号，不可修改</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-tag>{{ roleLabel }}</el-tag>
        </el-form-item>
        <el-form-item label="组织名称" prop="name">
          <el-input v-model="form.name" placeholder="如：环测学院青年志愿者协会" />
          <span class="form-tip">显示在活动页面的归属组织处</span>
        </el-form-item>
        <el-form-item label="负责人" prop="contactPerson">
          <el-input v-model="form.contactPerson" placeholder="如：张三" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="如：138xxxx" />
        </el-form-item>
        <el-form-item label="所属单位" prop="organization">
          <el-input v-model="form.organization" placeholder="如：环境与测绘学院团委" />
        </el-form-item>
        <el-form-item label="工号" v-if="userInfo?.role !== 'student'">
          <el-input v-model="form.employeeId" placeholder="组织者工号（选填）" />
        </el-form-item>
        <el-form-item label="负责区域">
          <el-input v-model="form.responsibleArea" placeholder="如：南湖校区博学楼区域、图书馆" type="textarea" :rows="2" />
          <span class="form-tip">描述贵组织主要负责的校园区域或活动范围</span>
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
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import request from '@/api'

const userStore = useUserStore()
const userInfo = ref<any>(null)
const formRef = ref()
const loading = ref(true)
const saving = ref(false)

const roleLabel = computed(() => {
  const map: Record<string, string> = {
    organizer: '活动组织者', admin: '管理员', student: '学生',
  }
  return map[userInfo.value?.role] || userInfo.value?.role || ''
})

const form = reactive({
  name: '',
  contactPerson: '',
  phone: '',
  organization: '',
  employeeId: '',
  responsibleArea: '',
})

const rules = {
  name: [{ required: true, message: '请输入组织名称', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
}

onMounted(async () => {
  try {
    userInfo.value = await request.get('/auth/me')
    form.name = userInfo.value.name || ''
    form.phone = userInfo.value.phone || ''
    form.organization = userInfo.value.organization || ''
    form.employeeId = userInfo.value.employeeId || ''
    form.responsibleArea = userInfo.value.responsibleArea || ''
  } catch {
    ElMessage.error('加载组织信息失败')
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
    // error handled by interceptor
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 640px;
  margin: 20px auto;
  padding: 0 16px;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-top: 2px;
}
</style>
