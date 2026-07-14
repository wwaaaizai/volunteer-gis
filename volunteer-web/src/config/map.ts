import type { StyleSpecification } from 'maplibre-gl'
import { wgs84ToGcj02 } from '@/utils/coordConvert'

/**
 * 地图集中配置。
 *
 * <p>原先天地图 Key、中心点、zoom 等全部硬编码在 {@code Map.vue}，
 * 现统一收敛至此，便于多页面复用与切换图商。</p>
 *
 * <p><b>配置外置</b>：天地图 Key 通过 Vite 环境变量 {@code VITE_TIANDITU_KEY} 注入。</p>
 *
 * <p><b>坐标说明</b>：数据库存储 WGS-84，地图显示 GCJ-02 对齐天地图底图。</p>
 *
 * <p><b>P2-AM GeoServer 集成</b>：
 * GeoServer WFS 矢量数据可通过前端 {@code wgs84ToGcj02()} 变换坐标后叠加；
 * GeoServer WMS 栅格图层也可通过本配置的工厂函数加载（需注意 GCJ-02 偏移）。
 * 所有 GeoServer 请求通过 Vite 代理 {@code /geoserver} 转发，避免 CORS 问题。</p>
 */

// WGS-84 原始坐标（由 GCJ-02 [117.140,34.215] 反推）
const WGS84_CENTER: [number, number] = [117.134, 34.217]

/** 中国矿业大学（南湖校区）中心坐标（GCJ-02） */
export const DEFAULT_CENTER: [number, number] = [117.140, 34.215]

/** 默认缩放级别 */
export const DEFAULT_ZOOM = 15

/** 最小缩放级别（P2-AM-13） */
export const MIN_ZOOM = 14
/** 最大缩放级别（P2-AM-13） */
export const MAX_ZOOM = 17
/** 卫星底图最大缩放级别（天地图 img_w 校园级通常仅到 17 级） */
export const SATELLITE_MAX_ZOOM = 17

// ──── 矿大南湖校区边界框（GCJ-02，天地图使用）───
// 以 [117.140, 34.215] 为中心，各方向外扩约 2km
// 与后端 SpatialCalculator 常量同步

const GCJ02_SW: [number, number] = [117.118, 34.197]  // 西南角
const GCJ02_NE: [number, number] = [117.162, 34.233]  // 东北角

/** 校区边界框（GCJ-02），用于 MapLibre maxBounds 限制拖拽范围 */
export const CAMPUS_BOUNDS_GCJ02: [[number, number], [number, number]] = [
  GCJ02_SW,
  GCJ02_NE,
]

/** 校区中心点（GCJ-02） */
export const CAMPUS_CENTER_GCJ02: [number, number] = [117.140, 34.215]

/** 天地图 API Key（从环境变量读取，占位兜底为空） */
export const TIANDITU_KEY = import.meta.env.VITE_TIANDITU_KEY || ''

/** 底图模式 */
export type BaseMapMode = 'standard' | 'satellite'

/** 标准底图图层 ID */
export const STANDARD_LAYER_IDS = ['tianditu-vec', 'tianditu-cva']
/** 卫星底图图层 ID */
export const SATELLITE_LAYER_IDS = ['tianditu-img', 'tianditu-cia']

/**
 * 构造天地图 MapLibre style（含标准 + 卫星两套源，初始激活标准底图）。
 */
export function buildTiandituStyle(key: string = TIANDITU_KEY): StyleSpecification {
  return {
    version: 8,
    sources: {
      // 标准底图
      'tianditu-vec': {
        type: 'raster',
        tiles: [
          `https://t0.tianditu.gov.cn/vec_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=vec&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${key}`,
        ],
        tileSize: 256,
      },
      'tianditu-cva': {
        type: 'raster',
        tiles: [
          `https://t0.tianditu.gov.cn/cva_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cva&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${key}`,
        ],
        tileSize: 256,
      },
      // 卫星影像 + 注记
      'tianditu-img': {
        type: 'raster',
        tiles: [
          `https://t0.tianditu.gov.cn/img_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=img&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${key}`,
        ],
        tileSize: 256,
      },
      'tianditu-cia': {
        type: 'raster',
        tiles: [
          `https://t0.tianditu.gov.cn/cia_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cia&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${key}`,
        ],
        tileSize: 256,
      },
    },
    layers: [
      { id: 'tianditu-vec', type: 'raster', source: 'tianditu-vec' },
      { id: 'tianditu-cva', type: 'raster', source: 'tianditu-cva' },
    ],
  }
}

// ──── GeoServer 配置（P2-AM）─────────────────────────

/**
 * GeoServer 服务路径（通过 Vite 代理转发到 http://localhost:9091/geoserver）。
 * 前端所有 GeoServer 请求使用此路径，避免浏览器 CORS 限制。
 */
