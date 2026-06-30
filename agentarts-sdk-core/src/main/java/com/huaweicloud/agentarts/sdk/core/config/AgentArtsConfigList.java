package com.huaweicloud.agentarts.sdk.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AgentArts configuration model for {@code .agentarts_config.yaml}.
 *
 * <p>Mirrors Python {@code agentarts.toolkit.utils.runtime.config.AgentArtsConfigList}.</p>
 *
 * <p>This is the top-level configuration that supports multiple agents,
 * each with its own base, SWR, and runtime configuration.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentArtsConfigList {

    @JsonProperty("default_agent")
    private String defaultAgent;

    @JsonProperty("agents")
    private Map<String, AgentArtsConfig> agents = new LinkedHashMap<>();

    public AgentArtsConfigList() {
    }

    public String getDefaultAgent() {
        return defaultAgent;
    }

    public void setDefaultAgent(String defaultAgent) {
        this.defaultAgent = defaultAgent;
    }

    public Map<String, AgentArtsConfig> getAgents() {
        return agents;
    }

    public void setAgents(Map<String, AgentArtsConfig> agents) {
        this.agents = agents != null ? agents : new LinkedHashMap<>();
    }

    public AgentArtsConfig getAgent(String name) {
        return agents.get(name);
    }

    public void addAgent(String name, AgentArtsConfig config) {
        agents.put(name, config);
    }

    public void removeAgent(String name) {
        agents.remove(name);
    }
}
