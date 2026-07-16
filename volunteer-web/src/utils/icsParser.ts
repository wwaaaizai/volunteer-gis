/**
 * ICS 课表文件解析器
 * - 解析 .ics 文件中的 VEVENT 事件
 * - UTC 时间 → 北京时区 (UTC+8)
 * - 根据时间映射到 10 小节时段
 */

export interface ParsedCourse {
  /** 清洗后的课程名称（去除 ● 和【调】前缀） */
  name: string
  /** 教师姓名 */
  teacher: string
  /** 学分数 */
  credits: string
  /** 校区+教室 */
  location: string
  /** 星期几 0=周日, 1=周一, ..., 6=周六 */
  weekDay: number
  /** 大节编号 1~5（兼容保留，用于空闲时段计算） */
  bigPeriod: number
  /** 起始小节编号 1~10 */
  smallPeriod: number
  /** 结束小节编号 1~10（用于计算 rowspan） */
  endSmallPeriod: number
  /** 具体日期 yyyy-MM-dd */
  date: string
  /** 开始时间 HH:mm (北京时间) */
  startTime: string
  /** 结束时间 HH:mm (北京时间) */
  endTime: string
  /** 原始 SUMMARY（含【调】标记等备注信息） */
  rawSummary: string
  /** 课程唯一标识（用于颜色分配） */
  uid: string
}

// ──── 大节定义（兼容保留） ──────────────────

/** 大节时段定义（北京时间） */
const PERIOD_SLOTS = [
  { bigPeriod: 1, startH: 8, startM: 0, endH: 9, endM: 45 },
  { bigPeriod: 2, startH: 10, startM: 15, endH: 12, endM: 0 },
  { bigPeriod: 3, startH: 14, startM: 0, endH: 15, endM: 45 },
  { bigPeriod: 4, startH: 16, startM: 15, endH: 18, endM: 0 },
  { bigPeriod: 5, startH: 19, startM: 0, endH: 20, endM: 45 },
]

/** 大节标签 */
const PERIOD_LABELS = ['', '1-2节', '3-4节', '5-6节', '7-8节', '9-10节']

/** 大节时间范围字符串 */
const PERIOD_TIMES = [
  '',
  '08:00~09:45',
  '10:15~12:00',
  '14:00~15:45',
  '16:15~18:00',
  '19:00~20:45',
]

// ──── 小节定义（新增） ──────────────────────

/** 10 小节时段定义（北京时间） */
const SMALL_PERIOD_SLOTS = [
  { smallPeriod: 1, startH: 8, startM: 0, endH: 8, endM: 50 },
  { smallPeriod: 2, startH: 8, startM: 55, endH: 9, endM: 45 },
  { smallPeriod: 3, startH: 10, startM: 15, endH: 11, endM: 5 },
  { smallPeriod: 4, startH: 11, startM: 10, endH: 12, endM: 0 },
  { smallPeriod: 5, startH: 14, startM: 0, endH: 14, endM: 50 },
  { smallPeriod: 6, startH: 14, startM: 55, endH: 15, endM: 45 },
  { smallPeriod: 7, startH: 16, startM: 15, endH: 17, endM: 5 },
  { smallPeriod: 8, startH: 17, startM: 10, endH: 18, endM: 0 },
  { smallPeriod: 9, startH: 19, startM: 0, endH: 19, endM: 50 },
  { smallPeriod: 10, startH: 19, startM: 55, endH: 20, endM: 45 },
]

/** 小节标签 */
const SMALL_PERIOD_LABELS = [
  '',
  '第1节', '第2节', '第3节', '第4节', '第5节',
  '第6节', '第7节', '第8节', '第9节', '第10节',
]

/** 小节时间范围字符串 */
const SMALL_PERIOD_TIMES = [
  '',
  '08:00~08:50',
  '08:55~09:45',
  '10:15~11:05',
  '11:10~12:00',
  '14:00~14:50',
  '14:55~15:45',
  '16:15~17:05',
  '17:10~18:00',
  '19:00~19:50',
  '19:55~20:45',
]

export { PERIOD_SLOTS, PERIOD_LABELS, PERIOD_TIMES }
export { SMALL_PERIOD_SLOTS, SMALL_PERIOD_LABELS, SMALL_PERIOD_TIMES }

/**
 * 将 UTC 时间字符串转换为北京时间的 Date 对象
 * ICS 格式: YYYYMMDDTHHMMSSZ
 */
