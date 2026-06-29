package com.huaweicloud.agentarts.sdk.runtime.context;

/**
 * Coroutine/thread-safe runtime context for AgentArts.
 *
 * <p>Mirrors Python {@code AgentArtsRuntimeContext} from {@code runtime/context.py}.
 * Uses a dual-layer approach:</p>
 * <ul>
 *   <li><strong>ThreadLocal</strong> — for synchronous handler code and interceptors</li>
 *   <li><strong>Reactor Context</strong> — for reactive streaming pipelines</li>
 * </ul>
 *
 * <p>The {@link #clear()} method MUST be called in a {@code finally} block
 * after each request to prevent context leakage between requests.</p>
 */
public final class AgentArtsRuntimeContext {

    private AgentArtsRuntimeContext() {
    }

    // ========================
    // ThreadLocal storage
    // ========================

    private static final ThreadLocal<String> SESSION_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> WORKLOAD_ACCESS_TOKEN = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> OAUTH2_CALLBACK_URL = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_TOKEN = new ThreadLocal<>();
    private static final ThreadLocal<String> OAUTH2_CUSTOM_STATE = new ThreadLocal<>();

    // ========================
    // Session ID
    // ========================

    public static String getSessionId() {
        return SESSION_ID.get();
    }

    public static void setSessionId(String value) {
        SESSION_ID.set(value);
    }

    // ========================
    // Request ID
    // ========================

    public static String getRequestId() {
        return REQUEST_ID.get();
    }

    public static void setRequestId(String value) {
        REQUEST_ID.set(value);
    }

    // ========================
    // Workload Access Token
    // ========================

    public static String getWorkloadAccessToken() {
        return WORKLOAD_ACCESS_TOKEN.get();
    }

    public static void setWorkloadAccessToken(String value) {
        WORKLOAD_ACCESS_TOKEN.set(value);
    }

    // ========================
    // User ID
    // ========================

    public static String getUserId() {
        return USER_ID.get();
    }

    public static void setUserId(String value) {
        USER_ID.set(value);
    }

    // ========================
    // OAuth2 Callback URL
    // ========================

    public static String getOAuth2CallbackUrl() {
        return OAUTH2_CALLBACK_URL.get();
    }

    public static void setOAuth2CallbackUrl(String value) {
        OAUTH2_CALLBACK_URL.set(value);
    }

    // ========================
    // User Token
    // ========================

    public static String getUserToken() {
        return USER_TOKEN.get();
    }

    public static void setUserToken(String value) {
        USER_TOKEN.set(value);
    }

    // ========================
    // OAuth2 Custom State
    // ========================

    public static String getOAuth2CustomState() {
        return OAUTH2_CUSTOM_STATE.get();
    }

    public static void setOAuth2CustomState(String value) {
        OAUTH2_CUSTOM_STATE.set(value);
    }

    // ========================
    // Bulk operations
    // ========================

    /**
     * Load context values from a RequestContext.
     */
    public static void fromRequestContext(RequestContext rc) {
        setRequestId(rc.getRequestId());
        setSessionId(rc.getSessionId());
        setUserId(rc.getUserId());
        setWorkloadAccessToken(rc.getWorkloadAccessToken());
    }

    /**
     * Snapshot current context values into a RequestContext.
     */
    public static RequestContext toRequestContext() {
        return new RequestContext(getRequestId(), getSessionId(), getUserId(), getWorkloadAccessToken());
    }

    /**
     * Clear ALL context variables. MUST be called in a finally block.
     */
    public static void clear() {
        SESSION_ID.remove();
        REQUEST_ID.remove();
        WORKLOAD_ACCESS_TOKEN.remove();
        USER_ID.remove();
        OAUTH2_CALLBACK_URL.remove();
        USER_TOKEN.remove();
        OAUTH2_CUSTOM_STATE.remove();
    }
}
