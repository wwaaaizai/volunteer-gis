<template>
  <div class="gis-analysis-page">
    <el-button text @click="$router.push('/')" style="margin-bottom:8px">← 返回地图</el-button>
    <h2>GIS 空间分析工具</h2>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- ══════ 缓冲区分析 ══════ -->
      <el-tab-pane label="缓冲区分析" name="buffer">
        <p class="desc">以活动点为中心画缓冲区圆，统计覆盖范围内的报名学生数</p>
        <el-form :inline="true" size="small">
          <el-form-item label="选择活动">
            <el-select v-model="bufferActivityId" placeholder="选择活动" @change="onBufferActivityChange" clearable>
              <el-option v-for="a in activities" :key="a.id" :label="a.title" :value="a.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="缓冲区半径(m)">
            <el-input-number v-model="bufferRadius" :min="100" :max="2000" :step="100" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="runBuffer">执行分析</el-button>
            <el-button @click="clearBuffer">取消</el-button>
          </el-form-item>
        </el-form>
        <BaseMap ref="bufferMapRef" style="height: 500px; border-radius: 6px; margin-top: 8px" />
        <el-card v-if="bufferResult" style="margin-top: 12px">
          <template #header>缓冲区分析结果
            <el-button size="small" text style="float:right" @click="clearBuffer">清除</el-button>
          </template>
          <el-tag type="success">覆盖 {{ bufferResult.coveredCount }} 个活动</el-tag>
          <el-tag type="warning" style="margin-left: 8px">总计 {{ bufferResult.totalSignups }} 人次报名</el-tag>
          <el-table :data="bufferResult.activities" size="small" style="margin-top:8px">
            <el-table-column prop="title" label="活动" />
            <el-table-column prop="distance" label="距离(m)" width="100" />
            <el-table-column prop="signedCount" label="报名人数" width="100" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ══════ 覆盖率分析 ══════ -->
      <el-tab-pane label="覆盖率分析" name="coverage">
        <p class="desc">网格化校区，统计各区域活动覆盖密度，标注"服务盲区"</p>
        <el-form :inline="true" size="small">
          <el-form-item label="网格精度">
            <el-select v-model="coverageGrid" @change="runCoverage">
              <el-option label="4×4 (粗)" :value="4" />
              <el-option label="6×6 (中)" :value="6" />
              <el-option label="8×8 (细)" :value="8" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="runCoverage">执行分析</el-button>
            <el-button @click="clearCoverage">取消</el-button>
          </el-form-item>
        </el-form>
        <BaseMap ref="coverageMapRef" style="height: 500px; border-radius: 6px; margin-top: 8px" />
        <div class="legend" style="margin-top:8px;display:flex;gap:12px;align-items:center">
          <span style="font-size:12px;color:#666">覆盖率:</span>
          <span class="leg" style="background:#f0f0f0">空白</span>
          <span class="leg" style="background:#c6dbef">≤1</span>
          <span class="leg" style="background:#6baed6">2-3</span>
          <span class="leg" style="background:#2171b5">4+</span>
        </div>
      </el-tab-pane>

      <!-- ══════ 时段空间分布 ══════ -->
      <el-tab-pane label="时段空间分布" name="timeline">
        <p class="desc">按月份查看活动空间分布变化，支持时间轴联动</p>
        <el-form :inline="true" size="small">
          <el-form-item label="选择月份">
            <el-select v-model="timelineMonth" @change="runTimeline" clearable>
              <el-option label="全部" value="" />
              <el-option label="2026-06" value="2026-06" />
              <el-option label="2026-07" value="2026-07" />
              <el-option label="2026-09" value="2026-09" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="clearTimeline">取消</el-button>
          </el-form-item>
        </el-form>
        <BaseMap ref="timelineMapRef" style="height: 500px; border-radius: 6px; margin-top: 8px" />
      </el-tab-pane>

      <!-- ══════ 集合点推荐 ══════ -->
      <el-tab-pane label="集合点推荐" name="meeting">
        <p class="desc">根据报名学生签到坐标聚类，推荐最优集合点</p>
        <el-form :inline="true" size="small">
          <el-form-item label="选择活动">
            <el-select v-model="meetingActivityId" placeholder="选择活动">
              <el-option v-for="a in activities" :key="a.id" :label="a.title" :value="a.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="聚类数">
            <el-input-number v-model="meetingK" :min="2" :max="5" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="runMeeting">执行分析</el-button>
            <el-button @click="clearMeeting">取消</el-button>
          </el-form-item>
        </el-form>
        <BaseMap ref="meetingMapRef" style="height: 500px; border-radius: 6px; margin-top: 8px" />
        <el-card v-if="meetingResult.length" style="margin-top: 12px">
          <template #header>推荐集合点</template>
          <div v-for="mp in meetingResult" :key="mp.index" class="meeting-item">
            <el-tag>{{ mp.name }}</el-tag>
            <span style="font-family:monospace;font-size:12px;margin-left:8px;color:#666">
              GCJ-02: {{ mp.lng.toFixed(5) }}, {{ mp.lat.toFixed(5) }}
            </span>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ══════ 路径规划 ══════ -->
      <el-tab-pane label="路径规划" name="route">
        <p class="desc">从当前位置到活动地点的步行/骑行/驾车导航</p>
        <el-form :inline="true" size="small">
          <el-form-item label="起点">
            <el-radio-group v-model="routeFrom" size="small">
              <el-radio-button value="gps">我的位置 (GPS)</el-radio-button>
              <el-radio-button value="pick">地图选点</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="目标活动">
            <el-select v-model="routeActivityId" placeholder="选择目标活动" size="small">
              <el-option v-for="a in activities" :key="a.id" :label="a.title" :value="a.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="出行方式">
            <el-radio-group v-model="routeMode" size="small" @change="runRoute">
              <el-radio-button value="foot">🚶 步行</el-radio-button>
              <el-radio-button value="bike">🚲 骑行</el-radio-button>
              <el-radio-button value="car">🚗 驾车</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="runRoute">规划路线</el-button>
            <el-button @click="clearRoute">取消</el-button>
          </el-form-item>
        </el-form>
        <div class="gps-hint">
          <el-button size="small" type="success" @click="getGpsLocation" v-if="!userLng">
            📍 获取我的位置
          </el-button>
          <el-tag v-else type="success" effect="plain">
            📍 GPS: {{ userLng.toFixed(5) }}, {{ userLat.toFixed(5) }}
          </el-tag>
        </div>
        <BaseMap ref="routeMapRef" style="height: 500px; border-radius: 6px; margin-top: 8px" />
        <el-card v-if="routeResult" style="margin-top: 12px">
          <template #header>路径规划结果
            <el-button size="small" text style="float:right" @click="clearRoute">清除</el-button>
          </template>
          <el-descriptions :column="3" size="small" border>
            <el-descriptions-item label="出行方式">
              {{ routeMode === 'foot' ? '步行' : routeMode === 'bike' ? '骑行' : '驾车' }}
            </el-descriptions-item>
            <el-descriptions-item label="总距离">
              {{ (routeResult.distance / 1000).toFixed(1) }} km
            </el-descriptions-item>
            <el-descriptions-item label="预计时间">
              {{ Math.round(routeResult.duration / 60) }} 分钟
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import BaseMap from '@/components/map/BaseMap.vue'
import { wgs84ToGcj02 } from '@/utils/coordConvert'
import request from '@/api'

