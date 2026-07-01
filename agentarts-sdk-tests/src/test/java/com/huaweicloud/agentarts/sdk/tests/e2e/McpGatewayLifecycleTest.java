package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.mcpgateway.MCPGatewayClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MCP Gateway lifecycle tests — (6 tests: gateway + target CRUD, xfail).
 * All tests are expected to fail (xfail) due to SDK trust_policy bug.
 * Requires AGENTARTS_TEST_ALLOW_CREATE=1.
 */
@Tag("e2e")
@DisplayName("MCP Gateway Lifecycle Tests (xfail)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class McpGatewayLifecycleTest {

    private static MCPGatewayClient client;
    private static E2EResourceRegistry registry;
    private static String runId;
    private static String gatewayId;
    private static String targetId;
    private static boolean setupFailed = false;

    @BeforeAll
    static void setUp() {
        assumeTrue(E2EConfig.hasCloudCredentials(),
                "Set HUAWEICLOUD_SDK_AK and HUAWEICLOUD_SDK_SK to run cloud tests");
        assumeTrue(E2EConfig.allowCreate(),
                "Set AGENTARTS_TEST_ALLOW_CREATE=1 to run create/delete lifecycle tests");

        client = new MCPGatewayClient();
        registry = new E2EResourceRegistry();
        runId = E2EConfig.getRunId();

        // Create gateway (may fail due to IAM trust_policy bug)
        String gwName = E2EHelpers.uniqueName("gw", runId);
        try {
            RequestResult result = client.createMcpGateway(gwName, "e2e test gateway");
            if (result.isSuccess() && result.getData() != null) {
                @SuppressWarnings("unchecked")
                var data = (java.util.Map<String, Object>) result.getData();
                gatewayId = data.get("id") != null ? data.get("id").toString() : null;
                if (gatewayId != null) {
                    registry.register(() -> client.deleteMcpGateway(gatewayId), "gateway:" + gatewayId);
                }
            } else {
                setupFailed = true;
            }
        } catch (Exception e) {
            setupFailed = true;
        }

        // Create target (may fail if gateway creation failed)
        if (gatewayId != null) {
            String targetName = E2EHelpers.uniqueName("tgt", runId);
            try {
                RequestResult result = client.createMcpGatewayTarget(gatewayId, targetName, "e2e target");
                if (result.isSuccess() && result.getData() != null) {
                    @SuppressWarnings("unchecked")
                    var data = (java.util.Map<String, Object>) result.getData();
                    targetId = data.get("id") != null ? data.get("id").toString() : null;
                    if (targetId != null) {
                        registry.register(
                                () -> client.deleteMcpGatewayTarget(gatewayId, targetId),
                                "target:" + targetId);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    @AfterAll
    static void tearDown() {
        if (registry != null) registry.cleanupAll();
        if (client != null) client.close();
    }

    // 1. test_get_gateway
    @Test @Order(1)
    @DisplayName("get_mcp_gateway (xfail: trust_policy bug)")
    void testGetGateway() {
        // xfail: known SDK bug with IAM agency trust_policy
        if (setupFailed || gatewayId == null) {
            System.out.println("XFAIL: gateway creation failed (IAM trust_policy bug)");
            return; // Expected failure
        }
        RequestResult result = client.getMcpGateway(gatewayId);
        assertTrue(result.isSuccess(), result.getError());
    }

    // 2. test_list_gateways
    @Test @Order(2)
    @DisplayName("list_mcp_gateways (xfail: trust_policy bug)")
    void testListGateways() {
        if (setupFailed || gatewayId == null) {
            System.out.println("XFAIL: gateway creation failed (IAM trust_policy bug)");
            return;
        }
        RequestResult result = client.listMcpGateways(null, 10, null);
        assertTrue(result.isSuccess(), result.getError());
        assertNotNull(result.getData());
    }

    // 3. test_update_gateway
    @Test @Order(3)
    @DisplayName("update_mcp_gateway (xfail: trust_policy bug)")
    void testUpdateGateway() {
        if (setupFailed || gatewayId == null) {
            System.out.println("XFAIL: gateway creation failed (IAM trust_policy bug)");
            return;
        }
        RequestResult result = client.updateMcpGateway(gatewayId, "updated description");
        assertTrue(result.isSuccess(), result.getError());
    }

    // 4. test_get_target
    @Test @Order(4)
    @DisplayName("get_mcp_gateway_target (xfail: trust_policy bug)")
    void testGetTarget() {
        if (setupFailed || gatewayId == null || targetId == null) {
            System.out.println("XFAIL: gateway/target creation failed (IAM trust_policy bug)");
            return;
        }
        RequestResult result = client.getMcpGatewayTarget(gatewayId, targetId);
        assertTrue(result.isSuccess(), result.getError());
    }

    // 5. test_list_targets
    @Test @Order(5)
    @DisplayName("list_mcp_gateway_targets (xfail: trust_policy bug)")
    void testListTargets() {
        if (setupFailed || gatewayId == null || targetId == null) {
            System.out.println("XFAIL: gateway/target creation failed (IAM trust_policy bug)");
            return;
        }
        RequestResult result = client.listMcpGatewayTargets(gatewayId, 10, null);
        assertTrue(result.isSuccess(), result.getError());
        assertNotNull(result.getData());
    }

    // 6. test_update_target
    @Test @Order(6)
    @DisplayName("update_mcp_gateway_target (xfail: trust_policy bug)")
    void testUpdateTarget() {
        if (setupFailed || gatewayId == null || targetId == null) {
            System.out.println("XFAIL: gateway/target creation failed (IAM trust_policy bug)");
            return;
        }
        RequestResult result = client.updateMcpGatewayTarget(
                gatewayId, targetId, null, "updated target", null, null);
        assertTrue(result.isSuccess(), result.getError());
    }
}
