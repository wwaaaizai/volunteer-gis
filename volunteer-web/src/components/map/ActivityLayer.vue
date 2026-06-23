<template>
  <!-- 纯逻辑图层组件：无自身 DOM，挂载到父级地图实例上 -->
</template>

<script setup lang="ts">
import { watch, onBeforeUnmount } from 'vue'
import maplibregl from 'maplibre-gl'
import type { Map } from 'maplibre-gl'
import type { FeatureCollection } from '@/types/geo'

const SOURCE_ID = 'activities'
const LAYER_ID = 'activity-markers'

const props = defineProps<{
  map: Map | null
  geojson: FeatureCollection | null
}>()

const emit = defineEmits<{
  (e: 'feature-click', id: number): void
}>()

// 持有事件回调引用，用于 onBeforeUnmount 清理
let layerAdded = false

function onLayerClick(e: maplibregl.MapMouseEvent & { features?: any[] }) {
  const id = e.features?.[0]?.properties?.id
  if (id != null) emit('feature-click', Number(id))
}

/**
 * 将活动 GeoJSON 作为 source + circle 图层叠加到地图。
 * 数据变化时刷新 source；地图首次就绪时若已有数据则立即叠加。
 */
function addOrUpdateLayer(map: Map, data: FeatureCollection) {
  if (map.getSource(SOURCE_ID)) {
    ;(map.getSource(SOURCE_ID) as maplibregl.GeoJSONSource).setData(data as any)
    return
  }

  map.addSource(SOURCE_ID, { type: 'geojson', data: data as any })
  map.addLayer({
    id: LAYER_ID,
    type: 'circle',
    source: SOURCE_ID,
    paint: {
      'circle-radius': 8,
      'circle-color': '#409eff',
      'circle-stroke-width': 2,
      'circle-stroke-color': '#fff',
    },
  })

  map.on('click', LAYER_ID, onLayerClick)
  map.on('mouseenter', LAYER_ID, () => {
    map.getCanvas().style.cursor = 'pointer'
  })
  map.on('mouseleave', LAYER_ID, () => {
    map.getCanvas().style.cursor = ''
  })

  layerAdded = true
}

/** 清理当前 map 上已绑定的图层与事件，避免事件泄漏 */
function cleanupLayer() {
  const m = props.map
  if (!m) return
  if (layerAdded && m.getLayer(LAYER_ID)) {
    m.off('click', LAYER_ID, onLayerClick)
  }
  if (m.getLayer(LAYER_ID)) {
    m.removeLayer(LAYER_ID)
  }
  if (m.getSource(SOURCE_ID)) {
    m.removeSource(SOURCE_ID)
  }
  layerAdded = false
}

// 地图就绪 + 数据到达时叠加
watch(
  () => [props.map, props.geojson] as const,
  ([m, data]) => {
    if (m && data) addOrUpdateLayer(m, data)
  },
  { immediate: true }
)

// 组件卸载前清理 map 上的图层与事件，避免事件泄漏
onBeforeUnmount(() => {
  cleanupLayer()
})
</script>
