package com.huaweicloud.agentarts.sdk.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires STS (Security Token Service) credentials to be fetched before the annotated method is called.
 *
 * <p>STS credentials are fetched via the AgentArts Identity service and injected as a
 * method parameter named by {@link #into()}.</p>
 *
 * <p>Mirrors Python {@code @require_sts_token} decorator.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequireStsToken {

    /** The credential provider name. */
    String providerName();

    /** The agency session name. */
    String agencySessionName();

    /** The parameter name to inject the STS credentials into. Default: "stsCredentials". */
    String into() default "stsCredentials";

    /** Duration in seconds for the STS token. */
    int durationSeconds() default 0;

    /** IAM policy JSON string. */
    String policy() default "";

    /** Source identity for auditing. */
    String sourceIdentity() default "";

    /** Transitive tag keys. */
    String[] transitiveTagKeys() default {};

    /** Whether to ignore SSL verification. */
    boolean ignoreSslVerification() default false;
}
