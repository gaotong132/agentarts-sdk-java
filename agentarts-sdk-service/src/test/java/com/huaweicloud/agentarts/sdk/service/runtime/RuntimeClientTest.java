package com.huaweicloud.agentarts.sdk.service.runtime;

import com.huaweicloud.agentarts.sdk.service.runtime.model.CreateAgentRequest;
import com.huaweicloud.agentarts.sdk.service.runtime.model.UpdateAgentRequest;
import org.junit.jupiter.api.*;
import com.sun.net.httpserver.HttpServer;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
            assertNotNull(cls.getMethod("createAgent", CreateAgentRequest.class));
            assertNotNull(cls.getMethod("createAgent", String.class, String.class));
            assertNotNull(cls.getMethod("updateAgent", String.class, UpdateAgentRequest.class));
            assertNotNull(cls.getMethod("createOrUpdateAgent", String.class, CreateAgentRequest.class));
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
        void constructorsDoNotThrow() {
            // RuntimeClient exposes no region/signMode getters, so the smoke test
            // is that each public constructor completes without throwing.
            new RuntimeClient();
            new RuntimeClient("cn-southwest-2");
            new RuntimeClient("cn-southwest-2", true);
        }

        @Test
        void perCallEndpointBearerAndEncodedQueryAreApplied() throws Exception {
            AtomicReference<String> authorization = new AtomicReference<>();
            AtomicReference<String> rawQuery = new AtomicReference<>();
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                rawQuery.set(exchange.getRequestURI().getRawQuery());
                byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();

            String endpoint = "http://127.0.0.1:" + server.getAddress().getPort();
            try (RuntimeClient client = new RuntimeClient("cn-southwest-2")) {
                Map<String, Object> result = client.invokeAgent(
                        "agent", "session", "{}", "per-call-token", endpoint,
                        2, "user", null);
                assertNotNull(result);
                assertEquals("Bearer per-call-token", authorization.get());

                client.downloadFiles(
                        "agent", "session", "/home/user/a b.txt", true,
                        "download-token", endpoint, null, 2);
                assertEquals("Bearer download-token", authorization.get());
                assertTrue(rawQuery.get().contains("path=%2Fhome%2Fuser%2Fa%20b.txt"));
                assertTrue(rawQuery.get().contains("recursive=true"));
            } finally {
                server.stop(0);
            }
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
            LocalRuntimeClient c0 = new LocalRuntimeClient();
            assertEquals(8080, c0.getPort());
            assertEquals("localhost", c0.getHost());

            LocalRuntimeClient c1 = new LocalRuntimeClient(3000);
            assertEquals(3000, c1.getPort());
            assertEquals("localhost", c1.getHost());

            LocalRuntimeClient c2 = new LocalRuntimeClient(3000, "localhost", 60);
            assertEquals(3000, c2.getPort());
            assertEquals("localhost", c2.getHost());
        }

        @Test
        void defaultPortAndHost() {
            LocalRuntimeClient client = new LocalRuntimeClient();
            assertEquals(8080, client.getPort());
            assertEquals("localhost", client.getHost());
        }
    }
}
