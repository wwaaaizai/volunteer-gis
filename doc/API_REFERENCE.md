# API 参考文档 — 校园志愿活动服务系统

> 完整 API 接口清单。统一响应格式：`{ code: 200, message: "success", data: T }`
> 认证方式：`Authorization: Bearer <token>`（Header）
> Base Path: `http://localhost:8080`

---

## 1. 认证模块 (UPM) — `/api/auth`

### POST `/api/auth/register` — 注册
- **鉴权**: 无
- **Content-Type**: `application/json`
```json
// Request
{
  "studentId": "08230001",
  "password": "123456",
  "name": "张三",
  "phone": "13800138000"
}
// Response
{ "code": 200, "message": "注册成功", "data": null }
```
- **业务规则**: 默认角色=student；学号唯一性校验；密码 bcrypt 加密存储

### POST `/api/auth/login` — 登录
- **鉴权**: 无
```json
// Request
{ "studentId": "08230001", "password": "123456" }
// Response
{ "code": 200, "message": "success", "data": { "token": "eyJhbGciOiJIUzI1NiJ9..." } }
```
- **Token 格式**: JWT HS256，Payload: `{ sub: userId, studentId, role, iat, exp }`，有效期 24h

### GET `/api/auth/me` — 获取当前用户
- **鉴权**: Bearer Token
```json
// Response
{
  "code": 200, "message": "success",
  "data": { "userId": "1", "studentId": "admin", "role": "admin" }
}
```

---

## 2. 活动地图 (AM) — `/api/map`

### GET `/api/map/activities` — 活动 GeoJSON
- **鉴权**: Bearer Token
- **查询**: 只返回 `status = 'published'` 的活动
```json
// Response data (GeoJSON FeatureCollection)
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": { "type": "Point", "coordinates": [117.1500000, 34.2200000] },
      "properties": {
        "id": 1,
        "title": "图书馆整理",
        "locationName": "图书馆一楼",
        "startTime": "2026-06-20T09:00:00"
      }
    }
  ]
}
```

---

## 3. 活动管理 (ACA) — `/api/activities`

### GET `/api/activities` — 活动列表
- **鉴权**: Bearer Token
- **查询**: 只返回 `published` 状态，按 created_at 降序
```json
// Response data: Activity[]
[
  {
    "id": 1,
    "title": "图书馆整理",
    "description": "整理图书...",
    "locationName": "图书馆一楼",
    "longitude": 117.1500000,
    "latitude": 34.2200000,
    "startTime": "2026-06-20T09:00:00",
    "endTime": "2026-06-20T12:00:00",
    "maxParticipants": 50,
    "signedCount": 12,
    "status": "published",
    "creatorId": 1,
    "createdAt": "2026-06-15T10:00:00"
  }
]
```

### GET `/api/activities/{id}` — 活动详情
- **鉴权**: Bearer Token
- **Response data**: Activity（单条，同上结构）

### POST `/api/activities` — 创建活动
- **鉴权**: Bearer Token + `@PreAuthorize("hasRole('admin')")`
- **Content-Type**: `application/json`
```json
// Request
{
  "title": "图书馆整理",
  "description": "整理图书并分类",
  "locationName": "图书馆一楼",
  "longitude": 117.15,
  "latitude": 34.22,
  "startTime": "2026-06-20T09:00:00",
  "endTime": "2026-06-20T12:00:00",
  "maxParticipants": 50
}
// Response: { "code": 200, "message": "创建成功", "data": null }
```
- **默认值**: status = `draft`, signedCount = 0, creatorId = 当前用户

### PUT `/api/activities/{id}/publish` — 发布活动
- **鉴权**: Bearer Token + `@PreAuthorize("hasRole('admin')")`
- **业务规则**: 仅将 status 从 `draft` 改为 `published`
```json
// Response: { "code": 200, "message": "发布成功", "data": null }
```

### GET `/api/activities/search?keyword=xxx` — 搜索
- **鉴权**: Bearer Token
- **查询**: 模糊匹配 title + status = published，MVP 阶段用 MySQL LIKE
```json
// Response data: Activity[]（与列表接口相同结构）
```

---

## 4. 报名管理 (ABM) — `/api/signups`

### POST `/api/signups?activityId=1` — 报名
- **鉴权**: Bearer Token
- **业务规则**（事务控制）:
  1. 校验活动存在且 status = published
  2. 校验 signedCount < maxParticipants
  3. 查重（同一用户不可重复报名）
  4. 插入 signup (status = signed) + activity.signedCount++
