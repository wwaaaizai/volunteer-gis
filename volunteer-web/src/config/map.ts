import type { StyleSpecification } from 'maplibre-gl'
import { wgs84ToGcj02 } from '@/utils/coordConvert'

/**
 * 地图集中配置。
 *
 * <p>原先天地图 Key、中心点、zoom 等全部硬编码在 {@code Map.vue}，
 * 现统一收敛至此，便于多页面复用与切换图商。</p>
 *
 * <p><b>配置外置</b>：天地图 Key 通过 Vite 环境变量 {@code VITE_TIANDITU_KEY} 注入，
 * 见 {@code .env / .env.example}。</p>
 *
 * <p><b>坐标说明</b>：数据库存储 WGS-84，地图显示时转为 GCJ-02 对齐天地图底图。</p>
 */

// WGS-84 原始坐标（由 GCJ-02 [117.140,34.215] 反推）
const WGS84_CENTER: [number, number] = [117.134, 34.217]

/** 中国矿业大学（南湖校区）中心坐标（GCJ-02） */
export const DEFAULT_CENTER: [number, number] = [117.140, 34.215]

/** 默认缩放级别 */
export const DEFAULT_ZOOM = 14

/** 最小缩放级别（P2-AM-13） */
export const MIN_ZOOM = 13
/** 最大缩放级别（P2-AM-13） */
export const MAX_ZOOM = 19

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

// ──── GeoServer WMS 图层配置（P2-AM-03）──────────────────

/** GeoServer WMS 服务基础地址（部署后修改为实际地址） */
export const GEOSERVER_WMS_URL = 'http://localhost:8080/geoserver/campus/wms'

/** 校园图层定义（与后端 /api/map/layers 返回数据同步） */
export interface CampusLayerDef {
  id: string
  name: string
  geometryType: 'polygon' | 'line' | 'point'
  description: string
  /** 是否默认显示 */
  defaultVisible: boolean
  /** 默认透明度 0~1 */
  defaultOpacity: number
}

export const CAMPUS_LAYERS: CampusLayerDef[] = [
  {
    id: 'campus:buildings', name: '校园建筑', geometryType: 'polygon',
    description: '建筑物轮廓', defaultVisible: true, defaultOpacity: 0.7,
  },
  {
    id: 'campus:roads', name: '校园道路', geometryType: 'line',
    description: '主干道和支路', defaultVisible: true, defaultOpacity: 0.8,
  },
  {
    id: 'campus:greenland', name: '校园绿地', geometryType: 'polygon',
    description: '绿化带和草坪', defaultVisible: true, defaultOpacity: 0.6,
  },
  {
    id: 'campus:water', name: '校园水系', geometryType: 'polygon',
    description: '镜湖和河流', defaultVisible: true, defaultOpacity: 0.5,
  },
  {
    id: 'campus:poi', name: '校园POI', geometryType: 'point',
    description: '校门/食堂/超市', defaultVisible: false, defaultOpacity: 0.9,
  },
]

/**
 * 构造 GeoServer WMS 栅格瓦片 source 配置（P2-AM-03）。
 *
 * <p>将 GeoServer WMS GetMap 请求封装为 MapLibre raster source，
 * 以 256px 瓦片方式请求，与天地图底图对齐。</p>
 *
 * @param layerName WMS 图层名，如 campus:buildings
 * @param baseUrl   GeoServer WMS 地址
 */
export function buildGeoserverWmsSource(
  layerName: string,
  baseUrl: string = GEOSERVER_WMS_URL,
) {
  return {
    type: 'raster' as const,
    tiles: [
      `${baseUrl}?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap` +
      `&LAYERS=${encodeURIComponent(layerName)}` +
      `&STYLES=&CRS=EPSG:3857` +
      `&BBOX={bbox-epsg-3857}` +
      `&WIDTH=256&HEIGHT=256` +
      `&FORMAT=image/png&TRANSPARENT=true`,
    ],
    tileSize: 256,
    // GeoServer 可能不支持高缩放级别，限制在 13~19
    minzoom: 13,
    maxzoom: 19,
  }
}

/**
 * 构造天地图 MapLibre style（底图 + 注记）。
 * 引入第二个图商时，可在此提供高德/百度等替代 style 工厂。
 */
export function buildTiandituStyle(key: string = TIANDITU_KEY): StyleSpecification {
  return {
    version: 8,
    sources: {
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
    },
    layers: [
      { id: 'tianditu-vec', type: 'raster', source: 'tianditu-vec' },
      { id: 'tianditu-cva', type: 'raster', source: 'tianditu-cva' },
    ],
  }
}
