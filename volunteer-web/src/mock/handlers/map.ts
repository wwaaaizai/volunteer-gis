import { http, HttpResponse } from 'msw'
import { getDB } from '../db'
import { wgs84ToGcj02 } from '@/utils/coordConvert'
import type { FeatureCollection, ActivityFeature } from '@/types/geo'

/**
 * 地图 GeoJSON Mock Handler。
 *
 * <p>TS 版 GeoJSON 构造逻辑，与后端 {@code GeoJsonBuilder} 对应——
 * 同一份空间语义在前后端各有一份实现，证明 geo 抽象的可移植性。</p>
 *
 * <p>坐标处理：数据库存 WGS-84，输出时转为 GCJ-02 以对齐天地图底图。</p>
 */

export const mapHandlers = [
  /** GET /api/map/activities — 活动 GeoJSON */
  http.get('/api/map/activities', () => {
    const db = getDB()
    const published = db.activities.filter(
      a => a.deleted === 0 && a.status === 'published'
    )

    const features: ActivityFeature[] = published.map(a => {
      const [gcjLng, gcjLat] = wgs84ToGcj02(a.longitude, a.latitude)
      return {
        type: 'Feature' as const,
        geometry: {
          type: 'Point' as const,
          coordinates: [gcjLng, gcjLat] as [number, number],
        },
        properties: {
          id: a.id,
          title: a.title,
          locationName: a.locationName,
          startTime: a.startTime,
        },
      }
    })

    const geojson: FeatureCollection = {
      type: 'FeatureCollection',
      features,
    }

    return HttpResponse.json({ code: 200, message: 'success', data: geojson })
  }),
]
