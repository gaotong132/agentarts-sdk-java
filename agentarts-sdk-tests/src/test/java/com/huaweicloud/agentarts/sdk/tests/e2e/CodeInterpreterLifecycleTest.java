package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.tools.CodeInterpreterClient;
import com.huaweicloud.agentarts.sdk.tools.model.CodeInterpreterInfo;
import com.huaweicloud.agentarts.sdk.tools.model.CreateCodeInterpreterRequest;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Code Interpreter lifecycle tests — (3 tests: get, list, update).
 * Requires AGENTARTS_TEST_ALLOW_CREATE=1.
 */
@Tag("e2e")
@DisplayName("Code Interpreter Lifecycle Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CodeInterpreterLifecycleTest {

    private static CodeInterpreterClient client;
    private static E2EResourceRegistry registry;
    private static String runId;
    private static String ciId;
    private static String ciName;

    @BeforeAll
    static void setUp() {
        assumeTrue(E2EConfig.hasCloudCredentials(),
                "Set HUAWEICLOUD_SDK_AK and HUAWEICLOUD_SDK_SK to run cloud tests");
        assumeTrue(E2EConfig.allowCreate(),
                "Set AGENTARTS_TEST_ALLOW_CREATE=1 to run create/delete lifecycle tests");

        client = new CodeInterpreterClient(E2EConfig.getRegion());
        registry = new E2EResourceRegistry();
        runId = E2EConfig.getRunId();

        // Create code interpreter
        ciName = E2EHelpers.uniqueName("ci", runId);
        CodeInterpreterInfo result = client.createCodeInterpreter(
                new CreateCodeInterpreterRequest()
                        .withName(ciName)
                        .withAuthType("API_KEY")
                        .withApiKeyName(ciName + "-ak")
                        .withDescription("e2e test CI"));
        assertNotNull(result);
        ciId = result.getId();
        if (ciId != null) {
            registry.register(() -> client.deleteCodeInterpreter(ciId), "code-interpreter:" + ciId);
        }
    }

    @AfterAll
    static void tearDown() {
        if (registry != null) registry.cleanupAll();
        if (client != null) client.close();
    }

    // 1. test_get_code_interpreter
    @Test @Order(1)
    @DisplayName("get_code_interpreter returns the created CI")
    void testGetCodeInterpreter() {
        assertNotNull(ciId, "Code interpreter ID should be set from setup");
        CodeInterpreterInfo got = client.getCodeInterpreter(ciId);
        assertEquals(ciId, got.getId());
    }

    // 2. test_list_code_interpreters
    @Test @Order(2)
    @DisplayName("list_code_interpreters contains the created CI")
    void testListCodeInterpreters() {
        var result = client.listCodeInterpreters(null, 100, 0);
        assertNotNull(result);
        assertNotNull(result.getItems(), "items should be a list");
        assertTrue(result.getItems() instanceof java.util.List, "items should be a List");
        assertTrue(result.getTotalCount() >= 0, "total_count should be a non-negative int");
        // The created CI should appear in the list (match by id).
        boolean contains = result.getItems().stream()
                .anyMatch(ci -> ciId.equals(ci.getId()));
        assertTrue(contains, "created code interpreter " + ciId + " should appear in list");
    }

    // 3. test_update_code_interpreter
    @Test @Order(3)
    @DisplayName("update_code_interpreter persists the new tags")
    void testUpdateCodeInterpreter() {
        assertNotNull(ciId, "Code interpreter ID should be set from setup");
        java.util.Map<String, String> tagPair = new java.util.LinkedHashMap<>();
        tagPair.put("key", "env");
        tagPair.put("value", "aa-it");
        CodeInterpreterInfo updated = client.updateCodeInterpreter(ciId,
                null, java.util.List.of(tagPair));
        assertNotNull(updated);
        assertEquals(ciId, updated.getId());

        // Re-get and assert the tags include the pair we sent.
        CodeInterpreterInfo refetched = client.getCodeInterpreter(ciId);
        assertNotNull(refetched);
        assertNotNull(refetched.getTags(), "tags should be present on the CI after update");
        boolean hasPair = refetched.getTags().stream()
                .anyMatch(t -> "env".equals(t.get("key")) && "aa-it".equals(t.get("value")));
        assertTrue(hasPair, "re-fetched CI tags should include {env=aa-it}: " + refetched.getTags());
    }
}
