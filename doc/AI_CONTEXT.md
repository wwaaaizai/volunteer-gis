# AI Context — 校园志愿活动服务系统项目速览

> 本文档供大模型（AI）快速理解项目全貌，替代重复读取源代码。
> 包含：项目定位、架构决策、数据流、启动方法、关键实现细节。

---

## 1. 项目身份

| 项目 | 描述 |
|------|------|
| **名称** | 基于GIS的校园志愿活动服务系统（volunteer-gis） |
| **目标** | 为中国矿业大学学生提供志愿活动的 GIS 地图浏览、报名、定位签到/签退的 Web 系统 |
| **当前阶段** | Phase 2 主体完成，GeoServer WFS 图层集成已实现（前端 WFS + GCJ-02 坐标变换零偏移叠加） |
| **编码** | GBK（本地），UTF-8（项目源文件） |

---

## 2. 技术栈

| 层次 | 技术 | 版本 | 备注 |
|------|------|------|------|
| 后端框架 | Spring Boot | 3.2.0 | JDK 17 |
| ORM | MyBatis-Plus | 3.5.5 | 简化 CRUD |
| 鉴权 | Spring Security 6 + JWT | jjwt 0.12.3 | HS256（MVP）→ RS256（生产） |
| 数据库 | MySQL | 8.0 | InnoDB, utf8mb4 |
| 密码加密 | BCrypt | Spring Security 内置 | |
| 前端框架 | Vue 3 + TypeScript | 3.4+ | Composition API + `<script setup>` |
| 构建工具 | Vite | 5.1+ | 开发端口 5173 |
| UI 组件 | Element Plus | 2.5+ | |
| GIS 地图 | MapLibre GL JS | 4.0+ | WebGL 矢量渲染 |
| 底图 | 天地图 API | — | WMTS 瓦片（GCJ-02） |
| GIS 服务 | GeoServer | 2.24+ | WFS 矢量服务（EPSG:4326 → 前端 GCJ-02 变换） |
| 状态管理 | Pinia | 2.1+ | |

---

## 3. 架构 —— 5 子系统单体外加横切 geo 模块

```
单一 Spring Boot 应用 (volunteer-server)
├── upm/  User & Permission   → 用户注册、登录、JWT、角色管理
├── aca/  Activity Create & Audit → 活动创建、状态流转 (draft→published→...)
├── am/   Activity Maps       → 活动→GeoJSON 转换，注入 GeoJsonBuilder
├── abm/  Activity Booking    → 报名/取消，名额管理，事务控制
├── apm/  Activity Process    → 签到/签退/扫码，注入 SpatialCalculator
└── geo/  GIS 横切模块        → 空间计算与 GeoJSON 构造的独立抽象
         ├── model/           → GeoPoint, FeatureCollection, Feature, Geometry（POJO）
         ├── service/         → SpatialCalculator(Haversine), GeoJsonBuilder
         └── repository/      → ActivitySpatialRepository（PostGIS 迁移抽象锁）
```

`am/` 和 `apm/` 不再内联空间逻辑，改为注入 `geo` 模块组件。
`ActivitySpatialRepository` 接口当前由 MySQL 实现（内存 Haversine 过滤），
未来换 PostGIS 只需替换实现类，业务层零改动。

---

## 4. 安全架构

```
请求 → JwtAuthFilter(OncePerRequestFilter)
     → 从 Authorization: Bearer xxx 提取 Token
     → JwtUtils.parseToken() 解析 → CurrentUser(userId, studentId, role)
     → SecurityContextHolder 设置认证
     → SecurityConfig 规则:
        - /api/auth/** → permitAll（注册、登录、组织者申请）
        - /uploads/** → permitAll
        - /api/map/** → permitAll（地图数据公开）
        - 其他 → 需认证
     → 角色控制: @PreAuthorize("hasRole('admin')") 或 hasRole('organizer')
```

**CurrentUser** 是自定义 Principal 对象，非数据库实体，仅含：
- `userId` (Long)
- `studentId` (String)
- `role` (String: "student" | "organizer" | "admin")

---

## 5. 数据库 —— 6 张表

