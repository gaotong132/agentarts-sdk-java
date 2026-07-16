package com.huaweicloud.agentarts.sdk.memory;

import com.huaweicloud.agentarts.sdk.core.APIException;
import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import com.huaweicloud.agentarts.sdk.memory.model.MemoryListFilter;
import com.huaweicloud.agentarts.sdk.service.http.BaseHttpClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

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
        when(controlPlane.get("/spaces?limit=20&offset=0"))
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
    void executesDataPlaneOperationsAndEncodesFilters() throws Exception {
        when(dataPlane.post(eq("/spaces/space/sessions"), isNull(), any()))
                .thenReturn(Mono.just(success("{\"id\":\"session\"}")));
        when(dataPlane.get("/spaces/space/memories?limit=5&offset=2&actor_id=actor"))
                .thenReturn(Mono.just(success("{\"memories\":[],\"total\":0}")));

        assertEquals("session",
                client.createMemorySession("space", "session", "actor", "assistant").getId());

        MemoryListFilter filter = new MemoryListFilter();
        filter.setActorId("actor");
        assertNotNull(client.listMemories("space", 5, 2, filter));
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
