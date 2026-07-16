<template>
  <div class="register-container">
    <el-card class="register-card">
      <h2>注册账号</h2>
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="studentId">
          <el-input v-model="form.studentId" placeholder="学号" size="large" />
        </el-form-item>
        <el-form-item prop="name">
          <el-input v-model="form.name" placeholder="姓名" size="large" />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" size="large" />
        </el-form-item>
        <el-form-item prop="grade">
          <el-select v-model="form.grade" placeholder="年级（选填）" size="large" style="width:100%">
            <el-option label="2023级" value="2023" />
            <el-option label="2024级" value="2024" />
            <el-option label="2025级" value="2025" />
            <el-option label="2026级" value="2026" />
          </el-select>
        </el-form-item>
        <el-form-item prop="college">
          <el-input v-model="form.college" placeholder="院系（选填，如：计算机科学与技术学院）" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（至少6位）" size="large" />
        </el-form-item>

        <!-- 组织者申请（P2-UPM-03） -->
        <el-divider />
        <el-form-item>
          <el-checkbox v-model="form.applyOrganizer">
            申请成为活动组织者
          </el-checkbox>
        </el-form-item>
        <template v-if="form.applyOrganizer">
          <el-form-item prop="organization">
            <el-input v-model="form.organization" placeholder="所属机构（如：校团委、学生会）" size="large" />
          </el-form-item>
          <el-form-item prop="employeeId">
            <el-input v-model="form.employeeId" placeholder="工号/编号" size="large" />
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%" @click="handleRegister" :loading="loading">
            注册
          </el-button>
        </el-form-item>
      </el-form>
      <p class="login-link">
        已有账号？
        <router-link to="/login">去登录</router-link>
      </p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  studentId: '',
  name: '',
  phone: '',
  grade: '',
  college: '',
  password: '',
  applyOrganizer: false,
  organization: '',
  employeeId: '',
})

const rules = {
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  organization: [{ required: true, message: '请输入所属机构', trigger: 'blur' }],
  employeeId: [{ required: true, message: '请输入工号', trigger: 'blur' }],
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await request.post('/auth/register', { ...form })
    ElMessage.success(form.applyOrganizer
      ? '注册成功！组织者申请已提交，请等待管理员审批'
      : '注册成功，请登录')
    router.push('/login')
  } catch {
    // 错误已在拦截器中提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.register-card {
  width: 460px;
}
.register-card h2 {
  text-align: center;
  margin-bottom: 24px;
  color: #303133;
}
.login-link {
  text-align: center;
  color: #909399;
}
</style>
