# Phase 2 GIS 开发工作总结

---

## 1. GeoServer 矢量图层集成

### 1.1 WFS 矢量图层

通过 GeoServer WFS 服务接入校园矢量数据，实现三大图层叠加显示：

| 图层 | GeoServer 工作区 | 几何类型 | 用途 |
|------|-----------------|---------|------|
| 校园建筑 (jianzhu) | `ol_campus` | Polygon | 建筑物底面展示与点击查询 |
| 校园边界 (bianjie) | `ol_campus` | Line | 校区范围边界线 |
| 运动场 (yundongchang) | `ol_campus` | Polygon | 运动场区域标识 |

**技术实现**：

- **`src/composables/useWfsLayer.ts`** — WFS 数据加载 composable，负责：
  - 通过 `fetch` 请求 GeoServer WFS GetFeature 接口（`service=WFS&request=GetFeature&typeName=ol_campus:xxx&outputFormat=application/json&srsName=EPSG:4326`）
  - 递归遍历 GeoJSON 全部坐标，执行 WGS-84 → GCJ-02 坐标变换
  - 支持按图层配置叠加 `translate` 微调（如建筑层 `[-0.00580, 0.00112]`），修正源数据平移偏差
  - 设计决策：前端变换而非修改源数据，保留 WGS-84 真值

- **`src/components/map/WfsLayer.vue`** — 逻辑图层组件（无视觉 DOM），负责：
  - MapLibre `fill` + `line` 图层创建/销毁生命周期
  - `keepQueryable` 模式：透明度归零但保留图层，`queryRenderedFeatures` 仍可命中
  - 点击/悬停事件绑定与光标反馈

- **`src/components/map/GeoServerLayer.vue`** — WMS 栅格图层（备选），已知有 GCJ-02 偏移问题

### 1.2 坐标系转换体系

```
数据库 (WGS-84) → 后端 API → 前端接收 (WGS-84)
                                  ↓ wgs84ToGcj02()
                              天地图底图 (GCJ-02) ← 前端渲染

GeoServer (WGS-84) → WFS GeoJSON
                       ↓ transformCoordsInPlace() + translate微调
                    前端渲染 (GCJ-02 零偏移)
```

- **`src/utils/coordConvert.ts`** — WGS-84 ↔ GCJ-02 双向转换
- 活动类数据：`GET /api/map/activities` → `ActivityLayer.vue` 中转换为 GCJ-02
- GeoServer 数据：`useWfsLayer.ts` 加载后批量转换 + 叠加每层独立 `translate` 矫正
- 地图选点组件：`MapPicker.vue` 同时展示 GCJ-02（地图显示）和 WGS-84（提交存储）

---

## 2. 底图切换系统

### 2.1 天地图 WMTS 瓦片底图

- **标准矢量底图**（`tianditu-vec` + `tianditu-cva`）：国家级矢量地图 + 中文标注
- **卫星影像底图**（`tianditu-img` + `tianditu-cia`）：卫星影像 + 中文标注叠加
- 所有底图通过天地图 WMTS 协议加载，`tileSize: 256`，GCJ-02 坐标系

### 2.2 底图切换实现 (`useMap.ts`)

- `switchBaseMap()` 移除当前底图栅格图层并加载目标底图
- 图层不随切换销毁：仅操作底图 `raster` 图层 ID，矢量覆盖层（活动点、WFS 图层）完全不受影响
- 卫星底图 `maxZoom` 限制为 17 级（标准底图 17 级），取决于天地图卫星影像最高层级
- `BaseMap.vue` 通过 `defineExpose` 暴露 `switchBaseMap()` 供父组件调用

### 2.3 底图切换 UI（Map.vue）

- 图标按钮，30×30px，半透白底 + 阴影
- 标准底图：`/icon/standard.ico`
- 卫星底图：`/icon/Satellitemap.ico`
- 切换通过 computed 自动响应 `BaseMap` 暴露的 `currentBaseMap` 状态
- 位置：`top: 145px; right: 10px`（桌面端），定位控件下方
- 移动端：纳入 `mobile-controls` 垂直按钮栏

---

## 3. 建筑物点击高亮与显示控制

### 3.1 建筑透明点击模式 (keepQueryable)

