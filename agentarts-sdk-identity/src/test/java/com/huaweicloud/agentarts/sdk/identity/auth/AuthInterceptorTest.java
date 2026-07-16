package com.huaweicloud.agentarts.sdk.identity.auth;

import com.huaweicloud.agentarts.sdk.core.annotation.RequireAccessToken;
import com.huaweicloud.agentarts.sdk.core.annotation.RequireApiKey;
import com.huaweicloud.agentarts.sdk.core.annotation.RequireStsToken;
import com.huaweicloud.agentarts.sdk.runtime.context.AgentArtsRuntimeContext;
import com.huaweicloud.sdk.agentidentity.v1.model.GetResourceOauth2TokenRequestBody;
import com.huaweicloud.sdk.agentidentity.v1.model.GetResourceStsTokenResponseBodyCredentials;
import com.huaweicloud.sdk.agentidentity.v1.model.StsTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthInterceptorTest {

    private static final String WORKLOAD_TOKEN = "workload-token";

    @AfterEach
    void clearContext() {
        AgentArtsRuntimeContext.clear();
    }

    interface ApiKeyHandler {
        @RequireApiKey(providerName = "provider", into = "credential")
        String handle(String credential, int payload);
    }

    @Test
    void injectsApiKeyByDeclaredParameterName() {
        StubResolver resolver = new StubResolver();
        resolver.apiKey = "secret-api-key";
        AgentArtsRuntimeContext.setWorkloadAccessToken(WORKLOAD_TOKEN);

        ApiKeyHandler proxy = AuthInterceptor.wrap(
                (credential, payload) -> credential + ":" + payload,
                ApiKeyHandler.class, resolver);

        assertEquals("secret-api-key:7", proxy.handle(null, 7));
        assertEquals("provider", resolver.providerName);
        assertEquals(WORKLOAD_TOKEN, resolver.workloadToken);
    }

    interface OAuthHandler {
        @RequireAccessToken(
                providerName = "oauth-provider",
                into = "accessToken",
                scopes = {"read", "write"},
                authFlow = RequireAccessToken.AuthFlow.M2M,
                callbackUrl = "https://callback.example.com",
                forceAuthentication = true)
        String handle(int payload, String accessToken);
    }

    @Test
    void forwardsOauthAnnotationOptionsAndInjectsTokenValue() {
        StubResolver resolver = new StubResolver();
        resolver.oauthToken = "oauth-token";
        AgentArtsRuntimeContext.setWorkloadAccessToken(WORKLOAD_TOKEN);

        OAuthHandler proxy = AuthInterceptor.wrap(
                (payload, accessToken) -> payload + ":" + accessToken,
                OAuthHandler.class, resolver);

        assertEquals("3:oauth-token", proxy.handle(3, null));
        assertEquals("oauth-provider", resolver.providerName);
        assertEquals(GetResourceOauth2TokenRequestBody.Oauth2FlowEnum.M2M, resolver.authFlow);
        assertEquals(List.of("read", "write"), resolver.scopes);
        assertEquals("https://callback.example.com", resolver.callbackUrl);
        assertTrue(resolver.forceAuthentication);
    }

    interface StsHandler {
        @RequireStsToken(
                providerName = "sts-provider",
                agencySessionName = "session",
                into = "credentials",
                durationSeconds = 900,
                policy = "policy-json",
                sourceIdentity = "source",
                transitiveTagKeys = {"team"})
        String handle(GetResourceStsTokenResponseBodyCredentials credentials, int payload);
    }

    @Test
    void forwardsStsRestrictionsAndInjectsCredentialsValue() {
        StubResolver resolver = new StubResolver();
        GetResourceStsTokenResponseBodyCredentials credentials =
                new GetResourceStsTokenResponseBodyCredentials().withAccessKeyId("temporary-ak");
        resolver.stsCredentials = credentials;
        AgentArtsRuntimeContext.setWorkloadAccessToken(WORKLOAD_TOKEN);

        StsHandler proxy = AuthInterceptor.wrap(
                (injected, payload) -> injected.getAccessKeyId() + ":" + payload,
                StsHandler.class, resolver);

        assertEquals("temporary-ak:5", proxy.handle(null, 5));
        assertEquals("sts-provider", resolver.providerName);
        assertEquals("session", resolver.agencySessionName);
        assertEquals(900, resolver.durationSeconds);
        assertEquals("policy-json", resolver.policy);
        assertEquals("source", resolver.sourceIdentity);
        assertEquals(List.of("team"), resolver.transitiveTagKeys);
    }

    interface InvalidHandler {
        @RequireApiKey(providerName = "provider")
        void handle();
    }

    @Test
    void rejectsAnnotatedMethodWithoutInjectionParameter() {
        StubResolver resolver = new StubResolver();
        resolver.apiKey = "key";
        AgentArtsRuntimeContext.setWorkloadAccessToken(WORKLOAD_TOKEN);
        InvalidHandler proxy = AuthInterceptor.wrap(() -> { }, InvalidHandler.class, resolver);

        IllegalStateException error = assertThrows(IllegalStateException.class, proxy::handle);
        assertTrue(error.getMessage().contains("injection parameter"));
    }

    interface MisspelledIntoHandler {
        @RequireApiKey(providerName = "provider", into = "misspelled")
        String handle(String apiKey);
    }

    @Test
    void rejectsUnknownNamedInjectionParameter() {
        StubResolver resolver = new StubResolver();
        resolver.apiKey = "key";
        AgentArtsRuntimeContext.setWorkloadAccessToken(WORKLOAD_TOKEN);
        MisspelledIntoHandler proxy = AuthInterceptor.wrap(
                apiKey -> apiKey, MisspelledIntoHandler.class, resolver);

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> proxy.handle(null));
        assertTrue(error.getMessage().contains("misspelled"));
    }

    interface ThrowingHandler {
        String handle();
    }

    @Test
    void unwrapsTargetException() {
        StubResolver resolver = new StubResolver();
        IllegalArgumentException expected = new IllegalArgumentException("boom");
        ThrowingHandler proxy = AuthInterceptor.wrap(
                () -> { throw expected; }, ThrowingHandler.class, resolver);

        assertSame(expected, assertThrows(IllegalArgumentException.class, proxy::handle));
    }

    private static final class StubResolver implements AuthCredentialResolver {
        private String providerName;
        private String workloadToken;
        private String apiKey;
        private String oauthToken;
        private GetResourceOauth2TokenRequestBody.Oauth2FlowEnum authFlow;
        private List<String> scopes;
        private String callbackUrl;
        private boolean forceAuthentication;
        private GetResourceStsTokenResponseBodyCredentials stsCredentials;
        private String agencySessionName;
        private Integer durationSeconds;
        private String policy;
        private String sourceIdentity;
        private List<String> transitiveTagKeys;

        @Override
        public String getResourceApiKeyValue(String providerName, String workloadAccessToken) {
            record(providerName, workloadAccessToken);
            return apiKey;
        }

        @Override
        public String getResourceOauth2AccessToken(
                String providerName, String workloadAccessToken,
                GetResourceOauth2TokenRequestBody.Oauth2FlowEnum authFlow,
                List<String> scopes, String callbackUrl, boolean forceAuthentication) {
            record(providerName, workloadAccessToken);
            this.authFlow = authFlow;
            this.scopes = scopes;
            this.callbackUrl = callbackUrl;
            this.forceAuthentication = forceAuthentication;
            return oauthToken;
        }

        @Override
        public GetResourceStsTokenResponseBodyCredentials getResourceStsCredentials(
                String providerName, String workloadAccessToken, String agencySessionName,
                Integer durationSeconds, String policy, String sourceIdentity,
                List<StsTag> tags, List<String> transitiveTagKeys) {
            record(providerName, workloadAccessToken);
            this.agencySessionName = agencySessionName;
            this.durationSeconds = durationSeconds;
            this.policy = policy;
            this.sourceIdentity = sourceIdentity;
            this.transitiveTagKeys = transitiveTagKeys;
            return stsCredentials;
        }

        private void record(String providerName, String workloadToken) {
            this.providerName = providerName;
            this.workloadToken = workloadToken;
        }
    }
}
