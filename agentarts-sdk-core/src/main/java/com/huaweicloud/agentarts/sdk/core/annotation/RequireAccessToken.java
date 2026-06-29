package com.huaweicloud.agentarts.sdk.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires an OAuth2 access token to be fetched before the annotated method is called.
 *
 * <p>The token is fetched via the AgentArts Identity service and injected as a
 * method parameter named by {@link #into()}.</p>
 *
 * <p>Mirrors Python {@code @require_access_token} decorator.</p>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * @RequireAccessToken(providerName = "github", authFlow = AuthFlow.M2M)
 * public void doSomething(String accessToken) { ... }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequireAccessToken {

    /** The credential provider name (e.g., "github", "google"). */
    String providerName();

    /** The parameter name to inject the token into. Default: "accessToken". */
    String into() default "accessToken";

    /** OAuth2 scopes to request. */
    String[] scopes() default {};

    /** Authentication flow type: M2M (machine-to-machine) or USER_FEDERATION. */
    AuthFlow authFlow();

    /** Custom callback URL for USER_FEDERATION flow. */
    String callbackUrl() default "";

    /** Whether to force re-authentication. */
    boolean forceAuthentication() default false;

    /** Whether to ignore SSL verification. */
    boolean ignoreSslVerification() default false;

    /** OAuth2 authentication flow types. */
    enum AuthFlow {
        /** Machine-to-machine flow (no user interaction). */
        M2M,
        /** User federation flow (user authorizes via browser). */
        USER_FEDERATION
    }
}
