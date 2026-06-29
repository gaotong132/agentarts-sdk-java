package com.huaweicloud.agentarts.sdk.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as the WebSocket handler for the AgentArts runtime.
 *
 * <p>The annotated method will handle {@code /ws} WebSocket connections.
 * It should accept a Vert.x {@code ServerWebSocket} and optionally a {@code RequestContext}.</p>
 *
 * <p>Mirrors Python {@code @app.websocket} decorator.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WebSocket {
}
