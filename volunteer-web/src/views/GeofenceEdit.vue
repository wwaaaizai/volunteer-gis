<template>
  <div class="geofence-edit-page">
    <el-button text @click="$router.back()" style="margin-bottom:8px">← 返回</el-button>
    <el-card>
      <template #header>
        <span>签到围栏设置 — {{ activityTitle }}</span>
      </template>

      <!-- 活动位置信息 -->
      <div class="activity-location" v-if="activityLng && activityLat">
        <el-tag type="primary" effect="plain">
          活动地点：{{ activityLocationName }}
        </el-tag>
        <span class="coord-info">
          WGS-84: {{ activityLng.toFixed(6) }}, {{ activityLat.toFixed(6) }}
        </span>
        <span class="coord-info gcj">
          GCJ-02: {{ gcjLng.toFixed(6) }}, {{ gcjLat.toFixed(6) }}
        </span>
      </div>

      <!-- 地图容器 -->
      <BaseMap ref="baseMapRef" style="height: 500px; border-radius: 6px" />

      <GeofenceEditor
        :map="mapInstance"
        :activity-id="activityId"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import type { GeoJSONSource } from 'maplibre-gl'
import BaseMap from '@/components/map/BaseMap.vue'
import GeofenceEditor from '@/components/map/GeofenceEditor.vue'
import { wgs84ToGcj02 } from '@/utils/coordConvert'
import request from '@/api'

const route = useRoute()
const activityId = Number(route.params.id)

const baseMapRef = ref<InstanceType<typeof BaseMap>>()
const mapInstance = computed(() => baseMapRef.value?.map ?? null)
const activityTitle = ref('')
const activityLocationName = ref('')
const activityLng = ref(0)
const activityLat = ref(0)
const gcjLng = ref(0)
const gcjLat = ref(0)

const ACT_SOURCE = 'activity-point'
const ACT_LAYER = 'activity-point-layer'

/** 在地图上显示活动选点标记 */
async function showActivityMarker() {
  await new Promise(r => setTimeout(r, 300)) // 等地图 load
  const map = mapInstance.value
  if (!map || !activityLng.value) return

  const gcj = wgs84ToGcj02(activityLng.value, activityLat.value)
  gcjLng.value = gcj[0]
  gcjLat.value = gcj[1]

  if (!map.loaded()) {
    map.once('load', () => addMarker(map, gcj))
  } else {
    addMarker(map, gcj)
  }
}

function addMarker(map: any, gcj: [number, number]) {
  const geojson = {
    type: 'FeatureCollection',
    features: [{
      type: 'Feature',
      geometry: { type: 'Point', coordinates: gcj },
      properties: { name: activityTitle.value },
    }],
  }
  if (map.getSource(ACT_SOURCE)) {
    (map.getSource(ACT_SOURCE) as GeoJSONSource).setData(geojson as any)
  } else {
    map.addSource(ACT_SOURCE, { type: 'geojson', data: geojson })
    map.addLayer({
      id: ACT_LAYER, type: 'circle', source: ACT_SOURCE,
      paint: {
        'circle-radius': 10,
        'circle-color': '#409eff',
        'circle-stroke-width': 3,
        'circle-stroke-color': '#fff',
      },
    })
  }
}

onMounted(async () => {
  try {
    const data: any = await request.get(`/activities/${activityId}`)
    activityTitle.value = data?.title || `活动 #${activityId}`
    activityLocationName.value = data?.locationName || '未知'
    activityLng.value = data?.longitude || 0
    activityLat.value = data?.latitude || 0

    if (activityLng.value && activityLat.value) {
      showActivityMarker()
    }
  } catch { activityTitle.value = `活动 #${activityId}` }
})
</script>

<style scoped>
.geofence-edit-page {
  max-width: 960px;
  margin: 12px auto;
  padding: 0 16px;
}

.activity-location {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 8px 12px;
  margin-bottom: 12px;
  background: #ecf5ff;
  border-radius: 6px;
  font-size: 13px;
}

.coord-info {
  color: #606266;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 12px;
}

.coord-info.gcj {
  color: #909399;
}
</style>
