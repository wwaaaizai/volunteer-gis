<template>
  <div class="map-picker">
    <div class="map-picker-hint" v-if="!pickedLng || !pickedLat">
      <span v-if="!mapReady">🗺️ 地图加载中...</span>
      <span v-else>📍 点击地图选择活动位置</span>
    </div>
    <BaseMap ref="baseMapRef" class="picker-map" />
    <!-- 已选坐标 -->
    <div class="picked-coords" v-if="pickedLng && pickedLat">
      <el-tag type="success" closable @close="clearPick">
        已选：{{ pickedLng.toFixed(6) }}, {{ pickedLat.toFixed(6) }}
      </el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, unref, onMounted } from 'vue'
import type { Map } from 'maplibre-gl'
import { wgs84ToGcj02, gcj02ToWgs84 } from '@/utils/coordConvert'
import BaseMap from '@/components/map/BaseMap.vue'

const props = defineProps<{
  modelLng?: number
  modelLat?: number
}>()

const emit = defineEmits<{
  (e: 'update', lng: number, lat: number): void
}>()

const baseMapRef = ref<InstanceType<typeof BaseMap>>()
const pickedLng = ref<number | null>(props.modelLng ?? null)
const pickedLat = ref<number | null>(props.modelLat ?? null)
const mapReady = ref(false)

const MARKER_SOURCE = 'picker-point'
const MARKER_LAYER = 'picker-marker'

/** 获取 map 实例（兼容 defineExpose 不会自动解包 ref 的情况） */
function getMap(): Map | null {
  const comp = baseMapRef.value
  if (!comp) return null
  // unref 兼容 ref 和普通值
  return (unref(comp.map) as Map | null) ?? null
}

function getMapReady(): boolean {
  const comp = baseMapRef.value
  if (!comp) return false
  return (unref(comp.mapReady) as boolean) ?? false
}

/** 轮询等待地图就绪（避开 defineExpose ref 解包问题） */
function waitForMap(): Promise<Map> {
  return new Promise((resolve) => {
    const check = () => {
      const m = getMap()
      if (m && getMapReady()) {
        resolve(m)
      } else {
        setTimeout(check, 200)
      }
    }
    check()
  })
}

/** 绑定点击选点 */
function bindClick(map: Map) {
  map.on('click', (e) => {
    const wgs = gcj02ToWgs84(e.lngLat.lng, e.lngLat.lat)
    pickedLng.value = wgs[0]
    pickedLat.value = wgs[1]
    emit('update', wgs[0], wgs[1])
    updateMarker(map, e.lngLat.lng, e.lngLat.lat)
  })

  // 光标样式
  map.on('mouseenter', MARKER_LAYER, () => {
    map.getCanvas().style.cursor = 'pointer'
  })
  map.on('mouseleave', MARKER_LAYER, () => {
    map.getCanvas().style.cursor = ''
  })
}

/** 在地图上显示标记点 */
function updateMarker(map: Map, lng: number, lat: number) {
  const geojson = {
    type: 'FeatureCollection' as const,
    features: [{
      type: 'Feature' as const,
      geometry: { type: 'Point' as const, coordinates: [lng, lat] },
      properties: {},
    }],
  }
  if (map.getSource(MARKER_SOURCE)) {
    ;(map.getSource(MARKER_SOURCE) as any).setData(geojson)
  } else {
    map.addSource(MARKER_SOURCE, { type: 'geojson', data: geojson as any })
    map.addLayer({
      id: MARKER_LAYER,
      type: 'circle',
      source: MARKER_SOURCE,
      paint: {
        'circle-radius': 10,
        'circle-color': '#f56c6c',
        'circle-stroke-width': 3,
        'circle-stroke-color': '#fff',
      },
    })
  }
}

function clearPick() {
  pickedLng.value = null
  pickedLat.value = null
  const map = getMap()
  if (map && map.getSource(MARKER_SOURCE)) {
    ;(map.getSource(MARKER_SOURCE) as any).setData({
      type: 'FeatureCollection',
      features: [],
    })
  }
}

onMounted(async () => {
  const map = await waitForMap()
  mapReady.value = true
  bindClick(map)

  // 编辑模式回填已有坐标
  if (props.modelLng && props.modelLat) {
    const gcj = wgs84ToGcj02(props.modelLng, props.modelLat)
    updateMarker(map, gcj[0], gcj[1])
  }
})

defineExpose({ pickedLng, pickedLat })
</script>

<style scoped>
.map-picker {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}
.map-picker-hint {
  position: absolute;
  top: 8px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  background: rgba(255, 255, 255, 0.92);
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
  pointer-events: none;
}
.picker-map {
  width: 100%;
  height: 320px;
}
.picked-coords {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
}
</style>
