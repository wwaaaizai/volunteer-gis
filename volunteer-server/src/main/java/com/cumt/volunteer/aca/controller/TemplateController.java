package com.cumt.volunteer.aca.controller;

import com.cumt.volunteer.common.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 活动模板控制器（预设年常活动 + 用户自定义模板）。
 */
@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    /** 年常活动预设模板 */
    private static List<Map<String, Object>> buildPresets() {
        List<Map<String, Object>> list = new java.util.ArrayList<>();

        Map<String, Object> t1 = new LinkedHashMap<>();
        t1.put("id", -1); t1.put("name", "📚 图书馆共建活动"); t1.put("category", "campus");
        t1.put("tags", "图书馆,共建,室内"); t1.put("title", "图书馆共建志愿服务");
        t1.put("description", "参与图书馆日常维护与共建，包括图书整理上架、阅览室环境维护、读者引导咨询、电子资源推广等。");
        t1.put("locationName", "图书馆"); t1.put("longitude", 117.133135); t1.put("latitude", 34.217480);
        t1.put("maxParticipants", 30); t1.put("preset", true);
        list.add(t1);

        Map<String, Object> t2 = new LinkedHashMap<>();
        t2.put("id", -2); t2.put("name", "🏃 校运会志愿活动"); t2.put("category", "campus");
        t2.put("tags", "运动会,户外,体力"); t2.put("title", "校运会志愿服务");
        t2.put("description", "协助校运会赛事组织，包括场地布置、器材搬运、赛道维护、秩序引导等。");
        t2.put("locationName", "第一运动场"); t2.put("longitude", 117.134128); t2.put("latitude", 34.221646);
        t2.put("maxParticipants", 60); t2.put("preset", true);
        list.add(t2);

        return list;
    }
    private static final List<Map<String, Object>> PRESETS = buildPresets();

    @GetMapping
    @PreAuthorize("hasAnyRole('admin','organizer','student')")
    public Result<List<Map<String, Object>>> listTemplates() {
        // 当前返回预设模板，未来可扩展数据库存储
        return Result.ok(new ArrayList<>(PRESETS));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('admin','organizer')")
    public Result<Map<String, Object>> saveTemplate(@RequestBody Map<String, Object> body) {
        Map<String, Object> template = new LinkedHashMap<>(body);
        template.put("id", System.currentTimeMillis());
        template.put("preset", false);
        template.put("createdAt", new Date().toString());
        return Result.ok(template);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin','organizer')")
    public Result<String> deleteTemplate(@PathVariable Long id) {
        return Result.ok("删除成功");
    }
}
