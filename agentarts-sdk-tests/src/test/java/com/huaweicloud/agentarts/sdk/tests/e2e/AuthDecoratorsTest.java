package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.identity.IdentityClient;
import com.huaweicloud.sdk.agentidentity.v1.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Auth decorator tests — (3 tests: api_key, sts_token, oauth2_3lo).
 * Requires AGENTARTS_TEST_ALLOW_CREATE=1.
 */
@Tag("e2e")
@DisplayName("Auth Decorator Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthDecoratorsTest {

    private static IdentityClient identityClient;
    private static E2EResourceRegistry registry;
    private static String runId;
    private static String workloadIdentityName;
    private static String apiKeyProviderName;
    private static Path tempDir;

    @BeforeAll
    static void setUp() throws Exception {
        assumeTrue(E2EConfig.hasCloudCredentials(),
                "Set HUAWEICLOUD_SDK_AK and HUAWEICLOUD_SDK_SK to run cloud tests");
        assumeTrue(E2EConfig.allowCreate(),
                "Set AGENTARTS_TEST_ALLOW_CREATE=1 to run create/delete lifecycle tests");

        identityClient = new IdentityClient(E2EConfig.getRegion());
        registry = new E2EResourceRegistry();
        runId = E2EConfig.getRunId();

        // Create workload identity
        workloadIdentityName = E2EHelpers.uniqueName("auth", runId);
        identityClient.createWorkloadIdentity(workloadIdentityName);
        registry.register(
                () -> identityClient.deleteWorkloadIdentity(workloadIdentityName),
                "workload-identity:" + workloadIdentityName);

        // Create API key provider
        apiKeyProviderName = E2EHelpers.uniqueName("akp", runId);
        identityClient.createApiKeyCredentialProvider(apiKeyProviderName, "aa-it-dummy-api-key-fedcba9876543210");
        registry.register(
                () -> {
                    try {
                        identityClient.getServiceClient().deleteApiKeyCredentialProvider(
                                new DeleteApiKeyCredentialProviderRequest()
                                        .withCredentialProviderName(apiKeyProviderName));
                    } catch (Exception ignored) {}
                },
                "api-key-provider:" + apiKeyProviderName);

        // Seed .agent_identity.json in temp dir
        tempDir = Files.createTempDirectory("aa-it-auth-");
        String configJson = "{\"workload_identity_name\": \"" + workloadIdentityName + "\"}";
        Files.writeString(tempDir.resolve(".agent_identity.json"), configJson);
    }

    @AfterAll
    static void tearDown() {
        if (registry != null) registry.cleanupAll();
        if (tempDir != null) {
            try {
                Files.walk(tempDir).sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile).forEach(File::delete);
            } catch (Exception ignored) {}
        }
    }

    // 1. test_require_api_key_injects_key
    @Test @Order(1)
    @DisplayName("require_api_key injects API key into handler")
    void testRequireApiKeyInjectsKey() {
        String token = identityClient.createWorkloadAccessToken(workloadIdentityName);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        var result = identityClient.getResourceApiKey(apiKeyProviderName, token);
        assertNotNull(result);
    }

    // 2. test_require_sts_token_injects_credentials
    @Test @Order(2)
    @DisplayName("require_sts_token injects STS credentials")
    void testRequireStsTokenInjectsCredentials() {
        String urn = E2EConfig.getStsAgencyUrn();
        assumeTrue(urn != null && !urn.isEmpty(),
                "Set AGENTARTS_TEST_STS_AGENCY_URN (iam::<agency_name>) to exercise STS credential-provider lifecycle");

        String stsName = E2EHelpers.uniqueName("sts", runId);
        identityClient.createStsCredentialProvider(stsName);
        try {
            String token = identityClient.createWorkloadAccessToken(workloadIdentityName);
            var creds = identityClient.getResourceStsToken(stsName, token, "aa-it-session");
            assertNotNull(creds);
        } finally {
            identityClient.getServiceClient().deleteStsCredentialProvider(
                    new DeleteStsCredentialProviderRequest()
                            .withCredentialProviderName(stsName));
        }
    }

    // 3. test_require_access_token_3lo_is_manual
    @Test @Order(3)
    @DisplayName("require_access_token (OAuth2 3LO) is manual — always skipped")
    void testRequireAccessToken3loIsManual() {
        assumeTrue(false,
                "require_access_token (OAuth2 3LO) is interactive; run manually with AGENTARTS_TEST_OAUTH2_*");
    }
}
