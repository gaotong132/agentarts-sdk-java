package com.huaweicloud.agentarts.sdk.tools.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Code Interpreter session information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeInterpreterSessionInfo {
    @JsonProperty("code_interpreter_id") private String codeInterpreterId;
    @JsonProperty("session_id") private String sessionId;
    @JsonProperty("session_name") private String sessionName;
    @JsonProperty("session_timeout") private int sessionTimeout;
    @JsonProperty("created_at") private String createdAt;

    public String getCodeInterpreterId() { return codeInterpreterId; }
    public String getSessionId() { return sessionId; }
    public String getSessionName() { return sessionName; }
    public int getSessionTimeout() { return sessionTimeout; }
    public String getCreatedAt() { return createdAt; }
}
