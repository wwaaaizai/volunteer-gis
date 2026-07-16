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
    /** 移动端模式：不添加内置缩放/指南针/定位控件，由外部自定义按钮控制 */
    mobile?: boolean
  }>(),
  {}
)

/** 对外暴露地图就绪状态与实例，供父组件叠加图层 */
const mapContainer = ref<HTMLElement>()
const { map, mapReady, currentBaseMap, init, switchBaseMap, locate } = useMap(mapContainer, {
  center: props.center,
  zoom: props.zoom,
  mobile: props.mobile,
})

defineExpose({ map, mapReady, currentBaseMap, switchBaseMap, locate })

onMounted(init)
</script>

<style scoped>
.base-map {
  width: 100%;
  height: 100%;
}
</style>
