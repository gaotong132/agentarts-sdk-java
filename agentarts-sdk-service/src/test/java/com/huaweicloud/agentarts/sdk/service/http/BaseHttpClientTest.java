package com.huaweicloud.agentarts.sdk.service.http;

import com.sun.net.httpserver.HttpServer;
import com.huaweicloud.agentarts.sdk.core.SignMode;
import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredentialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BaseHttpClient}, {@link RequestConfig}, and {@link RequestResult}.
 */
class BaseHttpClientTest {

    // ========================
    // RequestConfig tests
    // ========================

    @Nested
    @DisplayName("RequestConfig")
    class RequestConfigTests {

        @Test
        void shouldHaveDefaults() {
            RequestConfig config = new RequestConfig();
            assertEquals("", config.getBaseUrl());
            assertEquals(30.0, config.getTimeoutSeconds());
            assertTrue(config.isVerifySsl());
        }

        @Test
        void shouldBuildWithValues() {
            RequestConfig config = RequestConfig.builder()
                    .baseUrl("https://example.com")
                    .timeoutSeconds(60.0)
                    .verifySsl(false)
                    .build();
            assertEquals("https://example.com", config.getBaseUrl());
            assertEquals(60.0, config.getTimeoutSeconds());
            assertFalse(config.isVerifySsl());
        }
    }

    // ========================
    // RequestResult tests
    // ========================

    @Nested
    @DisplayName("RequestResult")
    class RequestResultTests {

        @Test
        void shouldBuildSuccessResult() {
            RequestResult result = RequestResult.builder()
                    .success(true)
                    .statusCode(200)
                    .data("response body")
                    .headers(Map.of("Content-Type", "application/json"))
                    .build();

            assertTrue(result.isSuccess());
            assertEquals(200, result.getStatusCode());
            assertEquals("response body", result.getDataAsString());
            assertFalse(result.isStreaming());
            assertEquals("application/json", result.getHeaders().get("Content-Type"));
        }

        @Test
        void shouldBuildErrorResult() {
            RequestResult result = RequestResult.builder()
                    .success(false)
                    .statusCode(500)
                    .error("Internal Server Error")
                    .build();

            assertFalse(result.isSuccess());
            assertEquals(500, result.getStatusCode());
            assertEquals("Internal Server Error", result.getError());
        }

        @Test
        void shouldBuildStreamingResult() {
            Flux<String> lines = Flux.just("data: {\"msg\":\"hello\"}", "", "data: {\"msg\":\"world\"}");
            Flux<byte[]> bytes = Flux.just("test".getBytes());

            RequestResult result = RequestResult.builder()
                    .success(true)
                    .statusCode(200)
                    .streaming(true)
                    .lineStream(lines)
                    .byteStream(bytes)
                    .headers(Map.of("Content-Type", "text/event-stream"))
                    .build();

            assertTrue(result.isStreaming());
            assertNull(result.getData());

            // Verify iterLines works
            var lineList = result.iterLines().collectList().block();
            assertNotNull(lineList);
            assertEquals(3, lineList.size());
        }

        @Test
        void shouldThrowOnIterLinesWhenNotStreaming() {
            RequestResult result = RequestResult.builder()
                    .success(true)
                    .statusCode(200)
                    .data("not streaming")
                    .build();

            assertThrows(IllegalStateException.class, result::iterLines);
            assertThrows(IllegalStateException.class, result::iterBytes);
        }

        @Test
        void shouldCloseStreamingResultExactlyOnce() {
            AtomicInteger closes = new AtomicInteger();
            RequestResult result = RequestResult.builder()
                    .success(true)
                    .statusCode(200)
                    .streaming(true)
                    .lineStream(Flux.never())
                    .byteStream(Flux.never())
                    .closeAction(closes::incrementAndGet)
                    .build();

            result.close();
            result.close();

            assertEquals(1, closes.get());
        }
    }

    // ========================
    // BaseHttpClient construction tests
    // ========================

    @Nested
    @DisplayName("BaseHttpClient")
    class ClientTests {

        @Test
        void shouldCreateWithDefaultSignMode() {
            RequestConfig config = RequestConfig.builder()
                    .baseUrl("https://example.com")
                    .build();

            try (BaseHttpClient client = new BaseHttpClient(config)) {
                assertEquals(SignMode.SDK_HMAC_SHA256, client.getSignMode());
            }
        }

