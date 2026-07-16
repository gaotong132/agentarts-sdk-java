package com.huaweicloud.agentarts.sdk.service.http;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Result of an HTTP request, with support for streaming responses.
 *
 * <p>Result of an HTTP request, with support for streaming responses.</p>
 *
 * <p>For non-streaming responses, {@link #getData()} contains the parsed JSON
 * (as {@link JsonNode}) or the raw response body as a String.</p>
 *
 * <p>For streaming responses (Content-Type: text/event-stream or application/x-ndjson),
 * {@link #isStreaming()} returns true and the response body is available via
 * {@link #iterLines()} or {@link #iterBytes()}. The network body is single-use:
 * callers must choose one representation and subscribe once. Cancelling that
 * subscription aborts the underlying HTTP response.</p>
 */
public class RequestResult implements AutoCloseable {

    private final boolean success;
    private final int statusCode;
    private final Object data;
    private final String error;
    private final Map<String, String> headers;
    private final boolean streaming;
    private final Flux<String> lineStream;
    private final Flux<byte[]> byteStream;
    private final Runnable closeAction;
    private final AtomicBoolean closed = new AtomicBoolean();

    private RequestResult(Builder builder) {
        this.success = builder.success;
        this.statusCode = builder.statusCode;
        this.data = builder.data;
        this.error = builder.error;
        this.headers = builder.headers != null
                ? Collections.unmodifiableMap(new HashMap<>(builder.headers))
                : Collections.emptyMap();
        this.streaming = builder.streaming;
        this.lineStream = builder.lineStream;
        this.byteStream = builder.byteStream;
        this.closeAction = builder.closeAction != null ? builder.closeAction : () -> { };
    }

    /** Whether the response was successful (2xx status code). */
    public boolean isSuccess() {
        return success;
    }

    /** HTTP status code (0 if request failed before receiving a response). */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Parsed response data.
     * For JSON responses: {@link JsonNode}.
     * For non-JSON responses: {@link String}.
     * For streaming responses: {@code null}.
     */
    public Object getData() {
        return data;
    }

    /** Get data as JsonNode, or null if not JSON. */
    public JsonNode getDataAsJson() {
        if (data instanceof JsonNode) {
            return (JsonNode) data;
        }
        return null;
    }

    /** Get data as String. */
    public String getDataAsString() {
        if (data instanceof String) {
            return (String) data;
        }
        if (data != null) {
            return data.toString();
        }
        return null;
    }

    /** Error message if the request failed. */
    public String getError() {
        return error;
    }

    /** Response headers (unmodifiable). */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /** Whether this is a streaming response. */
    public boolean isStreaming() {
        return streaming;
    }

    /**
     * Stream response body as lines (for SSE / NDJSON).
     * Empty delimiter lines are preserved and CRLF is normalized.
     *
     * @throws IllegalStateException if not a streaming response
     */
    public Flux<String> iterLines() {
        if (!streaming || lineStream == null) {
            throw new IllegalStateException("Not a streaming response");
        }
        return lineStream;
    }

    /**
     * Stream response body as byte chunks.
     * This cannot be consumed after {@link #iterLines()} has been subscribed.
     *
     * @throws IllegalStateException if not a streaming response
     */
    public Flux<byte[]> iterBytes() {
        if (!streaming || byteStream == null) {
            throw new IllegalStateException("Not a streaming response");
        }
        return byteStream;
    }

    /**
     * Release an unconsumed or partially consumed streaming response.
     *
     * <p>Closing a non-streaming result is a no-op. Closing is idempotent. A
     * streaming subscription also releases the response when it is cancelled,
     * so try-with-resources is mainly useful when code may decide not to consume
     * a response after inspecting its status or headers.</p>
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            closeAction.run();
        }
    }

    /**
     * Extract error message from a failed JSON response.
     * Checks common error fields: "error", "message", "error_msg".
     */
    public String extractErrorMessage() {
        if (error != null) {
            return error;
        }
        JsonNode json = getDataAsJson();
        if (json != null) {
            if (json.has("error")) {
                JsonNode errNode = json.get("error");
                if (errNode.isTextual()) {
                    return errNode.asText();
                }
                if (errNode.isObject() && errNode.has("message")) {
                    return errNode.get("message").asText();
                }
            }
            if (json.has("message")) {
                return json.get("message").asText();
            }
            if (json.has("error_msg")) {
                return json.get("error_msg").asText();
            }
        }
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success;
        private int statusCode;
        private Object data;
        private String error;
        private Map<String, String> headers;
        private boolean streaming;
        private Flux<String> lineStream;
        private Flux<byte[]> byteStream;
        private Runnable closeAction;

        public Builder success(boolean success) { this.success = success; return this; }
        public Builder statusCode(int statusCode) { this.statusCode = statusCode; return this; }
        public Builder data(Object data) { this.data = data; return this; }
        public Builder error(String error) { this.error = error; return this; }
        public Builder headers(Map<String, String> headers) { this.headers = headers; return this; }
        public Builder streaming(boolean streaming) { this.streaming = streaming; return this; }
        public Builder lineStream(Flux<String> lineStream) { this.lineStream = lineStream; return this; }
        public Builder byteStream(Flux<byte[]> byteStream) { this.byteStream = byteStream; return this; }
        public Builder closeAction(Runnable closeAction) { this.closeAction = closeAction; return this; }

        public RequestResult build() {
            return new RequestResult(this);
        }
    }
}
