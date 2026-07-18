import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, __dirname, '')

  // GeoServer 地址：通过环境变量 VITE_GEOSERVER_URL 配置
  // 默认 http://localhost:9091（本机 GeoServer）
  // 其他设备演示时，在 .env.local 中设置为 http://<主机IP>:9091
  const geoserverTarget = env.VITE_GEOSERVER_URL || 'http://localhost:8338'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: 'http://localhost:8081',
          changeOrigin: true,
        },
        '/uploads': {
          target: 'http://localhost:8081',
          changeOrigin: true,
        },
        // P2-AM-03：GeoServer WMS/WFS 代理（避免浏览器 CORS 限制）
        // 前端请求 /geoserver/ol_campus/wms → 代理到 VITE_GEOSERVER_URL/geoserver/ol_campus/wms
        '/geoserver': {
          target: geoserverTarget,
          changeOrigin: true,
        },
      },
    },
  }
})
