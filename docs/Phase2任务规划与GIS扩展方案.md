# Phase 2 任务规划与 GIS 功能扩展方案

> **基础代码状态**：Phase 1 MVP 已完成（用户注册/登录、活动 CRUD、地图浏览、报名、签到签退、时长统计）
> **本文档目标**：(1) 规范化任务规划体系 (2) 细化当前 4 项需求 (3) 提出可扩展的 GIS 功能模块方案
>
> 2026年6月

---

# 第一部分：任务规划规范

## 1.1 任务分层标准

所有开发任务按以下四层划分，每层有明确的交付物和验收标准：

| 层级 | 名称 | 说明 | 验收标准 |
|------|------|------|----------|
| **L0** | 基础设施 | 数据库、配置、环境、依赖 | 环境可启动，配置可切换 |
| **L1** | 核心链路 | 端到端业务流程可走通 | 完整流程无报错 |
| **L2** | 功能完善 | 边界条件、异常处理、UI 体验 | 异常有提示，操作可撤销 |
| **L3** | 体验增强 | 动画、性能、可扩展性 | 操作流畅，文档齐全 |

## 1.2 任务编号规范

```
[P{阶段}-{子系统缩写}-{序号}] {层级标签} 任务描述
```

**子系统缩写对照**：

| 缩写 | 子系统 | 说明 |
|------|--------|------|
| UPM | User & Permission | 用户与权限 |
| ACA | Activity Create & Audit | 活动创建与审核 |
| AM | Activity Maps | 活动地图 |
| ABM | Activity Booking | 报名管理 |
| APM | Activity Process | 签到签退 |
| GEO | GIS Common | GIS 公共模块 |
| MSG | Message | 站内信通知 |

**示例**：`[P2-UPM-01] 活动组织者角色与权限`、`[P2-AM-03] 地图视图范围限制`

## 1.3 分支与提交映射

```
分支命名:  feature/P2-{缩写}-{简短描述}
提交前缀:  feat: / fix: / refactor: / docs:
关联文档:  每个 P2 子系统完成时同步更新 doc/ 对应文档
```

---

# 第二部分：当前 4 项需求详细任务拆解

## 需求 ① 完善登录系统，创建活动组织者身份

### 现状分析

- 当前仅有 `student` / `admin` 两种角色（[User.java](../volunteer-server/src/main/java/com/cumt/volunteer/entity/User.java)）
- `DataInitializer` 仅创建 admin 初始账号
- 注册默认角色为 student，admin 无法通过注册产生

### 差距分析

1. 缺少 `organizer`（活动组织者）角色
2. 缺少组织者注册/申请审批流程
3. 缺少组织者专属权限和界面路由
4. 管理员需能够管理组织者身份

### 任务清单

| 编号 | 层级 | 任务 | 涉及文件 | 预计工时 |
|------|------|------|---------|----------|
| P2-UPM-01 | L0 | 扩展 `user.role` 枚举为 `student / organizer / admin` | entity/User.java, db/init.sql | 0.5d |
| P2-UPM-02 | L0 | 添加组织者初始权限配置（Security 角色映射） | config/SecurityConfig.java | 0.5d |
| P2-UPM-03 | L1 | 注册页面增加"组织者申请"选项，提交时回填机构/工号 | views/Login.vue, AuthController | 1d |
| P2-UPM-04 | L1 | 管理员后台增加"组织者审批"Tab，审核通过才能获得 organizer 角色 | Admin.vue, UserService | 1d |
| P2-UPM-05 | L1 | 前端路由守卫支持 `meta.role = ['admin', 'organizer']` | router/index.ts | 0.5d |
| P2-UPM-06 | L2 | 组织者个人信息管理页（修改联系方式、所属机构） | 新页面 OrganizerProfile.vue | 1d |
| P2-UPM-07 | L3 | 组织者操作日志（创建活动、编辑活动记录） | entity/OperationLog, 新表 | 1d |

### 数据库变更

