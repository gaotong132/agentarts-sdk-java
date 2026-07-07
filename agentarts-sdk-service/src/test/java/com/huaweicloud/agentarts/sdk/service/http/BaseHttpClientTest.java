package com.huaweicloud.agentarts.sdk.service.http;

import com.huaweicloud.agentarts.sdk.core.SignMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.Map;

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
