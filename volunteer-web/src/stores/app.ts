import { ref, computed, readonly } from 'vue'
import { defineStore } from 'pinia'
import { getDeviceInfo, type DeviceInfo } from '@/utils/device'

/**
 * 全局应用状态 — 设备信息、窗口尺寸等跨组件共享状态。
 * 由 App.vue 在 onMounted 时初始化，Layout.vue、Map.vue 等消费。
 */
export const useAppStore = defineStore('app', () => {
  // ──── 设备状态 ──────────────────────────────
  const device = ref<DeviceInfo>(getDeviceInfo())
  const isMobile = computed(() => device.value.isMobile)
  const isPhone = computed(() => device.value.isPhone)
  const isTablet = computed(() => device.value.isTablet)

  /** 刷新设备信息（窗口 resize / orientationchange 时调用） */
  function refreshDevice() {
    device.value = getDeviceInfo()
  }

  return {
    device: readonly(device),
    isMobile,
    isPhone,
    isTablet,
    refreshDevice,
  }
})
