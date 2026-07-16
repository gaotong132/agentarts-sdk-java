package com.huaweicloud.agentarts.sdk.identity;

import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.identity.config.LocalIdentityConfig;
import com.huaweicloud.agentarts.sdk.identity.auth.TokenPoller;
import com.huaweicloud.agentarts.sdk.identity.auth.AuthCredentialResolver;
import com.huaweicloud.agentarts.sdk.service.identity.IdentityServiceClient;
import com.huaweicloud.sdk.agentidentity.v1.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * High-level Identity client for AgentArts workload identity management.
 *
 * <p>Service client wrapping Huawei Cloud AgentIdentity API operations.
 * Wraps {@link IdentityServiceClient} and provides convenience methods for
 * workload identity bootstrap, token creation, and resource token retrieval.</p>
 */
public class IdentityClient implements AuthCredentialResolver {

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
                .withBody(new CreateWorkloadIdentityReqBody()
                        .withName(name)
                        .withAuthorizerType(AuthorizerType.NONE));
        return serviceClient.createWorkloadIdentity(request);
    }

    public GetWorkloadIdentityResponse getWorkloadIdentity(String name) {
        return serviceClient.getWorkloadIdentity(
                new GetWorkloadIdentityRequest().withWorkloadIdentityName(name));
    }

    /**
     * Update an existing workload identity's configuration.
     *
     * @param name                              the workload identity name
     * @param allowedResourceOauth2ReturnUrls   OAuth2 callback URLs to allow; may be {@code null}
     * @param authorizerConfiguration           authorizer configuration; may be {@code null}
     * @return the update response
     */
    public UpdateWorkloadIdentityResponse updateWorkloadIdentity(
            String name, List<String> allowedResourceOauth2ReturnUrls,
            AuthorizerConfiguration authorizerConfiguration) {
        UpdateWorkloadIdentityReqBody body = new UpdateWorkloadIdentityReqBody();
        if (allowedResourceOauth2ReturnUrls != null) {
            body.withAllowedResourceOauth2ReturnUrls(allowedResourceOauth2ReturnUrls);
        }
        if (authorizerConfiguration != null) {
            body.withAuthorizerConfiguration(authorizerConfiguration);
        }
        UpdateWorkloadIdentityRequest request = new UpdateWorkloadIdentityRequest()
                .withWorkloadIdentityName(name)
                .withBody(body);
        return serviceClient.updateWorkloadIdentity(request);
    }

    /**
     * Update an existing workload identity's OAuth2 return URLs.
     */
    public UpdateWorkloadIdentityResponse updateWorkloadIdentity(
            String name, List<String> allowedResourceOauth2ReturnUrls) {
        return updateWorkloadIdentity(name, allowedResourceOauth2ReturnUrls, null);
    }

    public ListWorkloadIdentitiesResponse listWorkloadIdentities() {
        return serviceClient.listWorkloadIdentities(new ListWorkloadIdentitiesRequest());
    }

    /** List API key credential providers (convenience wrapper). */
    public ListApiKeyCredentialProvidersResponse listApiKeyCredentialProviders() {
        return serviceClient.listApiKeyCredentialProviders(new ListApiKeyCredentialProvidersRequest());
    }

    /** List OAuth2 credential providers (convenience wrapper). */
    public ListOauth2CredentialProvidersResponse listOauth2CredentialProviders() {
        return serviceClient.listOauth2CredentialProviders(new ListOauth2CredentialProvidersRequest());
    }

    /** List STS credential providers (convenience wrapper). */
    public ListStsCredentialProvidersResponse listStsCredentialProviders() {
        return serviceClient.listStsCredentialProviders(new ListStsCredentialProvidersRequest());
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

    /**
     * Create an OAuth2 credential provider.
     *
     * <p>Dispatches the vendor-specific config block (Github / Google / Microsoft /
     * Custom) the same way the reference implementation does. {@code tenantId} is
     * required for {@link CredentialProviderVendor#MICROSOFTOAUTH2}; an
     * {@code oauthDiscovery} is required for
     * {@link CredentialProviderVendor#CUSTOMOAUTH2}.</p>
     *
     * @param name            the credential provider name
     * @param vendor          the OAuth2 vendor
     * @param clientId        the OAuth2 client id
     * @param clientSecret    the OAuth2 client secret
     * @param tenantId        the tenant id (Microsoft only); {@code null} otherwise
     * @param oauthDiscovery  the OAuth2 discovery config (Custom only); {@code null} otherwise
     */
    public CreateOauth2CredentialProviderResponse createOauth2CredentialProvider(
            String name, CredentialProviderVendor vendor,
            String clientId, String clientSecret,
            String tenantId, Oauth2Discovery oauthDiscovery) {
        Oauth2ProviderConfigInput config = new Oauth2ProviderConfigInput();
        if (vendor == CredentialProviderVendor.GITHUBOAUTH2) {
            config.withGithubOauth2ProviderConfig(
                    new GithubOauth2ProviderConfigInput()
                            .withClientId(clientId)
                            .withClientSecret(clientSecret));
        } else if (vendor == CredentialProviderVendor.GOOGLEOAUTH2) {
            config.withGoogleOauth2ProviderConfig(
                    new GoogleOauth2ProviderConfigInput()
                            .withClientId(clientId)
                            .withClientSecret(clientSecret));
        } else if (vendor == CredentialProviderVendor.MICROSOFTOAUTH2) {
            config.withMicrosoftOauth2ProviderConfig(
                    new MicrosoftOauth2ProviderConfigInput()
                            .withClientId(clientId)
                            .withClientSecret(clientSecret)
                            .withTenantId(tenantId));
        } else if (vendor == CredentialProviderVendor.CUSTOMOAUTH2) {
            config.withCustomOauth2ProviderConfig(
                    new CustomOauth2ProviderConfigInput()
                            .withClientId(clientId)
                            .withClientSecret(clientSecret)
                            .withOauth2Discovery(oauthDiscovery));
        } else {
            throw new IllegalArgumentException("Unsupported OAuth2 vendor: " + vendor);
        }
        CreateOauth2CredentialProviderRequest request = new CreateOauth2CredentialProviderRequest()
                .withBody(new CreateOauth2CredentialProviderReqBody()
                        .withName(name)
                        .withCredentialProviderVendor(vendor)
                        .withOauth2ProviderConfigInput(config));
        return serviceClient.createOauth2CredentialProvider(request);
    }

    /**
     * Create an OAuth2 credential provider (Github/Google shorthand without
     * tenant id or discovery).
     */
    public CreateOauth2CredentialProviderResponse createOauth2CredentialProvider(
            String name, CredentialProviderVendor vendor,
            String clientId, String clientSecret) {
        return createOauth2CredentialProvider(name, vendor, clientId, clientSecret, null, null);
    }

    /**
     * Create an STS credential provider bound to an IAM agency URN.
     *
     * @param providerName the credential provider name
     * @param agencyUrn    the IAM agency URN (iam::{agencyName})
     */
    public CreateStsCredentialProviderResponse createStsCredentialProvider(
            String providerName, String agencyUrn) {
        CreateStsCredentialProviderRequest request = new CreateStsCredentialProviderRequest()
                .withBody(new CreateStsCredentialProviderReqBody()
                        .withName(providerName)
                        .withAgencyUrn(agencyUrn));
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
     * Get the API key value, matching the high-level Python SDK contract.
     */
    public String getResourceApiKeyValue(String providerName, String workloadAccessToken) {
        GetResourceApiKeyResponse response = getResourceApiKey(providerName, workloadAccessToken);
        if (response == null || response.getApiKey() == null || response.getApiKey().isBlank()) {
            throw new IllegalStateException("Identity service returned an empty API key");
        }
        return response.getApiKey();
    }

    /**
     * Get an OAuth2 token from a credential provider.
     */
    public GetResourceOauth2TokenResponse getResourceOauth2Token(
            String providerName, String workloadAccessToken) {
        return getResourceOauth2Token(providerName, workloadAccessToken, null, null,
                null, false, null, null, null);
    }

    /**
     * Get an OAuth2 token response with the complete request contract.
     */
    public GetResourceOauth2TokenResponse getResourceOauth2Token(
            String providerName, String workloadAccessToken,
            GetResourceOauth2TokenRequestBody.Oauth2FlowEnum authFlow,
            List<String> scopes, String callbackUrl, boolean forceAuthentication,
            String customState, Map<String, String> customParameters, String sessionUri) {
        GetResourceOauth2TokenRequestBody body = new GetResourceOauth2TokenRequestBody()
                .withResourceCredentialProviderName(providerName)
                .withWorkloadAccessToken(workloadAccessToken)
                .withOauth2Flow(authFlow)
                .withScopes(scopes)
                .withResourceOauth2ReturnUrl(callbackUrl)
                .withForceAuthentication(forceAuthentication)
                .withCustomState(customState)
                .withCustomParameters(customParameters)
                .withSessionUri(sessionUri);
        return serviceClient.getResourceOauth2Token(
                new GetResourceOauth2TokenRequest().withBody(body));
    }

    /**
     * Resolve an OAuth2 access-token value. USER_FEDERATION responses are polled
     * after the authorization URL has been emitted to the application log.
     */
    public String getResourceOauth2AccessToken(
            String providerName, String workloadAccessToken,
            GetResourceOauth2TokenRequestBody.Oauth2FlowEnum authFlow,
            List<String> scopes, String callbackUrl, boolean forceAuthentication) {
        GetResourceOauth2TokenResponse initial = getResourceOauth2Token(
                providerName, workloadAccessToken, authFlow, scopes, callbackUrl,
                forceAuthentication, null, null, null);
        if (initial != null && initial.getAccessToken() != null && !initial.getAccessToken().isBlank()) {
            return initial.getAccessToken();
        }
        if (initial == null || initial.getAuthorizationUrl() == null
                || initial.getAuthorizationUrl().isBlank() || initial.getSessionUri() == null) {
            throw new IllegalStateException(
                    "Identity service returned neither an access token nor an authorization URL");
        }

        LOG.info("OAuth2 user authorization required: {}", initial.getAuthorizationUrl());
        String sessionUri = initial.getSessionUri();
        return new TokenPoller() {
            @Override
            public PollResult poll() {
                GetResourceOauth2TokenResponse response = getResourceOauth2Token(
                        providerName, workloadAccessToken, authFlow, scopes, callbackUrl,
                        false, null, null, sessionUri);
                if (response != null && response.getAccessToken() != null
                        && !response.getAccessToken().isBlank()) {
                    return PollResult.completed(response.getAccessToken());
                }
                if (response != null
                        && GetResourceOauth2TokenResponse.SessionStatusEnum.FAILED
                        .equals(response.getSessionStatus())) {
                    return PollResult.failed("OAuth2 authorization failed");
                }
                return PollResult.inProgress();
            }
        }.waitForToken();
    }

    /**
     * Get STS credentials from a credential provider.
     */
    public GetResourceStsTokenResponse getResourceStsToken(
            String providerName, String workloadAccessToken,
            String agencySessionName) {
        return getResourceStsToken(providerName, workloadAccessToken, agencySessionName,
                null, null, null, null, null);
    }

    /**
     * Get STS credentials with all optional restriction fields.
     */
    public GetResourceStsTokenResponse getResourceStsToken(
            String providerName, String workloadAccessToken, String agencySessionName,
            Integer durationSeconds, String policy, String sourceIdentity,
            List<StsTag> tags, List<String> transitiveTagKeys) {
        GetResourceStsTokenRequest request = new GetResourceStsTokenRequest()
                .withBody(new GetResourceStsTokenRequestBody()
                        .withResourceCredentialProviderName(providerName)
                        .withWorkloadAccessToken(workloadAccessToken)
                        .withAgencySessionName(agencySessionName)
                        .withDurationSeconds(durationSeconds)
                        .withPolicy(policy)
                        .withSourceIdentity(sourceIdentity)
                        .withTags(tags)
                        .withTransitiveTagKeys(transitiveTagKeys));
        return serviceClient.getResourceStsToken(request);
    }

    /**
     * Get the STS credentials value, matching the high-level Python SDK contract.
     */
    public GetResourceStsTokenResponseBodyCredentials getResourceStsCredentials(
            String providerName, String workloadAccessToken, String agencySessionName,
            Integer durationSeconds, String policy, String sourceIdentity,
            List<StsTag> tags, List<String> transitiveTagKeys) {
        GetResourceStsTokenResponse response = getResourceStsToken(
                providerName, workloadAccessToken, agencySessionName, durationSeconds,
                policy, sourceIdentity, tags, transitiveTagKeys);
        if (response == null || response.getCredentials() == null) {
            throw new IllegalStateException("Identity service returned empty STS credentials");
        }
        return response.getCredentials();
    }

    /**
     * Complete resource token auth (confirm OAuth2 user session).
     */
    public CompleteResourceTokenAuthResponse completeResourceTokenAuth(
            String sessionUri, UserIdentifier userIdentifier) {
        CompleteResourceTokenAuthRequest request = new CompleteResourceTokenAuthRequest()
                .withBody(new CompleteResourceTokenAuthRequestBody()
                        .withSessionUri(sessionUri)
                        .withUserIdentifier(userIdentifier));
        return serviceClient.completeResourceTokenAuth(request);
    }

    /**
     * @deprecated use {@link #completeResourceTokenAuth(String, UserIdentifier)}
     * so the user identity is explicitly bound to the authorization session.
     */
    @Deprecated
    public CompleteResourceTokenAuthResponse completeResourceTokenAuth(String sessionUri) {
        throw new IllegalArgumentException(
                "userIdentifier is required; use completeResourceTokenAuth(sessionUri, userIdentifier)");
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
