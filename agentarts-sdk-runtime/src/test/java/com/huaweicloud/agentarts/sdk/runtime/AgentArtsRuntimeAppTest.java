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
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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

    @Test
    @DisplayName("runtime binds the requested host and validates lifecycle inputs")
    void bindsRequestedHostAndValidatesLifecycle() throws Exception {
        assertThrows(IllegalStateException.class, () -> app.run(0, "127.0.0.1"));

        app.stop();
        AgentArtsRuntimeApp loopbackApp = new AgentArtsRuntimeApp(15, vertx);
        app = loopbackApp;
        assertThrows(IllegalArgumentException.class, () -> loopbackApp.run(-1, "localhost"));
        assertThrows(IllegalArgumentException.class, () -> loopbackApp.run(65_536, "localhost"));
        assertThrows(IllegalArgumentException.class, () -> loopbackApp.run(0, " "));

        loopbackApp.run(0, "127.0.0.1");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> statusCode = new AtomicReference<>();
        webClient.get(loopbackApp.getPort(), "127.0.0.1", "/ping")
                .send()
                .onComplete(result -> {
                    if (result.succeeded()) {
                        statusCode.set(result.result().statusCode());
                    }
                    latch.countDown();
                });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(200, statusCode.get());

        loopbackApp.stop();
        loopbackApp.stop();
        assertThrows(IllegalStateException.class, () -> loopbackApp.run(0, "localhost"));
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
    @DisplayName("POST /invocations rejects request bodies above the configured limit")
    void invocationRejectsOversizedBodyBeforeCallingHandler() throws Exception {
        app.stop();
        app = new AgentArtsRuntimeApp(15, vertx);
        app.setMaxRequestBodyBytes(4);
        AtomicReference<Boolean> invoked = new AtomicReference<>(false);
        app.setEntrypoint((payload, ctx) -> {
            invoked.set(true);
            return Map.of("ok", true);
        });
        app.run(0);
        port = app.getPort();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> statusCode = new AtomicReference<>();
        webClient.post(port, "localhost", "/invocations")
                .putHeader("Content-Type", "application/json")
                .sendBuffer(Buffer.buffer("{\"message\":\"too large\"}"))
                .onComplete(ar -> {
                    assertTrue(ar.succeeded());
                    statusCode.set(ar.result().statusCode());
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(413, statusCode.get());
        assertFalse(invoked.get());
        assertThrows(IllegalStateException.class, () -> app.setMaxRequestBodyBytes(8));
    }

    @Test
    void requestBodyLimitMustBePositive() {
        assertEquals(AgentArtsRuntimeApp.DEFAULT_MAX_REQUEST_BODY_BYTES,
                app.getMaxRequestBodyBytes());
        assertThrows(IllegalArgumentException.class, () -> app.setMaxRequestBodyBytes(0));
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

    @Test
    @DisplayName("SSE requests one item at a time from an off-event-loop publisher")
    void sseAppliesBackpressureAcrossSchedulerBoundary() throws Exception {
        AtomicLong largestRequest = new AtomicLong();
        AtomicLong requestSignals = new AtomicLong();
        app.setEntrypoint((payload, ctx) -> Flux.range(1, 20)
                .publishOn(Schedulers.parallel(), 1)
                .doOnRequest(requested -> {
                    largestRequest.accumulateAndGet(requested, Math::max);
                    requestSignals.incrementAndGet();
                })
                .map(value -> Map.of("chunk", value)));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> responseBody = new AtomicReference<>();
        webClient.post(port, "localhost", "/invocations")
                .putHeader("Content-Type", "application/json")
                .sendBuffer(Buffer.buffer("{}"))
                .onComplete(ar -> {
                    responseBody.set(ar.result().bodyAsString());
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(responseBody.get().contains("\"chunk\":20"));
        assertTrue(requestSignals.get() >= 20);
        assertEquals(1, largestRequest.get(),
                "Runtime must not request the complete Flux before socket writes drain");
    }

    @Test
    @DisplayName("POST /invocations resolves Mono responses")
    void invocationReturnsMonoResponse() throws Exception {
        app.setEntrypoint((payload, ctx) -> Mono.just(Map.of("async", true)));
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> body = new AtomicReference<>();

        webClient.post(port, "localhost", "/invocations")
                .putHeader("Content-Type", "application/json")
                .sendBuffer(Buffer.buffer("{}"))
                .onComplete(ar -> {
                    body.set(ar.result().bodyAsString());
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(body.get().contains("\"async\":true"));
    }

    @Test
    @DisplayName("streaming invocation holds concurrency permit until completion")
    void streamingInvocationHoldsConcurrencyPermit() throws Exception {
        app.stop();
        app = new AgentArtsRuntimeApp(1, vertx);
        app.run(0);
        port = app.getPort();

        Sinks.Many<Map<String, Object>> sink = Sinks.many().unicast().onBackpressureBuffer();
        CountDownLatch subscribed = new CountDownLatch(1);
        app.setEntrypoint((payload, ctx) -> sink.asFlux().doOnSubscribe(ignored -> subscribed.countDown()));

        CountDownLatch firstDone = new CountDownLatch(1);
        webClient.post(port, "localhost", "/invocations")
                .putHeader("Content-Type", "application/json")
                .sendBuffer(Buffer.buffer("{}"))
                .onComplete(ar -> firstDone.countDown());
        assertTrue(subscribed.await(5, TimeUnit.SECONDS));

        CountDownLatch secondDone = new CountDownLatch(1);
        AtomicReference<Integer> secondStatus = new AtomicReference<>();
        webClient.post(port, "localhost", "/invocations")
                .putHeader("Content-Type", "application/json")
                .sendBuffer(Buffer.buffer("{}"))
                .onComplete(ar -> {
                    secondStatus.set(ar.result().statusCode());
                    secondDone.countDown();
                });
        assertTrue(secondDone.await(5, TimeUnit.SECONDS));
        assertEquals(503, secondStatus.get());

        sink.tryEmitComplete();
        assertTrue(firstDone.await(5, TimeUnit.SECONDS));
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

    @Test
    @DisplayName("AgentArtsRuntimeContext propagates across a Reactor scheduler and is cleared")
    void contextPropagatesAcrossReactorScheduler() throws Exception {
        Scheduler scheduler = Schedulers.newSingle("agentarts-context-test");
        try {
            app.setEntrypoint((payload, ctx) -> Mono.just(1)
                    .publishOn(scheduler)
                    .map(ignored -> Map.of(
                            "session", AgentArtsRuntimeContext.getSessionId(),
                            "request", AgentArtsRuntimeContext.getRequestId(),
                            "user", AgentArtsRuntimeContext.getUserId(),
                            "token", AgentArtsRuntimeContext.getWorkloadAccessToken())));

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> responseBody = new AtomicReference<>();
            webClient.post(port, "localhost", "/invocations")
                    .putHeader("Content-Type", "application/json")
                    .putHeader("x-hw-agentarts-session-id", "reactive-session")
                    .putHeader("X-Request-Id", "reactive-request")
                    .putHeader("X-HW-AgentGateway-User-Id", "reactive-user")
                    .putHeader("X-HW-AgentGateway-Workload-Access-Token", "reactive-token")
                    .sendBuffer(Buffer.buffer("{}"))
                    .onComplete(ar -> {
                        responseBody.set(ar.result().bodyAsString());
                        latch.countDown();
                    });

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            String body = responseBody.get();
            assertTrue(body.contains("\"session\":\"reactive-session\""));
            assertTrue(body.contains("\"request\":\"reactive-request\""));
            assertTrue(body.contains("\"user\":\"reactive-user\""));
            assertTrue(body.contains("\"token\":\"reactive-token\""));

            String leaked = Mono.fromCallable(AgentArtsRuntimeContext::getSessionId)
                    .subscribeOn(scheduler)
                    .block(Duration.ofSeconds(2));
            assertNull(leaked, "request context must not leak on a reused scheduler thread");
        } finally {
            scheduler.dispose();
        }
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
        String id = ctx.getRequestId();
        assertNotNull(id);
        assertFalse(id.isEmpty());
        // Must be the generated 32-hex-char UUID form (no dashes), not just any non-empty
        // string — a constant or leaked field would satisfy non-empty alone.
        assertTrue(id.matches("[0-9a-f]{32}"),
                "generated request id should be a 32-char hex UUID, got: " + id);
        // Two generations must differ (non-constant).
        String id2 = RequestContext.fromHeaders(name -> null).getRequestId();
        assertNotEquals(id, id2, "generated request ids should be unique");
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
