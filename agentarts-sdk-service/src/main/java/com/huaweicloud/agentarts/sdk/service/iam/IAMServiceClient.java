package com.huaweicloud.agentarts.sdk.service.iam;

import com.huaweicloud.sdk.iam.v5.IamClient;
import com.huaweicloud.sdk.iam.v5.IamAsyncClient;
import com.huaweicloud.sdk.iam.v5.model.CreateAgencyReqBody;
import com.huaweicloud.sdk.iam.v5.model.CreateAgencyV5Request;
import com.huaweicloud.sdk.iam.v5.model.CreateAgencyV5Response;
import com.huaweicloud.sdk.core.auth.GlobalCredentials;
import com.huaweicloud.sdk.core.auth.ICredentialProvider;
import com.huaweicloud.sdk.core.http.HttpConfig;
import com.huaweicloud.sdk.core.ClientBuilder;
import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import com.huaweicloud.agentarts.sdk.service.auth.CredentialProviders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Service client wrapping Huawei Cloud IAM v5 for agency management.
 *
 * <p>Service client wrapping Huawei Cloud IAM v5 for agency management.
 * Uses IAM v5 (not v3) for agency creation, matching the Python SDK.</p>
 *
 * <h2>APIs:</h2>
 * <ul>
 *   <li>{@link #createAgency} — creates an IAM agency (v5 API)</li>
 * </ul>
 */
public class IAMServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(IAMServiceClient.class);

    private final IamClient syncClient;
    private final IamAsyncClient asyncClient;
    private final String region;

    public IAMServiceClient(String region, boolean ignoreSslVerification) {
        this(region, ignoreSslVerification, CredentialProviders.defaultGlobalProvider());
    }

    /**
     * Create a client with an explicit global-credential provider.
     */
    public IAMServiceClient(String region, boolean ignoreSslVerification,
                            ICredentialProvider credentialProvider) {
        this.region = region != null ? region : Constants.getRegion();

        HttpConfig httpConfig = HttpConfig.getDefaultHttpConfig();
        httpConfig.setIgnoreSSLVerification(ignoreSslVerification);

        String endpoint = Constants.getIamEndpoint(this.region);

        GlobalCredentials credentials = CredentialProviders.resolveGlobal(credentialProvider);

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
    // Agency management (IAM v5)
    // ========================

    /**
     * Create an IAM agency for delegated access.
     *
     * <p>Create an IAM agency for delegated access.
     * using IAM v5 API.</p>
     *
     * @param agencyName          name of the agency to create
     * @param trustPolicy         trust policy JSON string
     * @param path                agency path (nullable, default null)
     * @param maxSessionDuration  max session duration in seconds (nullable)
     * @param description         agency description (nullable)
     * @return the created agency response
     */
    public CreateAgencyV5Response createAgency(String agencyName, String trustPolicy,
                                                 String path, Integer maxSessionDuration,
                                                 String description) {
        CreateAgencyReqBody body = new CreateAgencyReqBody()
                .withAgencyName(agencyName)
                .withTrustPolicy(trustPolicy);

        if (JsonUtils.isNotBlank(path)) {
            body.withPath(path);
        }
        if (maxSessionDuration != null) {
            body.withMaxSessionDuration(maxSessionDuration);
        }
        if (JsonUtils.isNotBlank(description)) {
            body.withDescription(description);
        }

        CreateAgencyV5Request request = new CreateAgencyV5Request().withBody(body);
        LOG.info("Creating IAM agency: {}", agencyName);
        return syncClient.createAgencyV5(request);
    }

    /**
     * Convenience overload with required parameters only.
     */
    public CreateAgencyV5Response createAgency(String agencyName, String trustPolicy) {
        return createAgency(agencyName, trustPolicy, null, null, null);
    }

    /**
     * Async version of {@link #createAgency}.
     */
    public CompletableFuture<CreateAgencyV5Response> createAgencyAsync(
            String agencyName, String trustPolicy,
            String path, Integer maxSessionDuration, String description) {

        CreateAgencyReqBody body = new CreateAgencyReqBody()
                .withAgencyName(agencyName)
                .withTrustPolicy(trustPolicy);

        if (JsonUtils.isNotBlank(path)) {
            body.withPath(path);
        }
        if (maxSessionDuration != null) {
            body.withMaxSessionDuration(maxSessionDuration);
        }
        if (JsonUtils.isNotBlank(description)) {
            body.withDescription(description);
        }

        CreateAgencyV5Request request = new CreateAgencyV5Request().withBody(body);
        return asyncClient.createAgencyV5Async(request);
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
