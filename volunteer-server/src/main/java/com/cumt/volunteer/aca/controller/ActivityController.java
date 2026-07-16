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
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /**
     * 获取活动列表
     * <p>管理员传 {@code showAll=true} 可查看全部活动（含草稿/进行中/已结束）。</p>
     */
    @GetMapping
    public Result<List<Activity>> listActivities(@RequestParam(required = false, defaultValue = "false") boolean showAll) {
        if (showAll) {
            return Result.ok(activityService.listAll());
        }
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
     * 创建活动（管理员 / 组织者）
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('admin','organizer')")
    public Result<?> createActivity(@RequestBody Activity activity,
                                    @AuthenticationPrincipal CurrentUser user) {
        activityService.createActivity(activity, user.getUserId());
        return Result.ok("创建成功");
    }

    /**
     * 发布活动（管理员 / 组织者）
     */
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('admin','organizer')")
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

    /**
     * 附近活动推荐：按 GPS 坐标距离排序已发布活动
     */
    @GetMapping("/nearby")
    public Result<List<Activity>> nearbyActivities(@RequestParam double lng,
                                                    @RequestParam double lat) {
        return Result.ok(activityService.listNearby(lng, lat));
    }

    /**
     * 我的活动列表（组织者视角，按状态筛选）
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('admin','organizer')")
    public Result<List<Activity>> listMyActivities(@RequestParam(required = false) String status,
                                                    @AuthenticationPrincipal CurrentUser user) {
        return Result.ok(activityService.listByOrganizer(user.getUserId(), status));
    }

    /**
     * 编辑活动（组织者 / 管理员，仅草稿可全编，已发布仅可改描述/封面）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin','organizer')")
    public Result<?> updateActivity(@PathVariable Long id,
                                    @RequestBody Activity activity) {
        activityService.updateActivity(id, activity);
        return Result.ok("更新成功");
    }

    /**
     * 保存签到地理围栏（组织者/管理员，签到围栏功能）
     */
    @PutMapping("/{id}/geofence")
    @PreAuthorize("hasAnyRole('admin','organizer')")
    public Result<?> saveGeofence(@PathVariable Long id,
                                   @RequestBody Map<String, String> body) {
        String geojson = body.get("geojson");
        activityService.saveGeofence(id, geojson);
        return Result.ok("围栏保存成功");
    }

    /**
     * 删除活动（管理员，逻辑删除）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<?> deleteActivity(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return Result.ok("删除成功");
    }

    /**
     * 获取签到地理围栏（供前端可视化展示）
     */
    @GetMapping("/{id}/geofence")
    public Result<Map<String, String>> getGeofence(@PathVariable Long id) {
        Activity activity = activityService.getById(id);
        String geojson = activity != null ? activity.getCheckinRegion() : null;
        return Result.ok(Map.of("geojson", geojson != null ? geojson : ""));
    }
}
