package com.huaweicloud.agentarts.sdk.tests.e2e;

import com.huaweicloud.agentarts.sdk.core.annotation.RequireApiKey;
import com.huaweicloud.agentarts.sdk.core.annotation.RequireStsToken;
import com.huaweicloud.agentarts.sdk.identity.IdentityClient;
import com.huaweicloud.agentarts.sdk.identity.auth.AuthInterceptor;
import com.huaweicloud.agentarts.sdk.runtime.context.AgentArtsRuntimeContext;
import com.huaweicloud.sdk.agentidentity.v1.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Auth decorator tests — (3 tests: api_key, sts_token, oauth2_3lo).
 *
 * <p>Exercises the real {@link AuthInterceptor} dynamic proxy end-to-end against
 * a shared workload identity + credential providers. The annotated handler
 * receives the fetched credential as its last parameter; the test verifies the
 * credential is fetched and injected (matching the Python {@code @require_api_key}
 * / {@code @require_sts_token} decorator coverage).</p>
 *
 * <p>Requires {@code AGENTARTS_TEST_ALLOW_CREATE=1}. STS injection additionally
 * requires {@code AGENTARTS_TEST_STS_AGENCY_URN}. The OAuth2 3LO flow is
 * interactive and is intentionally skipped.</p>
 *
 * <p><b>Note:</b> Java annotation attributes require compile-time constants, so
 * the credential-provider names are fixed strings (not run-id-unique). Creation
 * is idempotent — a leftover provider from a prior run is tolerated and cleaned
 * up at session end.</p>
 */
@Tag("e2e")
@DisplayName("Auth Decorator Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthDecoratorsTest {

    /** Fixed provider name so the {@code @RequireApiKey} annotation can reference it. */
    static final String API_KEY_PROVIDER = "aa-it-auth-akp";
    /** Fixed STS provider name for the {@code @RequireStsToken} annotation. */
    static final String STS_PROVIDER = "aa-it-auth-sts";
    private static final String STS_SESSION = "aa-it-session";

    private static IdentityClient identityClient;
    private static E2EResourceRegistry registry;
    private static String runId;
    private static String workloadIdentityName;

    @BeforeAll
    static void setUp() {
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

        // Create API key provider with the fixed annotation name (idempotent:
        // a leftover from a prior run is fine — getResourceApiKey returns its key).
        try {
            identityClient.createApiKeyCredentialProvider(
                    API_KEY_PROVIDER, "aa-it-dummy-api-key-fedcba9876543210");
        } catch (Exception e) {
            // Provider likely already exists from a prior run — tolerated.
            System.out.println("API key provider '" + API_KEY_PROVIDER
                    + "' already exists or create failed: " + e.getMessage());
        }
        registry.register(
                () -> {
                    try {
                        identityClient.getServiceClient().deleteApiKeyCredentialProvider(
                                new DeleteApiKeyCredentialProviderRequest()
                                        .withCredentialProviderName(API_KEY_PROVIDER));
                    } catch (Exception ignored) {}
                },
                "api-key-provider:" + API_KEY_PROVIDER);
    }

    @AfterAll
    static void tearDown() {
        if (registry != null) registry.cleanupAll();
    }

    /** Handler interface whose last param is the injected API key response. */
    public interface ApiKeyHandler {
        @RequireApiKey(providerName = API_KEY_PROVIDER)
        String handle(Map<String, Object> payload, GetResourceApiKeyResponse apiKey);
    }

    // 1. test_require_api_key_injects_key
    @Test @Order(1)
    @DisplayName("@RequireApiKey injects API key response into handler")
    void testRequireApiKeyInjectsKey() {
        AtomicReference<GetResourceApiKeyResponse> captured = new AtomicReference<>();
        ApiKeyHandler impl = (payload, apiKey) -> {
            captured.set(apiKey);
            return "ok";
        };
        ApiKeyHandler proxy = AuthInterceptor.wrap(impl, ApiKeyHandler.class, identityClient);

        // Inject a workload access token into the runtime context so the
        // interceptor fetches the resource API key for this identity.
        String token = identityClient.createWorkloadAccessToken(workloadIdentityName);
        AgentArtsRuntimeContext.setWorkloadAccessToken(token);
        try {
            // Pass null for the credential slot; the interceptor replaces it.
            String result = proxy.handle(Map.of("x", 1), null);
            assertEquals("ok", result);
            assertNotNull(captured.get(), "API key response should be injected");
            assertNotNull(captured.get().getApiKey(), "injected api key must be non-empty");
            assertFalse(captured.get().getApiKey().isEmpty());
        } finally {
            AgentArtsRuntimeContext.clear();
        }
    }

    /** Handler interface whose last param is the injected STS credentials. */
    public interface StsHandler {
        @RequireStsToken(providerName = STS_PROVIDER, agencySessionName = STS_SESSION)
        String handle(GetResourceStsTokenResponse stsCredentials);
    }

    // 2. test_require_sts_token_injects_credentials
    @Test @Order(2)
    @DisplayName("@RequireStsToken injects STS credentials into handler")
    void testRequireStsTokenInjectsCredentials() {
        String urn = E2EConfig.getStsAgencyUrn();
        assumeTrue(urn != null && !urn.isEmpty(),
                "Set AGENTARTS_TEST_STS_AGENCY_URN (iam::<agency_name>) to exercise STS credential-provider lifecycle");

        // Create (or reuse) the STS provider bound to the agency URN.
        try {
            identityClient.createStsCredentialProvider(STS_PROVIDER, urn);
        } catch (Exception e) {
            System.out.println("STS provider '" + STS_PROVIDER
                    + "' already exists or create failed: " + e.getMessage());
        }
        registry.register(
                () -> {
                    try {
                        identityClient.getServiceClient().deleteStsCredentialProvider(
                                new DeleteStsCredentialProviderRequest()
                                        .withCredentialProviderName(STS_PROVIDER));
                    } catch (Exception ignored) {}
                },
                "sts-provider:" + STS_PROVIDER);

        AtomicReference<GetResourceStsTokenResponse> captured = new AtomicReference<>();
        StsHandler impl = creds -> {
            captured.set(creds);
            return "ok";
        };
        StsHandler proxy = AuthInterceptor.wrap(impl, StsHandler.class, identityClient);

        String token = identityClient.createWorkloadAccessToken(workloadIdentityName);
        AgentArtsRuntimeContext.setWorkloadAccessToken(token);
        try {
            String result = proxy.handle(null);
            assertEquals("ok", result);
            assertNotNull(captured.get(), "STS credentials response should be injected");
            assertNotNull(captured.get().getCredentials(), "credentials must be present");
            assertNotNull(captured.get().getCredentials().getAccessKeyId(),
                    "access_key_id must be non-null");
            assertNotNull(captured.get().getCredentials().getSecretAccessKey(),
                    "secret_access_key must be non-null");
        } finally {
            AgentArtsRuntimeContext.clear();
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
