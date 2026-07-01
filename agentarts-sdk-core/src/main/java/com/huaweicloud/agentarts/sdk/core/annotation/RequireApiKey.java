package com.huaweicloud.agentarts.sdk.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires an API key to be fetched before the annotated method is called.
 *
 * <p>The API key is fetched via the AgentArts Identity service and injected as a
 * method parameter named by {@link #into()}.</p>
 *
 * <p>Injects an API key into the annotated method parameter.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequireApiKey {

    /** The credential provider name. */
    String providerName();

    /** The parameter name to inject the API key into. Default: "apiKey". */
    String into() default "apiKey";

    /** Whether to ignore SSL verification. */
    boolean ignoreSslVerification() default false;
}
