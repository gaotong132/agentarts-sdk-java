package com.huaweicloud.agentarts.toolkit.commands;

import com.huaweicloud.agentarts.sdk.mcpgateway.MCPGatewayClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Map;

/**
 * MCP Gateway management subcommand group.
 *
 * <p>CLI command: manage MCP gateways and targets. Each subcommand builds an
 * {@link MCPGatewayClient} (AK/SK read from the environment via {@code Constants})
 * and prints the API result as JSON to stdout.</p>
 */
@Command(
    name = "mcp-gateway",
    aliases = {"gateway"},
    mixinStandardHelpOptions = true,
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

    @Command(name = "create-mcp-gateway", aliases = {"create"}, mixinStandardHelpOptions = true, description = "Create an MCP gateway")
    static class CreateGateway implements Runnable {
        @Option(names = {"-n", "--name"}, description = "Gateway name") String name;
        @Option(names = {"-d", "--description"}, description = "Gateway description") String description;
        @Option(names = "--protocol-type", description = "Protocol type", defaultValue = "mcp") String protocolType;
        @Option(names = "--authorizer-type", description = "Authorizer type", defaultValue = "iam") String authorizerType;
        @Option(names = "--agency-name", description = "Agency name") String agencyName;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            try (MCPGatewayClient client = new MCPGatewayClient(!skipSsl)) {
                RequestResult result = client.createMcpGateway(
                        name, description, protocolType, authorizerType, agencyName);
                if (!result.isSuccess()) {
                    CliSupport.fail("Error creating MCP gateway (HTTP "
                            + result.getStatusCode() + "): " + result.getError());
                }
                CliSupport.printJson(result.getData());
            } catch (com.huaweicloud.agentarts.sdk.core.APIException e) {
                CliSupport.fail("Error creating MCP gateway (HTTP "
                        + e.getStatusCode() + "): " + e.getMessage());
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Error creating MCP gateway: " + e.getMessage());
            }
        }
    }

    @Command(name = "update-mcp-gateway", aliases = {"update"}, mixinStandardHelpOptions = true, description = "Update an MCP gateway")
    static class UpdateGateway implements Runnable {
        @Parameters(index = "0", description = "Gateway ID") String gatewayId;
        @Option(names = {"-d", "--description"}) String description;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            try (MCPGatewayClient client = new MCPGatewayClient(!skipSsl)) {
                RequestResult result = client.updateMcpGateway(gatewayId, description);
                if (!result.isSuccess()) {
                    CliSupport.fail("Error updating MCP gateway (HTTP "
                            + result.getStatusCode() + "): " + result.getError());
                }
                CliSupport.printJson(result.getData());
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Error updating MCP gateway: " + e.getMessage());
            }
        }
    }

    @Command(name = "delete-mcp-gateway", aliases = {"delete"}, mixinStandardHelpOptions = true, description = "Delete an MCP gateway")
    static class DeleteGateway implements Runnable {
        @Parameters(index = "0", description = "Gateway ID") String gatewayId;
        @Option(names = {"-f", "--force"}, description = "Delete without confirmation") boolean force;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            if (!CliSupport.confirmDestructiveAction("delete MCP gateway '" + gatewayId + "'", force)) {
                return;
            }
            try (MCPGatewayClient client = new MCPGatewayClient(!skipSsl)) {
                RequestResult result = client.deleteMcpGateway(gatewayId);
                if (!result.isSuccess()) {
                    CliSupport.fail("Error deleting MCP gateway (HTTP "
                            + result.getStatusCode() + "): " + result.getError());
                }
                if (result.getData() != null) {
                    CliSupport.printJson(result.getData());
                } else {
                    System.out.println("Gateway deleted successfully: " + gatewayId);
                }
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Error deleting MCP gateway: " + e.getMessage());
            }
        }
    }

    @Command(name = "get-mcp-gateway", aliases = {"get"}, mixinStandardHelpOptions = true, description = "Get MCP gateway details")
    static class GetGateway implements Runnable {
        @Parameters(index = "0", description = "Gateway ID") String gatewayId;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            try (MCPGatewayClient client = new MCPGatewayClient(!skipSsl)) {
                RequestResult result = client.getMcpGateway(gatewayId);
                if (!result.isSuccess()) {
                    CliSupport.fail("Error getting MCP gateway (HTTP "
                            + result.getStatusCode() + "): " + result.getError());
                }
                CliSupport.printJson(result.getData());
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Error getting MCP gateway: " + e.getMessage());
            }
        }
    }

    @Command(name = "list-mcp-gateways", aliases = {"list"}, mixinStandardHelpOptions = true, description = "List MCP gateways")
    static class ListGateways implements Runnable {
        @Option(names = "--name") String name;
        @Option(names = "--status") String status;
        @Option(names = "--gateway-id") String gatewayId;
        @Option(names = "--limit") Integer limit;
        @Option(names = "--offset") Integer offset;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            try (MCPGatewayClient client = new MCPGatewayClient(!skipSsl)) {
                RequestResult result = client.listMcpGateways(
                        name, status, gatewayId, null, null, null, null, limit, offset);
                if (!result.isSuccess()) {
                    CliSupport.fail("Error listing MCP gateways (HTTP "
                            + result.getStatusCode() + "): " + result.getError());
                }
                CliSupport.printJson(result.getData());
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Error listing MCP gateways: " + e.getMessage());
            }
        }
    }

    @Command(name = "create-mcp-gateway-target", aliases = {"create-target"}, mixinStandardHelpOptions = true, description = "Create an MCP gateway target")
    static class CreateTarget implements Runnable {
        @Parameters(index = "0", description = "Gateway ID") String gatewayId;
        @Option(names = {"-n", "--name"}) String name;
        @Option(names = {"-d", "--description"}) String description;
        @Option(names = "--target-configuration", required = true) String targetConfig;
        @Option(names = "--credential-provider-configuration") String credProviderConfig;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            Map<String, Object> targetConfigMap = CliSupport.parseJsonMap(targetConfig, "target-configuration");
            Map<String, Object> credConfigMap = CliSupport.parseJsonMap(credProviderConfig, "credential-provider-configuration");
            try (MCPGatewayClient client = new MCPGatewayClient(!skipSsl)) {
                RequestResult result = client.createMcpGatewayTarget(
                        gatewayId, name, description, targetConfigMap, credConfigMap);
                if (!result.isSuccess()) {
                    CliSupport.fail("Error creating MCP gateway target (HTTP "
                            + result.getStatusCode() + "): " + result.getError());
                }
                CliSupport.printJson(result.getData());
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Error creating MCP gateway target: " + e.getMessage());
            }
        }
    }

    @Command(name = "update-mcp-gateway-target", aliases = {"update-target"}, mixinStandardHelpOptions = true, description = "Update an MCP gateway target")
    static class UpdateTarget implements Runnable {
        @Parameters(index = "0", description = "Gateway ID") String gatewayId;
        @Parameters(index = "1", description = "Target ID") String targetId;
        @Option(names = {"-n", "--name"}) String name;
        @Option(names = {"-d", "--description"}) String description;
        @Option(names = "--target-configuration") String targetConfig;
        @Option(names = "--credential-provider-configuration") String credProviderConfig;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            Map<String, Object> targetConfigMap = CliSupport.parseJsonMap(targetConfig, "target-configuration");
            Map<String, Object> credConfigMap = CliSupport.parseJsonMap(credProviderConfig, "credential-provider-configuration");
            try (MCPGatewayClient client = new MCPGatewayClient(!skipSsl)) {
                RequestResult result = client.updateMcpGatewayTarget(
                        gatewayId, targetId, name, description, targetConfigMap, credConfigMap);
                if (!result.isSuccess()) {
                    CliSupport.fail("Error updating MCP gateway target (HTTP "
                            + result.getStatusCode() + "): " + result.getError());
                }
                CliSupport.printJson(result.getData());
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Error updating MCP gateway target: " + e.getMessage());
            }
        }
    }

    @Command(name = "delete-mcp-gateway-target", aliases = {"delete-target"}, mixinStandardHelpOptions = true, description = "Delete an MCP gateway target")
    static class DeleteTarget implements Runnable {
        @Parameters(index = "0", description = "Gateway ID") String gatewayId;
        @Parameters(index = "1", description = "Target ID") String targetId;
        @Option(names = {"-f", "--force"}, description = "Delete without confirmation") boolean force;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            if (!CliSupport.confirmDestructiveAction("delete MCP gateway target '" + targetId + "'", force)) {
                return;
            }
            try (MCPGatewayClient client = new MCPGatewayClient(!skipSsl)) {
                RequestResult result = client.deleteMcpGatewayTarget(gatewayId, targetId);
                if (!result.isSuccess()) {
                    CliSupport.fail("Error deleting MCP gateway target (HTTP "
                            + result.getStatusCode() + "): " + result.getError());
                }
                if (result.getData() != null) {
                    CliSupport.printJson(result.getData());
                } else {
                    System.out.println("Target deleted successfully: " + targetId);
                }
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Error deleting MCP gateway target: " + e.getMessage());
            }
        }
    }

    @Command(name = "get-mcp-gateway-target", aliases = {"get-target"}, mixinStandardHelpOptions = true, description = "Get MCP gateway target details")
    static class GetTarget implements Runnable {
        @Parameters(index = "0", description = "Gateway ID") String gatewayId;
        @Parameters(index = "1", description = "Target ID") String targetId;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            try (MCPGatewayClient client = new MCPGatewayClient(!skipSsl)) {
                RequestResult result = client.getMcpGatewayTarget(gatewayId, targetId);
                if (!result.isSuccess()) {
                    CliSupport.fail("Error getting MCP gateway target (HTTP "
                            + result.getStatusCode() + "): " + result.getError());
                }
                CliSupport.printJson(result.getData());
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Error getting MCP gateway target: " + e.getMessage());
            }
        }
    }

    @Command(name = "list-mcp-gateway-targets", aliases = {"list-targets"}, mixinStandardHelpOptions = true, description = "List MCP gateway targets")
    static class ListTargets implements Runnable {
        @Parameters(index = "0", description = "Gateway ID") String gatewayId;
        @Option(names = "--limit") Integer limit;
        @Option(names = "--offset") Integer offset;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            try (MCPGatewayClient client = new MCPGatewayClient(!skipSsl)) {
                RequestResult result = client.listMcpGatewayTargets(gatewayId, limit, offset);
                if (!result.isSuccess()) {
                    CliSupport.fail("Error listing MCP gateway targets (HTTP "
                            + result.getStatusCode() + "): " + result.getError());
                }
                CliSupport.printJson(result.getData());
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Error listing MCP gateway targets: " + e.getMessage());
            }
        }
    }
}
