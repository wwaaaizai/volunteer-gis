<template>
  <div class="course-page" :class="{ 'course-page--mobile': appStore.isMobile }">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button @click="prevWeek" circle size="small" :disabled="weekNavDisabled.prev">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6" /></svg>
        </el-button>
        <span class="week-label">
          第 {{ courseStore.weekNumber }} 周
          <span v-if="courseStore.hasImported" class="semester-edit-btn" @click="openSemesterEdit" title="修改开学日期">
            <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          </span>
        </span>
        <el-button @click="nextWeek" circle size="small" :disabled="weekNavDisabled.next">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6" /></svg>
        </el-button>
        <el-date-picker
          v-model="weekPickerDate"
          type="week"
          format="yyyy 第 ww 周"
          placeholder="选择周"
          size="small"
          class="week-picker"
          :disabled-date="disabledWeekDate"
          @change="onWeekPick"
        />
      </div>
      <div class="toolbar-right">
        <template v-if="courseStore.hasImported">
          <el-button size="small" @click="handleReimport">重新导入</el-button>
          <el-button size="small" type="danger" plain @click="handleClear">清空课表</el-button>
        </template>
        <el-button v-else size="small" type="primary" @click="triggerUpload">导入课表(.ics)</el-button>
        <input
          ref="fileInput"
          type="file"
          accept=".ics,.ical,text/calendar"
          style="display:none"
          @change="handleFileChange"
        />
      </div>
    </div>

    <!-- 未导入课表时的占位提示 -->
    <div v-if="!courseStore.hasImported" class="empty-hint">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" width="64" height="64" fill="none" stroke="#c0c4cc" stroke-width="1">
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <line x1="8" y1="9" x2="16" y2="9" />
          <line x1="8" y1="13" x2="16" y2="13" />
          <line x1="8" y1="17" x2="12" y2="17" />
        </svg>
      </div>
      <p>请导入 .ics 课表文件</p>
      <p class="empty-sub">支持从教务系统导出的 iCalendar 格式</p>
    </div>

    <!-- 日历表格 -->
    <div v-else class="calendar-wrapper">
      <div class="calendar-scroll" ref="calendarScroll">
        <table class="course-table">
          <thead>
            <tr>
              <th class="period-header">节次</th>
              <th
                v-for="(day, i) in courseStore.weekDays"
                :key="day.date"
                :class="['day-header', { 'day-header--today': day.date === todayDate }]"
              >
                <div class="day-label">{{ day.label }}</div>
                <div class="day-date">{{ day.shortDate }}</div>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="sp in 10" :key="sp">
              <td class="period-cell">
                <div class="period-num">{{ sp }}</div>
                <div class="period-time period-time--start">{{ getPeriodTime(sp).start }}</div>
                <div class="period-time period-time--end">{{ getPeriodTime(sp).end }}</div>
              </td>
              <!-- 按日期+小节索引渲染各天单元格 -->
              <template v-for="day in courseStore.weekDays" :key="`${day.date}-${sp}`">
                <!-- 有课程起始：按实际跨度渲染 rowspan -->
                <template v-if="getSlotCourse(day.date, sp)">
                  <td
                    :rowspan="getCourseSpan(day.date, sp)"
                    :class="['course-cell', 'course-cell--has-course', { 'course-cell--today': day.date === todayDate }]"
                  >
                    <div
                      class="course-card"
                      :style="{ backgroundColor: getColor(getSlotCourse(day.date, sp)!.uid) }"
                      @click="showDetail(getSlotCourse(day.date, sp)!)"
                    >
                      <div class="course-name">{{ getSlotCourse(day.date, sp)!.name }}</div>
                      <div class="course-location">{{ getSlotCourse(day.date, sp)!.location }}</div>
                    </div>
                  </td>
                </template>
                <!-- 未被 rowspan 覆盖→空白单元格 -->
                <template v-else-if="!isCoveredByRowspan(day.date, sp)">
                  <td :class="['course-cell', { 'course-cell--today': day.date === todayDate }]" />
                </template>
                <!-- 被 rowspan 覆盖→不渲染 td（浏览器自动用上方 rowspan 填充） -->
              </template>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 空闲时间筛选面板 -->
    <div v-if="courseStore.hasImported" class="filter-panel">
      <div class="filter-header">
        <span class="filter-title">活动筛选</span>
        <el-switch v-model="showFreeFilter" active-text="仅显示空闲时段可参加的活动" />
      </div>
      <div v-if="showFreeFilter && filteredActivities.length !== allActivities.length" class="filter-result">
        已筛选 {{ filteredActivities.length }} / {{ allActivities.length }} 个活动
      </div>
    </div>

    <!-- 筛选后的活动列表 -->
    <div v-if="courseStore.hasImported && showFreeFilter" class="activity-section">
      <div v-if="filteredActivities.length === 0" class="empty-activities">暂无空闲时段内可参加的活动</div>
      <div v-else class="activity-list">
        <div
          v-for="act in filteredActivities"
          :key="act.id"
          class="activity-card"
          @click="router.push(`/activity/${act.id}`)"
        >
          <div class="act-title">{{ act.title }}</div>
          <div class="act-info">
            <span>{{ formatDateTime(act.startTime) }}</span>
            <span>{{ act.locationName }}</span>
            <span>{{ act.volunteerHours }}h</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 开学日期设置 Dialog -->
    <el-dialog v-model="semesterDialogVisible" :title="semesterDialogTitle" :width="appStore.isMobile ? '90%' : '380px'">
      <div class="semester-dialog-body">
        <p class="semester-dialog-hint">设置第 1 周周一日期，用于计算课表周数</p>
        <el-date-picker
          v-model="semesterPickerDate"
          type="date"
          placeholder="选择开学日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width:100%"
        />
      </div>
      <template #footer>
        <el-button @click="semesterDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSemesterStart">确认</el-button>
      </template>
    </el-dialog>

    <!-- 课程详情 Dialog -->
    <el-dialog v-model="detailVisible" title="课程详情" :width="appStore.isMobile ? '90%' : '420px'">
      <div v-if="detailCourse" class="course-detail">
        <div class="detail-row">
          <span class="detail-label">课程名称</span>
          <span class="detail-value">{{ detailCourse.rawSummary }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">教师</span>
          <span class="detail-value">{{ detailCourse.teacher || '暂无' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">学分</span>
          <span class="detail-value">{{ detailCourse.credits || '暂无' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">地点</span>
          <span class="detail-value">{{ detailCourse.location || '暂无' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">时间</span>
          <span class="detail-value">{{ detailCourse.startTime }}~{{ detailCourse.endTime }}</span>
        </div>
        <div v-if="hasAdjustMark(detailCourse.rawSummary)" class="detail-row">
          <span class="detail-label">备注</span>
          <span class="detail-value" style="color:#e6a23c">{{ getAdjustMark(detailCourse.rawSummary) }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCourseStore } from '@/stores/course'
import { useAppStore } from '@/stores/app'
import { SMALL_PERIOD_TIMES, SMALL_PERIOD_LABELS, parseIcs, getMondayOfDate } from '@/utils/icsParser'
import { formatDateISO } from '@/utils/icsParser'
import type { ParsedCourse } from '@/utils/icsParser'
import request from '@/api'

const router = useRouter()
const courseStore = useCourseStore()
const appStore = useAppStore()

/** 将 "HH:mm~HH:mm" 格式拆分为起止时间 */
function getPeriodTime(sp: number): { start: string; end: string } {
  const timeStr = SMALL_PERIOD_TIMES[sp] || ''
  const idx = timeStr.indexOf('~')
  if (idx === -1) return { start: timeStr, end: '' }
  return { start: timeStr.slice(0, idx), end: timeStr.slice(idx + 1) }
}

// ──── 初始化 ──────────────────────────────
onMounted(() => {
  courseStore.init()
  if (courseStore.hasImported) {
    fetchActivities()
  }
})

// ──── 开学日期 ────────────────────────────
const semesterDialogVisible = ref(false)
const semesterPickerDate = ref('')
const semesterDialogTitle = ref('设置开学日期')
const pendingIcsText = ref('')

/** 推断第一个课程所在周的周一 */
function inferSemesterMonday(icsText: string): string {
  try {
    const parsed = parseIcs(icsText)
    if (parsed.length === 0) return ''
    const firstDate = new Date(parsed[0].date + 'T00:00:00+08:00')
    const monday = getMondayOfDate(firstDate)
    return formatDateISO(monday)
  } catch {
    return ''
  }
}

/** 打开开学日期编辑弹窗 */
function openSemesterEdit() {
  semesterDialogTitle.value = '修改开学日期'
  semesterPickerDate.value = courseStore.semesterStartDate || ''
  pendingIcsText.value = ''
  semesterDialogVisible.value = true
}

/** 确认开学日期 */
function confirmSemesterStart() {
  if (!semesterPickerDate.value) {
    ElMessage.warning('请选择开学日期')
    return
  }
  if (pendingIcsText.value) {
    // 导入流程：设置学期日期后完成导入
    const ok = courseStore.importIcs(pendingIcsText.value, semesterPickerDate.value)
    if (ok) {
      ElMessage.success(`已导入 ${courseStore.courses.length} 门课程`)
      fetchActivities()
    } else {
      ElMessage.error('课表解析失败，请检查文件格式')
    }
    pendingIcsText.value = ''
  } else {
    // 纯修改学期日期
    courseStore.setSemesterStartDate(semesterPickerDate.value)
    ElMessage.success('开学日期已更新')
  }
  semesterDialogVisible.value = false
}

// ──── 文件上传 ────────────────────────────
const fileInput = ref<HTMLInputElement>()

function triggerUpload() {
  fileInput.value?.click()
}

function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    const text = reader.result as string
    // 先预解析获取第一个课程日期，弹出开学日期确认框
    const inferredMonday = inferSemesterMonday(text)
    semesterDialogTitle.value = '设置开学日期'
    semesterPickerDate.value = inferredMonday || courseStore.semesterStartDate || ''
    pendingIcsText.value = text
    semesterDialogVisible.value = true
  }
  reader.readAsText(file, 'UTF-8')
  // 重置 input 以允许重复选择同一文件
  target.value = ''
}

function handleReimport() {
  triggerUpload()
}

function handleClear() {
  courseStore.clearCourses()
  allActivities.value = []
  showFreeFilter.value = false
  ElMessage.info('课表已清空')
}

// ──── 周导航 ──────────────────────────────
const weekPickerDate = ref<Date>()

/** 周导航按钮禁用状态 */
const weekNavDisabled = computed(() => {
  const wn = courseStore.weekNumber
  return {
    prev: wn <= 1,
    next: wn >= 30 || wn <= 0,
  }
})

/** 限制周选择器可选周范围：第1周~第30周 */
function disabledWeekDate(date: Date): boolean {
  if (!courseStore.hasImported) return false
  // 获取第1周周一
  const d1 = new Date(courseStore.semesterStartDate + 'T00:00:00+08:00')
  if (isNaN(d1.getTime())) return false
  // 获取第30周周一
  const d30 = new Date(d1)
  d30.setDate(d30.getDate() + 29 * 7)
  // 传入 date 为周中某天，计算其周一
  const monday = new Date(date)
  monday.setHours(0, 0, 0, 0)
  const day = monday.getDay()
  const diff = day === 0 ? -6 : 1 - day
  monday.setDate(monday.getDate() + diff)
  return monday < d1 || monday > d30
}

function prevWeek() {
  courseStore.prevWeek()
}

function nextWeek() {
  courseStore.nextWeek()
}

function onWeekPick(date: Date | null) {
  if (!date) return
  const monday = new Date(date)
  // date-picker week 模式可能返回的不是周一，修正
  const day = monday.getDay()
  const diff = day === 0 ? -6 : 1 - day
  monday.setDate(monday.getDate() + diff)
  courseStore.setWeek(formatDateISO(monday))
}

// 今天日期
const todayDate = computed(() => {
  const d = new Date()
  const y = d.getFullYear()
  const m = (d.getMonth() + 1).toString().padStart(2, '0')
  const day = d.getDate().toString().padStart(2, '0')
  return `${y}-${m}-${day}`
})

// ──── 课程颜色 ────────────────────────────
const COLORS = ['#e3f2fd', '#fce4ec', '#e8f5e9', '#fff3e0', '#f3e5f5', '#e0f7fa']

function getColor(uid: string): string {
  if (!uid) return COLORS[0]
  let hash = 0
  for (let i = 0; i < uid.length; i++) {
    hash = ((hash << 5) - hash) + uid.charCodeAt(i)
    hash |= 0
  }
  return COLORS[Math.abs(hash) % COLORS.length]
}

// ──── 课程 slot 查找（按具体日期 + 起始小节） ─
function getSlotCourse(date: string, smallPeriod: number): ParsedCourse | undefined {
  return courseStore.coursesByDateSlot.get(`${date}-${smallPeriod}`)
}

/** 获取课程跨越的小节数（rowspan） */
function getCourseSpan(date: string, sp: number): number {
  const course = getSlotCourse(date, sp)
  if (!course) return 1
  return course.endSmallPeriod - course.smallPeriod + 1
}

/** 检查当前小节是否被前面课程的 rowspan 覆盖 */
function isCoveredByRowspan(date: string, sp: number): boolean {
  // 向前查找，看是否有课程从此处之前开始并跨越到此处
  for (let prevSp = sp - 1; prevSp >= 1; prevSp--) {
    const course = getSlotCourse(date, prevSp)
    if (course && course.endSmallPeriod >= sp) {
      return true
    }
  }
  return false
}

// ──── 课程详情弹窗 ─────────────────────────
const detailVisible = ref(false)
const detailCourse = ref<ParsedCourse | null>(null)

function showDetail(course: ParsedCourse) {
  detailCourse.value = course
  detailVisible.value = true
}

function hasAdjustMark(raw: string): boolean {
  return raw.includes('【调】') || raw.includes('【补】')
}

function getAdjustMark(raw: string): string {
  if (raw.includes('【调】')) return '调课安排'
  if (raw.includes('【补】')) return '补课安排'
  return ''
}

// ──── 活动筛选 ─────────────────────────────
const showFreeFilter = ref(false)
const allActivities = ref<any[]>([])
const filteredActivities = computed(() => {
  if (!showFreeFilter.value) return allActivities.value
  const freeSlots = courseStore.freeSlots
  if (freeSlots.length === 0) return allActivities.value

  return allActivities.value.filter(act => {
    if (!act.startTime || !act.endTime) return true
    return isActivityInFreeSlot(act, freeSlots)
  })
})

function isActivityInFreeSlot(act: any, freeSlots: { date: string; startTime: string; endTime: string }[]): boolean {
  const actStart = new Date(act.startTime)
  const actEnd = new Date(act.endTime)
  const actDate = formatDateISO(actStart)

  for (const slot of freeSlots) {
    if (slot.date !== actDate) continue

    const [slotStartH, slotStartM] = slot.startTime.split(':').map(Number)
    const [slotEndH, slotEndM] = slot.endTime.split(':').map(Number)

    const actStartMin = actStart.getHours() * 60 + actStart.getMinutes()
    const actEndMin = actEnd.getHours() * 60 + actEnd.getMinutes()
    const slotStartMin = slotStartH * 60 + slotStartM
    const slotEndMin = slotEndH * 60 + slotEndM

    // 活动时间完全在空闲时段内
    if (actStartMin >= slotStartMin && actEndMin <= slotEndMin) {
      return true
    }
  }

  // 如果活动时间不在当周内，也保留
  if (courseStore.freeSlots.length > 0) {
    const firstSlotDate = courseStore.freeSlots[0].date
    if (actDate < firstSlotDate || actDate > firstSlotDate) {
      // 不在当周且找不到匹配 → 不筛选掉
      // 如果跨周检查没命中则过滤
    }
  }

  return false
}

async function fetchActivities() {
  try {
    const data = await request.get('/activities')
    allActivities.value = Array.isArray(data) ? data : []
  } catch {
    allActivities.value = []
  }
}

watch(showFreeFilter, (val) => {
  if (val && allActivities.value.length === 0) {
    fetchActivities()
  }
})

function formatDateTime(dt: string): string {
  if (!dt) return ''
  const d = new Date(dt)
  const m = (d.getMonth() + 1).toString().padStart(2, '0')
  const day = d.getDate().toString().padStart(2, '0')
  const h = d.getHours().toString().padStart(2, '0')
  const min = d.getMinutes().toString().padStart(2, '0')
  return `${m}/${day} ${h}:${min}`
}
</script>

<style scoped>
.course-page {
  min-height: 100%;
  background: #f5f7fa;
  padding-bottom: 20px;
}

/* ──── 工具栏 ────────────────────────────── */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-wrap: wrap;
  gap: 8px;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.week-label {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  min-width: 80px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.semester-edit-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  cursor: pointer;
  color: #909399;
  transition: all 0.15s;
  flex-shrink: 0;
}
.semester-edit-btn:hover {
  color: #409eff;
  background: #ecf5ff;
}
.week-picker {
  width: 160px;
}

/* ──── 空状态 ────────────────────────────── */
.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: #909399;
}
.empty-hint p {
  margin: 8px 0 0;
  font-size: 15px;
}
.empty-sub {
  font-size: 12px;
  color: #c0c4cc;
}
.empty-icon {
  margin-bottom: 8px;
}

/* ──── 日历表格 ──────────────────────────── */
.calendar-wrapper {
  margin: 12px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}
.calendar-scroll {
  overflow-x: auto;
}
.course-table {
  width: 100%;
  min-width: 700px;
  border-collapse: collapse;
  table-layout: fixed;
}

/* 表头 */
.course-table thead th {
  position: sticky;
  top: 0;
  background: #fafafa;
  z-index: 1;
  border-bottom: 2px solid #e4e7ed;
  padding: 8px 4px;
  text-align: center;
}
.period-header {
  width: 52px;
  color: #909399;
  font-size: 12px;
}
.day-header {
  color: #606266;
}
.day-header--today {
  background: #e6f7ff !important;
}
.day-label {
  font-size: 13px;
  font-weight: 500;
}
.day-date {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

/* 节次列 */
.period-cell {
  text-align: center;
  padding: 2px 2px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
  width: 52px;
  vertical-align: middle;
}
.period-num {
  font-size: 12px;
  font-weight: 600;
  color: #303133;
  line-height: 1.3;
}
.period-time {
  font-size: 10px;
  color: #909399;
  line-height: 1.3;
}
.period-time--start {
  margin-top: 1px;
}
.period-time--end {
  /* 与 start 样式一致 */
}

/* 课程格子 */
.course-cell {
  border-bottom: 1px solid #ebeef5;
  border-left: 1px solid #f0f0f0;
  padding: 4px;
  vertical-align: top;
}
.course-cell--today {
  background: #e6f7ff50;
}
/* 有课程的格子设 relative 作为 absolute 卡片的定位基准 */
.course-cell--has-course {
  position: relative;
}

/* 课程卡片 — 绝对定位填充整格，跨 rowspan 时自动撑满 */
.course-card {
  position: absolute;
  top: 4px;
  left: 4px;
  right: 4px;
  bottom: 4px;
  border-radius: 6px;
  padding: 6px 8px;
  cursor: pointer;
  transition: transform 0.1s, box-shadow 0.1s;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}
.course-card:hover {
  transform: scale(1.02);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.course-name {
  font-size: 12px;
  font-weight: 600;
  color: #303133;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.course-location {
  font-size: 10px;
  color: #606266;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ──── 筛选面板 ──────────────────────────── */
.filter-panel {
  margin: 0 12px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
}
.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.filter-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}
.filter-result {
  margin-top: 8px;
  font-size: 12px;
  color: #67c23a;
}

/* ──── 活动列表 ──────────────────────────── */
.activity-section {
  margin: 0 12px;
}
.empty-activities {
  text-align: center;
  padding: 24px 0;
  color: #c0c4cc;
  font-size: 13px;
}
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}
.activity-card {
  padding: 12px 14px;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.activity-card:active {
  background: #f5f7fa;
}
.act-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}
.act-info {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

/* ──── 开学日期弹窗 ──────────────────────── */
.semester-dialog-body {
  padding: 0 8px;
}
.semester-dialog-hint {
  font-size: 13px;
  color: #909399;
  margin: 0 0 12px;
}

/* ──── 课程详情弹窗 ──────────────────────── */
.course-detail {
  padding: 0 8px;
}
.detail-row {
  display: flex;
  margin-bottom: 12px;
  line-height: 1.5;
}
.detail-label {
  width: 64px;
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
}
.detail-value {
  font-size: 14px;
  color: #303133;
  flex: 1;
}

/* ──── 移动端适配 ────────────────────────── */
.course-page--mobile .toolbar {
  padding: 10px 12px;
}
.course-page--mobile .week-picker {
  width: 130px;
}
.course-page--mobile .course-table {
  min-width: 420px;
  font-size: 12px;
}
.course-page--mobile .period-header {
  width: 40px;
  font-size: 10px;
}
.course-page--mobile .period-cell {
  width: 40px;
  padding: 1px 1px;
}
.course-page--mobile .period-num {
  font-size: 10px;
}
.course-page--mobile .period-time {
  font-size: 8px;
}
.course-page--mobile .course-cell {
  min-height: 40px;
}
.course-page--mobile .course-name {
  font-size: 10px;
}
.course-page--mobile .course-location {
  font-size: 8px;
}
.course-page--mobile .course-card {
  padding: 4px 5px;
  top: 2px;
  left: 2px;
  right: 2px;
  bottom: 2px;
}
.course-page--mobile .day-label {
  font-size: 11px;
}
.course-page--mobile .day-date {
  font-size: 9px;
}
</style>
