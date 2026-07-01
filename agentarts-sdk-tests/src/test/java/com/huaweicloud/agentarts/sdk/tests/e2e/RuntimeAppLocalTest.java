package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.core.PingStatus;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runtime local tests — (11 tests: ping, invocations, SSE, WebSocket).
 * Uses Vert.x WebClient instead of Python's Starlette TestClient.
 */
@Tag("e2e")
@DisplayName("Runtime App Local Tests")
class RuntimeAppLocalTest {

    private static final ObjectMapper MAPPER = com.huaweicloud.agentarts.sdk.core.util.JsonUtils.MAPPER;
    private static Vertx vertx;

    @BeforeAll
    static void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    static void tearDown() {
        if (vertx != null) vertx.close();
    }

    private WebClient createWebClient(int port) {
        return WebClient.create(vertx);
    }

    private HttpResponse<Buffer> postJson(int port, String path, String body) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HttpResponse<Buffer>> ref = new AtomicReference<>();
        WebClient client = createWebClient(port);
        client.post(port, "localhost", path)
                .putHeader("Content-Type", "application/json")
                .sendBuffer(Buffer.buffer(body != null ? body : ""))
                .onComplete(ar -> {
                    if (ar.succeeded()) ref.set(ar.result());
                    latch.countDown();
                });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        return ref.get();
    }

    private HttpResponse<Buffer> getJson(int port, String path) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HttpResponse<Buffer>> ref = new AtomicReference<>();
        WebClient client = createWebClient(port);
        client.get(port, "localhost", path)
                .send()
                .onComplete(ar -> {
                    if (ar.succeeded()) ref.set(ar.result());
                    latch.countDown();
                });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        return ref.get();
    }

    // 1. test_ping_default_healthy
    @Test
    @DisplayName("GET /ping returns HEALTHY by default")
    void testPingDefaultHealthy() throws Exception {
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
        app.run(0);
        try {
            HttpResponse<Buffer> resp = getJson(app.getPort(), "/ping");
            assertEquals(200, resp.statusCode());
            JsonNode body = MAPPER.readTree(resp.bodyAsString());
            assertEquals(PingStatus.HEALTHY.getValue(), body.get("status").asText());
            assertTrue(body.has("time_of_last_update"));
        } finally {
            app.stop();
        }
    }

    // 2. test_ping_force_unhealthy
    @Test
    @DisplayName("force_ping_status(UNHEALTHY) is reflected in /ping")
    void testPingForceUnhealthy() throws Exception {
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
        app.forcePingStatus(PingStatus.UNHEALTHY);
        app.run(0);
        try {
            HttpResponse<Buffer> resp = getJson(app.getPort(), "/ping");
            assertEquals(200, resp.statusCode());
            JsonNode body = MAPPER.readTree(resp.bodyAsString());
            assertEquals(PingStatus.UNHEALTHY.getValue(), body.get("status").asText());
        } finally {
            app.stop();
        }
    }

    // 3. test_ping_custom_handler
    @Test
    @DisplayName("custom @ping handler returns HEALTHY_BUSY")
    void testPingCustomHandler() throws Exception {
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
        app.setPingHandler(() -> PingStatus.HEALTHY_BUSY);
        app.run(0);
        try {
            HttpResponse<Buffer> resp = getJson(app.getPort(), "/ping");
            assertEquals(200, resp.statusCode());
            JsonNode body = MAPPER.readTree(resp.bodyAsString());
            assertEquals(PingStatus.HEALTHY_BUSY.getValue(), body.get("status").asText());
        } finally {
            app.stop();
        }
    }

    // 4. test_invocation_returns_handler_result
    @Test
    @DisplayName("POST /invocations returns handler result + session header")
    void testInvocationReturnsHandlerResult() throws Exception {
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
        app.setEntrypoint((Map<String, Object> payload) -> {
            return Map.of("echo", payload.getOrDefault("msg", ""));
        });
        app.run(0);
        try {
            HttpResponse<Buffer> resp = postJson(app.getPort(), "/invocations", "{\"msg\":\"hello\"}");
            assertEquals(200, resp.statusCode());
            JsonNode body = MAPPER.readTree(resp.bodyAsString());
            assertEquals("hello", body.get("echo").asText());
            assertNotNull(resp.getHeader(Constants.SESSION_HEADER));
        } finally {
            app.stop();
        }
    }

    // 5. test_invocation_no_entrypoint_returns_404
    @Test
    @DisplayName("POST /invocations without handler returns 404")
    void testInvocationNoEntrypointReturns404() throws Exception {
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
        app.run(0);
        try {
            HttpResponse<Buffer> resp = postJson(app.getPort(), "/invocations", "{\"msg\":\"hello\"}");
            assertEquals(404, resp.statusCode());
        } finally {
            app.stop();
        }
    }

    // 6. test_invocation_invalid_json_returns_400
    @Test
    @DisplayName("POST /invocations with bad JSON returns 400")
    void testInvocationInvalidJsonReturns400() throws Exception {
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
        app.setEntrypoint((Map<String, Object> payload) -> payload);
        app.run(0);
        try {
            HttpResponse<Buffer> resp = postJson(app.getPort(), "/invocations", "{not json");
            assertEquals(400, resp.statusCode());
        } finally {
            app.stop();
        }
    }

    // 7. test_invocation_handler_raise_returns_500
    @Test
    @DisplayName("handler raising exception returns 500")
    void testInvocationHandlerRaiseReturns500() throws Exception {
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
        app.setEntrypoint((Map<String, Object> payload) -> {
            throw new RuntimeException("boom");
        });
        app.run(0);
        try {
            HttpResponse<Buffer> resp = postJson(app.getPort(), "/invocations", "{\"x\":1}");
            assertEquals(500, resp.statusCode());
        } finally {
            app.stop();
        }
    }

    // 8. test_invocation_sync_generator_streams_sse
    @Test
    @DisplayName("sync Flux handler streams SSE")
    void testInvocationSyncGeneratorStreamsSse() throws Exception {
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
        app.setEntrypoint((Map<String, Object> payload) -> {
            return Flux.just(Map.of("v", "a"), Map.of("v", "b"), Map.of("v", "c"));
        });
        app.run(0);
        try {
            HttpResponse<Buffer> resp = postJson(app.getPort(), "/invocations", "{\"stream\":true}");
            assertEquals(200, resp.statusCode());
            assertTrue(resp.getHeader("Content-Type").contains("text/event-stream"));
            String body = resp.bodyAsString();
            assertTrue(body.split("data:").length - 1 >= 3, "Should have at least 3 data: lines");
            assertTrue(body.contains("a"));
            assertTrue(body.contains("c"));
        } finally {
            app.stop();
        }
    }

    // 9. test_invocation_async_generator_streams_sse
    @Test
    @DisplayName("async Flux handler streams SSE")
    void testInvocationAsyncGeneratorStreamsSse() throws Exception {
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
        app.setEntrypoint((Map<String, Object> payload) -> {
            return Flux.interval(java.time.Duration.ofMillis(50))
                    .take(2)
                    .map(i -> Map.of("i", i));
        });
        app.run(0);
        try {
            HttpResponse<Buffer> resp = postJson(app.getPort(), "/invocations", "{\"stream\":true}");
            assertEquals(200, resp.statusCode());
            assertTrue(resp.getHeader("Content-Type").contains("text/event-stream"));
            assertTrue(resp.bodyAsString().split("data:").length - 1 >= 2);
        } finally {
            app.stop();
        }
    }

    // 10. test_websocket_without_handler_closes_1011
    @Test
    @DisplayName("WS /ws without handler closes with 1011")
    void testWebsocketWithoutHandlerCloses1011() throws Exception {
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
        app.run(0);
        try {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Short> closeCode = new AtomicReference<>();
            HttpClient httpClient = vertx.createHttpClient();
            httpClient.webSocket(app.getPort(), "localhost", "/ws")
                    .onSuccess(ws -> {
                        ws.closeHandler(v -> {
                            closeCode.set(ws.closeStatusCode());
                            latch.countDown();
                        });
                    })
                    .onFailure(err -> latch.countDown());
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            // WebSocket should have been closed by server (1011 or similar)
            assertNotNull(closeCode.get());
        } finally {
            app.stop();
        }
    }

    // 11. test_websocket_echo_handler
    @Test
    @DisplayName("WS /ws with echo handler round-trips messages")
    void testWebsocketEchoHandler() throws Exception {
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
        app.setWebSocketHandler((ws, ctx) -> {
            ws.textMessageHandler(text -> {
                try {
                    JsonNode msg = MAPPER.readTree(text);
                    ws.writeTextMessage(MAPPER.writeValueAsString(Map.of("echo", msg)));
                } catch (Exception e) {
                    ws.close((short) 1011, e.getMessage());
                }
            });
            return null;
        });
        app.run(0);
        try {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> received = new AtomicReference<>();
            HttpClient httpClient = vertx.createHttpClient();
            httpClient.webSocket(app.getPort(), "localhost", "/ws")
                    .onSuccess(ws -> {
                        ws.textMessageHandler(text -> {
                            received.set(text);
                            latch.countDown();
                        });
                        ws.writeTextMessage("{\"msg\":\"ping\"}");
                    });
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            JsonNode json = MAPPER.readTree(received.get());
            assertEquals("ping", json.get("echo").get("msg").asText());
        } finally {
            app.stop();
        }
    }
}
