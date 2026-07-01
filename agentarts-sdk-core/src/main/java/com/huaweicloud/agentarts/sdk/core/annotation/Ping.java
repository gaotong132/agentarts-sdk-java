package com.huaweicloud.agentarts.sdk.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as the health-check ping handler for the AgentArts runtime.
 *
 * <p>The annotated method will handle {@code GET /ping} requests.
 * It should return a {@code PingStatus} value.</p>
 *
 * <p>Marks a method as the health check handler for the runtime.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Ping {
}
