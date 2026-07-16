# 基于GIS的校园志愿活动服务系统

> 面向中国矿业大学的校园志愿活动管理平台。学生可以在地图上浏览和报名志愿活动，管理员可以发布活动、管理签到和统计志愿时长。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 (Composition API) | ^3.4 |
| 前端语言 | TypeScript | ^5.3 |
| 构建工具 | Vite | ^5.1 |
| UI 组件库 | Element Plus | ^2.5 |
| GIS 地图 | MapLibre GL JS + 天地图 WMTS | ^4.0 |
| 状态管理 | Pinia | ^2.1 |
| HTTP 客户端 | Axios | ^1.6 |
| 后端框架 | Spring Boot | 3.2.0 |
| 持久层 | MyBatis-Plus | 3.5.5 |
| 安全框架 | Spring Security 6 + JWT (jjwt 0.12.3) | — |
| 数据库 | MySQL 8.0 | utf8mb4 |
| 运行时 | JDK 17 | — |

## 项目结构

```
volunteer-gis/
├── volunteer-web/                # Vue 3 前端
│   ├── src/
│   │   ├── views/                # 页面组件 (9个)
│   │   ├── components/map/      # 地图组件 (BaseMap + ActivityLayer)
│   │   ├── composables/         # 组合式函数 (useMap)
│   │   ├── stores/              # Pinia 状态管理
│   │   ├── api/                 # Axios 请求封装
│   │   ├── mock/                # MSW 前端 Mock 数据
│   │   ├── types/               # TypeScript 类型定义
│   │   └── config/              # 地图配置
│   └── package.json
│
├── volunteer-server/             # Spring Boot 后端
│   └── src/main/java/com/cumt/volunteer/
│       ├── common/              # 公共类 (JWT工具、全局异常、统一响应)
│       ├── config/              # Security 配置 + 初始数据注入
│       ├── entity/              # 数据库实体 (User, Activity, Signup, Message)
│       ├── upm/                 # 子系统: 用户与权限 (注册/登录/JWT)
│       ├── aca/                 # 子系统: 活动创建与审核
│       ├── am/                  # 子系统: 活动地图 (数据库 → GeoJSON)
│       ├── abm/                 # 子系统: 报名管理 (报名/取消/名额)
│       ├── apm/                 # 子系统: 签到签退 (定位/扫码/时长计算)
│       └── geo/                 # GIS 公共模块 (Haversine距离/GeoJSON构造)
│
├── doc/                         # 技术参考文档 (API/数据库/前端结构)
├── docs/                        # 项目文档 (开发计划/ADR/文档规范)
└── CLAUDE.md                    # AI 编码规范与文档同步指令
```

## 快速开始

### 环境要求

| 软件 | 版本 | 必需 |
|------|------|------|
| Git | 最新版 | ✅ |
| Node.js | 18.x LTS | ✅ |
| JDK 17 | 17.x | 仅后端/联调 |
| MySQL 8.0 | 8.0.x | 仅后端/联调 |
| IntelliJ IDEA | Community | 推荐（后端开发） |

### 方式一：纯前端 Mock 模式（推荐入门）

无需安装 Java、MySQL，适合前端开发和新人上手。

```bash
# 克隆项目
git clone https://github.com/wwaaaizai/volunteer-gis.git
cd volunteer-gis/volunteer-web

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

浏览器打开 `http://localhost:5173`，即可看到登录页面。

> **原理**：`.env.development` 中 `VITE_USE_MOCK=true`，前端使用 MSW 拦截所有 `/api/*` 请求，返回模拟数据，数据持久化在 `localStorage`。

**内置测试账号：**

| 账号 | 密码 | 角色 |
|------|------|------|
| `admin` | `admin123` | 管理员（发布活动/管理签到） |
| `student` | `123456` | 学生（报名/签到） |

### 方式二：完整模式（前后端联调）