```
showBuildings = false → opacity = 0 (隐藏但图层保活)
    → queryRenderedFeatures 仍命中 → 弹出建筑名称 + 蓝色高亮框

showBuildings = true → opacity = 0.45 → 建筑面 + 运动场面可见
```

### 3.2 建筑弹窗交互

- `selectedBuilding` 状态驱动浮层弹窗，显示建筑名称
- 同时在地图上叠加蓝色高亮线框（`building-highlight` source/layer，3px 蓝色描边）
- 关闭弹窗时自动清除高亮
- 桌面端：`bottom: 24px; left: 12px` 卡片
- 移动端：`right: 8px` 全宽，关闭按钮放大到 18px + 4px padding

### 3.3 建筑显示开关

- UI：图标按钮 (`/icon/jianzhu.ico`)，30×30px，与底图切换按钮尺寸一致
- 位置：`top: 180px; right: 10px`（桌面端），底图切换按钮正下方
- 移动端：纳入 `mobile-controls` 栏
- 联动图层：同时控制 `ol_campus:jianzhu` 和 `ol_campus:yundongchang` 两个图层

---

## 4. 活动热力图

### 4.1 数据获取

- `GET /api/map/heatmap` — 返回活动点位 GeoJSON
- 支持按活动分类、时间段筛选
- 后端 `MapServiceImpl` 实现服务端聚合计算

### 4.2 前端实现 (Map.vue)

- MapLibre `heatmap` 图层类型
- 6 级渐变色带：蓝 → 青 → 绿 → 黄 → 橙 → 红
- `heatmap-weight` 从 feature properties `weight` 字段读取
- 源/图层生命周期使用 try-catch 安全守卫

### 4.3 热力图面板 UI

- 触发按钮：`bottom: 24px; right: 12px`，含火焰图标（&#x1f525;）
- 展开面板：分类下拉 + 时间段下拉筛选，`el-select` 组件
- 移动端：全宽底部弹出式面板（`border-radius: 12px 12px 0 0`）

---

## 5. 签到地理围栏系统

### 5.1 围栏绘制编辑器 (`GeofenceEditor.vue`)

- **绘制流程**：点击地图逐点添加顶点，橙色虚线连接，顶点圆形标记
- **两种状态机**：
  - 编辑中：`editing=true, finished=false` — 收集顶点
  - 已闭合：`finished=true` — 半透明橙色填充，可保存
- **交互操作**：撤销末点、全部清除、保存围栏
- **工具栏样式**：淡黄色背景 `#fffbe6` + 橙色边框，视觉区分编辑模式
- **加载已有围栏**：`GET /activities/:id/geofence` → 解析 GeoJSON → 恢复顶点
- **保存围栏**：序列化为 GeoJSON Polygon → `PUT /activities/:id/geofence`

### 5.2 围栏编辑页 (`GeofenceEdit.vue`)

- 复用 `BaseMap` + `GeofenceEditor` 组件
- 活动位置蓝色标记 + 坐标面板（WGS-84 / GCJ-02 双显示）
- `max-width: 960px; margin: auto` 居中布局

### 5.3 地图选点组件 (`MapPicker.vue`)

- 自建 MapLibre 实例（与主地图独立）
- 点击选点 → 蓝色圆形标记
- GCJ-02 / WGS-84 双坐标面板 + 一键复制按钮
- 围栏 GeoJSON 预览叠加（半透明橙色填充 + 虚线边框）
- 可配置地图高度（默认 400px）
- 上下文提示栏切换："点击地图选择位置" / "已选中位置"

---

## 6. 志愿足迹地图

### 6.1 空时可视化 (`MyFootprint.vue`)

- **空间维度**：MapLibre 地图，按时间顺序连线 + 彩色标记点
  - 起点：蓝色大点 (radius 10)
  - 中间点：灰色点 (radius 8)
  - 终点：绿色点 (radius 9)
  - 连线：蓝色半透明 polyline (width 3, opacity 0.6)
- **时间维度**：Element Plus `el-timeline` 时间轴
- **统计栏**：活动总数、去重地点数、累计志愿时长

### 6.2 技术细节

- 坐标转换：所有足迹点从 WGS-84 转为 GCJ-02 后渲染
- 地图就绪处理：`setTimeout` + `map.loaded()` 检查 + `map.once('load')` 兜底
- `map.flyTo` 定位到首个足迹点