const activeTab = ref('buffer')
const activities = ref<any[]>([])

// ── Buffer ──
const bufferActivityId = ref<number | null>(null)
const bufferRadius = ref(500)
const bufferMapRef = ref<InstanceType<typeof BaseMap>>()
const bufferResult = ref<any>(null)

function onBufferActivityChange(id: number | null) {
  if (!id) return
  const act = activities.value.find(a => a.id === id)
  if (act) runBuffer()
}

async function runBuffer() {
  if (!bufferActivityId.value) return
  const act = activities.value.find(a => a.id === bufferActivityId.value)
  if (!act) return
  const gcj = wgs84ToGcj02(act.longitude, act.latitude)
  try {
    bufferResult.value = await request.get('/map/buffer', {
      params: { lng: act.longitude, lat: act.latitude, radius: bufferRadius.value }
    })
    await nextTick()
    showBufferOnMap(gcj[0], gcj[1], bufferRadius.value)
  } catch { /* */ }
}

function showBufferOnMap(lng: number, lat: number, radius: number) {
  const map = bufferMapRef.value?.map; if (!map) return
  const sid = 'buffer-circle'; const lid = 'buffer-circle-layer'
  try { if (map.getLayer(lid)) map.removeLayer(lid) } catch { /* */ }
  try { if (map.getSource(sid)) map.removeSource(sid) } catch { /* */ }

  // Draw circle as GeoJSON (approximate with many points)
  const points: [number, number][] = []
  const steps = 64
  const radiusDeg = radius / 111320
  for (let i = 0; i <= steps; i++) {
    const angle = (i / steps) * Math.PI * 2
    points.push([lng + radiusDeg * Math.cos(angle), lat + radiusDeg * Math.sin(angle)])
  }

  map.addSource(sid, {
    type: 'geojson',
    data: { type: 'Feature', geometry: { type: 'Polygon', coordinates: [points] }, properties: {} },
  })
  map.addLayer({ id: lid, type: 'fill', source: sid,
    paint: { 'fill-color': '#409eff', 'fill-opacity': 0.15 } })
  map.addLayer({ id: lid + '-line', type: 'line', source: sid,
    paint: { 'line-color': '#409eff', 'line-width': 2 } })
}

