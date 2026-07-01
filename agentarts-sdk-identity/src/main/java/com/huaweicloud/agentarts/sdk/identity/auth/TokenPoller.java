package com.huaweicloud.agentarts.sdk.identity.auth;

/**
 * Abstract base class for polling OAuth2 token authorization status.
 *
 * <p>Subclasses implement {@link #poll()} to check the token status.
 * The poller runs at a configurable interval until the token is authorized
 * or the timeout is reached.</p>
 */
public abstract class TokenPoller {

    /** Polling interval in seconds. Default: 5s. */
    protected int intervalSeconds = 5;

    /** Maximum polling duration in seconds. Default: 300s (5 minutes). */
    protected int timeoutSeconds = 300;

    /**
     * Poll for the token status.
     *
     * @return current token status
     */
    public abstract PollResult poll();

    /**
     * Wait for the token to become available, polling at intervals.
     *
     * @return the token value, or null if timed out
     */
    public String waitForToken() {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            PollResult result = poll();

            switch (result.status()) {
                case COMPLETED:
                    return result.token();
                case FAILED:
                    throw new RuntimeException("Token polling failed: " + result.error());
                case IN_PROGRESS:
                    try {
                        Thread.sleep(intervalSeconds * 1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Token polling interrupted", e);
                    }
                    break;
            }
        }

        throw new RuntimeException("Token polling timed out after " + timeoutSeconds + " seconds");
    }

    public TokenPoller withInterval(int seconds) {
        this.intervalSeconds = seconds;
        return this;
    }

    public TokenPoller withTimeout(int seconds) {
        this.timeoutSeconds = seconds;
        return this;
    }

    // ========================
    // Poll result
    // ========================

    public record PollResult(PollStatus status, String token, String error) {
        public static PollResult inProgress() {
            return new PollResult(PollStatus.IN_PROGRESS, null, null);
        }

        public static PollResult completed(String token) {
            return new PollResult(PollStatus.COMPLETED, token, null);
        }

        public static PollResult failed(String error) {
            return new PollResult(PollStatus.FAILED, null, error);
        }
    }

    public enum PollStatus {
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}
