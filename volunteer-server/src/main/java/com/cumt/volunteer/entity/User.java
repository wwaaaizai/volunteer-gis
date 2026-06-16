package com.cumt.volunteer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 用户表
 */
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学号（登录账号） */
    private String studentId;

    /** 密码（bcrypt） */
    private String password;

    /** 姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 角色：student / admin */
    private String role;

    /** 累计志愿时长 */
    private BigDecimal totalHours;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
