package com.huaweicloud.agentarts.sdk.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as the main invocation entrypoint for the AgentArts runtime.
 *
 * <p>The annotated method will handle {@code POST /invocations} requests.
 * It should accept a JSON payload (as String, Map, or custom POJO) and optionally
 * a {@code RequestContext} as the second parameter.</p>
 *
 * <p>Marks a method as the main invocation handler for the runtime.</p>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * @Entrypoint
 * public Object handle(Map<String, Object> payload, RequestContext context) {
 *     return Map.of("result", "processed: " + payload);
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Entrypoint {
}
