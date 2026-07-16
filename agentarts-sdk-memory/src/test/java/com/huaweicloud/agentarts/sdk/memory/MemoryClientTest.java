package com.huaweicloud.agentarts.sdk.memory;

import com.huaweicloud.agentarts.sdk.core.APIException;
import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import com.huaweicloud.agentarts.sdk.memory.model.CreateMemorySessionRequest;
import com.huaweicloud.agentarts.sdk.memory.model.CreateSpaceRequest;
import com.huaweicloud.agentarts.sdk.memory.model.MemoryListFilter;
import com.huaweicloud.agentarts.sdk.memory.model.UpdateSpaceRequest;
import com.huaweicloud.agentarts.sdk.service.http.BaseHttpClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MemoryClientTest {

    private BaseHttpClient controlPlane;
    private BaseHttpClient dataPlane;
    private MemoryClient client;

    @BeforeEach
    void setUp() {
        controlPlane = mock(BaseHttpClient.class);
        dataPlane = mock(BaseHttpClient.class);
        client = new MemoryClient("test-region", "key", true, controlPlane, dataPlane);
    }

    @Test
    void executesControlPlaneOperationsAndParsesResponses() throws Exception {
        when(controlPlane.request(eq("GET"), eq("/spaces"), isNull(), isNull(), anyMap()))
                .thenReturn(Mono.just(success("{\"spaces\":[],\"total\":0,\"size\":20,\"offset\":0}")));
        when(controlPlane.post(eq("/space-keys"), isNull(), any()))
                .thenReturn(Mono.just(success("{\"id\":\"key-id\",\"api_key\":\"key\"}")));
        when(controlPlane.put(eq("/spaces/space"), isNull(), any()))
                .thenReturn(Mono.just(success("{\"id\":\"space\",\"name\":\"updated\"}")));

        var spaces = client.listSpaces();
        assertNotNull(spaces);
        assertEquals(0, spaces.getTotal());
        assertEquals("key-id", client.createApiKey().getId());
        assertEquals("updated", client.updateSpace("space", "updated", null, null).getName());
        assertEquals("test-region", client.getRegionName());
    }

    @Test
    void sendsCompleteCreateSpaceConfiguration() throws Exception {
        when(controlPlane.post(eq("/space-keys"), isNull(), any()))
                .thenReturn(Mono.just(success("{\"id\":\"generated-id\",\"api_key\":\"generated-value\"}")));
        when(controlPlane.post(eq("/spaces"), isNull(), any()))
                .thenReturn(Mono.just(success("{\"id\":\"space-id\"}")));

        var created = client.createSpace(
                "production-space",
                336,
                "production configuration",
                List.of(Map.of("key", "environment", "value", "production")),
                60,
                4000,
                100,
                false,
                "vpc-id",
                "subnet-id",
                List.of("semantic"),
                List.of(Map.of("name", "custom")));

        ArgumentCaptor<CreateSpaceRequest> request = ArgumentCaptor.forClass(CreateSpaceRequest.class);
        verify(controlPlane).post(eq("/spaces"), isNull(), request.capture());
        CreateSpaceRequest body = request.getValue();
        assertEquals("space-id", created.getId());
        assertEquals("generated-value", created.getApiKey());
        assertEquals("generated-id", created.getApiKeyId());
        assertEquals("generated-id", body.getApiKeyId());
        assertEquals(60, body.getMemoryExtractIdleSeconds());
        assertEquals(List.of("semantic"), body.getMemoryStrategiesBuiltin());
        assertEquals(false, body.getNetworkAccess().get("public_access_enable"));
        @SuppressWarnings("unchecked")
        Map<String, Object> privateAccess =
                (Map<String, Object>) body.getNetworkAccess().get("private_access_config");
        assertEquals("vpc-id", privateAccess.get("vpc_id"));
        assertEquals("subnet-id", privateAccess.get("subnet_id"));
    }

    @Test
    void basicCreateSpaceUsesPythonDefaults() throws Exception {
        when(controlPlane.post(eq("/space-keys"), isNull(), any()))
                .thenReturn(Mono.just(success("{\"id\":\"generated-id\"}")));
        when(controlPlane.post(eq("/spaces"), isNull(), any()))
                .thenReturn(Mono.just(success("{\"id\":\"space-id\"}")));

        client.createSpace("basic-space");

        ArgumentCaptor<CreateSpaceRequest> request = ArgumentCaptor.forClass(CreateSpaceRequest.class);
        verify(controlPlane).post(eq("/spaces"), isNull(), request.capture());
        assertNull(request.getValue().getMemoryStrategiesBuiltin());
        assertEquals(true, request.getValue().getNetworkAccess().get("public_access_enable"));
    }

    @Test
    void sendsCompleteUpdateSpaceConfiguration() throws Exception {
        when(controlPlane.put(eq("/spaces/space-id"), isNull(), any()))
                .thenReturn(Mono.just(success("{\"id\":\"space-id\"}")));

        client.updateSpace(
                "space-id",
                "renamed",
                "updated",
                336,
                true,
                60,
                4000,
                100,
                List.of(Map.of("key", "environment", "value", "production")),
                List.of("semantic"));

        ArgumentCaptor<UpdateSpaceRequest> request = ArgumentCaptor.forClass(UpdateSpaceRequest.class);
        verify(controlPlane).put(eq("/spaces/space-id"), isNull(), request.capture());
        UpdateSpaceRequest body = request.getValue();
        assertTrue(body.getMemoryExtractEnabled());
        assertEquals(60, body.getMemoryExtractIdleSeconds());
        assertEquals(4000, body.getMemoryExtractMaxTokens());
        assertEquals(100, body.getMemoryExtractMaxMessages());
        assertEquals(List.of("semantic"), body.getMemoryStrategiesBuiltin());
        assertEquals("production", body.getTags().get(0).get("value"));
    }

    @Test
    void executesDataPlaneOperationsAndEncodesFilters() throws Exception {
        when(dataPlane.post(eq("/spaces/space/sessions"), isNull(), any()))
                .thenReturn(Mono.just(success("{\"id\":\"session\"}")));
        when(dataPlane.request(eq("GET"), eq("/spaces/space/memories"),
                isNull(), isNull(), anyMap()))
                .thenReturn(Mono.just(success("{\"memories\":[],\"total\":0}")));

        assertEquals("session",
                client.createMemorySession("space", "session", "actor", "assistant").getId());

        MemoryListFilter filter = new MemoryListFilter();
        filter.setActorId("actor&limit=999");
        assertNotNull(client.listMemories("space", 5, 2, filter));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, List<String>>> query = ArgumentCaptor.forClass(Map.class);
        verify(dataPlane).request(eq("GET"), eq("/spaces/space/memories"),
                isNull(), isNull(), query.capture());
        assertEquals(List.of("actor&limit=999"), query.getValue().get("actor_id"));
        assertEquals(List.of("5"), query.getValue().get("limit"));
        assertEquals(List.of("2"), query.getValue().get("offset"));
    }

    @Test
    void sendsSessionMetadataInSyncAndAsyncCalls() throws Exception {
        when(dataPlane.post(eq("/spaces/space/sessions"), isNull(), any()))
                .thenReturn(Mono.just(success("{\"id\":\"session\"}")));

        Map<String, Object> metadata = Map.of("tenant", "example");
        client.createMemorySession("space", "session", "actor", "assistant", metadata);
        client.createMemorySessionAsync("space", "session", "actor", "assistant", metadata).block();

        ArgumentCaptor<CreateMemorySessionRequest> request =
                ArgumentCaptor.forClass(CreateMemorySessionRequest.class);
        verify(dataPlane, times(2)).post(eq("/spaces/space/sessions"), isNull(), request.capture());
        assertEquals(metadata, request.getAllValues().get(0).getMeta());
        assertEquals(metadata, request.getAllValues().get(1).getMeta());
    }

    @Test
    void rejectsEmptyAndUnsupportedMessagesBeforeTransport() {
        assertThrows(
                IllegalArgumentException.class,
                () -> client.addMessages("space", "session", List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> client.addMessages("space", "session", List.of("unsupported")));
        assertThrows(
                IllegalArgumentException.class,
                () -> client.addMessages("space", "session", java.util.Arrays.asList((Object) null)));
        assertThrows(
                IllegalArgumentException.class,
                () -> client.addMessages(
                        "space", "session", java.util.Collections.nCopies(101, "unsupported")));

        verifyNoInteractions(dataPlane);
    }

    @Test
    void failsClosedForUnsuccessfulDeletes() {
        when(dataPlane.delete("/spaces/space/memories/memory"))
                .thenReturn(Mono.just(RequestResult.builder()
                        .success(false).statusCode(403).error("denied").build()));

        APIException error = assertThrows(APIException.class,
                () -> client.deleteMemory("space", "memory"));
        assertEquals(403, error.getStatusCode());
    }

    @Test
    void closesInjectedTransportsIdempotently() {
        client.close();
        client.close();

        verify(controlPlane).close();
        verify(dataPlane).close();
    }

    private static RequestResult success(String json) throws Exception {
        return RequestResult.builder()
                .success(true)
                .statusCode(200)
                .data(JsonUtils.MAPPER.readTree(json))
                .build();
    }
}
