package com.huaweicloud.agentarts.toolkit.commands;

import com.huaweicloud.agentarts.toolkit.operations.DestroyOperation;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Destroy agent from Huawei Cloud.
 *
 * <p>Mirrors Python {@code destroy} command from {@code cli/runtime/destroy.py}.</p>
 */
@Command(name = "destroy", description = "Destroy agent from Huawei Cloud")
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
        if (!skipConfirm) {
            System.out.print("Are you sure you want to destroy agent '" + agentName + "'? [y/N]: ");
            try {
                String answer = System.console().readLine();
                if (answer == null || !answer.toLowerCase().startsWith("y")) {
                    System.out.println("Aborted.");
                    return;
                }
            } catch (Exception e) {
                // Non-interactive mode, proceed
            }
        }
        try {
            DestroyOperation.destroyAgent(agentName, region, skipSsl);
            System.out.println("Agent '" + agentName + "' destroyed successfully.");
        } catch (Exception e) {
            System.err.println("Error destroying agent: " + e.getMessage());
        }
    }
}
