package com.huaweicloud.agentarts.sdk.service.iam;

import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.iam.v5.IamAsyncClient;
import com.huaweicloud.sdk.iam.v5.IamClient;
import com.huaweicloud.sdk.iam.v5.model.Agency;
import com.huaweicloud.sdk.iam.v5.model.AttachAgencyPolicyV5Request;
import com.huaweicloud.sdk.iam.v5.model.CreateAgencyV5Request;
import com.huaweicloud.sdk.iam.v5.model.CreateAgencyV5Response;
import com.huaweicloud.sdk.iam.v5.model.ListAgenciesV5Request;
import com.huaweicloud.sdk.iam.v5.model.ListAgenciesV5Response;
import com.huaweicloud.sdk.iam.v5.model.ListPoliciesV5Request;
import com.huaweicloud.sdk.iam.v5.model.ListPoliciesV5Response;
import com.huaweicloud.sdk.iam.v5.model.PageInfo;
import com.huaweicloud.sdk.iam.v5.model.Policy;
import com.huaweicloud.sdk.iam.v5.model.TrustAgency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IAMServiceClientTest {

    private IamClient syncClient;
    private IamAsyncClient asyncClient;
    private IAMServiceClient client;

    @BeforeEach
    void setUp() {
        syncClient = mock(IamClient.class);
        asyncClient = mock(IamAsyncClient.class);
        client = new IAMServiceClient(syncClient, asyncClient, "cn-test-1");
    }

    @Test
    void createAgencyUsesPythonCompatibleDefaults() {
        CreateAgencyV5Response response = new CreateAgencyV5Response();
        when(syncClient.createAgencyV5(any())).thenReturn(response);

        assertSame(response, client.createAgency("agency", "{}"));

        ArgumentCaptor<CreateAgencyV5Request> captor =
                ArgumentCaptor.forClass(CreateAgencyV5Request.class);
        verify(syncClient).createAgencyV5(captor.capture());
        assertEquals("", captor.getValue().getBody().getPath());
        assertEquals(3600, captor.getValue().getBody().getMaxSessionDuration());
    }

    @Test
    void listAndAttachOperationsMapAllSupportedParameters() {
        when(syncClient.listPoliciesV5(any())).thenReturn(new ListPoliciesV5Response());
        when(syncClient.listAgenciesV5(any())).thenReturn(new ListAgenciesV5Response());

        client.listPolicies("system", 50, "next", "/service/", true);
        client.listAgencies(25, "agency-next", "/agents/");
        client.attachAgencyPolicy("agency-id", "policy-id");

        ArgumentCaptor<ListPoliciesV5Request> policies =
                ArgumentCaptor.forClass(ListPoliciesV5Request.class);
        verify(syncClient).listPoliciesV5(policies.capture());
        assertEquals("system", policies.getValue().getPolicyType().getValue());
        assertEquals(50, policies.getValue().getLimit());
        assertEquals("next", policies.getValue().getMarker());
        assertEquals("/service/", policies.getValue().getPathPrefix());
        assertEquals(true, policies.getValue().getOnlyAttached());

        ArgumentCaptor<ListAgenciesV5Request> agencies =
                ArgumentCaptor.forClass(ListAgenciesV5Request.class);
        verify(syncClient).listAgenciesV5(agencies.capture());
        assertEquals(25, agencies.getValue().getLimit());
        assertEquals("agency-next", agencies.getValue().getMarker());
        assertEquals("/agents/", agencies.getValue().getPathPrefix());

        ArgumentCaptor<AttachAgencyPolicyV5Request> attach =
                ArgumentCaptor.forClass(AttachAgencyPolicyV5Request.class);
        verify(syncClient).attachAgencyPolicyV5(attach.capture());
        assertEquals("policy-id", attach.getValue().getPolicyId());
        assertEquals("agency-id", attach.getValue().getBody().getAgencyId());
    }

    @Test
    void asyncOperationsUseAsyncClient() {
        CompletableFuture<ListPoliciesV5Response> policies =
                CompletableFuture.completedFuture(new ListPoliciesV5Response());
        CompletableFuture<ListAgenciesV5Response> agencies =
                CompletableFuture.completedFuture(new ListAgenciesV5Response());
        when(asyncClient.listPoliciesV5Async(any())).thenReturn(policies);
        when(asyncClient.listAgenciesV5Async(any())).thenReturn(agencies);

        assertSame(policies, client.listPoliciesAsync(null, null, null, null, null));
        assertSame(agencies, client.listAgenciesAsync(null, null, null));
    }

    @Test
    void createsAgencyAndAttachesNamedSystemPolicy() {
        CreateAgencyV5Response created = new CreateAgencyV5Response()
                .withAgency(new TrustAgency().withAgencyId("agency-id"));
        when(syncClient.createAgencyV5(any())).thenReturn(created);
        when(syncClient.listPoliciesV5(any())).thenReturn(new ListPoliciesV5Response()
                .withPolicies(List.of(new Policy()
                        .withPolicyName("policy-name").withPolicyId("policy-id"))));

        assertSame(created, client.createAgencyWithPolicy(
                "agency-name", "{}", "policy-name"));

        ArgumentCaptor<AttachAgencyPolicyV5Request> attach =
                ArgumentCaptor.forClass(AttachAgencyPolicyV5Request.class);
        verify(syncClient).attachAgencyPolicyV5(attach.capture());
        assertEquals("agency-id", attach.getValue().getBody().getAgencyId());
        assertEquals("policy-id", attach.getValue().getPolicyId());
    }

    @Test
    void existingAgencyAndAttachmentAreHandledIdempotentlyAcrossPages() {
        when(syncClient.createAgencyV5(any()))
                .thenThrow(new ServiceResponseException(409, "conflict", "", ""));
        when(syncClient.listAgenciesV5(any()))
                .thenReturn(new ListAgenciesV5Response()
                                .withAgencies(List.of(new Agency().withAgencyName("other")))
                                .withPageInfo(new PageInfo().withNextMarker("agency-page-2")),
                        new ListAgenciesV5Response().withAgencies(List.of(new Agency()
                                .withAgencyName("agency-name").withAgencyId("agency-id"))));
        when(syncClient.listPoliciesV5(any()))
                .thenReturn(new ListPoliciesV5Response()
                                .withPolicies(List.of())
                                .withPageInfo(new PageInfo().withNextMarker("policy-page-2")),
                        new ListPoliciesV5Response().withPolicies(List.of(new Policy()
                                .withPolicyName("policy-name").withPolicyId("policy-id"))));
        when(syncClient.attachAgencyPolicyV5(any()))
                .thenThrow(new ServiceResponseException(409, "conflict", "", ""));

        assertNull(client.createAgencyWithPolicy("agency-name", "{}", "policy-name"));
        verify(syncClient, times(2)).listAgenciesV5(any());
        verify(syncClient, times(2)).listPoliciesV5(any());
    }

    @Test
    void failsFastOnRepeatedPaginationMarker() {
        when(syncClient.createAgencyV5(any()))
                .thenThrow(new ServiceResponseException(409, "conflict", "", ""));
        when(syncClient.listAgenciesV5(any())).thenReturn(new ListAgenciesV5Response()
                .withAgencies(List.of())
                .withPageInfo(new PageInfo().withNextMarker("same")));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> client.createAgencyWithPolicy("agency-name", "{}", "policy-name"));
        assertEquals("IAM pagination returned a repeated marker", error.getMessage());
        verify(syncClient, times(2)).listAgenciesV5(any());
    }

    @Test
    void rejectsMissingPolicyAndPropagatesNonConflictResponses() {
        CreateAgencyV5Response created = new CreateAgencyV5Response()
                .withAgency(new TrustAgency().withAgencyId("agency-id"));
        when(syncClient.createAgencyV5(any()))
                .thenReturn(created)
                .thenThrow(new ServiceResponseException(403, "forbidden", "", ""));
        when(syncClient.listPoliciesV5(any()))
                .thenReturn(new ListPoliciesV5Response().withPolicies(List.of()));

        assertThrows(IllegalArgumentException.class,
                () -> client.createAgencyWithPolicy("agency", "{}", "missing"));
        assertThrows(ServiceResponseException.class,
                () -> client.createAgencyWithPolicy("agency", "{}", "policy"));
    }
}
