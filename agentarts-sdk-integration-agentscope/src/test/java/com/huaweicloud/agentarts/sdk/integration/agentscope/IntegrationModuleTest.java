package com.huaweicloud.agentarts.sdk.integration.agentscope;

import com.huaweicloud.agentarts.sdk.integration.agentscope.message.MessageConverter;
import com.huaweicloud.agentarts.sdk.integration.agentscope.runtime.AgentscopeRuntimeHost;
import com.huaweicloud.agentarts.sdk.integration.agentscope.state.MemoryAgentStateStore;
import com.huaweicloud.agentarts.sdk.integration.agentscope.tool.CodeInterpreterTool;
import com.huaweicloud.agentarts.sdk.integration.agentscope.tool.MCPGatewayTool;
import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.sdk.memory.model.TextMessage;
import com.huaweicloud.agentarts.sdk.memory.model.ToolCallMessage;
import com.huaweicloud.agentarts.sdk.memory.model.ToolResultMessage;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import com.huaweicloud.agentarts.sdk.runtime.context.RequestContext;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for agentscope integration: interface contracts, message conversion, runtime bridge.
 */
class IntegrationModuleTest {

    // ========================
    // MemoryAgentStateStore — AgentStateStore interface contract
    // ========================

    @Nested
    @DisplayName("MemoryAgentStateStore implements AgentStateStore")
    class StateStoreTests {

        private MemoryAgentStateStore store;

        @BeforeEach
        void setUp() {
            MemoryClient client = new MemoryClient("cn-southwest-2", "test-key");
            store = new MemoryAgentStateStore(client, "space-123");
        }

        @Test
        void implementsAgentStateStore() {
            assertTrue(AgentStateStore.class.isAssignableFrom(MemoryAgentStateStore.class));
        }

        @Test
        void saveAndGetSingleState() {
            TestState state = new TestState("hello");
            store.save("user1", "session1", "key1", state);

            Optional<TestState> result = store.get("user1", "session1", "key1", TestState.class);
            assertTrue(result.isPresent());
            assertEquals("hello", result.get().value);
        }

        @Test
        void saveAndGetListState() {
            List<TestState> states = List.of(new TestState("a"), new TestState("b"), new TestState("c"));
            store.save("user1", "session1", "messages", states);

            List<TestState> result = store.getList("user1", "session1", "messages", TestState.class);
            assertEquals(3, result.size());
            assertEquals("a", result.get(0).value);
            assertEquals("b", result.get(1).value);
            assertEquals("c", result.get(2).value);
        }

        @Test
        void saveListDoesFullReplacement() {
            store.save("user1", "session1", "items", List.of(new TestState("old1"), new TestState("old2")));
            store.save("user1", "session1", "items", List.of(new TestState("new1")));

            List<TestState> result = store.getList("user1", "session1", "items", TestState.class);
            assertEquals(1, result.size());
            assertEquals("new1", result.get(0).value);
        }

        @Test
        void getReturnsEmptyWhenNotFound() {
            Optional<TestState> result = store.get("user1", "session1", "nonexistent", TestState.class);
            assertTrue(result.isEmpty());
        }

        @Test
        void getListReturnsEmptyWhenNotFound() {
            List<TestState> result = store.getList("user1", "session1", "nonexistent", TestState.class);
            assertTrue(result.isEmpty());
        }

        @Test
        void existsReturnsTrueAfterSave() {
            store.save("user1", "session1", "key1", new TestState("val"));
            assertTrue(store.exists("user1", "session1"));
        }

        @Test
        void existsReturnsFalseForUnknown() {
            assertFalse(store.exists("user1", "unknown-session"));
        }

        @Test
        void deleteRemovesSession() {
            store.save("user1", "session1", "key1", new TestState("val"));
            store.delete("user1", "session1");
            assertFalse(store.exists("user1", "session1"));
        }

        @Test
        void deleteByKeyRemovesOnlyKey() {
            store.save("user1", "session1", "key1", new TestState("val1"));
            store.save("user1", "session1", "key2", new TestState("val2"));
            store.delete("user1", "session1", "key1");

            assertTrue(store.get("user1", "session1", "key1", TestState.class).isEmpty());
            assertTrue(store.get("user1", "session1", "key2", TestState.class).isPresent());
        }