---

## 7. 界面与图标设计

### 7.1 图标方案

| 图标 | 用途 | 文件格式 | 部署位置 |
|------|------|---------|---------|
| 卫星底图 | 底图切换 | ICO | `/icon/Satellitemap.ico` |
| 标准底图 | 底图切换 | ICO | `/icon/standard.ico` |
| 建筑显示 | 建筑开关 | ICO | `/icon/jianzhu.ico` |
| 虚线辅助 | 备用 | ICO | `/icon/xuxian.ico` |
| 扫一扫 | 移动端控制栏 | 内联 SVG | Layout.vue / Map.vue |
| 定位 | 移动端控制栏 | 内联 SVG | Map.vue |
| 地图 | 移动端底部导航 | 内联 SVG | Layout.vue |
| 个人中心 | 移动端底部导航 | 内联 SVG | Layout.vue |

**设计特点**：
- 地图控件按钮统一 30×30px，圆角 4px，半透白底 `rgba(255,255,255,0.92)` + 阴影
- 地图控件图标统一 18×18px 居中显示
- 移动端内联 SVG 无需额外 HTTP 请求
- PNG 备选文件已提供（删除的 `Satellitemap.png` / `standard.png` 已用 ICO 替代）

### 7.2 登录页设计

- 渐变背景：`linear-gradient(135deg, #667eea 0%, #764ba2 100%)`（靛蓝到紫色）
- 居中卡片：400px 宽，包含学号/密码输入 + 记住我复选框 + 登录按钮
- `autocomplete` 属性支持浏览器密码管理器

### 7.3 组织者仪表盘

- 活动管理表格，支持发布/编辑/查看报名等操作
- 新建活动复用 `CreateActivity.vue`（MapPicker 选点）
- 个人信息页：`OrganizerProfile.vue`

---

## 8. 移动端适配

### 8.1 响应式导航 (`Layout.vue`)

| 维度 | 桌面端 | 移动端 |
|------|--------|--------|
| 导航栏 | 顶部水平 `el-menu` | 底部双 Tab 栏 |
| 用户菜单 | `el-dropdown` 下拉 | 无（在"我的"页面中操作） |
| 页面头部 | `el-header` 60px | 隐藏，全屏地图 |
| Tab 选项 | 活动地图/足迹/报名/签到/管理等 | 仅"活动地图"+"我的" |

### 8.2 地图控件适配 (`Map.vue`)

**桌面端** (`.map-page`)：
- `height: calc(100vh - 60px)`
- 底部切换按钮：`top: 145px / 180px; right: 10px`
- 热力图按钮：`bottom: 24px; right: 12px`
- 建筑弹窗：`bottom: 24px; left: 12px`

**移动端** (`.map-page--mobile`)：
- `height: 100%`
- 右侧控制栏：`top: 20px; right: 10px`，垂直排列 4 个 40×40px 按钮
  1. 扫一扫（二维码跳转至 Scan.vue）
  2. 底图切换
  3. 建筑显示/隐藏
  4. 定位
- 触摸优化：`-webkit-tap-highlight-color: transparent`
- 安全区适配：`env(safe-area-inset-top, 0px)`

**移动端热力图**：
- 按钮放大：`padding: 12px 20px`
- 面板全宽：`left: 8px; right: 8px`，底部弹出式

**移动端建筑弹窗**：
- 全宽卡片：`right: 8px` + `margin-left: 8px`
- 关闭按钮放大：18px + 4px padding

### 8.3 移动端页面 (`My.vue`)

- 用户信息卡片 + 功能入口列表
- 替代桌面端的顶部导航功能入口

### 8.4 扫一扫 (`Scan.vue`)

- 移动端扫码功能占位页面

---

## 9. 认证与安全

### 9.1 登录拦截与路由守卫

- 未登录用户访问任何非 `guest` 页面 → 重定向到 `/login`
- 已登录用户访问登录/注册页 → 重定向到 `/`
- 角色权限校验：`meta.roles` 数组匹配（`['admin']` / `['admin','organizer']`）
- Token 存在但用户信息未加载 → 先 `fetchUser()` 再放行

### 9.2 记住我功能

