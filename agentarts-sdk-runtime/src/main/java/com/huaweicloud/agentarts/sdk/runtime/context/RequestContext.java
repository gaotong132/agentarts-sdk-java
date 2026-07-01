package com.huaweicloud.agentarts.sdk.runtime.context;

import com.huaweicloud.agentarts.sdk.core.Constants;

/**
 * Immutable snapshot of an incoming request's context.
 *
 * <p>Immutable snapshot of an incoming request context.</p>
 *
 * <p>Holds the session ID, request ID, and workload access token
 * extracted from HTTP headers.</p>
 */
public class RequestContext {

    private final String requestId;
    private final String sessionId;
    private final String userId;
    private final String workloadAccessToken;

    public RequestContext(String requestId, String sessionId, String userId, String workloadAccessToken) {
        this.requestId = requestId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.workloadAccessToken = workloadAccessToken;
    }

    public RequestContext(String requestId, String sessionId) {
        this(requestId, sessionId, null, null);
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public String getWorkloadAccessToken() {
        return workloadAccessToken;
    }

    /**
     * Build a RequestContext from HTTP headers.
     *
     * @param headerGetter function to get header value by name
     * @return populated RequestContext
     */
    public static RequestContext fromHeaders(java.util.function.Function<String, String> headerGetter) {
        String sessionId = getHeaderCaseInsensitive(headerGetter, Constants.SESSION_HEADER);
        String accessToken = getHeaderCaseInsensitive(headerGetter, Constants.ACCESS_TOKEN_HEADER);
        String userId = getHeaderCaseInsensitive(headerGetter, Constants.USER_ID_HEADER);
        String requestId = getHeaderCaseInsensitive(headerGetter, Constants.REQUEST_ID_HEADER);

        if (requestId == null || requestId.isEmpty()) {
            requestId = java.util.UUID.randomUUID().toString().replace("-", "");
        }

        return new RequestContext(requestId, sessionId, userId, accessToken);
    }

    private static String getHeaderCaseInsensitive(
            java.util.function.Function<String, String> getter, String name) {
        String value = getter.apply(name);
        if (value == null || value.isEmpty()) {
            value = getter.apply(name.toLowerCase(java.util.Locale.ROOT));
        }
        return value;
    }
}
