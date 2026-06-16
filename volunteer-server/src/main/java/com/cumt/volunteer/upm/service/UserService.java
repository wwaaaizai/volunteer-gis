package com.cumt.volunteer.upm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cumt.volunteer.entity.User;

public interface UserService extends IService<User> {

    /**
     * 注册
     */
    void register(String studentId, String password, String name, String phone);

    /**
     * 登录，返回 JWT Token
     */
    String login(String studentId, String password);

    /**
     * 根据学号查找用户
     */
    User findByStudentId(String studentId);
}
