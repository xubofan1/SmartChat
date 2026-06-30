package com.yizhaoqi.smartpai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 全局 AI 相关配置，包含 Prompt 模板和生成参数。
 */
@Component
@ConfigurationProperties(prefix = "ai")
@Data
public class AiProperties {

    private Prompt prompt = new Prompt();
    private Generation generation = new Generation();
    private Summary summary = new Summary();

    @Data
    public static class Prompt {
        /** 规则文案 */
        private String rules;
        /** 引用开始分隔符 */
        private String refStart;
        /** 引用结束分隔符 */
        private String refEnd;
        /** 无检索结果时的占位文案 */
        private String noResultText;
    }

    @Data
    public static class Generation {
        /** 采样温度 */
        private Double temperature = 0.3;
        /** 最大输出 tokens */
        private Integer maxTokens = 2000;
        /** nucleus top-p */
        private Double topP = 0.9;
    }

    @Data
    public static class Summary {
        /** 是否启用历史摘要 */
        private boolean enabled = true;
        /** 超过该消息条数时触发摘要 */
        private int triggerMessageCount = 12;
        /** 发送给模型时保留最近原文消息条数 */
        private int recentMessageCount = 6;
        /** 摘要最大字符数（超过则截断） */
        private int maxSummaryChars = 1200;
        /** 摘要提示词模板 */
        private String promptTemplate = """
                你将接收一段多轮对话历史，请输出用于后续问答的精炼摘要。
                要求：
                1) 保留用户目标、关键事实、约束条件、已确认结论；
                2) 删除寒暄、重复、无关内容；
                3) 使用简体中文，条目化输出，控制在 {maxChars} 字以内。

                已有摘要：
                {existingSummary}

                新增历史：
                {history}
                """;
    }
} 