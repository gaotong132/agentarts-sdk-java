package com.huaweicloud.agentarts.sdk.memory;

import com.huaweicloud.agentarts.sdk.memory.model.MemoryInfo;
import com.huaweicloud.agentarts.sdk.memory.model.MemoryListFilter;
import com.huaweicloud.agentarts.sdk.memory.model.MemoryListResponse;
import com.huaweicloud.agentarts.sdk.memory.model.MemorySearchFilter;
import com.huaweicloud.agentarts.sdk.memory.model.MemorySearchResponse;
import com.huaweicloud.agentarts.sdk.memory.model.MessageBatchResponse;
import com.huaweicloud.agentarts.sdk.memory.model.MessageInfo;
import com.huaweicloud.agentarts.sdk.memory.model.MessageListResponse;
import com.huaweicloud.agentarts.sdk.memory.model.SessionInfo;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.publisher.Mono;

/** Reactor-based convenience wrapper that binds a Memory space, actor, and session. */
public final class AsyncMemorySession implements AutoCloseable {
    private final MemoryClient client;
    private final String spaceId;
    private final String actorId;
    private final String regionName;
    private final boolean ownsClient;
    private final AtomicBoolean closed = new AtomicBoolean();

    private volatile String sessionId;
    private Mono<String> initialization;

    /** Create a wrapper around an existing client. The client remains caller-owned. */
    public AsyncMemorySession(
            MemoryClient client,
            String spaceId,
            String actorId,
            String sessionId) {
        this(client, spaceId, actorId, sessionId, false);
    }

    private AsyncMemorySession(
            MemoryClient client,
            String spaceId,
            String actorId,
            String sessionId,
            boolean ownsClient) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.spaceId = requireNonBlank(spaceId, "spaceId");
        this.actorId = requireNonBlank(actorId, "actorId");
        this.sessionId = sessionId;
        this.regionName = client.getRegionName();
        this.ownsClient = ownsClient;
    }

    /** Create a self-contained session with an internally owned Memory client. */
    public static AsyncMemorySession of(
            String spaceId,
            String actorId,
            String sessionId,
            String regionName,
            String apiKey) {
        MemoryClient client = new MemoryClient(regionName, apiKey);
        return new AsyncMemorySession(client, spaceId, actorId, sessionId, true);
    }

    /** Create a self-contained session and lazily allocate its remote session ID. */
    public static AsyncMemorySession of(String spaceId, String actorId) {
        return of(spaceId, actorId, null, null, null);
    }

    /** Initialize the remote session if necessary. Concurrent subscribers share one request. */
    public Mono<AsyncMemorySession> initialize() {
        return ensureInitialized().thenReturn(this);
    }

    public Mono<List<MessageInfo>> getLastKMessages(int k) {
        return ensureInitialized().flatMap(id -> client.getLastKMessagesAsync(id, k, spaceId));
    }

    public Mono<MessageBatchResponse> addMessages(List<?> messages) {
        return ensureInitialized().flatMap(id -> client.addMessagesAsync(spaceId, id, messages));
    }

    public Mono<MessageBatchResponse> addMessages(
            List<?> messages,
            Long timestamp,
            String idempotencyKey,
            boolean isForceExtract) {
        return ensureInitialized().flatMap(id -> client.addMessagesAsync(
                spaceId, id, messages, timestamp, idempotencyKey, isForceExtract));
    }

    public Mono<MessageListResponse> listMessages(int limit, int offset) {
        return ensureInitialized().flatMap(id -> client.listMessagesAsync(spaceId, id, limit, offset));
    }

    public Mono<MessageListResponse> listMessages() {
        return listMessages(10, 0);
    }

    public Mono<MessageInfo> getMessage(String messageId) {
        return ensureInitialized().flatMap(id -> client.getMessageAsync(messageId, spaceId, id));
    }

    public Mono<MemorySearchResponse> searchMemories(MemorySearchFilter filters) {
        return ensureInitialized().flatMap(ignored -> client.searchMemoriesAsync(spaceId, filters));
    }

    public Mono<MemorySearchResponse> searchMemories() {
        return searchMemories(null);
    }

    public Mono<MemoryListResponse> listMemories(
            int limit,
            int offset,
            MemoryListFilter filters) {
        return ensureInitialized().flatMap(
                ignored -> client.listMemoriesAsync(spaceId, limit, offset, filters));
    }

    public Mono<MemoryListResponse> listMemories() {
        return listMemories(10, 0, null);
    }

    public Mono<MemoryInfo> getMemory(String memoryId) {
        return ensureInitialized().flatMap(ignored -> client.getMemoryAsync(spaceId, memoryId));
    }

    public Mono<Void> deleteMemory(String memoryId) {
        return ensureInitialized().flatMap(ignored -> client.deleteMemoryAsync(spaceId, memoryId));
    }

    /** Asynchronously close resources owned by this wrapper. */
    public Mono<Void> closeAsync() {
        return Mono.fromRunnable(this::close);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true) && ownsClient) {
            client.close();
        }
    }

    public String getSpaceId() { return spaceId; }
    public String getActorId() { return actorId; }
    public String getSessionId() { return sessionId; }
    public String getRegionName() { return regionName; }

    private synchronized Mono<String> ensureInitialized() {
        if (closed.get()) {
            return Mono.error(new IllegalStateException("AsyncMemorySession is closed"));
        }
        if (sessionId != null) {
            return Mono.just(sessionId);
        }
        if (initialization == null) {
            initialization = client.createMemorySessionAsync(spaceId, null, actorId, null)
                    .map(this::requireSessionId)
                    .doOnNext(id -> sessionId = id)
                    .doOnError(ignored -> clearFailedInitialization())
                    .cache();
        }
        return initialization;
    }

    private String requireSessionId(SessionInfo session) {
        if (session == null || session.getId() == null || session.getId().isBlank()) {
            throw new IllegalStateException("Memory service returned an empty session ID");
        }
        return session.getId();
    }

    private synchronized void clearFailedInitialization() {
        if (sessionId == null) {
            initialization = null;
        }
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
