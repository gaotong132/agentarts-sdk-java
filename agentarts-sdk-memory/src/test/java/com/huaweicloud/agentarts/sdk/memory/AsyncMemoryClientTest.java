package com.huaweicloud.agentarts.sdk.memory;

import com.huaweicloud.agentarts.sdk.memory.model.MemoryInfo;
import com.huaweicloud.agentarts.sdk.memory.model.MemoryListResponse;
import com.huaweicloud.agentarts.sdk.memory.model.MessageBatchResponse;
import com.huaweicloud.agentarts.sdk.memory.model.MessageInfo;
import com.huaweicloud.agentarts.sdk.memory.model.MessageListResponse;
import com.huaweicloud.agentarts.sdk.memory.model.SessionInfo;
import com.huaweicloud.agentarts.sdk.memory.model.SpaceInfo;
import com.huaweicloud.agentarts.sdk.memory.model.SpaceListResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncMemoryClientTest {

    @Test
    void controlPlaneMethodsRemainSynchronous() {
        MemoryClient delegate = mock(MemoryClient.class);
        AsyncMemoryClient client = new AsyncMemoryClient(delegate);
        SpaceInfo space = new SpaceInfo();
        SpaceListResponse spaces = new SpaceListResponse();
        when(delegate.createSpace("sample")).thenReturn(space);
        when(delegate.getSpace("space")).thenReturn(space);
        when(delegate.listSpaces(5, 2)).thenReturn(spaces);
        when(delegate.updateSpace("space", "new", null, 24)).thenReturn(space);

        assertSame(space, client.createSpace("sample"));
        assertSame(space, client.getSpace("space"));
        assertSame(spaces, client.listSpaces(5, 2));
        assertSame(space, client.updateSpace("space", "new", null, 24));
        client.deleteSpace("space");

        verify(delegate).deleteSpace("space");
    }

    @Test
    void dataPlaneMethodsAreColdAndDelegateWithoutBlocking() {
        MemoryClient delegate = mock(MemoryClient.class);
        AsyncMemoryClient client = new AsyncMemoryClient(delegate);
        SessionInfo session = new SessionInfo();
        session.setId("session");
        when(delegate.createMemorySessionAsync(
                "space", "session", "actor", "assistant", Map.of("key", "value")))
                .thenReturn(Mono.just(session));

        Mono<SessionInfo> operation = client.createMemorySession(
                "space", "session", "actor", "assistant", Map.of("key", "value"));
        verify(delegate, never()).createMemorySessionAsync(
                "space", "session", "actor", "assistant", Map.of("key", "value"));
        assertSame(session, operation.block());
    }

    @Test
    void exposesTheCompleteDataPlaneSurface() {
        MemoryClient delegate = mock(MemoryClient.class);
        AsyncMemoryClient client = new AsyncMemoryClient(delegate);
        MessageInfo message = new MessageInfo();
        MessageBatchResponse batch = new MessageBatchResponse();
        MessageListResponse messages = new MessageListResponse();
        MemoryInfo memory = new MemoryInfo();
        MemoryListResponse memories = new MemoryListResponse();
        when(delegate.getLastKMessagesAsync("session", 3, "space"))
                .thenReturn(Mono.just(List.of(message)));
        when(delegate.getMessageAsync("message", "space", "session"))
                .thenReturn(Mono.just(message));
        when(delegate.addMessagesAsync("space", "session", List.of(message), null, null, false))
                .thenReturn(Mono.just(batch));
        when(delegate.listMessagesAsync("space", "session", 10, 0))
                .thenReturn(Mono.just(messages));
        when(delegate.listMemoriesAsync("space", 10, 0, null))
                .thenReturn(Mono.just(memories));
        when(delegate.getMemoryAsync("space", "memory")).thenReturn(Mono.just(memory));
        when(delegate.deleteMemoryAsync("space", "memory")).thenReturn(Mono.empty());

        assertEquals(1, client.getLastKMessages("session", 3, "space").block().size());
        assertSame(message, client.getMessage("message", "space", "session").block());
        assertSame(batch, client.addMessages("space", "session", List.of(message)).block());
        assertSame(messages, client.listMessages("space", "session", 10, 0).block());
        assertSame(memories, client.listMemories("space").block());
        assertSame(memory, client.getMemory("space", "memory").block());
        client.deleteMemory("space", "memory").block();
    }

    @Test
    void closeIsColdIdempotentAndRejectsLaterOperations() {
        MemoryClient delegate = mock(MemoryClient.class);
        when(delegate.getRegionName()).thenReturn("cn-test-1");
        AsyncMemoryClient client = new AsyncMemoryClient(delegate);

        assertEquals("cn-test-1", client.getRegionName());
        Mono<Void> close = client.closeAsync();
        verify(delegate, never()).close();
        close.block();
        client.close();
        verify(delegate, times(1)).close();

        assertThrows(IllegalStateException.class, () -> client.getSpace("space"));
        assertThrows(IllegalStateException.class,
                () -> client.getMemory("space", "memory").block());
        assertFalse(client.closeAsync().hasElement().block());
    }
}
