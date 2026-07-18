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
    private static final List<Map<String, Object>> PRESETS = List.of(
        Map.of("id", -1, "name", "🎓 迎新志愿活动", "category", "campus",
            "tags", "迎新,引导,大型", "title", "迎新志愿服务",
            "description", "协助新生报到工作，包括校门口引导、行李搬运、宿舍指引、报到流程咨询等。",
            "locationName", "矿大南湖校区各校门及报到点", "maxParticipants", 80, "preset", true),
        Map.of("id", -2, "name", "🧹 学雷锋志愿活动", "category", "community",
            "tags", "学雷锋,公益,社区", "title", "\"雷锋月\"校园志愿服务",
            "description", "弘扬雷锋精神，开展校园清洁、社区服务、敬老院慰问等系列志愿活动。",
            "locationName", "矿大南湖校区及周边社区", "maxParticipants", 100, "preset", true),
        Map.of("id", -3, "name", "🏃 校运会志愿活动", "category", "campus",
            "tags", "运动会,户外,体力", "title", "校运会志愿服务",
            "description", "协助校运会赛事组织，包括场地布置、器材搬运、赛道维护、秩序引导等。",
            "locationName", "南湖体育场", "maxParticipants", 60, "preset", true),
        Map.of("id", -4, "name", "🎉 毕业季志愿活动", "category", "campus",
            "tags", "毕业季,服务,校园", "title", "毕业季志愿服务",
            "description", "协助毕业典礼筹备与执行，包括学位服发放回收、场地布置、拍照引导、校友接待等。",
            "locationName", "大学生活动中心及图书馆广场", "maxParticipants", 50, "preset", true),
        Map.of("id", -5, "name", "📚 图书馆共建活动", "category", "campus",
            "tags", "图书馆,共建,室内", "title", "图书馆共建志愿服务",
            "description", "参与图书馆日常维护与共建，包括图书整理上架、阅览室环境维护、读者引导咨询、电子资源推广等。",
            "locationName", "矿大图书馆", "maxParticipants", 30, "preset", true)
    );

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
