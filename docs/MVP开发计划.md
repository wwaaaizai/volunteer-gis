# MVP 开发计划

**基于GIS的校园志愿活动服务系统**

2026年6月

---

# 一、MVP最小模型定义

## 1.1 核心原则

> **能跑起来 > 架构完美**
>
> MVP的目标是构建一个可演示的端到端流程：**发布活动 → 地图展示 → 报名 → 签到 → 时长统计**。所有"锦上添花"的中间件和功能在MVP阶段一律砍掉，后续按需加回。

## 1.2 MVP功能清单

### 保留（Core）

| 功能 | 说明 |
|------|------|
| 用户注册/登录 | 学号+密码，JWT认证，bcrypt存储 |
| 两级角色 | 学生 + 管理员（管理员合并组织者、审核员职能） |
| 活动CRUD | 管理员创建/编辑/发布活动，含地图选点 |
| 活动地图展示 | MapLibre GL JS + 天地图底图，标注已发布活动点 |
| 活动详情与报名 | 学生浏览活动详情，一键报名（数据库级名额校验） |
| 签到签退 | 二维码签到 + 浏览器定位签到，签到时间记录 |
| 志愿时长 | 签到-签退时间差自动计算，管理员审核 |
| 个人中心 | 我的报名、我的时长统计 |
| 管理员后台 | 活动审核、报名名单查看、时长审核 |

### 砍掉（MVP不做）

| 原需求 | 砍掉原因 | 替代方案 |
|--------|---------|---------|
| Redis | 校园场景并发量低，数据库扛得住 | MySQL直接计数 |
| RabbitMQ | 同步操作足够 | 接口内直接调用 |
| Elasticsearch | 校园活动量少（月均百级） | MySQL LIKE + 全文索引 |
| MinIO | 图片量小 | Spring Boot本地文件目录 |
| Docker / K8s | 本机开发，无需容器化 | 裸机直接跑 |
| Nginx | Spring Boot内嵌Tomcat够用 | 一个端口跑全部 |
| 两级审核 | MVP阶段简化流程 | 管理员直接审核 |
| 四级RBAC | 学生+管理员两角色足够演示 | 按需加回 |
| 活动聚类（Cluster） | 校园活动量少 | 直接标注Marker |
| 候补补位 | 边缘场景 | 后期加 |
| 活动搜索联想 | 需ES支撑 | 简单关键词搜索 |
| 时段冲突检测 | 增加复杂度 | 提示但不阻止 |
| ELK日志栈 | 开发阶段console够了 | logback输出文件 |
| Prometheus/Grafana | 运维工具，非演示必需 | 无 |
| 可视化大屏 | 锦上添花 | 简单统计表格替代 |
| PDF证明生成 | PDF库学习成本高 | HTML页面 + 浏览器打印 |
| 微信小程序 | 巨大工作量 | Web响应式覆盖移动端 |
| 短信验证码 | 需对接短信服务商 | 图形验证码（免费） |
| 统一身份认证对接 | 需学校配合 | 自行注册登录 |

## 1.3 MVP技术栈（简化版）

| 原始技术栈 | MVP技术栈 | 变化说明 |
|-----------|----------|---------|
| MySQL 8.0 + Spatial | **MySQL 8.0 + Spatial** | 保留 |
| Redis 7.x | **无** | 数据库直接查，并发低无需缓存 |
| RabbitMQ 3.12 | **无** | 同步调用，无异步需求 |
| Elasticsearch 8.x | **无** | MySQL LIKE搜索 |
| MinIO | **无** | Spring Boot本地 `uploads/` 目录 |
| Docker + K8s | **无** | Windows裸机运行 |
| Nginx | **无** | Spring Boot内嵌Tomcat |
| 天地图API | **天地图API** | 保留（GIS核心依赖） |
| Spring Boot 3.2 | **Spring Boot 3.2** | 保留 |
| Vue 3 + Element Plus | **Vue 3 + Element Plus** | 保留 |
| MapLibre GL JS | **MapLibre GL JS** | 保留 |
| JWT (RS256) | **JWT (HS256)** | 改用对称加密，减少密钥管理 |

**MVPS**仅需安装3样东西：

