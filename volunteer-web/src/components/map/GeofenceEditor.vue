<template>
  <div class="geofence-editor" v-if="editing">
    <!-- 多围栏标签切换 -->
    <div class="polygon-tabs" v-if="polygons.length > 0">
      <span
        v-for="(p, i) in polygons"
        :key="i"
        class="polygon-tab"
        :class="{ active: currentPolygon === i, finished: p.finished }"
        @click="switchPolygon(i)"
      >
        {{ p.finished ? '✓' : '●' }} 围栏{{ i + 1 }}
        <span class="tab-close" @click.stop="deletePolygon(i)" v-if="polygons.length > 1">×</span>
      </span>
    </div>

    <div class="geofence-toolbar">
      <span class="toolbar-title">签到围栏绘制</span>
      <span class="hint">点击地图添加顶点</span>
      <el-button size="small" @click="undoVertex" :disabled="currentVertices.length === 0">撤销</el-button>
      <el-button size="small" @click="clearCurrent" :disabled="currentVertices.length === 0">清空</el-button>
      <el-button size="small" type="primary" @click="finishDrawing" :disabled="currentVertices.length < 3">完成绘制</el-button>
      <el-button size="small" type="info" @click="addNewPolygon">+ 新围栏(分会场)</el-button>
      <el-button size="small" type="success" @click="saveGeofence"
        :disabled="!hasFinishedPolygon || saving" :loading="saving">保存全部</el-button>
      <el-button size="small" @click="cancelEdit">取消</el-button>
    </div>

    <div class="vertex-info">
      当前围栏{{ currentPolygon + 1 }}: 已添加 <strong>{{ currentVertices.length }}</strong> 个顶点
      （需 ≥3 个顶点构成多边形{{ currentPolygon >= 0 && polygons[currentPolygon]?.finished ? '，✅ 已完成' : '' }}）
      <span v-if="polygons.length > 1" style="margin-left:8px;color:#409eff">
        共 {{ polygons.length }} 个围栏 | 已完成 {{ finishedCount }} 个
      </span>
    </div>
  </div>
  <el-button v-else size="small" type="warning" @click="startEdit" style="margin-top:8px">
    编辑签到围栏
  </el-button>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import type { GeoJSONSource } from 'maplibre-gl'
import { ElMessage } from 'element-plus'
import request from '@/api'

const props = defineProps<{ map: any | null; activityId: number }>()

// 多个围栏：每个围栏有独立顶点数组和完成状态
interface PolygonData { vertices: [number, number][]; finished: boolean }
const polygons = ref<PolygonData[]>([])
const currentPolygon = ref(0)
const editing = ref(false)
const finished = ref(false)  // deprecated, use hasFinishedPolygon
const saving = ref(false)

// 当前选中围栏的顶点
const currentVertices = computed(() =>
  polygons.value[currentPolygon.value]?.vertices ?? []
)
const hasFinishedPolygon = computed(() =>
  polygons.value.some(p => p.finished)
)
const finishedCount = computed(() =>
  polygons.value.filter(p => p.finished).length
)

const SOURCE_ID = 'geofence-draw'
const LINE_PREFIX = 'geofence-line-'
const FILL_PREFIX = 'geofence-fill-'
const VERTEX_PREFIX = 'geofence-vert-'

function onMapClick(e: any) {
  if (!editing.value) return
  const p = polygons.value[currentPolygon.value]
  if (!p || p.finished) return
  p.vertices.push([e.lngLat.lng, e.lngLat.lat])
  redrawAll()
}

// ── Polygon management ──
function addNewPolygon() {
  polygons.value.push({ vertices: [], finished: false })
  currentPolygon.value = polygons.value.length - 1
}
function switchPolygon(i: number) { currentPolygon.value = i }
function deletePolygon(i: number) {
  polygons.value.splice(i, 1)
  if (currentPolygon.value >= polygons.value.length) currentPolygon.value = Math.max(0, polygons.value.length - 1)
  if (polygons.value.length === 0) addNewPolygon()
  redrawAll()
}
function undoVertex() {
  const p = polygons.value[currentPolygon.value]
  if (p && !p.finished) { p.vertices.pop(); redrawAll() }
}
function clearCurrent() {
  const p = polygons.value[currentPolygon.value]
  if (p) { p.vertices = []; p.finished = false; redrawAll() }
}
function finishDrawing() {
  const p = polygons.value[currentPolygon.value]
  if (!p || p.vertices.length < 3) { ElMessage.warning('至少需要3个顶点'); return }
  p.finished = true
  redrawAll()
}

