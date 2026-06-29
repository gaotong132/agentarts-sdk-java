package com.huaweicloud.agentarts.sdk.core;

/**
 * Centralized constants and environment variable accessors for AgentArts SDK.
 *
 * <p>Mirrors Python {@code agentarts.sdk.utils.constant} module. All values are
 * loaded from environment variables.</p>
 *
 * <p>Environment variable priority for region:
 * <ol>
 *   <li>{@code HUAWEICLOUD_SDK_REGION}</li>
 *   <li>{@code HUAWEICLOUD_REGION}</li>
 *   <li>{@code OS_REGION_NAME} (OpenStack compatibility)</li>
 *   <li>Default: {@code cn-southwest-2}</li>
 * </ol>
 */
public final class Constants {

    private Constants() {
        // utility class
    }

    // ========================
    // Environment variable names
    // ========================

    public static final String ENV_HUAWEICLOUD_SDK_AK = "HUAWEICLOUD_SDK_AK";
    public static final String ENV_HUAWEICLOUD_SDK_SK = "HUAWEICLOUD_SDK_SK";
    public static final String ENV_HUAWEICLOUD_SDK_SECURITY_TOKEN = "HUAWEICLOUD_SDK_SECURITY_TOKEN";
    public static final String ENV_HUAWEICLOUD_SDK_REGION = "HUAWEICLOUD_SDK_REGION";
    public static final String ENV_HUAWEICLOUD_REGION = "HUAWEICLOUD_REGION";
    public static final String ENV_OS_REGION_NAME = "OS_REGION_NAME";

    public static final String ENV_HUAWEICLOUD_SDK_IDP_ID = "HUAWEICLOUD_SDK_IDP_ID";
    public static final String ENV_HUAWEICLOUD_SDK_ID_TOKEN_FILE = "HUAWEICLOUD_SDK_ID_TOKEN_FILE";
    public static final String ENV_HUAWEICLOUD_SDK_PROJECT_ID = "HUAWEICLOUD_SDK_PROJECT_ID";

    public static final String ENV_AGENTARTS_CONTROL_ENDPOINT = "AGENTARTS_CONTROL_ENDPOINT";
    public static final String ENV_AGENTARTS_RUNTIME_DATA_ENDPOINT = "AGENTARTS_RUNTIME_DATA_ENDPOINT";
    public static final String ENV_AGENTARTS_MEMORY_DATA_ENDPOINT = "AGENTARTS_MEMORY_DATA_ENDPOINT";
    public static final String ENV_AGENTARTS_CODEINTERPRETER_DATA_ENDPOINT = "AGENTARTS_CODEINTERPRETER_DATA_ENDPOINT";

    public static final String ENV_HUAWEICLOUD_SDK_IAM_ENDPOINT = "HUAWEICLOUD_SDK_IAM_ENDPOINT";
    public static final String ENV_HUAWEICLOUD_SDK_SWR_ENDPOINT = "HUAWEICLOUD_SDK_SWR_ENDPOINT";
    public static final String ENV_HUAWEICLOUD_SDK_AGENTIDENTITY_ENDPOINT = "HUAWEICLOUD_SDK_AGENTIDENTITY_ENDPOINT";

    public static final String ENV_AGENTARTS_LOG_LEVEL = "AGENTARTS_LOG_LEVEL";
    public static final String ENV_AGENTARTS_BIND_IP = "AGENTARTS_BIND_IP";

    // ========================
    // HTTP Header constants (matching Python runtime/model.py)
    // ========================

    public static final String SESSION_HEADER = "x-hw-agentarts-session-id";
    public static final String ACCESS_TOKEN_HEADER = "X-HW-AgentGateway-Workload-Access-Token";
    public static final String USER_ID_HEADER = "X-HW-AgentGateway-User-Id";
    public static final String CUSTOM_HEADER_PREFIX = "X-Hw-AgentArts-Runtime-Custom-";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    // ========================
    // Default values
    // ========================

    public static final String DEFAULT_REGION = "cn-southwest-2";
    public static final int DEFAULT_PORT = 8080;
    public static final int DEFAULT_MAX_CONCURRENCY = 15;
    public static final double DEFAULT_TIMEOUT_SECONDS = 30.0;

    // ========================
    // Region accessor
    // ========================

    /**
     * Get the current Huawei Cloud region.
     *
     * <p>Checks environment variables in order:
     * HUAWEICLOUD_SDK_REGION, HUAWEICLOUD_REGION, OS_REGION_NAME.
     * Falls back to {@value #DEFAULT_REGION}.</p>
     */
    public static String getRegion() {
        String region = getEnv(ENV_HUAWEICLOUD_SDK_REGION);
        if (region != null && !region.isEmpty()) {
            return region;
        }
        region = getEnv(ENV_HUAWEICLOUD_REGION);
        if (region != null && !region.isEmpty()) {
            return region;
        }
        region = getEnv(ENV_OS_REGION_NAME);
        if (region != null && !region.isEmpty()) {
            return region;
        }
        return DEFAULT_REGION;
    }

    // ========================
    // Credential accessors
    // ========================

    /** Get Huawei Cloud Access Key ID from environment. */
    public static String getAk() {
        return getEnvOrDefault(ENV_HUAWEICLOUD_SDK_AK, "");
    }

    /** Get Huawei Cloud Secret Access Key from environment. */
    public static String getSk() {
        return getEnvOrDefault(ENV_HUAWEICLOUD_SDK_SK, "");
    }

    /** Get Huawei Cloud Security Token from environment (for STS). */
    public static String getSecurityToken() {
        return getEnvOrDefault(ENV_HUAWEICLOUD_SDK_SECURITY_TOKEN, "");
    }

