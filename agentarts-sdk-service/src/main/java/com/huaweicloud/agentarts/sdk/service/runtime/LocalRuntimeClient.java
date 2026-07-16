package com.huaweicloud.agentarts.sdk.service.runtime;

import com.huaweicloud.agentarts.sdk.core.APIException;
import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import com.huaweicloud.agentarts.sdk.core.util.UrlUtils;
import com.huaweicloud.agentarts.sdk.service.http.BaseHttpClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestConfig;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Local runtime client for development — connects to a local AgentArts Runtime server.
 *
 * <p>No signing or cloud credentials needed. Connects via plain HTTP to
 * {@code http://{host}:{port}}.</p>
 */
public class LocalRuntimeClient implements AutoCloseable {

    private final BaseHttpClient httpClient;
    private final int port;
    private final String host;

    /**
     * Create a local runtime client.
     *
     * @param port    server port (default 8080)
     * @param host    server host (default localhost)
     * @param timeout request timeout in seconds (default 300)
     */
    public LocalRuntimeClient(int port, String host, int timeout) {
        this.port = port > 0 ? port : 8080;
        this.host = host != null ? host : "localhost";
        String baseUrl = "http://" + this.host + ":" + this.port;
        RequestConfig config = RequestConfig.builder()
                .baseUrl(baseUrl)
                .timeoutSeconds(timeout > 0 ? timeout : 300)
                .verifySsl(false)
                .build();
        this.httpClient = new BaseHttpClient(config);
    }

    public LocalRuntimeClient(int port) {
        this(port, "localhost", 300);
    }

    public LocalRuntimeClient() {
        this(8080, "localhost", 300);
    }

    /**
     * Invoke the local agent.
     *
     * @param payload     JSON payload string
     * @param sessionId   optional session ID
     * @param bearerToken optional Bearer token
     * @param userId      optional user ID
     * @param customPath  optional custom path (appended to /invocations)
     * @return invocation result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> invokeAgent(String payload, String sessionId,
                                            String bearerToken, String userId,
                                            String customPath) {
        return invokeAgent(payload, sessionId, bearerToken, null, userId, customPath);
    }

    /** Invoke the local agent with the complete Python SDK-compatible option set. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> invokeAgent(String payload, String sessionId,
                                            String bearerToken, String endpoint,
                                            String userId, String customPath) {
        try (RequestResult result = invokeAgentRaw(
                payload, sessionId, bearerToken, endpoint, userId, customPath)) {
            if (result.isStreaming()) {
                throw new APIException(result.getStatusCode(), "invoke_agent",
                        "streaming response requires invokeAgentRaw");
            }
            Object data = result.getData();
            if (data instanceof Map) return (Map<String, Object>) data;
            return Map.of();
        }
    }

    /** Invoke and return the raw response, including streaming responses. */
    public RequestResult invokeAgentRaw(String payload, String sessionId,
                                         String bearerToken, String endpoint,
                                         String userId, String customPath) {
        String path = "/invocations";
        if (JsonUtils.isNotBlank(customPath)) {
            path += "/" + UrlUtils.encodeRelativePath(customPath, "customPath");
        }

        Map<String, String> headers = new LinkedHashMap<>();
        if (JsonUtils.isNotBlank(sessionId)) {
            headers.put(Constants.SESSION_HEADER, sessionId);
        }
        if (JsonUtils.isNotBlank(userId)) {
            headers.put(Constants.USER_ID_HEADER, userId);
        }
        if (JsonUtils.isNotBlank(bearerToken)) {
            headers.put("Authorization", "Bearer " + bearerToken);
        }

        Map<String, java.util.List<String>> query = JsonUtils.isNotBlank(endpoint)
                ? Map.of("endpoint", java.util.List.of(endpoint)) : null;
        RequestResult result = httpClient.request(
                "POST", path, headers.isEmpty() ? null : headers, payload, query).block();
        if (result == null || !result.isSuccess()) {
            int status = result != null ? result.getStatusCode() : 0;
            String err = result != null ? result.getError() : "null response";
            if (result != null) {
                try (result) {
                    throw new APIException(status, "invoke_agent", err);
                }
            }
            throw new APIException(status, "invoke_agent", err);
        }
        return result;
    }

    public Map<String, Object> invokeAgent(String payload) {
        return invokeAgent(payload, null, null, null, null);
    }

    /**
     * Ping the local agent health endpoint.
     *
     * @return ping result (status, time_of_last_update)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> pingAgent(String sessionId) {
        Map<String, String> headers = null;
        if (JsonUtils.isNotBlank(sessionId)) {
            headers = Map.of(Constants.SESSION_HEADER, sessionId);
        }

        RequestResult result = httpClient.get("/ping", headers).block();
        if (result == null || !result.isSuccess()) {
            int status = result != null ? result.getStatusCode() : 0;
            String err = result != null ? result.getError() : "null response";
            throw new APIException(status, "ping_agent", err);
        }
        Object data = result.getData();
        if (data instanceof Map) return (Map<String, Object>) data;
        return Map.of();
    }

    public Map<String, Object> pingAgent() {
        return pingAgent(null);
    }

    public int getPort() { return port; }
    public String getHost() { return host; }

    @Override
    public void close() {
        httpClient.close();
    }
}
