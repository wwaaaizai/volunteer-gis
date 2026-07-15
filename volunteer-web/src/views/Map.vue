<template>
  <div class="map-page">
    <BaseMap ref="baseMapRef" />
    <ActivityLayer
      :map="mapInstance"
      :geojson="geojson"
      @feature-click="handleFeatureClick"
    />

    <!-- 热力图控制面板 -->
    <div class="heatmap-panel" v-if="heatmapVisible">
      <div class="heatmap-header">
        <span>活动热力图</span>
        <el-button size="small" text @click="heatmapVisible = false">✕</el-button>
      </div>
      <div class="heatmap-controls">
        <el-select v-model="heatmapCategory" placeholder="全部分类" size="small" clearable
          @change="loadHeatmap">
          <el-option label="环保" value="environmental" />
          <el-option label="助学" value="support" />
          <el-option label="支教" value="education" />
          <el-option label="社区" value="community" />
          <el-option label="校园" value="campus" />
        </el-select>
        <el-select v-model="heatmapMonths" size="small" @change="loadHeatmap">
          <el-option label="近3个月" :value="3" />
          <el-option label="近6个月" :value="6" />
          <el-option label="近12个月" :value="12" />
        </el-select>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="map-tools">
      <div class="heatmap-toggle" v-if="!heatmapVisible" @click="heatmapVisible = true; loadHeatmap()">
        <span class="ht-icon">🔥</span>
        <span>热力图</span>
      </div>
      <div class="gis-tool-btn" @click="$router.push('/gis-analysis')">
        <span class="ht-icon">📊</span>
        <span>GIS分析</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import BaseMap from '@/components/map/BaseMap.vue'
import ActivityLayer from '@/components/map/ActivityLayer.vue'
import request from '@/api'
import type { FeatureCollection } from '@/types/geo'

const router = useRouter()
const baseMapRef = ref<InstanceType<typeof BaseMap>>()
const mapInstance = computed(() => baseMapRef.value?.map ?? null)

const geojson = ref<FeatureCollection | null>(null)

// ──── 热力图 ────────────────────────────────────

const HEATMAP_SOURCE = 'heatmap-signups'
const HEATMAP_LAYER = 'heatmap-layer'

const heatmapVisible = ref(false)
const heatmapCategory = ref('')
const heatmapMonths = ref(6)

async function loadHeatmap() {
  if (!heatmapVisible.value) return
  try {
    const params: any = { months: heatmapMonths.value }
    if (heatmapCategory.value) params.category = heatmapCategory.value
    const data = (await request.get('/map/heatmap', { params })) as FeatureCollection
    addHeatmapLayer(data)
  } catch (err) {
    console.warn('热力图数据加载失败:', err)
  }
}

function addHeatmapLayer(data: FeatureCollection) {
  const map = mapInstance.value
  if (!map) return

  // 移除旧图层
  try { if (map.getLayer(HEATMAP_LAYER)) map.removeLayer(HEATMAP_LAYER) } catch { /* */ }
  try { if (map.getSource(HEATMAP_SOURCE)) map.removeSource(HEATMAP_SOURCE) } catch { /* */ }

  if (!data?.features?.length) return

  map.addSource(HEATMAP_SOURCE, { type: 'geojson', data: data as any })

  map.addLayer({
    id: HEATMAP_LAYER,
    type: 'heatmap',
    source: HEATMAP_SOURCE,
    paint: {
      'heatmap-weight': ['get', 'weight'],
      'heatmap-intensity': 1.5,
      'heatmap-color': [
        'interpolate', ['linear'], ['heatmap-density'],
        0, 'rgba(33,102,172,0)',
        0.2, 'rgb(103,169,207)',
        0.4, 'rgb(209,229,240)',
        0.6, 'rgb(253,219,199)',
        0.8, 'rgb(239,138,98)',
        1.0, 'rgb(178,24,43)',
      ],
      'heatmap-radius': 50,
      'heatmap-opacity': 0.8,
    },
  })
}

// 热力图显隐切换
watch(heatmapVisible, (v) => {
  if (!v) {
    const map = mapInstance.value
    if (!map) return
    try { if (map.getLayer(HEATMAP_LAYER)) map.removeLayer(HEATMAP_LAYER) } catch { /* */ }
    try { if (map.getSource(HEATMAP_SOURCE)) map.removeSource(HEATMAP_SOURCE) } catch { /* */ }
  }
})

// ──── 活动数据 ──────────────────────────────────

onMounted(async () => {
  try {
    geojson.value = (await request.get('/map/activities')) as FeatureCollection
  } catch (err) {
    console.error('加载活动数据失败:', err)
  }
})

function handleFeatureClick(id: number) {
  router.push(`/activity/${id}`)
}
</script>

<style scoped>
.map-page {
  position: relative;
  width: 100%;
  height: calc(100vh - 60px);
}

.map-tools {
  position: absolute;
  bottom: 24px;
  right: 12px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.heatmap-toggle, .gis-tool-btn {
  background: linear-gradient(135deg, #ff6b35, #f7c948);
  background: linear-gradient(135deg, #ff6b35, #f7c948);
  color: #fff;
  padding: 10px 18px;
  border-radius: 24px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 2px 12px rgba(255,107,53,0.4);
  display: flex;
  align-items: center;
  gap: 6px;
  user-select: none;
  transition: transform 0.15s;
}
.heatmap-toggle:hover {
  transform: scale(1.05);
}
.ht-icon {
  font-size: 18px;
}

.gis-tool-btn {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}
.heatmap-panel {
  position: absolute;
  bottom: 24px;
  right: 12px;
  z-index: 10;
  background: rgba(255,255,255,0.95);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  padding: 10px 12px;
  min-width: 180px;
}

.heatmap-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
  color: #303133;
}

.heatmap-controls {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.heatmap-controls :deep(.el-select) {
  width: 100%;
}
</style>
