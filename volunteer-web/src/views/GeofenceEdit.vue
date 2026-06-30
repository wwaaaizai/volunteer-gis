<template>
  <div class="geofence-edit-page">
    <el-button text @click="$router.back()" style="margin-bottom:8px">← 返回</el-button>
    <el-card>
      <template #header>
        <span>签到围栏设置 — {{ activityTitle }}</span>
      </template>
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
import BaseMap from '@/components/map/BaseMap.vue'
import GeofenceEditor from '@/components/map/GeofenceEditor.vue'
import request from '@/api'

const route = useRoute()
const activityId = Number(route.params.id)

const baseMapRef = ref<InstanceType<typeof BaseMap>>()
const mapInstance = computed(() => baseMapRef.value?.map ?? null)
const activityTitle = ref('')

onMounted(async () => {
  try {
    const data: any = await request.get(`/activities/${activityId}`)
    activityTitle.value = data?.title || `活动 #${activityId}`
  } catch { activityTitle.value = `活动 #${activityId}` }
})
</script>

<style scoped>
.geofence-edit-page {
  max-width: 900px;
  margin: 12px auto;
  padding: 0 16px;
}
</style>
