package com.huaweicloud.agentarts.sdk.runtime.context;

import io.micrometer.context.ContextRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coroutine/thread-safe runtime context for AgentArts.
 *
 * <p>Thread-local runtime context for AgentArts request processing.
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
    private static final String CONTEXT_PREFIX = AgentArtsRuntimeContext.class.getName() + ".";
    private static final String SESSION_ID_KEY = CONTEXT_PREFIX + "sessionId";
    private static final String REQUEST_ID_KEY = CONTEXT_PREFIX + "requestId";
    private static final String WORKLOAD_ACCESS_TOKEN_KEY = CONTEXT_PREFIX + "workloadAccessToken";
    private static final String USER_ID_KEY = CONTEXT_PREFIX + "userId";
    private static final String OAUTH2_CALLBACK_URL_KEY = CONTEXT_PREFIX + "oauth2CallbackUrl";
    private static final String USER_TOKEN_KEY = CONTEXT_PREFIX + "userToken";
    private static final String OAUTH2_CUSTOM_STATE_KEY = CONTEXT_PREFIX + "oauth2CustomState";
    private static final AtomicBoolean REACTOR_PROPAGATION_ENABLED = new AtomicBoolean();

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
     * Register AgentArts ThreadLocals with Micrometer Context Propagation and
     * enable Reactor's automatic restoration mode. Registration is idempotent
     * for the current class loader and affects new Reactor subscriptions.
     */
    public static void enableReactorContextPropagation() {
        if (!REACTOR_PROPAGATION_ENABLED.compareAndSet(false, true)) {
            return;
        }
        ContextRegistry registry = ContextRegistry.getInstance();
        registry.registerThreadLocalAccessor(SESSION_ID_KEY, SESSION_ID);
        registry.registerThreadLocalAccessor(REQUEST_ID_KEY, REQUEST_ID);
        registry.registerThreadLocalAccessor(WORKLOAD_ACCESS_TOKEN_KEY, WORKLOAD_ACCESS_TOKEN);
        registry.registerThreadLocalAccessor(USER_ID_KEY, USER_ID);
        registry.registerThreadLocalAccessor(OAUTH2_CALLBACK_URL_KEY, OAUTH2_CALLBACK_URL);
        registry.registerThreadLocalAccessor(USER_TOKEN_KEY, USER_TOKEN);
        registry.registerThreadLocalAccessor(OAUTH2_CUSTOM_STATE_KEY, OAUTH2_CUSTOM_STATE);
        Hooks.enableAutomaticContextPropagation();
    }

    /** Capture the current AgentArts context and attach it to a Flux subscription. */
    public static <T> Flux<T> propagate(Flux<T> publisher) {
        Objects.requireNonNull(publisher, "publisher must not be null");
        enableReactorContextPropagation();
        Snapshot snapshot = Snapshot.capture();
        return publisher.contextWrite(snapshot::writeTo);
    }

    /** Capture the current AgentArts context and attach it to a Mono subscription. */
    public static <T> Mono<T> propagate(Mono<T> publisher) {
        Objects.requireNonNull(publisher, "publisher must not be null");
        enableReactorContextPropagation();
        Snapshot snapshot = Snapshot.capture();
        return publisher.contextWrite(snapshot::writeTo);
    }

    private record Snapshot(
            String sessionId,
            String requestId,
            String workloadAccessToken,
            String userId,
            String oauth2CallbackUrl,
            String userToken,
            String oauth2CustomState) {

        static Snapshot capture() {
            return new Snapshot(getSessionId(), getRequestId(), getWorkloadAccessToken(),
                    getUserId(), getOAuth2CallbackUrl(), getUserToken(), getOAuth2CustomState());
        }

        Context writeTo(Context context) {
            Context result = putOrDelete(context, SESSION_ID_KEY, sessionId);
            result = putOrDelete(result, REQUEST_ID_KEY, requestId);
            result = putOrDelete(result, WORKLOAD_ACCESS_TOKEN_KEY, workloadAccessToken);
            result = putOrDelete(result, USER_ID_KEY, userId);
            result = putOrDelete(result, OAUTH2_CALLBACK_URL_KEY, oauth2CallbackUrl);
            result = putOrDelete(result, USER_TOKEN_KEY, userToken);
            return putOrDelete(result, OAUTH2_CUSTOM_STATE_KEY, oauth2CustomState);
        }

        private static Context putOrDelete(Context context, String key, String value) {
            return value != null ? context.put(key, value) : context.delete(key);
        }
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
