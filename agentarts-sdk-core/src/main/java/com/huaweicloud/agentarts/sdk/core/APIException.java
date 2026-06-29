package com.huaweicloud.agentarts.sdk.core;

/**
 * Exception thrown when an API call to AgentArts services returns a non-success response.
 *
 * <p>Mirrors Python {@code agentarts.sdk.service.http_client.APIException}.</p>
 */
public class APIException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;
    private final String errorMsg;

    public APIException(int statusCode, String errorCode, String errorMsg) {
        super(formatMessage(statusCode, errorCode, errorMsg));
        this.statusCode = statusCode;
        this.errorCode = errorCode != null ? errorCode : "";
        this.errorMsg = errorMsg != null ? errorMsg : "";
    }

    public APIException(int statusCode, String errorCode, String errorMsg, Throwable cause) {
        super(formatMessage(statusCode, errorCode, errorMsg), cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode != null ? errorCode : "";
        this.errorMsg = errorMsg != null ? errorMsg : "";
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    private static String formatMessage(int statusCode, String errorCode, String errorMsg) {
        return "[" + (errorCode != null ? errorCode : "") + "] HTTP " + statusCode
                + ": " + (errorMsg != null ? errorMsg : "");
    }
}
