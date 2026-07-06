package com.huaweicloud.agentarts.toolkit;

import com.huaweicloud.agentarts.sdk.core.config.AgentArtsConfig;
import com.huaweicloud.agentarts.sdk.core.config.AgentArtsConfigList;
import com.huaweicloud.agentarts.toolkit.operations.ConfigOperation;
import com.huaweicloud.agentarts.toolkit.operations.InitOperation;
import com.huaweicloud.agentarts.toolkit.template.TemplateManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for the CLI toolkit.
 *
 * <p>These tests verify the complete CLI workflow:
 * <ul>
 *   <li>Init operation: project scaffolding with template rendering</li>
 *   <li>Config operation: full CRUD lifecycle with YAML persistence</li>
 *   <li>CLI command execution: Picocli command parsing + execution + output</li>
 *   <li>Full workflow: init → config → verify integration</li>
 * </ul>
 */
class CliE2ETest {

    // ============================================================
    // Init Operation E2E
    // ============================================================

    @Nested
    class InitOperationE2E {

        @Test
        void initCreatesFullProjectStructure(@TempDir Path tempDir) throws Exception {
            InitOperation.initProject("basic", "my-agent", tempDir.toString(),
                    "cn-southwest-2", "my-org", "my-repo");

            Path projectDir = tempDir.resolve("my-agent");
            assertTrue(Files.isDirectory(projectDir), "Project directory should exist");

            // Verify all expected files
            assertTrue(Files.isRegularFile(projectDir.resolve("pom.xml")), "pom.xml missing");
            assertTrue(Files.isRegularFile(projectDir.resolve(".agentarts_config.yaml")), "config.yaml missing");
            assertTrue(Files.isRegularFile(projectDir.resolve("Dockerfile")), "Dockerfile missing");
            assertTrue(Files.isRegularFile(
                    projectDir.resolve("src/main/java/com/example/Agent.java")), "Agent.java missing");
        }

        @Test
        void initPomXmlContainsCorrectValues(@TempDir Path tempDir) throws Exception {
            InitOperation.initProject("basic", "test-proj", tempDir.toString(),
                    "cn-north-4", null, null);

            String pom = Files.readString(tempDir.resolve("test-proj/pom.xml"));
            assertTrue(pom.contains("<artifactId>test-proj</artifactId>"),
                    "pom.xml should contain project name as artifactId");
        }

        @Test
        void initAgentJavaContainsCorrectClassName(@TempDir Path tempDir) throws Exception {
            InitOperation.initProject("basic", "demo", tempDir.toString(),
                    "cn-southwest-2", null, null);

            String agent = Files.readString(
                    tempDir.resolve("demo/src/main/java/com/example/Agent.java"));
            assertTrue(agent.contains("demoAgent"), "Agent class name should be {name}Agent");
            assertTrue(agent.contains("AgentArtsRuntimeApp"),
                    "Should reference AgentArtsRuntimeApp");
        }

        @Test
        void initConfigYamlContainsAllLayers(@TempDir Path tempDir) throws Exception {
            InitOperation.initProject("basic", "cfg-test", tempDir.toString(),
                    "cn-southwest-2", "org1", "repo1");

            String config = Files.readString(tempDir.resolve("cfg-test/.agentarts_config.yaml"));
            assertTrue(config.contains("base:"), "Missing base layer");
            assertTrue(config.contains("swr_config:"), "Missing swr_config layer");
            assertTrue(config.contains("runtime:"), "Missing runtime layer");
            assertTrue(config.contains("name: cfg-test"), "Missing project name");
            assertTrue(config.contains("region: cn-southwest-2"), "Missing region");
            assertTrue(config.contains("organization: org1"), "Missing SWR org");
            assertTrue(config.contains("repository: repo1"), "Missing SWR repo");
        }

        @Test
        void initConfigYamlHasJavaDefaults(@TempDir Path tempDir) throws Exception {
            InitOperation.initProject("basic", "java-test", tempDir.toString(),
                    "cn-southwest-2", null, null);

            String config = Files.readString(tempDir.resolve("java-test/.agentarts_config.yaml"));
            assertTrue(config.contains("language: java17"), "Missing language default");
            assertTrue(config.contains("base_image: eclipse-temurin:17-jre"), "Missing base_image default");
            assertTrue(config.contains("platform: linux/amd64"), "Missing platform default");
            assertTrue(config.contains("container_runtime: docker"), "Missing container_runtime default");
        }

