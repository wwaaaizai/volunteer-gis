import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'maplibre-gl/dist/maplibre-gl.css'

import App from './App.vue'
import router from './router'

async function bootstrap() {
  // Mock 启用：由 .env.development / .env.local 中 VITE_USE_MOCK 控制
  if (import.meta.env.VITE_USE_MOCK === 'true') {
    const { worker } = await import('./mock/browser')
    await worker.start({ onUnhandledRequest: 'bypass' })
  } else {
    // 非 Mock 模式：主动清除上一次 Mock 模式残留的 Service Worker
    if ('serviceWorker' in navigator) {
      const registrations = await navigator.serviceWorker.getRegistrations()
      for (const reg of registrations) {
        if (reg.active?.scriptURL.includes('mockServiceWorker')) {
          await reg.unregister()
          console.log('已注销旧 Mock Service Worker')
        }
      }
    }
  }

  const app = createApp(App)
  app.use(createPinia())
  app.use(router)
  app.use(ElementPlus)
  app.mount('#app')
}

bootstrap()