export const GEOSERVER_BASE = '/geoserver'

/** GeoServer 工作区名称 */
export const GEOSERVER_WORKSPACE = 'ol_campus'

/**
 * 可用图层清单（与 GeoServer 实际发布的图层对应）。
 *
 * 当前已发布图层：
 * - ol_campus:jianzhu   校园建筑物轮廓面
 * - ol_campus:bianjie   校区边界（仅边框无填充）
 * - ol_campus:yundongchang 校园运动场
 */
export interface GeoServerLayerMeta {
  /** 图层完整名称（工作区:图层名） */
  name: string
  /** 中文标题 */
  title: string
  /** 几何类型 */
  geometryType: 'polygon' | 'line' | 'point'
  /** 默认可见 */
  visible: boolean
  /** 填充颜色（polygon 类型默认色） */
  color?: string
  /** 轮廓颜色 */
  strokeColor?: string
  /** 是否填充（默认 true，设为 false 则仅显示边框） */
  fill?: boolean
  /**
   * 源数据平移矫正 [dlng, dlat]（度数），于 WGS-84 → GCJ-02 变换后叠加。
   * 当源数据本身存在整体平移（如测量误差）时使用。
   * 正值表示向东/北，负值表示向西/南。
   */
  translate?: [number, number]
}

export const GEOSERVER_LAYERS: GeoServerLayerMeta[] = [
  {
    name: 'ol_campus:jianzhu',
    title: '校园建筑',
    geometryType: 'polygon',
    visible: true,
    color: '#f59e0b',
    strokeColor: '#d97706',
    translate: [-0.00580, 0.00112],
  },
  {
    name: 'ol_campus:bianjie',
    title: '校区边界',
    geometryType: 'polygon',
    visible: true,
    strokeColor: '#1d4ed8',
    fill: false,
    translate: [-0.00563, 0.00110],
  },

 {
    name: 'ol_campus:yundongchang',
    title: '校园运动场',
    geometryType: 'polygon',
    visible: true,
    color: '#ef6306',
    strokeColor: '#ef6306',
    translate: [-0.00580, 0.00112],
  },
  	
]

/**
 * GeoServer WMS GetMap 请求基础 URL 模板。
 * 实现为函数以支持不同图层的灵活调用。
 */
export function buildWmsUrl(layerName: string): string {
  return [
    `${GEOSERVER_BASE}/${GEOSERVER_WORKSPACE}/wms?`,
    'service=WMS&version=1.1.0&request=GetMap',
    `&layers=${encodeURIComponent(layerName)}`,
    '&srs=EPSG:4326',
    '&bbox={bbox-epsg-4326}',
    '&width=256&height=256',
    '&format=image/png',
    '&transparent=true',
    '&styles=',
  ].join('')
}

/**
 * 构造 GeoServer WMS raster 图层源配置。
 *
 * <p><b>注意 GCJ-02 偏移</b>：GeoServer 返回的 WMS 栅格图像使用 WGS-84 坐标系，
 * 叠加到天地图 GCJ-02 底图上会有约 300-500 米偏移。
 * 如需要零偏移叠加，优先使用 WFS 矢量方式（{@code useWfsLayer}）。</p>
 */
export function buildWmsSource(layerName: string) {
  return {
    type: 'raster' as const,
    tiles: [buildWmsUrl(layerName)],
    tileSize: 256,
  }
}

/**
 * GeoServer WFS GetFeature 请求 URL 构造。
 * 请求 SRS=EPSG:4326 让 GeoServer 即时重投影为经纬度，
 * 前端拿到 WGS-84 坐标后再用 {@code wgs84ToGcj02()} 变换对齐天地图。
 */
export function buildWfsUrl(layerName: string): string {
  return [
    `${GEOSERVER_BASE}/${GEOSERVER_WORKSPACE}/ows?`,
    'service=WFS&version=1.0.0&request=GetFeature',
    `&typeName=${encodeURIComponent(layerName)}`,
    '&outputFormat=application/json',
    '&srsName=EPSG:4326',
  ].join('')
}

/**
 * 向已有 style 追加 GeoServer WMS 图层（用于地图初始化时一次性配置）。
 */
export function addWmsLayer(
  style: StyleSpecification,
  sourceId: string,
  layerName: string,
  opacity = 0.5,
): StyleSpecification {
  return {
    ...style,
    sources: {
      ...style.sources,
      [sourceId]: buildWmsSource(layerName),
    },
    layers: [
      ...style.layers,
      {
        id: sourceId,
        type: 'raster',
        source: sourceId,
        paint: { 'raster-opacity': opacity },
      },
    ],
  }
}
