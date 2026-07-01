package com.huaweicloud.agentarts.sdk.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Invoke configuration: protocol, port, file transfer config, URL match type.
 *
 * <p>Invoke configuration: protocol, port, file transfer config, URL match type.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvokeConfig {

    @JsonProperty("protocol")
    private String protocol = "HTTP";

    @JsonProperty("port")
    private int port = 8080;

    @JsonProperty("file_transfer_config")
    private Map<String, Object> fileTransferConfig;

    @JsonProperty("url_match_type")
    private String urlMatchType = "ACCURATE_MATCH";

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public Map<String, Object> getFileTransferConfig() { return fileTransferConfig; }
    public void setFileTransferConfig(Map<String, Object> fileTransferConfig) { this.fileTransferConfig = fileTransferConfig; }

    public String getUrlMatchType() { return urlMatchType; }
    public void setUrlMatchType(String urlMatchType) { this.urlMatchType = urlMatchType; }
}
