package com.huaweicloud.agentarts.sdk.identity;

import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.identity.config.LocalIdentityConfig;
import com.huaweicloud.agentarts.sdk.service.identity.IdentityServiceClient;
import com.huaweicloud.sdk.agentidentity.v1.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-level Identity client for AgentArts workload identity management.
 *
 * <p>Service client wrapping Huawei Cloud AgentIdentity API operations.
 * Wraps {@link IdentityServiceClient} and provides convenience methods for
 * workload identity bootstrap, token creation, and resource token retrieval.</p>
 */
public class IdentityClient {

    private static final Logger LOG = LoggerFactory.getLogger(IdentityClient.class);

    private final IdentityServiceClient serviceClient;
    private final String region;
    private final boolean ignoreSslVerification;

    public IdentityClient(String region, boolean ignoreSslVerification) {
        this.region = region != null ? region : Constants.getRegion();
        this.ignoreSslVerification = ignoreSslVerification;
        this.serviceClient = new IdentityServiceClient(this.region, ignoreSslVerification);
    }

    public IdentityClient(String region) {
        this(region, false);
    }

    public IdentityClient() {
        this(null, false);
    }

    public IdentityClient(IdentityServiceClient serviceClient) {
        this.serviceClient = serviceClient;
        this.region = serviceClient.getRegion();
        this.ignoreSslVerification = false;
    }

    // ========================
    // Workload Identity
    // ========================

    public CreateWorkloadIdentityResponse createWorkloadIdentity(String name) {
        CreateWorkloadIdentityRequest request = new CreateWorkloadIdentityRequest()
                .withBody(new CreateWorkloadIdentityReqBody().withName(name));
        return serviceClient.createWorkloadIdentity(request);
    }

    public GetWorkloadIdentityResponse getWorkloadIdentity(String name) {
        return serviceClient.getWorkloadIdentity(
                new GetWorkloadIdentityRequest().withWorkloadIdentityName(name));
    }

    public ListWorkloadIdentitiesResponse listWorkloadIdentities() {
        return serviceClient.listWorkloadIdentities(new ListWorkloadIdentitiesRequest());
    }

    public DeleteWorkloadIdentityResponse deleteWorkloadIdentity(String name) {
        return serviceClient.deleteWorkloadIdentity(
                new DeleteWorkloadIdentityRequest().withWorkloadIdentityName(name));
    }

    // ========================
    // Workload Access Token
    // ========================

    /**
     * Create a workload access token (not acting on behalf of a user).
     */
    public String createWorkloadAccessToken(String workloadName) {
        CreateWorkloadAccessTokenRequest request = new CreateWorkloadAccessTokenRequest()
                .withBody(new CreateWorkloadAccessTokenRequestBody()
                        .withWorkloadName(workloadName));
        CreateWorkloadAccessTokenResponse response = serviceClient.createWorkloadAccessToken(request);
        return response.getWorkloadAccessToken();
    }

    /**
     * Create a workload access token acting on behalf of a user.
     */
    public String createWorkloadAccessTokenForUserId(String workloadName, String userId) {
        CreateWorkloadAccessTokenForUserIdRequest request = new CreateWorkloadAccessTokenForUserIdRequest()
                .withBody(new CreateWorkloadAccessTokenForUserIdRequestBody()
                        .withWorkloadName(workloadName)
                        .withUserId(userId));
        CreateWorkloadAccessTokenForUserIdResponse response =
                serviceClient.createWorkloadAccessTokenForUserId(request);
        return response.getWorkloadAccessToken();
    }

    /**
     * Create a workload access token using JWT (acting on behalf of a user).
     */
    public String createWorkloadAccessTokenForJwt(String workloadName, String userToken) {
        CreateWorkloadAccessTokenForJwtRequest request = new CreateWorkloadAccessTokenForJwtRequest()
                .withBody(new CreateWorkloadAccessTokenForJwtRequestBody()
                        .withWorkloadName(workloadName)
                        .withUserToken(userToken));
        CreateWorkloadAccessTokenForJwtResponse response =
                serviceClient.createWorkloadAccessTokenForJwt(request);
        return response.getWorkloadAccessToken();
    }

    // ========================
    // Credential Providers
    // ========================

