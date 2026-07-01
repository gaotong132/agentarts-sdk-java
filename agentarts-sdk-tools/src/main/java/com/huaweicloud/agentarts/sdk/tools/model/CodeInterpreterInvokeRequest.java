package com.huaweicloud.agentarts.sdk.tools.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * Request body for invoking a Code Interpreter session.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodeInterpreterInvokeRequest {

    @JsonProperty("operate_type")
    private String operateType;

    @JsonProperty("arguments")
    private Map<String, Object> arguments;

    @JsonProperty("code_interpreter_name")
    private String codeInterpreterName;

    @JsonProperty("session_id")
    private String sessionId;

    public CodeInterpreterInvokeRequest withOperateType(String operateType) { this.operateType = operateType; return this; }
    public CodeInterpreterInvokeRequest withArguments(Map<String, Object> arguments) { this.arguments = arguments; return this; }
    public CodeInterpreterInvokeRequest withCodeInterpreterName(String codeInterpreterName) { this.codeInterpreterName = codeInterpreterName; return this; }
    public CodeInterpreterInvokeRequest withSessionId(String sessionId) { this.sessionId = sessionId; return this; }

    public String getOperateType() { return operateType; }
    public void setOperateType(String operateType) { this.operateType = operateType; }
    public Map<String, Object> getArguments() { return arguments; }
    public void setArguments(Map<String, Object> arguments) { this.arguments = arguments; }
    public String getCodeInterpreterName() { return codeInterpreterName; }
    public void setCodeInterpreterName(String codeInterpreterName) { this.codeInterpreterName = codeInterpreterName; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CodeInterpreterInvokeRequest that)) return false;
        return Objects.equals(operateType, that.operateType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operateType);
    }

    @Override
    public String toString() {
        return "CodeInterpreterInvokeRequest{operateType='" + operateType + "'}";
    }
}
