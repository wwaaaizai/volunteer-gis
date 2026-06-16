package com.cumt.volunteer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 报名记录表
 */
@Data
@TableName("signup")
public class Signup {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private Long userId;

    /** 状态：signed / signed_in / signed_out / cancelled */
    private String status;

    /** 签到时间 */
    private LocalDateTime signInTime;

    /** 签到经度 */
    private java.math.BigDecimal signInLng;

    /** 签到纬度 */
    private java.math.BigDecimal signInLat;

    /** 签退时间 */
    private LocalDateTime signOutTime;

    /** 签退经度 */
    private java.math.BigDecimal signOutLng;

    /** 签退纬度 */
    private java.math.BigDecimal signOutLat;

    /** 志愿时长（小时） */
    private java.math.BigDecimal volunteerHours;

    /** 时长是否已审核 */
    private Boolean hourVerified;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
