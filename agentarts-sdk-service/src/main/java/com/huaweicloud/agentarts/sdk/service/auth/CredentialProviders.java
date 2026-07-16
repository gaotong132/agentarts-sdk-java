package com.huaweicloud.agentarts.sdk.service.auth;

import com.huaweicloud.sdk.core.auth.AbstractCredentials;
import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.CredentialProviderChain;
import com.huaweicloud.sdk.core.auth.GlobalCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.auth.ICredentialProvider;

import java.util.Objects;

/**
 * Credential-provider helpers shared by AgentArts service clients.
 *
 * <p>The default chains are supplied by Huawei Cloud SDK Core and support its
 * standard environment, profile, metadata and pod-identity credential sources.
 * A provider is resolved once when building generated Huawei Cloud clients;
 * credentials returned by metadata providers retain the SDK's refresh support.
 * The low-level HTTP client resolves the provider for every signed request so a
 * custom rotating provider can atomically replace the credential snapshot.</p>
 */
public final class CredentialProviders {

    private CredentialProviders() {
    }

    public static ICredentialProvider defaultBasicProvider() {
        return CredentialProviderChain.basic();
    }

    public static ICredentialProvider defaultGlobalProvider() {
        return CredentialProviderChain.global();
    }

    public static BasicCredentials resolveBasic(ICredentialProvider provider) {
        return resolve(provider, BasicCredentials.class, "basic");
    }

    public static GlobalCredentials resolveGlobal(ICredentialProvider provider) {
        return resolve(provider, GlobalCredentials.class, "global");
    }

    private static <T extends AbstractCredentials<T>> T resolve(
            ICredentialProvider provider, Class<T> credentialType, String description) {
        Objects.requireNonNull(provider, "credentialProvider must not be null");
        final ICredential credential;
        try {
            credential = provider.getCredentials();
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Failed to load Huawei Cloud " + description + " credentials from the configured provider", e);
        }
        if (!credentialType.isInstance(credential)) {
            String actualType = credential == null ? "null" : credential.getClass().getName();
            throw new IllegalStateException("Credential provider returned " + actualType
                    + "; expected " + credentialType.getName());
        }
        T resolved = credentialType.cast(credential);
        if (isBlank(resolved.getAk()) || isBlank(resolved.getSk())) {
            throw new IllegalStateException("Credential provider returned incomplete Huawei Cloud credentials: "
                    + "both AK and SK are required");
        }
        return resolved;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
