package com.cumt.volunteer.aca.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cumt.volunteer.aca.mapper.ActivityMapper;
import com.cumt.volunteer.aca.service.ActivityService;
import com.cumt.volunteer.entity.Activity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    @Override
    public void createActivity(Activity activity, Long creatorId) {
        activity.setCreatorId(creatorId);
        // 设置组织者ID（如果未指定，则等于creatorId）
        if (activity.getOrganizerId() == null) {
            activity.setOrganizerId(creatorId);
        }
        activity.setStatus("draft");
        activity.setSignedCount(0);
        save(activity);
    }

    @Override
    public void publishActivity(Long activityId) {
        Activity activity = getById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        activity.setStatus("published");
        updateById(activity);
    }

    @Override
    public List<Activity> searchActivities(String keyword) {
        // MVP: MySQL LIKE 搜索标题
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Activity::getTitle, keyword);
        }
        wrapper.eq(Activity::getStatus, "published");
        wrapper.orderByDesc(Activity::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public List<Activity> listByOrganizer(Long userId, String status) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getOrganizerId, userId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(Activity::getStatus, status);
        }
        wrapper.orderByDesc(Activity::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public void updateActivity(Long activityId, Activity update) {
        Activity existing = getById(activityId);
        if (existing == null) {
            throw new RuntimeException("活动不存在");
        }
        // 草稿状态可编辑全部字段
        if ("draft".equals(existing.getStatus())) {
            update.setId(activityId);
            update.setCreatorId(existing.getCreatorId());
            update.setOrganizerId(existing.getOrganizerId());
            update.setSignedCount(existing.getSignedCount());
            update.setStatus("draft");
            updateById(update);
            return;
        }
        // 已发布只能改描述和封面
        if ("published".equals(existing.getStatus()) || "ongoing".equals(existing.getStatus())) {
            existing.setDescription(update.getDescription());
            existing.setCoverImage(update.getCoverImage());
            updateById(existing);
            return;
        }
        throw new RuntimeException("该状态的活动不可编辑");
    }
}
