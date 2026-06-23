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
    role        VARCHAR(16)  NOT NULL DEFAULT 'student' COMMENT '角色：student/admin',
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
