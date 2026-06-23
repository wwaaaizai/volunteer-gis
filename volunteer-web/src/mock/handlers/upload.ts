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
    // Mock: 返回占位图片 URL
    const mockUrl = 'https://via.placeholder.com/400x200.png?text=Cover'
    return HttpResponse.json({ code: 200, message: 'success', data: { url: mockUrl } })
  }),
]
