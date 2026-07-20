package com.huaweicloud.agentarts.examples.client;

public class SessionStartResponse {
    private int code;

    private String message;

    private SessionData data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SessionData getData() {
        return data;
    }

    public void setData(SessionData data) {
        this.data = data;
    }
}
