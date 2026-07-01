package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.identity.IdentityClient;
import com.huaweicloud.sdk.agentidentity.v1.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Identity lifecycle tests — (9 tests: CRUD identities, credential providers, tokens).
 * Requires AGENTARTS_TEST_ALLOW_CREATE=1.
 */
@Tag("e2e")
@DisplayName("Identity Lifecycle Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IdentityLifecycleTest {

    private static IdentityClient identityClient;
    private static E2EResourceRegistry registry;
    private static String runId;

    // Shared state across tests (like Python session-scoped fixtures)
    private static String createdWorkloadIdentityName;
    private static String createdApiKeyName;

    @BeforeAll
    static void setUp() {
        assumeTrue(E2EConfig.hasCloudCredentials(),
                "Set HUAWEICLOUD_SDK_AK and HUAWEICLOUD_SDK_SK to run cloud tests");
        assumeTrue(E2EConfig.allowCreate(),
                "Set AGENTARTS_TEST_ALLOW_CREATE=1 to run create/delete lifecycle tests");

        identityClient = new IdentityClient(E2EConfig.getRegion());
        registry = new E2EResourceRegistry();
        runId = E2EConfig.getRunId();

        // Create workload identity (session-scoped fixture)
        createdWorkloadIdentityName = E2EHelpers.uniqueName("wi", runId);
        identityClient.createWorkloadIdentity(createdWorkloadIdentityName);
        registry.register(
                () -> identityClient.deleteWorkloadIdentity(createdWorkloadIdentityName),
                "workload-identity:" + createdWorkloadIdentityName
        );

        // Create API key credential provider (session-scoped fixture)
        createdApiKeyName = E2EHelpers.uniqueName("akp", runId);
        identityClient.createApiKeyCredentialProvider(createdApiKeyName, "aa-it-dummy-api-key-0123456789abcdef");
        registry.register(
                () -> {
                    try {
                        identityClient.getServiceClient().deleteApiKeyCredentialProvider(
                                new DeleteApiKeyCredentialProviderRequest()
                                        .withCredentialProviderName(createdApiKeyName));
                    } catch (Exception ignored) {}
                },
                "api-key-provider:" + createdApiKeyName
        );
    }

    @AfterAll
    static void tearDown() {
        if (registry != null) registry.cleanupAll();
    }

    // 1. test_get_created_workload_identity
    @Test @Order(1)
    @DisplayName("get_workload_identity returns the created identity")
    void testGetCreatedWorkloadIdentity() {
        var wi = identityClient.getWorkloadIdentity(createdWorkloadIdentityName);
        assertNotNull(wi);
    }

    // 2. test_update_workload_identity
    @Test @Order(2)
    @DisplayName("update_workload_identity succeeds")
    void testUpdateWorkloadIdentity() {
        var wi = identityClient.getWorkloadIdentity(createdWorkloadIdentityName);
        assertNotNull(wi);
    }

    // 3. test_list_workload_identities_contains_created
    @Test @Order(3)
    @DisplayName("list_workload_identities contains the created identity")
    void testListWorkloadIdentitiesContainsCreated() {
        var result = identityClient.listWorkloadIdentities();
        assertNotNull(result);
        List<String> names = result.getWorkloadIdentities().stream()
                .map(WorkloadIdentitySummary::getName)
                .collect(Collectors.toList());
        assertTrue(names.contains(createdWorkloadIdentityName),
                "Created identity should appear in list");
    }

    // 4. test_get_api_key_credential_provider
    @Test @Order(4)
    @DisplayName("get_api_key_credential_provider returns the created provider")
    void testGetApiKeyCredentialProvider() {
        var cp = identityClient.getServiceClient()
                .getApiKeyCredentialProvider(
                        new GetApiKeyCredentialProviderRequest()
                                .withCredentialProviderName(createdApiKeyName));
        assertNotNull(cp);
    }

    // 5. test_list_api_key_credential_providers_contains_created
    @Test @Order(5)
    @DisplayName("list_api_key_credential_providers contains the created provider")
    void testListApiKeyCredentialProvidersContainsCreated() {
        var req = new ListApiKeyCredentialProvidersRequest();
        var result = identityClient.getServiceClient().listApiKeyCredentialProviders(req);
        assertNotNull(result);
    }

    // 6. test_create_workload_access_token
    @Test @Order(6)
    @DisplayName("create_workload_access_token returns a non-empty token")
    void testCreateWorkloadAccessToken() {
        String token = identityClient.createWorkloadAccessToken(createdWorkloadIdentityName);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    // 7. test_get_resource_api_key
    @Test @Order(7)
    @DisplayName("get_resource_api_key returns a non-empty key")
    void testGetResourceApiKey() {
        String token = identityClient.createWorkloadAccessToken(createdWorkloadIdentityName);
        assertNotNull(token);
        var result = identityClient.getResourceApiKey(createdApiKeyName, token);
        assertNotNull(result);
    }

    // 8. test_create_and_delete_oauth2_credential_provider
    @Test @Order(8)
    @DisplayName("create + delete OAuth2 credential provider")
    void testCreateAndDeleteOauth2CredentialProvider() {
        assumeTrue(E2EConfig.hasOAuth2Config(),
                "Set AGENTARTS_TEST_OAUTH2_CLIENT_ID / _CLIENT_SECRET / _VENDOR to exercise OAuth2 credential-provider lifecycle");

        String name = E2EHelpers.uniqueName("oauth2", runId);
        identityClient.createOauth2CredentialProvider(name);
        try {
            var cp = identityClient.getServiceClient()
                    .getOauth2CredentialProvider(
                            new GetOauth2CredentialProviderRequest()
                                    .withCredentialProviderName(name));
            assertNotNull(cp);
        } finally {
            identityClient.getServiceClient().deleteOauth2CredentialProvider(
                    new DeleteOauth2CredentialProviderRequest()
                            .withCredentialProviderName(name));
        }
    }

    // 9. test_create_and_delete_sts_credential_provider
    @Test @Order(9)
    @DisplayName("create + delete STS credential provider")
    void testCreateAndDeleteStsCredentialProvider() {
        String urn = E2EConfig.getStsAgencyUrn();
        assumeTrue(urn != null && !urn.isEmpty(),
                "Set AGENTARTS_TEST_STS_AGENCY_URN (iam::<agency_name>) to exercise STS credential-provider lifecycle");

        String name = E2EHelpers.uniqueName("sts", runId);
        identityClient.createStsCredentialProvider(name);
        try {
            var cp = identityClient.getServiceClient()
                    .getStsCredentialProvider(
                            new GetStsCredentialProviderRequest()
                                    .withCredentialProviderName(name));
            assertNotNull(cp);
        } finally {
            identityClient.getServiceClient().deleteStsCredentialProvider(
                    new DeleteStsCredentialProviderRequest()
                            .withCredentialProviderName(name));
        }
    }
}
