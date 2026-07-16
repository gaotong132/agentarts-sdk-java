package com.huaweicloud.agentarts.sdk.service.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link RequestConfig}, {@link RequestResult}.
 */
class HttpModelTest {

    // ============================================================
    // RequestConfig
    // ============================================================

    @Nested
    class RequestConfigTests {

        @Test
        void defaultValues() {
            RequestConfig config = new RequestConfig();
            assertEquals("", config.getBaseUrl());
            assertEquals(30.0, config.getTimeoutSeconds());
            assertTrue(config.isVerifySsl());
        }

        @Test
        void threeArgConstructor() {
            RequestConfig config = new RequestConfig("https://example.com", 60.0, false);
            assertEquals("https://example.com", config.getBaseUrl());
            assertEquals(60.0, config.getTimeoutSeconds());
            assertFalse(config.isVerifySsl());
        }

        @Test
        void constructorNullBaseUrl() {
            RequestConfig config = new RequestConfig(null, 30.0, true);
            assertEquals("", config.getBaseUrl());
        }

        @Test
        void settersAndGetters() {
            RequestConfig config = new RequestConfig();
            config.setBaseUrl("https://api.example.com");
            config.setTimeoutSeconds(120.0);
            config.setVerifySsl(false);

            assertEquals("https://api.example.com", config.getBaseUrl());
            assertEquals(120.0, config.getTimeoutSeconds());
            assertFalse(config.isVerifySsl());
        }

        @Test
        void setBaseUrlNullSafe() {
            RequestConfig config = new RequestConfig();
            config.setBaseUrl(null);
            assertEquals("", config.getBaseUrl());
        }

        @Test
        void builderDefaults() {
            RequestConfig config = RequestConfig.builder().build();
            assertEquals("", config.getBaseUrl());
            assertEquals(30.0, config.getTimeoutSeconds());
            assertTrue(config.isVerifySsl());
        }

        @Test
        void builderWithValues() {
            RequestConfig config = RequestConfig.builder()
                    .baseUrl("https://example.com")
                    .timeoutSeconds(45.0)
                    .verifySsl(false)
                    .build();
            assertEquals("https://example.com", config.getBaseUrl());
            assertEquals(45.0, config.getTimeoutSeconds());
            assertFalse(config.isVerifySsl());
        }

        @Test
        void builderChainingReturnsBuilder() {
            RequestConfig.Builder builder = RequestConfig.builder();
            assertSame(builder, builder.baseUrl("url"));
            assertSame(builder, builder.timeoutSeconds(1.0));
            assertSame(builder, builder.verifySsl(true));
        }

        @Test
        void timeoutMustBePositiveAndFinite() {
            RequestConfig config = new RequestConfig();
            assertThrows(IllegalArgumentException.class, () -> config.setTimeoutSeconds(0.0));
            assertThrows(IllegalArgumentException.class, () -> config.setTimeoutSeconds(-1.0));
            assertThrows(IllegalArgumentException.class, () -> config.setTimeoutSeconds(Double.NaN));
            assertThrows(IllegalArgumentException.class,
                    () -> config.setTimeoutSeconds(Double.POSITIVE_INFINITY));
        }

        @Test
        void timeoutFractional() {
            RequestConfig config = new RequestConfig();
            config.setTimeoutSeconds(0.5);
            assertEquals(0.5, config.getTimeoutSeconds());
        }
    }

    // ============================================================
    // RequestResult
    // ============================================================

    @Nested
    class RequestResultTests {

        @Test
        void successResult() {
            RequestResult result = RequestResult.builder()
                    .success(true)
                    .statusCode(200)
                    .data(JsonUtils.MAPPER.createObjectNode().put("key", "value"))
                    .headers(Map.of("Content-Type", "application/json"))
                    .build();

            assertTrue(result.isSuccess());
            assertEquals(200, result.getStatusCode());
            assertNotNull(result.getData());
            assertFalse(result.isStreaming());
        }

        @Test
        void errorResult() {
            RequestResult result = RequestResult.builder()
                    .success(false)
                    .statusCode(404)
                    .error("Not found")
                    .build();

            assertFalse(result.isSuccess());
            assertEquals(404, result.getStatusCode());
            assertEquals("Not found", result.getError());
        }

        @Test
        void nullDataReturnsNullForAccessors() {
            RequestResult result = RequestResult.builder()
                    .success(true)
                    .statusCode(200)
                    .build();

            assertNull(result.getData());
            assertNull(result.getDataAsJson());
            assertNull(result.getDataAsString());
        }

        @Test
        void jsonNodeDataAccessor() throws Exception {
            JsonNode node = JsonUtils.MAPPER.readTree("{\"key\":\"value\"}");
            RequestResult result = RequestResult.builder()
                    .success(true)
                    .statusCode(200)
                    .data(node)
                    .build();

            assertSame(node, result.getDataAsJson());
            assertNotNull(result.getDataAsString());
        }

        @Test
        void stringDataAccessor() {
            RequestResult result = RequestResult.builder()
                    .success(true)
                    .statusCode(200)
                    .data("raw text")
                    .build();

            assertNull(result.getDataAsJson());
            assertEquals("raw text", result.getDataAsString());
        }

        @Test
        void nonStringNonJsonDataAsString() {
            RequestResult result = RequestResult.builder()
                    .success(true)
                    .statusCode(200)
                    .data(42)
                    .build();

            assertNull(result.getDataAsJson());
            assertEquals("42", result.getDataAsString());
        }

