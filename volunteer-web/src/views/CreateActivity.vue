<template>
  <div class="create-activity">
    <h2>{{ isEdit ? '编辑活动' : '创建活动' }}</h2>
    <!-- ===== 模板选择面板 ===== -->
    <div class="template-bar" v-if="!isEdit">
      <el-button @click="showTemplatePanel = true" :icon="DocumentCopy">
        📋 从模板新建
      </el-button>
    </div>

    <!-- 模板选择对话框 -->
    <el-dialog v-model="showTemplatePanel" title="选择活动模板" width="640px">
      <el-tabs v-model="templateTab">
        <el-tab-pane label="📅 年度常规活动" name="preset">
          <div class="template-grid">
            <div
              v-for="t in presetTemplates"
              :key="t.id"
              class="template-card"
              @click="applyTemplate(t)"
            >
              <div class="template-card-name">{{ t.name }}</div>
              <div class="template-card-desc">{{ t.description.slice(0, 60) }}...</div>
              <div class="template-card-meta">
                <el-tag size="small">{{ categoryMap[t.category] || t.category }}</el-tag>
                <span style="color:#909399;font-size:12px">上限{{ t.maxParticipants }}人</span>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="📝 我的模板" name="my">
          <el-empty v-if="myTemplates.length === 0" description="暂无自定义模板，创建活动时可保存为模板" />
          <div class="template-grid" v-else>
            <div
              v-for="t in myTemplates"
              :key="t.id"
              class="template-card"
              @click="applyTemplate(t)"
            >
              <div class="template-card-name">{{ t.name }}</div>
              <div class="template-card-desc">{{ t.description.slice(0, 60) }}...</div>
              <div class="template-card-meta">
                <el-tag size="small">{{ categoryMap[t.category] || t.category }}</el-tag>
                <el-button size="small" type="danger" text @click.stop="deleteTemplate(t.id)">删除</el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-card style="margin-top:12px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">

        <!-- 活动标题 + AI 按钮 -->
        <el-form-item label="活动标题" prop="title">
          <div class="inline-with-ai">
            <el-input v-model="form.title" placeholder="请输入活动标题" style="flex:1" />
            <el-button
              type="warning" plain size="small"
              :loading="aiGenerating === 'title'"
              :disabled="!!aiGenerating"
              @click="aiGenerateDescription"
            >
              {{ aiGenerating === 'title' ? '⏳ AI 生成中...' : '🤖 AI 生成' }}
            </el-button>
          </div>
        </el-form-item>

        <!-- AI 关键词输入（点击 AI 后弹出） -->
        <el-form-item label="AI 关键词" v-if="showAiKeyword">
          <div class="inline-with-ai">
            <el-input v-model="aiKeyword" placeholder="输入关键词，如：图书馆 整理 周末" style="flex:1" />
            <el-button type="primary" size="small" :loading="aiGenerating === 'keyword'" @click="doAiGenerate">
              生成
            </el-button>
            <el-button size="small" @click="showAiKeyword = false">取消</el-button>
          </div>
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
            v-for="tag in form.tags" :key="tag" closable
            @close="removeTag(tag)" style="margin-right:6px"
          >{{ tag }}</el-tag>
          <el-input
            v-if="tagInputVisible" ref="tagInputRef"
            v-model="tagInputValue" size="small" style="width:100px"
            @keyup.enter="addTag" @blur="addTag"
          />
          <el-button v-else size="small" @click="showTagInput">+ 添加标签</el-button>
        </el-form-item>

        <!-- 活动描述 + AI 按钮 -->
        <el-form-item label="活动描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请描述活动内容" />
          <div style="margin-top:4px;text-align:right">
            <el-button type="warning" plain size="small"
              :loading="aiGenerating === 'desc'"
              :disabled="!!aiGenerating"
              @click="aiGenerateDescription"
            >🤖 AI 生成描述</el-button>
          </div>
        </el-form-item>

        <!-- 活动地点 -->
        <el-form-item label="活动地点" prop="locationName">
          <el-input v-model="form.locationName" placeholder="如：博学楼101" />
        </el-form-item>

        <!-- 地图选点 -->
        <el-form-item label="地图选点" prop="longitude">
          <MapPicker
            ref="mapPickerRef"
            :modelLng="isEdit ? form.longitude : undefined"
            :modelLat="isEdit ? form.latitude : undefined"
            @update="onMapPick"
          />
        </el-form-item>

        <!-- 经纬度 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="经度" prop="longitude">
              <el-input-number v-model="form.longitude" :precision="6" :step="0.001" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度" prop="latitude">
              <el-input-number v-model="form.latitude" :precision="6" :step="0.001" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 时间 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 报名上限 + 预设志愿时长 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="报名上限" prop="maxParticipants">
              <el-input-number v-model="form.maxParticipants" :min="1" :max="9999" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="志愿时长(h)">
              <el-input-number v-model="form.volunteerHours" :min="0" :max="999" :precision="1" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 面向对象 -->
        <el-form-item label="面向年级">
          <el-input v-model="form.targetGrade" placeholder="如：2023,2024（逗号分隔，留空或填ALL表示不限）" />
        </el-form-item>
        <el-form-item label="面向院系">
          <el-input v-model="form.targetCollege" placeholder="如：计算机学院,矿业学院（逗号分隔，留空或填ALL表示不限）" />
        </el-form-item>

        <!-- 归属组织 -->
        <el-form-item label="归属组织">
          <el-input v-model="form.organizationName" placeholder="如：校团委志愿者协会" />
        </el-form-item>

        <!-- 封面：上传 + AI 生成 -->
        <el-form-item label="封面图片">
          <div class="cover-actions">
            <el-upload
              :action="uploadUrl" :headers="uploadHeaders"
              :on-success="onUploadSuccess" :on-error="onUploadError"
              :before-upload="beforeUpload" :show-file-list="false" accept="image/*"
            >
              <el-button :loading="uploading">
                {{ form.coverImage ? '重新上传' : '📷 上传封面图' }}
              </el-button>
            </el-upload>
            <el-button
              type="warning" plain
              :loading="aiGenerating === 'cover'"
              :disabled="!!aiGenerating"
              @click="aiGenerateCover"
            >🤖 AI 生成封面</el-button>
          </div>

          <!-- AI 封面候选面板 -->
          <div class="ai-cover-panel" v-if="aiCovers.length > 0">
            <div class="ai-cover-title">AI 生成候选（点击选择）</div>
            <div class="ai-cover-list">
              <div
                v-for="(cover, i) in aiCovers" :key="i"
                class="ai-cover-item"
                :class="{ selected: selectedAiCover === i }"
                @click="selectAiCover(i)"
              >
                <img :src="cover" alt="封面候选" />
                <span>{{ i + 1 }}</span>
              </div>
            </div>
          </div>

          <!-- 当前封面预览 -->
          <img v-if="form.coverImage" :src="form.coverImage" class="cover-preview" alt="封面预览" />
        </el-form-item>

        <!-- 保存为模板 -->
        <el-form-item label=" " v-if="!isEdit">
          <el-checkbox v-model="saveAsTemplate">保存为模板（下次可直接复用）</el-checkbox>
          <el-input
            v-if="saveAsTemplate"
            v-model="templateName"
            placeholder="模板名称，如：图书馆整理模板"
            style="width:220px;margin-left:12px"
            size="small"
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { DocumentCopy } from '@element-plus/icons-vue'
import request from '@/api'
import { DEFAULT_CENTER } from '@/config/map'
import MapPicker from '@/components/map/MapPicker.vue'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const mapPickerRef = ref<InstanceType<typeof MapPicker>>()
const submitting = ref(false)
const uploading = ref(false)

