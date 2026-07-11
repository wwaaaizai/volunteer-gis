<template>
  <div class="map-page">
    <BaseMap ref="baseMapRef" />
    <ActivityLayer
      :map="mapInstance"
      :geojson="geojson"
      @feature-click="handleFeatureClick"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import BaseMap from '@/components/map/BaseMap.vue'
import ActivityLayer from '@/components/map/ActivityLayer.vue'
import request from '@/api'
import type { FeatureCollection } from '@/types/geo'

const router = useRouter()
const baseMapRef = ref<InstanceType<typeof BaseMap>>()
// BaseMap 通过 defineExpose 暴露 map 实例（shallowRef）
const mapInstance = computed(() => baseMapRef.value?.map ?? null)

const geojson = ref<FeatureCollection | null>(null)

// 加载活动 GeoJSON 数据
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
</style>