    public CreateApiKeyCredentialProviderResponse createApiKeyCredentialProvider(
            String providerName, String apiKey) {
        CreateApiKeyCredentialProviderRequest request = new CreateApiKeyCredentialProviderRequest()
                .withBody(new CreateApiKeyCredentialProviderReqBody()
                        .withName(providerName)
                        .withApiKey(apiKey));
        return serviceClient.createApiKeyCredentialProvider(request);
    }

    public CreateOauth2CredentialProviderResponse createOauth2CredentialProvider(
            String providerName) {
        CreateOauth2CredentialProviderRequest request = new CreateOauth2CredentialProviderRequest()
                .withBody(new CreateOauth2CredentialProviderReqBody()
                        .withName(providerName));
        return serviceClient.createOauth2CredentialProvider(request);
    }

    public CreateStsCredentialProviderResponse createStsCredentialProvider(
            String providerName) {
        CreateStsCredentialProviderRequest request = new CreateStsCredentialProviderRequest()
                .withBody(new CreateStsCredentialProviderReqBody()
                        .withName(providerName));
        return serviceClient.createStsCredentialProvider(request);
    }

    // ========================
    // Resource Token retrieval
    // ========================

    /**
     * Get an API key from a credential provider.
     */
    public GetResourceApiKeyResponse getResourceApiKey(
            String providerName, String workloadAccessToken) {
        GetResourceApiKeyRequest request = new GetResourceApiKeyRequest()
                .withBody(new GetResourceApiKeyRequestBody()
                        .withResourceCredentialProviderName(providerName)
                        .withWorkloadAccessToken(workloadAccessToken));
        return serviceClient.getResourceApiKey(request);
    }

    /**
     * Get an OAuth2 token from a credential provider.
     */
    public GetResourceOauth2TokenResponse getResourceOauth2Token(
            String providerName, String workloadAccessToken) {
        GetResourceOauth2TokenRequest request = new GetResourceOauth2TokenRequest()
                .withBody(new GetResourceOauth2TokenRequestBody()
                        .withResourceCredentialProviderName(providerName)
                        .withWorkloadAccessToken(workloadAccessToken));
        return serviceClient.getResourceOauth2Token(request);
    }

    /**
     * Get STS credentials from a credential provider.
     */
    public GetResourceStsTokenResponse getResourceStsToken(
            String providerName, String workloadAccessToken,
            String agencySessionName) {
        GetResourceStsTokenRequest request = new GetResourceStsTokenRequest()
                .withBody(new GetResourceStsTokenRequestBody()
                        .withResourceCredentialProviderName(providerName)
                        .withWorkloadAccessToken(workloadAccessToken)
                        .withAgencySessionName(agencySessionName));
        return serviceClient.getResourceStsToken(request);
    }

    /**
     * Complete resource token auth (confirm OAuth2 user session).
     */
    public CompleteResourceTokenAuthResponse completeResourceTokenAuth(String sessionUri) {
        CompleteResourceTokenAuthRequest request = new CompleteResourceTokenAuthRequest()
                .withBody(new CompleteResourceTokenAuthRequestBody()
                        .withSessionUri(sessionUri));
        return serviceClient.completeResourceTokenAuth(request);
    }

    // ========================
    // Local identity bootstrap
    // ========================

    /**
     * Ensure a workload identity exists locally, creating one if needed.
     * Saves the identity name to {@code .agent_identity.json}.
     *
     * @return the workload access token
     */
    public String ensureLocalAuthToken(String workloadName) {
        LocalIdentityConfig config = LocalIdentityConfig.load();

        String identityName = config.getWorkloadIdentityName();
        if (identityName == null || identityName.isEmpty()) {
            identityName = workloadName;
            try {
                createWorkloadIdentity(identityName);
            } catch (Exception e) {
                LOG.debug("Identity may already exist: {}", e.getMessage());
            }
            config.setWorkloadIdentityName(identityName);
            config.save();
        }

        String userId = config.getUserId();
        if (userId == null || userId.isEmpty()) {
            userId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            config.setUserId(userId);
            config.save();
        }

        return createWorkloadAccessTokenForUserId(identityName, userId);
    }

    // ========================
    // Accessors
    // ========================

    public IdentityServiceClient getServiceClient() {
        return serviceClient;
    }

    public String getRegion() {
        return region;
    }
}
