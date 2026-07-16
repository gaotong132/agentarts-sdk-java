package com.huaweicloud.agentarts.sdk.integration.agentscope.state;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.sdk.memory.model.*;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AgentStateStore 实现，通过 AgentArts Memory 数据面 API 持久化 agentscope State。
 *
 * <p>映射策略：每个 (userId, sessionId, key) 三元组对应一个 Memory Session，
 * State 对象序列化为 JSON 后作为 TextMessage 存入。每次 save 追加新消息，
 * 读取时取最新的消息。</p>
 *
 * <p>消息内容格式：</p>
 * <ul>
 *   <li>单 State: {@code __S__:{className}:{json}}</li>
 *   <li>列表 State: 每个元素一条 {@code __L__:{className}:{json}}</li>
 * </ul>
 */
public class MemoryAgentStateStore implements AgentStateStore {

    private static final String SINGLE_PREFIX = "__S__:";
    private static final String LIST_PREFIX = "__L__:";
    private static final String LIST_BATCH = "__LB__:";
    private static final String SESSION_PREFIX = "__as_";

    private final MemoryClient memoryClient;
    private final String spaceId;

    /** ObjectMapper 配置：忽略未知属性，与 agentscope 的 JacksonJsonCodec 行为一致。 */
    private final ObjectMapper mapper;

    /** 本地缓存: logicalKey → Memory Session ID，避免重复创建 Session。 */
    private final Map<String, String> sessionCache = new ConcurrentHashMap<>();

    /** 本地缓存: logicalKey → 已知的 session 存在状态（用于 exists/listSessionIds）。 */
    public MemoryAgentStateStore(MemoryClient memoryClient, String spaceId) {
        this.memoryClient = Objects.requireNonNull(memoryClient, "memoryClient");
        if (spaceId == null || spaceId.isBlank()) {
            throw new IllegalArgumentException("spaceId must not be blank");
        }
        this.spaceId = spaceId;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 便捷构造函数。
     */
    public MemoryAgentStateStore(String regionName, String apiKey, String spaceId) {
        this(new MemoryClient(regionName, apiKey), spaceId);
    }

    // ========================
    // AgentStateStore 接口实现
    // ========================

    @Override
    public void save(String userId, String sessionId, String key, State value) {
        Objects.requireNonNull(value, "value");
        String memSessionId = getOrCreateMemorySession(userId, sessionId, key);

        try {
            String json = mapper.writeValueAsString(value);
            String className = value.getClass().getName();
            String content = SINGLE_PREFIX + className + ":" + json;
            TextMessage msg = new TextMessage("system", content);
            memoryClient.addMessages(spaceId, memSessionId, List.of(msg), null, null, false);
        } catch (Exception e) {
            throw new AgentStateStoreException("Failed to save AgentScope state", e);
        }
    }

    @Override
    public void save(String userId, String sessionId, String key, List<? extends State> values) {
        Objects.requireNonNull(values, "values");
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("values must not contain null elements");
        }
        String memSessionId = getOrCreateMemorySession(userId, sessionId, key);

        try {
            List<TextMessage> messages = new ArrayList<>();
            // Batch marker: indicates start of a new list save (enables full-replacement semantics)
            messages.add(new TextMessage("system", LIST_BATCH + values.size()));
            for (State item : values) {
                String json = mapper.writeValueAsString(item);
                String className = item.getClass().getName();
                messages.add(new TextMessage("system", LIST_PREFIX + className + ":" + json));
            }
            memoryClient.addMessages(spaceId, memSessionId, messages, null, null, false);
        } catch (Exception e) {
            throw new AgentStateStoreException("Failed to save AgentScope state list", e);
        }
    }

    @Override
    public <T extends State> Optional<T> get(String userId, String sessionId, String key, Class<T> type) {
        Objects.requireNonNull(type, "type");
        String logicalKey = buildLogicalKey(userId, sessionId, key);
        String memSessionId = sessionCache.get(logicalKey);
        if (memSessionId == null) return Optional.empty();

        try {
            List<MessageInfo> msgs = memoryClient.getLastKMessages(memSessionId, 1, spaceId);
            if (msgs == null || msgs.isEmpty()) return Optional.empty();

            // 从最后一条消息开始查找 __S__: 前缀的消息
            for (int i = msgs.size() - 1; i >= 0; i--) {
                String text = extractText(msgs.get(i));
                if (text != null && text.startsWith(SINGLE_PREFIX)) {
                    String body = text.substring(SINGLE_PREFIX.length());
                    int colonIdx = body.indexOf(':');
                    if (colonIdx > 0) {
                        String json = body.substring(colonIdx + 1);
                        T result = mapper.readValue(json, type);
                        return Optional.of(result);
                    }
                }
            }
        } catch (Exception e) {
            throw new AgentStateStoreException("Failed to load AgentScope state", e);
        }
        return Optional.empty();
    }

