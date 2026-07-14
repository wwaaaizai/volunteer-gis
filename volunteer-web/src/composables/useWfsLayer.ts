/**
 * WFS 矢量图层 Composable（P2-AM-08 前端实现）。
 *
 * <p>拉取 GeoServer WFS GetFeature 返回的 GeoJSON 矢量数据，
 * 对其所有坐标递归执行 WGS-84 → GCJ-02 变换，
 * 并叠加可选的源数据平移矫正（degrees），使数据与天地图 GCJ-02 底图零偏移对齐。</p>
 *
 * <p>设计决策：前端变换而非修改源数据 —
 * 源数据保持 WGS-84 真值，前端运行时做坐标系适配 + 源数据平移矫正，
 * 将来切换底图到 WGS-84 原生（如 OSM）时无需重复处理。</p>
 */

import { ref, type Ref } from 'vue'
import { wgs84ToGcj02 } from '@/utils/coordConvert'
import { buildWfsUrl } from '@/config/map'

/** WFS 加载状态 */
export interface WfsLayerState {
  loading: Ref<boolean>
  error: Ref<string | null>
  geojson: Ref<Record<string, unknown> | null>
  load: (layerName: string, translate?: [number, number]) => Promise<void>
}

/**
 * WGS-84 → GCJ-02 变换 + 可选源数据平移矫正（度数）。
 *
 * 矫正量含义：正值向东/北，负值向西/南。
 * 例如 translate=[-0.00553, 0.00122] 表示向西移 0.00553°、向北移 0.00122°。
 */
function transform(lng: number, lat: number, translate?: [number, number]): [number, number] {
  const [gl, gt] = wgs84ToGcj02(lng, lat)
  if (!translate) return [gl, gt]
  return [gl + translate[0], gt + translate[1]]
}

/**
 * 递归遍历 GeoJSON 对象，对所有 [lng, lat] 坐标执行 WGS-84 → GCJ-02 变换，
 * 并叠加可选源数据平移矫正。
 *
 * 支持 Point、MultiPoint、LineString、MultiLineString、Polygon、MultiPolygon
 * 以及 GeometryCollection 和 Feature/FeatureCollection 的嵌套结构。
 */
function transformCoordsInPlace(obj: unknown, translate?: [number, number]): void {
  if (!obj || typeof obj !== 'object') return

  const o = obj as Record<string, unknown>

  // FeatureCollection / Feature 递归
  if (o.type === 'FeatureCollection' && Array.isArray(o.features)) {
    for (const feature of o.features) transformCoordsInPlace(feature, translate)
    return
  }
  if (o.type === 'Feature' && o.geometry) {
    transformCoordsInPlace(o.geometry, translate)
    return
  }
  if (o.type === 'GeometryCollection' && Array.isArray(o.geometries)) {
    for (const g of o.geometries as unknown[]) transformCoordsInPlace(g, translate)
    return
  }

  // 几何类型坐标变换
  if (!Array.isArray(o.coordinates)) return

  switch (o.type) {
    case 'Point': {
      const [lng, lat] = o.coordinates as [number, number]
      o.coordinates = transform(lng, lat, translate)
      break
    }
    case 'MultiPoint':
    case 'LineString': {
      o.coordinates = (o.coordinates as number[][]).map(
        ([lng, lat]) => transform(lng, lat, translate)
      )
      break
    }
    case 'MultiLineString':
    case 'Polygon': {
      o.coordinates = (o.coordinates as number[][][]).map((ring) =>
        ring.map(([lng, lat]) => transform(lng, lat, translate))
      )
      break
    }
    case 'MultiPolygon': {
      o.coordinates = (o.coordinates as number[][][][]).map((polygon) =>
        polygon.map((ring) =>
          ring.map(([lng, lat]) => transform(lng, lat, translate))
        )
      )
      break
    }
    default:
      // 未知几何类型跳过
      break
  }
}

/**
 * WFS 矢量图层加载 composable。
 *
 * 用法：
 * ```ts
 * const { loading, error, geojson, load } = useWfsLayer()
 * await load('ol_campus:jianzhu')
 * // geojson.value 已包含 GCJ-02 变换后的数据
 * ```
 */
export function useWfsLayer(): WfsLayerState {
  const loading = ref(false)
  const error = ref<string | null>(null)
  const geojson = ref<Record<string, unknown> | null>(null)

  async function load(layerName: string, translate?: [number, number]) {
    loading.value = true
    error.value = null
    try {
      const url = buildWfsUrl(layerName)
      const res = await fetch(url)
      if (!res.ok) {
        throw new Error(`GeoServer 返回 HTTP ${res.status}: ${res.statusText}`)
      }
      const data = await res.json()
      // 核心：WGS-84 → GCJ-02 坐标变换 + 源数据平移矫正
      transformCoordsInPlace(data, translate)
      geojson.value = data
    } catch (err) {
      const message = err instanceof Error ? err.message : 'WFS 数据加载失败'
      error.value = message
      console.error(`[useWfsLayer] ${layerName} 加载失败:`, err)
    } finally {
      loading.value = false
    }
  }

  return { loading, error, geojson, load }
}
