package com.cumt.volunteer.apm.service;

import java.math.BigDecimal;

public interface CheckInService {

    /**
     * 定位签到
     */
    void checkIn(Long activityId, Long userId, BigDecimal lng, BigDecimal lat);

    /**
     * 二维码签到
     */
    void checkInByQR(Long activityId, Long userId, String code);

    /**
     * 定位签退，自动计算时长
     */
    void checkOut(Long activityId, Long userId, BigDecimal lng, BigDecimal lat);

    /**
     * 生成活动签到二维码（Base64）
     */
    String generateQRCode(Long activityId);

    /**
     * 审核志愿时长
     */
    void verifyHours(Long signupId);
}
