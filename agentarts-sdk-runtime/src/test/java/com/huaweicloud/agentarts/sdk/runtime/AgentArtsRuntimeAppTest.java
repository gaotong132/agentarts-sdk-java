package com.huaweicloud.agentarts.sdk.runtime;

import com.huaweicloud.agentarts.sdk.core.PingStatus;
import com.huaweicloud.agentarts.sdk.runtime.context.AgentArtsRuntimeContext;
import com.huaweicloud.agentarts.sdk.runtime.context.RequestContext;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.junit.jupiter.api.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link AgentArtsRuntimeApp}.
 */
class AgentArtsRuntimeAppTest {

    private Vertx vertx;
    private WebClient webClient;
    private AgentArtsRuntimeApp app;
    private int port;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        webClient = WebClient.create(vertx, new WebClientOptions());
        app = new AgentArtsRuntimeApp(15, vertx);
        // Use port 0 for dynamic port allocation
        app.run(0);
        port = app.getPort();
    }

    @AfterEach
    void tearDown() {
        app.stop();
    }

    // ========================
    // GET /ping
    // ========================

    @Test
    @DisplayName("GET /ping returns Healthy by default")
    void pingReturnsHealthy() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> responseBody = new AtomicReference<>();
        AtomicReference<Integer> statusCode = new AtomicReference<>();

        webClient.get(port, "localhost", "/ping")
                .send()
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        statusCode.set(ar.result().statusCode());
                        responseBody.set(ar.result().bodyAsString());
                    }
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(200, statusCode.get());
        assertTrue(responseBody.get().contains("\"status\":\"Healthy\""));
    }

    @Test
    @DisplayName("GET /ping with custom handler")
    void pingWithCustomHandler() throws Exception {
        app.setPingHandler(() -> PingStatus.UNHEALTHY);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> responseBody = new AtomicReference<>();

        webClient.get(port, "localhost", "/ping")
                .send()
                .onComplete(ar -> {
                    responseBody.set(ar.result().bodyAsString());
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(responseBody.get().contains("\"status\":\"Unhealthy\""));
    }

    // ========================
    // POST /invocations
    // ========================

    @Test
    @DisplayName("POST /invocations returns 200 with JSON")
    void invocationReturnsJson() throws Exception {
        app.setEntrypoint((payload, ctx) ->
                Map.of("echo", payload.getOrDefault("message", "none")));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> responseBody = new AtomicReference<>();
        AtomicReference<Integer> statusCode = new AtomicReference<>();

        webClient.post(port, "localhost", "/invocations")
                .putHeader("Content-Type", "application/json")
                .sendBuffer(Buffer.buffer("{\"message\":\"hello\"}"))
                .onComplete(ar -> {
                    statusCode.set(ar.result().statusCode());
                    responseBody.set(ar.result().bodyAsString());
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(200, statusCode.get());
        assertTrue(responseBody.get().contains("\"echo\":\"hello\""));
    }

    @Test
    @DisplayName("POST /invocations extracts session ID from header")
    void invocationExtractsSessionId() throws Exception {
        AtomicReference<String> capturedSessionId = new AtomicReference<>();

        app.setEntrypoint((payload, ctx) -> {
            capturedSessionId.set(ctx.getSessionId());
            return Map.of("ok", true);
        });

        CountDownLatch latch = new CountDownLatch(1);

        webClient.post(port, "localhost", "/invocations")
                .putHeader("Content-Type", "application/json")
                .putHeader("x-hw-agentarts-session-id", "test-session-123")
                .sendBuffer(Buffer.buffer("{}"))
                .onComplete(ar -> latch.countDown());

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("test-session-123", capturedSessionId.get());
    }

    @Test
    @DisplayName("POST /invocations returns session header in response")
    void invocationReturnsSessionHeader() throws Exception {
        app.setEntrypoint((payload, ctx) -> Map.of("ok", true));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> sessionHeader = new AtomicReference<>();

        webClient.post(port, "localhost", "/invocations")
                .putHeader("Content-Type", "application/json")
                .putHeader("x-hw-agentarts-session-id", "session-456")
                .sendBuffer(Buffer.buffer("{}"))
                .onComplete(ar -> {
                    sessionHeader.set(ar.result().getHeader("x-hw-agentarts-session-id"));
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("session-456", sessionHeader.get());
    }

    @Test
    @DisplayName("POST /invocations returns 400 for invalid JSON")
    void invocationReturns400ForInvalidJson() throws Exception {
        app.setEntrypoint((payload, ctx) -> Map.of("ok", true));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> statusCode = new AtomicReference<>();

        webClient.post(port, "localhost", "/invocations")
                .putHeader("Content-Type", "application/json")
                .sendBuffer(Buffer.buffer("{invalid json}"))
                .onComplete(ar -> {
                    statusCode.set(ar.result().statusCode());
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(400, statusCode.get());
    }

    @Test
    @DisplayName("POST /invocations returns 500 on handler exception")
    void invocationReturns500OnError() throws Exception {
        app.setEntrypoint((BiFunction<Map<String, Object>, RequestContext, Object>) (payload, ctx) -> {
            throw new RuntimeException("Intentional error");
        });

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> statusCode = new AtomicReference<>();
        AtomicReference<String> responseBody = new AtomicReference<>();

        webClient.post(port, "localhost", "/invocations")
                .putHeader("Content-Type", "application/json")
                .sendBuffer(Buffer.buffer("{}"))
                .onComplete(ar -> {
                    statusCode.set(ar.result().statusCode());
                    responseBody.set(ar.result().bodyAsString());
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(500, statusCode.get());
        assertTrue(responseBody.get().contains("RuntimeException"));
    }

    @Test
    @DisplayName("POST /invocations returns 404 when no handler registered")
    void invocationReturns404WithoutHandler() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> statusCode = new AtomicReference<>();

        webClient.post(port, "localhost", "/invocations")
                .putHeader("Content-Type", "application/json")
                .sendBuffer(Buffer.buffer("{}"))
                .onComplete(ar -> {
                    statusCode.set(ar.result().statusCode());
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(404, statusCode.get());
    }

    // ========================
    // SSE Streaming
    // ========================

    @Test
    @DisplayName("POST /invocations returns SSE stream from Flux")
    void invocationReturnsSseStream() throws Exception {
        app.setEntrypoint((payload, ctx) ->
                Flux.just(
                        Map.of("chunk", 1),
                        Map.of("chunk", 2),
                        Map.of("chunk", 3)
                )
        );

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> responseBody = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();

        webClient.post(port, "localhost", "/invocations")
                .putHeader("Content-Type", "application/json")
                .sendBuffer(Buffer.buffer("{}"))
                .onComplete(ar -> {
                    contentType.set(ar.result().getHeader("Content-Type"));
                    responseBody.set(ar.result().bodyAsString());
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(contentType.get().contains("text/event-stream"));
        String body = responseBody.get();
        assertTrue(body.contains("data: "));
        assertTrue(body.contains("\"chunk\":1"));
        assertTrue(body.contains("\"chunk\":2"));
        assertTrue(body.contains("\"chunk\":3"));
    }

    // ========================
    // Context management
    // ========================

    @Test
    @DisplayName("AgentArtsRuntimeContext is cleared after request")
    void contextIsClearedAfterRequest() throws Exception {
        app.setEntrypoint((payload, ctx) -> Map.of("ok", true));

        CountDownLatch latch = new CountDownLatch(1);

        webClient.post(port, "localhost", "/invocations")
                .putHeader("Content-Type", "application/json")
                .putHeader("x-hw-agentarts-session-id", "to-be-cleared")
                .sendBuffer(Buffer.buffer("{}"))
                .onComplete(ar -> latch.countDown());

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // After the request, the context should be cleared
        assertNull(AgentArtsRuntimeContext.getSessionId());
        assertNull(AgentArtsRuntimeContext.getRequestId());
    }

    // ========================
    // Request context extraction
    // ========================

    @Test
    @DisplayName("RequestContext.fromHeaders extracts all headers")
    void requestContextFromHeaders() {
        Map<String, String> headers = Map.of(
                "x-hw-agentarts-session-id", "sess-abc",
                "X-HW-AgentGateway-Workload-Access-Token", "token-xyz",
                "X-HW-AgentGateway-User-Id", "user-123",
                "X-Request-Id", "req-456"
        );

        RequestContext ctx = RequestContext.fromHeaders(headers::get);

        assertEquals("sess-abc", ctx.getSessionId());
        assertEquals("token-xyz", ctx.getWorkloadAccessToken());
        assertEquals("user-123", ctx.getUserId());
        assertEquals("req-456", ctx.getRequestId());
    }

    @Test
    @DisplayName("RequestContext generates UUID when X-Request-Id missing")
    void requestContextGeneratesRequestId() {
        RequestContext ctx = RequestContext.fromHeaders(name -> null);
        assertNotNull(ctx.getRequestId());
        assertFalse(ctx.getRequestId().isEmpty());
    }

    // ========================
    // Empty body → 400
    // ========================

    @Test
    @DisplayName("POST /invocations returns 400 for empty body")
    void invocationReturns400ForEmptyBody() throws Exception {
        app.setEntrypoint((payload, ctx) -> Map.of("ok", true));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> statusCode = new AtomicReference<>();

        webClient.post(port, "localhost", "/invocations")
                .putHeader("Content-Type", "application/json")
                .sendBuffer(Buffer.buffer(""))
                .onComplete(ar -> {
                    statusCode.set(ar.result().statusCode());
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(400, statusCode.get());
    }

    // ========================
    // Ping always includes session header
    // ========================

    @Test
    @DisplayName("GET /ping always includes session header")
    void pingAlwaysIncludesSessionHeader() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> sessionHeader = new AtomicReference<>();

        webClient.get(port, "localhost", "/ping")
                .send()
                .onComplete(ar -> {
                    sessionHeader.set(ar.result().getHeader("x-hw-agentarts-session-id"));
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        // Match Python: session header is always present (even as empty string)
        assertNotNull(sessionHeader.get());
    }

    // ========================
    // Async task tracking
    // ========================

    @Test
    @DisplayName("hasRunningTasks tracks active tasks")
    void hasRunningTasksTracksTasks() {
        assertFalse(app.hasRunningTasks());

        long taskId = app.addTask("test-task");
        assertTrue(app.hasRunningTasks());

        app.completeTask(taskId);
        assertFalse(app.hasRunningTasks());
    }

    @Test
    @DisplayName("Ping returns HealthyBusy when tasks are running")
    void pingReturnsHealthyBusy() throws Exception {
        long taskId = app.addTask("background-work");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> responseBody = new AtomicReference<>();

        webClient.get(port, "localhost", "/ping")
                .send()
                .onComplete(ar -> {
                    responseBody.set(ar.result().bodyAsString());
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(responseBody.get().contains("\"status\":\"HealthyBusy\""));

        app.completeTask(taskId);
    }
}
