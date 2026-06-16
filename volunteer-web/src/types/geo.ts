/**
 * GeoJSON 相关 TypeScript 类型。
 * 与后端 {@code com.cumt.volunteer.geo.model} 的 POJO 结构一一对应，
 * 消除组件中散落的 {@code as any} 类型断言。
 */

/** GeoJSON 坐标：[经度, 纬度] */
export type Position = [number, number]

/** Point 几何 */
export interface PointGeometry {
  type: 'Point'
  coordinates: Position
}

/** 要素属性（活动点） */
export interface ActivityProperties {
  id: number
  title: string
  locationName: string
  startTime: string | null
}

/** GeoJSON Feature（活动点要素） */
export interface ActivityFeature {
  type: 'Feature'
  geometry: PointGeometry
  properties: ActivityProperties
}

/** GeoJSON FeatureCollection */
export interface FeatureCollection {
  type: 'FeatureCollection'
  features: ActivityFeature[]
}
