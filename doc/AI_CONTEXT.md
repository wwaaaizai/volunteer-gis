# AI Context — 校园志愿活动服务系统项目速览

> 本文档供大模型（AI）快速理解项目全貌，替代重复读取源代码。
> 包含：项目定位、架构决策、数据流、启动方法、关键实现细节。

---

## 1. 项目身份

| 项目 | 描述 |
|------|------|
| **名称** | 基于GIS的校园志愿活动服务系统（volunteer-gis） |
| **目标** | 为中国矿业大学学生提供志愿活动的 GIS 地图浏览、报名、定位签到/签退的 Web 系统 |
| **当前阶段** | MVP（Phase 1：后端 + Web 前端），小程序搁置 |
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
| 底图 | 天地图 API | — | WMTS 瓦片 |
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
        - /api/auth/** → permitAll（注册、登录）
        - /uploads/** → permitAll
        - 其他 → 需认证
     → 管理员接口: @PreAuthorize("hasRole('admin')")
```

**CurrentUser** 是自定义 Principal 对象，非数据库实体，仅含：
- `userId` (Long)
- `studentId` (String)
- `role` (String: "student" | "admin")

---

## 5. 数据库 —— 4 张表

| 表名 | 核心字段 | 关键索引 |
|------|---------|---------|
| `user` | id, student_id(UNIQUE), password(bcrypt), name, phone, role(student/admin), total_hours | idx_role |
| `activity` | id, title, location_name, longitude/latitude(经纬度), signed_count, max_participants, status(draft/published/ongoing/ended/cancelled), creator_id | idx_status, idx_status_time, idx_creator |
| `signup` | id, activity_id, user_id, status(signed/signed_in/signed_out/cancelled), sign_in_time/lng/lat, sign_out_time/lng/lat, volunteer_hours | idx_activity, idx_user, idx_user_activity |
| `message` | id, user_id, title, content, type, is_read | idx_user_read |

DDL 脚本：[src/main/resources/db/init.sql](../volunteer-server/src/main/resources/db/init.sql)

---

## 6. API 总览 —— 14 个端点

### 公开（无需登录）
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 → 返回 JWT Token |

### 需登录（学生）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/auth/me` | 获取当前用户信息 |
| GET | `/api/map/activities` | GeoJSON FeatureCollection（已发布活动） |
| GET | `/api/activities` | 活动列表 |
| GET | `/api/activities/{id}` | 活动详情 |
| GET | `/api/activities/search?keyword=` | 搜索活动 |
| POST | `/api/signups?activityId=` | 报名活动 |
| DELETE | `/api/signups?activityId=` | 取消报名 |
| GET | `/api/signups/my` | 我的报名记录 |
| POST | `/api/checkin/location?activityId=&lng=&lat=` | 定位签到 |
| POST | `/api/checkin/qr?activityId=&code=` | 扫码签到 |
| POST | `/api/checkin/out?activityId=&lng=&lat=` | 签退 |

### 需管理员
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/activities` | 创建活动 |
| PUT | `/api/activities/{id}/publish` | 发布活动 |
| GET | `/api/signups/activity/{activityId}` | 查看活动报名名单 |
| GET | `/api/checkin/qrcode/{activityId}` | 生成签到二维码码 |
| PUT | `/api/checkin/verify-hours/{signupId}` | 审核志愿时长 |

统一响应格式：`{ code: 200, message: "success", data: T }`（Result 类）

---

## 7. 前端路由树 + 新增目录

```
/components/
  └── map/
      ├── BaseMap.vue          # 基础地图容器（封装 MapLibre + 天地图）
      └── ActivityLayer.vue    # 活动点图层（GeoJSON → circle markers）
/composables/
  └── useMap.ts               # 地图实例生命周期管理（含 onUnmounted 清理）
/config/
  └── map.ts                  # 集中地图配置（天地图 Key/中心/zoom，从 env 读取）
/types/
  └── geo.ts                  # GeoJSON TS 类型（FeatureCollection/Feature/Geometry）
.--- 视图 ---

/login              → Login.vue       (guest only)
/register           → Register.vue    (guest only)
/                   → Layout.vue
  ├─ /              → Map.vue         (地图主页，组合 BaseMap + ActivityLayer)
  ├─ /activity/:id  → ActivityDetail.vue
  ├─ /my-signups    → MySignups.vue
  ├─ /checkin       → CheckIn.vue
  └─ /admin         → Admin.vue       (role: admin)
      └─ /admin/create-activity → CreateActivity.vue

/mock/                              # MSW 前端 Mock（脱离后端独立运行）
  ├── README.md
  ├── browser.ts                    # setupWorker 入口
  ├── db.ts                         # 内存 DB（localStorage 持久化）
  ├── data/seed.ts                  # 种子数据（8 活动，2 用户，4 报名）
  └── handlers/                     # 17 个 API Handler
      ├── auth.ts / activities.ts / map.ts / signups.ts / checkin.ts
```

路由守卫逻辑：`router.beforeEach`
1. 有 token 但无 user → 调 `/api/auth/me` 获取用户
2. 访问 `meta.role === 'admin'` → 校验 role 是否为 admin
3. 已登录访问 guest 页 → 重定向到 /

---

## 8. 关键业务规则

### 报名
- 活动 status 必须为 `published`，signedCount < maxParticipants
- @Transactional 包裹：插入 signup + signedCount++（原子操作）
- 防止重复报名（LambdaQuery 查 user_id + activity_id）

### 签到
- 定位签到：前端 `navigator.geolocation.getCurrentPosition()` 获取 GPS
- 后端 Haversine 公式校验距离 ≤ 500m（中国矿业大学直径约2km）
- 扫码签到：Base64 解码 → 比对 `"CHECKIN:" + activityId`

### 签退
- 状态必须为 `signed_in`
- 自动计算志愿时长 = sign_out_time - sign_in_time → 转小时 → BigDecimal 保留1位

### 管理员
- 初始账号由 `DataInitializer` (ApplicationRunner) 创建：admin / admin123
- 注册用户的默认角色：student
- 管理员无法通过注册途径产生

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
|  已完成 | 用户注册/登录、JWT 鉴权、RBAC、活动 CRUD + 发布、GeoJSON API、活动地图、报名/取消、定位签到/签退、Haversine 距离校验、签退自动算时长 |
|  新增 | 后端 `geo` 横切模块（SpatialCalculator/GeoJsonBuilder/ActivitySpatialRepository）、前端 Map 组件化（BaseMap/ActivityLayer/useMap）、地图配置外置（.env + config/map.ts）、配置占位（application.yml ${ENV}）、前端 MSW Mock（17 端点，localStorage 持久化，脱离后端运行） |
|  待开发 | 封面图片上传、签到二维码前端生成、时长审核完整流程、站内信通知、状态定时更新（published→ongoing→ended） |
|  Phase 2 | 小程序接入、审核工作流（院级→校级）、Redis 缓存、PostGIS 迁移（有 ActivitySpatialRepository 抽象锁，只换 Impl） |

---

## 11. 文件路径映射

| 文件 | 定位 |
|------|------|
| `volunteer-server/pom.xml` | Maven 依赖 |
| `volunteer-server/src/main/resources/application.yml` | 后端配置（支持 ${ENV} 占位） |
| `volunteer-server/src/main/resources/db/init.sql` | 数据库 DDL |
| `volunteer-server/src/main/java/.../geo/model/` | GeoPoint, FeatureCollection, Feature, Geometry POJO |
| `volunteer-server/src/main/java/.../geo/service/SpatialCalculator.java` | Haversine 距离计算 |
| `volunteer-server/src/main/java/.../geo/service/GeoJsonBuilder.java` | GeoJSON 构造器 |
| `volunteer-server/src/main/java/.../geo/repository/ActivitySpatialRepository.java` | 空间查询抽象（PostGIS 锚点） |
| `volunteer-web/vite.config.ts` | Vite 配置（代理+别名） |
| `volunteer-web/.env.development` | 开发环境变量（已提交，默认开 Mock） |
| `volunteer-web/.env.example` | 环境变量模板 |
| `volunteer-web/src/config/map.ts` | 地图集中配置（key/center/zoom/style） |
| `volunteer-web/src/types/geo.ts` | GeoJSON TS 类型 |
| `volunteer-web/src/composables/useMap.ts` | MapLibre 实例生命周期 |
| `volunteer-web/src/components/map/BaseMap.vue` | 基础地图容器 |
| `volunteer-web/src/components/map/ActivityLayer.vue` | 活动点图层 |
| `volunteer-web/src/views/Map.vue` | 地图主页（组合 BaseMap+ActivityLayer） |
| `volunteer-web/src/router/index.ts` | 路由表+守卫 |
| `volunteer-web/src/stores/user.ts` | Pinia 用户状态 |
| `volunteer-web/src/api/index.ts` | Axios 实例+拦截器 |
| `volunteer-web/src/mock/README.md` | MSW Mock 使用说明 |
| `volunteer-web/src/mock/db.ts` | Mock 内存 DB（localStorage） |
| `volunteer-web/src/mock/data/seed.ts` | Mock 种子数据 |
| `volunteer-web/src/mock/handlers/` | MSW API Handler（auth/activities/map/signups/checkin） |
| `volunteer-web/src/mock/browser.ts` | MSW setupWorker 入口 |
| `doc/API_REFERENCE.md` | 完整 API 参考文档 |
