import { http, HttpResponse } from 'msw'
import { getDB, saveDB } from '../db'
import { parseMockUser } from './auth'

/**
 * 活动模板 Mock Handler（含年常活动预设模板）。
 */

/** 预设的年度常规活动模板 */
const PRESET_TEMPLATES = [
  {
    id: -1,
    name: '📚 图书馆共建活动',
    category: 'campus',
    tags: '图书馆,共建,室内',
    title: '图书馆共建志愿服务',
    description: '参与图书馆日常维护与共建，包括图书整理上架、阅览室环境维护、读者引导咨询、电子资源推广等。',
    locationName: '图书馆',
    longitude: 117.133135,
    latitude: 34.217480,
    maxParticipants: 30,
    preset: true,
  },
  {
    id: -2,
    name: '🏃 校运会志愿活动',
    category: 'campus',
    tags: '运动会,户外,体力',
    title: '校运会志愿服务',
    description: '协助校运会赛事组织，包括场地布置、器材搬运、赛道维护、秩序引导等。',
    locationName: '第一运动场',
    longitude: 117.134128,
    latitude: 34.221646,
    maxParticipants: 60,
    preset: true,
  },
]

export const templateHandlers = [
  /** GET /api/templates — 获取模板列表（预设 + 用户自定义） */
  http.get('/api/templates', ({ request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user) {
      return HttpResponse.json({ code: 403, message: '无权限', data: null }, { status: 403 })
    }
    const db = getDB()
    const myTemplates = (db.activityTemplates || []).filter(t => t.userId === user.id)
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: [...PRESET_TEMPLATES, ...myTemplates],
    })
  }),

  /** POST /api/templates — 保存活动为模板 */
  http.post('/api/templates', async ({ request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user || (user.role !== 'admin' && user.role !== 'organizer')) {
      return HttpResponse.json({ code: 403, message: '无权限', data: null }, { status: 403 })
    }
    const body = await request.json() as Record<string, unknown>
    const db = getDB()
    if (!db.activityTemplates) db.activityTemplates = []

    const template = {
      id: db._nextId.template ?? 1,
      userId: user.id,
      name: (body.name as string) || '未命名模板',
      title: (body.title as string) || '',
      description: (body.description as string) || '',
      category: (body.category as string) || 'campus',
      tags: (body.tags as string) || '',
      locationName: (body.locationName as string) || '',
      maxParticipants: Number(body.maxParticipants) || 50,
      createdAt: new Date().toISOString(),
      preset: false,
    }
    db.activityTemplates.push(template)
    db._nextId.template = (db._nextId.template ?? 1) + 1
    saveDB()
    return HttpResponse.json({ code: 200, message: '保存成功', data: template })
  }),

  /** DELETE /api/templates/:id — 删除自定义模板 */
  http.delete('/api/templates/:id', ({ params, request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user) {
      return HttpResponse.json({ code: 403, message: '无权限', data: null }, { status: 403 })
    }
    const db = getDB()
    if (!db.activityTemplates) return HttpResponse.json({ code: 500, message: '模板不存在', data: null })
    const idx = db.activityTemplates.findIndex(t => t.id === Number(params.id) && t.userId === user.id)
    if (idx === -1) return HttpResponse.json({ code: 500, message: '模板不存在', data: null })
    db.activityTemplates.splice(idx, 1)
    saveDB()
    return HttpResponse.json({ code: 200, message: '删除成功', data: null })
  }),
]
