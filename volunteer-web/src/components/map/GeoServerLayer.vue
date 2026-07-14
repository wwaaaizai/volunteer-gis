<template>
  <!-- WMS 栅格图层组件：以 raster tile layer 形式叠加 GeoServer WMS 图层。
       注意：WMS 使用 WGS-84，叠加到天地图 GCJ-02 底图上会有约 300-500m 偏移。
       推荐优先使用 WfsLayer 矢量方式实现零偏移叠加。 -->
</template>

<script setup lang="ts">
import { watch, onUnmounted } from 'vue'
import type { Map } from 'maplibre-gl'
import { buildWmsUrl } from '@/config/map'

const props = withDefaults(
  defineProps<{
    map: Map | null
    /** GeoServer 图层完整名称（工作区:图层名） */
    layerName: string
    /** 图层显示标题（用于 source/layer id 生成） */
    sourceId: string
    visible?: boolean
    opacity?: number
  }>(),
  {
    visible: true,
    opacity: 0.5,
  }
)

function addLayer() {
  const m = props.map
  if (!m || !props.visible) return

  removeLayer()

  m.addSource(props.sourceId, {
    type: 'raster',
    tiles: [buildWmsUrl(props.layerName)],
    tileSize: 256,
  })

  m.addLayer({
    id: props.sourceId,
    type: 'raster',
    source: props.sourceId,
    paint: { 'raster-opacity': props.opacity },
  })
}

function removeLayer() {
  const m = props.map
  if (!m) return
  try {
    if (m.getLayer(props.sourceId)) m.removeLayer(props.sourceId)
  } catch { /* */ }
  try {
    if (m.getSource(props.sourceId)) m.removeSource(props.sourceId)
  } catch { /* */ }
}

// 地图就绪时叠加
watch(() => props.map, (m) => { if (m) addLayer() })

// 可见性/透明度变化时更新
watch(
  () => [props.visible, props.opacity] as const,
  ([v, o]) => {
    if (v) {
      addLayer()
    } else {
      removeLayer()
    }
    // 更新透明度（不重建图层）
    const m = props.map
    if (m && v && m.getLayer(props.sourceId)) {
      m.setPaintProperty(props.sourceId, 'raster-opacity', o)
    }
  }
)

// 组件卸载时清理
onUnmounted(removeLayer)
</script>
