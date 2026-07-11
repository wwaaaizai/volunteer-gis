package com.cumt.volunteer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 组织者操作日志表（P2-UPM-07）
 */
@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作者用户ID */
    private Long userId;

    /** 操作类型：create_activity / edit_activity / publish_activity */
    private String operationType;

    /** 操作描述 */
    private String description;

    /** 关联活动ID */
    private Long activityId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
