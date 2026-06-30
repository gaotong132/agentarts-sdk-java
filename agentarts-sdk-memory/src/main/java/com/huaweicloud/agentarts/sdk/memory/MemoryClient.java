package com.huaweicloud.agentarts.sdk.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.core.SignMode;
import com.huaweicloud.agentarts.sdk.memory.model.*;
import com.huaweicloud.agentarts.sdk.service.http.BaseHttpClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestConfig;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Memory client with dual-plane architecture.
 *
 * <p>Mirrors Python {@code MemoryClient} from {@code memory/__init__.py}.
 * Control plane operations use AK/SK signing.
 * Data plane operations use API Key (Bearer token) authentication.</p>
 *
 * <h3>Control Plane (AK/SK signed):</h3>
 * <ul>
 *   <li>{@link #createSpace} / {@link #getSpace} / {@link #listSpaces}</li>
 *   <li>{@link #updateSpace} / {@link #deleteSpace} / {@link #createApiKey}</li>
 * </ul>
 *
 * <h3>Data Plane (API Key auth):</h3>
 * <ul>
 *   <li>{@link #createMemorySession} / {@link #addMessages} / {@link #getLastKMessages}</li>
 *   <li>{@link #getMessage} / {@link #listMessages} / {@link #searchMemories}</li>
 *   <li>{@link #getMemory} / {@link #listMemories} / {@link #deleteMemory}</li>
 * </ul>
 */
public class MemoryClient implements AutoCloseable {

    private static final ObjectMapper MAPPER = com.huaweicloud.agentarts.sdk.core.util.JsonUtils.MAPPER;

    private final String regionName;
    private final String apiKey;
    private final boolean verifySsl;

    private BaseHttpClient controlPlaneClient;
    private BaseHttpClient dataPlaneClient;

    public MemoryClient(String regionName, String apiKey, boolean verifySsl) {
        this.regionName = regionName != null ? regionName : Constants.getRegion();
        this.apiKey = apiKey;
        this.verifySsl = verifySsl;
    }

    public MemoryClient(String regionName, String apiKey) {
        this(regionName, apiKey, true);
    }

    public MemoryClient() {
        this(null, null, true);
    }

    // ========================
    // Lazy client initialization
    // ========================

    private synchronized BaseHttpClient getControlPlaneClient() {
        if (controlPlaneClient == null) {
            String endpoint = Constants.getMemoryEndpoint("control", regionName);
            RequestConfig config = RequestConfig.builder()
                    .baseUrl(endpoint)
                    .verifySsl(verifySsl)
                    .build();
            controlPlaneClient = new BaseHttpClient(config, true, SignMode.SDK_HMAC_SHA256, regionName);
        }
        return controlPlaneClient;
    }

    private synchronized BaseHttpClient getDataPlaneClient() {
        if (dataPlaneClient == null) {
            String endpoint = Constants.getMemoryEndpoint("data", regionName);
            RequestConfig config = RequestConfig.builder()
                    .baseUrl(endpoint)
                    .verifySsl(verifySsl)
                    .build();
            dataPlaneClient = new BaseHttpClient(config, false, SignMode.SDK_HMAC_SHA256, regionName);
            // Set API key auth
            String key = apiKey;
            if (key == null || key.isEmpty()) {
                key = System.getenv("HUAWEICLOUD_SDK_MEMORY_API_KEY");
            }
            if (key != null && !key.isEmpty()) {
                dataPlaneClient.setAuthToken("Bearer", key);
            }
        }
        return dataPlaneClient;
    }

    // ========================
    // Control Plane: Space management
    // ========================

    public SpaceInfo createSpace(String name, int messageTtlHours, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("message_ttl_hours", messageTtlHours);
        if (description != null) body.put("description", description);

        RequestResult result = getControlPlaneClient()
                .post("/v1/spaces", null, body).block();
        return parseResult(result, SpaceInfo.class);
    }

    public SpaceInfo createSpace(String name) {
        return createSpace(name, 168, null);
    }

    public SpaceInfo getSpace(String spaceId) {
        RequestResult result = getControlPlaneClient()
                .get("/v1/spaces/" + spaceId).block();
        return parseResult(result, SpaceInfo.class);
    }

    public SpaceListResponse listSpaces(int limit, int offset) {
        String url = "/v1/spaces?limit=" + limit + "&offset=" + offset;
        RequestResult result = getControlPlaneClient().get(url).block();
        return parseResult(result, SpaceListResponse.class);
    }

    public SpaceListResponse listSpaces() {
        return listSpaces(20, 0);
    }

    public SpaceInfo updateSpace(String spaceId, String name, String description, Integer messageTtlHours) {
        Map<String, Object> body = new HashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (messageTtlHours != null) body.put("message_ttl_hours", messageTtlHours);

        RequestResult result = getControlPlaneClient()
                .put("/v1/spaces/" + spaceId, null, body).block();
        return parseResult(result, SpaceInfo.class);
    }

    public void deleteSpace(String spaceId) {
        getControlPlaneClient().delete("/v1/spaces/" + spaceId).block();
    }

    public Map<String, Object> createApiKey() {
        RequestResult result = getControlPlaneClient()
                .post("/v1/api-keys", null, Map.of()).block();
        return parseResultAsMap(result);
    }

    // ========================
    // Data Plane: Session management
    // ========================

    public SessionInfo createMemorySession(String spaceId, String id, String actorId, String assistantId) {
        Map<String, Object> body = new HashMap<>();
        if (id != null) body.put("id", id);
        if (actorId != null) body.put("actor_id", actorId);
        if (assistantId != null) body.put("assistant_id", assistantId);

        RequestResult result = getDataPlaneClient()
                .post("/v1/spaces/" + spaceId + "/sessions", null, body).block();
        return parseResult(result, SessionInfo.class);
    }

    public SessionInfo createMemorySession(String spaceId) {
        return createMemorySession(spaceId, null, null, null);
    }

    // ========================
    // Data Plane: Messages
    // ========================

    public MessageBatchResponse addMessages(String spaceId, String sessionId,
                                             List<?> messages,
                                             Long timestamp, String idempotencyKey,
                                             boolean isForceExtract) {
        List<Map<String, Object>> msgDicts = new ArrayList<>();
        for (Object msg : messages) {
            if (msg instanceof TextMessage tm) {
                msgDicts.add(tm.toDict());
            } else if (msg instanceof ToolCallMessage tcm) {
                msgDicts.add(tcm.toDict());
            } else if (msg instanceof ToolResultMessage trm) {
                msgDicts.add(trm.toDict());
            } else if (msg instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) msg;
                msgDicts.add(m);
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("messages", msgDicts);
        if (timestamp != null) body.put("timestamp", timestamp);
        if (idempotencyKey != null) body.put("idempotency_key", idempotencyKey);
        body.put("is_force_extract", isForceExtract);

        String url = "/v1/spaces/" + spaceId + "/sessions/" + sessionId + "/messages";
        RequestResult result = getDataPlaneClient().post(url, null, body).block();
        return parseResult(result, MessageBatchResponse.class);
    }

    public MessageBatchResponse addMessages(String spaceId, String sessionId, List<?> messages) {
        return addMessages(spaceId, sessionId, messages, null, null, false);
    }

    public List<MessageInfo> getLastKMessages(String sessionId, int k, String spaceId) {
        String url = "/v1/spaces/" + spaceId + "/sessions/" + sessionId + "/messages/last-k?k=" + k;
        RequestResult result = getDataPlaneClient().get(url).block();
        return parseResultAsList(result, MessageInfo.class);
    }

    public MessageInfo getMessage(String messageId, String spaceId, String sessionId) {
        String url = "/v1/spaces/" + spaceId + "/sessions/" + sessionId + "/messages/" + messageId;
        RequestResult result = getDataPlaneClient().get(url).block();
        return parseResult(result, MessageInfo.class);
    }

    public MessageListResponse listMessages(String spaceId, String sessionId, int limit, int offset) {
        String url = "/v1/spaces/" + spaceId + "/messages?limit=" + limit + "&offset=" + offset;
        if (sessionId != null) url += "&session_id=" + sessionId;
        RequestResult result = getDataPlaneClient().get(url).block();
        return parseResult(result, MessageListResponse.class);
    }

    public MessageListResponse listMessages(String spaceId) {
        return listMessages(spaceId, null, 10, 0);
    }

    // ========================
    // Data Plane: Memories
    // ========================

    public MemorySearchResponse searchMemories(String spaceId, MemorySearchFilter filters) {
        Map<String, Object> body = filters != null ? filters.toDict() : Map.of();
        String url = "/v1/spaces/" + spaceId + "/memories/search";
        RequestResult result = getDataPlaneClient().post(url, null, body).block();
        return parseResult(result, MemorySearchResponse.class);
    }

    public MemorySearchResponse searchMemories(String spaceId) {
        return searchMemories(spaceId, null);
    }

    public MemoryListResponse listMemories(String spaceId, int limit, int offset, MemoryListFilter filters) {
        StringBuilder url = new StringBuilder("/v1/spaces/" + spaceId + "/memories?limit=" + limit + "&offset=" + offset);
        if (filters != null) {
            Map<String, Object> f = filters.toDict();
            for (Map.Entry<String, Object> e : f.entrySet()) {
                url.append("&").append(e.getKey()).append("=").append(e.getValue());
            }
        }
        RequestResult result = getDataPlaneClient().get(url.toString()).block();
        return parseResult(result, MemoryListResponse.class);
    }

    public MemoryListResponse listMemories(String spaceId) {
        return listMemories(spaceId, 10, 0, null);
    }

    public MemoryInfo getMemory(String spaceId, String memoryId) {
        String url = "/v1/spaces/" + spaceId + "/memories/" + memoryId;
        RequestResult result = getDataPlaneClient().get(url).block();
        return parseResult(result, MemoryInfo.class);
    }

    public void deleteMemory(String spaceId, String memoryId) {
        String url = "/v1/spaces/" + spaceId + "/memories/" + memoryId;
        getDataPlaneClient().delete(url).block();
    }

    // ========================
    // Helpers
    // ========================

    private <T> T parseResult(RequestResult result, Class<T> type) {
        if (result == null || !result.isSuccess()) {
            String err = result != null ? result.getError() : "null response";
            throw new RuntimeException("API call failed: " + err);
        }
        try {
            JsonNode data = result.getDataAsJson();
            if (data != null) {
                return MAPPER.treeToValue(data, type);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response", e);
        }
    }

    private Map<String, Object> parseResultAsMap(RequestResult result) {
        if (result == null || !result.isSuccess()) {
            throw new RuntimeException("API call failed");
        }
        try {
            JsonNode data = result.getDataAsJson();
            if (data != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = MAPPER.treeToValue(data, Map.class);
                return map;
            }
            return Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response", e);
        }
    }

    private <T> List<T> parseResultAsList(RequestResult result, Class<T> type) {
        if (result == null || !result.isSuccess()) {
            throw new RuntimeException("API call failed");
        }
        try {
            JsonNode data = result.getDataAsJson();
            if (data != null && data.isArray()) {
                List<T> list = new ArrayList<>();
                for (JsonNode node : data) {
                    list.add(MAPPER.treeToValue(node, type));
                }
                return list;
            }
            return List.of();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response", e);
        }
    }

    @Override
    public void close() {
        if (controlPlaneClient != null) controlPlaneClient.close();
        if (dataPlaneClient != null) dataPlaneClient.close();
    }

    public String getRegionName() { return regionName; }
}
