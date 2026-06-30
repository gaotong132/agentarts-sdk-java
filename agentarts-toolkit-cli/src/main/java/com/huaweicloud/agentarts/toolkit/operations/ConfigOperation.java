package com.huaweicloud.agentarts.toolkit.operations;

import com.huaweicloud.agentarts.sdk.core.config.AgentArtsConfig;
import com.huaweicloud.agentarts.sdk.core.config.AgentArtsConfigList;
import com.huaweicloud.agentarts.sdk.core.config.BaseConfig;
import com.huaweicloud.agentarts.sdk.core.config.SWRConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Config operation: manages .agentarts_config.yaml.
 *
 * <p>Mirrors Python {@code operations/runtime/config.py}.</p>
 */
public class ConfigOperation {

    private static final String CONFIG_FILE = ".agentarts_config.yaml";
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(
            new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

    /**
     * Load agent configuration from {@code .agentarts_config.yaml}.
     * Creates a default config file if it does not exist.
     *
     * @return the loaded or newly created config list
     */
    public static AgentArtsConfigList loadConfig() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            AgentArtsConfigList config = new AgentArtsConfigList();
            saveConfig(config);
            return config;
        }
        try {
            return YAML_MAPPER.readValue(file, AgentArtsConfigList.class);
        } catch (IOException e) {
            System.err.println("Error loading config: " + e.getMessage());
            return new AgentArtsConfigList();
        }
    }

    /** Save agent configuration to {@code .agentarts_config.yaml}. */
    public static void saveConfig(AgentArtsConfigList config) {
        try {
            YAML_MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(CONFIG_FILE), config);
        } catch (IOException e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
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
            System.err.println("Agent '" + name + "' not found.");
            return;
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
            System.err.println("Agent '" + name + "' not found.");
            return;
        }
        try {
            System.out.println(YAML_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(agent));
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /** Get a configuration value by dot-notation key (e.g. {@code base.region}). */
    public static void getConfigValue(String key, String agentName) {
        AgentArtsConfigList config = loadConfig();
        String name = agentName != null ? agentName : config.getDefaultAgent();
        AgentArtsConfig agent = config.getAgent(name);
        if (agent == null) {
            System.err.println("Agent '" + name + "' not found.");
            return;
        }
        // Simple dot-notation accessor
        String[] parts = key.split("\\.");
        if (parts.length == 2 && "base".equals(parts[0])) {
            switch (parts[1]) {
                case "name": System.out.println(agent.getBase().getName()); return;
                case "entrypoint": System.out.println(agent.getBase().getEntrypoint()); return;
                case "region": System.out.println(agent.getBase().getRegion()); return;
            }
        }
        System.err.println("Unknown key: " + key);
    }

    /** Set a configuration value by dot-notation key (e.g. {@code base.region}). */
    public static void setConfigValue(String key, String value, String agentName) {
        AgentArtsConfigList config = loadConfig();
        String name = agentName != null ? agentName : config.getDefaultAgent();
        AgentArtsConfig agent = config.getAgent(name);
        if (agent == null) {
            System.err.println("Agent '" + name + "' not found.");
            return;
        }
        String[] parts = key.split("\\.");
        if (parts.length == 2 && "base".equals(parts[0])) {
            switch (parts[1]) {
                case "name": agent.getBase().setName(value); break;
                case "entrypoint": agent.getBase().setEntrypoint(value); break;
                case "region": agent.getBase().setRegion(value); break;
                default: System.err.println("Unknown key: " + key); return;
            }
        }
        saveConfig(config);
        System.out.println("Set " + key + " = " + value);
    }

    /** Remove an agent from the configuration. */
    public static void removeAgent(String name) {
        AgentArtsConfigList config = loadConfig();
        if (config.getAgent(name) == null) {
            System.err.println("Agent '" + name + "' not found.");
            return;
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
            System.err.println("Agent '" + name + "' not found.");
            return;
        }
        if (agent.getRuntime().getEnvironmentVariables() == null) {
            agent.getRuntime().setEnvironmentVariables(new java.util.HashMap<>());
        }
        agent.getRuntime().getEnvironmentVariables().put(key, value);
        saveConfig(config);
        System.out.println("Set env " + key + "=" + value + " for agent '" + name + "'.");
    }

    /** Remove an environment variable from an agent's runtime configuration. */
    public static void removeEnv(String key, String agentName) {
        AgentArtsConfigList config = loadConfig();
        String name = agentName != null ? agentName : config.getDefaultAgent();
        AgentArtsConfig agent = config.getAgent(name);
        if (agent == null || agent.getRuntime().getEnvironmentVariables() == null) return;
        agent.getRuntime().getEnvironmentVariables().remove(key);
        saveConfig(config);
        System.out.println("Removed env " + key + " for agent '" + name + "'.");
    }

    /** List all environment variables for an agent's runtime configuration. */
    public static void listEnv(String agentName) {
        AgentArtsConfigList config = loadConfig();
        String name = agentName != null ? agentName : config.getDefaultAgent();
        AgentArtsConfig agent = config.getAgent(name);
        if (agent == null || agent.getRuntime().getEnvironmentVariables() == null) {
            System.out.println("No environment variables configured.");
            return;
        }
        agent.getRuntime().getEnvironmentVariables().forEach((k, v) ->
                System.out.println("  " + k + "=" + v));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
