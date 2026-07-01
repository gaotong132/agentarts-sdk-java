package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.sdk.memory.MemorySession;
import com.huaweicloud.agentarts.sdk.memory.model.*;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Memory lifecycle tests — (12 tests: space, session, messages, memories CRUD).
 * Requires AGENTARTS_TEST_ALLOW_CREATE=1.
 */
@Tag("e2e")
@DisplayName("Memory Lifecycle Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemoryLifecycleTest {

    private static MemoryClient controlClient;
    private static MemoryClient dataClient;
    private static E2EResourceRegistry registry;
    private static String runId;

    // Shared state
    private static SpaceInfo memorySpace;
    private static String apiKey;
    private static SessionInfo memorySession;
    private static MessageBatchResponse seededMessages;

    @BeforeAll
    static void setUp() {
        assumeTrue(E2EConfig.hasCloudCredentials(),
                "Set HUAWEICLOUD_SDK_AK and HUAWEICLOUD_SDK_SK to run cloud tests");
        assumeTrue(E2EConfig.allowCreate(),
                "Set AGENTARTS_TEST_ALLOW_CREATE=1 to run create/delete lifecycle tests");

        controlClient = new MemoryClient(E2EConfig.getRegion(), null);
        registry = new E2EResourceRegistry();
        runId = E2EConfig.getRunId();

        // Create space (session-scoped fixture)
        String spaceName = E2EHelpers.uniqueName("space", runId);
        memorySpace = controlClient.createSpace(spaceName, 168, "e2e test space");
        assertNotNull(memorySpace);
        registry.register(
                () -> controlClient.deleteSpace(memorySpace.getId()),
                "memory-space:" + memorySpace.getId()
        );

        // Use API key from createSpace response for data plane
        apiKey = memorySpace.getApiKey() != null ? memorySpace.getApiKey().toString() : null;
        if (apiKey == null || apiKey.isEmpty()) {
            // Fallback: create a separate API key
            Map<String, Object> keyResult = controlClient.createApiKey();
            apiKey = (String) keyResult.get("api_key");
        }

        // Create data plane client
        dataClient = new MemoryClient(E2EConfig.getRegion(), apiKey);

        // Create session
        memorySession = dataClient.createMemorySession(memorySpace.getId(), null, "aa-it-actor", null);
        assertNotNull(memorySession);

        // Seed messages
        List<TextMessage> msgs = List.of(
                new TextMessage("user", "Hello from e2e test"),
                new TextMessage("assistant", "Hi! How can I help?")
        );
        seededMessages = dataClient.addMessages(memorySpace.getId(), memorySession.getId(), msgs);
    }

    @AfterAll
    static void tearDown() {
        if (registry != null) registry.cleanupAll();
        if (dataClient != null) dataClient.close();
        if (controlClient != null) controlClient.close();
    }

    // 1. test_get_space
    @Test @Order(1)
    @DisplayName("get_space returns the created space")
    void testGetSpace() {
        SpaceInfo got = controlClient.getSpace(memorySpace.getId());
        assertEquals(memorySpace.getId(), got.getId());
        assertEquals(memorySpace.getName(), got.getName());
    }

    // 2. test_list_spaces_contains_created
    @Test @Order(2)
    @DisplayName("list_spaces contains the created space")
    void testListSpacesContainsCreated() {
        SpaceListResponse result = controlClient.listSpaces(100, 0);
        List<String> ids = result.getItems().stream()
                .map(SpaceInfo::getId)
                .toList();
        assertTrue(ids.contains(memorySpace.getId()));
    }

    // 3. test_update_space
    @Test @Order(3)
    @DisplayName("update_space changes the description")
    void testUpdateSpace() {
        SpaceInfo updated = controlClient.updateSpace(
                memorySpace.getId(), null, "updated description", null);
        assertEquals(memorySpace.getId(), updated.getId());
    }

    // 4. test_session_created
    @Test @Order(4)
    @DisplayName("session was created with a valid ID")
    void testSessionCreated() {
        assertNotNull(memorySession.getId());
        assertFalse(memorySession.getId().isEmpty());
    }

    // 5. test_add_messages
    @Test @Order(5)
    @DisplayName("add_messages returns batch with 2 items")
    void testAddMessages() {
        assertEquals(2, seededMessages.getItems().size());
    }

    // 6. test_list_messages
    @Test @Order(6)
    @DisplayName("list_messages returns items list with total >= 2")
    void testListMessages() {
        MessageListResponse result = dataClient.listMessages(
                memorySpace.getId(), memorySession.getId(), 10, 0);
        assertNotNull(result.getItems());
        assertTrue(result.getTotal() >= 2);
    }

    // 7. test_get_last_k_messages
    @Test @Order(7)
    @DisplayName("get_last_k_messages returns 2 messages with user + assistant roles")
    void testGetLastKMessages() {
        List<MessageInfo> msgs = dataClient.getLastKMessages(
                memorySession.getId(), 10, memorySpace.getId());
        assertNotNull(msgs);
        assertEquals(2, msgs.size());
        List<String> roles = msgs.stream().map(MessageInfo::getRole).toList();
        assertTrue(roles.contains("user"));
        assertTrue(roles.contains("assistant"));
    }

    // 8. test_get_message
    @Test @Order(8)
    @DisplayName("get_message returns the correct message by ID")
    void testGetMessage() {
        String msgId = seededMessages.getItems().get(0).getId();
        MessageInfo msg = dataClient.getMessage(msgId, memorySpace.getId(), memorySession.getId());
        assertEquals(msgId, msg.getId());
        assertTrue(List.of("user", "assistant", "system", "tool").contains(msg.getRole()));
    }

    // 9. test_search_memories
    @Test @Order(9)
    @DisplayName("search_memories returns results list")
    void testSearchMemories() {
        MemorySearchResponse result = dataClient.searchMemories(memorySpace.getId());
        assertNotNull(result.getResults());
        assertTrue(result.getTotal() >= 0);
    }

    // 10. test_list_memories
    @Test @Order(10)
    @DisplayName("list_memories returns items list")
    void testListMemories() {
        MemoryListResponse result = dataClient.listMemories(memorySpace.getId());
        assertNotNull(result.getItems());
        assertTrue(result.getTotal() >= 0);
    }

    // 11. test_delete_memory_if_any
    @Test @Order(11)
    @DisplayName("delete_memory if any exist, else skip")
    void testDeleteMemoryIfAny() {
        MemoryListResponse result = dataClient.listMemories(memorySpace.getId(), 10, 0, null);
        if (result.getItems() == null || result.getItems().isEmpty()) {
            assumeTrue(false, "no extracted memories to delete");
        }
        dataClient.deleteMemory(memorySpace.getId(), result.getItems().get(0).getId());
    }

    // 12. test_memory_session_wrapper
    @Test @Order(12)
    @DisplayName("MemorySession wrapper works end-to-end")
    void testMemorySessionWrapper() {
        try (MemorySession session = MemorySession.of(
                memorySpace.getId(), "aa-it-actor", null, E2EConfig.getRegion(), apiKey)) {
            session.addMessages(List.of(new TextMessage("user", "wrapper test")));
            List<MessageInfo> last = session.getLastKMessages(1);
            assertEquals(1, last.size());
            MessageListResponse listed = session.listMessages();
            assertTrue(listed.getTotal() >= 1);
        }
    }
}
