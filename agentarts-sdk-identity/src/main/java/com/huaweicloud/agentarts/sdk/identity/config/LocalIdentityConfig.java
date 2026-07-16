package com.huaweicloud.agentarts.sdk.identity.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

/**
 * Local identity configuration stored in {@code .agent_identity.json}.
 *
 * <p>Local identity configuration stored in {@code .agent_identity.json}.</p>
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

    @JsonIgnore
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
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Identity config path must not be blank");
        }
        Path configPath = Path.of(filename).toAbsolutePath().normalize();
        if (Files.exists(configPath)) {
            if (!Files.isRegularFile(configPath)) {
                throw new IllegalStateException("Identity config is not a regular file: " + filename);
            }
            try {
                LocalIdentityConfig config = OBJECT_MAPPER.readValue(configPath.toFile(), LocalIdentityConfig.class);
                config.setPath(filename);
                return config;
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load identity config " + filename, e);
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
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("Identity config path must not be blank");
        }
        Path target = Path.of(path).toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent == null) {
            throw new IllegalStateException("Identity config path has no parent: " + path);
        }
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, ".agent-identity-", ".tmp");
            byte[] json = OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(this).getBytes(StandardCharsets.UTF_8);
            Files.write(temporary, json, StandardOpenOption.TRUNCATE_EXISTING);
            applyOwnerOnlyPermissions(temporary);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            try {
                Files.move(temporary, target,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            applyOwnerOnlyPermissions(target);
        } catch (IOException e) {
            LOG.error("Failed to save {}: {}", path, e.getMessage());
            throw new RuntimeException("Failed to save identity config", e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException cleanupError) {
                    LOG.warn("Failed to remove temporary identity config: {}", cleanupError.getMessage());
                }
            }
        }
    }

    private static void applyOwnerOnlyPermissions(Path file) throws IOException {
        try {
            Set<PosixFilePermission> permissions = EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(file, permissions);
        } catch (UnsupportedOperationException ignored) {
            // Windows and non-POSIX filesystems use platform ACLs instead.
        }
    }
}
