package com.huaweicloud.agentarts.toolkit.operations;

import com.huaweicloud.agentarts.sdk.core.config.AgentArtsConfig;
import com.huaweicloud.agentarts.sdk.core.config.AgentArtsConfigList;
import com.huaweicloud.agentarts.sdk.core.config.BaseConfig;
import com.huaweicloud.agentarts.sdk.core.config.SWRConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.huaweicloud.agentarts.toolkit.commands.CliSupport;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Map;

/**
 * Config operation: manages .agentarts_config.yaml.
 *
 * <p>Operation: manage configuration file.</p>
 */
public class ConfigOperation {

    private static final String CONFIG_FILE = ".agentarts_config.yaml";
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(
            new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

    /**
     * Thread-local override for the config file location.
     *
     * <p>Production code resolves the config file to {@code .agentarts_config.yaml}
     * in the current working directory (matching the Python toolkit's
     * {@code Path.cwd() / ".agentarts_config.yaml"}). Tests redirect this to a
     * temp directory — the Java analog of the Python tests'
     * {@code monkeypatch.chdir(tmp_project)}.</p>
     */
    private static final ThreadLocal<File> CONFIG_FILE_OVERRIDE = new ThreadLocal<>();

    /**
     * Redirect config file reads/writes to {@code file} for the current thread.
     * Pass {@code null} (or call {@link #clearConfigFileOverride()}) to restore
     * the default {@code .agentarts_config.yaml} location.
     */
    public static void setConfigFileOverride(File file) {
        CONFIG_FILE_OVERRIDE.set(file);
    }

    /** Clear any thread-local config file override. */
    public static void clearConfigFileOverride() {
        CONFIG_FILE_OVERRIDE.remove();
    }

    private static File configFile() {
        File override = CONFIG_FILE_OVERRIDE.get();
        if (override != null) {
            return override;
        }
        // Resolve against the live `user.dir` system property rather than a
        // relative path. For real CLI use `user.dir` is the process CWD (set at
        // JVM startup), so this is equivalent to `new File(CONFIG_FILE)`. But
        // unlike a relative path it also honors runtime updates to `user.dir`
        // (Java cannot chdir at runtime; tests redirect by setting the property,
        // the Java analog of the Python suite's `monkeypatch.chdir`).
        String cwd = System.getProperty("user.dir", ".");
        return new File(cwd, CONFIG_FILE);
    }

    /**
     * Load agent configuration from {@code .agentarts_config.yaml}.
     * Creates a default config file if it does not exist.
     *
     * @return the loaded or newly created config list
     */
    public static AgentArtsConfigList loadConfig() {
        File file = configFile();
        if (!file.exists()) {
            AgentArtsConfigList config = new AgentArtsConfigList();
            saveConfig(config);
            return config;
        }
        try {
            AgentArtsConfigList config = YAML_MAPPER.readValue(file, AgentArtsConfigList.class);
            if (config == null) {
                throw new IOException("Configuration file is empty");
            }
            return config;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load CLI configuration", e);
        }
    }

    /** Save agent configuration to {@code .agentarts_config.yaml}. */
    public static void saveConfig(AgentArtsConfigList config) {
        saveAtomically(config);
    }

    /**
     * Add a new agent to the configuration and set it as default.
     *
     * @param name           agent name
     * @param entrypoint     Java entrypoint class (nullable, auto-generated if null)
     * @param region         Huawei Cloud region (nullable, defaults to cn-southwest-2)
     * @param dependencyFile build file path (nullable, defaults to pom.xml)
     * @param swrOrg         SWR organization (nullable, defaults to agent name)
     * @param swrRepo        SWR repository (nullable, defaults to agent name)
     */
    public static void addAgent(String name, String entrypoint, String region,
                                 String dependencyFile, String swrOrg, String swrRepo) {
        AgentArtsConfigList config = loadConfig();
        AgentArtsConfig agent = new AgentArtsConfig();

        BaseConfig base = new BaseConfig();
        base.setName(name);
        base.setEntrypoint(entrypoint != null ? entrypoint : "com.example." + capitalize(name) + "Agent");
        base.setRegion(region != null ? region : "cn-southwest-2");
        base.setDependencyFile(dependencyFile != null ? dependencyFile : "pom.xml");
        agent.setBase(base);

        SWRConfig swr = new SWRConfig();
        swr.setOrganization(swrOrg != null ? swrOrg : name);
        swr.setRepository(swrRepo != null ? swrRepo : name);
        agent.setSwrConfig(swr);

        config.addAgent(name, agent);
        config.setDefaultAgent(name);
        saveConfig(config);
        System.out.println("Agent '" + name + "' added and set as default.");
    }

    /** Print all configured agents to stdout, marking the default with {@code *}. */
    public static void printConfigList() {
        AgentArtsConfigList config = loadConfig();
        if (config.getAgents().isEmpty()) {
            System.out.println("No agents configured.");
            return;
        }
        for (Map.Entry<String, AgentArtsConfig> entry : config.getAgents().entrySet()) {
            String marker = entry.getKey().equals(config.getDefaultAgent()) ? " *" : "";
            System.out.println("  " + entry.getKey() + marker);
        }
    }

    /** Set the default agent by name. */
    public static void setDefaultAgent(String name) {
        AgentArtsConfigList config = loadConfig();
        if (config.getAgent(name) == null) {
            CliSupport.failCli("Agent '" + name + "' not found");
        }
        config.setDefaultAgent(name);
        saveConfig(config);
        System.out.println("Default agent set to '" + name + "'.");
    }

    /** Print detailed YAML configuration for a specific agent (or the default). */
    public static void printAgentDetail(String agentName) {
        AgentArtsConfigList config = loadConfig();
        String name = agentName != null ? agentName : config.getDefaultAgent();
        AgentArtsConfig agent = config.getAgent(name);
        if (agent == null) {
            CliSupport.failCli("Agent '" + name + "' not found");
        }
        try {
            JsonNode redacted = CliSupport.redactSensitiveValues(YAML_MAPPER.valueToTree(agent));
            System.out.println(YAML_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(redacted));
        } catch (Exception e) {
            CliSupport.failCli("Unable to serialize agent configuration safely");
        }
    }

    /**
     * Get a configuration value by dot-notation key (e.g. {@code base.region},
     * {@code base.description}). Operates on the raw YAML tree so arbitrary
     * dotted paths under an agent are supported (matching the Python toolkit's
     * generic dict-walking accessor, which leverages Pydantic {@code extra="allow"}).
     *
     * @param key        dotted config key
     * @param agentName  agent name (defaults to the configured default agent)
     */
    public static void getConfigValue(String key, String agentName) {
        JsonNode root = loadConfigTree();
        String name = agentName != null ? agentName : root.path("default_agent").asText(null);
        JsonNode agent = name == null ? null : root.path("agents").get(name);
        if (agent == null || agent.isMissingNode() || agent.isNull()) {
            CliSupport.failCli("Agent '" + name + "' not found");
        }
        JsonNode value = agent;
        for (String part : key.split("\\.")) {
            if (value != null && value.isObject()) {
                value = value.get(part);
            } else {
                value = null;
                break;
            }
        }
        if (value == null || value.isMissingNode() || value.isNull()) {
            CliSupport.failCli("Unknown configuration key: " + key);
        } else {
            if (CliSupport.isSensitiveName(key)) {
                System.out.println("[REDACTED]");
            } else {
                System.out.println(value.isTextual() ? value.asText() : value.toString());
            }
        }
    }

    /**
     * Set a configuration value by dot-notation key (e.g. {@code base.region},
     * {@code base.description}). Operates on the raw YAML tree so arbitrary
     * dotted paths under an agent are persisted (matching the Python toolkit's
     * generic dict-walking mutator).
     *
     * @param key        dotted config key
     * @param value      config value
     * @param agentName  agent name (defaults to the configured default agent)
     */
    public static void setConfigValue(String key, String value, String agentName) {
        JsonNode root = loadConfigTree();
        if (!root.isObject()) {
            root = YAML_MAPPER.createObjectNode();
        }
        String name = agentName != null ? agentName : root.path("default_agent").asText(null);
        JsonNode agentsNode = root.path("agents");
        if (!agentsNode.isObject()) {
            agentsNode = YAML_MAPPER.createObjectNode();
            ((ObjectNode) root).set("agents", agentsNode);
        }
        JsonNode agent = name == null ? null : agentsNode.get(name);
        if (name == null || agent == null || agent.isMissingNode() || agent.isNull() || !agent.isObject()) {
            CliSupport.failCli("Agent '" + name + "' not found");
        }
        String[] parts = key.split("\\.");
        if (parts.length == 0 || java.util.Arrays.stream(parts).anyMatch(String::isBlank)) {
            CliSupport.failCli("Configuration key must contain non-empty dot-separated segments");
        }
        ObjectNode current = (ObjectNode) agent;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonNode child = current.get(parts[i]);
            if (child == null || !child.isObject()) {
                child = YAML_MAPPER.createObjectNode();
                current.set(parts[i], child);
            }
            current = (ObjectNode) child;
        }
        current.put(parts[parts.length - 1], value);
        if ("base.name".equals(key) && !name.equals(value)) {
            if (value == null || value.isBlank()) {
                CliSupport.failCli("Agent name must not be blank");
            }
            ObjectNode agents = (ObjectNode) agentsNode;
            if (agents.has(value)) {
                CliSupport.failCli("Agent '" + value + "' already exists");
            }
            agents.set(value, agent);
            agents.remove(name);
            if (name.equals(root.path("default_agent").asText(null))) {
                ((ObjectNode) root).put("default_agent", value);
            }
        }
        saveConfigTree(root);
        String displayed = CliSupport.isSensitiveName(key) ? "[REDACTED]" : value;
        System.out.println("Set " + key + " = " + displayed);
    }

