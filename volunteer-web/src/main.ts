import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'maplibre-gl/dist/maplibre-gl.css'

import App from './App.vue'
import router from './router'

async function bootstrap() {
  // Mock 启用：由 .env.development / .env.local 中 VITE_USE_MOCK 控制
  // MSW 在 Service Worker 层拦截 /api/*，此时 vite proxy 不会被触发
  // 设为 false 即恢复连接真实后端 http://localhost:8080
  if (import.meta.env.VITE_USE_MOCK === 'true') {
    const { worker } = await import('./mock/browser')
    await worker.start({ onUnhandledRequest: 'bypass' })
  }

  const app = createApp(App)
  app.use(createPinia())
  app.use(router)
  app.use(ElementPlus)
  app.mount('#app')
}

bootstrap()
