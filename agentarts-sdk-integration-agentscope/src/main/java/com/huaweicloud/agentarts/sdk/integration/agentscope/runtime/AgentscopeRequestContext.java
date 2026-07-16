package com.huaweicloud.agentarts.sdk.integration.agentscope.runtime;

import io.agentscope.core.state.SessionKey;
import io.agentscope.core.tool.ToolExecutionContext;

/**
 * Immutable per-request context used by the AgentArts-to-AgentScope bridge.
 *
 * <p>The context can be registered in AgentScope's {@link ToolExecutionContext}
 * without copying secrets into logs or global state. Its session key includes
 * the user identifier to prevent state collisions between tenants.</p>
 */
public record AgentscopeRequestContext(
        String sessionId,
        String userId,
        String requestId,
        String workloadAccessToken) {

    private static final String DEFAULT_USER_ID = "anonymous";
    private static final String DEFAULT_SESSION_ID = "default";

    /** Return a tenant-aware key suitable for AgentScope {@code Session} storage. */
    public SessionKey sessionKey() {
        return new RequestSessionKey(normalize(userId, DEFAULT_USER_ID),
                normalize(sessionId, DEFAULT_SESSION_ID));
    }

    /** Build the context consumed by AgentScope tools for this invocation. */
    public ToolExecutionContext toToolExecutionContext() {
        return ToolExecutionContext.builder().register(this).build();
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /** Structured session key keeps user and session identity unambiguous. */
    public record RequestSessionKey(String userId, String sessionId) implements SessionKey {
        public RequestSessionKey {
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("userId must not be blank");
            }
            if (sessionId == null || sessionId.isBlank()) {
                throw new IllegalArgumentException("sessionId must not be blank");
            }
        }

        @Override
        public String toIdentifier() {
            return lengthPrefix(userId) + userId + lengthPrefix(sessionId) + sessionId;
        }

        private static String lengthPrefix(String value) {
            return value.length() + ":";
        }
    }
}
