package com.huaweicloud.agentarts.examples.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.agentscope.core.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OpenAI 格式转换器
 * 将 AgentScope 事件流转换为 OpenAI SSE 格式
 */
public class OpenAiEventConverter {
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(OpenAiEventConverter.class);

    // 用于跟踪工具调用状态
    private final Map<String, ToolCallState> toolCallStates = new HashMap<>();
    private final String modelName;
    private final String id;
    private final long created;
    private boolean hasSentRole = false;
    private String finishReason = null;

    // 当前正在处理的工具调用索引
    private int currentToolIndex = 0;

    public OpenAiEventConverter(String modelName) {
        this.modelName = modelName;
        this.id = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        this.created = System.currentTimeMillis() / 1000;
    }

    /**
     * 主转换方法 - 将事件转换为 OpenAI 格式的 JSON 字符串
     */
    public String convertEventToOpenAiChunk(AgentEvent event) {
        if (event == null) {
            return null;
        }

        try {
            // 创建基础响应对象
            ObjectNode chunk = OBJECT_MAPPER.createObjectNode();
            chunk.put("id", id);
            chunk.put("object", "chat.completion.chunk");
            chunk.put("created", created);
            chunk.put("model", modelName);

            ObjectNode choice = OBJECT_MAPPER.createObjectNode();
            choice.put("index", 0);

            ObjectNode delta = OBJECT_MAPPER.createObjectNode();

            // 根据事件类型处理
            if (event instanceof ThinkingBlockStartEvent) {
                // 不需要在流中输出特殊内容，可以忽略
                return null;
            }
            else if (event instanceof ThinkingBlockDeltaEvent reasoningDelta) {
                // 推理内容 - 可以作为普通文本输出
                String reasoningContent = reasoningDelta.getDelta();
                if (reasoningContent != null && !reasoningContent.isEmpty()) {
                    delta.put("content", reasoningContent);
                    // 首次发送时需要设置角色
                    if (!hasSentRole) {
                        delta.put("role", "assistant");
                        hasSentRole = true;
                    }
                }
            }
            else if (event instanceof TextBlockStartEvent textStart) {
                // 文本块开始 - 可以忽略或发送空内容
                if (!hasSentRole) {
                    delta.put("role", "assistant");
                    hasSentRole = true;
                }
                // 可以选择发送一个空 content 来开始流
                delta.put("content", "");
            }
            else if (event instanceof TextBlockDeltaEvent textDelta) {
                // 文本增量 - 主要的内容输出
                String content = textDelta.getDelta();
                if (content != null && !content.isEmpty()) {
                    delta.put("content", content);
                    if (!hasSentRole) {
                        delta.put("role", "assistant");
                        hasSentRole = true;
                    }
                }
            }
            else if (event instanceof TextBlockEndEvent) {
                // 文本块结束 - 不需要特殊处理
                return null;
            }
            else if (event instanceof ToolCallStartEvent toolStart) {
                // 工具调用开始
                String toolId = toolStart.getId() != null ? toolStart.getId() :
                        "call_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

                // 保存工具调用状态
                ToolCallState state = new ToolCallState();
                state.toolId = toolId;
                state.toolName = toolStart.getToolCallName();
                state.arguments = new StringBuilder();
                toolCallStates.put(toolId, state);

                // 构建 tool_calls 增量
                ObjectNode toolCallObj = OBJECT_MAPPER.createObjectNode();
                toolCallObj.put("index", currentToolIndex++);
                toolCallObj.put("id", toolId);
                toolCallObj.put("type", "function");

                ObjectNode function = OBJECT_MAPPER.createObjectNode();
                function.put("name", toolStart.getToolCallName());
                function.put("arguments", ""); // 初始为空
                toolCallObj.putIfAbsent("function", function);

                delta.putArray("tool_calls").add(toolCallObj);

                if (!hasSentRole) {
                    delta.put("role", "assistant");
                    hasSentRole = true;
                }
            }
            else if (event instanceof ToolCallDeltaEvent toolDelta) {
                // 工具调用参数增量
                String toolId = toolDelta.getId();
                String argumentsDelta = toolDelta.getDelta();

                // 更新工具调用状态
                ToolCallState state = toolCallStates.get(toolId);
                if (state != null) {
                    state.arguments.append(argumentsDelta);
                }

                // 构建 tool_calls 增量
                ObjectNode toolCallObj = OBJECT_MAPPER.createObjectNode();
                toolCallObj.put("index", getToolIndex(toolId));
                toolCallObj.put("id", toolId);
                toolCallObj.put("type", "function");

                ObjectNode function = OBJECT_MAPPER.createObjectNode();
                function.put("name", ""); // 增量中不重复发送名称
                function.put("arguments", argumentsDelta != null ? argumentsDelta : "");
                toolCallObj.putIfAbsent("function", function);

                delta.putArray("tool_calls").add(toolCallObj);
            }
            else if (event instanceof ToolCallEndEvent toolEnd) {
                // 工具调用结束
                String toolId = toolEnd.getId();
                String result = "success";

                // 如果工具执行结果需要返回给用户，创建一个包含结果的文本块
                // 格式化工具结果，类似于 AgentScope Web Starter 的做法
                String formattedResult = "[Tool: " + toolEnd.getToolCallId() + "] " + result;
                // 这里我们选择将工具结果作为普通文本发送
                // 注意：某些场景可能需要发送多个消息
                ObjectNode resultDelta = OBJECT_MAPPER.createObjectNode();
                resultDelta.put("content", formattedResult);
                resultDelta.put("role", "assistant");

                ObjectNode resultChunk = OBJECT_MAPPER.createObjectNode();
                resultChunk.put("id", id);
                resultChunk.put("object", "chat.completion.chunk");
                resultChunk.put("created", created);
                resultChunk.put("model", modelName);

                ObjectNode resultChoice = OBJECT_MAPPER.createObjectNode();
                resultChoice.put("index", 0);
                resultChoice.putIfAbsent("delta", resultDelta);

                resultChunk.putArray("choices").add(resultChoice);
                return resultChunk.toString();
            }
            else if (event instanceof AgentEndEvent) {
                // Agent 结束 - 发送 finish_reason
                finishReason = "stop";
            }
            else {
                // 未知事件类型，忽略
                logger.debug("Unhandled event type: {}", event.getClass().getSimpleName());
                return null;
            }

            // 如果 delta 为空且没有 finish_reason，返回 null
            if (delta.isEmpty() && finishReason == null) {
                return null;
            }

            // 设置 finish_reason
            if (finishReason != null) {
                choice.put("finish_reason", finishReason);
            }

            choice.putIfAbsent("delta", delta);
            chunk.putArray("choices").add(choice);

            return chunk.toString();

        } catch (Exception e) {
            logger.error("Error converting event to OpenAI format", e);
            return buildErrorJson("Event conversion failed: " + e.getMessage());
        }
    }

