package com.huaweicloud.agentarts.sdk.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.agentarts.sdk.core.APIException;
import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.core.SignMode;
import com.huaweicloud.agentarts.sdk.service.http.BaseHttpClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestConfig;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;
import com.huaweicloud.agentarts.sdk.tools.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Code Interpreter client with dual-plane architecture.
 *
 * <p>Control plane uses AK/SK signing. Data plane uses API Key or IAM.</p>
 *
 * <h3>Control Plane (AK/SK):</h3>
 * create/list/update/get/delete_code_interpreter
 *
 * <h3>Data Plane (API Key or IAM):</h3>
 * start_session/stop_session/get_session/invoke/execute_code/execute_command/
 * upload_file/upload_files/download_file/download_files/install_packages/clear_context
 */
public class CodeInterpreterClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(CodeInterpreterClient.class);
    private static final ObjectMapper MAPPER = com.huaweicloud.agentarts.sdk.core.util.JsonUtils.MAPPER;
    private static final String DEFAULT_PATH = "/home/user";

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

    /**
     * Create a Code Interpreter with full configuration.
     *
     * @param name                 interpreter name (required)
     * @param authType             auth type: "API_KEY" or "IAM" (default "API_KEY")
     * @param apiKeyName           API key name (required for API_KEY auth)
     * @param description          description
     * @param executionAgencyName  IAM agency name
     * @param observability        observability configuration
     * @param networkConfig        network configuration
     * @param agentGatewayId       agent gateway ID
     * @param tags                 tag list of {key, value} maps
     * @return created interpreter data
     */
    private static final java.util.regex.Pattern NAME_PATTERN =
            java.util.regex.Pattern.compile("[a-z][a-z0-9-]{0,38}[a-z0-9]$");

    public Map<String, Object> createCodeInterpreter(String name, String authType, String apiKeyName,
                                                       String description, String executionAgencyName,
                                                       Map<String, Object> observability,
                                                       Map<String, Object> networkConfig,
                                                       String agentGatewayId,
                                                       List<Map<String, String>> tags) {
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Name must start with lowercase letter, end with lowercase letter or digit, " +
                    "contain only lowercase letters, digits, and hyphens, and be 2-40 characters long.");
        }
        String resolvedAuthType = authType != null ? authType : "API_KEY";
        if ("API_KEY".equals(resolvedAuthType) && apiKeyName == null) {
            throw new IllegalArgumentException("API_KEY auth_type requires api_key_name.");
        }

        CreateCodeInterpreterRequest req = new CreateCodeInterpreterRequest()
                .withName(name)
                .withAuthType(resolvedAuthType)
                .withApiKeyName(apiKeyName)
                .withDescription(description)
                .withExecutionAgencyName(executionAgencyName)
                .withObservability(observability)
                .withNetworkConfig(networkConfig)
                .withAgentGatewayId(agentGatewayId)
                .withTags(tags);

        RequestResult r = getControlClient().post("/code-interpreters", null, req).block();
        return parseMap(r);
    }

    /** Convenience overload: create with auth type and description. */
    public Map<String, Object> createCodeInterpreter(String name, String authType, String apiKeyName,
                                                       String description) {
        return createCodeInterpreter(name, authType, apiKeyName, description,
                null, null, null, null, null);
    }

    public Map<String, Object> createCodeInterpreter(String name, String description) {
        return createCodeInterpreter(name, "IAM", null, description);
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

    /**
     * Update a Code Interpreter with typed parameters.
     *
     * @param codeInterpreterId interpreter ID
     * @param observability     observability configuration (nullable)
     * @param tags              tag list (nullable)
     * @return updated interpreter data
     */
    public Map<String, Object> updateCodeInterpreter(String codeInterpreterId,
                                                       Map<String, Object> observability,
                                                       List<Map<String, String>> tags) {
        UpdateCodeInterpreterRequest req = new UpdateCodeInterpreterRequest()
                .withObservability(observability)
                .withTags(tags);

        RequestResult r = getControlClient().put("/code-interpreters/" + codeInterpreterId, null, req).block();
        return parseMap(r);
    }

    /** Convenience overload: update with a generic map (backward compatible). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> updateCodeInterpreter(String codeInterpreterId, Map<String, Object> updates) {
        Map<String, Object> observability = (Map<String, Object>) updates.get("observability");
        List<Map<String, String>> tags = (List<Map<String, String>>) updates.get("tags");
        return updateCodeInterpreter(codeInterpreterId, observability, tags);
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

    /**
     * Start a code interpreter session.
     * PUT /v1/code-interpreters/{name}/sessions-start
     */
    public String startSession(String codeInterpreterName, String sessionName, Integer sessionTimeout) {
        StartCodeInterpreterSessionRequest req = new StartCodeInterpreterSessionRequest()
                .withName(sessionName)
                .withSessionTimeout(sessionTimeout);

        String url = "/v1/code-interpreters/" + codeInterpreterName + "/sessions-start";
        RequestResult r = getDataClient().put(url, null, req).block();
        Map<String, Object> data = parseMap(r);
        this.codeInterpreterName = codeInterpreterName;
        this.sessionId = (String) data.get("session_id");
        return this.sessionId;
    }

    public String startSession(String codeInterpreterName, String sessionName) {
        return startSession(codeInterpreterName, sessionName, 900);
    }

    /**
     * Get session details.
     * GET /v1/code-interpreters/{name}/sessions-get
     */
    public Map<String, Object> getSession(String codeInterpreterName, String sessionId) {
        String sid = sessionId != null ? sessionId : this.sessionId;
        String url = "/v1/code-interpreters/" + codeInterpreterName + "/sessions-get";
        Map<String, String> headers = sid != null
                ? Map.of(Constants.CODE_INTERPRETER_SESSION_HEADER, sid) : null;
        RequestResult r = getDataClient().get(url, headers).block();
        return parseMap(r);
    }

    /**
     * Stop the current session.
     * PUT /v1/code-interpreters/{name}/sessions-stop
     */
    public boolean stopSession() {
        if (sessionId == null || codeInterpreterName == null) return true;

        String url = "/v1/code-interpreters/" + codeInterpreterName + "/sessions-stop";
        Map<String, String> headers = Map.of(Constants.CODE_INTERPRETER_SESSION_HEADER, sessionId);
        RequestResult r = getDataClient().put(url, headers, null).block();
        this.codeInterpreterName = null;
        this.sessionId = null;
        return r != null && r.isSuccess();
    }

    // ========================
    // Data Plane: Invoke
    // ========================

    /**
     * Invoke a code interpreter session.
     * POST /v1/code-interpreters/{name}/invoke
     */
    public Map<String, Object> invoke(String operateType, Map<String, Object> arguments) {
        if (sessionId == null || codeInterpreterName == null) {
            throw new IllegalStateException("No active session — call startSession() first");
        }

        CodeInterpreterInvokeRequest req = new CodeInterpreterInvokeRequest()
                .withOperateType(operateType)
                .withArguments(arguments);

        String url = "/v1/code-interpreters/" + codeInterpreterName + "/invoke";
        Map<String, String> headers = Map.of(Constants.CODE_INTERPRETER_SESSION_HEADER, sessionId);
        RequestResult r = getDataClient().post(url, headers, req).block();
        return parseMap(r);
    }

    private static final List<String> VALID_LANGUAGES = List.of("python");
    private static final java.util.regex.Pattern COMMAND_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9_\\-\\.=\\s/\\.:]+$");

    public Map<String, Object> executeCode(String code, String language, boolean clearContext) {
        String lang = language != null ? language : "python";
        if (!VALID_LANGUAGES.contains(lang)) {
            throw new IllegalArgumentException(
                    "Invalid language. Supported languages are: " + String.join(", ", VALID_LANGUAGES));
        }
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("code", code);
        args.put("language", lang);
        args.put("clear_context", clearContext);
        return invoke("execute_code", args);
    }

    public Map<String, Object> executeCode(String code) {
        return executeCode(code, "python", false);
    }

    public Map<String, Object> executeCommand(String command) {
        if (!COMMAND_PATTERN.matcher(command).matches()) {
            throw new IllegalArgumentException("Invalid command format");
        }
        return invoke("execute_command", Map.of("command", command));
    }

    /**
     * Upload a file using write_files operation.
     *
     * @param path        file path
     * @param content     file content (text)
     * @param description file description
     * @return upload result
     */
    public Map<String, Object> uploadFile(String path, String content, String description) {
        String resolvedPath = resolvePath(path);
        Map<String, Object> fileContent = new LinkedHashMap<>();
        fileContent.put("path", resolvedPath);
        fileContent.put("text", content);

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("write_contents", List.of(fileContent));
        return invoke("write_files", args);
    }

    /**
     * Upload multiple files using write_files operation.
     *
     * @param files list of file specs with "path" and "content" keys
     * @return upload result
     */
    public Map<String, Object> uploadFiles(List<Map<String, String>> files) {
        List<Map<String, Object>> writeContents = new ArrayList<>();
        for (Map<String, String> file : files) {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("path", resolvePath(file.get("path")));
            fc.put("text", file.get("content"));
            writeContents.add(fc);
        }

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("write_contents", writeContents);
        return invoke("write_files", args);
    }

    /**
     * Download a file using read_files operation.
     *
     * @param path file path (must start with /home/user)
     * @return file content (String for text files, byte[] for binary, or raw Map on parse failure)
     */
    @SuppressWarnings("unchecked")
    public Object downloadFile(String path) {
        validateDownloadPath(path);
        Map<String, Object> result = invoke("read_files", Map.of("paths", List.of(path)));
        if (result == null) return null;

        // Parse response: result.result.content[0]
        Object resultObj = result.get("result");
        if (resultObj instanceof Map) {
            Map<String, Object> resultMap = (Map<String, Object>) resultObj;
            Object contentList = resultMap.get("content");
            if (contentList instanceof List) {
                List<Map<String, Object>> contents = (List<Map<String, Object>>) contentList;
                if (!contents.isEmpty()) {
                    return extractFileContent(contents.get(0));
                }
            }
        }
        // Fallback: return raw "content" field if present
        return result.get("content");
    }

    /**
     * Download multiple files using read_files operation.
     *
     * @param paths list of file paths
     * @return map of path → content
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> downloadFiles(List<String> paths) {
        for (String path : paths) {
            validateDownloadPath(path);
        }
        Map<String, Object> result = invoke("read_files", Map.of("paths", paths));
        if (result == null) return Map.of();

        Map<String, Object> files = new LinkedHashMap<>();
        Object resultObj = result.get("result");
        if (resultObj instanceof Map) {
            Map<String, Object> resultMap = (Map<String, Object>) resultObj;
            Object contentList = resultMap.get("content");
            if (contentList instanceof List) {
                List<Map<String, Object>> contents = (List<Map<String, Object>>) contentList;
                for (Map<String, Object> item : contents) {
                    String uri = (String) item.getOrDefault("uri", "");
                    String filePath = uri.replace("file://", "");
                    files.put(filePath, extractFileContent(item));
                }
            }
        }
        return files;
    }

    /**
     * Install Python packages via pip.
     *
     * @param packages list of package names (supports version specs like "numpy==1.24.3")
     * @param upgrade  whether to upgrade existing packages
     * @return command execution result
     */
    public Map<String, Object> installPackages(List<String> packages, boolean upgrade) {
        if (packages == null || packages.isEmpty()) {
            throw new IllegalArgumentException("Package list cannot be empty");
        }
        for (String pkg : packages) {
            if (pkg.chars().anyMatch(c -> c == ';' || c == '&' || c == '|' || c == '`' || c == '$')) {
                throw new IllegalArgumentException("Invalid package name: " + pkg);
            }
        }
        String packageStr = String.join(" ", packages);
        String upgradeFlag = upgrade ? " --upgrade" : "";
        String command = "pip install " + packageStr + upgradeFlag;
        return invoke("execute_command", Map.of("command", command));
    }

    public Map<String, Object> installPackages(List<String> packages) {
        return installPackages(packages, false);
    }

    /**
     * Clear interpreter context by executing code with clear_context=true.
     *
     * @return execution result
     */
    public Map<String, Object> clearContext() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("code", "# Context cleared");
        args.put("language", "python");
        args.put("clear_context", true);
        return invoke("execute_code", args);
    }

    // ========================
    // Helpers
    // ========================

    /**
     * Resolve file path: relative paths are joined with DEFAULT_PATH,
     * absolute paths must start with DEFAULT_PATH.
     */
    private String resolvePath(String path) {
        if (!path.startsWith("/")) {
            return DEFAULT_PATH + "/" + path;
        }
        if (!path.startsWith(DEFAULT_PATH)) {
            throw new IllegalArgumentException("Invalid path. Path must start with " + DEFAULT_PATH);
        }
        return path;
    }

    /**
     * Validate download path: must start with DEFAULT_PATH.
     */
    private void validateDownloadPath(String path) {
        if (!path.startsWith(DEFAULT_PATH)) {
            throw new IllegalArgumentException("Invalid path. Path must start with " + DEFAULT_PATH);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(RequestResult result) {
        if (result == null || !result.isSuccess()) {
            int status = result != null ? result.getStatusCode() : 0;
            String err = result != null ? result.getError() : "null response";
            throw new APIException(status, "code_interpreter", err);
        }
        try {
            JsonNode data = result.getDataAsJson();
            if (data != null) return MAPPER.treeToValue(data, Map.class);
            return Map.of();
        } catch (Exception e) {
            throw new APIException(result.getStatusCode(), "code_interpreter",
                    "Failed to parse response: " + e.getMessage(), e);
        }
    }

    /**
     * Extract file content from a content item (text/image/resource types).
     */
    @SuppressWarnings("unchecked")
    private Object extractFileContent(Map<String, Object> item) {
        String type = (String) item.get("type");
        if ("text".equals(type)) {
            return item.getOrDefault("text", "");
        }
        if ("image".equals(type)) {
            String data = (String) item.getOrDefault("data", "");
            return Base64.getDecoder().decode(data);
        }
        if ("resource".equals(type)) {
            Map<String, Object> resource = (Map<String, Object>) item.get("resource");
            if (resource != null) {
                String resourceType = (String) resource.get("type");
                if ("text".equals(resourceType)) {
                    return resource.get("text");
                }
                if ("blob".equals(resourceType)) {
                    return Base64.getDecoder().decode((String) resource.get("blob"));
                }
            }
        }
        return item;
    }

    @Override
    public synchronized void close() {
        if (controlClient != null) {
            controlClient.close();
            controlClient = null;
        }
        if (dataClient != null) {
            dataClient.close();
            dataClient = null;
        }
    }
}
