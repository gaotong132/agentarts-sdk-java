package com.huaweicloud.agentarts.sdk.service.runtime;

import org.junit.jupiter.api.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RuntimeClient and LocalRuntimeClient.
 */
class RuntimeClientTest {

    @Nested
    @DisplayName("RuntimeClient API methods")
    class ApiMethodTests {

        @Test
        void controlPlaneMethodsExist() throws Exception {
            Class<?> cls = RuntimeClient.class;
            assertNotNull(cls.getMethod("createAgent", String.class, String.class));
            assertNotNull(cls.getMethod("updateAgent", String.class, String.class,
                    Map.class, Map.class, Map.class, Map.class,
                    String.class, String.class, List.class, List.class));
            assertNotNull(cls.getMethod("createOrUpdateAgent", String.class, String.class,
                    Map.class, Map.class, Map.class, Map.class, Map.class,
                    String.class, String.class, List.class, List.class));
            assertNotNull(cls.getMethod("getAgents", String.class, int.class, int.class));
            assertNotNull(cls.getMethod("getAgents"));
            assertNotNull(cls.getMethod("findAgentByName", String.class));
            assertNotNull(cls.getMethod("findAgentById", String.class));
            assertNotNull(cls.getMethod("deleteAgentByName", String.class));
        }

        @Test
        void endpointMethodsExist() throws Exception {
            Class<?> cls = RuntimeClient.class;
            assertNotNull(cls.getMethod("createAgentEndpoint", String.class, String.class));
            assertNotNull(cls.getMethod("updateAgentEndpoint", String.class, String.class, Map.class));
            assertNotNull(cls.getMethod("deleteAgentEndpoint", String.class, String.class));
            assertNotNull(cls.getMethod("findAgentEndpoint", String.class, String.class));
        }

        @Test
        void dataPlaneMethodsExist() throws Exception {
            Class<?> cls = RuntimeClient.class;
            assertNotNull(cls.getMethod("invokeAgent", String.class, String.class, String.class));
            assertNotNull(cls.getMethod("execCommand", String.class, String.class, String.class));
            assertNotNull(cls.getMethod("uploadFiles", String.class, String.class, List.class));
            assertNotNull(cls.getMethod("downloadFiles", String.class, String.class, String.class));
            assertNotNull(cls.getMethod("startSession", String.class));
            assertNotNull(cls.getMethod("stopSession", String.class, String.class));
        }

        @Test
        void implementsAutoCloseable() {
            assertTrue(AutoCloseable.class.isAssignableFrom(RuntimeClient.class));
        }

        @Test
        void constructorsWork() {
            assertNotNull(new RuntimeClient());
            assertNotNull(new RuntimeClient("cn-southwest-2"));
            assertNotNull(new RuntimeClient("cn-southwest-2", true));
        }
    }

    @Nested
    @DisplayName("LocalRuntimeClient API methods")
    class LocalClientTests {

        @Test
        void methodsExist() throws Exception {
            Class<?> cls = LocalRuntimeClient.class;
            assertNotNull(cls.getMethod("invokeAgent", String.class));
            assertNotNull(cls.getMethod("pingAgent"));
        }

        @Test
        void implementsAutoCloseable() {
            assertTrue(AutoCloseable.class.isAssignableFrom(LocalRuntimeClient.class));
        }

        @Test
        void constructorsWork() {
            assertNotNull(new LocalRuntimeClient());
            assertNotNull(new LocalRuntimeClient(3000));
            assertNotNull(new LocalRuntimeClient(3000, "localhost", 60));
        }

        @Test
        void defaultPortAndHost() {
            LocalRuntimeClient client = new LocalRuntimeClient();
            assertEquals(8080, client.getPort());
            assertEquals("localhost", client.getHost());
        }
    }
}
