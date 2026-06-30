package com.huaweicloud.agentarts.sdk.service.swr;

import com.huaweicloud.sdk.swr.v2.SwrClient;
import com.huaweicloud.sdk.swr.v2.SwrAsyncClient;
import com.huaweicloud.sdk.swr.v2.model.*;
import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.http.HttpConfig;
import com.huaweicloud.sdk.core.ClientBuilder;
import com.huaweicloud.agentarts.sdk.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Service client wrapping Huawei Cloud {@link SwrClient} for container registry operations.
 *
 * <p>Mirrors Python {@code SWRClient} from {@code service/swr_client.py}.
 * Provides namespace (organization), repository, and authorization token management
 * for deploying agent containers to SWR.</p>
 */
public class SWRServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(SWRServiceClient.class);

    private final SwrClient syncClient;
    private final SwrAsyncClient asyncClient;
    private final String region;

    public SWRServiceClient(String region, boolean ignoreSslVerification) {
        this.region = region != null ? region : Constants.getRegion();

        HttpConfig httpConfig = HttpConfig.getDefaultHttpConfig();
        httpConfig.setIgnoreSSLVerification(ignoreSslVerification);

        String endpoint = Constants.getSwrEndpoint(this.region);

        BasicCredentials credentials = new BasicCredentials()
                .withAk(Constants.getAk())
                .withSk(Constants.getSk());

        String securityToken = Constants.getSecurityToken();
        if (com.huaweicloud.agentarts.sdk.core.util.JsonUtils.isNotBlank(securityToken)) {
            credentials.withSecurityToken(securityToken);
        }

        this.syncClient = new ClientBuilder<>(SwrClient::new)
                .withCredential(credentials)
                .withHttpConfig(httpConfig)
                .withEndpoint(endpoint)
                .build();

        this.asyncClient = new ClientBuilder<>(SwrAsyncClient::new)
                .withCredential(credentials)
                .withHttpConfig(httpConfig)
                .withEndpoint(endpoint)
                .build();
    }

    public SWRServiceClient(String region) {
        this(region, false);
    }

    public SWRServiceClient() {
        this(null, false);
    }

    // ========================
    // Namespace (Organization) management
    // ========================

    /**
     * Create a new namespace (organization) in SWR.
     */
    public CreateNamespaceResponse createNamespace(CreateNamespaceRequest request) {
        return syncClient.createNamespace(request);
    }

    public CompletableFuture<CreateNamespaceResponse> createNamespaceAsync(CreateNamespaceRequest request) {
        return asyncClient.createNamespaceAsync(request);
    }

    /**
     * List all namespaces (organizations).
     */
    public ListNamespacesResponse listNamespaces(ListNamespacesRequest request) {
        return syncClient.listNamespaces(request);
    }

    public CompletableFuture<ListNamespacesResponse> listNamespacesAsync(ListNamespacesRequest request) {
        return asyncClient.listNamespacesAsync(request);
    }

    /**
     * Get details of a specific namespace.
     */
    public ShowNamespaceResponse showNamespace(ShowNamespaceRequest request) {
        return syncClient.showNamespace(request);
    }

    public CompletableFuture<ShowNamespaceResponse> showNamespaceAsync(ShowNamespaceRequest request) {
        return asyncClient.showNamespaceAsync(request);
    }

    // ========================
    // Repository management
    // ========================

    /**
     * Create a new repository under a namespace.
     */
    public CreateRepoResponse createRepo(CreateRepoRequest request) {
        return syncClient.createRepo(request);
    }

    public CompletableFuture<CreateRepoResponse> createRepoAsync(CreateRepoRequest request) {
        return asyncClient.createRepoAsync(request);
    }

    /**
     * List repository details.
     */
    public ListReposDetailsResponse listReposDetails(ListReposDetailsRequest request) {
        return syncClient.listReposDetails(request);
    }

    public CompletableFuture<ListReposDetailsResponse> listReposDetailsAsync(ListReposDetailsRequest request) {
        return asyncClient.listReposDetailsAsync(request);
    }

    /**
     * Show details of a specific repository.
     */
    public ShowRepositoryResponse showRepository(ShowRepositoryRequest request) {
        return syncClient.showRepository(request);
    }

    public CompletableFuture<ShowRepositoryResponse> showRepositoryAsync(ShowRepositoryRequest request) {
        return asyncClient.showRepositoryAsync(request);
    }

    // ========================
    // Authorization Token
    // ========================

    /**
     * Create an authorization token for Docker login.
     *
     * <p>Returns a map of {@link AuthInfo} objects containing Base64-encoded
     * authentication info, plus Docker login command headers.</p>
     */
    public CreateAuthorizationTokenResponse createAuthorizationToken(CreateAuthorizationTokenRequest request) {
        return syncClient.createAuthorizationToken(request);
    }

    public CompletableFuture<CreateAuthorizationTokenResponse> createAuthorizationTokenAsync(CreateAuthorizationTokenRequest request) {
        return asyncClient.createAuthorizationTokenAsync(request);
    }

    /**
     * Create temporary login credentials (secret).
     */
    public CreateSecretResponse createSecret(CreateSecretRequest request) {
        return syncClient.createSecret(request);
    }

    public CompletableFuture<CreateSecretResponse> createSecretAsync(CreateSecretRequest request) {
        return asyncClient.createSecretAsync(request);
    }

    // ========================
    // Convenience methods
    // ========================

    /**
     * Create a namespace if it doesn't already exist.
     *
     * @param namespace the namespace name
     * @return true if created, false if already existed
     */
    public boolean createNamespaceIfNotExists(String namespace) {
        try {
            showNamespace(new ShowNamespaceRequest().withNamespace(namespace));
            return false; // already exists
        } catch (Exception e) {
            // Not found, create it
            CreateNamespaceRequest request = new CreateNamespaceRequest()
                    .withBody(new CreateNamespaceRequestBody().withNamespace(namespace));
            createNamespace(request);
            return true;
        }
    }

    /**
     * Create a repository if it doesn't already exist.
     *
     * @param namespace the namespace name
     * @param repository the repository name
     * @return true if created, false if already existed
     */
    public boolean createRepoIfNotExists(String namespace, String repository) {
        try {
            showRepository(new ShowRepositoryRequest()
                    .withNamespace(namespace)
                    .withRepository(repository));
            return false; // already exists
        } catch (Exception e) {
            CreateRepoRequest request = new CreateRepoRequest()
                    .withNamespace(namespace)
                    .withBody(new CreateRepoRequestBody()
                            .withRepository(repository)
                            .withIsPublic(false));
            createRepo(request);
            return true;
        }
    }

    /**
     * Get the full Docker image name for a SWR repository.
     *
     * @param namespace  the namespace
     * @param repository the repository
     * @param tag        the image tag
     * @return full image name (e.g., "swr.cn-southwest-2.myhuaweicloud.com/namespace/repo:tag")
     */
    public String getFullImageName(String namespace, String repository, String tag) {
        String swrHost = "swr." + region + ".myhuaweicloud.com";
        return swrHost + "/" + namespace + "/" + repository + ":" + tag;
    }

    // ========================
    // Accessors
    // ========================

    public SwrClient getSyncClient() {
        return syncClient;
    }

    public SwrAsyncClient getAsyncClient() {
        return asyncClient;
    }

    public String getRegion() {
        return region;
    }
}