```
Windows 本机
├── Java 17          (跑 Spring Boot)
├── MySQL 8.0        (跑数据库)
└── Node.js 18+      (跑 Vue 3 开发服务器 / 构建)
```

---

# 二、Windows本机作为服务器的可行性分析

## 2.1 结论：完全可行

Spring Boot、MySQL、Node.js 三者均对 Windows 有一流支持。对于毕业设计/课程项目的开发与演示场景，Windows 本机完全够用，不需要 Linux 服务器。

## 2.2 各组件在Windows下的表现

| 组件 | Windows兼容性 | 说明 |
|------|-------------|------|
| Java 17 | 原生支持 | Oracle JDK / OpenJDK 均有 Windows 安装包 |
| MySQL 8.0 | 原生支持 | MySQL Installer for Windows，图形化安装 |
| MySQL Spatial | 原生支持 | Windows版MySQL 8.0包含完整Spatial扩展 |
| Spring Boot | 原生支持 | 基于JVM，跨平台一致 |
| Vue 3 + Vite | 原生支持 | Node.js on Windows 完全正常 |
| MapLibre GL JS | 无关 | 运行在浏览器中，与服务器OS无关 |
| 天地图API | 无关 | 浏览器端发起HTTP请求，与服务器OS无关 |
| 文件路径 | 注意分隔符 | Java `File.separator` / Spring `ResourceUtils` 自动处理 |

## 2.3 演示时的网络方案

```
方案一（同一局域网）：
  你的Windows本机 ←── 其他同学的电脑/手机浏览器
  http://192.168.x.x:8080

方案二（仅本机演示）：
  浏览器打开 http://localhost:8080
  切换到移动端视图（Chrome F12 → 手机模拟）
```

**注意**：如果使用天地图API，需在天地图开发者平台将 `localhost` 或本机IP加入Key的白名单。天地图对 `localhost` 默认放行，一般无需额外配置。

## 2.4 本机方案的限制（可接受）

| 限制 | 影响 | 应对 |
|------|------|------|
| 关机后服务停 | 其他人无法访问 | 演示时开机即可 |
| 无域名/HTTPS | 某些浏览器API可能受限（如Geolocation需HTTPS） | `localhost` 被浏览器视为安全上下文，Geolocation可用 |
| 性能 | 大量并发扛不住 | 演示场景只有几个人访问，完全没问题 |
| 数据安全 | 本机硬盘故障可能丢数据 | 定期导出SQL备份到U盘/云盘 |

## 2.5 推荐环境配置

```
操作系统：Windows 10/11 64位
Java：     JDK 17 (Oracle 或 Eclipse Temurin)
MySQL：    MySQL 8.0.33+ Community Server
Node.js：  18.x LTS
IDE：      IntelliJ IDEA Community + VS Code
数据库工具：MySQL Workbench（免费）
浏览器：   Chrome 90+（用于开发调试）
```


# 三、开发启动清单

## 3.1 第一步要做的事

```
□ 1. 安装 JDK 17
□ 2. 安装 MySQL 8.0，创建数据库 volunteer_db
□ 3. 安装 Node.js 18 LTS
□ 4. 用 Spring Initializr 生成项目骨架
     - Spring Boot 3.2
     - 依赖：Spring Web, Spring Security, MySQL Driver, MyBatis-Plus, Validation
□ 5. 用 Vite 创建 Vue 3 + TypeScript 项目
     - 依赖：Vue Router, Element Plus, MapLibre GL JS, Axios
□ 6. 跑通第一个接口：后端 Hello World + 前端调用
□ 7. 设计并执行数据库建表 SQL
□ 8. 申请天地图 API Key
```

## 3.2 第一个里程碑

> **第2周末，以下流程可跑通：**
> 1. 浏览器打开 `http://localhost:5173`
> 2. 看到登录页面
> 3. 输入学号+密码 → 登录成功 → 跳转到活动地图页
> 4. 地图加载天地图底图（即使还没活动数据）

达到这个状态后，后续都是在已有框架上"加功能"，开发速度会明显加快。

---

*本文档的MVP范围可根据实际开发进度动态调整。在保证"地图展示+活动流程"核心链路完整的前提下，允许简化或推迟非关键功能。*
