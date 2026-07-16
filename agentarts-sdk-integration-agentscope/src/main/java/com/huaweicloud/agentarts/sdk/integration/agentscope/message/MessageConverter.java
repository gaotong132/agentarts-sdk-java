package com.huaweicloud.agentarts.sdk.integration.agentscope.message;

import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import com.huaweicloud.agentarts.sdk.memory.model.TextMessage;
import com.huaweicloud.agentarts.sdk.memory.model.ToolCallMessage;
import com.huaweicloud.agentarts.sdk.memory.model.ToolResultMessage;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolResultState;
import io.agentscope.core.message.ContentBlock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Bidirectional converter between AgentArts memory messages and agentscope ContentBlocks.
 *
 * <p>Mapping:</p>
 * <ul>
 *   <li>AgentArts {@link TextMessage} ↔ agentscope {@link TextBlock}</li>
 *   <li>AgentArts {@link ToolCallMessage} ↔ agentscope {@link ToolUseBlock}</li>
 *   <li>AgentArts {@link ToolResultMessage} ↔ agentscope {@link ToolResultBlock}</li>
 * </ul>
 */
public final class MessageConverter {

    private static final String STATE_META_KEY = "agentscope_tool_result_state";

    private MessageConverter() {
    }

    // ========================
    // AgentArts → agentscope
    // ========================

    /**
     * Convert an AgentArts TextMessage to an agentscope TextBlock.
     */
    public static TextBlock toTextBlock(TextMessage msg) {
        Objects.requireNonNull(msg, "msg");
        return TextBlock.builder().text(msg.getContent()).build();
    }

    /**
     * Convert an AgentArts ToolCallMessage to an agentscope ToolUseBlock.
     */
    public static ToolUseBlock toToolUseBlock(ToolCallMessage msg) {
        Objects.requireNonNull(msg, "msg");
        Map<String, Object> input;
        try {
            String args = msg.getArguments();
            if (JsonUtils.isNotBlank(args)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = JsonUtils.MAPPER
                        .readValue(args, Map.class);
                input = parsed;
            } else {
                input = Map.of();
            }
        } catch (Exception e) {
            input = Map.of("raw", msg.getArguments());
        }

        return ToolUseBlock.builder()
                .id(msg.getId())
                .name(msg.getName())
                .input(input)
                .build();
    }

    /**
     * Convert an AgentArts ToolResultMessage to an agentscope ToolResultBlock.
     */
    public static ToolResultBlock toToolResultBlock(ToolResultMessage msg) {
        Objects.requireNonNull(msg, "msg");
        TextBlock textOutput = TextBlock.builder().text(msg.getContent()).build();
        return ToolResultBlock.of(msg.getToolCallId(), "tool_result", textOutput)
                .withState(readState(msg.getMeta()));
    }

    // ========================
    // agentscope → AgentArts
    // ========================

    /**
     * Convert an agentscope TextBlock to an AgentArts TextMessage.
     */
    public static TextMessage toTextMessage(TextBlock block, String role) {
        Objects.requireNonNull(block, "block");
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role must not be blank");
        }
        return new TextMessage(role, block.getText());
    }

    /**
     * Convert an agentscope ToolUseBlock to an AgentArts ToolCallMessage.
     */
    public static ToolCallMessage toToolCallMessage(ToolUseBlock block) {
        Objects.requireNonNull(block, "block");
        String args;
        try {
            args = JsonUtils.MAPPER
                    .writeValueAsString(block.getInput());
        } catch (Exception e) {
            throw new MessageConversionException("Failed to serialize tool input", e);
        }
        return new ToolCallMessage(block.getId(), block.getName(), args);
    }

    /**
     * Convert an agentscope ToolResultBlock to an AgentArts ToolResultMessage.
     */
    public static ToolResultMessage toToolResultMessage(ToolResultBlock block) {
        Objects.requireNonNull(block, "block");
        if (block.getState() == ToolResultState.RUNNING) {
            throw new IllegalArgumentException("A running tool result cannot be persisted");
        }
        StringBuilder content = new StringBuilder();
        if (block.getOutput() != null) {
            for (ContentBlock cb : block.getOutput()) {
                if (cb instanceof TextBlock tb) {
                    content.append(tb.getText());
                }
            }
        }
        ToolResultMessage result = new ToolResultMessage(
                block.getId() != null ? block.getId() : "", content.toString());
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put(STATE_META_KEY, block.getState().getValue());
        result.setMeta(JsonUtils.toJson(meta));
        return result;
    }

    private static ToolResultState readState(String meta) {
        if (meta == null || meta.isBlank()) return ToolResultState.SUCCESS;
        try {
            JsonNodeHolder holder = JsonUtils.MAPPER.readValue(meta, JsonNodeHolder.class);
            if (holder.agentscopeToolResultState == null) return ToolResultState.SUCCESS;
            for (ToolResultState state : ToolResultState.values()) {
                if (state.getValue().equals(holder.agentscopeToolResultState)) return state;
            }
            throw new IllegalArgumentException("Unknown AgentScope tool result state");
        } catch (MessageConversionException e) {
            throw e;
        } catch (Exception e) {
            throw new MessageConversionException("Failed to read tool result metadata", e);
        }
    }

    private static final class JsonNodeHolder {
        @com.fasterxml.jackson.annotation.JsonProperty(STATE_META_KEY)
        private String agentscopeToolResultState;
    }
}
