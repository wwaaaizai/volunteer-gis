package com.cumt.volunteer.aca.controller;

import com.cumt.volunteer.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器（封面图等）
 */
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * 上传封面图片（组织者/管理员）
     */
    @PostMapping("/image")
    @PreAuthorize("hasAnyRole('admin','organizer')")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "文件为空");
        }

        // 仅允许图片格式
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
            return Result.error(400, "仅支持 jpg/png/gif/webp 格式");
        }

        // 限制文件大小（5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error(400, "文件大小不能超过 5MB");
        }

        try {
            // 确保上传目录存在
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名
            String ext = originalName.substring(originalName.lastIndexOf('.'));
            String newName = UUID.randomUUID().toString() + ext;
            File dest = uploadPath.resolve(newName).toFile();
            file.transferTo(dest);

            String url = "/uploads/" + newName;
            return Result.ok(Map.of("url", url));
        } catch (IOException e) {
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }
}
