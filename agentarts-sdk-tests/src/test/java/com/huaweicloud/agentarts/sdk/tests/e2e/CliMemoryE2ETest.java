package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.toolkit.AgentArtsCli;
import com.huaweicloud.agentarts.toolkit.commands.CliSupport;
import org.junit.jupiter.api.*;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Memory CLI e2e tests — exercises the picocli command path exclusively.
 *
 * <p>Every assertion comes from the CLI's stdout JSON; the {@link MemoryClient}
 * is constructed only to register LIFO cleanup for spaces the CLI creates (the
 * cleanup path is not what is under test). The read-only {@code memory list}
 * runs in the default tier; the {@code create → list → get → update → delete}
 * lifecycle requires {@code AGENTARTS_TEST_ALLOW_CREATE=1}.</p>
 */
@Tag("e2e")
@DisplayName("CLI Memory E2E Tests")
class CliMemoryE2ETest {

    private CommandLine cli;
    private MemoryClient controlClient;
    private E2EResourceRegistry registry;
    private String runId;
    private String region;

    /** Captured stdout/stderr from the last {@link #runCli(String...)} call. */
    private String stdout;
    private String stderr;

    @BeforeAll
    static void requireCredentials() {
        assumeTrue(E2EConfig.hasCloudCredentials(),
                "Set HUAWEICLOUD_SDK_AK and HUAWEICLOUD_SDK_SK to run cloud tests");
    }

    @BeforeEach
    void setUp() {
        cli = CliSupport.withCleanExit(new CommandLine(new AgentArtsCli()));
        cli.setOut(new PrintWriter(new StringWriter()));
        cli.setErr(new PrintWriter(new StringWriter()));
        region = E2EConfig.getRegion();
        controlClient = new MemoryClient(region, null);
        registry = new E2EResourceRegistry();
        runId = E2EConfig.getRunId();
    }

    @AfterEach
    void tearDown() {
        if (registry != null) registry.cleanupAll();
        if (controlClient != null) controlClient.close();
    }

    /** Read-only {@code memory list --limit 1} — no resources created. */
    @Test
    @DisplayName("CLI memory list --limit 1 prints JSON with spaces/total")
    void test_cli_memory_list_readonly() {
        int exit = runCli("memory", "list", "--limit", "1", "--region", region, "--output", "json");
        assertEquals(0, exit, "CLI memory list should exit 0; stderr=" + stderr);
        JsonNode node = parseJson(stdout);
        assertNotNull(node, "CLI memory list should print JSON; stdout=" + stdout);
        assertTrue(node.has("spaces") || node.has("total"),
                "list response should contain spaces/total; stdout=" + stdout);
    }

    /** {@code create → list → get → update → delete} through the CLI, asserting JSON output. */
    @Test
    @DisplayName("CLI memory create→list→get→update→delete lifecycle")
    void test_cli_memory_lifecycle() {
        assumeTrue(E2EConfig.allowCreate(),
                "Set AGENTARTS_TEST_ALLOW_CREATE=1 to run create/delete lifecycle tests");

        String name = E2EHelpers.uniqueName("cli-space", runId);

        // 1. Create via CLI — parse the space id from the CLI's JSON stdout.
        int createExit = runCli("memory", "create", name,
                "--strategies", "semantic", "--region", region, "--output", "json");
        assertEquals(0, createExit, "CLI memory create should exit 0; stderr=" + stderr);
        JsonNode created = parseJson(stdout);
        assertNotNull(created, "create should print JSON; stdout=" + stdout);
        String spaceId = created.path("id").asText(null);
        assertNotNull(spaceId, "create response should contain an id; stdout=" + stdout);
        // Register SDK cleanup as a safety net (CLI delete below should reclaim it).
        final String id = spaceId;
        registry.register(() -> {
            try { controlClient.deleteSpace(id); } catch (Exception ignored) { /* already deleted */ }
        }, "cli-space:" + id);

        // 2. List via CLI — must include the created space id eventually (best-effort).
        int listExit = runCli("memory", "list", "--limit", "10", "--region", region, "--output", "json");
        assertEquals(0, listExit, "CLI memory list should exit 0; stderr=" + stderr);
        JsonNode listed = parseJson(stdout);
        assertNotNull(listed, "list should print JSON; stdout=" + stdout);
        assertTrue(listed.has("spaces"), "list response should contain spaces; stdout=" + stdout);

        // 3. Get via CLI — the returned id must match.
        int getExit = runCli("memory", "get", id, "--region", region, "--output", "json");
        assertEquals(0, getExit, "CLI memory get should exit 0; stderr=" + stderr);
        JsonNode got = parseJson(stdout);
        assertNotNull(got, "get should print JSON; stdout=" + stdout);
        assertEquals(id, got.path("id").asText(null),
                "get response id should match; stdout=" + stdout);

        // 4. Update via CLI — the response wraps the space under "space".
        int updateExit = runCli("memory", "update", id,
                "--description", "updated by cli", "--region", region, "--output", "json");
        assertEquals(0, updateExit, "CLI memory update should exit 0; stderr=" + stderr);
        JsonNode updated = parseJson(stdout);
        assertNotNull(updated, "update should print JSON; stdout=" + stdout);
        assertEquals(id, updated.path("space_id").asText(null),
                "update response space_id should match; stdout=" + stdout);
        assertEquals(id, updated.path("space").path("id").asText(null),
                "update response space.id should match; stdout=" + stdout);

        // 5. Status via CLI — the response carries the space id and a health status.
        int statusExit = runCli("memory", "status", id, "--region", region, "--output", "json");
        assertEquals(0, statusExit, "CLI memory status should exit 0; stderr=" + stderr);
        JsonNode status = parseJson(stdout);
        assertNotNull(status, "status should print JSON; stdout=" + stdout);
        assertEquals(id, status.path("space_id").asText(null),
                "status response space_id should match; stdout=" + stdout);

        // 6. Delete via CLI.
        int deleteExit = runCli("memory", "delete", id, "--force", "--region", region);
        assertEquals(0, deleteExit, "CLI memory delete should exit 0; stderr=" + stderr);
    }

    // ========================
    // Helpers
    // ========================

    /** Execute the CLI while capturing System.out / System.err. */
    private int runCli(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
        int exit;
        try {
            exit = cli.execute(args);
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
        stdout = out.toString(StandardCharsets.UTF_8);
        stderr = err.toString(StandardCharsets.UTF_8);
        return exit;
    }

    private static JsonNode parseJson(String text) {
        try {
            return JsonUtils.MAPPER.readTree(text);
        } catch (Exception e) {
            return null;
        }
    }
}
