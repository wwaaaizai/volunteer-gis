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
// Request（学生注册）
{
  "studentId": "08230001",
  "password": "123456",
  "name": "张三",
  "phone": "13800138000",
  "grade": "2024",
  "college": "计算机科学与技术学院"
}
// Request（组织者申请注册，含 applyAsOrganizer）
{
  "studentId": "08230002",
  "password": "123456",
  "name": "李四",
  "phone": "13800138001",
  "grade": "2023",
  "college": "矿业工程学院",
  "applyAsOrganizer": true,
  "organization": "校团委",
  "employeeId": "T001",
  "reason": "负责校园志愿活动组织工作"
}
// Response
{ "code": 200, "message": "注册成功", "data": null }
```
- **业务规则**: 默认角色=student；学号唯一性校验；密码 bcrypt 加密存储；若 `applyAsOrganizer=true` 则同时创建 `organizer_apply` 待审批记录

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
  "data": { "userId": "1", "studentId": "admin", "role": "admin", "name": "管理员", "phone": "13800000000", "grade": "", "college": "", "organization": null, "totalHours": "0" }
}
```

### GET `/api/auth/organizer-applies` — 查看组织者申请列表
- **鉴权**: Bearer Token + `@PreAuthorize("hasRole('admin')")`
```json
// Response data: OrganizerApply[]
[
  {
    "id": 1, "userId": 3, "organization": "校团委", "reason": "负责校园志愿活动组织工作",
    "status": "pending", "createdAt": "2026-07-01T10:00:00"
  }
]
```

### PUT `/api/auth/organizer-applies/{id}/review` — 审批组织者申请
- **鉴权**: Bearer Token + `@PreAuthorize("hasRole('admin')")`
```json
// Request
{ "status": "approved" }
// 或
{ "status": "rejected" }
// Response
{ "code": 200, "message": "审批通过", "data": null }
```
- **业务规则**: 通过时自动更新 user.role 为 `organizer`；拒绝时仅更新申请状态

### PUT `/api/auth/profile` — 更新个人信息
- **鉴权**: Bearer Token + `@PreAuthorize("hasRole('admin')")`
```json
// Request
{ "name": "新名字", "phone": "13900000000", "organization": "校学生会" }
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

### GET `/api/map/heatmap?category=&months=` — 活动热力图
- **鉴权**: Bearer Token
- **查询参数**: `category`（可选：environmental/support/education/community/campus）、`months`（可选：时间范围月数，默认 6）
- **Response data**: GeoJSON FeatureCollection（Point），含 `weight` 属性（基于志愿时长）
```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": { "type": "Point", "coordinates": [117.150, 34.220] },
      "properties": { "weight": 2.5 }
    }
  ]
}
```

### GET `/api/map/layers` — 可用 GIS 图层清单
- **鉴权**: 无
- **Response data**: 图层元数据数组（当前为静态数据，预留 GeoServer 对接）
```json
[
  { "id": "campus:buildings", "name": "校园建筑", "type": "wms", "visible": true },
  { "id": "campus:roads", "name": "校园道路", "type": "wms", "visible": false },
  { "id": "campus:greenland", "name": "校园绿地", "type": "wms", "visible": false },
  { "id": "campus:water", "name": "校园水系", "type": "wms", "visible": false },
  { "id": "campus:poi", "name": "校园POI", "type": "wms", "visible": false }
]
```

### GET `/api/map/campus-bounds` — 校区边界
- **鉴权**: 无
```json
// Response
{
  "code": 200, "message": "success",
  "data": {
    "wgs84": { "sw": [117.112, 34.199], "ne": [117.156, 34.235] },
    "gcj02": { "sw": [117.118, 34.197], "ne": [117.162, 34.233] }
  }
}
```

### GET `/api/map/check-bounds?lng=&lat=` — 校验坐标是否在校区内
- **鉴权**: 无
```json
// Response
{ "code": 200, "message": "success", "data": true }
```

---

## 3. 活动管理 (ACA) — `/api/activities`

### GET `/api/activities` — 活动列表
- **鉴权**: Bearer Token
- **查询参数**: `showAll`（可选，默认 false；设 `true` 返回全部状态的活动，供管理员后台使用）
- **查询**: 默认只返回 `published` 状态，按 created_at 降序
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
    "volunteerHours": 3.0,
    "targetGrade": "ALL",
    "targetCollege": "ALL",
    "organizationName": "校团委",
    "signedCount": 12,
    "status": "published",
    "creatorId": 1,
    "createdAt": "2026-06-15T10:00:00"
  }
]
```

### GET `/api/activities/{id}` — 活动详情
- **鉴权**: Bearer Token
- **Response data**: Activity（含 category、tags、coverImage、organizerId、checkinRegion 等 Phase 2 字段）
```json
{
  "id": 1, "title": "图书馆整理", "description": "整理图书...",
  "locationName": "图书馆一楼", "longitude": 117.15, "latitude": 34.22,
  "startTime": "2026-06-20T09:00:00", "endTime": "2026-06-20T12:00:00",
  "signupStart": "2026-06-15T00:00:00", "signupEnd": "2026-06-19T23:59:59",
  "maxParticipants": 50, "volunteerHours": 3.0,
  "targetGrade": "ALL", "targetCollege": "计算机学院",
  "organizationName": "校团委", "proposal": null,
  "signedCount": 12, "coverImage": "/uploads/cover_xxx.jpg",
  "category": "education", "tags": "图书,整理",
  "status": "published", "creatorId": 1, "organizerId": 1,
  "checkinRegion": null,
  "createdAt": "2026-06-15T10:00:00"
}
```

