import { http, HttpResponse } from 'msw'
import { getDB, saveDB } from '../db'
import { parseMockUser } from './auth'

/**
 * 报名管理 Mock Handler。
 * 对照 doc/API_REFERENCE.md §4 实现。
 */

export const signupHandlers = [
  /** POST /api/signups?activityId=1 — 报名 */
  http.post('/api/signups', ({ request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user) {
      return HttpResponse.json({ code: 401, message: '请先登录', data: null }, { status: 401 })
    }
    const activityId = Number(new URL(request.url).searchParams.get('activityId'))
    const db = getDB()

    const activity = db.activities.find(a => a.id === activityId && a.deleted === 0)
    if (!activity) {
      return HttpResponse.json({ code: 500, message: '活动不存在', data: null })
    }
    if (activity.status !== 'published') {
      return HttpResponse.json({ code: 500, message: '该活动不在报名期', data: null })
    }
    if (activity.signedCount >= activity.maxParticipants) {
      return HttpResponse.json({ code: 500, message: '报名已满', data: null })
    }
    // 查重
    if (db.signups.some(s => s.activityId === activityId && s.userId === user.id && s.status !== 'cancelled')) {
      return HttpResponse.json({ code: 500, message: '您已报名该活动', data: null })
    }

    const now = new Date().toISOString().replace('T', ' ').slice(0, 19)
    db.signups.push({
      id: db._nextId.signup++,
      activityId,
      userId: user.id,
      status: 'signed',
      signInTime: null, signInLng: null, signInLat: null,
      signOutTime: null, signOutLng: null, signOutLat: null,
      volunteerHours: null,
      hourVerified: false,
      createdAt: now,
    })
    activity.signedCount++
    saveDB()
    return HttpResponse.json({ code: 200, message: '报名成功', data: null })
  }),

  /** DELETE /api/signups?activityId=1 — 取消报名 */
  http.delete('/api/signups', ({ request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user) {
      return HttpResponse.json({ code: 401, message: '请先登录', data: null }, { status: 401 })
    }
    const activityId = Number(new URL(request.url).searchParams.get('activityId'))
    const db = getDB()

    const signup = db.signups.find(
      s => s.activityId === activityId && s.userId === user.id && s.status === 'signed'
    )
    if (!signup) {
      return HttpResponse.json({ code: 500, message: '未找到可取消的报名记录', data: null })
    }

    signup.status = 'cancelled'
    const activity = db.activities.find(a => a.id === activityId)
    if (activity && activity.signedCount > 0) activity.signedCount--
    saveDB()
    return HttpResponse.json({ code: 200, message: '已取消报名', data: null })
  }),

  /** GET /api/signups/my — 我的报名 */
  http.get('/api/signups/my', ({ request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user) {
      return HttpResponse.json({ code: 401, message: '请先登录', data: null }, { status: 401 })
    }
    const db = getDB()
    const list = db.signups.filter(s => s.userId === user.id)
    return HttpResponse.json({ code: 200, message: 'success', data: list })
  }),

  /** GET /api/signups/activity/:activityId — 活动报名名单（管理员/组织者） */
  http.get('/api/signups/activity/:activityId', ({ params, request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user || (user.role !== 'admin' && user.role !== 'organizer')) {
      return HttpResponse.json({ code: 403, message: '无权限', data: null }, { status: 403 })
    }
    const db = getDB()
    const list = db.signups.filter(s => s.activityId === Number(params.activityId))
    return HttpResponse.json({ code: 200, message: 'success', data: list })
  }),

  /** PUT /api/signups/:id/review — 审核报名（通过/拒绝） */
  http.put('/api/signups/:id/review', async ({ params, request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user || (user.role !== 'admin' && user.role !== 'organizer')) {
      return HttpResponse.json({ code: 403, message: '无权限', data: null }, { status: 403 })
    }
    const body = await request.json() as Record<string, string>
    const action = body.action // 'approve' | 'reject'
    const reason = body.reason || ''

    const db = getDB()
    const signup = db.signups.find(s => s.id === Number(params.id))
    if (!signup) {
      return HttpResponse.json({ code: 500, message: '报名记录不存在', data: null })
    }
    if (action === 'approve') {
      signup.status = 'approved' as any
    } else if (action === 'reject') {
      signup.status = 'rejected' as any
      ;(signup as any).reviewReason = reason
    } else {
      return HttpResponse.json({ code: 400, message: '无效操作', data: null })
    }
    saveDB()
    return HttpResponse.json({ code: 200, message: '操作成功', data: null })
  }),
]
