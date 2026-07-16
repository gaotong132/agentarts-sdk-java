package com.huaweicloud.agentarts.sdk.service.swr;

import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.swr.v2.SwrAsyncClient;
import com.huaweicloud.sdk.swr.v2.SwrClient;
import com.huaweicloud.sdk.swr.v2.model.CreateNamespaceRequest;
import com.huaweicloud.sdk.swr.v2.model.CreateRepoRequest;
import com.huaweicloud.sdk.swr.v2.model.ShowNamespaceRequest;
import com.huaweicloud.sdk.swr.v2.model.ShowNamespaceResponse;
import com.huaweicloud.sdk.swr.v2.model.ShowRepositoryRequest;
import com.huaweicloud.sdk.swr.v2.model.ShowRepositoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SWRServiceClientTest {

    private SwrClient syncClient;
    private SWRServiceClient client;

    @BeforeEach
    void setUp() {
        syncClient = mock(SwrClient.class);
        client = new SWRServiceClient(
                syncClient, mock(SwrAsyncClient.class), "cn-test-1");
    }

    @Test
    void existingNamespaceIsNotCreated() {
        when(syncClient.showNamespace(any())).thenReturn(new ShowNamespaceResponse());

        assertFalse(client.createNamespaceIfNotExists("organization"));
        verify(syncClient, never()).createNamespace(any());
    }

    @Test
    void missingNamespaceIsCreatedAndConcurrentConflictIsIdempotent() {
        when(syncClient.showNamespace(any()))
                .thenThrow(new ServiceResponseException(404, "not found", "", ""));

        assertTrue(client.createNamespaceIfNotExists("organization"));
        ArgumentCaptor<CreateNamespaceRequest> request =
                ArgumentCaptor.forClass(CreateNamespaceRequest.class);
        verify(syncClient).createNamespace(request.capture());
        assertEquals("organization", request.getValue().getBody().getNamespace());

        when(syncClient.createNamespace(any()))
                .thenThrow(new ServiceResponseException(409, "conflict", "", ""));
        assertFalse(client.createNamespaceIfNotExists("organization"));
    }

    @Test
    void namespaceLookupDoesNotTreatAuthorizationOrNetworkFailuresAsMissing() {
        ServiceResponseException forbidden =
                new ServiceResponseException(403, "forbidden", "", "");
        when(syncClient.showNamespace(any())).thenThrow(forbidden);

        assertSame(forbidden, assertThrows(ServiceResponseException.class,
                () -> client.createNamespaceIfNotExists("organization")));
        verify(syncClient, never()).createNamespace(any());
    }

    @Test
    void missingRepositoryIsCreatedWithPrivateVisibility() {
        when(syncClient.showRepository(any()))
                .thenThrow(new ServiceResponseException(404, "not found", "", ""));

        assertTrue(client.createRepoIfNotExists("organization", "repository"));

        ArgumentCaptor<CreateRepoRequest> request =
                ArgumentCaptor.forClass(CreateRepoRequest.class);
        verify(syncClient).createRepo(request.capture());
        assertEquals("organization", request.getValue().getNamespace());
        assertEquals("repository", request.getValue().getBody().getRepository());
        assertFalse(request.getValue().getBody().getIsPublic());
    }

    @Test
    void repositoryLookupDoesNotTreatServerFailuresAsMissing() {
        ServiceResponseException unavailable =
                new ServiceResponseException(503, "unavailable", "", "");
        when(syncClient.showRepository(any())).thenThrow(unavailable);

        assertSame(unavailable, assertThrows(ServiceResponseException.class,
                () -> client.createRepoIfNotExists("organization", "repository")));
        verify(syncClient, never()).createRepo(any());
    }

    @Test
    void pythonCompatibleConvenienceOperationsReturnCurrentResources() {
        ShowNamespaceResponse organization = new ShowNamespaceResponse();
        ShowRepositoryResponse repository = new ShowRepositoryResponse();
        when(syncClient.showNamespace(any())).thenReturn(organization);
        when(syncClient.showRepository(any())).thenReturn(repository);

        assertSame(organization, client.getOrganization("organization"));
        assertSame(organization, client.createOrGetOrganization("organization"));
        assertSame(repository, client.getRepository("organization", "repository"));
        assertSame(repository,
                client.createOrGetRepository("organization", "repository"));
        assertEquals("swr.cn-test-1.myhuaweicloud.com/organization/repository:latest",
                client.getFullImageName("organization", "repository"));
    }

    @Test
    void createRepositoryMapsRequestedVisibilityAndReturnsCurrentResource() {
        ShowRepositoryResponse repository = new ShowRepositoryResponse();
        when(syncClient.showRepository(any())).thenReturn(repository);

        assertSame(repository,
                client.createRepository("organization", "repository", true));

        ArgumentCaptor<CreateRepoRequest> request =
                ArgumentCaptor.forClass(CreateRepoRequest.class);
        verify(syncClient).createRepo(request.capture());
        assertTrue(request.getValue().getBody().getIsPublic());
    }
}
