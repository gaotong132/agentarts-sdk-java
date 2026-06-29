package com.example;

import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import com.huaweicloud.agentarts.sdk.runtime.context.RequestContext;
import java.util.Map;

public class {{ name }}Agent {

    public static void main(String[] args) {
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();

        app.setEntrypoint((Map<String, Object> payload, RequestContext ctx) -> {
            String message = (String) payload.getOrDefault("message", "");
            return Map.of("result", "Hello from {{ name }}: " + message);
        });

        app.setPingHandler(() -> com.huaweicloud.agentarts.sdk.core.PingStatus.HEALTHY);

        System.out.println("Starting {{ name }} agent...");
        app.run(8080);
    }
}
