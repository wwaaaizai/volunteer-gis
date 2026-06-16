package com.cumt.volunteer.abm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cumt.volunteer.entity.Signup;
import java.util.List;

public interface SignupService extends IService<Signup> {

    /**
     * 报名活动（学生）
     */
    void signup(Long activityId, Long userId);

    /**
     * 取消报名
     */
    void cancelSignup(Long activityId, Long userId);

    /**
     * 查看我的报名记录
     */
    List<Signup> getMySignups(Long userId);

    /**
     * 查看活动的报名名单（管理员）
     */
    List<Signup> getActivitySignups(Long activityId);
}
