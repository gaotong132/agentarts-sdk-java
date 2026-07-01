package com.huaweicloud.agentarts.sdk.memory;

import com.huaweicloud.agentarts.sdk.memory.model.*;

import java.util.List;

/**
 * Convenience wrapper that binds space_id and session_id.
 *
 * <p>Convenience wrapper that binds space_id and session_id.
 * Pre-binds spaceId and sessionId so callers don't need to pass them repeatedly.</p>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * MemorySession session = MemorySession.of("space-123", "user-456");
 * session.addMessages(List.of(new TextMessage("user", "Hello!")));
 * List<MessageInfo> msgs = session.getLastKMessages(10);
 * }</pre>
 */
public class MemorySession implements AutoCloseable {

    private final MemoryClient client;
    private final String spaceId;
    private final String actorId;
    private String sessionId;

    public MemorySession(MemoryClient client, String spaceId, String actorId, String sessionId) {
        this.client = client;
        this.spaceId = spaceId;
        this.actorId = actorId;
        this.sessionId = sessionId;
    }

    /**
     * Factory method. Creates a session, auto-creating if sessionId is null.
     * If sessionId is null, a new session is auto-created.
     */
    public static MemorySession of(String spaceId, String actorId, String sessionId,
                                    String regionName, String apiKey) {
        MemoryClient client = new MemoryClient(regionName, apiKey);
        MemorySession session = new MemorySession(client, spaceId, actorId, sessionId);
        if (sessionId == null) {
            SessionInfo info = client.createMemorySession(spaceId, null, actorId, null);
            session.sessionId = info.getId();
        }
        return session;
    }

    public static MemorySession of(String spaceId, String actorId) {
        return of(spaceId, actorId, null, null, null);
    }

    // ========================
    // Pre-bound methods
    // ========================

    public MessageBatchResponse addMessages(List<?> messages) {
        return client.addMessages(spaceId, sessionId, messages);
    }

    public MessageBatchResponse addMessages(List<?> messages, Long timestamp,
                                             String idempotencyKey, boolean isForceExtract) {
        return client.addMessages(spaceId, sessionId, messages, timestamp, idempotencyKey, isForceExtract);
    }

    public List<MessageInfo> getLastKMessages(int k) {
        return client.getLastKMessages(sessionId, k, spaceId);
    }

    public MessageInfo getMessage(String messageId) {
        return client.getMessage(messageId, spaceId, sessionId);
    }

    public MessageListResponse listMessages(int limit, int offset) {
        return client.listMessages(spaceId, sessionId, limit, offset);
    }

    public MessageListResponse listMessages() {
        return client.listMessages(spaceId, sessionId, 10, 0);
    }

    public MemorySearchResponse searchMemories(MemorySearchFilter filters) {
        return client.searchMemories(spaceId, filters);
    }

    public MemorySearchResponse searchMemories() {
        return client.searchMemories(spaceId);
    }

    public MemoryListResponse listMemories(int limit, int offset, MemoryListFilter filters) {
        return client.listMemories(spaceId, limit, offset, filters);
    }

    public MemoryListResponse listMemories() {
        return client.listMemories(spaceId);
    }

    public MemoryInfo getMemory(String memoryId) {
        return client.getMemory(spaceId, memoryId);
    }

    public void deleteMemory(String memoryId) {
        client.deleteMemory(spaceId, memoryId);
    }

    // ========================
    // Accessors
    // ========================

    public String getSpaceId() { return spaceId; }
    public String getActorId() { return actorId; }
    public String getSessionId() { return sessionId; }

    @Override
    public void close() {
        client.close();
    }
}
