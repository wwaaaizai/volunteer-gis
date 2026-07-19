<template>
  <div class="geofence-editor" v-if="editing">
    <div class="geofence-toolbar">
      <span class="toolbar-title">签到围栏绘制</span>
      <span class="hint">点击地图添加顶点</span>
      <el-button size="small" @click="undoVertex" :disabled="vertices.length === 0">
        撤销
      </el-button>
      <el-button size="small" @click="clearAll" :disabled="vertices.length === 0">
        清空
      </el-button>
      <el-button size="small" type="primary" @click="finishDrawing"
        :disabled="vertices.length < 3">
        完成绘制
      </el-button>
      <el-button size="small" type="success" @click="saveGeofence"
        :disabled="!finished || saving" :loading="saving">
        保存围栏
      </el-button>
      <el-button size="small" @click="cancelEdit">取消</el-button>
    </div>
    <div class="vertex-list" v-if="vertices.length > 0">
      已添加 <strong>{{ vertices.length }}</strong> 个顶点
      （需 ≥3 个顶点构成多边形）
    </div>
  </div>
  <el-button v-else size="small" type="warning" @click="startEdit" style="margin-top:8px">
    编辑签到围栏
  </el-button>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import type { Map, GeoJSONSource } from 'maplibre-gl'
import { ElMessage } from 'element-plus'
import request from '@/api'

const props = defineProps<{
  map: any | null
  activityId: number
}>()

const SOURCE_ID = 'geofence-draw'
const LINE_ID = 'geofence-line'
const FILL_ID = 'geofence-fill'
const VERTEX_ID = 'geofence-vertices'

const editing = ref(false)
const finished = ref(false)
const saving = ref(false)
const vertices = ref<[number, number][]>([])

// 点击地图添加顶点
function onMapClick(e: any) {
  if (!editing.value || finished.value) return
  vertices.value.push([e.lngLat.lng, e.lngLat.lat])
  redrawPolygon()
}

// 开始编辑
async function startEdit() {
  editing.value = true
  finished.value = false
  vertices.value = []

  // 尝试加载已有围栏
  try {
    const data: any = await request.get(`/activities/${props.activityId}/geofence`)
    if (data?.geojson) {
      const coords = parseGeoJsonCoords(data.geojson)
      if (coords.length >= 3) {
        vertices.value = coords
        finished.value = true
        ElMessage.info('已加载现有围栏，点击"编辑围栏"可重新绘制')
      }
    }
  } catch { /* 无现有围栏 */ }

  setupMapListeners()
  redrawPolygon()
}

function setupMapListeners() {
  const map = props.map
  if (!map) return
  map.on('click', onMapClick)
  map.getCanvas().style.cursor = 'crosshair'
}

function removeMapListeners() {
  const map = props.map
  if (!map) return
  map.off('click', onMapClick)
  map.getCanvas().style.cursor = ''
  removeDrawLayers()
}

function parseGeoJsonCoords(geojson: string): [number, number][] {
  try {
    const obj = JSON.parse(geojson)
    return obj.coordinates?.[0]?.map((c: number[]) => [c[0], c[1]] as [number, number]) || []
  } catch {
    return []
  }
}

// 绘制/更新多边形
function redrawPolygon() {
  const map = props.map
  if (!map) return

  removeDrawLayers()

  if (vertices.value.length === 0) return

  // 构造 GeoJSON
  const coords = finished.value
    ? [...vertices.value, vertices.value[0]] // 闭合成环
    : vertices.value
  const geojson: any = {
    type: 'FeatureCollection',
    features: [
      { type: 'Feature', geometry: { type: 'LineString', coordinates: coords }, properties: {} },
    ],
  }
  if (finished.value && vertices.value.length >= 3) {
    geojson.features.push({
      type: 'Feature',
      geometry: { type: 'Polygon', coordinates: [[...vertices.value, vertices.value[0]]] },
      properties: {},
    })
  }

  map.addSource(SOURCE_ID, { type: 'geojson', data: geojson })

  // 边线
  map.addLayer({
    id: LINE_ID, type: 'line', source: SOURCE_ID,
    paint: { 'line-color': '#ff6600', 'line-width': 2, 'line-dasharray': [4, 2] },
    filter: ['==', '$type', 'LineString'],
  })

  // 顶点圆点
  map.addLayer({
    id: VERTEX_ID, type: 'circle', source: SOURCE_ID,
    paint: { 'circle-radius': 5, 'circle-color': '#ff6600', 'circle-stroke-width': 2, 'circle-stroke-color': '#fff' },
    filter: ['==', '$type', 'Point'],
  })

  // 填充面（仅完成时）
  if (finished.value) {
    map.addLayer({
      id: FILL_ID, type: 'fill', source: SOURCE_ID,
      paint: { 'fill-color': '#ff6600', 'fill-opacity': 0.15 },
      filter: ['==', '$type', 'Polygon'],
    })
  }

  // 为顶点添加 Point features
  const pointFeatures = vertices.value.map((v, i) => ({
    type: 'Feature' as const,
    geometry: { type: 'Point' as const, coordinates: v },
    properties: { index: i },
  }))
  const source = map.getSource(SOURCE_ID) as GeoJSONSource
  if (source) {
    const data: any = source._data
    geojson.features.push(...pointFeatures)
    source.setData(geojson)
  }
}

function removeDrawLayers() {
  const map = props.map
  if (!map) return
  try { if (map.getLayer(FILL_ID)) map.removeLayer(FILL_ID) } catch { /* */ }
  try { if (map.getLayer(VERTEX_ID)) map.removeLayer(VERTEX_ID) } catch { /* */ }
  try { if (map.getLayer(LINE_ID)) map.removeLayer(LINE_ID) } catch { /* */ }
  try { if (map.getSource(SOURCE_ID)) map.removeSource(SOURCE_ID) } catch { /* */ }
}

function undoVertex() {
  vertices.value.pop()
  redrawPolygon()
}

function clearAll() {
  vertices.value = []
  finished.value = false
  removeDrawLayers()
}

function finishDrawing() {
  if (vertices.value.length < 3) {
    ElMessage.warning('至少需要 3 个顶点构成多边形')
    return
  }
  finished.value = true
  redrawPolygon()
}

async function saveGeofence() {
  if (vertices.value.length < 3) return
  saving.value = true
  try {
    // 构造 GeoJSON Polygon
    const geojson = JSON.stringify({
      type: 'Polygon',
      coordinates: [[...vertices.value, vertices.value[0]]],
    })
    await request.put(`/activities/${props.activityId}/geofence`, { geojson })
    ElMessage.success('签到围栏保存成功')
    cancelEdit()
  } catch {
    // 错误已在拦截器提示
  } finally {
    saving.value = false
  }
}

function cancelEdit() {
  editing.value = false
  finished.value = false
  vertices.value = []
  removeMapListeners()
}

onUnmounted(() => {
  removeMapListeners()
})
</script>

<style scoped>
.geofence-editor {
  margin-top: 8px;
}
.geofence-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 8px 12px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
}
.toolbar-title {
  font-weight: 600;
  color: #d46b08;
  margin-right: 8px;
}
.hint {
  font-size: 12px;
  color: #999;
  flex: 1;
}
.vertex-list {
  margin-top: 4px;
  font-size: 12px;
  color: #666;
}
</style>
