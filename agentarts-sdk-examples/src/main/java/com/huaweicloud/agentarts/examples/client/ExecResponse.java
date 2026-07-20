package com.huaweicloud.agentarts.examples.client;

public class ExecResponse {
    private int code;

    private String message;

    private ExecData data;

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

    public ExecData getData() {
        return data;
    }

    public void setData(ExecData data) {
        this.data = data;
    }
}
