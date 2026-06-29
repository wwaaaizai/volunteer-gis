<template>
  <div class="map-page">
    <BaseMap ref="baseMapRef" />

    <!-- P2-AM-05：GeoServer WMS 业务图层组 -->
    <GeoServerLayer
      v-for="layerState in geoLayers"
      :key="layerState.def.id"
      :map="mapInstance"
      :layer="layerState.def"
      :visible="layerState.visible"
      :opacity="layerState.opacity"
    />

    <!-- 活动标注点图层 -->
    <ActivityLayer
      :map="mapInstance"
      :geojson="geojson"
      @feature-click="handleFeatureClick"
    />

    <!-- P2-AM-07：图层控制面板 -->
    <LayerControl
      :available-layer-ids="availableLayerIds"
      @layers-change="onLayersChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import BaseMap from '@/components/map/BaseMap.vue'
import ActivityLayer from '@/components/map/ActivityLayer.vue'
import GeoServerLayer from '@/components/map/GeoServerLayer.vue'
import LayerControl from '@/components/map/LayerControl.vue'
import type { LayerState } from '@/components/map/LayerControl.vue'
import request from '@/api'
import type { FeatureCollection } from '@/types/geo'

const router = useRouter()
const baseMapRef = ref<InstanceType<typeof BaseMap>>()
const mapInstance = computed(() => baseMapRef.value?.map ?? null)

const geojson = ref<FeatureCollection | null>(null)
const geoLayers = ref<LayerState[]>([])
const availableLayerIds = ref<string[]>()

// 加载活动 GeoJSON 数据
onMounted(async () => {
  try {
    geojson.value = (await request.get('/map/activities')) as FeatureCollection
  } catch (err) {
    console.error('加载活动数据失败:', err)
  }

  // P2-AM-06：加载可用 GeoServer 图层清单
  try {
    const layers = (await request.get('/map/layers')) as Array<{ id: string }>
    availableLayerIds.value = layers.map(l => l.id)
  } catch {
    // GeoServer 未就绪时静默，地图功能不受影响
    console.warn('GeoServer 图层清单加载失败，仅使用底图+活动标注')
  }
})

/** 图层控制面板变更回调（P2-AM-07） */
function onLayersChange(states: LayerState[]) {
  geoLayers.value = states
}

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
</style>
