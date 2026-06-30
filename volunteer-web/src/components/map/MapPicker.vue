<template>
  <div class="map-picker">
    <!-- 提示条 -->
    <div class="picker-hint" v-if="!pickedLng || !pickedLat">
      <el-icon><Location /></el-icon>
      <span>点击地图选择活动位置</span>
    </div>
    <div class="picker-hint picked" v-else>
      <el-icon><Check /></el-icon>
      <span>已选中位置，可重新点击地图更改</span>
    </div>

    <!-- 地图容器 -->
    <div ref="mapEl" class="picker-map" :style="{ height: mapHeight + 'px' }"></div>

    <!-- 坐标信息面板 -->
    <div class="coord-panel" v-if="pickedLng && pickedLat">
      <div class="coord-row">
        <span class="coord-label">地图坐标 (GCJ-02)</span>
        <span class="coord-val">{{ gcjLng.toFixed(6) }}, {{ gcjLat.toFixed(6) }}</span>
        <el-button link size="small" @click="copyCoord('gcj')">复制</el-button>
      </div>
      <div class="coord-row">
        <span class="coord-label">GPS 坐标 (WGS-84)</span>
        <span class="coord-val">{{ pickedLng.toFixed(6) }}, {{ pickedLat.toFixed(6) }}</span>
        <el-button link size="small" @click="copyCoord('wgs')">复制</el-button>
      </div>
    </div>

    <!-- 按钮行 -->
    <div class="picker-actions">
      <el-button size="small" @click="clearPick" :disabled="!pickedLng">清除选点</el-button>
      <el-button
        v-if="showGeofenceBtn"
        size="small"
        type="warning"
        @click="goToGeofence"
      >
        设置签到围栏
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import maplibregl from 'maplibre-gl'
import { ElMessage } from 'element-plus'
import { Location, Check } from '@element-plus/icons-vue'
import { buildTiandituStyle, DEFAULT_CENTER, DEFAULT_ZOOM } from '@/config/map'
import { wgs84ToGcj02, gcj02ToWgs84 } from '@/utils/coordConvert'
import request from '@/api'

const props = withDefaults(defineProps<{
  modelLng?: number
  modelLat?: number
  mapHeight?: number
  showGeofenceBtn?: boolean
  activityId?: number
  geofenceGeojson?: string
}>(), {
  mapHeight: 320,
})

const emit = defineEmits<{
  (e: 'update', lng: number, lat: number): void
}>()

const router = useRouter()
const mapEl = ref<HTMLElement>()

// WGS-84 坐标（存入数据库的）
const pickedLng = ref<number | null>(props.modelLng ?? null)
const pickedLat = ref<number | null>(props.modelLat ?? null)

// GCJ-02 坐标（地图上显示的）
const gcjLng = ref(0)
const gcjLat = ref(0)

const SOURCE_MARKER = 'picker-marker'
const LAYER_MARKER = 'picker-marker-layer'
const SOURCE_FENCE = 'picker-fence'
const LAYER_FENCE_FILL = 'picker-fence-fill'
const LAYER_FENCE_LINE = 'picker-fence-line'

let map: maplibregl.Map | null = null

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
  if (map.getSource(SOURCE_MARKER)) {
    (map.getSource(SOURCE_MARKER) as any).setData(geojson)
  } else {
    map.addSource(SOURCE_MARKER, { type: 'geojson', data: geojson as any })
    map.addLayer({
      id: LAYER_MARKER, type: 'circle', source: SOURCE_MARKER,
      paint: {
        'circle-radius': 12,
        'circle-color': '#409eff',
        'circle-stroke-width': 3,
        'circle-stroke-color': '#fff',
        'circle-stroke-opacity': 0.9,
      },
    })
  }
}

