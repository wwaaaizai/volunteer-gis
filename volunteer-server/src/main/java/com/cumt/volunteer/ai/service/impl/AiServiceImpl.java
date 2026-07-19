package com.cumt.volunteer.ai.service.impl;

import com.cumt.volunteer.ai.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * AI 服务实现（智谱 GLM API + 本地降级）。
 */
@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    private static final String ZHIPU_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:glm-4-flash}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Random random = new Random();
    private boolean apiReady = false;

    @PostConstruct
    public void init() {
        apiReady = apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("${");
        log.info("AI provider: {}", apiReady ? "智谱 GLM (" + model + ")" : "本地降级（未配置 API Key）");
    }

    // ─── 文本生成 ─────────────────────────────────────────────

    @Override
    public Map<String, String> generateDescription(String keyword) {
        if (apiReady) {
            try {
                return callZhipu(keyword);
            } catch (Exception e) {
                log.warn("智谱 API 失败，降级本地: {}", e.getMessage());
            }
        }
        return localGenerate(keyword);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> callZhipu(String keyword) throws Exception {
        String systemPrompt = "你是校园志愿活动策划师。根据关键词生成活动标题和描述。严格返回JSON：{\"title\":\"标题\",\"desc\":\"描述\"}。标题20字以内，描述150-300字。";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.9);
        body.put("messages", List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", "关键词：" + keyword)
        ));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ZHIPU_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> result = mapper.readValue(resp.body(), Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
        String content = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");

        // 提取 JSON
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            Map<String, Object> parsed = mapper.readValue(content.substring(start, end + 1), Map.class);
            String title = (String) parsed.getOrDefault("title", keyword + "志愿活动");
            String desc = (String) parsed.getOrDefault("desc", parsed.get("description"));
            if (title != null && !title.isBlank() && desc != null && !desc.isBlank()) {
                return Map.of("title", title, "description", desc);
            }
        }
        // JSON 解析失败，用原始文本
        return Map.of("title", extractTitle(content, keyword), "description", content);
    }

    private String extractTitle(String text, String keyword) {
        for (String line : text.split("[\\n。]")) {
            line = line.trim();
            if (line.length() > 4 && line.length() < 50) return line;
        }
        return keyword + "志愿活动";
    }

    // ─── 封面生成（智谱 CogView + 本地降级）───────────────────

    private static final String COGVIEW_URL = "https://open.bigmodel.cn/api/paas/v4/images/generations";

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> generateCover(String prompt) {
        List<String> covers = new ArrayList<>();
        String[] styles = {
            "A warm campus volunteer activity poster for " + prompt + ", sunshine, green trees, university buildings, clean design, text-free",
            "A professional charity banner for " + prompt + ", teamwork, helping hands, warm tones, modern, no text",
            "An energetic illustration for " + prompt + ", young volunteers, flat design, bright colors, no text or logo",
        };

        if (apiReady) {
            for (int i = 0; i < 3; i++) {
                try {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("model", "cogview-3-flash");
                    body.put("prompt", styles[i]);

                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(COGVIEW_URL))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer " + apiKey)
                            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                            .timeout(Duration.ofSeconds(30))
                            .build();

                    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                    Map<String, Object> result = mapper.readValue(resp.body(), Map.class);
                    List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                    if (data != null && !data.isEmpty()) {
                        String imgUrl = (String) data.get(0).get("url");
                        // 下载图片转 Base64，永不过期
                        try {
                            HttpRequest imgReq = HttpRequest.newBuilder().uri(URI.create(imgUrl)).GET().timeout(Duration.ofSeconds(15)).build();
                            HttpResponse<byte[]> imgResp = httpClient.send(imgReq, HttpResponse.BodyHandlers.ofByteArray());
                            String b64 = Base64.getEncoder().encodeToString(imgResp.body());
                            covers.add("data:image/png;base64," + b64);
                        } catch (Exception e2) {
                            covers.add(fallbackSvg(i, prompt));
                        }
                        continue;
                    }
                } catch (Exception e) {
                    log.warn("CogView 第{}张失败: {}", i + 1, e.getMessage());
                }
                // 单张失败 → SVG 兜底
                covers.add(fallbackSvg(i, prompt));
            }
        } else {
            for (int i = 0; i < 3; i++) covers.add(fallbackSvg(i, prompt));
        }
        return Map.of("covers", covers);
    }

    private String fallbackSvg(int i, String prompt) {
        String[] colors = {"#409EFF", "#67C23A", "#E6A23C"};
        String[] icons = {"📋", "🤝", "🌟"};
        String sp = prompt.length() > 8 ? prompt.substring(0, 8) : prompt;
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"600\" height=\"300\" viewBox=\"0 0 600 300\">"
            + "<defs><linearGradient id=\"bg" + i + "\" x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"100%\">"
            + "<stop offset=\"0%\" style=\"stop-color:" + colors[i] + ";stop-opacity:0.15\"/>"
            + "<stop offset=\"100%\" style=\"stop-color:" + colors[i] + ";stop-opacity:0.05\"/>"
            + "</linearGradient></defs>"
            + "<rect width=\"600\" height=\"300\" fill=\"url(#bg" + i + ")\" rx=\"12\"/>"
            + "<text x=\"300\" y=\"130\" text-anchor=\"middle\" font-size=\"48\">" + icons[i] + "</text>"
            + "<text x=\"300\" y=\"180\" text-anchor=\"middle\" font-size=\"22\" fill=\"" + colors[i] + "\" font-weight=\"bold\" font-family=\"sans-serif\">" + sp + "</text>"
            + "<text x=\"300\" y=\"210\" text-anchor=\"middle\" font-size=\"13\" fill=\"#909399\" font-family=\"sans-serif\">AI 生成失败，占位图</text>"
            + "<rect x=\"230\" y=\"240\" width=\"140\" height=\"32\" rx=\"16\" fill=\"" + colors[i] + "\" opacity=\"0.1\"/>"
            + "<text x=\"300\" y=\"261\" text-anchor=\"middle\" font-size=\"12\" fill=\"" + colors[i] + "\" font-family=\"sans-serif\">方案 " + (i+1) + "</text>"
            + "</svg>";
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    // ─── 本地降级 ────────────────────────────────────────────

    private Map<String, String> localGenerate(String keyword) {
        String[] tpls = {
            "\"" + keyword + "先锋\"志愿服务活动",
            keyword + "志愿行动",
            "矿大" + keyword + "公益计划",
            "\"青春志愿行\"——" + keyword,
            "\"爱在矿大\"" + keyword + "活动",
            keyword + "校园志愿服务",
            "\"奉献青春\"" + keyword + "行动",
            keyword + "志愿者招募",
            "\"暖心校园\"" + keyword,
            "矿大" + keyword + "公益行动",
            "\"携手同行\"" + keyword + "志愿",
            keyword + "爱心活动",
        };
        String title = tpls[random.nextInt(tpls.length)];

        String[] descs = {
            "【活动背景】弘扬志愿服务精神，丰富校园文化生活，现面向全校招募" + keyword + "志愿者。\n\n【工作内容】1.协助" + keyword + "相关工作；2.现场秩序维护与引导；3.物资整理与发放。\n\n【招募要求】吃苦耐劳，有责任心，服从组织安排。\n\n【注意事项】提前10分钟到达集合点，穿着志愿者马甲。",
            "【活动简介】本次" + keyword + "活动旨在服务校园、锻炼自我。\n\n【具体任务】• 前期准备：物资采购与场地布置；• 活动期间：签到引导与秩序管理；• 活动结束：物资回收与场地清理。\n\n【招募对象】全体在校学生，热心公益，有团队合作精神。",
            "【活动目的】通过" + keyword + "志愿服务，培养大学生的社会责任感。\n\n【任务安排】一、前期准备：物资采购与场地布置。二、活动期间：签到引导与秩序维护。三、活动结束：场地清理与总结。\n\n【报名条件】时间充裕，责任心强。",
            "【活动概述】" + keyword + "志愿活动期待你的加入！\n\n【你将参与】\n1. " + keyword + "现场协调与管理\n2. 参与者引导与咨询服务\n3. 活动物资的整理与分发\n\n【我们希望你】热心公益、认真负责、乐于沟通。\n\n【你将获得】志愿时长认证 + 难忘的团队经历。",
            "【活动主题】以" + keyword + "为载体，践行志愿精神。\n\n【工作安排】\n• 第一阶段：活动筹备与宣传\n• 第二阶段：现场执行与服务\n• 第三阶段：总结反馈与表彰\n\n【招募说明】不限年级专业，只要你有热情和责任心。",
        };
        return Map.of("title", title, "description", descs[random.nextInt(descs.length)]);
    }
}
