package com.huaweicloud.agentarts.sdk.mcpgateway.model;

import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for all MCP Gateway model classes.
 */
class MCPGatewayModelTest {

    // ============================================================
    // CreateMcpGatewayRequest
    // ============================================================

    @Nested
    class CreateMcpGatewayRequestTests {

        @Test
        void defaultValues() {
            CreateMcpGatewayRequest req = new CreateMcpGatewayRequest();
            assertNull(req.getName());
            assertNull(req.getDescription());
            assertNull(req.getProtocolType());
            assertNull(req.getAuthorizerType());
            assertNull(req.getAgencyName());
        }

        @Test
        void fluentApi() {
            CreateMcpGatewayRequest req = new CreateMcpGatewayRequest()
                    .withName("my-gateway")
                    .withDescription("Test gateway")
                    .withProtocolType("mcp")
                    .withAuthorizerType("iam")
                    .withAgencyName("my-agency");

            assertEquals("my-gateway", req.getName());
            assertEquals("Test gateway", req.getDescription());
            assertEquals("mcp", req.getProtocolType());
            assertEquals("iam", req.getAuthorizerType());
            assertEquals("my-agency", req.getAgencyName());
        }

        @Test
        void jsonSerialization() {
            CreateMcpGatewayRequest req = new CreateMcpGatewayRequest()
                    .withName("gw-1")
                    .withProtocolType("mcp")
                    .withAuthorizerType("iam");
            String json = JsonUtils.toJson(req);
            assertTrue(json.contains("\"name\":\"gw-1\""));
            assertTrue(json.contains("\"protocol_type\":\"mcp\""));
            assertTrue(json.contains("\"authorizer_type\":\"iam\""));
        }

        @Test
        void jsonSerializationExcludesNulls() {
            CreateMcpGatewayRequest req = new CreateMcpGatewayRequest()
                    .withName("gw-1");
            String json = JsonUtils.toJson(req);
            assertTrue(json.contains("\"name\":\"gw-1\""));
            assertFalse(json.contains("\"description\""));
            assertFalse(json.contains("\"agency_name\""));
        }

        @Test
        void jsonDeserialization() throws Exception {
            String json = "{\"name\":\"from-json\",\"description\":\"desc\","
                    + "\"protocol_type\":\"mcp\",\"authorizer_type\":\"iam\","
                    + "\"agency_name\":\"agency\"}";
            CreateMcpGatewayRequest req = JsonUtils.MAPPER.readValue(json, CreateMcpGatewayRequest.class);
            assertEquals("from-json", req.getName());
            assertEquals("desc", req.getDescription());
            assertEquals("mcp", req.getProtocolType());
            assertEquals("iam", req.getAuthorizerType());
            assertEquals("agency", req.getAgencyName());
        }

        @Test
        void equalityAndHashCode() {
            CreateMcpGatewayRequest a = new CreateMcpGatewayRequest()
                    .withName("gw").withDescription("d").withProtocolType("mcp")
                    .withAuthorizerType("iam").withAgencyName("a");
            CreateMcpGatewayRequest b = new CreateMcpGatewayRequest()
                    .withName("gw").withDescription("d").withProtocolType("mcp")
                    .withAuthorizerType("iam").withAgencyName("a");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        void inequality() {
            CreateMcpGatewayRequest a = new CreateMcpGatewayRequest().withName("gw1");
            CreateMcpGatewayRequest b = new CreateMcpGatewayRequest().withName("gw2");
            assertNotEquals(a, b);
        }

        @Test
        void toStringContainsName() {
            CreateMcpGatewayRequest req = new CreateMcpGatewayRequest().withName("my-gw");
            assertTrue(req.toString().contains("my-gw"));
        }

        @Test
        void standardSetters() {
            CreateMcpGatewayRequest req = new CreateMcpGatewayRequest();
            req.setName("gw");
            req.setDescription("desc");
            req.setProtocolType("mcp");
            req.setAuthorizerType("iam");
            req.setAgencyName("agency");

            assertEquals("gw", req.getName());
            assertEquals("desc", req.getDescription());
            assertEquals("mcp", req.getProtocolType());
            assertEquals("iam", req.getAuthorizerType());
            assertEquals("agency", req.getAgencyName());
        }
    }

    // ============================================================
    // UpdateMcpGatewayRequest
    // ============================================================

    @Nested
    class UpdateMcpGatewayRequestTests {

        @Test
        void fluentApi() {
            UpdateMcpGatewayRequest req = new UpdateMcpGatewayRequest()
                    .withDescription("Updated description");
            assertEquals("Updated description", req.getDescription());
        }

        @Test
        void jsonSerialization() {
            UpdateMcpGatewayRequest req = new UpdateMcpGatewayRequest()
                    .withDescription("Updated");
            String json = JsonUtils.toJson(req);
            assertTrue(json.contains("\"description\":\"Updated\""));
        }

