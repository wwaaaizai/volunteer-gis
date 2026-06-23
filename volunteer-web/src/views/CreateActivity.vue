<template>
  <div class="create-activity">
    <h2>{{ isEdit ? '编辑活动' : '创建活动' }}</h2>
    <el-card>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <!-- 活动标题 -->
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入活动标题" />
        </el-form-item>

        <!-- 活动分类 -->
        <el-form-item label="活动分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="环保" value="environmental" />
            <el-option label="助学" value="support" />
            <el-option label="支教" value="education" />
            <el-option label="社区" value="community" />
            <el-option label="校园" value="campus" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>

        <!-- 活动标签 -->
        <el-form-item label="活动标签">
          <el-tag
            v-for="tag in form.tags"
            :key="tag"
            closable
            @close="removeTag(tag)"
            style="margin-right: 6px"
          >
            {{ tag }}
          </el-tag>
          <el-input
            v-if="tagInputVisible"
            ref="tagInputRef"
            v-model="tagInputValue"
            size="small"
            style="width: 100px"
            @keyup.enter="addTag"
            @blur="addTag"
          />
          <el-button v-else size="small" @click="showTagInput">+ 添加标签</el-button>
        </el-form-item>

        <!-- 活动描述 -->
        <el-form-item label="活动描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请描述活动内容" />
        </el-form-item>

        <!-- 活动地点 -->
        <el-form-item label="活动地点" prop="locationName">
          <el-input v-model="form.locationName" placeholder="如：博学楼101" />
        </el-form-item>

        <!-- 地图选点（只在有经纬度需求时显示，实际始终显示） -->
        <el-form-item label="地图选点" prop="longitude">
          <MapPicker
            ref="mapPickerRef"
            :modelLng="isEdit ? form.longitude : undefined"
            :modelLat="isEdit ? form.latitude : undefined"
            @update="onMapPick"
          />
          <span class="coord-tip">点击地图自动填入坐标，或手动输入</span>
        </el-form-item>

        <!-- 经纬度 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="经度" prop="longitude">
              <el-input-number
                v-model="form.longitude"
                :precision="6"
                :step="0.001"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度" prop="latitude">
              <el-input-number
                v-model="form.latitude"
                :precision="6"
                :step="0.001"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 时间 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 报名上限 -->
        <el-form-item label="报名上限" prop="maxParticipants">
          <el-input-number v-model="form.maxParticipants" :min="1" :max="9999" />
        </el-form-item>

        <!-- 封面图上传 -->
        <el-form-item label="封面图片">
          <el-upload
            :action="uploadUrl"
            :headers="uploadHeaders"
            :on-success="onUploadSuccess"
            :on-error="onUploadError"
            :before-upload="beforeUpload"
            :show-file-list="false"
            accept="image/*"
          >
            <el-button type="primary" :loading="uploading">
              {{ form.coverImage ? '重新上传' : '上传封面图' }}
            </el-button>
          </el-upload>
          <img
            v-if="form.coverImage"
            :src="form.coverImage"
            class="cover-preview"
            alt="封面预览"
          />
        </el-form-item>

        <!-- 操作按钮 -->
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存修改' : '创建活动' }}
          </el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api'
import { DEFAULT_CENTER } from '@/config/map'
import MapPicker from '@/components/map/MapPicker.vue'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const mapPickerRef = ref<InstanceType<typeof MapPicker>>()
const submitting = ref(false)
const uploading = ref(false)

// 标签输入
const tagInputVisible = ref(false)
const tagInputValue = ref('')
const tagInputRef = ref()

// 判断是否编辑模式
const isEdit = computed(() => !!route.query.edit)

const form = reactive({
  title: '',
  description: '',
  category: '',
  tags: [] as string[],
  locationName: '',
  longitude: DEFAULT_CENTER[0],
  latitude: DEFAULT_CENTER[1],
  startTime: '',
  endTime: '',
  maxParticipants: 50,
  coverImage: '',
})

const rules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  locationName: [{ required: true, message: '请输入活动地点', trigger: 'blur' }],
  longitude: [{ required: true, message: '请选择活动位置', trigger: 'blur' }],
  latitude: [{ required: true, message: '请选择活动位置', trigger: 'blur' }],
  maxParticipants: [{ required: true, message: '请设置报名上限', trigger: 'blur' }],
}

/** 上传地址（Vite 代理到后端） */
const uploadUrl = '/api/upload/image'

/** 上传请求头（附带 Token） */
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
}))

/** 编辑模式 — 加载已有活动数据 */
onMounted(async () => {
  const editId = route.query.edit
  if (!editId) return
  try {
    const data: any = await request.get(`/activities/${editId}`)
    Object.assign(form, {
      title: data.title || '',
      description: data.description || '',
      category: data.category || '',
      tags: data.tags ? data.tags.split(',').filter(Boolean) : [],
      locationName: data.locationName || '',
      longitude: data.longitude,
      latitude: data.latitude,
      startTime: data.startTime || '',
      endTime: data.endTime || '',
      maxParticipants: data.maxParticipants || 50,
      coverImage: data.coverImage || '',
    })
  } catch {
    ElMessage.error('加载活动数据失败')
    router.back()
  }
})

/** 地图选点回调 */
function onMapPick(lng: number, lat: number) {
  form.longitude = lng
  form.latitude = lat
}

/** 标签操作 */
function showTagInput() {
  tagInputVisible.value = true
  nextTick(() => tagInputRef.value?.focus())
}
function addTag() {
  const val = tagInputValue.value.trim()
  if (val && !form.tags.includes(val)) {
    form.tags.push(val)
  }
  tagInputVisible.value = false
  tagInputValue.value = ''
}
function removeTag(tag: string) {
  form.tags = form.tags.filter((t) => t !== tag)
}

/** 封面上传 */
function beforeUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('仅支持图片格式')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  uploading.value = true
  return true
}
function onUploadSuccess(response: any) {
  uploading.value = false
  form.coverImage = response.data?.url || response.url || ''
  ElMessage.success('封面上传成功')
}
function onUploadError() {
  uploading.value = false
  ElMessage.error('封面上传失败')
}

/** 提交 */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 基本校验：经纬度必须填
  if (!form.longitude || !form.latitude) {
    ElMessage.warning('请在地图上点击选择活动位置')
    return
  }

  submitting.value = true
  try {
    const payload = {
      ...form,
      tags: form.tags.join(','), // 数组转为逗号分隔字符串
    }

    if (isEdit.value) {
      // 编辑模式
      const editId = route.query.edit as string
      await request.put(`/activities/${editId}`, payload)
      ElMessage.success('修改保存成功')
      router.push('/organizer')
    } else {
      // 新建模式
      await request.post('/activities', payload)
      ElMessage.success('创建成功')
      router.push('/organizer')
    }
  } catch {
    // 错误已在拦截器中提示
  } finally {
    submitting.value = false
  }
}

/** 返回上一页 */
function goBack() {
  // 判断来源：组织者或管理员
  if (route.path.includes('organizer')) {
    router.push('/organizer')
  } else {
    router.push('/admin')
  }
}
</script>

<style scoped>
.create-activity {
  max-width: 740px;
  margin: 20px auto;
  padding: 0 16px;
}
.coord-tip {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-top: 4px;
}
.cover-preview {
  width: 200px;
  margin-top: 8px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}
</style>
