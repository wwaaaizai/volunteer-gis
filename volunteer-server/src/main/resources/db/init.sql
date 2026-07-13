-- ============================================
-- 校园志愿活动服务系统 - 数据库初始化脚本
-- 数据库: MySQL 8.0
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS volunteer_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE volunteer_db;

-- -------------------------------------------
-- 1. 用户表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id  VARCHAR(32)  NOT NULL UNIQUE COMMENT '学号（登录账号）',
    password    VARCHAR(255) NOT NULL           COMMENT '密码（bcrypt）',
    name        VARCHAR(64)  NOT NULL           COMMENT '姓名',
    phone       VARCHAR(16)                     COMMENT '手机号',
    role        VARCHAR(16)  NOT NULL DEFAULT 'student' COMMENT '角色：student/organizer/admin',
    organization VARCHAR(64)                    COMMENT '所属机构（组织者填写）',
    employee_id VARCHAR(32)                     COMMENT '工号（组织者填写）',
    total_hours DECIMAL(8,1) NOT NULL DEFAULT 0 COMMENT '累计志愿时长',
    deleted     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 管理员账号由 Spring Boot 启动时自动创建（DataInitializer），无需手动 INSERT

-- -------------------------------------------
-- 2. 志愿活动表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `activity` (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    title            VARCHAR(128)   NOT NULL              COMMENT '活动标题',
    description      TEXT                                  COMMENT '活动描述',
    location_name    VARCHAR(255)   NOT NULL              COMMENT '活动地点名称',
    longitude        DECIMAL(11,7)  NOT NULL              COMMENT '经度（WGS84）',
    latitude         DECIMAL(10,7)  NOT NULL              COMMENT '纬度（WGS84）',
    start_time       DATETIME                             COMMENT '活动开始时间',
    end_time         DATETIME                             COMMENT '活动结束时间',
    signup_start     DATETIME                             COMMENT '报名开始时间',
    signup_end       DATETIME                             COMMENT '报名截止时间',
    max_participants INT            NOT NULL DEFAULT 50   COMMENT '报名上限',
    signed_count     INT            NOT NULL DEFAULT 0    COMMENT '已报名人数',
    cover_image      VARCHAR(255)                         COMMENT '封面图片路径',
    status           VARCHAR(16)    NOT NULL DEFAULT 'draft' COMMENT '状态：draft/published/ongoing/ended/cancelled',
    creator_id       BIGINT                               COMMENT '创建者ID',
    organizer_id     BIGINT                               COMMENT '组织者ID',
    category         VARCHAR(32)                          COMMENT '活动分类：environmental/support/education/community/campus/other',
    tags             VARCHAR(255)                         COMMENT '活动标签，逗号分隔',
    checkin_region   TEXT                                  COMMENT '签到围栏 GeoJSON Polygon（JSON字符串，null则使用500m圆形兜底）',
    deleted          TINYINT(1)     NOT NULL DEFAULT 0    COMMENT '逻辑删除',
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_status_time (status, created_at),
    INDEX idx_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='志愿活动表';

-- -------------------------------------------
-- 3. 报名记录表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `signup` (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id      BIGINT        NOT NULL              COMMENT '活动ID',
    user_id          BIGINT        NOT NULL              COMMENT '用户ID',
    status           VARCHAR(16)   NOT NULL DEFAULT 'signed' COMMENT '状态：signed/signed_in/signed_out/cancelled',
    sign_in_time     DATETIME                            COMMENT '签到时间',
    sign_in_lng      DECIMAL(11,7)                       COMMENT '签到经度',
    sign_in_lat      DECIMAL(10,7)                       COMMENT '签到纬度',
    sign_out_time    DATETIME                            COMMENT '签退时间',
    sign_out_lng     DECIMAL(11,7)                       COMMENT '签退经度',
    sign_out_lat     DECIMAL(10,7)                       COMMENT '签退纬度',
    volunteer_hours  DECIMAL(5,1)                        COMMENT '志愿时长（小时）',
    review_reason    VARCHAR(255)                         COMMENT '拒绝理由',
    hour_verified    TINYINT(1)     NOT NULL DEFAULT 0   COMMENT '时长是否已审核',
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_activity (activity_id),
    INDEX idx_user (user_id),
    INDEX idx_user_activity (user_id, activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报名记录表';

-- -------------------------------------------
-- 4. 站内信表
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `message` (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT        NOT NULL              COMMENT '用户ID',
    title       VARCHAR(128)  NOT NULL              COMMENT '标题',
    content     TEXT                                COMMENT '内容',
    type        VARCHAR(16)   NOT NULL DEFAULT 'system' COMMENT '类型：system/signup/signin',
    is_read     TINYINT(1)    NOT NULL DEFAULT 0   COMMENT '是否已读',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内信表';

-- -------------------------------------------
-- 5. 组织者申请表（P2-UPM-04）
-- -------------------------------------------
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

-- -------------------------------------------
-- 6. 操作日志表（P2-UPM-07）
-- -------------------------------------------
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
-- -------------------------------------------
-- Phase 2 ACA 增量迁移：activity 表新增字段
-- -------------------------------------------

-- organizer_id
SET @col_o = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'volunteer_db' AND TABLE_NAME = 'activity' AND COLUMN_NAME = 'organizer_id');
SET @sql_o = IF(@col_o = 0,
  'ALTER TABLE activity ADD COLUMN organizer_id BIGINT COMMENT ''组织者ID'' AFTER creator_id',
  'SELECT ''Column organizer_id already exists''');
PREPARE stmt_o FROM @sql_o; EXECUTE stmt_o; DEALLOCATE PREPARE stmt_o;

-- category
SET @col_c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'volunteer_db' AND TABLE_NAME = 'activity' AND COLUMN_NAME = 'category');
SET @sql_c = IF(@col_c = 0,
  'ALTER TABLE activity ADD COLUMN category VARCHAR(32) COMMENT ''活动分类'' AFTER organizer_id',
  'SELECT ''Column category already exists''');
PREPARE stmt_c FROM @sql_c; EXECUTE stmt_c; DEALLOCATE PREPARE stmt_c;

-- tags
SET @col_t = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'volunteer_db' AND TABLE_NAME = 'activity' AND COLUMN_NAME = 'tags');
SET @sql_t = IF(@col_t = 0,
  'ALTER TABLE activity ADD COLUMN tags VARCHAR(255) COMMENT ''活动标签，逗号分隔'' AFTER category',
  'SELECT ''Column tags already exists''');
PREPARE stmt_t FROM @sql_t; EXECUTE stmt_t; DEALLOCATE PREPARE stmt_t;

-- 为已有活动补全新增字段默认值
UPDATE activity SET organizer_id = creator_id WHERE organizer_id IS NULL;
UPDATE activity SET category = '' WHERE category IS NULL;
UPDATE activity SET tags = '' WHERE tags IS NULL;

-- ============================================
-- Phase 3 增量迁移：报名审核 + AI 功能支撑
-- ============================================

-- signup 表新增拒绝理由字段（INFORMATION_SCHEMA 兼容检测）
SET @col_rr = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'volunteer_db' AND TABLE_NAME = 'signup' AND COLUMN_NAME = 'review_reason');
SET @sql_rr = IF(@col_rr = 0,
  'ALTER TABLE signup ADD COLUMN review_reason VARCHAR(255) COMMENT ''拒绝理由''',
  'SELECT ''Column review_reason already exists''');
