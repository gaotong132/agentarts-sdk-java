package com.huaweicloud.agentarts.toolkit.operations;

import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

        File missingDefault = Files.writeString(tempDir.resolve("missing-default.yaml"),
                "agents: {}\n").toFile();
        assertNull(DevOperation.resolveEntrypoint(missingDefault));
        File malformed = Files.writeString(tempDir.resolve("malformed.yaml"), "agents: [\n")
                .toFile();
        assertNull(DevOperation.resolveEntrypoint(malformed));
    }

    @Test
    void loadsValidFactoriesAndFallsBackForInvalidEntrypoints() {
        AgentArtsRuntimeApp valid = DevOperation.buildApp(ValidEntrypoint.class.getName(), null);
        AgentArtsRuntimeApp noFactory = DevOperation.buildApp(NoFactory.class.getName(), null);
        AgentArtsRuntimeApp missing = DevOperation.buildApp("not.present.Entrypoint", null);
        AgentArtsRuntimeApp defaultApp = DevOperation.buildApp(null, null);
        try {
            assertSame(ValidEntrypoint.APP, valid);
            assertNotNull(noFactory);
            assertNotNull(missing);
            assertNotNull(defaultApp);
        } finally {
            valid.stop();
            noFactory.stop();
            missing.stop();
            defaultApp.stop();
        }
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
}