> **适用**：已安装 MySQL 8.0 + JDK 17 + Node.js，需要真实数据库存储。

#### 前置准备

1. **安装 MySQL 8.0**，root 密码设为 `Cumt123`（或自行修改 `application.yml`）
2. **安装 JDK 17**，配置 `JAVA_HOME` 环境变量
3. **申请天地图 Key**（见下方说明），写入 `.env.local`

#### 每次启动步骤

⚠️ **以下三个终端窗口都要保持运行，不能关闭。**

```
终端1（MySQL）：
    # MySQL 设为 Windows 服务开机自启即可，一般不需要手动操作
    # 检查是否运行：netstat -ano | findstr 3306
    # 如果未运行：net start MySQL

终端2（后端，端口 9090）：
    cd volunteer-server
    mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
     # powershell
    .\mvnw.cmd spring-boot:run "-Dserver.port=9090"
    # 看到 "Started VolunteerApplication" 表示启动成功

终端3（前端，端口 5173）：
    cd volunteer-web
    npm install          # 仅首次需要
    npx vite --port 5173
    # 看到 "Local: http://localhost:5173/" 表示启动成功
```

**浏览器打开 `http://localhost:5173`**

#### 验证是否启动成功

| 端口 | 检查方式 |
|------|---------|
| 3306 | `netstat -ano \| findstr 3306` |
| 9090 | `curl http://localhost:9090/api/auth/me` |
| 9091 | geoserver |
| 5173 | 浏览器打开 `http://localhost:5173` |

#### 配置文件

| 文件 | 作用 | 需创建？ |
|------|------|---------|
| `volunteer-web/.env.local` | 前端环境变量 | 是（已被 gitignore） |
| `volunteer-server/src/main/resources/application.yml` | 后端配置 | 否 |

`.env.local` 示例：
```
VITE_USE_MOCK=false
VITE_TIANDITU_KEY=你的天地图Key
```

### 天地图 API Key

天地图是自然资源部提供的免费地图服务。申请步骤：

