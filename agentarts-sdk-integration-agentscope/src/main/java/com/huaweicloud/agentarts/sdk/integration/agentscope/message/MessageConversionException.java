package com.huaweicloud.agentarts.sdk.integration.agentscope.message;

/** Signals that a message cannot be converted without losing data. */
public class MessageConversionException extends RuntimeException {

    public MessageConversionException(String message) {
        super(message);
    }

    public MessageConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
