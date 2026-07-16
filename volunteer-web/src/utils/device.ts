/**
 * 设备检测工具 — 通过 UA 和屏幕特征判断当前设备类型。
 * 用于全局移动端适配：Map.vue 控件触控放大、Layout.vue 导航收起、各页面响应式布局。
 */

/** 设备类型 */
export type DeviceType = 'phone' | 'tablet' | 'desktop'

const UA = navigator.userAgent.toLowerCase()

/** 手机品牌/型号特征 */
const PHONE_REGEX = /(iphone|ipod|android.*mobile|blackberry|windows phone|webos)/i
/** 平板特征 */
const TABLET_REGEX = /(ipad|android(?!.*mobile)|tablet|kindle|silk)/i

/** 检测是否为手机 */
export function isPhone(): boolean {
  return PHONE_REGEX.test(UA)
}

/** 检测是否为平板 */
export function isTablet(): boolean {
  return TABLET_REGEX.test(UA)
}

/** 检测是否为移动设备（手机+平板），同时兜底触摸屏+小宽度判定 */
export function isMobile(): boolean {
  if (isPhone() || isTablet()) return true
  // 兜底：如果 UA 没识别出来但有触摸屏且宽度 ≤ 1024，也视为移动端
  const hasTouch = 'ontouchstart' in window || navigator.maxTouchPoints > 0
  const isSmallScreen = window.innerWidth <= 1024
  return hasTouch && isSmallScreen
}

/** 获取当前设备类型 */
export function getDeviceType(): DeviceType {
  if (isPhone()) return 'phone'
  if (isTablet()) return 'tablet'
  return 'desktop'
}

/** 设备信息，供全局组件消费 */
export interface DeviceInfo {
  type: DeviceType
  isPhone: boolean
  isTablet: boolean
  isMobile: boolean
  isDesktop: boolean
  /** 屏幕宽度（响应式实时值） */
  screenWidth: number
  /** 屏幕高度 */
  screenHeight: number
}

/** 快照当前设备信息 */
export function getDeviceInfo(): DeviceInfo {
  const type = getDeviceType()
  return {
    type,
    isPhone: type === 'phone',
    isTablet: type === 'tablet',
    isMobile: type === 'phone' || type === 'tablet',
    isDesktop: type === 'desktop',
    screenWidth: window.innerWidth,
    screenHeight: window.innerHeight,
  }
}
