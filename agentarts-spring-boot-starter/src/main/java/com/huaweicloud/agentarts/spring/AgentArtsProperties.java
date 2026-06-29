package com.huaweicloud.agentarts.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Boot configuration properties for AgentArts SDK.
 *
 * <p>Binds properties under the {@code agentarts.*} prefix:</p>
 * <pre>
 * agentarts:
 *   region: cn-southwest-2
 *   access-key: ${HUAWEICLOUD_SDK_AK}
 *   secret-key: ${HUAWEICLOUD_SDK_SK}
 *   runtime:
 *     port: 8080
 *     max-concurrency: 15
 *   memory:
 *     api-key: ...
 *     space-id: ...
 * </pre>
 */
@ConfigurationProperties(prefix = "agentarts")
public class AgentArtsProperties {

    /**
     * Huawei Cloud region. Defaults to env HUAWEICLOUD_SDK_REGION or "cn-southwest-2".
     */
    private String region;

    /**
     * Huawei Cloud Access Key ID. Falls back to env HUAWEICLOUD_SDK_AK.
     */
    private String accessKey;

    /**
     * Huawei Cloud Secret Access Key. Falls back to env HUAWEICLOUD_SDK_SK.
     */
    private String secretKey;

    /**
     * Runtime server configuration.
     */
    private Runtime runtime = new Runtime();

    /**
     * Memory service configuration.
     */
    private Memory memory = new Memory();

    /**
     * Identity service configuration.
     */
    private Identity identity = new Identity();

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Runtime getRuntime() {
        return runtime;
    }

    public void setRuntime(Runtime runtime) {
        this.runtime = runtime;
    }

    public Memory getMemory() {
        return memory;
    }

    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    /**
     * Resolve the effective region: property → env → default.
     */
    public String resolveRegion() {
        if (region != null && !region.isEmpty()) {
            return region;
        }
        return com.huaweicloud.agentarts.sdk.core.Constants.getRegion();
    }

    /**
     * Resolve the effective AK: property → env → empty.
     */
    public String resolveAccessKey() {
        if (accessKey != null && !accessKey.isEmpty()) {
            return accessKey;
        }
        return com.huaweicloud.agentarts.sdk.core.Constants.getAk();
    }

    /**
     * Resolve the effective SK: property → env → empty.
     */
    public String resolveSecretKey() {
        if (secretKey != null && !secretKey.isEmpty()) {
            return secretKey;
        }
        return com.huaweicloud.agentarts.sdk.core.Constants.getSk();
    }

    public static class Runtime {
        private int port = 8080;
        private int maxConcurrency = 15;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getMaxConcurrency() {
            return maxConcurrency;
        }

        public void setMaxConcurrency(int maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
        }
    }

    public static class Memory {
        private String apiKey;
        private String spaceId;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getSpaceId() {
            return spaceId;
        }

        public void setSpaceId(String spaceId) {
            this.spaceId = spaceId;
        }
    }

    public static class Identity {
        private String workloadName;
        private String providerName;

        public String getWorkloadName() {
            return workloadName;
        }

        public void setWorkloadName(String workloadName) {
            this.workloadName = workloadName;
        }

        public String getProviderName() {
            return providerName;
        }

        public void setProviderName(String providerName) {
            this.providerName = providerName;
        }
    }
}
