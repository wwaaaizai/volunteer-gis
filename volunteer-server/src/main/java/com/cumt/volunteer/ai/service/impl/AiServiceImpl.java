package com.cumt.volunteer.ai.service.impl;

import com.cumt.volunteer.ai.service.AiService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI 服务实现（基于 Pollinations.ai，完全免费，无需 API Key）。
 *
 * <p>同学拉取代码即可直接使用，零配置。
 * Pollinations.ai 是开源免费 AI 服务，无需注册。</p>
 */
@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private static final String TEXT_API = "https://text.pollinations.ai/";
    private static final String IMAGE_API = "https://image.pollinations.ai/prompt/";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @PostConstruct
    public void init() {
        log.info("AI provider: Pollinations.ai (免费，无需 API Key)");
    }

    // ─── 公开接口 ─────────────────────────────────────────────

    @Override
    public Map<String, String> generateDescription(String keyword) {
        try {
            String prompt = URLEncoder.encode(
                "你是一个校园志愿活动策划师。请根据以下关键词生成一个中国矿业大学志愿活动的标题和描述。" +
                "以JSON格式返回，格式严格为{\"title\":\"活动标题\",\"description\":\"活动描述\"}。" +
                "关键词：" + keyword,
                StandardCharsets.UTF_8
            );

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(TEXT_API + prompt))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            String text = resp.body();

            // 提取 JSON
            String json = extractJson(text);
            if (!json.equals("{}")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(json, Map.class);
                return Map.of(
                    "title", (String) parsed.getOrDefault("title", keyword + "志愿活动"),
                    "description", (String) parsed.getOrDefault("description", "请参考关键词自行填写")
                );
            }
        } catch (Exception e) {
            log.warn("AI 文本生成失败，降级 Mock: {}", e.getMessage());
        }
        return mockGenerateDescription(keyword);
    }

    @Override
    public Map<String, Object> generateCover(String prompt) {
        try {
            // 三种不同风格 prompt，生成不同候选
            String[] styles = {
                "A warm and inspiring poster for university volunteer activity: " + prompt +
                    ", campus background, bright sunshine, green trees, Chinese university campus, clean modern design, text-free",
                "A professional charity event banner for: " + prompt +
                    ", students helping community, warm tones, teamwork spirit, minimalist style, no text",
                "A creative illustration for campus volunteer program: " + prompt +
                    ", young students volunteering, colorful flat design, positive energy, no text or logo",
            };

            List<String> covers = java.util.Arrays.stream(styles)
                .map(s -> IMAGE_API + URLEncoder.encode(s, StandardCharsets.UTF_8)
                        + "?width=600&height=300&nologo=true")
                .toList();

            return Map.of("covers", covers);

        } catch (Exception e) {
            log.warn("AI 封面生成失败，降级占位图: {}", e.getMessage());
        }
        return mockGenerateCover(prompt);
    }

    // ─── 工具 ─────────────────────────────────────────────────

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return (start >= 0 && end > start) ? text.substring(start, end + 1) : "{}";
    }

    // ─── Mock 降级（网络不通时自动使用）───────────────────────

    private Map<String, String> mockGenerateDescription(String keyword) {
        String title = mockTitle(keyword);
        return Map.of("title", title, "description", mockDesc(keyword, title));
    }

    private String mockTitle(String k) {
        if (k.contains("图书馆")) return "\"书香矿大\"图书馆志愿整理活动";
        if (k.contains("迎新"))   return "\"新起点\"迎新志愿服务行动";
        if (k.contains("雷锋"))   return "\"雷锋月\"校园公益志愿活动";
        return "\"爱心传递\"" + k + "志愿服务活动";
    }

    private String mockDesc(String keyword, String title) {
        String t = title.replaceAll("\"", "");
        return "【活动背景】弘扬志愿服务精神，丰富校园文化生活，特举办本次" + t + "活动。\n\n" +
               "【活动内容】" + keyword + "相关志愿服务工作，包括现场引导、物资整理、秩序维护等。\n\n" +
               "【招募要求】吃苦耐劳，服从安排，有志愿服务经验者优先。";
    }

    private Map<String, Object> mockGenerateCover(String prompt) {
        String[] colors = {"#409EFF", "#67C23A", "#E6A23C"};
        List<String> covers = java.util.Arrays.stream(colors)
                .map(c -> placeholderSvg(c, prompt))
                .toList();
        return Map.of("covers", covers);
    }

    private String placeholderSvg(String color, String prompt) {
        String p = prompt.length() > 12 ? prompt.substring(0, 12) : prompt;
        return "data:image/svg+xml," + URLEncoder.encode(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"400\" height=\"200\">" +
            "<rect fill=\"" + color + "\" width=\"400\" height=\"200\" rx=\"8\" opacity=\"0.15\"/>" +
            "<text fill=\"" + color + "\" font-size=\"28\" font-weight=\"bold\" font-family=\"sans-serif\" x=\"200\" y=\"90\" text-anchor=\"middle\">AI 生成</text>" +
            "<text fill=\"" + color + "\" font-size=\"14\" font-family=\"sans-serif\" x=\"200\" y=\"120\" text-anchor=\"middle\">" + p + "</text>" +
            "</svg>", StandardCharsets.UTF_8
        );
    }
}
