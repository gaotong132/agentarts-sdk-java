package com.huaweicloud.agentarts.examples.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SessionData {
    @JsonProperty("session_id")
    private String sessionId;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
