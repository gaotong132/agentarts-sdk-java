package com.huaweicloud.agentarts.sdk.integration.agentscope.runtime;

import com.huaweicloud.agentarts.sdk.core.PingStatus;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import com.huaweicloud.agentarts.sdk.runtime.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Bridge layer between AgentArts Runtime and agentscope-java.
 *
 * <p>Registers entrypoint and ping handlers on the AgentArts Runtime
 * that translate between AgentArts RequestContext and an immutable AgentScope request context.</p>
 *
 * <h2>Context mapping:</h2>
 * <ul>
 *   <li>RequestContext.sessionId → AgentscopeRequestContext.sessionId</li>
 *   <li>RequestContext.userId → AgentscopeRequestContext.userId</li>
 *   <li>RequestContext.requestId → AgentscopeRequestContext.requestId</li>
 *   <li>RequestContext.workloadAccessToken → AgentscopeRequestContext.workloadAccessToken</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
 * ReActAgent agent = buildAgent();
 * new AgentscopeRuntimeHost(app, agent);
 * app.run(8080);
 * }</pre>
 */
public final class AgentscopeRuntimeHost {

    private static final Logger LOG = LoggerFactory.getLogger(AgentscopeRuntimeHost.class);
    private final AgentArtsRuntimeApp app;

    /**
     * Create a runtime host that bridges AgentArts Runtime with an agentscope handler.
     *
     * @param app     the AgentArts runtime app
     * @param handler function that receives payload and request context and returns a result
     */
    public AgentscopeRuntimeHost(AgentArtsRuntimeApp app,
                                  BiFunction<Map<String, Object>, AgentscopeRequestContext, Object> handler) {
        this(app, handler, () -> PingStatus.HEALTHY);
    }

    /** Create a runtime host with an application-specific readiness probe. */
    public AgentscopeRuntimeHost(
            AgentArtsRuntimeApp app,
            BiFunction<Map<String, Object>, AgentscopeRequestContext, Object> handler,
            Supplier<PingStatus> pingHandler) {
        this.app = Objects.requireNonNull(app, "app must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        Objects.requireNonNull(pingHandler, "pingHandler must not be null");

        // Register entrypoint that bridges the immutable request context.
        this.app.setEntrypoint((Map<String, Object> payload, RequestContext requestCtx) -> {
            AgentscopeRequestContext runtimeCtx = bridgeContext(requestCtx);
            return handler.apply(payload, runtimeCtx);
        });

        // Default ping handler
        this.app.setPingHandler(pingHandler);

        LOG.info("AgentscopeRuntimeHost registered on AgentArts Runtime");
    }

    /**
     * Bridge AgentArts RequestContext to an AgentScope invocation context.
     */
    public static AgentscopeRequestContext bridgeContext(RequestContext requestCtx) {
        Objects.requireNonNull(requestCtx, "requestCtx must not be null");
        return new AgentscopeRequestContext(
                requestCtx.getSessionId(),
                requestCtx.getUserId(),
                requestCtx.getRequestId(),
                requestCtx.getWorkloadAccessToken());
    }

    public AgentArtsRuntimeApp getApp() {
        return app;
    }
}
