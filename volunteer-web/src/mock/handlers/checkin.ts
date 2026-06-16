import { http, HttpResponse } from 'msw'
import { getDB, saveDB } from '../db'
import { parseMockUser } from './auth'

/**
 * 签到签退 Mock Handler。
 *
 * <p>内置 TS 版 Haversine 距离计算，与后端 {@code SpatialCalculator} 逻辑一致，
 * 500m 阈值保持同步。这证明空间逻辑的抽象可以在前后端各自独立实现。</p>
 */

/** 签到允许最大距离（米），与后端 SpatialCalculator / CheckInServiceImpl 一致 */
const MAX_SIGN_DISTANCE = 500

/** 从 request.url 解析查询参数（MSW v2 resolver 不暴露 url，需自行解析） */
function queryParams(request: Request): URLSearchParams {
  return new URL(request.url).searchParams
}

/** Haversine 公式（与后端 com.cumt.volunteer.geo.service.SpatialCalculator 一致） */
function haversine(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6_371_000
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLng = (lng2 - lng1) * Math.PI / 180
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLng / 2) * Math.sin(dLng / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

export const checkinHandlers = [
  /** POST /api/checkin/location?activityId=1&lng=117.15&lat=34.22 — 定位签到 */
  http.post('/api/checkin/location', ({ request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user) {
      return HttpResponse.json({ code: 401, message: '请先登录', data: null }, { status: 401 })
    }
    const params = queryParams(request)
    const activityId = Number(params.get('activityId'))
    const lng = Number(params.get('lng'))
    const lat = Number(params.get('lat'))
    const db = getDB()

    const signup = db.signups.find(
      s => s.activityId === activityId && s.userId === user.id && s.status === 'signed'
    )
    if (!signup) {
      return HttpResponse.json({ code: 500, message: '当前状态不允许签到', data: null })
    }

    const activity = db.activities.find(a => a.id === activityId)
    if (!activity) {
      return HttpResponse.json({ code: 500, message: '活动不存在', data: null })
    }

    // Haversine 距离校验
    const distance = haversine(activity.latitude, activity.longitude, lat, lng)
    if (distance > MAX_SIGN_DISTANCE) {
      return HttpResponse.json({
        code: 500,
        message: `距离活动地点太远，请到达活动地点后签到（当前距离: ${Math.round(distance)}米）`,
        data: null,
      })
    }

    const now = new Date().toISOString().replace('T', ' ').slice(0, 19)
    signup.status = 'signed_in'
    signup.signInTime = now
    signup.signInLng = lng
    signup.signInLat = lat
    saveDB()
    return HttpResponse.json({ code: 200, message: '签到成功', data: null })
  }),

  /** POST /api/checkin/qr?activityId=1&code=xxx — 扫码签到 */
  http.post('/api/checkin/qr', ({ request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user) {
      return HttpResponse.json({ code: 401, message: '请先登录', data: null }, { status: 401 })
    }
    const params = queryParams(request)
    const activityId = Number(params.get('activityId'))
    const code = params.get('code') || ''
    const db = getDB()

    const signup = db.signups.find(
      s => s.activityId === activityId && s.userId === user.id && s.status === 'signed'
    )
    if (!signup) {
      return HttpResponse.json({ code: 500, message: '当前状态不允许签到', data: null })
    }

    // 验证二维码
    const expected = btoa(`CHECKIN:${activityId}`)
    if (code !== expected) {
      return HttpResponse.json({ code: 500, message: '签到二维码无效', data: null })
    }

    const now = new Date().toISOString().replace('T', ' ').slice(0, 19)
    signup.status = 'signed_in'
    signup.signInTime = now
    saveDB()
    return HttpResponse.json({ code: 200, message: '签到成功', data: null })
  }),

  /** POST /api/checkin/out?activityId=1&lng=117.15&lat=34.22 — 签退 */
  http.post('/api/checkin/out', ({ request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user) {
      return HttpResponse.json({ code: 401, message: '请先登录', data: null }, { status: 401 })
    }
    const params = queryParams(request)
    const activityId = Number(params.get('activityId'))
    const lng = Number(params.get('lng'))
    const lat = Number(params.get('lat'))
    const db = getDB()

    const signup = db.signups.find(
      s => s.activityId === activityId && s.userId === user.id && s.status === 'signed_in'
    )
    if (!signup) {
      return HttpResponse.json({ code: 500, message: '请先完成签到', data: null })
    }

    const now = new Date().toISOString().replace('T', ' ').slice(0, 19)
    signup.status = 'signed_out'
    signup.signOutTime = now
    signup.signOutLng = lng
    signup.signOutLat = lat

    // 自动计算志愿时长（与后端 CheckInServiceImpl 一致）
    if (signup.signInTime) {
      const signIn = new Date(signup.signInTime.replace(' ', 'T'))
      const signOut = new Date(now.replace(' ', 'T'))
      const hours = (signOut.getTime() - signIn.getTime()) / 3600000
      signup.volunteerHours = Math.round(hours * 10) / 10
    }
    signup.hourVerified = false
    saveDB()
    return HttpResponse.json({ code: 200, message: '签退成功', data: null })
  }),

  /** GET /api/checkin/qrcode/:activityId — 生成签到二维码（管理员） */
  http.get('/api/checkin/qrcode/:activityId', ({ params, request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user || user.role !== 'admin') {
      return HttpResponse.json({ code: 403, message: '无权限', data: null }, { status: 403 })
    }
    const activityId = Number(params.activityId)
    const qrcode = btoa(`CHECKIN:${activityId}`)
    return HttpResponse.json({ code: 200, message: 'success', data: qrcode })
  }),

  /** PUT /api/checkin/verify-hours/:signupId — 审核时长（管理员） */
  http.put('/api/checkin/verify-hours/:signupId', ({ params, request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user || user.role !== 'admin') {
      return HttpResponse.json({ code: 403, message: '无权限', data: null }, { status: 403 })
    }
    const db = getDB()
    const signup = db.signups.find(s => s.id === Number(params.signupId))
    if (!signup) {
      return HttpResponse.json({ code: 500, message: '签到记录不存在', data: null })
    }
    signup.hourVerified = true
    saveDB()
    return HttpResponse.json({ code: 200, message: '审核通过', data: null })
  }),
]
