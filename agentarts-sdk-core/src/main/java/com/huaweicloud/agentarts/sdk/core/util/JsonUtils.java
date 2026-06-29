package com.huaweicloud.agentarts.sdk.core.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Shared JSON utilities for the AgentArts SDK.
 *
 * <p>Provides a single, thread-safe {@link ObjectMapper} instance used across
 * all SDK modules, eliminating duplicate instances and ensuring consistent
 * serialization behavior.</p>
 */
public final class JsonUtils {

    private JsonUtils() {}

    /** Shared, thread-safe ObjectMapper instance. */
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Extract an error message from a JSON response body.
     *
     * <p>Tries common error field names: "error", "message", "error_msg".
     * Returns null if parsing fails or no error fields found.</p>
     */
    public static String extractErrorMessage(String body) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(body);
            // Try "error" field first
            JsonNode errorNode = node.get("error");
            if (errorNode != null && errorNode.isTextual()) {
                String error = errorNode.asText();
                // Also check for "message" field
                JsonNode msgNode = node.get("message");
                if (msgNode != null && msgNode.isTextual()) {
                    return error + ": " + msgNode.asText();
                }
                return error;
            }
            // Try "message" field alone
            JsonNode msgNode = node.get("message");
            if (msgNode != null && msgNode.isTextual()) {
                return msgNode.asText();
            }
            // Try "error_msg" field (Huawei Cloud SDK format)
            JsonNode errorMsgNode = node.get("error_msg");
            if (errorMsgNode != null && errorMsgNode.isTextual()) {
                return errorMsgNode.asText();
            }
        } catch (Exception ignored) {
            // Not valid JSON
        }
        return null;
    }

    /**
     * Safely serialize an object to JSON string.
     * Returns "{}" on failure.
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Check if a string is null or empty.
     */
    public static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Check if a string is non-null and non-empty.
     */
    public static boolean isNotBlank(String s) {
        return s != null && !s.isEmpty();
    }
}
