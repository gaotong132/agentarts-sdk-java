package com.huaweicloud.agentarts.sdk.integration.agentscope;

import com.huaweicloud.agentarts.sdk.integration.agentscope.memory.InMemoryLongTermMemory;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.memory.StaticLongTermMemoryHook;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.tool.Toolkit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 ReActAgent 的 .longTermMemory + .longTermMemoryMode 是否自动注册
 * StaticLongTermMemoryHook —— 这是"STATIC_CONTROL 模式下记忆会被自动 record/retrieve"
 * 的前提。构建期即可验证，不需要 LLM（dummy model 不触网）。
 */
class LongTermMemoryWiringTest {

    @Test
    void staticControlAutoRegistersHook() {
        ReActAgent agent = ReActAgent.builder()
                .name("wiring-test")
                .model(OpenAIChatModel.builder().apiKey("dummy").modelName("gpt-4o").build())
                .toolkit(new Toolkit())
                .longTermMemory(new InMemoryLongTermMemory("user-1"))
                .longTermMemoryMode(LongTermMemoryMode.STATIC_CONTROL)
                .build();

        boolean hasStaticHook = agent.getHooks().stream()
                .anyMatch(h -> h instanceof StaticLongTermMemoryHook);
        assertTrue(hasStaticHook,
                "STATIC_CONTROL 模式应自动注册 StaticLongTermMemoryHook，否则 record/retrieve 不会被调用");
    }

    @Test
    void agentControlDoesNotRegisterStaticHook() {
        ReActAgent agent = ReActAgent.builder()
                .name("wiring-test-2")
                .model(OpenAIChatModel.builder().apiKey("dummy").modelName("gpt-4o").build())
                .toolkit(new Toolkit())
                .longTermMemory(new InMemoryLongTermMemory("user-1"))
                .longTermMemoryMode(LongTermMemoryMode.AGENT_CONTROL)
                .build();

        boolean hasStaticHook = agent.getHooks().stream()
                .anyMatch(h -> h instanceof StaticLongTermMemoryHook);
        assertFalse(hasStaticHook, "AGENT_CONTROL 不应注册 StaticLongTermMemoryHook（改由工具触发）");
    }

    @Test
    void noLongTermMemoryNoHook() {
        ReActAgent agent = ReActAgent.builder()
                .name("wiring-test-3")
                .model(OpenAIChatModel.builder().apiKey("dummy").modelName("gpt-4o").build())
                .toolkit(new Toolkit())
                .build();

        boolean anyLtmHook = agent.getHooks().stream()
                .anyMatch(h -> h instanceof StaticLongTermMemoryHook);
        assertFalse(anyLtmHook, "未设 longTermMemory 时不应有长期记忆 hook");
    }
}
