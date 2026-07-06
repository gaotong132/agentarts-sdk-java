package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.identity.IdentityClient;
import com.huaweicloud.sdk.agentidentity.v1.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Identity read-only tests — (5 tests: list identities, list providers, get/token).
 */
@Tag("e2e")
@DisplayName("Identity Read-Only Tests")
class IdentityReadonlyTest {

    private static IdentityClient identityClient;

    @BeforeAll
    static void setUp() {
        assumeTrue(E2EConfig.hasCloudCredentials(),
                "Set HUAWEICLOUD_SDK_AK and HUAWEICLOUD_SDK_SK to run cloud tests");
        identityClient = new IdentityClient(E2EConfig.getRegion());
    }

    // 1. test_list_workload_identities
    @Test
    @DisplayName("list_workload_identities returns a list")
    void testListWorkloadIdentities() {
        var result = identityClient.listWorkloadIdentities();
        assertNotNull(result);
        assertNotNull(result.getWorkloadIdentities(), "items should be a list");
        assertTrue(result.getWorkloadIdentities() instanceof java.util.List,
                "workload identities should be a List");
    }

    // 2. test_list_api_key_credential_providers
    @Test
    @DisplayName("list_api_key_credential_providers returns a parseable response")
    void testListApiKeyCredentialProviders() {
        var result = identityClient.listApiKeyCredentialProviders();
        assertNotNull(result);
        assertNotNull(result.getCredentialProviders(), "items should be a list");
        assertTrue(result.getCredentialProviders() instanceof java.util.List,
                "api key providers should be a List");
    }

    // 3. test_list_oauth2_credential_providers
    @Test
    @DisplayName("list_oauth2_credential_providers returns a parseable response")
    void testListOauth2CredentialProviders() {
        var result = identityClient.listOauth2CredentialProviders();
        assertNotNull(result);
        assertNotNull(result.getCredentialProviders(), "items should be a list");
        assertTrue(result.getCredentialProviders() instanceof java.util.List,
                "oauth2 providers should be a List");
    }

    // 4. test_list_sts_credential_providers
    @Test
    @DisplayName("list_sts_credential_providers returns a parseable response")
    void testListStsCredentialProviders() {
        var result = identityClient.listStsCredentialProviders();
        assertNotNull(result);
        assertNotNull(result.getCredentialProviders(), "items should be a list");
        assertTrue(result.getCredentialProviders() instanceof java.util.List,
                "sts providers should be a List");
    }

    // 5. test_get_and_token_for_workload_identity
    @Test
    @DisplayName("get + token for workload identity (creates temp if no pre-provisioned)")
    void testGetAndTokenForWorkloadIdentity() {
        String name = E2EConfig.getPreWorkloadIdentity();
        boolean isTemporary = false;

        if (name == null || name.isEmpty()) {
            // No pre-provisioned identity — create a temporary one
            name = "aa-it-readonly-" + E2EConfig.getRunId();
            identityClient.createWorkloadIdentity(name);
            isTemporary = true;
        }

        try {
            var wi = identityClient.getWorkloadIdentity(name);
            assertNotNull(wi);
            assertNotNull(wi.getWorkloadIdentity(), "workload identity body should be present");
            assertEquals(name, wi.getWorkloadIdentity().getName(),
                    "get_workload_identity should return the identity whose name matches the request");

            String token = identityClient.createWorkloadAccessToken(name);
            assertNotNull(token);
            assertFalse(token.isEmpty());
        } finally {
            if (isTemporary) {
                try { identityClient.deleteWorkloadIdentity(name); } catch (Exception ignored) {}
            }
        }
    }
}
