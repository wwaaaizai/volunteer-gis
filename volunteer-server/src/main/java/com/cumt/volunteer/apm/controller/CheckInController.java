package com.cumt.volunteer.apm.controller;

import com.cumt.volunteer.apm.service.CheckInService;
import com.cumt.volunteer.common.Result;
import com.cumt.volunteer.upm.service.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    /**
     * 签到（定位方式）
     */
    @PostMapping("/location")
    public Result<?> checkInByLocation(@RequestParam Long activityId,
                                       @RequestParam BigDecimal lng,
                                       @RequestParam BigDecimal lat,
                                       @AuthenticationPrincipal CurrentUser user) {
        checkInService.checkIn(activityId, user.getUserId(), lng, lat);
        return Result.ok("签到成功");
    }

    /**
     * 签到（扫码方式）
     */
    @PostMapping("/qr")
    public Result<?> checkInByQR(@RequestParam Long activityId,
                                 @RequestParam String code,
                                 @AuthenticationPrincipal CurrentUser user) {
        checkInService.checkInByQR(activityId, user.getUserId(), code);
        return Result.ok("签到成功");
    }

    /**
     * 签退
     */
    @PostMapping("/out")
    public Result<?> checkOut(@RequestParam Long activityId,
                              @RequestParam BigDecimal lng,
                              @RequestParam BigDecimal lat,
                              @AuthenticationPrincipal CurrentUser user) {
        checkInService.checkOut(activityId, user.getUserId(), lng, lat);
        return Result.ok("签退成功");
    }

    /**
     * 生成签到二维码（管理员）
     */
    @GetMapping("/qrcode/{activityId}")
    @PreAuthorize("hasRole('admin')")
    public Result<String> generateQRCode(@PathVariable Long activityId) {
        return Result.ok(checkInService.generateQRCode(activityId));
    }

    /**
     * 审核志愿时长（管理员）
     */
    @PutMapping("/verify-hours/{signupId}")
    @PreAuthorize("hasRole('admin')")
    public Result<?> verifyHours(@PathVariable Long signupId) {
        checkInService.verifyHours(signupId);
        return Result.ok("审核通过");
    }
}
