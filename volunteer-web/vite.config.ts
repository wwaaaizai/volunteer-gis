import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
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
      '/geoserver': {
        target: 'http://localhost:9091',
        changeOrigin: true,
        // 路径重写：去掉 /geoserver 前缀（因为 GeoServer 自身上下文就是 /geoserver）
        // 前端请求 /geoserver/ol_campus/wms → 代理到 http://localhost:9091/geoserver/ol_campus/wms
      },
    },
  },
})
