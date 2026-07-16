import { http, HttpResponse, delay } from 'msw'
import { getDB, saveDB } from '../db'
import type { MockUser } from '../db'

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
      grade: body.grade || '',
      college: body.college || '',
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
        name: user.name || '',
        phone: user.phone || '',
        grade: user.grade || '',
        college: user.college || '',
        organization: user.organization || '',
        totalHours: String(user.totalHours ?? 0),
      },
    })
  }),

  // ============================================================
  // 管理员用户管理（/api/admin/users）
  // ============================================================

  /** GET /api/admin/users — 列出所有用户 */
  http.get('/api/admin/users', async ({ request }) => {
    const authResult = requireAdmin(request.headers.get('Authorization'))
    if (authResult) return authResult

    await delay(200)
    const db = getDB()
    const list = db.users
      .filter(u => u.deleted === 0)
      .map(({ password, ...rest }) => ({ ...rest }))
    return HttpResponse.json({ code: 200, message: 'success', data: list })
  }),

  /** POST /api/admin/users — 创建用户 */
  http.post('/api/admin/users', async ({ request }) => {
    const authResult = requireAdmin(request.headers.get('Authorization'))
    if (authResult) return authResult

    const body = await request.json() as Record<string, string>
    const db = getDB()

    if (db.users.some(u => u.studentId === body.studentId && u.deleted === 0)) {
      return HttpResponse.json({ code: 500, message: '该学号已注册', data: null })
    }

    const newUser: MockUser = {
      id: db._nextId.user++,
      studentId: body.studentId,
      password: body.password,
      name: body.name || '',
      phone: body.phone || '',
      grade: body.grade || '',
      college: body.college || '',
      role: (body.role as MockUser['role']) || 'student',
      organization: body.organization || '',
      totalHours: 0,
      deleted: 0,
      createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
      updatedAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
    }
    db.users.push(newUser)
    saveDB()

    return HttpResponse.json({ code: 200, message: 'success', data: { id: newUser.id, message: '创建成功' } })
  }),

  /** PUT /api/admin/users/:id/password — 重置密码（必须在 :id 泛化路由之前） */
  http.put('/api/admin/users/:id/password', async ({ request, params }) => {
    const authResult = requireAdmin(request.headers.get('Authorization'))
    if (authResult) return authResult

    const id = Number(params.id)
    const body = await request.json() as Record<string, string>
    const db = getDB()
    const user = db.users.find(u => u.id === id && u.deleted === 0)
    if (!user) {
      return HttpResponse.json({ code: 500, message: '用户不存在', data: null })
    }
    user.password = body.password
    user.updatedAt = new Date().toISOString().replace('T', ' ').slice(0, 19)
    saveDB()

    return HttpResponse.json({ code: 200, message: '密码重置成功', data: null })
  }),

  /** PUT /api/admin/users/:id — 更新用户信息 */
  http.put('/api/admin/users/:id', async ({ request, params }) => {
    const authResult = requireAdmin(request.headers.get('Authorization'))
    if (authResult) return authResult

    const id = Number(params.id)
    const body = await request.json() as Record<string, string>
    const db = getDB()
    const user = db.users.find(u => u.id === id && u.deleted === 0)
    if (!user) {
      return HttpResponse.json({ code: 500, message: '用户不存在', data: null })
    }
    if (body.name !== undefined && body.name !== '') user.name = body.name
    if (body.phone !== undefined && body.phone !== '') user.phone = body.phone
    if (body.role !== undefined && body.role !== '') user.role = body.role as MockUser['role']
    if (body.organization !== undefined && body.organization !== '') user.organization = body.organization
    if (body.grade !== undefined) user.grade = body.grade
    if (body.college !== undefined) user.college = body.college
    user.updatedAt = new Date().toISOString().replace('T', ' ').slice(0, 19)
    saveDB()

    return HttpResponse.json({ code: 200, message: '更新成功', data: null })
  }),

  /** DELETE /api/admin/users/:id — 删除用户（逻辑删除） */
  http.delete('/api/admin/users/:id', async ({ request, params }) => {
    const authResult = requireAdmin(request.headers.get('Authorization'))
    if (authResult) return authResult

    const id = Number(params.id)
    const db = getDB()
    const user = db.users.find(u => u.id === id && u.deleted === 0)
    if (!user) {
      return HttpResponse.json({ code: 500, message: '用户不存在', data: null })
    }
    if (user.role === 'admin') {
      return HttpResponse.json({ code: 500, message: '不能删除管理员账户', data: null })
    }
    user.deleted = 1
    user.updatedAt = new Date().toISOString().replace('T', ' ').slice(0, 19)
    saveDB()

    return HttpResponse.json({ code: 200, message: '删除成功', data: null })
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

/**
 * 管理员鉴权中间件。
 * - 无有效 mock token → 401（提示重新登录）
 * - 有 token 但非 admin 角色 → 403（权限不足）
 * - admin 角色 → 返回 undefined，放行
 */
function requireAdmin(authHeader: string | null) {
  const user = parseMockUser(authHeader)
  if (!user) {
    return HttpResponse.json(
      { code: 401, message: '未登录或登录已过期，请重新登录', data: null },
      { status: 401 },
    )
  }
  if (user.role !== 'admin') {
    return HttpResponse.json(
      { code: 403, message: '当前账号无管理员权限', data: null },
      { status: 403 },
    )
  }
  return undefined
}
