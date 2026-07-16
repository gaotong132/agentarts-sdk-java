package com.huaweicloud.agentarts.sdk.service.auth;

import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.GlobalCredentials;
import com.huaweicloud.sdk.core.auth.ICredentialProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialProvidersTest {

    @Test
    void resolvesBasicCredentialWithoutCopyingAwayRefreshState() {
        BasicCredentials credential = new BasicCredentials()
                .withAk("ak")
                .withSk("sk")
                .withSecurityToken("token");

        assertEquals(credential, CredentialProviders.resolveBasic(() -> credential));
    }

    @Test
    void resolvesGlobalCredential() {
        GlobalCredentials credential = new GlobalCredentials().withAk("ak").withSk("sk");

        assertEquals(credential, CredentialProviders.resolveGlobal(() -> credential));
    }

    @Test
    void rejectsMissingAndWrongCredentialTypes() {
        IllegalStateException missing = assertThrows(IllegalStateException.class,
                () -> CredentialProviders.resolveBasic(() -> null));
        assertTrue(missing.getMessage().contains("expected"));

        ICredentialProvider wrongType = () -> new GlobalCredentials().withAk("ak").withSk("sk");
        assertThrows(IllegalStateException.class, () -> CredentialProviders.resolveBasic(wrongType));

        ICredentialProvider incomplete = () -> new BasicCredentials().withAk("ak").withSk(" ");
        assertThrows(IllegalStateException.class, () -> CredentialProviders.resolveBasic(incomplete));
    }

    @Test
    void wrapsProviderFailureWithoutLeakingCredentialMaterial() {
        ICredentialProvider provider = () -> {
            throw new IllegalArgumentException("provider failed");
        };

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> CredentialProviders.resolveBasic(provider));
        assertTrue(exception.getMessage().contains("Failed to load"));
    }
}
