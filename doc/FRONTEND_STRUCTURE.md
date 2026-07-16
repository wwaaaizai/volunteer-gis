# 前端结构参考 — 校园志愿活动服务系统

> 包含：路由树、组件层级、状态管理、API 客户端、关键交互流程。

---

## 1. 入口与启动

```
main.ts
  ├── createApp(App.vue)
  ├── app.use(createPinia())       → Pinia 状态管理
  ├── app.use(router)              → Vue Router
  ├── app.use(ElementPlus)         → Element Plus UI
  ├── import 'element-plus/dist/index.css'
  ├── import 'maplibre-gl/dist/maplibre-gl.css'
  └── app.mount('#app')

App.vue → <router-view /> (纯路由出口)
```

---

## 2. 路由表

```
router/index.ts → createWebHistory()
```

| Path | Component | meta | 说明 |
|------|-----------|------|------|
| /login | Login.vue | guest: true | 登录页（渐变背景） |
| /register | Register.vue | guest: true | 注册页（含组织者申请选项） |
| / | Layout.vue | | 主布局容器 |
| / | Map.vue | title='活动地图' | 天地图 + GeoJSON 标注 + GeoServer WFS图层 + 热力图 + 底图切换 + 建筑开关 + 点击高亮 |
| /activity/:id | ActivityDetail.vue | | 活动详情 + 报名 + 附近推荐 |
| /my-signups | MySignups.vue | | 报名记录表格 |
| /my-footprint | MyFootprint.vue | | 志愿足迹地图 + 时间线统计 |
| /checkin | CheckIn.vue | | 签到/签退表单 |
| /admin | Admin.vue | roles: ['admin'] | 活动管理+组织者审批+时长审核 |
| /admin/create-activity | CreateActivity.vue | roles: ['admin', 'organizer'] | 创建活动表单 |
| /admin/geofence/:id | GeofenceEdit.vue | roles: ['admin', 'organizer'] | 签到围栏编辑 |
| /organizer | OrganizerDashboard.vue | roles: ['organizer', 'admin'] | 组织者仪表盘（我的活动） |
| /organizer/create | CreateActivity.vue | roles: ['organizer', 'admin'] | 创建活动（地图选点） |
| /organizer/activity/:id | OrganizerActivityDetail.vue | roles: ['organizer', 'admin'] | 活动详情（含报名名单+签到统计） |
| /organizer/geofence/:id | GeofenceEdit.vue | roles: ['organizer', 'admin'] | 签到围栏编辑 |
| /organizer/profile | OrganizerProfile.vue | roles: ['organizer', 'admin'] | 组织者个人信息 |
| /course-schedule | CourseSchedule.vue | | 课表日历视图 + 空闲时段活动筛选 |

**路由守卫 (`router.beforeEach`)**:
1. 有 token 但无 user → `userStore.fetchUser()` → 调 `/api/auth/me`
2. `to.meta.roles` 数组 → 校验 `user.role` 是否在允许的角色列表中
3. `to.meta.guest` 且已登录 → 重定向到 `/`
4. 无角色匹配 → 重定向到 `/`（无权限）

---

## 3. 组件树

