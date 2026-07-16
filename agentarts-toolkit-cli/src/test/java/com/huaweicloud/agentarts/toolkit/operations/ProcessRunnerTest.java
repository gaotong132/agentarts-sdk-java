package com.huaweicloud.agentarts.toolkit.operations;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessRunnerTest {

    @AfterEach
    void clearProperties() {
        System.clearProperty(ProcessRunner.TIMEOUT_PROPERTY);
    }

    @Test
    void configuredTimeoutIsBounded() {
        assertEquals(Duration.ofSeconds(ProcessRunner.DEFAULT_TIMEOUT_SECONDS),
                ProcessRunner.configuredTimeout());

        System.setProperty(ProcessRunner.TIMEOUT_PROPERTY, "12");
        assertEquals(Duration.ofSeconds(12), ProcessRunner.configuredTimeout());

        System.setProperty(ProcessRunner.TIMEOUT_PROPERTY, "0");
        assertThrows(IllegalArgumentException.class, ProcessRunner::configuredTimeout);
        System.setProperty(ProcessRunner.TIMEOUT_PROPERTY, "not-a-number");
        assertThrows(IllegalArgumentException.class, ProcessRunner::configuredTimeout);
    }

    @Test
    void rejectsInvalidCommandsInputAndTimeouts() {
        assertThrows(IllegalArgumentException.class,
                () -> ProcessRunner.run(List.of(), null, null, "empty", Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> ProcessRunner.run(List.of("java"), null, null, "zero", Duration.ZERO));
        String oversized = "x".repeat(ProcessRunner.MAX_STDIN_BYTES + 1);
        assertThrows(IllegalArgumentException.class,
                () -> ProcessRunner.run(List.of("java"), null, oversized, "input",
                        Duration.ofSeconds(1)));
    }

    @Test
    void returnsExitCodeAndHandlesMissingExecutables() {
        int success = ProcessRunner.run(javaCommand("success"), null, "input", "child",
                Duration.ofSeconds(5));
        assertEquals(0, success);

        int missing = ProcessRunner.run(List.of("agentarts-command-that-does-not-exist"),
                null, null, "missing", Duration.ofSeconds(1));
        assertEquals(-1, missing);
    }

    @Test
    void terminatesHungProcessesWithinTheDeadline() {
        long started = System.nanoTime();
        int exit = ProcessRunner.run(javaCommand("sleep"), null, null, "sleeping-child",
                Duration.ofMillis(100));
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();

        assertEquals(-1, exit);
        assertTrue(elapsedMillis < 5_000, "timed-out process must be terminated promptly");
    }

    private static List<String> javaCommand(String mode) {
        String executable = Path.of(System.getProperty("java.home"), "bin",
                isWindows() ? "java.exe" : "java").toString();
        final String classes;
        try {
            classes = Path.of(ProcessRunnerTest.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toString();
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException("Cannot resolve test class path", e);
        }
        return List.of(executable, "-cp", classes, ChildProcess.class.getName(), mode);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static final class ChildProcess {
        private ChildProcess() {
        }

        public static void main(String[] args) throws IOException, InterruptedException {
            if ("sleep".equals(args[0])) {
                Thread.sleep(60_000);
                return;
            }
            String input = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
            if (!"input".equals(input)) {
                System.exit(2);
            }
            System.out.print("ok");
        }
    }
}
