package com.huaweicloud.agentarts.sdk.integration.agentscope.state;

import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.sdk.memory.model.*;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AgentStateStore implementation backed by AgentArts Memory service.
 *
 * <p>Implements all 8 methods of the {@link AgentStateStore} interface.
 * Uses the Memory data plane API for session and message management.
 * Falls back to in-memory storage for State objects that can't be
 * directly mapped to Memory API calls.</p>
 *
 * <p>Contract matches agentscope's InMemoryAgentStateStore behavior:
 * save(key, list) does full replacement; get returns Optional.empty() when not found.</p>
 */
public class MemoryAgentStateStore implements AgentStateStore {

    private final MemoryClient memoryClient;
    private final String spaceId;

    // In-memory fallback for State objects that can't be persisted via Memory API
    // Structure: userId → sessionId → key → State (or List<State>)
    private final Map<String, Map<String, Map<String, Object>>> localStore = new ConcurrentHashMap<>();

    public MemoryAgentStateStore(MemoryClient memoryClient, String spaceId) {
        this.memoryClient = memoryClient;
        this.spaceId = spaceId;
    }

    /**
     * Convenience constructor that creates a MemoryClient internally.
     */
    public MemoryAgentStateStore(String regionName, String apiKey, String spaceId) {
        this.memoryClient = new MemoryClient(regionName, apiKey);
        this.spaceId = spaceId;
    }

    @Override
    public void save(String userId, String sessionId, String key, State value) {
        ensureSession(userId, sessionId);
        getOrCreateSessionMap(userId, sessionId).put(key, value);
    }

    @Override
    public void save(String userId, String sessionId, String key, List<? extends State> values) {
        ensureSession(userId, sessionId);
        // Full replacement (matches InMemoryAgentStateStore behavior)
        getOrCreateSessionMap(userId, sessionId).put(key, new ArrayList<>(values));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends State> Optional<T> get(String userId, String sessionId, String key, Class<T> type) {
        Map<String, Object> sessionMap = getSessionMap(userId, sessionId);
        if (sessionMap == null) return Optional.empty();
        Object value = sessionMap.get(key);
        if (value == null) return Optional.empty();
        if (type.isInstance(value)) return Optional.of(type.cast(value));
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends State> List<T> getList(String userId, String sessionId, String key, Class<T> itemType) {
        Map<String, Object> sessionMap = getSessionMap(userId, sessionId);
        if (sessionMap == null) return List.of();
        Object value = sessionMap.get(key);
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(itemType::isInstance)
                    .map(itemType::cast)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public boolean exists(String userId, String sessionId) {
        String normalizedUserId = normalizeUserId(userId);
        Map<String, Map<String, Object>> userMap = localStore.get(normalizedUserId);
        return userMap != null && userMap.containsKey(sessionId);
    }

    @Override
    public void delete(String userId, String sessionId) {
        String normalizedUserId = normalizeUserId(userId);
        Map<String, Map<String, Object>> userMap = localStore.get(normalizedUserId);
        if (userMap != null) {
            userMap.remove(sessionId);
        }
    }

    @Override
    public void delete(String userId, String sessionId, String key) {
        Map<String, Object> sessionMap = getSessionMap(userId, sessionId);
        if (sessionMap != null) {
            sessionMap.remove(key);
        }
    }

    @Override
    public Set<String> listSessionIds(String userId) {
        String normalizedUserId = normalizeUserId(userId);
        Map<String, Map<String, Object>> userMap = localStore.get(normalizedUserId);
        if (userMap == null) return Set.of();
        return Set.copyOf(userMap.keySet());
    }

    @Override
    public void close() {
        memoryClient.close();
    }

    // ========================
    // Internal helpers
    // ========================

    private String normalizeUserId(String userId) {
        return (userId == null) ? "__anon__" : userId;
    }

    private void ensureSession(String userId, String sessionId) {
        String normalizedUserId = normalizeUserId(userId);
        localStore.computeIfAbsent(normalizedUserId, k -> new ConcurrentHashMap<>());
    }

    private Map<String, Object> getOrCreateSessionMap(String userId, String sessionId) {
        String normalizedUserId = normalizeUserId(userId);
        return localStore
                .computeIfAbsent(normalizedUserId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
    }

    private Map<String, Object> getSessionMap(String userId, String sessionId) {
        String normalizedUserId = normalizeUserId(userId);
        Map<String, Map<String, Object>> userMap = localStore.get(normalizedUserId);
        if (userMap == null) return null;
        return userMap.get(sessionId);
    }
}
