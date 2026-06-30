package com.cumt.volunteer.apm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cumt.volunteer.abm.mapper.SignupMapper;
import com.cumt.volunteer.apm.service.CheckInService;
import com.cumt.volunteer.aca.mapper.ActivityMapper;
import com.cumt.volunteer.entity.Activity;
import com.cumt.volunteer.entity.Signup;
import com.cumt.volunteer.entity.User;
import com.cumt.volunteer.geo.model.GeoPoint;
import com.cumt.volunteer.geo.service.SpatialCalculator;
import com.cumt.volunteer.upm.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private final SignupMapper signupMapper;
    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;
    private final SpatialCalculator spatialCalculator;

    /** 签到允许的最大距离（米），中国矿业大学南湖校区直径约2km */
    private static final double MAX_SIGN_DISTANCE = 500;

    @Override
    public void checkIn(Long activityId, Long userId, BigDecimal lng, BigDecimal lat) {
        Signup signup = getSignupRecord(activityId, userId);
        if (!"signed".equals(signup.getStatus())) {
            throw new RuntimeException("当前状态不允许签到");
        }

        // 验证签到位置
        Activity activity = activityMapper.selectById(activityId);

        // 优先使用地理围栏校验；无围栏则兜底500m圆形校验
        if (activity.getCheckinRegion() != null && !activity.getCheckinRegion().isBlank()) {
            double[][] polygon = spatialCalculator.parsePolygonFromGeoJson(activity.getCheckinRegion());
            if (polygon != null && !spatialCalculator.isPointInPolygon(
                    lng.doubleValue(), lat.doubleValue(), polygon)) {
                throw new RuntimeException("您不在签到区域内，请到达活动指定区域后签到");
            }
        } else {
            double distance = spatialCalculator.distanceMeters(
                    GeoPoint.of(activity.getLongitude(), activity.getLatitude()),
                    GeoPoint.of(lng, lat)
            );
            if (distance > MAX_SIGN_DISTANCE) {
                throw new RuntimeException("距离活动地点太远，请到达活动地点后签到（当前距离: "
                        + String.format("%.0f", distance) + "米）");
            }
        }

        signup.setStatus("signed_in");
        signup.setSignInTime(LocalDateTime.now());
        signup.setSignInLng(lng);
        signup.setSignInLat(lat);
        signupMapper.updateById(signup);
    }

    @Override
    public void checkInByQR(Long activityId, Long userId, String code) {
        Signup signup = getSignupRecord(activityId, userId);
        if (!"signed".equals(signup.getStatus())) {
            throw new RuntimeException("当前状态不允许签到");
        }

        // 验证二维码内容
        String expected = "CHECKIN:" + activityId;
        String decoded = new String(Base64.getDecoder().decode(code));
        if (!expected.equals(decoded)) {
            throw new RuntimeException("签到二维码无效");
        }

        signup.setStatus("signed_in");
        signup.setSignInTime(LocalDateTime.now());
        signupMapper.updateById(signup);
    }

    @Override
    public void checkOut(Long activityId, Long userId, BigDecimal lng, BigDecimal lat) {
        Signup signup = getSignupRecord(activityId, userId);
        if (!"signed_in".equals(signup.getStatus())) {
            throw new RuntimeException("请先完成签到");
        }

        signup.setStatus("signed_out");
        signup.setSignOutTime(LocalDateTime.now());
        signup.setSignOutLng(lng);
        signup.setSignOutLat(lat);

        // 自动计算志愿时长
        Duration duration = Duration.between(signup.getSignInTime(), signup.getSignOutTime());
        double hours = duration.toMinutes() / 60.0;
        signup.setVolunteerHours(BigDecimal.valueOf(hours).setScale(1, RoundingMode.HALF_UP));
        signup.setHourVerified(false);

        signupMapper.updateById(signup);
    }

    @Override
    public String generateQRCode(Long activityId) {
        // MVP: 返回 Base64 编码的签到码，前端用此生成二维码
        return Base64.getEncoder().encodeToString(("CHECKIN:" + activityId).getBytes());
    }

    @Override
    public void verifyHours(Long signupId) {
        Signup signup = signupMapper.selectById(signupId);
        if (signup == null) {
            throw new RuntimeException("签到记录不存在");
        }
        if (Boolean.TRUE.equals(signup.getHourVerified())) {
            throw new RuntimeException("该记录已审核通过，无需重复审核");
        }
        // 标记为已审核
        signup.setHourVerified(true);
        signupMapper.updateById(signup);
        // 累加志愿时长到用户总时长
        User user = userMapper.selectById(signup.getUserId());
        if (user != null && signup.getVolunteerHours() != null) {
            user.setTotalHours(user.getTotalHours().add(signup.getVolunteerHours()));
            userMapper.updateById(user);
        }
    }

    private Signup getSignupRecord(Long activityId, Long userId) {
        Signup signup = signupMapper.selectOne(new LambdaQueryWrapper<Signup>()
                .eq(Signup::getActivityId, activityId)
                .eq(Signup::getUserId, userId));
        if (signup == null) {
            throw new RuntimeException("您未报名该活动");
        }
        return signup;
    }
}
