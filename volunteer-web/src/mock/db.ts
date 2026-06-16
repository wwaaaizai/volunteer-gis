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
  role: 'student' | 'admin'
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
  signedCount: number
  coverImage: string | null
  status: 'draft' | 'published' | 'ongoing' | 'ended' | 'cancelled'
  creatorId: number
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

export interface MockDB {
  users: MockUser[]
  activities: MockActivity[]
  signups: MockSignup[]
  /** 自增 ID 计数器 */
  _nextId: { user: number; activity: number; signup: number }
}

const STORAGE_KEY = 'volunteer-mock-db'

let db: MockDB | null = null

/** 初始化或加载 Mock DB */
export function getDB(): MockDB {
  if (db) return db
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
