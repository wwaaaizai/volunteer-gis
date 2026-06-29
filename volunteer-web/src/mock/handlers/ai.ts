import { http, HttpResponse, delay } from 'msw'
import { parseMockUser } from './auth'

/**
 * AI 功能 Mock Handler（描述生成 + 封面生成）。
 */
export const aiHandlers = [
  /** POST /api/ai/generate-description — AI 生成活动描述 */
  http.post('/api/ai/generate-description', async ({ request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user || (user.role !== 'admin' && user.role !== 'organizer')) {
      return HttpResponse.json({ code: 403, message: '无权限', data: null }, { status: 403 })
    }
    await delay(800) // 模拟 AI 思考时间
    const body = await request.json() as Record<string, string>
    const keyword = body.keyword || '志愿活动'

    // 模拟生成结果
    const title = keyword.includes('图书馆')
      ? `"书香矿大"图书馆志愿整理活动`
      : keyword.includes('迎新')
        ? `"新起点"迎新志愿服务行动`
        : keyword.includes('雷锋')
          ? `"雷锋月"校园公益志愿活动`
          : `"爱心传递"${keyword}志愿服务活动`

    const description = `【活动背景】为弘扬志愿服务精神，丰富校园文化生活，特举办本次"${title.replace(/"/g, '')}"活动。\n\n【活动内容】${keyword}相关志愿服务工作，包括现场引导、物资整理、秩序维护等。\n\n【招募要求】吃苦耐劳，服从安排，有志愿服务经验者优先。\n\n【注意事项】请提前10分钟到达集合点，穿着志愿者马甲，听从负责人统一指挥。`

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: { title, description },
    })
  }),

  /** POST /api/ai/generate-cover — AI 生成封面图（调用 Pollinations.ai，免费无需 Key） */
  http.post('/api/ai/generate-cover', async ({ request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user || (user.role !== 'admin' && user.role !== 'organizer')) {
      return HttpResponse.json({ code: 403, message: '无权限', data: null }, { status: 403 })
    }
    await delay(500)

    const body = await request.json() as Record<string, string>
    const prompt = body.prompt || '志愿活动'

    // 三种不同风格的英文 prompt 生成不同候选
    const styles = [
      `A warm and inspiring poster for university volunteer activity: ${prompt}, campus background, bright sunshine, green trees, Chinese university campus, clean modern design, text-free`,
      `A professional charity event banner for: ${prompt}, students helping community, warm tones, teamwork spirit, minimalist style, no text`,
      `A creative illustration for campus volunteer program: ${prompt}, young students volunteering, colorful flat design, positive energy, no text or logo`,
    ]

    const covers = styles.map(s =>
      `https://image.pollinations.ai/prompt/${encodeURIComponent(s)}?width=600&height=300&nologo=true`
    )

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: { covers },
    })
  }),
]
