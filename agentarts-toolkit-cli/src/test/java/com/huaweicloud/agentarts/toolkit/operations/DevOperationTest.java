package com.huaweicloud.agentarts.toolkit.operations;

import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevOperationTest {

    @Test
    void resolvesExplicitAndProjectConfigurationFiles(@TempDir Path tempDir) throws Exception {
        Path explicit = Files.writeString(tempDir.resolve("explicit.yaml"), "agents: {}\n");
        assertEquals(explicit.toFile(),
                DevOperation.resolveConfigFile(explicit.toString(), null));
        assertNull(DevOperation.resolveConfigFile(tempDir.resolve("missing").toString(), null));

        Path projectConfig = Files.writeString(tempDir.resolve(".agentarts_config.yaml"),
                "agents: {}\n");
        assertEquals(projectConfig.toFile(),
                DevOperation.resolveConfigFile(null, tempDir.toString()));
    }

    @Test
    void validatesNetworkAndProjectInputs(@TempDir Path tempDir) {
        DevOperation.validateAddress(0, "127.0.0.1");
        DevOperation.validateAddress(65_535, "localhost");
        assertThrows(IllegalArgumentException.class,
                () -> DevOperation.validateAddress(-1, "localhost"));
        assertThrows(IllegalArgumentException.class,
                () -> DevOperation.validateAddress(65_536, "localhost"));
        assertThrows(IllegalArgumentException.class,
                () -> DevOperation.validateAddress(8080, null));
        assertThrows(IllegalArgumentException.class,
                () -> DevOperation.validateAddress(8080, " "));
        assertEquals(tempDir.toAbsolutePath().normalize(),
                DevOperation.resolveProjectPath(tempDir.toString()));
        assertThrows(IllegalArgumentException.class,
                () -> DevOperation.resolveProjectPath(tempDir.resolve("missing").toString()));
        assertThrows(IllegalArgumentException.class,
                () -> DevServerLauncher.main(new String[0]));
        assertThrows(IllegalArgumentException.class,
                () -> DevServerLauncher.main(new String[] {
                        "localhost", "-1", "entrypoint", tempDir.toString()}));
        DevOperation.terminateTree(null);
    }

    @Test
    void readsOnlyAValidDefaultAgentEntrypoint(@TempDir Path tempDir) throws Exception {
        File valid = Files.writeString(tempDir.resolve("valid.yaml"), """
                default_agent: sample
                agents:
                  sample:
                    base:
                      entrypoint: com.example.Agent
                """).toFile();
        assertEquals("com.example.Agent", DevOperation.resolveEntrypoint(valid));
        assertNull(DevOperation.resolveEntrypoint(null));

        File implicitDefault = Files.writeString(tempDir.resolve("implicit-default.yaml"), """
                agents:
                  sample:
                    base:
                      entrypoint: com.example.Implicit
                """).toFile();
        assertEquals("com.example.Implicit", DevOperation.resolveEntrypoint(implicitDefault));
        File malformed = Files.writeString(tempDir.resolve("malformed.yaml"), "agents: [\n")
                .toFile();
        assertThrows(IllegalArgumentException.class,
                () -> DevOperation.resolveEntrypoint(malformed));
    }

    @Test
    void loadsOnlyValidPublicFactories() {
        DevOperation.LoadedApp loaded = DevOperation.loadApp(
                ValidEntrypoint.class.getName(), null);
        try {
            assertSame(ValidEntrypoint.APP, loaded.app());
        } finally {
            loaded.app().stop();
            DevOperation.closeLoader(loaded.loader());
        }
        assertThrows(IllegalArgumentException.class,
                () -> DevOperation.loadApp(NoFactory.class.getName(), null));
        assertThrows(IllegalArgumentException.class,
                () -> DevOperation.loadApp("not.present.Entrypoint", null));
        assertThrows(IllegalStateException.class,
                () -> DevOperation.loadApp(NullEntrypoint.class.getName(), null));
        assertThrows(IllegalArgumentException.class,
                () -> DevOperation.loadApp(FailingEntrypoint.class.getName(), null));
        assertThrows(NullPointerException.class,
                () -> DevOperation.loadApp(null, null));
    }

    @Test
    void validatesAndMergesEnvironmentWithoutExposingValues(@TempDir Path tempDir)
            throws Exception {
        File config = Files.writeString(tempDir.resolve("config.yaml"), """
                default_agent: sample
                agents:
                  sample:
                    base:
                      entrypoint: com.example.Agent
                    runtime:
                      environment_variables:
                        - key: SERVICE_TOKEN
                          value: from-config
                        - key: EMPTY_VALUE
                          value:
                """).toFile();

        assertEquals(Map.of("SERVICE_TOKEN", "from-config", "EMPTY_VALUE", ""),
                DevOperation.resolveConfiguredEnvironment(config));
        assertEquals(Map.of("SERVICE_TOKEN", "from-cli", "PLAIN", "value=with=equals"),
                DevOperation.parseEnvironment(new String[] {
                        "SERVICE_TOKEN=from-cli", "PLAIN=value=with=equals"}));
        assertTrue(DevOperation.parseEnvironment(null).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> DevOperation.parseEnvironment(new String[] {null}));
        assertThrows(IllegalArgumentException.class,
                () -> DevOperation.parseEnvironment(new String[] {"MISSING_SEPARATOR"}));
        assertThrows(IllegalArgumentException.class,
                () -> DevOperation.parseEnvironment(new String[] {"BAD-NAME=value"}));

        File mapConfig = Files.writeString(tempDir.resolve("map-config.yaml"), """
                agents:
                  sample:
                    base:
                      entrypoint: com.example.Agent
                    runtime:
                      environment_variables:
                        FIRST: one
                        SECOND: 2
                """).toFile();
        assertEquals(Map.of("FIRST", "one", "SECOND", "2"),
                DevOperation.resolveConfiguredEnvironment(mapConfig));

        File noRuntime = Files.writeString(tempDir.resolve("no-runtime.yaml"), """
                agents:
                  sample:
                    base:
                      entrypoint: com.example.Agent
                """).toFile();
        assertTrue(DevOperation.resolveConfiguredEnvironment(noRuntime).isEmpty());
    }

    @Test
    void snapshotsSourceArtifactAndConfigChanges(@TempDir Path tempDir) throws Exception {
        Path source = Files.createDirectories(tempDir.resolve("src/main/java"))
                .resolve("Agent.java");
        Path classes = Files.createDirectories(tempDir.resolve("target/classes"))
                .resolve("Agent.class");
        Path config = Files.writeString(tempDir.resolve(".agentarts_config.yaml"), "a: 1\n");
        Files.writeString(source, "class Agent {}\n");
        Files.write(classes, new byte[] {1});
        DevOperation.ProjectState initial = DevOperation.snapshot(tempDir, config);

        Files.writeString(source, "class Agent { int value; }\n");
        DevOperation.ProjectState sourceChanged = DevOperation.snapshot(tempDir, config);
        assertTrue(sourceChanged.sources() != initial.sources());

        Files.write(classes, new byte[] {1, 2});
        DevOperation.ProjectState artifactChanged = DevOperation.snapshot(tempDir, config);
        assertTrue(artifactChanged.artifacts() != sourceChanged.artifacts());

        Files.writeString(config, "a: 22\n");
        DevOperation.ProjectState configChanged = DevOperation.snapshot(tempDir, config);
        assertTrue(configChanged.config() != artifactChanged.config());
    }

    @Test
    void childLauncherReceivesRealEnvironmentAndServesTheEntrypoint(@TempDir Path tempDir)
            throws Exception {
        int port;
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        Path launcherClasspath = DevOperation.buildLauncherClasspath(
                Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize());
        String executable = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name", "").toLowerCase().contains("win")
                        ? "java.exe" : "java").toString();
        ProcessBuilder builder = new ProcessBuilder(
                executable, "-cp", launcherClasspath.toString(),
                DevServerLauncher.class.getName(), "127.0.0.1", Integer.toString(port),
                EnvironmentEntrypoint.class.getName(), tempDir.toString())
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD);
        builder.environment().put("AGENTARTS_DEV_TEST_VALUE", "child-only");
        Process child = builder.start();
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(1)).build();
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create("http://127.0.0.1:" + port + "/invocations"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();
            HttpResponse<String> response = null;
            long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
            while (response == null && System.nanoTime() < deadline) {
                try {
                    response = client.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (IOException e) {
                    if (!child.isAlive()) {
                        break;
                    }
                    Thread.sleep(100);
                }
            }
            assertNotNull(response,
                    () -> "Child did not listen; alive=" + child.isAlive());
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("child-only"));
        } finally {
            DevOperation.terminateTree(child);
        }
    }

    @Test
    void buildsBoundedLauncherClasspathAndTerminatesChildProcesses(@TempDir Path tempDir)
            throws Exception {
        Path target = Files.createDirectories(tempDir.resolve("target"));
        Files.createDirectories(target.resolve("classes"));
        Path dependency = Files.write(tempDir.resolve("runtime-dependency.jar"), new byte[] {1});
        Files.writeString(target.resolve("agentarts-dev-classpath.txt"),
                dependency.toAbsolutePath().toString());

        Path launcher = DevOperation.buildLauncherClasspath(tempDir);
        try (JarFile jar = new JarFile(launcher.toFile())) {
            String classPath = jar.getManifest().getMainAttributes()
                    .getValue(Attributes.Name.CLASS_PATH);
            assertTrue(classPath.contains(dependency.toUri().toASCIIString()));
        }

        String executable = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name", "").toLowerCase().contains("win")
                        ? "java.exe" : "java").toString();
        String testClasses = Path.of(ProcessRunnerTest.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toString();
        Process child = new ProcessBuilder(executable, "-cp", testClasses,
                ProcessRunnerTest.ChildProcess.class.getName(), "sleep")
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
        assertTrue(child.isAlive());
        DevOperation.terminateTree(child);
        assertTrue(child.waitFor(5, TimeUnit.SECONDS));
        assertFalse(child.isAlive());
    }

    @Test
    void buildsAProjectClassLoaderFromClassesAndDependencyJars(@TempDir Path tempDir)
            throws Exception {
        assertSame(DevOperation.class.getClassLoader(),
                DevOperation.buildProjectClassLoader(tempDir.toString()));

        Files.createDirectories(tempDir.resolve("target/classes"));
        Files.createDirectories(tempDir.resolve("target/dependency"));
        Files.createFile(tempDir.resolve("target/dependency/dependency.jar"));
        ClassLoader loader = DevOperation.buildProjectClassLoader(tempDir.toString());
        assertTrue(loader instanceof URLClassLoader);
        URLClassLoader urls = (URLClassLoader) loader;
        try {
            assertEquals(2, urls.getURLs().length);
        } finally {
            urls.close();
        }
    }

    public static final class ValidEntrypoint {
        static final AgentArtsRuntimeApp APP = new AgentArtsRuntimeApp();

        public static AgentArtsRuntimeApp createApp() {
            return APP;
        }
    }

    public static final class NoFactory {
        public static String createApp() {
            return "wrong type";
        }
    }

    public static final class EnvironmentEntrypoint {
        public static AgentArtsRuntimeApp createApp() {
            AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
            app.setEntrypoint(payload -> Map.of(
                    "value", System.getenv("AGENTARTS_DEV_TEST_VALUE")));
            return app;
        }
    }

    public static final class NullEntrypoint {
        public static AgentArtsRuntimeApp createApp() {
            return null;
        }
    }

    public static final class FailingEntrypoint {
        public static AgentArtsRuntimeApp createApp() {
            throw new IllegalStateException("expected test failure");
        }
    }
}
