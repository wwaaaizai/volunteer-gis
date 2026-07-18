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
              {{ aiGenerating === 'title' ? '⏳ AI 生成中...' : (aiGenerated ? '🔄 重新生成' : '🤖 AI 生成') }}
            </el-button>
            <el-button
              v-if="aiGenerated"
              size="small"
              text
              @click="showAiKeyword = true; aiKeyword = form.title"
            >
              ✏️ 改关键词
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
            >{{ aiGenerated ? '🔄 重新生成描述' : '🤖 AI 生成描述' }}</el-button>
          </div>
        </el-form-item>

        <!-- 活动地点 -->
        <el-form-item label="活动地点" prop="locationName">
          <el-autocomplete
            v-model="form.locationName"
            :fetch-suggestions="queryPoiSuggestions"
            placeholder="如：博学楼101（输入时自动匹配校园POI库）"
            style="width:100%"
            clearable
          />
        </el-form-item>

        <!-- 地图选点 -->
        <el-form-item label="地图选点" prop="longitude" class="map-picker-form-item">
          <MapPicker
            ref="mapPickerRef"
            :modelLng="form.longitude"
            :modelLat="form.latitude"
            :mapHeight="340"
            @update="onMapPick"
          />
          <!-- 分地点列表 -->
          <div class="extra-locations" v-if="extraLocations.length > 0">
            <div class="extra-loc-title">📌 分地点（共 {{ extraLocations.length }} 处）</div>
            <div v-for="(loc, i) in extraLocations" :key="i" class="extra-loc-row">
              <el-input v-model="loc.name" placeholder="地点名称" size="small" style="width:160px" />
              <span class="extra-loc-coord">{{ loc.lng.toFixed(6) }}, {{ loc.lat.toFixed(6) }}</span>
              <el-button size="small" type="danger" text @click="removeExtraLoc(i)">删除</el-button>
            </div>
          </div>
          <div class="extra-loc-add" v-if="form.longitude && form.latitude">
            <el-button size="small" @click="addExtraLoc">
              + 添加分地点（如活动在多个位置开展）
            </el-button>
          </div>
        </el-form-item>

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
          <el-autocomplete
            v-model="form.organizationName"
            :fetch-suggestions="queryOrgSuggestions"
            placeholder="选择或输入，如：环测学院志愿者协会"
            style="width:100%"
            clearable
          />
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
          <el-button type="success" @click="showPreview = true">👁️ 预览</el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>

      </el-form>

      <!-- 预览对话框 -->
      <el-dialog v-model="showPreview" title="活动预览（学生视角）" width="700px" top="5vh">
        <el-card shadow="never">
          <h3 style="margin-top:0">{{ form.title || '未填写标题' }}</h3>
          <el-tag :type="form.status === 'draft' ? 'info' : 'success'" size="small" style="margin-bottom:12px">
            {{ form.status === 'draft' ? '草稿' : '已发布' }}
          </el-tag>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="地点">{{ form.locationName || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ categoryLabel(form.category) }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ form.startTime || '未设定' }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ form.endTime || '未设定' }}</el-descriptions-item>
            <el-descriptions-item label="报名上限">{{ form.maxParticipants }} 人</el-descriptions-item>
            <el-descriptions-item label="标签">
              <el-tag v-for="t in form.tags" :key="t" size="small" style="margin-right:4px">{{ t }}</el-tag>
              <span v-if="form.tags.length===0" style="color:#909399">无</span>
            </el-descriptions-item>
            <el-descriptions-item label="分地点" :span="2" v-if="extraLocations.length > 0">
              <span v-for="(loc,i) in extraLocations" :key="i" style="margin-right:12px">
                📍{{ loc.name }}
              </span>
            </el-descriptions-item>
          </el-descriptions>
          <div style="margin-top:16px">
            <h4>活动描述</h4>
            <p style="white-space:pre-wrap;color:#606266">{{ form.description || '未填写描述' }}</p>
          </div>
        </el-card>
      </el-dialog>
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
import { findNearestPoi, searchPoi } from '@/utils/campusPoi'
import MapPicker from '@/components/map/MapPicker.vue'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const mapPickerRef = ref<InstanceType<typeof MapPicker>>()
const submitting = ref(false)
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
const aiGenerated = ref(false)
const showAiKeyword = ref(false)
const aiKeyword = ref('')

const isEdit = computed(() => !!route.query.edit)
const showPreview = ref(false)

function categoryLabel(cat: string) {
  const map: Record<string, string> = {
    environmental: '环保', support: '助学', education: '支教',
    community: '社区', campus: '校园', other: '其他',
  }
  return map[cat] || cat || '未分类'
}

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
})

const rules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  locationName: [{ required: true, message: '请输入活动地点', trigger: 'blur' }],
  longitude: [{ required: true, message: '请选择活动位置', trigger: 'blur' }],
  latitude: [{ required: true, message: '请选择活动位置', trigger: 'blur' }],
  maxParticipants: [{ required: true, message: '请设置报名上限', trigger: 'blur' }],
}