// ── Coverage ──
const coverageGrid = ref(8)
const coverageMapRef = ref<InstanceType<typeof BaseMap>>()

async function runCoverage() {
  try {
    const data = await request.get('/map/coverage', { params: { gridSize: coverageGrid.value } }) as any
    await nextTick()
    showCoverageOnMap(data)
  } catch { /* */ }
}

function showCoverageOnMap(data: any) {
  const map = coverageMapRef.value?.map; if (!map) return
  const sid = 'coverage-grid'; const lid = 'coverage-fill'
  try { if (map.getLayer(lid)) map.removeLayer(lid) } catch { /* */ }
  try { if (map.getSource(sid)) map.removeSource(sid) } catch { /* */ }

  map.addSource(sid, { type: 'geojson', data })
  map.addLayer({
    id: lid, type: 'fill', source: sid,
    paint: {
      'fill-color': ['step', ['get', 'count'],
        '#f0f0f0', 1, '#c6dbef', 2, '#6baed6', 4, '#2171b5'],
      'fill-opacity': 0.6,
      'fill-outline-color': '#999',
    },
  })
}

// ── Timeline ──
const timelineMonth = ref('')
const timelineMapRef = ref<InstanceType<typeof BaseMap>>()

async function runTimeline() {
  try {
    const data = await request.get('/map/timeline', {
      params: timelineMonth.value ? { yearMonth: timelineMonth.value } : {}
    }) as any
    await nextTick()
    showTimelineOnMap(data)
  } catch { /* */ }
}

function showTimelineOnMap(data: any) {
  const map = timelineMapRef.value?.map; if (!map) return
  const sid = 'timeline-points'; const lid = 'timeline-circles'
  try { if (map.getLayer(lid)) map.removeLayer(lid) } catch { /* */ }
  try { if (map.getSource(sid)) map.removeSource(sid) } catch { /* */ }
  if (!data?.features?.length) return

  map.addSource(sid, { type: 'geojson', data })
  map.addLayer({
    id: lid, type: 'circle', source: sid,
    paint: { 'circle-radius': 8, 'circle-color': '#e6a817',
      'circle-stroke-width': 2, 'circle-stroke-color': '#fff' },
  })
}

// ── Meeting ──
const meetingActivityId = ref<number | null>(null)
const meetingK = ref(3)
const meetingMapRef = ref<InstanceType<typeof BaseMap>>()
const meetingResult = ref<any[]>([])

async function runMeeting() {
  if (!meetingActivityId.value) return
  try {
    meetingResult.value = await request.get('/map/cluster-meeting', {
      params: { activityId: meetingActivityId.value, k: meetingK.value }
    }) as any[]
    await nextTick()
    showMeetingOnMap()
  } catch { /* */ }
}

