package com.huaweicloud.agentarts.sdk.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an async background task for the AgentArts runtime.
 *
 * <p>The annotated method runs as a tracked background task. The runtime's
 * {@code /ping} endpoint will report {@code HealthyBusy} while tasks are running.</p>
 *
 * <p>Mirrors Python {@code @app.async_task} decorator.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AsyncTask {
}