```json
// Response: { "code": 200, "message": "报名成功", "data": null }
```

### DELETE `/api/signups?activityId=1` — 取消报名
- **鉴权**: Bearer Token
- **业务规则**（事务控制）:
  1. 查询 user_id + activity_id + status = `signed` 的记录
  2. 将 status 改为 `cancelled`
  3. activity.signedCount--
```json
// Response: { "code": 200, "message": "已取消报名", "data": null }
```

### GET `/api/signups/my` — 我的报名
- **鉴权**: Bearer Token
```json
// Response data: Signup[]
[
  {
    "id": 1,
    "activityId": 1,
    "userId": 2,
    "status": "signed_out",
    "signInTime": "2026-06-20T09:05:00",
    "signInLng": 117.1500100,
    "signInLat": 34.2200200,
    "signOutTime": "2026-06-20T12:00:00",
    "signOutLng": 117.1500100,
    "signOutLat": 34.2200200,
    "volunteerHours": 2.9,
    "hourVerified": false,
    "createdAt": "2026-06-16T08:00:00"
  }
]
```

### GET `/api/signups/activity/{activityId}` — 活动报名名单
- **鉴权**: Bearer Token + `@PreAuthorize("hasRole('admin')")`
- **Response data**: Signup[]（同上结构）

---

## 5. 签到签退 (APM) — `/api/checkin`

### POST `/api/checkin/location?activityId=1&lng=117.15&lat=34.22` — 定位签到
- **鉴权**: Bearer Token
- **业务规则**:
  1. 查询报名记录，status 必须为 `signed`
  2. Haversine 距离 ≤ 500m（与活动经纬度比对）
  3. 更新 status=`signed_in`, sign_in_time, sign_in_lng/lat
```json
// Response: { "code": 200, "message": "签到成功", "data": null }
// 距离过远: { "code": 500, "message": "距离活动地点太远，请到达活动地点后签到（当前距离: 1234米）" }
```

### POST `/api/checkin/qr?activityId=1&code=Q0hFQ0tJTjox` — 扫码签到
- **鉴权**: Bearer Token
- **业务规则**:
  1. Base64 解码 code → 比对 `"CHECKIN:" + activityId`
  2. 更新 status=`signed_in`, sign_in_time
```json
// Response: { "code": 200, "message": "签到成功", "data": null }
// 无效二维码: { "code": 500, "message": "签到二维码无效" }
```

### POST `/api/checkin/out?activityId=1&lng=117.15&lat=34.22` — 签退
- **鉴权**: Bearer Token
- **业务规则**:
  1. 查询报名记录，status 必须为 `signed_in`
  2. 获取定位坐标
  3. 更新 status=`signed_out`, sign_out_time, sign_out_lng/lat
  4. 自动计算 volunteer_hours = Duration.between(signInTime, signOutTime) → 保留1位小数
```json
// Response: { "code": 200, "message": "签退成功", "data": null }
```

### GET `/api/checkin/qrcode/{activityId}` — 生成签到码
- **鉴权**: Bearer Token + `@PreAuthorize("hasRole('admin')")`
- **返回**: Base64 编码字符串，内容为 `"CHECKIN:" + activityId`
```json
// Response data (String): "Q0hFQ0tJTjox"
// 前端用此值生成二维码（"CHECKIN:1" 的 Base64）
```

### PUT `/api/checkin/verify-hours/{signupId}` — 审核时长
- **鉴权**: Bearer Token + `@PreAuthorize("hasRole('admin')")`
- **业务规则**: 将 hour_verified 设为 true
```json
// Response: { "code": 200, "message": "审核通过", "data": null }
```

---

## 6. 错误处理

### 后端统一格式
```json
{ "code": 500, "message": "报名已满", "data": null }
{ "code": 400, "message": "title: 请输入活动标题; locationName: 请输入活动地点", "data": null }
```

### 前端拦截器处理
- `response.data.code !== 200` → ElMessage.error(message)
- HTTP `401` → 清除 token → router.push('/login')

### Activity 状态枚举
| 值 | 含义 |
|----|------|
| draft | 草稿（创建后默认，不可报名） |
| published | 已发布（可报名） |
| ongoing | 进行中 |
| ended | 已结束 |
| cancelled | 已取消 |

### Signup 状态枚举
| 值 | 含义 |
|----|------|
| signed | 已报名 |
| signed_in | 已签到 |
| signed_out | 已签退 |
| cancelled | 已取消 |
