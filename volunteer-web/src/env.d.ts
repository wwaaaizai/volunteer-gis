/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface ImportMetaEnv {
  /** 是否启用前端 MSW Mock（true=纯前端运行，false=连真实后端） */
  readonly VITE_USE_MOCK: string
  /** 天地图 API Key */
  readonly VITE_TIANDITU_KEY: string
  /** GeoServer 地址（供 vite 代理使用） */
  readonly VITE_GEOSERVER_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
