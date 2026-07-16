package com.huaweicloud.agentarts.sdk.core.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** URL construction helpers shared by service clients. */
public final class UrlUtils {

    private UrlUtils() {}

    /**
     * Encode one untrusted URL path segment according to RFC 3986.
     * Separators and traversal characters are encoded rather than interpreted.
     */
    public static String encodePathSegment(String value, String parameterName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " must not be blank");
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%7E", "~")
                .replace("*", "%2A");
    }

    /** Encode a slash-delimited relative path while rejecting traversal and empty segments. */
    public static String encodeRelativePath(String value, String parameterName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " must not be blank");
        }
        String[] segments = value.split("/", -1);
        StringBuilder encoded = new StringBuilder();
        for (String segment : segments) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException(
                        parameterName + " must not contain empty or traversal segments");
            }
            if (!encoded.isEmpty()) encoded.append('/');
            encoded.append(encodePathSegment(segment, parameterName));
        }
        return encoded.toString();
    }
}