| 表名 | 核心字段 | 关键索引 |
|------|---------|---------|
| `user` | id, student_id(UNIQUE), password(bcrypt), name, phone, role(student/organizer/admin), organization, employee_id, total_hours | idx_role |
| `activity` | id, title, location_name, longitude/latitude(经纬度), start_time/end_time, signup_start/signup_end, signed_count, max_participants, cover_image, status(draft/published/ongoing/ended/cancelled), creator_id, organizer_id, category, tags, checkin_region(GeoJSON) | idx_status, idx_status_time, idx_creator |
| `signup` | id, activity_id, user_id, status(signed/approved/rejected/signed_in/signed_out/cancelled), sign_in_time/lng/lat, sign_out_time/lng/lat, volunteer_hours, hour_verified, review_reason | idx_activity, idx_user, idx_user_activity |
| `message` | id, user_id, title, content, type, is_read | idx_user_read |
| `organizer_apply` | id, user_id, organization, reason, status(pending/approved/rejected), reviewed_by | idx_user, idx_status |
| `operation_log` | id, user_id, operation_type, description, activity_id | idx_user, idx_created |

DDL 脚本：[src/main/resources/db/init.sql](../volunteer-server/src/main/resources/db/init.sql)

---

## 6. API 总览 —— 25+ 个端点

### 公开（无需登录）
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册（含组织者申请） |
| POST | `/api/auth/login` | 登录 → 返回 JWT Token |
| GET | `/api/map/activities` | GeoJSON FeatureCollection（已发布活动） |
| GET | `/api/map/heatmap?category=&months=` | 活动热力图数据 |
| GET | `/api/map/layers` | 可用 GIS 图层清单 |
| GET | `/api/map/campus-bounds` | 校区边界坐标 |

### 需登录（学生）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/auth/me` | 获取当前用户信息 |
| GET | `/api/activities` | 活动列表 |
| GET | `/api/activities/{id}` | 活动详情 |
| GET | `/api/activities/search?keyword=` | 搜索活动 |
| GET | `/api/activities/nearby?lng=&lat=&radius=` | 附近活动推荐 |
| POST | `/api/signups?activityId=` | 报名活动 |
| DELETE | `/api/signups?activityId=` | 取消报名 |
| GET | `/api/signups/my` | 我的报名记录 |
| GET | `/api/signups/my-footprint` | 志愿足迹数据 |
| POST | `/api/checkin/location?activityId=&lng=&lat=` | 定位签到（支持围栏/圆形双重校验） |
| POST | `/api/checkin/qr?activityId=&code=` | 扫码签到 |
| POST | `/api/checkin/out?activityId=&lng=&lat=` | 签退 |

### 需管理员/组织者
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/activities` | 创建活动 |
| PUT | `/api/activities/{id}/publish` | 发布活动 |
| PUT | `/api/activities/{id}` | 编辑活动 |
| GET | `/api/activities/my` | 我的活动（组织者视角） |
| PUT | `/api/activities/{id}/geofence` | 保存签到围栏（GeoJSON Polygon） |
| GET | `/api/activities/{id}/geofence` | 获取签到围栏 |
| GET | `/api/signups/activity/{activityId}` | 查看活动报名名单 |
| PUT | `/api/signups/{id}/review` | 审核报名（通过/拒绝） |
| GET | `/api/checkin/qrcode/{activityId}` | 生成签到二维码 |
| PUT | `/api/checkin/verify-hours/{signupId}` | 审核志愿时长 |

### 需管理员
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/auth/organizer-applies` | 查看组织者申请列表 |
| PUT | `/api/auth/organizer-applies/{id}/review` | 审批组织者申请 |
| PUT | `/api/auth/profile` | 更新个人信息 |

统一响应格式：`{ code: 200, message: "success", data: T }`（Result 类）

---

## 7. 前端路由树 + 组件目录

