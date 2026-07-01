package com.huaweicloud.agentarts.sdk.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Per-agent configuration in {@code .agentarts_config.yaml}.
 *
 * <p>Agent configuration with three sections: base, swr_config, runtime.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentArtsConfig {

    @JsonProperty("base")
    private BaseConfig base = new BaseConfig();

    @JsonProperty("swr_config")
    private SWRConfig swrConfig = new SWRConfig();

    @JsonProperty("runtime")
    private RuntimeConfig runtime = new RuntimeConfig();

    public BaseConfig getBase() {
        return base;
    }

    public void setBase(BaseConfig base) {
        this.base = base;
    }

    public SWRConfig getSwrConfig() {
        return swrConfig;
    }

    public void setSwrConfig(SWRConfig swrConfig) {
        this.swrConfig = swrConfig;
    }

    public RuntimeConfig getRuntime() {
        return runtime;
    }

    public void setRuntime(RuntimeConfig runtime) {
        this.runtime = runtime;
    }
}
