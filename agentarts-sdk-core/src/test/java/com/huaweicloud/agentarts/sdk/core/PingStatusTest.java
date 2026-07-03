package com.huaweicloud.agentarts.sdk.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link PingStatus}.
 */
class PingStatusTest {

    @Nested
    class Values {

        @Test
        void healthyHasCorrectValue() {
            assertEquals("Healthy", PingStatus.HEALTHY.getValue());
        }

        @Test
        void healthyBusyHasCorrectValue() {
            assertEquals("HealthyBusy", PingStatus.HEALTHY_BUSY.getValue());
        }

        @Test
        void unhealthyHasCorrectValue() {
            assertEquals("Unhealthy", PingStatus.UNHEALTHY.getValue());
        }

        @Test
        void exactlyThreeValues() {
            assertEquals(3, PingStatus.values().length);
        }
    }

    @Nested
    class FromValue {

        @Test
        void parsesHealthy() {
            assertEquals(PingStatus.HEALTHY, PingStatus.fromValue("Healthy"));
        }

        @Test
        void parsesHealthyBusy() {
            assertEquals(PingStatus.HEALTHY_BUSY, PingStatus.fromValue("HealthyBusy"));
        }

        @Test
        void parsesUnhealthy() {
            assertEquals(PingStatus.UNHEALTHY, PingStatus.fromValue("Unhealthy"));
        }

        @Test
        void throwsForUnknownValue() {
            assertThrows(IllegalArgumentException.class, () -> PingStatus.fromValue("Unknown"));
        }

        @Test
        void throwsForNull() {
            // null doesn't match any value string
            assertThrows(Exception.class, () -> PingStatus.fromValue(null));
        }

        @Test
        void throwsForEmpty() {
            assertThrows(IllegalArgumentException.class, () -> PingStatus.fromValue(""));
        }

        @Test
        void caseSensitive() {
            // "healthy" != "Healthy"
            assertThrows(IllegalArgumentException.class, () -> PingStatus.fromValue("healthy"));
        }

        @Test
        void throwsForLowercase() {
            assertThrows(IllegalArgumentException.class, () -> PingStatus.fromValue("healthybusy"));
        }

        @Test
        void roundTrip() {
            for (PingStatus status : PingStatus.values()) {
                assertEquals(status, PingStatus.fromValue(status.getValue()));
            }
        }
    }

    @Nested
    class ValueOf {
        @Test
        void valueOfWorksForEnumNames() {
            assertEquals(PingStatus.HEALTHY, PingStatus.valueOf("HEALTHY"));
            assertEquals(PingStatus.HEALTHY_BUSY, PingStatus.valueOf("HEALTHY_BUSY"));
            assertEquals(PingStatus.UNHEALTHY, PingStatus.valueOf("UNHEALTHY"));
        }

        @Test
        void valueOfThrowsForInvalid() {
            assertThrows(IllegalArgumentException.class, () -> PingStatus.valueOf("Healthy"));
        }
    }
}
