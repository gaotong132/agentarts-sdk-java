package com.huaweicloud.agentarts.toolkit.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Shared helpers for CLI command implementations: JSON output and error handling.
 *
 * <p>Commands print results as pretty-printed JSON to stdout (matching the
 * reference CLI output format) and surface failures by printing to stderr and
 * raising a {@link CliFailure} (a {@code RuntimeException}). The {@code CommandLine}
 * is configured with an execution-exception handler that turns a {@link CliFailure}
 * into a non-zero exit code without dumping a stack trace.</p>
 */
public final class CliSupport {

    private CliSupport() {}

    /** Shared {@link ObjectMapper} (use the SDK singleton, never create new instances). */
    static final ObjectMapper MAPPER = JsonUtils.MAPPER;

    /**
     * Serialize and print a value as pretty JSON to stdout. Falls back to a
     * plain string representation if serialization fails.
     */
    public static void printJson(Object data) {
        if (data == null) {
            System.out.println("{}");
            return;
        }
        try {
            JsonNode redacted = redactSensitiveValues(MAPPER.valueToTree(data));
            System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(redacted));
        } catch (Exception e) {
            fail("Unable to serialize CLI output safely");
        }
    }

    /** Return a deep copy with credential-like fields replaced by a fixed marker. */
    public static JsonNode redactSensitiveValues(JsonNode source) {
        if (source == null) return MAPPER.nullNode();
        if (source.isObject()) {
            ObjectNode result = MAPPER.createObjectNode();
            source.properties().forEach(entry -> result.set(
                    entry.getKey(),
                    isSensitiveName(entry.getKey())
                            ? MAPPER.getNodeFactory().textNode("[REDACTED]")
                            : redactSensitiveValues(entry.getValue())));
            return result;
        }
        if (source.isArray()) {
            ArrayNode result = MAPPER.createArrayNode();
            source.forEach(value -> result.add(redactSensitiveValues(value)));
            return result;
        }
        return source.deepCopy();
    }

    /** Whether a field or environment-variable name is likely to contain credentials. */
    public static boolean isSensitiveName(String name) {
        if (name == null) return false;
        String normalized = name.toUpperCase(java.util.Locale.ROOT).replace('-', '_');
        return normalized.contains("PASSWORD")
                || normalized.contains("SECRET")
                || normalized.contains("TOKEN")
                || normalized.contains("API_KEY")
                || normalized.contains("ACCESS_KEY")
                || normalized.contains("PRIVATE_KEY")
                || normalized.contains("AUTHORIZATION")
                || normalized.contains("CREDENTIAL");
    }

    /** Resolve an explicitly supplied token, falling back to the reference CLI environment variable. */
    public static String resolveBearerToken(String explicitToken) {
        if (explicitToken != null && !explicitToken.isBlank()) {
            return explicitToken;
        }
        String environmentToken = System.getenv("BEARER_TOKEN");
        return environmentToken == null || environmentToken.isBlank() ? null : environmentToken;
    }

    /**
     * Require explicit confirmation before a destructive operation. An EOF or
     * unreadable stdin is treated as a failure, so non-interactive callers must
     * deliberately supply the command's force flag.
     */
    public static boolean confirmDestructiveAction(String description, boolean force) {
        if (force) return true;
        System.out.print("Are you sure you want to " + description + "? [y/N]: ");
        System.out.flush();
        final String answer;
        try {
            answer = new BufferedReader(new InputStreamReader(
                    System.in, StandardCharsets.UTF_8)).readLine();
        } catch (IOException e) {
            fail("Unable to read confirmation; use --force for non-interactive execution");
            return false; // unreachable
        }
        if (answer == null) {
            fail("Confirmation required; use --force for non-interactive execution");
        }
        if (!"y".equalsIgnoreCase(answer.trim()) && !"yes".equalsIgnoreCase(answer.trim())) {
            System.out.println("Aborted.");
            return false;
        }
        return true;
    }

    /**
     * Print an error message to stderr and raise a {@link CliFailure} so the
     * CLI exits non-zero. The {@code CommandLine} execution-exception handler
     * turns this into exit code 1 without a stack trace.
     */
    static void fail(String message) {
        System.err.println(message);
        throw new CliFailure(message);
    }

    /**
     * Print an error message to stderr and raise a {@link CliFailure} so the
     * CLI exits non-zero. Public for cross-package operations that drive the CLI
     * flow but live outside {@code commands}.
     */
    public static void failCli(String message) {
        fail(message);
    }

    /**
     * Parse a JSON string into a {@code Map<String,Object>}. Returns {@code null}
     * when the input is blank. On invalid JSON, prints an error and raises
     * {@link CliFailure}.
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> parseJsonMap(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, Map.class);
        } catch (Exception e) {
            fail("Invalid JSON for " + fieldName + ": " + e.getMessage());
            return null; // unreachable — fail() throws
        }
    }

    /**
     * Configure a {@link CommandLine} so a {@link CliFailure} raised from a
     * command body produces exit code 1 without a noisy stack trace (the
     * message is already written to stderr by {@link #fail(String)}).
     */
    public static CommandLine withCleanExit(CommandLine cli) {
        cli.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            if (ex instanceof CliFailure) {
                return 1;
            }
            // Unexpected error: surface the message, then exit non-zero.
            System.err.println("Error: " + ex.getMessage());
            return 1;
        });
        return cli;
    }

    /** Unchecked exception carrying a CLI failure (caught by the exit handler). */
    static final class CliFailure extends RuntimeException {
        CliFailure(String message) {
            super(message);
        }
    }
}
