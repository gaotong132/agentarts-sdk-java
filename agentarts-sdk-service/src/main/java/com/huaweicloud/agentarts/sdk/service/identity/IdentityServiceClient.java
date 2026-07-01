package com.huaweicloud.agentarts.sdk.service.identity;

import com.huaweicloud.sdk.agentidentity.v1.AgentIdentityClient;
import com.huaweicloud.sdk.agentidentity.v1.AgentIdentityAsyncClient;
import com.huaweicloud.sdk.agentidentity.v1.model.*;
import com.huaweicloud.sdk.agentidentity.v1.region.AgentIdentityRegion;
import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.GlobalCredentials;
import com.huaweicloud.sdk.core.http.HttpConfig;
import com.huaweicloud.sdk.core.ClientBuilder;
import com.huaweicloud.agentarts.sdk.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Service client wrapping Huawei Cloud {@link AgentIdentityClient}.
 *
 * <p>Service client wrapping Huawei Cloud AgentIdentity API operations.
 * Wraps all 31 API methods from the Huawei Cloud Java SDK AgentIdentityClient,
 * providing both sync and async invocation patterns.</p>
 *
 * <p>The underlying client uses SDK-HMAC-SHA256 signing via Huawei Cloud SDK's
 * built-in credential mechanism.</p>
 */
