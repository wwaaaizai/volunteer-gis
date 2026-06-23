package com.cumt.volunteer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 组织者申请表
 */
@Data
@TableName("organizer_apply")
public class OrganizerApply {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请人用户ID */
    private Long userId;

    /** 所属机构 */
    private String organization;

    /** 申请理由 */
    private String reason;

    /** 状态：pending / approved / rejected */
    private String status;

    /** 审核管理员ID */
    private Long reviewedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