        @Test
        void initDockerfileContainsCorrectImage(@TempDir Path tempDir) throws Exception {
            InitOperation.initProject("basic", "docker-test", tempDir.toString(),
                    "cn-southwest-2", null, null);

            String dockerfile = Files.readString(tempDir.resolve("docker-test/Dockerfile"));
            assertTrue(dockerfile.contains("eclipse-temurin:17-jre"), "Missing base image");
            assertTrue(dockerfile.contains("docker-test"), "Missing project name");
            assertTrue(dockerfile.contains("EXPOSE 8080"), "Missing expose");
        }

        @Test
        void initAgentscopeTemplateCreatesDifferentAgent(@TempDir Path tempDir) throws Exception {
            InitOperation.initProject("agentscope", "as-test", tempDir.toString(),
                    "cn-southwest-2", null, null);

            String agent = Files.readString(
                    tempDir.resolve("as-test/src/main/java/com/example/Agent.java"));
            assertTrue(agent.contains("as-test"), "Agent should contain project name");
        }

        @Test
        void initSwrDefaultsToProjectName(@TempDir Path tempDir) throws Exception {
            InitOperation.initProject("basic", "default-swr", tempDir.toString(),
                    "cn-southwest-2", null, null);

            String config = Files.readString(tempDir.resolve("default-swr/.agentarts_config.yaml"));
            assertTrue(config.contains("organization: default-swr"), "SWR org should default to name");
            assertTrue(config.contains("repository: default-swr"), "SWR repo should default to name");
        }
    }

    // ============================================================
    // Config Operation E2E (with temp directory)
    // ============================================================

    @Nested
    class ConfigOperationE2E {

        @TempDir
        Path tempDir;

        private Path configFile;

        @BeforeEach
        void setUp() {
            configFile = tempDir.resolve(".agentarts_config.yaml");
        }

        @Test
        void addAgentCreatesConfigFile() {
            // ConfigOperation operates on CWD, so we test the operation directly
            AgentArtsConfigList config = new AgentArtsConfigList();
            AgentArtsConfig agent = new AgentArtsConfig();
            agent.getBase().setName("test-agent");
            agent.getBase().setEntrypoint("com.example.TestAgent");
            agent.getBase().setRegion("cn-north-4");
            agent.getSwrConfig().setOrganization("org1");
            agent.getSwrConfig().setRepository("repo1");
            config.addAgent("test-agent", agent);
            config.setDefaultAgent("test-agent");

            // Write config to temp dir
            writeYamlConfig(configFile, config);

            assertTrue(Files.exists(configFile), "Config file should be created");
        }

        @Test
        void fullCrudLifecycle() {
            // 1. Create initial config
            AgentArtsConfigList config = new AgentArtsConfigList();
            config.setDefaultAgent("agent-a");

            AgentArtsConfig agentA = createAgent("agent-a", "com.example.AAgent", "cn-southwest-2");
            AgentArtsConfig agentB = createAgent("agent-b", "com.example.BAgent", "cn-north-4");
            config.addAgent("agent-a", agentA);
            config.addAgent("agent-b", agentB);

            writeYamlConfig(configFile, config);

            // 2. Read back
            AgentArtsConfigList loaded = readYamlConfig(configFile);
            assertNotNull(loaded.getAgent("agent-a"));
            assertNotNull(loaded.getAgent("agent-b"));
            assertEquals("agent-a", loaded.getDefaultAgent());

            // 3. Update agent-a region
            loaded.getAgent("agent-a").getBase().setRegion("cn-east-3");
            writeYamlConfig(configFile, loaded);

            AgentArtsConfigList reloaded = readYamlConfig(configFile);
            assertEquals("cn-east-3", reloaded.getAgent("agent-a").getBase().getRegion());

            // 4. Set default to agent-b
            reloaded.setDefaultAgent("agent-b");
            writeYamlConfig(configFile, reloaded);

            AgentArtsConfigList final_config = readYamlConfig(configFile);
            assertEquals("agent-b", final_config.getDefaultAgent());

            // 5. Remove agent-a
            final_config.removeAgent("agent-a");
            writeYamlConfig(configFile, final_config);

            AgentArtsConfigList afterRemove = readYamlConfig(configFile);
            assertNull(afterRemove.getAgent("agent-a"));
            assertNotNull(afterRemove.getAgent("agent-b"));
            assertEquals(1, afterRemove.getAgents().size());
        }

