package com.cumt.volunteer.ai.controller;

import com.cumt.volunteer.ai.service.AiService;
import com.cumt.volunteer.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 功能控制器（描述生成 + 封面生成）。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * AI 生成活动描述。
     * 请求体：{ "keyword": "图书馆 整理 周末" }
     * 响应：{ "title": "...", "description": "..." }
     */
    @PostMapping("/generate-description")
    @PreAuthorize("hasAnyRole('admin','organizer')")
    public Result<Map<String, String>> generateDescription(@RequestBody Map<String, String> body) {
        String keyword = body.getOrDefault("keyword", "").trim();
        if (keyword.isEmpty()) {
            return Result.error(400, "关键词不能为空");
        }
        Map<String, String> result = aiService.generateDescription(keyword);
        return Result.ok(result);
    }

    /**
     * AI 生成封面图。
     * 请求体：{ "prompt": "图书馆志愿活动" }
     * 响应：{ "covers": ["url1", "url2", "url3"] }
     */
    @PostMapping("/generate-cover")
    @PreAuthorize("hasAnyRole('admin','organizer')")
    public Result<Map<String, Object>> generateCover(@RequestBody Map<String, String> body) {
        String prompt = body.getOrDefault("prompt", "").trim();
        if (prompt.isEmpty()) {
            return Result.error(400, "提示词不能为空");
        }
        Map<String, Object> result = aiService.generateCover(prompt);
        return Result.ok(result);
    }
}
