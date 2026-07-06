package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import com.huaweicloud.agentarts.toolkit.AgentArtsCli;
import com.huaweicloud.agentarts.toolkit.operations.InitOperation;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Local CLI e2e tests — mirrors {@code tests/integration/toolkit/test_cli_local.py}.
 *
 * <p>Default tier — no credentials, no Docker, no cloud. Invokes the picocli
 * {@link AgentArtsCli} in-process for {@code init} (asserting on generated
 * files and non-zero exit on validation failure) and drives the {@code dev}
 * subcommand (backed by {@link com.huaweicloud.agentarts.toolkit.operations.DevOperation})
 * to assert {@code /ping} and {@code POST /invocations} succeed.</p>
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
     *  must exit non-zero and not create a project. The Java {@code InitCommand}
     *  lowercases the name then validates against {@code [a-z0-9-]+}. */
    @Test
    @DisplayName("CLI init with invalid name does not create a project")
    void test_init_invalid_name_fails(@TempDir Path tmp) {
        int exitCode = cli.execute("init",
                "--name", "Bad_Name!",
                "--template", "basic",
                "--region", "cn-southwest-2",
                "--path", tmp.toString());
        // InitCommand now returns a non-zero exit code on validation failure.
        assertNotEquals(0, exitCode, "init with an invalid name should exit non-zero");
        assertFalse(Files.isDirectory(tmp.resolve("Bad_Name!")),
                "invalid-name project should not be created");
        // The lowercased+validated form must also be absent
        assertFalse(Files.isDirectory(tmp.resolve("bad_name!")),
                "invalid-name project (lowercased) should not be created");
    }

    // ---------------------------------------------------------------
    // dev (blocking server — driven via the `dev` CLI subcommand)
    // ---------------------------------------------------------------

    /** Mirrors {@code test_dev_server_serves_ping_and_invocations}.
     *
     * <p>Drives the {@code dev} CLI subcommand in a background thread (which runs
     * {@link com.huaweicloud.agentarts.toolkit.operations.DevOperation#runDevServer}),
     * polls {@code /ping} on the bound port, POSTs {@code /invocations}, asserts the
     * echo, then stops the dev server by interrupting the thread.</p>
     */
    @Test
    @DisplayName("dev server serves /ping and POST /invocations")
    void test_dev_server_serves_ping_and_invocations(@TempDir Path tmp) throws Exception {
        // 1. Scaffold a basic project so .agentarts_config.yaml exists for DevOperation.
        InitOperation.initProject("basic", "myagent", tmp.toString(),
                "cn-southwest-2", null, null);
        assertTrue(Files.isRegularFile(tmp.resolve("myagent/pom.xml")));

        // 2. Launch the dev subcommand in a background thread. DevOperation loads the
        //    config, falls back to a default echo entrypoint (the scaffolded class is
        //    not compiled onto the test classpath), binds to an ephemeral port, and
        //    prints "DEV_SERVER_LISTENING on port N" to stdout. picocli only redirects
        //    its own writer output, so capture System.out directly.
        CommandLine devCli = new CommandLine(new AgentArtsCli());
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
        PrintStream capturedOut = new PrintStream(outBuf, true, StandardCharsets.UTF_8);
        PrintStream capturedErr = new PrintStream(errBuf, true, StandardCharsets.UTF_8);
        PrintStream origOut = System.out;
        PrintStream origErr = System.err;
        System.setOut(capturedOut);
        System.setErr(capturedErr);

        Thread devThread = new Thread(() -> devCli.execute("dev",
                "--port", "0",
                "--host", "127.0.0.1",
                "--path", tmp.resolve("myagent").toString()));
        devThread.setDaemon(true);
        devThread.start();

        try {
            // 3. Discover the bound port from stdout.
            int port = -1;
            String prefix = "DEV_SERVER_LISTENING on port ";
            for (int i = 0; i < 80 && port < 0; i++) {
                String soFar = outBuf.toString(StandardCharsets.UTF_8);
                int idx = soFar.indexOf(prefix);
                if (idx >= 0) {
                    String rest = soFar.substring(idx + prefix.length()).trim();
                    try { port = Integer.parseInt(rest.split("\\s+")[0]); }
                    catch (NumberFormatException ignored) { }
                }
                if (port < 0) Thread.sleep(250);
            }
            capturedOut.flush();
            capturedErr.flush();
            String outStr = outBuf.toString(StandardCharsets.UTF_8);
            String errStr = errBuf.toString(StandardCharsets.UTF_8);
            assertTrue(port > 0, "dev server did not announce port. stdout=" + outStr + " err=" + errStr);

            // 4. Poll /ping until healthy.
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

            // 5. POST /invocations and assert the echo.
            HttpURLConnection conn = (HttpURLConnection)
                    new URL("http://127.0.0.1:" + port + "/invocations").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            String body = "{\"message\":\"hello from dev e2e\"}";
            conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            assertEquals(200, conn.getResponseCode(), "POST /invocations should return 200");
            JsonNode resp = JsonUtils.MAPPER.readTree(
                    new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            assertTrue(resp.has("response"), "response body should contain 'response'");
            assertEquals("hello from dev e2e", resp.get("response").asText(),
                    "dev server should echo the message under 'response'");
            conn.disconnect();
        } finally {
            // 6. Stop the dev server and restore streams.
            devThread.interrupt();
            devThread.join(5000);
            System.setOut(origOut);
            System.setErr(origErr);
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
