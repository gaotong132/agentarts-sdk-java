package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.core.PingStatus;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import com.huaweicloud.agentarts.sdk.runtime.context.RequestContext;

import java.util.Map;

/**
 * Test fixture loaded by {@code agentarts dev} to verify the
 * {@code public static AgentArtsRuntimeApp createApp()} factory contract that the
 * scaffolded {@code Agent.java} templates expose. The E2E test copies this compiled
 * fixture into the temporary project's {@code target/classes}, avoiding any
 * dependency on a stale SDK snapshot in the local Maven repository.
 */
public class DevTestAgent {

    public static AgentArtsRuntimeApp createApp() {
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
        app.setEntrypoint((Map<String, Object> payload, RequestContext ctx) ->
                Map.of("result", "dev-test: " + payload.getOrDefault("message", "")));
        app.setPingHandler(() -> PingStatus.HEALTHY);
        return app;
    }
}