PREPARE stmt_rr FROM @sql_rr;
EXECUTE stmt_rr;
DEALLOCATE PREPARE stmt_rr;

-- -------------------------------------------
-- Phase 2 UPM 增量迁移：user 表新增字段
-- -------------------------------------------

-- organization
SET @col_org = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'volunteer_db' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'organization');
SET @sql_org = IF(@col_org = 0,
  'ALTER TABLE `user` ADD COLUMN organization VARCHAR(64) COMMENT ''所属机构（组织者填写）'' AFTER role',
  'SELECT ''Column organization already exists''');
PREPARE stmt_org FROM @sql_org; EXECUTE stmt_org; DEALLOCATE PREPARE stmt_org;

-- employee_id
SET @col_ei = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'volunteer_db' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'employee_id');
SET @sql_ei = IF(@col_ei = 0,
  'ALTER TABLE `user` ADD COLUMN employee_id VARCHAR(32) COMMENT ''工号（组织者填写）'' AFTER organization',
  'SELECT ''Column employee_id already exists''');
PREPARE stmt_ei FROM @sql_ei; EXECUTE stmt_ei; DEALLOCATE PREPARE stmt_ei;

-- ============================================
-- Phase 4 增量迁移：签到地理围栏（P2-AM）
-- ============================================

-- activity 表新增签到围栏字段（INFORMATION_SCHEMA 兼容检测）
SET @col_cr = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'volunteer_db' AND TABLE_NAME = 'activity' AND COLUMN_NAME = 'checkin_region');
SET @sql_cr = IF(@col_cr = 0,
  'ALTER TABLE activity ADD COLUMN checkin_region TEXT COMMENT ''签到围栏 GeoJSON Polygon（JSON字符串）''',
  'SELECT ''Column checkin_region already exists''');
PREPARE stmt_cr FROM @sql_cr;
EXECUTE stmt_cr;
DEALLOCATE PREPARE stmt_cr;
