package com.cumt.volunteer.aca.controller;

import com.cumt.volunteer.aca.service.ActivityService;
import com.cumt.volunteer.common.Result;
import com.cumt.volunteer.entity.Activity;
import com.cumt.volunteer.upm.service.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /**
     * 获取活动列表
     */
    @GetMapping
    public Result<List<Activity>> listActivities() {
        return Result.ok(activityService.searchActivities(null));
    }

    /**
     * 获取活动详情
     */
    @GetMapping("/{id}")
    public Result<Activity> getActivity(@PathVariable Long id) {
        return Result.ok(activityService.getById(id));
    }

    /**
     * 创建活动（管理员）
     */
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public Result<?> createActivity(@RequestBody Activity activity,
                                    @AuthenticationPrincipal CurrentUser user) {
        activityService.createActivity(activity, user.getUserId());
        return Result.ok("创建成功");
    }

    /**
     * 发布活动（管理员）
     */
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('admin')")
    public Result<?> publishActivity(@PathVariable Long id) {
        activityService.publishActivity(id);
        return Result.ok("发布成功");
    }

    /**
     * 搜索活动
     */
    @GetMapping("/search")
    public Result<List<Activity>> searchActivities(@RequestParam String keyword) {
        return Result.ok(activityService.searchActivities(keyword));
    }
}
