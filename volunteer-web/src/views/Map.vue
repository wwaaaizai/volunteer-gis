<template>
  <div class="map-page">
    <!-- 加载/错误状态浮层 -->
    <div v-if="loading" class="map-overlay">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载活动数据...</span>
    </div>
    <div v-else-if="error" class="map-overlay map-error">
      <span>{{ error }}</span>
      <el-button size="small" @click="loadActivities">重试</el-button>
    </div>

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
import { Loading } from '@element-plus/icons-vue'
import BaseMap from '@/components/map/BaseMap.vue'
import ActivityLayer from '@/components/map/ActivityLayer.vue'
import request from '@/api'
import type { FeatureCollection } from '@/types/geo'

const router = useRouter()
const baseMapRef = ref<InstanceType<typeof BaseMap>>()
// BaseMap 通过 defineExpose 暴露 map 实例（shallowRef）
const mapInstance = computed(() => baseMapRef.value?.map ?? null)

const geojson = ref<FeatureCollection | null>(null)
const loading = ref(false)
const error = ref('')

async function loadActivities() {
  loading.value = true
  error.value = ''
  try {
    geojson.value = await request.get('/map/activities') as FeatureCollection
  } catch (err: any) {
    error.value = err?.message || '加载活动数据失败，请检查网络连接'
    console.error('加载活动数据失败:', err)
  } finally {
    loading.value = false
  }
}

function handleFeatureClick(id: number) {
  router.push(`/activity/${id}`)
}

// onMounted 内异步加载，不阻塞组件渲染与路由导航
onMounted(() => {
  loadActivities()
})
</script>

<style scoped>
.map-page {
  position: relative;
  width: 100%;
  height: calc(100vh - 60px);
}
.map-overlay {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.92);
  padding: 8px 16px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  font-size: 14px;
}
.map-error {
  color: #f56c6c;
}
</style>
