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

// WGS-84 原始坐标（中国矿业大学南湖校区中心）
const WGS84_CENTER: [number, number] = [117.2050, 34.2173]

/** 中国矿业大学（南湖校区）中心坐标（已转为 GCJ-02） */
export const DEFAULT_CENTER: [number, number] = wgs84ToGcj02(...WGS84_CENTER)

/** 默认缩放级别 */
export const DEFAULT_ZOOM = 14

/** 最小缩放级别（P2-AM-13） */
export const MIN_ZOOM = 13
/** 最大缩放级别（P2-AM-13） */
export const MAX_ZOOM = 19

// ──── 矿大南湖校区边界框（GCJ-02，天地图使用）───
// 由 WGS-84 边界经 wgs84ToGcj02() 转换后取 min/max 得到
// 与后端 SpatialCalculator 中的常量同步

const GCJ02_SW: [number, number] = [117.195, 34.206]  // 西南角
const GCJ02_NE: [number, number] = [117.221, 34.224]  // 东北角

/** 校区边界框（GCJ-02），用于 MapLibre maxBounds 限制拖拽范围 */
export const CAMPUS_BOUNDS_GCJ02: [[number, number], [number, number]] = [
  GCJ02_SW,
  GCJ02_NE,
]

/** 校区中心点（GCJ-02） */
export const CAMPUS_CENTER_GCJ02: [number, number] = [117.208, 34.215]

/** 天地图 API Key（从环境变量读取，占位兜底为空） */
export const TIANDITU_KEY = import.meta.env.VITE_TIANDITU_KEY || ''

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