        @Test
        void shouldCreateWithV11SignMode() {
            RequestConfig config = RequestConfig.builder()
                    .baseUrl("https://agentarts.cn-southwest-2.myhuaweicloud.com")
                    .build();

            try (BaseHttpClient client = new BaseHttpClient(config, true, SignMode.V11_HMAC_SHA256, "cn-southwest-2")) {
                assertEquals(SignMode.V11_HMAC_SHA256, client.getSignMode());
            }
        }

        @Test
        void signingFailsClosedWhenCredentialsAreMissing() {
            RequestConfig config = RequestConfig.builder()
                    .baseUrl("http://127.0.0.1:1")
                    .timeoutSeconds(1)
                    .build();

            ICredentialProvider provider = () -> new BasicCredentials().withAk("").withSk("");
            try (BaseHttpClient client = new BaseHttpClient(
                    config, true, SignMode.SDK_HMAC_SHA256, "cn-southwest-2", provider)) {
                RequestResult result = client.get("/must-not-be-sent").block();
                assertNotNull(result);
                assertFalse(result.isSuccess());
                assertEquals(0, result.getStatusCode());
                assertTrue(result.getError().contains("Failed to sign request"));
            }
        }

        @Test
        void resolvesOneCredentialSnapshotForEverySignedRequest() throws Exception {
            AtomicInteger loads = new AtomicInteger();
            AtomicReference<String> authorization = new AtomicReference<>();
            AtomicReference<String> securityToken = new AtomicReference<>();
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/signed", exchange -> {
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                securityToken.set(exchange.getRequestHeaders().getFirst("X-Security-Token"));
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            });
            server.start();

