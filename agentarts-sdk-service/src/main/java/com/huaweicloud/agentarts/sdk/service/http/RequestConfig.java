package com.huaweicloud.agentarts.sdk.service.http;

/**
 * Configuration for an HTTP client instance.
 *
 * <p>HTTP request configuration: base URL, timeout, SSL verification.</p>
 */
public class RequestConfig {

    public static final long DEFAULT_MAX_RESPONSE_BODY_BYTES = 64L * 1024 * 1024;

    private String baseUrl = "";
    private double timeoutSeconds = 30.0;
    private boolean verifySsl = true;
    private long maxResponseBodyBytes = DEFAULT_MAX_RESPONSE_BODY_BYTES;

    public RequestConfig() {
    }

    public RequestConfig(String baseUrl, double timeoutSeconds, boolean verifySsl) {
        setBaseUrl(baseUrl);
        setTimeoutSeconds(timeoutSeconds);
        this.verifySsl = verifySsl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "";
    }

    public double getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(double timeoutSeconds) {
        if (!Double.isFinite(timeoutSeconds) || timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be a finite value greater than zero");
        }
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isVerifySsl() {
        return verifySsl;
    }

    public void setVerifySsl(boolean verifySsl) {
        this.verifySsl = verifySsl;
    }

    public long getMaxResponseBodyBytes() {
        return maxResponseBodyBytes;
    }

    public void setMaxResponseBodyBytes(long maxResponseBodyBytes) {
        if (maxResponseBodyBytes < 1 || maxResponseBodyBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "maxResponseBodyBytes must be between 1 and Integer.MAX_VALUE");
        }
        this.maxResponseBodyBytes = maxResponseBodyBytes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RequestConfig config = new RequestConfig();

        public Builder baseUrl(String baseUrl) {
            config.setBaseUrl(baseUrl);
            return this;
        }

        public Builder timeoutSeconds(double timeout) {
            config.setTimeoutSeconds(timeout);
            return this;
        }

        public Builder verifySsl(boolean verifySsl) {
            config.setVerifySsl(verifySsl);
            return this;
        }

        public Builder maxResponseBodyBytes(long maximumBytes) {
            config.setMaxResponseBodyBytes(maximumBytes);
            return this;
        }

        public RequestConfig build() {
            return config;
        }
    }
}
