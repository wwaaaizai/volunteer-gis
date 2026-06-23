<template>
  <div class="map-picker">
    <div class="map-picker-hint" v-if="loading">
      🗺️ 地图加载中...
    </div>
    <div class="map-picker-hint" v-else-if="!pickedLng || !pickedLat">
      📍 点击地图选择活动位置
    </div>
    <div ref="mapEl" class="picker-map"></div>
    <div class="picked-coords" v-if="pickedLng && pickedLat">
      <el-tag type="success" closable @close="clearPick">
        已选：{{ pickedLng.toFixed(6) }}, {{ pickedLat.toFixed(6) }}
      </el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import maplibregl from 'maplibre-gl'
import { buildTiandituStyle, DEFAULT_CENTER, DEFAULT_ZOOM } from '@/config/map'
import { wgs84ToGcj02, gcj02ToWgs84 } from '@/utils/coordConvert'

const props = defineProps<{
  modelLng?: number
  modelLat?: number
}>()

const emit = defineEmits<{
  (e: 'update', lng: number, lat: number): void
}>()

const mapEl = ref<HTMLElement>()
const pickedLng = ref<number | null>(props.modelLng ?? null)
const pickedLat = ref<number | null>(props.modelLat ?? null)
const loading = ref(true)

let map: maplibregl.Map | null = null
const SOURCE_ID = 'picker-point'
const LAYER_ID = 'picker-marker'

/** 在地图上显示选点标记 */
function showMarker(lng: number, lat: number) {
  if (!map) return
  const geojson = {
    type: 'FeatureCollection' as const,
    features: [{
      type: 'Feature' as const,
      geometry: { type: 'Point' as const, coordinates: [lng, lat] },
      properties: {},
    }],
  }
  if (map.getSource(SOURCE_ID)) {
    ;(map.getSource(SOURCE_ID) as any).setData(geojson)
  } else {
    map.addSource(SOURCE_ID, { type: 'geojson', data: geojson as any })
    map.addLayer({
      id: LAYER_ID,
      type: 'circle',
      source: SOURCE_ID,
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
  if (map?.getSource(SOURCE_ID)) {
    ;(map.getSource(SOURCE_ID) as any).setData({ type: 'FeatureCollection', features: [] })
  }
}

onMounted(() => {
  if (!mapEl.value) return

  try {
    map = new maplibregl.Map({
      container: mapEl.value,
      style: buildTiandituStyle(),
      center: DEFAULT_CENTER,
      zoom: DEFAULT_ZOOM,
    })

    map.addControl(new maplibregl.NavigationControl(), 'top-right')

    map.on('load', () => {
      loading.value = false

      // 绑定点击选点
      map!.on('click', (e) => {
        const wgs = gcj02ToWgs84(e.lngLat.lng, e.lngLat.lat)
        pickedLng.value = wgs[0]
        pickedLat.value = wgs[1]
        emit('update', wgs[0], wgs[1])
        showMarker(e.lngLat.lng, e.lngLat.lat)
      })

      // 编辑模式：回填已有坐标
      if (props.modelLng && props.modelLat) {
        const gcj = wgs84ToGcj02(props.modelLng, props.modelLat)
        showMarker(gcj[0], gcj[1])
      }
    })

    map.on('error', (e) => {
      console.warn('地图瓦片加载失败:', e.error?.message ?? e)
    })
  } catch (e) {
    console.error('地图初始化失败', e)
    loading.value = false
  }
})

onUnmounted(() => {
  if (map) {
    map.remove()
    map = null
  }
})
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
