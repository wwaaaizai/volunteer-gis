/**
 * Mock 内存数据库。
 *
 * <p>基于 localStorage 的轻量持久化，key 为 {@code volunteer-mock-db}。
 * 启动时若无数据则用种子数据初始化；所有写操作回写 localStorage，
 * 刷新页面后状态不丢失（模拟真实后端持久化）。</p>
 */

export interface MockUser {
  id: number
  studentId: string
  password: string
  name: string
  phone: string
  grade?: string
  college?: string
  role: 'student' | 'admin' | 'organizer'
  organization?: string
  employeeId?: string
  totalHours: number
  deleted: number
  createdAt: string
  updatedAt: string
}

export interface MockActivity {
  id: number
  title: string
  description: string
  locationName: string
  longitude: number
  latitude: number
  startTime: string | null
  endTime: string | null
  signupStart: string | null
  signupEnd: string | null
  maxParticipants: number
  volunteerHours?: number | null
  targetGrade?: string
  targetCollege?: string
  organizationName?: string
  proposal?: string
  signedCount: number
  coverImage: string | null
  status: 'draft' | 'published' | 'ongoing' | 'ended' | 'cancelled'
  creatorId: number
  organizerId: number
  category: string
  tags: string
  deleted: number
  createdAt: string
  updatedAt: string
}

export interface MockSignup {
  id: number
  activityId: number
  userId: number
  status: 'signed' | 'signed_in' | 'signed_out' | 'cancelled'
  signInTime: string | null
  signInLng: number | null
  signInLat: number | null
  signOutTime: string | null
  signOutLng: number | null
  signOutLat: number | null
  volunteerHours: number | null
  hourVerified: boolean
  createdAt: string
}

export interface MockTemplate {
  id: number
  userId: number
  name: string
  title: string
  description: string
  category: string
  tags: string
  locationName: string
  maxParticipants: number
  createdAt: string
  preset: boolean
}

export interface MockDB {
  users: MockUser[]
  activities: MockActivity[]
  signups: MockSignup[]
  activityTemplates?: MockTemplate[]
  /** 自增 ID 计数器 */
  _nextId: { user: number; activity: number; signup: number; template?: number }
}

const STORAGE_KEY = 'volunteer-mock-db'
/** 种子数据版本号，升级种子数据时递增，旧版本自动清除 */
const SEED_VERSION = 7

let db: MockDB | null = null

/** 初始化或加载 Mock DB */
export function getDB(): MockDB {
  if (db) return db

  // 检查版本号，旧版本种子数据自动清除
  const storedVersion = localStorage.getItem(STORAGE_KEY + '-version')
  if (!storedVersion || Number(storedVersion) < SEED_VERSION) {
    localStorage.removeItem(STORAGE_KEY)
    localStorage.setItem(STORAGE_KEY + '-version', String(SEED_VERSION))
  }

  const raw = localStorage.getItem(STORAGE_KEY)
  if (raw) {
    try {
      db = JSON.parse(raw) as MockDB
    } catch {
      db = null
    }
  }
  if (!db) {
    db = createSeedDB()
    saveDB()
    localStorage.setItem(STORAGE_KEY + '-version', String(SEED_VERSION))
  }
  return db
}

/** 回写 localStorage */
export function saveDB(): void {
  if (db) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(db))
  }
}

/** 重置为种子数据 */
export function resetDB(): MockDB {
  db = createSeedDB()
  saveDB()
  return db
}

import { createSeedDB } from './data/seed'