```sql
-- 扩展 role 列注释（无需改 DDL，VARCHAR 已支持新值）
ALTER TABLE user MODIFY COLUMN role VARCHAR(16) NOT NULL DEFAULT 'student'
    COMMENT 'student / organizer / admin';

-- 新增：组织者申请表（用于审批流）
CREATE TABLE organizer_apply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    organization VARCHAR(64) COMMENT '所属机构',
    reason TEXT COMMENT '申请理由',
    status VARCHAR(16) NOT NULL DEFAULT 'pending'
        COMMENT 'pending / approved / rejected',
    reviewed_by BIGINT COMMENT '审核管理员 ID',
    created_at DATETIME NOT NULL DEFAULT NOW(),
    updated_at DATETIME NOT NULL ON UPDATE NOW(),
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 需求 ② 在活动组织者身份界面完成活动创建子系统

### 现状分析

- 活动创建页面 [CreateActivity.vue](../volunteer-web/src/views/CreateActivity.vue) 仅从 `/admin` 路由进入
- 当前表单仅有：标题、描述、地点名、经纬度（数字输入）、时间、人数上限
- 没有活动分类、封面图、所需技能、组织者专属视图

### 差距分析

1. 组织者需要独立的仪表盘，而非混用管理员界面
2. 活动创建缺少：封面图上传、活动分类标签、地图选点
3. 组织者应能查看自己创建的活动列表及报名情况
4. 缺少活动编辑功能（草稿状态可修改）

### 任务清单

| 编号 | 层级 | 任务 | 涉及文件 | 预计工时 |
|------|------|------|---------|----------|
| P2-ACA-01 | L0 | 活动表增加 `category`、`tags`、`organizer_id` 字段 | entity/Activity.java, init.sql | 0.5d |
| P2-ACA-02 | L1 | 新建组织者专属路由 `/organizer` 及仪表盘视图 | OrganizerDashboard.vue, router/index.ts | 1.5d |
| P2-ACA-03 | L1 | "我的活动"列表（按状态 Tab：草稿/已发布/进行中/已结束） | OrganizerDashboard.vue, ActivityController | 1d |
| P2-ACA-04 | L1 | 重构活动创建页：增加地图选点组件（替代数字输入经纬度） | CreateActivity.vue, MapPicker.vue | 2d |
| P2-ACA-05 | L1 | 活动创建增加：分类下拉、标签输入、封面图上传 | CreateActivity.vue, FileUploadController | 1.5d |
| P2-ACA-06 | L2 | 活动编辑功能（草稿可全编辑，已发布仅可改描述/封面） | CreateActivity.vue 复用, ActivityService | 1.5d |
| P2-ACA-07 | L2 | 组织者视角活动详情页（含报名名单、签到统计） | OrganizerActivityDetail.vue | 1d |
| P2-ACA-08 | L3 | 活动模板功能（保存模板快速创建同类活动） | activity_template 表, 新页面 | 1.5d |

### 前端路由新增

```
/organizer                    → OrganizerDashboard.vue  (role: organizer)
  ├─ /organizer/create        → CreateActivity.vue
  └─ /organizer/activity/:id  → OrganizerActivityDetail.vue
