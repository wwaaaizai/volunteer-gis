<template>
  <!-- 纯逻辑图层组件：无自身 DOM，挂载 WMS 栅格图层到父级地图实例 -->
</template>

<script setup lang="ts">
import { watch, onUnmounted } from 'vue'
import type { Map } from 'maplibre-gl'
import { buildGeoserverWmsSource, type CampusLayerDef } from '@/config/map'

const props = defineProps<{
  map: Map | null
  layer: CampusLayerDef
  visible: boolean
  opacity: number
}>()

const SOURCE_PREFIX = 'geoserver-'

function sourceId() { return SOURCE_PREFIX + props.layer.id }
function layerId() { return sourceId() + '-raster' }

/** 添加 GeoServer WMS 栅格图层到地图 */
function addLayer(map: Map) {
  const sid = sourceId()
  const lid = layerId()

  // 如果已存在则跳过（由 watch 处理更新）
  if (map.getSource(sid)) return

  try {
    map.addSource(sid, buildGeoserverWmsSource(props.layer.id) as any)
    map.addLayer({
      id: lid,
      type: 'raster',
      source: sid,
      layout: { visibility: props.visible ? 'visible' : 'none' },
      paint: {
        'raster-opacity': props.opacity,
        // 瓦片加载失败时淡化处理
        'raster-fade-duration': 300,
      },
      // 插入到天地图注记层之上、活动标注层之下
    })
  } catch (e) {
    // GeoServer 不可用时静默失败，不影响地图基础功能
    console.warn(`GeoServer 图层 "${props.layer.name}" 加载失败:`, e)
  }
}

/** 移除图层（组件卸载时清理） */
function removeLayer(map: Map) {
  const lid = layerId()
  const sid = sourceId()
  try {
    if (map.getLayer(lid)) map.removeLayer(lid)
    if (map.getSource(sid)) map.removeSource(sid)
  } catch { /* 忽略 */ }
}

/** 更新图层可见性 */
function setVisibility(map: Map, visible: boolean) {
  const lid = layerId()
  if (map.getLayer(lid)) {
    map.setLayoutProperty(lid, 'visibility', visible ? 'visible' : 'none')
  }
}

/** 更新图层透明度 */
function setOpacity(map: Map, opacity: number) {
  const lid = layerId()
  if (map.getLayer(lid)) {
    map.setPaintProperty(lid, 'raster-opacity', opacity)
  }
}

// 地图就绪时添加图层
watch(
  () => props.map,
  (m) => {
    if (m) addLayer(m)
  },
  { immediate: true },
)

// 可见性变化
watch(() => props.visible, (v) => {
  if (props.map) setVisibility(props.map, v)
})

// 透明度变化
watch(() => props.opacity, (o) => {
  if (props.map) setOpacity(props.map, o)
})

// 清理
onUnmounted(() => {
  if (props.map) removeLayer(props.map)
})
</script>
