package com.cumt.volunteer.upm.controller;

import com.cumt.volunteer.common.Result;
import com.cumt.volunteer.upm.service.UserService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final UserService userService;

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> body) {
        userService.register(
                body.get("studentId"),
                body.get("password"),
                body.get("name"),
                body.get("phone")
        );
        return Result.ok("注册成功");
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
        return Result.ok(Map.of(
                "userId", user.getUserId().toString(),
                "studentId", user.getStudentId(),
                "role", user.getRole()
        ));
    }
}