```
/components/
  └── map/
      ├── BaseMap.vue          # 基础地图容器（封装 MapLibre + 天地图）
      ├── ActivityLayer.vue    # 活动点图层（GeoJSON → circle markers）
      ├── WfsLayer.vue         # GeoServer WFS 矢量图层（useWfsLayer → GeoJSON → GCJ-02）
      ├── GeoServerLayer.vue   # GeoServer WMS 栅格图层（备选，有 GCJ-02 偏移）
      ├── LayerControl.vue     # 图层控制面板（开关/透明度/状态）
      ├── GeofenceEditor.vue   # 签到围栏交互绘制工具（多边形）
      └── MapPicker.vue        # 地图选点组件
/composables/
  ├── useMap.ts               # 地图实例生命周期管理（maxBounds/zoom 限制）
  └── useWfsLayer.ts          # WFS 数据拉取 + GCJ-02 坐标递归变换
/config/
  └── map.ts                  # 集中地图配置（天地图 Key/中心/zoom/边界/GeoServer 图层，从 env 读取）
/types/
  └── geo.ts                  # GeoJSON TS 类型（FeatureCollection/Feature/Geometry）
/utils/
  └── coordConvert.ts         # WGS-84 ↔ GCJ-02 坐标转换
.--- 视图 ---

/login                  → Login.vue           (guest only)
/register               → Register.vue        (guest only，含组织者申请选项)
/                       → Layout.vue
  ├─ /                  → Map.vue             (活动地图+热力图+GeoServer WFS图层，组合 BaseMap+ActivityLayer+WfsLayer+LayerControl)
  ├─ /activity/:id      → ActivityDetail.vue  (活动详情+附近推荐)
  ├─ /my-signups        → MySignups.vue       (我的报名)
  ├─ /my-footprint      → MyFootprint.vue     (志愿足迹地图+时间线)
  ├─ /checkin           → CheckIn.vue         (签到/签退)
  ├─ /admin             → Admin.vue           (role: admin)
  │   ├─ /admin/create-activity → CreateActivity.vue
  │   └─ /admin/geofence/:id    → GeofenceEdit.vue
  └─ /organizer         → OrganizerDashboard.vue  (role: organizer, admin)
      ├─ /organizer/create      → CreateActivity.vue
      ├─ /organizer/activity/:id → OrganizerActivityDetail.vue
      ├─ /organizer/geofence/:id → GeofenceEdit.vue
      └─ /organizer/profile     → OrganizerProfile.vue

/mock/                              # MSW 前端 Mock（脱离后端独立运行）
  ├── README.md
  ├── browser.ts                    # setupWorker 入口
  ├── db.ts                         # 内存 DB（localStorage 持久化）
  ├── data/seed.ts                  # 种子数据
  └── handlers/                     # API Handler
      ├── auth.ts / activities.ts / map.ts / signups.ts / checkin.ts
```

路由守卫逻辑：`router.beforeEach`
1. 有 token 但无 user → 调 `/api/auth/me` 获取用户
2. 访问 `meta.roles: ['admin']` 或 `['admin', 'organizer']` → 校验 role 是否在数组内
3. 已登录访问 guest 页 → 重定向到 /

---

## 8. 关键业务规则

### 报名
- 活动 status 必须为 `published`，signedCount < maxParticipants
- 报名后状态为 `signed`，需组织者审核通过(`approved`)方可参加
- @Transactional 包裹：插入 signup + signedCount++（原子操作）
- 防止重复报名（LambdaQuery 查 user_id + activity_id）

### 签到
- 定位签到：前端 `navigator.geolocation.getCurrentPosition()` 获取 GPS
- **围栏优先**: 若活动设置了 `checkin_region`（GeoJSON Polygon），使用射线法判定 GPS 是否在多边形内
- **圆形兜底**: 无围栏时，Haversine 公式校验距离 ≤ 500m
- 扫码签到：Base64 解码 → 比对 `"CHECKIN:" + activityId`

### 签退
- 状态必须为 `signed_in`
- 自动计算志愿时长 = sign_out_time - sign_in_time → 转小时 → BigDecimal 保留1位

### 角色体系
- 三种角色: `student`（默认）/ `organizer`（组织者）/ `admin`（管理员）
- 组织者通过注册时申请 → 管理员审批产生
- 组织者权限：创建/编辑/发布活动、查看报名名单、设置签到围栏、审核报名
- 管理员权限：组织者权限超集 + 审批组织者申请 + 审核志愿时长
- 初始账号由 `DataInitializer` 创建：admin/admin123、organizer/organizer123

