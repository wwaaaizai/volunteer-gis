# Mock Service Worker（MSW）前端 Mock

本项目使用 [MSW v2](https://mswjs.io/) 在浏览器层拦截所有 `/api/*` 请求，
**无需启动后端（Spring Boot）和 MySQL 即可完整运行前端**。

## 快速开始

```bash
cd volunteer-web
npm install
npm run dev
# 浏览器自动打开 http://localhost:5173
```

默认 `.env.development` 中 `VITE_USE_MOCK=true`，MSW 自动启用。

## 切换真实后端

编辑 `.env.development` 或创建 `.env.local`：

```env
VITE_USE_MOCK=false
```

重启 dev server 后请求将走 vite proxy → `http://localhost:8080`。

## 种子数据

| 账号 | 密码 | 角色 |
|------|------|------|
| `admin` | `admin123` | 管理员 |
| `student` | `123456` | 学生 |

预置 8 个活动（published / ongoing / draft / ended / cancelled 多状态），
student 账号有 4 条报名记录（signed / signed_in / signed_out / cancelled）。

## 数据持久化

Mock 数据存储在 `localStorage`（key: `volunteer-mock-db`），
**刷新页面不丢失**（报名、发布、签到等写操作会持久化）。

如需重置为初始状态，在浏览器控制台执行：

```js
localStorage.removeItem('volunteer-mock-db')
location.reload()
```

## 覆盖的 API

严格对照 `doc/API_REFERENCE.md` 实现，共 17 个端点：

- **认证** (3)：register / login / me
- **活动** (5)：list / detail / create / publish / search
- **地图** (1)：activities GeoJSON
- **报名** (4)：signup / cancel / my / activity-signups
- **签到** (5)：location / qr / out / qrcode / verify-hours

## 空间逻辑复刻

Mock 中复刻了后端 geo 模块的空间计算逻辑：

| 后端 | Mock | 说明 |
|------|------|------|
| `GeoJsonBuilder` | `handlers/map.ts` | Activity → GeoJSON Point 构造 |
| `SpatialCalculator` (Haversine) | `handlers/checkin.ts` | 500m 签到距离校验 |

两者保持一致的阈值和算法，证明 geo 抽象的可移植性。

## 目录结构

```
src/mock/
├── browser.ts        # MSW worker 入口
├── db.ts              # 内存数据库（localStorage 持久化）
├── data/
│   └── seed.ts        # 种子数据
└── handlers/
    ├── index.ts       # Handler 汇总
    ├── auth.ts        # 认证
    ├── activities.ts  # 活动管理
    ├── map.ts         # GeoJSON
    ├── signups.ts     # 报名
    └── checkin.ts     # 签到签退
```
