package com.huaweicloud.agentarts.toolkit.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
                    System.out.println("Setting env: " + key + "=[REDACTED]");
                    System.setProperty(key, value);
                }
            }
        }

        File configFile = resolveConfigFile(configPath, projectPath);
        String entrypoint = resolveEntrypoint(configFile);

        System.out.println("Starting AgentArts dev server on " + host + ":" + port
                + (configFile != null ? " (config: " + configFile + ")" : "")
                + (entrypoint != null ? " (entrypoint: " + entrypoint + ")" : ""));

        AgentArtsRuntimeApp app = buildApp(entrypoint, projectPath);

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

    static File resolveConfigFile(String configPath, String projectPath) {
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
    static String resolveEntrypoint(File configFile) {
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
     * Build the runtime app. Loads the entrypoint class declared in the config from
     * the project's compiled {@code target/classes} (plus any {@code target/dependency/*.jar})
     * via a URLClassLoader, and invokes its {@code public static AgentArtsRuntimeApp createApp()}
     * factory — the same contract the Python {@code create_app} entrypoint uses. If the class
     * cannot be loaded (not compiled / not on classpath) or has no {@code createApp()} factory,
     * a clear warning is printed and a default echo entrypoint is used so the dev server is
     * still usable.
     */
    static AgentArtsRuntimeApp buildApp(String entrypointClassName, String projectPath) {
        if (entrypointClassName != null) {
            ClassLoader loader = buildProjectClassLoader(projectPath);
            try {
                Class<?> cls = Class.forName(entrypointClassName, true, loader);
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
                System.err.println("Warning: entrypoint '" + entrypointClassName
                        + "' has no 'public static AgentArtsRuntimeApp createApp()' factory"
                        + " — using default echo entrypoint. (The scaffolded Agent must expose"
                        + " createApp() for `agentarts dev` to drive it.)");
            } catch (ClassNotFoundException e) {
                System.err.println("Warning: entrypoint class '" + entrypointClassName
                        + "' not found on project classpath — using default echo entrypoint."
                        + " Run 'mvn compile' so target/classes/com/example/Agent.class exists"
                        + " (and ensure the config's entrypoint matches the class).");
            } catch (Exception e) {
                System.err.println("Warning: could not load entrypoint '" + entrypointClassName
                        + "': " + e.getMessage() + " — using default echo entrypoint.");
            }
        }
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
        // Sensible default: echo the incoming message back under "response".
        app.setEntrypoint((Map<String, Object> payload) ->
                Map.of("response", payload.getOrDefault("message", "")));
        return app;
    }

    /**
     * Build a URLClassLoader over the project's {@code target/classes} and
     * {@code target/dependency/*.jar} so the user's compiled Agent is visible to
     * {@code Class.forName}. The parent loader is the CLI's own loader, which holds
     * the SDK classes the Agent depends on. Returns the CLI's loader unchanged when
     * no project output directory exists.
     */
    static ClassLoader buildProjectClassLoader(String projectPath) {
        Path base = (projectPath != null && !projectPath.isEmpty())
                ? Path.of(projectPath) : Path.of(".");
        List<URL> urls = new ArrayList<>();
        Path classesDir = base.resolve("target/classes");
        if (Files.isDirectory(classesDir)) {
            urls.add(toUrl(classesDir));
        }
        Path depDir = base.resolve("target/dependency");
        if (Files.isDirectory(depDir)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(depDir, "*.jar")) {
                for (Path jar : ds) {
                    urls.add(toUrl(jar));
                }
            } catch (IOException ignored) { }
        }
        if (urls.isEmpty()) {
            return DevOperation.class.getClassLoader();
        }
        URL[] array = urls.toArray(new URL[0]);
        return new URLClassLoader(array, DevOperation.class.getClassLoader());
    }

    private static URL toUrl(Path path) {
        try {
            return path.toAbsolutePath().normalize().toUri().toURL();
        } catch (java.net.MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
