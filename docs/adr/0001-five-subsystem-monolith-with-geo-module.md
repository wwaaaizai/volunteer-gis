# 0001. 五子系统单体架构 + geo 横切模块

## 状态

已接受

## 上下文

本项目是一个校园志愿活动服务系统（MVP 阶段），核心功能包括用户管理、活动创建与管理、GIS 地图展示、活动报名、签到签退流程。后端需要在单体 Spring Boot 应用内组织代码，同时 GIS 相关的空间计算（Haversine 距离、GeoJSON 构造）需要独立抽象，以支持未来从 MySQL 内存计算迁移到 PostGIS。

## 决策

采用 5 个业务子系统 + 1 个 geo 横切模块的包结构：

```
volunteer-server/
├── upm/  (User & Permission)        → 用户注册、登录、JWT、角色管理
├── aca/  (Activity Create & Audit)   → 活动创建、状态流转
├── am/   (Activity Maps)            → 活动→GeoJSON 转换
├── abm/  (Activity Booking & Management) → 报名/取消，名额管理
├── apm/  (Activity Process & Management) → 签到/签退/扫码
└── geo/  (GIS 横切模块)             → 空间计算与 GeoJSON 构造
```

- `am/` 和 `apm/` 通过依赖注入使用 `geo/` 模块的 `SpatialCalculator` 和 `GeoJsonBuilder`
- `geo/repository/ActivitySpatialRepository` 接口作为 PostGIS 迁移的抽象锚点，当前由 MySQL 实现（内存 Haversine 过滤）

## 理由

- **子系统划分**：按业务边界（而非技术层）组织包结构，使每个子系统的代码内聚性更高，新成员能快速定位功能代码
- **geo 横切模块**：GIS 逻辑被 `am/` 和 `apm/` 共享，提取为独立模块避免重复；`ActivitySpatialRepository` 接口使得未来 PostGIS 迁移只需替换实现类，业务层零改动
- **单体而非微服务**：MVP 阶段 3 人团队，微服务的运维复杂度不划算；单体内按子系统划分已足够清晰
