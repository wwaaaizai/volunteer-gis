package com.cumt.volunteer.aca.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cumt.volunteer.entity.Activity;
import java.util.List;

public interface ActivityService extends IService<Activity> {

    /**
     * 创建活动（组织者/管理员）
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
     * 按组织者查询活动（可选状态过滤）
     */
    List<Activity> listByOrganizer(Long userId, String status);

    /**
     * 编辑活动（草稿可全编，已发布仅限描述和封面）
     */
    void updateActivity(Long activityId, Activity update);

    /**
     * 保存签到地理围栏（GeoJSON Polygon）
     */
    void saveGeofence(Long activityId, String geojson);

    /**
     * 附近活动推荐：按距离排序已发布活动
     */
    List<Activity> listNearby(double lng, double lat);
}
