package com.huaweicloud.agentarts.sdk.service.iam;

import com.huaweicloud.sdk.iam.v5.IamClient;
import com.huaweicloud.sdk.iam.v5.IamAsyncClient;
import com.huaweicloud.sdk.iam.v5.model.CreateAgencyReqBody;
import com.huaweicloud.sdk.iam.v5.model.CreateAgencyV5Request;
import com.huaweicloud.sdk.iam.v5.model.CreateAgencyV5Response;
import com.huaweicloud.sdk.iam.v5.model.AttachAgencyPolicyReqBody;
import com.huaweicloud.sdk.iam.v5.model.AttachAgencyPolicyV5Request;
import com.huaweicloud.sdk.iam.v5.model.AttachAgencyPolicyV5Response;
import com.huaweicloud.sdk.iam.v5.model.ListAgenciesV5Request;
import com.huaweicloud.sdk.iam.v5.model.ListAgenciesV5Response;
import com.huaweicloud.sdk.iam.v5.model.ListPoliciesV5Request;
import com.huaweicloud.sdk.iam.v5.model.ListPoliciesV5Response;
import com.huaweicloud.sdk.iam.v5.model.Policy;
import com.huaweicloud.sdk.core.auth.GlobalCredentials;
import com.huaweicloud.sdk.core.auth.ICredentialProvider;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.core.http.HttpConfig;
import com.huaweicloud.sdk.core.ClientBuilder;
import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import com.huaweicloud.agentarts.sdk.service.auth.CredentialProviders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Service client wrapping Huawei Cloud IAM v5 agency and policy operations.
 *
 * <p>Provides synchronous and asynchronous operations, plus an idempotent helper for
 * creating an agency and attaching a named system policy.</p>
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

    IAMServiceClient(IamClient syncClient, IamAsyncClient asyncClient, String region) {
        this.syncClient = Objects.requireNonNull(syncClient, "syncClient must not be null");
        this.asyncClient = Objects.requireNonNull(asyncClient, "asyncClient must not be null");
        this.region = Objects.requireNonNull(region, "region must not be null");
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
                .withTrustPolicy(trustPolicy)
                .withPath(path == null ? "" : path)
                .withMaxSessionDuration(maxSessionDuration == null ? 3600 : maxSessionDuration);

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
                .withTrustPolicy(trustPolicy)
                .withPath(path == null ? "" : path)
                .withMaxSessionDuration(maxSessionDuration == null ? 3600 : maxSessionDuration);

        if (JsonUtils.isNotBlank(description)) {
            body.withDescription(description);
        }

        CreateAgencyV5Request request = new CreateAgencyV5Request().withBody(body);
        return asyncClient.createAgencyV5Async(request);
    }

    /** List policies using IAM v5 pagination and filtering options. */
    public ListPoliciesV5Response listPolicies(String policyType, Integer limit,
                                                String marker, String pathPrefix,
                                                Boolean onlyAttached) {
        return syncClient.listPoliciesV5(policyListRequest(
                policyType, limit, marker, pathPrefix, onlyAttached));
    }

    public ListPoliciesV5Response listPolicies() {
        return listPolicies(null, null, null, null, null);
    }

    /** Async version of {@link #listPolicies(String, Integer, String, String, Boolean)}. */
    public CompletableFuture<ListPoliciesV5Response> listPoliciesAsync(
            String policyType, Integer limit, String marker, String pathPrefix,
            Boolean onlyAttached) {
        return asyncClient.listPoliciesV5Async(policyListRequest(
                policyType, limit, marker, pathPrefix, onlyAttached));
    }

    /** List agencies using IAM v5 pagination options. */
    public ListAgenciesV5Response listAgencies(Integer limit, String marker, String pathPrefix) {
        return syncClient.listAgenciesV5(agencyListRequest(limit, marker, pathPrefix));
    }

    public ListAgenciesV5Response listAgencies() {
        return listAgencies(null, null, null);
    }

    /** Async version of {@link #listAgencies(Integer, String, String)}. */
    public CompletableFuture<ListAgenciesV5Response> listAgenciesAsync(
            Integer limit, String marker, String pathPrefix) {
        return asyncClient.listAgenciesV5Async(agencyListRequest(limit, marker, pathPrefix));
    }

    /** Attach an IAM policy to an agency. */
    public AttachAgencyPolicyV5Response attachAgencyPolicy(String agencyId, String policyId) {
        return syncClient.attachAgencyPolicyV5(agencyPolicyRequest(agencyId, policyId));
    }

    /** Async version of {@link #attachAgencyPolicy(String, String)}. */
    public CompletableFuture<AttachAgencyPolicyV5Response> attachAgencyPolicyAsync(
            String agencyId, String policyId) {
        return asyncClient.attachAgencyPolicyV5Async(agencyPolicyRequest(agencyId, policyId));
    }

    /**
     * Create an agency and attach a named system policy.
     *
     * <p>The operation is idempotent for IAM conflict responses. An existing agency is resolved
     * by name, and a policy that is already attached is accepted.</p>
     *
     * @return the create response, or {@code null} when the agency already existed
     */
    public CreateAgencyV5Response createAgencyWithPolicy(
            String agencyName, String trustPolicy, String policyName,
            String path, Integer maxSessionDuration, String description) {
        CreateAgencyV5Response createResponse = null;
        String agencyId;
        try {
            createResponse = createAgency(
                    agencyName, trustPolicy, path, maxSessionDuration, description);
            if (createResponse == null || createResponse.getAgency() == null
                    || !JsonUtils.isNotBlank(createResponse.getAgency().getAgencyId())) {
                throw new IllegalStateException("IAM create response did not contain an agency ID");
            }
            agencyId = createResponse.getAgency().getAgencyId();
        } catch (ServiceResponseException e) {
            if (e.getHttpStatusCode() != 409) {
                throw e;
            }
            agencyId = findAgencyIdByName(agencyName);
        }

        Policy policy = findSystemPolicyByName(policyName);
        try {
            attachAgencyPolicy(agencyId, policy.getPolicyId());
        } catch (ServiceResponseException e) {
            if (e.getHttpStatusCode() != 409) {
                throw e;
            }
            LOG.debug("IAM policy is already attached to agency {}", agencyName);
        }
        return createResponse;
    }

    public CreateAgencyV5Response createAgencyWithPolicy(
            String agencyName, String trustPolicy, String policyName) {
        return createAgencyWithPolicy(
                agencyName, trustPolicy, policyName, null, null, null);
    }

    private String findAgencyIdByName(String agencyName) {
        String marker = null;
        Set<String> visitedMarkers = new HashSet<>();
        do {
            ListAgenciesV5Response response = listAgencies(200, marker, null);
            for (com.huaweicloud.sdk.iam.v5.model.Agency agency
                    : response == null || response.getAgencies() == null
                    ? Collections.<com.huaweicloud.sdk.iam.v5.model.Agency>emptyList()
                    : response.getAgencies()) {
                if (agencyName.equals(agency.getAgencyName())) {
                    return agency.getAgencyId();
                }
            }
            marker = nextMarker(response == null ? null : response.getPageInfo(), visitedMarkers);
        } while (marker != null);
        throw new IllegalArgumentException(
                "IAM agency already exists but could not be resolved by name: " + agencyName);
    }

    private Policy findSystemPolicyByName(String policyName) {
        String marker = null;
        Set<String> visitedMarkers = new HashSet<>();
        do {
            ListPoliciesV5Response response = listPolicies("system", 200, marker, null, null);
            for (Policy policy : response == null || response.getPolicies() == null
                    ? Collections.<Policy>emptyList() : response.getPolicies()) {
                if (policyName.equals(policy.getPolicyName())) {
                    return policy;
                }
            }
            marker = nextMarker(response == null ? null : response.getPageInfo(), visitedMarkers);
        } while (marker != null);
        throw new IllegalArgumentException("IAM system policy not found: " + policyName);
    }

    private static ListPoliciesV5Request policyListRequest(
            String policyType, Integer limit, String marker, String pathPrefix,
            Boolean onlyAttached) {
        ListPoliciesV5Request request = new ListPoliciesV5Request();
        if (JsonUtils.isNotBlank(policyType)) {
            request.withPolicyType(ListPoliciesV5Request.PolicyTypeEnum.fromValue(policyType));
        }
        if (limit != null) {
            request.withLimit(limit);
        }
        if (JsonUtils.isNotBlank(marker)) {
            request.withMarker(marker);
        }
        if (JsonUtils.isNotBlank(pathPrefix)) {
            request.withPathPrefix(pathPrefix);
        }
        if (onlyAttached != null) {
            request.withOnlyAttached(onlyAttached);
        }
        return request;
    }

    private static ListAgenciesV5Request agencyListRequest(
            Integer limit, String marker, String pathPrefix) {
        ListAgenciesV5Request request = new ListAgenciesV5Request();
        if (limit != null) {
            request.withLimit(limit);
        }
        if (JsonUtils.isNotBlank(marker)) {
            request.withMarker(marker);
        }
        if (JsonUtils.isNotBlank(pathPrefix)) {
            request.withPathPrefix(pathPrefix);
        }
        return request;
    }

    private static AttachAgencyPolicyV5Request agencyPolicyRequest(
            String agencyId, String policyId) {
        return new AttachAgencyPolicyV5Request()
                .withPolicyId(policyId)
                .withBody(new AttachAgencyPolicyReqBody().withAgencyId(agencyId));
    }

    private static String nextMarker(com.huaweicloud.sdk.iam.v5.model.PageInfo pageInfo,
                                     Set<String> visitedMarkers) {
        if (pageInfo == null || !JsonUtils.isNotBlank(pageInfo.getNextMarker())) {
            return null;
        }
        String marker = pageInfo.getNextMarker();
        if (!visitedMarkers.add(marker)) {
            throw new IllegalStateException("IAM pagination returned a repeated marker");
        }
        return marker;
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