### POST `/api/activities` — 创建活动
- **鉴权**: Bearer Token + `@PreAuthorize("hasRole('admin') or hasRole('organizer')")`
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
  "signupStart": "2026-06-15T00:00:00",
  "signupEnd": "2026-06-19T23:59:59",
  "maxParticipants": 50,
  "volunteerHours": 3.0,
  "targetGrade": "ALL",
  "targetCollege": "计算机学院",
  "organizationName": "校团委",
  "proposal": null,
  "category": "education",
  "tags": "图书,整理,校园",
  "coverImage": "/uploads/cover_xxx.jpg"
}
// Response: { "code": 200, "message": "创建成功", "data": null }
```
- **默认值**: status = `draft`, signedCount = 0, creatorId = organizerId = 当前用户

### PUT `/api/activities/{id}` — 编辑活动
- **鉴权**: Bearer Token + 组织者或管理员（仅允许编辑自己创建的活动）
- **业务规则**: 草稿可全编辑；已发布仅可修改描述和封面
```json
// Request: 与创建请求相同结构（部分字段）
// Response: { "code": 200, "message": "更新成功", "data": null }
```

### PUT `/api/activities/{id}/publish` — 发布活动
- **鉴权**: Bearer Token + `@PreAuthorize("hasRole('admin') or hasRole('organizer')")`
- **业务规则**: 仅将 status 从 `draft` 改为 `published`
```json
// Response: { "code": 200, "message": "发布成功", "data": null }
```

### DELETE `/api/activities/{id}` — 删除活动
- **鉴权**: Bearer Token + `@PreAuthorize("hasRole('admin')")`（仅管理员）
- **业务规则**: 逻辑删除（设置 deleted=1），已删除的活动不可恢复
```json
// Response: { "code": 200, "message": "删除成功", "data": null }
```

### GET `/api/activities/my` — 我的活动（组织者视角）
- **鉴权**: Bearer Token + 组织者或管理员
- **Response data**: Activity[]，按状态分组（草稿/已发布/进行中/已结束）

### GET `/api/activities/search?keyword=xxx` — 搜索
- **鉴权**: Bearer Token
- **查询**: 模糊匹配 title + status = published
```json
// Response data: Activity[]
```

### GET `/api/activities/nearby?lng=117.15&lat=34.22&radius=3000` — 附近活动推荐
- **鉴权**: Bearer Token
- **查询**: 在指定半径内查找 published 活动，按距离升序
```json
// Response data: Activity[]（含 distance 字段）
```

### 签到围栏接口

#### PUT `/api/activities/{id}/geofence` — 保存签到围栏
- **鉴权**: Bearer Token + 组织者或管理员（仅允许编辑自己创建的活动）
- **Content-Type**: `application/json`
```json
// Request: GeoJSON Polygon
{
  "type": "Polygon",
  "coordinates": [[[117.150, 34.220], [117.160, 34.220], [117.160, 34.225], [117.150, 34.225], [117.150, 34.220]]]
}
// Response: { "code": 200, "message": "围栏保存成功", "data": null }
```

#### GET `/api/activities/{id}/geofence` — 获取签到围栏
- **鉴权**: Bearer Token
- **Response data**: GeoJSON Polygon（null 表示使用圆形兜底）

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
- **鉴权**: Bearer Token + `@PreAuthorize("hasRole('admin') or hasRole('organizer')")`
- **Response data**: Signup[]（含用户姓名、状态、签到时间等）

### PUT `/api/signups/{id}/review` — 审核报名
- **鉴权**: Bearer Token + 组织者或管理员
```json
// Request: { "status": "approved" } 或 { "status": "rejected", "reason": "人数已满" }
// Response: { "code": 200, "message": "审核通过", "data": null }
```

### GET `/api/signups/my-footprint` — 志愿足迹
- **鉴权**: Bearer Token
```json
// Response data: FootprintItem[]
[
  {
    "activityId": 1, "activityTitle": "图书馆整理",
    "signInTime": "2026-06-20T09:05:00",
    "signInLng": 117.15001, "signInLat": 34.22002,
    "volunteerHours": 2.9
  }
]
```

---

## 5. 签到签退 (APM) — `/api/checkin`

### POST `/api/checkin/location?activityId=1&lng=117.15&lat=34.22` — 定位签到
- **鉴权**: Bearer Token
- **业务规则**:
  1. 查询报名记录，status 必须为 `approved`
  2. **围栏优先**: 若活动设了 `checkin_region`（GeoJSON Polygon），使用射线法判定 GPS 是否在多边形内
  3. **圆形兜底**: 无围栏时，Haversine 距离 ≤ 500m（与活动经纬度比对）
  4. 更新 status=`signed_in`, sign_in_time, sign_in_lng/lat
```json
// Response: { "code": 200, "message": "签到成功", "data": null }
// 距离过远: { "code": 500, "message": "距离活动地点太远，请到达活动地点后签到（当前距离: 1234米）" }
// 不在围栏内: { "code": 500, "message": "您不在签到范围内，无法签到" }
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
| signed | 已报名（待审核） |
| approved | 已通过（可签到） |
| rejected | 已拒绝 |
| signed_in | 已签到 |
| signed_out | 已签退 |
| cancelled | 已取消 |
