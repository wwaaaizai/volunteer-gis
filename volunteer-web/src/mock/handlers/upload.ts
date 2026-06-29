import { http, HttpResponse } from 'msw'
import { parseMockUser } from './auth'

/**
 * 文件上传 Mock Handler。
 * Mock 模式下不实际存储文件，返回一个占位 URL。
 */
export const uploadHandlers = [
  http.post('/api/upload/image', ({ request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user || (user.role !== 'admin' && user.role !== 'organizer')) {
      return HttpResponse.json({ code: 403, message: '无权限', data: null }, { status: 403 })
    }
    // Mock: 返回内联占位图（避免外部服务不可用）
    const mockUrl = 'data:image/svg+xml,' + encodeURIComponent(
      '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="200">' +
      '<rect fill="#f0f2f5" width="400" height="200" rx="4"/>' +
      '<text fill="#909399" font-size="16" font-family="sans-serif" x="200" y="105" text-anchor="middle">封面预览</text>' +
      '</svg>'
    )
    return HttpResponse.json({ code: 200, message: 'success', data: { url: mockUrl } })
  }),
]