/** 上传地址（Vite 代理到后端） */
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
  // 模板有坐标 → 地图自动定位
  if (t.longitude && t.latitude) {
    form.longitude = t.longitude
    form.latitude = t.latitude
  }
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
  // 已生成过：直接用当前标题重新生成，跳过输入步骤
  if (aiGenerated.value) {
    doAiGenerateFromKeyword(form.title || aiKeyword.value || '志愿活动')
    return
  }
  // 首次：弹出关键词输入
  showAiKeyword.value = true
  if (form.title) aiKeyword.value = form.title
}

async function doAiGenerate() {
  if (!aiKeyword.value.trim()) {
    ElMessage.warning('请输入关键词')
    return
  }
  await doAiGenerateFromKeyword(aiKeyword.value)
}

async function doAiGenerateFromKeyword(keyword: string) {
  if (!keyword.trim()) {
    ElMessage.warning('关键词为空')
    return
  }
  aiGenerating.value = 'keyword'
  try {
    const res: any = await request.post('/ai/generate-description', { keyword })
    form.title = res.title || form.title
    form.description = res.description || form.description
    showAiKeyword.value = false
    aiKeyword.value = ''
    aiGenerated.value = true
    ElMessage.success('AI 生成完成，点🔄可直接重新生成')
  } catch {
    ElMessage.error('AI 生成失败')
  } finally {
    aiGenerating.value = null
  }
}

// ─── 编辑模式 ───
onMounted(async () => {
  loadTemplates()
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
    })
    // 回填分地点
    if (data.extraLocations) {
      try {
        extraLocations.value = JSON.parse(data.extraLocations)
      } catch { extraLocations.value = [] }
    }
  } catch {
    ElMessage.error('加载活动数据失败')
    router.back()
  }
})

// ─── 归属组织预设 ───
const ORG_LIST = [
  '环测学院志愿者协会', '计算机学院志愿者协会', '矿业学院志愿者协会',
  '机电学院志愿者协会', '信控学院志愿者协会', '化工学院志愿者协会',
  '材料与物理学院志愿者协会', '力学与土木学院志愿者协会',
  '经管学院志愿者协会', '公共管理学院志愿者协会', '建筑学院志愿者协会',
  '资源学院志愿者协会', '体育学院志愿者协会',
  '校团委志愿者协会', '校学生会志愿部', '图书馆志愿者服务队',
]
function queryOrgSuggestions(kw: string, cb: (list: { value: string }[]) => void) {
  if (!kw) { cb(ORG_LIST.map(v => ({ value: v }))); return }
  cb(ORG_LIST.filter(v => v.includes(kw)).map(v => ({ value: v })))
}

// ─── POI 地点搜索 ───
function queryPoiSuggestions(keyword: string, cb: (list: { value: string }[]) => void) {
  if (!keyword || keyword.length < 1) { cb([]); return }
  const results = searchPoi(keyword).slice(0, 8).map(p => ({ value: p.name }))
  cb(results)
}

// ─── 地图 ───
function onMapPick(lng: number, lat: number) {
  form.longitude = lng
  form.latitude = lat
  // POI 自动识别：只在用户未手动输入时自动填充
  const poi = findNearestPoi(lng, lat)
  if (poi && !form.locationName) {
    form.locationName = poi.name
  }
}

// ─── 多点选取 ───
interface ExtraLocation { name: string; lng: number; lat: number }
const extraLocations = ref<ExtraLocation[]>([])

function syncLocationName() {
  const seen = new Set<string>()
  const names: string[] = []
  // 主地点 POI
  const mainPoi = findNearestPoi(form.longitude, form.latitude)
  if (mainPoi && !seen.has(mainPoi.name)) { names.push(mainPoi.name); seen.add(mainPoi.name) }
  // 分地点（去重）
  for (const loc of extraLocations.value) {
    if (!seen.has(loc.name)) { names.push(loc.name); seen.add(loc.name) }
  }
  form.locationName = names.join('、') || form.locationName
}

function addExtraLoc() {
  if (!form.longitude || !form.latitude) return
  const poi = findNearestPoi(form.longitude, form.latitude)
  console.log('[POI] 点击坐标:', form.longitude.toFixed(6), form.latitude.toFixed(6), '→ 匹配:', poi?.name || '无')
  const name = poi ? poi.name : `分地点${extraLocations.value.length + 1}`
  extraLocations.value.push({
    name,
    lng: form.longitude,
    lat: form.latitude,
  })
  syncLocationName()
}

function removeExtraLoc(i: number) {
  extraLocations.value.splice(i, 1)
  syncLocationName()
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
    const payload: any = { ...form, tags: form.tags.join(',') }
    if (extraLocations.value.length > 0) {
      payload.extraLocations = JSON.stringify(extraLocations.value)
    }

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
  max-width: 1100px;
  margin: 20px auto;
  padding: 0 24px;
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
.map-picker-form-item :deep(.el-form-item__content) {
  width: 100%;
  flex-wrap: wrap;
}
.extra-locations {
  width: 100%;
  margin-top: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}
.extra-loc-title {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
  margin-bottom: 6px;
}
.extra-loc-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.extra-loc-coord {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
}
.extra-loc-add {
  width: 100%;
  margin-top: 6px;
}
</style>
