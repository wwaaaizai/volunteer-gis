package com.cumt.volunteer.ai.service;

import java.util.Map;

/**
 * AI 服务接口（活动描述生成 + 封面图生成）。
 */
public interface AiService {

    /**
     * 根据关键词生成活动标题和描述
     * @param keyword 用户输入的关键词
     * @return Map { title, description }
     */
    Map<String, String> generateDescription(String keyword);

    /**
     * 根据提示词生成封面图候选
     * @param prompt 描述提示词
     * @return Map { covers: [url1, url2, url3] }
     */
    Map<String, Object> generateCover(String prompt);
}
