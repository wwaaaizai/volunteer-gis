import { http, HttpResponse } from 'msw'
import { getDB, saveDB } from '../db'
import { parseMockUser } from './auth'

/**
 * 活动管理 Mock Handler。
 * 对照 doc/API_REFERENCE.md §3 实现。
 * Phase 2: 支持组织者角色 + 新字段（category/tags/organizerId）
 */

/** 为旧数据自动补全新字段 */
function normalizeActivity(a: any) {
  return {
    ...a,
    organizerId: a.organizerId ?? a.creatorId ?? 1,
    category: a.category || '',
    tags: a.tags || '',
  }
}

export const activityHandlers = [
  // ── 精确路径 / 更具体的路径放前面 ──

  /** GET /api/activities — 活动列表（学生视角，仅已发布） */
  http.get('/api/activities', () => {
    const db = getDB()
    const list = db.activities
      .filter(a => a.deleted === 0 && a.status === 'published')
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
      .map(normalizeActivity)
    return HttpResponse.json({ code: 200, message: 'success', data: list })
  }),

  /** GET /api/activities/my — 我的活动列表（组织者视角，按状态筛选） */
  http.get('/api/activities/my', ({ request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user || (user.role !== 'admin' && user.role !== 'organizer')) {
      return HttpResponse.json({ code: 403, message: '无权限', data: null }, { status: 403 })
    }
    const url = new URL(request.url)
    const status = url.searchParams.get('status')
    const db = getDB()
    let list = db.activities
      .filter(a => a.deleted === 0 && a.organizerId === user.id)
    if (status) {
      list = list.filter(a => a.status === status)
    }
    list.sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    return HttpResponse.json({ code: 200, message: 'success', data: list.map(normalizeActivity) })
  }),

  /** GET /api/activities/:id — 活动详情 */
  http.get('/api/activities/:id', ({ params }) => {
    const db = getDB()
    const activity = db.activities.find(a => a.id === Number(params.id) && a.deleted === 0)
    if (!activity) {
      return HttpResponse.json({ code: 500, message: '活动不存在', data: null })
    }
    return HttpResponse.json({ code: 200, message: 'success', data: normalizeActivity(activity) })
  }),

  /** POST /api/activities — 创建活动（管理员/组织者） */
  http.post('/api/activities', async ({ request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user || (user.role !== 'admin' && user.role !== 'organizer')) {
      return HttpResponse.json({ code: 403, message: '无权限', data: null }, { status: 403 })
    }
    const body = await request.json() as Record<string, unknown>
    const db = getDB()
    const now = new Date().toISOString().replace('T', ' ').slice(0, 19)
    const newActivity = {
      id: db._nextId.activity++,
      title: (body.title as string) || '',
      description: (body.description as string) || '',
      locationName: (body.locationName as string) || '',
      longitude: Number(body.longitude) || 117.205,
      latitude: Number(body.latitude) || 34.2173,
      startTime: (body.startTime as string) || null,
      endTime: (body.endTime as string) || null,
      signupStart: (body.signupStart as string) || null,
      signupEnd: (body.signupEnd as string) || null,
      maxParticipants: Number(body.maxParticipants) || 50,
      signedCount: 0,
      coverImage: (body.coverImage as string) || null,
      status: 'draft' as const,
      creatorId: user.id,
      organizerId: user.id,                                  // 新增：组织者ID
      category: (body.category as string) || '',              // 新增：分类
      tags: (body.tags as string) || '',                      // 新增：标签（逗号分隔）
      deleted: 0,
      createdAt: now,
      updatedAt: now,
    }
    db.activities.push(newActivity)
    saveDB()
    return HttpResponse.json({ code: 200, message: '创建成功', data: null })
  }),

  /** PUT /api/activities/:id — 编辑活动（管理员/组织者） */
  http.put('/api/activities/:id', async ({ params, request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user || (user.role !== 'admin' && user.role !== 'organizer')) {
      return HttpResponse.json({ code: 403, message: '无权限', data: null }, { status: 403 })
    }
    const db = getDB()
    const activity = db.activities.find(a => a.id === Number(params.id) && a.deleted === 0)
    if (!activity) {
      return HttpResponse.json({ code: 500, message: '活动不存在', data: null })
    }
    const body = await request.json() as Record<string, unknown>

    if (activity.status === 'draft') {
      // 草稿可全编
      Object.assign(activity, {
        title: body.title || activity.title,
        description: body.description || activity.description,
        locationName: body.locationName || activity.locationName,
        longitude: body.longitude != null ? Number(body.longitude) : activity.longitude,
        latitude: body.latitude != null ? Number(body.latitude) : activity.latitude,
        startTime: body.startTime || activity.startTime,
        endTime: body.endTime || activity.endTime,
        maxParticipants: body.maxParticipants != null ? Number(body.maxParticipants) : activity.maxParticipants,
        coverImage: body.coverImage !== undefined ? body.coverImage : activity.coverImage,
        category: body.category !== undefined ? body.category : activity.category,
        tags: body.tags !== undefined ? body.tags : activity.tags,
        updatedAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
      })
    } else if (activity.status === 'published' || activity.status === 'ongoing') {
      // 已发布仅可改描述和封面
      activity.description = (body.description as string) || activity.description
      activity.coverImage = body.coverImage !== undefined ? (body.coverImage as string) : activity.coverImage
      activity.updatedAt = new Date().toISOString().replace('T', ' ').slice(0, 19)
    } else {
      return HttpResponse.json({ code: 500, message: '该状态的活动不可编辑', data: null })
    }
    saveDB()
    return HttpResponse.json({ code: 200, message: '更新成功', data: null })
  }),

  /** PUT /api/activities/:id/publish — 发布活动（管理员/组织者） */
  http.put('/api/activities/:id/publish', ({ params, request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user || (user.role !== 'admin' && user.role !== 'organizer')) {
      return HttpResponse.json({ code: 403, message: '无权限', data: null }, { status: 403 })
    }
    const db = getDB()
    const activity = db.activities.find(a => a.id === Number(params.id) && a.deleted === 0)
    if (!activity) {
      return HttpResponse.json({ code: 500, message: '活动不存在', data: null })
    }
    activity.status = 'published'
    activity.updatedAt = new Date().toISOString().replace('T', ' ').slice(0, 19)
    saveDB()
    return HttpResponse.json({ code: 200, message: '发布成功', data: null })
  }),

  /** GET /api/activities/search?keyword=xxx — 搜索 */
  http.get('/api/activities/search', ({ request }) => {
    const keyword = new URL(request.url).searchParams.get('keyword') || ''
    const db = getDB()
    let list = db.activities.filter(a => a.deleted === 0 && a.status === 'published')
    if (keyword) {
      list = list.filter(a => a.title.includes(keyword))
    }
    list.sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    return HttpResponse.json({ code: 200, message: 'success', data: list.map(normalizeActivity) })
  }),
]
