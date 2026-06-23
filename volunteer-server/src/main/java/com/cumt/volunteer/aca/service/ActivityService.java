package com.cumt.volunteer.aca.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cumt.volunteer.entity.Activity;
import java.util.List;

public interface ActivityService extends IService<Activity> {

    /**
     * 创建活动（管理员）
     */
    void createActivity(Activity activity, Long creatorId);

    /**
     * 发布活动
     */
    void publishActivity(Long activityId);

    /**
     * 关键词搜索活动
     */
    List<Activity> searchActivities(String keyword);

    /**
     * 查询全部活动（管理员用，不做状态过滤）
     */
    List<Activity> listAllActivities();
}
