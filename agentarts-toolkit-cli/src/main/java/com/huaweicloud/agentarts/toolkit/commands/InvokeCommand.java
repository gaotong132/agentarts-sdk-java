package com.huaweicloud.agentarts.toolkit.commands;

import com.huaweicloud.agentarts.toolkit.operations.InvokeOperation;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Invoke agent with JSON payload.
 *
 * <p>Mirrors Python {@code invoke} command from {@code cli/runtime/invoke.py}.
 * Registered at both top-level and under 'runtime' subcommand.</p>
 */
@Command(name = "invoke", description = "Invoke agent with JSON payload")
public class InvokeCommand implements Runnable {

    @Parameters(index = "0", description = "JSON payload (e.g., '{\"message\": \"hello\"}')")
    String payload;

    @Option(names = {"-a", "--agent"}, description = "Agent name")
    String agentName;

    @Option(names = {"-m", "--mode"}, description = "Invoke mode: 'local' or 'cloud'", defaultValue = "cloud")
    String mode;

    @Option(names = {"-r", "--region"}, description = "Huawei Cloud region (cloud mode)")
    String region;

    @Option(names = {"-p", "--port"}, description = "Local port (local mode, default: 8080)")
    Integer port;

    @Option(names = {"-e", "--endpoint"}, description = "Endpoint name")
    String endpoint;

    @Option(names = {"-s", "--session"}, description = "Session ID for stateful agents")
    String sessionId;

    @Option(names = {"-bt", "--bearer-token"}, description = "Bearer token for authentication")
    String bearerToken;

    @Option(names = "--timeout", description = "Request timeout in seconds", defaultValue = "900")
    int timeout;

    @Option(names = {"-k", "--skip-ssl-verification"}, description = "Skip SSL certificate verification")
    boolean skipSsl;

    @Option(names = {"-u", "--user-id"}, description = "User ID for OAuth2 outbound credentials")
    String userId;

    @Option(names = "--custom-path", description = "Custom path appended to /invocations")
    String customPath;

    @Override
    public void run() {
        try {
            InvokeOperation.invokeAgent(payload, agentName, mode, region, port, endpoint,
                    sessionId, bearerToken, timeout, skipSsl, userId, customPath);
        } catch (Exception e) {
            System.err.println("Error invoking agent: " + e.getMessage());
        }
    }
}
