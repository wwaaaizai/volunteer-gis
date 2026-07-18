/**
 * 矿大南湖校区 POI 库 + 逆地理编码（坐标→地名）。
 *
 * 点击地图时自动匹配最近 POI，无需手动输入地点名。
 */

export interface CampusPoi {
  name: string
  lng: number
  lat: number
  aliases?: string[]  // 别名
}

/** 矿大南湖校区 POI 库（坐标由地图实测校准，精准匹配） */
const POI_LIST: CampusPoi[] = [
  { name: '图书馆', lng: 117.133135, lat: 34.217480, aliases: ['图书馆一楼', '图书馆二楼'] },
  { name: '博学楼一', lng: 117.131064, lat: 34.215807, aliases: ['博一', '博学1'] },
  { name: '博学楼二', lng: 117.132409, lat: 34.216071, aliases: ['博二', '博学2'] },
  { name: '博学楼三', lng: 117.131003, lat: 34.214744, aliases: ['博三', '博学3'] },
  { name: '博学楼四', lng: 117.132060, lat: 34.214098, aliases: ['博四', '博学4'] },
  { name: '博学楼五', lng: 117.133579, lat: 34.214174, aliases: ['博五', '博学5'] },
  { name: '计算机学院', lng: 117.135552, lat: 34.219400 },
  { name: '第一运动场', lng: 117.134128, lat: 34.221646, aliases: ['运动场', '一运'] },
  { name: '第二运动场', lng: 117.129967, lat: 34.220484, aliases: ['二运'] },
  { name: '桃苑篮球场', lng: 117.128501, lat: 34.219042, aliases: ['篮球场'] },
  { name: '桃苑食堂', lng: 117.129586, lat: 34.217812, aliases: ['桃苑'] },
  { name: '1818食堂', lng: 117.127603, lat: 34.215885, aliases: ['1818'] },
  { name: '梅苑食堂', lng: 117.128860, lat: 34.213192, aliases: ['梅苑'] },
  { name: '中国银行', lng: 117.137976, lat: 34.215651 },
  { name: '深地全国重点实验室', lng: 117.139013, lat: 34.216509, aliases: ['深地实验室'] },
  { name: '矿业科学中心', lng: 117.140026, lat: 34.217483, aliases: ['科学中心'] },
  { name: '国旗广场', lng: 117.135666, lat: 34.215881 },
  { name: '信控学院', lng: 117.136797, lat: 34.216759 },
  { name: '经管学院', lng: 117.136090, lat: 34.217633 },
  { name: '机电学院', lng: 117.138164, lat: 34.217578 },
  { name: '材料与物理学院', lng: 117.136844, lat: 34.218452, aliases: ['材物学院'] },
  { name: '力学与土木学院', lng: 117.137905, lat: 34.219115, aliases: ['力土学院'] },
  { name: '化工学院', lng: 117.139060, lat: 34.218702 },
  { name: '环测学院', lng: 117.139572, lat: 34.219556 },
  { name: '资源学院', lng: 117.139266, lat: 34.220665 },
  { name: '矿业学院', lng: 117.138676, lat: 34.220178 },
  { name: '中国煤炭科技博物馆', lng: 117.138177, lat: 34.220656, aliases: ['博物馆'] },
  { name: '建筑学院', lng: 117.136716, lat: 34.221044 },
  { name: '公共管理学院', lng: 117.136201, lat: 34.220280, aliases: ['公管学院'] },
  { name: '网球场', lng: 117.133236, lat: 34.220188 },
  { name: '桃苑宿舍', lng: 117.131196, lat: 34.218855, aliases: ['桃苑'] },
  { name: '体育学院', lng: 117.131188, lat: 34.221518 },
  { name: '体育馆', lng: 117.131897, lat: 34.222299 },
  { name: '松苑宿舍', lng: 117.128592, lat: 34.216776, aliases: ['松苑'] },
  { name: '杏苑宿舍', lng: 117.126513, lat: 34.216429, aliases: ['杏苑'] },
  { name: '校医院', lng: 117.125914, lat: 34.215135 },
  { name: '竹苑宿舍', lng: 117.127822, lat: 34.214593, aliases: ['竹苑'] },
  { name: '梅苑宿舍', lng: 117.127733, lat: 34.212627, aliases: ['梅苑'] },
  { name: '兰苑宿舍', lng: 117.128556, lat: 34.211658, aliases: ['兰苑'] },
  { name: '第三运动场', lng: 117.125861, lat: 34.211774, aliases: ['三运'] },
  { name: '大学生活动中心', lng: 117.132500, lat: 34.220000, aliases: ['活动中心'] },
  { name: '镜湖大讲堂', lng: 117.131093, lat: 34.213770, aliases: ['镜湖'] },
  { name: '南湖校门', lng: 117.128000, lat: 34.217000, aliases: ['校门口', '南门'] },
]

/**
 * 计算两点间 Haversine 距离（米）
 */
function haversineMeters(lng1: number, lat1: number, lng2: number, lat2: number): number {
  const R = 6371000
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLng = (lng2 - lng1) * Math.PI / 180
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

/** 匹配阈值（米），超过此距离不匹配 */
/** 匹配阈值（米）——宿舍/食堂密集区调小避免误匹配 */
const MATCH_THRESHOLD = 150

/** POI 匹配结果 */
export interface PoiMatch {
  name: string
  distance: number // 米
}

/**
 * 根据坐标查找最近的 POI。
 * @param lng WGS-84 经度
 * @param lat WGS-84 纬度
 * @returns 最近 POI 名称，未匹配返回 null
 */
export function findNearestPoi(lng: number, lat: number): PoiMatch | null {
  let best: PoiMatch | null = null
  let bestDist = Infinity

  for (const poi of POI_LIST) {
    const dist = haversineMeters(lng, lat, poi.lng, poi.lat)
    if (dist < bestDist && dist <= MATCH_THRESHOLD) {
      bestDist = dist
      best = { name: poi.name, distance: Math.round(dist) }
    }
  }
  return best
}

/**
 * 根据关键词搜索 POI。
 */
export function searchPoi(keyword: string): CampusPoi[] {
  const kw = keyword.toLowerCase()
  return POI_LIST.filter(p =>
    p.name.includes(kw) || p.aliases?.some(a => a.includes(kw))
  )
}