        @Test
        void listSessionIdsReturnsAll() {
            store.save("user1", "s1", "k", new TestState("1"));
            store.save("user1", "s2", "k", new TestState("2"));
            store.save("user1", "s3", "k", new TestState("3"));

            Set<String> ids = store.listSessionIds("user1");
            assertEquals(3, ids.size());
            assertTrue(ids.contains("s1"));
            assertTrue(ids.contains("s2"));
            assertTrue(ids.contains("s3"));
        }

        @Test
        void listSessionIdsReturnsEmptyForUnknownUser() {
            Set<String> ids = store.listSessionIds("unknown");
            assertTrue(ids.isEmpty());
        }

        @Test
        void nullUserIdMapsToAnon() {
            store.save(null, "session1", "key1", new TestState("val"));
            assertTrue(store.exists(null, "session1"));

            Optional<TestState> result = store.get(null, "session1", "key1", TestState.class);
            assertTrue(result.isPresent());
        }
    }

    // ========================
    // MCPGatewayTool — AgentTool interface contract
    // ========================

    @Nested
    @DisplayName("MCPGatewayTool implements AgentTool")
    class MCPGatewayToolTests {

        @Test
        void implementsAgentTool() {
            assertTrue(AgentTool.class.isAssignableFrom(MCPGatewayTool.class));
        }

        @Test
        void hasCorrectName() {
            MCPGatewayTool tool = new MCPGatewayTool(null);
            assertEquals("mcp_gateway_call", tool.getName());
        }

        @Test
        void hasDescription() {
            MCPGatewayTool tool = new MCPGatewayTool(null);
            assertNotNull(tool.getDescription());
            assertFalse(tool.getDescription().isEmpty());
        }

        @Test
        void parametersSchemaIsValid() {
            MCPGatewayTool tool = new MCPGatewayTool(null);
            Map<String, Object> params = tool.getParameters();
            assertEquals("object", params.get("type"));
            assertNotNull(params.get("properties"));
            assertNotNull(params.get("required"));

            @SuppressWarnings("unchecked")
            List<String> required = (List<String>) params.get("required");
            assertTrue(required.contains("gateway_id"));
            assertTrue(required.contains("target_id"));
            assertTrue(required.contains("tool_name"));
        }

        @Test
        void getStrictReturnsNull() {
            MCPGatewayTool tool = new MCPGatewayTool(null);
            assertNull(tool.getStrict());
        }

        @Test
        void getOutputSchemaReturnsNull() {
            MCPGatewayTool tool = new MCPGatewayTool(null);
            assertNull(tool.getOutputSchema());
        }
    }

    // ========================
    // CodeInterpreterTool — AgentTool interface contract
    // ========================

    @Nested
    @DisplayName("CodeInterpreterTool implements AgentTool")
    class CodeInterpreterToolTests {

        @Test
        void implementsAgentTool() {
            assertTrue(AgentTool.class.isAssignableFrom(CodeInterpreterTool.class));
        }

        @Test
        void hasCorrectName() {
            CodeInterpreterTool tool = new CodeInterpreterTool(null);
            assertEquals("code_interpreter", tool.getName());
        }

        @Test
        void hasDescription() {
            CodeInterpreterTool tool = new CodeInterpreterTool(null);
            assertNotNull(tool.getDescription());
            assertTrue(tool.getDescription().contains("code"));
        }

        @Test
        void parametersSchemaRequiresCode() {
            CodeInterpreterTool tool = new CodeInterpreterTool(null);
            Map<String, Object> params = tool.getParameters();
            assertEquals("object", params.get("type"));

            @SuppressWarnings("unchecked")
            List<String> required = (List<String>) params.get("required");
            assertTrue(required.contains("code"));
        }
    }

    // ========================
    // AgentscopeRuntimeHost — context bridging
    // ========================

    @Nested
    @DisplayName("AgentscopeRuntimeHost context bridging")
    class RuntimeHostTests {

        @Test
        void bridgeContextMapsSessionId() {
            RequestContext rc = new RequestContext("req-1", "sess-123", "user-456", "token-789");
            RuntimeContext ctx = AgentscopeRuntimeHost.bridgeContext(rc);
            assertEquals("sess-123", ctx.getSessionId());
        }