        @Test
        void jsonDeserialization() throws Exception {
            String json = "{\"description\":\"new desc\"}";
            UpdateMcpGatewayRequest req = JsonUtils.MAPPER.readValue(json, UpdateMcpGatewayRequest.class);
            assertEquals("new desc", req.getDescription());
        }

        @Test
        void equalityAndHashCode() {
            UpdateMcpGatewayRequest a = new UpdateMcpGatewayRequest().withDescription("d");
            UpdateMcpGatewayRequest b = new UpdateMcpGatewayRequest().withDescription("d");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }

    // ============================================================
    // CreateMcpGatewayTargetRequest
    // ============================================================

    @Nested
    class CreateMcpGatewayTargetRequestTests {

        @Test
        void defaultValues() {
            CreateMcpGatewayTargetRequest req = new CreateMcpGatewayTargetRequest();
            assertNull(req.getName());
            assertNull(req.getDescription());
            assertNull(req.getTargetConfiguration());
            assertNull(req.getCredentialProviderConfiguration());
        }

        @Test
        void fluentApi() {
            CreateMcpGatewayTargetRequest req = new CreateMcpGatewayTargetRequest()
                    .withName("my-target")
                    .withDescription("Test target")
                    .withTargetConfiguration(Map.of("url", "https://target.example.com"))
                    .withCredentialProviderConfiguration(Map.of("credential_provider_type", "none"));

            assertEquals("my-target", req.getName());
            assertEquals("Test target", req.getDescription());
            assertEquals("https://target.example.com", req.getTargetConfiguration().get("url"));
            assertEquals("none", req.getCredentialProviderConfiguration().get("credential_provider_type"));
        }

        @Test
        void jsonSerialization() {
            CreateMcpGatewayTargetRequest req = new CreateMcpGatewayTargetRequest()
                    .withName("target-1")
                    .withTargetConfiguration(Map.of("url", "https://example.com"));
            String json = JsonUtils.toJson(req);
            assertTrue(json.contains("\"name\":\"target-1\""));
            assertTrue(json.contains("\"target_configuration\""));
            assertTrue(json.contains("\"url\":\"https://example.com\""));
        }

        @Test
        void jsonSerializationExcludesNulls() {
            CreateMcpGatewayTargetRequest req = new CreateMcpGatewayTargetRequest()
                    .withName("t1");
            String json = JsonUtils.toJson(req);
            assertTrue(json.contains("\"name\":\"t1\""));
            assertFalse(json.contains("\"description\""));
            assertFalse(json.contains("\"target_configuration\""));
        }

        @Test
        void equalityAndHashCode() {
            CreateMcpGatewayTargetRequest a = new CreateMcpGatewayTargetRequest()
                    .withName("t").withDescription("d");
            CreateMcpGatewayTargetRequest b = new CreateMcpGatewayTargetRequest()
                    .withName("t").withDescription("d");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }

    // ============================================================
    // UpdateMcpGatewayTargetRequest
    // ============================================================

    @Nested
    class UpdateMcpGatewayTargetRequestTests {

        @Test
        void fluentApi() {
            UpdateMcpGatewayTargetRequest req = new UpdateMcpGatewayTargetRequest()
                    .withName("updated-target")
                    .withDescription("Updated")
                    .withTargetConfiguration(Map.of("url", "https://new.example.com"));

            assertEquals("updated-target", req.getName());
            assertEquals("Updated", req.getDescription());
            assertEquals("https://new.example.com", req.getTargetConfiguration().get("url"));
        }

        @Test
        void jsonSerialization() {
            UpdateMcpGatewayTargetRequest req = new UpdateMcpGatewayTargetRequest()
                    .withName("t1")
                    .withCredentialProviderConfiguration(Map.of("type", "api_key"));
            String json = JsonUtils.toJson(req);
            assertTrue(json.contains("\"name\":\"t1\""));
            assertTrue(json.contains("\"credential_provider_configuration\""));
        }

        @Test
        void jsonDeserialization() throws Exception {
            String json = "{\"name\":\"t1\",\"description\":\"d\","
                    + "\"target_configuration\":{\"url\":\"https://example.com\"},"
                    + "\"credential_provider_configuration\":{\"type\":\"none\"}}";
            UpdateMcpGatewayTargetRequest req = JsonUtils.MAPPER.readValue(json, UpdateMcpGatewayTargetRequest.class);
            assertEquals("t1", req.getName());
            assertEquals("d", req.getDescription());
            assertNotNull(req.getTargetConfiguration());
            assertNotNull(req.getCredentialProviderConfiguration());
        }

        @Test
        void equalityAndHashCode() {
            UpdateMcpGatewayTargetRequest a = new UpdateMcpGatewayTargetRequest()
                    .withName("t").withDescription("d");
            UpdateMcpGatewayTargetRequest b = new UpdateMcpGatewayTargetRequest()
                    .withName("t").withDescription("d");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }
}
