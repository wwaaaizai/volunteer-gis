<template>
  <div class="map-picker">
    <div class="map-picker-hint" v-if="!pickedLng || !pickedLat">
      📍 点击地图选择活动位置
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
import { ref, watch } from 'vue'
import type { Map } from 'maplibre-gl'
import { wgs84ToGcj02, gcj02ToWgs84 } from '@/utils/coordConvert'
import BaseMap from '@/components/map/BaseMap.vue'

const props = defineProps<{
  /** WGS-84 初始经度（编辑模式回填） */
  modelLng?: number
  /** WGS-84 初始纬度（编辑模式回填） */
  modelLat?: number
}>()

const emit = defineEmits<{
  (e: 'update', lng: number, lat: number): void
}>()

const baseMapRef = ref<InstanceType<typeof BaseMap>>()
const pickedLng = ref<number | null>(props.modelLng ?? null)
const pickedLat = ref<number | null>(props.modelLat ?? null)

const MARKER_SOURCE = 'picker-point'
const MARKER_LAYER = 'picker-marker'

/** 地图就绪后绑定点击事件 + 回填已有坐标 */
watch(
  () => baseMapRef.value?.mapReady,
  (ready) => {
    if (!ready) return
    const map = baseMapRef.value?.map as Map | null
    if (!map) return

    // 点击地图选点
    map.on('click', (e) => {
      // 地图上点击得到的坐标是 GCJ-02（天地图底图），转为 WGS-84 存储
      const wgs = gcj02ToWgs84(e.lngLat.lng, e.lngLat.lat)
      pickedLng.value = wgs[0]
      pickedLat.value = wgs[1]
      emit('update', wgs[0], wgs[1])
      // 添加或更新标记
      updateMarker(map, e.lngLat.lng, e.lngLat.lat)
    })

    // 如果有初始坐标，在地图上显示标记
    if (props.modelLng && props.modelLat) {
      const gcj = wgs84ToGcj02(props.modelLng, props.modelLat)
      updateMarker(map, gcj[0], gcj[1])
    }
  }
)

/** 在地图上显示/移动标记点 */
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

/** 清除已选点 */
function clearPick() {
  pickedLng.value = null
  pickedLat.value = null
  const map = baseMapRef.value?.map as Map | null
  if (map && map.getSource(MARKER_SOURCE)) {
    ;(map.getSource(MARKER_SOURCE) as any).setData({
      type: 'FeatureCollection',
      features: [],
    })
  }
}

/** 暴露当前 WGS-84 坐标供父组件读取 */
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
  background: rgba(255, 255, 255, 0.9);
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  color: #909399;
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
