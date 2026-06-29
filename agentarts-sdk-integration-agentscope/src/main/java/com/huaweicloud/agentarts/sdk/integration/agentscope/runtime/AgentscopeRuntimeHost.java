package com.huaweicloud.agentarts.sdk.integration.agentscope.runtime;

import com.huaweicloud.agentarts.sdk.core.PingStatus;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import com.huaweicloud.agentarts.sdk.runtime.context.RequestContext;
import io.agentscope.core.agent.RuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Bridge layer between AgentArts Runtime and agentscope-java.
 *
 * <p>Registers entrypoint and ping handlers on the AgentArts Runtime
 * that translate between AgentArts RequestContext and agentscope RuntimeContext.</p>
 *
 * <h3>Context mapping:</h3>
 * <ul>
 *   <li>RequestContext.sessionId → RuntimeContext.sessionId</li>
 *   <li>RequestContext.userId → RuntimeContext.userId</li>
 *   <li>RequestContext.requestId → RuntimeContext.put("requestId", ...)</li>
 *   <li>RequestContext.workloadAccessToken → RuntimeContext.put("workloadAccessToken", ...)</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
 * ReActAgent agent = buildAgent();
 * new AgentscopeRuntimeHost(app, agent);
 * app.run(8080);
 * }</pre>
 */
public class AgentscopeRuntimeHost {

    private static final Logger LOG = LoggerFactory.getLogger(AgentscopeRuntimeHost.class);

    private final AgentArtsRuntimeApp app;

    /**
     * Create a runtime host that bridges AgentArts Runtime with an agentscope handler.
     *
     * @param app     the AgentArts runtime app
     * @param handler function that receives (payload, RuntimeContext) and returns a result
     */
    public AgentscopeRuntimeHost(AgentArtsRuntimeApp app,
                                  BiFunction<Map<String, Object>, RuntimeContext, Object> handler) {
        this.app = app;

        // Register entrypoint that bridges RequestContext → RuntimeContext
        app.setEntrypoint((Map<String, Object> payload, RequestContext requestCtx) -> {
            RuntimeContext runtimeCtx = bridgeContext(requestCtx);
            return handler.apply(payload, runtimeCtx);
        });

        // Default ping handler
        app.setPingHandler(() -> PingStatus.HEALTHY);

        LOG.info("AgentscopeRuntimeHost registered on AgentArts Runtime");
    }

    /**
     * Bridge AgentArts RequestContext to agentscope RuntimeContext.
     */
    public static RuntimeContext bridgeContext(RequestContext requestCtx) {
        RuntimeContext.Builder builder = RuntimeContext.builder();

        if (requestCtx.getSessionId() != null) {
            builder.sessionId(requestCtx.getSessionId());
        }
        if (requestCtx.getUserId() != null) {
            builder.userId(requestCtx.getUserId());
        }
        if (requestCtx.getRequestId() != null) {
            builder.put("requestId", requestCtx.getRequestId());
        }
        if (requestCtx.getWorkloadAccessToken() != null) {
            builder.put("workloadAccessToken", requestCtx.getWorkloadAccessToken());
        }

        return builder.build();
    }

    public AgentArtsRuntimeApp getApp() {
        return app;
    }
}
