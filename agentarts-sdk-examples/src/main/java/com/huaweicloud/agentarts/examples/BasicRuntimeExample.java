package com.huaweicloud.agentarts.examples;

import com.huaweicloud.agentarts.sdk.core.PingStatus;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import com.huaweicloud.agentarts.sdk.runtime.context.RequestContext;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

/**
 * Basic Runtime example: a standalone AgentArts agent using Vert.x HTTP server.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>Registering an entrypoint handler (sync JSON + SSE streaming)</li>
 *   <li>Registering a ping handler</li>
 *   <li>Accessing RequestContext (sessionId, userId, requestId)</li>
 *   <li>Starting the server on port 8080</li>
 * </ul>
 *
 * <h3>Run:</h3>
 * <pre>
 * mvn compile exec:java -pl agentarts-sdk-examples \
 *   -Dexec.mainClass="com.huaweicloud.agentarts.examples.BasicRuntimeExample"
 * </pre>
 *
 * <h3>Test:</h3>
 * <pre>
 * # Health check
 * curl http://localhost:8080/ping
 * # → {"status":"Healthy","time_of_last_update":...}
 *
 * # Sync invocation
 * curl -X POST http://localhost:8080/invocations \
 *   -H "Content-Type: application/json" \
 *   -d '{"message":"Hello!"}'
 * # → {"reply":"Echo: Hello!","sessionId":"...","requestId":"..."}
 *
 * # Streaming invocation
 * curl -X POST http://localhost:8080/invocations \
 *   -H "Content-Type: application/json" \
 *   -d '{"stream":true,"message":"Hello!"}'
 * # → SSE stream: data: {"chunk":0,"text":"Hello!"}\n\n ...
 * </pre>
 */
public class BasicRuntimeExample {

    public static void main(String[] args) {
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();

        // Register entrypoint: handles both sync and streaming modes
        app.setEntrypoint((Map<String, Object> payload, RequestContext ctx) -> {
            String message = (String) payload.getOrDefault("message", "");
            boolean stream = Boolean.TRUE.equals(payload.get("stream"));

            if (stream) {
                // SSE streaming: emit chunks with delay
                return Flux.interval(Duration.ofMillis(200))
                        .take(5)
                        .map(i -> Map.of(
                                "chunk", i,
                                "text", message + " (chunk " + i + ")",
                                "sessionId", ctx.getSessionId() != null ? ctx.getSessionId() : ""
                        ));
            } else {
                // Sync JSON response
                return Map.of(
                        "reply", "Echo: " + message,
                        "sessionId", ctx.getSessionId() != null ? ctx.getSessionId() : "",
                        "requestId", ctx.getRequestId()
                );
            }
        });

        // Register ping handler
        app.setPingHandler(() -> PingStatus.HEALTHY);

        // Start the server
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        System.out.println("Starting BasicRuntimeExample on port " + port + "...");
        System.out.println("  Health check:  GET  http://localhost:" + port + "/ping");
        System.out.println("  Invocation:    POST http://localhost:" + port + "/invocations");
        app.run(port);
    }
}