function showMeetingOnMap() {
  const map = meetingMapRef.value?.map; if (!map) return
  const sid = 'meeting-points'; const lid = 'meeting-circles'
  try { if (map.getLayer(lid)) map.removeLayer(lid) } catch { /* */ }
  try { if (map.getSource(sid)) map.removeSource(sid) } catch { /* */ }

  const features = meetingResult.value.map(mp => ({
    type: 'Feature', geometry: { type: 'Point', coordinates: [mp.lng, mp.lat] },
    properties: { name: mp.name },
  }))
  map.addSource(sid, { type: 'geojson', data: { type: 'FeatureCollection', features } })
  map.addLayer({
    id: lid, type: 'circle', source: sid,
    paint: { 'circle-radius': 12, 'circle-color': '#f56c6c',
      'circle-stroke-width': 3, 'circle-stroke-color': '#fff' },
  })
}

// ── Clear functions ──
function clearBuffer() {
  bufferResult.value = null
  const map = bufferMapRef.value?.map
  try { if (map?.getLayer('buffer-circle-layer')) map.removeLayer('buffer-circle-layer') } catch { /* */ }
  try { if (map?.getLayer('buffer-circle-layer-line')) map.removeLayer('buffer-circle-layer-line') } catch { /* */ }
  try { if (map?.getSource('buffer-circle')) map.removeSource('buffer-circle') } catch { /* */ }
}
function clearCoverage() {
  const map = coverageMapRef.value?.map
  try { if (map?.getLayer('coverage-fill')) map.removeLayer('coverage-fill') } catch { /* */ }
  try { if (map?.getSource('coverage-grid')) map.removeSource('coverage-grid') } catch { /* */ }
}
function clearTimeline() {
  const map = timelineMapRef.value?.map
  try { if (map?.getLayer('timeline-circles')) map.removeLayer('timeline-circles') } catch { /* */ }
  try { if (map?.getSource('timeline-points')) map.removeSource('timeline-points') } catch { /* */ }
}
function clearMeeting() {
  meetingResult.value = []
  const map = meetingMapRef.value?.map
  try { if (map?.getLayer('meeting-circles')) map.removeLayer('meeting-circles') } catch { /* */ }
  try { if (map?.getSource('meeting-points')) map.removeSource('meeting-points') } catch { /* */ }
}

// ── Route ──
const routeFrom = ref<'gps' | 'pick'>('gps')
const routeMode = ref<'foot' | 'bike' | 'car'>('foot')
const routeActivityId = ref<number | null>(null)
const routeMapRef = ref<InstanceType<typeof BaseMap>>()
const routeResult = ref<any>(null)
const userLng = ref(0)
const userLat = ref(0)
const pickLng = ref(0)
const pickLat = ref(0)

function getGpsLocation() {
  if (!navigator.geolocation) return
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      userLng.value = pos.coords.longitude
      userLat.value = pos.coords.latitude
    },
    () => { ElMessage.warning('GPS定位失败，请使用地图选点') },
    { enableHighAccuracy: true, timeout: 10000 },
  )
}

async function runRoute() {
  if (!routeActivityId.value) return
  const act = activities.value.find(a => a.id === routeActivityId.value)
  if (!act) return

  // Determine start coordinates (WGS-84)
  let startLng: number, startLat: number
  if (routeFrom.value === 'gps') {
    if (!userLng.value) { getGpsLocation(); return }
    startLng = userLng.value; startLat = userLat.value
  } else {
    if (!pickLng.value) return
    startLng = pickLng.value; startLat = pickLat.value
  }

  // Convert to GCJ-02 for OSRM (OSRM uses WGS-84 worldwide, GCJ offset in China is small enough for demo)
  const endGcj = wgs84ToGcj02(act.longitude, act.latitude)

  try {
    // 通过后端代理调用高德API，解决CORS跨域问题
    const resp = await request.get('/map/route', {
      params: { mode: routeMode.value, originLng: startLng, originLat: startLat,
                destLng: act.longitude, destLat: act.latitude }
    }) as any
    if (resp.error) {
      ElMessage.warning('路径规划失败: ' + resp.error)
      drawStraightLine(act, startLng, startLat, endGcj)
      return
    }
    const data = JSON.parse(resp.raw)
    if (data.status !== '1' || !data.route?.paths?.[0]) {
      ElMessage.warning('路径规划失败: ' + (data.info || '起点与终点间无可通行道路'))
      drawStraightLine(act, startLng, startLat, endGcj)
      return
    }
    const path = data.route.paths[0]
    routeResult.value = { distance: parseInt(path.distance), duration: parseInt(path.duration) }
    const polyline = parseAmapPolyline(path)
    await nextTick()
    drawRoutePolyline(polyline, act, startLng, startLat, endGcj)
  } catch {
    drawStraightLine(act, startLng, startLat, endGcj)
  }
}

