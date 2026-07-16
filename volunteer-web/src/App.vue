<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()

function onResize() {
  appStore.refreshDevice()
}

onMounted(() => {
  appStore.refreshDevice()
  window.addEventListener('resize', onResize)
  window.addEventListener('orientationchange', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  window.removeEventListener('orientationchange', onResize)
})
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', Arial, sans-serif;
}
#app {
  min-height: 100vh;
}
</style>
