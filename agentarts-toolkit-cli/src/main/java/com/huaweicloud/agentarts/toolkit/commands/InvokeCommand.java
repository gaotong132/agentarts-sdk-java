package com.huaweicloud.agentarts.toolkit.commands;

import com.huaweicloud.agentarts.toolkit.operations.InvokeOperation;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Invoke agent with JSON payload.
 *
 * <p>CLI command: invoke a deployed agent.
 * Registered at both top-level and under 'runtime' subcommand.</p>
 */
@Command(name = "invoke", mixinStandardHelpOptions = true,
        description = "Invoke agent with JSON payload")
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

    @Option(names = {"-bt", "--bearer-token"}, arity = "0..1", interactive = true,
            description = "Bearer token (omit value for hidden prompt; defaults to BEARER_TOKEN)")
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
        String resolvedBearerToken = CliSupport.resolveBearerToken(bearerToken);
        try {
            InvokeOperation.invokeAgent(payload, agentName, mode, region, port, endpoint,
                    sessionId, resolvedBearerToken, timeout, skipSsl, userId, customPath);
        } catch (CliSupport.CliFailure e) {
            throw e;
        } catch (Exception e) {
            CliSupport.fail("Error invoking agent: " + e.getMessage());
        }
    }
}
