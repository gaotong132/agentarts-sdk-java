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
import com.huaweicloud.agentarts.sdk.memory.model.SpaceInfo;
import com.huaweicloud.agentarts.sdk.memory.model.SpaceListResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.publisher.Mono;

/**
 * Asynchronous Memory client aligned with the Python {@code AsyncMemoryClient} surface.
 *
 * <p>Low-frequency space control-plane methods remain synchronous, matching the Python
 * client. High-frequency data-plane methods return cold Reactor publishers backed by
 * the non-blocking HTTP transport.</p>
 */
public final class AsyncMemoryClient implements AutoCloseable {

    private final MemoryClient delegate;
    private final AtomicBoolean closed = new AtomicBoolean();

    public AsyncMemoryClient(String regionName, String apiKey, boolean verifySsl) {
        this(new MemoryClient(regionName, apiKey, verifySsl));
    }

    public AsyncMemoryClient(String regionName, String apiKey) {
        this(regionName, apiKey, true);
    }

    public AsyncMemoryClient() {
        this(null, null, true);
    }

    AsyncMemoryClient(MemoryClient delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    // Control plane: deliberately synchronous, matching Python AsyncMemoryClient.

    public SpaceInfo createSpace(String name) {
        ensureOpen();
        return delegate.createSpace(name);
    }

    public SpaceInfo createSpace(String name, int messageTtlHours, String description) {
        ensureOpen();
        return delegate.createSpace(name, messageTtlHours, description);
    }

    public SpaceInfo createSpace(
            String name,
            int messageTtlHours,
            String description,
            List<Map<String, String>> tags,
            Integer memoryExtractIdleSeconds,
            Integer memoryExtractMaxTokens,
            Integer memoryExtractMaxMessages,
            boolean publicAccessEnable,
            String privateVpcId,
            String privateSubnetId,
            List<String> memoryStrategiesBuiltin,
            List<Map<String, Object>> memoryStrategiesCustomized) {
        ensureOpen();
        return delegate.createSpace(name, messageTtlHours, description, tags,
                memoryExtractIdleSeconds, memoryExtractMaxTokens,
                memoryExtractMaxMessages, publicAccessEnable, privateVpcId,
                privateSubnetId, memoryStrategiesBuiltin, memoryStrategiesCustomized);
    }

    public SpaceInfo getSpace(String spaceId) {
        ensureOpen();
        return delegate.getSpace(spaceId);
    }

    public SpaceListResponse listSpaces() {
        ensureOpen();
        return delegate.listSpaces();
    }

    public SpaceListResponse listSpaces(int limit, int offset) {
        ensureOpen();
        return delegate.listSpaces(limit, offset);
    }

    public SpaceInfo updateSpace(
            String spaceId, String name, String description, Integer messageTtlHours) {
        ensureOpen();
        return delegate.updateSpace(spaceId, name, description, messageTtlHours);
    }

    public SpaceInfo updateSpace(
            String spaceId,
            String name,
            String description,
            Integer messageTtlHours,
            Boolean memoryExtractEnabled,
            Integer memoryExtractIdleSeconds,
            Integer memoryExtractMaxTokens,
            Integer memoryExtractMaxMessages,
            List<Map<String, String>> tags,
            List<String> memoryStrategiesBuiltin) {
        ensureOpen();
        return delegate.updateSpace(spaceId, name, description, messageTtlHours,
                memoryExtractEnabled, memoryExtractIdleSeconds, memoryExtractMaxTokens,
                memoryExtractMaxMessages, tags, memoryStrategiesBuiltin);
    }

    public void deleteSpace(String spaceId) {
        ensureOpen();
        delegate.deleteSpace(spaceId);
    }

    // Data plane: cold non-blocking publishers.

    public Mono<SessionInfo> createMemorySession(
            String spaceId, String id, String actorId, String assistantId,
            Map<String, Object> meta) {
        return defer(() -> delegate.createMemorySessionAsync(
                spaceId, id, actorId, assistantId, meta));
    }

    public Mono<SessionInfo> createMemorySession(
            String spaceId, String id, String actorId, String assistantId) {
        return createMemorySession(spaceId, id, actorId, assistantId, null);
    }

    public Mono<SessionInfo> createMemorySession(String spaceId) {
        return createMemorySession(spaceId, null, null, null, null);
    }

    public Mono<List<MessageInfo>> getLastKMessages(String sessionId, int k, String spaceId) {
        return defer(() -> delegate.getLastKMessagesAsync(sessionId, k, spaceId));
    }

    public Mono<MessageInfo> getMessage(String messageId, String spaceId, String sessionId) {
        return defer(() -> delegate.getMessageAsync(messageId, spaceId, sessionId));
    }

    public Mono<MessageBatchResponse> addMessages(
            String spaceId, String sessionId, List<?> messages, Long timestamp,
            String idempotencyKey, boolean isForceExtract) {
        return defer(() -> delegate.addMessagesAsync(spaceId, sessionId, messages,
                timestamp, idempotencyKey, isForceExtract));
    }

    public Mono<MessageBatchResponse> addMessages(
            String spaceId, String sessionId, List<?> messages) {
        return addMessages(spaceId, sessionId, messages, null, null, false);
    }

    public Mono<MessageListResponse> listMessages(
            String spaceId, String sessionId, int limit, int offset) {
        return defer(() -> delegate.listMessagesAsync(spaceId, sessionId, limit, offset));
    }

    public Mono<MessageListResponse> listMessages(String spaceId) {
        return listMessages(spaceId, null, 10, 0);
    }

    public Mono<MemorySearchResponse> searchMemories(
            String spaceId, MemorySearchFilter filters) {
        return defer(() -> delegate.searchMemoriesAsync(spaceId, filters));
    }

    public Mono<MemorySearchResponse> searchMemories(String spaceId) {
        return searchMemories(spaceId, null);
    }

    public Mono<MemoryListResponse> listMemories(
            String spaceId, int limit, int offset, MemoryListFilter filters) {
        return defer(() -> delegate.listMemoriesAsync(spaceId, limit, offset, filters));
    }

    public Mono<MemoryListResponse> listMemories(String spaceId) {
        return listMemories(spaceId, 10, 0, null);
    }

    public Mono<MemoryInfo> getMemory(String spaceId, String memoryId) {
        return defer(() -> delegate.getMemoryAsync(spaceId, memoryId));
    }

    public Mono<Void> deleteMemory(String spaceId, String memoryId) {
        return defer(() -> delegate.deleteMemoryAsync(spaceId, memoryId));
    }

    public Mono<Void> closeAsync() {
        return Mono.fromRunnable(this::close);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            delegate.close();
        }
    }

    public String getRegionName() {
        return delegate.getRegionName();
    }

    private <T> Mono<T> defer(java.util.function.Supplier<Mono<T>> operation) {
        return Mono.defer(() -> {
            ensureOpen();
            return operation.get();
        });
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("AsyncMemoryClient is closed");
        }
    }
}