// 标签
const tagInputVisible = ref(false)
const tagInputValue = ref('')
const tagInputRef = ref()

// 模板
const showTemplatePanel = ref(false)
const templateTab = ref('preset')
const presetTemplates = ref<any[]>([])
const myTemplates = ref<any[]>([])
const saveAsTemplate = ref(false)
const templateName = ref('')

// AI
const aiGenerating = ref<string | null>(null)
const showAiKeyword = ref(false)
const aiKeyword = ref('')
const aiCovers = ref<string[]>([])
const selectedAiCover = ref(-1)

const isEdit = computed(() => !!route.query.edit)

const categoryMap: Record<string, string> = {
  environmental: '环保', support: '助学', education: '支教',
  community: '社区', campus: '校园', other: '其他',
}

const form = reactive({
  title: '', description: '', category: '', tags: [] as string[],
  locationName: '', longitude: DEFAULT_CENTER[0], latitude: DEFAULT_CENTER[1],
  startTime: '', endTime: '', maxParticipants: 50,
  volunteerHours: undefined as number | undefined,
  targetGrade: '', targetCollege: '', organizationName: '',
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

// ─── 模板 ───
async function loadTemplates() {
  try {
    const list: any[] = await request.get('/templates')
    presetTemplates.value = list.filter((t: any) => t.preset)
    myTemplates.value = list.filter((t: any) => !t.preset)
  } catch { /* ignore */ }
}

function applyTemplate(t: any) {
  form.title = t.title || ''
  form.description = t.description || ''
  form.category = t.category || ''
  form.tags = t.tags ? t.tags.split(',').filter(Boolean) : []
  form.locationName = t.locationName || ''
  form.maxParticipants = t.maxParticipants || 50
  form.volunteerHours = t.volunteerHours ?? undefined
  form.targetGrade = t.targetGrade || ''
  form.targetCollege = t.targetCollege || ''
  form.organizationName = t.organizationName || ''
  showTemplatePanel.value = false
  ElMessage.success(`已应用模板：${t.name}`)
}

async function deleteTemplate(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该模板？', '提示', { type: 'warning' })
    await request.delete(`/templates/${id}`)
    ElMessage.success('已删除')
    loadTemplates()
  } catch { /* ignore */ }
}

// ─── AI ───
function aiGenerateDescription() {
  if (!form.title.trim()) {
    ElMessage.warning('请先输入活动标题或关键词')
  }
  showAiKeyword.value = true
  if (form.title) aiKeyword.value = form.title
}

async function doAiGenerate() {
  if (!aiKeyword.value.trim()) {
    ElMessage.warning('请输入关键词')
    return
  }
  aiGenerating.value = 'keyword'
  try {
    const res: any = await request.post('/ai/generate-description', { keyword: aiKeyword.value })
    form.title = res.title || form.title
    form.description = res.description || form.description
    showAiKeyword.value = false
    aiKeyword.value = ''
    ElMessage.success('AI 生成完成，请审核修改后保存')
  } catch {
    ElMessage.error('AI 生成失败')
  } finally {
    aiGenerating.value = null
  }
}

async function aiGenerateCover() {
  const prompt = form.title || aiKeyword.value || '志愿活动'
  aiGenerating.value = 'cover'
  try {
    const res: any = await request.post('/ai/generate-cover', { prompt })
    aiCovers.value = res.covers || []
    selectedAiCover.value = -1
    if (aiCovers.value.length > 0) ElMessage.success('封面生成完成，请选择一张')
  } catch {
    ElMessage.error('AI 封面生成失败')
  } finally {
    aiGenerating.value = null
  }
}

function selectAiCover(i: number) {
  selectedAiCover.value = i
  form.coverImage = aiCovers.value[i]
}

// ─── 编辑模式 ───
onMounted(async () => {
  try { loadTemplates() } catch { /* templates optional */ }
  const editId = route.query.edit
  if (!editId) return
  try {
    const data: any = await request.get(`/activities/${editId}`)
    Object.assign(form, {
      title: data.title || '', description: data.description || '',
      category: data.category || '',
      tags: data.tags ? data.tags.split(',').filter(Boolean) : [],
      locationName: data.locationName || '',
      longitude: data.longitude, latitude: data.latitude,
      startTime: data.startTime || '', endTime: data.endTime || '',
      maxParticipants: data.maxParticipants || 50,
      volunteerHours: data.volunteerHours ?? undefined,
      targetGrade: data.targetGrade || '', targetCollege: data.targetCollege || '',
      organizationName: data.organizationName || '',
      coverImage: data.coverImage || '',
    })
  } catch {
    ElMessage.error('加载活动数据失败')
    router.back()
  }
})

// ─── 地图 ───
function onMapPick(lng: number, lat: number) {
  form.longitude = lng
  form.latitude = lat
}

// ─── 标签 ───
function showTagInput() {
  tagInputVisible.value = true
  nextTick(() => tagInputRef.value?.focus())
}
function addTag() {
  const val = tagInputValue.value.trim()
  if (val && !form.tags.includes(val)) form.tags.push(val)
  tagInputVisible.value = false
  tagInputValue.value = ''
}
function removeTag(tag: string) {
  form.tags = form.tags.filter(t => t !== tag)
}

// ─── 上传 ───
function beforeUpload(file: File) {
  if (!file.type.startsWith('image/')) { ElMessage.error('仅支持图片格式'); return false }
  if (file.size / 1024 / 1024 > 5) { ElMessage.error('图片大小不能超过 5MB'); return false }
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

// ─── 提交 ───
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
    const payload = { ...form, tags: form.tags.join(',') }

    if (isEdit.value) {
      await request.put(`/activities/${route.query.edit}`, payload)
      ElMessage.success('修改保存成功')
      router.push('/organizer')
    } else {
      await request.post('/activities', payload)
      ElMessage.success('创建成功')

      // 保存为模板
      if (saveAsTemplate.value) {
        const tplName = templateName.value.trim() || form.title || '未命名模板'
        try {
          await request.post('/templates', { ...payload, name: tplName })
        } catch { /* 模板保存失败不影响活动创建 */ }
      }
      router.push('/organizer')
    }
  } catch { /* error handled by interceptor */ }
  finally { submitting.value = false }
}

