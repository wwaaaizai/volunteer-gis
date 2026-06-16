package com.cumt.volunteer.abm.controller;

import com.cumt.volunteer.abm.service.SignupService;
import com.cumt.volunteer.common.Result;
import com.cumt.volunteer.entity.Signup;
import com.cumt.volunteer.upm.service.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/signups")
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;

    /**
     * 报名活动
     */
    @PostMapping
    public Result<?> signup(@RequestParam Long activityId,
                            @AuthenticationPrincipal CurrentUser user) {
        signupService.signup(activityId, user.getUserId());
        return Result.ok("报名成功");
    }

    /**
     * 取消报名
     */
    @DeleteMapping
    public Result<?> cancelSignup(@RequestParam Long activityId,
                                  @AuthenticationPrincipal CurrentUser user) {
        signupService.cancelSignup(activityId, user.getUserId());
        return Result.ok("已取消报名");
    }

    /**
     * 我的报名
     */
    @GetMapping("/my")
    public Result<List<Signup>> getMySignups(@AuthenticationPrincipal CurrentUser user) {
        return Result.ok(signupService.getMySignups(user.getUserId()));
    }

    /**
     * 查看活动报名名单（管理员）
     */
    @GetMapping("/activity/{activityId}")
    @PreAuthorize("hasRole('admin')")
    public Result<List<Signup>> getActivitySignups(@PathVariable Long activityId) {
        return Result.ok(signupService.getActivitySignups(activityId));
    }
}