1. 注册 [天地图账号](https://uums.tianditu.gov.cn/register)
2. 进入 [控制台](https://console.tianditu.gov.cn/) 创建应用，类型选"浏览器端"
3. 将获得的 Key 填入 `volunteer-web/.env.local` 的 `VITE_TIANDITU_KEY=` 后

## 页面一览

| 路由 | 页面 | 说明 |
|------|------|------|
| `/login` | 登录页 | 未登录用户 |
| `/register` | 注册页 | 未登录用户 |
| `/` | 地图主页 | 查看地图上的活动标注 |
| `/activity/:id` | 活动详情 | 活动信息 + 报名按钮 |
| `/my-signups` | 我的报名 | 个人报名记录 |
| `/checkin` | 签到签退 | GPS定位签到 + 签退 |
| `/admin` | 管理后台 | 活动管理（仅管理员） |
| `/admin/create-activity` | 创建活动 | 发布新活动（仅管理员） |

## 核心业务流程

```
学生视角：
  浏览地图 → 点击活动 → 报名 → 活动当天到现场 → GPS定位签到 → 活动结束签退 → 自动计算志愿时长

管理员视角：
  创建活动 → 发布 → 管理报名 → 现场签到管理 → 审核时长
```

**地图工作原理：**
1. 前端请求 `GET /api/map/activities`
2. 后端将活动数据转换为 GeoJSON FeatureCollection
3. 前端在 MapLibre 地图上绘制活动标注点
4. 点击标注跳转到活动详情页

**签到规则：** 浏览器获取 GPS 定位，后端使用 Haversine 公式计算与活动地点的距离，≤ 500m 允许签到。

## API 概览

| 模块 | 端点 | 说明 |
|------|------|------|
| 认证 | `POST /api/auth/register` | 注册 |
| 认证 | `POST /api/auth/login` | 登录 → 返回 JWT |
| 认证 | `GET /api/auth/me` | 获取当前用户 |
| 活动 | `GET/POST /api/activities` | 活动列表 / 创建 |
| 活动 | `GET /api/activities/{id}` | 活动详情 |
| 活动 | `PUT /api/activities/{id}/publish` | 发布活动（管理员） |
| 活动 | `GET /api/activities/search` | 搜索活动 |
| 地图 | `GET /api/map/activities` | 地图 GeoJSON 数据 |
| 报名 | `POST/DELETE /api/signups` | 报名 / 取消 |
| 报名 | `GET /api/signups/my` | 我的报名 |
| 签到 | `POST /api/checkin/location` | GPS 定位签到 |
| 签到 | `POST /api/checkin/qr` | 扫码签到 |
| 签到 | `POST /api/checkin/out` | 签退 |

完整请求/响应格式见 [`doc/API_REFERENCE.md`](doc/API_REFERENCE.md)。

## 开发指南

### 分支与提交

```bash
# 创建个人分支
git checkout -b dev_你的名字

# 日常提交流程
git add <具体文件>
git commit -m "feat: 新增xxx功能"
git push
# 在 GitHub 上发起 Pull Request
```

**提交信息格式：**

```
feat: 新增活动搜索功能
fix: 修复地图标注不显示的bug
docs: 更新API文档
style: 调整登录页样式
refactor: 重构签到距离计算逻辑
```

### 分工建议

- **前端开发**：使用 Mock 模式，修改 `volunteer-web/src/views/` 下的页面，新增 API 时在 `src/mock/handlers/` 添加模拟数据
- **后端开发**：启动 MySQL + 后端即可，用 Postman 测试 API
- **联调**：前端 `.env.local` 设 `VITE_USE_MOCK=false`，后端运行在 `localhost:8080`

### 推荐学习路径

**前端：** Vue 3 基础 → TypeScript 入门 → Element Plus → MapLibre GL JS

**后端：** Java 基础 → Spring Boot → MyBatis-Plus → MySQL → Spring Security

## 常见问题

| 问题 | 解决方案 |
|------|---------|
| `npm install` 很慢 | `npm config set registry https://registry.npmmirror.com` |
| 地图不显示 | 检查 `VITE_TIANDITU_KEY` 是否正确，天地图控制台是否添加了 `localhost` 白名单 |
| Mock 模式登录后 401 | 清除浏览器 localStorage（F12 → Application → Clear Storage） |
| MySQL 连接失败 | 确认 MySQL 服务已启动，密码与 `application.yml` 一致 |
| 端口 8080 被占用 | `netstat -ano | findstr 8080` 查找并关闭占用进程 |
| `mvnw` 命令不识别 | 确认 JDK 17 已安装，`JAVA_HOME` 环境变量已设置 |
| 拉代码有冲突 | `git stash` → `git pull` → `git stash pop` |

## 文档索引

| 文档 | 路径 | 说明 |
|------|------|------|
| 项目速览 | [`doc/AI_CONTEXT.md`](doc/AI_CONTEXT.md) | 架构、技术栈、业务规则、文件映射 |
| API 参考 | [`doc/API_REFERENCE.md`](doc/API_REFERENCE.md) | 完整的 REST 端点文档 |
| 数据库参考 | [`doc/DATABASE.md`](doc/DATABASE.md) | 表结构、索引、状态枚举 |
| 前端结构 | [`doc/FRONTEND_STRUCTURE.md`](doc/FRONTEND_STRUCTURE.md) | 路由、组件树、状态管理 |
| MVP 开发计划 | [`docs/MVP开发计划.md`](docs/MVP开发计划.md) | 功能范围与里程碑 |
| 文档导航 | [`CONTEXT-MAP.md`](CONTEXT-MAP.md) | 所有文档的索引与阅读指引 |
| 架构决策记录 | [`docs/adr/`](docs/adr/) | 重大架构决策的记录 |

## 许可证

本项目仅供学习交流使用。

---

*最后更新：2026年6月*