```
App.vue
├── Login.vue          (未登录入口)
│   └── el-form → login()
├── Register.vue       (未登录入口)
│   └── el-form → POST /api/auth/register（含组织者申请字段）
└── Layout.vue         (已登录容器)
    ├── el-header
    │   ├── 标题："志愿活动系统"
    │   ├── el-menu (horizontal, router)
    │   │   ├── 活动地图 (/)
    │   │   ├── 我的报名 (/my-signups)
    │   │   ├── 签到 (/checkin)
    │   │   ├── 志愿足迹 (/my-footprint)
    │   │   ├── 课程表 (/course-schedule) [v-if="isStudent"]
    │   │   ├── 管理后台 (/admin) [v-if="isAdmin"]
    │   │   └── 组织者后台 (/organizer) [v-if="isOrganizer || isAdmin"]
    │   └── el-dropdown → "退出登录"
    └── el-main → <router-view />
        ├── Map.vue             地图页
        │   ├── BaseMap: 天地图 WMTS 瓦片 + maxBounds + zoom 限制 + 标准/卫星切换
        │   ├── ActivityLayer: GET /api/map/activities → circle markers
        │   ├── WfsLayer (校园建筑/边界/运动场): GET /geoserver/ol_campus/ows (WFS) → GCJ-02变换 → fill/line
        │   │   └── 建筑 keepQueryable 模式: 默认隐藏(opacity=0)，仍可点击选中
        │   ├── 底图切换按钮: 图标按钮（卫星/标准），位于定位控件下方
        │   ├── 建筑开关按钮: 图标按钮，默认关闭，点击后显示建筑+运动场面
        │   ├── 建筑弹窗: 点击建筑面 → 蓝色高亮轮廓 + 名称弹窗（P2-AM-08）
        │   └── 热力图面板: GET /api/map/heatmap → heatmap layer (分类/时间筛选)
        ├── ActivityDetail.vue  详情页
        │   ├── el-descriptions (地点/人数/时间/分类/标签)
        │   ├── el-button → POST /api/signups
        │   └── 附近活动推荐 → GET /api/activities/nearby
        ├── MySignups.vue      我的报名
        │   └── el-table → GET /api/signups/my
        ├── MyFootprint.vue    志愿足迹
        │   ├── 统计面板（活动数/地点数/总时长）
        │   ├── BaseMap → 足迹连线(LineString) + 标记点
        │   └── el-timeline → 时间线历史
        ├── CheckIn.vue        签到页
        │   ├── el-radio-group → location / qr
        │   ├── handleCheckIn() → geolocation → POST /api/checkin/location
        │   └── handleCheckOut() → geolocation → POST /api/checkin/out
        ├── Admin.vue          管理后台
        │   ├── el-tabs (活动管理 / 组织者审批 / 时长审核)
        │   ├── el-table → GET /api/activities
        │   └── 审批操作 → PUT /api/auth/organizer-applies/{id}/review
        ├── CreateActivity.vue 创建活动
        │   ├── el-form (标题/描述/分类/标签/封面/人数/时间)
        │   └── MapPicker → 地图选点组件
        ├── GeofenceEdit.vue   围栏编辑器
        │   ├── BaseMap + 活动标记
        │   └── GeofenceEditor → 多边形绘制/编辑 + 保存
        ├── OrganizerDashboard.vue  组织者仪表盘
        │   ├── el-tabs (草稿/已发布/进行中/已结束)
        │   └── el-table → GET /api/activities/my
        ├── OrganizerActivityDetail.vue  组织者活动详情
        │   ├── 活动基本信息
        │   ├── 报名名单 → GET /api/signups/activity/{id}
        │   └── 审核报名 → PUT /api/signups/{id}/review
        └── OrganizerProfile.vue  组织者个人信息
            └── el-form → PUT /api/auth/profile
        └── CourseSchedule.vue  课表日历
            ├── el-upload → 导入 .ics 文件 → parseIcs()
            ├── el-calendar → 周视图 (5大节 × 7天)
            ├── el-dialog → 课程详情（教师/学分/备注）
            └── 空闲时段活动筛选 → GET /api/activities + 本地时间匹配
```

---

## 4. 状态管理 (Pinia)

**文件**: `src/stores/user.ts`

```
useUserStore = defineStore('user')
├── state
│   ├── user: UserInfo | null    { userId, studentId, role, name?, phone?, organization? }
│   └── token: string            (从 localStorage 恢复)
├── login(studentId, password)
│   ├── POST /api/auth/login → 获取 token
│   ├── 存储 token 到 localStorage
│   └── fetchUser() → GET /api/auth/me → 设置 user
├── fetchUser()
│   └── GET /api/auth/me → user.value = ...
└── logout()
    ├── 清除 token (localStorage.removeItem)
    └── user.value = null
```

**文件**: `src/stores/course.ts`

```
useCourseStore = defineStore('course')
├── state
│   ├── courses: ParsedCourse[]      (解析后的课程列表)
│   ├── sourceText: string           (原始 ICS 文本，localStorage 持久化)
│   ├── hasImported: boolean
│   └── weekStartDate: string        (当前展示周的周一日期)
├── getters
│   ├── weekNumber: number           (第几周)
│   ├── weekDays: WeekDay[]          (周一~周日 + 具体日期)
│   ├── coursesBySlot: Map           (weekDay, bigPeriod) → ParsedCourse
│   ├── currentWeekCourses           (当前周课程)
│   └── freeSlots: FreeSlot[]        (空闲时段列表)
├── importIcs(text) → parseIcs() → 存入 localStorage
├── setWeek(date): 切换查看周
├── nextWeek() / prevWeek(): 周导航
├── clearCourses(): 清空
└── init(): 从 localStorage 恢复
```

**数据流**：
```
Login.vue / Register.vue
  → userStore.login() / request.post()
  → token 存入 localStorage
  → router.push('/')
  → Layout.vue 通过 userStore.user 显示角色菜单
  → 路由守卫（beforeEach）通过 userStore.token / userStore.user 鉴权
```

---

## 5. API 客户端

**文件**: `src/api/index.ts` (default export: request)

