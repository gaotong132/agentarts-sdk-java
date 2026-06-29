package com.huaweicloud.agentarts.spring;

import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.core.PingStatus;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot AutoConfiguration for AgentArts SDK.
 *
 * <p>Auto-configures the following beans when the corresponding SDK modules
 * are on the classpath:</p>
 * <ul>
 *   <li>{@link AgentArtsRuntimeApp} — Vert.x HTTP server for agent endpoints</li>
 *   <li>{@link AgentArtsProperties} — bound from {@code agentarts.*} properties</li>
 * </ul>
 *
 * <p>The runtime server auto-starts when {@code agentarts.runtime.auto-start=true}.
 * Health indicator is provided when Spring Boot Actuator is on the classpath.</p>
 *
 * <h3>Usage:</h3>
 * <pre>
 * # application.yml
 * agentarts:
 *   region: cn-southwest-2
 *   runtime:
 *     port: 8080
 *     max-concurrency: 15
 *     auto-start: true
 * </pre>
 */
@AutoConfiguration
@ConditionalOnClass(AgentArtsRuntimeApp.class)
@EnableConfigurationProperties(AgentArtsProperties.class)
public class AgentArtsAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(AgentArtsAutoConfiguration.class);

    /**
     * Create the AgentArts Runtime application bean.
     */
    @Bean
    @ConditionalOnMissingBean
    public AgentArtsRuntimeApp agentArtsRuntimeApp(AgentArtsProperties properties) {
        int maxConcurrency = properties.getRuntime().getMaxConcurrency();
        LOG.info("Creating AgentArtsRuntimeApp with maxConcurrency={}, region={}",
                maxConcurrency, properties.resolveRegion());
        return new AgentArtsRuntimeApp(maxConcurrency);
    }

    /**
     * Auto-start the runtime server if port is configured.
     */
    @Bean
    @ConditionalOnProperty(prefix = "agentarts.runtime", name = "auto-start", havingValue = "true", matchIfMissing = false)
    public AgentArtsRuntimeStarter agentArtsRuntimeStarter(
            AgentArtsRuntimeApp app, AgentArtsProperties properties) {
        return new AgentArtsRuntimeStarter(app, properties);
    }

    /**
     * Inner class that manages runtime lifecycle within Spring context.
     * Uses Spring's InitializingBean/DisposableBean instead of jakarta.annotation.
     */
    public static class AgentArtsRuntimeStarter implements InitializingBean, DisposableBean {

        private static final Logger LOG = LoggerFactory.getLogger(AgentArtsRuntimeStarter.class);

        private final AgentArtsRuntimeApp app;
        private final int port;

        public AgentArtsRuntimeStarter(AgentArtsRuntimeApp app, AgentArtsProperties properties) {
            this.app = app;
            this.port = properties.getRuntime().getPort();
        }

        @Override
        public void afterPropertiesSet() {
            app.setPingHandler(() -> PingStatus.HEALTHY);
            LOG.info("Auto-starting AgentArts Runtime on port {}", port);
            new Thread(() -> app.run(port), "agentarts-runtime").start();
        }

        @Override
        public void destroy() {
            LOG.info("Stopping AgentArts Runtime");
            app.stop();
        }
    }
}
