package com.cumt.volunteer.upm.controller;

import com.cumt.volunteer.common.Result;
import com.cumt.volunteer.entity.OrganizerApply;
import com.cumt.volunteer.upm.service.CurrentUser;
import com.cumt.volunteer.upm.service.UserService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final UserService userService;

    /**
     * 注册（支持组织者申请）
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> body) {
        boolean applyOrganizer = "true".equals(body.get("applyOrganizer"));
        userService.register(
                body.get("studentId"),
                body.get("password"),
                body.get("name"),
                body.get("phone"),
                applyOrganizer,
                body.get("organization"),
                body.get("employeeId")
        );
        return Result.ok(applyOrganizer ? "注册成功，组织者申请已提交，请等待管理员审批" : "注册成功");
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String token = userService.login(body.get("studentId"), body.get("password"));
        return Result.ok(Map.of("token", token));
    }

    /**
     * 获取待审批的组织者申请列表（管理员）
     */
    @GetMapping("/organizer-applies")
    @PreAuthorize("hasRole('admin')")
    public Result<List<OrganizerApply>> listApplies() {
        return Result.ok(userService.listPendingApplies());
    }

    /**
     * 审批组织者申请（管理员）
     */
    @PutMapping("/organizer-applies/{id}/review")
    @PreAuthorize("hasRole('admin')")
    public Result<?> reviewApply(@PathVariable Long id,
                                  @RequestBody Map<String, Boolean> body,
                                  @AuthenticationPrincipal CurrentUser user) {
        boolean approved = body.getOrDefault("approved", false);
        userService.reviewOrganizerApply(id, approved, user.getUserId());
        return Result.ok(approved ? "已通过" : "已拒绝");
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody Map<String, String> body,
                                    @AuthenticationPrincipal CurrentUser user) {
        userService.updateProfile(user.getUserId(),
                body.get("name"), body.get("phone"), body.get("organization"));
        return Result.ok("更新成功");
    }

    /**
     * 获取当前用户信息（供前端路由守卫使用）
     */
    @GetMapping("/me")
    public Result<?> me() {
        // 由 JwtAuthFilter 设置 SecurityContext，这里从 SecurityContext 中获取当前用户
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.cumt.volunteer.upm.service.CurrentUser user)) {
            return Result.error(401, "未登录");
        }
        // 从数据库加载完整用户信息
        var fullUser = userService.getById(user.getUserId());
        // 注意：Map.of 不接受 null 值，需显式处理
        String name = (fullUser != null && fullUser.getName() != null) ? fullUser.getName() : "";
        String phone = (fullUser != null && fullUser.getPhone() != null) ? fullUser.getPhone() : "";
        String org = (fullUser != null && fullUser.getOrganization() != null) ? fullUser.getOrganization() : "";
        return Result.ok(Map.of(
                "userId", user.getUserId().toString(),
                "studentId", user.getStudentId(),
                "role", user.getRole(),
                "name", name,
                "phone", phone,
                "organization", org
        ));
    }
}
