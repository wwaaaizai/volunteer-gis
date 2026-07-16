package com.cumt.volunteer.abm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cumt.volunteer.entity.Signup;
import java.util.List;
import java.util.Map;

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
     * 查看活动的报名名单（管理员/组织者）
     */
    List<Signup> getActivitySignups(Long activityId);

    /**
     * 审核报名（通过/拒绝）
     */
    void reviewSignup(Long signupId, String action, String reason);

    /**
     * 志愿足迹：返回用户的签到坐标+时间线
     */
    List<Map<String, Object>> getFootprintData(Long userId);
}
