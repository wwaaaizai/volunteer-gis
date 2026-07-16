package com.cumt.volunteer.upm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cumt.volunteer.entity.OrganizerApply;
import com.cumt.volunteer.entity.User;

import java.util.List;

public interface UserService extends IService<User> {

    /**
     * 注册
     */
    void register(String studentId, String password, String name, String phone,
                  String grade, String college,
                  boolean applyOrganizer, String organization, String employeeId);

    /**
     * 登录，返回 JWT Token
     */
    String login(String studentId, String password);

    /**
     * 根据学号查找用户
     */
    User findByStudentId(String studentId);

    /**
     * 获取待审批的组织者申请列表
     */
    List<OrganizerApply> listPendingApplies();

    /**
     * 审批组织者申请（通过/拒绝）
     */
    void reviewOrganizerApply(Long applyId, boolean approved, Long reviewerId);

    /**
     * 更新用户个人信息
     */
    void updateProfile(Long userId, String name, String phone, String organization);

    /**
     * 列出所有用户（管理员功能）
     */
    List<User> listAllUsers();

    /**
     * 管理员创建用户
     */
    User createUser(String studentId, String password, String name, String phone,
                    String role, String organization);

    /**
     * 管理员更新用户信息
     */
    void updateUser(Long userId, String name, String phone, String role, String organization);

    /**
     * 管理员删除用户（逻辑删除）
     */
    void deleteUser(Long userId);

    /**
     * 管理员重置用户密码
     */
    void resetPassword(Long userId, String newPassword);
}
