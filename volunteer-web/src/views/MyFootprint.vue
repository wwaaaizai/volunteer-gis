<template>
  <div class="footprint-page">
    <h2>我的志愿足迹</h2>

    <div class="footprint-stats" v-if="footprints.length > 0">
      <el-tag type="success">共参与 {{ footprints.length }} 次活动</el-tag>
      <el-tag type="warning">覆盖 {{ uniqueLocations }} 个地点</el-tag>
      <el-tag>累计 {{ totalHours }} 小时</el-tag>
    </div>

    <BaseMap ref="baseMapRef" :center="mapCenter" :zoom="14" style="height: 520px; border-radius: 8px; margin-top:12px" />

    <el-card style="margin-top: 16px" v-if="footprints.length > 0">
      <template #header>足迹时间线</template>
      <el-timeline>
        <el-timeline-item
          v-for="(fp, idx) in footprints"
          :key="idx"
          :timestamp="fp.signInTime"
          placement="top"
          :type="idx === 0 ? 'primary' : 'info'"
          :hollow="idx > 0"
        >
          <strong>#{{ idx + 1 }}</strong>
          {{ fp.activityTitle }} —
          {{ fp.volunteerHours != null ? fp.volunteerHours + 'h' : '签到' }}
        </el-timeline-item>
      </el-timeline>
    </el-card>
    <el-empty v-else description="暂无志愿足迹，快去报名参加活动吧！" style="margin-top: 60px" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import BaseMap from '@/components/map/BaseMap.vue'
import { wgs84ToGcj02 } from '@/utils/coordConvert'
import { DEFAULT_CENTER } from '@/config/map'
import request from '@/api'
import type { GeoJSONSource } from 'maplibre-gl'

interface Footprint {
  activityId: number
  activityTitle: string
  lng: number
  lat: number
  signInTime: string
  volunteerHours: number | null
}

const baseMapRef = ref<InstanceType<typeof BaseMap>>()
const footprints = ref<Footprint[]>([])
const mapCenter = ref<[number, number]>(DEFAULT_CENTER)

const uniqueLocations = computed(() =>
  new Set(footprints.value.map(f => `${f.lng},${f.lat}`)).size
)
const totalHours = computed(() =>
  footprints.value.reduce((s, f) => s + (f.volunteerHours || 0), 0).toFixed(1)
)

const FP_SOURCE = 'footprint-points'
const FP_LINE = 'footprint-line'
const FP_POINTS = 'footprint-circles'

function addFootprintLayer() {
  const map = baseMapRef.value?.map
  if (!map || footprints.value.length === 0) return

  // Convert WGS-84 to GCJ-02
  const gcjPoints = footprints.value.map(f => {
    const gcj = wgs84ToGcj02(f.lng, f.lat)
    return { lng: gcj[0], lat: gcj[1], title: f.activityTitle, idx: footprints.value.indexOf(f) }
  })

  // Center map on first footprint
  if (gcjPoints.length > 0) {
    mapCenter.value = [gcjPoints[0].lng, gcjPoints[0].lat]
  }

  const geojson: any = { type: 'FeatureCollection', features: [] }

  // Line connecting all points in time order
  if (gcjPoints.length >= 2) {
    geojson.features.push({
      type: 'Feature',
      geometry: {
        type: 'LineString',
        coordinates: gcjPoints.map(p => [p.lng, p.lat]),
      },
      properties: {},
    })
  }

  // Point markers
  gcjPoints.forEach((p, i) => {
    geojson.features.push({
      type: 'Feature',
      geometry: { type: 'Point', coordinates: [p.lng, p.lat] },
      properties: { title: p.title, index: i },
    })
  })

  if (!map.loaded()) {
    map.once('load', () => drawLayers(map, geojson))
  } else {
    drawLayers(map, geojson)
  }
}

function drawLayers(map: any, geojson: any) {
  if (map.getSource(FP_SOURCE)) {
    (map.getSource(FP_SOURCE) as GeoJSONSource).setData(geojson)
  } else {
    map.addSource(FP_SOURCE, { type: 'geojson', data: geojson })

    // Time-ordered line
    map.addLayer({
      id: FP_LINE, type: 'line', source: FP_SOURCE,
      paint: { 'line-color': '#409eff', 'line-width': 3, 'line-opacity': 0.6 },
      filter: ['==', '$type', 'LineString'],
    })
    // Point circles
    map.addLayer({
      id: FP_POINTS, type: 'circle', source: FP_SOURCE,
      paint: {
        'circle-radius': ['case', ['==', ['get', 'index'], 0], 10, 7],
        'circle-color': ['case', ['==', ['get', 'index'], 0], '#409eff',
                         ['==', ['get', 'index'], footprints.value.length - 1], '#67c23a',
                         '#909399'],
        'circle-stroke-width': 2,
        'circle-stroke-color': '#fff',
      },
      filter: ['==', '$type', 'Point'],
    })
  }
}

onMounted(async () => {
  try {
    footprints.value = (await request.get('/signups/my-footprint')) as Footprint[]
    if (footprints.value.length > 0) {
      // Wait for map to mount
      setTimeout(addFootprintLayer, 500)
    }
  } catch (err) {
    console.error('加载足迹数据失败:', err)
  }
})
</script>

<style scoped>
.footprint-page {
  max-width: 960px;
  margin: 20px auto;
  padding: 0 16px;
}
.footprint-stats {
  display: flex;
  gap: 12px;
  margin-top: 12px;
  flex-wrap: wrap;
}
</style>
