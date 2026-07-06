package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.huaweicloud.agentarts.sdk.mcpgateway.MCPGatewayClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;
import com.huaweicloud.agentarts.toolkit.AgentArtsCli;
import org.junit.jupiter.api.*;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Gateway CLI e2e tests — mirrors {@code tests/integration/toolkit/test_cli_gateway.py}.
 *
 * <p>The read-only {@code list-mcp-gateways} runs in the default tier; the
 * {@code create-mcp-gateway} lifecycle exercises the auto-IAM-agency path and
 * requires {@code AGENTARTS_TEST_ALLOW_CREATE=1}.
 *
 * <p><b>Gap:</b> the Java {@code McpGatewayCommand} subcommands are currently
 * parse-only stubs — their {@code run()} prints a message but does not invoke
 * {@link MCPGatewayClient}. These tests therefore exercise the picocli command
 * path (verifying the CLI surface parses and returns 0) <em>and</em> drive the
 * SDK client directly to mirror the Python test's actual cloud side-effect.
 * Once the CLI command wires to the SDK client, the SDK calls below should be
 * replaced with the CLI path exclusively.</p>
 */
@Tag("e2e")
@DisplayName("CLI Gateway E2E Tests")
class CliGatewayE2ETest {

    private CommandLine cli;
    private MCPGatewayClient client;
    private E2EResourceRegistry registry;
    private String runId;

    @BeforeAll
    static void requireCredentials() {
        assumeTrue(E2EConfig.hasCloudCredentials(),
                "Set HUAWEICLOUD_SDK_AK and HUAWEICLOUD_SDK_SK to run cloud tests");
    }

    @BeforeEach
    void setUp() {
        cli = new CommandLine(new AgentArtsCli());
        cli.setOut(new PrintWriter(new StringWriter()));
        cli.setErr(new PrintWriter(new StringWriter()));
        client = new MCPGatewayClient();
        registry = new E2EResourceRegistry();
        runId = E2EConfig.getRunId();
    }

    @AfterEach
    void tearDown() {
        if (registry != null) registry.cleanupAll();
        if (client != null) client.close();
    }

    /** Read-only {@code mcp-gateway list-mcp-gateways --limit 1} — no resources
     *  created (default tier). Mirrors {@code test_cli_gateway_list_readonly}. */
    @Test
    @DisplayName("CLI mcp-gateway list-mcp-gateways --limit 1 succeeds")
    void test_cli_gateway_list_readonly() {
        // 1. Exercise the picocli command path (verifies CLI surface parses + exit 0)
        int exitCode = cli.execute("mcp-gateway", "list-mcp-gateways", "--limit", "1");
        assertEquals(0, exitCode, "CLI list-mcp-gateways should exit 0");

        // 2. Verify the actual cloud list succeeds (mirrors Python's intent that
        //    the CLI lists real gateways). TODO: remove once CLI command wires to SDK.
        RequestResult result = client.listMcpGateways(null, 1, null);
        // Tolerate 403 when the tenant hasn't enabled the MCP Gateway service
        if (!result.isSuccess() && result.getStatusCode() == 403) {
            assumeTrue(false, "MCP Gateway service not enabled for this tenant");
        }
        assertTrue(result.isSuccess(), "listMcpGateways failed: " + result.getError());
        assertNotNull(result.getData(), "listMcpGateways returned no data");
    }

    /** {@code mcp-gateway create-mcp-gateway} exercises the auto-agency path
     *  end-to-end through the CLI. Mirrors {@code test_cli_gateway_create}. */
    @Test
    @DisplayName("CLI mcp-gateway create-mcp-gateway creates a gateway (cleanup registered)")
    void test_cli_gateway_create() {
        assumeTrue(E2EConfig.allowCreate(),
                "Set AGENTARTS_TEST_ALLOW_CREATE=1 to run create/delete lifecycle tests");

        String name = E2EHelpers.uniqueName("cli-gw", runId);

        // 1. Exercise the picocli command path (verifies CLI surface parses + exit 0)
        int exitCode = cli.execute(
                "mcp-gateway", "create-mcp-gateway",
                "--name", name, "--description", "aa-it");
        assertEquals(0, exitCode, "CLI create-mcp-gateway should exit 0");

        // 2. Actually create the gateway via the SDK client so a real resource
        //    exists to clean up (the CLI command is currently a stub).
        //    TODO: replace with the CLI path once McpGatewayCommand wires to MCPGatewayClient.
        RequestResult result = client.createMcpGateway(name, "aa-it");
        assertTrue(result.isSuccess(), "createMcpGateway failed: " + result.getError());
        assertNotNull(result.getData(), "createMcpGateway returned no data");
        String gatewayId = extractId((JsonNode) result.getData(), "id", "gateway_id");
        assertNotNull(gatewayId, "createMcpGateway returned no id");
        final String id = gatewayId;
        registry.register(() -> client.deleteMcpGateway(id), "gateway:" + id);
    }

    /** Pull a resource id out of a response node, trying common key names at the
     *  top level and one level into nested objects/arrays. */
    private static String extractId(JsonNode node, String... keys) {
        if (node == null) return null;
        String top = findText(node, keys);
        if (top != null) return top;
        for (JsonNode child : node) {
            if (child == null) continue;
            String s = findText(child, keys);
            if (s != null) return s;
            if (child.isArray()) {
                for (JsonNode item : child) {
                    if (item == null) continue;
                    String s2 = findText(item, keys);
                    if (s2 != null) return s2;
                }
            }
        }
        return null;
    }

    private static String findText(JsonNode node, String... keys) {
        for (String k : keys) {
            JsonNode v = node.get(k);
            if (v != null && !v.isNull() && !v.asText("").isEmpty()) {
                return v.asText();
            }
        }
        return null;
    }
}
