package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.sdk.memory.model.SpaceInfo;
import com.huaweicloud.agentarts.sdk.memory.model.SpaceListResponse;
import com.huaweicloud.agentarts.toolkit.AgentArtsCli;
import org.junit.jupiter.api.*;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Memory CLI e2e tests — mirrors {@code tests/integration/toolkit/test_cli_memory.py}.
 *
 * <p>The read-only {@code memory list} runs in the default tier; the
 * {@code memory create → list → get → update → delete} lifecycle requires
 * {@code AGENTARTS_TEST_ALLOW_CREATE=1}. The region is passed explicitly to
 * match the account under test.
 *
 * <p><b>Gap:</b> the Java {@code MemoryCommand} subcommands are currently
 * parse-only stubs — their {@code run()} prints a message but does not invoke
 * {@link MemoryClient}. These tests therefore exercise the picocli command path
 * (verifying the CLI surface parses and returns 0) <em>and</em> drive the SDK
 * client directly to mirror the Python test's actual cloud side-effect. Once
 * the CLI command wires to the SDK client, the SDK calls below should be
 * replaced with the CLI path exclusively.</p>
 */
@Tag("e2e")
@DisplayName("CLI Memory E2E Tests")
class CliMemoryE2ETest {

    private CommandLine cli;
    private MemoryClient controlClient;
    private E2EResourceRegistry registry;
    private String runId;
    private String region;

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

    /** Read-only {@code memory list --limit 1} — no resources created (default
     *  tier). Mirrors {@code test_cli_memory_list_readonly}. */
    @Test
    @DisplayName("CLI memory list --limit 1 succeeds")
    void test_cli_memory_list_readonly() {
        // 1. Exercise the picocli command path (verifies CLI surface parses + exit 0)
        int exitCode = cli.execute("memory", "list", "--limit", "1", "--region", region);
        assertEquals(0, exitCode, "CLI memory list should exit 0");

        // 2. Verify the actual cloud list succeeds (mirrors Python's intent that
        //    the CLI lists real spaces). TODO: remove once CLI command wires to SDK.
        SpaceListResponse result = controlClient.listSpaces(1, 0);
        assertNotNull(result, "listSpaces should return a response");
        assertNotNull(result.getItems(), "items should be a list");
    }

    /** {@code memory create → list → get → update → delete} through the CLI
     *  with a unique space name. Mirrors {@code test_cli_memory_lifecycle}. */
    @Test
    @DisplayName("CLI memory create→list→get→update→delete lifecycle")
    void test_cli_memory_lifecycle() {
        assumeTrue(E2EConfig.allowCreate(),
                "Set AGENTARTS_TEST_ALLOW_CREATE=1 to run create/delete lifecycle tests");

        String name = E2EHelpers.uniqueName("cli-space", runId);

        // 1. Exercise the picocli create command path (verifies CLI surface parses + exit 0)
        int createExit = cli.execute("memory", "create", name,
                "--strategies", "semantic", "--region", region, "--output", "json");
        assertEquals(0, createExit, "CLI memory create should exit 0");

        // 2. Actually create the space via the SDK client so a real resource exists
        //    to clean up and to mirror Python's cloud side-effect (the CLI command is
        //    currently a stub). TODO: replace with the CLI path once MemoryCommand
        //    wires to MemoryClient.
        SpaceInfo space = controlClient.createSpace(name, 168, "aa-it");
        assertNotNull(space, "createSpace should return a space");
        assertNotNull(space.getId(), "created space should have an id");
        String spaceId = space.getId();
        registry.register(() -> controlClient.deleteSpace(spaceId),
                "cli-space:" + spaceId);

        // 3. list / get / update / delete through the CLI (parse + exit 0 each)
        assertEquals(0, cli.execute("memory", "list", "--limit", "1", "--region", region),
                "CLI memory list should exit 0");
        assertEquals(0, cli.execute("memory", "get", spaceId, "--region", region),
                "CLI memory get should exit 0");
        assertEquals(0, cli.execute("memory", "update", spaceId,
                "--description", "updated by cli", "--region", region),
                "CLI memory update should exit 0");

        // 4. Actually update via the SDK client to verify the cloud side-effect
        //    (mirrors Python asserting the update round-trip). TODO: CLI path.
        SpaceInfo updated = controlClient.updateSpace(spaceId, null, "updated by cli", null);
        assertNotNull(updated, "updateSpace should return a space");
        assertEquals(spaceId, updated.getId());

        // 5. delete through the CLI + actually delete via the SDK client so the
        //    resource is reclaimed. TODO: replace with the CLI path.
        assertEquals(0, cli.execute("memory", "delete", spaceId,
                "--force", "--region", region),
                "CLI memory delete should exit 0");
        controlClient.deleteSpace(spaceId);
        // Resource already registered for cleanup; the explicit delete above is
        // idempotent-best-effort (registry will swallow the follow-up not-found).
    }
}
