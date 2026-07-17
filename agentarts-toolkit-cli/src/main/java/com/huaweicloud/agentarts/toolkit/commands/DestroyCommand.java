package com.huaweicloud.agentarts.toolkit.commands;

import com.huaweicloud.agentarts.toolkit.operations.DestroyOperation;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Destroy agent from Huawei Cloud.
 *
 * <p>CLI command: destroy a deployed agent.</p>
 */
@Command(name = "destroy", mixinStandardHelpOptions = true,
        description = "Destroy agent from Huawei Cloud")
public class DestroyCommand implements Runnable {

    @Option(names = {"-a", "--agent"}, description = "Agent name to destroy")
    String agentName;

    @Option(names = {"-r", "--region"}, description = "Huawei Cloud region")
    String region;

    @Option(names = {"-y", "--yes"}, description = "Skip confirmation prompt")
    boolean skipConfirm;

    @Option(names = {"-k", "--skip-ssl-verification"}, description = "Skip SSL certificate verification")
    boolean skipSsl;

    @Override
    public void run() {
        String target = agentName == null ? "the configured agent" : "agent '" + agentName + "'";
        if (!CliSupport.confirmDestructiveAction("destroy " + target, skipConfirm)) {
            return;
        }
        try {
            DestroyOperation.destroyAgent(agentName, region, skipSsl);
        } catch (Exception e) {
            CliSupport.fail("Error destroying agent: " + e.getMessage());
        }
    }
}