```
axios.create({ baseURL: '/api', timeout: 10000 })

请求拦截器:
  → 从 localStorage 取 token
  → 设置 Authorization: Bearer <token>

响应拦截器（成功）:
  → response.data.code === 200 → return data (自动解包 Result)
  → response.data.code !== 200 → ElMessage.error + reject

响应拦截器（错误）:
  → error.response.status === 401
    → localStorage.removeItem('token')
    → router.push('/login')
    → ElMessage.error('登录已过期')

返回值类型:
  request.post('/auth/login', body)      → Promise<{ token: string }>
  request.get('/auth/me')                → Promise<{ userId, studentId, role }>
  request.get('/activities')             → Promise<Activity[]>
  request.get('/map/activities')         → Promise<GeoJSON FeatureCollection>
  request.get('/signups/my')             → Promise<Signup[]>
```

**注意**: 拦截器成功回调中 `return data` 自动解包了 `Result<T>` 的 data 字段，因此所有请求的返回值都是 `T`，不是 `Result<T>`。

---

## 6. Vite 配置

**文件**: `vite.config.ts`

```ts
server.port = 5173
server.proxy:
  /api        → http://localhost:8080
  /uploads    → http://localhost:8080
  /geoserver  → VITE_GEOSERVER_URL（默认 http://localhost:9091）(GeoServer WMS/WFS，避免 CORS)
                支持 .env.local 覆盖，用于其他设备连接主机 GeoServer

resolve.alias:
  @ → src/
```

---

## 7. 关键交互流程

### 地图加载（useMap composable）
```
BaseMap.vue / useMap.ts init()
  → new maplibregl.Map({
      container, style: buildTiandituStyle(),
      maxBounds: CAMPUS_BOUNDS_GCJ02, minZoom: 14, maxZoom: 17
    })
  → map.addControl(NavigationControl + GeolocateControl)
  → map.on('load') → mapReady = true
  → onUnmounted → map.remove()
```

### GeoServer WFS 矢量图层（useWfsLayer + WfsLayer）
```
WfsLayer.vue
  → useWfsLayer().load(layerName)
    → fetch(/geoserver/ol_campus/ows?service=WFS...&srsName=EPSG:4326)
    → GeoJSON 数据（WGS-84 坐标）
    → transformCoordsInPlace(): 递归 wgs84ToGcj02() 变换所有坐标
    → 叠加 translate 微调（修正源数据整体平移偏差）
    → map.addSource({ type: 'geojson', data })  ← 零偏移对齐天地图
    → map.addLayer({ type: 'fill', paint })      ← polygon 填充
    → map.addLayer({ type: 'line', paint })      ← polygon 轮廓
    → map.on('click') → emit('feature-click', properties, geometry)

keepQueryable 模式（建筑图层专用）:
  → 建筑物默认 showBuildings=false, layer 初始 opacity=0
  → 不移除图层（keepQueryable=true），仅通过 setPaintProperty 控制透明度
  → 用户仍可点击"隐形"建筑面 → queryRenderedFeatures 命中 → 弹出名称
  → 同时在地图上叠加蓝色高亮线框（building-highlight source/layer）
  → 关闭弹窗时自动清除高亮

建筑开关按钮:
  → 图标按钮切换 showBuildings
  → true → 同步 setPaintProperty opacity=0.45（显示建筑+运动场面）
  → false → opacity=0（隐藏但 keepQueryable 保活）
```

### 活动标注图层
```
ActivityLayer.vue
  → GET /api/map/activities → GeoJSON FeatureCollection
  → map.addSource('activities', { type: 'geojson', data })
  → map.addLayer('activity-markers', { type: 'circle', paint })
  → map.on('click', 'activity-markers') → router.push(`/activity/${id}`)
```

### 热力图
```
Map.vue 热力图面板
  → GET /api/map/heatmap?category=&months= → GeoJSON Point[]
  → map.addLayer({ type: 'heatmap', paint: { heatmap-color: 6色渐变 } })
  → 参数可调: radius(50), intensity(1.5), opacity(0.8)
  → 筛选: 活动分类下拉 + 时间范围选择(3/6/12个月)
```

### 志愿足迹
```
MyFootprint.vue onMounted
  → GET /api/signups/my-footprint → FootprintItem[]
  → 统计面板: 总活动数、覆盖地点数、总时长
  → BaseMap → map.addLayer(足迹连线 LineString + 首/末/中间 标记点)
  → el-timeline → 按时间倒序展示每次活动
  → 坐标转换: WGS-84 → GCJ-02 (wgs84ToGcj02)
```

### 定位签到（围栏优先）
```
CheckIn.vue handleCheckIn()
  → navigator.geolocation.getCurrentPosition()
  → { lng, lat }
  → POST /api/checkin/location?activityId=&lng=&lat=
  → 后端:
    1. 若有 checkin_region → 射线法判定是否在多边形内
    2. 若无 → Haversine 校验距离 ≤ 500m
  → 成功: status = signed_in
```