```

---

## 需求 ③ 活动地图子系统完成调用 GeoServer 发布的图层

### 现状分析

- 当前地图仅使用天地图 WMTS 栅格瓦片底图 + 前端 GeoJSON 活动标注层
- MapController 返回的 GeoJSON 来源于数据库查询，不经过 GeoServer
- 地图配置集中在 [config/map.ts](../volunteer-web/src/config/map.ts)，仅支持 tianditu-vec / tianditu-cva 两个栅格源

### 差距分析

1. 需要接入 GeoServer 发布的 WMS/WFS 图层（校园建筑、道路、绿地等）
2. 需要图层管理机制（默认显示、透明度、叠加顺序）
3. 后端需 GeoServer 代理或前端直连 CORS 配置
4. 天地图底图 + GeoServer 业务图层混合渲染

### 任务清单

| 编号 | 层级 | 任务 | 涉及文件 | 预计工时 |
|------|------|------|---------|----------|
| P2-AM-01 | L0 | 安装配置 GeoServer，发布校园矢量数据（Shapefile→PostGIS→WMS/WFS） | GeoServer 环境 | 2d |
| P2-AM-02 | L0 | 准备校园 GIS 数据：建筑轮廓、道路、绿地、水系、POI | QGIS/PostGIS | 2d |
| P2-AM-03 | L1 | 前端 map.ts 新增 GeoServer WMS/WFS 图层源配置工厂函数 | config/map.ts | 1d |
| P2-AM-04 | L1 | BaseMap 支持多图层叠加（底图 + WMS + 标注层） | components/map/BaseMap.vue | 1.5d |
| P2-AM-05 | L1 | 新增 GeoServerLayer 组件（封装 WMS Tile 请求，管理透明度/显隐） | components/map/GeoServerLayer.vue | 1d |
| P2-AM-06 | L1 | 后端新增 `/api/map/layers` 接口，返回可用图层清单 | MapController, MapService | 0.5d |
| P2-AM-07 | L2 | 图层控制面板（地图右上角抽屉：开关/透明度/图例） | components/map/LayerControl.vue | 2d |
| P2-AM-08 | L2 | GeoServer CORS + 前端 WFS GetFeature 点击查询建筑信息 | SecurityConfig, Map.vue | 1.5d |
| P2-AM-09 | L3 | 活动创建地图选点叠加 GeoServer 建筑图层辅助定位 | MapPicker.vue | 1d |
| P2-AM-10 | L3 | 图层缓存与离线降级（Service Worker 缓存瓦片） | vite.config.ts, sw.js | 2d |

### 地图图层架构（改造后）

```
MapLibre Map
├── [底图层] 天地图 vec_w / cva_w（栅格 WMTS）          ← 保留
├── [业务层] GeoServer WMS 图层组                        ← 新增
│   ├── campus:buildings     校园建筑面
│   ├── campus:roads         校园道路线
│   ├── campus:greenland     校园绿地面
│   ├── campus:water         校园水系面
│   └── campus:poi           校园 POI 点
└── [标注层] GeoJSON 活动标注点（前端 Circle layer）     ← 保留
```

### GeoServer 数据准备

```
数据来源优先级：
  ① OSM 导出 + QGIS 裁剪（免费，推荐）
  ② 学校基建处 CAD 图纸 → GIS 转换（如有授权）
  ③ 天地图矢量瓦片解析（技术可行但数据量有限）
  ④ 手动标注（工作量大，不推荐）

推荐流程：
  OSM 导出 → QGIS 裁剪至校区范围 → 分层 → SLD 配图
  → 导入 PostGIS → GeoServer 发布 WMS/WFS
```

---

## 需求 ④ 限制地图显示范围在矿大南湖校区附近，显示中心在学校内

### 现状分析

- 当前 `DEFAULT_CENTER` 为 `[117.2050, 34.2173]`（WGS84），zoom 为 14
- 天地图 WMTS 瓦片可自由拖拽到全球任意位置
- 没有地图边界约束

### 差距分析

1. 需精确设定矿大南湖校区的经纬度边界框
2. 限制地图可拖拽范围（maxBounds）
3. 限制最小/最大缩放级别
4. 边界外数据可能存在安全问题

### 任务清单

| 编号 | 层级 | 任务 | 涉及文件 | 预计工时 |
|------|------|------|---------|----------|
| P2-AM-11 | L0 | 确定矿大南湖校区精确边界框（bBox）坐标 | 实地勘测/GIS 数据 | 0.5d |
| P2-AM-12 | L1 | 配置 MapLibre `maxBounds` 限制地图可拖拽范围 | config/map.ts, useMap.ts | 0.5d |
| P2-AM-13 | L1 | 配置 `minZoom`/`maxZoom` 限制缩放级别 | config/map.ts | 0.25d |
| P2-AM-14 | L2 | 地图拖拽边界视觉反馈（超出弹性回弹提示） | useMap.ts | 0.5d |
| P2-AM-15 | L2 | 后端校验：活动创建/签到时经纬度是否在校区范围内 | SpatialCalculator, ActivityService | 0.5d |
| P2-AM-16 | L3 | 校区边界可视化图层（半透明边框叠加） | ActivityLayer.vue 或新组件 | 1d |

### 边界参数配置（待实地确认）

```typescript
// config/map.ts 新增

