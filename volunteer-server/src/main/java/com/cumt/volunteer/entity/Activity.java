package com.cumt.volunteer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 志愿活动表
 */
@Data
@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动标题 */
    private String title;

    /** 活动描述 */
    private String description;

    /** 活动地点名称（如 博学楼101） */
    private String locationName;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 报名开始时间 */
    private LocalDateTime signupStart;

    /** 报名截止时间 */
    private LocalDateTime signupEnd;

    /** 报名上限 */
    private Integer maxParticipants;

    /** 预设志愿时长（小时） */
    private BigDecimal volunteerHours;

    /** 面向年级，逗号分隔（如 "2023,2024"），ALL=不限 */
    private String targetGrade;

    /** 面向院系，逗号分隔（如 "计算机学院,矿业学院"），ALL=不限 */
    private String targetCollege;

    /** 归属组织名称（冗余存储，创建时从组织者organization填充） */
    private String organizationName;

    /** 策划案 JSON（15章节结构化数据） */
    private String proposal;

    /** 已报名人数 */
    private Integer signedCount;

    /** 封面图片路径 */
    private String coverImage;

    /** 状态：draft / published / ongoing / ended / cancelled */
    private String status;

    /** 创建者ID */
    private Long creatorId;

    /** 组织者ID（活动归属的组织者用户ID） */
    private Long organizerId;

    /** 活动分类：environmental/support/education/community/campus/other */
    private String category;

    /** 活动标签，逗号分隔（如 "户外,体力,长期"） */
    private String tags;

    /** 签到围栏 GeoJSON Polygon（JSON字符串，null则使用500m圆形兜底） */
    private String checkinRegion;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