// ── Draw ──
function redrawAll() {
  const map = props.map; if (!map) return
  removeDrawLayers()

  const allFeatures: any[] = []
  polygons.value.forEach((p, pi) => {
    if (p.vertices.length === 0) return
    const coords = p.finished ? [...p.vertices, p.vertices[0]] : p.vertices
    allFeatures.push({
      type: 'Feature', geometry: { type: 'LineString', coordinates: coords },
      properties: { polygonIndex: pi, type: 'outline' },
    })
    if (p.finished && p.vertices.length >= 3) {
      allFeatures.push({
        type: 'Feature', geometry: { type: 'Polygon', coordinates: [[...p.vertices, p.vertices[0]]] },
        properties: { polygonIndex: pi, type: 'fill' },
      })
    }
    // Vertex points
    p.vertices.forEach((v, vi) => {
      allFeatures.push({
        type: 'Feature', geometry: { type: 'Point', coordinates: v },
        properties: { polygonIndex: pi, vertexIndex: vi, type: 'vertex' },
      })
    })
  })

  const geojson: any = { type: 'FeatureCollection', features: allFeatures }
  map.addSource(SOURCE_ID, { type: 'geojson', data: geojson })

  // Lines per polygon
  polygons.value.forEach((p, i) => {
    if (p.vertices.length === 0) return
    const color = i === currentPolygon.value ? '#ff6600' : '#b0b0b0'
    map.addLayer({
      id: LINE_PREFIX + i, type: 'line', source: SOURCE_ID,
      paint: { 'line-color': color, 'line-width': 2, 'line-dasharray': [4, 2] },
      filter: ['all', ['==', '$type', 'LineString'], ['==', ['get', 'polygonIndex'], i]],
    })
    map.addLayer({
      id: VERTEX_PREFIX + i, type: 'circle', source: SOURCE_ID,
      paint: { 'circle-radius': 5, 'circle-color': color, 'circle-stroke-width': 2, 'circle-stroke-color': '#fff' },
      filter: ['all', ['==', '$type', 'Point'], ['==', ['get', 'polygonIndex'], i]],
    })
    if (p.finished) {
      map.addLayer({
        id: FILL_PREFIX + i, type: 'fill', source: SOURCE_ID,
        paint: { 'fill-color': color, 'fill-opacity': 0.12 },
        filter: ['all', ['==', '$type', 'Polygon'], ['==', ['get', 'polygonIndex'], i]],
      })
    }
  })
}

function removeDrawLayers() {
  const map = props.map; if (!map) return
  const total = Math.max(polygons.value.length, 10)
  for (let i = 0; i < total; i++) {
    try { if (map.getLayer(FILL_PREFIX + i)) map.removeLayer(FILL_PREFIX + i) } catch {/* */}
    try { if (map.getLayer(VERTEX_PREFIX + i)) map.removeLayer(VERTEX_PREFIX + i) } catch {/* */}
    try { if (map.getLayer(LINE_PREFIX + i)) map.removeLayer(LINE_PREFIX + i) } catch {/* */}
  }
  try { if (map.getSource(SOURCE_ID)) map.removeSource(SOURCE_ID) } catch {/* */}
}

// ── Save as MultiPolygon ──
async function saveGeofence() {
  const finishedPolygons = polygons.value.filter(p => p.finished && p.vertices.length >= 3)
  if (finishedPolygons.length === 0) return
  saving.value = true
  try {
    let geojson: string
    if (finishedPolygons.length === 1) {
      geojson = JSON.stringify({
        type: 'Polygon',
        coordinates: [[...finishedPolygons[0].vertices, finishedPolygons[0].vertices[0]]],
      })
    } else {
      geojson = JSON.stringify({
        type: 'MultiPolygon',
        coordinates: finishedPolygons.map(p => [[...p.vertices, p.vertices[0]]]),
      })
    }
    await request.put(`/activities/${props.activityId}/geofence`, { geojson })
    ElMessage.success(`${finishedPolygons.length}个签到围栏保存成功`)
    cancelEdit()
  } catch {/* */} finally { saving.value = false }
}

// ── Start/Cancel ──
async function startEdit() {
  editing.value = true
  polygons.value = [{ vertices: [], finished: false }]
  currentPolygon.value = 0
  try {
    const data: any = await request.get(`/activities/${props.activityId}/geofence`)
    if (data?.geojson) {
      const allCoords = parseMultiPolygonCoords(data.geojson)
      if (allCoords.length > 0) {
        polygons.value = allCoords.map(c => ({ vertices: c, finished: c.length >= 3 }))
        ElMessage.info(`已加载${allCoords.length}个现有围栏`)
      }
    }
  } catch {/* */}
  setupMapListeners()
  redrawAll()
}

function cancelEdit() {
  editing.value = false
  polygons.value = []
  currentPolygon.value = 0
  removeMapListeners()
}

function setupMapListeners() {
  props.map?.on('click', onMapClick)
  if (props.map) props.map.getCanvas().style.cursor = 'crosshair'
}
function removeMapListeners() {
  props.map?.off('click', onMapClick)
  if (props.map) props.map.getCanvas().style.cursor = ''
  removeDrawLayers()
}

function parseMultiPolygonCoords(geojson: string): [number, number][][] {
  try {
    const obj = JSON.parse(geojson)
    if (obj.type === 'MultiPolygon') {
      return (obj.coordinates as number[][][][]).map(ring => ring[0].map(c => [c[0], c[1]] as [number, number]))
    } else if (obj.type === 'Polygon') {
      return [(obj.coordinates[0] as number[][]).map(c => [c[0], c[1]] as [number, number])]
    }
  } catch {/* */}
  return []
}

onUnmounted(() => removeMapListeners())
</script>

<style scoped>
.geofence-editor { margin-top: 8px; }
.polygon-tabs { display: flex; gap: 4px; flex-wrap: wrap; margin-bottom: 4px; }
.polygon-tab {
  padding: 2px 10px; border-radius: 12px; font-size: 12px;
  cursor: pointer; border: 1px solid #ddd; color: #666;
}
.polygon-tab.active { border-color: #ff6600; color: #ff6600; font-weight: 600; }
.polygon-tab.finished { border-color: #67c23a; color: #67c23a; }
.tab-close { margin-left: 4px; color: #f56c6c; font-weight: bold; }
.geofence-toolbar {
  display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
  padding: 8px 10px; background: #fffbe6; border: 1px solid #ffe58f; border-radius: 6px;
}
.toolbar-title { font-weight: 600; color: #d46b08; margin-right: 4px; }
.hint { font-size: 12px; color: #999; flex: 1; }
.vertex-info { margin-top: 4px; font-size: 12px; color: #666; }
</style>