/** 矿大南湖校区 WGS-84 边界框（待精确测绘） */
export const CAMPUS_BOUNDS_WGS84: [[number, number], [number, number]] = [
  [117.1950, 34.2100],  // 西南角 [lng, lat]
  [117.2150, 34.2240],  // 东北角 [lng, lat]
]

// GCJ-02 边界（用于 MapLibre maxBounds）
export const CAMPUS_BOUNDS_GCJ02: [[number, number], [number, number]] = [
  wgs84ToGcj02(...CAMPUS_BOUNDS_WGS84[0]),
  wgs84ToGcj02(...CAMPUS_BOUNDS_WGS84[1]),
]

/** 缩放限制 */
export const MIN_ZOOM = 13
export const MAX_ZOOM = 19
```

### 后端校验

```java
// SpatialCalculator.java 新增
public boolean isWithinCampus(double lng, double lat) {
    return lng >= CAMPUS_LNG_MIN && lng <= CAMPUS_LNG_MAX
        && lat >= CAMPUS_LAT_MIN && lat <= CAMPUS_LAT_MAX;
}
```

---

# 第三部分：可增加的 GIS 功能开发模块方案

> 按优先级分三档：**P0（本阶段必做）**、**P1（下阶段推荐）**、**P2（远期规划）**

---

## 模块 A：校园空间数据管理子系统 [P1]

### 模块定位

为校园 GIS 系统提供持续的数据维护能力，解决"谁来更新地图数据"的问题。

### 功能点

| 编号 | 功能 | 描述 | 工作量 |
|------|------|------|--------|
| GIS-A01 | 建筑信息管理 | CRUD 校园建筑（楼名、楼层、用途、GeoJSON 轮廓），同步刷新 GeoServer 图层 | 3d |
| GIS-A02 | POI 标注管理 | 管理校园兴趣点（校门、食堂、超市、ATM 等），支持图标自定义 | 2d |
| GIS-A03 | 图层样式配置 | 管理员在线调整 GeoServer SLD 样式，无需直接编辑 XML | 3d |
| GIS-A04 | 数据导入导出 | 支持 GeoJSON/KML/Shapefile 格式的校园数据导入导出 | 2d |

### 技术要点

- 后端新增 `campus_building`、`campus_poi` 两张空间数据表
- 利用 MySQL 8.0 Spatial 扩展（`GEOMETRY` 类型 + 空间索引）
- GeoServer REST API 程序化刷新图层
- 前端用 MapLibre `fill` / `line` / `symbol` 图层渲染

---

## 模块 B：志愿活动热力图与空间分析 [P1]

### 模块定位

从"地图看活动"升级到"地图分析活动"，为管理员和学校团委提供决策支持。

### 功能点

| 编号 | 功能 | 描述 | 工作量 |
|------|------|------|--------|
| GIS-B01 | 活动热力图 | 基于历史活动位置生成热力图，展示志愿活动高频区域 | 2d |
| GIS-B02 | 服务覆盖率分析 | 以建筑为单位统计志愿活动覆盖情况，标注"服务盲区" | 2.5d |
| GIS-B03 | 时段空间分布 | 按学期/月份筛选，动态展示活动空间分布的时序变化 | 2d |
| GIS-B04 | 人数空间统计 | 按区域聚合志愿者人数，可视化人力资源分布 | 1.5d |

### 技术要点

- 前端使用 MapLibre `heatmap` 图层类型
- 后端 `ActivitySpatialRepository` 新增空间聚合查询
- 前端 Chart.js/ECharts 配合地图做联动统计面板

---

## 模块 C：路径导航与可达性分析 [P1]

### 模块定位

为学生提供从当前位置到活动地点的校内步行/骑行路线规划。

### 功能点

| 编号 | 功能 | 描述 | 工作量 |
|------|------|------|--------|
| GIS-C01 | 校内路径导航 | 基于校园路网数据，计算最短路径 | 4d |
| GIS-C02 | 步行时间估算 | 根据路径距离和步行速度估算到达时间 | 1d |
| GIS-C03 | 无障碍路线 | 标注无障碍通道（坡道、电梯），提供替代路线 | 2d |
| GIS-C04 | 集合点推荐 | 根据报名学生宿舍分布，推荐最优集合点 | 2d |

### 技术要点

- 需要校园路网矢量数据（可从 OSM 或 CAD 提取）
- 后端实现 Dijkstra/A* 算法，或使用 pgRouting（PostGIS 迁移后）
- 前端 `line` 图层 + 箭头标注动态绘制路线
- 首次可降级为"直线距离 + 简单提示"，Phase 3 上真实路网

---

## 模块 D：三维校园可视化 [P2]

### 模块定位

从 2D 平面地图升级到 3D 校园场景，增强沉浸式体验。

### 功能点

| 编号 | 功能 | 描述 | 工作量 |
|------|------|------|--------|
| GIS-D01 | 建筑白模 | 基于建筑轮廓 + 高度属性生成 3D 建筑体块 | 3d |
| GIS-D02 | 活动飞线 | 3D 飞线从报名学生位置汇聚到活动地点 | 2d |
| GIS-D03 | 第一人称漫游 | 校园 3D 场景自由行走，用于新生熟悉校园 | 5d |
| GIS-D04 | 楼层室内图 | 关键建筑（如图书馆）的楼层平面图叠加 | 5d |

### 技术要点

- MapLibre `fill-extrusion` layer（2.5D）或 Cesium.js（全 3D）
- 数据需要建筑高度字段（可从 OSM `building:levels` 估计）
- 渐进式：先 MapLibre 2.5D 挤出，后 Cesium 全 3D

---

## 模块 E：空间围栏与签到增强 [P0]

### 模块定位

将签到距离校验从「以活动点为圆心 500m」升级为「指定地理围栏范围」，解决户外大型活动中签到区域不规则问题。

### 功能点

| 编号 | 功能 | 描述 | 工作量 |
|------|------|------|--------|
| GIS-E01 | 围栏绘制工具 | 活动组织者在地图上绘制多边形签到区域 | 2d |
| GIS-E02 | 围栏内签到校验 | 后端使用射线法（Ray Casting）判定 GPS 是否在多边形内 | 1.5d |
| GIS-E03 | 多区域签到 | 同一活动支持多个签到区域（主会场/分会场） | 1.5d |
| GIS-E04 | 围栏可视化 | 地图上半透明填充面展示签到范围 | 1d |

### 技术要点

- 前端使用 MapLibre GL Draw 插件实现多边形绘制
- 活动表新增 `checkin_region` 字段（存储 GeoJSON Polygon，NULL 兜底圆形）
- 后端 `SpatialCalculator` 新增 `isPointInPolygon()` 方法
- 兼容现有圆形签到逻辑（无围栏时保持 Haversine 500m）

---

## 模块 F：实时位置共享与安全管理 [P2]

### 模块定位

大型志愿活动期间，志愿者可选共享位置，组织者在地图上实时查看人员分布。

### 功能点

| 编号 | 功能 | 描述 | 工作量 |
|------|------|------|--------|
| GIS-F01 | 位置共享开关 | 志愿者自主开启/关闭（仅活动期间有效） | 1.5d |
| GIS-F02 | 实时位置地图 | 组织者地图上不同颜色标注在线志愿者（轮询/WebSocket） | 2.5d |
| GIS-F03 | 电子围栏预警 | 志愿者超出活动区域时自动通知组织者 | 1.5d |
| GIS-F04 | 一键集合 | 组织者发"集合"指令，志愿者收到推送+路线 | 3d |

### 隐私设计

- 位置共享仅活动期间有效，活动结束后自动清除轨迹
- 需明确用户授权协议

---

## 模块 G：志愿足迹与个人时空档案 [P1]

### 模块定位

为学生提供"我的志愿足迹"时空回顾，增强成就感和个人品牌。

### 功能点

| 编号 | 功能 | 描述 | 工作量 |
|------|------|------|--------|
| GIS-G01 | 志愿足迹地图 | 标记所有参与活动位置，按时间连线 | 2d |
| GIS-G02 | 时空统计面板 | 按学期/学年统计次数、时长、覆盖区域数 | 1.5d |
| GIS-G03 | 志愿成就徽章 | 时空数据触发成就（"覆盖全校10栋建筑"等） | 2d |
| GIS-G04 | 志愿地图海报 | Canvas 导出"我的志愿地图"分享图 | 1d |

### 技术要点

- 从 signup 表提取历史签到经纬度
- MapLibre `line` 图层按时间顺序连线 + `symbol` 标注序号
- Combine Canvas 导出海报图片

---

## 模块 H：天气与户外活动适配 [P2]

### 模块定位

接入天气 API，为户外志愿活动提供天气参考和预警。

### 功能点

| 编号 | 功能 | 描述 | 工作量 |
|------|------|------|--------|
| GIS-H01 | 天气图层叠加 | 地图叠加实时天气图层（降水雷达等） | 2d |
| GIS-H02 | 户外活动建议 | 创建户外活动时自动检查活动日期天气预报 | 1d |
| GIS-H03 | 天气预警推送 | 活动前 24h 获取天气，恶劣天气推站内信 | 1.5d |

---

# 第四部分：推荐实施路线图

## Phase 2.0 — 基础改造（当前阶段，约 3 周）

```
第1周: 需求① — UPM 角色扩展 + 组织者注册审批
       P2-UPM-01 ~ P2-UPM-05  组织者角色全链路
       产出：可注册/审批组织者，组织者登录独立界面

