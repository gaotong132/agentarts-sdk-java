package com.huaweicloud.agentarts.toolkit.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Cloud runtime operations subcommand group.
 *
 * <p>Mirrors Python {@code runtime} sub-app from {@code cli/runtime/commands.py}.
 * Subcommands: invoke, exec-command, upload-files, download-files, start-session, stop-session.</p>
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
            System.out.println("Executing command '" + command + "' on agent '" + agentName + "'...");
            // TODO: delegate to RuntimeOperation.execCommand
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
            System.out.println("Uploading " + files.length + " file(s) to agent '" + agentName + "'...");
            // TODO: delegate to RuntimeOperation.uploadFiles
        }
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
            System.out.println("Downloading '" + remotePath + "' from agent '" + agentName + "'...");
            // TODO: delegate to RuntimeOperation.downloadFiles
        }
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
            System.out.println("Starting session for agent '" + agentName + "'...");
            // TODO: delegate to RuntimeOperation.startSession
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
            System.out.println("Stopping session '" + sessionId + "' for agent '" + agentName + "'...");
            // TODO: delegate to RuntimeOperation.stopSession
        }
    }
}
