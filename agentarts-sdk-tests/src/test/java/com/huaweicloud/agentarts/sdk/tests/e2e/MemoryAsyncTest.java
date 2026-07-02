package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.sdk.memory.MemorySession;
import com.huaweicloud.agentarts.sdk.memory.model.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Memory async tests — (8 tests: async memory operations + session wrapper).
 * Uses Reactor's Mono.fromCallable + Schedulers.boundedElastic to simulate
 * Python's asyncio pattern.
 * Requires AGENTARTS_TEST_ALLOW_CREATE=1.
 */
@Tag("e2e")
@DisplayName("Memory Async Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemoryAsyncTest {

    private static MemoryClient controlClient;
    private static MemoryClient dataClient;
    private static E2EResourceRegistry registry;
    private static String runId;
    private static SpaceInfo memorySpace;
    private static String apiKey;
    private static String sessionId;
    private static List<String> msgIds;

    @BeforeAll
    static void setUp() {
        assumeTrue(E2EConfig.hasCloudCredentials(),
                "Set HUAWEICLOUD_SDK_AK and HUAWEICLOUD_SDK_SK to run cloud tests");
        assumeTrue(E2EConfig.allowCreate(),
                "Set AGENTARTS_TEST_ALLOW_CREATE=1 to run create/delete lifecycle tests");

        controlClient = new MemoryClient(E2EConfig.getRegion(), null);
        registry = new E2EResourceRegistry();
        runId = E2EConfig.getRunId();

        // Create space
        String spaceName = E2EHelpers.uniqueName("async", runId);
        memorySpace = controlClient.createSpace(spaceName, 168, "async e2e test");
        registry.register(() -> controlClient.deleteSpace(memorySpace.getId()), "space:" + memorySpace.getId());

        apiKey = memorySpace.getApiKey() != null ? memorySpace.getApiKey().toString() : null;
        if (apiKey == null || apiKey.isEmpty()) {
            ApiKeyInfo keyResult = controlClient.createApiKey();
            apiKey = keyResult != null ? keyResult.getApiKey() : null;
        }
        dataClient = new MemoryClient(E2EConfig.getRegion(), apiKey);

        // Seed session + messages
        SessionInfo sess = dataClient.createMemorySession(memorySpace.getId(), null, "aa-it-actor", null);
        sessionId = sess.getId();
        List<TextMessage> msgs = List.of(
                new TextMessage("user", "async hello"),
                new TextMessage("assistant", "async reply")
        );
        MessageBatchResponse batch = dataClient.addMessages(memorySpace.getId(), sessionId, msgs,
                null, null, true); // is_force_extract=true
        msgIds = batch.getItems().stream().map(MessageInfo::getId).toList();
    }

    @AfterAll
    static void tearDown() {
        if (registry != null) registry.cleanupAll();
        if (dataClient != null) dataClient.close();
        if (controlClient != null) controlClient.close();
    }

    /** Wrap a sync call in Reactor Mono (simulates Python async). */
    private <T> T asyncCall(java.util.concurrent.Callable<T> callable) {
        return Mono.fromCallable(callable)
                .subscribeOn(Schedulers.boundedElastic())
                .block();
    }

    // 1. test_async_get_last_k_messages
    @Test @Order(1)
    @DisplayName("async get_last_k_messages returns 2 messages")
    void testAsyncGetLastKMessages() {
        List<MessageInfo> msgs = asyncCall(
                () -> dataClient.getLastKMessages(sessionId, 10, memorySpace.getId()));
        assertEquals(2, msgs.size());
    }

    // 2. test_async_list_messages
    @Test @Order(2)
    @DisplayName("async list_messages returns total >= 2")
    void testAsyncListMessages() {
        MessageListResponse result = asyncCall(
                () -> dataClient.listMessages(memorySpace.getId(), sessionId, 10, 0));
        assertTrue(result.getTotal() >= 2);
    }

    // 3. test_async_get_message
    @Test @Order(3)
    @DisplayName("async get_message returns correct message")
    void testAsyncGetMessage() {
        MessageInfo msg = asyncCall(
                () -> dataClient.getMessage(msgIds.get(0), memorySpace.getId(), sessionId));
        assertEquals(msgIds.get(0), msg.getId());
    }

    // 4. test_async_search_memories
    @Test @Order(4)
    @DisplayName("async search_memories returns results")
    void testAsyncSearchMemories() {
        MemorySearchResponse result = asyncCall(
                () -> dataClient.searchMemories(memorySpace.getId()));
        assertNotNull(result.getResults());
        assertTrue(result.getTotal() >= 0);
    }

    // 5. test_async_list_memories
    @Test @Order(5)
    @DisplayName("async list_memories returns items")
    void testAsyncListMemories() {
        MemoryListResponse result = asyncCall(
                () -> dataClient.listMemories(memorySpace.getId()));
        assertNotNull(result.getItems());
        assertTrue(result.getTotal() >= 0);
    }

    // 6. test_async_delete_memory_if_any
    @Test @Order(6)
    @DisplayName("async delete_memory if any exist, else skip")
    void testAsyncDeleteMemoryIfAny() throws Exception {
        // Poll for memory extraction (force_extract may still be async on the backend)
        MemoryListResponse result = null;
        for (int i = 0; i < 10; i++) {
            result = asyncCall(() -> dataClient.listMemories(memorySpace.getId(), 10, 0, null));
            if (result.getItems() != null && !result.getItems().isEmpty()) break;
            Thread.sleep(5000);
        }
        if (result == null || result.getItems() == null || result.getItems().isEmpty()) {
            assumeTrue(false, "no extracted memories to delete in async path after polling (10x5s)");
        }
        final MemoryListResponse finalResult = result;
        asyncCall(() -> {
            dataClient.deleteMemory(memorySpace.getId(), finalResult.getItems().get(0).getId());
            return null;
        });
    }

    // 7. test_async_create_session_and_add_messages
    @Test @Order(7)
    @DisplayName("async create session + add messages + list messages")
    void testAsyncCreateSessionAndAddMessages() {
        MemoryClient asyncClient = new MemoryClient(E2EConfig.getRegion(), apiKey);
        try {
            SessionInfo session = asyncCall(
                    () -> asyncClient.createMemorySession(memorySpace.getId(), null, "async-actor", null));
            assertNotNull(session.getId());

            MessageBatchResponse batch = asyncCall(() -> asyncClient.addMessages(
                    memorySpace.getId(), session.getId(),
                    List.of(new TextMessage("user", "async create test"))));
            assertEquals(1, batch.getItems().size());

            MessageListResponse listed = asyncCall(
                    () -> asyncClient.listMessages(memorySpace.getId(), session.getId(), 10, 0));
            assertTrue(listed.getTotal() >= 1);
        } finally {
            asyncClient.close();
        }
    }

    // 8. test_async_session_wrapper
    @Test @Order(8)
    @DisplayName("AsyncMemorySession wrapper works end-to-end")
    void testAsyncSessionWrapper() {
        try (MemorySession session = MemorySession.of(
                memorySpace.getId(), "async-actor", null, E2EConfig.getRegion(), apiKey)) {
            session.addMessages(List.of(new TextMessage("user", "async wrapper test")));
            List<MessageInfo> last = session.getLastKMessages(1);
            assertEquals(1, last.size());
            MessageListResponse listed = session.listMessages();
            assertTrue(listed.getTotal() >= 1);
        }
    }
}
