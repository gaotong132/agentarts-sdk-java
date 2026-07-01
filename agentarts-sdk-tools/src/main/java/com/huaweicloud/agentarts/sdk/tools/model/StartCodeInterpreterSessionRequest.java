package com.huaweicloud.agentarts.sdk.tools.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Request body for starting a Code Interpreter session.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StartCodeInterpreterSessionRequest {

    @JsonProperty("name")
    private String name;

    @JsonProperty("session_timeout")
    private Integer sessionTimeout;

    public StartCodeInterpreterSessionRequest withName(String name) { this.name = name; return this; }
    public StartCodeInterpreterSessionRequest withSessionTimeout(Integer sessionTimeout) { this.sessionTimeout = sessionTimeout; return this; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSessionTimeout() { return sessionTimeout; }
    public void setSessionTimeout(Integer sessionTimeout) { this.sessionTimeout = sessionTimeout; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StartCodeInterpreterSessionRequest that)) return false;
        return Objects.equals(name, that.name) && Objects.equals(sessionTimeout, that.sessionTimeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, sessionTimeout);
    }

    @Override
    public String toString() {
        return "StartCodeInterpreterSessionRequest{name='" + name + "', sessionTimeout=" + sessionTimeout + "}";
    }
}