    /** Load the config file as a raw {@link JsonNode} tree (preserves all keys). */
    private static JsonNode loadConfigTree() {
        File file = configFile();
        if (!file.exists()) {
            return YAML_MAPPER.createObjectNode();
        }
        try {
            JsonNode tree = YAML_MAPPER.readTree(file);
            if (tree == null) {
                throw new IOException("Configuration file is empty");
            }
            return tree;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load CLI configuration", e);
        }
    }

    /** Save a raw {@link JsonNode} tree back to the config file. */
    private static void saveConfigTree(JsonNode tree) {
        saveAtomically(tree);
    }

    /** Serialize to a restricted temporary file and atomically replace the config. */
    private static void saveAtomically(Object value) {
        Path target = configFile().toPath().toAbsolutePath().normalize();
        Path parent = target.getParent();
        Path temporary = null;
        try {
            if (parent == null) {
                throw new IOException("Configuration path has no parent directory");
            }
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, ".agentarts-config-", ".tmp");
            restrictOwnerAccess(temporary);
            YAML_MAPPER.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), value);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            try {
                Files.move(temporary, target,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save CLI configuration", e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Best-effort cleanup after a failed write. The file contains
                    // only the new serialized config and already has owner-only
                    // permissions on POSIX file systems.
                }
            }
        }
    }

    private static void restrictOwnerAccess(Path path) throws IOException {
        PosixFileAttributeView view = Files.getFileAttributeView(
                path, PosixFileAttributeView.class);
        if (view != null) {
            view.setPermissions(EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        }
    }

    /** Remove an agent from the configuration. */
    public static void removeAgent(String name) {
        AgentArtsConfigList config = loadConfig();
        if (config.getAgent(name) == null) {
            CliSupport.failCli("Agent '" + name + "' not found");
        }
        config.removeAgent(name);
        saveConfig(config);
        System.out.println("Agent '" + name + "' removed.");
    }

    /** Set an environment variable for an agent's runtime configuration. */
    public static void setEnv(String key, String value, String agentName) {
        AgentArtsConfigList config = loadConfig();
        String name = agentName != null ? agentName : config.getDefaultAgent();
        AgentArtsConfig agent = config.getAgent(name);
        if (agent == null) {
            CliSupport.failCli("Agent '" + name + "' not found");
        }
        if (agent.getRuntime().getEnvironmentVariables() == null) {
            agent.getRuntime().setEnvironmentVariables(new java.util.HashMap<>());
        }
        agent.getRuntime().getEnvironmentVariables().put(key, value);
        saveConfig(config);
        System.out.println("Set env " + key + "=[REDACTED] for agent '" + name + "'.");
    }

    /** Remove an environment variable from an agent's runtime configuration. */
    public static void removeEnv(String key, String agentName) {
        AgentArtsConfigList config = loadConfig();
        String name = agentName != null ? agentName : config.getDefaultAgent();
        AgentArtsConfig agent = config.getAgent(name);
        if (agent == null) {
            CliSupport.failCli("Agent '" + name + "' not found");
        }
        if (agent.getRuntime().getEnvironmentVariables() == null
                || !agent.getRuntime().getEnvironmentVariables().containsKey(key)) {
            CliSupport.failCli("Environment variable '" + key + "' not found");
        }
        agent.getRuntime().getEnvironmentVariables().remove(key);
        saveConfig(config);
        System.out.println("Removed env " + key + " for agent '" + name + "'.");
    }

    /** List all environment variables for an agent's runtime configuration. */
    public static void listEnv(String agentName) {
        AgentArtsConfigList config = loadConfig();
        String name = agentName != null ? agentName : config.getDefaultAgent();
        AgentArtsConfig agent = config.getAgent(name);
        if (agent == null) {
            CliSupport.failCli("Agent '" + name + "' not found");
        }
        if (agent.getRuntime().getEnvironmentVariables() == null) {
            System.out.println("No environment variables configured.");
            return;
        }
        agent.getRuntime().getEnvironmentVariables().forEach((k, v) ->
                System.out.println("  " + k + "=[REDACTED]"));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
