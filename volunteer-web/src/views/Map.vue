<template>
  <div class="map-page" :class="{ 'map-page--mobile': appStore.isMobile }">
    <BaseMap ref="baseMapRef" :mobile="appStore.isMobile" />

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
      :keep-queryable="name === 'ol_campus:jianzhu' || name === 'ol_campus:yundongchang'"
      @layer-ready="onLayerReady"
      @layer-error="onLayerError"
      @feature-click="onBuildingClick"
    />

    <!-- 底图切换按钮（桌面端） -->
    <div v-if="!appStore.isMobile" class="basemap-toggle" @click="toggleBaseMap" :title="basemapLabel">
      <img class="basemap-icon" :src="basemapIcon" alt="" />
    </div>

    <!-- 建筑/运动场 显示开关（桌面端） -->
    <div v-if="!appStore.isMobile" class="buildings-toggle" @click="showBuildings = !showBuildings" :title="showBuildings ? '隐藏建筑' : '显示建筑'">
      <img class="buildings-icon" src="/icon/jianzhu.ico" alt="" />
    </div>

    <!-- 移动端：右上角四按钮控制条 -->
    <div v-if="appStore.isMobile" class="mobile-controls">
      <!-- 扫一扫 -->
      <div class="mc-btn" @click="goToScan" title="扫一扫">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
      <!-- 正方形外框，四边中间断开约 1/3 -->
         <path d="M4 4 h5.33 M14.67 4 H20 M20 4 v5.33 M20 14.67 V20 M20 20 h-5.33 M9.33 20 H4 M4 20 v-5.33 M4 4 v5.33" />
      <!-- 中间横线，约 2/3 边长 -->
         <path d="M6.67 12 h10.67" />
        </svg>
      </div>
      <!-- 图层切换 -->
      <div class="mc-btn" @click="toggleBaseMap" :title="basemapLabel">
        <img class="mc-icon-img" :src="basemapIcon" alt="" />
      </div>
      <!-- 建筑显示 -->
      <div class="mc-btn" :class="{ active: showBuildings }" @click="showBuildings = !showBuildings" title="显示建筑">
        <img class="mc-icon-img" src="/icon/jianzhu.ico" alt="" />
      </div>
      <!-- 定位 -->
      <div class="mc-btn" @click="handleLocate" title="定位">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="3" />
          <path d="M12 2v4M12 18v4M2 12h4M18 12h4" />
        </svg>
      </div>
    </div>

    <!-- 底部上拉背板：搜索 / 活动详情 / 建筑信息 -->
    <div
      class="activity-drawer"
      :class="{
        'activity-drawer--peek': drawerMode === 'peek',
        'activity-drawer--expanded': drawerMode === 'expanded',
        'activity-drawer--mobile': appStore.isMobile,
      }"
    >
      <!-- 拖拽手柄 + 收起态搜索栏 -->
      <div class="drawer-handle-area" ref="handleRef">
        <div class="drag-handle"></div>
      </div>

      <!-- 收起态：搜索框 -->
      <div class="drawer-body" :class="{ 'drawer-body--scrollable': drawerMode !== 'collapsed' }"
        v-if="drawerMode === 'collapsed' || drawerContent.type === 'search'">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索活动..."
          clearable
          @input="onSearchInput"
          @clear="clearSearch"
          @focus="onSearchFocus"
        >
          <template #prefix>
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
            </svg>
          </template>
        </el-input>
      </div>

      <!-- 活动详情（peek / expanded） -->
      <div class="drawer-body drawer-body--scrollable"
        v-if="drawerContent.type === 'activity' && drawerMode !== 'collapsed'">
        <!-- 拖拽手柄内嵌关闭按钮 -->
        <div class="drawer-section">
          <div class="drawer-title-row">
            <h3 class="drawer-title">{{ drawerContent.activity?.title }}</h3>
            <el-tag v-if="statusTag" :type="statusTagType" size="small">{{ statusTag }}</el-tag>
          </div>
        </div>
        <div class="drawer-section drawer-meta">
          <div class="meta-item">
            <span class="meta-label">地点</span>
            <span class="meta-value">{{ drawerContent.activity?.locationName }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">时间</span>
            <span class="meta-value">{{ formatTime(drawerContent.activity?.startTime) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">人数</span>
            <span class="meta-value">{{ drawerContent.activity?.signedCount ?? 0 }} / {{ drawerContent.activity?.maxParticipants ?? 0 }}</span>
          </div>
        </div>
        <div class="drawer-section" v-if="drawerContent.activity?.description">
          <p class="drawer-desc" :class="{ 'drawer-desc--clamped': drawerMode === 'peek' }">
            {{ drawerContent.activity?.description }}
          </p>
        </div>
        <div class="drawer-section drawer-actions">
          <el-button type="primary" size="large" class="signup-btn"
            :disabled="!canSignupDrawer" :loading="signing"
            @click="handleDrawerSignup">
            {{ signupBtnTextDrawer }}
          </el-button>
          <el-button v-if="drawerMode === 'peek'" text type="primary" size="small"
            @click="drawerMode = 'expanded'">
            查看更多
          </el-button>
        </div>
      </div>

      <!-- 建筑信息（peek） -->
      <div class="drawer-body drawer-body--scrollable"
        v-if="drawerContent.type === 'building' && drawerMode !== 'collapsed'">
        <div class="drawer-section">
          <div class="drawer-title-row">
            <h3 class="drawer-title">建筑信息</h3>
          </div>
        </div>
        <div class="drawer-section drawer-props">
          <p class="building-name-text">{{ buildingName || '未知建筑' }}</p>
        </div>
      </div>
    </div>

    <!-- 热力图控制面板 -->
    <div class="heatmap-panel" :class="{ 'heatmap-panel--mobile': appStore.isMobile }" v-if="heatmapVisible">
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
    <div class="heatmap-toggle" :class="{ 'heatmap-toggle--mobile': appStore.isMobile }"
      v-if="!heatmapVisible" @click="heatmapVisible = true; loadHeatmap()">
      <span class="ht-icon">&#x1f525;</span>
      <span>活动热力图</span>
    </div>

    <!-- GIS分析按钮（置底，热力图上方） -->
    <div class="gis-tool-btn" :class="{ 'gis-tool-btn--mobile': appStore.isMobile }"
      @click="$router.push('/gis-analysis')">
      <span class="ht-icon">&#x1f4ca;</span>
      <span>GIS分析</span>
    </div>

    <!-- 角色图例：学生/组织者高亮说明 -->
    <div v-if="showLegend" class="map-legend" :class="{ 'map-legend--mobile': appStore.isMobile }">
      <div class="legend-item">
        <span class="legend-dot legend-dot--default"></span> 所有活动
      </div>
      <div class="legend-item">
        <span class="legend-dot" :class="legendHighlightClass"></span>
        {{ legendHighlightLabel }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { Map, GeoJSONSource } from 'maplibre-gl'
import BaseMap from '@/components/map/BaseMap.vue'
import ActivityLayer from '@/components/map/ActivityLayer.vue'
import WfsLayer from '@/components/map/WfsLayer.vue'
import { GEOSERVER_LAYERS, type GeoServerLayerMeta, type BaseMapMode } from '@/config/map'
import request from '@/api'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import type { FeatureCollection } from '@/types/geo'

const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

// ──── 图例 ──────────────────────────────────────

const showLegend = computed(() => LEGEND_SHOW_ROLES.includes(userStore.user?.role || ''))

const legendHighlightClass = computed(() =>
  userStore.user?.role === 'student' ? 'legend-dot--student' : 'legend-dot--organizer'
)

const legendHighlightLabel = computed(() =>
  userStore.user?.role === 'student' ? '我的报名' : '我的活动'
)

const baseMapRef = ref<InstanceType<typeof BaseMap>>()
const mapInstance = computed<Map | null>(() => baseMapRef.value?.map ?? null)

const geojson = ref<FeatureCollection | null>(null)

/** 原始 GeoJSON 缓存（用于搜索过滤后恢复） */
const geojsonOriginal = ref<FeatureCollection | null>(null)

// ──── 底部上拉背板状态 ────────────────────────────

type DrawerMode = 'collapsed' | 'peek' | 'expanded'

type DrawerContent =
  | { type: 'search' }
  | { type: 'activity'; activity: Record<string, unknown> | null; loading: boolean }
  | { type: 'building'; properties: Record<string, unknown> }

const drawerMode = ref<DrawerMode>('collapsed')
const drawerContent = ref<DrawerContent>({ type: 'search' })
const searchKeyword = ref('')
const signing = ref(false)

/** 注册 tap 在地图上的点击清除背板——监听 map click 事件 */
function registerMapDismiss() {
  const m = mapInstance.value
  if (!m) return

  m.on('click', (e) => {
    // 如果点击目标是 activity-markers 或 wfs fill layer，跳过
    const features = m.queryRenderedFeatures(e.point, {
      layers: [
        'activity-markers',
        'role-highlights-circle',
        'wfs-ol_campus-jianzhu-fill',
        'wfs-ol_campus-yundongchang-fill',
        'wfs-ol_campus-bianjie-line',
      ],
    })
    if (features.length > 0) return

    // 点击空白区域 → 收起背板
    drawerMode.value = 'collapsed'
    drawerContent.value = { type: 'search' }
    // 清除建筑高亮
    if (selectedBuilding.value) {
      selectedBuilding.value = null
    }
    // 恢复搜索过滤
    restoreGeoJSON()
  })
}

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

/** 移动端：打开扫一扫页面 */
function goToScan() {
  router.push('/scan')
}

/** 移动端：手动定位 */
function handleLocate() {
  baseMapRef.value?.locate?.()
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

// ──── P2-AM-08：建筑物点击 → 统一背板展示 + 选中高亮 ─────────

const selectedBuilding = ref<Record<string, unknown> | null>(null)

const selectedLayerName = ref<string>('')

/** 从 WFS 属性中提取名称：jianzhu 取 Entity 字段，yundongchang 取 name 字段，后续可扩展为后端信息库映射 */
const buildingName = computed(() => {
  if (!selectedBuilding.value) return ''
  const field = selectedLayerName.value === 'ol_campus:yundongchang' ? 'name' : 'Entity'
  const val = selectedBuilding.value[field]
  return val != null ? String(val) : ''
})

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

function onBuildingClick(layerName: string, properties: Record<string, unknown>, geometry?: unknown) {
  selectedLayerName.value = layerName
  selectedBuilding.value = properties
  drawerContent.value = { type: 'building', properties }
  drawerMode.value = 'peek'
  if (geometry) {
    highlightBuilding(geometry)
  }
}

// 弹窗关闭时清除高亮
watch(selectedBuilding, (v) => {
  if (!v) {
    clearHighlight()
    // 如果当前背板显示建筑信息且 building 被清空，回到搜索态
    if (drawerContent.value.type === 'building') {
      drawerContent.value = { type: 'search' }
    }
  }
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

// ──── 角色感知高亮层 ─────────────────────────────

const HIGHLIGHT_SOURCE_ID = 'role-highlights'
const HIGHLIGHT_LAYER_ID = 'role-highlights-circle'
const LEGEND_SHOW_ROLES = ['student', 'organizer']

let myHighlightIds = new Set<number>()

/**
 * 学生：高亮已报名活动（绿色圆圈）
 * 组织者：高亮自己创建的活动（橙色圆圈）
 */
async function loadRoleHighlights() {
  const role = userStore.user?.role
  if (!role || !LEGEND_SHOW_ROLES.includes(role)) return

  try {
    let myIds: number[] = []
    if (role === 'student') {
      const mySignups = await request.get('/signups/my') as any[]
      myIds = mySignups.map((s: any) => s.activityId)
    } else if (role === 'organizer') {
      const myActivities = await request.get('/activities/my', { params: { status: 'published' } }) as any[]
      myIds = myActivities.map((a: any) => a.id)
    }
    myHighlightIds = new Set(myIds)
    addRoleHighlightLayer()
  } catch {
    // 静默失败
  }
}

function addRoleHighlightLayer() {
  const map = mapInstance.value
  if (!map || myHighlightIds.size === 0) return

  // 在 GeoJSON 功能件中标记 isMyItem
  const features = geojson.value?.features
  if (features) {
    for (const f of features) {
      (f.properties as any).isMyItem = myHighlightIds.has(f.properties.id)
    }
    // 更新 source 数据以触发重渲染
    const source = map.getSource('activities') as GeoJSONSource | undefined
    if (source) {
      source.setData(geojson.value as any)
    }
  }

  const role = userStore.user?.role
  const color = role === 'student' ? '#67c23a' : '#e6a23c'
  const strokeColor = role === 'student' ? '#ffffff' : '#ffffff'

  try { if (map.getLayer(HIGHLIGHT_LAYER_ID)) map.removeLayer(HIGHLIGHT_LAYER_ID) } catch { /* */ }
  try { if (map.getSource(HIGHLIGHT_SOURCE_ID)) map.removeSource(HIGHLIGHT_SOURCE_ID) } catch { /* */ }

  // 使用独立的 source 创建高亮层（避免 filter 在同一个 source 上的复杂性）
  const highlightFeatures = (geojson.value?.features || [])
    .filter(f => myHighlightIds.has((f.properties as any).id))
    .map(f => ({ type: 'Feature', properties: {}, geometry: f.geometry }))

  if (highlightFeatures.length === 0) return

  map.addSource(HIGHLIGHT_SOURCE_ID, {
    type: 'geojson',
    data: { type: 'FeatureCollection', features: highlightFeatures } as any,
  })

  map.addLayer({
    id: HIGHLIGHT_LAYER_ID,
    type: 'circle',
    source: HIGHLIGHT_SOURCE_ID,
    paint: {
      'circle-radius': 10,
      'circle-color': color,
      'circle-stroke-width': 3,
      'circle-stroke-color': strokeColor,
      'circle-opacity': 0.85,
    },
  })
}

// ──── 活动数据 ──────────────────────────────────

onMounted(async () => {
  try {
    geojson.value = (await request.get('/map/activities')) as FeatureCollection
    // 缓存原始数据供搜索过滤恢复
    geojsonOriginal.value = JSON.parse(JSON.stringify(geojson.value))
    // 角色高亮：在地图就绪后额外标记
    await loadRoleHighlights()
  } catch (err) {
    console.error('加载活动数据失败:', err)
  }
})

/** 关注 map 就绪后注册点击背景收起背板 */
watch(mapInstance, (m) => {
  if (m) registerMapDismiss()
})

// ──── 活动详情获取 & 背板 ─────────────────────────

/** 活动详情数据 */
const selectedActivity = ref<Record<string, unknown> | null>(null)

/** 点击活动标记 → 拉取详情 → 打开 peek 面板 */
async function handleFeatureClick(id: number) {
  drawerContent.value = { type: 'activity', activity: null, loading: true }
  drawerMode.value = 'peek'
  try {
    const detail = await request.get(`/activities/${id}`) as Record<string, unknown>
    selectedActivity.value = detail
    drawerContent.value = { type: 'activity', activity: detail, loading: false }
  } catch {
    drawerContent.value = { type: 'search' }
    drawerMode.value = 'collapsed'
    ElMessage.error('获取活动详情失败')
  }
}

/** 状态 Tag */
const statusTag = computed(() => {
  const s = selectedActivity.value?.status
  return s ? { draft: '草稿', published: '报名中', ongoing: '进行中', ended: '已结束', cancelled: '已取消' }[s as string] || s : ''
})

const statusTagType = computed(() => {
  const s = selectedActivity.value?.status
  if (s === 'published') return 'success'
  if (s === 'ongoing') return ''
  if (s === 'ended' || s === 'cancelled') return 'info'
  return 'info'
})

const canSignupDrawer = computed(() => {
  if (!selectedActivity.value) return false
  return selectedActivity.value.status === 'published' &&
    (selectedActivity.value.signedCount as number) < (selectedActivity.value.maxParticipants as number)
})

const signupBtnTextDrawer = computed(() => {
  if (!selectedActivity.value) return '加载中'
  if (selectedActivity.value.status !== 'published') return '不在报名期'
  if ((selectedActivity.value.signedCount as number) >= (selectedActivity.value.maxParticipants as number)) return '名额已满'
  return '立即报名'
})

/** 背板内报名 */
async function handleDrawerSignup() {
  if (!selectedActivity.value) return
  signing.value = true
  try {
    await request.post('/signups', null, { params: { activityId: selectedActivity.value.id } })
    ElMessage.success('报名成功')
    // 刷新数据
    const detail = await request.get(`/activities/${(selectedActivity.value as any).id}`) as Record<string, unknown>
    selectedActivity.value = detail
    drawerContent.value = { type: 'activity', activity: detail, loading: false }
    // 刷新角色高亮
    await loadRoleHighlights()
  } catch {
    // 错误已在拦截器处理
  } finally {
    signing.value = false
  }
}

/** 格式化时间 */
function formatTime(t: unknown): string {
  if (!t) return '-'
  const s = String(t)
  return s.replace('T', ' ').substring(0, 16)
}

// ──── 搜索过滤 ───────────────────────────────────

let searchTimer: ReturnType<typeof setTimeout> | null = null

function onSearchInput(keyword: string) {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    applySearchFilter(keyword)
  }, 250)
}

function applySearchFilter(keyword: string) {
  const map = mapInstance.value
  const src = map?.getSource('activities') as GeoJSONSource | undefined
  if (!src || !geojsonOriginal.value) return

  if (!keyword.trim()) {
    restoreGeoJSON()
    return
  }

  const kw = keyword.trim().toLowerCase()
  const filtered = {
    ...geojsonOriginal.value,
    features: geojsonOriginal.value.features.filter((f) =>
      f.properties.title.toLowerCase().includes(kw)
    ),
  }
  geojson.value = filtered
  src.setData(filtered as any)
}

function restoreGeoJSON() {
  const map = mapInstance.value
  const src = map?.getSource('activities') as GeoJSONSource | undefined
  if (!src || !geojsonOriginal.value) return
  geojson.value = geojsonOriginal.value
  src.setData(geojsonOriginal.value as any)
}

function clearSearch() {
  searchKeyword.value = ''
  restoreGeoJSON()
}

function onSearchFocus() {
  // 聚焦搜索框时若背板收起则不变；保持当前状态
}

// ──── 拖拽手势 ───────────────────────────────────

const handleRef = ref<HTMLElement | null>(null)

let dragStartY = 0
let dragMoveY = 0
let dragging = false

function onDragStart(e: PointerEvent) {
  dragging = true
  dragStartY = e.clientY
  dragMoveY = 0
  const el = e.currentTarget as HTMLElement
  el.setPointerCapture(e.pointerId)
}

function onDragMove(e: PointerEvent) {
  if (!dragging) return
  dragMoveY = e.clientY - dragStartY
}

function onDragEnd(e: PointerEvent) {
  if (!dragging) return
  dragging = false
  const el = e.currentTarget as HTMLElement
  el.releasePointerCapture(e.pointerId)

  const threshold = 50
  if (dragMoveY > threshold) {
    // 向下拖 → 状态降级
    if (drawerMode.value === 'expanded') {
      drawerMode.value = 'peek'
    } else if (drawerMode.value === 'peek') {
      drawerMode.value = 'collapsed'
      drawerContent.value = { type: 'search' }
      restoreGeoJSON()
    }
  } else if (dragMoveY < -threshold && drawerMode.value === 'collapsed') {
    // 向上拖 → 不自动打开（需要点击活动标记激活）
  }
}

function setupDragListeners() {
  const el = handleRef.value
  if (!el) return
  el.addEventListener('pointerdown', onDragStart as EventListener)
  el.addEventListener('pointermove', onDragMove as EventListener)
  el.addEventListener('pointerup', onDragEnd as EventListener)
  el.addEventListener('pointercancel', onDragEnd as EventListener)
}

function teardownDragListeners() {
  const el = handleRef.value
  if (!el) return
  el.removeEventListener('pointerdown', onDragStart as EventListener)
  el.removeEventListener('pointermove', onDragMove as EventListener)
  el.removeEventListener('pointerup', onDragEnd as EventListener)
  el.removeEventListener('pointercancel', onDragEnd as EventListener)
}

// 拖拽手柄引用变化时绑定事件
watch(handleRef, (el, old) => {
  if (old) teardownDragListeners()
  if (el) setupDragListeners()
})

onUnmounted(() => {
  teardownDragListeners()
})
</script>

<style scoped>
.map-page {
  position: relative;
  width: 100%;
  height: calc(100vh - 60px);
}
/* 移动端 flex 布局：填满父容器高度 */
.map-page--mobile {
  height: 100%;
}

/* ──── 底部上拉背板 ──────────────────────────── */
.activity-drawer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: #fff;
  border-radius: 16px 16px 0 0;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.12);
  transform: translateY(calc(100% - 56px));
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  /* 桌面端：限制宽度避免横跨整个屏幕 */
  max-width: 420px;
  margin: 0 auto;
}

.activity-drawer--peek {
  transform: translateY(25%);
}

.activity-drawer--expanded {
  transform: translateY(10%);
}

/* 移动端：全宽 */
.activity-drawer--mobile {
  max-width: 100%;
  border-radius: 16px 16px 0 0;
}

/* 拖拽手柄区域 */
.drawer-handle-area {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px 0 4px;
  touch-action: none;
  cursor: grab;
  user-select: none;
  -webkit-user-select: none;
  flex-shrink: 0;
}
.drawer-handle-area:active {
  cursor: grabbing;
}

.drag-handle {
  width: 40px;
  height: 4px;
  border-radius: 2px;
  background: #c0c4cc;
}

/* 面板主体 */
.drawer-body {
  padding: 0 16px 12px;
  flex-shrink: 0;
}
.drawer-body--scrollable {
  flex: 1;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-bottom: calc(12px + env(safe-area-inset-bottom, 0px));
}

/* ──── 活动详情面板 ──────────────────────────── */
.drawer-section {
  margin-bottom: 8px;
}

.drawer-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.drawer-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
  flex: 1;
}

.drawer-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 0;
  border-top: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.meta-label {
  color: #909399;
  flex-shrink: 0;
  width: 36px;
}

.meta-value {
  color: #303133;
}

.drawer-desc {
  margin: 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
}

.drawer-desc--clamped {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.drawer-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding-top: 4px;
}

.signup-btn {
  width: 100%;
}

/* ──── 建筑属性面板 ─────────────────────────── */
.drawer-props {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 0;
  border-top: 1px solid #ebeef5;
}

.prop-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 13px;
  padding: 4px 0;
}

.prop-label {
  color: #909399;
  flex-shrink: 0;
  min-width: 60px;
}

.prop-value {
  color: #303133;
  word-break: break-all;
}

.building-name-text {
  color: #303133;
  font-size: 15px;
  font-weight: 500;
  margin: 0;
}

/* ──── 热力图 ──────────────────────────────────── */
.heatmap-toggle {
  position: absolute;
  bottom: 72px;
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
/* 移动端：放大按钮保证触控面积 */
.heatmap-toggle--mobile {
  bottom: 60px;
  right: 8px;
  padding: 12px 20px;
  font-size: 13px;
  border-radius: 22px;
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

/* ──── GIS分析按钮（热力图按钮下方，不重叠）─── */
.gis-tool-btn {
  position: absolute;
  bottom: 24px;
  right: 12px;
  z-index: 10;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: #fff;
  padding: 8px 16px;
  border-radius: 24px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.35);
  display: flex;
  align-items: center;
  gap: 5px;
  user-select: none;
  transition: transform 0.15s;
}
.gis-tool-btn:hover { transform: scale(1.05); }
.gis-tool-btn--mobile {
  bottom: 12px;
  right: 8px;
  padding: 6px 12px;
  font-size: 11px;
}
/* 移动端：面板靠左、铺满宽度 */
.heatmap-panel--mobile {
  bottom: 8px;
  left: 8px;
  right: 8px;
  min-width: unset;
  padding: 12px;
  border-radius: 12px 12px 0 0;
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

/* ──── 建筑/运动场 显示开关（桌面端）─────────────── */
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

/* ──── 移动端右上角控制条 ──────────────────────── */
.mobile-controls {
  position: absolute;
  top: calc(8px + env(safe-area-inset-top, 0px));
  right: 8px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.mc-btn {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #303133;
  user-select: none;
  box-sizing: border-box;
  -webkit-tap-highlight-color: transparent;
}
.mc-btn:active {
  background: rgba(0, 0, 0, 0.06);
}
.mc-btn.active {
  background: rgba(64, 158, 255, 0.15);
  color: #409eff;
}

.mc-icon-img {
  width: 22px;
  height: 22px;
  display: block;
}

/* ──── 图例面板 ────────────────────────────────── */
.map-legend {
  position: absolute;
  bottom: 72px;
  left: 12px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.93);
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 12px;
  color: #606266;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.1);
}
.map-legend--mobile {
  bottom: 60px;
  left: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.8;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.legend-dot--default {
  background: #409eff;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px #409eff;
}
.legend-dot--student {
  background: #67c23a;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px #67c23a;
}
.legend-dot--organizer {
  background: #e6a23c;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px #e6a23c;
}
</style>
