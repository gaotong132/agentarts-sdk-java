package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.toolkit.AgentArtsCli;
import com.huaweicloud.agentarts.toolkit.operations.DeployOperation;
import com.huaweicloud.agentarts.toolkit.operations.InitOperation;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CLI runtime e2e against a Docker-deployed agent — mirrors
 * {@code tests/integration/toolkit/test_cli_deployed_runtime.py}.
 *
 * <p>The Python suite uses a session-scoped {@code deployed_runtime_agent}
 * fixture that runs {@code init → config → deploy} (Docker build + SWR push +
 * cloud runtime create) once; the four tests reuse that live agent and
 * {@code destroy} runs as the fixture's session-end teardown.
 *
 * <p><b>Gap:</b> the Java {@link DeployOperation#deployProject} is currently a
 * stub — it prints messages but does not perform Docker build / SWR push /
 * runtime create (see its TODO markers). The {@code invoke} / {@code runtime}
 * CLI subcommands ({@code exec-command}, {@code upload-files},
 * {@code start-session}, {@code stop-session}) are likewise parse-only stubs.
 * This class therefore skips entirely (via {@code assumeTrue(false, ...)} in
 * {@link #setUp()}) until the deploy chain is implemented. The four test
 * skeletons below mirror the Python assertions so the coverage gap is visible
 * and the cases exist for when the deploy chain lands.</p>
 *
 * <p>Gated behind Docker + cloud_credentials + ALLOW_CREATE + RUN_BILLABLE,
 * so it skips by default. SWR org/repo/image would persist (documented
 * residue) once enabled.</p>
 */
@Tag("e2e")
@DisplayName("CLI Deployed Runtime E2E Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CliDeployedRuntimeE2ETest {

    private static CommandLine cli;
    private static E2EResourceRegistry registry;
    private static String runId;
    private static Path projectDir;
    private static String agentName;

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setUp() {
        // Gate the whole class on cloud credentials + create + billable tiers.
        assumeTrue(E2EConfig.hasCloudCredentials(),
                "Set HUAWEICLOUD_SDK_AK and HUAWEICLOUD_SDK_SK to run cloud tests");
        assumeTrue(E2EConfig.allowCreate(),
                "Set AGENTARTS_TEST_ALLOW_CREATE=1 to run deploy lifecycle tests");
        assumeTrue(E2EConfig.allowBillable(),
                "Set AGENTARTS_TEST_RUN_BILLABLE=1 to run billable deploy/invoke tests");

        // Gate on Docker availability. The Java deploy chain is not implemented
        // yet (DeployOperation is a stub), so this always skips in CI. Once the
        // chain lands, replace this with a real Docker daemon check (e.g. ping
        // the docker socket / run `docker info`).
        assumeTrue(false,
                "Java deploy chain (Docker build + SWR push + runtime create) is not yet " +
                        "implemented — DeployOperation.deployProject is a stub. Requires a real " +
                        "Docker daemon, SWR credentials, and RUN_BILLABLE=1. Skipped until wired.");

        cli = new CommandLine(new AgentArtsCli());
        cli.setOut(new PrintWriter(new StringWriter()));
        cli.setErr(new PrintWriter(new StringWriter()));
        registry = new E2EResourceRegistry();
        runId = E2EConfig.getRunId();
        agentName = E2EHelpers.uniqueName("cli-rt", runId);
        projectDir = tempDir.resolve(agentName);

        // Mirrors the Python `deployed_runtime_agent` fixture:
        //   init → config → deploy (Docker build + SWR push + cloud runtime create).
        // TODO: once DeployOperation is implemented, drive it via the CLI:
        //   cli.execute("init", "--name", agentName, "--path", tempDir.toString(),
        //               "--region", E2EConfig.getRegion());
        //   cli.execute("config", ...);  // set entrypoint / SWR / runtime
        //   cli.execute("deploy", "--agent", agentName, "--mode", "cloud");
        // and register cleanup:
        //   registry.register(() -> cli.execute("destroy", "--agent", agentName, "-y"),
        //           "runtime-agent:" + agentName);
    }

    @AfterAll
    static void tearDown() {
        if (registry != null) registry.cleanupAll();
    }

    /** Mirrors {@code test_deploy_succeeds}: the fixture's deploy created a
     *  cloud runtime; the config now carries an {@code agent_id}. */
    @Test
    @Order(1)
    @DisplayName("deploy creates a cloud runtime (config carries agent_id)")
    void test_deploy_succeeds() {
        assertNotNull(agentName, "agent name should be set by the fixture");
        // TODO: read .agentarts_config.yaml and assert "agent_id" is present
        // (deploy writes the created agent's id back to config), mirroring:
        //   cfg = (projectDir / ".agentarts_config.yaml").read_text()
        //   assert "agent_id" in cfg
        Path config = projectDir.resolve(".agentarts_config.yaml");
        assertTrue(Files.exists(config),
                "project config should exist after init (deploy chain not wired yet)");
    }

    /** Mirrors {@code test_invoke_deployed_agent}: invoke the deployed agent
     *  via the CLI with a JSON payload. */
    @Test
    @Order(2)
    @DisplayName("invoke deployed agent via CLI")
    void test_invoke_deployed_agent() {
        // TODO: once the invoke chain is wired, drive via CLI:
        //   int exit = cli.execute("invoke", "--agent", agentName, "--mode", "cloud",
        //           "{\"message\": \"hello from deployed e2e\"}");
        //   assertEquals(0, exit);
        // The cwd should be projectDir so the CLI resolves the data-plane endpoint
        // from .agentarts_config.yaml + a control-plane lookup.
        assertNotNull(agentName);
    }

    /** Mirrors {@code test_runtime_session_on_deployed_agent}: core session
     *  lifecycle — start-session → exec-command → stop-session. */
    @Test
    @Order(3)
    @DisplayName("runtime start-session → exec-command → stop-session on deployed agent")
    void test_runtime_session_on_deployed_agent() {
        // TODO: once the runtime subcommands are wired, drive via CLI:
        //   cli.execute("runtime", "start-session", "--agent", agentName);
        //   ... parse session_id from stdout ...
        //   cli.execute("runtime", "exec-command", "--agent", agentName,
        //           "--session", sessionId, "echo aa-it");
        //   cli.execute("runtime", "stop-session", "--agent", agentName,
        //           "--session", sessionId);
        assertNotNull(agentName);
    }

    /** Mirrors {@code test_runtime_file_transfer_on_deployed_agent}: best-effort
     *  file round-trip (upload → download). The upload endpoint may require a
     *  bearer token that an IAM-only agent lacks (401) — in that case the
     *  Python test skips rather than fails. */
    @Test
    @Order(4)
    @DisplayName("runtime upload-files → download-files on deployed agent")
    void test_runtime_file_transfer_on_deployed_agent() {
        // TODO: once the runtime subcommands are wired, drive via CLI:
        //   cli.execute("runtime", "start-session", "--agent", agentName);
        //   ... parse session_id ...
        //   cli.execute("runtime", "upload-files", "--agent", agentName,
        //           "--session", sessionId, "--files", localFile, "--path", "/home/user/");
        //   if 401 → assumeTrue(false, "upload-files needs a bearer token")
        //   cli.execute("runtime", "download-files", "--agent", agentName,
        //           "--session", sessionId, "--path", remoteFile, "--output", outPath);
        //   cli.execute("runtime", "stop-session", ...);
        assertNotNull(agentName);
    }
}
