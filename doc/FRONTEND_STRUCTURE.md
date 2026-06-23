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
| /register | Register.vue | guest: true | 注册页 |
| / | Layout.vue + .html# | 主布局容器 |
| / | MenuView.vue | title='活动地图' | 天地图 + GeoJSON 标注 |
| /activity/:id | ActivityDetail.vue | | 活动详情 + 报名按钮 |
| /my-signups | MySignups.vue | | 报名记录表格 |
| /checkin | CheckIn.vue | | 签到/签退表单 |
| /admin | Admin.vue | role: 'admin' | 活动管理+时长审核 |
| /admin/create-activity | CreateActivity.vue | role: 'admin' | 创建活动表单 |

**路由守卫 (`router.beforeEach`)**:
1. 有 token 但无 user → `userStore.fetchUser()` → 调 `/api/auth/me`
2. `to.meta.role === 'admin'` → 校验 `user.role !== 'admin'` → 重定向到 `/`
3. `to.meta.guest` 且已登录 → 重定向到 `/`

---

## 3. 组件树

```
App.vue
├── Login.vue          (未登录入口)
│   └── el-form → login()
├── Register.vue       (未登录入口)
│   └── el-form → POST /api/auth/register
└── Layout.vue         (已登录容器)
    ├── el-header
    │   ├── 标题："志愿活动系统"
    │   ├── el-menu (horizontal, router)
    │   │   ├── 活动地图 (/)
    │   │   ├── 我的报名 (/my-signups)
    │   │   ├── 签到 (/checkin)
    │   │   └── 管理后台 (/admin) [v-if="isAdmin"]
    │   └── el-dropdown → "退出登录"
    └── el-main → <router-view />
        ├── Map.vue             地图页
        │   ├── <div ref="mapContainer"> → maplibregl.Map
        │   ├── 天地图 WMTS 瓦片（vec_w + cva_w）
        │   ├── GET /api/map/activities → addSource + addLayer (circle)
        │   └── click activity-markers → router.push /activity/:id
        ├── ActivityDetail.vue  详情页
        │   ├── el-descriptions (地点/人数/时间)
        │   └── el-button → POST /api/signups
        ├── MySignups.vue      我的报名
        │   └── el-table → GET /api/signups/my
        ├── CheckIn.vue        签到页
        │   ├── el-radio-group → location / qr
        │   ├── handleCheckIn() → navigator.geolocation / POST /api/checkin/location
        │   └── handleCheckOut() → navigator.geolocation / POST /api/checkin/out
        ├── Admin.vue          管理后台
        │   ├── el-tabs (活动管理 / 时长审核)
        │   ├── el-table → GET /api/activities/admin（全状态）
        │   └── 发布按钮 → PUT /api/activities/{id}/publish
        └── CreateActivity.vue 创建活动
            └── el-form → POST /api/activities
```

---

## 4. 状态管理 (Pinia)

**文件**: `src/stores/user.ts`

```
useUserStore = defineStore('user')
├── state
│   ├── user: UserInfo | null    { userId, studentId, role }
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
  /api       → http://localhost:8080
  /uploads   → http://localhost:8080

resolve.alias:
  @ → src/
```

---

## 7. 关键交互流程

### 地图加载
```
Map.vue onMounted
  → BaseMap onMounted → new maplibregl.Map({ container, style: { sources: 天地图 WMTS } })
  → loadActivities() → request.get('/map/activities') → GeoJSON
  → ActivityLayer watch([map, geojson]) → addSource + addLayer (circle)
  → map.on('click', 'activity-markers') → router.push(`/activity/${id}`)
```

### 定位签到
```
CheckIn.vue handleCheckIn()
  → navigator.geolocation.getCurrentPosition()
  → { lng, lat }
  → POST /api/checkin/location?activityId=&lng=&lat=
  → 后端 Haversine 校验距离 ≤ 500m
  → 成功: status = signed_in
```

### 签退自动算时长
```
CheckIn.vue handleCheckOut()
  → navigator.geolocation.getCurrentPosition()
  → POST /api/checkin/out?activityId=&lng=&lat=
  → 后端: volunteerHours = Duration.between(signInTime, signOutTime)
  → 成功: status = signed_out
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
| src/router/index.ts | 路由表 + beforeEach 守卫 |
| src/stores/user.ts | Pinia: 登录状态 + token 管理 |
| src/api/index.ts | Axios: 拦截器 + 统一错误处理 |
| src/views/Login.vue | 登录页 |
| src/views/Register.vue | 注册页 |
| src/views/Layout.vue | 主布局（导航栏 + 内容区） |
| src/views/Map.vue | 地图主页（MapLibre GL + 天地图） |
| src/views/ActivityDetail.vue | 活动详情 + 报名 |
| src/views/MySignups.vue | 我的报名列表 |
| src/views/CheckIn.vue | 签到/签退 |
| src/views/Admin.vue | 管理后台 |
| src/views/CreateActivity.vue | 创建活动表单 |
| vite.config.ts | Vite + 代理 + @ 别名 |
| package.json | 依赖清单 |