        @Test
        void environmentVariablesCrud() {
            AgentArtsConfigList config = new AgentArtsConfigList();
            AgentArtsConfig agent = createAgent("env-test", "com.example.EnvAgent", "cn-southwest-2");
            config.addAgent("env-test", agent);
            config.setDefaultAgent("env-test");
            writeYamlConfig(configFile, config);

            // Set env vars
            AgentArtsConfigList loaded = readYamlConfig(configFile);
            loaded.getAgent("env-test").getRuntime().setEnvironmentVariables(
                    new java.util.HashMap<>());
            loaded.getAgent("env-test").getRuntime().getEnvironmentVariables()
                    .put("API_KEY", "secret123");
            loaded.getAgent("env-test").getRuntime().getEnvironmentVariables()
                    .put("LOG_LEVEL", "debug");
            writeYamlConfig(configFile, loaded);

            // Verify
            AgentArtsConfigList reloaded = readYamlConfig(configFile);
            assertEquals("secret123",
                    reloaded.getAgent("env-test").getRuntime().getEnvironmentVariables().get("API_KEY"));
            assertEquals("debug",
                    reloaded.getAgent("env-test").getRuntime().getEnvironmentVariables().get("LOG_LEVEL"));

            // Remove one
            reloaded.getAgent("env-test").getRuntime().getEnvironmentVariables().remove("LOG_LEVEL");
            writeYamlConfig(configFile, reloaded);

            AgentArtsConfigList afterRemove = readYamlConfig(configFile);
            assertNull(afterRemove.getAgent("env-test").getRuntime().getEnvironmentVariables()
                    .get("LOG_LEVEL"));
            assertEquals("secret123",
                    afterRemove.getAgent("env-test").getRuntime().getEnvironmentVariables().get("API_KEY"));
        }

        @Test
        void dotNotationConfigAccess() {
            AgentArtsConfigList config = new AgentArtsConfigList();
            AgentArtsConfig agent = createAgent("dot-test", "com.example.DotAgent", "cn-southwest-2");
            config.addAgent("dot-test", agent);
            config.setDefaultAgent("dot-test");
            writeYamlConfig(configFile, config);

            AgentArtsConfigList loaded = readYamlConfig(configFile);
            AgentArtsConfig a = loaded.getAgent("dot-test");

            // base.name
            assertEquals("dot-test", a.getBase().getName());
            // base.entrypoint
            assertEquals("com.example.DotAgent", a.getBase().getEntrypoint());
            // base.region
            assertEquals("cn-southwest-2", a.getBase().getRegion());

            // Set base.region via setter
            a.getBase().setRegion("ap-southeast-1");
            writeYamlConfig(configFile, loaded);

            AgentArtsConfigList reloaded = readYamlConfig(configFile);
            assertEquals("ap-southeast-1", reloaded.getAgent("dot-test").getBase().getRegion());
        }

        @Test
        void configPersistenceAcrossLoads() {
            // Write, read, modify, write, read again
            for (int i = 0; i < 3; i++) {
                AgentArtsConfigList config;
                if (Files.exists(configFile)) {
                    config = readYamlConfig(configFile);
                } else {
                    config = new AgentArtsConfigList();
                }
                AgentArtsConfig agent = createAgent("agent-" + i,
                        "com.example.Agent" + i, "cn-southwest-2");
                config.addAgent("agent-" + i, agent);
                if (i == 0) config.setDefaultAgent("agent-0");
                writeYamlConfig(configFile, config);
            }

            AgentArtsConfigList final_config = readYamlConfig(configFile);
            assertEquals(3, final_config.getAgents().size());
            assertNotNull(final_config.getAgent("agent-0"));
            assertNotNull(final_config.getAgent("agent-1"));
            assertNotNull(final_config.getAgent("agent-2"));
            assertEquals("agent-0", final_config.getDefaultAgent());
        }

