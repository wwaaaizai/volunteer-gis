<template>
  <div ref="mapContainer" class="base-map"></div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useMap } from '@/composables/useMap'

const props = withDefaults(
  defineProps<{
    center?: [number, number]
    zoom?: number
  }>(),
  {}
)

/** 对外暴露地图就绪状态与实例，供父组件叠加图层 */
const mapContainer = ref<HTMLElement>()
const { map, mapReady, init } = useMap(mapContainer, {
  center: props.center,
  zoom: props.zoom,
})

defineExpose({ map, mapReady })

onMounted(init)
</script>

<style scoped>
.base-map {
  width: 100%;
  height: 100%;
}
</style>
