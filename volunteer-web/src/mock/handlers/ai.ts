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
    await delay(600) // 模拟 AI 思考时间
    const body = await request.json() as Record<string, string>
    const keyword = body.keyword || '志愿活动'
    const seed = Date.now() % 100  // 随机种子，每次生成不同

    // 模拟多种生成结果（每次随机选一个）
    const titles = [
      `"爱心接力"${keyword}志愿行动`,
      `"矿大力量"${keyword}服务活动`,
      `"青春志愿行"——${keyword}专项活动`,
      `"奉献矿大"${keyword}志愿服务`,
      `"暖心校园"${keyword}公益行动`,
    ]
    const title = titles[seed % titles.length]

    const templates = [
      `【活动背景】为弘扬"奉献、友爱、互助、进步"的志愿精神，现面向全校同学招募${keyword}志愿者。\n\n【活动内容】\n1. ${keyword}相关服务工作\n2. 现场秩序维护与引导\n3. 物资整理与发放\n\n【招募要求】吃苦耐劳，有责任心，服从组织安排。\n\n【注意事项】请提前10分钟到达集合点。`,
      `【活动简介】本次${keyword}活动旨在服务校园、锻炼自我。\n\n【工作内容】\n• 协助${keyword}相关事务\n• 引导参与者有序开展活动\n• 活动结束后清理场地\n\n【报名条件】热心公益，时间充裕，有团队合作精神。\n\n【温馨提示】穿着舒适，自备饮用水。`,
      `【活动目的】通过${keyword}志愿服务，培养大学生的社会责任感和实践能力。\n\n【具体任务】\n一、前期准备：物资采购与场地布置\n二、活动期间：签到引导与秩序管理\n三、活动结束：物资回收与场地清理\n\n【招募对象】全体在校学生。\n\n【备注】提供志愿者马甲和饮用水。`,
    ]
    const description = templates[seed % templates.length]

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: { title, description },
    })
  }),

  /** POST /api/ai/generate-cover — AI 生成封面图（精致 SVG） */
  http.post('/api/ai/generate-cover', async ({ request }) => {
    const user = parseMockUser(request.headers.get('Authorization'))
    if (!user || (user.role !== 'admin' && user.role !== 'organizer')) {
      return HttpResponse.json({ code: 403, message: '无权限', data: null }, { status: 403 })
    }
    await delay(300)
    const body = await request.json() as Record<string, string>
    const title = body.prompt || '志愿活动'
    const p = title.length > 8 ? title.slice(0, 8) : title

    const themes = [
      { name: '温暖阳光', bg: '#fff7e6', grad1: '#faad14', grad2: '#ffc53d', icon: '🤝', subtitle: '携手同行·温暖校园' },
      { name: '青春活力', bg: '#f0f5ff', grad1: '#4096ff', grad2: '#69b1ff', icon: '🌟', subtitle: '志愿服务·青春力量' },
      { name: '自然环保', bg: '#f6ffed', grad1: '#52c41a', grad2: '#95de64', icon: '🌿', subtitle: '绿色校园·你我共建' },
    ]

    const covers = themes.map(t => {
      const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="600" height="300" viewBox="0 0 600 300">
<defs><linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="${t.grad1}" stop-opacity="0.12"/><stop offset="100%" stop-color="${t.grad2}" stop-opacity="0.06"/></linearGradient></defs>
<rect width="600" height="300" fill="${t.bg}" rx="14"/>
<rect width="600" height="300" fill="url(#g)" rx="14"/>
<circle cx="480" cy="60" r="140" fill="${t.grad1}" opacity="0.06"/>
<circle cx="120" cy="240" r="100" fill="${t.grad2}" opacity="0.05"/>
<rect x="30" y="30" width="540" height="240" fill="none" stroke="${t.grad1}" stroke-width="1.5" rx="10" opacity="0.2"/>
<text x="300" y="110" text-anchor="middle" font-size="52">${t.icon}</text>
<text x="300" y="165" text-anchor="middle" font-size="24" fill="${t.grad1}" font-weight="bold" font-family="sans-serif">${p}</text>
<text x="300" y="195" text-anchor="middle" font-size="13" fill="#8c8c8c" font-family="sans-serif">${t.subtitle}</text>
<rect x="200" y="222" width="200" height="34" rx="17" fill="${t.grad1}" opacity="0.08"/>
<text x="300" y="244" text-anchor="middle" font-size="12" fill="${t.grad1}" font-family="sans-serif">中国矿业大学 · ${t.name}风</text>
</svg>`
      // btoa 不支持中文，先转义
      const b64 = btoa(unescape(encodeURIComponent(svg)))
      return `data:image/svg+xml;base64,${b64}`
    })

    return HttpResponse.json({ code: 200, message: 'success', data: { covers } })
  }),
]
