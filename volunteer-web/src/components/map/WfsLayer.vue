<template>
  <!-- 纯逻辑图层组件：通过 useWfsLayer 拉取 GeoServer WFS 数据，变换坐标后作为 GeoJSON source 叠加到地图 -->
</template>

<script setup lang="ts">
import { watch, onUnmounted } from 'vue'
import type { Map } from 'maplibre-gl'
import { useWfsLayer } from '@/composables/useWfsLayer'
import type { GeoServerLayerMeta } from '@/config/map'

const props = withDefaults(
  defineProps<{
    /** MapLibre 地图实例 */
    map: Map | null
    /** 图层元数据 */
    layer: GeoServerLayerMeta
    /** 是否可见 */
    visible?: boolean
    /** 填充透明度 */
    fillOpacity?: number
    /**
     * 不可见时不移除图层，仅将透明度降为 0。
     * 用于需要 queryRenderedFeatures 命中"隐藏"图层的场景（如建筑点击高亮）。
     */
    keepQueryable?: boolean
  }>(),
  {
    visible: true,
    fillOpacity: 0.45,
    keepQueryable: false,
  }
)

const emit = defineEmits<{
  (e: 'layer-ready', layerName: string): void
  (e: 'layer-error', layerName: string, message: string): void
  (e: 'feature-click', layerName: string, properties: Record<string, unknown>, geometry?: unknown): void
}>()

const { loading, error, geojson, load } = useWfsLayer()

/** source/layer 的 MapLibre ID */
function sourceId(): string {
  return `wfs-${props.layer.name.replace(':', '-')}`
}

function fillLayerId(): string {
  return `${sourceId()}-fill`
}

function lineLayerId(): string {
  return `${sourceId()}-line`
}

/** 清理地图上的旧图层 */
function removeLayer() {
  const m = props.map
  if (!m) return
  try {
    if (m.getLayer(fillLayerId())) m.removeLayer(fillLayerId())
  } catch { /* */ }
  try {
    if (m.getLayer(lineLayerId())) m.removeLayer(lineLayerId())
  } catch { /* */ }
  try {
    if (m.getSource(sourceId())) m.removeSource(sourceId())
  } catch { /* */ }
}

/** 将 GeoJSON 数据叠加到地图 */
function addLayer() {
  const m = props.map
  if (!m || !geojson.value) return

  removeLayer()

  // 非 keepQueryable 模式：不可见时跳过
  if (!props.keepQueryable && !props.visible) return

  m.addSource(sourceId(), { type: 'geojson', data: geojson.value as any })

  // polygon 类型：填充（可选） + 轮廓
  if (props.layer.geometryType === 'polygon' || !props.layer.geometryType) {
    const needFill = props.layer.fill !== false
    if (needFill) {
      m.addLayer({
        id: fillLayerId(),
        type: 'fill',
        source: sourceId(),
        paint: {
          'fill-color': props.layer.color ?? '#f59e0b',
          'fill-opacity': props.fillOpacity,
        },
      })
    }
    m.addLayer({
      id: lineLayerId(),
      type: 'line',
      source: sourceId(),
      paint: {
        'line-color': props.layer.strokeColor ?? '#d97706',
        'line-width': 2,
      },
    })

    // click 查询（P2-AM-08：仅当有填充层时绑定 click 事件）
    if (needFill) {
      m.on('click', fillLayerId(), (e) => {
        if (e.features?.[0]?.properties) {
          emit('feature-click', props.layer.name, e.features[0].properties as Record<string, unknown>, e.features[0].geometry)
        }
      })
      m.on('mouseenter', fillLayerId(), () => {
        m.getCanvas().style.cursor = 'pointer'
      })
      m.on('mouseleave', fillLayerId(), () => {
        m.getCanvas().style.cursor = ''
      })
    }
  }

  // line 类型：仅轮廓
  if (props.layer.geometryType === 'line') {
    m.addLayer({
      id: lineLayerId(),
      type: 'line',
      source: sourceId(),
      paint: {
        'line-color': props.layer.strokeColor ?? '#3b82f6',
        'line-width': 3,
      },
    })
  }

  // keepQueryable 模式：根据 visible 状态同步视觉可见性
  if (props.keepQueryable) {
    syncVisibility()
  }
}

/** keepQueryable 模式：通过透明度控制视觉可见，不移除图层 */
function syncVisibility() {
  const m = props.map
  if (!m) return
  const fillOpacity = props.visible ? props.fillOpacity : 0
  const lineOpacity = props.visible ? 1 : 0
  try {
    if (m.getLayer(fillLayerId())) m.setPaintProperty(fillLayerId(), 'fill-opacity', fillOpacity)
  } catch { /* */ }
  try {
    if (m.getLayer(lineLayerId())) m.setPaintProperty(lineLayerId(), 'line-opacity', lineOpacity)
  } catch { /* */ }
}

// ──── 数据加载 ────────────────────────────────────

// 组件挂载后立即加载 WFS 数据
load(props.layer.name, props.layer.translate)

// 数据到达后叠加图层
watch(geojson, () => {
  if (geojson.value) {
    addLayer()
    emit('layer-ready', props.layer.name)
  }
})

// 错误处理
watch(error, () => {
  if (error.value) {
    emit('layer-error', props.layer.name, error.value)
  }
})

// 可见性切换
watch(() => props.visible, () => {
  if (!props.map) return
  if (props.keepQueryable) {
    // queryable 模式：不移除图层，仅调透明度
    syncVisibility()
  } else if (!props.visible || !geojson.value) {
    removeLayer()
  } else {
    addLayer()
  }
})

// 地图实例就绪 + 已有数据时叠加
watch(
  () => props.map,
  (m) => {
    if (m && geojson.value) addLayer()
  }
)

// 组件卸载时清理
onUnmounted(removeLayer)
</script>

<style scoped>
/* 纯逻辑组件，无样式 */
</style>
