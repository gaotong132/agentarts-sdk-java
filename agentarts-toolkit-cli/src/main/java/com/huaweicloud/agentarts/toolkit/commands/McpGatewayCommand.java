package com.huaweicloud.agentarts.toolkit.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * MCP Gateway management subcommand group.
 *
 * <p>Mirrors Python {@code mcp-gateway} sub-app with 10 CRUD commands.</p>
 */
@Command(
    name = "mcp-gateway",
    description = "MCP Gateway management",
    subcommands = {
        McpGatewayCommand.CreateGateway.class,
        McpGatewayCommand.UpdateGateway.class,
        McpGatewayCommand.DeleteGateway.class,
        McpGatewayCommand.GetGateway.class,
        McpGatewayCommand.ListGateways.class,
        McpGatewayCommand.CreateTarget.class,
        McpGatewayCommand.UpdateTarget.class,
        McpGatewayCommand.DeleteTarget.class,
        McpGatewayCommand.GetTarget.class,
        McpGatewayCommand.ListTargets.class
    }
)
public class McpGatewayCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Use 'agentarts mcp-gateway --help' for available subcommands.");
    }

    @Command(name = "create-mcp-gateway", description = "Create an MCP gateway")
    static class CreateGateway implements Runnable {
        @Option(names = {"-n", "--name"}, description = "Gateway name") String name;
        @Option(names = {"-d", "--description"}, description = "Gateway description") String description;
        @Option(names = "--protocol-type", description = "Protocol type", defaultValue = "mcp") String protocolType;
        @Option(names = "--authorizer-type", description = "Authorizer type", defaultValue = "iam") String authorizerType;
        @Option(names = "--agency-name", description = "Agency name") String agencyName;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            System.out.println("Creating MCP gateway '" + name + "'...");
        }
    }

    @Command(name = "update-mcp-gateway", description = "Update an MCP gateway")
    static class UpdateGateway implements Runnable {
        @Parameters(index = "0", description = "Gateway ID") String gatewayId;
        @Option(names = {"-d", "--description"}) String description;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            System.out.println("Updating MCP gateway '" + gatewayId + "'...");
        }
    }

    @Command(name = "delete-mcp-gateway", description = "Delete an MCP gateway")
    static class DeleteGateway implements Runnable {
        @Parameters(index = "0", description = "Gateway ID") String gatewayId;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            System.out.println("Deleting MCP gateway '" + gatewayId + "'...");
        }
    }

    @Command(name = "get-mcp-gateway", description = "Get MCP gateway details")
    static class GetGateway implements Runnable {
        @Parameters(index = "0", description = "Gateway ID") String gatewayId;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            System.out.println("Getting MCP gateway '" + gatewayId + "'...");
        }
    }

    @Command(name = "list-mcp-gateways", description = "List MCP gateways")
    static class ListGateways implements Runnable {
        @Option(names = "--name") String name;
        @Option(names = "--status") String status;
        @Option(names = "--gateway-id") String gatewayId;
        @Option(names = "--limit") Integer limit;
        @Option(names = "--offset") Integer offset;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            System.out.println("Listing MCP gateways...");
        }
    }

    @Command(name = "create-mcp-gateway-target", description = "Create an MCP gateway target")
    static class CreateTarget implements Runnable {
        @Parameters(index = "0", description = "Gateway ID") String gatewayId;
        @Option(names = {"-n", "--name"}) String name;
        @Option(names = {"-d", "--description"}) String description;
        @Option(names = "--target-configuration", required = true) String targetConfig;
        @Option(names = "--credential-provider-configuration") String credProviderConfig;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            System.out.println("Creating target for gateway '" + gatewayId + "'...");
        }
    }

    @Command(name = "update-mcp-gateway-target", description = "Update an MCP gateway target")
    static class UpdateTarget implements Runnable {
        @Parameters(index = "0", description = "Gateway ID") String gatewayId;
        @Parameters(index = "1", description = "Target ID") String targetId;
        @Option(names = {"-n", "--name"}) String name;
        @Option(names = {"-d", "--description"}) String description;
        @Option(names = "--target-configuration") String targetConfig;
        @Option(names = "--credential-provider-configuration") String credProviderConfig;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            System.out.println("Updating target '" + targetId + "' for gateway '" + gatewayId + "'...");
        }
    }

    @Command(name = "delete-mcp-gateway-target", description = "Delete an MCP gateway target")
    static class DeleteTarget implements Runnable {
        @Parameters(index = "0", description = "Gateway ID") String gatewayId;
        @Parameters(index = "1", description = "Target ID") String targetId;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            System.out.println("Deleting target '" + targetId + "'...");
        }
    }

    @Command(name = "get-mcp-gateway-target", description = "Get MCP gateway target details")
    static class GetTarget implements Runnable {
        @Parameters(index = "0", description = "Gateway ID") String gatewayId;
        @Parameters(index = "1", description = "Target ID") String targetId;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            System.out.println("Getting target '" + targetId + "'...");
        }
    }

    @Command(name = "list-mcp-gateway-targets", description = "List MCP gateway targets")
    static class ListTargets implements Runnable {
        @Parameters(index = "0", description = "Gateway ID") String gatewayId;
        @Option(names = "--limit") Integer limit;
        @Option(names = "--offset") Integer offset;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            System.out.println("Listing targets for gateway '" + gatewayId + "'...");
        }
    }
}
