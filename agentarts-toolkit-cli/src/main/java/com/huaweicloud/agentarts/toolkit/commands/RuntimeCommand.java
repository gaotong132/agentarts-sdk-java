package com.huaweicloud.agentarts.toolkit.commands;

import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;
import com.huaweicloud.agentarts.sdk.service.runtime.RuntimeClient;
import com.huaweicloud.agentarts.toolkit.operations.RuntimeResolver;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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

        @Option(names = {"-bt", "--bearer-token"}, description = "Bearer token")
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
            // Split the command string into a argv list. The reference CLI uses shlex.split;
            // a whitespace split is a sufficient approximation for the common cases.
            List<String> commandArray = new ArrayList<>(Arrays.asList(command.trim().split("\\s+")));
            commandArray.removeIf(String::isEmpty);
            if (commandArray.isEmpty()) {
                CliSupport.fail("Command cannot be empty");
            }
            try (RuntimeClient client = RuntimeResolver.resolve(agentName, region, !skipSsl, bearerToken)) {
                if (JsonUtils.isNotBlank(bearerToken)) {
                    client.setAuthToken(bearerToken);
                }
                Map<String, Object> result = client.execCommand(
                        agentName, sessionId, commandArray, chunked,
                        bearerToken, endpoint, userId, timeout);
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

        @Option(names = {"-bt", "--bearer-token"}, description = "Bearer token")
        String bearerToken;

        @Option(names = {"-r", "--region"}, description = "Region name")
        String region;

        @Option(names = {"-k", "--skip-ssl-verification"}, description = "Skip SSL verification")
        boolean skipSsl;

        @Option(names = "--timeout", description = "Timeout in seconds", defaultValue = "900")
        int timeout;

        @Override
        public void run() {
            // TODO: the Java RuntimeClient.uploadFiles posts an UploadFilesRequest as JSON
            // (files carried as base64 content). The backend's /upload-files endpoint expects
            // application/octet-stream (single file) or multipart/form-data (multiple files),
            // as the reference CLI streams. Until the Java client gains a streaming upload path,
            // this command reads each local file and carries its bytes (Jackson base64-encodes
            // byte[]) — real uploads against the cloud may not succeed via this JSON path.
            List<Map<String, Object>> fileMaps = new ArrayList<>();
            for (String f : files) {
                Path local = Path.of(f);
                if (!Files.isRegularFile(local)) {
                    CliSupport.fail("Local file not found: " + f);
                }
                try {
                    Map<String, Object> spec = new LinkedHashMap<>();
                    spec.put("filename", local.getFileName().toString());
                    spec.put("content", Files.readAllBytes(local));
                    fileMaps.add(spec);
                } catch (Exception e) {
                    CliSupport.fail("Failed to read file " + f + ": " + e.getMessage());
                }
            }
            try (RuntimeClient client = RuntimeResolver.resolve(agentName, region, !skipSsl, bearerToken)) {
                if (JsonUtils.isNotBlank(bearerToken)) {
                    client.setAuthToken(bearerToken);
                }
                Map<String, Object> result = client.uploadFiles(
                        agentName, sessionId, fileMaps, remotePath,
                        null, null, null, bearerToken, endpoint, userId, timeout);
                CliSupport.printJson(result);
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Failed to upload files: " + e.getMessage());
            }
        }

        // NOTE: --endpoint and --user-id are inherited from the picocli option set but
        // uploadFiles does not currently thread --endpoint through; left as TODO.
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

        @Option(names = {"-bt", "--bearer-token"}, description = "Bearer token")
        String bearerToken;

        @Option(names = {"-r", "--region"}, description = "Region name")
        String region;

        @Option(names = {"-k", "--skip-ssl-verification"}, description = "Skip SSL verification")
        boolean skipSsl;

        @Option(names = "--timeout", description = "Timeout in seconds", defaultValue = "900")
        int timeout;

        @Override
        public void run() {
            if (remotePath == null || remotePath.isEmpty()) {
                CliSupport.fail("Path is required (--path)");
            }
            try (RuntimeClient client = RuntimeResolver.resolve(agentName, region, !skipSsl, bearerToken)) {
                if (JsonUtils.isNotBlank(bearerToken)) {
                    client.setAuthToken(bearerToken);
                }
                RequestResult result = client.downloadFiles(
                        agentName, sessionId, remotePath, recursive,
                        bearerToken, endpoint, userId, timeout);
                if (!result.isSuccess()) {
                    CliSupport.fail("Failed to download files (HTTP "
                            + result.getStatusCode() + "): " + result.getError());
                }
                // Resolve the local output path.
                String filename = remotePath.contains("/")
                        ? remotePath.substring(remotePath.lastIndexOf('/') + 1)
                        : remotePath;
                if (filename.isEmpty()) filename = "downloaded";
                String out = (outputPath != null && !outputPath.isEmpty()) ? outputPath : filename;

                // TODO: the Java BaseHttpClient materializes the response body as a UTF-8
                // string, which corrupts binary content (e.g. tar archives from --recursive).
                // Writing the string bytes back is a best-effort until the client exposes a
                // raw byte/stream download path.
                byte[] bytes = result.getDataAsString() != null
                        ? result.getDataAsString().getBytes(java.nio.charset.StandardCharsets.UTF_8)
                        : new byte[0];
                try {
                    Files.write(Path.of(out), bytes);
                } catch (Exception e) {
                    CliSupport.fail("Failed to write output file " + out + ": " + e.getMessage());
                }
                Map<String, Object> outMap = new LinkedHashMap<>();
                outMap.put("saved_path", out);
                outMap.put("size", bytes.length);
                outMap.put("content_type", result.getHeaders().get("Content-Type"));
                outMap.put("path", remotePath);
                CliSupport.printJson(outMap);
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Failed to download files: " + e.getMessage());
            }
        }

        // NOTE: --endpoint and --user-id are part of the option set; downloadFiles does not
        // currently thread --endpoint through; left as TODO.
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

        @Option(names = {"-bt", "--bearer-token"}, description = "Bearer token")
        String bearerToken;

        @Option(names = {"-e", "--endpoint"}, description = "Endpoint name")
        String endpoint;

        @Option(names = {"-k", "--skip-ssl-verification"}, description = "Skip SSL verification")
        boolean skipSsl;

        @Option(names = {"-u", "--user-id"}, description = "User ID for OAuth2 outbound")
        String userId;

        @Override
        public void run() {
            try (RuntimeClient client = RuntimeResolver.resolve(agentName, region, !skipSsl, bearerToken)) {
                if (JsonUtils.isNotBlank(bearerToken)) {
                    client.setAuthToken(bearerToken);
                }
                Map<String, Object> result = client.startSession(
                        agentName, bearerToken, endpoint, userId, 30);
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

        @Option(names = {"-bt", "--bearer-token"}, description = "Bearer token")
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
            try (RuntimeClient client = RuntimeResolver.resolve(agentName, region, !skipSsl, bearerToken)) {
                if (JsonUtils.isNotBlank(bearerToken)) {
                    client.setAuthToken(bearerToken);
                }
                Map<String, Object> result = client.stopSession(
                        agentName, sessionId, bearerToken, endpoint, userId, 30);
                CliSupport.printJson(result);
            } catch (Exception e) {
                if (e instanceof CliSupport.CliFailure) throw (CliSupport.CliFailure) e;
                CliSupport.fail("Failed to stop session: " + e.getMessage());
            }
        }
    }
}