public class IdentityServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(IdentityServiceClient.class);

    private final AgentIdentityClient syncClient;
    private final AgentIdentityAsyncClient asyncClient;
    private final String region;
    private final boolean ignoreSslVerification;

    /**
     * Create an IdentityServiceClient.
     *
     * @param region                Huawei Cloud region (e.g., "cn-southwest-2")
     * @param ignoreSslVerification whether to ignore SSL certificate verification
     */
    public IdentityServiceClient(String region, boolean ignoreSslVerification) {
        this.region = region != null ? region : Constants.getRegion();
        this.ignoreSslVerification = ignoreSslVerification;

        HttpConfig httpConfig = HttpConfig.getDefaultHttpConfig();
        httpConfig.setIgnoreSSLVerification(ignoreSslVerification);

        String endpoint = Constants.getIdentityEndpoint(this.region);

        BasicCredentials credentials = new BasicCredentials()
                .withAk(Constants.getAk())
                .withSk(Constants.getSk());

        String securityToken = Constants.getSecurityToken();
        if (com.huaweicloud.agentarts.sdk.core.util.JsonUtils.isNotBlank(securityToken)) {
            credentials.withSecurityToken(securityToken);
        }

        this.syncClient = new ClientBuilder<>(AgentIdentityClient::new)
                .withCredential(credentials)
                .withHttpConfig(httpConfig)
                .withEndpoint(endpoint)
                .build();

        this.asyncClient = new ClientBuilder<>(AgentIdentityAsyncClient::new)
                .withCredential(credentials)
                .withHttpConfig(httpConfig)
                .withEndpoint(endpoint)
                .build();
    }

    public IdentityServiceClient(String region) {
        this(region, false);
    }

    public IdentityServiceClient() {
        this(null, false);
    }

    // ========================
    // Workload Identity CRUD (5 methods)
    // ========================

    public CreateWorkloadIdentityResponse createWorkloadIdentity(CreateWorkloadIdentityRequest request) {
        return syncClient.createWorkloadIdentity(request);
    }

    public CompletableFuture<CreateWorkloadIdentityResponse> createWorkloadIdentityAsync(CreateWorkloadIdentityRequest request) {
        return asyncClient.createWorkloadIdentityAsync(request);
    }

    public UpdateWorkloadIdentityResponse updateWorkloadIdentity(UpdateWorkloadIdentityRequest request) {
        return syncClient.updateWorkloadIdentity(request);
    }

    public CompletableFuture<UpdateWorkloadIdentityResponse> updateWorkloadIdentityAsync(UpdateWorkloadIdentityRequest request) {
        return asyncClient.updateWorkloadIdentityAsync(request);
    }

    public GetWorkloadIdentityResponse getWorkloadIdentity(GetWorkloadIdentityRequest request) {
        return syncClient.getWorkloadIdentity(request);
    }

    public CompletableFuture<GetWorkloadIdentityResponse> getWorkloadIdentityAsync(GetWorkloadIdentityRequest request) {
        return asyncClient.getWorkloadIdentityAsync(request);
    }

    public ListWorkloadIdentitiesResponse listWorkloadIdentities(ListWorkloadIdentitiesRequest request) {
        return syncClient.listWorkloadIdentities(request);
    }

    public CompletableFuture<ListWorkloadIdentitiesResponse> listWorkloadIdentitiesAsync(ListWorkloadIdentitiesRequest request) {
        return asyncClient.listWorkloadIdentitiesAsync(request);
    }

    public DeleteWorkloadIdentityResponse deleteWorkloadIdentity(DeleteWorkloadIdentityRequest request) {
        return syncClient.deleteWorkloadIdentity(request);
    }

    public CompletableFuture<DeleteWorkloadIdentityResponse> deleteWorkloadIdentityAsync(DeleteWorkloadIdentityRequest request) {
        return asyncClient.deleteWorkloadIdentityAsync(request);
    }

    // ========================
    // Workload Access Token (3 methods)
    // ========================

    public CreateWorkloadAccessTokenResponse createWorkloadAccessToken(CreateWorkloadAccessTokenRequest request) {
        return syncClient.createWorkloadAccessToken(request);
    }

    public CompletableFuture<CreateWorkloadAccessTokenResponse> createWorkloadAccessTokenAsync(CreateWorkloadAccessTokenRequest request) {
        return asyncClient.createWorkloadAccessTokenAsync(request);
    }

    public CreateWorkloadAccessTokenForJwtResponse createWorkloadAccessTokenForJwt(CreateWorkloadAccessTokenForJwtRequest request) {
        return syncClient.createWorkloadAccessTokenForJwt(request);
    }

    public CompletableFuture<CreateWorkloadAccessTokenForJwtResponse> createWorkloadAccessTokenForJwtAsync(CreateWorkloadAccessTokenForJwtRequest request) {
        return asyncClient.createWorkloadAccessTokenForJwtAsync(request);
    }

    public CreateWorkloadAccessTokenForUserIdResponse createWorkloadAccessTokenForUserId(CreateWorkloadAccessTokenForUserIdRequest request) {
        return syncClient.createWorkloadAccessTokenForUserId(request);
    }

    public CompletableFuture<CreateWorkloadAccessTokenForUserIdResponse> createWorkloadAccessTokenForUserIdAsync(CreateWorkloadAccessTokenForUserIdRequest request) {
        return asyncClient.createWorkloadAccessTokenForUserIdAsync(request);
    }

    // ========================
    // API Key Credential Provider CRUD (5 methods)
    // ========================

    public CreateApiKeyCredentialProviderResponse createApiKeyCredentialProvider(CreateApiKeyCredentialProviderRequest request) {
        return syncClient.createApiKeyCredentialProvider(request);
    }

    public CompletableFuture<CreateApiKeyCredentialProviderResponse> createApiKeyCredentialProviderAsync(CreateApiKeyCredentialProviderRequest request) {
        return asyncClient.createApiKeyCredentialProviderAsync(request);
    }

    public GetApiKeyCredentialProviderResponse getApiKeyCredentialProvider(GetApiKeyCredentialProviderRequest request) {
        return syncClient.getApiKeyCredentialProvider(request);
    }

    public CompletableFuture<GetApiKeyCredentialProviderResponse> getApiKeyCredentialProviderAsync(GetApiKeyCredentialProviderRequest request) {
        return asyncClient.getApiKeyCredentialProviderAsync(request);
    }

    public ListApiKeyCredentialProvidersResponse listApiKeyCredentialProviders(ListApiKeyCredentialProvidersRequest request) {
        return syncClient.listApiKeyCredentialProviders(request);
    }

    public CompletableFuture<ListApiKeyCredentialProvidersResponse> listApiKeyCredentialProvidersAsync(ListApiKeyCredentialProvidersRequest request) {
        return asyncClient.listApiKeyCredentialProvidersAsync(request);
    }

    public UpdateApiKeyCredentialProviderResponse updateApiKeyCredentialProvider(UpdateApiKeyCredentialProviderRequest request) {
        return syncClient.updateApiKeyCredentialProvider(request);
    }

    public CompletableFuture<UpdateApiKeyCredentialProviderResponse> updateApiKeyCredentialProviderAsync(UpdateApiKeyCredentialProviderRequest request) {
        return asyncClient.updateApiKeyCredentialProviderAsync(request);
    }

    public DeleteApiKeyCredentialProviderResponse deleteApiKeyCredentialProvider(DeleteApiKeyCredentialProviderRequest request) {
        return syncClient.deleteApiKeyCredentialProvider(request);
    }

    public CompletableFuture<DeleteApiKeyCredentialProviderResponse> deleteApiKeyCredentialProviderAsync(DeleteApiKeyCredentialProviderRequest request) {
        return asyncClient.deleteApiKeyCredentialProviderAsync(request);
    }

    // ========================
    // OAuth2 Credential Provider CRUD (5 methods)
    // ========================

    public CreateOauth2CredentialProviderResponse createOauth2CredentialProvider(CreateOauth2CredentialProviderRequest request) {
        return syncClient.createOauth2CredentialProvider(request);
    }

    public CompletableFuture<CreateOauth2CredentialProviderResponse> createOauth2CredentialProviderAsync(CreateOauth2CredentialProviderRequest request) {
        return asyncClient.createOauth2CredentialProviderAsync(request);
    }

    public GetOauth2CredentialProviderResponse getOauth2CredentialProvider(GetOauth2CredentialProviderRequest request) {
        return syncClient.getOauth2CredentialProvider(request);
    }

    public CompletableFuture<GetOauth2CredentialProviderResponse> getOauth2CredentialProviderAsync(GetOauth2CredentialProviderRequest request) {
        return asyncClient.getOauth2CredentialProviderAsync(request);
    }

    public ListOauth2CredentialProvidersResponse listOauth2CredentialProviders(ListOauth2CredentialProvidersRequest request) {
        return syncClient.listOauth2CredentialProviders(request);
    }

    public CompletableFuture<ListOauth2CredentialProvidersResponse> listOauth2CredentialProvidersAsync(ListOauth2CredentialProvidersRequest request) {
        return asyncClient.listOauth2CredentialProvidersAsync(request);
    }

    public UpdateOauth2CredentialProviderResponse updateOauth2CredentialProvider(UpdateOauth2CredentialProviderRequest request) {
        return syncClient.updateOauth2CredentialProvider(request);
    }

    public CompletableFuture<UpdateOauth2CredentialProviderResponse> updateOauth2CredentialProviderAsync(UpdateOauth2CredentialProviderRequest request) {
        return asyncClient.updateOauth2CredentialProviderAsync(request);
    }

    public DeleteOauth2CredentialProviderResponse deleteOauth2CredentialProvider(DeleteOauth2CredentialProviderRequest request) {
        return syncClient.deleteOauth2CredentialProvider(request);
    }

    public CompletableFuture<DeleteOauth2CredentialProviderResponse> deleteOauth2CredentialProviderAsync(DeleteOauth2CredentialProviderRequest request) {
        return asyncClient.deleteOauth2CredentialProviderAsync(request);
    }

    // ========================
    // STS Credential Provider CRUD (5 methods)
    // ========================

    public CreateStsCredentialProviderResponse createStsCredentialProvider(CreateStsCredentialProviderRequest request) {
        return syncClient.createStsCredentialProvider(request);
    }

    public CompletableFuture<CreateStsCredentialProviderResponse> createStsCredentialProviderAsync(CreateStsCredentialProviderRequest request) {
        return asyncClient.createStsCredentialProviderAsync(request);
    }

    public GetStsCredentialProviderResponse getStsCredentialProvider(GetStsCredentialProviderRequest request) {
        return syncClient.getStsCredentialProvider(request);
    }

    public CompletableFuture<GetStsCredentialProviderResponse> getStsCredentialProviderAsync(GetStsCredentialProviderRequest request) {
        return asyncClient.getStsCredentialProviderAsync(request);
    }

    public ListStsCredentialProvidersResponse listStsCredentialProviders(ListStsCredentialProvidersRequest request) {
        return syncClient.listStsCredentialProviders(request);
    }

    public CompletableFuture<ListStsCredentialProvidersResponse> listStsCredentialProvidersAsync(ListStsCredentialProvidersRequest request) {
        return asyncClient.listStsCredentialProvidersAsync(request);
    }

    public UpdateStsCredentialProviderResponse updateStsCredentialProvider(UpdateStsCredentialProviderRequest request) {
        return syncClient.updateStsCredentialProvider(request);
    }

    public CompletableFuture<UpdateStsCredentialProviderResponse> updateStsCredentialProviderAsync(UpdateStsCredentialProviderRequest request) {
        return asyncClient.updateStsCredentialProviderAsync(request);
    }

    public DeleteStsCredentialProviderResponse deleteStsCredentialProvider(DeleteStsCredentialProviderRequest request) {
        return syncClient.deleteStsCredentialProvider(request);
    }

    public CompletableFuture<DeleteStsCredentialProviderResponse> deleteStsCredentialProviderAsync(DeleteStsCredentialProviderRequest request) {
        return asyncClient.deleteStsCredentialProviderAsync(request);
    }

    // ========================
    // Resource Token retrieval (3 methods)
    // ========================

    public GetResourceApiKeyResponse getResourceApiKey(GetResourceApiKeyRequest request) {
        return syncClient.getResourceApiKey(request);
    }

    public CompletableFuture<GetResourceApiKeyResponse> getResourceApiKeyAsync(GetResourceApiKeyRequest request) {
        return asyncClient.getResourceApiKeyAsync(request);
    }

    public GetResourceOauth2TokenResponse getResourceOauth2Token(GetResourceOauth2TokenRequest request) {
        return syncClient.getResourceOauth2Token(request);
    }

    public CompletableFuture<GetResourceOauth2TokenResponse> getResourceOauth2TokenAsync(GetResourceOauth2TokenRequest request) {
        return asyncClient.getResourceOauth2TokenAsync(request);
    }

    public GetResourceStsTokenResponse getResourceStsToken(GetResourceStsTokenRequest request) {
        return syncClient.getResourceStsToken(request);
    }

    public CompletableFuture<GetResourceStsTokenResponse> getResourceStsTokenAsync(GetResourceStsTokenRequest request) {
        return asyncClient.getResourceStsTokenAsync(request);
    }

    // ========================
    // OAuth2 flow (2 methods)
    // ========================

    public Oauth2AuthorizeResponse oauth2Authorize(Oauth2AuthorizeRequest request) {
        return syncClient.oauth2Authorize(request);
    }

    public CompletableFuture<Oauth2AuthorizeResponse> oauth2AuthorizeAsync(Oauth2AuthorizeRequest request) {
        return asyncClient.oauth2AuthorizeAsync(request);
    }

    public Oauth2CallbackResponse oauth2Callback(Oauth2CallbackRequest request) {
        return syncClient.oauth2Callback(request);
    }

    public CompletableFuture<Oauth2CallbackResponse> oauth2CallbackAsync(Oauth2CallbackRequest request) {
        return asyncClient.oauth2CallbackAsync(request);
    }

    // ========================
    // Complete resource token auth (1 method)
    // ========================

    public CompleteResourceTokenAuthResponse completeResourceTokenAuth(CompleteResourceTokenAuthRequest request) {
        return syncClient.completeResourceTokenAuth(request);
    }

    public CompletableFuture<CompleteResourceTokenAuthResponse> completeResourceTokenAuthAsync(CompleteResourceTokenAuthRequest request) {
        return asyncClient.completeResourceTokenAuthAsync(request);
    }

    // ========================
    // Token Vault (1 method)
    // ========================

    public GetTokenVaultResponse getTokenVault(GetTokenVaultRequest request) {
        return syncClient.getTokenVault(request);
    }

    public CompletableFuture<GetTokenVaultResponse> getTokenVaultAsync(GetTokenVaultRequest request) {
        return asyncClient.getTokenVaultAsync(request);
    }

    // ========================
    // Authorizer Configuration (1 method)
    // ========================

    public GetWorkloadIdentityAuthorizerConfigurationResponse getWorkloadIdentityAuthorizerConfiguration(
            GetWorkloadIdentityAuthorizerConfigurationRequest request) {
        return syncClient.getWorkloadIdentityAuthorizerConfiguration(request);
    }

    public CompletableFuture<GetWorkloadIdentityAuthorizerConfigurationResponse> getWorkloadIdentityAuthorizerConfigurationAsync(
            GetWorkloadIdentityAuthorizerConfigurationRequest request) {
        return asyncClient.getWorkloadIdentityAuthorizerConfigurationAsync(request);
    }

    // ========================
    // Accessors
    // ========================

    public AgentIdentityClient getSyncClient() {
        return syncClient;
    }

    public AgentIdentityAsyncClient getAsyncClient() {
        return asyncClient;
    }

    public String getRegion() {
        return region;
    }
}
