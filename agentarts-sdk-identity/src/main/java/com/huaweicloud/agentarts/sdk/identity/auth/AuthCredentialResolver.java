package com.huaweicloud.agentarts.sdk.identity.auth;

import com.huaweicloud.sdk.agentidentity.v1.model.GetResourceOauth2TokenRequestBody;
import com.huaweicloud.sdk.agentidentity.v1.model.GetResourceStsTokenResponseBodyCredentials;
import com.huaweicloud.sdk.agentidentity.v1.model.StsTag;

import java.util.List;

/**
 * Resolves concrete credential values for {@link AuthInterceptor}.
 *
 * <p>The small interface keeps annotation interception independently testable
 * and allows applications to supply a cached or otherwise customized resolver.</p>
 */
public interface AuthCredentialResolver {

    String getResourceApiKeyValue(String providerName, String workloadAccessToken);

    String getResourceOauth2AccessToken(
            String providerName, String workloadAccessToken,
            GetResourceOauth2TokenRequestBody.Oauth2FlowEnum authFlow,
            List<String> scopes, String callbackUrl, boolean forceAuthentication);

    GetResourceStsTokenResponseBodyCredentials getResourceStsCredentials(
            String providerName, String workloadAccessToken, String agencySessionName,
            Integer durationSeconds, String policy, String sourceIdentity,
            List<StsTag> tags, List<String> transitiveTagKeys);
}
