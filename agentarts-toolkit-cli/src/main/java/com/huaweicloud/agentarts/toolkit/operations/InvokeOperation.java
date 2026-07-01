package com.huaweicloud.agentarts.toolkit.operations;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Invoke operation: send JSON payload to local or cloud agent.
 * Operation: invoke agent with JSON payload (local or cloud).
 */
public class InvokeOperation {

    /**
     * Invoke an agent with a JSON payload (local or cloud).
     *
     * @param payload      JSON payload string
     * @param agentName    agent name (for cloud mode)
     * @param mode         invocation mode: "local" or "cloud"
     * @param region       Huawei Cloud region (for cloud mode)
     * @param port         local server port (for local mode, default 8080)
     * @param endpoint     custom endpoint URL (for cloud mode, nullable)
     * @param sessionId    session ID for context continuity (nullable)
     * @param bearerToken  Bearer token for cloud auth (nullable)
     * @param timeout      request timeout in seconds
     * @param skipSsl      skip SSL verification
     * @param userId       user ID header (nullable)
     * @param customPath   custom invocation path (nullable)
     */
    public static void invokeAgent(String payload, String agentName, String mode,
                                    String region, Integer port, String endpoint,
                                    String sessionId, String bearerToken, int timeout,
                                    boolean skipSsl, String userId, String customPath) throws Exception {
        if ("local".equals(mode)) {
            int p = port != null ? port : 8080;
            invokeLocal(payload, p, sessionId, timeout);
        } else {
            invokeCloud(payload, agentName, region, endpoint, sessionId, bearerToken,
                    timeout, skipSsl, userId, customPath);
        }
    }

    private static void invokeLocal(String payload, int port, String sessionId, int timeout) throws Exception {
        Vertx vertx = Vertx.vertx();
        WebClient client = WebClient.create(vertx, new WebClientOptions()
                .setConnectTimeout(timeout * 1000));

        CountDownLatch latch = new CountDownLatch(1);
        client.post(port, "localhost", "/invocations")
                .putHeader("Content-Type", "application/json")
                .putHeader("x-hw-agentarts-session-id", sessionId != null ? sessionId : "")
                .sendBuffer(Buffer.buffer(payload))
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        System.out.println(ar.result().bodyAsString());
                    } else {
                        System.err.println("Error: " + ar.cause().getMessage());
                    }
                    latch.countDown();
                });

        latch.await(timeout, TimeUnit.SECONDS);
        client.close();
        vertx.close();
    }

    private static void invokeCloud(String payload, String agentName, String region,
                                     String endpoint, String sessionId, String bearerToken,
                                     int timeout, boolean skipSsl, String userId,
                                     String customPath) throws Exception {
        System.out.println("Invoking cloud agent '" + agentName + "' in region " + region + "...");
        // TODO: RuntimeServiceClient.invokeAgent with V11 signing
    }
}
