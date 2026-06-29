package com.huaweicloud.agentarts.sdk.memory;

import com.huaweicloud.agentarts.sdk.memory.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Memory module: model classes, MemoryClient API parity, MemorySession, RetrievalConfig.
 */
class MemoryModuleTest {

    // ========================
    // Python Parity: Method signature verification
    // ========================

    @Nested
    @DisplayName("Python Parity: MemoryClient API methods")
    class PythonParityTests {

        @Test
        void controlPlaneMethodsExist() throws Exception {
            // Mirrors Python MemoryClient control plane methods
            Class<?> cls = MemoryClient.class;
            assertNotNull(cls.getMethod("createSpace", String.class, int.class, String.class));
            assertNotNull(cls.getMethod("createSpace", String.class));
            assertNotNull(cls.getMethod("getSpace", String.class));
            assertNotNull(cls.getMethod("listSpaces", int.class, int.class));
            assertNotNull(cls.getMethod("listSpaces"));
            assertNotNull(cls.getMethod("updateSpace", String.class, String.class, String.class, Integer.class));
            assertNotNull(cls.getMethod("deleteSpace", String.class));
            assertNotNull(cls.getMethod("createApiKey"));
        }

        @Test
        void dataPlaneMethodsExist() throws Exception {
            // Mirrors Python MemoryClient data plane methods
            Class<?> cls = MemoryClient.class;
            assertNotNull(cls.getMethod("createMemorySession", String.class, String.class, String.class, String.class));
            assertNotNull(cls.getMethod("createMemorySession", String.class));
            assertNotNull(cls.getMethod("addMessages", String.class, String.class, List.class));
            assertNotNull(cls.getMethod("addMessages", String.class, String.class, List.class, Long.class, String.class, boolean.class));
            assertNotNull(cls.getMethod("getLastKMessages", String.class, int.class, String.class));
            assertNotNull(cls.getMethod("getMessage", String.class, String.class, String.class));
            assertNotNull(cls.getMethod("listMessages", String.class, String.class, int.class, int.class));
            assertNotNull(cls.getMethod("listMessages", String.class));
            assertNotNull(cls.getMethod("searchMemories", String.class, MemorySearchFilter.class));
            assertNotNull(cls.getMethod("searchMemories", String.class));
            assertNotNull(cls.getMethod("listMemories", String.class, int.class, int.class, MemoryListFilter.class));
            assertNotNull(cls.getMethod("listMemories", String.class));
            assertNotNull(cls.getMethod("getMemory", String.class, String.class));
            assertNotNull(cls.getMethod("deleteMemory", String.class, String.class));
        }

        @Test
        void constructorMatchesPython() throws Exception {
            // Python: __init__(region_name=None, api_key=None, verify_ssl=True)
            Class<?> cls = MemoryClient.class;
            assertNotNull(cls.getConstructor(String.class, String.class, boolean.class));
            assertNotNull(cls.getConstructor(String.class, String.class));
            assertNotNull(cls.getConstructor());
        }

        @Test
        void implementsAutoCloseable() {
            // Python: __enter__/__exit__ (context manager)
            assertTrue(AutoCloseable.class.isAssignableFrom(MemoryClient.class));
        }
    }

    // ========================
    // Model classes
    // ========================

    @Nested
    @DisplayName("Model classes")
    class ModelTests {

        @Test
        void textMessageToDict() {
            TextMessage msg = new TextMessage("user", "Hello!");
            Map<String, Object> dict = msg.toDict();
            assertEquals("text", dict.get("type"));
            assertEquals("user", dict.get("role"));
            assertEquals("Hello!", dict.get("content"));
        }

        @Test
        void textMessageWithActorId() {
            TextMessage msg = new TextMessage("assistant", "Hi");
            msg.setActorId("actor-1");
            Map<String, Object> dict = msg.toDict();
            assertEquals("actor-1", dict.get("actor_id"));
        }

        @Test
        void toolCallMessageToDict() {
            ToolCallMessage msg = new ToolCallMessage("call-1", "search", Map.of("query", "test"));
            Map<String, Object> dict = msg.toDict();
            assertEquals("tool_call", dict.get("type"));
            assertEquals("call-1", dict.get("id"));
            assertEquals("search", dict.get("name"));
            assertTrue(dict.get("arguments").toString().contains("query"));
        }

