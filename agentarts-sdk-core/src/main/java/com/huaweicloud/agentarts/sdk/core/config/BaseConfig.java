package com.huaweicloud.agentarts.sdk.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base configuration for an agent: name, entrypoint, region, platform, etc.
 *
 * <p>Mirrors Python {@code BaseConfig}.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseConfig {

    @JsonProperty("name")
    private String name;

    @JsonProperty("entrypoint")
    private String entrypoint;

    @JsonProperty("dependency_file")
    private String dependencyFile;

    @JsonProperty("region")
    private String region;

    @JsonProperty("platform")
    private String platform = "linux/amd64";

    @JsonProperty("language")
    private String language = "java17";

    @JsonProperty("container_runtime")
    private String containerRuntime = "docker";

    @JsonProperty("base_image")
    private String baseImage = "eclipse-temurin:17-jre";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEntrypoint() { return entrypoint; }
    public void setEntrypoint(String entrypoint) { this.entrypoint = entrypoint; }

    public String getDependencyFile() { return dependencyFile; }
    public void setDependencyFile(String dependencyFile) { this.dependencyFile = dependencyFile; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getContainerRuntime() { return containerRuntime; }
    public void setContainerRuntime(String containerRuntime) { this.containerRuntime = containerRuntime; }

    public String getBaseImage() { return baseImage; }
    public void setBaseImage(String baseImage) { this.baseImage = baseImage; }
}
