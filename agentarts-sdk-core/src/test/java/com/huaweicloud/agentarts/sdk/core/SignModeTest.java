package com.huaweicloud.agentarts.sdk.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link SignMode}.
 */
class SignModeTest {

    @Nested
    class Values {

        @Test
        void sdkHmacSha256HasCorrectValue() {
            assertEquals("sdk", SignMode.SDK_HMAC_SHA256.getValue());
        }

        @Test
        void v11HmacSha256HasCorrectValue() {
            assertEquals("v11", SignMode.V11_HMAC_SHA256.getValue());
        }

        @Test
        void exactlyTwoValues() {
            assertEquals(2, SignMode.values().length);
        }

        @Test
        void matchesPythonSignMode() {
            // Python: SignMode.SDK_HMAC_SHA256 = "sdk", SignMode.V11_HMAC_SHA256 = "v11"
            assertEquals("sdk", SignMode.SDK_HMAC_SHA256.getValue());
            assertEquals("v11", SignMode.V11_HMAC_SHA256.getValue());
        }
    }

    @Nested
    class ValueOf {
        @Test
        void valueOfWorks() {
            assertEquals(SignMode.SDK_HMAC_SHA256, SignMode.valueOf("SDK_HMAC_SHA256"));
            assertEquals(SignMode.V11_HMAC_SHA256, SignMode.valueOf("V11_HMAC_SHA256"));
        }

        @Test
        void valueOfThrowsForInvalid() {
            assertThrows(IllegalArgumentException.class, () -> SignMode.valueOf("invalid"));
        }
    }
}
