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
    }

    // 2. test_list_api_key_credential_providers
    @Test
    @DisplayName("list_api_key_credential_providers returns a list")
    void testListApiKeyCredentialProviders() {
        var result = identityClient.getServiceClient()
                .listApiKeyCredentialProviders(new ListApiKeyCredentialProvidersRequest());
        assertNotNull(result);
    }

    // 3. test_list_oauth2_credential_providers
    @Test
    @DisplayName("list_oauth2_credential_providers returns a list")
    void testListOauth2CredentialProviders() {
        var result = identityClient.getServiceClient()
                .listOauth2CredentialProviders(new ListOauth2CredentialProvidersRequest());
        assertNotNull(result);
    }

    // 4. test_list_sts_credential_providers
    @Test
    @DisplayName("list_sts_credential_providers returns a list")
    void testListStsCredentialProviders() {
        var result = identityClient.getServiceClient()
                .listStsCredentialProviders(new ListStsCredentialProvidersRequest());
        assertNotNull(result);
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