    @Override
    public <T extends State> List<T> getList(String userId, String sessionId, String key, Class<T> itemType) {
        Objects.requireNonNull(itemType, "itemType");
        String logicalKey = buildLogicalKey(userId, sessionId, key);
        String memSessionId = sessionCache.get(logicalKey);
        if (memSessionId == null) return List.of();

        try {
            // 获取消息总数
            MessageListResponse first = memoryClient.listMessages(spaceId, memSessionId, 1, 0);
            int total = first.getTotal();
            if (total == 0) return List.of();

            // 获取全部消息
            List<MessageInfo> allMsgs = memoryClient.getLastKMessages(memSessionId, total, spaceId);
            if (allMsgs == null || allMsgs.isEmpty()) return List.of();

            // 从末尾找到最后一个 __LB__: 批次标记，只读取该批次的 __L__: 消息
            int batchStart = -1;
            int batchCount = 0;
            for (int i = allMsgs.size() - 1; i >= 0; i--) {
                String text = extractText(allMsgs.get(i));
                if (text != null && text.startsWith(LIST_BATCH)) {
                    batchStart = i;
                    batchCount = Integer.parseInt(text.substring(LIST_BATCH.length()));
                    break;
                }
            }
            if (batchStart < 0) return List.of();

            // 读取 batchStart 之后的 batchCount 条 __L__: 消息
            List<T> result = new ArrayList<>();
            for (int i = batchStart + 1; i < allMsgs.size() && result.size() < batchCount; i++) {
                String text = extractText(allMsgs.get(i));
                if (text != null && text.startsWith(LIST_PREFIX)) {
                    String body = text.substring(LIST_PREFIX.length());
                    int colonIdx = body.indexOf(':');
                    if (colonIdx > 0) {
                        String json = body.substring(colonIdx + 1);
                        result.add(mapper.readValue(json, itemType));
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new AgentStateStoreException("Failed to load AgentScope state list", e);
        }
    }

    @Override
    public boolean exists(String userId, String sessionId) {
        String prefix = normalizeUserId(userId) + "|" + sessionId + "|";
        return sessionCache.keySet().stream().anyMatch(k -> k.startsWith(prefix));
    }

    @Override
    public void delete(String userId, String sessionId) {
        String prefix = normalizeUserId(userId) + "|" + sessionId + "|";
        sessionCache.keySet().removeIf(k -> k.startsWith(prefix));
        // Memory API 无 deleteSession，数据会在 Space TTL 过期后自动清理
    }

    @Override
    public void delete(String userId, String sessionId, String key) {
        String logicalKey = buildLogicalKey(userId, sessionId, key);
        sessionCache.remove(logicalKey);
    }

    @Override
    public Set<String> listSessionIds(String userId) {
        String normUser = normalizeUserId(userId);
        String prefix = normUser + "|";
        return sessionCache.keySet().stream()
                .filter(k -> k.startsWith(prefix))
                .map(k -> {
                    // 提取 sessionId 部分: normUser|sessionId|key → sessionId
                    String rest = k.substring(prefix.length());
                    int pipeIdx = rest.indexOf('|');
                    return pipeIdx > 0 ? rest.substring(0, pipeIdx) : rest;
                })
                .collect(Collectors.toSet());
    }

    @Override
    public void close() {
        memoryClient.close();
    }

    // ========================
    // 内部方法
    // ========================

    /**
     * 获取或创建 Memory Session。
     * Memory API 要求 Session ID 为 UUID 格式，因此让服务端自动生成，
     * 本地缓存 logicalKey → server-generated UUID 的映射。
     *
     * @return Memory Session ID（UUID），创建失败时返回 null
     */
    private String getOrCreateMemorySession(String userId, String sessionId, String key) {
        String logicalKey = buildLogicalKey(userId, sessionId, key);
        return sessionCache.computeIfAbsent(logicalKey, k -> {
            try {
                // 传 null 让服务端自动生成 UUID 格式的 session ID
                SessionInfo session = memoryClient.createMemorySession(
                        spaceId, null, normalizeUserId(userId), null);
                if (session != null && session.getId() != null) {
                    return session.getId();
                }
            } catch (Exception e) {
                throw new AgentStateStoreException("Failed to create AgentArts Memory session", e);
            }
            throw new AgentStateStoreException(
                    "AgentArts Memory did not return a session identifier");
        });
    }

    /**
     * 构建逻辑键: {normalizedUserId}|{sessionId}|{key}
     */
    private String buildLogicalKey(String userId, String sessionId, String key) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        return normalizeUserId(userId) + "|" + sessionId + "|" + key;
    }

    private String normalizeUserId(String userId) {
        return (userId == null) ? "__anon__" : userId;
    }

    /**
     * 从 MessageInfo 中提取文本内容。
     *
     * <p>每个 part 既可能是结构化对象（{@code {"type":"text","text":...}}），
     * 也可能是后端在内容加密不可读时返回的字符串标记（如 {@code "_encrypted"}）。
     * 非 Map 的 part 一律跳过。</p>
     */
    @SuppressWarnings("unchecked")
    private String extractText(MessageInfo msg) {
        if (msg == null || msg.getParts() == null) return null;
        StringBuilder sb = new StringBuilder();
        for (Object part : msg.getParts()) {
            if (!(part instanceof Map)) continue;
            Map<String, Object> p = (Map<String, Object>) part;
            if ("text".equals(p.get("type")) && p.get("text") != null) {
                sb.append(p.get("text"));
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