    /** Get Huawei Cloud Project ID from environment. */
    public static String getProjectId() {
        return getEnvOrDefault(ENV_HUAWEICLOUD_SDK_PROJECT_ID, "");
    }

    // ========================
    // Endpoint constructors
    // ========================

    /**
     * Get the AgentArts control plane endpoint URL.
     *
     * <p>Example: {@code https://agentarts.cn-southwest-2.myhuaweicloud.com}</p>
     */
    public static String getControlPlaneEndpoint(String region) {
        String endpoint = getEnv(ENV_AGENTARTS_CONTROL_ENDPOINT);
        if (endpoint != null && !endpoint.isEmpty()) {
            return ensureHttps(endpoint);
        }
        String r = (region != null && !region.isEmpty()) ? region : getRegion();
        return "https://agentarts." + r + ".myhuaweicloud.com";
    }

    public static String getControlPlaneEndpoint() {
        return getControlPlaneEndpoint(null);
    }

    /**
     * Get the AgentArts runtime data plane endpoint URL.
     */
    public static String getRuntimeDataPlaneEndpoint() {
        String endpoint = getEnv(ENV_AGENTARTS_RUNTIME_DATA_ENDPOINT);
        return ensureHttps(endpoint != null ? endpoint : "");
    }

    /**
     * Get the AgentArts code interpreter data plane endpoint URL.
     */
    public static String getCodeInterpreterDataPlaneEndpoint(String endpoint) {
        String codeEndpoint = getEnv(ENV_AGENTARTS_CODEINTERPRETER_DATA_ENDPOINT);
        if (codeEndpoint != null && !codeEndpoint.isEmpty()) {
            return ensureHttps(codeEndpoint);
        }
        if (endpoint != null && !endpoint.isEmpty()) {
            return ensureHttps(endpoint);
        }
        String runtimeEndpoint = getEnvOrDefault(ENV_AGENTARTS_RUNTIME_DATA_ENDPOINT, "");
        return ensureHttps(runtimeEndpoint);
    }

    public static String getCodeInterpreterDataPlaneEndpoint() {
        return getCodeInterpreterDataPlaneEndpoint(null);
    }

    /**
     * Get the AgentArts memory service endpoint URL.
     *
     * @param endpointType "control" or "data"
     * @param region       Huawei Cloud region (nullable, auto-detected if null)
     */
    public static String getMemoryEndpoint(String endpointType, String region) {
        if ("control".equals(endpointType)) {
            return getControlPlaneEndpoint(region);
        }
        if ("data".equals(endpointType)) {
            String memoryEndpoint = getEnv(ENV_AGENTARTS_MEMORY_DATA_ENDPOINT);
            if (memoryEndpoint != null && !memoryEndpoint.isEmpty()) {
                return ensureHttps(memoryEndpoint);
            }
            String r = (region != null && !region.isEmpty()) ? region : getRegion();
            return "https://memory." + r + ".huaweicloud-agentarts.com";
        }
        throw new IllegalArgumentException("Invalid endpoint type: " + endpointType);
    }

    public static String getMemoryEndpoint(String endpointType) {
        return getMemoryEndpoint(endpointType, null);
    }

    /**
     * Get the Huawei Cloud IAM endpoint URL.
     *
     * <p>Example: {@code https://iam.cn-southwest-2.myhuaweicloud.com}</p>
     */
    public static String getIamEndpoint(String region) {
        String endpoint = getEnv(ENV_HUAWEICLOUD_SDK_IAM_ENDPOINT);
        if (endpoint != null && !endpoint.isEmpty()) {
            return ensureHttps(endpoint);
        }
        String r = (region != null && !region.isEmpty()) ? region : getRegion();
        return "https://iam." + r + ".myhuaweicloud.com";
    }

    public static String getIamEndpoint() {
        return getIamEndpoint(null);
    }

    /**
     * Get the Huawei Cloud SWR endpoint URL.
     *
     * <p>Example: {@code https://swr-api.cn-southwest-2.myhuaweicloud.com}</p>
     */
    public static String getSwrEndpoint(String region) {
        String endpoint = getEnv(ENV_HUAWEICLOUD_SDK_SWR_ENDPOINT);
        if (endpoint != null && !endpoint.isEmpty()) {
            return ensureHttps(endpoint);
        }
        String r = (region != null && !region.isEmpty()) ? region : getRegion();
        return "https://swr-api." + r + ".myhuaweicloud.com";
    }

    public static String getSwrEndpoint() {
        return getSwrEndpoint(null);
    }

    /**
     * Get the Huawei Cloud Agent Identity endpoint URL.
     *
     * <p>Example: {@code https://agent-identity.cn-southwest-2.myhuaweicloud.com}</p>
     */
    public static String getIdentityEndpoint(String region) {
        String endpoint = getEnv(ENV_HUAWEICLOUD_SDK_AGENTIDENTITY_ENDPOINT);
        if (endpoint != null && !endpoint.isEmpty()) {
            return ensureHttps(endpoint);
        }
        String r = (region != null && !region.isEmpty()) ? region : getRegion();
        return "https://agent-identity." + r + ".myhuaweicloud.com";
    }

    public static String getIdentityEndpoint() {
        return getIdentityEndpoint(null);
    }

    // ========================
    // Internal helpers
    // ========================

    /**
     * Ensure endpoint has https:// prefix.
     */
    static String ensureHttps(String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) {
            return endpoint;
        }
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            return "https://" + endpoint;
        }
        return endpoint;
    }

    private static String getEnv(String name) {
        return System.getenv(name);
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}
