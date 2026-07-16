package com.huaweicloud.agentarts.toolkit.commands;

import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;
import com.huaweicloud.agentarts.sdk.service.runtime.RuntimeClient;
import com.huaweicloud.agentarts.toolkit.operations.RuntimeResolver;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cloud runtime operations subcommand group.
 *
 * <p>CLI command: manage runtime sessions and file transfers.
 * Subcommands: invoke, exec-command, upload-files, download-files, start-session, stop-session.
 * Each subcommand builds a {@link RuntimeClient} and prints the result as JSON to stdout.</p>
 */
@Command(
    name = "runtime",
    description = "Cloud runtime operations",
    subcommands = {
        RuntimeCommand.RuntimeInvokeCommand.class,
        RuntimeCommand.ExecCommandCommand.class,
        RuntimeCommand.UploadFilesCommand.class,
        RuntimeCommand.DownloadFilesCommand.class,
        RuntimeCommand.StartSessionCommand.class,
        RuntimeCommand.StopSessionCommand.class
    }
)
public class RuntimeCommand implements Runnable {

    private static final long MAX_UPLOAD_FILE_BYTES = 64L * 1024 * 1024;
    private static final long MAX_UPLOAD_TOTAL_BYTES = 128L * 1024 * 1024;

    @Override
    public void run() {
        System.out.println("Use 'agentarts runtime --help' for available subcommands.");
    }

    @Command(name = "invoke", description = "Invoke agent via runtime")
    static class RuntimeInvokeCommand extends InvokeCommand {}

    @Command(name = "exec-command", description = "Execute command with streaming response")
    static class ExecCommandCommand implements Runnable {
        @Parameters(index = "0", description = "Command to execute (e.g., 'ls -la')")
        String command;

        @Option(names = {"-a", "--agent"}, description = "Agent name", required = true)
        String agentName;

        @Option(names = {"-s", "--session"}, description = "Session ID")
        String sessionId;

        @Option(names = "--chunked", description = "Enable chunked streaming (ndjson)")
        boolean chunked;

        @Option(names = {"-bt", "--bearer-token"}, arity = "0..1", interactive = true,
                description = "Bearer token (omit value for hidden prompt; defaults to BEARER_TOKEN)")
        String bearerToken;

        @Option(names = {"-r", "--region"}, description = "Region name")
        String region;

        @Option(names = {"-e", "--endpoint"}, description = "Endpoint name")
        String endpoint;

        @Option(names = {"-k", "--skip-ssl-verification"}, description = "Skip SSL verification")
        boolean skipSsl;

        @Option(names = {"-u", "--user-id"}, description = "User ID for OAuth2 outbound")
        String userId;

        @Option(names = "--timeout", description = "Timeout in seconds (max: 300)", defaultValue = "60")
        int timeout;

        @Override
        public void run() {
            String resolvedBearerToken = CliSupport.resolveBearerToken(bearerToken);
            List<String> commandArray = CliSupport.parseCommandArguments(command);
            if (timeout < 1 || timeout > 300) {
                CliSupport.fail("timeout must be between 1 and 300 seconds");
            }
            try (RuntimeClient client = RuntimeResolver.resolve(
                    agentName, region, !skipSsl, resolvedBearerToken, endpoint)) {
                if (JsonUtils.isNotBlank(resolvedBearerToken)) {
                    client.setAuthToken(resolvedBearerToken);
                }
                Map<String, Object> result = client.execCommand(
                        agentName, sessionId, commandArray, chunked,
                        resolvedBearerToken, endpoint, userId, timeout);
                CliSupport.printJson(result);
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Failed to execute command: " + e.getMessage());
            }
        }
    }

    @Command(name = "upload-files", description = "Upload files to runtime")
    static class UploadFilesCommand implements Runnable {
        @Option(names = {"-a", "--agent"}, description = "Agent name", required = true)
        String agentName;

        @Option(names = {"-s", "--session"}, description = "Session ID", required = true)
        String sessionId;

        @Option(names = {"-f", "--files"}, description = "Local file paths. Repeatable.", required = true)
        String[] files;

        @Option(names = {"-p", "--path"}, description = "Remote directory path", defaultValue = "/home/user/")
        String remotePath;

        @Option(names = {"-bt", "--bearer-token"}, arity = "0..1", interactive = true,
                description = "Bearer token (omit value for hidden prompt; defaults to BEARER_TOKEN)")
        String bearerToken;

