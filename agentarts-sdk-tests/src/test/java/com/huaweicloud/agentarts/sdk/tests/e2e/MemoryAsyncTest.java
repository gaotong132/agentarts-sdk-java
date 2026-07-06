package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.sdk.memory.MemorySession;
import com.huaweicloud.agentarts.sdk.memory.model.*;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Memory async tests — (8 tests) exercising the genuine Reactor {@code Mono}-returning
 * async API on {@link MemoryClient}. Each test subscribes to the returned Mono (via
 * {@code .block()} in the test only); the production async methods never block.
 *
 * <p>Requires AGENTARTS_TEST_ALLOW_CREATE=1.</p>
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

        // Seed session + messages (use the async API end-to-end)
        SessionInfo sess = dataClient.createMemorySessionAsync(
                memorySpace.getId(), null, "aa-it-actor", null).block();
        assertNotNull(sess);
        sessionId = sess.getId();
        List<TextMessage> msgs = List.of(
                new TextMessage("user", "async hello"),
                new TextMessage("assistant", "async reply")
        );
        MessageBatchResponse batch = dataClient.addMessagesAsync(memorySpace.getId(), sessionId, msgs,
                null, null, true).block(); // is_force_extract=true
        assertNotNull(batch);
        msgIds = batch.getItems().stream().map(MessageInfo::getId).toList();
    }

    @AfterAll
    static void tearDown() {
        if (registry != null) registry.cleanupAll();
        if (dataClient != null) dataClient.close();
        if (controlClient != null) controlClient.close();
    }

    // 1. test_async_get_last_k_messages
    @Test @Order(1)
    @DisplayName("async get_last_k_messages returns 2 messages")
    void testAsyncGetLastKMessages() {
        List<MessageInfo> msgs = dataClient.getLastKMessagesAsync(sessionId, 10, memorySpace.getId())
                .block();
        assertNotNull(msgs);
        assertEquals(2, msgs.size());
    }

    // 2. test_async_list_messages
    @Test @Order(2)
    @DisplayName("async list_messages returns a consistent page (total == items.size)")
    void testAsyncListMessages() {
        MessageListResponse result = dataClient.listMessagesAsync(
                memorySpace.getId(), sessionId, 10, 0).block();
        assertNotNull(result);
        assertNotNull(result.getItems(), "items should be a list");
        assertTrue(result.getItems() instanceof java.util.List, "items should be a List");
        assertTrue(result.getTotal() >= 2, "total should be at least 2 (seeded messages)");
        assertEquals(result.getItems().size(), result.getTotal(),
                "total must match items.size() for the returned page");
    }

    // 3. test_async_get_message
    @Test @Order(3)
    @DisplayName("async get_message returns correct message")
    void testAsyncGetMessage() {
        MessageInfo msg = dataClient.getMessageAsync(msgIds.get(0), memorySpace.getId(), sessionId)
                .block();
        assertNotNull(msg);
        assertEquals(msgIds.get(0), msg.getId());
    }

    // 4. test_async_search_memories
    @Test @Order(4)
    @DisplayName("async search_memories returns a consistent results list")
    void testAsyncSearchMemories() {
        MemorySearchResponse result = dataClient.searchMemoriesAsync(memorySpace.getId()).block();
        assertNotNull(result);
        assertNotNull(result.getResults(), "results should be a list");
        assertTrue(result.getResults() instanceof java.util.List, "results should be a List");
        assertTrue(result.getTotal() >= 0, "total should be a non-negative int");
        assertEquals(result.getResults().size(), result.getTotal(),
                "total must match results.size()");
    }

    // 5. test_async_list_memories
    @Test @Order(5)
    @DisplayName("async list_memories returns a consistent items list")
    void testAsyncListMemories() {
        MemoryListResponse result = dataClient.listMemoriesAsync(memorySpace.getId()).block();
        assertNotNull(result);
        assertNotNull(result.getItems(), "items should be a list");
        assertTrue(result.getItems() instanceof java.util.List, "items should be a List");
        assertTrue(result.getTotal() >= 0, "total should be a non-negative int");
        assertEquals(result.getItems().size(), result.getTotal(),
                "total must match items.size() for the returned page");
    }

    // 6. test_async_delete_memory
    @Test @Order(6)
    @DisplayName("async delete_memory removes an extracted memory (force_extract=true must yield memories)")
    void testAsyncDeleteMemory() throws Exception {
        // force_extract=true was requested in setUp; memory extraction should produce
        // memories to delete. Poll using the async API, then ASSERT non-empty (do not skip).
        MemoryListResponse result = null;
        for (int i = 0; i < 24; i++) {
            result = dataClient.listMemoriesAsync(memorySpace.getId(), 10, 0, null).block();
            if (result.getItems() != null && !result.getItems().isEmpty()) break;
            Thread.sleep(5000);
        }
        assertNotNull(result, "listMemoriesAsync should return a response after polling");
        assertNotNull(result.getItems(), "extracted memories list should not be null");
        assertFalse(result.getItems().isEmpty(),
                "is_force_extract=true but no memories extracted after polling (24x5s) — extraction may be broken");
        String memoryId = result.getItems().get(0).getId();
        assertNotNull(memoryId, "extracted memory should have an id");
        dataClient.deleteMemoryAsync(memorySpace.getId(), memoryId).block();
        // Re-list and assert the deleted memory is gone.
        MemoryListResponse after = dataClient.listMemoriesAsync(memorySpace.getId(), 20, 0, null).block();
        assertNotNull(after);
        boolean stillThere = after.getItems() == null
                || after.getItems().stream().noneMatch(m -> memoryId.equals(m.getId()));
        assertTrue(stillThere, "deleted memory " + memoryId + " should no longer appear in list");
    }

    // 7. test_async_create_session_and_add_messages
    @Test @Order(7)
    @DisplayName("async create session + add messages + list messages")
    void testAsyncCreateSessionAndAddMessages() {
        MemoryClient asyncClient = new MemoryClient(E2EConfig.getRegion(), apiKey);
        try {
            SessionInfo session = asyncClient.createMemorySessionAsync(
                    memorySpace.getId(), null, "async-actor", null).block();
            assertNotNull(session);
            assertNotNull(session.getId());

            MessageBatchResponse batch = asyncClient.addMessagesAsync(
                    memorySpace.getId(), session.getId(),
                    List.of(new TextMessage("user", "async create test"))).block();
            assertNotNull(batch);
            assertEquals(1, batch.getItems().size());

            MessageListResponse listed = asyncClient.listMessagesAsync(
                    memorySpace.getId(), session.getId(), 10, 0).block();
            assertNotNull(listed);
            assertTrue(listed.getTotal() >= 1);
            assertEquals(listed.getItems().size(), listed.getTotal(),
                    "total must match items.size() for the returned page");
        } finally {
            asyncClient.close();
        }
    }

    // 8. test_async_session_wrapper
    @Test @Order(8)
    @DisplayName("MemorySession wrapper works end-to-end")
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