function icsToBeijingDate(icsTimeStr: string): Date {
  const year = parseInt(icsTimeStr.substring(0, 4))
  const month = parseInt(icsTimeStr.substring(4, 6)) - 1
  const day = parseInt(icsTimeStr.substring(6, 8))
  const hour = parseInt(icsTimeStr.substring(9, 11))
  const min = parseInt(icsTimeStr.substring(11, 13))
  const sec = parseInt(icsTimeStr.substring(13, 15))

  // 使用 Date.UTC 创建时间戳后转为本地 Date，在 +8 时区下自动得到北京时间
  return new Date(Date.UTC(year, month, day, hour, min, sec))
}

/**
 * 根据北京时间映射到大节编号
 * @returns bigPeriod (1~5) 或 0（不匹配）
 */
function mapToBigPeriod(hour: number, minute: number): number {
  const totalMinutes = hour * 60 + minute
  for (const slot of PERIOD_SLOTS) {
    const start = slot.startH * 60 + slot.startM
    const end = slot.endH * 60 + slot.endM
    if (totalMinutes >= start && totalMinutes < end) {
      return slot.bigPeriod
    }
  }
  return 0
}

/**
 * 根据北京时间映射到起始小节编号
 * @returns smallPeriod (1~10) 或 0（不匹配）
 */
function mapToSmallPeriod(hour: number, minute: number): number {
  const totalMinutes = hour * 60 + minute
  for (const slot of SMALL_PERIOD_SLOTS) {
    const start = slot.startH * 60 + slot.startM
    const end = slot.endH * 60 + slot.endM
    if (totalMinutes >= start && totalMinutes < end) {
      return slot.smallPeriod
    }
  }
  return 0
}

/**
 * 根据北京时间映射到结束小节编号（含上界，因为下课恰好在边界）
 * @returns smallPeriod (1~10) 或 0（不匹配）
 */
function mapToSmallPeriodEnd(hour: number, minute: number): number {
  const totalMinutes = hour * 60 + minute
  // 从后往前查找，优先匹配含边界的区间
  for (let i = SMALL_PERIOD_SLOTS.length - 1; i >= 0; i--) {
    const slot = SMALL_PERIOD_SLOTS[i]
    const start = slot.startH * 60 + slot.startM
    const end = slot.endH * 60 + slot.endM
    if (totalMinutes > start && totalMinutes <= end) {
      return slot.smallPeriod
    }
  }
  // 没有精确匹配时，用起始映射兜底
  return mapToSmallPeriod(hour, minute)
}

/**
 * 格式化时间为 HH:mm
 */
function formatTime(date: Date): string {
  const h = date.getHours().toString().padStart(2, '0')
  const m = date.getMinutes().toString().padStart(2, '0')
  return `${h}:${m}`
}

/**
 * 清洗课程名称：去除 ● 符号和【调】前缀
 */
function cleanCourseName(raw: string): string {
  return raw
    .replace(/^【[调补]】/, '')
    .replace(/●$/, '')
    .trim()
}

/**
 * 解析 DESCRIPTION 字段
 * 格式: " 教师名  学分数" 或 " 教师名  学分数"
 */
function parseDescription(desc: string): { teacher: string; credits: string } {
  if (!desc) return { teacher: '', credits: '' }
  const trimmed = desc.trim()
  // 用多个空格或汉字"学分"拆分
  const parts = trimmed.split(/\s+/)
  if (parts.length >= 2) {
    const lastPart = parts[parts.length - 1]
    // 提取学分数字
    const creditMatch = lastPart.match(/(\d+\.?\d*)\s*学分/)
    const credits = creditMatch ? creditMatch[1] + '学分' : lastPart
    const teacher = parts.slice(0, -1).join(' ').trim()
    return { teacher, credits }
  }
  return { teacher: trimmed, credits: '' }
}

/**
 * 解析 LOCATION 字段
 * 格式: " 南湖校区  博5-B406" 或 " 南湖校区  南区网球场"
 */
function parseLocation(loc: string): string {
  if (!loc) return ''
  return loc.trim().replace(/\s+/g, ' ')
}

/**
 * 解析 ICS 文本，返回课程列表（每个具体日期的事件保留）
 */
