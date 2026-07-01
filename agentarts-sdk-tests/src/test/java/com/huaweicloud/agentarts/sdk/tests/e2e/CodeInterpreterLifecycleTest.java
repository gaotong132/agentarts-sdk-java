package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.tools.CodeInterpreterClient;
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
        Map<String, Object> result = client.createCodeInterpreter(
                ciName, "API_KEY", ciName + "-ak", "e2e test CI",
                null, null, null, null, null);
        assertNotNull(result);
        ciId = (String) result.get("id");
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
        Map<String, Object> got = client.getCodeInterpreter(ciId);
        assertEquals(ciId, got.get("id"));
    }

    // 2. test_list_code_interpreters
    @Test @Order(2)
    @DisplayName("list_code_interpreters returns a dict")
    void testListCodeInterpreters() {
        Map<String, Object> result = client.listCodeInterpreters(null, 10, 0);
        assertNotNull(result);
    }

    // 3. test_update_code_interpreter
    @Test @Order(3)
    @DisplayName("update_code_interpreter succeeds")
    void testUpdateCodeInterpreter() {
        assertNotNull(ciId, "Code interpreter ID should be set from setup");
        Map<String, Object> updated = client.updateCodeInterpreter(ciId,
                null, java.util.List.of(Map.of("key", "env", "value", "aa-it")));
        assertNotNull(updated);
    }
}
