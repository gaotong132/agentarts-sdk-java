package com.huaweicloud.agentarts.toolkit.commands;

import com.huaweicloud.agentarts.toolkit.operations.DevOperation;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Run local development server.
 *
 * <p>CLI command: start local development server.</p>
 */
@Command(name = "dev", mixinStandardHelpOptions = true,
        description = "Run local development server")
public class DevCommand implements Runnable {

    @Option(names = {"-p", "--port"}, description = "Server port", defaultValue = "8080")
    int port;

    @Option(names = {"-H", "--host"}, description = "Server host", defaultValue = "0.0.0.0")
    String host;

    @Option(names = "--reload", description = "Enable auto-reload")
    boolean reload;

    @Option(names = {"-c", "--config"}, description = "Configuration file path")
    String configPath;

    @Option(names = "--path", description = "Project path containing .agentarts_config.yaml")
    String projectPath;

    @Option(names = {"-e", "--env"}, description = "Environment variables (KEY=VALUE). Repeatable.")
    String[] envVars;

    @Override
    public void run() {
        try {
            DevOperation.runDevServer(port, host, reload, configPath, projectPath, envVars);
        } catch (Exception e) {
            CliSupport.fail("Error starting dev server: " + e.getMessage());
        }
    }
}
