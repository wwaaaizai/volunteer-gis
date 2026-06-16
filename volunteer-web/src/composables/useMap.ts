import { ref, shallowRef, onUnmounted, type Ref } from 'vue'
import maplibregl from 'maplibre-gl'
import { buildTiandituStyle, DEFAULT_CENTER, DEFAULT_ZOOM } from '@/config/map'

export interface UseMapOptions {
  /** 中心点 [经度, 纬度]，默认南湖校区 */
  center?: [number, number]
  /** 缩放级别 */
  zoom?: number
}

/**
 * 地图实例 Composable。
 *
 * <p>封装 MapLibre 地图的创建与销毁生命周期：
 * <ul>
 *   <li>创建天地图底图实例</li>
 *   <li>用 {@link shallowRef} 持有实例（避免 Vue 深度代理 MapLibre 内部对象）</li>
 *   <li>{@link onUnmounted} 时调用 {@code map.remove()} 释放资源，修复原 Map.vue 的内存泄漏</li>
 * </ul>
 *
 * <p>调用方拿到 {@code mapReady} 与 {@code map} 后，可在此基础上叠加业务图层。
 */
export function useMap(container: Ref<HTMLElement | undefined>, options: UseMapOptions = {}) {
  const map = shallowRef<maplibregl.Map | null>(null)
  const mapReady = ref(false)

  function init() {
    if (!container.value) return
    const instance = new maplibregl.Map({
      container: container.value,
      style: buildTiandituStyle(),
      center: options.center ?? DEFAULT_CENTER,
      zoom: options.zoom ?? DEFAULT_ZOOM,
    })
    instance.on('load', () => {
      mapReady.value = true
    })
    map.value = instance
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
