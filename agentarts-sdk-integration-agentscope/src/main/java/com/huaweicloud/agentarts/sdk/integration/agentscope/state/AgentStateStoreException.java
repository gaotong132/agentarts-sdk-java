package com.huaweicloud.agentarts.sdk.integration.agentscope.state;

/**
 * Signals that AgentScope state could not be persisted or loaded from AgentArts Memory.
 */
public class AgentStateStoreException extends RuntimeException {

    public AgentStateStoreException(String message) {
        super(message);
    }

    public AgentStateStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
