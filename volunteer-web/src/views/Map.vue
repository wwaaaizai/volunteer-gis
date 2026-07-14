<template>
  <div class="map-page">
    <BaseMap ref="baseMapRef" />

    <!-- 活动标注图层（GeoJSON Circle） -->
    <ActivityLayer
      :map="mapInstance"
      :geojson="geojson"
      @feature-click="handleFeatureClick"
    />

    <!-- P2-AM-05：GeoServer WFS 矢量图层（WGS-84 → GCJ-02 变换后零偏移叠加） -->
    <WfsLayer
      v-for="[name, state] in layerState"
      :key="name"
      :map="mapInstance"
      :layer="getLayerMeta(name)"
      :visible="isLayerVisible(name)"
      :fill-opacity="state.opacity"
      :keep-queryable="name === 'ol_campus:jianzhu'"
      @layer-ready="onLayerReady"
      @layer-error="onLayerError"
      @feature-click="onBuildingClick"
    />

    <!-- 底图切换按钮 -->
    <div class="basemap-toggle" @click="toggleBaseMap" :title="basemapLabel">
      <img class="basemap-icon" :src="basemapIcon" alt="" />
    </div>

    <!-- 建筑/运动场 显示开关 -->
    <div class="buildings-toggle" @click="showBuildings = !showBuildings" :title="showBuildings ? '隐藏建筑' : '显示建筑'">
      <img class="buildings-icon" src="/icon/jianzhu.ico" alt="" />
    </div>

    <!-- P2-AM-08：建筑物属性弹窗 -->
    <div class="building-popup" v-if="selectedBuilding" @click="selectedBuilding = null">
      <div class="popup-card" @click.stop>
        <div class="popup-header">
          <strong>建筑物信息</strong>
          <span class="popup-close" @click="selectedBuilding = null">✕</span>
        </div>
        <div class="popup-body">
          <p class="popup-name">{{ selectedBuilding.name }}</p>
        </div>
      </div>
    </div>

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

    <!-- 热力图开关按钮 -->
    <div class="heatmap-toggle" v-if="!heatmapVisible" @click="heatmapVisible = true; loadHeatmap()">
      <span class="ht-icon">&#x1f525;</span>
      <span>活动热力图</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { Map, GeoJSONSource } from 'maplibre-gl'
import BaseMap from '@/components/map/BaseMap.vue'
import ActivityLayer from '@/components/map/ActivityLayer.vue'
import WfsLayer from '@/components/map/WfsLayer.vue'
import { GEOSERVER_LAYERS, type GeoServerLayerMeta, type BaseMapMode } from '@/config/map'
import request from '@/api'
import type { FeatureCollection } from '@/types/geo'

const router = useRouter()
const baseMapRef = ref<InstanceType<typeof BaseMap>>()
const mapInstance = computed<Map | null>(() => baseMapRef.value?.map ?? null)

const geojson = ref<FeatureCollection | null>(null)

// ──── 底图切换 ──────────────────────────────────

const basemapMode = computed<BaseMapMode>(
  () => baseMapRef.value?.currentBaseMap ?? 'standard'
)
const basemapLabel = computed(() => (basemapMode.value === 'standard' ? '切换到卫星底图' : '切换到标准底图'))
const basemapIcon = computed(() =>
  basemapMode.value === 'standard' ? '/icon/Satellitemap.ico' : '/icon/standard.ico'
)

function toggleBaseMap() {
  const next: BaseMapMode = basemapMode.value === 'standard' ? 'satellite' : 'standard'
  baseMapRef.value?.switchBaseMap(next)
}

// ──── GeoServer 图层状态（P2-AM-07）───────────────

interface LayerState {
  visible: boolean
  opacity: number
  loading: boolean
  ready: boolean
  error: string | null
}

const layerMetaMap = new Map<string, GeoServerLayerMeta>(
  GEOSERVER_LAYERS.map((l) => [l.name, l])
)

/** 根据图层名获取元数据（模板中调用） */
function getLayerMeta(name: string): GeoServerLayerMeta {
  return layerMetaMap.get(name)!
}

const layerState = reactive<Map<string, LayerState>>(
  new Map(
    GEOSERVER_LAYERS.map((l) => [
      l.name,
      { visible: l.visible, opacity: 0.45, loading: true, ready: false, error: null },
    ])
  )
)

