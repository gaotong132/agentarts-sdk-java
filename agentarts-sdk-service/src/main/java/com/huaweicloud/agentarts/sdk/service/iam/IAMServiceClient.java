package com.huaweicloud.agentarts.sdk.service.iam;

import com.huaweicloud.sdk.iam.v3.IamClient;
import com.huaweicloud.sdk.iam.v3.IamAsyncClient;
import com.huaweicloud.sdk.iam.v3.model.*;
import com.huaweicloud.sdk.core.auth.GlobalCredentials;
import com.huaweicloud.sdk.core.http.HttpConfig;
import com.huaweicloud.sdk.core.ClientBuilder;
import com.huaweicloud.agentarts.sdk.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Service client wrapping Huawei Cloud {@link IamClient} for IAM operations.
 *
 * <p>Mirrors Python {@code IAMClient} from {@code service/iam_client.py}.
 * Provides agency creation and token management for AgentArts services.</p>
 */
public class IAMServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(IAMServiceClient.class);

    private final IamClient syncClient;
    private final IamAsyncClient asyncClient;
    private final String region;

    public IAMServiceClient(String region, boolean ignoreSslVerification) {
        this.region = region != null ? region : Constants.getRegion();

        HttpConfig httpConfig = HttpConfig.getDefaultHttpConfig();
        httpConfig.setIgnoreSSLVerification(ignoreSslVerification);

        String endpoint = Constants.getIamEndpoint(this.region);

        GlobalCredentials credentials = new GlobalCredentials()
                .withAk(Constants.getAk())
                .withSk(Constants.getSk());

        String securityToken = Constants.getSecurityToken();
        if (securityToken != null && !securityToken.isEmpty()) {
            credentials.withSecurityToken(securityToken);
        }

        this.syncClient = new ClientBuilder<>(IamClient::new)
                .withCredential(credentials)
                .withHttpConfig(httpConfig)
                .withEndpoint(endpoint)
                .build();

        this.asyncClient = new ClientBuilder<>(IamAsyncClient::new)
                .withCredential(credentials)
                .withHttpConfig(httpConfig)
                .withEndpoint(endpoint)
                .build();
    }

    public IAMServiceClient(String region) {
        this(region, false);
    }

    public IAMServiceClient() {
        this(null, false);
    }

    // ========================
    // Agency management
    // ========================

    /**
     * Create an IAM agency for delegated access.
     *
     * <p>Used by MCP Gateway to create the AgentArtsCoreGateway agency.</p>
     */
    public KeystoneCreateAgencyTokenResponse createAgencyToken(KeystoneCreateAgencyTokenRequest request) {
        return syncClient.keystoneCreateAgencyToken(request);
    }

    public CompletableFuture<KeystoneCreateAgencyTokenResponse> createAgencyTokenAsync(KeystoneCreateAgencyTokenRequest request) {
        return asyncClient.keystoneCreateAgencyTokenAsync(request);
    }

    // ========================
    // Accessors
    // ========================

    public IamClient getSyncClient() {
        return syncClient;
    }

    public IamAsyncClient getAsyncClient() {
        return asyncClient;
    }

    public String getRegion() {
        return region;
    }
}