        @Option(names = {"-r", "--region"}, description = "Region name")
        String region;

        @Option(names = {"-k", "--skip-ssl-verification"}, description = "Skip SSL verification")
        boolean skipSsl;

        @Option(names = "--timeout", description = "Timeout in seconds", defaultValue = "900")
        int timeout;

        @Override
        public void run() {
            String resolvedBearerToken = CliSupport.resolveBearerToken(bearerToken);
            if (timeout < 1) {
                CliSupport.fail("timeout must be greater than zero");
            }
            // Read each local file into memory and carry its bytes + filename.
            // RuntimeClient.uploadFiles streams single-file uploads as
            // application/octet-stream (raw bytes) and multi-file uploads as
            // multipart/form-data, matching the reference CLI's wire format.
            List<Map<String, Object>> fileMaps = new ArrayList<>();
            long totalBytes = 0;
            for (String f : files) {
                Path local = Path.of(f);
                byte[] content = CliSupport.readFileBytes(local, MAX_UPLOAD_FILE_BYTES);
                totalBytes += content.length;
                if (totalBytes > MAX_UPLOAD_TOTAL_BYTES) {
                    CliSupport.fail("Selected files exceed the " + MAX_UPLOAD_TOTAL_BYTES
                            + " byte total upload limit");
                }
                Map<String, Object> spec = new LinkedHashMap<>();
                spec.put("filename", local.getFileName().toString());
                spec.put("content", content);
                fileMaps.add(spec);
            }
            try (RuntimeClient client = RuntimeResolver.resolve(
                    agentName, region, !skipSsl, resolvedBearerToken, endpoint)) {
                if (JsonUtils.isNotBlank(resolvedBearerToken)) {
                    client.setAuthToken(resolvedBearerToken);
                }
                Map<String, Object> result = client.uploadFiles(
                        agentName, sessionId, fileMaps, remotePath,
                        null, null, null, resolvedBearerToken, endpoint, userId, timeout);
                CliSupport.printJson(result);
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Failed to upload files: " + e.getMessage());
            }
        }

        @Option(names = {"-e", "--endpoint"}, description = "Endpoint name")
        String endpoint;
        @Option(names = {"-u", "--user-id"}, description = "User ID for OAuth2 outbound")
        String userId;
    }

    @Command(name = "download-files", description = "Download files from runtime")
    static class DownloadFilesCommand implements Runnable {
        @Option(names = {"-a", "--agent"}, description = "Agent name", required = true)
        String agentName;

        @Option(names = {"-s", "--session"}, description = "Session ID", required = true)
        String sessionId;

        @Option(names = {"-p", "--path"}, description = "Remote file/directory path", required = true, defaultValue = "")
        String remotePath;

        @Option(names = {"-o", "--output"}, description = "Local output path")
        String outputPath;

        @Option(names = "--recursive", description = "Download directory as tar archive")
        boolean recursive;

        @Option(names = {"-f", "--force"}, description = "Replace an existing output file")
        boolean force;

        @Option(names = {"-bt", "--bearer-token"}, arity = "0..1", interactive = true,
                description = "Bearer token (omit value for hidden prompt; defaults to BEARER_TOKEN)")
        String bearerToken;

        @Option(names = {"-r", "--region"}, description = "Region name")
        String region;

        @Option(names = {"-k", "--skip-ssl-verification"}, description = "Skip SSL verification")
        boolean skipSsl;

        @Option(names = "--timeout", description = "Timeout in seconds", defaultValue = "900")
        int timeout;

