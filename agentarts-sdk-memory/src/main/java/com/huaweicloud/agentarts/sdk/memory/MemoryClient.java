package com.huaweicloud.agentarts.sdk.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.agentarts.sdk.core.APIException;
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
 * <p>Memory client with dual-plane architecture (AK/SK control + API Key data).
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

    /**
     * Create a MemoryClient with full configuration.
     *
     * @param regionName Huawei Cloud region (nullable, auto-detected if null)
     * @param apiKey     data plane API key for Bearer auth (nullable, falls back to env)
     * @param verifySsl  whether to verify SSL certificates
     */
    public MemoryClient(String regionName, String apiKey, boolean verifySsl) {
        this.regionName = regionName != null ? regionName : Constants.getRegion();
        this.apiKey = apiKey;
        this.verifySsl = verifySsl;
    }

    /** Create a MemoryClient with SSL verification enabled. */
    public MemoryClient(String regionName, String apiKey) {
        this(regionName, apiKey, true);
    }

    /** Create a MemoryClient with default region and SSL settings. */
    public MemoryClient() {
        this(null, null, true);
    }

    // ========================
    // Lazy client initialization
    // ========================

    private synchronized BaseHttpClient getControlPlaneClient() {
        if (controlPlaneClient == null) {
            String endpoint = Constants.getMemoryEndpoint("control", regionName) + "/v1/core";
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
            String endpoint = Constants.getMemoryEndpoint("data", regionName) + "/v1/core";
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
            if (com.huaweicloud.agentarts.sdk.core.util.JsonUtils.isNotBlank(key)) {
                dataPlaneClient.setAuthToken("Bearer", key);
            }
        }
        return dataPlaneClient;
    }

    // ========================
    // Control Plane: Space management
    // ========================

    /**
     * Create a memory space (Control Plane, AK/SK signed).
     *
     * @param name             space name
     * @param messageTtlHours  message TTL in hours (default 168 = 7 days)
     * @param description      optional description
     * @return created space info
     */
    public SpaceInfo createSpace(String name, int messageTtlHours, String description) {
        // Create API key first — space creation requires api_key_id
        Map<String, Object> keyResult = createApiKey();
        String apiKeyId = keyResult != null ? (String) keyResult.get("id") : null;
        String apiKeyValue = keyResult != null ? (String) keyResult.get("api_key") : null;

        CreateSpaceRequest req = new CreateSpaceRequest()
                .withName(name)
                .withMessageTtlHours(messageTtlHours)
                .withDescription(description)
                .withApiKeyId(apiKeyId)
                .withNetworkAccess(Map.of("public_access_enable", true))
                .withMemoryStrategiesBuiltin(List.of("semantic", "user_preference", "episodic"));

        RequestResult result = getControlPlaneClient()
                .post("/spaces", null, req).block();
        SpaceInfo space = parseResult(result, SpaceInfo.class);
        // Inject the api_key into the response (server doesn't return it)
        if (space != null && apiKeyValue != null) {
            space.setApiKey(apiKeyValue);
        }
        return space;
    }

    /** Create a memory space with default TTL (168 hours). */
    public SpaceInfo createSpace(String name) {
        return createSpace(name, 168, null);
    }

    /** Get a memory space by ID (Control Plane). */
    public SpaceInfo getSpace(String spaceId) {
        RequestResult result = getControlPlaneClient()
                .get("/spaces/" + spaceId).block();
        return parseResult(result, SpaceInfo.class);
    }

    /** List memory spaces with pagination (Control Plane). */
    public SpaceListResponse listSpaces(int limit, int offset) {
        String url = "/spaces?limit=" + limit + "&offset=" + offset;
        RequestResult result = getControlPlaneClient().get(url).block();
        return parseResult(result, SpaceListResponse.class);
    }

    /** List memory spaces with default pagination (limit=20, offset=0). */
    public SpaceListResponse listSpaces() {
        return listSpaces(20, 0);
    }

    /** Update a memory space (Control Plane). Null fields are not updated. */
    public SpaceInfo updateSpace(String spaceId, String name, String description, Integer messageTtlHours) {
        UpdateSpaceRequest req = new UpdateSpaceRequest()
                .withName(name)
                .withDescription(description)
                .withMessageTtlHours(messageTtlHours);

        RequestResult result = getControlPlaneClient()
                .put("/spaces/" + spaceId, null, req).block();
        return parseResult(result, SpaceInfo.class);
    }

    /** Delete a memory space (Control Plane). */
    public void deleteSpace(String spaceId) {
        getControlPlaneClient().delete("/spaces/" + spaceId).block();
    }

    /** Create an API key for data plane access (Control Plane). */
    public Map<String, Object> createApiKey() {
        RequestResult result = getControlPlaneClient()
                .post("/space-keys", null, Map.of()).block();
        return parseResultAsMap(result);
    }

    // ========================
    // Data Plane: Session management
    // ========================

    /**
     * Create a memory session (Data Plane, API Key auth).
     *
     * @param spaceId     space ID
     * @param id          optional session ID (auto-generated if null)
     * @param actorId     optional actor identifier
     * @param assistantId optional assistant identifier
     * @return created session info
     */
    public SessionInfo createMemorySession(String spaceId, String id, String actorId, String assistantId) {
        CreateMemorySessionRequest req = new CreateMemorySessionRequest()
                .withId(id)
                .withActorId(actorId)
                .withAssistantId(assistantId);

        RequestResult result = getDataPlaneClient()
                .post("/spaces/" + spaceId + "/sessions", null, req).block();
        return parseResult(result, SessionInfo.class);
    }

    /** Create a memory session with auto-generated ID (Data Plane). */
    public SessionInfo createMemorySession(String spaceId) {
        return createMemorySession(spaceId, null, null, null);
    }

    // ========================
    // Data Plane: Messages
    // ========================

    /**
     * Add messages to a session (Data Plane).
     *
     * @param spaceId          space ID
     * @param sessionId        session ID
     * @param messages         list of TextMessage, ToolCallMessage, or ToolResultMessage
     * @param timestamp        optional message timestamp
     * @param idempotencyKey   optional idempotency key
     * @param isForceExtract   whether to force memory extraction
     * @return batch response with added messages
     */
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

        AddMessagesRequest req = new AddMessagesRequest()
                .withMessages(msgDicts)
                .withTimestamp(timestamp)
                .withIdempotencyKey(idempotencyKey)
                .withIsForceExtract(isForceExtract);

        String url = "/spaces/" + spaceId + "/sessions/" + sessionId + "/messages";
        RequestResult result = getDataPlaneClient().post(url, null, req).block();
        return parseResult(result, MessageBatchResponse.class);
    }

    /** Add messages with default options (Data Plane). */
    public MessageBatchResponse addMessages(String spaceId, String sessionId, List<?> messages) {
        return addMessages(spaceId, sessionId, messages, null, null, false);
    }

    /** Get the last K messages from a session (Data Plane). */
    public List<MessageInfo> getLastKMessages(String sessionId, int k, String spaceId) {
        // First call to get total count
        MessageListResponse first = listMessages(spaceId, sessionId, 1, 0);
        int total = first.getTotal();
        int offset = Math.max(0, total - k);
        // Second call to get last K messages
        MessageListResponse result = listMessages(spaceId, sessionId, k, offset);
        return result.getItems() != null ? result.getItems() : List.of();
    }

    /** Get a single message by ID (Data Plane). */
    public MessageInfo getMessage(String messageId, String spaceId, String sessionId) {
        String url = "/spaces/" + spaceId + "/sessions/" + sessionId + "/messages/" + messageId;
        RequestResult result = getDataPlaneClient().get(url).block();
        return parseResult(result, MessageInfo.class);
    }

    /** List messages with pagination (Data Plane). */
    public MessageListResponse listMessages(String spaceId, String sessionId, int limit, int offset) {
        String url;
        if (sessionId != null) {
            url = "/spaces/" + spaceId + "/sessions/" + sessionId + "/messages?limit=" + limit + "&offset=" + offset;
        } else {
            url = "/spaces/" + spaceId + "/messages?limit=" + limit + "&offset=" + offset;
        }
        RequestResult result = getDataPlaneClient().get(url).block();
        return parseResult(result, MessageListResponse.class);
    }

    /** List messages with default pagination (Data Plane). */
    public MessageListResponse listMessages(String spaceId) {
        return listMessages(spaceId, null, 10, 0);
    }

    // ========================
    // Data Plane: Memories
    // ========================

    /** Search memories with optional filters (Data Plane). */
    public MemorySearchResponse searchMemories(String spaceId, MemorySearchFilter filters) {
        Map<String, Object> body = filters != null ? filters.toDict() : Map.of();
        String url = "/spaces/" + spaceId + "/memories/search";
        RequestResult result = getDataPlaneClient().post(url, null, body).block();
        return parseResult(result, MemorySearchResponse.class);
    }

    /** Search memories without filters (Data Plane). */
    public MemorySearchResponse searchMemories(String spaceId) {
        return searchMemories(spaceId, null);
    }

    /** List memories with pagination and optional filters (Data Plane). */
    public MemoryListResponse listMemories(String spaceId, int limit, int offset, MemoryListFilter filters) {
        StringBuilder url = new StringBuilder("/spaces/" + spaceId + "/memories?limit=" + limit + "&offset=" + offset);
        if (filters != null) {
            Map<String, Object> f = filters.toDict();
            for (Map.Entry<String, Object> e : f.entrySet()) {
                url.append("&").append(e.getKey()).append("=").append(e.getValue());
            }
        }
        RequestResult result = getDataPlaneClient().get(url.toString()).block();
        return parseResult(result, MemoryListResponse.class);
    }

    /** List memories with default pagination (Data Plane). */
    public MemoryListResponse listMemories(String spaceId) {
        return listMemories(spaceId, 10, 0, null);
    }

    /** Get a memory by ID (Data Plane). */
    public MemoryInfo getMemory(String spaceId, String memoryId) {
        String url = "/spaces/" + spaceId + "/memories/" + memoryId;
        RequestResult result = getDataPlaneClient().get(url).block();
        return parseResult(result, MemoryInfo.class);
    }

    /** Delete a memory by ID (Data Plane). */
    public void deleteMemory(String spaceId, String memoryId) {
        String url = "/spaces/" + spaceId + "/memories/" + memoryId;
        getDataPlaneClient().delete(url).block();
    }

    // ========================
    // Helpers
    // ========================

    private <T> T parseResult(RequestResult result, Class<T> type) {
        if (result == null || !result.isSuccess()) {
            int status = result != null ? result.getStatusCode() : 0;
            String err = result != null ? result.getError() : "null response";
            throw new APIException(status, "memory_api", err);
        }
        try {
            JsonNode data = result.getDataAsJson();
            if (data != null) {
                return MAPPER.treeToValue(data, type);
            }
            return null;
        } catch (Exception e) {
            throw new APIException(result.getStatusCode(), "memory_api",
                    "Failed to parse response: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> parseResultAsMap(RequestResult result) {
        if (result == null || !result.isSuccess()) {
            int status = result != null ? result.getStatusCode() : 0;
            String err = result != null ? result.getError() : "null response";
            throw new APIException(status, "memory_api", err);
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
            throw new APIException(result.getStatusCode(), "memory_api",
                    "Failed to parse response: " + e.getMessage(), e);
        }
    }

    private <T> List<T> parseResultAsList(RequestResult result, Class<T> type) {
        if (result == null || !result.isSuccess()) {
            int status = result != null ? result.getStatusCode() : 0;
            String err = result != null ? result.getError() : "null response";
            throw new APIException(status, "memory_api", err);
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
            throw new APIException(result.getStatusCode(), "memory_api",
                    "Failed to parse response: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void close() {
        if (controlPlaneClient != null) {
            controlPlaneClient.close();
            controlPlaneClient = null;
        }
        if (dataPlaneClient != null) {
            dataPlaneClient.close();
            dataPlaneClient = null;
        }
    }

    public String getRegionName() { return regionName; }
}