### 地图约束
- 拖拽范围限制在矿大南湖校区（maxBounds GCJ-02）
- 缩放限制 14~17 级（标准底图），17 级（卫星底图）
- 底图支持标准/卫星图标切换，图层不随切换销毁
- 数据库存 WGS-84，前端显示 GCJ-02（对齐天地图底图）

---

## 9. 启动方法

### 纯前端 Mock 模式（推荐 Demo，无需后端/MySQL）
```bash
cd volunteer-web
npm install
npm run dev              # 默认 VITE_USE_MOCK=true，MSW 拦截 /api/*
# 访问 http://localhost:5173
# 种子账号：admin/admin123（管理员）、student/123456（学生）
```

### 完整模式（需后端 + MySQL）
```bash
# 1. 启动 MySQL → 执行 init.sql 建库建表
# 2. 后端
cd volunteer-server
set MYSQL_PASSWORD=your_password  # 或直接使用 application.yml 默认值
mvnw.cmd spring-boot:run
# 3. 前端
cd volunteer-web
# 编辑 .env.development 或创 .env.local，设 VITE_USE_MOCK=false
npm install
npm run dev
# 4. 访问 http://localhost:5173
```

### 配置说明
- 前端环境变量：`.env.development`（默认，已提交）、`.env.local`（个人，gitignored）
  - `VITE_USE_MOCK=true|false` — 是否启 MSW Mock
  - `VITE_TIANDITU_KEY=<key>` — 天地图 API Key
- 后端环境变量：`application.yml` 使用 `${ENV:default}` 占位
  - `MYSQL_URL` / `MYSQL_USERNAME` / `MYSQL_PASSWORD` — DB 连接
  - `JWT_SECRET` — JWT 签名密钥
- Vite 代理：开发环境下 `/api`、`/uploads` → `http://localhost:8080`（Mock 启用时不触发）

---

## 10. 开发方向（已完成 → 待开发）

| 状态 | 功能 |
|------|------|
|  已完成 | 用户注册/登录、JWT 鉴权、三重 RBAC(student/organizer/admin)、活动 CRUD + 发布、GeoJSON API、活动地图、报名/取消、定位签到/签退（围栏+圆形）、Haversine 距离校验、射线法多边形判定、签退自动算时长 |
|  Phase 2 已完成 | 组织者角色全链路（申请/审批/仪表盘）、活动分类/标签/封面上传、MapPicker 地图选点、GeofenceEditor 围栏绘制、签到围栏校验、活动热力图（按分类/时间筛选）、志愿足迹地图+时间线、附近活动推荐、报名审核(通过/拒绝)、校区边界限制(maxBounds+zoom)、操作日志表、AI 描述/封面生成 |
|  已实现（GeoServer） | WFS 矢量图层叠加（useWfsLayer + wgs84ToGcj02 零偏移）、WmsLayer 栅格备选、建筑物默认隐藏+keepQueryable 点击选中高亮(P2-AM-08)、底图标准/卫星图标切换、建筑/运动场图标开关 |
|  待开发 | GeoServer WMS 图层叠加、离线缓存(Service Worker)、活动模板功能、地图范围定时校验、PostGIS 迁移 |
|  Phase 3 | Redis 缓存、小程序接入、3D 校园可视化、路径导航、实时位置共享 |

---

## 11. 文件路径映射

