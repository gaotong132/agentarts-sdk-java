package com.huaweicloud.agentarts.toolkit.operations;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Runs CLI child processes with bounded input, output buffering, and execution time. */
final class ProcessRunner {

    static final String TIMEOUT_PROPERTY = "agentarts.cli.processTimeoutSeconds";
    static final long DEFAULT_TIMEOUT_SECONDS = 600;
    static final long MAX_TIMEOUT_SECONDS = 3600;
    static final int MAX_STDIN_BYTES = 64 * 1024;

    private ProcessRunner() {
    }

    static Duration configuredTimeout() {
        String raw = System.getProperty(TIMEOUT_PROPERTY,
                Long.toString(DEFAULT_TIMEOUT_SECONDS));
        final long seconds;
        try {
            seconds = Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(TIMEOUT_PROPERTY + " must be an integer", e);
        }
        if (seconds < 1 || seconds > MAX_TIMEOUT_SECONDS) {
            throw new IllegalArgumentException(TIMEOUT_PROPERTY + " must be between 1 and "
                    + MAX_TIMEOUT_SECONDS + " seconds");
        }
        return Duration.ofSeconds(seconds);
    }

    static int run(List<String> command, File workDir, String stdin, String label,
                   Duration timeout) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(timeout, "timeout");
        if (command.isEmpty()) {
            throw new IllegalArgumentException("command must not be empty");
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        byte[] input = stdin == null ? new byte[0] : stdin.getBytes(StandardCharsets.UTF_8);
        if (input.length > MAX_STDIN_BYTES) {
            throw new IllegalArgumentException("process stdin exceeds " + MAX_STDIN_BYTES + " bytes");
        }

        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        if (workDir != null) {
            builder.directory(workDir);
        }

        Process process = null;
        ExecutorService outputExecutor = null;
        Future<?> outputTask = null;
        try {
            process = builder.start();
            Process runningProcess = process;
            outputExecutor = Executors.newSingleThreadExecutor(task -> {
                Thread thread = new Thread(task, "agentarts-cli-process-output");
                thread.setDaemon(true);
                return thread;
            });
            outputTask = outputExecutor.submit(() -> streamOutput(runningProcess, label));

            try (var output = process.getOutputStream()) {
                if (input.length > 0) {
                    output.write(input);
                    output.flush();
                }
            }

            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                terminateTree(process);
                System.err.println("  " + label + " timed out after " + formatTimeout(timeout));
                awaitOutput(outputTask);
                return -1;
            }
            awaitOutput(outputTask);
            return process.exitValue();
        } catch (IOException e) {
            System.err.println("  " + label + " failed to start: " + e.getMessage()
                    + " (is '" + command.get(0) + "' installed and on PATH?)");
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                terminateTree(process);
            }
            System.err.println("  " + label + " interrupted");
            return -1;
        } finally {
            if (process != null && process.isAlive()) {
                terminateTree(process);
            }
            if (outputTask != null && !outputTask.isDone()) {
                outputTask.cancel(true);
            }
            if (outputExecutor != null) {
                outputExecutor.shutdownNow();
            }
        }
    }

    private static void streamOutput(Process process, String label) {
        char[] buffer = new char[8192];
        try (Reader reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
            int count;
            while ((count = reader.read(buffer)) >= 0) {
                if (count > 0) {
                    System.out.print("  [" + label + "] ");
                    System.out.print(new String(buffer, 0, count));
                }
            }
        } catch (IOException e) {
            if (process.isAlive()) {
                System.err.println("  " + label + " output failed: " + e.getMessage());
            }
        }
    }

    private static void awaitOutput(Future<?> outputTask) throws InterruptedException {
        if (outputTask == null) {
            return;
        }
        try {
            outputTask.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException ignored) {
            outputTask.cancel(true);
        }
    }

    private static String formatTimeout(Duration timeout) {
        return timeout.toMillis() < 1000
                ? timeout.toMillis() + " milliseconds"
                : timeout.toSeconds() + " seconds";
    }

    private static void terminateTree(Process process) {
        List<ProcessHandle> descendants = process.descendants().toList();
        descendants.forEach(ProcessHandle::destroy);
        process.destroy();
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                descendants.forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            descendants.forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }
}