    /**
     * 获取工具调用的索引
     */
    private int getToolIndex(String toolId) {
        // 这里简化处理，实际使用中可能需要维护更复杂的映射
        // 可以通过工具调用顺序来确定索引
        return 0;
    }

    /**
     * 构建错误 JSON
     */
    private String buildErrorJson(String errorMessage) {
        ObjectNode error = OBJECT_MAPPER.createObjectNode();
        error.put("message", errorMessage);
        error.put("type", "server_error");

        ObjectNode errorChunk = OBJECT_MAPPER.createObjectNode();
        errorChunk.putIfAbsent("error", error);

        return errorChunk.toString();
    }

    /**
     * 构建错误 JSON (从 Exception)
     */
    private String buildErrorJson(Throwable error) {
        ObjectNode errorObj = OBJECT_MAPPER.createObjectNode();
        errorObj.put("message", error.getMessage() != null ? error.getMessage() : "Unknown error");
        errorObj.put("type", error.getClass().getSimpleName());

        ObjectNode errorChunk = OBJECT_MAPPER.createObjectNode();
        errorChunk.putIfAbsent("error", errorObj);

        return errorChunk.toString();
    }

    /**
     * 工具调用状态跟踪
     */
    private static class ToolCallState {
        String toolId;
        String toolName;
        StringBuilder arguments;
    }

    /**
     * 重置转换器状态（每个新请求调用）
     */
    public void reset() {
        toolCallStates.clear();
        currentToolIndex = 0;
        hasSentRole = false;
        finishReason = null;
    }

    /**
     * 发送最终 DONE 信号前，生成最后一个包含 finish_reason 的 chunk
     */
    public String buildFinalChunk() {
        ObjectNode chunk = OBJECT_MAPPER.createObjectNode();
        chunk.put("id", id);
        chunk.put("object", "chat.completion.chunk");
        chunk.put("created", created);
        chunk.put("model", modelName);

        ObjectNode choice = OBJECT_MAPPER.createObjectNode();
        choice.put("index", 0);
        choice.put("finish_reason", finishReason != null ? finishReason : "stop");
        choice.putIfAbsent("delta", OBJECT_MAPPER.createObjectNode()); // 空 delta

        chunk.putArray("choices").add(choice);

        return chunk.toString();
    }
}
