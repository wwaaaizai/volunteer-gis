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

    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
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
