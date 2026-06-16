import { http, HttpResponse, delay } from 'msw'
import { getDB, saveDB } from '../db'

/**
 * 认证模块 Mock Handler。
 *
 * <p>Token 格式：{@code mock-jwt-<userId>-<role>}（明文，仅 Mock 用），
 * handler 中间件通过解析 token 反查用户身份。</p>
 */

export const authHandlers = [
  /** POST /api/auth/register */
  http.post('/api/auth/register', async ({ request }) => {
    const body = await request.json() as Record<string, string>
    const db = getDB()

    // 学号唯一性校验
    if (db.users.some(u => u.studentId === body.studentId && u.deleted === 0)) {
      return HttpResponse.json({ code: 500, message: '学号已存在', data: null })
    }

    const newUser = {
      id: db._nextId.user++,
      studentId: body.studentId,
      password: body.password,
      name: body.name || '',
      phone: body.phone || '',
      role: 'student' as const,
      totalHours: 0,
      deleted: 0,
      createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
      updatedAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
    }
    db.users.push(newUser)
    saveDB()

    return HttpResponse.json({ code: 200, message: '注册成功', data: null })
  }),

  /** POST /api/auth/login */
  http.post('/api/auth/login', async ({ request }) => {
    const body = await request.json() as Record<string, string>
    const db = getDB()

    const user = db.users.find(
      u => u.studentId === body.studentId && u.password === body.password && u.deleted === 0
    )
    if (!user) {
      return HttpResponse.json({ code: 500, message: '学号或密码错误', data: null })
    }

    // Mock token: 格式为 mock-jwt-<userId>-<role>
    const token = `mock-jwt-${user.id}-${user.role}`
    return HttpResponse.json({ code: 200, message: 'success', data: { token } })
  }),

  /** GET /api/auth/me */
  http.get('/api/auth/me', async ({ request }) => {
    const authHeader = request.headers.get('Authorization')
    const user = parseMockUser(authHeader)
    if (!user) {
      return HttpResponse.json({ code: 401, message: '未登录', data: null }, { status: 401 })
    }
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        userId: String(user.id),
        studentId: user.studentId,
        role: user.role,
      },
    })
  }),
]

// ── 工具函数 ──────────────────────────────────────────────

/** 从 Authorization header 解析 mock token → MockUser */
export function parseMockUser(authHeader: string | null) {
  if (!authHeader?.startsWith('Bearer ')) return null
  const token = authHeader.slice(7)
  if (!token.startsWith('mock-jwt-')) return null
  const parts = token.split('-')
  if (parts.length < 4) return null
  const userId = Number(parts[2])
  const db = getDB()
  return db.users.find(u => u.id === userId && u.deleted === 0) ?? null
}
