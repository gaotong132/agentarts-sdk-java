package com.huaweicloud.agentarts.sdk.mcpgateway;

import com.huaweicloud.agentarts.sdk.mcpgateway.model.CreateMcpGatewayRequest;
import com.huaweicloud.agentarts.sdk.mcpgateway.model.CreateMcpGatewayTargetRequest;
import com.huaweicloud.agentarts.sdk.mcpgateway.model.UpdateMcpGatewayRequest;
import com.huaweicloud.agentarts.sdk.mcpgateway.model.UpdateMcpGatewayTargetRequest;
import com.huaweicloud.agentarts.sdk.service.http.BaseHttpClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;
import com.huaweicloud.sdk.core.auth.ICredentialProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MCPGatewayClientTest {

    private BaseHttpClient httpClient;
    private MCPGatewayClient client;
    private RequestResult success;

    @BeforeEach
    void setUp() {
        httpClient = mock(BaseHttpClient.class);
        success = RequestResult.builder().success(true).statusCode(200).build();
        client = new MCPGatewayClient(true, mock(ICredentialProvider.class), httpClient);
    }

    @Test
    void executesGatewayCrudWithDefaultsAndQueryParameters() {
        when(httpClient.post(eq("/gateways"), isNull(), any()))
                .thenReturn(Mono.just(success));
        when(httpClient.put(eq("/gateways/gateway"), isNull(), any()))
                .thenReturn(Mono.just(success));
        when(httpClient.get("/gateways/gateway")).thenReturn(Mono.just(success));
        when(httpClient.delete("/gateways/gateway")).thenReturn(Mono.just(success));
        when(httpClient.request(eq("GET"), eq("/gateways"), isNull(), isNull(), anyMap()))
                .thenReturn(Mono.just(success));

        assertSame(success, client.createMcpGateway("name", "description", null, "none", null));
        assertSame(success, client.updateMcpGateway("gateway", "updated"));
        assertSame(success, client.getMcpGateway("gateway"));
        assertSame(success, client.deleteMcpGateway("gateway"));
        assertSame(success, client.listMcpGateways("name", 20, 10));

        var create = ArgumentCaptor.forClass(CreateMcpGatewayRequest.class);
        verify(httpClient).post(eq("/gateways"), isNull(), create.capture());
        assertEquals("name", create.getValue().getName());
        assertEquals("mcp", create.getValue().getProtocolType());
        assertEquals("none", create.getValue().getAuthorizerType());
        assertNull(create.getValue().getAgencyName());
        assertEquals(Map.of("enabled", false), create.getValue().getLogDeliveryConfiguration());
        assertEquals(Map.of("network_mode", "public"),
                create.getValue().getOutboundNetworkConfiguration());

        var update = ArgumentCaptor.forClass(UpdateMcpGatewayRequest.class);
        verify(httpClient).put(eq("/gateways/gateway"), isNull(), update.capture());
        assertEquals("updated", update.getValue().getDescription());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, List<String>>> query = ArgumentCaptor.forClass(Map.class);
        verify(httpClient).request(eq("GET"), eq("/gateways"), isNull(), isNull(), query.capture());
        assertEquals(List.of("name"), query.getValue().get("name"));
        assertEquals(List.of("20"), query.getValue().get("limit"));
        assertEquals(List.of("10"), query.getValue().get("offset"));
    }

    @Test
    void executesTargetCrudAndAppliesSafeCredentialProviderDefault() {
        String targets = "/gateways/gateway/targets";
        String target = targets + "/target";
        when(httpClient.post(eq(targets), isNull(), any())).thenReturn(Mono.just(success));
        when(httpClient.put(eq(target), isNull(), any())).thenReturn(Mono.just(success));
        when(httpClient.get(target)).thenReturn(Mono.just(success));
        when(httpClient.delete(target)).thenReturn(Mono.just(success));
        when(httpClient.request(eq("GET"), eq(targets), isNull(), isNull(), anyMap()))
                .thenReturn(Mono.just(success));

        assertSame(success, client.createMcpGatewayTarget(
                "gateway", "name", "description", Map.of("url", "https://example.test"), null));
        assertSame(success, client.updateMcpGatewayTarget(
                "gateway", "target", "updated", "description",
                Map.of("url", "https://updated.example.test"), Map.of("type", "configured")));
        assertSame(success, client.getMcpGatewayTarget("gateway", "target"));
        assertSame(success, client.deleteMcpGatewayTarget("gateway", "target"));
        assertSame(success, client.listMcpGatewayTargets("gateway", 50, 5));

        var create = ArgumentCaptor.forClass(CreateMcpGatewayTargetRequest.class);
        verify(httpClient).post(eq(targets), isNull(), create.capture());
        assertEquals("name", create.getValue().getName());
        assertEquals(Map.of("credential_provider_type", "none"),
                create.getValue().getCredentialProviderConfiguration());

        var update = ArgumentCaptor.forClass(UpdateMcpGatewayTargetRequest.class);
        verify(httpClient).put(eq(target), isNull(), update.capture());
        assertEquals("updated", update.getValue().getName());
        assertEquals(Map.of("type", "configured"),
                update.getValue().getCredentialProviderConfiguration());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, List<String>>> query = ArgumentCaptor.forClass(Map.class);
        verify(httpClient).request(eq("GET"), eq(targets), isNull(), isNull(), query.capture());
        assertEquals(List.of("50"), query.getValue().get("limit"));
        assertEquals(List.of("5"), query.getValue().get("offset"));
    }

    @Test
    void listOverloadsSendEmptyQueryMapsAndCloseTransport() {
        when(httpClient.request(anyString(), anyString(), isNull(), isNull(), anyMap()))
                .thenReturn(Mono.just(success));

        assertSame(success, client.listMcpGateways());
        assertSame(success, client.listMcpGatewayTargets("gateway"));
        client.close();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, List<String>>> query = ArgumentCaptor.forClass(Map.class);
        verify(httpClient).request(eq("GET"), eq("/gateways"), isNull(), isNull(), query.capture());
        assertTrue(query.getValue().isEmpty());
        verify(httpClient).request(eq("GET"), eq("/gateways/gateway/targets"),
                isNull(), isNull(), query.capture());
        assertTrue(query.getValue().isEmpty());
        verify(httpClient).close();
    }
}