/** 受建筑开关统一控制的图层 */
const BUILDING_LAYER_NAMES = ['ol_campus:jianzhu', 'ol_campus:yundongchang']

const showBuildings = ref(false)

/** 计算每个图层的有效可见性：建筑/运动场受统一开关控制，其余保持原状态 */
function isLayerVisible(name: string): boolean {
  if (BUILDING_LAYER_NAMES.includes(name)) {
    return showBuildings.value
  }
  const s = layerState.get(name)
  return s?.visible ?? true
}

function onLayerReady(name: string) {
  const s = layerState.get(name)
  if (s) {
    s.ready = true
    s.loading = false
  }
}

function onLayerError(name: string, message: string) {
  const s = layerState.get(name)
  if (s) {
    s.error = message
    s.loading = false
  }
}

// ──── P2-AM-08：建筑物点击弹窗 + 选中高亮 ─────────

const selectedBuilding = ref<Record<string, unknown> | null>(null)

const HIGHLIGHT_SOURCE = 'building-highlight'
const HIGHLIGHT_LAYER = 'building-highlight-line'

function highlightBuilding(geometry: unknown) {
  const map = mapInstance.value
  if (!map) return
  clearHighlight()
  map.addSource(HIGHLIGHT_SOURCE, {
    type: 'geojson',
    data: {
      type: 'FeatureCollection',
      features: [{ type: 'Feature', properties: {}, geometry }],
    } as any,
  })
  map.addLayer({
    id: HIGHLIGHT_LAYER,
    type: 'line',
    source: HIGHLIGHT_SOURCE,
    paint: {
      'line-color': '#2563eb',
      'line-width': 3,
      'line-opacity': 0.95,
    },
  })
}

function clearHighlight() {
  const map = mapInstance.value
  if (!map) return
  try { if (map.getLayer(HIGHLIGHT_LAYER)) map.removeLayer(HIGHLIGHT_LAYER) } catch { /* */ }
  try { if (map.getSource(HIGHLIGHT_SOURCE)) map.removeSource(HIGHLIGHT_SOURCE) } catch { /* */ }
}

function onBuildingClick(_layerName: string, properties: Record<string, unknown>, geometry?: unknown) {
  selectedBuilding.value = properties
  if (geometry) {
    highlightBuilding(geometry)
  }
}

// 弹窗关闭时清除高亮
watch(selectedBuilding, (v) => {
  if (!v) clearHighlight()
})

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

/* ──── 建筑物弹窗 ─────────────────────────────── */
.building-popup {
  position: absolute;
  bottom: 24px;
  left: 12px;
  z-index: 11;
  cursor: pointer;
}

.popup-card {
  background: rgba(255, 255, 255, 0.97);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  padding: 10px 14px;
  max-width: 260px;
  cursor: default;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-size: 14px;
}

.popup-close {
  cursor: pointer;
  color: #909399;
  font-size: 14px;
}

.popup-name {
  font-size: 13px;
  color: #303133;
  margin: 0;
}

/* ──── 热力图（保持原有） ──────────────────────── */
.heatmap-toggle {
  position: absolute;
  bottom: 24px;
  right: 12px;
  z-index: 10;
  background: linear-gradient(135deg, #ff6b35, #f7c948);
  color: #fff;
  padding: 10px 18px;
  border-radius: 24px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 2px 12px rgba(255, 107, 53, 0.4);
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

.heatmap-panel {
  position: absolute;
  bottom: 24px;
  right: 12px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
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

/* ──── 底图切换按钮 ───────────────────────────── */
.basemap-toggle {
  position: absolute;
  top: 145px;
  right: 10px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  width: 30px;
  height: 30px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  box-sizing: border-box;
}
.basemap-toggle:hover {
  background: rgba(255, 255, 255, 1);
}
.basemap-icon {
  width: 18px;
  height: 18px;
  display: block;
}

/* ──── 建筑/运动场 显示开关 ────────────────────── */
.buildings-toggle {
  position: absolute;
  top: 180px;
  right: 10px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  width: 30px;
  height: 30px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  box-sizing: border-box;
}
.buildings-toggle:hover {
  background: rgba(255, 255, 255, 1);
}
.buildings-icon {
  width: 18px;
  height: 18px;
  display: block;
}
</style>
