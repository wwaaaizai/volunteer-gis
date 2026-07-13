package com.cumt.volunteer.abm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cumt.volunteer.abm.mapper.SignupMapper;
import com.cumt.volunteer.abm.service.SignupService;
import com.cumt.volunteer.aca.mapper.ActivityMapper;
import com.cumt.volunteer.entity.Activity;
import com.cumt.volunteer.entity.Signup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SignupServiceImpl extends ServiceImpl<SignupMapper, Signup> implements SignupService {

    private final ActivityMapper activityMapper;

    @Override
    @Transactional
    public void signup(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        if (!"published".equals(activity.getStatus())) {
            throw new RuntimeException("活动未开放报名");
        }
        if (activity.getSignedCount() >= activity.getMaxParticipants()) {
            throw new RuntimeException("报名已满");
        }
        // 检查是否已报名
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<Signup>()
                .eq(Signup::getActivityId, activityId)
                .eq(Signup::getUserId, userId));
        if (count > 0) {
            throw new RuntimeException("已报名该活动");
        }
        // 创建报名记录
        Signup signup = new Signup();
        signup.setActivityId(activityId);
        signup.setUserId(userId);
        signup.setStatus("signed");
        signup.setCreatedAt(LocalDateTime.now());
        save(signup);

        // 更新已报名人数
        activity.setSignedCount(activity.getSignedCount() + 1);
        activityMapper.updateById(activity);
    }

    @Override
    @Transactional
    public void cancelSignup(Long activityId, Long userId) {
        Signup signup = baseMapper.selectOne(new LambdaQueryWrapper<Signup>()
                .eq(Signup::getActivityId, activityId)
                .eq(Signup::getUserId, userId)
                .eq(Signup::getStatus, "signed"));
        if (signup == null) {
            throw new RuntimeException("未找到报名记录");
        }
        signup.setStatus("cancelled");
        updateById(signup);

        Activity activity = activityMapper.selectById(activityId);
        if (activity != null && activity.getSignedCount() > 0) {
            activity.setSignedCount(activity.getSignedCount() - 1);
            activityMapper.updateById(activity);
        }
    }

    @Override
    public List<Signup> getMySignups(Long userId) {
        return list(new LambdaQueryWrapper<Signup>()
                .eq(Signup::getUserId, userId)
                .orderByDesc(Signup::getCreatedAt));
    }

    @Override
    public List<Signup> getActivitySignups(Long activityId) {
        return list(new LambdaQueryWrapper<Signup>()
                .eq(Signup::getActivityId, activityId));
    }

    @Override
    @Transactional
    public void reviewSignup(Long signupId, String action, String reason) {
        Signup signup = getById(signupId);
        if (signup == null) {
            throw new RuntimeException("报名记录不存在");
        }
        if (!"signed".equals(signup.getStatus())) {
            throw new RuntimeException("该报名记录状态不允许审核");
        }
        if ("approve".equals(action)) {
            signup.setStatus("approved");
        } else if ("reject".equals(action)) {
            signup.setStatus("rejected");
            signup.setReviewReason(reason);
        } else {
            throw new RuntimeException("无效的审核操作: " + action);
        }
        updateById(signup);
    }

    @Override
    public List<Map<String, Object>> getFootprintData(Long userId) {
        // 查询有签到坐标的记录，按签到时间排序
        List<Signup> signups = list(new LambdaQueryWrapper<Signup>()
                .eq(Signup::getUserId, userId)
                .isNotNull(Signup::getSignInLng)
                .isNotNull(Signup::getSignInLat)
                .orderByAsc(Signup::getSignInTime));

        // 收集activityIds批量查询活动名
        Set<Long> activityIds = signups.stream()
                .map(Signup::getActivityId).collect(Collectors.toSet());
        Map<Long, String> titleMap = new HashMap<>();
        if (!activityIds.isEmpty()) {
            activityMapper.selectBatchIds(activityIds)
                .forEach(a -> titleMap.put(a.getId(), a.getTitle()));
        }

        return signups.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("activityId", s.getActivityId());
            m.put("activityTitle", titleMap.getOrDefault(s.getActivityId(), "未知活动"));
            m.put("lng", s.getSignInLng());
            m.put("lat", s.getSignInLat());
            m.put("signInTime", s.getSignInTime() != null ? s.getSignInTime().toString() : null);
            m.put("volunteerHours", s.getVolunteerHours());
            return m;
        }).collect(Collectors.toList());
    }
}
