package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.integration.agentscope.state.MemoryAgentStateStore;
import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.sdk.memory.model.*;
import io.agentscope.core.state.State;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MemoryAgentStateStore E2E test — validates real Memory API roundtrip.
 *
 * <p>Creates a Memory Space, uses the space's API key to build a
 * MemoryAgentStateStore, then tests save/get roundtrip against the
 * real AgentArts Memory backend.</p>
 *
 * <p>Requires AGENTARTS_TEST_ALLOW_CREATE=1.</p>
 */
@Tag("e2e")
@DisplayName("MemoryAgentStateStore E2E (real API)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemoryAgentStateStoreE2ETest {

    private static MemoryClient controlClient;
    private static MemoryClient dataClient;
    private static MemoryAgentStateStore store;
    private static E2EResourceRegistry registry;
    private static SpaceInfo space;
    private static String apiKey;

    @BeforeAll
    static void setUp() {
        assumeTrue(E2EConfig.hasCloudCredentials(),
                "Set HUAWEICLOUD_SDK_AK and HUAWEICLOUD_SDK_SK");
        assumeTrue(E2EConfig.allowCreate(),
                "Set AGENTARTS_TEST_ALLOW_CREATE=1");

        controlClient = new MemoryClient(E2EConfig.getRegion(), null);
        registry = new E2EResourceRegistry();

        // Create a Memory Space
        String spaceName = E2EHelpers.uniqueName("state-store", E2EConfig.getRunId());
        space = controlClient.createSpace(spaceName, 168, "MemoryAgentStateStore E2E test");
        assertNotNull(space, "createSpace should return a valid SpaceInfo");
        registry.register(() -> controlClient.deleteSpace(space.getId()), "space:" + space.getId());

        // Get API key from the space
        apiKey = space.getApiKey() != null ? space.getApiKey().toString() : null;
        if (apiKey == null || apiKey.isEmpty()) {
            Map<String, Object> keyResult = controlClient.createApiKey();
            apiKey = (String) keyResult.get("api_key");
        }
        assertNotNull(apiKey, "API key should be available");

        dataClient = new MemoryClient(E2EConfig.getRegion(), apiKey);

        // Create the MemoryAgentStateStore
        store = new MemoryAgentStateStore(dataClient, space.getId());
    }

    @AfterAll
    static void tearDown() {
        if (store != null) store.close();
        if (registry != null) registry.cleanupAll();
        if (controlClient != null) controlClient.close();
    }

    // 1. Single state roundtrip
    @Test @Order(1)
    @DisplayName("save + get single state roundtrip")
    void testSingleStateRoundtrip() {
        TestState original = new TestState("hello-e2e", 42);
        store.save("e2e-user", "e2e-session", "test_key", original);

        Optional<TestState> retrieved = store.get("e2e-user", "e2e-session", "test_key", TestState.class);
        assertTrue(retrieved.isPresent(), "get should return the saved state");
        assertEquals("hello-e2e", retrieved.get().name);
        assertEquals(42, retrieved.get().value);
    }

    // 2. List state roundtrip
    @Test @Order(2)
    @DisplayName("save + getList roundtrip preserves order")
    void testListStateRoundtrip() {
        List<TestState> original = List.of(
                new TestState("item-a", 1),
                new TestState("item-b", 2),
                new TestState("item-c", 3));
        store.save("e2e-user", "e2e-session", "list_key", original);

        List<TestState> retrieved = store.getList("e2e-user", "e2e-session", "list_key", TestState.class);
        assertEquals(3, retrieved.size());
        assertEquals("item-a", retrieved.get(0).name);
        assertEquals("item-b", retrieved.get(1).name);
        assertEquals("item-c", retrieved.get(2).name);
    }

    // 3. List replacement semantics
    @Test @Order(3)
    @DisplayName("save list replaces previous list (full replacement)")
    void testListReplacement() {
        store.save("e2e-user", "e2e-session", "replace_key",
                List.of(new TestState("old-1", 10), new TestState("old-2", 20)));
        store.save("e2e-user", "e2e-session", "replace_key",
                List.of(new TestState("new-1", 99)));

        List<TestState> retrieved = store.getList("e2e-user", "e2e-session", "replace_key", TestState.class);
        assertEquals(1, retrieved.size(), "Should only return the latest list");
        assertEquals("new-1", retrieved.get(0).name);
        assertEquals(99, retrieved.get(0).value);
    }

    // 4. exists returns true after save
    @Test @Order(4)
    @DisplayName("exists returns true for sessions that have been saved")
    void testExists() {
        store.save("e2e-user", "exists-session", "k", new TestState("val", 0));
        assertTrue(store.exists("e2e-user", "exists-session"));
        assertFalse(store.exists("e2e-user", "nonexistent-session"));
    }

    // 5. Multiple users isolated
    @Test @Order(5)
    @DisplayName("different users with same sessionId are isolated")
    void testUserIsolation() {
        store.save("alice", "shared-session", "key", new TestState("alice-data", 1));
        store.save("bob", "shared-session", "key", new TestState("bob-data", 2));

        TestState aliceState = store.get("alice", "shared-session", "key", TestState.class).orElse(null);
        TestState bobState = store.get("bob", "shared-session", "key", TestState.class).orElse(null);

        assertNotNull(aliceState);
        assertNotNull(bobState);
        assertEquals("alice-data", aliceState.name);
        assertEquals("bob-data", bobState.name);
    }

    // 6. get returns empty for nonexistent key
    @Test @Order(6)
    @DisplayName("get returns empty for nonexistent key")
    void testGetNonexistent() {
        Optional<TestState> result = store.get("e2e-user", "no-such-session", "no-such-key", TestState.class);
        assertTrue(result.isEmpty());
    }

    // 7. listSessionIds
    @Test @Order(7)
    @DisplayName("listSessionIds returns all sessions for a user")
    void testListSessionIds() {
        store.save("list-user", "s1", "k", new TestState("1", 1));
        store.save("list-user", "s2", "k", new TestState("2", 2));

        Set<String> ids = store.listSessionIds("list-user");
        assertTrue(ids.contains("s1"));
        assertTrue(ids.contains("s2"));
    }

    // Jackson-serializable test state
    public static class TestState implements State {
        public String name;
        public int value;

        public TestState() {}
        public TestState(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }
}
