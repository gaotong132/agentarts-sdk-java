package com.huaweicloud.agentarts.sdk.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.core.SignMode;
import com.huaweicloud.agentarts.sdk.service.http.BaseHttpClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestConfig;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;

import java.util.*;

/**
 * Code Interpreter client with dual-plane architecture.
 *
 * <p>Code Interpreter client with dual-plane architecture.
 * Control plane uses AK/SK signing. Data plane uses API Key or IAM.</p>
 *
 * <h3>Control Plane (AK/SK):</h3>
 * create/list/update/get/delete_code_interpreter
 *
 * <h3>Data Plane (API Key or IAM):</h3>
 * start_session/stop_session/get_session/invoke/execute_code/execute_command/
 * upload_file/upload_files/download_file/download_files/install_packages/clear_context
 */
public class CodeInterpreterClient implements AutoCloseable {

    private static final ObjectMapper MAPPER = com.huaweicloud.agentarts.sdk.core.util.JsonUtils.MAPPER;

    private final String region;
    private final String dataEndpoint;
    private final String authType;
    private final boolean verifySsl;

    private BaseHttpClient controlClient;
    private BaseHttpClient dataClient;
    private String codeInterpreterName;
    private String sessionId;

    public CodeInterpreterClient(String region, String dataEndpoint, String authType, boolean verifySsl) {
        this.region = region != null ? region : Constants.getRegion();
        this.dataEndpoint = dataEndpoint;
        this.authType = authType != null ? authType : "API_KEY";
        this.verifySsl = verifySsl;
    }

    public CodeInterpreterClient(String region, String dataEndpoint) {
        this(region, dataEndpoint, "API_KEY", true);
    }

    public CodeInterpreterClient(String region) {
        this(region, null, "API_KEY", true);
    }

    // ========================
    // Properties
    // ========================