第2周: 需求② — ACA 组织者活动子系统
       P2-ACA-01 ~ P2-ACA-07  组织者仪表盘 + 活动创建优化
       产出：组织者完整创建/管理自己发布的活动

第3周: 需求③④ — AM 地图子系统升级
       P2-AM-01 ~ P2-AM-08    GeoServer 图层集成
       P2-AM-11 ~ P2-AM-15    地图边界限制
       产出：地图叠加校园建筑图层，范围限制在校区内
```

## Phase 2.1 — GIS 增强（约 4 周）

```
第4-5周: 模块E（空间围栏 P0） + 模块A（空间数据管理 P1）
第6-7周: 模块B（热力图分析 P1） + 模块C（路径导航 P1）
```

## Phase 3 — 体验升级（约 4 周，远期规划）

```
第8-9周:  模块G（志愿足迹 P1） + 模块F（位置共享 P2）
第10-11周: 模块D（3D校园 P2） + 模块H（天气集成 P2）
```

---

# 第五部分：风险与应对

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| GeoServer 学习曲线陡峭 | 阻碍需求③ | 中 | 准备 GeoServer Docker 镜像，编写配置 SOP |
| 校园 GIS 数据获取困难 | 模块 A/B/C/D 缺数据 | 高 | 优先用 OSM 免费数据 QGIS 裁剪；CAD 图纸备选 |
| 浏览器 Geolocation 精度不足 | 签到围栏判定不准 | 中 | 保留 500m 圆形兜底，围栏适当扩大缓冲区 |
| 前端多图层叠加性能下降 | 低端设备卡顿 | 低 | 按需加载，默认只显示底图+活动标注 |
| WGS84↔GCJ02 坐标转换混乱 | 位置偏移 | 中 | 数据库存 WGS84，前端统一转 GCJ02，后端全部 WGS84 |

---

# 附录：文档更新清单

Phase 2 开发过程中需同步更新：

| 文档 | 更新内容 |
|------|---------|
| `doc/API_REFERENCE.md` | 新增组织者审批、GeoServer 图层、围栏等 API |
| `doc/DATABASE.md` | 新增 `organizer_apply`、`campus_building`、`campus_poi` 等表 |
| `doc/FRONTEND_STRUCTURE.md` | 新增组织者路由、地图子组件、图层控制面板 |
| `doc/AI_CONTEXT.md` | 更新技术栈（GeoServer）、子系统架构、角色模型 |
| `CONTEXT-MAP.md` | 新增本文档索引 |
| `docs/adr/` | 新建 ADR：GeoServer 引入决策、围栏策略选型 |

---

*本文档为 Phase 2 开发总纲，每个模块的详细技术方案在启动前评审确认。*