        @Test
        void headersAreUnmodifiable() {
            RequestResult result = RequestResult.builder()
                    .success(true)
                    .statusCode(200)
                    .headers(Map.of("key", "value"))
                    .build();

            assertThrows(UnsupportedOperationException.class,
                    () -> result.getHeaders().put("new", "value"));
        }

        @Test
        void headersAreDefensivelyCopied() {
            Map<String, String> source = new HashMap<>();
            source.put("key", "value");
            RequestResult result = RequestResult.builder()
                    .success(true)
                    .statusCode(200)
                    .headers(source)
                    .build();

            source.put("key", "changed");
            assertEquals("value", result.getHeaders().get("key"));
        }

        @Test
        void nullHeadersBecomeEmptyMap() {
            RequestResult result = RequestResult.builder()
                    .success(true)
                    .statusCode(200)
                    .build();

            assertNotNull(result.getHeaders());
            assertTrue(result.getHeaders().isEmpty());
        }

        @Test
        void streamingResultThrowsOnIterLinesWhenNotStreaming() {
            RequestResult result = RequestResult.builder()
                    .success(true)
                    .statusCode(200)
                    .streaming(false)
                    .build();

            assertThrows(IllegalStateException.class, result::iterLines);
            assertThrows(IllegalStateException.class, result::iterBytes);
        }

        @Test
        void streamingResultWithFlux() {
            Flux<String> lines = Flux.just("line1", "line2", "line3");
            Flux<byte[]> bytes = Flux.just("data".getBytes());

            RequestResult result = RequestResult.builder()
                    .success(true)
                    .statusCode(200)
                    .streaming(true)
                    .lineStream(lines)
                    .byteStream(bytes)
                    .build();

            assertTrue(result.isStreaming());
            assertSame(lines, result.iterLines());
            assertSame(bytes, result.iterBytes());
        }

        @Test
        void streamingResultWithNullFluxThrows() {
            RequestResult result = RequestResult.builder()
                    .success(true)
                    .statusCode(200)
                    .streaming(true)
                    .build();

            // streaming=true but no flux set -> should throw
            assertThrows(IllegalStateException.class, result::iterLines);
            assertThrows(IllegalStateException.class, result::iterBytes);
        }

        // ============================================================
        // extractErrorMessage
        // ============================================================

        @Test
        void extractErrorFromErrorField() {
            RequestResult result = RequestResult.builder()
                    .success(false)
                    .statusCode(400)
                    .error("Bad request")
                    .build();
            assertEquals("Bad request", result.extractErrorMessage());
        }

        @Test
        void extractErrorFromJsonErrorField() throws Exception {
            JsonNode data = JsonUtils.MAPPER.readTree("{\"error\":\"Forbidden\"}");
            RequestResult result = RequestResult.builder()
                    .success(false)
                    .statusCode(403)
                    .data(data)
                    .build();
            assertEquals("Forbidden", result.extractErrorMessage());
        }

        @Test
        void extractErrorFromNestedErrorObject() throws Exception {
            JsonNode data = JsonUtils.MAPPER.readTree("{\"error\":{\"message\":\"Nested error\"}}");
            RequestResult result = RequestResult.builder()
                    .success(false)
                    .statusCode(500)
                    .data(data)
                    .build();
            assertEquals("Nested error", result.extractErrorMessage());
        }

        @Test
        void extractErrorFromMessageField() throws Exception {
            JsonNode data = JsonUtils.MAPPER.readTree("{\"message\":\"Something went wrong\"}");
            RequestResult result = RequestResult.builder()
                    .success(false)
                    .statusCode(500)
                    .data(data)
                    .build();
            assertEquals("Something went wrong", result.extractErrorMessage());
        }

        @Test
        void extractErrorFromErrorMsgField() throws Exception {
            JsonNode data = JsonUtils.MAPPER.readTree("{\"error_msg\":\"Huawei Cloud error\"}");
            RequestResult result = RequestResult.builder()
                    .success(false)
                    .statusCode(500)
                    .data(data)
                    .build();
            assertEquals("Huawei Cloud error", result.extractErrorMessage());
        }

        @Test
        void errorFieldTakesPrecedenceOverJson() throws Exception {
            JsonNode data = JsonUtils.MAPPER.readTree("{\"error\":\"json error\"}");
            RequestResult result = RequestResult.builder()
                    .success(false)
                    .statusCode(500)
                    .error("direct error")
                    .data(data)
                    .build();
            assertEquals("direct error", result.extractErrorMessage());
        }

        @Test
        void extractErrorReturnsNullWhenNoError() {
            RequestResult result = RequestResult.builder()
                    .success(true)
                    .statusCode(200)
                    .data("ok")
                    .build();
            assertNull(result.extractErrorMessage());
        }

        @Test
        void extractErrorReturnsNullForEmptyJson() throws Exception {
            JsonNode data = JsonUtils.MAPPER.readTree("{}");
            RequestResult result = RequestResult.builder()
                    .success(false)
                    .statusCode(500)
                    .data(data)
                    .build();
            assertNull(result.extractErrorMessage());
        }

        @Test
        void extractErrorWithStringDataReturnsNull() {
            // String data is not JsonNode, so no JSON error extraction
            RequestResult result = RequestResult.builder()
                    .success(false)
                    .statusCode(500)
                    .data("some text")
                    .build();
            assertNull(result.extractErrorMessage());
        }

        @Test
        void statusCodeZeroWhenNoResponse() {
            RequestResult result = RequestResult.builder()
                    .success(false)
                    .statusCode(0)
                    .error("Connection refused")
                    .build();
            assertEquals(0, result.getStatusCode());
            assertEquals("Connection refused", result.getError());
        }
    }
}
