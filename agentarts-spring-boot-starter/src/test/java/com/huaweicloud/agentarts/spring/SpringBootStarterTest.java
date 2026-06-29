package com.huaweicloud.agentarts.spring;

import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.core.PingStatus;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Spring Boot Starter: AutoConfiguration, Properties, HealthIndicator.
 */
class SpringBootStarterTest {

    // ========================
    // AgentArtsProperties
    // ========================

    @Nested
    @DisplayName("AgentArtsProperties")
    class PropertiesTests {

        @Test
        void defaultRegionIsNull() {
            AgentArtsProperties props = new AgentArtsProperties();
            assertNull(props.getRegion());
        }

        @Test
        void resolveRegionFallsBackToConstants() {
            AgentArtsProperties props = new AgentArtsProperties();
            String resolved = props.resolveRegion();
            // Should fall back to Constants.getRegion() which returns cn-southwest-2
            assertEquals(Constants.DEFAULT_REGION, resolved);
        }

        @Test
        void resolveRegionUsesPropertyWhenSet() {
            AgentArtsProperties props = new AgentArtsProperties();
            props.setRegion("cn-north-4");
            assertEquals("cn-north-4", props.resolveRegion());
        }

        @Test
        void resolveAccessKeyFallsBackToConstants() {
            AgentArtsProperties props = new AgentArtsProperties();
            // When no AK env var, falls back to empty string
            String resolved = props.resolveAccessKey();
            assertNotNull(resolved);
        }

        @Test
        void resolveAccessKeyUsesPropertyWhenSet() {
            AgentArtsProperties props = new AgentArtsProperties();
            props.setAccessKey("test-ak");
            assertEquals("test-ak", props.resolveAccessKey());
        }

        @Test
        void runtimeDefaultsAreCorrect() {
            AgentArtsProperties props = new AgentArtsProperties();
            assertEquals(8080, props.getRuntime().getPort());
            assertEquals(15, props.getRuntime().getMaxConcurrency());
        }

        @Test
        void runtimePortCanBeSet() {
            AgentArtsProperties props = new AgentArtsProperties();
            props.getRuntime().setPort(9090);
            assertEquals(9090, props.getRuntime().getPort());
        }

        @Test
        void memoryDefaultsAreNull() {
            AgentArtsProperties props = new AgentArtsProperties();
            assertNull(props.getMemory().getApiKey());
            assertNull(props.getMemory().getSpaceId());
        }

        @Test
        void memoryCanBeConfigured() {
            AgentArtsProperties props = new AgentArtsProperties();
            props.getMemory().setApiKey("key-123");
            props.getMemory().setSpaceId("space-456");
            assertEquals("key-123", props.getMemory().getApiKey());
            assertEquals("space-456", props.getMemory().getSpaceId());
        }

        @Test
        void identityDefaultsAreNull() {
            AgentArtsProperties props = new AgentArtsProperties();
            assertNull(props.getIdentity().getWorkloadName());
            assertNull(props.getIdentity().getProviderName());
        }

        @Test
        void identityCanBeConfigured() {
            AgentArtsProperties props = new AgentArtsProperties();
            props.getIdentity().setWorkloadName("my-agent");
            props.getIdentity().setProviderName("my-provider");
            assertEquals("my-agent", props.getIdentity().getWorkloadName());
            assertEquals("my-provider", props.getIdentity().getProviderName());
        }
    }

    // ========================
    // AgentArtsAutoConfiguration (unit-level, no Spring context)
    // ========================

    @Nested
    @DisplayName("AgentArtsAutoConfiguration")
    class AutoConfigurationTests {

        @Test
        void createsRuntimeAppFromProperties() {
            AgentArtsAutoConfiguration config = new AgentArtsAutoConfiguration();
            AgentArtsProperties props = new AgentArtsProperties();
            props.getRuntime().setMaxConcurrency(20);

            AgentArtsRuntimeApp app = config.agentArtsRuntimeApp(props);
            assertNotNull(app);
        }

        @Test
        void createsRuntimeAppWithDefaultProperties() {
            AgentArtsAutoConfiguration config = new AgentArtsAutoConfiguration();
            AgentArtsProperties props = new AgentArtsProperties();

            AgentArtsRuntimeApp app = config.agentArtsRuntimeApp(props);
            assertNotNull(app);
        }
    }

    // ========================
    // AgentArtsHealthIndicator
    // ========================

    @Nested
    @DisplayName("AgentArtsHealthIndicator")
    class HealthIndicatorTests {

        @Test
        void reportsUnknownWhenServerNotStarted() {
            AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
            AgentArtsHealthIndicatorAutoConfiguration.AgentArtsHealthIndicator indicator =
                    new AgentArtsHealthIndicatorAutoConfiguration.AgentArtsHealthIndicator(app);

            org.springframework.boot.actuate.health.Health.Builder builder =
                    new org.springframework.boot.actuate.health.Health.Builder();
            indicator.doHealthCheck(builder);

            org.springframework.boot.actuate.health.Health health = builder.build();
            assertEquals(org.springframework.boot.actuate.health.Status.UNKNOWN, health.getStatus());
            assertEquals(false, health.getDetails().get("running"));
        }

        @Test
        void reportsUpWhenServerStarted() throws Exception {
            AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
            app.setPingHandler(() -> PingStatus.HEALTHY);
            app.run(0); // random port

            try {
                AgentArtsHealthIndicatorAutoConfiguration.AgentArtsHealthIndicator indicator =
                        new AgentArtsHealthIndicatorAutoConfiguration.AgentArtsHealthIndicator(app);

                org.springframework.boot.actuate.health.Health.Builder builder =
                        new org.springframework.boot.actuate.health.Health.Builder();
                indicator.doHealthCheck(builder);

                org.springframework.boot.actuate.health.Health health = builder.build();
                assertEquals(org.springframework.boot.actuate.health.Status.UP, health.getStatus());
                assertEquals(true, health.getDetails().get("running"));
                assertEquals("healthy", health.getDetails().get("status"));
            } finally {
                app.stop();
            }
        }

        @Test
        void reportsBusyWhenTasksRunning() throws Exception {
            AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
            app.setPingHandler(() -> PingStatus.HEALTHY);
            app.run(0);

            try {
                long taskId = app.addTask("test-task");

                AgentArtsHealthIndicatorAutoConfiguration.AgentArtsHealthIndicator indicator =
                        new AgentArtsHealthIndicatorAutoConfiguration.AgentArtsHealthIndicator(app);

                org.springframework.boot.actuate.health.Health.Builder builder =
                        new org.springframework.boot.actuate.health.Health.Builder();
                indicator.doHealthCheck(builder);

                org.springframework.boot.actuate.health.Health health = builder.build();
                assertEquals(org.springframework.boot.actuate.health.Status.UP, health.getStatus());
                assertEquals("busy", health.getDetails().get("status"));

                app.completeTask(taskId);
            } finally {
                app.stop();
            }
        }
    }
}
