package com.cumt.volunteer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 站内信（消息通知）
 */
@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 类型：system / signup / signin */
    private String type;

    /** 是否已读 */
    private Boolean isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