            ICredentialProvider provider = () -> {
                int sequence = loads.incrementAndGet();
                return new BasicCredentials()
                        .withAk("rotating-ak-" + sequence)
                        .withSk("rotating-sk-" + sequence)
                        .withSecurityToken("rotating-token-" + sequence);
            };
            RequestConfig config = RequestConfig.builder()
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                    .timeoutSeconds(5)
                    .build();
            try (BaseHttpClient client = new BaseHttpClient(
                    config, true, SignMode.SDK_HMAC_SHA256, "cn-southwest-2", provider)) {
                assertTrue(client.get("/signed").block(Duration.ofSeconds(5)).isSuccess());
                assertEquals(1, loads.get());
                assertNotNull(authorization.get());
                assertTrue(authorization.get().contains("rotating-ak-1"));
                assertEquals("rotating-token-1", securityToken.get());

                assertTrue(client.get("/signed").block(Duration.ofSeconds(5)).isSuccess());
                assertEquals(2, loads.get());
                assertTrue(authorization.get().contains("rotating-ak-2"));
                assertEquals("rotating-token-2", securityToken.get());
            } finally {
                server.stop(0);
            }
        }

        @Test
        void preservesBinaryResponsesWithoutUtf8Conversion() throws Exception {
            byte[] payload = new byte[] {0, 1, 2, (byte) 0x80, (byte) 0xff, 10, 13};
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/binary", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                exchange.sendResponseHeaders(200, payload.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(payload);
                }
            });
            server.start();

            RequestConfig config = RequestConfig.builder()
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                    .timeoutSeconds(5)
                    .build();
            try (BaseHttpClient client = new BaseHttpClient(config)) {
                RequestResult result = client.get("/binary").block(Duration.ofSeconds(5));
                assertNotNull(result);
                assertTrue(result.isSuccess());
                assertArrayEquals(payload, result.getDataAsBytes());
                assertNull(result.getDataAsString());

                byte[] callerCopy = result.getDataAsBytes();
                callerCopy[0] = 99;
                assertArrayEquals(payload, result.getDataAsBytes(),
                        "binary response data must be defensively copied");
            } finally {
                server.stop(0);
            }
        }

        @Test
        void rejectsBlankAuthTokensAndHeaderNames() {
            RequestConfig config = RequestConfig.builder()
                    .baseUrl("https://example.com")
                    .build();
            try (BaseHttpClient client = new BaseHttpClient(config)) {
                assertThrows(IllegalArgumentException.class, () -> client.setAuthToken("Bearer", " "));
                assertThrows(IllegalArgumentException.class, () -> client.setAuthToken(" ", "token"));
                assertThrows(IllegalArgumentException.class, () -> client.setHeader(" ", "value"));
                assertThrows(NullPointerException.class, () -> client.setHeader("X-Test", null));
            }
        }

        @Test
        void ignoresTrailingEmptyQuerySegmentForSigning() {
            Map<String, List<String>> query = BaseHttpClient.parseQueryParams(
                    URI.create("https://example.com/gateways?limit=1&"));

            assertEquals(Map.of("limit", List.of("1")), query);
        }
    }

    @Nested
    @DisplayName("Streaming transport")
    class StreamingTransportTests {

        @Test
        void explicitlyStreamsBinaryResponsesWithoutBuffering() throws Exception {
            CountDownLatch releaseBody = new CountDownLatch(1);
            byte[] payload = new byte[] {0, 1, (byte) 0x80, (byte) 0xff};
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/binary-stream", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                exchange.sendResponseHeaders(200, 0);
                try (OutputStream output = exchange.getResponseBody()) {
                    if (!releaseBody.await(15, TimeUnit.SECONDS)) return;
                    output.write(payload);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            server.start();

            RequestConfig config = RequestConfig.builder()
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                    .timeoutSeconds(10)
                    .build();
            try (BaseHttpClient client = new BaseHttpClient(config)) {
                RequestResult result = client.requestBinaryStream(
                        "GET", "/binary-stream", null, null, null, 10.0)
                        .block(Duration.ofSeconds(5));
                assertNotNull(result);
                assertTrue(result.isStreaming(), "binary response must return at headers");

                releaseBody.countDown();
                byte[] downloaded = result.iterBytes()
                        .reduce(new ByteArrayOutputStream(), (output, chunk) -> {
                            output.writeBytes(chunk);
                            return output;
                        })
                        .map(ByteArrayOutputStream::toByteArray)
                        .block(Duration.ofSeconds(5));
                assertArrayEquals(payload, downloaded);
            } finally {
                releaseBody.countDown();
                server.stop(0);
            }
        }

        @Test
        void returnsAtHeadersAndDecodesUtf8LinesAcrossChunks() throws Exception {
            CountDownLatch releaseBody = new CountDownLatch(1);
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/stream", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
                exchange.sendResponseHeaders(200, 0);
                try (OutputStream output = exchange.getResponseBody()) {
                    byte[] payload = "data: 你\r\n\r\n{\"x\":1}\nlast".getBytes(StandardCharsets.UTF_8);
                    int splitInsideUtf8 = "data: ".getBytes(StandardCharsets.UTF_8).length + 1;
                    output.write(payload, 0, splitInsideUtf8);
                    output.flush();
                    if (!releaseBody.await(15, TimeUnit.SECONDS)) {
                        return;
                    }
                    output.write(payload, splitInsideUtf8, payload.length - splitInsideUtf8);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            server.start();

            RequestConfig config = RequestConfig.builder()
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                    .timeoutSeconds(10)
                    .build();
            try (BaseHttpClient client = new BaseHttpClient(config)) {
                // The server has sent headers but intentionally withholds the body.
                // A buffering client times out here; a true streaming client returns.
                RequestResult result = client.get("/stream").block(Duration.ofSeconds(5));
                assertNotNull(result);
                assertTrue(result.isStreaming());

                releaseBody.countDown();
                assertEquals(List.of("data: 你", "", "{\"x\":1}", "last"),
                        result.iterLines().collectList().block(Duration.ofSeconds(5)));

                IllegalStateException secondConsumer = assertThrows(
                        IllegalStateException.class,
                        () -> result.iterBytes().collectList().block(Duration.ofSeconds(2)));
                assertTrue(secondConsumer.getMessage().contains("only be consumed once"));
            } finally {
                releaseBody.countDown();
                server.stop(0);
            }
        }
    }

    // ========================
    // SignMode tests
    // ========================

    @Nested
    @DisplayName("SignMode")
    class SignModeTests {

        @Test
        void shouldHaveTwoModes() {
            assertEquals(2, SignMode.values().length);
            assertNotNull(SignMode.SDK_HMAC_SHA256);
            assertNotNull(SignMode.V11_HMAC_SHA256);
        }

        @Test
        void shouldHaveCorrectValues() {
            assertEquals("sdk", SignMode.SDK_HMAC_SHA256.getValue());
            assertEquals("v11", SignMode.V11_HMAC_SHA256.getValue());
        }
    }
}
