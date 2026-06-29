package com.huaweicloud.agentarts.sdk.service.http;

/**
 * Configuration for an HTTP client instance.
 *
 * <p>Mirrors Python {@code RequestConfig} from {@code http_client.py}.</p>
 */
public class RequestConfig {

    private String baseUrl = "";
    private double timeoutSeconds = 30.0;
    private boolean verifySsl = true;

    public RequestConfig() {
    }

    public RequestConfig(String baseUrl, double timeoutSeconds, boolean verifySsl) {
        this.baseUrl = baseUrl != null ? baseUrl : "";
        this.timeoutSeconds = timeoutSeconds;
        this.verifySsl = verifySsl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl != null ? baseUrl : "";
    }

    public double getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(double timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isVerifySsl() {
        return verifySsl;
    }

    public void setVerifySsl(boolean verifySsl) {
        this.verifySsl = verifySsl;
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

        public RequestConfig build() {
            return config;
        }
    }
}