/** 返回上一页 */
function goBack() {
  if (route.path.includes('organizer')) {
    router.push('/organizer')
  } else {
    router.push('/admin')
  }
}
</script>

<style scoped>
.create-activity {
  max-width: 900px;
  margin: 20px auto;
  padding: 0 16px;
}
.template-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 4px;
}
.template-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.template-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.template-card:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 8px rgba(64,158,255,0.15);
}
.template-card-name {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
}
.template-card-desc {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  line-height: 1.4;
}
.template-card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.inline-with-ai {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
}
.cover-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.ai-cover-panel {
  margin-top: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
  background: #fafafa;
}
.ai-cover-title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}
.ai-cover-list {
  display: flex;
  gap: 10px;
}
.ai-cover-item {
  width: 130px;
  cursor: pointer;
  border: 2px solid transparent;
  border-radius: 6px;
  overflow: hidden;
  text-align: center;
  transition: border 0.2s;
}
.ai-cover-item.selected {
  border-color: #409EFF;
}
.ai-cover-item img {
  width: 100%;
  height: 65px;
  object-fit: cover;
  display: block;
}
.ai-cover-item span {
  font-size: 11px;
  color: #909399;
  display: block;
  padding: 2px 0;
}
.cover-preview {
  width: 200px;
  margin-top: 10px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}
</style>