### 围栏编辑
```
GeofenceEdit.vue
  → GET /api/activities/{id}/geofence → 已有围栏 GeoJSON
  → GeofenceEditor.vue:
    - 点击地图添加顶点, 双击完成多边形
    - 支持撤销最后顶点, 保存 GeoJSON
  → PUT /api/activities/{id}/geofence → 保存围栏

### 签退自动算时长
```
CheckIn.vue handleCheckOut()
  → navigator.geolocation.getCurrentPosition()
  → POST /api/checkin/out?activityId=&lng=&lat=
  → 后端: volunteerHours = Duration.between(signInTime, signOutTime)
  → 成功: status = signed_out
```

### 组织者申请与审批
```
Register.vue (applyAsOrganizer=true)
  → POST /api/auth/register → 创建用户 + organizer_apply
  → Admin.vue "组织者审批" Tab
    → GET /api/auth/organizer-applies → 待审批列表
    → 通过: PUT /api/auth/organizer-applies/{id}/review {status:'approved'}
      → 自动更新 user.role = 'organizer'
```

---

## 8. 依赖版本

| 包 | 版本 |
|----|------|
| vue | ^3.4.0 |
| vue-router | ^4.3.0 |
| pinia | ^2.1.0 |
| element-plus | ^2.5.0 |
| maplibre-gl | ^4.0.0 |
| axios | ^1.6.0 |
| @vitejs/plugin-vue | ^5.0.0 |
| typescript | ^5.3.0 |
| vite | ^5.1.0 |

---

## 9. 文件索引

| 文件路径 | 用途 |
|----------|------|
| src/main.ts | 入口：挂载 Pinia + Router + Element Plus |
| src/App.vue | 根组件：纯 `<router-view>` |
| src/router/index.ts | 路由表 + beforeEach 守卫（支持 meta.roles 数组） |
| src/stores/user.ts | Pinia: 登录状态 + token 管理 |
| src/stores/course.ts | Pinia: 课表数据 + 空闲时段计算 |
| src/utils/icsParser.ts | ICS 课表文件解析器（UTC→北京时区+大节映射） |
| src/api/index.ts | Axios: 拦截器 + 统一错误处理 |
| src/config/map.ts | 地图集中配置（天地图WMTS源/GeoServer WMS-WFS工厂/图层清单/边界常量） |
| src/composables/useMap.ts | MapLibre 实例生命周期（maxBounds+zoom 限制） |
| src/composables/useWfsLayer.ts | WFS 数据拉取 + GCJ-02 坐标递归变换 |
| src/types/geo.ts | GeoJSON TS 类型定义 |
| src/utils/coordConvert.ts | WGS-84 ↔ GCJ-02 坐标转换 |
| src/components/map/BaseMap.vue | 基础地图容器 |
| src/components/map/ActivityLayer.vue | 活动点图层 |
| src/components/map/WfsLayer.vue | GeoServer WFS 矢量图层（useWfsLayer → GeoJSON → fill/line） |
| src/components/map/GeoServerLayer.vue | GeoServer WMS 栅格图层（备选，有 GCJ-02 偏移） |
| src/components/map/LayerControl.vue | 图层控制面板（开关/透明度/加载状态） |
| src/components/map/GeofenceEditor.vue | 签到围栏绘制组件 |
| src/components/map/MapPicker.vue | 地图选点组件 |
| src/views/Login.vue | 登录页 |
| src/views/Register.vue | 注册页（含组织者申请） |
| src/views/Layout.vue | 主布局（导航栏 + 内容区） |
| src/views/Map.vue | 地图主页（BaseMap+ActivityLayer+WfsLayer+热力图+底图切换+建筑开关+点击高亮） |
| src/views/ActivityDetail.vue | 活动详情 + 报名 + 附近推荐 |
| src/views/MySignups.vue | 我的报名列表 |
| src/views/MyFootprint.vue | 志愿足迹地图 + 时间线 |
| src/views/CheckIn.vue | 签到/签退 |
| src/views/Admin.vue | 管理后台（活动管理+组织者审批+时长审核） |
| src/views/GeofenceEdit.vue | 签到围栏编辑页 |
| src/views/CreateActivity.vue | 创建活动表单（含 MapPicker） |
| src/views/OrganizerDashboard.vue | 组织者仪表盘 |
| src/views/OrganizerActivityDetail.vue | 组织者活动详情 |
| src/views/OrganizerProfile.vue | 组织者个人信息页 |
| src/views/My.vue | 学生"我的"页面（功能菜单入口） |
| src/views/CourseSchedule.vue | 课表日历视图 + 空闲时段活动筛选 |
| vite.config.ts | Vite + 代理 + @ 别名 |
| package.json | 依赖清单 |
