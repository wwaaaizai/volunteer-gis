import { ref, shallowRef, onUnmounted, type Ref } from 'vue'
import maplibregl from 'maplibre-gl'
import {
  buildTiandituStyle,
  DEFAULT_CENTER,
  DEFAULT_ZOOM,
  MIN_ZOOM,
  MAX_ZOOM,
  CAMPUS_BOUNDS_GCJ02,
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

  function destroy() {
    if (map.value) {
      map.value.remove()
      map.value = null
      mapReady.value = false
    }
  }

  onUnmounted(destroy)

  return { map, mapReady, init, destroy }
}
