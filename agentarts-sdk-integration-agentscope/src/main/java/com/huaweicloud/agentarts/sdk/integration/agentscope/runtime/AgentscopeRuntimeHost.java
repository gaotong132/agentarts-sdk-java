package com.huaweicloud.agentarts.sdk.integration.agentscope.runtime;

import com.huaweicloud.agentarts.sdk.core.PingStatus;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import com.huaweicloud.agentarts.sdk.runtime.context.RequestContext;
import io.agentscope.core.agent.RuntimeContext;
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
 * that translate between AgentArts RequestContext and agentscope RuntimeContext.</p>
 *
 * <h2>Context mapping:</h2>
 * <ul>
 *   <li>RequestContext.sessionId → RuntimeContext.sessionId</li>
 *   <li>RequestContext.userId → RuntimeContext.userId</li>
 *   <li>RequestContext.requestId → RuntimeContext.put("requestId", ...)</li>
 *   <li>RequestContext.workloadAccessToken → RuntimeContext.put("workloadAccessToken", ...)</li>
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
    public static final String REQUEST_ID_KEY = "requestId";
    public static final String WORKLOAD_ACCESS_TOKEN_KEY = "workloadAccessToken";

    private final AgentArtsRuntimeApp app;

    /**
     * Create a runtime host that bridges AgentArts Runtime with an agentscope handler.
     *
     * @param app     the AgentArts runtime app
     * @param handler function that receives (payload, RuntimeContext) and returns a result
     */
    public AgentscopeRuntimeHost(AgentArtsRuntimeApp app,
                                  BiFunction<Map<String, Object>, RuntimeContext, Object> handler) {
        this(app, handler, () -> PingStatus.HEALTHY);
    }

    /** Create a runtime host with an application-specific readiness probe. */
    public AgentscopeRuntimeHost(
            AgentArtsRuntimeApp app,
            BiFunction<Map<String, Object>, RuntimeContext, Object> handler,
            Supplier<PingStatus> pingHandler) {
        this.app = Objects.requireNonNull(app, "app must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        Objects.requireNonNull(pingHandler, "pingHandler must not be null");

        // Register entrypoint that bridges RequestContext → RuntimeContext
        this.app.setEntrypoint((Map<String, Object> payload, RequestContext requestCtx) -> {
            RuntimeContext runtimeCtx = bridgeContext(requestCtx);
            return handler.apply(payload, runtimeCtx);
        });

        // Default ping handler
        this.app.setPingHandler(pingHandler);

        LOG.info("AgentscopeRuntimeHost registered on AgentArts Runtime");
    }

    /**
     * Bridge AgentArts RequestContext to agentscope RuntimeContext.
     */
    public static RuntimeContext bridgeContext(RequestContext requestCtx) {
        Objects.requireNonNull(requestCtx, "requestCtx must not be null");
        RuntimeContext.Builder builder = RuntimeContext.builder();

        if (requestCtx.getSessionId() != null) {
            builder.sessionId(requestCtx.getSessionId());
        }
        if (requestCtx.getUserId() != null) {
            builder.userId(requestCtx.getUserId());
        }
        if (requestCtx.getRequestId() != null) {
            builder.put(REQUEST_ID_KEY, requestCtx.getRequestId());
        }
        if (requestCtx.getWorkloadAccessToken() != null) {
            builder.put(WORKLOAD_ACCESS_TOKEN_KEY, requestCtx.getWorkloadAccessToken());
        }

        return builder.build();
    }

    public AgentArtsRuntimeApp getApp() {
        return app;
    }
}
