package com.huaweicloud.agentarts.sdk.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link APIException}.
 */
class APIExceptionTest {

    // ============================================================
    // Constructor with 3 parameters
    // ============================================================

    @Nested
    class ThreeArgConstructor {

        @Test
        void storesStatusCode() {
            APIException e = new APIException(404, "NOT_FOUND", "Resource not found");
            assertEquals(404, e.getStatusCode());
        }

        @Test
        void storesErrorCode() {
            APIException e = new APIException(400, "BAD_REQUEST", "Invalid input");
            assertEquals("BAD_REQUEST", e.getErrorCode());
        }

        @Test
        void storesErrorMsg() {
            APIException e = new APIException(500, "INTERNAL", "Server error");
            assertEquals("Server error", e.getErrorMsg());
        }

        @Test
        void formatsMessage() {
            APIException e = new APIException(403, "FORBIDDEN", "Access denied");
            assertEquals("[FORBIDDEN] HTTP 403: Access denied", e.getMessage());
        }

        @Test
        void handlesNullErrorCode() {
            APIException e = new APIException(500, null, "Error");
            assertEquals("", e.getErrorCode());
            assertTrue(e.getMessage().contains("HTTP 500"));
        }

        @Test
        void handlesNullErrorMsg() {
            APIException e = new APIException(500, "ERR", null);
            assertEquals("", e.getErrorMsg());
            assertTrue(e.getMessage().contains("[ERR]"));
        }

        @Test
        void handlesBothNull() {
            APIException e = new APIException(500, null, null);
            assertEquals("", e.getErrorCode());
            assertEquals("", e.getErrorMsg());
            assertEquals("[] HTTP 500: ", e.getMessage());
        }

        @Test
        void isRuntimeException() {
            APIException e = new APIException(500, "ERR", "err");
            assertInstanceOf(RuntimeException.class, e);
        }
    }

    // ============================================================
    // Constructor with 4 parameters (cause)
    // ============================================================

    @Nested
    class FourArgConstructor {

        @Test
        void storesCause() {
            Throwable cause = new RuntimeException("root cause");
            APIException e = new APIException(500, "ERR", "Error", cause);
            assertSame(cause, e.getCause());
        }

        @Test
        void formatsMessageWithCause() {
            Throwable cause = new RuntimeException("root");
            APIException e = new APIException(502, "GATEWAY", "Bad gateway", cause);
            assertEquals("[GATEWAY] HTTP 502: Bad gateway", e.getMessage());
            assertSame(cause, e.getCause());
        }

        @Test
        void handlesNullCause() {
            APIException e = new APIException(500, "ERR", "Error", null);
            assertNull(e.getCause());
        }
    }

    // ============================================================
    // Message format matches Python: "[error_code] HTTP status_code: error_msg"
    // ============================================================

    @Nested
    class MessageFormat {

        @Test
        void matchesPythonFormat() {
            APIException e = new APIException(401, "UNAUTHORIZED", "Invalid credentials");
            // Python format: "[UNAUTHORIZED] HTTP 401: Invalid credentials"
            assertEquals("[UNAUTHORIZED] HTTP 401: Invalid credentials", e.getMessage());
        }

        @Test
        void statusCodeIsInteger() {
            APIException e = new APIException(429, "RATE_LIMIT", "Too many requests");
            assertTrue(e.getMessage().contains("429"));
            assertFalse(e.getMessage().contains("\"429\""));
        }

        @Test
        void emptyErrorCodeProducesBracketsOnly() {
            APIException e = new APIException(500, "", "Error");
            assertTrue(e.getMessage().startsWith("[]"));
        }

        @Test
        void emptyErrorMsgProducesColonSpace() {
            APIException e = new APIException(500, "ERR", "");
            assertTrue(e.getMessage().endsWith(": "));
        }
    }

    // ============================================================
    // Edge cases
    // ============================================================

    @Nested
    class EdgeCases {

        @Test
        void zeroStatusCode() {
            APIException e = new APIException(0, "NO_RESPONSE", "No response");
            assertEquals(0, e.getStatusCode());
            assertTrue(e.getMessage().contains("HTTP 0"));
        }

        @Test
        void negativeStatusCode() {
            APIException e = new APIException(-1, "NETWORK", "Connection refused");
            assertEquals(-1, e.getStatusCode());
        }

        @Test
        void largeStatusCode() {
            APIException e = new APIException(9999, "CUSTOM", "Custom");
            assertEquals(9999, e.getStatusCode());
        }

        @Test
        void specialCharsInErrorCode() {
            APIException e = new APIException(400, "ERR-001.V2", "Error");
            assertEquals("ERR-001.V2", e.getErrorCode());
        }

        @Test
        void unicodeInErrorMsg() {
            APIException e = new APIException(400, "ERR", "错误: 参数无效");
            assertEquals("错误: 参数无效", e.getErrorMsg());
            assertTrue(e.getMessage().contains("错误: 参数无效"));
        }

        @Test
        void canBeThrownAndCaught() {
            assertThrows(APIException.class, () -> {
                throw new APIException(500, "TEST", "test error");
            });
        }

        @Test
        void canBeCaughtAsRuntimeException() {
            assertThrows(RuntimeException.class, () -> {
                throw new APIException(500, "TEST", "test error");
            });
        }
    }
}
