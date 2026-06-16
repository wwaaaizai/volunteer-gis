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
}