    public String getCodeInterpreterName() { return codeInterpreterName; }
    public void setCodeInterpreterName(String name) { this.codeInterpreterName = name; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    // ========================
    // Lazy clients
    // ========================

    private synchronized BaseHttpClient getControlClient() {
        if (controlClient == null) {
            String endpoint = Constants.getControlPlaneEndpoint(region) + "/v1/core";
            RequestConfig config = RequestConfig.builder().baseUrl(endpoint).verifySsl(verifySsl).build();
            controlClient = new BaseHttpClient(config, true, SignMode.SDK_HMAC_SHA256, region);
        }
        return controlClient;
    }

    private synchronized BaseHttpClient getDataClient() {
        if (dataClient == null) {
            String endpoint = Constants.getCodeInterpreterDataPlaneEndpoint(dataEndpoint);
            boolean useAkSk = "IAM".equals(authType);
            RequestConfig config = RequestConfig.builder().baseUrl(endpoint).verifySsl(verifySsl).build();
            dataClient = new BaseHttpClient(config, useAkSk, SignMode.SDK_HMAC_SHA256, region);
            if (!useAkSk) {
                String key = System.getenv("HUAWEICLOUD_SDK_CODE_INTERPRETER_API_KEY");
                if (com.huaweicloud.agentarts.sdk.core.util.JsonUtils.isNotBlank(key)) {
                    dataClient.setAuthToken("Bearer", key);
                }
            }
        }
        return dataClient;
    }

    // ========================
    // Control Plane: CRUD
    // ========================

    public Map<String, Object> createCodeInterpreter(String name, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        if (description != null) body.put("description", description);
        RequestResult r = getControlClient().post("/code-interpreters", null, body).block();
        return parseMap(r);
    }

    public Map<String, Object> createCodeInterpreter(String name) {
        return createCodeInterpreter(name, null);
    }

    public Map<String, Object> listCodeInterpreters(String name, int limit, int offset) {
        String url = "/code-interpreters?limit=" + limit + "&offset=" + offset;
        if (name != null) url += "&name=" + name;
        RequestResult r = getControlClient().get(url).block();
        return parseMap(r);
    }

    public Map<String, Object> listCodeInterpreters() {
        return listCodeInterpreters(null, 10, 0);
    }

    public Map<String, Object> updateCodeInterpreter(String codeInterpreterId, Map<String, Object> updates) {
        RequestResult r = getControlClient().put("/code-interpreters/" + codeInterpreterId, null, updates).block();
        return parseMap(r);
    }

    public Map<String, Object> getCodeInterpreter(String codeInterpreterId) {
        RequestResult r = getControlClient().get("/code-interpreters/" + codeInterpreterId).block();
        return parseMap(r);
    }

    public void deleteCodeInterpreter(String codeInterpreterId) {
        getControlClient().delete("/code-interpreters/" + codeInterpreterId).block();
    }

    // ========================
    // Data Plane: Sessions
    // ========================

    public String startSession(String codeInterpreterName, String sessionName, Integer sessionTimeout) {
        Map<String, Object> body = new HashMap<>();
        body.put("code_interpreter_name", codeInterpreterName);
        body.put("session_name", sessionName);
        if (sessionTimeout != null) body.put("session_timeout", sessionTimeout);
        RequestResult r = getDataClient().post("/v1/sessions", null, body).block();
        Map<String, Object> data = parseMap(r);
        this.codeInterpreterName = codeInterpreterName;
        this.sessionId = (String) data.get("session_id");
        return this.sessionId;
    }

    public String startSession(String codeInterpreterName, String sessionName) {
        return startSession(codeInterpreterName, sessionName, 900);
    }

    public Map<String, Object> getSession(String codeInterpreterName, String sessionId) {
        String url = "/v1/sessions?code_interpreter_name=" + codeInterpreterName;
        if (sessionId != null) url += "&session_id=" + sessionId;
        RequestResult r = getDataClient().get(url).block();
        return parseMap(r);
    }

    public boolean stopSession() {
        RequestResult r = getDataClient().post("/v1/sessions/stop", null, Map.of()).block();
        return r != null && r.isSuccess();
    }

    // ========================
    // Data Plane: Invoke
    // ========================

    public Map<String, Object> invoke(String operateType, Map<String, Object> arguments) {
        Map<String, Object> body = new HashMap<>();
        body.put("operate_type", operateType);
        body.put("arguments", arguments);
        if (codeInterpreterName != null) body.put("code_interpreter_name", codeInterpreterName);
        if (sessionId != null) body.put("session_id", sessionId);
        RequestResult r = getDataClient().post("/v1/invoke", null, body).block();
        return parseMap(r);
    }

    public Map<String, Object> executeCode(String code, String language, boolean clearContext) {
        Map<String, Object> args = new HashMap<>();
        args.put("code", code);
        args.put("language", language != null ? language : "python");
        args.put("clear_context", clearContext);
        return invoke("execute_code", args);
    }

    public Map<String, Object> executeCode(String code) {
        return executeCode(code, "python", false);
    }

    public Map<String, Object> executeCommand(String command) {
        return invoke("execute_command", Map.of("command", command));
    }

    public Map<String, Object> uploadFile(String path, String content, String description) {
        Map<String, Object> args = new HashMap<>();
        args.put("path", path);
        args.put("content", content);
        args.put("description", description != null ? description : "");
        return invoke("upload_file", args);
    }

    public Map<String, Object> uploadFiles(List<Map<String, String>> files) {
        return invoke("upload_files", Map.of("files", files));
    }

    public Object downloadFile(String path) {
        Map<String, Object> result = invoke("download_file", Map.of("path", path));
        return result != null ? result.get("content") : null;
    }

    public Map<String, Object> downloadFiles(List<String> paths) {
        return invoke("download_files", Map.of("paths", paths));
    }

    public Map<String, Object> installPackages(List<String> packages, boolean upgrade) {
        Map<String, Object> args = new HashMap<>();
        args.put("packages", packages);
        args.put("upgrade", upgrade);
        return invoke("install_packages", args);
    }

    public Map<String, Object> installPackages(List<String> packages) {
        return installPackages(packages, false);
    }

    public Map<String, Object> clearContext() {
        return invoke("clear_context", Map.of());
    }

    // ========================
    // Helpers
    // ========================

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(RequestResult result) {
        if (result == null || !result.isSuccess()) {
            String err = result != null ? result.getError() : "null";
            throw new RuntimeException("API call failed: " + err);
        }
        try {
            JsonNode data = result.getDataAsJson();
            if (data != null) return MAPPER.treeToValue(data, Map.class);
            return Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Parse error", e);
        }
    }

    @Override
    public void close() {
        if (controlClient != null) controlClient.close();
        if (dataClient != null) dataClient.close();
    }
}
