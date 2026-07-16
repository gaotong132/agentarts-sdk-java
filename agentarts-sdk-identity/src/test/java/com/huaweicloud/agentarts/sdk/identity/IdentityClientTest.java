package com.huaweicloud.agentarts.sdk.identity;

import com.huaweicloud.agentarts.sdk.service.identity.IdentityServiceClient;
import com.huaweicloud.sdk.agentidentity.v1.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IdentityClientTest {

    private IdentityServiceClient serviceClient;
    private IdentityClient client;

    @BeforeEach
    void setUp() {
        serviceClient = mock(IdentityServiceClient.class);
        when(serviceClient.getRegion()).thenReturn("test-region");
        client = new IdentityClient(serviceClient);
    }

    @Test
    void delegatesWorkloadIdentityOperationsWithCompleteRequests() {
        var createResponse = new CreateWorkloadIdentityResponse();
        var getResponse = new GetWorkloadIdentityResponse();
        var updateResponse = new UpdateWorkloadIdentityResponse();
        var listResponse = new ListWorkloadIdentitiesResponse();
        var deleteResponse = new DeleteWorkloadIdentityResponse();
        when(serviceClient.createWorkloadIdentity(any())).thenReturn(createResponse);
        when(serviceClient.getWorkloadIdentity(any())).thenReturn(getResponse);
        when(serviceClient.updateWorkloadIdentity(any())).thenReturn(updateResponse);
        when(serviceClient.listWorkloadIdentities(any())).thenReturn(listResponse);
        when(serviceClient.deleteWorkloadIdentity(any())).thenReturn(deleteResponse);

        assertSame(createResponse, client.createWorkloadIdentity("workload"));
        assertSame(getResponse, client.getWorkloadIdentity("workload"));
        assertSame(updateResponse, client.updateWorkloadIdentity("workload", List.of("https://callback")));
        assertSame(listResponse, client.listWorkloadIdentities());
        assertSame(deleteResponse, client.deleteWorkloadIdentity("workload"));

        var create = ArgumentCaptor.forClass(CreateWorkloadIdentityRequest.class);
        verify(serviceClient).createWorkloadIdentity(create.capture());
        assertEquals("workload", create.getValue().getBody().getName());
        assertEquals(AuthorizerType.NONE, create.getValue().getBody().getAuthorizerType());

        var update = ArgumentCaptor.forClass(UpdateWorkloadIdentityRequest.class);
        verify(serviceClient).updateWorkloadIdentity(update.capture());
        assertEquals("workload", update.getValue().getWorkloadIdentityName());
        assertEquals(List.of("https://callback"),
                update.getValue().getBody().getAllowedResourceOauth2ReturnUrls());
    }

    @Test
    void delegatesCredentialProviderLists() {
        var apiKeys = new ListApiKeyCredentialProvidersResponse();
        var oauth2 = new ListOauth2CredentialProvidersResponse();
        var sts = new ListStsCredentialProvidersResponse();
        when(serviceClient.listApiKeyCredentialProviders(any())).thenReturn(apiKeys);
        when(serviceClient.listOauth2CredentialProviders(any())).thenReturn(oauth2);
        when(serviceClient.listStsCredentialProviders(any())).thenReturn(sts);

        assertSame(apiKeys, client.listApiKeyCredentialProviders());
        assertSame(oauth2, client.listOauth2CredentialProviders());
        assertSame(sts, client.listStsCredentialProviders());
    }

    @Test
    void returnsWorkloadTokensFromAllSupportedFlows() {
        when(serviceClient.createWorkloadAccessToken(any()))
                .thenReturn(new CreateWorkloadAccessTokenResponse().withWorkloadAccessToken("token"));
        when(serviceClient.createWorkloadAccessTokenForUserId(any()))
                .thenReturn(new CreateWorkloadAccessTokenForUserIdResponse().withWorkloadAccessToken("user-token"));
        when(serviceClient.createWorkloadAccessTokenForJwt(any()))
                .thenReturn(new CreateWorkloadAccessTokenForJwtResponse().withWorkloadAccessToken("jwt-token"));

        assertEquals("token", client.createWorkloadAccessToken("workload"));
        assertEquals("user-token", client.createWorkloadAccessTokenForUserId("workload", "user"));
        assertEquals("jwt-token", client.createWorkloadAccessTokenForJwt("workload", "jwt"));
    }

    @Test
    void buildsApiKeyAndStsCredentialProviderRequests() {
        var apiResponse = new CreateApiKeyCredentialProviderResponse();
        var stsResponse = new CreateStsCredentialProviderResponse();
        when(serviceClient.createApiKeyCredentialProvider(any())).thenReturn(apiResponse);
        when(serviceClient.createStsCredentialProvider(any())).thenReturn(stsResponse);

        assertSame(apiResponse, client.createApiKeyCredentialProvider("provider", "key"));
        assertSame(stsResponse, client.createStsCredentialProvider("provider", "agency"));

        var apiRequest = ArgumentCaptor.forClass(CreateApiKeyCredentialProviderRequest.class);
        verify(serviceClient).createApiKeyCredentialProvider(apiRequest.capture());
        assertEquals("provider", apiRequest.getValue().getBody().getName());
        assertEquals("key", apiRequest.getValue().getBody().getApiKey());
    }

    @Test
    void buildsEveryOauth2VendorConfigurationAndRejectsUnknownVendor() {
        when(serviceClient.createOauth2CredentialProvider(any()))
                .thenReturn(new CreateOauth2CredentialProviderResponse());

        client.createOauth2CredentialProvider(
                "github", CredentialProviderVendor.GITHUBOAUTH2, "client", "secret");
        client.createOauth2CredentialProvider(
                "google", CredentialProviderVendor.GOOGLEOAUTH2, "client", "secret");
        client.createOauth2CredentialProvider(
                "microsoft", CredentialProviderVendor.MICROSOFTOAUTH2,
                "client", "secret", "tenant", null);
        client.createOauth2CredentialProvider(
                "custom", CredentialProviderVendor.CUSTOMOAUTH2,
                "client", "secret", null, new Oauth2Discovery());

        verify(serviceClient, times(4)).createOauth2CredentialProvider(any());
        assertThrows(IllegalArgumentException.class,
                () -> client.createOauth2CredentialProvider(
                        "invalid", null, "client", "secret", null, null));
    }

    @Test
    void resolvesApiKeyValueAndFailsClosedForEmptyResponses() {
        when(serviceClient.getResourceApiKey(any()))
                .thenReturn(new GetResourceApiKeyResponse().withApiKey("key"));
        assertEquals("key", client.getResourceApiKeyValue("provider", "token"));

        when(serviceClient.getResourceApiKey(any())).thenReturn(null);
        assertThrows(IllegalStateException.class,
                () -> client.getResourceApiKeyValue("provider", "token"));

        when(serviceClient.getResourceApiKey(any()))
                .thenReturn(new GetResourceApiKeyResponse().withApiKey(" "));
        assertThrows(IllegalStateException.class,
                () -> client.getResourceApiKeyValue("provider", "token"));
    }

    @Test
    void resolvesImmediateOauth2TokenAndRejectsIncompleteAuthorizationResponse() {
        when(serviceClient.getResourceOauth2Token(any()))
                .thenReturn(new GetResourceOauth2TokenResponse().withAccessToken("access-token"));
        assertEquals("access-token", client.getResourceOauth2AccessToken(
                "provider", "token", GetResourceOauth2TokenRequestBody.Oauth2FlowEnum.M2M,
                List.of("scope"), "https://callback", false));

        when(serviceClient.getResourceOauth2Token(any()))
                .thenReturn(new GetResourceOauth2TokenResponse());
        assertThrows(IllegalStateException.class, () -> client.getResourceOauth2AccessToken(
                "provider", "token", GetResourceOauth2TokenRequestBody.Oauth2FlowEnum.USER_FEDERATION,
                List.of("scope"), "https://callback", true));
    }

    @Test
    void resolvesStsCredentialsAndFailsClosedForEmptyResponses() {
        var credentials = new GetResourceStsTokenResponseBodyCredentials();
        when(serviceClient.getResourceStsToken(any()))
                .thenReturn(new GetResourceStsTokenResponse().withCredentials(credentials));

        assertSame(credentials, client.getResourceStsCredentials(
                "provider", "token", "session", 900, "policy", "source",
                List.of(), List.of()));

        when(serviceClient.getResourceStsToken(any())).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> client.getResourceStsCredentials(
                "provider", "token", "session", null, null, null, null, null));
    }

    @Test
    @SuppressWarnings("deprecation")
    void completesAuthorizationOnlyWithExplicitUserIdentity() {
        var response = new CompleteResourceTokenAuthResponse();
        when(serviceClient.completeResourceTokenAuth(any())).thenReturn(response);
        var user = new UserIdentifier();

        assertSame(response, client.completeResourceTokenAuth("session", user));
        assertThrows(IllegalArgumentException.class,
                () -> client.completeResourceTokenAuth("session"));
        assertSame(serviceClient, client.getServiceClient());
        assertEquals("test-region", client.getRegion());
    }
}
