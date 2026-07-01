package com.huaweicloud.agentarts.sdk.service.runtime.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Request body for stopping a runtime session.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StopSessionRequest {

    @JsonProperty("session_id")
    private String sessionId;

    public StopSessionRequest withSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StopSessionRequest that = (StopSessionRequest) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        return "StopSessionRequest{sessionId='" + sessionId + "'}";
    }
}
