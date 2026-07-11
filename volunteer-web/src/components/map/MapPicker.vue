<template>
  <div class="map-picker">
    <div class="map-picker-hint" v-if="loading">
      🗺️ 地图加载中...
    </div>
    <div class="map-picker-hint" v-else-if="!pickedLng || !pickedLat">
      📍 点击地图选择活动位置
    </div>
    <div ref="mapEl" class="picker-map"></div>
    <div class="picker-info">
      <div class="picker-info-row">
        <span class="picker-info-label">选中坐标</span>
        <template v-if="pickedLng && pickedLat">
          <el-tag type="success" closable @close="clearPick" size="small">
            {{ pickedLng.toFixed(6) }}, {{ pickedLat.toFixed(6) }}
          </el-tag>
        </template>
        <span v-else class="picker-info-empty">在地图上点击选取</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, shallowRef, onMounted, onUnmounted, nextTick } from 'vue'
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

const map = shallowRef<maplibregl.Map | null>(null)
const SOURCE_ID = 'picker-point'
const LAYER_ID = 'picker-marker'

function showMarker(lng: number, lat: number) {
  const m = map.value
  if (!m) return
  const geojson = {
    type: 'FeatureCollection' as const,
    features: [{
      type: 'Feature' as const,
      geometry: { type: 'Point' as const, coordinates: [lng, lat] },
      properties: {},
    }],
  }
  if (m.getSource(SOURCE_ID)) {
    ;(m.getSource(SOURCE_ID) as any).setData(geojson)
  } else {
    m.addSource(SOURCE_ID, { type: 'geojson', data: geojson as any })
    m.addLayer({
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
  const m = map.value
  if (m?.getSource(SOURCE_ID)) {
    ;(m.getSource(SOURCE_ID) as any).setData({ type: 'FeatureCollection', features: [] })
  }
}

onMounted(async () => {
  await nextTick()
  if (!mapEl.value) {
    console.error('[MapPicker] 容器元素未找到')
    loading.value = false
    return
  }

  try {
    const instance = new maplibregl.Map({
      container: mapEl.value,
      style: buildTiandituStyle(),
      center: DEFAULT_CENTER,
      zoom: DEFAULT_ZOOM,
    })

    instance.addControl(new maplibregl.NavigationControl(), 'top-right')

    instance.on('load', () => {
      loading.value = false

      instance.on('click', (e) => {
        const wgs = gcj02ToWgs84(e.lngLat.lng, e.lngLat.lat)
        pickedLng.value = wgs[0]
        pickedLat.value = wgs[1]
        emit('update', wgs[0], wgs[1])
        showMarker(e.lngLat.lng, e.lngLat.lat)
      })

      if (props.modelLng && props.modelLat) {
        const gcj = wgs84ToGcj02(props.modelLng, props.modelLat)
        showMarker(gcj[0], gcj[1])
      }
    })

    instance.on('error', (e) => {
      console.warn('[MapPicker] 瓦片加载失败:', e.error?.message ?? e)
    })

    map.value = instance
  } catch (e) {
    console.error('[MapPicker] 地图初始化失败', e)
    loading.value = false
  }
})

onUnmounted(() => {
  if (map.value) {
    map.value.remove()
    map.value = null
  }
})
</script>

<style scoped>
.map-picker {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  width: 100%;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.map-picker-hint {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  background: rgba(255, 255, 255, 0.95);
  padding: 6px 18px;
  border-radius: 20px;
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
  pointer-events: none;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}
.picker-map {
  width: 100%;
  height: 340px;
}
.picker-info {
  border-top: 1px solid #ebeef5;
  background: #fafafa;
  padding: 8px 14px;
}
.picker-info-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}
.picker-info-label {
  color: #909399;
  flex-shrink: 0;
}
.picker-info-empty {
  color: #c0c4cc;
  font-size: 13px;
}
</style>
