/**
 * WGS-84 → GCJ-02 坐标转换工具。
 *
 * 天地图底图使用 GCJ-02（国测局坐标系/火星坐标系），
 * 而数据库和浏览器 GPS 返回 WGS-84，两者在中国境内约有 300-500 米偏移。
 * 此模块将 WGS-84 坐标转换为 GCJ-02，使标注点与天地图底图对齐。
 */

const PI = Math.PI
const A = 6378245.0 // 长半轴
const EE = 0.00669342162296594323 // 扁率

function transformLat(x: number, y: number): number {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
  ret += ((20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(y * PI) + 40.0 * Math.sin((y / 3.0) * PI)) * 2.0) / 3.0
  ret += ((160.0 * Math.sin((y / 12.0) * PI) + 320.0 * Math.sin((y * PI) / 30.0)) * 2.0) / 3.0
  return ret
}

function transformLon(x: number, y: number): number {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
  ret += ((20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(x * PI) + 40.0 * Math.sin((x / 3.0) * PI)) * 2.0) / 3.0
  ret += ((150.0 * Math.sin((x / 12.0) * PI) + 300.0 * Math.sin((x / 30.0) * PI)) * 2.0) / 3.0
  return ret
}

/**
 * 判断是否在中国境内（境外坐标不需要转换）
 */
function outOfChina(lng: number, lat: number): boolean {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
}

/**
 * 单个坐标点 WGS-84 → GCJ-02 转换
 */
export function wgs84ToGcj02(lng: number, lat: number): [number, number] {
  if (outOfChina(lng, lat)) {
    return [lng, lat]
  }
  let dLat = transformLat(lng - 105.0, lat - 35.0)
  let dLon = transformLon(lng - 105.0, lat - 35.0)
  const radLat = (lat / 180.0) * PI
  let magic = Math.sin(radLat)
  magic = 1 - EE * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / (((A * (1 - EE)) / (magic * sqrtMagic)) * PI)
  dLon = (dLon * 180.0) / ((A / sqrtMagic) * Math.cos(radLat) * PI)
  return [lng + dLon, lat + dLat]
}

/**
 * 单个坐标点 GCJ-02 → WGS-84 反向转换（迭代逼近法）
 *
 * 天地图底图为 GCJ-02，地图点击返回的是 GCJ-02 坐标，
 * 数据库存储 WGS-84，需要反向转换。
 */
export function gcj02ToWgs84(lng: number, lat: number): [number, number] {
  if (outOfChina(lng, lat)) {
    return [lng, lat]
  }
  // 迭代逼近：GCJ → 近似 WGS → 再转 GCJ → 修正差值
  let wgsLng = lng
  let wgsLat = lat
  for (let i = 0; i < 5; i++) {
    const [gcjLng, gcjLat] = wgs84ToGcj02(wgsLng, wgsLat)
    wgsLng += lng - gcjLng
    wgsLat += lat - gcjLat
  }
  return [wgsLng, wgsLat]
}

/**
 * 批量将 WGS-84 坐标数组转换为 GCJ-02
 */
export function batchWgs84ToGcj02(
  coords: Array<{ lng: number; lat: number }>
): Array<{ lng: number; lat: number }> {
  return coords.map(({ lng, lat }) => {
    const [newLng, newLat] = wgs84ToGcj02(lng, lat)
    return { lng: newLng, lat: newLat }
  })
}
