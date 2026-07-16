import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import {
  parseIcs,
  getMondayOfDate,
  getWeekNumber,
  formatDateISO,
  formatDateShort,
  PERIOD_SLOTS,
  getSemesterStart,
  setSemesterStart,
} from '@/utils/icsParser'
import type { ParsedCourse } from '@/utils/icsParser'

const STORAGE_KEY = 'volunteer_course_ics'
const STORAGE_KEY_WEEK = 'volunteer_course_week'
const STORAGE_KEY_SEMESTER = 'volunteer_course_semester_start'

export interface WeekDay {
  /** 星期标签 (周一~周日) */
  label: string
  /** 日期 yyyy-MM-dd */
  date: string
  /** 简短日期 MM/dd */
  shortDate: string
  /** weekDay: 1=周一...7=周日 */
  weekDay: number
}

export interface FreeSlot {
  /** 日期 yyyy-MM-dd */
  date: string
  /** bigPeriod 1~5 */
  bigPeriod: number
  /** 开始时间 HH:mm */
  startTime: string
  /** 结束时间 HH:mm */
  endTime: string
}

const WEEKDAY_LABELS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

export const useCourseStore = defineStore('course', () => {
  // ──── State ──────────────────────────────
  const courses = ref<ParsedCourse[]>([])
  const sourceText = ref<string>('')
  const hasImported = ref(false)
  const weekStartDate = ref<string>('') // 当前展示周的周一日期 yyyy-MM-dd
  const semesterStartDate = ref<string>('') // 学期开始日期（第1周周一）yyyy-MM-dd

  // 初始化：从 localStorage 恢复
  function init() {
    // 恢复学期开始日期
    const savedSemester = localStorage.getItem(STORAGE_KEY_SEMESTER)
    if (savedSemester) {
      semesterStartDate.value = savedSemester
      setSemesterStart(new Date(savedSemester + 'T00:00:00+08:00'))
    }
    if (hasImported.value) return
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      sourceText.value = saved
      try {
        courses.value = parseIcs(saved)
        hasImported.value = true

        // 未保存学期开始日期时，从第一个课程日期推断，确保首周为第1周
        if (!savedSemester && courses.value.length > 0) {
          const firstCourseDate = new Date(courses.value[0].date + 'T00:00:00+08:00')
          const monday = getMondayOfDate(firstCourseDate)
          setSemesterStartDate(formatDateISO(monday))
        }
      } catch {
        console.warn('课表数据解析失败，将重新导入')
        localStorage.removeItem(STORAGE_KEY)
      }
    }
    // 恢复上次查看的周
    const savedWeek = localStorage.getItem(STORAGE_KEY_WEEK)
    if (savedWeek) {
      weekStartDate.value = savedWeek
    } else {
      // 默认定位到当前日期所在周
      const today = new Date()
      const monday = getMondayOfDate(today)
      weekStartDate.value = formatDateISO(monday)
    }

    // 确保 weekStartDate 在合法范围内（第1周~第30周）
    clampWeekToValidRange()
  }

  /** 将 weekStartDate 钳位到学期开始后才有的有效周范围内 */
  function clampWeekToValidRange() {
    if (!hasImported.value || !semesterStartDate.value || !weekStartDate.value) return
    // 检查当前 weekStartDate 对应的周数
    const monday = new Date(weekStartDate.value + 'T00:00:00+08:00')
    const wn = getWeekNumber(monday)
    if (wn < 1) {
      // 当前周在学期开始之前，定位到第1周
      weekStartDate.value = semesterStartDate.value
      localStorage.setItem(STORAGE_KEY_WEEK, weekStartDate.value)
    } else if (wn > 30) {
      // 超出范围，钳位到第30周
      const semMonday = new Date(semesterStartDate.value + 'T00:00:00+08:00')
      semMonday.setDate(semMonday.getDate() + 29 * 7)
      weekStartDate.value = formatDateISO(semMonday)
      localStorage.setItem(STORAGE_KEY_WEEK, weekStartDate.value)
    }
  }

  // ──── Getters ─────────────────────────────

  /** 当前周数 */
  const weekNumber = computed(() => {
    if (!weekStartDate.value) return 0
    const monday = new Date(weekStartDate.value + 'T00:00:00+08:00')
    return getWeekNumber(monday)
  })

  /** 当前周 Mon~Sun 的日期列表 */
  const weekDays = computed<WeekDay[]>(() => {
    if (!weekStartDate.value) return []
    const monday = new Date(weekStartDate.value + 'T00:00:00+08:00')
    const days: WeekDay[] = []
    for (let i = 0; i < 7; i++) {
      const d = new Date(monday)
      d.setDate(d.getDate() + i)
      const weekDay = d.getDay() || 7 // 周日→7
      days.push({
        label: WEEKDAY_LABELS[weekDay > 6 ? 0 : weekDay],
        date: formatDateISO(d),
        shortDate: formatDateShort(d),
        weekDay: weekDay > 6 ? 0 : weekDay,
      })
    }
    return days
  })

  /** 课程按 (date, smallPeriod) 索引 — 每条记录有具体的日期 */
  const coursesByDateSlot = computed(() => {
    const map = new Map<string, ParsedCourse>()
    for (const c of courses.value) {
      const key = `${c.date}-${c.smallPeriod}`
      if (!map.has(key)) {
        map.set(key, c)
      }
    }
    return map
  })

  /** 当前周所有无课的大节（空闲时段） */
  const freeSlots = computed<FreeSlot[]>(() => {
    if (!weekStartDate.value || courses.value.length === 0) return []
    const monday = new Date(weekStartDate.value + 'T00:00:00+08:00')
    const occupied = new Set<string>()

    // 标记当前周被占用的时段（使用具体日期）
    for (const c of currentWeekCourses.value) {
      occupied.add(`${c.date}-${c.bigPeriod}`)
    }

    // 生成空闲时段（仅工作日的未来时段）
    const now = new Date()
    const slots: FreeSlot[] = []
    for (let i = 0; i < 7; i++) {
      const d = new Date(monday)
      d.setDate(d.getDate() + i)
      const dateStr = formatDateISO(d)
      for (let bp = 1; bp <= 5; bp++) {
        if (occupied.has(`${dateStr}-${bp}`)) continue
        const slotDef = PERIOD_SLOTS[bp - 1]
        // 跳过已过时的时间段
        const slotEnd = new Date(d)
        slotEnd.setHours(slotDef.endH, slotDef.endM, 0, 0)
        if (slotEnd < now) continue
        slots.push({
          date: dateStr,
          bigPeriod: bp,
          startTime: `${slotDef.startH.toString().padStart(2, '0')}:${slotDef.startM.toString().padStart(2, '0')}`,
          endTime: `${slotDef.endH.toString().padStart(2, '0')}:${slotDef.endM.toString().padStart(2, '0')}`,
        })
      }
    }
    return slots
  })

  /** 当前周的课程列表（按具体日期范围过滤） */
  const currentWeekCourses = computed(() => {
    if (!weekStartDate.value) return courses.value
    const monday = new Date(weekStartDate.value + 'T00:00:00+08:00')
    const sunday = new Date(monday)
    sunday.setDate(sunday.getDate() + 6)
    const mondayStr = formatDateISO(monday)
    const sundayStr = formatDateISO(sunday)
    return courses.value.filter(c => c.date >= mondayStr && c.date <= sundayStr)
  })

  // ──── Actions ─────────────────────────────

  /** 导入 ICS 文本（在外部设置完学期日期后调用） */
  function importIcs(text: string, semesterDate?: string): boolean {
    try {
      const parsed = parseIcs(text)
      if (parsed.length === 0) {
        return false
      }
      courses.value = parsed
      sourceText.value = text
      hasImported.value = true
      localStorage.setItem(STORAGE_KEY, text)

      // 设置学期开始日期（如果外部传入）
      if (semesterDate) {
        setSemesterStartDate(semesterDate)
      } else if (!semesterStartDate.value && parsed.length > 0) {
        // 自动推断：第一个课程所在周的周一
        const firstCourseDate = new Date(parsed[0].date + 'T00:00:00+08:00')
        const monday = getMondayOfDate(firstCourseDate)
        setSemesterStartDate(formatDateISO(monday))
      }

      // 自动定位到第一个课程所在周
      if (parsed.length > 0 && !weekStartDate.value) {
        const firstCourseDate = new Date(parsed[0].date + 'T00:00:00+08:00')
        const monday = getMondayOfDate(firstCourseDate)
        weekStartDate.value = formatDateISO(monday)
        localStorage.setItem(STORAGE_KEY_WEEK, weekStartDate.value)
      }

      return true
    } catch {
      return false
    }
  }

  /** 设置学期开始日期 */
  function setSemesterStartDate(dateStr: string) {
    semesterStartDate.value = dateStr
    setSemesterStart(new Date(dateStr + 'T00:00:00+08:00'))
    localStorage.setItem(STORAGE_KEY_SEMESTER, dateStr)
  }

  /** 设置查看周 */
  function setWeek(dateStr: string) {
    weekStartDate.value = dateStr
    localStorage.setItem(STORAGE_KEY_WEEK, dateStr)
  }

  /** 切换到下一周（最多显示到第30周） */
  function nextWeek() {
    if (weekNumber.value >= 30) return
    const monday = new Date(weekStartDate.value + 'T00:00:00+08:00')
    monday.setDate(monday.getDate() + 7)
    setWeek(formatDateISO(monday))
  }

  /** 切换到上一周（不允许小于第1周） */
  function prevWeek() {
    if (weekNumber.value <= 1) return
    const monday = new Date(weekStartDate.value + 'T00:00:00+08:00')
    monday.setDate(monday.getDate() - 7)
    setWeek(formatDateISO(monday))
  }

  /** 清除课表数据 */
  function clearCourses() {
    courses.value = []
    sourceText.value = ''
    hasImported.value = false
    localStorage.removeItem(STORAGE_KEY)
    localStorage.removeItem(STORAGE_KEY_WEEK)
  }

  return {
    courses,
    sourceText,
    hasImported,
    weekStartDate,
    semesterStartDate,
    weekNumber,
    weekDays,
    coursesByDateSlot,
    freeSlots,
    currentWeekCourses,
    init,
    importIcs,
    setSemesterStartDate,
    setWeek,
    nextWeek,
    prevWeek,
    clearCourses,
  }
})