        @Test
        void toolCallMessageStringArgs() {
            ToolCallMessage msg = new ToolCallMessage("call-2", "exec", "{\"code\":\"print(1)\"}");
            assertEquals("{\"code\":\"print(1)\"}", msg.getArguments());
        }

        @Test
        void toolResultMessageToDict() {
            ToolResultMessage msg = new ToolResultMessage("call-1", "result data");
            Map<String, Object> dict = msg.toDict();
            assertEquals("tool_result", dict.get("type"));
            assertEquals("call-1", dict.get("tool_call_id"));
            assertEquals("result data", dict.get("content"));
        }

        @Test
        void memorySearchFilterToDict() {
            MemorySearchFilter filter = new MemorySearchFilter();
            filter.setQuery("test query");
            filter.setActorId("actor-1");
            filter.setTopK(5);
            filter.setMinScore(0.7);
            Map<String, Object> dict = filter.toDict();
            assertEquals("test query", dict.get("query"));
            assertEquals("actor-1", dict.get("actor_id"));
            assertEquals(5, dict.get("top_k"));
            assertEquals(0.7, dict.get("min_score"));
        }

        @Test
        void memorySearchFilterExcludesNulls() {
            MemorySearchFilter filter = new MemorySearchFilter();
            filter.setQuery("test");
            Map<String, Object> dict = filter.toDict();
            assertTrue(dict.containsKey("query"));
            assertFalse(dict.containsKey("actor_id"));
            assertFalse(dict.containsKey("top_k"));
        }

        @Test
        void memoryListFilterToDict() {
            MemoryListFilter filter = new MemoryListFilter();
            filter.setActorId("actor-1");
            filter.setSortBy("created_at");
            filter.setSortOrder("desc");
            Map<String, Object> dict = filter.toDict();
            assertEquals("actor-1", dict.get("actor_id"));
            assertEquals("created_at", dict.get("sort_by"));
            assertEquals("desc", dict.get("sort_order"));
        }
    }

    // ========================
    // RetrievalConfig (Python parity)
    // ========================

    @Nested
    @DisplayName("RetrievalConfig Python parity")
    class RetrievalConfigTests {

        @Test
        void defaultValuesMatchPython() {
            // Python: user_id=None, max_tokens=0, top_k=2, score_threshold=0.6
            RetrievalConfig config = new RetrievalConfig();
            assertNull(config.getUserId());
            assertEquals(0, config.getMaxTokens());
            assertEquals(2, config.getTopK());
            assertEquals(0.6, config.getScoreThreshold(), 0.001);
        }

        @Test
        void fieldsSettable() {
            RetrievalConfig config = new RetrievalConfig();
            config.setUserId("user-1");
            config.setMaxTokens(100);
            config.setTopK(5);
            config.setScoreThreshold(0.8);
            assertEquals("user-1", config.getUserId());
            assertEquals(100, config.getMaxTokens());
            assertEquals(5, config.getTopK());
            assertEquals(0.8, config.getScoreThreshold(), 0.001);
        }
    }

    // ========================
    // MemorySession (Python parity)
    // ========================

    @Nested
    @DisplayName("MemorySession Python parity")
    class SessionTests {

        @Test
        void factoryMethodExists() throws Exception {
            // Python: MemorySession.of(space_id, actor_id, session_id=None, region_name=None, api_key=None)
            assertNotNull(MemorySession.class.getMethod("of", String.class, String.class, String.class, String.class, String.class));
            assertNotNull(MemorySession.class.getMethod("of", String.class, String.class));
        }

        @Test
        void preBoundMethodsExist() throws Exception {
            // Python: MemorySession pre-binds space_id/session_id
            Class<?> cls = MemorySession.class;
            assertNotNull(cls.getMethod("addMessages", List.class));
            assertNotNull(cls.getMethod("getLastKMessages", int.class));
            assertNotNull(cls.getMethod("getMessage", String.class));
            assertNotNull(cls.getMethod("listMessages"));
            assertNotNull(cls.getMethod("searchMemories"));
            assertNotNull(cls.getMethod("listMemories"));
            assertNotNull(cls.getMethod("getMemory", String.class));
            assertNotNull(cls.getMethod("deleteMemory", String.class));
        }

        @Test
        void implementsAutoCloseable() {
            assertTrue(AutoCloseable.class.isAssignableFrom(MemorySession.class));
        }
    }
}