        @Override
        public void run() {
            String resolvedBearerToken = CliSupport.resolveBearerToken(bearerToken);
            if (remotePath == null || remotePath.isBlank()) {
                CliSupport.fail("Path is required (--path)");
            }
            if (timeout < 1) {
                CliSupport.fail("timeout must be greater than zero");
            }
            try (RuntimeClient client = RuntimeResolver.resolve(
                    agentName, region, !skipSsl, resolvedBearerToken, endpoint)) {
                if (JsonUtils.isNotBlank(resolvedBearerToken)) {
                    client.setAuthToken(resolvedBearerToken);
                }
                RequestResult result = client.downloadFiles(
                        agentName, sessionId, remotePath, recursive,
                        resolvedBearerToken, endpoint, userId, timeout);
                try (result) {
                    if (!result.isSuccess()) {
                        CliSupport.fail("Failed to download files (HTTP "
                                + result.getStatusCode() + "): " + result.getError());
                    }
                    String filename = remotePath.contains("/")
                            ? remotePath.substring(remotePath.lastIndexOf('/') + 1)
                            : remotePath;
                    if (filename.isEmpty()) filename = "downloaded";
                    String out = outputPath != null && !outputPath.isEmpty() ? outputPath : filename;

                    byte[] bytes = result.getDataAsBytes();
                    if (bytes == null && result.getDataAsString() != null) {
                        bytes = result.getDataAsString()
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    }
                    if (bytes == null) {
                        CliSupport.fail("Download returned no file content");
                    }
                    Path savedPath = CliSupport.writeFileAtomically(Path.of(out), bytes, force);
                    Map<String, Object> outMap = new LinkedHashMap<>();
                    outMap.put("saved_path", savedPath.toString());
                    outMap.put("size", bytes.length);
                    outMap.put("content_type", result.getHeaders().get("Content-Type"));
                    outMap.put("path", remotePath);
                    CliSupport.printJson(outMap);
                }
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Failed to download files: " + e.getMessage());
            }
        }

        @Option(names = {"-e", "--endpoint"}, description = "Endpoint name")
        String endpoint;
        @Option(names = {"-u", "--user-id"}, description = "User ID for OAuth2 outbound")
        String userId;
    }

    @Command(name = "start-session", description = "Start runtime session")
    static class StartSessionCommand implements Runnable {
        @Option(names = {"-a", "--agent"}, description = "Agent name", required = true)
        String agentName;

        @Option(names = {"-r", "--region"}, description = "Region name")
        String region;

        @Option(names = {"-bt", "--bearer-token"}, arity = "0..1", interactive = true,
                description = "Bearer token (omit value for hidden prompt; defaults to BEARER_TOKEN)")
        String bearerToken;

        @Option(names = {"-e", "--endpoint"}, description = "Endpoint name")
        String endpoint;

        @Option(names = {"-k", "--skip-ssl-verification"}, description = "Skip SSL verification")
        boolean skipSsl;

        @Option(names = {"-u", "--user-id"}, description = "User ID for OAuth2 outbound")
        String userId;

        @Override
        public void run() {
            String resolvedBearerToken = CliSupport.resolveBearerToken(bearerToken);
            try (RuntimeClient client = RuntimeResolver.resolve(
                    agentName, region, !skipSsl, resolvedBearerToken, endpoint)) {
                if (JsonUtils.isNotBlank(resolvedBearerToken)) {
                    client.setAuthToken(resolvedBearerToken);
                }
                Map<String, Object> result = client.startSession(
                        agentName, resolvedBearerToken, endpoint, userId, 30);
                CliSupport.printJson(result);
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Failed to start session: " + e.getMessage());
            }
        }
    }

    @Command(name = "stop-session", description = "Stop runtime session")
    static class StopSessionCommand implements Runnable {
        @Option(names = {"-a", "--agent"}, description = "Agent name", required = true)
        String agentName;

        @Option(names = {"-s", "--session"}, description = "Session ID", required = true)
        String sessionId;

        @Option(names = {"-bt", "--bearer-token"}, arity = "0..1", interactive = true,
                description = "Bearer token (omit value for hidden prompt; defaults to BEARER_TOKEN)")
        String bearerToken;

        @Option(names = {"-r", "--region"}, description = "Region name")
        String region;

        @Option(names = {"-e", "--endpoint"}, description = "Endpoint name")
        String endpoint;

        @Option(names = {"-k", "--skip-ssl-verification"}, description = "Skip SSL verification")
        boolean skipSsl;

        @Option(names = {"-u", "--user-id"}, description = "User ID for OAuth2 outbound")
        String userId;

        @Override
        public void run() {
            String resolvedBearerToken = CliSupport.resolveBearerToken(bearerToken);
            try (RuntimeClient client = RuntimeResolver.resolve(
                    agentName, region, !skipSsl, resolvedBearerToken, endpoint)) {
                if (JsonUtils.isNotBlank(resolvedBearerToken)) {
                    client.setAuthToken(resolvedBearerToken);
                }
                Map<String, Object> result = client.stopSession(
                        agentName, sessionId, resolvedBearerToken, endpoint, userId, 30);
                CliSupport.printJson(result);
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Failed to stop session: " + e.getMessage());
            }
        }
    }
}
