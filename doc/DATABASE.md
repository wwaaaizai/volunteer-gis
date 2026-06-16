# 数据库参考 — 校园志愿活动服务系统

> 完整 DDL → [volunteer-server/src/main/resources/db/init.sql](../volunteer-server/src/main/resources/db/init.sql)
> 数据库名称: `volunteer_db`，字符集: `utf8mb4`

---

## 1. 表关系图

```
user (1) ──────< signup >────── (1) activity
  │                                     │
  └──< message                         creator_id → user(id)
```

---

## 2. `user` 用户表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| student_id | VARCHAR(32) | UNIQUE, NN | 学号（登录账号） |
| password | VARCHAR(255) | NN | bcrypt($2a$10$...) |
| name | VARCHAR(64) | NN | 姓名 |
| phone | VARCHAR(16) | | 手机号 |
| role | VARCHAR(16) | NN, DEFAULT 'student' | student / admin |
| total_hours | DECIMAL(8,1) | NN, DEFAULT 0 | 累计志愿时长 |
| deleted | TINYINT(1) | NN, DEFAULT 0 | MyBatis-Plus 逻辑删除 |
| created_at | DATETIME | NN, DEFAULT NOW() | |
| updated_at | DATETIME | NN, ON UPDATE NOW() | |

**索引**: `idx_role (role)`

---

## 3. `activity` 志愿活动表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| title | VARCHAR(128) | NN | 活动标题 |
| description | TEXT | | 活动描述 |
| location_name | VARCHAR(255) | NN | 地点名称（如"图书馆一楼"） |
| longitude | DECIMAL(11,7) | NN | 经度 WGS84 |
| latitude | DECIMAL(10,7) | NN | 纬度 WGS84 |
| start_time | DATETIME | | 活动开始时间 |
| end_time | DATETIME | | 活动结束时间 |
| signup_start | DATETIME | | 报名开始时间 |
| signup_end | DATETIME | | 报名截止时间 |
| max_participants | INT | NN, DEFAULT 50 | 人数上限 |
| signed_count | INT | NN, DEFAULT 0 | 当前报名人数 |
| cover_image | VARCHAR(255) | | 封面图片路径 |
| status | VARCHAR(16) | NN, DEFAULT 'draft' | 见状态枚举 |
| creator_id | BIGINT | | FK → user.id |
| deleted | TINYINT(1) | NN, DEFAULT 0 | |
| created_at | DATETIME | NN | |
| updated_at | DATETIME | NN | |

**索引**: `idx_status(status)`, `idx_status_time(status, created_at)`, `idx_creator(creator_id)`

---

## 4. `signup` 报名记录表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| activity_id | BIGINT | NN | FK → activity.id |
| user_id | BIGINT | NN | FK → user.id |
| status | VARCHAR(16) | NN, DEFAULT 'signed' | 见状态枚举 |
| sign_in_time | DATETIME | | 签到时间 |
| sign_in_lng | DECIMAL(11,7) | | 签到经度 |
| sign_in_lat | DECIMAL(10,7) | | 签到纬度 |
| sign_out_time | DATETIME | | 签退时间 |
| sign_out_lng | DECIMAL(11,7) | | 签退经度 |
| sign_out_lat | DECIMAL(10,7) | | 签退纬度 |
| volunteer_hours | DECIMAL(5,1) | | 志愿时长（h），保留1位 |
| hour_verified | TINYINT(1) | NN, DEFAULT 0 | 时长是否已审核 |
| created_at | DATETIME | NN | |

**索引**: `idx_activity(activity_id)`, `idx_user(user_id)`, `idx_user_activity(user_id, activity_id)`

---

## 5. `message` 站内信表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | NN | FK → user.id |
| title | VARCHAR(128) | NN | 标题 |
| content | TEXT | | 内容 |
| type | VARCHAR(16) | NN, DEFAULT 'system' | system/signup/signin |
| is_read | TINYINT(1) | NN, DEFAULT 0 | 是否已读 |
| created_at | DATETIME | NN | |

**索引**: `idx_user_read(user_id, is_read)`

---

## 6. 状态枚举

### Activity.status
```
draft    → 草稿（创建后默认，仅管理员可见）
published → 已发布（学生可查看、可报名）
ongoing  → 进行中
ended    → 已结束
cancelled → 已取消
```

### Signup.status
```
signed     → 已报名
signed_in  → 已签到
signed_out → 已签退
cancelled  → 已取消
```

### User.role
```
student → 学生（默认角色）
admin   → 管理员（维护。初始账号 admin/admin123 由 DataInitializer 注入）
```

### Message.type
```
system  → 系统通知
signup  → 报名相关
signin  → 签到相关
```

---

## 7. 初始化

执行顺序：
```bash
mysql -u root -p < volunteer-server/src/main/resources/db/init.sql
```

该脚本创建 `volunteer_db` 数据库 + 4 张表。管理员初始账号由 Spring Boot 启动时 `DataInitializer`（`ApplicationRunner`）自动创建，无需手动 INSERT。
