package com.huaweicloud.agentarts.sdk.core;

/**
 * Runtime ping status for health-check endpoint.
 *
 * <p>Mirrors Python {@code agentarts.sdk.runtime.model.PingStatus}.</p>
 */
public enum PingStatus {

    /** Service is healthy and accepting requests. */
    HEALTHY("Healthy"),

    /** Service is healthy but at maximum concurrency. */
    HEALTHY_BUSY("HealthyBusy"),

    /** Service is unhealthy. */
    UNHEALTHY("Unhealthy");

    private final String value;

    PingStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Parse a PingStatus from its string value.
     *
     * @param value the string value (e.g., "Healthy", "HealthyBusy", "Unhealthy")
     * @return the matching PingStatus
     * @throws IllegalArgumentException if no match found
     */
    public static PingStatus fromValue(String value) {
        for (PingStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown PingStatus: " + value);
    }
}