- `localStorage` 缓存学号 + 记住开关
- 登录页 `onMounted` 自动填充学号
- 退出登录保留学号，方便下次自动填充
- 密码不存储到 localStorage，由浏览器密码管理器通过 `autocomplete` 属性处理

### 9.3 401 拦截器

- 动态导入 `useUserStore` 避免循环依赖
- 同步清除 Pinia store 和 localStorage token
- 跳转登录页 + 友好提示

---

## 10. 后端 GIS 服务

### 10.1 后端 API 扩展

| API | 方法 | 用途 | Phase |
|-----|------|------|-------|
| `/api/map/activities` | GET | 活动 GeoJSON（含 GCJ-02 坐标） | MVP |
| `/api/map/heatmap` | GET | 热力图数据（分类/时间筛选） | P2 |
| `/api/map/footprint` | GET | 用户足迹轨迹数据 | P2 |
| `/api/activities/nearby` | GET | 附近活动推荐 | P2 |
| `/api/activities/:id/geofence` | GET/PUT | 围栏读写 | P2 |
| `/api/map/wfs-proxy` | GET | WFS 代理（备选） | P2 |

### 10.2 GeoServer 部署

- 端口：9091
- 工作区：`ol_campus`
- 前端通过 Vite proxy `/geoserver → http://localhost:9091` 避免 CORS
- 数据源：PostGIS（已迁移部分校园矢量数据）

---

## 11. 数据库迁移

| 迁移内容 | 表 | 说明 |
|---------|-----|------|
| 签到围栏 GeoJSON | `activities.geofence` | TEXT 列存 GeoJSON Polygon |
| 活动分类/标签/封面 | `activities` | category/tags/coverImage |
| 报名审核状态 | `signups.status` | PENDING/APPROVED/REJECTED |
| 操作日志 | `operation_logs` 新表 | 管理员操作审计 |

详见 `doc/DATABASE_MIGRATIONS.md`，所有迁移通过 `db/init.sql` 增量 ALTER TABLE 块 + `IF NOT EXISTS` 保证多设备可重复执行。

---

## 12. 文件变更统计

最近 3 次提交 (504c01d / 527864f / 30aa37d)：

```
48 个文件变更
+2949 行  −257 行
```

**新增文件（共计 12 个）**：

| 文件 | 类型 | 用途 |
|------|------|------|
| `WfsLayer.vue` | 组件 | WFS 矢量图层渲染 |
| `useWfsLayer.ts` | Composable | WFS 数据加载+坐标转换 |
| `GeoServerLayer.vue` | 组件 | WMS 栅格图层（备选） |
| `GeoServerLayer.vue` | 组件 | WMS 栅格图层渲染 |
| `LayerControl.vue` | 组件 | 图层控制面板 |
| `GeofenceEditor.vue` | 组件 | 围栏绘制编辑器 |
| `MyFootprint.vue` | 页面 | 志愿足迹地图+时间线 |
| `GeofenceEdit.vue` | 页面 | 围栏编辑页 |
| `SpatialCalculator.java` | 后端 | 空间计算工具类 |
| `DATABASE_MIGRATIONS.md` | 文档 | 数据库迁移指引 |
| ICO 图标 × 5 | 资源 | 底图/建筑/虚线图标 |
| `map.ts` | 配置 | 地图数据中心化配置 |

---

## 13. 技术亮点

1. **双坐标系零偏移渲染** — WGS-84 数据库 → GCJ-02 前端显示，GeoServer 源经 WGS→GCJ + translate 双重矫正
2. **透明可点击建筑交互** — keepQueryable 模式在不显示建筑面时仍支持点击查询，兼顾地图简洁性与信息获取
3. **纯前端坐标变换** — 源数据保持 WGS-84 真值，前端运行时变换，将来切换 WGS-84 底图零成本
4. **移动端深度适配** — 双导航系统 + safe-area-inset + 触摸优化 + 40px 最小触控区域
5. **shallowRef 解耦 MapLibre** — 避免 Vue 深度代理干扰 MapLibre 内部对象
6. **组件复用设计** — BaseMap + GeofenceEditor + MapPicker 跨页面复用，减少重复代码
7. **数据库迁移安全策略** — IF NOT EXISTS + 增量 ALTER TABLE，多设备协作零风险
