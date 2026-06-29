package com.huaweicloud.agentarts.toolkit.commands;

import com.huaweicloud.agentarts.toolkit.operations.DevOperation;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Run local development server.
 *
 * <p>Mirrors Python {@code dev} command from {@code cli/runtime/dev.py}.</p>
 */
@Command(name = "dev", description = "Run local development server")
public class DevCommand implements Runnable {

    @Option(names = {"-p", "--port"}, description = "Server port", defaultValue = "8080")
    int port;

    @Option(names = {"-h", "--host"}, description = "Server host", defaultValue = "0.0.0.0")
    String host;

    @Option(names = "--reload", description = "Enable auto-reload")
    boolean reload;

    @Option(names = {"-c", "--config"}, description = "Configuration file path")
    String configPath;

    @Option(names = {"-e", "--env"}, description = "Environment variables (KEY=VALUE). Repeatable.")
    String[] envVars;

    @Override
    public void run() {
        try {
            DevOperation.runDevServer(port, host, reload, configPath, envVars);
        } catch (Exception e) {
            System.err.println("Error starting dev server: " + e.getMessage());
        }
    }
}
