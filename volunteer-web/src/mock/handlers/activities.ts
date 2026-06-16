import { http, HttpResponse } from 'msw'
import { getDB, saveDB } from '../db'
import { parseMockUser } from './auth'

/**
 * 活动管理 Mock Handler。
 * 对照 doc/API_REFERENCE.md §3 实现。
 */

export const activityHandlers = [
  /** GET /api/activities — 活动列表 */
  http.get('/api/activities', () => {
    const db = getDB()
    const list = db.activities
      .filter(a => a.deleted === 0 && a.status === 'published')
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    return HttpResponse.json({ code: 200, message: 'success', data: list })
  }),

  /** GET /api/activities/:id — 活动详情 */
  http.get('/api/activities/:id', ({ params }) => {
    const db = getDB()
    const activity = db.activities.find(a => a.id === Number(params.id) && a.deleted === 0)
    if (!activity) {
      return HttpResponse.json({ code: 500, message: '活动不存在', data: null })
    }
    return HttpResponse.json({ code: 200, message: 'success', data: activity })
  }),

  /** POST /api/activities — 创建活动（管理员） */
  http.post('/api/activities', async ({ request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user || user.role !== 'admin') {
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
      coverImage: null,
      status: 'draft' as const,
      creatorId: user.id,
      deleted: 0,
      createdAt: now,
      updatedAt: now,
    }
    db.activities.push(newActivity)
    saveDB()
    return HttpResponse.json({ code: 200, message: '创建成功', data: null })
  }),

  /** PUT /api/activities/:id/publish — 发布活动（管理员） */
  http.put('/api/activities/:id/publish', ({ params, request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user || user.role !== 'admin') {
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
    return HttpResponse.json({ code: 200, message: 'success', data: list })
  }),
]