/** 解析高德 polyline 编码为坐标数组（GCJ-02） */
function parseAmapPolyline(path: any): [number, number][] {
  const coords: [number, number][] = []
  if (path.steps) {
    for (const step of path.steps) {
      if (step.polyline) {
        const decoded = decodeAmapPolyline(step.polyline)
        coords.push(...decoded)
      }
    }
  }
  return coords
}

/** 高德 polyline 编码解码（差分编码，与 Google polyline 类似但不同） */
function decodeAmapPolyline(encoded: string): [number, number][] {
  const result: [number, number][] = []
  let i = 0, lng = 0, lat = 0
  while (i < encoded.length) {
    let b, shift = 0, val = 0
    do { b = encoded.charCodeAt(i++) - 63; val |= (b & 0x1f) << shift; shift += 5 } while (b >= 0x20)
    const dlng = (val & 1) ? ~(val >> 1) : (val >> 1)
    lng += dlng
    shift = 0; val = 0
    do { b = encoded.charCodeAt(i++) - 63; val |= (b & 0x1f) << shift; shift += 5 } while (b >= 0x20)
    const dlat = (val & 1) ? ~(val >> 1) : (val >> 1)
    lat += dlat
    result.push([lng / 1e6, lat / 1e6])
  }
  return result
}

function drawRoutePolyline(coords: [number, number][], act: any, startLng: number, startLat: number, endGcj: [number, number]) {
  const map = routeMapRef.value?.map; if (!map) return
  const sid = 'route-path'; const lid = 'route-line'
  try { if (map.getLayer(lid)) map.removeLayer(lid) } catch { /* */ }
  try { if (map.getSource(sid)) map.removeSource(sid) } catch { /* */ }

  const geometry = { type: 'LineString', coordinates: coords }
  map.addSource(sid, { type: 'geojson', data: { type: 'Feature', geometry, properties: {} } })
  map.addLayer({
    id: lid, type: 'line', source: sid,
    paint: { 'line-color': '#409eff', 'line-width': 5, 'line-opacity': 0.85 },
  })

  addRouteMarkers(map, startLng, startLat, endGcj)
  const bounds = new (window as any).maplibregl.LngLatBounds()
  coords.forEach(c => bounds.extend(c as any))
  map.fitBounds(bounds, { padding: 80, maxZoom: 17 })
}

function drawStraightLine(act: any, startLng: number, startLat: number, endGcj: [number, number]) {
  const map = routeMapRef.value?.map; if (!map) return
  ElMessage.info('高德API规划失败，显示直线参考路径')
  const sid = 'route-path'; const lid = 'route-line'
  try { if (map.getLayer(lid)) map.removeLayer(lid) } catch { /* */ }
  try { if (map.getSource(sid)) map.removeSource(sid) } catch { /* */ }

  const geojson = {
    type: 'Feature', geometry: {
      type: 'LineString', coordinates: [[startLng, startLat], [endGcj[0], endGcj[1]]],
    }, properties: {},
  }
  map.addSource(sid, { type: 'geojson', data: geojson })
  map.addLayer({
    id: lid, type: 'line', source: sid,
    paint: { 'line-color': '#ff6600', 'line-width': 3, 'line-dasharray': [6, 4] },
  })
  addRouteMarkers(map, startLng, startLat, endGcj)
  const bounds = new (window as any).maplibregl.LngLatBounds()
  bounds.extend([startLng, startLat]); bounds.extend(endGcj)
  map.fitBounds(bounds, { padding: 80, maxZoom: 17 })
  const dist = Math.sqrt((endGcj[0]-startLng)**2 + (endGcj[1]-startLat)**2) * 111320
  routeResult.value = { distance: dist, duration: dist / 1.4 }
}