/** 显示围栏叠加层 */
function showGeofenceOverlay(geojsonStr: string) {
  if (!map) return
  try {
    const poly = JSON.parse(geojsonStr)
    const geojson: any = {
      type: 'FeatureCollection',
      features: [{ type: 'Feature', geometry: poly, properties: {} }],
    }
    if (map.getSource(SOURCE_FENCE)) {
      (map.getSource(SOURCE_FENCE) as any).setData(geojson)
    } else {
      map.addSource(SOURCE_FENCE, { type: 'geojson', data: geojson })
      map.addLayer({
        id: LAYER_FENCE_FILL, type: 'fill', source: SOURCE_FENCE,
        paint: { 'fill-color': '#ff6600', 'fill-opacity': 0.15 },
      })
      map.addLayer({
        id: LAYER_FENCE_LINE, type: 'line', source: SOURCE_FENCE,
        paint: { 'line-color': '#ff6600', 'line-width': 2, 'line-dasharray': [6, 3] },
      })
    }
  } catch { /* 忽略解析错误 */ }
}

function clearPick() {
  pickedLng.value = null
  pickedLat.value = null
  if (map?.getSource(SOURCE_MARKER)) {
    (map.getSource(SOURCE_MARKER) as any).setData({ type: 'FeatureCollection', features: [] })
  }
  emit('update', 0, 0)
}

/** 复制坐标 */
function copyCoord(type: 'gcj' | 'wgs') {
  const text = type === 'gcj'
    ? `${gcjLng.value.toFixed(6)}, ${gcjLat.value.toFixed(6)}`
    : `${pickedLng.value!.toFixed(6)}, ${pickedLat.value!.toFixed(6)}`
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('坐标已复制')
  })
}

function goToGeofence() {
  if (props.activityId) {
    router.push(`/organizer/geofence/${props.activityId}`)
  }
}

// 同步外部 model 值
watch(() => [props.modelLng, props.modelLat], ([lng, lat]) => {
  if (lng && lat) {
    pickedLng.value = lng
    pickedLat.value = lat
    const gcj = wgs84ToGcj02(lng, lat)
    gcjLng.value = gcj[0]
    gcjLat.value = gcj[1]
    if (map?.loaded()) {
      showMarker(gcj[0], gcj[1])
    }
  }
})

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

    map.on('load', async () => {
      // 点击地图选点
      map!.on('click', (e) => {
        const wgs = gcj02ToWgs84(e.lngLat.lng, e.lngLat.lat)
        pickedLng.value = wgs[0]
        pickedLat.value = wgs[1]
        gcjLng.value = e.lngLat.lng
        gcjLat.value = e.lngLat.lat
        emit('update', wgs[0], wgs[1])
        showMarker(e.lngLat.lng, e.lngLat.lat)
      })

      // 回填已有坐标
      if (props.modelLng && props.modelLat) {
        const gcj = wgs84ToGcj02(props.modelLng, props.modelLat)
        gcjLng.value = gcj[0]
        gcjLat.value = gcj[1]
        showMarker(gcj[0], gcj[1])
      }

      // 加载围栏叠加层
      if (props.geofenceGeojson) {
        showGeofenceOverlay(props.geofenceGeojson)
      }
    })

    map.on('error', (e) => {
      console.warn('地图瓦片加载失败:', e.error?.message ?? e)
    })
  } catch (e) {
    console.error('地图初始化失败', e)
  }
})

onUnmounted(() => {
  if (map) { map.remove(); map = null }
})
</script>

<style scoped>
.map-picker {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
}
.picker-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 13px;
  color: #909399;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}
.picker-hint.picked {
  color: #67c23a;
  background: #f0f9eb;
}
.picker-map {
  width: 100%;
}
.coord-panel {
  padding: 10px 12px;
  background: #fafafa;
  border-top: 1px solid #ebeef5;
}
.coord-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 0;
  font-size: 13px;
}
.coord-label {
  color: #909399;
  width: 130px;
  flex-shrink: 0;
}
.coord-val {
  font-family: 'Consolas', 'Courier New', monospace;
  color: #303133;
  flex: 1;
}
.picker-actions {
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-top: 1px solid #ebeef5;
}
</style>
