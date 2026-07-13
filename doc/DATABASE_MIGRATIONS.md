# 数据库迁移指引 — 校园志愿活动服务系统

> 本文档记录所有数据库增量变更，供多设备协作时快速同步 schema。
> 完整 DDL → `volunteer-server/src/main/resources/db/init.sql`

---

## 快速上手

### 全新设备 / 首次建库

在 MySQL 中执行 init.sql 即可一次性创建所有表和列：

```sql
SOURCE volunteer-server/src/main/resources/db/init.sql;
```

`CREATE TABLE IF NOT EXISTS` 支持重复执行，已存在的表会被跳过。

### 增量同步（已有数据库、仅需追上新变更）

找到下方变更日志中你**上次同步之后**的 Phase，依次执行对应的 SQL 块。

> **提示**：不记得上次同步到哪了？直接重新 `SOURCE init.sql` 即可，
> CREATE TABLE 会被跳过，迁移块使用 `INFORMATION_SCHEMA` 检测列是否存在，可安全重复执行。

---

## 变更日志

### Phase 2 ACA — activity 表扩充字段

**关联提交**: `e39df51` feat(aca,ai,abm): AI描述生成+AI封面+报名审核+活动模板+年常活动预设

**变更**：`activity` 表新增 `organizer_id`、`category`、`tags` 三列。

```sql
USE volunteer_db;

ALTER TABLE activity ADD COLUMN organizer_id BIGINT COMMENT '组织者ID' AFTER creator_id;
ALTER TABLE activity ADD COLUMN category VARCHAR(32) COMMENT '活动分类：environmental/support/education/community/campus/other' AFTER organizer_id;
ALTER TABLE activity ADD COLUMN tags VARCHAR(255) COMMENT '活动标签，逗号分隔' AFTER category;

-- 为已有活动补全默认值
UPDATE activity SET organizer_id = creator_id WHERE organizer_id IS NULL;
UPDATE activity SET category = '' WHERE category IS NULL;
UPDATE activity SET tags = '' WHERE tags IS NULL;
```

---

### Phase 2 UPM — user 表扩充字段

**关联提交**: `e43f893` feat(upm): P2-UPM 组织者角色全链路 + 校区边界校验

**变更**：`user` 表新增 `organization`、`employee_id` 两列。

```sql
USE volunteer_db;

ALTER TABLE `user` ADD COLUMN organization VARCHAR(64) COMMENT '所属机构（组织者填写）' AFTER role;
ALTER TABLE `user` ADD COLUMN employee_id VARCHAR(32) COMMENT '工号（组织者填写）' AFTER organization;
```

---

### Phase 2 UPM — 新增 organizer_apply 表

**关联提交**: `e43f893` feat(upm): P2-UPM 组织者角色全链路 + 校区边界校验

**变更**：新建 `organizer_apply` 组织者申请表。

```sql
USE volunteer_db;

CREATE TABLE IF NOT EXISTS `organizer_apply` (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT        NOT NULL              COMMENT '申请人用户ID',
    organization VARCHAR(64)                        COMMENT '所属机构',
    reason      TEXT                                COMMENT '申请理由',
    status      VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
    reviewed_by BIGINT                              COMMENT '审核管理员ID',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织者申请表';
```

---

### Phase 2 UPM — 新增 operation_log 表

**关联提交**: `e43f893` feat(upm): P2-UPM 组织者角色全链路 + 校区边界校验

**变更**：新建 `operation_log` 操作日志表。

```sql
USE volunteer_db;

CREATE TABLE IF NOT EXISTS `operation_log` (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT        NOT NULL          COMMENT '操作者用户ID',
    operation_type  VARCHAR(32)                     COMMENT '操作类型：create_activity/edit_activity/publish_activity',
    description     VARCHAR(255)                    COMMENT '操作描述',
    activity_id     BIGINT                          COMMENT '关联活动ID',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
```

---

### Phase 3 — signup 表新增 review_reason

**关联提交**: `e39df51` feat(aca,ai,abm): AI描述生成+AI封面+报名审核+活动模板+年常活动预设

**变更**：`signup` 表新增 `review_reason` 列，用于报名审核拒绝理由。

```sql
USE volunteer_db;

ALTER TABLE signup ADD COLUMN review_reason VARCHAR(255) COMMENT '拒绝理由';
```

---

### Phase 4 — activity 表新增 checkin_region

**关联提交**: 当前工作区（签到围栏功能，尚未提交）

**变更**：`activity` 表新增 `checkin_region` 列，存储签到围栏 GeoJSON。

```sql
USE volunteer_db;

ALTER TABLE activity ADD COLUMN checkin_region TEXT COMMENT '签到围栏 GeoJSON Polygon（JSON字符串，null则使用500m圆形兜底）';
```

---

## 验证

执行完毕后，确认表与列完整：

```sql
USE volunteer_db;

-- 应有 6 张表：user, activity, signup, message, organizer_apply, operation_log
SHOW TABLES;

-- 确认 activity 表包含 checkin_region 列
DESC activity;

-- 确认 signup 表包含 review_reason 列
DESC signup;
```