function addRouteMarkers(map: any, startLng: number, startLat: number, endGcj: [number, number]) {
  const msid = 'route-markers'; const mlid = 'route-marker-layer'
  const tlid = 'route-marker-labels'
  try { if (map.getLayer(mlid)) map.removeLayer(mlid) } catch { /* */ }
  try { if (map.getLayer(tlid)) map.removeLayer(tlid) } catch { /* */ }
  try { if (map.getSource(msid)) map.removeSource(msid) } catch { /* */ }

  map.addSource(msid, {
    type: 'geojson',
    data: { type: 'FeatureCollection', features: [
      { type: 'Feature', geometry: { type: 'Point', coordinates: [startLng, startLat] },
        properties: { type: 'start', label: '起点' } },
      { type: 'Feature', geometry: { type: 'Point', coordinates: endGcj },
        properties: { type: 'end', label: '终点' } },
    ]},
  })
  // Large markers
  map.addLayer({ id: mlid, type: 'circle', source: msid, paint: {
    'circle-radius': 12,
    'circle-color': ['case', ['==', ['get', 'type'], 'start'], '#67c23a', '#f56c6c'],
    'circle-stroke-width': 3, 'circle-stroke-color': '#fff',
  }})
  // Text labels
  map.addLayer({ id: tlid, type: 'symbol', source: msid, layout: {
    'text-field': ['get', 'label'],
    'text-size': 13,
    'text-offset': [0, -1.8],
    'text-anchor': 'top',
  }, paint: {
    'text-color': '#303133',
    'text-halo-color': '#fff',
    'text-halo-width': 2,
  }})
}

function clearRoute() {
  routeResult.value = null
  const map = routeMapRef.value?.map
  try { if (map?.getLayer('route-line')) map.removeLayer('route-line') } catch { /* */ }
  try { if (map?.getLayer('route-marker-layer')) map.removeLayer('route-marker-layer') } catch { /* */ }
  try { if (map?.getLayer('route-marker-labels')) map.removeLayer('route-marker-labels') } catch { /* */ }
  try { if (map?.getSource('route-path')) map.removeSource('route-path') } catch { /* */ }
  try { if (map?.getSource('route-markers')) map.removeSource('route-markers') } catch { /* */ }
}

// Setup map-click for pick mode with visual feedback
watch(routeMapRef, (ref) => {
  const map = ref?.map
  if (!map) return
  map.on('click', (e: any) => {
    if (routeFrom.value !== 'pick') return
    pickLng.value = e.lngLat.lng
    pickLat.value = e.lngLat.lat
    // Show a temporary marker at the pick point
    const psid = 'pick-point'
    try { if (map.getSource(psid)) map.removeSource(psid) } catch { /* */ }
    try { if (map.getLayer(psid)) map.removeLayer(psid) } catch { /* */ }
    map.addSource(psid, { type: 'geojson', data: { type: 'Feature', geometry: { type: 'Point', coordinates: [e.lngLat.lng, e.lngLat.lat] }, properties: {} } })
    map.addLayer({ id: psid, type: 'circle', source: psid, paint: { 'circle-radius': 8, 'circle-color': '#67c23a', 'circle-stroke-width': 2, 'circle-stroke-color': '#fff' } })
  })
})

onMounted(async () => {
  try { activities.value = await request.get('/activities') } catch { /* */ }
})
</script>

<style scoped>
.gis-analysis-page { max-width: 1200px; margin: 12px auto; padding: 0 16px; }
.desc { color: #909399; font-size: 13px; margin-bottom: 12px; }
.leg { padding: 2px 10px; border-radius: 4px; font-size: 11px; color: #333; border: 1px solid #ddd; }
.meeting-item { display: flex; align-items: center; gap: 8px; padding: 4px 0; }
</style>
