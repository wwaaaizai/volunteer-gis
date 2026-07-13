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
import java.util.Map;

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
     * 查看活动报名名单（管理员 / 组织者）
     */
    @GetMapping("/activity/{activityId}")
    @PreAuthorize("hasAnyRole('admin','organizer')")
    public Result<List<Signup>> getActivitySignups(@PathVariable Long activityId) {
        return Result.ok(signupService.getActivitySignups(activityId));
    }

    /**
     * 审核报名（通过/拒绝）
     * 请求体：{ "action": "approve" | "reject", "reason": "..." }
     */
    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('admin','organizer')")
    public Result<?> reviewSignup(@PathVariable Long id,
                               @RequestBody Map<String, String> body) {
        String action = body.get("action");
        String reason = body.getOrDefault("reason", "");
        if (action == null || action.isEmpty()) {
            return Result.error(400, "审核操作不能为空");
        }
        try {
            signupService.reviewSignup(id, action, reason);
            return Result.ok("审核完成");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 志愿足迹：返回当前用户的所有签到记录（含坐标，用于地图展示）
     */
    @GetMapping("/my-footprint")
    public Result<List<Map<String, Object>>> getMyFootprint(
            @AuthenticationPrincipal CurrentUser user) {
        return Result.ok(signupService.getFootprintData(user.getUserId()));
    }
}
