package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import com.huaweicloud.agentarts.toolkit.AgentArtsCli;
import com.huaweicloud.agentarts.toolkit.operations.InitOperation;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Local CLI e2e tests — mirrors {@code tests/integration/toolkit/test_cli_local.py}.
 *
 * <p>Default tier — no credentials, no Docker, no cloud. Invokes the picocli
 * {@link AgentArtsCli} in-process for {@code init} (asserting on generated
 * files) and drives the runtime app directly for the {@code dev} server
 * equivalent (the Java {@code DevOperation} is currently a stub — see TODO on
 * {@link #test_dev_server_serves_ping_and_invocations}).</p>
 */
@Tag("e2e")
@DisplayName("CLI Local E2E Tests")
class CliLocalE2ETest {

    private CommandLine cli;

    @BeforeEach
    void setUp() {
        cli = new CommandLine(new AgentArtsCli());
        cli.setOut(new PrintWriter(new StringWriter()));
        cli.setErr(new PrintWriter(new StringWriter()));
    }

    // ---------------------------------------------------------------
    // init
    // ---------------------------------------------------------------

    /** Mirrors {@code test_init_path_option}: {@code init --path <target>}
     *  creates the project in the target dir (not cwd). */
    @Test
    @DisplayName("CLI init --path creates project in the target directory")
    void test_init_path_option(@TempDir Path tmp) throws Exception {
        Path target = tmp.resolve("sub");
        Files.createDirectories(target);

        int exitCode = cli.execute("init",
                "--name", "myagent",
                "--template", "basic",
                "--region", "cn-southwest-2",
                "--path", target.toString());
        assertEquals(0, exitCode, "CLI init should exit 0");

        Path project = target.resolve("myagent");
        assertTrue(Files.isDirectory(project), "project directory should exist");
        assertTrue(Files.isRegularFile(project.resolve("pom.xml")), "pom.xml missing");
        assertTrue(Files.isRegularFile(project.resolve(".agentarts_config.yaml")),
                ".agentarts_config.yaml missing");
        assertTrue(Files.isRegularFile(project.resolve("Dockerfile")), "Dockerfile missing");
        assertTrue(Files.isRegularFile(
                project.resolve("src/main/java/com/example/Agent.java")), "Agent.java missing");
    }

    /** Mirrors {@code test_init_invalid_name_fails}: a name with invalid chars
     *  must not create a project. The Java {@code InitCommand} lowercases the
     *  name then validates against {@code [a-z0-9-]+}. */
    @Test
    @DisplayName("CLI init with invalid name does not create a project")
    void test_init_invalid_name_fails(@TempDir Path tmp) {
        int exitCode = cli.execute("init",
                "--name", "Bad_Name!",
                "--template", "basic",
                "--region", "cn-southwest-2",
                "--path", tmp.toString());
        // InitCommand prints an error and returns early (Runnable → exit 0),
        // but no project directory should be created.
        assertEquals(0, exitCode);
        assertFalse(Files.isDirectory(tmp.resolve("Bad_Name!")),
                "invalid-name project should not be created");
        // The lowercased+validated form must also be absent
        assertFalse(Files.isDirectory(tmp.resolve("bad_name!")),
                "invalid-name project (lowercased) should not be created");
    }

    // ---------------------------------------------------------------
    // dev (blocking server — driven via AgentArtsRuntimeApp directly)
    // ---------------------------------------------------------------

    /** Mirrors {@code test_dev_server_serves_ping_and_invocations}.
     *
     * <p><b>Gap:</b> the Java {@code DevOperation.runDevServer} is currently a
     * stub (TODO: instantiate {@link AgentArtsRuntimeApp} from the config
     * entrypoint and run). This test therefore drives
     * {@link AgentArtsRuntimeApp} directly — scaffolding a basic project via
     * {@link InitOperation}, then starting the runtime app with an echo
     * entrypoint on a random port, hitting {@code /ping} and
     * {@code POST /invocations}, and stopping it. Once {@code DevOperation}
     * wires to {@link AgentArtsRuntimeApp}, this test should launch the
     * {@code dev} CLI subcommand in-process instead.</p>
     */
    @Test
    @DisplayName("dev server serves /ping and POST /invocations")
    void test_dev_server_serves_ping_and_invocations(@TempDir Path tmp) throws Exception {
        // 1. Scaffold a basic project (mirrors the Python init step)
        InitOperation.initProject("basic", "myagent", tmp.toString(),
                "cn-southwest-2", null, null);
        assertTrue(Files.isRegularFile(tmp.resolve("myagent/pom.xml")));

        // 2. Start the runtime app directly on a random port (DevOperation is a
        //    stub). app.run(0) starts the Vert.x server asynchronously and returns.
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
        app.setEntrypoint((Map<String, Object> payload) ->
                Map.of("response", payload.getOrDefault("message", "")));
        app.run(0);

        try {
            int port = app.getPort();
            assertTrue(port > 0, "runtime app should bind to a port");

            // 3. Poll /ping until healthy (~10s startup window)
            boolean pingOk = false;
            for (int i = 0; i < 40; i++) {
                try {
                    HttpURLConnection conn = (HttpURLConnection)
                            new URL("http://127.0.0.1:" + port + "/ping").openConnection();
                    conn.setConnectTimeout(1000);
                    conn.setReadTimeout(1000);
                    if (conn.getResponseCode() == 200) {
                        pingOk = true;
                        conn.disconnect();
                        break;
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    Thread.sleep(250);
                }
            }
            assertTrue(pingOk, "dev server did not come up (GET /ping)");

            // 4. POST /invocations
            HttpURLConnection conn = (HttpURLConnection)
                    new URL("http://127.0.0.1:" + port + "/invocations").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            String body = "{\"message\":\"hello from deployed e2e\"}";
            conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            assertEquals(200, conn.getResponseCode(), "POST /invocations should return 200");
            JsonNode resp = JsonUtils.MAPPER.readTree(
                    new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            assertTrue(resp.has("response"), "response body should contain 'response'");
            assertEquals("hello from deployed e2e", resp.get("response").asText());
            conn.disconnect();
        } finally {
            app.stop();
        }
    }

    // ---------------------------------------------------------------
    // Template coverage gap (Python: basic, langgraph, langchain, google-adk)
    // ---------------------------------------------------------------

    /** The Python suite parametrizes init over {@code [basic, langgraph,
     *  langchain, google-adk]}. The Java toolkit only ships {@code basic} and
     *  {@code agentscope} templates (see
     *  {@code agentarts-toolkit-cli/src/main/resources/templates/}). These
     *  three tests document the gap: init with an unsupported template throws
     *  because {@link com.huaweicloud.agentarts.toolkit.template.TemplateManager}
     *  cannot load the missing {@code Agent.java.tpl}. */
    @Test
    @DisplayName("langgraph template is not supported (throws)")
    void test_langgraph_template_not_supported(@TempDir Path tmp) {
        assertThrows(IOException.class, () ->
                InitOperation.initProject("langgraph", "x", tmp.toString(),
                        "cn-southwest-2", null, null));
    }

    @Test
    @DisplayName("langchain template is not supported (throws)")
    void test_langchain_template_not_supported(@TempDir Path tmp) {
        assertThrows(IOException.class, () ->
                InitOperation.initProject("langchain", "x", tmp.toString(),
                        "cn-southwest-2", null, null));
    }

    @Test
    @DisplayName("google-adk template is not supported (throws)")
    void test_google_adk_template_not_supported(@TempDir Path tmp) {
        assertThrows(IOException.class, () ->
                InitOperation.initProject("google-adk", "x", tmp.toString(),
                        "cn-southwest-2", null, null));
    }
}
