import { ref, shallowRef, onUnmounted, type Ref } from 'vue'
import maplibregl from 'maplibre-gl'
import {
  buildTiandituStyle,
  DEFAULT_CENTER,
  DEFAULT_ZOOM,
  MIN_ZOOM,
  MAX_ZOOM,
  SATELLITE_MAX_ZOOM,
  CAMPUS_BOUNDS_GCJ02,
  STANDARD_LAYER_IDS,
  SATELLITE_LAYER_IDS,
  type BaseMapMode,
} from '@/config/map'

export interface UseMapOptions {
  /** 中心点 [经度, 纬度]，默认南湖校区 */
  center?: [number, number]
  /** 缩放级别 */
  zoom?: number
}

/**
 * 地图实例 Composable（P2-AM-12/13：限制拖拽范围 + 缩放级别）。
 *
 * <p>封装 MapLibre 地图的创建与销毁生命周期：
 * <ul>
 *   <li>创建天地图底图实例</li>
 *   <li>用 {@link shallowRef} 持有实例（避免 Vue 深度代理 MapLibre 内部对象）</li>
 *   <li>{@link onUnmounted} 时调用 {@code map.remove()} 释放资源</li>
 *   <li>maxBounds 限制地图不可拖出矿大南湖校区</li>
 *   <li>minZoom/maxZoom 限制缩放级别 13~19</li>
 * </ul>
 */
export function useMap(container: Ref<HTMLElement | undefined>, options: UseMapOptions = {}) {
  const map = shallowRef<maplibregl.Map | null>(null)
  const mapReady = ref(false)

  function init() {
    if (!container.value) return
    try {
      const instance = new maplibregl.Map({
        container: container.value,
        style: buildTiandituStyle(),
        center: options.center ?? DEFAULT_CENTER,
        zoom: options.zoom ?? DEFAULT_ZOOM,
        // P2-AM-12：限制地图拖拽范围在矿大南湖校区内
        maxBounds: CAMPUS_BOUNDS_GCJ02,
        // P2-AM-13：限制缩放级别
        minZoom: MIN_ZOOM,
        maxZoom: MAX_ZOOM,
      })
      // 添加缩放控件和定位按钮
      instance.addControl(new maplibregl.NavigationControl(), 'top-right')
      instance.addControl(new maplibregl.GeolocateControl({
        positionOptions: { enableHighAccuracy: true },
        trackUserLocation: true,
        showUserHeading: true,
      }), 'top-right')
      instance.on('load', () => {
        mapReady.value = true
        // 加载完成后飞向矿大南湖校区
        instance.flyTo({
          center: options.center ?? DEFAULT_CENTER,
          zoom: options.zoom ?? DEFAULT_ZOOM,
          duration: 0,
        })
      })
      // 瓦片加载失败不崩溃
      instance.on('error', (e) => {
        console.warn('地图瓦片加载失败（可能需要有效的天地图 Key）:', e.error?.message ?? e)
      })
      map.value = instance
    } catch (e) {
      console.error('地图初始化失败，请检查天地图 Key 是否有效')
      console.error(e)
    }
  }

  const currentBaseMap = ref<BaseMapMode>('standard')

  /** 底图标准/卫星切换（不销毁矢量图层） */
  function switchBaseMap(mode: BaseMapMode) {
    const m = map.value
    if (!m || mode === currentBaseMap.value) return

    const style = m.getStyle()
    if (!style) return

    // 找到第一个非底图图层，用于将瓦片插入正确位置
    const allBasemapIds = [...STANDARD_LAYER_IDS, ...SATELLITE_LAYER_IDS]
    const firstOther = style.layers.find((l) => !allBasemapIds.includes(l.id))

    if (mode === 'satellite') {
      // 移除标准底图
      for (const id of STANDARD_LAYER_IDS) {
        try { if (m.getLayer(id)) m.removeLayer(id) } catch { /* */ }
      }
      // 添加卫星底图（插入到矢量图层之前）
      m.addLayer({ id: 'tianditu-img', type: 'raster', source: 'tianditu-img' }, firstOther?.id)
      m.addLayer({ id: 'tianditu-cia', type: 'raster', source: 'tianditu-cia' }, firstOther?.id)
      // 卫星影像在校园级通常仅到 18 级，降低最大缩放
      m.setMaxZoom(SATELLITE_MAX_ZOOM)
      if (m.getZoom() > SATELLITE_MAX_ZOOM) {
        m.zoomTo(SATELLITE_MAX_ZOOM)
      }
    } else {
      // 移除卫星底图
      for (const id of SATELLITE_LAYER_IDS) {
        try { if (m.getLayer(id)) m.removeLayer(id) } catch { /* */ }
      }
      // 恢复标准底图
      m.addLayer({ id: 'tianditu-vec', type: 'raster', source: 'tianditu-vec' }, firstOther?.id)
      m.addLayer({ id: 'tianditu-cva', type: 'raster', source: 'tianditu-cva' }, firstOther?.id)
      // 恢复标准最大缩放
      m.setMaxZoom(MAX_ZOOM)
    }

    currentBaseMap.value = mode
  }

  function destroy() {
    if (map.value) {
      map.value.remove()
      map.value = null
      mapReady.value = false
    }
  }

  onUnmounted(destroy)

  return { map, mapReady, currentBaseMap, init, destroy, switchBaseMap }
}
