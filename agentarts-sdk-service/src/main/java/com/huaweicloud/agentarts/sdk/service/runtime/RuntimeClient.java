package com.huaweicloud.agentarts.sdk.service.runtime;

import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.core.SignMode;
import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import com.huaweicloud.agentarts.sdk.service.http.BaseHttpClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestConfig;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Runtime client for managing AgentArts Runtime agents and data-plane operations.
 *
 * <p>Dual-plane architecture:</p>
 * <ul>
 *   <li><b>Control Plane</b> (AK/SK signed) — agent CRUD, endpoint CRUD</li>
 *   <li><b>Data Plane</b> (configurable signing or Bearer token) — invoke, exec, upload, download, sessions</li>
 * </ul>
 *
 * <h3>Control Plane paths:</h3>
 * <pre>
 * POST/GET   /v1/core/runtimes
 * GET/PUT/DELETE /v1/core/runtimes/{agentId}
 * POST/GET/PUT/DELETE /v1/core/runtimes/{agentId}/endpoints/{name}
 * </pre>
 *
 * <h3>Data Plane paths:</h3>
 * <pre>
 * POST /runtimes/{name}/invocations
 * POST /runtimes/{name}/commands
 * POST /runtimes/{name}/upload-files
 * GET  /runtimes/{name}/download-files
 * POST /runtimes/{name}/sessions-start
 * POST /runtimes/{name}/sessions-stop
 * </pre>
 */
