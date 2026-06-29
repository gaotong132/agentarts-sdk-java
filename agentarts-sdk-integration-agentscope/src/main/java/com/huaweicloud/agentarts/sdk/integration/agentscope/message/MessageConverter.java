package com.huaweicloud.agentarts.sdk.integration.agentscope.message;

import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import com.huaweicloud.agentarts.sdk.memory.model.TextMessage;
import com.huaweicloud.agentarts.sdk.memory.model.ToolCallMessage;
import com.huaweicloud.agentarts.sdk.memory.model.ToolResultMessage;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ContentBlock;

import java.util.Map;

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
public class MessageConverter {

    // ========================
    // AgentArts → agentscope
    // ========================

    /**
     * Convert an AgentArts TextMessage to an agentscope TextBlock.
     */
    public static TextBlock toTextBlock(TextMessage msg) {
        return TextBlock.builder().text(msg.getContent()).build();
    }

    /**
     * Convert an AgentArts ToolCallMessage to an agentscope ToolUseBlock.
     */
    public static ToolUseBlock toToolUseBlock(ToolCallMessage msg) {
        Map<String, Object> input;
        try {
            String args = msg.getArguments();
            if (args != null && !args.isEmpty()) {
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
        TextBlock textOutput = TextBlock.builder().text(msg.getContent()).build();
        return ToolResultBlock.of(msg.getToolCallId(), "tool_result", textOutput);
    }

    // ========================
    // agentscope → AgentArts
    // ========================

    /**
     * Convert an agentscope TextBlock to an AgentArts TextMessage.
     */
    public static TextMessage toTextMessage(TextBlock block, String role) {
        TextMessage msg = new TextMessage(role, block.getText());
        return msg;
    }

    /**
     * Convert an agentscope ToolUseBlock to an AgentArts ToolCallMessage.
     */
    public static ToolCallMessage toToolCallMessage(ToolUseBlock block) {
        String args;
        try {
            args = JsonUtils.MAPPER
                    .writeValueAsString(block.getInput());
        } catch (Exception e) {
            args = "{}";
        }
        return new ToolCallMessage(block.getId(), block.getName(), args);
    }

    /**
     * Convert an agentscope ToolResultBlock to an AgentArts ToolResultMessage.
     */
    public static ToolResultMessage toToolResultMessage(ToolResultBlock block) {
        String content = "";
        if (block.getOutput() != null) {
            for (ContentBlock cb : block.getOutput()) {
                if (cb instanceof TextBlock tb) {
                    content += tb.getText();
                }
            }
        }
        return new ToolResultMessage(block.getId() != null ? block.getId() : "", content);
    }
}