| 文件 | 定位 |
|------|------|
| `volunteer-server/pom.xml` | Maven 依赖 |
| `volunteer-server/src/main/resources/application.yml` | 后端配置（支持 ${ENV} 占位，时区 Asia/Shanghai） |
| `volunteer-server/src/main/java/.../config/MybatisPlusMetaObjectHandler.java` | MyBatis-Plus 自动填充（createdAt / updatedAt） |
| `volunteer-server/src/main/resources/db/init.sql` | 数据库 DDL |
| `volunteer-server/src/main/java/.../geo/model/` | GeoPoint, FeatureCollection, Feature, Geometry POJO |
| `volunteer-server/src/main/java/.../geo/service/SpatialCalculator.java` | Haversine 距离计算 |
| `volunteer-server/src/main/java/.../geo/service/GeoJsonBuilder.java` | GeoJSON 构造器 |
| `volunteer-server/src/main/java/.../geo/repository/ActivitySpatialRepository.java` | 空间查询抽象（PostGIS 锚点） |
| `volunteer-server/src/main/java/.../entity/OrganizerApply.java` | 组织者申请实体 |
| `volunteer-server/src/main/java/.../entity/OperationLog.java` | 操作日志实体 |
| `volunteer-server/src/main/java/.../upm/controller/AuthController.java` | 认证控制器（权限管理+组织者管理） |
| `volunteer-server/src/main/java/.../aca/controller/ActivityController.java` | 活动控制器 |
| `volunteer-server/src/main/java/.../am/controller/MapController.java` | 地图控制器 |
| `volunteer-server/src/main/java/.../abm/controller/SignupController.java` | 报名控制器 |
| `volunteer-server/src/main/java/.../apm/controller/CheckInController.java` | 签到控制器 |
| `volunteer-server/src/main/java/.../ai/controller/AiController.java` | AI 辅助控制器 |
| `volunteer-web/vite.config.ts` | Vite 配置（代理+别名） |
| `volunteer-web/.env.development` | 开发环境变量（已提交，默认开 Mock） |
| `volunteer-web/.env.example` | 环境变量模板 |
| `volunteer-web/src/config/map.ts` | 地图集中配置（key/center/zoom/style） |
| `volunteer-web/src/types/geo.ts` | GeoJSON TS 类型 |
| `volunteer-web/src/composables/useMap.ts` | MapLibre 实例生命周期 |
| `volunteer-web/src/composables/useWfsLayer.ts` | WFS 数据拉取 + GCJ-02 坐标变换 |
| `volunteer-web/src/components/map/BaseMap.vue` | 基础地图容器 |
| `volunteer-web/src/components/map/ActivityLayer.vue` | 活动点图层 |
| `volunteer-web/src/components/map/WfsLayer.vue` | GeoServer WFS 矢量图层（GeoJSON → GCJ-02） |
| `volunteer-web/src/components/map/GeoServerLayer.vue` | GeoServer WMS 栅格图层（备选） |
| `volunteer-web/src/components/map/LayerControl.vue` | 图层控制面板（开关/透明度） |
| `volunteer-web/src/components/map/GeofenceEditor.vue` | 签到围栏绘制组件 |
| `volunteer-web/src/components/map/MapPicker.vue` | 地图选点组件 |
| `volunteer-web/src/utils/coordConvert.ts` | WGS-84 ↔ GCJ-02 坐标转换 |
| `volunteer-web/src/views/Map.vue` | 地图主页（BaseMap+ActivityLayer+WfsLayer+热力图+底图切换+建筑开关+建筑点击高亮） |
| `volunteer-web/src/views/GeofenceEdit.vue` | 围栏编辑页 |
| `volunteer-web/src/views/MyFootprint.vue` | 志愿足迹页 |
| `volunteer-web/src/views/OrganizerDashboard.vue` | 组织者仪表盘 |
| `volunteer-web/src/views/OrganizerActivityDetail.vue` | 组织者活动详情 |
| `volunteer-web/src/views/OrganizerProfile.vue` | 组织者个人信息 |
| `volunteer-web/src/router/index.ts` | 路由表+守卫 |
| `volunteer-web/src/stores/user.ts` | Pinia 用户状态 |
| `volunteer-web/src/api/index.ts` | Axios 实例+拦截器 |
| `volunteer-web/src/mock/README.md` | MSW Mock 使用说明 |
| `volunteer-web/src/mock/db.ts` | Mock 内存 DB（localStorage） |
| `volunteer-web/src/mock/data/seed.ts` | Mock 种子数据 |
| `volunteer-web/src/mock/handlers/` | MSW API Handler（auth/activities/map/signups/checkin） |
| `volunteer-web/src/mock/browser.ts` | MSW setupWorker 入口 |
| `doc/API_REFERENCE.md` | 完整 API 参考文档 |
