package com.huaweicloud.agentarts.toolkit.commands;

import com.huaweicloud.agentarts.toolkit.operations.ConfigOperation;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Configuration management (alias: configure).
 *
 * <p>CLI command: manage agent configuration.
 * Subcommands: list, set-default, get, set, remove, set-env, remove-env, list-env.</p>
 */
@Command(
    name = "config",
    aliases = {"configure"},
    description = "Configuration management",
    subcommands = {
        ConfigCommand.ListCommand.class,
        ConfigCommand.SetDefaultCommand.class,
        ConfigCommand.GetCommand.class,
        ConfigCommand.SetCommand.class,
        ConfigCommand.RemoveCommand.class,
        ConfigCommand.SetEnvCommand.class,
        ConfigCommand.RemoveEnvCommand.class,
        ConfigCommand.ListEnvCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class ConfigCommand implements Runnable {

    @Option(names = {"-n", "--name"}, description = "Agent name")
    String name;

    @Option(names = {"-e", "--entrypoint"}, description = "Agent entrypoint (e.g., com.example.MyAgent)")
    String entrypoint;

    @Option(names = {"-r", "--region"}, description = "Huawei Cloud region")
    String region;

    @Option(names = {"-d", "--dependency-file"}, description = "Path to dependency file (pom.xml)")
    String dependencyFile;

    @Option(names = "--swr-org", description = "SWR organization name")
    String swrOrg;

    @Option(names = "--swr-repo", description = "SWR repository name")
    String swrRepo;

    @Override
    public void run() {
        // Default callback: interactive or direct agent configuration
        if (name == null) {
            System.out.println("Use 'agentarts config list' to see agents, or 'agentarts config -n <name>' to add one.");
            return;
        }
        ConfigOperation.addAgent(name, entrypoint, region, dependencyFile, swrOrg, swrRepo);
    }

    // ========================
    // Subcommands
    // ========================

    @Command(name = "list", description = "List all configured agents")
    static class ListCommand implements Runnable {
        @Override
        public void run() {
            ConfigOperation.printConfigList();
        }
    }

    @Command(name = "set-default", description = "Set the default agent")
    static class SetDefaultCommand implements Runnable {
        @Parameters(index = "0", description = "Agent name to set as default")
        String agentName;

        @Override
        public void run() {
            ConfigOperation.setDefaultAgent(agentName.toLowerCase());
        }
    }

    @Command(name = "get", description = "Get configuration value or agent details")
    static class GetCommand implements Runnable {
        @Parameters(index = "0", defaultValue = "", description = "Config key (dot notation, e.g. base.region)")
        String key;

        @Option(names = {"-a", "--agent"}, description = "Agent name")
        String agent;

        @Override
        public void run() {
            if (key == null || key.isEmpty()) {
                ConfigOperation.printAgentDetail(agent);
            } else {
                ConfigOperation.getConfigValue(key, agent);
            }
        }
    }

    @Command(name = "set", description = "Set configuration value")
    static class SetCommand implements Runnable {
        @Parameters(index = "0", description = "Config key (dot notation)")
        String key;

        @Parameters(index = "1", arity = "0..1",
                description = "Config value (omit to read securely from stdin)")
        String value;

        @Option(names = {"-a", "--agent"}, description = "Agent name")
        String agent;

        @Override
        public void run() {
            if (value == null) {
                value = CliSupport.readSecretValue("Configuration value: ");
            }
            if ("base.name".equals(key)) {
                value = value.toLowerCase();
            }
            ConfigOperation.setConfigValue(key, value, agent);
        }
    }

    @Command(name = "remove", description = "Remove an agent configuration")
    static class RemoveCommand implements Runnable {
        @Parameters(index = "0", description = "Agent name to remove")
        String agentName;

        @Override
        public void run() {
            ConfigOperation.removeAgent(agentName);
        }
    }

    @Command(name = "set-env", description = "Set environment variable for an agent")
    static class SetEnvCommand implements Runnable {
        @Parameters(index = "0", description = "Environment variable name")
        String key;

        @Parameters(index = "1", arity = "0..1",
                description = "Environment variable value (omit to read securely from stdin)")
        String value;

        @Option(names = {"-a", "--agent"}, description = "Agent name")
        String agent;

        @Override
        public void run() {
            if (value == null) {
                value = CliSupport.readSecretValue("Environment variable value: ");
            }
            ConfigOperation.setEnv(key, value, agent);
        }
    }

    @Command(name = "remove-env", description = "Remove environment variable")
    static class RemoveEnvCommand implements Runnable {
        @Parameters(index = "0", description = "Environment variable name to remove")
        String key;

        @Option(names = {"-a", "--agent"}, description = "Agent name")
        String agent;

        @Override
        public void run() {
            ConfigOperation.removeEnv(key, agent);
        }
    }

    @Command(name = "list-env", description = "List environment variables")
    static class ListEnvCommand implements Runnable {
        @Option(names = {"-a", "--agent"}, description = "Agent name")
        String agent;

        @Override
        public void run() {
            ConfigOperation.listEnv(agent);
        }
    }
}
