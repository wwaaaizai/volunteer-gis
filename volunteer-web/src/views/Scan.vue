<template>
  <div class="scan-page">
    <!-- 顶部导航 -->
    <div class="scan-header">
      <el-button text @click="goBack">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </el-button>
      <span class="scan-title">扫一扫</span>
      <div style="width: 32px;"></div>
    </div>

    <!-- 摄像头预览区域 -->
    <div class="scan-preview">
      <video ref="videoRef" autoplay playsinline class="scan-video"></video>

      <!-- 扫描框 -->
      <div class="scan-frame">
        <div class="scan-corner tl"></div>
        <div class="scan-corner tr"></div>
        <div class="scan-corner bl"></div>
        <div class="scan-corner br"></div>
      </div>

      <!-- 未检测到摄像头时的文件选择兜底 -->
      <div v-if="!cameraReady && !cameraError" class="scan-loading">
        <span>正在启动摄像头...</span>
      </div>
      <div v-if="cameraError" class="scan-error">
        <p>{{ cameraError }}</p>
        <el-button type="primary" @click="openFilePicker">从相册选择二维码图片</el-button>
      </div>
    </div>

    <!-- 底部提示 -->
    <div class="scan-footer">
      <p>将二维码放入框内，即可自动识别</p>
      <el-button text type="primary" @click="openFilePicker">从相册选择</el-button>
    </div>

    <!-- 隐藏的文件选择器 -->
    <input
      ref="fileInputRef"
      type="file"
      accept="image/*"
      style="display:none"
      @change="onFileSelected"
    />

    <!-- 扫码结果弹窗 -->
    <el-dialog v-model="resultVisible" title="识别结果" width="300px" :close-on-click-modal="false">
      <div class="scan-result">
        <p class="result-text">{{ scanResult }}</p>
      </div>
      <template #footer>
        <el-button @click="resultVisible = false; startScan()">继续扫描</el-button>
        <el-button type="primary" @click="processResult">处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const videoRef = ref<HTMLVideoElement>()
const fileInputRef = ref<HTMLInputElement>()
const cameraReady = ref(false)
const cameraError = ref('')
const scanResult = ref('')
const resultVisible = ref(false)

let stream: MediaStream | null = null
let scanTimer: ReturnType<typeof setInterval> | null = null
let barcodeDetector: BarcodeDetector | null = null

// 检查 BarcodeDetector API 是否可用
const supportsBarcodeDetector = 'BarcodeDetector' in window

onMounted(async () => {
  if (supportsBarcodeDetector) {
    try {
      // @ts-expect-error BarcodeDetector 为实验性 API
      barcodeDetector = new BarcodeDetector({ formats: ['qr_code', 'ean_13', 'ean_8', 'code_128', 'code_39'] })
    } catch {
      barcodeDetector = null
    }
  }

  if (!barcodeDetector) {
    // BarcodeDetector 不可用，直接提示用户用相册
    cameraError.value = '当前浏览器不支持实时扫码，请使用相册图片扫码。'
    return
  }

  await startCamera()
  if (cameraReady.value) {
    startScan()
  }
})

onUnmounted(() => {
  stopScan()
  stopCamera()
})

async function startCamera() {
  try {
    stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } },
    })
    if (videoRef.value) {
      videoRef.value.srcObject = stream
      cameraReady.value = true
    }
  } catch (err: any) {
    cameraError.value = '无法访问摄像头：' + (err.message || '权限被拒绝')
  }
}

function stopCamera() {
  if (stream) {
    stream.getTracks().forEach((t) => t.stop())
    stream = null
  }
  cameraReady.value = false
}

function startScan() {
  if (!barcodeDetector || !videoRef.value || !cameraReady.value) return
  stopScan()

  scanTimer = setInterval(async () => {
    if (!videoRef.value || videoRef.value.readyState < 2) return
    try {
      const detections = await barcodeDetector!.detect(videoRef.value)
      if (detections.length > 0) {
        const code = detections[0].rawValue
        if (code) {
          stopScan()
          scanResult.value = code
          resultVisible.value = true
        }
      }
    } catch {
      // 持续尝试
    }
  }, 300)
}

function stopScan() {
  if (scanTimer) {
    clearInterval(scanTimer)
    scanTimer = null
  }
}

function openFilePicker() {
  fileInputRef.value?.click()
}

async function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  input.value = ''

  if (barcodeDetector) {
    // 用 BarcodeDetector 检测图片
    try {
      const img = new Image()
      img.src = URL.createObjectURL(file)
      await new Promise<void>((resolve) => { img.onload = () => resolve() })
      const detections = await barcodeDetector.detect(img)
      URL.revokeObjectURL(img.src)
      if (detections.length > 0) {
        scanResult.value = detections[0].rawValue
        resultVisible.value = true
        return
      }
    } catch {
      // 失败则继续走 fallback
    }
  }

  // 无 BarcodeDetector 或检测失败：通过后端解码
  ElMessage.warning('未识别到二维码，请确认图片中包含二维码')
}

function processResult() {
  const text = scanResult.value.trim()
  resultVisible.value = false

  // 判断结果类型
  if (/^https?:\/\//i.test(text)) {
    // URL → 尝试提取活动ID或直接跳转
    const activityMatch = text.match(/activity\/(\d+)/)
    if (activityMatch) {
      router.replace(`/activity/${activityMatch[1]}`)
      return
    }
    // 签到 URL
    if (/checkin/i.test(text)) {
      window.location.href = text
      return
    }
    // 其他 URL
    window.location.href = text
  } else {
    // 纯文本编码 → 可能是活动ID
    if (/^\d+$/.test(text)) {
      router.replace(`/activity/${text}`)
    } else {
      ElMessage.info(`识别内容: ${text}`)
    }
  }
}

function goBack() {
  stopScan()
  stopCamera()
  router.back()
}
</script>

<style scoped>
.scan-page {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 999;
  background: #000;
  display: flex;
  flex-direction: column;
}

.scan-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 8px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  flex-shrink: 0;
}
.scan-header :deep(.el-button.is-text) {
  color: #fff;
}
.scan-title {
  font-size: 17px;
  font-weight: 600;
}

.scan-preview {
  flex: 1;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #111;
}

.scan-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 扫描框 */
.scan-frame {
  position: absolute;
  width: 220px;
  height: 220px;
  border: 2px solid rgba(64, 158, 255, 0.5);
  border-radius: 4px;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.4);
}
.scan-corner {
  position: absolute;
  width: 20px;
  height: 20px;
  border-color: #409eff;
  border-style: solid;
}
.tl { top: -2px; left: -2px; border-width: 3px 0 0 3px; border-radius: 4px 0 0 0; }
.tr { top: -2px; right: -2px; border-width: 3px 3px 0 0; border-radius: 0 4px 0 0; }
.bl { bottom: -2px; left: -2px; border-width: 0 0 3px 3px; border-radius: 0 0 0 4px; }
.br { bottom: -2px; right: -2px; border-width: 0 3px 3px 0; border-radius: 0 0 4px 0; }

.scan-loading,
.scan-error {
  position: absolute;
  text-align: center;
  color: #fff;
  padding: 20px;
}
.scan-error p {
  margin-bottom: 12px;
  font-size: 14px;
  opacity: 0.85;
}

.scan-footer {
  padding: 20px 0 30px;
  text-align: center;
  background: rgba(0, 0, 0, 0.6);
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  flex-shrink: 0;
}
.scan-footer p {
  margin: 0 0 8px;
}

.scan-result {
  text-align: center;
}
.result-text {
  word-break: break-all;
  font-size: 14px;
  color: #303133;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
}
</style>