        @Test
        void bridgeContextMapsUserId() {
            RequestContext rc = new RequestContext("req-1", "sess-123", "user-456", "token-789");
            RuntimeContext ctx = AgentscopeRuntimeHost.bridgeContext(rc);
            assertEquals("user-456", ctx.getUserId());
        }

        @Test
        void bridgeContextMapsRequestId() {
            RequestContext rc = new RequestContext("req-1", "sess-123", "user-456", "token-789");
            RuntimeContext ctx = AgentscopeRuntimeHost.bridgeContext(rc);
            assertEquals("req-1", ctx.get("requestId"));
        }

        @Test
        void bridgeContextMapsWorkloadAccessToken() {
            RequestContext rc = new RequestContext("req-1", "sess-123", "user-456", "token-789");
            RuntimeContext ctx = AgentscopeRuntimeHost.bridgeContext(rc);
            assertEquals("token-789", ctx.get("workloadAccessToken"));
        }

        @Test
        void bridgeContextHandlesNullFields() {
            RequestContext rc = new RequestContext(null, null, null, null);
            RuntimeContext ctx = AgentscopeRuntimeHost.bridgeContext(rc);
            assertNotNull(ctx);
            assertNull(ctx.getSessionId());
            assertNull(ctx.getUserId());
        }
    }

    // ========================
    // MessageConverter — bidirectional conversion
    // ========================

    @Nested
    @DisplayName("MessageConverter bidirectional conversion")
    class MessageConverterTests {

        @Test
        void textMessageToTextBlock() {
            TextMessage msg = new TextMessage("user", "Hello world");
            TextBlock block = MessageConverter.toTextBlock(msg);
            assertEquals("Hello world", block.getText());
        }

        @Test
        void textBlockToTextMessage() {
            TextBlock block = TextBlock.builder().text("Hello world").build();
            TextMessage msg = MessageConverter.toTextMessage(block, "assistant");
            assertEquals("Hello world", msg.getContent());
            assertEquals("assistant", msg.getRole());
        }

        @Test
        void toolCallMessageToToolUseBlock() {
            ToolCallMessage msg = new ToolCallMessage("call-1", "search", "{\"query\":\"test\"}");
            ToolUseBlock block = MessageConverter.toToolUseBlock(msg);
            assertEquals("call-1", block.getId());
            assertEquals("search", block.getName());
            assertEquals("test", block.getInput().get("query"));
        }

        @Test
        void toolUseBlockToToolCallMessage() {
            ToolUseBlock block = ToolUseBlock.builder()
                    .id("call-2")
                    .name("execute")
                    .input(Map.of("code", "print(1)"))
                    .build();
            ToolCallMessage msg = MessageConverter.toToolCallMessage(block);
            assertEquals("call-2", msg.getId());
            assertEquals("execute", msg.getName());
            assertTrue(msg.getArguments().contains("print(1)"));
        }

        @Test
        void toolResultMessageToToolResultBlock() {
            ToolResultMessage msg = new ToolResultMessage("call-1", "result data");
            ToolResultBlock block = MessageConverter.toToolResultBlock(msg);
            assertEquals("call-1", block.getId());
            assertFalse(block.getOutput().isEmpty());
        }

        @Test
        void toolResultBlockToToolResultMessage() {
            TextBlock textOut = TextBlock.builder().text("output text").build();
            ToolResultBlock block = ToolResultBlock.of("call-1", "tool", textOut);
            ToolResultMessage msg = MessageConverter.toToolResultMessage(block);
            assertEquals("call-1", msg.getToolCallId());
            assertEquals("output text", msg.getContent());
        }

        @Test
        void roundTripTextPreservesContent() {
            TextMessage original = new TextMessage("user", "Round trip test");
            TextBlock block = MessageConverter.toTextBlock(original);
            TextMessage roundTripped = MessageConverter.toTextMessage(block, "user");
            assertEquals(original.getContent(), roundTripped.getContent());
        }
    }

    // ========================
    // Helper: test State implementation
    // ========================

    static class TestState implements State {
        final String value;
        TestState(String value) { this.value = value; }
    }
}
