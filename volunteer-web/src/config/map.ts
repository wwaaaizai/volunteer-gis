import type { StyleSpecification } from 'maplibre-gl'

/**
 * 地图集中配置。
 *
 * <p>原先天地图 Key、中心点、zoom 等全部硬编码在 {@code Map.vue}，
 * 现统一收敛至此，便于多页面复用与切换图商。</p>
 *
 * <p><b>配置外置</b>：天地图 Key 通过 Vite 环境变量 {@code VITE_TIANDITU_KEY} 注入，
 * 见 {@code .env / .env.example}。</p>
 */

/** 中国矿业大学（南湖校区）中心坐标 [经度, 纬度] */
export const DEFAULT_CENTER: [number, number] = [117.2050, 34.2173]

/** 默认缩放级别 */
export const DEFAULT_ZOOM = 15

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