public class RuntimeClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeClient.class);

    private final String region;
    private final boolean verifySsl;
    private final SignMode signMode;

    private BaseHttpClient controlClient;
    private BaseHttpClient dataClient;

    /**
     * Create a RuntimeClient with full configuration.
     *
     * @param region    Huawei Cloud region (nullable, auto-detected)
     * @param verifySsl whether to verify SSL certificates
     * @param signMode  signing mode for data plane (default SDK_HMAC_SHA256)
     */
    public RuntimeClient(String region, boolean verifySsl, SignMode signMode) {
        this.region = region != null ? region : Constants.getRegion();
        this.verifySsl = verifySsl;
        this.signMode = signMode != null ? signMode : SignMode.SDK_HMAC_SHA256;
    }

    public RuntimeClient(String region, boolean verifySsl) {
        this(region, verifySsl, SignMode.SDK_HMAC_SHA256);
    }

    public RuntimeClient(String region) {
        this(region, true, SignMode.SDK_HMAC_SHA256);
    }

    public RuntimeClient() {
        this(null, true, SignMode.SDK_HMAC_SHA256);
    }

    /**
     * Set Bearer token for data plane authentication.
     */
    public void setAuthToken(String token) {
        getDataClient().setAuthToken("Bearer", token);
    }

    // ========================
    // Lazy client initialization
    // ========================

    private synchronized BaseHttpClient getControlClient() {
        if (controlClient == null) {
            String endpoint = Constants.getControlPlaneEndpoint(region) + "/v1/core";
            RequestConfig config = RequestConfig.builder()
                    .baseUrl(endpoint)
                    .verifySsl(verifySsl)
                    .build();
            controlClient = new BaseHttpClient(config, true, SignMode.SDK_HMAC_SHA256, region);
        }
        return controlClient;
    }

    private synchronized BaseHttpClient getDataClient() {
        if (dataClient == null) {
            String endpoint = Constants.getRuntimeDataPlaneEndpoint();
            boolean useAkSk = (signMode == SignMode.V11_HMAC_SHA256);
            RequestConfig config = RequestConfig.builder()
                    .baseUrl(endpoint)
                    .verifySsl(verifySsl)
                    .build();
            dataClient = new BaseHttpClient(config, useAkSk, signMode, region);
        }
        return dataClient;
    }

    // ========================
    // Control Plane: Agent CRUD
    // ========================

    /**
     * Create a Runtime agent.
     *
     * @param name                  agent name
     * @param description           agent description
     * @param artifactSourceConfig  artifact source configuration (nullable)
     * @param identityConfig        identity configuration (nullable)
     * @param invokeConfig          invoke configuration (nullable)
     * @param networkConfig         network configuration (nullable)
     * @param observabilityConfig   observability configuration (nullable)
     * @param executionAgencyName   execution agency name (nullable)
     * @param agentGatewayId        agent gateway ID (nullable)
     * @param envVars               environment variables as list of {key, value} maps (nullable)
     * @param tagsConfig            tags as list of {key, value} maps (nullable)
     * @return created agent data
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createAgent(String name, String description,
                                            Map<String, Object> artifactSourceConfig,
                                            Map<String, Object> identityConfig,
                                            Map<String, Object> invokeConfig,
                                            Map<String, Object> networkConfig,
                                            Map<String, Object> observabilityConfig,
                                            String executionAgencyName,
                                            String agentGatewayId,
                                            List<Map<String, String>> envVars,
                                            List<Map<String, String>> tagsConfig) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        if (JsonUtils.isNotBlank(description)) body.put("description", description);
        if (artifactSourceConfig != null) body.put("artifact_source_config", artifactSourceConfig);
        if (identityConfig != null) body.put("identity_config", identityConfig);
        if (invokeConfig != null) body.put("invoke_config", invokeConfig);
        if (networkConfig != null) body.put("network_config", networkConfig);
        if (observabilityConfig != null) body.put("observability_config", observabilityConfig);
        if (JsonUtils.isNotBlank(executionAgencyName)) body.put("execution_agency_name", executionAgencyName);
        if (JsonUtils.isNotBlank(agentGatewayId)) body.put("agent_gateway_id", agentGatewayId);
        if (envVars != null) body.put("env_vars", envVars);
        if (tagsConfig != null) body.put("tags_config", tagsConfig);

        RequestResult result = getControlClient().post("/runtimes", body).block();
        return check(result, "create_agent");
    }

    /** Convenience overload with required parameters only. */
    public Map<String, Object> createAgent(String name, String description) {
        return createAgent(name, description, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Update a Runtime agent by ID.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> updateAgent(String agentId, String description,
                                            Map<String, Object> artifactSourceConfig,
                                            Map<String, Object> invokeConfig,
                                            Map<String, Object> networkConfig,
                                            Map<String, Object> observabilityConfig,
                                            String executionAgencyName,
                                            String agentGatewayId,
                                            List<Map<String, String>> envVars,
                                            List<Map<String, String>> tagsConfig) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (JsonUtils.isNotBlank(description)) body.put("description", description);
        if (artifactSourceConfig != null) body.put("artifact_source_config", artifactSourceConfig);
        if (invokeConfig != null) body.put("invoke_config", invokeConfig);
        if (networkConfig != null) body.put("network_config", networkConfig);
        if (observabilityConfig != null) body.put("observability_config", observabilityConfig);
        if (JsonUtils.isNotBlank(executionAgencyName)) body.put("execution_agency_name", executionAgencyName);
        if (JsonUtils.isNotBlank(agentGatewayId)) body.put("agent_gateway_id", agentGatewayId);
        if (envVars != null) body.put("env_vars", envVars);
        if (tagsConfig != null) body.put("tags_config", tagsConfig);

        RequestResult result = getControlClient().put("/runtimes/" + agentId, body).block();
        return check(result, "update_agent");
    }

    /**
     * Create or update an agent: find by name first, then create or update.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createOrUpdateAgent(String agentName, String description,
                                                    Map<String, Object> artifactSourceConfig,
                                                    Map<String, Object> identityConfig,
                                                    Map<String, Object> invokeConfig,
                                                    Map<String, Object> networkConfig,
                                                    Map<String, Object> observabilityConfig,
                                                    String executionAgencyName,
                                                    String agentGatewayId,
                                                    List<Map<String, String>> envVars,
                                                    List<Map<String, String>> tagsConfig) {
        Map<String, Object> existing = findAgentByName(agentName);
        if (existing != null) {
            String agentId = (String) existing.get("id");
            return updateAgent(agentId, description, artifactSourceConfig, invokeConfig,
                    networkConfig, observabilityConfig, executionAgencyName, agentGatewayId,
                    envVars, tagsConfig);
        } else {
            return createAgent(agentName, description, artifactSourceConfig, identityConfig,
                    invokeConfig, networkConfig, observabilityConfig, executionAgencyName,
                    agentGatewayId, envVars, tagsConfig);
        }
    }

    /**
     * List agents with optional filtering and pagination.
     *
     * @param agentName filter by name (nullable)
     * @param offset    pagination offset (default 1)
     * @param limit     page size (default 10)
     * @return list of agent maps
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAgents(String agentName, int offset, int limit) {
        StringBuilder url = new StringBuilder("/runtimes?offset=").append(offset).append("&limit=").append(limit);
        if (JsonUtils.isNotBlank(agentName)) {
            url.append("&name=").append(agentName);
        }
        RequestResult result = getControlClient().get(url.toString()).block();
        Map<String, Object> data = check(result, "get_agents");
        Object items = data.get("items");
        if (items instanceof List) {
            return (List<Map<String, Object>>) items;
        }
        return List.of();
    }

    /** List agents with default pagination. */
    public List<Map<String, Object>> getAgents() {
        return getAgents(null, 1, 10);
    }

    /**
     * Find an agent by name.
     *
     * @return agent data map, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> findAgentByName(String agentName) {
        List<Map<String, Object>> agents = getAgents(agentName, 1, 1);
        if (agents.isEmpty()) return null;
        Map<String, Object> first = agents.get(0);
        if (agentName.equals(first.get("name"))) return first;
        return null;
    }

    /**
     * Find an agent by ID.
     *
     * @return agent data map, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> findAgentById(String agentId) {
        try {
            RequestResult result = getControlClient().get("/runtimes/" + agentId).block();
            if (result != null && result.isSuccess()) {
                Object data = result.getData();
                if (data instanceof Map) return (Map<String, Object>) data;
            }
        } catch (Exception e) {
            LOG.debug("findAgentById failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Delete an agent by name (finds by name first, then deletes by ID).
     *
     * @return true if deleted, false if not found
     */
    public boolean deleteAgentByName(String agentName) {
        Map<String, Object> agent = findAgentByName(agentName);
        if (agent == null) return false;
        String agentId = (String) agent.get("id");
        RequestResult result = getControlClient().delete("/runtimes/" + agentId).block();
        return result != null && result.isSuccess();
    }

    // ========================
    // Control Plane: Endpoint CRUD
    // ========================

    /**
     * Create an agent endpoint.
     */
    public Map<String, Object> createAgentEndpoint(String agentId, String endpointName,
                                                     String endpointType, Map<String, Object> config) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("endpoint_name", endpointName);
        if (JsonUtils.isNotBlank(endpointType)) body.put("endpoint_type", endpointType);
        if (config != null) body.put("config", config);

        RequestResult result = getControlClient().post("/runtimes/" + agentId + "/endpoints", body).block();
        return check(result, "create_agent_endpoint");
    }

    public Map<String, Object> createAgentEndpoint(String agentId, String endpointName) {
        return createAgentEndpoint(agentId, endpointName, "invocations", null);
    }

    /**
     * Update an agent endpoint.
     */
    public Map<String, Object> updateAgentEndpoint(String agentId, String endpointName,
                                                     Map<String, Object> config) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (config != null) body.put("config", config);

        RequestResult result = getControlClient().put(
                "/runtimes/" + agentId + "/endpoints/" + endpointName, body).block();
        return check(result, "update_agent_endpoint");
    }

    /**
     * Delete an agent endpoint.
     */
    public Map<String, Object> deleteAgentEndpoint(String agentId, String endpointName) {
        RequestResult result = getControlClient().delete(
                "/runtimes/" + agentId + "/endpoints/" + endpointName).block();
        return check(result, "delete_agent_endpoint");
    }

    /**
     * Find an agent endpoint.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> findAgentEndpoint(String agentId, String endpointName) {
        RequestResult result = getControlClient().get(
                "/runtimes/" + agentId + "/endpoints/" + endpointName).block();
        return check(result, "find_agent_endpoint");
    }

    // ========================
    // Data Plane: Invoke / Exec
    // ========================

    /**
     * Invoke a runtime agent with a JSON payload.
     *
     * @param agentName   agent name
     * @param sessionId   session ID (nullable)
     * @param payload     JSON payload string
     * @param bearerToken optional Bearer token override
     * @param endpoint    optional custom endpoint URL
     * @param timeout     request timeout in seconds
     * @param userId      optional user ID header
     * @param customPath  optional custom path appended to invocations
     * @return invocation result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> invokeAgent(String agentName, String sessionId, String payload,
                                            String bearerToken, String endpoint, int timeout,
                                            String userId, String customPath) {
        String path = "/runtimes/" + agentName + "/invocations";
        if (JsonUtils.isNotBlank(customPath)) {
            path += "/" + customPath;
        }

        Map<String, String> headers = buildDataHeaders(sessionId, bearerToken, userId);
        RequestResult result = getDataClient().post(path, headers, payload).block();
        return check(result, "invoke_agent");
    }

    /** Convenience overload with defaults. */
    public Map<String, Object> invokeAgent(String agentName, String sessionId, String payload) {
        return invokeAgent(agentName, sessionId, payload, null, null, 900, null, null);
    }

    /**
     * Execute a command in a runtime session.
     *
     * @param agentName   agent name
     * @param sessionId   session ID
     * @param command     command as list of strings
     * @param chunked     whether to use chunked transfer
     * @param bearerToken optional Bearer token
     * @param endpoint    optional custom endpoint
     * @param userId      optional user ID
     * @param timeout     timeout in seconds
     * @return execution result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> execCommand(String agentName, String sessionId, List<String> command,
                                            boolean chunked, String bearerToken, String endpoint,
                                            String userId, int timeout) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("command", command);
        body.put("chunked", chunked);

        Map<String, String> headers = buildDataHeaders(sessionId, bearerToken, userId);
        RequestResult result = getDataClient().post("/runtimes/" + agentName + "/commands", headers, body).block();
        return check(result, "exec_command");
    }

    public Map<String, Object> execCommand(String agentName, String sessionId, String command) {
        return execCommand(agentName, sessionId, List.of(command), false, null, null, null, 900);
    }

    // ========================
    // Data Plane: File Operations
    // ========================

    /**
     * Upload files to a runtime session.
     *
     * @param agentName     agent name
     * @param sessionId     session ID
     * @param files         list of file maps ({path, content, description})
     * @param remotePath    remote directory path (default /home/user/)
     * @param fileUserId    file owner user ID (nullable)
     * @param fileGroupId   file owner group ID (nullable)
     * @param fileMode      file permission mode (nullable)
     * @param bearerToken   optional Bearer token
     * @param endpoint      optional custom endpoint
     * @param userId        optional user ID
     * @param timeout       timeout in seconds
     * @return upload result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadFiles(String agentName, String sessionId,
                                            List<Map<String, Object>> files, String remotePath,
                                            Integer fileUserId, Integer fileGroupId, String fileMode,
                                            String bearerToken, String endpoint, String userId,
                                            int timeout) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("files", files);
        body.put("path", remotePath != null ? remotePath : "/home/user/");
        if (fileUserId != null) body.put("file_user_id", fileUserId);
        if (fileGroupId != null) body.put("file_group_id", fileGroupId);
        if (JsonUtils.isNotBlank(fileMode)) body.put("file_mode", fileMode);

        Map<String, String> headers = buildDataHeaders(sessionId, bearerToken, userId);
        RequestResult result = getDataClient().post("/runtimes/" + agentName + "/upload-files", headers, body).block();
        return check(result, "upload_files");
    }

    public Map<String, Object> uploadFiles(String agentName, String sessionId, List<Map<String, Object>> files) {
        return uploadFiles(agentName, sessionId, files, null, null, null, null, null, null, null, 900);
    }

    /**
     * Download files from a runtime session.
     *
     * @param agentName   agent name
     * @param sessionId   session ID
     * @param path        remote file/directory path
     * @param recursive   whether to download recursively
     * @param bearerToken optional Bearer token
     * @param endpoint    optional custom endpoint
     * @param userId      optional user ID
     * @param timeout     timeout in seconds
     * @return download result (check isSuccess, access data or streaming body)
     */
    public RequestResult downloadFiles(String agentName, String sessionId, String path,
                                        boolean recursive, String bearerToken, String endpoint,
                                        String userId, int timeout) {
        StringBuilder url = new StringBuilder("/runtimes/").append(agentName)
                .append("/download-files?path=").append(path);
        if (recursive) url.append("&recursive=true");

        Map<String, String> headers = buildDataHeaders(sessionId, bearerToken, userId);
        return getDataClient().get(url.toString(), headers).block();
    }

    public RequestResult downloadFiles(String agentName, String sessionId, String path) {
        return downloadFiles(agentName, sessionId, path, false, null, null, null, 900);
    }

    // ========================
    // Data Plane: Session Management
    // ========================

    /**
     * Start a runtime session.
     *
     * @param agentName   agent name
     * @param bearerToken optional Bearer token
     * @param endpoint    optional custom endpoint
     * @param userId      optional user ID
     * @param timeout     timeout in seconds
     * @return session data (includes session_id)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> startSession(String agentName, String bearerToken,
                                             String endpoint, String userId, int timeout) {
        Map<String, String> headers = buildDataHeaders(null, bearerToken, userId);
        RequestResult result = getDataClient().post(
                "/runtimes/" + agentName + "/sessions-start", headers, Map.of()).block();
        return check(result, "start_session");
    }

    public Map<String, Object> startSession(String agentName) {
        return startSession(agentName, null, null, null, 30);
    }

    /**
     * Stop a runtime session.
     *
     * @param agentName   agent name
     * @param sessionId   session ID
     * @param bearerToken optional Bearer token
     * @param endpoint    optional custom endpoint
     * @param userId      optional user ID
     * @param timeout     timeout in seconds
     * @return stop result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> stopSession(String agentName, String sessionId,
                                            String bearerToken, String endpoint,
                                            String userId, int timeout) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (JsonUtils.isNotBlank(sessionId)) body.put("session_id", sessionId);

        Map<String, String> headers = buildDataHeaders(sessionId, bearerToken, userId);
        RequestResult result = getDataClient().post(
                "/runtimes/" + agentName + "/sessions-stop", headers, body).block();
        return check(result, "stop_session");
    }

    public Map<String, Object> stopSession(String agentName, String sessionId) {
        return stopSession(agentName, sessionId, null, null, null, 30);
    }

    // ========================
    // Helpers
    // ========================

    private Map<String, String> buildDataHeaders(String sessionId, String bearerToken, String userId) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (JsonUtils.isNotBlank(sessionId)) {
            headers.put(Constants.SESSION_HEADER, sessionId);
        }
        if (JsonUtils.isNotBlank(userId)) {
            headers.put(Constants.USER_ID_HEADER, userId);
        }
        return headers.isEmpty() ? null : headers;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> check(RequestResult result, String operation) {
        if (result == null) {
            throw new RuntimeException(operation + " failed: null response");
        }
        if (!result.isSuccess()) {
            throw new RuntimeException(operation + " failed: " + result.getError()
                    + " (HTTP " + result.getStatusCode() + ")");
        }
        Object data = result.getData();
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        return Map.of();
    }

    @Override
    public void close() {
        if (controlClient != null) controlClient.close();
        if (dataClient != null) dataClient.close();
    }
}
