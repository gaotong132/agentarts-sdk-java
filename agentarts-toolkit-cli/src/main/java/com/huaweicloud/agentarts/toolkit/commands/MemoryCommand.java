package com.huaweicloud.agentarts.toolkit.commands;

import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.sdk.memory.model.SpaceInfo;
import com.huaweicloud.agentarts.sdk.memory.model.SpaceListResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Memory space management subcommand group.
 *
 * <p>CLI command: manage memory spaces. Each subcommand builds a
 * {@link MemoryClient} (control-plane AK/SK read from the environment via
 * {@code Constants}) and prints the result as JSON to stdout when
 * {@code --output json} is set.</p>
 */
@Command(
    name = "memory",
    mixinStandardHelpOptions = true,
    description = "Memory space management",
    subcommands = {
        MemoryCommand.Create.class,
        MemoryCommand.Get.class,
        MemoryCommand.List.class,
        MemoryCommand.Update.class,
        MemoryCommand.Delete.class,
        MemoryCommand.Status.class
    }
)
public class MemoryCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Use 'agentarts memory --help' for available subcommands.");
    }

    private static boolean isJson(String output) {
        return output != null && "json".equalsIgnoreCase(output.trim());
    }

    private static java.util.List<String> parseStrategies(String raw) {
        if (raw == null) return null;
        java.util.List<String> parsed = new ArrayList<>();
        for (String value : raw.split(",", -1)) {
            String strategy = value.trim();
            if (strategy.isEmpty()) {
                CliSupport.fail("Invalid memory strategies: values must be non-empty");
            }
            parsed.add(strategy);
        }
        return parsed;
    }

    private static java.util.List<Map<String, String>> parseTags(String raw) {
        if (raw == null) return null;
        java.util.List<Map<String, String>> parsed = new ArrayList<>();
        for (String pair : raw.split(",", -1)) {
            int separator = pair.indexOf('=');
            if (separator < 1) {
                CliSupport.fail("Invalid tags: use key=value pairs separated by commas");
            }
            String key = pair.substring(0, separator).trim();
            String value = pair.substring(separator + 1).trim();
            if (key.isEmpty()) {
                CliSupport.fail("Invalid tags: tag keys must be non-empty");
            }
            Map<String, String> tag = new LinkedHashMap<>();
            tag.put("key", key);
            tag.put("value", value);
            parsed.add(tag);
        }
        return parsed;
    }

    private static void validateTtl(Integer ttl) {
        if (ttl != null && (ttl < 1 || ttl > 8760)) {
            CliSupport.fail("Message TTL must be between 1 and 8760 hours");
        }
    }

    @Command(name = "create", mixinStandardHelpOptions = true, description = "Create a Memory Space")
    static class Create implements Runnable {
        @Parameters(index = "0", description = "Space name (1-128 chars)") String name;
        @Option(names = {"-t", "--ttl"}, description = "Message TTL in hours (1-8760)", defaultValue = "168") int ttl;
        @Option(names = {"-d", "--description"}) String description;
        @Option(names = "--extract-idle") Integer extractIdle;
        @Option(names = "--extract-tokens") Integer extractTokens;
        @Option(names = "--extract-messages") Integer extractMessages;
        @Option(names = {"-s", "--strategies"}, description = "Built-in strategies (comma-separated)") String strategies;
        @Option(names = "--tags", description = "Tags (key1=value1,key2=value2)") String tags;
        @Option(names = "--public", description = "Enable public access", defaultValue = "true") boolean publicAccess;
        @Option(names = "--vpc-id") String vpcId;
        @Option(names = "--subnet-id") String subnetId;
        @Option(names = {"-r", "--region"}) String region;
        @Option(names = {"-o", "--output"}, description = "Output format: table, json", defaultValue = "table") String output;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            if (name == null || name.isBlank() || name.trim().length() > 128) {
                CliSupport.fail("Space name must contain between 1 and 128 characters");
            }
            validateTtl(ttl);
            if ((vpcId == null) != (subnetId == null)
                    || (vpcId != null && (vpcId.isBlank() || subnetId.isBlank()))) {
                CliSupport.fail("Both non-empty VPC ID and subnet ID are required for private access");
            }
            java.util.List<String> parsedStrategies = parseStrategies(strategies);
            java.util.List<Map<String, String>> parsedTags = parseTags(tags);
            try (MemoryClient client = new MemoryClient(region, null, !skipSsl)) {
                SpaceInfo space = client.createSpace(
                        name.trim(),
                        ttl,
                        description,
                        parsedTags,
                        extractIdle,
                        extractTokens,
                        extractMessages,
                        publicAccess,
                        vpcId == null ? null : vpcId.trim(),
                        subnetId == null ? null : subnetId.trim(),
                        parsedStrategies,
                        null);
                if (isJson(output)) {
                    CliSupport.printJson(space);
                } else {
                    System.out.println("Space created successfully!");
                    System.out.println("  Space ID: " + space.getId());
                    System.out.println("  Status: " + (space.getStatus() != null ? space.getStatus() : "N/A"));
                }
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Failed to create memory space: " + e.getMessage());
            }
        }
    }

    @Command(name = "get", mixinStandardHelpOptions = true, description = "Get Space details")
    static class Get implements Runnable {
        @Parameters(index = "0", description = "Space ID") String spaceId;
        @Option(names = {"-r", "--region"}) String region;
        @Option(names = {"-o", "--output"}, defaultValue = "table") String output;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            try (MemoryClient client = new MemoryClient(region, null, !skipSsl)) {
                SpaceInfo space = client.getSpace(spaceId);
                if (isJson(output)) {
                    CliSupport.printJson(space);
                } else {
                    System.out.println("Space Details");
                    System.out.println("  Space ID: " + (space.getId() != null ? space.getId() : spaceId));
                    System.out.println("  Status: " + (space.getStatus() != null ? space.getStatus() : "N/A"));
                    System.out.println("  Message TTL: " + space.getMessageTtlHours() + " hours");
                }
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Failed to get memory space: " + e.getMessage());
            }
        }
    }

    @Command(name = "list", mixinStandardHelpOptions = true, description = "List Memory Spaces")
    static class List implements Runnable {
        @Option(names = {"-l", "--limit"}, defaultValue = "20") int limit;
        @Option(names = "--offset", defaultValue = "0") int offset;
        @Option(names = {"-r", "--region"}) String region;
        @Option(names = {"-o", "--output"}, defaultValue = "table") String output;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            try (MemoryClient client = new MemoryClient(region, null, !skipSsl)) {
                SpaceListResponse response = client.listSpaces(limit, offset);
                if (isJson(output)) {
                    CliSupport.printJson(response);
                } else {
                    int total = response.getTotal() != 0 ? response.getTotal()
                            : (response.getItems() != null ? response.getItems().size() : 0);
                    System.out.println("Spaces (Total: " + total + ")");
                    if (response.getItems() != null) {
                        for (SpaceInfo space : response.getItems()) {
                            System.out.println("  " + (space.getId() != null ? space.getId() : "N/A")
                                    + "  " + (space.getStatus() != null ? space.getStatus() : "N/A"));
                        }
                    }
                }
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Failed to list memory spaces: " + e.getMessage());
            }
        }
    }

    @Command(name = "update", mixinStandardHelpOptions = true, description = "Update a Memory Space")
    static class Update implements Runnable {
        @Parameters(index = "0", description = "Space ID") String spaceId;
        @Option(names = {"-t", "--ttl"}) Integer ttl;
        @Option(names = {"-d", "--description"}) String description;
        @Option(names = "--extract-idle") Integer extractIdle;
        @Option(names = "--extract-tokens") Integer extractTokens;
        @Option(names = "--extract-messages") Integer extractMessages;
        @Option(names = {"-s", "--strategies"}) String strategies;
        @Option(names = "--tags") String tags;
        @Option(names = {"-r", "--region"}) String region;
        @Option(names = {"-o", "--output"}, defaultValue = "table") String output;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            validateTtl(ttl);
            java.util.List<String> parsedStrategies = parseStrategies(strategies);
            java.util.List<Map<String, String>> parsedTags = parseTags(tags);
            try (MemoryClient client = new MemoryClient(region, null, !skipSsl)) {
                SpaceInfo space = client.updateSpace(
                        spaceId,
                        null,
                        description,
                        ttl,
                        null,
                        extractIdle,
                        extractTokens,
                        extractMessages,
                        parsedTags,
                        parsedStrategies);
                if (isJson(output)) {
                    Map<String, Object> wrapper = new LinkedHashMap<>();
                    wrapper.put("space_id", spaceId);
                    wrapper.put("space", space);
                    CliSupport.printJson(wrapper);
                } else {
                    System.out.println("Space updated successfully!");
                    System.out.println("  Space ID: " + spaceId);
                }
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Failed to update memory space: " + e.getMessage());
            }
        }
    }

    @Command(name = "delete", mixinStandardHelpOptions = true, description = "Delete a Memory Space")
    static class Delete implements Runnable {
        @Parameters(index = "0", description = "Space ID") String spaceId;
        @Option(names = {"-r", "--region"}) String region;
        @Option(names = {"-f", "--force"}, description = "Force deletion without confirmation") boolean force;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            if (!CliSupport.confirmDestructiveAction("delete memory space '" + spaceId + "'", force)) {
                return;
            }
            try (MemoryClient client = new MemoryClient(region, null, !skipSsl)) {
                client.deleteSpace(spaceId);
                System.out.println("Space deleted successfully!");
                System.out.println("  Space ID: " + spaceId);
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Failed to delete memory space: " + e.getMessage());
            }
        }
    }

    @Command(name = "status", mixinStandardHelpOptions = true, description = "Check Space status")
    static class Status implements Runnable {
        @Parameters(index = "0", description = "Space ID") String spaceId;
        @Option(names = {"-r", "--region"}) String region;
        @Option(names = {"-o", "--output"}, defaultValue = "table") String output;
        @Option(names = {"-k", "--skip-ssl-verification"}) boolean skipSsl;

        @Override public void run() {
            try (MemoryClient client = new MemoryClient(region, null, !skipSsl)) {
                SpaceInfo space = client.getSpace(spaceId);
                if (isJson(output)) {
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("space_id", spaceId);
                    Map<String, Object> statusAvailable = new LinkedHashMap<>();
                    statusAvailable.put("status", space.getStatus() != null ? space.getStatus() : "unknown");
                    statusAvailable.put("message_ttl_hours", space.getMessageTtlHours());
                    statusAvailable.put("memory_extract_enabled", space.isMemoryExtractEnabled());
                    out.put("status_available", statusAvailable);
                    out.put("health_status", computeHealth(space.getStatus()));
                    CliSupport.printJson(out);
                } else {
                    System.out.println("Memory Space Status");
                    System.out.println("  Space ID: " + spaceId);
                    System.out.println("  Status: " + (space.getStatus() != null ? space.getStatus() : "unknown"));
                    System.out.println("  Health: " + computeHealth(space.getStatus()));
                }
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Failed to get memory space status: " + e.getMessage());
            }
        }

        private static String computeHealth(String status) {
            if (status == null) return "unknown";
            String s = status.toUpperCase();
            if (s.equals("ERROR") || s.equals("FAILED")) return "error";
            if (s.equals("STOPPED") || s.equals("INACTIVE")) return "warning";
            if (s.equals("ACTIVE") || s.equals("RUNNING") || s.equals("READY")) return "healthy";
            return "unknown";
        }
    }
}
