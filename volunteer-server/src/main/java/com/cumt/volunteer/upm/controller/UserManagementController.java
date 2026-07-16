package com.cumt.volunteer.upm.controller;

import com.cumt.volunteer.common.Result;
import com.cumt.volunteer.entity.User;
import com.cumt.volunteer.upm.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员用户管理接口（账号 CRUD）
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserService userService;

    /**
     * 列出所有用户
     */
    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public Result<List<User>> listUsers() {
        return Result.ok(userService.listAllUsers());
    }

    /**
     * 创建用户
     */
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public Result<Map<String, Object>> createUser(@RequestBody Map<String, String> body) {
        User user = userService.createUser(
                body.get("studentId"),
                body.get("password"),
                body.get("name"),
                body.get("phone"),
                body.get("role"),
                body.get("organization")
        );
        return Result.ok(Map.of("id", user.getId(), "message", "创建成功"));
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<?> updateUser(@PathVariable Long id,
                                 @RequestBody Map<String, String> body) {
        userService.updateUser(id,
                body.get("name"),
                body.get("phone"),
                body.get("role"),
                body.get("organization"));
        return Result.ok("更新成功");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok("删除成功");
    }

    /**
     * 重置用户密码
     */
    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('admin')")
    public Result<?> resetPassword(@PathVariable Long id,
                                    @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return Result.ok("密码重置成功");
    }
}
