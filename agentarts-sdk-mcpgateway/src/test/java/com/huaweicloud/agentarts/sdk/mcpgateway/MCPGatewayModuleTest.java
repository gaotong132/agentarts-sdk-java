package com.huaweicloud.agentarts.sdk.mcpgateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MCP Gateway module: MCPGatewayClient API parity.
 */
class MCPGatewayModuleTest {

    @Nested
    @DisplayName("Python Parity: MCPGatewayClient API methods")
    class PythonParityTests {

        @Test
        void gatewayCrudMethodsExist() throws Exception {
            // Mirrors Python: create/update/delete/get/list_mcp_gateway
            Class<?> cls = MCPGatewayClient.class;
            assertNotNull(cls.getMethod("createMcpGateway", String.class, String.class, String.class, String.class, String.class));
            assertNotNull(cls.getMethod("createMcpGateway", String.class, String.class));
            assertNotNull(cls.getMethod("updateMcpGateway", String.class, String.class));
            assertNotNull(cls.getMethod("deleteMcpGateway", String.class));
            assertNotNull(cls.getMethod("getMcpGateway", String.class));
            assertNotNull(cls.getMethod("listMcpGateways", String.class, Integer.class, Integer.class));
            assertNotNull(cls.getMethod("listMcpGateways"));
        }

        @Test
        void targetCrudMethodsExist() throws Exception {
            // Mirrors Python: create/update/delete/get/list_mcp_gateway_target
            Class<?> cls = MCPGatewayClient.class;
            assertNotNull(cls.getMethod("createMcpGatewayTarget", String.class, String.class, String.class, Map.class, Map.class));
            assertNotNull(cls.getMethod("createMcpGatewayTarget", String.class, String.class, String.class));
            assertNotNull(cls.getMethod("updateMcpGatewayTarget", String.class, String.class, String.class, String.class, Map.class, Map.class));
            assertNotNull(cls.getMethod("deleteMcpGatewayTarget", String.class, String.class));
            assertNotNull(cls.getMethod("getMcpGatewayTarget", String.class, String.class));
            assertNotNull(cls.getMethod("listMcpGatewayTargets", String.class, Integer.class, Integer.class));
            assertNotNull(cls.getMethod("listMcpGatewayTargets", String.class));
        }

        @Test
        void constructorMatchesPython() throws Exception {
            // Python: __init__(verify_ssl=True)
            Class<?> cls = MCPGatewayClient.class;
            assertNotNull(cls.getConstructor(boolean.class));
            assertNotNull(cls.getConstructor());
        }

        @Test
        void implementsAutoCloseable() {
            assertTrue(AutoCloseable.class.isAssignableFrom(MCPGatewayClient.class));
        }

        @Test
        void methodCountMatchesPython() {
            // Python MCPGatewayClient has 10 public CRUD methods:
            // create/update/delete/get/list gateway + create/update/delete/get/list target
            Class<?> cls = MCPGatewayClient.class;
            int gatewayMethods = 0;
            int targetMethods = 0;
            for (var m : cls.getDeclaredMethods()) {
                String name = m.getName();
                if (name.contains("McpGateway") && !name.contains("Target")) gatewayMethods++;
                if (name.contains("McpGatewayTarget")) targetMethods++;
            }
            // At least 5 gateway + 5 target (may have overloads)
            assertTrue(gatewayMethods >= 5, "Expected ≥5 gateway methods, got " + gatewayMethods);
            assertTrue(targetMethods >= 5, "Expected ≥5 target methods, got " + targetMethods);
        }
    }
}
