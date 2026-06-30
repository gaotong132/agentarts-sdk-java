package com.huaweicloud.agentarts.sdk.identity.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Local identity configuration stored in {@code .agent_identity.json}.
 *
 * <p>Mirrors Python {@code Config} from {@code identity/config.py}.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocalIdentityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(LocalIdentityConfig.class);
    private static final ObjectMapper OBJECT_MAPPER = com.huaweicloud.agentarts.sdk.core.util.JsonUtils.MAPPER;
    private static final String DEFAULT_FILENAME = ".agent_identity.json";

    @JsonProperty("workload_identity_name")
    private String workloadIdentityName;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("path")
    private String path = DEFAULT_FILENAME;

    public LocalIdentityConfig() {
    }

    public String getWorkloadIdentityName() {
        return workloadIdentityName;
    }

    public void setWorkloadIdentityName(String name) {
        this.workloadIdentityName = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Load config from {@code .agent_identity.json} in the current directory.
     *
     * @return loaded config, or a new empty config if file doesn't exist
     */
    public static LocalIdentityConfig load() {
        return load(DEFAULT_FILENAME);
    }

    public static LocalIdentityConfig load(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            try {
                LocalIdentityConfig config = OBJECT_MAPPER.readValue(file, LocalIdentityConfig.class);
                config.setPath(filename);
                return config;
            } catch (IOException e) {
                LOG.warn("Failed to load {}: {}", filename, e.getMessage());
            }
        }
        LocalIdentityConfig config = new LocalIdentityConfig();
        config.setPath(filename);
        return config;
    }

    /**
     * Save config to the JSON file.
     */
    public void save() {
        try {
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(path), this);
        } catch (IOException e) {
            LOG.error("Failed to save {}: {}", path, e.getMessage());
            throw new RuntimeException("Failed to save identity config", e);
        }
    }
}