export function parseIcs(icsText: string): ParsedCourse[] {
  const courses: ParsedCourse[] = []
  const seen = new Set<string>() // 去重 key: "date-smallPeriod"

  // 按 VEVENT 切分
  const eventBlocks = icsText.split(/BEGIN:VEVENT/)
  for (const block of eventBlocks) {
    if (!block.includes('END:VEVENT')) continue

    // 提取关键字段
    const dtstart = extractField(block, 'DTSTART')
    const dtend = extractField(block, 'DTEND')
    const summary = extractField(block, 'SUMMARY')
    const description = extractField(block, 'DESCRIPTION')
    const location = extractField(block, 'LOCATION')
    const uid = extractField(block, 'UID')

    if (!dtstart || !dtend || !summary) continue

    // 转换为北京时间
    const startBeijing = icsToBeijingDate(dtstart)
    const endBeijing = icsToBeijingDate(dtend)

    // 星期几 (0=周日, 1=周一...6=周六)
    const weekDay = startBeijing.getDay()

    // 大节编号（兼容保留）
    const bigPeriod = mapToBigPeriod(startBeijing.getHours(), startBeijing.getMinutes())
    if (bigPeriod === 0) continue // 不在标准大节内

    // 小节编号：起始 + 结束
    const smallPeriod = mapToSmallPeriod(startBeijing.getHours(), startBeijing.getMinutes())
    const endSmallPeriod = mapToSmallPeriodEnd(endBeijing.getHours(), endBeijing.getMinutes())
    if (smallPeriod === 0 || endSmallPeriod === 0) continue

    // 具体日期
    const date = formatDateISO(startBeijing)

    // 去重：同一日期+同一起始小节只保留一条
    const slotKey = `${date}-${smallPeriod}`
    if (seen.has(slotKey)) continue
    seen.add(slotKey)

    // 解析详情
    const { teacher, credits } = parseDescription(description)

    courses.push({
      name: cleanCourseName(summary),
      teacher,
      credits,
      location: parseLocation(location),
      weekDay,
      bigPeriod,
      smallPeriod,
      endSmallPeriod,
      date,
      startTime: formatTime(startBeijing),
      endTime: formatTime(endBeijing),
      rawSummary: summary.trim().replace(/●$/, ''),
      uid: uid || '',
    })
  }

  // 按日期 + smallPeriod 排序
  courses.sort((a, b) => {
    if (a.date !== b.date) return a.date.localeCompare(b.date)
    return a.smallPeriod - b.smallPeriod
  })

  return courses
}

/**
 * 从 ICS 事件块中提取字段值
 */
function extractField(block: string, fieldName: string): string {
  const regex = new RegExp(`^${fieldName}(?:;[^:]*)?:(.+)$`, 'm')
  const match = block.match(regex)
  if (!match) return ''

  // 处理折行 (ICS 的行延续以空格开头)
  let value = match[1].trim()
  const lines = block.split(/\r?\n/)
  let collecting = false
  for (const line of lines) {
    if (collecting && line.startsWith(' ')) {
      value += line.trimStart()
    } else {
      collecting = false
    }
    if (line.includes(`${fieldName}:`) || line.includes(`${fieldName};`)) {
      collecting = true
    }
  }

  return value
}

/**
 * 学期开始日期（用于推算周数）
 * 默认为 2026年2月23日（周一），可通过 setSemesterStart() 修改
 */
let _semesterStart = new Date(2026, 1, 23) // 2026-02-23

/** 获取当前学期开始日期 */
export function getSemesterStart(): Date {
  return new Date(_semesterStart)
}

/** 设置学期开始日期（第1周周一） */
export function setSemesterStart(date: Date): void {
  _semesterStart = new Date(date)
  _semesterStart.setHours(0, 0, 0, 0)
}

/**
 * 计算给定日期所在的周数
 * @param date 要查询的日期
 * @returns 周数（从1开始）
 */
export function getWeekNumber(date: Date): number {
  const diffMs = date.getTime() - _semesterStart.getTime()
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
  return Math.floor(diffDays / 7) + 1
}

/**
 * 获取某周的周一日期
 */
export function getMondayOfWeek(weekNum: number): Date {
  const monday = new Date(_semesterStart)
  monday.setDate(monday.getDate() + (weekNum - 1) * 7)
  return monday
}

/**
 * 获取指定日期所在周的周一日期
 */
export function getMondayOfDate(date: Date): Date {
  const d = new Date(date)
  const day = d.getDay()
  const diff = day === 0 ? -6 : 1 - day
  d.setDate(d.getDate() + diff)
  d.setHours(0, 0, 0, 0)
  return d
}

/**
 * 格式化日期为 MM/dd
 */
export function formatDateShort(date: Date): string {
  const m = (date.getMonth() + 1).toString().padStart(2, '0')
  const d = date.getDate().toString().padStart(2, '0')
  return `${m}/${d}`
}

/**
 * 格式化日期为 yyyy-MM-dd
 */
export function formatDateISO(date: Date): string {
  const y = date.getFullYear()
  const m = (date.getMonth() + 1).toString().padStart(2, '0')
  const d = date.getDate().toString().padStart(2, '0')
  return `${y}-${m}-${d}`
}
