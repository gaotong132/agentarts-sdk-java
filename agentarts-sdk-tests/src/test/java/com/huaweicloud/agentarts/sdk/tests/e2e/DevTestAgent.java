package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.core.PingStatus;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import com.huaweicloud.agentarts.sdk.runtime.context.RequestContext;

import java.util.Map;

/**
 * Test fixture loaded by {@code agentarts dev} to verify the
 * {@code public static AgentArtsRuntimeApp createApp()} factory contract that the
 * scaffolded {@code Agent.java} templates expose. Lives on the test classpath so
 * {@link com.huaweicloud.agentarts.toolkit.operations.DevOperation} can resolve it
 * via its URLClassLoader parent (the same surefire classloader) without having to
 * compile a project inside the test.
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
