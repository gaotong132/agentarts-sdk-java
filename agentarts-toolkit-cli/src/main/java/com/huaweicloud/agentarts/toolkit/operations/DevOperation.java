package com.huaweicloud.agentarts.toolkit.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Dev operation: run local development server.
 *
 * <p>Loads the entrypoint declared in {@code .agentarts_config.yaml}, instantiates an
 * {@link AgentArtsRuntimeApp}, and serves {@code /ping} and {@code POST /invocations}
 * on the requested port until the process is interrupted.</p>
 */
public class DevOperation {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * Start a local development server for the agent.
     *
     * @param port        server port (0 picks an ephemeral port)
     * @param host        bind host (currently advisory; the app binds to all interfaces)
     * @param reload      whether to enable hot-reload (currently advisory)
     * @param configPath  path to {@code .agentarts_config.yaml} (nullable)
     * @param projectPath project directory containing the config (nullable, defaults to cwd)
     * @param envVars     additional environment variables in KEY=VALUE format
     */
    public static void runDevServer(int port, String host, boolean reload,
                                     String configPath, String projectPath,
                                     String[] envVars) throws Exception {
        // Apply environment variables from --env flags
        if (envVars != null) {
            for (String env : envVars) {
                int eq = env.indexOf('=');
                if (eq > 0) {
                    String key = env.substring(0, eq);
                    String value = env.substring(eq + 1);
                    System.out.println("Setting env: " + key + "=" + value);
                    System.setProperty(key, value);
                }
            }
        }

        File configFile = resolveConfigFile(configPath, projectPath);
        String entrypoint = resolveEntrypoint(configFile);

        System.out.println("Starting AgentArts dev server on " + host + ":" + port
                + (configFile != null ? " (config: " + configFile + ")" : "")
                + (entrypoint != null ? " (entrypoint: " + entrypoint + ")" : ""));

        AgentArtsRuntimeApp app = buildApp(entrypoint);

        CountDownLatch stopLatch = new CountDownLatch(1);
        Thread shutdownHook = new Thread(() -> {
            stopLatch.countDown();
            try { app.stop(); } catch (Exception ignored) { }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        try {
            app.run(port);
            System.out.println("DEV_SERVER_LISTENING on port " + app.getPort());
            stopLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            try { Runtime.getRuntime().removeShutdownHook(shutdownHook); } catch (Exception ignored) { }
            try { app.stop(); } catch (Exception ignored) { }
        }
    }

    /** Backwards-compatible overload (no project path). */
    public static void runDevServer(int port, String host, boolean reload,
                                     String configPath, String[] envVars) throws Exception {
        runDevServer(port, host, reload, configPath, null, envVars);
    }

    private static File resolveConfigFile(String configPath, String projectPath) {
        File file;
        if (configPath != null && !configPath.isEmpty()) {
            file = new File(configPath);
        } else {
            Path base = (projectPath != null && !projectPath.isEmpty())
                    ? Path.of(projectPath) : Path.of(".");
            file = base.resolve(".agentarts_config.yaml").toFile();
        }
        return file.exists() ? file : null;
    }

    @SuppressWarnings("unchecked")
    private static String resolveEntrypoint(File configFile) {
        if (configFile == null) return null;
        try {
            Map<String, Object> root = YAML_MAPPER.readValue(configFile, Map.class);
            if (root == null) return null;
            Object defaultAgent = root.get("default_agent");
            Object agentsObj = root.get("agents");
            if (!(agentsObj instanceof Map<?, ?> agents) || defaultAgent == null) return null;
            Object agentCfg = agents.get(defaultAgent);
            if (!(agentCfg instanceof Map<?, ?> agent)) return null;
            Object baseObj = agent.get("base");
            if (!(baseObj instanceof Map<?, ?> base)) return null;
            Object ep = base.get("entrypoint");
            return ep instanceof String s && !s.isEmpty() ? s : null;
        } catch (Exception e) {
            System.err.println("Warning: failed to parse " + configFile + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Build the runtime app. If the entrypoint class is on the classpath and exposes a
     * {@code public static AgentArtsRuntimeApp createApp()} factory, use it; otherwise
     * create an app with a default echo entrypoint so the dev server is still usable.
     */
    private static AgentArtsRuntimeApp buildApp(String entrypointClassName) {
        if (entrypointClassName != null) {
            try {
                Class<?> cls = Class.forName(entrypointClassName);
                for (Method m : cls.getDeclaredMethods()) {
                    if (java.lang.reflect.Modifier.isStatic(m.getModifiers())
                            && m.getParameterCount() == 0
                            && AgentArtsRuntimeApp.class.isAssignableFrom(m.getReturnType())
                            && "createApp".equals(m.getName())) {
                        m.setAccessible(true);
                        AgentArtsRuntimeApp factoryApp = (AgentArtsRuntimeApp) m.invoke(null);
                        if (factoryApp != null) {
                            return factoryApp;
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                System.out.println("Entrypoint class '" + entrypointClassName
                        + "' not on classpath — using default echo entrypoint.");
            } catch (Exception e) {
                System.out.println("Could not load entrypoint '" + entrypointClassName
                        + "': " + e.getMessage() + " — using default echo entrypoint.");
            }
        }
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
        // Sensible default: echo the incoming message back under "response".
        app.setEntrypoint((Map<String, Object> payload) ->
                Map.of("response", payload.getOrDefault("message", "")));
        return app;
    }
}
