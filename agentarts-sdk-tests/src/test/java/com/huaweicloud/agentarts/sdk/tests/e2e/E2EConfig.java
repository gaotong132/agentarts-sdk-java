package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;

/**
 * E2E test configuration — providing three-tier safety model gates.
 *
 * <p>Three-tier safety model:</p>
 * <ul>
 *   <li><b>Default (read-only)</b> — list/get + local RuntimeApp, no cloud writes</li>
 *   <li><b>ALLOW_CREATE</b> — create→get→update→delete lifecycle</li>
 *   <li><b>RUN_BILLABLE</b> — code-interpreter sandbox, runtime invoke (real money)</li>
 * </ul>
 */
public final class E2EConfig {

    private E2EConfig() {}

    // ---- Environment variable names ----
    public static final String ENV_AK = "HUAWEICLOUD_SDK_AK";
    public static final String ENV_SK = "HUAWEICLOUD_SDK_SK";
    public static final String ENV_REGION = "HUAWEICLOUD_SDK_REGION";
    public static final String ENV_ALLOW_CREATE = "AGENTARTS_TEST_ALLOW_CREATE";
    public static final String ENV_RUN_BILLABLE = "AGENTARTS_TEST_RUN_BILLABLE";
    public static final String ENV_RUN_ID = "AGENTARTS_TEST_RUN_ID";
    public static final String ENV_MEMORY_API_KEY = "HUAWEICLOUD_SDK_MEMORY_API_KEY";
    public static final String ENV_CODE_INTERPRETER_API_KEY = "HUAWEICLOUD_SDK_CODE_INTERPRETER_API_KEY";
    public static final String ENV_PRE_WORKLOAD_IDENTITY = "AGENTARTS_TEST_WORKLOAD_IDENTITY_NAME";
    public static final String ENV_RUNTIME_AGENT_NAME = "AGENTARTS_TEST_RUNTIME_AGENT_NAME";
    public static final String ENV_CODE_INTERPRETER_NAME = "AGENTARTS_TEST_CODE_INTERPRETER_NAME";
    public static final String ENV_OAUTH2_CLIENT_ID = "AGENTARTS_TEST_OAUTH2_CLIENT_ID";
    public static final String ENV_OAUTH2_CLIENT_SECRET = "AGENTARTS_TEST_OAUTH2_CLIENT_SECRET";
    public static final String ENV_OAUTH2_VENDOR = "AGENTARTS_TEST_OAUTH2_VENDOR";
    public static final String ENV_STS_AGENCY_URN = "AGENTARTS_TEST_STS_AGENCY_URN";

    // ---- Accessors ----

    public static String getAk() { return System.getenv(ENV_AK); }
    public static String getSk() { return System.getenv(ENV_SK); }
    public static String getRegion() {
        String r = System.getenv(ENV_REGION);
        return JsonUtils.isNotBlank(r) ? r : "cn-southwest-2";
    }

    public static boolean hasCloudCredentials() {
        return JsonUtils.isNotBlank(getAk()) && JsonUtils.isNotBlank(getSk());
    }

    public static boolean allowCreate() { return envTruthy(ENV_ALLOW_CREATE); }
    public static boolean allowBillable() { return envTruthy(ENV_RUN_BILLABLE); }

    public static String getRunId() {
        String id = System.getenv(ENV_RUN_ID);
        return JsonUtils.isNotBlank(id) ? id : Long.toHexString(System.nanoTime()).substring(0, 8);
    }

    public static String getPreWorkloadIdentity() { return System.getenv(ENV_PRE_WORKLOAD_IDENTITY); }
    public static String getCodeInterpreterApiKey() { return System.getenv(ENV_CODE_INTERPRETER_API_KEY); }
    public static String getRuntimeAgentName() { return System.getenv(ENV_RUNTIME_AGENT_NAME); }
    public static String getCodeInterpreterName() { return System.getenv(ENV_CODE_INTERPRETER_NAME); }
    public static String getStsAgencyUrn() { return System.getenv(ENV_STS_AGENCY_URN); }

    public static boolean hasOAuth2Config() {
        return JsonUtils.isNotBlank(System.getenv(ENV_OAUTH2_CLIENT_ID))
                && JsonUtils.isNotBlank(System.getenv(ENV_OAUTH2_CLIENT_SECRET));
    }

    public static String getOAuth2ClientId() { return System.getenv(ENV_OAUTH2_CLIENT_ID); }
    public static String getOAuth2ClientSecret() { return System.getenv(ENV_OAUTH2_CLIENT_SECRET); }
    public static String getOAuth2Vendor() { return System.getenv(ENV_OAUTH2_VENDOR); }

    private static boolean envTruthy(String name) {
        String v = System.getenv(name);
        if (v == null) return false;
        return v.equals("1") || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("on");
    }
}