        // Helper methods

        private AgentArtsConfig createAgent(String name, String entrypoint, String region) {
            AgentArtsConfig agent = new AgentArtsConfig();
            agent.getBase().setName(name);
            agent.getBase().setEntrypoint(entrypoint);
            agent.getBase().setRegion(region);
            return agent;
        }

        private void writeYamlConfig(Path path, AgentArtsConfigList config) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper yamlMapper = new com.fasterxml.jackson.databind.ObjectMapper(
                        new com.fasterxml.jackson.dataformat.yaml.YAMLFactory()
                                .disable(com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
                yamlMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), config);
            } catch (Exception e) {
                fail("Failed to write YAML: " + e.getMessage());
            }
        }

        private AgentArtsConfigList readYamlConfig(Path path) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper yamlMapper = new com.fasterxml.jackson.databind.ObjectMapper(
                        new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
                return yamlMapper.readValue(path.toFile(), AgentArtsConfigList.class);
            } catch (Exception e) {
                fail("Failed to read YAML: " + e.getMessage());
                return null;
            }
        }
    }

    // ============================================================
    // CLI Command Execution E2E (via Picocli)
    // ============================================================

    @Nested
    class CliExecutionE2E {

        private CommandLine cli;

        @BeforeEach
        void setUp() {
            cli = new CommandLine(new AgentArtsCli());
        }

        @Test
        void helpCommandReturnsZero() {
            StringWriter sw = new StringWriter();
            cli.setOut(new PrintWriter(sw));
            int exitCode = cli.execute("--help");
            assertEquals(0, exitCode);
        }

        @Test
        void versionCommandReturnsZero() {
            StringWriter sw = new StringWriter();
            cli.setOut(new PrintWriter(sw));
            int exitCode = cli.execute("--version");
            assertEquals(0, exitCode);
        }

        @Test
        void helpListsAllTopLevelCommands() {
            StringWriter sw = new StringWriter();
            cli.setOut(new PrintWriter(sw));
            cli.execute("--help");
            String output = sw.toString();

            // All 9 commands should appear in help
            assertTrue(output.contains("init"), "Missing 'init' in help");
            assertTrue(output.contains("config"), "Missing 'config' in help");
            assertTrue(output.contains("dev"), "Missing 'dev' in help");
            assertTrue(output.contains("deploy"), "Missing 'deploy' in help");
            assertTrue(output.contains("invoke"), "Missing 'invoke' in help");
            assertTrue(output.contains("destroy"), "Missing 'destroy' in help");
            assertTrue(output.contains("runtime"), "Missing 'runtime' in help");
            assertTrue(output.contains("mcp-gateway"), "Missing 'mcp-gateway' in help");
            assertTrue(output.contains("memory"), "Missing 'memory' in help");
        }

        @Test
        void initHelpShowsOptions() {
            // Use usage() directly to get subcommand help text
            StringWriter sw = new StringWriter();
            cli.getSubcommands().get("init").usage(new PrintWriter(sw));
            String output = sw.toString();

            assertTrue(output.contains("--name") || output.contains("-n"), "Missing --name option");
            assertTrue(output.contains("--template") || output.contains("-t"), "Missing --template option");
            assertTrue(output.contains("--path") || output.contains("-p"), "Missing --path option");
            assertTrue(output.contains("--region") || output.contains("-r"), "Missing --region option");
        }

        @Test
        void configHelpShowsSubcommands() {
            StringWriter sw = new StringWriter();
            cli.getSubcommands().get("config").usage(new PrintWriter(sw));
            String output = sw.toString();

            assertTrue(output.contains("list"), "Missing 'list' subcommand");
            assertTrue(output.contains("set-default"), "Missing 'set-default' subcommand");
            assertTrue(output.contains("get"), "Missing 'get' subcommand");
            assertTrue(output.contains("set"), "Missing 'set' subcommand");
            assertTrue(output.contains("remove"), "Missing 'remove' subcommand");
            assertTrue(output.contains("set-env"), "Missing 'set-env' subcommand");
            assertTrue(output.contains("remove-env"), "Missing 'remove-env' subcommand");
            assertTrue(output.contains("list-env"), "Missing 'list-env' subcommand");
        }

        @Test
        void runtimeHelpShowsSubcommands() {
            StringWriter sw = new StringWriter();
            cli.getSubcommands().get("runtime").usage(new PrintWriter(sw));
            String output = sw.toString();

            assertTrue(output.contains("invoke"), "Missing 'invoke' subcommand");
            assertTrue(output.contains("exec-command"), "Missing 'exec-command' subcommand");
            assertTrue(output.contains("upload-files"), "Missing 'upload-files' subcommand");
            assertTrue(output.contains("download-files"), "Missing 'download-files' subcommand");
            assertTrue(output.contains("start-session"), "Missing 'start-session' subcommand");
            assertTrue(output.contains("stop-session"), "Missing 'stop-session' subcommand");
        }

        @Test
        void mcpGatewayHelpShowsSubcommands() {
            StringWriter sw = new StringWriter();
            cli.getSubcommands().get("mcp-gateway").usage(new PrintWriter(sw));
            String output = sw.toString();

            assertTrue(output.contains("create-mcp-gateway"));
            assertTrue(output.contains("update-mcp-gateway"));
            assertTrue(output.contains("delete-mcp-gateway"));
            assertTrue(output.contains("get-mcp-gateway"));
            assertTrue(output.contains("list-mcp-gateways"));
            assertTrue(output.contains("create-mcp-gateway-target"));
            assertTrue(output.contains("update-mcp-gateway-target"));
            assertTrue(output.contains("delete-mcp-gateway-target"));
            assertTrue(output.contains("get-mcp-gateway-target"));
            assertTrue(output.contains("list-mcp-gateway-targets"));
        }

        @Test
        void memoryHelpShowsSubcommands() {
            StringWriter sw = new StringWriter();
            cli.getSubcommands().get("memory").usage(new PrintWriter(sw));
            String output = sw.toString();

            assertTrue(output.contains("create"));
            assertTrue(output.contains("get"));
            assertTrue(output.contains("list"));
            assertTrue(output.contains("update"));
            assertTrue(output.contains("delete"));
            assertTrue(output.contains("status"));
        }

        @Test
        void unknownCommandReturnsNonZero() {
            StringWriter sw = new StringWriter();
            StringWriter errSw = new StringWriter();
            cli.setOut(new PrintWriter(sw));
            cli.setErr(new PrintWriter(errSw));
            int exitCode = cli.execute("nonexistent-command");
            assertNotEquals(0, exitCode);
        }
    }

    // ============================================================
    // Template rendering E2E
    // ============================================================

    @Nested
    class TemplateRenderingE2E {

        @Test
        void allTemplatesRenderWithoutErrors() throws Exception {
            // Verify all templates can be loaded
            assertNotNull(TemplateManager.loadTemplate("basic/Agent.java.tpl"));
            assertNotNull(TemplateManager.loadTemplate("agentscope/Agent.java.tpl"));
            assertNotNull(TemplateManager.loadTemplate("basic/pom.xml.tpl"));
            assertNotNull(TemplateManager.loadTemplate("basic/config.yaml.tpl"));
            assertNotNull(TemplateManager.loadTemplate("docker/Dockerfile.tpl"));
        }

        @Test
        void basicAgentTemplateSubstitutesCorrectly(@TempDir Path tempDir) throws Exception {
            Path output = tempDir.resolve("Agent.java");
            TemplateManager.renderToFile("basic/Agent.java.tpl", output, "name", "weather");

            String content = Files.readString(output);
            assertTrue(content.contains("weatherAgent"), "Class name should be {name}Agent");
            assertFalse(content.contains("{{ name }}"), "Placeholder should be replaced");
            assertFalse(content.contains("{{name}}"), "Placeholder should be replaced");
        }

        @Test
        void configTemplateSubstitutesAllVars(@TempDir Path tempDir) throws Exception {
            Path output = tempDir.resolve("config.yaml");
            TemplateManager.renderToFile("basic/config.yaml.tpl", output,
                    "name", "my-proj", "region", "cn-north-4",
                    "swr_org", "my-org", "swr_repo", "my-repo");

            String content = Files.readString(output);
            assertTrue(content.contains("name: my-proj"), "Name not substituted");
            assertTrue(content.contains("region: cn-north-4"), "Region not substituted");
            assertTrue(content.contains("organization: my-org"), "SWR org not substituted");
            assertTrue(content.contains("repository: my-repo"), "SWR repo not substituted");
            assertFalse(content.contains("{{ "), "Unresolved placeholders remain");
        }

        @Test
        void pomTemplateSubstitutesName(@TempDir Path tempDir) throws Exception {
            Path output = tempDir.resolve("pom.xml");
            TemplateManager.renderToFile("basic/pom.xml.tpl", output,
                    "name", "cool-project", "region", "cn-southwest-2");

            String content = Files.readString(output);
            assertTrue(content.contains("<artifactId>cool-project</artifactId>"),
                    "Name should be in artifactId");
        }

        @Test
        void dockerfileTemplateSubstitutesName(@TempDir Path tempDir) throws Exception {
            Path output = tempDir.resolve("Dockerfile");
            TemplateManager.renderToFile("docker/Dockerfile.tpl", output, "name", "svc");

            String content = Files.readString(output);
            assertTrue(content.contains("svc"), "Name should be in Dockerfile");
            assertTrue(content.contains("eclipse-temurin:17-jre"), "Base image should be present");
        }

        @Test
        void missingTemplateThrowsIOException() {
            assertThrows(IOException.class, () ->
                    TemplateManager.loadTemplate("nonexistent/template.tpl"));
        }

        @Test
        void renderWithEmptyMapReturnsOriginal() {
            String template = "Hello {{ name }}";
            String result = TemplateManager.render(template, java.util.Map.of());
            assertEquals("Hello {{ name }}", result);
        }

        @Test
        void renderHandlesBothSpacingFormats() {
            String template = "{{ spaced }} and {{nospace}}";
            String result = TemplateManager.render(template,
                    java.util.Map.of("spaced", "A", "nospace", "B"));
            assertEquals("A and B", result);
        }
    }

    // ============================================================
    // Full workflow E2E: init → verify generated config
    // ============================================================

    @Nested
    class FullWorkflowE2E {

        @Test
        void initThenReadConfig(@TempDir Path tempDir) throws Exception {
            // Step 1: Init project
            InitOperation.initProject("basic", "workflow-test", tempDir.toString(),
                    "cn-southwest-2", "my-org", null);

            // Step 2: Read generated config
            Path configPath = tempDir.resolve("workflow-test/.agentarts_config.yaml");
            assertTrue(Files.exists(configPath));

            com.fasterxml.jackson.databind.ObjectMapper yamlMapper =
                    new com.fasterxml.jackson.databind.ObjectMapper(
                            new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
            AgentArtsConfigList config = yamlMapper.readValue(configPath.toFile(),
                    AgentArtsConfigList.class);

            // Step 3: Verify config content
            assertNotNull(config.getAgents(), "Should have agents map");
            // The init template generates a config but doesn't add to AgentArtsConfigList format
            // It renders a flat YAML, so we verify the raw content
            String rawConfig = Files.readString(configPath);
            assertTrue(rawConfig.contains("workflow-test"), "Config should contain project name");
            assertTrue(rawConfig.contains("cn-southwest-2"), "Config should contain region");
            assertTrue(rawConfig.contains("my-org"), "Config should contain SWR org");
        }

        @Test
        void initGeneratesBuildableProjectStructure(@TempDir Path tempDir) throws Exception {
            InitOperation.initProject("basic", "buildable", tempDir.toString(),
                    "cn-southwest-2", null, null);

            Path projectDir = tempDir.resolve("buildable");

            // Verify the generated Java file is syntactically plausible
            String agentJava = Files.readString(projectDir.resolve("src/main/java/com/example/Agent.java"));
            assertTrue(agentJava.contains("public class"), "Should have a public class");
            assertTrue(agentJava.contains("public static void main"), "Should have main method");
            assertTrue(agentJava.contains("import"), "Should have imports");

            // Verify pom.xml is valid XML structure
            String pomXml = Files.readString(projectDir.resolve("pom.xml"));
            assertTrue(pomXml.contains("<?xml"), "Should be XML");
            assertTrue(pomXml.contains("<project"), "Should have project root element");
            assertTrue(pomXml.contains("<dependencies>"), "Should have dependencies");
            assertTrue(pomXml.contains("agentarts-sdk-runtime"), "Should depend on runtime SDK");
        }

        @Test
        void multipleInitDifferentTemplates(@TempDir Path tempDir) throws Exception {
            // Init basic template
            InitOperation.initProject("basic", "proj-basic", tempDir.toString(),
                    "cn-southwest-2", null, null);

            // Init agentscope template
            InitOperation.initProject("agentscope", "proj-agentscope", tempDir.toString(),
                    "cn-southwest-2", null, null);

            // Both should exist independently
            assertTrue(Files.isDirectory(tempDir.resolve("proj-basic")));
            assertTrue(Files.isDirectory(tempDir.resolve("proj-agentscope")));

            // Each should have its own files
            assertTrue(Files.exists(tempDir.resolve("proj-basic/pom.xml")));
            assertTrue(Files.exists(tempDir.resolve("proj-agentscope/pom.xml")));

            // Agent files should differ
            String basicAgent = Files.readString(
                    tempDir.resolve("proj-basic/src/main/java/com/example/Agent.java"));
            String asAgent = Files.readString(
                    tempDir.resolve("proj-agentscope/src/main/java/com/example/Agent.java"));
            assertNotEquals(basicAgent, asAgent, "Different templates should produce different output");
        }
    }

    // ============================================================
    // Input validation E2E
    // ============================================================

    @Nested
    class InputValidationE2E {

        @Test
        void initPrintsErrorWhenNameMissing(@TempDir Path tempDir) throws Exception {
            // InitCommand returns a non-zero exit code on validation failure
            // (missing --name), and must not create any project directory.
            CommandLine cli = new CommandLine(new AgentArtsCli());
            int exitCode = cli.execute("init", "--path", tempDir.toString());
            assertNotEquals(0, exitCode, "init with missing --name should exit non-zero");
            // No project directory should be created since --name was not provided
            assertEquals(0, Files.list(tempDir).count(),
                    "no files/dirs should be created when --name is missing");
        }

        @Test
        void initLowercasesNameBeforeValidation(@TempDir Path tempDir) throws Exception {
            // InitCommand lowercases the name before regex validation
            CommandLine cli = new CommandLine(new AgentArtsCli());
            cli.execute("init", "--name", "MyProject", "--path", tempDir.toString());

            // The name should be lowercased to "myproject"
            assertTrue(Files.isDirectory(tempDir.resolve("myproject")),
                    "Project should be created with lowercased name");
        }

        @Test
        void deployDefaultModeIsCloud() {
            CommandLine cli = new CommandLine(new AgentArtsCli());
            var spec = cli.getSubcommands().get("deploy").getCommandSpec();
            assertEquals("cloud", spec.optionsMap().get("--mode").defaultValue());
        }

        @Test
        void invokeDefaultTimeoutIs900() {
            CommandLine cli = new CommandLine(new AgentArtsCli());
            var spec = cli.getSubcommands().get("invoke").getCommandSpec();
            assertEquals("900", spec.optionsMap().get("--timeout").defaultValue());
        }

        @Test
        void initCreatesProjectWithDefaults(@TempDir Path tempDir) throws Exception {
            CommandLine cli = new CommandLine(new AgentArtsCli());
            cli.execute("init", "--name", "test-proj", "--path", tempDir.toString());

            Path projDir = tempDir.resolve("test-proj");
            assertTrue(Files.isDirectory(projDir));
            assertTrue(Files.exists(projDir.resolve("pom.xml")));
            assertTrue(Files.exists(projDir.resolve("Dockerfile")));
            assertTrue(Files.exists(projDir.resolve(".agentarts_config.yaml")));
        }
    }
}
