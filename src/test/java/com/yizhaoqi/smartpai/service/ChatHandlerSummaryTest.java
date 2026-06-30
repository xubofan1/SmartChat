package com.yizhaoqi.smartpai.service;

import com.yizhaoqi.smartpai.client.DeepSeekClient;
import com.yizhaoqi.smartpai.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatHandlerSummaryTest {

    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private DeepSeekClient deepSeekClient;
    private ChatHandler chatHandler;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        deepSeekClient = mock(DeepSeekClient.class);
        HybridSearchService searchService = mock(HybridSearchService.class);

        AiProperties aiProperties = new AiProperties();
        aiProperties.getSummary().setEnabled(true);
        aiProperties.getSummary().setTriggerMessageCount(4);
        aiProperties.getSummary().setRecentMessageCount(2);
        aiProperties.getSummary().setMaxSummaryChars(100);

        chatHandler = new ChatHandler(redisTemplate, searchService, deepSeekClient, aiProperties);
    }

    @Test
    void buildSummaryContext_shouldSummarizeOldHistoryAndKeepRecentMessages() throws Exception {
        when(valueOperations.get("conversation_summary:conv-1")).thenReturn("");
        when(deepSeekClient.summarizeConversation(anyString())).thenReturn("这是新摘要");

        List<Map<String, String>> history = List.of(
                Map.of("role", "user", "content", "问题1"),
                Map.of("role", "assistant", "content", "回答1"),
                Map.of("role", "user", "content", "问题2"),
                Map.of("role", "assistant", "content", "回答2"),
                Map.of("role", "user", "content", "问题3"),
                Map.of("role", "assistant", "content", "回答3")
        );

        Object context = invokeBuildSummaryContext("conv-1", history);
        String summary = (String) context.getClass().getDeclaredMethod("summary").invoke(context);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> modelHistory =
                (List<Map<String, String>>) context.getClass().getDeclaredMethod("modelHistory").invoke(context);

        assertEquals("这是新摘要", summary);
        assertEquals(2, modelHistory.size());
        assertEquals("问题3", modelHistory.get(0).get("content"));
        assertEquals("回答3", modelHistory.get(1).get("content"));
        verify(valueOperations).set(eq("conversation_summary:conv-1"), eq("这是新摘要"), eq(java.time.Duration.ofDays(7)));
    }

    @Test
    void buildSummaryContext_shouldDropInvalidHistoryEntries() throws Exception {
        when(valueOperations.get("conversation_summary:conv-2")).thenReturn("已有摘要");

        List<Map<String, String>> history = List.of(
                Map.of("role", "user", "content", "问题A"),
                Map.of("role", "assistant", "content", "回答A"),
                Map.of("role", "user", "content", "问题B"),
                Map.of("role", "assistant", "content", "回答B"),
                Map.of("role", "assistant", "content", ""),
                Map.of("content", "缺失角色")
        );

        Object context = invokeBuildSummaryContext("conv-2", history);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> modelHistory =
                (List<Map<String, String>>) context.getClass().getDeclaredMethod("modelHistory").invoke(context);

        assertTrue(modelHistory.stream().allMatch(item ->
                item.containsKey("role") && item.containsKey("content") && !item.get("content").isBlank()));
    }

    @Test
    void buildSummaryContext_shouldNotSummarizeWhenBelowTrigger() throws Exception {
        when(valueOperations.get("conversation_summary:conv-3")).thenReturn("");

        List<Map<String, String>> history = List.of(
                Map.of("role", "user", "content", "问题1"),
                Map.of("role", "assistant", "content", "回答1")
        );

        Object context = invokeBuildSummaryContext("conv-3", history);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> modelHistory =
                (List<Map<String, String>>) context.getClass().getDeclaredMethod("modelHistory").invoke(context);

        assertEquals(2, modelHistory.size());
        verify(deepSeekClient, never()).summarizeConversation(anyString());
    }

    private Object invokeBuildSummaryContext(String conversationId, List<Map<String, String>> history) throws Exception {
        Method method = ChatHandler.class.getDeclaredMethod("buildSummaryContext", String.class, List.class);
        method.setAccessible(true);
        return method.invoke(chatHandler, conversationId, history);
    }
}
